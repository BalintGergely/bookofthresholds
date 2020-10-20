package net.balintgergely.util;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;
import java.util.SortedMap;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

import net.balintgergely.util.NavigableList.BinarySearchList;

public class SimpleOrdinalMap<K,V> implements OrdinalMap<K,V>,NavigableMap<K,V>{
	OrdinalSet<K> keySet;
	ValueHolder values;
	@SuppressWarnings("unchecked")
	public SimpleOrdinalMap(SortedMap<K,V> entries,Class<K> keyClass){
		NavigableList<K> universe;
		Comparator<? super K> comparator = entries.comparator();
		if(keyClass == String.class && (comparator == null || comparator.equals(Comparator.naturalOrder()))){
			universe = (NavigableList<K>)new NavigableList.StringList((Set<String>)entries.keySet());
		}else{
			universe = new BinarySearchList<>(entries.keySet(),comparator,keyClass);
		}
		this.keySet = new SimpleOrdinalSet<>(universe);
		this.values = new ValueHolder();
		values.data = (V[])new Object[keySet.length()];
		for(int i = 0;i < values.data.length;i++){
			values.data[i] = entries.get(universe.get(i));
		}
	}
	@SuppressWarnings("unchecked")
	public SimpleOrdinalMap(OrdinalSet<K> keySet){
		this.keySet = new SimpleOrdinalSet<>(keySet);
		this.values = new ValueHolder();
		values.data = (V[])new Object[keySet.length()];
	}
	private SimpleOrdinalMap(OrdinalSet<K> keySet,V[] data,int fromIndex){
		this.keySet = keySet;
		this.values = new ValueHolder();
		values.data = data;
		values.off = fromIndex;
	}
	@Override
	public Comparator<? super K> comparator() {
		return keySet.comparator();
	}
	@Override
	public K firstKey() {
		return keySet.first();
	}
	@Override
	public K lastKey() {
		return keySet.last();
	}
	@Override
	public OrdinalSet<K> keySet() {
		return keySet;
	}
	@Override
	public NavigableSet<K> navigableKeySet() {
		return keySet;
	}
	@Override
	public OrdinalCollection<V> values() {
		return values;
	}
	@Override
	public int size() {
		return keySet.size();
	}
	@Override
	public boolean isEmpty() {
		return keySet.isEmpty();
	}
	@Override
	public boolean containsKey(Object key) {
		return keySet.contains(key);
	}
	@Override
	public boolean containsValue(Object value) {
		return values.contains(value);
	}
	@Override
	public V get(Object key) {
		int index = keySet.indexOf(key);
		return index >= 0 ? values.get(index) : null;
	}
	@Override
	public V put(K key, V value) {
		int index = keySet.indexOf(key);
		if(index >= 0){
			if(keySet.add(index)){
				values.set(index, value);
				return null;
			}else{
				return values.set(index, value);
			}
		}
		throw new IllegalArgumentException();
	}
	@Override
	public V remove(Object key) {
		int index = keySet.indexOf(key);
		if(index >= 0 && keySet.contains(index)){
			keySet.remove(index);
			return values.set(index, null);
		}
		return null;
	}
	@Override
	public void clear() {
		keySet.clear();
		Arrays.fill(values.data, values.off, values.off+keySet.length(), null);
	}
	public Entry<K, V> seekEntry(K key,boolean lower,boolean inclusive){
		int index = keySet.seek(key, lower, inclusive);
		return index >= 0 ? new MapEntry.SemiMutable<>(keySet.get(index), values.get(index), this) : null;
	}
	@Override
	public Entry<K, V> lowerEntry(K key) {
		return seekEntry(key, true, false);
	}
	@Override
	public K lowerKey(K key) {
		return keySet.lower(key);
	}
	@Override
	public Entry<K, V> floorEntry(K key) {
		return seekEntry(key, true, true);
	}
	@Override
	public K floorKey(K key) {
		return keySet.floor(key);
	}
	@Override
	public Entry<K, V> ceilingEntry(K key) {
		return seekEntry(key, false, true);
	}
	@Override
	public K ceilingKey(K key) {
		return keySet.ceiling(key);
	}
	@Override
	public Entry<K, V> higherEntry(K key) {
		return seekEntry(key, false, false);
	}
	@Override
	public K higherKey(K key) {
		return keySet.higher(key);
	}
	@Override
	public Entry<K, V> firstEntry() {
		int index = keySet.firstIndex();
		return index >= 0 ? new MapEntry.SemiMutable<>(keySet.get(index), values.get(index), this) : null;
	}
	@Override
	public Entry<K, V> lastEntry() {
		int index = keySet.lastIndex();
		return index >= 0 ? new MapEntry.SemiMutable<>(keySet.get(index), values.get(index), this) : null;
	}
	@Override
	public Entry<K, V> pollFirstEntry() {
		int index = keySet.firstIndex();
		if(index >= 0){
			keySet.remove(index);
			return new MapEntry<>(keySet.get(index), values.set(index,null));
		}
		return null;
	}
	@Override
	public Entry<K, V> pollLastEntry() {
		int index = keySet.lastIndex();
		if(index >= 0){
			keySet.remove(index);
			return new MapEntry<>(keySet.get(index), values.set(index,null));
		}
		return null;
	}
	@Override
	public NavigableMap<K, V> descendingMap() {
		return Mirror.mirror(this);
	}
	@Override
	public NavigableSet<K> descendingKeySet() {
		return keySet.descendingSet();
	}
	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		OrdinalSet<K> subSet = keySet.subSet(fromKey, fromInclusive, toKey, toInclusive);
		if(subSet.length() == 0){
			return Collections.emptyNavigableMap();
		}
		return new SimpleOrdinalMap<>(subSet, values.data, values.off+subSet.offset()-keySet.offset());
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
	public void putAll(Map<? extends K, ? extends V> m) {
		if(m instanceof OrdinalMap){
			OrdinalMap<? extends K,? extends V> ordM = (OrdinalMap<? extends K,? extends V>)m;
			OrdinalSet<? extends K> otherKeySet = ordM.keySet();
			int myFirstIndex = keySet.offset();
			int myLastIndex = myFirstIndex+keySet.length();
			int otFirstIndex = otherKeySet.offset();
			int otLastIndex = otFirstIndex+otherKeySet.length();
			if(myFirstIndex <= otFirstIndex && myLastIndex >= otLastIndex && keySet.universe() == otherKeySet.universe()){
				int lastIndex = Math.min(myLastIndex, otLastIndex);
				for(int index = Math.max(myFirstIndex, otFirstIndex);index < lastIndex;index++){
					int ox = index-otFirstIndex;
					if(ordM.contains(ox)){
						int lx = index-myFirstIndex;
						keySet.add(lx);
						values.set(lx,ordM.values().get(ox));
					}
				}
				return;
			}
		}
		for(Map.Entry<? extends K, ? extends V> e : m.entrySet()){
			put(e.getKey(),e.getValue());
		}
	}
	@Override
	public V getOrDefault(Object key, V defaultValue) {
		int index = keySet.indexOf(key);
		if(index >= 0 && keySet.contains(index)){
			return values.get(index);
		}
		return defaultValue;
	}
	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		PrimitiveIterator.OfInt indices = keySet.indexIterator();
		while(indices.hasNext()){
			int index = indices.nextInt();
			action.accept(keySet.get(index), values.get(index));
		}
	}
	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		PrimitiveIterator.OfInt indices = keySet.indexIterator();
		while(indices.hasNext()){
			int index = indices.nextInt();
			values.set(index, function.apply(keySet.get(index), values.get(index)));
		}
	}
	@Override
	public V putIfAbsent(K key, V value) {
		int index = keySet.indexOf(key);
		if(index >= 0){
			if(keySet.add(index)){
				values.set(index, value);
				return null;
			}else{
				return values.get(index);
			}
		}
		throw new IllegalArgumentException();
	}
	@Override
	public boolean remove(Object key, Object value) {
		int index = keySet.indexOf(key);
		if(index >= 0 && keySet.contains(index)){
			V v = values.get(index);
			if(value == null ? v == null : value.equals(v)){
				keySet.remove(index);
				values.set(index, null);
				return true;
			}
		}
		return false;
	}
	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		int index = keySet.indexOf(key);
		if(index >= 0 && keySet.contains(index)){
			V v = values.get(index);
			if(oldValue == null ? v == null : oldValue.equals(v)){
				values.set(index, newValue);
				return true;
			}
		}
		return false;
	}
	@Override
	public V replace(K key, V value) {
		int index = keySet.indexOf(key);
		if(index >= 0 && keySet.contains(index)){
			return values.set(index, value);
		}
		return null;
	}
	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		int index = keySet.indexOf(key);
		if(index >= 0){
			if(keySet.contains(index)){
				return values.get(index);
			}else{
				V v = mappingFunction.apply(key);
				if(v != null){
					keySet.add(index);
					values.set(index, v);
				}
				return v;
			}
		}
		throw new IllegalArgumentException();
	}
	@Override
	public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		int index = keySet.indexOf(key);
		if(index >= 0 && keySet.contains(index)){
			V v = values.get(index);
			V n = remappingFunction.apply(key, v);
			if(n == null){
				keySet.remove(index);
			}
			values.set(index, n);
			return n;
		}
		return null;
	}
	@Override
	public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		int index = keySet.indexOf(key);
		if(index >= 0){
			if(keySet.contains(index)){
				V v = values.get(index);
				V n = remappingFunction.apply(key, v);
				if(n == null){
					keySet.remove(index);
				}
				values.set(index, n);
				return n;
			}else{
				V v = remappingFunction.apply(key, null);
				if(v != null){
					keySet.add(index);
					values.set(index, v);
				}
				return v;
			}
		}
		throw new IllegalArgumentException();
	}
	@Override
	public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		int index = keySet.indexOf(key);
		if(index >= 0){
			a: if(keySet.contains(index)){
				V v = values.get(index);
				if(v == null){
					break a;
				}
				V n = remappingFunction.apply(v, value);
				if(n == null){
					keySet.remove(index);
				}
				values.set(index, n);
				return n;
			}
			keySet.add(index);
			values.set(index, value);
			return value;
		}
		throw new IllegalArgumentException();
	}
	private class ValueHolder extends AbstractCollection<V> implements OrdinalCollection<V>{
		V[] data;
		int off = 0;
		@Override
		public int size() {
			return keySet.size();
		}
		@Override
		public boolean isEmpty() {
			return keySet.isEmpty();
		}
		@Override
		public boolean contains(Object o) {
			PrimitiveIterator.OfInt indices = keySet.indexIterator();
			if(o == null){
				while(indices.hasNext()){
					if(data[off+indices.nextInt()] == null){
						return true;
					}
				}
			}else{
				while(indices.hasNext()){
					if(o.equals(data[off+indices.nextInt()])){
						return true;
					}
				}
			}
			return false;
		}
		@Override
		public Iterator<V> iterator() {
			PrimitiveIterator.OfInt indices = keySet.indexIterator();
			return new Iterator<V>() {
				@Override
				public boolean hasNext() {
					return indices.hasNext();
				}
				@Override
				public V next() {
					return data[off+indices.nextInt()];
				}
			};
		}
		@Override
		public Spliterator<V> spliterator() {
			class ValueSpliterator implements Spliterator<V>,IntConsumer{
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
				public boolean tryAdvance(Consumer<? super V> action) {
					if(indices.tryAdvance(this)){
						action.accept(data[off+index]);
						return true;
					}
					return false;
				}
				@Override
				public Spliterator<V> trySplit() {
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
			return new ValueSpliterator(keySet.indexSpliterator());
		}
		@Override
		public boolean remove(Object o) {
			PrimitiveIterator.OfInt indices = keySet.indexIterator();
			if(o == null){
				while(indices.hasNext()){
					if(data[off+indices.nextInt()] == null){
						indices.remove();
						return true;
					}
				}
			}else{
				while(indices.hasNext()){
					if(o.equals(data[off+indices.nextInt()])){
						indices.remove();
						return true;
					}
				}
			}
			return false;
		}
		@Override
		public boolean removeAll(Collection<?> c) {
			return removeImpl(c,true);
		}
		@Override
		public boolean retainAll(Collection<?> c) {
			return removeImpl(c,false);
		}
		private boolean removeImpl(Collection<?> c,boolean complement){
			boolean mod = false;
			PrimitiveIterator.OfInt indices = keySet.indexIterator();
			while(indices.hasNext()){
				if(c.contains(data[off+indices.nextInt()]) == complement){
					indices.remove();
					mod = true;
				}
			}
			return mod;
		}
		@Override
		public boolean removeIf(Predicate<? super V> p){
			boolean mod = false;
			PrimitiveIterator.OfInt indices = keySet.indexIterator();
			while(indices.hasNext()){
				if(p.test(data[off+indices.nextInt()])){
					indices.remove();
					mod = true;
				}
			}
			return mod;
		}
		@Override
		public void clear() {
			SimpleOrdinalMap.this.clear();
		}
		@Override
		public void forEach(Consumer<? super V> action) {
			PrimitiveIterator.OfInt indices = keySet.indexIterator();
			while(indices.hasNext()){
				action.accept(data[off+indices.nextInt()]);
			}
		}
		@Override
		public V get(int index){
			return data[off+index];
		}
		private V set(int index,V value){
			index += off;
			V v = data[index];
			data[index] = value;
			return v;
		}
		@Override
		public OfInt indexIterator() {
			return keySet.indexIterator();
		}
		@Override
		public java.util.Spliterator.OfInt indexSpliterator() {
			return keySet.indexSpliterator();
		}
		@Override
		public int firstIndex() {
			return keySet.firstIndex();
		}
		@Override
		public int lastIndex() {
			return keySet.lastIndex();
		}
		@Override
		public boolean contains(int index) {
			return keySet.contains(index);
		}
		@Override
		public boolean add(int index) {
			throw new UnsupportedOperationException();
		}
		@Override
		public int length() {
			return keySet.length();
		}
		@Override
		public Object remove(int index) {
			if(keySet.contains(index)){
				keySet.remove(index);
				index += off;
				Object obj = data[index];
				data[index] = null;
				return obj;
			}
			return null;
		}
	}
	@Override
	public Set<Entry<K, V>> entrySet() {
		return new AbstractSet<Entry<K, V>>(){
			@Override
			public int size() {
				return keySet.size();
			}
			@Override
			public boolean isEmpty() {
				return keySet.isEmpty();
			}
			@Override
			public void clear() {
				SimpleOrdinalMap.this.clear();
			}
			@Override
			public Iterator<Entry<K, V>> iterator() {
				PrimitiveIterator.OfInt indices = keySet.indexIterator();
				return new Iterator<Map.Entry<K,V>>() {
					int index = -1;
					@Override
					public boolean hasNext() {
						return indices.hasNext();
					}
					@Override
					public Entry<K, V> next() {
						index = indices.nextInt();
						return new MapEntry.SemiMutable<>(keySet.get(index), values.get(index), SimpleOrdinalMap.this);
					}
					@Override
					public void remove() {
						indices.remove();
						values.set(index, null);
					}
				};
			}
			@Override
			public Spliterator<Entry<K, V>> spliterator() {
				class EntrySpliterator implements Spliterator<Map.Entry<K, V>>,IntConsumer{
					Spliterator.OfInt indices;
					EntrySpliterator(Spliterator.OfInt in){
						this.indices = in;
					}
					@Override
					public void accept(int value) {
						index = value;
					}
					int index;
					@Override
					public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> action) {
						if(indices.tryAdvance(this)){
							action.accept(new MapEntry.SemiMutable<>(keySet.get(index), values.get(index), SimpleOrdinalMap.this));
							return true;
						}
						return false;
					}
					@Override
					public Spliterator<Map.Entry<K, V>> trySplit() {
						Spliterator.OfInt spl = indices.trySplit();
						return spl == null ? null : new EntrySpliterator(spl);
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
					@Override
					public Comparator<? super Entry<K, V>> getComparator() {
						return Comparator.comparing(Map.Entry::getKey, keySet.comparator());
					}
				}
				return new EntrySpliterator(keySet.indexSpliterator());
			}
		};
	}
}
