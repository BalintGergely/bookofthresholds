package net.balintgergely.util;

import java.util.Comparator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public abstract class IndexIterator<E> implements ListIterator<E>,Spliterator<E>{
	protected int minIndex,index,maxIndex,characteristics;
	public IndexIterator(int fromIndex,int index0,int toIndex,int characteristics) {
		minIndex = fromIndex;
		index = index0;
		maxIndex = toIndex;
		this.characteristics = characteristics | Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
	}
	@Override
	public boolean hasNext() {
		return index < maxIndex;
	}
	@Override
	public boolean hasPrevious() {
		return index > minIndex;
	}
	public int nextInt() {
		if(index >= maxIndex){
			throw new NoSuchElementException();
		}
		return index++;
	}
	public int previousInt() {
		if(index <= minIndex){
			throw new NoSuchElementException();
		}
		return --index;
	}
	@Override
	public int nextIndex() {
		return index-minIndex;
	}
	@Override
	public int previousIndex() {
		return index-1-minIndex;
	}
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	@Override
	public void set(Object e) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void add(Object e) {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean tryAdvance(Consumer<? super E> action) {
		if(hasNext()){
			action.accept(next());
			return true;
		}
		return false;
	}
	public boolean tryAdvanceBackward(Consumer<? super E> action) {
		if(hasPrevious()){
			action.accept(previous());
			return true;
		}
		return false;
	}
	@Override
	public IndexIterator<E> trySplit() {
		int start = index;
		int splitPoint = start+(maxIndex-start)/2;
		if(splitPoint != start){
			IndexIterator<E> newItr;
			try{
				newItr = clone();
			}catch(CloneNotSupportedException e){
				return null;
			}
			index = splitPoint;
			minIndex = splitPoint;
			newItr.minIndex = start;
			newItr.index = start;
			newItr.maxIndex = splitPoint;
			return newItr;
		}
		return null;
	}
	public IndexIterator<E> trySplitBackward() {
		int start = index;
		int splitPoint = start-(start-minIndex)/2;
		if(splitPoint != start){
			IndexIterator<E> newItr;
			try{
				newItr = clone();
			}catch(CloneNotSupportedException e){
				return null;
			}
			index = splitPoint;
			maxIndex = splitPoint;
			newItr.minIndex = splitPoint;
			newItr.index = start;
			newItr.maxIndex = start;
			return newItr;
		}
		return null;
	}
	@Override
	public long estimateSize() {
		return maxIndex-index;
	}
	public long estimateSizeBackward() {
		return index-minIndex;
	}
	@Override
	public int characteristics() {
		return characteristics;
	}
	@Override
	public void forEachRemaining(Consumer<? super E> action) {
		while(hasNext()){
			action.accept(next());
		}
	}
	@Override
	public long getExactSizeIfKnown() {
		return estimateSize();
	}
	@Override
	@SuppressWarnings("unchecked")
	public IndexIterator<E> clone() throws CloneNotSupportedException{
		return (IndexIterator<E>)super.clone();
	}
	public static class OfIndices extends IndexIterator<Integer> implements PrimitiveIterator.OfInt, Spliterator.OfInt{
		public OfIndices(int fromIndex, int index0, int toIndex) {
			super(fromIndex, index0, toIndex, Spliterator.SORTED | Spliterator.IMMUTABLE);
		}
		@Override
		public OfIndices trySplit() {
			return (OfIndices)super.trySplit();
		}
		@Override
		public void forEachRemaining(IntConsumer action) {
			while(hasNext()){
				action.accept(nextInt());
			}
		}
		@Override
		public Integer previous() {
			return Integer.valueOf(previousInt());
		}
		@Override
		public boolean tryAdvance(IntConsumer action) {
			if(hasNext()){
				action.accept(nextInt());
				return true;
			}
			return false;
		}
		@Override
		public Integer next() {
			return Integer.valueOf(nextInt());
		}
	}
	public static class OfArray<E> extends IndexIterator<E>{
		protected E[] array;
		public OfArray(E[] array,int fromIndex, int index0, int toIndex, int characteristics) {
			super(fromIndex, index0, toIndex, characteristics);
			this.array = array;
		}
		@Override
		public E next() {
			return array[nextInt()];
		}
		@Override
		public E previous() {
			return array[previousInt()];
		}
		public static class Sorted<E> extends OfArray<E>{
			protected Comparator<? super E> comparator;
			public Sorted(E[] array, int fromIndex, int index0, int toIndex, int characteristics, Comparator<? super E> comparator) {
				super(array, fromIndex, index0, toIndex, characteristics | Spliterator.SORTED);
				this.comparator = comparator;
			}
			@Override
			public Comparator<? super E> getComparator() {
				return comparator;
			}
		}
	}
	public static class OfFunction<E> extends IndexIterator<E>{
		protected IntFunction<E> function;
		public OfFunction(IntFunction<E> fn,int fromIndex, int index0, int toIndex, int characteristics) {
			super(fromIndex, index0, toIndex, characteristics);
			this.function = fn;
		}
		@Override
		public E next() {
			return function.apply(nextInt());
		}
		@Override
		public E previous() {
			return function.apply(previousInt());
		}
		public static class Sorted<E> extends OfFunction<E>{
			protected Comparator<? super E> comparator;
			public Sorted(IntFunction<E> function, int fromIndex, int index0, int toIndex, int characteristics, Comparator<? super E> comparator) {
				super(function, fromIndex, index0, toIndex, characteristics | Spliterator.SORTED);
				this.comparator = comparator;
			}
			@Override
			public Comparator<? super E> getComparator() {
				return comparator;
			}
		}
	}
}
