package net.balintgergely.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

interface Immutable{
	@SuppressWarnings("unchecked")
	static <E> E[] toArray(Collection<?> col,Class<E> clazz){
		Object[] array = (Object[])Array.newInstance(clazz, col.size());
		int index = 0;
		Iterator<?> itr = col.iterator();
		while(itr.hasNext()){
			if(index == array.length){
				array = Arrays.copyOf(array, Math.max(col.size(), index+1));
			}
			array[index++] = itr.next();
		}
		return (E[])(index == array.length ? array : Arrays.copyOf(array, index));
	}
	static class ImmutableList<E> extends ArrayListView<E> implements Immutable{
		ImmutableList(E[] data){
			super(data);
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
			return new ImmutableSubList<>(data, fromIndex, toIndex-fromIndex);
		}
	}
	static class ImmutableSubList<E> extends ArrayListView.SubList<E> implements Immutable{
		ImmutableSubList(E[] data,int offset,int length){
			super(data,offset,length);
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
			return fromIndex == 0 && toIndex == length ? this : new ImmutableSubList<>(data, offset+fromIndex, fromIndex-toIndex);
		}
	}
}
