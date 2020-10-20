package net.balintgergely.util;

import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.PrimitiveIterator.OfInt;

public interface OrdinalCollection<E> extends Collection<E>, IndexSet{
	/**
	 * @return The element at the specified index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	public E get(int index);
	/**
	 * Removes the specified index from this set.
	 * @return The element at the specified index or null if this collection didn't contain the index;
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	@Override
	public Object remove(int index);
	/**
	 * Trivial implementation for OrdinalCollection if also extends List.
	 * @author balintgergely
	 */
	public static interface OrdinalList<E> extends OrdinalCollection<E>,List<E>{
		@Override
		public default OfInt indexIterator() {
			return new IndexIterator.OfIndices(0, 0, size());
		}
		@Override
		public default Spliterator.OfInt indexSpliterator() {
			return new IndexIterator.OfIndices(0, 0, size());
		}
		@Override
		default int firstIndex() {
			return isEmpty() ? -1 : 0;
		}
		@Override
		default int lastIndex() {
			return size()-1;
		}
		@Override
		default boolean contains(int index) {
			if(index < 0 || index >= size()){
				throw new IndexOutOfBoundsException();
			}
			return true;
		}
		@Override
		default boolean add(int index) {
			return false;
		}
		@Override
		default int length() {
			return size();
		}
	}
}
