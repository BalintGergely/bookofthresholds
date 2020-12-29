package net.balintgergely.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
/**
 * An Ordinal Collection assigns an index to each element it contains. For as long as it contains a specific element
 * it never changes it's index. It also has a limited number of slots for elements.
 * @author balintgergely
 */
public interface OrdinalCollection<E> extends Collection<E>, IndexSet{
	@Override
	public default Iterator<E> iterator() {
		PrimitiveIterator.OfInt indices = indexIterator();
		return new Iterator<E>() {
			@Override
			public boolean hasNext() {
				return indices.hasNext();
			}
			@Override
			public E next() {
				return get(indices.nextInt());
			}
			@Override
			public void remove(){
				indices.remove();
			}
		};
	}
	@Override
	public default Spliterator<E> spliterator() {
		class ValueSpliterator implements Spliterator<E>,IntConsumer{
			Spliterator.OfInt indices;
			ValueSpliterator(Spliterator.OfInt in){
				this.indices = in;
			}
			@Override
			public void accept(int value) {
				index = value;
			}
			int index;
			@Override
			public boolean tryAdvance(Consumer<? super E> action) {
				if(indices.tryAdvance(this)){
					action.accept(get(index));
					return true;
				}
				return false;
			}
			@Override
			public Spliterator<E> trySplit() {
				Spliterator.OfInt spl = indices.trySplit();
				return spl == null ? null : new ValueSpliterator(spl);
			}
			@Override
			public long estimateSize() {
				return indices.estimateSize();
			}
			@Override
			public int characteristics() {
				return indices.characteristics() & (Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SORTED | Spliterator.SUBSIZED);
			}
			@Override
			public long getExactSizeIfKnown() {
				return indices.getExactSizeIfKnown();
			}
		}
		return new ValueSpliterator(indexSpliterator());
	}
	@Override
	public default void forEach(Consumer<? super E> cns){
		PrimitiveIterator.OfInt indices = indexIterator();
		while(indices.hasNext()){
			cns.accept(get(indices.nextInt()));
		}
	}
	/**
	 * @return The element at the specified index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	public E get(int index);
	/**
	 * @return The element at the specified index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	public E set(int index,E element);
	/**
	 * Removes the specified index from this set.
	 * @return The element at the specified index or null if this collection didn't contain the index;
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	@Override
	public E remove(int index);
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
		default Spliterator<E> spliterator() {
			return new IndexIterator.OfList<>(this, 0, 0, size(), 0);
		}
		@Override
		default Iterator<E> iterator() {
			return new IndexIterator.OfList<>(this, 0, 0, size(), 0);
		}
		@Override
		default ListIterator<E> listIterator() {
			return new IndexIterator.OfList<>(this, 0, 0, size(), 0);
		}
		@Override
		default ListIterator<E> listIterator(int index) {
			return new IndexIterator.OfList<>(this, 0, index, size(), 0);
		}
		@Override
		default int firstIndex() {
			return isEmpty() ? -1 : 0;
		}
		@Override
		default int lastIndex() {
			return size()-1;
		}
		/*@Override
		default int firstUnsetIndex() {
			return -1;
		}
		@Override
		default int lastUnsetIndex() {
			return -1;
		}*/
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
		@Override
		OrdinalList<E> subList(int fromIndex, int toIndex);
	}
}
