package net.balintgergely.util;

import static net.balintgergely.util.JSON.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class JSList implements Iterable<Object>{
	public static final JSList EMPTY_LIST = new JSList(List.of());
	public final List<Object> list;
	public JSList(){
		list = new ArrayList<Object>();
	}
	public JSList(int i){
		list = new ArrayList<Object>(i);
	}
	public JSList(List<Object> list){
		this.list = list;
	}
	/*private JSList(CharBuffer input){
		this();
		skipWhitespace(input);
		if(input.get(input.position()) == ']'){
			input.get();
			return;
		}
		while(true){
			list.add(parseValue(input));
			skipWhitespace(input);
			switch(input.get()){
			case ',':
				break;
			case ']':
				return;
			default:ex();
			}
			skipWhitespace(input);
		}
	}*/
	public int size(){
		return list.size();
	}
	public boolean isEmpty(){
		return list.isEmpty();
	}
	@Override
	public Iterator<Object> iterator() {
		return list.iterator();
	}
	@Override
	public String toString(){
		return quote(list);
	}
	@Override
	public int hashCode(){
		return list.hashCode();
	}
	@Override
	public boolean equals(Object that){
		if(that instanceof JSList){
			List<Object> l = ((JSList)that).list;
			return list == l || list.equals(l);
		}
		return false;
	}
	public JSList add(Object value){
		list.add(wrap(value));
		return this;
	}
	public JSList add(int key,Object value){
		list.add(key, wrap(value));
		return this;
	}
	public JSList add(Object... values){
		if(list instanceof ArrayList){
			((ArrayList<?>)list).ensureCapacity(list.size()+values.length);
		}
		for(Object obj : values){
			list.add(wrap(obj));
		}
		return this;
	}
	public JSList set(int key,Object value){
		list.set(key, wrap(value));
		return this;
	}
	public JSList addAll(Iterable<?> values){
		if(values instanceof Collection<?> && list instanceof ArrayList){
			((ArrayList<?>)list).ensureCapacity(list.size()+((Collection<?>)values).size());
		}
		for(Object obj : values){
			list.add(wrap(obj));
		}
		return this;
	}
	public JSList remove(int key){
		if(key >= 0 || key < list.size()){
			try{
				list.remove(key);
			}catch(IndexOutOfBoundsException e){}
		}
		return this;
	}
	@SuppressWarnings("unchecked")
	public <E> E peek(int key){
		if(key < 0 || key >= list.size()){
			return null;
		}
		try{
			return (E)list.get(key);
		}catch(IndexOutOfBoundsException e){
			return null;
		}
	}
	@SuppressWarnings("unchecked")
	public <E> E get(int key){
		return (E)list.get(key);
	}
	public <E> E getDeep(int key,Object... keys){
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
	public <E> E peek(int key,E valueIfAbsent){
		E e = peek(key);
		if(e == null){
			return valueIfAbsent;
		}
		return e;
	}
	public <E> E peekDeep(int key,Object... keys){
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
	public boolean getBoolean(int index){
		return asBoolean(get(index),true,false);
	}
	public boolean peekBoolean(int index){
		return asBoolean(peek(index),false,false);
	}
	public boolean peekBoolean(int index,boolean valueIfAbsent){
		return asBoolean(peek(index), false, valueIfAbsent);
	}
	public byte getByte(int index){
		return asByte(get(index), true, (byte)0);
	}
	public byte peekByte(int index){
		return asByte(peek(index), false, (byte)0);
	}
	public byte peekByte(int index,byte valueIfAbsent){
		return asByte(peek(index), false, valueIfAbsent);
	}
	public short getShort(int index){
		return asShort(get(index), true, (short)0);
	}
	public short peekShort(int index){
		return asShort(peek(index), false, (short)0);
	}
	public short peekShort(int index,short valueIfAbsent){
		return asShort(peek(index), false, valueIfAbsent);
	}
	public int getInt(int index){
		return asInt(get(index), true, 0);
	}
	public int peekInt(int index){
		return asInt(peek(index), false, 0);
	}
	public int peekInt(int index,int valueIfAbsent){
		return asInt(peek(index), false, valueIfAbsent);
	}
	public long getLong(int index){
		return asLong(get(index), true, 0l);
	}
	public long peekLong(int index){
		return asLong(peek(index), false, 0l);
	}
	public long peekLong(int index,long valueIfAbsent){
		return asLong(peek(index), false, valueIfAbsent);
	}
	public float getFloat(int index){
		return asFloat(get(index), true, 0f);
	}
	public float peekFloat(int index){
		return asFloat(peek(index), false, 0f);
	}
	public float peekFloat(int index,float valueIfAbsent){
		return asFloat(peek(index), false, valueIfAbsent);
	}
	public double getDouble(int index){
		return asDouble(get(index), true, 0d);
	}
	public double peekDouble(int index){
		return asDouble(peek(index), false, 0d);
	}
	public double peekDouble(int index,double valueIfAbsent){
		return asDouble(peek(index), false, valueIfAbsent);
	}
	public Number getNumber(int index){
		return asNumber(get(index), true, null);
	}
	public Number peekNumber(int index){
		return asNumber(peek(index), false, null);
	}
	public Number peekNumber(int index,Number valueIfAbsent){
		return asNumber(peek(index), false, valueIfAbsent);
	}
	public BigInteger getBigInteger(int index){
		return asBigInteger(get(index), true, null);
	}
	public BigInteger peekBigInteger(int index){
		return asBigInteger(peek(index), false, null);
	}
	public BigInteger peekBigInteger(int index,BigInteger valueIfAbsent){
		return asBigInteger(peek(index), false, valueIfAbsent);
	}
	public BigDecimal getBigDecimal(int index){
		return asBigDecimal(get(index), true, null);
	}
	public BigDecimal peekBigDecimal(int index){
		return asBigDecimal(peek(index), false, null);
	}
	public BigDecimal peekBigDecimal(int index,BigDecimal valueIfAbsent){
		return asBigDecimal(peek(index), false, valueIfAbsent);
	}
	public String getString(int index){
		return asString(get(index), true, null);
	}
	public String peekString(int index){
		return asString(peek(index), false, null);
	}
	public String peekString(int index,String valueIfAbsent){
		return asString(peek(index), false, valueIfAbsent);
	}
	public <E extends Enum<E>> E getEnum(Class<E> c,int index){
		return asEnum(c,get(index), true, null);
	}
	public <E extends Enum<E>> E peekEnum(Class<E> c,int index){
		return asEnum(c,peek(index), false, null);
	}
	public <E extends Enum<E>> E peekEnum(Class<E> c,int index,E valueIfAbsent){
		return asEnum(c,peek(index), false, valueIfAbsent);
	}
	public JSMap getJSMap(int index){
		return asJSMap(get(index), true);
	}
	public JSMap peekJSMap(int index){
		return asJSMap(peek(index), false);
	}
	public JSList getJSList(int index){
		return asJSList(get(index), true);
	}
	public JSList peekJSList(int index){
		return asJSList(peek(index), false);
	}
}