package net.balintgergely.util;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator.OfInt;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SimpleOrdinalSet<E> extends SimpleIndexSet implements OrdinalSet<E>{
	private OrdinalSet<E> universe;
	public SimpleOrdinalSet(OrdinalSet<E> universe){
		super(universe.length());
		this.universe = universe;
	}
	private int x(Object o){
		int x = universe.indexOf(o);
		if(x < 0){
			throw new IllegalArgumentException();
		}
		return x;
	}
	@Override
	public E pollFirst() {
		int index = 0;
		do{
			long value = words[index];
			if(value != 0){
				words[index] = value & ~Long.lowestOneBit(value);
				return universe.get(index*0x40+Long.numberOfTrailingZeros(value));
			}
			index++;
		}while(index < words.length);
		return null;
	}
	@Override
	public E pollLast() {
		int index = words.length;
		do{
			index--;
			long value = words[index];
			if(value != 0){
				long bit = Long.highestOneBit(value);
				words[index] = value & ~bit;
				return universe.get(index*0x40+Long.numberOfTrailingZeros(bit));
			}
		}while(index > 0);
		return null;
	}
	private class Itr implements Iterator<E>,Spliterator<E>{
		private Itr(int index,int limit){
			this.index = index;
			this.limit = limit;
		}
		private int lastIndex = -1;
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
		public E next() {
			if(hasNext()){
				return universe.get(lastIndex = index++);
			}
			throw new NoSuchElementException();
		}
		@Override
		public void remove() {
			if(lastIndex < 0){
				throw new IllegalStateException();
			}
			SimpleOrdinalSet.this.remove(lastIndex);
			lastIndex = -1;
		}
		@Override
		public boolean tryAdvance(Consumer<? super E> consumer) {
			if(hasNext()){
				consumer.accept(next());
				return true;
			}
			return false;
		}
		@Override
		public Spliterator<E> trySplit() {
			int half = index+(limit-index)/2;
			if(half != index){
				Itr n = new Itr(index, half);
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
		public Comparator<? super E> getComparator() {
			return universe.comparator();
		}
		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			while(hasNext()){
				action.accept(next());
			}
		}
	}
	@Override
	public Iterator<E> iterator() {
		return new Itr(0,length);
	}
	@Override
	public Spliterator<E> spliterator() {
		return new Itr(0, length);
	}
	private class DItr implements Iterator<E>,Spliterator<E>{
		private DItr(int index,int limit){
			this.limit = limit;
			this.index = index;
		}
		private int lastIndex = -1;
		private int index;
		private int limit;
		@Override
		public boolean hasNext() {
			while(index >= limit){
				if(contains(index)){
					return true;
				}
				index--;
			}
			return false;
		}
		@Override
		public E next() {
			if(hasNext()){
				return universe.get(lastIndex = index--);
			}
			throw new NoSuchElementException();
		}
		@Override
		public void remove() {
			if(lastIndex < 0){
				throw new IllegalStateException();
			}
			SimpleOrdinalSet.this.remove(lastIndex);
			lastIndex = -1;
		}
		@Override
		public boolean tryAdvance(Consumer<? super E> consumer) {
			if(hasNext()){
				consumer.accept(next());
				return true;
			}
			return false;
		}
		@Override
		public Spliterator<E> trySplit() {
			int half = index-(index-limit)/2;
			if(half != index){
				DItr n = new DItr(index, half);
				index = half;
				return n;
			}
			return null;
		}
		@Override
		public long estimateSize() {
			return index-limit;
		}
		@Override
		public int characteristics() {
			return Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.SORTED;
		}
		@Override
		public Comparator<? super E> getComparator() {
			return Collections.reverseOrder(universe.comparator());
		}
		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			while(hasNext()){
				action.accept(next());
			}
		}
	}
	@Override
	public Iterator<E> descendingIterator() {
		return new DItr(length-1,0);
	}
	@Override
	public Comparator<? super E> comparator() {
		return universe.comparator();
	}
	@Override
	public E first() {
		int index = 0;
		do{
			long value = words[index];
			if(value != 0){
				return universe.get(index*0x40+Long.numberOfTrailingZeros(value));
			}
			index++;
		}while(index < words.length);
		return null;
	}
	@Override
	public E last() {
		int index = words.length;
		do{
			index--;
			long value = words[index];
			if(value != 0){
				return universe.get(index*0x40+Long.numberOfTrailingZeros(Long.highestOneBit(value)));
			}
		}while(index > 0);
		return null;
	}
	@Override
	public boolean contains(Object o) {
		int dex = universe.indexOf(o);
		return dex >= 0 && contains(dex);
	}
	@Override
	public boolean add(E e) {
		return add(x(e));
	}
	@Override
	public boolean remove(Object o) {
		int index = universe.indexOf(o);
		if(index >= 0){
			long bit = 1l << index;
			index /= 0x40;
			long value = words[index];
			if((value & bit) != 0){
				words[index] &= ~bit;
				return true;
			}
		}
		return false;
	}
	@Override
	public E remove(int index) {
		long bit = 1l << index;
		index /= 0x40;
		long value = words[index];
		if((value & bit) != 0){
			words[index] &= ~bit;
		}
		return universe.get(index);//This should catch sneaky-sneaky illegal indices.
	}
	@Override
	public boolean addAll(Collection<? extends E> c) {
		if(c instanceof SimpleOrdinalSet){
			SimpleOrdinalSet<?> st = (SimpleOrdinalSet<?>)c;
			if(universe.equals(st.universe)){
				return super.addAll(st);
			}
		}
		boolean changed = false;
		for(E e : c){
			changed |= add(e);
		}
		return changed;
	}
	@Override
	public boolean retainAll(Collection<?> c) {
		if(c instanceof SimpleOrdinalSet){
			SimpleOrdinalSet<?> st = (SimpleOrdinalSet<?>)c;
			if(universe.equals(st.universe)){
				return super.retainAll(st);
			}
		}
		return fallbackRemove(c, false);
	}
	@Override
	public boolean removeAll(Collection<?> c) {
		if(c instanceof SimpleOrdinalSet){
			SimpleOrdinalSet<?> st = (SimpleOrdinalSet<?>)c;
			if(universe.equals(st.universe)){
				return super.removeAll(st);
			}
		}
		return fallbackRemove(c, true);
	}
	@Override
	public boolean containsAll(Collection<?> c) {
		if(c instanceof SimpleOrdinalSet){
			SimpleOrdinalSet<?> st = (SimpleOrdinalSet<?>)c;
			if(universe.equals(st.universe)){
				return super.containsAll(st);
			}
		}
		for(Object o : c){
			if(!contains(o)){
				return false;
			}
		}
		return true;
	}
	private boolean fallbackRemove(Collection<?> c,boolean n){
		int wordIndex = 0;
		boolean changed = false;
		do{
			long word = words[wordIndex];
			long cWord = word;
			int offset = 0;
			long bit = 1l;
			do{
				if((word & bit) != 0 && c.contains(universe.get(wordIndex*0x40+offset)) == n){
					word &= ~bit;
				}
				bit <<= 1;
			}while(++offset < 0x40);
			if(cWord != word){
				changed = true;
				words[wordIndex] = cWord;
			}
			wordIndex++;
		}while(wordIndex < words.length);
		return changed;	
	}
	@Override
	public boolean removeIf(Predicate<? super E> p){
		int wordIndex = 0;
		boolean changed = false;
		do{
			long word = words[wordIndex];
			long cWord = word;
			int offset = 0;
			long bit = 1l;
			do{
				if((word & bit) != 0 && p.test(universe.get(wordIndex*0x40+offset))){
					word &= ~bit;
				}
				bit <<= 1l;
			}while(++offset < 0x40);
			if(cWord != word){
				changed = true;
				words[wordIndex] = cWord;
			}
			wordIndex++;
		}while(wordIndex < words.length);
		return changed;
	}
	@Override
	public void forEach(Consumer<? super E> action) {
		int wordIndex = 0;
		do{
			long word = words[wordIndex];
			int offset = 0;
			long bit = 1l;
			do{
				if((word & bit) != 0){
					action.accept(universe.get(wordIndex*0x40+offset));
				}
				bit <<= 1;
			}while(++offset < 0x40);
			wordIndex++;
		}while(wordIndex < words.length);
	}
	@Override
	public void clear() {
		int index = words.length;
		do{
			index--;
			words[index] = 0l;
		}while(index > 0);
	}
	@Override
	public E get(int index) {
		return universe.get(index);
	}
	@Override
	public int indexOf(Object element) {
		return universe.indexOf(element);
	}
	@Override
	public int seek(E element, boolean lower, boolean inclusive) {
		int index = universe.seek(element, lower, inclusive);
		return index >= 0 ? seekIndex(index, lower, inclusive) : -1;
	}
	private int seekIndex(int index, boolean lower, boolean inclusive) {
		if(lower){
			int wordIndex = index/0x40;
			long word = words[wordIndex] & (WORD_MASK >>> ~index);
			while(true){
				if(word != 0){
					return wordIndex*0x40+Long.numberOfTrailingZeros(word);
				}
				wordIndex--;
				if(wordIndex < 0){
					return -1;
				}
				word = words[wordIndex];
			}
		}else{
			int wordIndex = index/0x40;
			long word = words[wordIndex] & (WORD_MASK << index);
			while(true){
				if(word != 0){
					return wordIndex*0x40+Long.numberOfTrailingZeros(Long.highestOneBit(word));
				}
				wordIndex++;
				if(wordIndex >= words.length){
					return -1;
				}
				word = words[wordIndex];
			}
		}
	}
	@Override
	public OrdinalSet<E> universe() {
		return universe.universe();
	}
	@Override
	public int offset() {
		return universe.offset();
	}
	@Override
	public Object[] toArray() {
		Object[] r = new Object[size()];
		Iterator<E> it = iterator();
		for (int i = 0; i < r.length; i++) {
			r[i] = it.next();
		}
		return r;
	}
	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		Iterator<E> it = iterator();
		int i = 0;
		for(; i < a.length; i++){
			if(!it.hasNext()){
				a[i] = null;
				return a;
			}
			a[i] = (T)it.next();
		}
		while(it.hasNext()){
			a = Arrays.copyOf(a, Math.max(a.length+1, size()));
			do{
				a[i++] = (T)it.next();
			}while(i < a.length && it.hasNext());
		}
		if(i < a.length){
			a[i] = null;
		}
		return a;
	}
	@Override
	public OrdinalSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
		int a = universe.seek(fromElement, false, fromInclusive);
		int b = universe.seek(toElement, true, toInclusive);
		if(a > b || a < 0){
			return OrdinalSet.emptySet();
		}
		return new SubSet(a, b);
	}
	private class SubSet extends AbstractSet<E> implements OrdinalSet<E>{
		private int fromIndex,toIndex;//Both are inclusive;
		private SubSet(int fromIndex,int toIndex){
			this.fromIndex = fromIndex;
			this.toIndex = toIndex;
		}
		private int tix(int index){
			if(index < 0){
				throw new IndexOutOfBoundsException(index);
			}
			index += fromIndex;
			if(index > toIndex){
				throw new IndexOutOfBoundsException(index-fromIndex);
			}
			return index;
		}
		@Override
		public E pollFirst() {
			int index = seekIndex(fromIndex, false, true);
			return index <= toIndex && index >= fromIndex ? SimpleOrdinalSet.this.remove(index) : null;
		}
		@Override
		public E pollLast() {
			int index = seekIndex(toIndex, true, true);
			return index >= fromIndex ? SimpleOrdinalSet.this.remove(index) : null;
		}
		@Override
		public Iterator<E> iterator() {
			return new Itr(fromIndex, toIndex+1);
		}
		@Override
		public Iterator<E> descendingIterator() {
			return new DItr(toIndex, fromIndex);
		}
		@Override
		public Comparator<? super E> comparator() {
			return universe.comparator();
		}
		@Override
		public E first() {
			int index = seekIndex(fromIndex, false, true);
			return index <= toIndex && index >= 0 ? universe.get(index) : null;
		}
		@Override
		public E last() {
			int index = seekIndex(toIndex, true, true);
			return index >= fromIndex ? universe.get(index) : null;
		}
		@Override
		public int firstIndex() {
			int index = seekIndex(fromIndex, false, true);
			return index <= toIndex && index >= 0 ? index : -1;
		}
		@Override
		public int lastIndex() {
			int index = seekIndex(toIndex, true, true);
			return index >= fromIndex ? index : -1;
		}
		@Override
		public int size() {
			int fromWord = fromIndex/0x40,toWord = toIndex/0x40;
			if(fromWord == toWord){
				return Long.bitCount(words[fromWord] & (WORD_MASK << fromIndex) & (WORD_MASK >>> ~toIndex));
			}else{
				int total = Long.bitCount(words[fromWord] & (WORD_MASK << fromIndex));
				while(++fromWord < toWord){
					total += Long.bitCount(words[fromWord]);
				}
				return total + Long.bitCount(words[fromWord] & (WORD_MASK >>> ~toIndex));
			}
		}
		@Override
		public boolean isEmpty() {
			int fromWord = fromIndex/0x40,toWord = toIndex/0x40;
			if(fromWord == toWord){
				return (words[fromWord] & (WORD_MASK << fromIndex) & (WORD_MASK >>> ~toIndex)) == 0;
			}else{
				if((words[fromWord] & (WORD_MASK << fromIndex)) != 0){
					return false;
				}
				while(++fromWord < toWord){
					if(words[fromWord] != 0){
						return false;
					}
				}
				return (words[fromWord] & (WORD_MASK >>> ~toIndex)) == 0;
			}
		}
		@Override
		public boolean contains(Object o) {
			int index = universe.indexOf(o);
			return index >= fromIndex && index <= toIndex && SimpleOrdinalSet.this.contains(index);
		}
		@Override
		public boolean add(E e) {
			int index = universe.indexOf(e);
			if(index < fromIndex || index > toIndex){
				throw new IllegalArgumentException();
			}
			return SimpleOrdinalSet.this.add(index);
		}
		@Override
		public boolean remove(Object o) {
			int index = universe.indexOf(o);
			if(index >= fromIndex && index <= toIndex){
				long bit = 1l << index;
				index /= 0x40;
				long value = words[index];
				if((value & bit) != 0){
					words[index] &= ~bit;
					return true;
				}
			}
			return false;
		}
		@Override
		public void clear() {
			int fromWord = fromIndex/0x40,toWord = toIndex/0x40;
			if(fromWord == toWord){
				words[fromWord] &= ~((WORD_MASK << fromIndex) & (WORD_MASK >>> ~toIndex));
			}else{
				words[fromWord] &= ~(WORD_MASK << fromIndex);
				while(++fromWord < toWord){
					words[fromWord] = 0l;
				}
				words[fromWord] &= ~(WORD_MASK >>> ~toIndex);
			}
		}
		@Override
		public Spliterator<E> spliterator() {
			return new Itr(fromIndex, toIndex+1);
		}
		@Override
		public OfInt indexIterator() {
			return new InItr(fromIndex, fromIndex, toIndex+1);
		}
		@Override
		public java.util.Spliterator.OfInt indexSpliterator() {
			return new InItr(fromIndex, fromIndex, toIndex+1);
		}
		@Override
		public E get(int index) {
			return universe.get(tix(index));
		}
		@Override
		public int indexOf(Object element) {
			int dex = universe.indexOf(element);
			if(dex < fromIndex || dex > toIndex){
				return -1;
			}
			return dex-fromIndex;
		}
		@Override
		public boolean contains(int index) {
			return SimpleOrdinalSet.this.contains(tix(index));
		}
		@Override
		public E remove(int index) {
			return SimpleOrdinalSet.this.remove(tix(index));
		}
		@Override
		public boolean add(int index) {
			return SimpleOrdinalSet.this.add(tix(index));
		}
		@Override
		public int seek(E element, boolean lower, boolean inclusive) {
			int index = SimpleOrdinalSet.this.seek(element, lower, inclusive);
			return index >= fromIndex && index <= toIndex ? index : -1;
		}
		@Override
		public OrdinalSet<E> universe() {
			return universe;
		}
		@Override
		public int offset() {
			return universe.offset()+fromIndex;
		}
		@Override
		public int length() {
			return toIndex-fromIndex+1;
		}
		@Override
		public OrdinalSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
			int a = universe.seek(fromElement, false, fromInclusive);
			if(a < fromIndex){
				a = fromIndex;
			}
			int b = universe.seek(toElement, true, toInclusive);
			if(b > toIndex){
				b = toIndex;
			}
			if(a > b || a < 0){
				return OrdinalSet.emptySet();
			}
			return new SubSet(a,b);
		}
	}
	@Override
	public int hashCode() {
		return universe.hashCode()^Arrays.hashCode(words);
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SimpleOrdinalSet){
			SimpleOrdinalSet<?> st = (SimpleOrdinalSet<?>)obj;
			return universe.equals(st.universe) && super.equals(st);
		}
		return false;
	}
	@Override
	@SuppressWarnings("unchecked")
	protected SimpleOrdinalSet<E> clone() throws CloneNotSupportedException {
		return (SimpleOrdinalSet<E>)super.clone();
	}
}
