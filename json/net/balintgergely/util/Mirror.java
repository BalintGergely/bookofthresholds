package net.balintgergely.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public interface Mirror {
	public static final class DescendingSet<E> extends AbstractSet<E> implements NavigableSet<E>,Mirror{
		public final NavigableSet<E> mirror;
		public DescendingSet(NavigableSet<E> set) {
			this.mirror = Objects.requireNonNull(set);
		}
		@Override
		public Comparator<? super E> comparator() {
			return mirror.comparator().reversed();
		}
		@Override
		public E first() {
			return mirror.last();
		}
		@Override
		public E last() {
			return mirror.first();
		}
		@Override
		public int size() {
			return mirror.size();
		}
		@Override
		public boolean isEmpty() {
			return mirror.isEmpty();
		}
		@Override
		public boolean contains(Object o) {
			return mirror.contains(o);
		}
		@Override
		public Object[] toArray() {
			return toArray(new Object[mirror.size()]);
		}
		@Override
		public <T> T[] toArray(T[] a) {
			a = mirror.toArray(a);
			int len = mirror.size()/2;
			int max = mirror.size()-1;
			for(int i = 0;i < len;i++){
				T t = a[i];
				a[i] = a[max-i];
				a[max-i] = t;
			}
			return a;
		}
		@Override
		public boolean add(E e) {
			return mirror.add(e);
		}
		@Override
		public boolean remove(Object o) {
			return mirror.remove(o);
		}
		@Override
		public boolean containsAll(Collection<?> c) {
			return mirror.containsAll(c);
		}
		@Override
		public boolean addAll(Collection<? extends E> c) {
			return mirror.addAll(c);
		}
		@Override
		public boolean retainAll(Collection<?> c) {
			return mirror.retainAll(c);
		}
		@Override
		public boolean removeAll(Collection<?> c) {
			return mirror.removeAll(c);
		}
		@Override
		public void clear() {
			mirror.clear();
		}
		@Override
		public E lower(E e) {
			return mirror.higher(e);
		}
		@Override
		public E floor(E e) {
			return mirror.ceiling(e);
		}
		@Override
		public E ceiling(E e) {
			return mirror.floor(e);
		}
		@Override
		public E higher(E e) {
			return mirror.lower(e);
		}
		@Override
		public E pollFirst() {
			return mirror.pollFirst();
		}
		@Override
		public E pollLast() {
			return mirror.pollLast();
		}
		@Override
		public Iterator<E> iterator() {
			return mirror.descendingIterator();
		}
		@Override
		public NavigableSet<E> descendingSet() {
			return mirror;
		}
		@Override
		public Iterator<E> descendingIterator() {
			return mirror.iterator();
		}
		@Override
		public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
				E toElement, boolean toInclusive) {
			return new DescendingSet<>(mirror.subSet(toElement, toInclusive, fromElement, fromInclusive)); 
		}
		@Override
		public NavigableSet<E> headSet(E toElement, boolean inclusive) {
			return new DescendingSet<>(mirror.tailSet(toElement, inclusive)); 
		}
		@Override
		public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
			return new DescendingSet<>(mirror.headSet(fromElement, inclusive)); 
		}
		@Override
		public SortedSet<E> subSet(E fromElement,
				E toElement) {
			return subSet(fromElement,true,toElement,false);
		}
		@Override
		public SortedSet<E> headSet(E toElement) {
			return headSet(toElement,false);
		}
		@Override
		public SortedSet<E> tailSet(E fromElement) {
			return tailSet(fromElement,true);
		}
		@SuppressWarnings("unchecked")
		@Override
		public Spliterator<E> spliterator() {
			Iterator<E> itr = mirror.descendingIterator();
			if(itr instanceof Spliterator){
				return (Spliterator<E>)itr;
			}else{
				return super.spliterator();
			}
		}
		@Override
		public <T> T[] toArray(IntFunction<T[]> generator) {
			return toArray(generator.apply(mirror.size()));
		}
		@Override
		public int hashCode() {
			return ~mirror.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			return obj instanceof NavigableSet && mirror.equals(((NavigableSet<?>)obj).descendingSet());
		}
		@Override
		public boolean removeIf(Predicate<? super E> filter) {
			return mirror.removeIf(filter);
		}
	}
	public static final class DescendingMap<K,V> extends AbstractMap<K,V> implements NavigableMap<K,V>,Mirror{
		public final NavigableMap<K,V> mirror;
		public DescendingMap(NavigableMap<K,V> mp){
			this.mirror = Objects.requireNonNull(mp);
		}
		@Override
		public Comparator<? super K> comparator() {
			return mirror.comparator().reversed();
		}
		@Override
		public K firstKey() {
			return mirror.lastKey();
		}
		@Override
		public K lastKey() {
			return mirror.lastKey();
		}
		@Override
		public Set<K> keySet() {
			return navigableKeySet();
		}
		@Override
		public Set<Entry<K, V>> entrySet() {
			Set<Entry<K, V>> st = mirror.entrySet();
			return st instanceof NavigableSet ? ((NavigableSet<Entry<K,V>>)st).descendingSet() : st;
		}
		@Override
		public int size() {
			return mirror.size();
		}
		@Override
		public boolean isEmpty() {
			return false;
		}
		@Override
		public boolean containsKey(Object key) {
			return mirror.containsKey(key);
		}
		@Override
		public boolean containsValue(Object value) {
			return mirror.containsValue(value);
		}
		@Override
		public V get(Object key) {
			return mirror.get(key);
		}
		@Override
		public V put(K key, V value) {
			return mirror.put(key, value);
		}
		@Override
		public V remove(Object key) {
			return mirror.remove(key);
		}
		@Override
		public void putAll(Map<? extends K, ? extends V> m) {
			mirror.putAll(m);
		}
		@Override
		public void clear() {
			mirror.clear();
		}
		@Override
		public V getOrDefault(Object key, V defaultValue) {
			return mirror.getOrDefault(key, defaultValue);
		}
		@Override
		public Entry<K, V> lowerEntry(K key) {
			return mirror.higherEntry(key);
		}
		@Override
		public K lowerKey(K key) {
			return mirror.higherKey(key);
		}
		@Override
		public Entry<K, V> floorEntry(K key) {
			return mirror.ceilingEntry(key);
		}
		@Override
		public K floorKey(K key) {
			return mirror.ceilingKey(key);
		}
		@Override
		public Entry<K, V> ceilingEntry(K key) {
			return mirror.floorEntry(key);
		}
		@Override
		public K ceilingKey(K key) {
			return mirror.floorKey(key);
		}
		@Override
		public Entry<K, V> higherEntry(K key) {
			return mirror.lowerEntry(key);
		}
		@Override
		public K higherKey(K key) {
			return mirror.lowerKey(key);
		}
		@Override
		public Entry<K, V> firstEntry() {
			return mirror.lastEntry();
		}
		@Override
		public Entry<K, V> lastEntry() {
			return mirror.firstEntry();
		}
		@Override
		public Entry<K, V> pollFirstEntry() {
			return mirror.pollLastEntry();
		}
		@Override
		public Entry<K, V> pollLastEntry() {
			return mirror.pollFirstEntry();
		}
		@Override
		public NavigableMap<K, V> descendingMap() {
			return mirror;
		}
		@Override
		public NavigableSet<K> navigableKeySet() {
			return mirror.descendingKeySet();
		}
		@Override
		public NavigableSet<K> descendingKeySet() {
			return mirror.navigableKeySet();
		}
		@Override
		public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
				boolean toInclusive) {
			return new DescendingMap<>(mirror.subMap(toKey, toInclusive, fromKey, fromInclusive));
		}
		@Override
		public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
			return new DescendingMap<>(mirror.tailMap(toKey, inclusive));
		}
		@Override
		public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
			return new DescendingMap<>(mirror.headMap(fromKey, inclusive));
		}
		@Override
		public NavigableMap<K,V> subMap(K fromElement,K toElement) {
			return subMap(fromElement,true,toElement,false);
		}
		@Override
		public NavigableMap<K,V> headMap(K toElement) {
			return headMap(toElement,false);
		}
		@Override
		public NavigableMap<K,V> tailMap(K fromElement) {
			return tailMap(fromElement,true);
		}
		@Override
		public int hashCode() {
			return ~mirror.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			return obj instanceof NavigableMap ? (mirror.equals(((NavigableMap<?,?>)obj).descendingMap())) : false;
		}
	}
}
