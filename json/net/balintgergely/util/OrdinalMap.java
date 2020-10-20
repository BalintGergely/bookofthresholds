package net.balintgergely.util;

import java.util.Map;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;

public interface OrdinalMap<K, V> extends Map<K, V>, IndexSet{
	@Override
	OrdinalSet<K> keySet();
	@Override
	OrdinalCollection<V> values();
	public default K getKey(int index){
		return keySet().get(index);
	}
	public default V getValue(int index){
		return values().get(index);
	}
	@Override
	default OfInt indexIterator() {
		return keySet().indexIterator();
	}
	@Override
	default java.util.Spliterator.OfInt indexSpliterator() {
		return keySet().indexSpliterator();
	}
	@Override
	default int firstIndex() {
		return keySet().firstIndex();
	}
	@Override
	default int lastIndex() {
		return keySet().lastIndex();
	}
	@Override
	default boolean contains(int index) {
		return keySet().contains(index);
	}
	@Override
	default boolean add(int index) {
		throw new UnsupportedOperationException();
	}
	@Override
	default Object remove(int index) {
		Object obj = values().remove(index);
		keySet().remove(index);
		return obj;
	}
	@Override
	default int length() {
		return keySet().length();
	}
	@Override
	default int size() {
		return keySet().size();
	}
	@Override
	default boolean isEmpty() {
		return keySet().isEmpty();
	}
	@Override
	default boolean containsKey(Object key) {
		return keySet().contains(key);
	}
	@Override
	default boolean containsValue(Object value) {
		return values().contains(value);
	}
	@Override
	default V get(Object key) {
		int index = keySet().indexOf(key);
		return index >= 0 ? getValue(index) : null;
	}
	@Override
	default V put(K key, V value) {
		throw new UnsupportedOperationException();
	}
	@Override
	default V remove(Object key) {
		throw new UnsupportedOperationException();
	}
	@Override
	default void putAll(Map<? extends K, ? extends V> m) {
		for(Map.Entry<? extends K, ? extends V> entry : m.entrySet()){
			put(entry.getKey(),entry.getValue());
		}
	}
	@Override
	default void clear() {
		throw new UnsupportedOperationException();
	}
	@Override
	default Set<Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}
}
