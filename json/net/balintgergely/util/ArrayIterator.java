package net.balintgergely.util;

import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;

public class ArrayIterator<E> implements ListIterator<E>,Spliterator<E>{
	E[] array; 
	private int index,limit;
	public ArrayIterator(E[] array0){
		array = array0;
		limit = array0.length;
	}
	public ArrayIterator(E[] array0,int fromIndex,int toIndex) {
		array = array0;
		index = fromIndex;
		limit = Math.min(array.length, toIndex);
	}
	@Override
	public boolean hasNext() {
		return index < limit;
	}
	@Override
	public E next() {
		if(index >= limit){
			throw new NoSuchElementException();
		}
		return array[index++];
	}
	@Override
	public boolean hasPrevious() {
		return index > 0;
	}
	@Override
	public E previous() {
		if(index <= 0){
			throw new NoSuchElementException();
		}
		return array[--index];
	}
	@Override
	public int nextIndex() {
		return index;
	}
	@Override
	public int previousIndex() {
		return index-1;
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
		if(index < limit){
			action.accept(array[index++]);
			return true;
		}
		return false;
	}
	@Override
	public Spliterator<E> trySplit() {
		int start = index;
		int middle = start+(limit-start)/2;
		if(middle != start){
			index = middle;
			return new ArrayIterator<>(array, start, middle);
		}
		return null;
	}
	@Override
	public long estimateSize() {
		return limit-index;
	}
	@Override
	public int characteristics() {
		return Spliterator.ORDERED | Spliterator.SIZED |
				Spliterator.SUBSIZED;
	}
	@Override
	public void forEachRemaining(Consumer<? super E> action) {
		while(index < limit){
			action.accept(array[index++]);
		}
	}
}