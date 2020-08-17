package net.balintgergely.util;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

interface Immutable {
	private static int compare(String a,String b){
		int c = a.compareTo(b);
		return c < 0 ? -1 : (c > 0 ? 1 : 0);
	}
	public static class ImmutableList<E> extends AbstractCollection<E> implements List<E>,Immutable{
		E[] data;
		int offset,length;
		ImmutableList(E[] data){
			this.data = data;
			this.offset = 0;
			this.length = data.length;
		}
		private ImmutableList(E[] data,int offset,int length){
			this.data = data;
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
			Object[] c = new Object[length];
			System.arraycopy(data, offset, c, 0, length);
			return c;
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
		public ArrayIterator<E> iterator() {
			return listIterator(0);
		}
		@Override
		public ArrayIterator<E> spliterator() {
			return listIterator(0);
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
		public ArrayIterator<E> listIterator() {
			return listIterator(0);
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
			return fromIndex == 0 && toIndex == length ? this : new ImmutableList<>(data, offset+fromIndex, fromIndex-toIndex);
		}
	}
	public static class ImmutableSet extends ImmutableList<Map.Entry<String, Object>> implements Set<Map.Entry<String, Object>>{
		ImmutableSet(Map.Entry<String, Object>[] data){
			super(data);
		}
		private ImmutableSet(Map.Entry<String, Object>[] data,int offset,int length){
			super(data,offset,length);
		}
		@Override
		public boolean equals(Object that){
			return that instanceof ImmutableSet && super.equals(that);
		}
		public Entry<String, Object> first() {
			return data[offset];
		}
		public Entry<String, Object> last() {
			return data[offset+length-1];
		}
		@Override
		public boolean contains(Object o){
			return indexOf(o) >= 0;
		}
		@Override
		public int indexOf(Object o) {
			if(o instanceof Map.Entry<?, ?>){
				Object k = ((Map.Entry<?, ?>) o).getKey();
				if(k instanceof String){
					int index = indexOf((String)k);
					if(index >= 0){
						Map.Entry<String, Object> ent = data[offset+index];
						Object v = ((Map.Entry<?,?>) o).getValue();
						return (v == null ? ent.getValue() == null : v.equals(ent.getValue())) ? index : -1;
					}
				}
			}
			return -1;
		}
		public int indexOf(String k) {
			if(k == null){
				return -1;
			}
			int min = offset,max = offset+length-1;
			while(min != max){
				int half = min+(max-min)/2;
				switch(compare(k,data[half].getKey())){
				case -1:max = half-1;break;
				case 0:return half-offset;
				case 1:min = half+1;break;
				}
			}
			return k.compareTo(data[min].getKey()) == 0 ? min-offset : -1;
		}
		public int seekNear(String k,boolean lower,boolean inclusive){
			if(k != null){
				int min = offset,max = offset+length-1;
				while(min != max){
					int half = min+(max-min)/2;
					switch(compare(k,data[half].getKey())){
					case -1:max = half-1;break;
					case 0:return (inclusive ? half : (lower ? half-1 : half+1))-offset;
					case 1:min = half+1;break;
					}
				}
				switch(compare(k,data[min].getKey())){
				case -1:return lower ? min-1 : min;
				case 0:return (inclusive ? min : (lower ? min-1 : min+1))-offset;
				case 1:return lower ? min : min+1;
				}
			}
			return -1;
		}
		@Override
		public int lastIndexOf(Object o) {
			return indexOf(o);
		}
		private static class Itr<E> extends ArrayIterator<E>{
			public Itr(E[] array0, int fromIndex, int index, int toIndex) {
				super(array0, fromIndex, index, toIndex);
			}
			@Override
			public int characteristics(){
				return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.NONNULL |
						Spliterator.SUBSIZED | Spliterator.SORTED | Spliterator.IMMUTABLE;
			}
			@Override
			public Itr<E> trySplit() {
				int start = index;
				int splitPoint = start+(maxIndex-start)/2;
				if(splitPoint != start){
					index = splitPoint;
					minIndex = splitPoint;
					return new Itr<>(array, start, start, splitPoint);
				}
				return null;
			}
			@Override
			@SuppressWarnings("unchecked")
			public Comparator<? super E> getComparator() {
				return (Comparator<? super E>)JSON.entryComparator;
			}
		}
		private static class RevItr<E> extends ArrayIterator.Descending<E>{
			public RevItr(E[] array0, int fromIndex, int index, int toIndex) {
				super(array0, fromIndex, index, toIndex);
			}
			@Override
			public int characteristics(){
				return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.NONNULL |
						Spliterator.SUBSIZED | Spliterator.SORTED | Spliterator.IMMUTABLE;
			}
			@Override
			public RevItr<E> trySplit() {
				int start = index;
				int splitPoint = start-(start-minIndex)/2;
				if(splitPoint != start){
					index = splitPoint;
					maxIndex = splitPoint;
					return new RevItr<>(array, splitPoint, start, start);
				}
				return null;
			}
			@Override
			@SuppressWarnings("unchecked")
			public Comparator<? super E> getComparator() {
				return (Comparator<? super E>)JSON.entryComparator;
			}
		}
		@Override
		public Itr<Entry<String, Object>> listIterator(int index) {
			return new Itr<>(data,offset,offset+index,offset+length);
		}
		public RevItr<Entry<String, Object>> descendingIterator() {
			return new RevItr<>(data, offset,offset+length,offset+length);
		}
		public Set<Entry<String, Object>> subSet(String fromElement, boolean fromInclusive,
				String toElement, boolean toInclusive) {
			int startIndex = fromElement == null ? -1 : seekNear(fromElement, false, fromInclusive);
			int endIndex = toElement == null ? -1 : seekNear(toElement, true, toInclusive);
			if(startIndex < 0){
				startIndex = 0;
			}
			if(endIndex < 0 || endIndex >= length){
				endIndex = length-1;
			}
			if(startIndex == 0 && endIndex == length-1){
				return this;
			}
			if(startIndex > endIndex){
				return Set.of();
			}
			return new ImmutableSet(data,offset+startIndex,endIndex-startIndex+1);
		}
		@Override
		public List<Map.Entry<String, Object>> subList(int fromIndex, int toIndex) {
			if(fromIndex < 0 || toIndex < 0 || fromIndex > toIndex || toIndex > length){
				throw new IndexOutOfBoundsException();
			}
			if(fromIndex == toIndex){
				return List.of();
			}
			return fromIndex == 0 && toIndex == length ? this : new ImmutableSet(data, offset+fromIndex, fromIndex-toIndex);
		}
	}
	public static class ImmutableKeySet extends AbstractSet<String> implements NavigableSet<String>,List<String>,Immutable{
		public final ImmutableSet set;
		public ImmutableKeySet(ImmutableSet s){
			this.set = s;
		}
		@Override
		public Comparator<? super String> comparator() {
			return Comparator.naturalOrder();
		}
		@Override
		public String first() {
			return set.first().getKey();
		}
		@Override
		public String last() {
			return set.last().getKey();
		}
		private static class Eitr implements ListIterator<String>,Spliterator<String>{
			private ArrayIterator<Map.Entry<String, Object>> itr;
			private Eitr(ArrayIterator<Map.Entry<String, Object>> itr){
				this.itr = itr;
			}
			@Override
			public boolean tryAdvance(Consumer<? super String> action) {
				if(itr.hasNext()){
					action.accept(itr.next().getKey());
					return true;
				}
				return false;
			}
			@Override
			public Spliterator<String> trySplit() {
				ArrayIterator<Map.Entry<String, Object>> t = itr.trySplit();
				return t == null ? null : new Eitr(t);
			}
			@Override
			public long estimateSize() {
				return itr.estimateSize();
			}
			@Override
			public int characteristics() {
				return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.NONNULL |
						Spliterator.SUBSIZED | Spliterator.SORTED | Spliterator.IMMUTABLE;
			}
			@Override
			public boolean hasNext() {
				return itr.hasNext();
			}
			@Override
			public String next() {
				return itr.next().getKey();
			}
			@Override
			public void forEachRemaining(Consumer<? super String> action) {
				while(itr.hasNext()){
					action.accept(itr.next().getKey());
				}
			}
			@Override
			public Comparator<? super String> getComparator() {
				return itr instanceof ImmutableSet.RevItr ? Comparator.reverseOrder() : Comparator.naturalOrder();
			}
			@Override
			public boolean hasPrevious() {
				return itr.hasPrevious();
			}
			@Override
			public String previous() {
				return itr.previous().getKey();
			}
			@Override
			public int nextIndex() {
				return itr.nextIndex();
			}
			@Override
			public int previousIndex() {
				return itr.previousIndex();
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			@Override
			public void set(String e) {
				remove();
			}
			@Override
			public void add(String e) {
				remove();
			}
		}
		@Override
		public Spliterator<String> spliterator() {
			return new Eitr(set.listIterator(0));
		}
		@Override
		public int size() {
			return set.size();
		}
		@Override
		public boolean isEmpty() {
			return false;
		}
		@Override
		public boolean contains(Object o) {
			if(o instanceof String){
				return set.indexOf((String)o) >= 0;
			}
			return false;
		}
		@Override
		public Object[] toArray() {
			return toArray(new Object[set.length]);
		}
		@Override
		@SuppressWarnings("unchecked")
		public <T> T[] toArray(T[] a) {
			if(a.length < set.length){
				a = (T[])Array.newInstance(a.getClass().getComponentType(), set.length);
			}else if(a.length > set.length){
				a[set.length] = null;
			}
			for(int i = 0;i < set.length;i++){
				a[i] = (T)set.get(i).getKey();
			}
			return a;
		}
		@Override
		public boolean add(String e) {
			clear();
			return false;
		}
		@Override
		public boolean remove(Object o) {
			clear();
			return false;
		}
		@Override
		public boolean addAll(Collection<? extends String> c) {
			clear();
			return false;
		}
		@Override
		public boolean retainAll(Collection<?> c) {
			clear();
			return false;
		}
		@Override
		public boolean removeAll(Collection<?> c) {
			clear();
			return false;
		}
		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
		@Override
		public <T> T[] toArray(IntFunction<T[]> generator) {
			return toArray(generator.apply(set.size()));
		}
		@Override
		public boolean removeIf(Predicate<? super String> filter) {
			clear();
			return false;
		}
		@Override
		public String lower(String key) {
			int index = set.seekNear(key,true,false);
			if(index >= 0 && index < set.length){
				return set.get(index).getKey();
			}
			return null;
		}
		@Override
		public String floor(String key) {
			int index = set.seekNear(key,true,true);
			if(index >= 0 && index < set.length){
				return set.get(index).getKey();
			}
			return null;
		}
		@Override
		public String ceiling(String key) {
			int index = set.seekNear(key,false,true);
			if(index >= 0 && index < set.length){
				return set.get(index).getKey();
			}
			return null;
		}
		@Override
		public String higher(String key) {
			int index = set.seekNear(key,false,false);
			if(index >= 0 && index < set.length){
				return set.get(index).getKey();
			}
			return null;
		}
		@Override
		public String pollFirst() {
			clear();
			return null;
		}
		@Override
		public String pollLast() {
			clear();
			return null;
		}
		@Override
		public Iterator<String> iterator() {
			return new Eitr(set.listIterator(0));
		}
		@Override
		public NavigableSet<String> descendingSet() {
			return new Mirror.DescendingSet<>(this);
		}
		@Override
		public Iterator<String> descendingIterator() {
			return new Eitr(set.descendingIterator());
		}
		@Override
		public NavigableSet<String> subSet(String fromElement, boolean fromInclusive, String toElement,
				boolean toInclusive) {
			Set<Map.Entry<String, Object>> st = set.subSet(fromElement, fromInclusive, toElement, toInclusive);
			return st == set ? this : (st instanceof ImmutableSet ? new ImmutableKeySet((ImmutableSet)st) : Collections.emptyNavigableSet());
		}
		@Override
		public NavigableSet<String> headSet(String toElement, boolean inclusive) {
			return subSet(null,true,toElement,inclusive);
		}
		@Override
		public NavigableSet<String> tailSet(String fromElement, boolean inclusive) {
			return subSet(fromElement,inclusive,null,true);
		}
		@Override
		public SortedSet<String> subSet(String fromElement,
				String toElement) {
			return subSet(fromElement,true,toElement,false);
		}
		@Override
		public SortedSet<String> headSet(String toElement) {
			return subSet(null,true,toElement,false);
		}
		@Override
		public SortedSet<String> tailSet(String fromElement) {
			return subSet(fromElement,true,null,false);
		}
		@Override
		public boolean addAll(int index, Collection<? extends String> c) {
			clear();
			return false;
		}
		@Override
		public String get(int index) {
			return set.get(index).getKey();
		}
		@Override
		public String set(int index, String element) {
			clear();
			return null;
		}
		@Override
		public void add(int index, String element) {
			clear();
		}
		@Override
		public String remove(int index) {
			clear();
			return null;
		}
		@Override
		public int indexOf(Object o) {
			if(o instanceof String){
				return set.indexOf((String)o);
			}
			return -1;
		}
		@Override
		public int lastIndexOf(Object o) {
			return indexOf(o);
		}
		@Override
		public ListIterator<String> listIterator() {
			return new Eitr(set.listIterator(0));
		}
		@Override
		public ListIterator<String> listIterator(int index) {
			return new Eitr(set.listIterator(index));
		}
		@Override
		public List<String> subList(int fromIndex, int toIndex) {
			List<Map.Entry<String, Object>> sb = set.subList(fromIndex, toIndex);
			return sb == set ? this : (sb instanceof ImmutableSet ? new ImmutableKeySet((ImmutableSet)sb) : List.of());
		}
	}
	public static class ImmutableValueCollection extends AbstractCollection<Object> implements List<Object>,Immutable{
		public final ImmutableSet set;
		public ImmutableValueCollection(ImmutableSet s){
			this.set = s;
		}
		private static class Eitr implements ListIterator<Object>,Spliterator<Object>{
			private ImmutableSet.Itr<Map.Entry<String, Object>> itr;
			private Eitr(ImmutableSet.Itr<Map.Entry<String, Object>> itr){
				this.itr = itr;
			}
			@Override
			public boolean tryAdvance(Consumer<? super Object> action) {
				if(itr.hasNext()){
					action.accept(itr.next().getValue());
					return true;
				}
				return false;
			}
			@Override
			public Spliterator<Object> trySplit() {
				ImmutableSet.Itr<Map.Entry<String, Object>> t = itr.trySplit();
				return t == null ? null : new Eitr(t);
			}
			@Override
			public long estimateSize() {
				return itr.estimateSize();
			}
			@Override
			public int characteristics() {
				return Spliterator.ORDERED | Spliterator.SIZED | 
						Spliterator.SUBSIZED | Spliterator.IMMUTABLE;
			}
			@Override
			public boolean hasNext() {
				return itr.hasNext();
			}
			@Override
			public Object next() {
				return itr.next().getValue();
			}
			@Override
			public boolean hasPrevious() {
				return itr.hasPrevious();
			}
			@Override
			public Object previous() {
				return itr.previous().getValue();
			}
			@Override
			public void forEachRemaining(Consumer<? super Object> action) {
				while(itr.hasNext()){
					action.accept(itr.next().getValue());
				}
			}
			@Override
			public int nextIndex() {
				return itr.nextIndex();
			}
			@Override
			public int previousIndex() {
				return itr.previousIndex();
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			@Override
			public void set(Object e) {
				remove();
			}
			@Override
			public void add(Object e) {
				remove();
			}
		}
		@Override
		public Spliterator<Object> spliterator() {
			return new Eitr(set.listIterator(0));
		}
		@Override
		public int size() {
			return set.size();
		}
		@Override
		public boolean isEmpty() {
			return false;
		}
		@Override
		public boolean contains(Object o) {
			for(Map.Entry<String, Object> et : set){
				if(o == null ? et.getValue() == null : o.equals(et.getValue())){
					return true;
				}
			}
			return false;
		}
		@Override
		public Object[] toArray() {
			return toArray(new Object[set.length]);
		}
		@Override
		@SuppressWarnings("unchecked")
		public <T> T[] toArray(T[] a) {
			if(a.length < set.length){
				a = (T[])Array.newInstance(a.getClass().getComponentType(), set.length);
			}else if(a.length > set.length){
				a[set.length] = null;
			}
			for(int i = 0;i < set.length;i++){
				a[i] = (T)set.get(i).getValue();
			}
			return a;
		}
		@Override
		public boolean add(Object e) {
			clear();
			return false;
		}
		@Override
		public boolean remove(Object o) {
			clear();
			return false;
		}
		@Override
		public boolean addAll(Collection<? extends Object> c) {
			clear();
			return false;
		}
		@Override
		public boolean retainAll(Collection<?> c) {
			clear();
			return false;
		}
		@Override
		public boolean removeAll(Collection<?> c) {
			clear();
			return false;
		}
		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
		@Override
		public <T> T[] toArray(IntFunction<T[]> generator) {
			return toArray(generator.apply(set.size()));
		}
		@Override
		public boolean removeIf(Predicate<? super Object> filter) {
			clear();
			return false;
		}
		@Override
		public Iterator<Object> iterator() {
			return new Eitr(set.listIterator(0));
		}
		@Override
		public boolean addAll(int index, Collection<? extends Object> c) {
			clear();
			return false;
		}
		@Override
		public Object get(int index) {
			return set.get(index).getValue();
		}
		@Override
		public Object set(int index, Object element) {
			clear();
			return null;
		}
		@Override
		public void add(int index, Object element) {
			clear();
		}
		@Override
		public Object remove(int index) {
			clear();
			return null;
		}
		@Override
		public int indexOf(Object o) {
			int i = 0;
			while(i < set.length){
				if(o == null ? set.get(i).getValue() == null : o.equals(set.get(i).getValue())){
					return i;
				}
				i++;
			}
			return -1;
		}
		@Override
		public int lastIndexOf(Object o) {
			int i = set.length;
			while(i > 0){
				i--;
				if(o == null ? set.get(i).getValue() == null : o.equals(set.get(i).getValue())){
					return i;
				}
			}
			return -1;
		}
		@Override
		public ListIterator<Object> listIterator() {
			return new Eitr(set.listIterator(0));
		}
		@Override
		public ListIterator<Object> listIterator(int index) {
			return new Eitr(set.listIterator(index));
		}
		@Override
		public List<Object> subList(int fromIndex, int toIndex) {
			List<Map.Entry<String, Object>> sb = set.subList(fromIndex, toIndex);
			return sb == set ? this : (sb instanceof ImmutableSet ? new ImmutableValueCollection((ImmutableSet)sb) : List.of());
		}
	}
	public static class ImmutableMap implements NavigableMap<String,Object>,Immutable{
		public static String gkon(Map.Entry<String, ?> e){
			return e == null ? null : e.getKey();
		}
		private ImmutableSet entries;
		ImmutableMap(ImmutableSet ent){
			entries = ent;
		}
		@Override
		public Comparator<? super String> comparator() {
			return Comparator.naturalOrder();
		}
		@Override
		public String firstKey() {
			return entries.first().getKey();
		}
		@Override
		public String lastKey() {
			return entries.last().getKey();
		}
		@Override
		public Object getOrDefault(Object key, Object defaultValue) {
			if(key instanceof String){
				int index = entries.indexOf((String)key);
				if(index >= 0){
					return entries.get(index).getValue();
				}
			}
			return defaultValue;
		}
		@Override
		public Entry<String, Object> lowerEntry(String key) {
			int index = entries.seekNear(key,true,false);
			if(index >= 0 && index < entries.length){
				return entries.get(index);
			}
			return null;
		}
		@Override
		public String lowerKey(String key) {
			return gkon(lowerEntry(key));
		}
		@Override
		public Entry<String, Object> floorEntry(String key) {
			int index = entries.seekNear(key,true,true);
			if(index >= 0 && index < entries.length){
				return entries.get(index);
			}
			return null;
		}
		@Override
		public String floorKey(String key) {
			return gkon(floorEntry(key));
		}
		@Override
		public Entry<String, Object> ceilingEntry(String key) {
			int index = entries.seekNear(key,false,true);
			if(index >= 0 && index < entries.length){
				return entries.get(index);
			}
			return null;
		}
		@Override
		public String ceilingKey(String key) {
			return gkon(ceilingEntry(key));
		}
		@Override
		public Entry<String, Object> higherEntry(String key) {
			int index = entries.seekNear(key,false,false);
			if(index >= 0 && index < entries.length){
				return entries.get(index);
			}
			return null;
		}
		@Override
		public String higherKey(String key) {
			return gkon(higherEntry(key));
		}
		@Override
		public Entry<String, Object> firstEntry() {
			return entries.first();
		}
		@Override
		public Entry<String, Object> lastEntry() {
			return entries.last();
		}
		@Override
		public Entry<String, Object> pollFirstEntry() {
			clear();
			return null;
		}
		@Override
		public Entry<String, Object> pollLastEntry() {
			clear();
			return null;
		}
		@Override
		public NavigableMap<String, Object> descendingMap() {
			return new Mirror.DescendingMap<>(this);
		}
		@Override
		public NavigableSet<String> navigableKeySet() {
			return new ImmutableKeySet(entries);
		}
		@Override
		public NavigableSet<String> descendingKeySet() {
			return new ImmutableKeySet(entries).descendingSet();
		}
		@Override
		public NavigableMap<String, Object> subMap(String fromKey, boolean fromInclusive, String toKey,
				boolean toInclusive) {
			Set<Entry<String,Object>> st = entries.subSet(fromKey, fromInclusive, toKey, toInclusive);
			return st == entries ? this : (st instanceof ImmutableSet ? new ImmutableMap((ImmutableSet)st) : Collections.emptyNavigableMap());
		}
		@Override
		public NavigableMap<String, Object> headMap(String toKey, boolean inclusive) {
			return subMap(null, false, toKey, inclusive);
		}
		@Override
		public NavigableMap<String, Object> tailMap(String fromKey, boolean inclusive) {
			return subMap(fromKey, inclusive, null, false);
		}
		@Override
		public SortedMap<String, Object> subMap(String fromKey, String toKey) {
			return subMap(fromKey,true,toKey,false);
		}
		@Override
		public SortedMap<String, Object> headMap(String toKey) {
			return subMap(null,false,toKey,false);
		}
		@Override
		public SortedMap<String, Object> tailMap(String fromKey) {
			return subMap(fromKey,true,null,false);
		}
		@Override
		public int size() {
			return entries.size();
		}
		@Override
		public boolean isEmpty() {
			return false;
		}
		@Override
		public boolean containsKey(Object key) {
			return key instanceof String && (entries.indexOf((String)key) >= 0);
		}
		@Override
		public Object get(Object key) {
			if(key instanceof String){
				int index = entries.indexOf((String)key);
				return index < 0 ? null : entries.get(index).getValue();
			}
			return false;
		}
		@Override
		public Object put(String key, Object value) {
			clear();
			return null;
		}
		@Override
		public Object remove(Object key) {
			clear();
			return null;
		}
		@Override
		public void putAll(Map<? extends String, ? extends Object> m) {
			clear();
		}
		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
		@Override
		public Set<String> keySet() {
			return navigableKeySet();
		}
		@Override
		public Set<Entry<String, Object>> entrySet() {
			return entries;
		}
		@Override
		public Collection<Object> values() {
			return new ImmutableValueCollection(entries);
		}
		@Override
		public boolean containsValue(Object value) {
			for(Map.Entry<String, Object> et : entries){
				if(entries == null ? et.getValue() == null : value.equals(et.getValue())){
					return true;
				}
			}
			return false;
		}
	}
}
