package net.balintgergely.util;

import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.Spliterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntFunction;
/**
 * A basic universe implmentation for OrdinalSet. NavigableList is an immutable ordered, sorted collection of distinct elements.
 * It's universe is itself.
 * @author balintgergely
 */
@SuppressWarnings("unchecked")
public abstract class NavigableList<E> extends ArrayListView<E> implements OrdinalSet<E>{
	protected NavigableList(E... vals){
		super(vals);
	}
	@Override
	public E pollFirst() {
		clear();
		return null;
	}
	@Override
	public E pollLast() {
		clear();
		return null;
	}
	@Override
	public Iterator<E> descendingIterator() {
		return Mirror.mirror(new IndexIterator.OfArray<>(data, 0, data.length, data.length,
				Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.SORTED));
	}
	@Override
	public E first() {
		return data[0];
	}
	@Override
	public E last() {
		return data[data.length-1];
	}
	@Override
	public int offset() {
		return 0;
	}
	@Override
	public int length() {
		return data.length;
	}
	@Override
	public int firstIndex() {
		return 0;
	}
	@Override
	public int lastIndex() {
		return data.length-1;
	}
	@Override
	public boolean add(int index) {
		return false;
	}
	@Override
	public boolean contains(Object o) {
		return indexOf(o) >= 0;
	}
	@Override
	public boolean contains(int index) {
		if(index < 0 || index >= data.length){
			throw new IndexOutOfBoundsException();
		}
		return true;
	}
	@Override
	public abstract int indexOf(Object o);
	@Override
	public int lastIndexOf(Object o) {
		return indexOf(o);
	}
	@Override
	public int seek(E element, boolean lower, boolean inclusive) {
		return seek(element,0,data.length-1,lower,inclusive);
	}
	protected abstract int seek(E element, int min, int max, boolean lower, boolean inclusive);
	@Override
	public IndexIterator.OfArray<E> listIterator(int index) {
		return new IndexIterator.OfArray.Sorted<>(data, 0, 0, data.length,Spliterator.IMMUTABLE,comparator());
	}
	@Override
	public OrdinalSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
		int a = fromElement == null ? 0 : seek(fromElement, false, fromInclusive);
		int b = toElement == null ? data.length-1 : seek(toElement, true, toInclusive);
		if(a > b){
			return OrdinalSet.emptySet();
		}
		return new SubList(a, b-a+1);
	}
	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		if(fromIndex < 0 || toIndex < fromIndex || toIndex > data.length){
			throw new IllegalArgumentException();
		}
		if(fromIndex == toIndex){
			return (List<E>)OrdinalSet.emptySet();
		}
		return new SubList(fromIndex, toIndex);
	}
	@Override
	public OrdinalSet<E> universe(){
		return this;
	}
	private class SubList extends AbstractSet<E> implements OrdinalSet<E>,List<E>{
		private final int offset,length;
		private SubList(int offset,int length){
			this.offset = offset;
			this.length = length;
		}
		@Override
		public E pollFirst() {
			clear();
			return null;
		}
		@Override
		public E pollLast() {
			clear();
			return null;
		}
		@Override
		public int firstIndex() {
			return 0;
		}
		@Override
		public int lastIndex() {
			return length-1;
		}
		@Override
		public Iterator<E> iterator() {
			return listIterator(0);
		}
		@Override
		public Iterator<E> descendingIterator() {
			return Mirror.mirror(new IndexIterator.OfArray.Sorted<>(data, offset, offset+length, offset+length, Spliterator.IMMUTABLE, comparator()));
		}
		@Override
		public OfInt indexIterator() {
			return new IndexIterator.OfIndices(offset, offset, offset+length);
		}
		@Override
		public Spliterator.OfInt indexSpliterator() {
			return new IndexIterator.OfIndices(offset, offset, offset+length);
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
			int index = NavigableList.this.indexOf(o);
			return index >= offset && index < offset+length;
		}
		@Override
		public boolean contains(int index) {
			if(index < 0 || index >= length){
				throw new IndexOutOfBoundsException();
			}
			return true;
		}
		@Override
		public Object[] toArray() {
			return toArray(new Object[length]);
		}
		@Override
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
		public boolean remove(Object o) {
			clear();
			return false;
		}
		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
		@Override
		public Comparator<? super E> comparator() {
			return NavigableList.this.comparator();
		}
		@Override
		public E first() {
			return data[offset];
		}
		@Override
		public E last() {
			return data[offset+length-1];
		}
		@Override
		public <T> T[] toArray(IntFunction<T[]> generator) {
			return toArray(generator.apply(length));
		}
		@Override
		public void forEach(Consumer<? super E> action) {
			int i = offset,m = offset+length;
			while(i < m){
				action.accept(data[i]);
				i++;
			}
		}
		@Override
		public ListIterator<E> listIterator() {
			return listIterator();
		}
		@Override
		public IndexIterator<E> listIterator(int index) {
			return new IndexIterator.OfArray.Sorted<>(data, offset, offset, offset+length, Spliterator.IMMUTABLE, comparator());
		}
		@Override
		public Spliterator<E> spliterator() {
			return listIterator(0);
		}
		@Override
		public E get(int index) {
			if(index < 0 || index >= length){
				throw new IndexOutOfBoundsException();
			}
			return data[index+offset];
		}
		@Override
		public int indexOf(Object element) {
			int index = NavigableList.this.indexOf(element);
			return index >= offset && index < offset+length ? index : -1;
		}
		@Override
		public int seek(E element, boolean lower, boolean inclusive) {
			int index = NavigableList.this.seek(element, offset, offset+length-1, lower, inclusive);
			return index >= offset && index < offset+length ? index : -1;
		}
		@Override
		public int offset() {
			return offset;
		}
		@Override
		public int length() {
			return length;
		}
		@Override
		public NavigableSet<E> descendingSet() {
			return Mirror.mirror(this);
		}
		@Override
		public boolean addAll(int index, Collection<? extends E> c) {
			clear();
			return false;
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
		public int lastIndexOf(Object o) {
			return indexOf(o);
		}
		@Override
		public OrdinalSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
			int a = fromElement == null ? 0 : NavigableList.this.seek(fromElement, false, fromInclusive);
			int b = toElement == null ? data.length-1 : NavigableList.this.seek(toElement, true, toInclusive);
			int max = offset+length-1;
			if(a < offset){
				a = offset;
			}
			if(b > max){
				b = max;
			}
			if(a > b){
				return OrdinalSet.emptySet();
			}
			return (OrdinalSet<E>)NavigableList.this.subList(a, b+1);
		}
		@Override
		public List<E> subList(int fromIndex, int toIndex) {
			if(fromIndex < 0 || toIndex < fromIndex || toIndex > length){
				throw new IllegalArgumentException();
			}
			return NavigableList.this.subList(offset+fromIndex, offset+toIndex-fromIndex);
		}
		@Override
		public OrdinalSet<E> universe() {
			return NavigableList.this;
		}
		@Override
		public boolean add(int index) {
			return false;
		}
	}
	private static class SubListImmutable<E> extends NavigableList<E>.SubList{
		private SubListImmutable(NavigableList<E> list,int offset,int length){
			list.super(offset,length);
		}
	}
	/**
	 * A NavigableList determining indexes by performing binary search on the list of elements.
	 * @author balintgergely
	 */
	public static class BinarySearchList<E> extends NavigableList<E>{
		private static final Comparator<Object> I_AM_ERROR = (a,b) -> {throw new IllegalArgumentException();};
		private static final Comparator<?> NATURAL_ERROR = Comparator.naturalOrder().thenComparing(I_AM_ERROR);
		private final Comparator<? super E> comparator;
		public BinarySearchList(Collection<E> col,Comparator<? super E> comp,Class<E> clazz){
			super(Immutable.toArray(col,clazz));
			if(comp == null || comp == Comparator.naturalOrder()){
				Arrays.sort(data,(Comparator<E>)NATURAL_ERROR);
				comparator = (Comparator<E>)Comparator.naturalOrder();
			}else{
				Arrays.sort(data,comp.thenComparing(I_AM_ERROR));
				comparator = comp;
			}
		}
		public BinarySearchList(E[] data,Comparator<E> comparator){
			super(data);
			this.comparator = comparator;
		}
		private BinarySearchList(E[] data){
			super(data);
			comparator = (Comparator<E>)Comparator.naturalOrder();
		}
		@Override
		public int seek(E element, int min, int max, boolean lower, boolean inclusive) {
			if(element != null){
				while(min < max){
					int mid = (min+max) >>> 1,res = comparator.compare(element,data[mid]);
					if(res < 0){
						max = mid-1;
					}else if(res > 0){
						min = mid+1;
					}else{
						return (inclusive ? mid : (lower ? mid-1 : mid+1));
					}
				}
				int res = comparator.compare(element, data[min]);
				if(res < 0){
					return lower ? min-1 : min;
				}else if(res > 0){
					return lower ? min : min+1;
				}else{
					return (inclusive ? min : (lower ? min-1 : min+1));
				}
			}
			return -1;
		}
		@Override
		public Comparator<? super E> comparator() {
			return comparator;
		}
		@Override
		public int indexOf(Object o) {
			if(data.getClass().getComponentType().isInstance(o)){
				int min = 0,max = data.length-1;
				while(min < max){
					int mid = (min+max) >>> 1,res = comparator.compare((E)o,data[mid]);
					if(res < 0){
						max = mid-1;
					}else if(res > 0){
						min = mid+1;
					}else{
						return mid;
					}
				}
				return comparator.compare(data[min],(E)o) == 0 ? min : -1;
			}
			return -1;
		}
	}
	/**
	 * A NavigableList of String objects.
	 * @author balintgergely
	 */
	public static final class StringList extends BinarySearchList<String> implements Immutable{
		public StringList(Collection<String> col){
			super(col,null,String.class);
		}
		StringList(String[] str){
			super(str);
		}
		@Override
		public OrdinalSet<String> subSet(String fromElement, boolean fromInclusive, String toElement, boolean toInclusive) {
			int a = fromElement == null ? 0 : seek(fromElement, false, fromInclusive);
			int b = toElement == null ? data.length-1 : seek(toElement, true, toInclusive);
			if(a > b){
				return OrdinalSet.emptySet();
			}
			return new SubListImmutable<>(this, a, b-a+1);
		}
		@Override
		public List<String> subList(int fromIndex, int toIndex) {
			if(fromIndex < 0 || toIndex < fromIndex || toIndex > data.length){
				throw new IllegalArgumentException();
			}
			if(fromIndex == toIndex){
				return (List<String>)(List<?>)OrdinalSet.emptySet();
			}
			return new SubListImmutable<>(this,fromIndex, toIndex);
		}
	}
	/**
	 * A NavigableList of Enum constants. Each element's ordinal is it's index so <code>indexOf(E)</code> is trivial.
	 * @author balintgergely
	 */
	public static final class EnumList<E extends Enum<E>> extends NavigableList<E> implements Immutable{
		public EnumList(Class<E> clazz){
			super(clazz.getEnumConstants());
		}
		@Override
		public int seek(E element, int min, int max, boolean lower, boolean inclusive) {
			int o = element.ordinal();
			if(inclusive){
				return o;
			}else if(lower){
				return o-1;
			}else{
				o++;
				return o == data.length ? -1 : o;
			}
		}
		public Class<E> getType(){
			return (Class<E>)data.getClass().getComponentType();
		}
		@Override
		public Comparator<? super E> comparator() {
			return Comparator.naturalOrder();
		}
		@Override
		public int indexOf(Object o) {
			return data.getClass().getComponentType().isInstance(o) ? ((Enum<?>)o).ordinal() : -1;
		}
		@Override
		public OrdinalSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
			int a = fromElement == null ? 0 : seek(fromElement, false, fromInclusive);
			int b = toElement == null ? data.length-1 : seek(toElement, true, toInclusive);
			if(a > b){
				return OrdinalSet.emptySet();
			}
			return new SubListImmutable<>(this, a, b-a+1);
		}
		@Override
		public List<E> subList(int fromIndex, int toIndex) {
			if(fromIndex < 0 || toIndex < fromIndex || toIndex > data.length){
				throw new IllegalArgumentException();
			}
			if(fromIndex == toIndex){
				return (List<E>)(List<?>)OrdinalSet.emptySet();
			}
			return new SubListImmutable<>(this,fromIndex, toIndex);
		}
		@Override
		public boolean equals(Object that){
			if(that instanceof EnumList){
				return getType().equals(((EnumList<?>)that).getType());
			}
			return super.equals(that);
		}
		public static class Cache{
			private static ConcurrentHashMap<Class<? extends Enum<?>>, EnumList<?>> cache = new ConcurrentHashMap<>();
			private Cache(){}
			public static <E extends Enum<E>> EnumList<E> get(Class<E> type){
				if(!type.isEnum()){
					throw new IllegalArgumentException();
				}
				return (EnumList<E>)cache.computeIfAbsent(type, EnumList::new);
			}
			public static <E extends Enum<E>> EnumList<E> intern(EnumList<E> els){
				EnumList<E> vl = (EnumList<E>)cache.putIfAbsent(els.getType(), els);
				return vl == null ? els : vl;
			}
		}
	}
}
