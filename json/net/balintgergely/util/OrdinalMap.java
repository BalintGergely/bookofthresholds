package net.balintgergely.util;

import java.util.Map;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface OrdinalMap<K, V> extends Map<K, V>, IndexSet{
	@SuppressWarnings("unchecked")
	public static <K,V> EmptyOrdinalMap<K,V> emptyMap(){
		return (EmptyOrdinalMap<K, V>)EmptyOrdinalMap.INSTANCE;
	}
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
	/*@Override
	default int firstUnsetIndex() {
		return keySet().firstUnsetIndex();
	}
	@Override
	default int lastUnsetIndex() {
		return keySet().lastUnsetIndex();
	}*/
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
	default V putIfKey(K key, V value) {
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
	default void putAllKeys(Map<? extends K, ? extends V> m) {
		for(Map.Entry<? extends K, ? extends V> entry : m.entrySet()){
			putIfKey(entry.getKey(),entry.getValue());
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
	public static final class EmptyOrdinalMap<K,V> implements OrdinalMap<K, V>, Immutable{
		public static final EmptyOrdinalMap<?,?> INSTANCE = new EmptyOrdinalMap<>();
		@Override
		public V getOrDefault(Object key, V defaultValue) {
			return defaultValue;
		}
		@Override
		public void forEach(BiConsumer<? super K, ? super V> action) {}
		@Override
		public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {}
		@Override
		public V putIfAbsent(K key, V value) {
			throw new IllegalArgumentException();//Not a key.
		}
		@Override
		public boolean remove(Object key, Object value) {
			return false;
		}
		@Override
		public boolean replace(K key, V oldValue, V newValue) {
			return false;
		}
		@Override
		public V replace(K key, V value) {
			return null;
		}
		@Override
		public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
			throw new IllegalArgumentException();
		}
		@Override
		public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
			return null;
		}
		@Override
		public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
			throw new IllegalArgumentException();
		}
		@Override
		public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
			throw new IllegalArgumentException();
		}
		@Override
		public OrdinalSet<K> keySet() {
			return OrdinalSet.emptySet();
		}
		@Override
		public OrdinalCollection<V> values() {
			return OrdinalSet.emptyList();
		}
		@Override
		public int length() {
			return 0;
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
		public boolean containsKey(Object key) {
			return false;
		}
		@Override
		public boolean containsValue(Object value) {
			return false;
		}
		@Override
		public V get(Object key) {
			return null;
		}
		@Override
		public V put(K key, V value) {
			throw new IllegalArgumentException();
		}
		@Override
		public V putIfKey(K key, V value) {
			return null;
		}
		@Override
		public V remove(Object key) {
			return null;
		}
		@Override
		public void putAllKeys(Map<? extends K, ? extends V> m) {}
		@Override
		public void clear() {}
		@Override
		public Set<Entry<K, V>> entrySet() {
			return OrdinalSet.emptySet();
		}
		@Override
		public int hashCode() {
			return 0;
		}
		@Override
		public boolean equals(Object obj) {
			return obj instanceof Map && ((Map<?,?>)obj).isEmpty();
		}
	}
}
