package net.balintgergely.util;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
/**
 * Basically bit set but with a limit on how many bits there can be.
 * @author balintgergely
 *
 */
public class SimpleIndexSet implements IndexSet,Cloneable{
	static final long WORD_MASK = 0xffffffffffffffffL;
	long[] words;
	int length;
	public SimpleIndexSet(int length) {
		words = new long[(length+0x3F)/0x40];
		this.length = length;
	}
	public SimpleIndexSet(SimpleIndexSet other,boolean reflect){
		this.words = reflect ? other.words : other.words.clone();
		this.length = other.length;
	}
	@Override
	public int firstIndex() {
		int index = 0;
		do{
			long value = words[index];
			if(value != 0){
				return index*0x40+Long.numberOfTrailingZeros(value);
			}
			index++;
		}while(index < words.length);
		return -1;
	}
	@Override
	public int lastIndex() {
		int index = words.length;
		do{
			index--;
			long value = words[index];
			if(value != 0){
				return index*0x40+Long.numberOfTrailingZeros(Long.highestOneBit(value));
			}
		}while(index > 0);
		return -1;
	}
	//@Override
	public int firstUnsetIndex() {
		int index = 0;
		do{
			long value = words[index];
			if(value != WORD_MASK){
				index = index*0x40+Long.numberOfTrailingZeros(value);
				return index >= length ? -1 : index;
			}
			index++;
		}while(index < words.length);
		return -1;
	}
	//@Override
	public int lastUnsetIndex() {
		int index = words.length;
		long lastWordMask = WORD_MASK << length;
		if(lastWordMask != WORD_MASK){
			lastWordMask = ~lastWordMask;
			index--;
			long value = words[index];
			if(value != lastWordMask){
				return index*0x40+Long.numberOfTrailingZeros(Long.highestOneBit(value ^ lastWordMask));
			}
		}
		do{
			index--;
			long value = words[index];
			if(value != (~0)){
				return index*0x40+Long.numberOfTrailingZeros(Long.highestOneBit(value));
			}
		}while(index > 0);
		return -1;
	}
	@Override
	public int size() {
		int index = words.length,count = 0;
		do{
			index--;
			long value = words[index];
			count += Long.bitCount(value);
		}while(index > 0);
		return count;
	}
	@Override
	public boolean isEmpty() {
		int index = words.length;
		do{
			index--;
			if(words[index] != 0){
				return false;
			}
		}while(index > 0);
		return true;
	}
	@Override
	public boolean contains(int index) {
		return index >= 0 && index < length && ((words[index/0x40]) & (1l << index)) != 0;
	}
	@Override
	public boolean add(int index) {
		if(index < 0 || index >= length){
			throw new IndexOutOfBoundsException();
		}
		int wordIndex = index/0x40;
		long word = words[wordIndex];
		long value = word | (1l << index);
		if(value != word){
			words[wordIndex] = value;
			return true;
		}
		return false;
	}
	@Override
	public Object remove(int index) {
		long bit = 1l << index;
		index /= 0x40;
		long value = words[index];
		if((value & bit) != 0){
			words[index] &= ~bit;
		}
		return null;
	}
	class InItr implements PrimitiveIterator.OfInt,Spliterator.OfInt{
		InItr(int offset,int index,int limit){
			this.index = index;
			this.limit = limit;
			this.offset = offset;
		}
		private int lastIndex = -1;
		private int offset;
		private int index;
		private int limit;
		@Override
		public boolean hasNext() {
			while(index < limit){
				if(contains(index)){
					return true;
				}
				index++;
			}
			return false;
		}
		@Override
		public int nextInt() {
			if(hasNext()){
				return (lastIndex = index++)-offset;
			}
			throw new NoSuchElementException();
		}
		@Override
		public void remove() {
			if(lastIndex < 0){
				throw new IllegalStateException();
			}
			SimpleIndexSet.this.remove(lastIndex);
			lastIndex = -1;
		}
		@Override
		public boolean tryAdvance(IntConsumer consumer) {
			if(hasNext()){
				consumer.accept(next());
				return true;
			}
			return false;
		}
		@Override
		public Spliterator.OfInt trySplit() {
			int half = index+(limit-index)/2;
			if(half != index){
				InItr n = new InItr(index, half, offset);
				index = half;
				return n;
			}
			return null;
		}
		@Override
		public long estimateSize() {
			return limit-index;
		}
		@Override
		public int characteristics() {
			return Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.SORTED;
		}
		@Override
		public void forEachRemaining(IntConsumer action) {
			while(hasNext()){
				action.accept(nextInt());
			}
		}
		@Override
		public void forEachRemaining(Consumer<? super Integer> action) {
			java.util.Spliterator.OfInt.super.forEachRemaining(action);
		}
	}
	@Override
	public OfInt indexIterator() {
		return new InItr(0, 0, length);
	}
	@Override
	public java.util.Spliterator.OfInt indexSpliterator() {
		return new InItr(0, 0, length);
	}
	public boolean addAll(SimpleIndexSet o) {
		if(o.words.length > words.length){
			for(int i = words.length;i < o.words.length;i++){
				if(o.words[i] != 0l){
					throw new IllegalArgumentException();
				}
			}
		}
		int lastWordIndex = length/0x40;
		boolean changed = false;
		long dontTransferThese = WORD_MASK << length;
		if(dontTransferThese != WORD_MASK){//Otherwise lastWordIndex == words.length!!
			long p = words[lastWordIndex];
			long n = p | o.words[lastWordIndex];
			if((n & dontTransferThese) != 0){
				throw new IllegalArgumentException();
			}
			if(n != p){
				words[lastWordIndex] = n;
				changed = true;
			}
		}
		for(int i = 0;i < lastWordIndex;i++){
			long p = words[i];
			long n = p | o.words[i];
			if(n != p){
				words[i] = n;
				changed = true;
			}
		}
		return changed;
	}
	public boolean retainAll(SimpleIndexSet o) {
		int len = Math.min(words.length, o.words.length);
		int index = 0;
		boolean changed = false;
		while(index < len){
			long oldValue = words[index];
			long putValue = oldValue & o.words[index];
			if(putValue != oldValue){
				changed = true;
				words[index] = putValue;
			}
			index++;
		}
		while(index < words.length){
			if(words[index] != 0){
				words[index] = 0l;
				changed = true;
			}
			index++;
		}
		return changed;
	}
	public boolean removeAll(SimpleIndexSet o) {
		int len = Math.min(words.length, o.words.length);
		int index = 0;
		boolean changed = false;
		while(index < len){
			long oldValue = words[index];
			long putValue = oldValue & ~o.words[index];
			if(putValue != oldValue){
				changed = true;
				words[index] = putValue;
			}
			index++;
		}
		return changed;
	}
	public boolean containsAll(SimpleIndexSet o){
		int len = Math.min(words.length, o.words.length);
		int index = 0;
		while(index < len){
			long x = o.words[index];
			if((words[index] & x) != x){
				return false;
			}
			index++;
		}
		while(index < o.words.length){
			if(o.words[index] != 0l){
				return false;
			}
			index++;
		}
		return true;
	}
	@Override
	public int length() {
		return length;
	}
	@Override
	public int hashCode() {
		return Arrays.hashCode(words)^length;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj == this){
			return true;
		}
		if(obj instanceof SimpleIndexSet){
			SimpleIndexSet other = (SimpleIndexSet)obj;
			if(length == other.length){
				for(int i = 0;i < words.length;i++){
					if(words[i] != other.words[i]){
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	@Override
	protected SimpleIndexSet clone() throws CloneNotSupportedException {
		SimpleIndexSet copy = (SimpleIndexSet)super.clone();
		copy.words = words.clone();
		return copy;
	}
	@Override
	public void clear() {
		Arrays.fill(words, 0l);
	}
}
