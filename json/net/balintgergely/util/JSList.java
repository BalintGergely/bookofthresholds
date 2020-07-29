package net.balintgergely.util;

import static net.balintgergely.util.JSON.*;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class JSList implements Iterable<Object>{
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
	public <E> E peek(int key,E valueIfAbsent){
		E e = peek(key);
		if(e == null){
			return valueIfAbsent;
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
	public static class ImmutableList<E> extends AbstractList<E>{
		E[] data;
		ImmutableList(E[] data){
			this.data = data;
		}
		@Override
		public int size() {
			return data.length;
		}
		@Override
		public boolean isEmpty() {
			return false;
		}
		@Override
		public boolean contains(Object o) {
			for(E e : data){
				if(o == null ? e == null : o.equals(e)){
					return true;
				}
			}
			return false;
		}
		@Override
		public Iterator<E> iterator() {
			return new ArrayIterator<>(data);
		}
		@Override
		public Object[] toArray() {
			Object[] c = new Object[data.length];
			System.arraycopy(data, 0, c, 0, c.length);
			return c;
		}
		@Override
		@SuppressWarnings("unchecked")
		public <T> T[] toArray(T[] a) {
			if(a.length < data.length){
				a = (T[])Array.newInstance(a.getClass().getComponentType(), data.length);
			}else if(a.length > data.length){
				a[data.length] = null;
			}
			System.arraycopy(data, 0, a, 0, data.length);
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
		public Spliterator<E> spliterator() {
			return new ArrayIterator<>(data);
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
			while(i < data.length){
				if(o == null ? data[i] == null : o.equals(data[i])){
					return i;
				}
				i++;
			}
			return 0;
		}
		@Override
		public int lastIndexOf(Object o) {
			int i = data.length;
			while(i > 0){
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
		public ListIterator<E> listIterator() {
			return new ArrayIterator<>(data);
		}
		@Override
		public ListIterator<E> listIterator(int index) {
			return new ArrayIterator<>(data,index,data.length);
		}
		@Override
		public boolean equals(Object o) {
			if(this == o){
				return true;
			}
			if(o instanceof ImmutableList){
				Object[] od = ((ImmutableList<?>)o).data;
				if(od.length != data.length){
					return false;
				}
				for(int i = 0;i < od.length;i++){
					Object x = data[i];
					if(x == null ? od[i] != null : !x.equals(od[i])){
						return false;
					}
				}
				return true;
			}
			return false;
		}
		@Override
		public int hashCode() {
			return Arrays.hashCode(data);
		}
		@Override
		protected void removeRange(int fromIndex, int toIndex) {
			clear();
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
			return new ImmutableList<>(Arrays.copyOfRange(data, fromIndex, toIndex));
		}
	}
	public static class ImmutableSet<E> extends ImmutableList<E> implements Set<E>{
		ImmutableSet(E[] data){
			super(data);
		}
		@Override
		public boolean equals(Object that){
			return that instanceof ImmutableSet && super.equals(that);
		}
	}
}