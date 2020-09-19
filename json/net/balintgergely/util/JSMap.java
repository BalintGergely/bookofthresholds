package net.balintgergely.util;

import static net.balintgergely.util.JSON.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
/**
 * @author Bálint János Gergely
 */
public final class JSMap{
	public static final JSMap EMPTY_MAP = new JSMap(Collections.emptyNavigableMap());
	public final Map<String,Object> map;
	public JSMap() {
		map = new HashMap<String, Object>();
	}
	public JSMap(Map<String,Object> map){
		this.map = Objects.requireNonNull(map);
	}
	public int size(){
		return map.size();
	}
	public boolean isEmpty(){
		return map.isEmpty();
	}
	@Override
	public String toString(){
		return quote(map);
	}
	@Override
	public int hashCode(){
		return map.hashCode();
	}
	@Override
	public boolean equals(Object that){
		if(that instanceof JSMap){
			Map<?,?> m = ((JSMap)that).map;
			return map == m || map.equals(m);
		}
		return false;
	}
	public JSMap clear(){
		map.clear();
		return this;
	}
	public JSMap put(String key,Object value){
		map.put(String.valueOf(key), wrap(value));
		return this;
	}
	public JSMap put(Object... keyAndValuePairs){
		for(int i = 0;i < keyAndValuePairs.length;i += 2){
			map.put(String.valueOf(keyAndValuePairs[i]), wrap(keyAndValuePairs[i+1]));
		}
		return this;
	}
	public JSMap putIfAbsent(String key,Object value){
		map.computeIfAbsent(key, (String s) -> wrap(value));
		return this;
	}
	public JSMap putAll(Map<?,?> values){
		for(Entry<?,?> e : values.entrySet()){
			map.put(String.valueOf(e.getKey()), wrap(e.getValue()));
		}
		return this;
	}
	public JSMap putAll(JSMap values){
		return putAll(values.map);
	}
	public JSMap remove(Object key){
		map.remove(key);
		return this;
	}
	public JSMap retain(Object... keys){
		map.keySet().retainAll(Arrays.asList(keys));
		return this;
	}
	@SuppressWarnings("unchecked")
	public <E> E peek(Object key){
		return (E)map.get(key);
	}
	public <E> E get(Object key){
		E e = peek(key);
		if(e == null){
			throw new NoSuchElementException();
		}
		return e;
	}
	public <E> E getDeep(Object key,Object... keys){
		E e = peek(key);
		for(Object o : keys){
			if(e instanceof JSList){
				e = ((JSList)e).get(JSON.asInt(o, true, -1));
			}else if(e instanceof JSMap){
				e = ((JSMap)e).get(JSON.asString(o, true, null));
			}else{
				throw new NoSuchElementException();
			}
		}
		return e;
	}
	public <E> E peek(Object key,E valueIfAbsent){
		E e = peek(key);
		if(e == null){
			return valueIfAbsent;
		}
		return e;
	}
	public <E> E peekDeep(Object key,Object... keys){
		E e = peek(key);
		for(Object o : keys){
			if(e == null){
				return null;
			}else if(e instanceof JSList){
				e = ((JSList)e).peek(JSON.asInt(o, false, -1));
			}else if(e instanceof JSMap){
				String str = JSON.asString(0, false, null);
				e = str == null ? null : ((JSMap)e).peek(str);
			}else{
				return null;
			}
		}
		return e;
	}
	//Compatibility: Boolean,Byte,Short,Int,Long,Float,Double,Number,BigInteger,BigDecimal,String,Enum
	public boolean getBoolean(Object key){
		return asBoolean(map.get(key),true,false);
	}
	public boolean peekBoolean(Object key){
		return asBoolean(map.get(key),false,false);
	}
	public boolean peekBoolean(Object key,boolean valueIfAbsent){
		return asBoolean(map.get(key), false, valueIfAbsent);
	}
	public byte getByte(Object key){
		return asByte(map.get(key), true, (byte)0);
	}
	public byte peekByte(Object key){
		return asByte(map.get(key), false, (byte)0);
	}
	public byte peekByte(Object key,byte valueIfAbsent){
		return asByte(map.get(key), false, valueIfAbsent);
	}
	public short getShort(Object key){
		return asShort(map.get(key), true, (short)0);
	}
	public short peekShort(Object key){
		return asShort(map.get(key), false, (short)0);
	}
	public short peekShort(Object key,short valueIfAbsent){
		return asShort(map.get(key), false, valueIfAbsent);
	}
	public int getInt(Object key){
		return asInt(map.get(key), true, 0);
	}
	public int peekInt(Object key){
		return asInt(map.get(key), false, 0);
	}
	public int peekInt(Object key,int valueIfAbsent){
		return asInt(map.get(key), false, valueIfAbsent);
	}
	public long getLong(Object key){
		return asLong(map.get(key), true, 0l);
	}
	public long peekLong(Object key){
		return asLong(map.get(key), false, 0l);
	}
	public long peekLong(Object key,long valueIfAbsent){
		return asLong(map.get(key), false, valueIfAbsent);
	}
	public float getFloat(Object key){
		return asFloat(map.get(key), true, 0f);
	}
	public float peekFloat(Object key){
		return asFloat(map.get(key), false, 0f);
	}
	public float peekFloat(Object key,float valueIfAbsent){
		return asFloat(map.get(key), false, valueIfAbsent);
	}
	public double getDouble(Object key){
		return asDouble(map.get(key), true, 0d);
	}
	public double peekDouble(Object key){
		return asDouble(map.get(key), false, 0d);
	}
	public double peekDouble(Object key,double valueIfAbsent){
		return asDouble(map.get(key), false, valueIfAbsent);
	}
	public Number getNumber(Object key){
		return asNumber(map.get(key), true, null);
	}
	public Number peekNumber(Object key){
		return asNumber(map.get(key), false, null);
	}
	public Number peekNumber(Object key,Number valueIfAbsent){
		return asNumber(map.get(key), false, valueIfAbsent);
	}
	public BigInteger getBigInteger(Object key){
		return asBigInteger(map.get(key), true, null);
	}
	public BigInteger peekBigInteger(Object key){
		return asBigInteger(map.get(key), false, null);
	}
	public BigInteger peekBigInteger(Object key,BigInteger valueIfAbsent){
		return asBigInteger(map.get(key), false, valueIfAbsent);
	}
	public BigDecimal getBigDecimal(Object key){
		return asBigDecimal(map.get(key), true, null);
	}
	public BigDecimal peekBigDecimal(Object key){
		return asBigDecimal(map.get(key), false, null);
	}
	public BigDecimal peekBigDecimal(Object key,BigDecimal valueIfAbsent){
		return asBigDecimal(map.get(key), false, valueIfAbsent);
	}
	public String getString(Object key){
		return asString(map.get(key), true, null);
	}
	public String peekString(Object key){
		return asString(map.get(key), false, null);
	}
	public String peekString(Object key,String valueIfAbsent){
		return asString(map.get(key), false, valueIfAbsent);
	}
	public <E extends Enum<E>> E getEnum(Class<E> c,Object key){
		return asEnum(c,map.get(key), true, null);
	}
	public <E extends Enum<E>> E peekEnum(Class<E> c,Object key){
		return asEnum(c,map.get(key), false, null);
	}
	public <E extends Enum<E>> E peekEnum(Class<E> c,Object key,E valueIfAbsent){
		return asEnum(c,map.get(key), false, valueIfAbsent);
	}
	public JSMap getJSMap(Object key){
		return asJSMap(map.get(key), true);
	}
	public JSMap peekJSMap(Object key){
		return asJSMap(map.get(key), false);
	}
	public JSList getJSList(Object key){
		return asJSList(map.get(key), true);
	}
	public JSList peekJSList(Object key){
		return asJSList(map.get(key), false);
	}
}
