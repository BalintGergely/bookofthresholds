package net.balintgergely.util;

import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
/**
 * A simple unmodifiable ListIterator and Spliterator implementation over an array.
 * @author balintgergely
 */
public class ArrayIterator<E> implements ListIterator<E>,Spliterator<E>{
	protected E[] array; 
	protected int minIndex,index,maxIndex;
	public ArrayIterator(E[] array0){
		array = array0;
		minIndex = 0;
		index = 0;
		maxIndex = array0.length;
	}
	public ArrayIterator(E[] array0,int index0){
		array = array0;
		minIndex = 0;
		index = index0;
		maxIndex = array0.length;
	}
	public ArrayIterator(E[] array0,int fromIndex,int index0,int toIndex) {
		array = array0;
		index = index0;
		minIndex = fromIndex;
		maxIndex = toIndex;
	}
	@Override
	public boolean hasNext() {
		return index < maxIndex;
	}
	@Override
	public E next() {
		if(index >= maxIndex){
			throw new NoSuchElementException();
		}
		return array[index++];
	}
	@Override
	public boolean hasPrevious() {
		return index > minIndex;
	}
	@Override
	public E previous() {
		if(index <= minIndex){
			throw new NoSuchElementException();
		}
		return array[--index];
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
	public void set(E e) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void add(E e) {
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
	@Override
	public ArrayIterator<E> trySplit() {
		int start = index;
		int splitPoint = start+(maxIndex-start)/2;
		if(splitPoint != start){
			index = splitPoint;
			minIndex = splitPoint;
			return new ArrayIterator<>(array, start, start, splitPoint);
		}
		return null;
	}
	@Override
	public long estimateSize() {
		return maxIndex-index;
	}
	@Override
	public int characteristics() {
		return Spliterator.ORDERED | Spliterator.SIZED |
				Spliterator.SUBSIZED;
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
	public static class Descending<E> extends ArrayIterator<E>{
		public Descending(E[] array0, int fromIndex, int index0, int toIndex) {
			super(array0, fromIndex, index0, toIndex);
		}
		public Descending(E[] array0, int index0) {
			super(array0, index0);
		}
		public Descending(E[] array0) {
			super(array0);
		}
		@Override
		public boolean hasNext() {
			return index > minIndex;
		}
		@Override
		public E next() {
			if(index <= minIndex){
				throw new NoSuchElementException();
			}
			return array[--index];
		}
		@Override
		public boolean hasPrevious() {
			return index < maxIndex;
		}
		@Override
		public E previous() {
			if(index >= maxIndex){
				throw new NoSuchElementException();
			}
			return array[index++];
		}
		@Override
		public int nextIndex() {
			return maxIndex-index;
		}
		@Override
		public int previousIndex() {
			return maxIndex-index-1;
		}
		@Override
		public ArrayIterator<E> trySplit() {
			int start = index;
			int splitPoint = start-(start-minIndex)/2;
			if(splitPoint != start){
				index = splitPoint;
				maxIndex = splitPoint;
				return new Descending<>(array, splitPoint, start, start);
			}
			return null;
		}
		@Override
		public long estimateSize() {
			return index-minIndex;
		}
	}
}