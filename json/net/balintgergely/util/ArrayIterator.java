package net.balintgergely.util;

import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;

public class ArrayIterator<E> implements ListIterator<E>,Spliterator<E>{
	E[] array; 
	int minIndex,index,maxIndex;
	public ArrayIterator(E[] array0){
		array = array0;
	}
	public ArrayIterator(E[] array0,int fromIndex,int index,int toIndex) {
		array = array0;
		index = fromIndex;
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
		int middle = start+(maxIndex-start)/2;
		if(middle != start){
			index = middle;
			return new ArrayIterator<>(array, minIndex, start, middle);
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
}