package net.balintgergely.util;

import java.io.EOFException;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.balintgergely.util.JSMap.ImmutableMap;
import net.balintgergely.util.JSList.ImmutableList;
import net.balintgergely.util.JSList.ImmutableSet;

public final class JSON {
	public static void main(String[] atgs){
		System.out.println(Map.entry("JS", "ON").getClass() == immMapEntry);
		System.out.println("JSON");
	}
	public static final Class<?> immMapEntry;
	static {
		Class<?> m = null;
		try{
			m = Class.forName(Map.class.getModule(), "java.util.KeyValueHolder");
		}catch(Throwable t){}
		if(m == null){
			m = Map.entry("", "").getClass();
		}
		immMapEntry = m;
	}
	private int flag = -2;
	private JSON(){}
	/**
	 * Skips whitespace and returns the first non-whitespace character it encounters.
	 */
	private static char skipWhitespace(Reader r) throws IOException{
		int c;
		do{
			c = r.read();
			if(c < 0){
				eofex();
			}
		}while(Character.isWhitespace((char)c));
		return (char)c;
	}
	/**
	 * Reads as much as the string parameter and throws ioex if the input does not match it.
	 */
	private static void x(String s,Reader b) throws IOException{
		int i = 0;
		while(i < s.length()){
			if(b.read() != s.charAt(i)){
				ioex();
			}
			i++;
		}
	}
	private static void ioex() throws IOException{
		throw new IOException();
	}
	private static void eofex() throws IOException{
		throw new EOFException();
	}
	/**
	 * Parses the specified String into a value. May not use up the whole string.
	 */
	public static Object parse(String input){
		try{
			Object obj = readObject(new StringReader(input),null);
			if(obj instanceof StringReader){
				throw new IllegalArgumentException();
			}
			return obj;
		}catch(IOException e){
			throw new IllegalArgumentException(e);
		}
	}
	public static Object readObject(Reader input) throws IOException{
		return readObject(input,null);
	}
	private static Object readObject(Reader input,JSON carrier) throws IOException{
		if(carrier != null){
			carrier.flag = -2;
		}
		char c = skipWhitespace(input);
		switch(c){
		case ']':
			return input;
		case '\"':
			return readString(input, '\"');
		case '\'':
			return readString(input, '\'');
		case '{':
			HashMap<String,Object> map = new HashMap<>();
			readMap(map, input, carrier == null ? new JSON() : carrier);
			return new JSMap(map);
		case '[':
			ArrayList<Object> list = new ArrayList<>();
			readList(list, input, carrier == null ? new JSON() : carrier);
			return new JSList(list);
		case 't':x("rue",input);
			return Boolean.TRUE;
		case 'f':x("alse",input);
			return Boolean.FALSE;
		case 'n':x("ull",input);
			return null;
		default:
			if("+-0123456789.eE".indexOf(c) < 0){
				ioex();
			}
			StringBuilder builder = new StringBuilder();
			builder.append(c);
			int v;
			do{
				v = input.read();
				if(v < 0){
					if(carrier == null){
						break;
					}
					eofex();
				}
				if("+-0123456789.eE".indexOf(v) >= 0){
					builder.append((char)v);
				}else{
					if(carrier != null && !Character.isWhitespace(v)){
						carrier.flag = v;
					}
					break;
				}
			}while(true);
			try{
				Number n = parseNumber(builder.toString());
				return n;
			}catch(NumberFormatException e){
				throw new IOException(e);
			}
		}
	}
	private static String readString(Reader input,char quote) throws IOException{
		int c;
		StringBuilder sb = new StringBuilder();
		char[] nexts = null;
		while(true){
			switch(c = input.read()){
			case '\\':
				switch(c = input.read()){
				case 'b':
					sb.append('\b');
					break;
				case 'f':
					sb.append('\f');
					break;
				case 'n':
					sb.append('\n');
					break;
				case 'r':
					sb.append('\r');
					break;
				case 't':
					sb.append('\t');
					break;
				case 'u':
					if(nexts == null){
						nexts = new char[4];
					}
					int v = 0;
					while(v < 4){
						int r = input.read(nexts,v,4-v);
						if(r < 0){
							eofex();
						}
						v += r;
					}
					sb.append((char)Integer.parseUnsignedInt(new String(nexts), 16));
					break;
				default:
					if(c < 0){
						eofex();
					}
					sb.append((char)c);
				}
				break;
			default:
				if(c < 0){
					eofex();
				}
				if(c == quote){
					return sb.toString();
				}
				sb.append((char)c);
			}
		}
	}
	public static void readMap(Map<String,Object> map,Reader input) throws IOException{
		if(skipWhitespace(input) != '{'){
			throw new IOException();
		}
		readMap(map,input,new JSON());
	}
	private static void readMap(Map<String,Object> map,Reader input,JSON carrier) throws IOException{
		main: while(true){
			String key = null;
			switch(skipWhitespace(input)){
			case '}':
				break main;
			case '\"':
				key = readString(input, '\"');
				break;
			case '\'':
				key = readString(input, '\'');
				break;
			default:ioex();
			}
			if(skipWhitespace(input) != ':'){
				ioex();
			}
			Object value = readObject(input,carrier);
			if(value == input){
				throw new IOException();
			}
			map.put(key, value);
			switch(carrier.flag == -2 ? skipWhitespace(input) : carrier.flag){
			case ',':
				break;
			case '}':
				break main;
			default:ioex();
			}
		}
		carrier.flag = -2;
	}
	public static void readList(Collection<Object> list,Reader input) throws IOException{
		if(skipWhitespace(input) != '['){
			throw new IOException();
		}
		readList(list,input,new JSON());
	}
	private static void readList(Collection<Object> list,Reader input,JSON carrier) throws IOException{
		Object value;
		main: while((value = readObject(input,carrier)) != input){
			list.add(value);
			switch(carrier.flag == -2 ? skipWhitespace(input) : carrier.flag){
			case ',':
				break;
			case ']':
				break main;
			default:ioex();
			}
		}
		carrier.flag = -2;
	}
	/**
	 * Produces a quote of the specified object. The return value is compatible with <code>parseValue(String)</code>.
	 */
	public static String quote(Object obj){
		if(obj == null){
			return "null";
		}
		StringBuilder b = new StringBuilder();
		try{
			write(obj,b);
		}catch(IOException e){
			throw new IllegalArgumentException(e);
		}
		return b.toString();
	}
	public static void write(Object obj,Appendable output) throws IOException{
		if(obj instanceof JSMap){
			write(((JSMap)obj).map,output);
		}else if(obj instanceof JSList){
			write(((JSList)obj).list,output);
		}else if(obj instanceof Number || obj instanceof Boolean){
			output.append(obj.toString());
		}else if(obj == null){
			output.append("null");
		}else if(obj instanceof Enum){
			write(((Enum<?>)obj).name(),output);
		}else if(obj instanceof Map){
			write((Map<?,?>)obj,output);
		}else if(obj instanceof Iterable){
			write((Iterable<?>)obj,output);
		}else{
			write(obj.toString(),output);
		}
	}
	public static void write(String str,Appendable output) throws IOException{
		if(str == null){
			output.append("null");
		}else{
			output.append('"');
			int l = str.length();
			for(int i = 0;i < l;i++){//a
				char c = str.charAt(i);
				switch(c) {
				case '\\':
				case '"':output.append('\\');break;//goto b
				case '\b':output.append("\\b");continue;//goto a
				case '\f':output.append("\\f");continue;//goto a
				case '\n':output.append("\\n");continue;//goto a
				case '\r':output.append("\\r");continue;//goto a
				case '\t':output.append("\\t");continue;//goto a
				}//b
				output.append(c);
			}
			output.append('"');
		}
	}
	public static void write(Map<?,?> map,Appendable b) throws IOException{
		if(map == null){
			b.append("null");
		}else{
			b.append('{');
			boolean f = false;
			for(Entry<?,?> e : map.entrySet()){
				Object k = e.getKey();
				if(k instanceof String || ((k = k.toString()) != null)){
					if(f){
						b.append(',');
					}
					write((String)k,b);
					b.append(':');
					write(e.getValue(),b);
					f = true;
				}
			}
			b.append('}');
		}
	}
	public static void write(Iterable<?> list,Appendable b) throws IOException{
		if(list == null){
			b.append("null");
		}else{
			b.append('[');
			boolean f = false;
			for(Object obj : list){
				if(f){
					b.append(',');
				}
				write(obj,b);
				f = true;
			}
			b.append(']');
		}
	}
	public static boolean asBoolean(Object obj,boolean e,boolean v){
		if(obj != null){
			if(obj instanceof Boolean){
				return Boolean.TRUE.equals(obj);
			}else if(obj instanceof AtomicBoolean){
				return ((AtomicBoolean) obj).get();
			}else if(obj instanceof Byte){
				return ((Byte)obj).byteValue() != 0;
			}
			String str = obj.toString();
			if("true".equalsIgnoreCase(str)){
				return true;
			}
			if("false".equalsIgnoreCase(str)){
				return false;
			}
		}
		if(e){
			throw new NoSuchElementException();
		}
		return v;
	}
	public static Number asNumber(Object obj,boolean e,Number v){
		if(obj != null){
			if(obj instanceof Number){
				return (Number)obj;
			}
			if(obj instanceof Enum){
				return wrap(((Enum<?>)obj).ordinal());
			}else if(e){
				return parseNumber(obj.toString());
			}else try{
				return parseNumber(obj.toString());
			}catch(Exception ex){}
		}
		if(e){
			throw new NoSuchElementException();
		}
		return v;
	}
	public static byte asByte(Object obj,boolean e,byte v){
		if(obj != null){
			if(obj instanceof Number){
				return ((Number) obj).byteValue();
			}
			if(obj instanceof Enum){
				int o = ((Enum<?>)obj).ordinal();
				if(o <= 0xff){
					return (byte)o;
				}
			}else if(e){
				return Byte.parseByte(obj.toString());
			}else try{
				return Byte.parseByte(obj.toString());
			}catch(Exception ex){}
		}
		if(e){
			throw new NoSuchElementException();
		}
		return v;
	}
	public static short asShort(Object obj,boolean e,short v){
		if(obj != null){
			if(obj instanceof Number){
				return ((Number) obj).shortValue();
			}else if(obj instanceof Enum){
				int o = ((Enum<?>)obj).ordinal();
				if(o <= 0xffff){
					return (short)o;
				}
			}else if(e){
				return Short.parseShort(obj.toString());
			}else try{
				return Short.parseShort(obj.toString());
			}catch(Exception ex){}
		}
		if(e){
			throw new NoSuchElementException();
		}
		return v;
	}
	public static int asInt(Object obj,boolean e,int v){
		if(obj != null){
			if(obj instanceof Number){
				return ((Number) obj).intValue();
			}
			if(obj instanceof Enum){
				return ((Enum<?>)obj).ordinal();
			}else if(e){
				return Integer.parseInt(obj.toString());
			}else try{
				return Integer.parseInt(obj.toString());
			}catch(Exception ex){}
		}
		if(e){
			throw new NoSuchElementException();
		}
		return v;
	}
	public static long asLong(Object obj,boolean e,long v){
		if(obj != null){
			if(obj instanceof Number){
				return ((Number) obj).longValue();
			}
			if(obj instanceof Enum){
				return ((Enum<?>)obj).ordinal();
			}else if(e){
				return Long.parseLong(obj.toString());
			}else try{
				return Long.parseLong(obj.toString());
			}catch(Exception ex){}
		}
		if(e){
			throw new NoSuchElementException();
		}
		return v;
	}
	public static float asFloat(Object obj,boolean e,float v){
		if(obj != null){
			if(obj instanceof Number){
				return ((Number) obj).floatValue();
			}
			if(obj instanceof Enum){
				return ((Enum<?>)obj).ordinal();
			}else if(e){
				return Float.parseFloat(obj.toString());
			}else try{
				return Float.parseFloat(obj.toString());
			}catch(Exception ex){}
		}
		if(e){
			throw new NoSuchElementException();
		}
		return v;
	}
	public static double asDouble(Object obj,boolean e,double v){
		if(obj != null){
			if(obj instanceof Number){
				return ((Number) obj).doubleValue();
			}
			if(obj instanceof Enum){
				return ((Enum<?>)obj).ordinal();
			}else if(e){
				return Double.parseDouble(obj.toString());
			}else try{
				return Double.parseDouble(obj.toString());
			}catch(Exception ex){}
		}
		if(e){
			throw new NoSuchElementException();
		}
		return v;
	}
	public static BigInteger asBigInteger(Object obj,boolean e,BigInteger v){
		if(obj instanceof Number){
			if(obj instanceof BigInteger){
				return (BigInteger)obj;
			}
			if(obj instanceof Long || obj instanceof Integer || obj instanceof Short || obj instanceof Byte){
				return BigInteger.valueOf(((Number)obj).longValue());
			}
		}
		if(obj != null){
			if(obj instanceof Enum){
				return BigInteger.valueOf(((Enum<?>)obj).ordinal());
			}else if(e){
				return new BigInteger(obj.toString());
			}else try{
				return new BigInteger(obj.toString());
			}catch(Exception ex){}
		}
		if(e){
			throw new NoSuchElementException();
		}
		return v;
	}
	public static BigDecimal asBigDecimal(Object obj,boolean e,BigDecimal v){
		if(obj instanceof Number){
			if(obj instanceof BigDecimal){
				return (BigDecimal)obj;
			}
			if(obj instanceof BigInteger){
				return new BigDecimal((BigInteger)obj);
			}
			if(obj instanceof Long || obj instanceof Integer || obj instanceof Short || obj instanceof Byte){
				return new BigDecimal(((Number)obj).longValue());
			}
			return new BigDecimal(((Number)obj).doubleValue());
		}
		if(obj != null){
			if(obj instanceof Enum){
				return BigDecimal.valueOf(((Enum<?>)obj).ordinal());
			}
			if(e){
				return new BigDecimal(obj.toString());
			}else try{
				return new BigDecimal(obj.toString());
			}catch(Exception ex){}
		}
		if(e){
			throw new NoSuchElementException();
		}
		return v;
	}
	@SuppressWarnings("unchecked")
	public static <E extends Enum<E>> E asEnum(Class<E> clazz,Object obj,boolean e,E v){
		if(obj != null){
			if(clazz.isInstance(obj)){
				return (E)obj;
			}
			if(e){
				return Enum.valueOf(clazz, obj.toString());
			}else try{
				return Enum.valueOf(clazz, obj.toString());
			}catch(Exception ex){}
		}
		if(e){
			throw new NoSuchElementException();
		}
		return v;
	}
	public static String asString(Object obj,boolean e,String v){
		if(obj != null){
			if(obj instanceof Enum){
				return ((Enum<?>)obj).name();
			}
			String str = obj.toString();
			if(str != null){
				return str;
			}else if(e){
				throw new NullPointerException();
			}
		}
		if(e){
			throw new NoSuchElementException();
		}
		return v;
	}
	@SuppressWarnings("unchecked")
	public static JSMap asJSMap(Object obj,boolean e){
		if(obj instanceof JSMap){
			return (JSMap) obj;
		}
		if(obj instanceof Map){
			return new JSMap((Map<String,Object>)obj);
		}
		if(e){
			throw new NoSuchElementException();
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	public static JSList asJSList(Object obj,boolean e){
		if(obj instanceof JSList){
			return (JSList) obj;
		}
		if(obj instanceof List){
			return new JSList((List<Object>)obj);
		}
		if(e){
			throw new NoSuchElementException();
		}
		return null;
	}
	/**
	 * Parses a number from a string.
	 * @return Can return an instance of the following types:
	 * <li>Byte
	 * <li>Short
	 * <li>Integer
	 * <li>Long
	 * <li>Double
	 * <li>BigInteger
	 * <li>BigDecimal
	 * @throws NumberFormatException If the specified String cannot be parsed to a number.
	 * @throws NullPointerException If the specified String is null.
	 */
	public static Number parseNumber(String input) throws NumberFormatException, NullPointerException{
		if(input.indexOf('.') >= 0 || input.indexOf('e') >= 0 || input.indexOf('E') >= 0){
			if(input.length() > 14){
				return new BigDecimal(input);
			}
			double d = Double.parseDouble(input);
			if(!Double.isFinite(d) || !Double.toString(d).equals(input)){
				return new BigDecimal(input);
			}
			return Double.valueOf(d);
		}
		BigInteger bi = new BigInteger(input);
		if(bi.bitLength() < 16){
			if(bi.bitLength() < 8){
				return Byte.valueOf(bi.byteValue());
			}
			return Short.valueOf(bi.shortValue());
		}
		if(bi.bitLength() < 64){
			if(bi.bitLength() < 32){
				return Integer.valueOf(bi.intValue());
			}
			return Long.valueOf(bi.longValue());
		}
		return bi;
	}
	/**
	 * Returns the most compact representation of the specified integer argument.
	 */
	public static Number wrap(long value){
		if(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE){
			if(value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE){
				return Byte.valueOf((byte)value);
			}else{
				return Short.valueOf((short)value);
			}
		}else{
			if(value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE){
				return Integer.valueOf((int)value);
			}else{
				return Long.valueOf(value);
			}
		}
	}
	/**
	 * Wraps the specified object.
	 * <li>Iterables and Arrays will be converted to JSList with all their elements wrapped.
	 * <li>Map will be converted to JSMap with all it's values wrapped.
	 */
	public static Object wrap(Object obj){
		if(obj != null && obj.getClass().isArray()){
			int len = Array.getLength(obj);
			JSList a = new JSList(len);
			for(int i = 0;i < len;i++){
				a.add(Array.get(obj, i));
			}
			return a;
		}
		if(obj instanceof Iterable){
			JSList a = new JSList();
			a.addAll((Iterable<?>)obj);
			return a;
		}
		if(obj instanceof Map){
			JSMap o = new JSMap();
			o.putAll((Map<?,?>)obj);
		}
		return obj;
	}
	/**
	 * Unwraps the data structure encapsulated within the object provided.
	 * <li>If obj is a JSMap, returns unWrap(obj.map)
	 * <li>If obj is an JSList, returns unWrap(obj.list)
	 * <li>If obj is a map, returns a new HashMap with the keys and values of obj unwrapped.
	 * <li>If obj is Iterable or an array, returns a new ArrayList with the values of obj unwrapped.
	 * <li>Otherwise if obj is Serializable or null, returns obj.
	 * <li>Otherwise returns obj.toString().<br>
	 * 
	 */
	public static Object unWrap(Object obj){
		if(obj instanceof JSMap){
			obj = ((JSMap)obj).map;
		}
		if(obj instanceof Map){
			@SuppressWarnings("unchecked")
			Map<?,?> source = (Map<String,?>)obj;
			HashMap<String,Object> nMap = new HashMap<String, Object>(source.size());
			for(Entry<?,?> entry : source.entrySet()){
				Object k = entry.getKey();
				nMap.put(k == null ? null : (k instanceof Enum ? ((Enum<?>)k).name() : k.toString()), unWrap(entry.getValue()));
			}
			return nMap;
		}
		if(obj instanceof Iterable){
			List<?> source = (List<?>)obj;
			ArrayList<Object> nList = new ArrayList<>(source.size());
			for(Object entry : source){
				nList.add(unWrap(entry));
			}
			return nList;
		}
		if(obj == null){
			return null;
		}
		if(obj.getClass().isArray()){
			int s = Array.getLength(obj);
			ArrayList<Object> nList = new ArrayList<>(s);
			for(int i = 0;i < s;i++){
				nList.add(unWrap(Array.get(obj, i)));
			}
			return nList;
		}
		return obj instanceof Serializable ? obj : obj.toString();
	}
	@SuppressWarnings("unchecked")
	public static Map<String,Object> freezeMap(Map<?,?> map){
		if(map instanceof ImmutableMap){
			return (Map<String,Object>)map;
		}
		if(map.isEmpty()){
			return Map.of();
		}
		Map.Entry<?,?>[] target = new Map.Entry<?, ?>[map.size()];
		int index = 0;
		for(Map.Entry<?, ?> entry : map.entrySet()){
			Object k = entry.getKey();
			String sk = k == null ? null : (k instanceof String ? (String)k : k.toString());
			if(sk != null){
				if(target.length == index){
					target = Arrays.copyOf(target, index+1);
				}
				Object v = entry.getValue();
				Object vf = freeze(v);
				if(k == sk && v == vf && (entry instanceof AbstractMap.SimpleImmutableEntry || immMapEntry.isInstance(entry))){
					target[index++] = entry;
				}else{
					target[index++] = new AbstractMap.SimpleImmutableEntry<>(sk, vf);
				}
			}
		}
		return index == 0 ? Map.of() : new ImmutableMap<>(
				new ImmutableSet<>((Map.Entry<String,Object>[])(index < target.length ? Arrays.copyOf(target, index) : target)));
	}
	@SuppressWarnings("unchecked")
	public static List<Object> freezeList(Iterable<?> itr){
		if(itr instanceof JSList){
			itr = ((JSList)itr).list;
		}
		if(itr instanceof ImmutableList){
			return (ImmutableList<Object>)itr;
		}
		if(itr instanceof Collection<?>){
			Collection<?> col = (Collection<?>)itr;
			if(col.isEmpty()){
				return List.of();
			}
			Object[] target = new Object[col.size()];
			int index = 0;
			for(Object obj : col){
				if(target.length == index){
					target = Arrays.copyOf(target, index+1);
				}
				target[index++] = freeze(obj);
			}
			return target.length == 0 ? List.of() : new ImmutableList<>(index < target.length ? Arrays.copyOf(target, index) : target);
		}
		LinkedList<Object> target = new LinkedList<>();
		for(Object obj : itr){
			target.add(freeze(obj));
		}
		return target.isEmpty() ? List.of() : new ImmutableList<>(target.toArray());
	}
	/**
	 * If the value is a known immutable type, returns the value.<br>
	 * Otherwise if it's a JSMap, Map, JSList or List, returns it's freezeCopy<br>
	 * Otherwise return value.toString();
	 */
	public static Object freeze(Object value){
		if(value instanceof JSMap){
			JSMap j = ((JSMap)value);
			Map<String,Object> f = freezeMap(j.map);
			if(f == j.map){
				return j;
			}else{
				return new JSMap(f);
			}
		}else if(value instanceof JSList){
			JSList j = ((JSList)value);
			List<Object> f = freezeList(j.list);
			if(f == j.list){
				return j;
			}else{
				return new JSList(f);
			}
		}else if(value == null
					|| value instanceof Enum//We always take Enum.name() of enums as their string representation because that definitely cannot change.
					|| value instanceof Boolean
					|| value instanceof Double
					|| value instanceof Integer
					|| value instanceof Long
					|| value instanceof Float
					|| value instanceof Short
					|| value instanceof Byte){
			return value;//Value is immutable by itself.
		}else if(value instanceof Number){//Not necessarily immutable number.
			Number val = (Number)value;
			if(val.getClass() == BigInteger.class || val.getClass() == BigDecimal.class){
				return val;
			}
			if(val instanceof AtomicInteger){
				return Integer.valueOf(val.intValue());
			}
			if(val instanceof AtomicLong || val instanceof LongAdder || val instanceof LongAccumulator){
				return Long.valueOf(val.longValue());
			}
			if(val instanceof DoubleAdder || val instanceof DoubleAccumulator){
				return Double.valueOf(val.doubleValue());
			}
			return parseNumber(val.toString());//Beyond this point we don't know it's size or whether it's immutable or not.
		}else if(value instanceof AtomicBoolean){
			return Boolean.valueOf(((AtomicBoolean)value).get());
		}else if(value instanceof Map){
			return new JSMap(freezeMap((Map<?,?>)value));
		}else if(value instanceof Iterable){
			return new JSList(freezeList((Iterable<?>)value));
		}else if(value.getClass().isArray()){
			int len = Array.getLength(value);
			if(len == 0){
				return new JSList(List.of());
			}
			Object[] data = new Object[len];
			for(int i = 0;i < len;i++){
				data[i] = freeze(Array.get(value, i));
			}
			return new JSList(new ImmutableList<>(data));
		}
		return value.toString();//May return null.
	}
	public static void forEachMapEntry(Object obj,BiConsumer<String,Object> cns){
		if(obj instanceof JSMap){
			obj = ((JSMap)obj).map;
		}
		if(obj instanceof Map){
			for(Map.Entry<?,?> entry : ((Map<?,?>)obj).entrySet()){
				Object k = entry.getKey();
				if(k instanceof String || (k = k.toString()) != null){
					Object value = entry.getValue();
					cns.accept((String)k, value);
					if(value instanceof JSMap || value instanceof Map || value instanceof JSList || value instanceof Iterable){
						forEachMapEntry(value, cns);
					}
				}
			}
		}else if(obj instanceof Iterable){
			for(Object v : (Iterable<?>)obj){
				if(v != null){
					forEachMapEntry(v, cns);
				}
			}
		}
	}
	public static void forEachValue(Object obj,Consumer<Object> cns){
		if(obj instanceof JSMap){
			obj = ((JSMap)obj).map;
		}
		if(obj instanceof Map){
			for(Map.Entry<?,?> entry : ((Map<?,?>)obj).entrySet()){
				Object k = entry.getKey();
				if(k instanceof String || k.toString() != null){
					Object value = entry.getValue();
					cns.accept(value);
					if(value instanceof JSMap || value instanceof Map || value instanceof JSList || value instanceof Iterable){
						forEachValue(value, cns);
					}
				}
			}
		}else if(obj instanceof Iterable){
			for(Object v : (Iterable<?>)obj){
				cns.accept(v);
				if(v != null){
					forEachValue(v, cns);
				}
			}
		}
	}
	public static class PrettyWriter extends FilterWriter{
		int tabs;
		boolean insideString;
		boolean slash;
		public PrettyWriter(Writer out) {
			super(out);
		}
		@Override
		public void write(int c) throws IOException {
			if(insideString){
				out.write(c);
				if(slash){
					slash = false;
				}else{
					if(c == '"'){
						insideString = false;
					}
					if(c == '\\'){
						slash = true;
					}
				}
			}else{
				if(c == '"'){
					out.write(c);
					insideString = true;
				}else if(c == ',' || c == '{' || c == '[' || c == '}' || c == ']'){
					if(c == '{' || c == '['){
						out.write(c);
						out.write("\r\n");
						tabs++;
					}else if(c == '}' || c == ']'){
						out.write("\r\n");
						tabs--;
					}else{
						out.write(c);
						out.write("\r\n");
					}
					for(int t = 0;t < tabs;t++){
						out.write('\t');
					}
					if(c == '}' || c == ']'){
						out.write(c);
					}
				}else{
					out.write(c);
				}
			}
		}
		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			for(int i = 0;i < len;i++){
				write(cbuf[off+i]);
			}
		}
		@Override
		public void write(String str, int off, int len) throws IOException {
			for(int i = 0;i < len;i++){
				write(str.charAt(off+i));
			}
		}
		@Override
		public void flush() throws IOException {
			out.flush();
		}
		@Override
		public void close() throws IOException {
			out.close();
		}
		@Override
		public Writer append(CharSequence csq) throws IOException {
			if(csq == null){
				write(String.valueOf(null));
			}else{
				append(csq,0,csq.length());
			}
			return this;
		}
		@Override
		public Writer append(CharSequence csq, int start, int end) throws IOException {
			if(csq == null){
				write(String.valueOf(null));
			}else while(start != end){
				write(csq.charAt(start));
				start++;
			}
			return this;
		}
		@Override
		public Writer append(char c) throws IOException {
			write(c);
			return this;
		}
	}
}
