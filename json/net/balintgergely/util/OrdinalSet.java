package net.balintgergely.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
/**
 * An ordinal set is a NavigableSet of ordered elements each associated with an index.
 * The <code>get(int)</code> and the <code>indexOf(E)</code> methods are present for index related operations
 * and queries. They work differently from those present in the List interface!
 * <li>Each element has a permanently assigned index that will not change if removed and later re-added to the set.
 * Nor will it change in response to other element removals and additions.
 * <li>Indexes are between 0 inclusive and <code>length()</code> exclusive. Every valid index is assigned to exactly one element.<br>
 * For this reason, each OrdinalSet has an immutable universe of elements and specifies an interval of elements within that universe
 * it can contain.<br>
 * The idea behind this interface is making runtime variants for enum sets and enum maps.
 * @author balintgergely
 */
public interface OrdinalSet<E> extends Set<E>,OrdinalCollection<E>{
	@SuppressWarnings("unchecked")
	public static <E> NavigableOrdinalSet<E> emptySet(){
		return (NavigableOrdinalSet<E>)EmptyOrdinalSet.INSTANCE;
	}
	@SuppressWarnings("unchecked")
	public static <E> OrdinalList<E> emptyList(){
		return (OrdinalList<E>)EmptyOrdinalSet.INSTANCE;
	}
	@Override
	default Spliterator<E> spliterator() {
		return Spliterators.spliterator(this,
				Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SIZED | Spliterator.SUBSIZED);
	}
	/**
	 * @return The element at the specified index regardless of whether this set currently contains the specified index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	@Override
	public E get(int index);
	@Override
	public default E set(int index, E element) {
		throw new UnsupportedOperationException();
	}
	/**
	 * @return The index of the specified element regardless of whether this set currently contains the specified element.
	 */
	public int indexOf(Object element);
	/**
	 * Performs a search for the specified element in this set.
	 * This method, depending on the parameters, returns the index of the element that would be returned by:
	 * <table border="1">
	 *  <tr>
	 *   <td>lower</td><td>inclusive</td><td>method</td>
	 *  </tr>
	 *  <tr>
	 *   <td>false</td><td>false</td><td>higher(element)</td>
	 *  </tr>
	 *  <tr>
	 *   <td>false</td><td>true</td><td>ceiling(element)</td>
	 *  </tr>
	 *  <tr>
	 *   <td>true</td><td>false</td><td>lower(element)</td>
	 *  </tr>
	 *  <tr>
	 *   <td>true</td><td>true</td><td>floor(element)</td>
	 *  </tr>
	 * </table>
	 * If this set contains no such element, returns -1.
	 */
	public int seek(E element,boolean lower,boolean inclusive);
	/**
	 * @return The immutable set of elements this set can contain.
	 * A set and all of it's subsets should return the same value for this method.
	 * The universe is an immutable set that contains all it's indices.
	 * Note that if this set is a subset, it is further limited to the interval specified by
	 * <code>offset()</code> and <code>length()</code>.
	 */
	public OrdinalSet<E> universe();
	/**
	 * @return The offset of this set within the universe.
	 */
	public int offset();
	/**
	 * Removes the specified index from this set.
	 * @return The element at the specified index regardless of whether this set currently contains the specified index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	@Override
	public E remove(int index);
	@Override
	default Iterator<E> iterator() {
		return OrdinalCollection.super.iterator();
	}
	private static void usoe(){
		throw new UnsupportedOperationException();
	}
	public static interface NavigableOrdinalSet<E> extends OrdinalSet<E>,NavigableSet<E>{
		@Override
		default E lower(E e) {
			int dex = seek(e, true, false);
			return dex >= 0 ? get(dex) : null;
		}
		@Override
		default E floor(E e) {
			int dex = seek(e, true, true);
			return dex >= 0 ? get(dex) : null;
		}
		@Override
		default E ceiling(E e) {
			int dex = seek(e, false, true);
			return dex >= 0 ? get(dex) : null;
		}
		@Override
		default E higher(E e) {
			int dex = seek(e, false, false);
			return dex >= 0 ? get(dex) : null;
		}
		@Override
		default NavigableSet<E> descendingSet() {
			return Mirror.mirror(this);
		}
		@Override
		public NavigableOrdinalSet<E> universe();
		@Override
		NavigableOrdinalSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive);
		@Override
		default NavigableOrdinalSet<E> headSet(E toElement, boolean inclusive) {
			return subSet(null,true,toElement,inclusive);
		}
		@Override
		default NavigableOrdinalSet<E> tailSet(E fromElement, boolean inclusive) {
			return subSet(fromElement,inclusive,null,false);
		}
		@Override
		default NavigableOrdinalSet<E> subSet(E fromElement, E toElement) {
			return subSet(fromElement,true,toElement,false);
		}
		@Override
		default NavigableOrdinalSet<E> headSet(E toElement) {
			return subSet(null,true,toElement,false);
		}
		@Override
		default NavigableOrdinalSet<E> tailSet(E fromElement) {
			return subSet(fromElement,true,null,false);
		}
		@Override
		default Spliterator<E> spliterator() {
			return Spliterators.spliterator(this,
					Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.SIZED | Spliterator.SUBSIZED);
		}
		@Override
		default Iterator<E> iterator() {
			return OrdinalSet.super.iterator();
		}
	}
	public static class EmptyOrdinalSet implements NavigableOrdinalSet<Object>,OrdinalList<Object>,Immutable{
		public static EmptyOrdinalSet INSTANCE = new EmptyOrdinalSet();
		private EmptyOrdinalSet(){}
		@Override
		public Object pollFirst() {
			return null;
		}
		@Override
		public Object pollLast() {
			return null;
		}
		@Override
		public Iterator<Object> iterator() {
			return Collections.emptyIterator();
		}
		@Override
		public Iterator<Object> descendingIterator() {
			return Collections.emptyIterator();
		}
		@Override
		@SuppressWarnings("unchecked")
		public Comparator<? super Object> comparator() {
			return (Comparator<Object>)Comparator.naturalOrder();
		}
		@Override
		public Object first() {
			return null;
		}
		@Override
		public Object last() {
			return null;
		}
		@Override
		public int firstIndex() {
			return -1;
		}
		@Override
		public int lastIndex() {
			return -1;
		}
		@Override
		public int size() {
			return 0;
		}
		@Override
		public boolean isEmpty() {
			return true;
		}
		@Override
		public boolean contains(Object o) {
			return false;
		}
		@Override
		public boolean contains(int index) {
			throw new IndexOutOfBoundsException();
		}
		@Override
		public Object[] toArray() {
			return new Object[0];
		}
		@Override
		public <T> T[] toArray(T[] a) {
			if(a.length > 0){
				a[0] = null;
			}
			return a;
		}
		@Override
		public boolean add(Object e) {
			usoe();
			return false;
		}
		@Override
		public boolean remove(Object o) {
			return false;
		}
		@Override
		public boolean add(int index) {
			throw new IndexOutOfBoundsException();
		}
		@Override
		public boolean containsAll(Collection<?> c) {
			return c.isEmpty();
		}
		@Override
		public boolean addAll(Collection<? extends Object> c) {
			usoe();
			return false;
		}
		@Override
		public boolean retainAll(Collection<?> c) {
			return false;
		}
		@Override
		public boolean removeAll(Collection<?> c) {
			return false;
		}
		@Override
		public void clear() {}
		@Override
		public <T> T[] toArray(IntFunction<T[]> generator) {
			return generator.apply(0);
		}
		@Override
		public boolean removeIf(Predicate<? super Object> filter) {
			return false;
		}
		@Override
		public void forEach(Consumer<? super Object> action) {}
		@Override
		public Spliterator<Object> spliterator() {
			return Spliterators.emptySpliterator();
		}
		@Override
		public OfInt indexIterator() {
			return new IndexIterator.OfIndices(0, 0, 0);
		}
		@Override
		public Spliterator.OfInt indexSpliterator() {
			return new IndexIterator.OfIndices(0, 0, 0);
		}
		@Override
		public Object get(int index) {
			throw new IndexOutOfBoundsException();
		}
		@Override
		public int indexOf(Object element) {
			return -1;
		}
		@Override
		public int seek(Object element, boolean lower, boolean inclusive) {
			return -1;
		}
		@Override
		public NavigableOrdinalSet<Object> universe() {
			return this;
		}
		@Override
		public int offset() {
			return 0;
		}
		@Override
		public int length() {
			return 0;
		}
		@Override
		public Object lower(Object e) {
			return null;
		}
		@Override
		public Object floor(Object e) {
			return null;
		}
		@Override
		public Object ceiling(Object e) {
			return null;
		}
		@Override
		public Object higher(Object e) {
			return null;
		}
		@Override
		public NavigableSet<Object> descendingSet() {
			return Reversed.INSTANCE;
		}
		@Override
		public OrdinalList<Object> subList(int fromIndex, int toIndex) {
			if(fromIndex != toIndex){
				throw new IndexOutOfBoundsException();
			}
			return this;
		}
		@Override
		public NavigableOrdinalSet<Object> subSet(Object fromElement, boolean fromInclusive, Object toElement,
				boolean toInclusive) {
			return this;
		}
		@Override
		public int hashCode() {
			return 1;
		}
		@Override
		public boolean addAll(int index, Collection<? extends Object> c) {
			usoe();
			return false;
		}
		@Override
		public Object set(int index, Object element) {
			throw new IndexOutOfBoundsException();
		}
		@Override
		public void add(int index, Object element) {
			usoe();
		}
		@Override
		public Object remove(int index) {
			throw new IndexOutOfBoundsException();
		}
		@Override
		public int lastIndexOf(Object o) {
			return -1;
		}
		@Override
		public ListIterator<Object> listIterator() {
			return Collections.emptyListIterator();
		}
		@Override
		public ListIterator<Object> listIterator(int index) {
			return Collections.emptyListIterator();
		}
		public static class Reversed extends EmptyOrdinalSet{
			@SuppressWarnings("hiding")
			public static Reversed INSTANCE = new Reversed();
			private Reversed(){}
			@SuppressWarnings("unchecked")
			@Override
			public Comparator<? super Object> comparator(){
				return (Comparator<Object>)Comparator.reverseOrder();
			}
			@Override
			public NavigableSet<Object> descendingSet() {
				return EmptyOrdinalSet.INSTANCE;
			}
		}
		@Override
		public boolean equals(Object obj){
			if(obj instanceof Collection){
				return ((Collection<?>)obj).isEmpty();
			}
			return false;
		}
	}
}