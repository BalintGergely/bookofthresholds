package net.balintgergely.util;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import net.balintgergely.util.Immutable.ImmutableList;

/**
 * A list view over an array. The array may not be modified through this list.
 * @author balintgergely
 */
public class ArrayListView<E> extends AbstractCollection<E> implements List<E>,RandomAccess{
	protected final E[] data;
	public ArrayListView(@SuppressWarnings("unchecked") E... data){
		this.data = data;
	}
	@Override
	public void forEach(Consumer<? super E> action) {
		for(E e : data){
			action.accept(e);
		}
	}
	@Override
	public int size() {
		return data.length;
	}
	@Override
	public boolean isEmpty() {
		return data.length == 0;
	}
	@Override
	public boolean contains(Object o) {
		for(E e : data){
			if(o == null ? e == null : o.equals(e)){
				return true;
			}
		}
		return false;
	}
	@Override
	public Object[] toArray() {
		return Arrays.copyOf(data, data.length, Object[].class);
	}
	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		if(a.length < data.length){
			a = (T[])Array.newInstance(a.getClass().getComponentType(), data.length);
		}else if(a.length > data.length){
			a[data.length] = null;
		}
		System.arraycopy(data, 0, a, 0, data.length);
		return a;
	}
	@Override
	public boolean add(E e) {
		clear();
		return false;
	}
	@Override
	public boolean remove(Object o) {
		clear();
		return false;
	}
	@Override
	public boolean addAll(Collection<? extends E> c) {
		clear();
		return false;
	}
	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		clear();
		return false;
	}
	@Override
	public boolean removeAll(Collection<?> c) {
		clear();
		return false;
	}
	@Override
	public boolean retainAll(Collection<?> c) {
		clear();
		return false;
	}
	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		clear();
	}
	@Override
	public void sort(Comparator<? super E> c) {
		clear();
	}
	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
	@Override
	public E get(int index) {
		return data[index];
	}
	@Override
	public E set(int index, E element) {
		clear();
		return null;
	}
	@Override
	public void add(int index, E element) {
		clear();
	}
	@Override
	public E remove(int index) {
		clear();
		return null;
	}
	@Override
	public int indexOf(Object o) {
		int i = 0;
		while(i < data.length){
			if(o == null ? data[i] == null : o.equals(data[i])){
				return i;
			}
			i++;
		}
		return -1;
	}
	@Override
	public int lastIndexOf(Object o) {
		int i = data.length;
		while(i > 0){
			i--;
			if(o == null ? data[i] == null : o.equals(data[i])){
				return i;
			}
		}
		return -1;
	}
	@Override
	public ArrayIterator<E> iterator() {
		return listIterator(0);
	}
	@Override
	public ArrayIterator<E> spliterator() {
		return listIterator(0);
	}
	@Override
	public ArrayIterator<E> listIterator() {
		return listIterator(0);
	}
	@Override
	public ArrayIterator<E> listIterator(int index) {
		return new ArrayIterator<>(data,index);
	}
	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		if(fromIndex < 0 || toIndex < 0 || fromIndex > toIndex || toIndex > data.length){
			throw new IndexOutOfBoundsException();
		}
		if(fromIndex == toIndex){
			return List.of();
		}
		return new SubList<>(data, fromIndex, toIndex-fromIndex);
	}
	public static class SubList<E> extends ArrayListView<E>{
		protected final int offset,length;
		protected SubList(E[] data,int offset,int length){
			super(data);
			if(length <= 0){
				throw new IllegalArgumentException();
			}
			this.offset = offset;
			this.length = length;
		}
		@Override
		public int size() {
			return length;
		}
		@Override
		public boolean isEmpty() {
			return false;
		}
		@Override
		public boolean contains(Object o) {
			for(int i = 0;i < length;i++){
				E e = data[i+offset];
				if(o == null ? e == null : o.equals(e)){
					return true;
				}
			}
			return false;
		}
		@Override
		public Object[] toArray() {
			return Arrays.copyOfRange(data, offset, offset+length, Object[].class);
		}
		@Override
		@SuppressWarnings("unchecked")
		public <T> T[] toArray(T[] a) {
			if(a.length < length){
				a = (T[])Array.newInstance(a.getClass().getComponentType(), length);
			}else if(a.length > length){
				a[length] = null;
			}
			System.arraycopy(data, offset, a, 0, length);
			return a;
		}
		@Override
		public void replaceAll(UnaryOperator<E> operator) {
			clear();
		}
		@Override
		public void sort(Comparator<? super E> c) {
			clear();
		}
		@Override
		public boolean removeIf(Predicate<? super E> filter) {
			clear();
			return false;
		}
		@Override
		public void forEach(Consumer<? super E> action) {
			for(E e : data){
				action.accept(e);
			}
		}
		@Override
		public boolean add(E e) {
			clear();
			return false;
		}
		@Override
		public E get(int index) {
			return data[index];
		}
		@Override
		public E set(int index, E element) {
			clear();
			return null;
		}
		@Override
		public void add(int index, E element) {
			clear();
		}
		@Override
		public E remove(int index) {
			clear();
			return null;
		}
		@Override
		public int indexOf(Object o) {
			int i = 0;
			while(i < length){
				if(o == null ? data[offset+i] == null : o.equals(data[offset+i])){
					return i;
				}
				i++;
			}
			return 0;
		}
		@Override
		public int lastIndexOf(Object o) {
			int i = offset+length;
			while(i > offset){
				i--;
				if(o == null ? data[i] == null : o.equals(data[i])){
					return i;
				}
			}
			return -1;
		}
		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
		@Override
		public boolean addAll(int index, Collection<? extends E> c) {
			clear();
			return false;
		}
		@Override
		public ArrayIterator<E> listIterator(int index) {
			return new ArrayIterator<>(data,offset,offset+index,offset+length);
		}
		@Override
		public boolean equals(Object o) {
			if(this == o){
				return true;
			}
			if(o instanceof ImmutableList){
				ImmutableList<?> od = (ImmutableList<?>)o;
				return Arrays.equals(data, offset, offset+length, od.data, od.offset, od.offset+od.length);
			}
			return false;
		}
		@Override
		public int hashCode() {
			return Arrays.hashCode(data);
		}
		@Override
		public <T> T[] toArray(IntFunction<T[]> generator) {
			return toArray(generator.apply(length));
		}
		@Override
		public boolean remove(Object o) {
			clear();
			return false;
		}
		@Override
		public boolean addAll(Collection<? extends E> c) {
			clear();
			return false;
		}
		@Override
		public boolean removeAll(Collection<?> c) {
			clear();
			return false;
		}
		@Override
		public boolean retainAll(Collection<?> c) {
			clear();
			return false;
		}
		@Override
		public List<E> subList(int fromIndex, int toIndex) {
			if(fromIndex < 0 || toIndex < 0 || fromIndex > toIndex || toIndex > length){
				throw new IndexOutOfBoundsException();
			}
			if(fromIndex == toIndex){
				return List.of();
			}
			return fromIndex == 0 && toIndex == length ? this : new SubList<>(data, offset+fromIndex, fromIndex-toIndex);
		}
	}
}
