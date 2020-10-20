package net.balintgergely.util;

import java.util.Map;
import java.util.Objects;

public class MapEntry<K,V> implements Map.Entry<K,V>{
	protected K key;
	protected V value;
	public MapEntry(K key,V value){
		this.key = key;
		this.value = value;
	}
	@Override
	public K getKey() {
		return key;
	}
	@Override
	public V getValue() {
		return value;
	}
	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException();
	}
	@Override
	public int hashCode() {
		return Objects.hashCode(key) ^ Objects.hashCode(value);
	}
	@Override
	public boolean equals(Object o) {
		if(o instanceof Map.Entry){
			Map.Entry<?,?> e = (Map.Entry<?,?>)o;
			return key.equals(e.getKey()) && value.equals(e.getValue());
		}
		return false;
	}
	@Override
	public String toString() {
		return key + "=" + value;
	}
	public static class Mutable<K, V> extends MapEntry<K, V>{
		public Mutable(K key, V value) {
			super(key, value);
		}
		@Override
		public V setValue(V value) {
			V old = this.value;
			this.value = value;
			return old;
		}
	}
	public static class SemiMutable<K, V> extends Mutable<K, V>{
		protected Map<K, V> map;
		public SemiMutable(K key, V value, Map<K, V> map) {
			super(key, value);
			this.map = map;
		}
		@Override
		public V setValue(V value) {
			V old = this.value;
			map.put(key, value);
			this.value = value;
			return old;
		}
	}
	public static class HookEntry<K, V> implements Map.Entry<K, V>{
		protected K key;
		protected Map<K, V> map;
		public HookEntry(K key,Map<K, V> map){
			this.key = key;
			this.map = map;
		}
		@Override
		public K getKey() {
			return key;
		}
		@Override
		public V getValue() {
			return map.get(key);
		}
		@Override
		public V setValue(V value) {
			return map.put(key, value);
		}
	}
}
