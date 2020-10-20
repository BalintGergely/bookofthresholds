package net.balintgergely.util;

import java.lang.reflect.Array;
import java.util.AbstractSet;
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
import java.util.Spliterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.Consumer;

import net.balintgergely.util.Immutable.ImmutableList;
import net.balintgergely.util.Immutable.ImmutableSubList;
import net.balintgergely.util.NavigableList.BinarySearchList;
import net.balintgergely.util.OrdinalCollection.OrdinalList;

public class ImmutableOrdinalMap<K,V> implements OrdinalMap<K,V>,NavigableMap<K,V>{
	private final OrdinalSet<K> keys;
	private final ArrayListView<V> values;
	@SuppressWarnings("unchecked")
	public static <K,V> OrdinalMap<K,V> of(SortedMap<K,V> entries,Class<K> keyClass){
		NavigableList<K> universe;
		Comparator<? super K> comparator = entries.comparator();
		if(keyClass == String.class && (comparator == null || comparator.equals(Comparator.naturalOrder()))){
			universe = (NavigableList<K>)new NavigableList.StringList((Set<String>)entries.keySet());
		}else{
			universe = new BinarySearchList<>(entries.keySet(),comparator,keyClass);
		}
		V[] values = (V[])new Object[universe.size()];
		for(int i = 0;i < values.length;i++){
			values[i] = entries.get(universe.get(i));
		}
		return new ImmutableOrdinalMap<>(universe, new ArrayListView<>(values));
	}
	public static <K,V> OrdinalMap<K,V> combine(OrdinalSet<K> keys,ArrayListView<V> values){
		if(keys.isEmpty()){
			throw new IllegalArgumentException();
		}
		int off = keys.offset();
		if(keys.size() != values.size() || (off != 0 && ((ArrayListView.SubList<V>)values).offset != off)){
			throw new IllegalArgumentException();
		}
		if(keys instanceof Immutable && values instanceof Immutable){
			return new II<>(keys, values);
		}else{
			return new ImmutableOrdinalMap<>(keys, values);
		}
	}
	ImmutableOrdinalMap(OrdinalSet<K> keys,ArrayListView<V> values){
		this.keys = keys;
		this.values = values;
	}
	@Override
	public Comparator<? super K> comparator() {
		return keys.comparator();
	}
	@Override
	public K firstKey() {
		return keys.first();
	}
	@Override
	public K lastKey() {
		return keys.last();
	}
	@Override
	public OrdinalSet<K> keySet() {
		return keys;
	}
	@Override
	public NavigableSet<K> navigableKeySet() {
		return keys;
	}
	@Override
	public OrdinalList<V> values() {
		return values;
	}
	@Override
	public int size() {
		return keys.size();
	}
	@Override
	public boolean isEmpty() {
		return false;
	}
	@Override
	public boolean containsKey(Object key) {
		return keys.contains(key);
	}
	@Override
	public boolean containsValue(Object value) {
		return values.contains(value);
	}
	@Override
	public V get(Object key) {
		int o = keys.indexOf(key);
		return o >= 0 ? values.get(o) : null;
	}
	@Override
	public V put(K key, V value) {
		clear();
		return null;
	}
	@Override
	public V remove(Object key) {
		clear();
		return null;
	}
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		clear();
	}
	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
	@Override
	public Entry<K, V> lowerEntry(K key) {
		int index = keys.seek(key, true, false);
		return index >= 0 ? new MapEntry<>(keys.get(index), values.get(index)) : null;
	}
	@Override
	public Entry<K, V> floorEntry(K key) {
		int index = keys.seek(key, true, true);
		return index >= 0 ? new MapEntry<>(keys.get(index), values.get(index)) : null;
	}
	@Override
	public Entry<K, V> ceilingEntry(K key) {
		int index = keys.seek(key, false, true);
		return index >= 0 ? new MapEntry<>(keys.get(index), values.get(index)) : null;
	}
	@Override
	public Entry<K, V> higherEntry(K key) {
		int index = keys.seek(key, false, false);
		return index >= 0 ? new MapEntry<>(keys.get(index), values.get(index)) : null;
	}
	@Override
	public K lowerKey(K key) {
		return keys.lower(key);
	}
	@Override
	public K floorKey(K key) {
		return keys.floor(key);
	}
	@Override
	public K ceilingKey(K key) {
		return keys.ceiling(key);
	}
	@Override
	public K higherKey(K key) {
		return keys.higher(key);
	}
	@Override
	public Entry<K, V> firstEntry() {
		return new MapEntry<>(keys.first(), values.get(0));
	}
	@Override
	public Entry<K, V> lastEntry() {
		return new MapEntry<>(keys.last(), values.get(values.size()-1));
	}
	@Override
	public Entry<K, V> pollFirstEntry() {
		clear();
		return null;
	}
	@Override
	public Entry<K, V> pollLastEntry() {
		clear();
		return null;
	}
	@Override
	public NavigableMap<K, V> descendingMap() {
		return Mirror.mirror(this);
	}
	@Override
	public NavigableSet<K> descendingKeySet() {
		return keys.descendingSet();
	}
	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		NavigableSet<K> subKeySet = keys.subSet(fromKey, fromInclusive, toKey, toInclusive);
		if(subKeySet instanceof OrdinalSet){
			OrdinalSet<K> sks = (OrdinalSet<K>)subKeySet;
			return new ImmutableOrdinalMap<>(sks, new ImmutableSubList<>(values.data, sks.offset(), sks.length()));
		}
		return Collections.emptyNavigableMap();
	}
	@Override
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		return subMap(null, true, toKey, inclusive);
	}
	@Override
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		return subMap(fromKey, inclusive, null, false);
	}
	@Override
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		return subMap(fromKey,true,toKey,false);
	}
	@Override
	public SortedMap<K, V> headMap(K toKey) {
		return subMap(null,true,toKey,false);
	}
	@Override
	public SortedMap<K, V> tailMap(K fromKey) {
		return subMap(fromKey,true,null,false);
	}
	@Override
	public Set<Entry<K, V>> entrySet() {
		return new EntrySet<>(keys,values);
	}
	static class II<K,V> extends ImmutableOrdinalMap<K,V> implements Immutable{
		II(OrdinalSet<K> keys, ArrayListView<V> values) {
			super(keys, values);
		}
		@Override
		public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
			NavigableSet<K> subKeySet = super.keys.subSet(fromKey, fromInclusive, toKey, toInclusive);
			if(subKeySet instanceof OrdinalSet){
				OrdinalSet<K> sks = (OrdinalSet<K>)subKeySet;
				return new II<>(sks, new ImmutableSubList<>(super.values.data, sks.offset(), sks.length()));
			}
			return Collections.emptyNavigableMap();
		}
		@Override
		public Set<Entry<K, V>> entrySet() {
			return new IIs<>(super.keys,super.values);
		}
	}
	static class EntrySet<K,V> extends AbstractSet<Entry<K, V>> implements OrdinalSet<Entry<K,V>>{
		private final OrdinalSet<K> keys;
		private final ArrayListView<V> values;
		private EntrySet(OrdinalSet<K> keys,ArrayListView<V> values){
			this.keys = keys;
			this.values = values;
		}
		@Override
		public void forEach(Consumer<? super Entry<K, V>> action) {
			int index = 0,length = values.size();
			while(index < length){
				action.accept(new MapEntry<>(keys.get(index), values.get(index)));
				index++;
			}
		}
		@Override
		public int size() {
			return keys.size();
		}
		@Override
		public boolean isEmpty() {
			return false;
		}
		@Override
		public boolean contains(Object o) {
			if(o instanceof Map.Entry){
				Object key = ((Map.Entry<?, ?>)o).getKey();
				int index;
				if(key != null && (index = keys.indexOf(key)) >= 0){
					o = ((Map.Entry<?, ?>)o).getValue();
					return o == null ? values.get(index) == null : o.equals(values.get(index));
				}
			}
			return false;
		}
		@Override
		public boolean contains(int index){
			return keys.contains(index);
		}
		@Override
		public Iterator<Entry<K, V>> iterator(){
			return listIterator(0);
		}
		@Override
		public Iterator<Entry<K, V>> descendingIterator() {
			return Mirror.mirror(new IndexIterator.OfFunction.Sorted<>(this::get, 0, keys.size(), keys.size(), 0, comparator()));
		}
		@Override
		public OfInt indexIterator() {
			return keys.indexIterator();
		}
		@Override
		public Spliterator.OfInt indexSpliterator() {
			return keys.indexSpliterator();
		}
		@Override
		public Object[] toArray() {
			return toArray(new Object[values.size()]);
		}
		@Override
		@SuppressWarnings("unchecked")
		public <T> T[] toArray(T[] a) {
			if(a.length < values.size()){
				a = (T[])Array.newInstance(a.getClass().getComponentType(), values.size());
			}else if(a.length > values.size()){
				a[values.size()] = null;
			}
			for(int i = 0;i < a.length;i++){
				a[i] = (T)new MapEntry<>(keys.get(i),values.get(i));
			}
			return a;
		}
		@Override
		public boolean add(Entry<K, V> e) {
			clear();
			return false;
		}
		@Override
		public boolean remove(Object o) {
			clear();
			return false;
		}
		@Override
		public Entry<K, V> get(int index) {
			if(index < 0 || index >= keys.size()){
				throw new IndexOutOfBoundsException(index);
			}
			return new MapEntry<>(keys.get(index),values.get(index));
		}
		@Override
		public Entry<K, V> remove(int index) {
			clear();
			return null;
		}
		@Override
		public int indexOf(Object o) {
			if(o instanceof Map.Entry<?, ?>){
				int index = keys.indexOf(((Map.Entry<?, ?>)o).getKey());
				if(index >= 0){
					o = ((Map.Entry<?, ?>)o).getValue();
					return (o == null ? values.get(index) == null : o.equals(values.get(index))) ? index : -1;
				}
			}
			return -1;
		}
		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
		@Override
		public Spliterator<Entry<K, V>> spliterator() {
			return listIterator(0);
		}
		public IndexIterator<Entry<K, V>> listIterator(int index) {
			if(index < 0 || index > values.size()){
				throw new IndexOutOfBoundsException(index);
			}
			return new IndexIterator.OfFunction.Sorted<>(this::get, 0, index, keys.size(), 0, comparator());
		}
		@Override
		@SuppressWarnings("unchecked")
		public Comparator<? super Entry<K, V>> comparator() {
			Comparator<? super K> kcomp = keys.comparator();
			return kcomp == Comparator.naturalOrder() ?
					(Comparator<? super Entry<K, V>>)(Comparator<?>)JSON.MAP_ENTRY_COMPARATOR :
					Comparator.comparing(Map.Entry::getKey, kcomp);
		}
		@Override
		public Entry<K, V> first() {
			return new MapEntry<>(keys.first(),values.get(0));
		}
		@Override
		public Entry<K, V> last() {
			return new MapEntry<>(keys.last(),values.get(values.size()-1));
		}
		@Override
		public Entry<K, V> lower(Entry<K, V> entry) {
			int index = keys.seek(entry.getKey(), true, false);
			return index >= 0 ? new MapEntry<>(keys.get(index), values.get(index)) : null;
		}
		@Override
		public Entry<K, V> floor(Entry<K, V> entry) {
			int index = keys.seek(entry.getKey(), true, true);
			return index >= 0 ? new MapEntry<>(keys.get(index), values.get(index)) : null;
		}
		@Override
		public Entry<K, V> ceiling(Entry<K, V> entry) {
			int index = keys.seek(entry.getKey(), false, true);
			return index >= 0 ? new MapEntry<>(keys.get(index), values.get(index)) : null;
		}
		@Override
		public Entry<K, V> higher(Entry<K, V> entry) {
			int index = keys.seek(entry.getKey(), false, false);
			return index >= 0 ? new MapEntry<>(keys.get(index), values.get(index)) : null;
		}
		@Override
		public Entry<K, V> pollFirst() {
			clear();
			return null;
		}
		@Override
		public Entry<K, V> pollLast() {
			clear();
			return null;
		}
		@Override
		public OrdinalSet<Entry<K, V>> subSet(Entry<K, V> fromElement, boolean fromInclusive, Entry<K, V> toElement,
				boolean toInclusive) {
			NavigableSet<K> subKeySet = keys.subSet(fromElement.getKey(), fromInclusive, toElement.getKey(), toInclusive);
			if(subKeySet instanceof OrdinalSet){
				OrdinalSet<K> sks = (OrdinalSet<K>)subKeySet;
				return new EntrySet<>(sks, new ImmutableSubList<>(values.data, sks.offset(), sks.length()));
			}
			return OrdinalSet.emptySet();
		}
		@Override
		public NavigableSet<Entry<K, V>> descendingSet() {
			return Mirror.mirror(this);
		}
		@Override
		public int seek(Entry<K, V> element, boolean lower, boolean inclusive) {
			return keys.seek(element.getKey(), lower, inclusive);
		}
		@Override
		public int offset() {
			return keys.offset();
		}
		@Override
		public int length() {
			return keys.length();
		}
		@Override
		public OrdinalSet<Entry<K, V>> universe() {
			return new EntrySet<>(keys.universe(), new ImmutableList<>(values.data));
		}
		@Override
		public boolean add(int index) {
			return false;
		}
		@Override
		public int firstIndex() {
			return keys.firstIndex();
		}
		@Override
		public int lastIndex() {
			return keys.lastIndex();
		}
	}
	static class IIs<K,V> extends EntrySet<K,V> implements List<Entry<K,V>>,Immutable{
		IIs(OrdinalSet<K> keys, ArrayListView<V> values) {
			super(keys, values);
		}
		@Override
		public ListIterator<Entry<K, V>> listIterator() {
			return listIterator(0);
		}
		@Override
		public boolean addAll(int index, Collection<? extends Entry<K, V>> c) {
			clear();
			return false;
		}
		@Override
		public Entry<K, V> set(int index, Entry<K, V> element) {
			clear();
			return null;
		}
		@Override
		public void add(int index, Entry<K, V> element) {
			clear();
		}
		@Override
		public int lastIndexOf(Object o) {
			return indexOf(o);
		}
		@Override
		public OrdinalSet<Entry<K, V>> universe() {
			return new EntrySet<>(super.keys.universe(), new ImmutableList<>(super.values.data));
		}
		@Override
		public List<Entry<K, V>> subList(int fromIndex, int toIndex) {
			@SuppressWarnings("unchecked")
			List<K> klist = ((List<K>)super.keys).subList(fromIndex, toIndex);
			if(klist.isEmpty()){
				return Collections.emptyList();
			}
			@SuppressWarnings("unchecked")
			OrdinalSet<K> kset = (OrdinalSet<K>)klist;
			return new IIs<>(kset, new ImmutableSubList<>(super.values.data,kset.offset(),kset.length()));
		}
		@Override
		public OrdinalSet<Entry<K, V>> subSet(Entry<K, V> fromElement, boolean fromInclusive, Entry<K, V> toElement,
				boolean toInclusive) {
			NavigableSet<K> subKeySet = super.keys.subSet(fromElement.getKey(), fromInclusive, toElement.getKey(), toInclusive);
			if(subKeySet instanceof OrdinalSet){
				OrdinalSet<K> sks = (OrdinalSet<K>)subKeySet;
				return new IIs<>(sks, new ImmutableSubList<>(super.values.data, sks.offset(), sks.length()));
			}
			return OrdinalSet.emptySet();
		}
	}
	@Override
	public int hashCode() {
		return keys.hashCode() ^ values.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Map<?,?>){
			Map<?,?> other = (Map<?,?>)obj;
			return keys.equals(other.keySet()) && values.equals(other.values());
		}
		return false;
	}
	
}
