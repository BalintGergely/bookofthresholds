package net.balintgergely.util;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import net.balintgergely.util.Immutable.ImmutableList;
import net.balintgergely.util.OrdinalCollection.OrdinalList;

/**
 * A list view over an array. The array may not be modified through this list.
 * @author balintgergely
 */
public class ArrayListView<E> extends AbstractCollection<E> implements OrdinalList<E>,RandomAccess{
	protected final E[] data;
	public ArrayListView(@SuppressWarnings("unchecked") E... data){
		this.data = Objects.requireNonNull(data);
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
	public IndexIterator.OfArray<E> iterator() {
		return listIterator(0);
	}
	@Override
	public IndexIterator.OfArray<E> spliterator() {
		return listIterator(0);
	}
	@Override
	public IndexIterator.OfArray<E> listIterator() {
		return listIterator(0);
	}
	@Override
	public IndexIterator.OfArray<E> listIterator(int index) {
		return new IndexIterator.OfArray<>(data,0,index,data.length,Spliterator.IMMUTABLE);
	}
	@Override
	public OrdinalList<E> subList(int fromIndex, int toIndex) {
		if(fromIndex < 0 || toIndex < 0 || fromIndex > toIndex || toIndex > data.length){
			throw new IndexOutOfBoundsException();
		}
		if(fromIndex == 0 && toIndex == data.length){
			return this;
		}
		if(fromIndex == toIndex){
			return OrdinalSet.emptyList();
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
		public IndexIterator.OfArray<E> listIterator(int index) {
			return new IndexIterator.OfArray<E>(data,offset,offset+index,offset+length,Spliterator.IMMUTABLE);
		}
		@Override
		public boolean equals(Object o) {
			if(this == o){
				return true;
			}
			if(o instanceof ImmutableList.SubList){
				ImmutableList.SubList<?> od = (ImmutableList.SubList<?>)o;
				return Arrays.equals(data, offset, offset+length, od.data, od.offset, od.offset+od.length);
			}
			if(o instanceof ImmutableList){
				ImmutableList<?> od = (ImmutableList<?>)o;
				return Arrays.equals(data, offset, offset+length, od.data, 0, od.data.length);
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
		public OrdinalList<E> subList(int fromIndex, int toIndex) {
			if(fromIndex < 0 || toIndex < 0 || fromIndex > toIndex || toIndex > length){
				throw new IndexOutOfBoundsException();
			}
			if(fromIndex == 0 && toIndex == length){
				return this;
			}
			if(fromIndex == toIndex){
				return OrdinalSet.emptyList();
			}
			return fromIndex == 0 && toIndex == length ? this : new SubList<>(data, offset+fromIndex, fromIndex-toIndex);
		}
	}
	@Override
	public int hashCode(){
		int index = 0,size = size();
		int hashCode = 1;
		while(index < size){
			hashCode = 31 * hashCode + Objects.hashCode(get(index));
			index++;
		}
		return hashCode;
	}
	@Override
	public boolean equals(Object o) {
		if(o == this){
			return true;
		}
		if(o instanceof List && o instanceof RandomAccess){
			List<?> lst = (List<?>)o;
			int length0 = size(),length1 = lst.size();
			if(length0 != length1){
				return false;
			}
			for(int index = 0;index < length0;index++){
				if(!Objects.equals(get(index), lst.get(index))){
					return false;
				}
			}
			return true;
		}else if(o instanceof Iterable){
			int length0 = size(),index = 0;
			Iterator<?> itr = ((Iterable<?>)o).iterator();
			while(index < length0){
				if(!(itr.hasNext() && Objects.equals(get(index), itr.next()))){
					return false;
				}
				index++;
			}
			return !itr.hasNext();
		}
		return false;
	}
}
