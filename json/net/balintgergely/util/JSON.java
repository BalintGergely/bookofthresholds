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
import java.util.Comparator;
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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class JSON {
	public static void main(String[] atgs){
		System.out.println(Map.entry("JS", "ON").getClass() == immMapEntry);
		System.out.println("JSON");
	}
	public static final Pattern NUMBER_PATTERN = Pattern.compile("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");
	public static final Class<?> immMapEntry;
	public static final Comparator<Map.Entry<String,Object>> MAP_ENTRY_COMPARATOR = Comparator.comparing(Map.Entry::getKey);
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
	private static char smartSkip(Reader r,JSON carry) throws IOException{
		int c;
		if(carry != null && (c = carry.flag) != -2){
			carry.flag = -2;
			if(c == '/'){
				skipComment(r);
			}else{
				return (char)c;
			}
		}
		while(true){
			c = r.read();
			if(c < 0){
				eofex();
			}
			if(c == '/'){
				skipComment(r);
			}else if(!Character.isWhitespace((char)c)){
				return (char)c;
			}
		}
	}
	/**
	 * Called after encountering the comment character '/' while not inside a string literal.
	 */
	private static void skipComment(Reader r) throws IOException{
		int c = r.read();
		if(c == '*'){
			while(true){
				c = r.read();
				while(c == '*'){
					c = r.read();
					if(c == '/'){
						return;
					}
				}
				if(c < 0){
					eofex();
				}
			}
		}else if(c == '/'){
			while(true){
				c = r.read();
				if(c == '\n'){
					return;
				}
				if(c < 0){
					eofex();
				}
			}
		}else{
			ioex();
		}
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
		Object obj = readObject(input,null);
		if(obj == input){
			throw new IOException();
		}
		return obj;
	}
	/**
	 * Reads an object using the carrier to store an over-read character if finds one.
	 * Special cased to return the input Reader if encounters a ']'.
	 * @param input
	 * @param carrier
	 * @return
	 * @throws IOException
	 */
	private static Object readObject(Reader input,JSON carrier) throws IOException{
		char c = smartSkip(input,carrier);
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
					try{
						sb.append((char)Integer.parseUnsignedInt(new String(nexts), 16));
					}catch(Exception e){
						throw new IOException(e);
					}
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
		if(smartSkip(input,null) != '{'){
			throw new IOException();
		}
		readMap(map,input,new JSON());
	}
	private static void readMap(Map<String,Object> map,Reader input,JSON carrier) throws IOException{
		main: while(true){
			String key = null;
			switch(smartSkip(input,carrier)){
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
			if(smartSkip(input,carrier) != ':'){
				ioex();
			}
			Object value = readObject(input,carrier);
			if(value == input){
				throw new IOException();
			}
			map.put(key, value);
			switch(smartSkip(input,carrier)){
			case ',':
				break;
			case '}':
				break main;
			default:ioex();
			}
		}
	}
	public static void readList(Collection<Object> list,Reader input) throws IOException{
		if(smartSkip(input,null) != '['){
			throw new IOException();
		}
		readList(list,input,new JSON());
	}
	private static void readList(Collection<Object> list,Reader input,JSON carrier) throws IOException{
		Object value;
		main: while((value = readObject(input,carrier)) != input){
			list.add(value);
			switch(smartSkip(input,carrier)){
			case ',':
				break;
			case ']':
				break main;
			default:ioex();
			}
		}
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
	public static String quote(Number value){
		if(value == null){
			return "null";
		}else if(value instanceof Double
				|| value instanceof Integer
				|| value instanceof Long
				|| value instanceof Float
				|| value instanceof Short
				|| value instanceof Byte){
			return value.toString();
		}else{
			String str = value.toString();
			if(str == null){
				return "null";
			}
			String strp = str.strip();
			if(strp.equals("null")){
				return str;
			}
			if(NUMBER_PATTERN.matcher(strp).matches()){
				return str;
			}
			throw new IllegalArgumentException();
		}
	}
	public static void write(Object obj,Appendable output) throws IOException{
		if(obj instanceof JSMap){
			write(((JSMap)obj).map,output);
		}else if(obj instanceof JSList){
			write(((JSList)obj).list,output);
		}else if(obj instanceof Boolean){
			output.append(obj.toString());
		}else if(obj instanceof Number){
			output.append(quote((Number)obj));
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
			boolean f = false;
			@SuppressWarnings("resource")
			Commentator cmt = b instanceof Commentator ? (Commentator)b : null;
			@SuppressWarnings("unchecked")
			Map<String,Object> castMap = cmt == null ? null : (Map<String,Object>)map;
			if(cmt == null){
				b.append('{');
			}else{
				cmt.openAndComment('{',cmt.commentator.apply(castMap, null));
			}
			for(Entry<?,?> e : map.entrySet()){
				Object k = e.getKey();
				if(k instanceof String || ((k = k.toString()) != null)){
					if(f){
						b.append(',');
					}
					String key = (String)k;
					if(cmt != null){
						cmt.writeComment(cmt.commentator.apply(castMap, key));
					}
					write(key,b);
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
	public static JSMap asJSMap(Object obj){
		if(obj instanceof JSMap){
			return (JSMap) obj;
		}
		if(obj instanceof Map){
			return new JSMap((Map<String,Object>)obj);
		}
		throw new NoSuchElementException();
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
	@SuppressWarnings("unchecked")
	public static JSList asJSList(Object obj){
		if(obj instanceof JSList){
			return (JSList) obj;
		}
		if(obj instanceof List){
			return new JSList((List<Object>)obj);
		}
		throw new NoSuchElementException();
	}
	@SuppressWarnings("unchecked")
	public static JSMap toJSMap(Object obj){
		if(obj instanceof JSMap){
			return (JSMap) obj;
		}
		if(obj instanceof Map){
			return new JSMap((Map<String,Object>)obj);
		}
		return JSMap.EMPTY_MAP;
	}
	@SuppressWarnings("unchecked")
	public static JSList toJSList(Object obj){
		if(obj instanceof JSList){
			return (JSList) obj;
		}
		if(obj instanceof List){
			return new JSList((List<Object>)obj);
		}
		return JSList.EMPTY_LIST;
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
		if(map instanceof Immutable){
			return (Map<String,Object>)map;
		}
		if(map.isEmpty()){
			return Map.of();
		}
		Map.Entry<String,Object>[] target = (Map.Entry<String,Object>[])new Map.Entry<?, ?>[map.size()];
		int length = 0;
		for(Map.Entry<?, ?> entry : map.entrySet()){
			Object k = entry.getKey();
			String sk = k == null ? null : (k instanceof String ? (String)k : k.toString());
			if(sk != null){
				if(target.length == length){
					target = Arrays.copyOf(target, length+1);
				}
				Object v = entry.getValue();
				Object vf = freeze(v);
				if(k == sk && v == vf && (entry instanceof AbstractMap.SimpleImmutableEntry || immMapEntry.isInstance(entry))){
					target[length++] = (Map.Entry<String, Object>)entry;
				}else{
					target[length++] = new AbstractMap.SimpleImmutableEntry<>(sk, vf);
				}
			}
		}
		if(length == 0){
			return Map.of();
		}
		Arrays.sort(target, 0, length, MAP_ENTRY_COMPARATOR);
		String latest = target[0].getKey();
		int a = 1,b = 1;
		while(a < length){
			String current = target[a].getKey();
			if(!latest.equals(current)){
				latest = current;
				target[b] = target[a];
				b++;
			}
			a++;
		}
		return new Immutable.ImmutableMap(b < target.length ? Arrays.copyOf(target, b) : target);
	}
	@SuppressWarnings("unchecked")
	public static List<Object> freezeList(Iterable<?> itr){
		if(itr instanceof JSList){
			itr = ((JSList)itr).list;
		}
		if(itr instanceof Immutable){
			return (List<Object>)itr;
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
			return target.length == 0 ? List.of() : new Immutable.ImmutableList<>(index < target.length ? Arrays.copyOf(target, index) : target);
		}
		LinkedList<Object> target = new LinkedList<>();
		for(Object obj : itr){
			target.add(freeze(obj));
		}
		return target.isEmpty() ? List.of() : new Immutable.ImmutableList<>(target.toArray());
	}
	/**
	 * If the value is a known immutable type, returns the value.<br>
	 * Otherwise if it's a JSMap, Map, JSList or List, returns it's freezeCopy<br>
	 * Otherwise return value.toString();
	 */
	public static Object freeze(Object value){
		if(value instanceof JSMap){
			return freeze((JSMap)value);
		}else if(value instanceof JSList){
			return freeze((JSList)value);
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
			return new JSList(new Immutable.ImmutableList<>(data));
		}
		return value.toString();//May return null.
	}
	public static JSMap freeze(JSMap j){
		Map<String,Object> f = freezeMap(j.map);
		if(f == j.map){
			return j;
		}else{
			return new JSMap(f);
		}
	}
	public static JSList freeze(JSList j){
		List<Object> f = freezeList(j.list);
		if(f == j.list){
			return j;
		}else{
			return new JSList(f);
		}
	}
	public static void forEachMapEntry(Object obj,BiConsumer<String,Object> cns,int maxDepth){
		if(obj instanceof JSMap){
			obj = ((JSMap)obj).map;
		}
		if(obj instanceof Map){
			maxDepth--;
			for(Map.Entry<?,?> entry : ((Map<?,?>)obj).entrySet()){
				Object k = entry.getKey();
				if(k instanceof String || (k = k.toString()) != null){
					Object value = entry.getValue();
					cns.accept((String)k, value);
					if(maxDepth != -1){
						forEachMapEntry(value, cns, maxDepth);
					}
				}
			}
		}else if(obj instanceof Iterable){
			if(maxDepth != 0){
				maxDepth--;
				for(Object v : (Iterable<?>)obj){
					if(v != null){
						forEachMapEntry(v, cns, maxDepth);
					}
				}
			}
		}
	}
	public static void forEachValue(Object obj,Consumer<Object> cns,int maxDepth){
		if(obj instanceof JSMap){
			obj = ((JSMap)obj).map;
		}
		if(obj instanceof Map){
			maxDepth--;
			for(Map.Entry<?,?> entry : ((Map<?,?>)obj).entrySet()){
				Object k = entry.getKey();
				if(k instanceof String || k.toString() != null){
					Object value = entry.getValue();
					cns.accept(value);
					if(maxDepth != -1){
						forEachValue(value, cns, maxDepth);
					}
				}
			}
		}else if(obj instanceof Iterable){
			maxDepth--;
			for(Object v : (Iterable<?>)obj){
				cns.accept(v);
				if(maxDepth != -1){
					forEachValue(v, cns, maxDepth);
				}
			}
		}
	}
	/**
	 * A PrettyWriter formats JSON as it is being written.
	 * @author balintgergely
	 */
	public static class PrettyWriter extends FilterWriter{
		protected int tabs;
		protected boolean insideString;
		protected boolean slash;
		public PrettyWriter(Writer out) {
			super(out);
		}
		protected void writeTabs() throws IOException{
			for(int t = 0;t < tabs;t++){
				out.write('\t');
			}
		}
		/**
		 * Writes the specified character, then the specified comment then proceeds as normal.
		 * The specified character can only be either <code>'{'</code> or <code>'['</code>
		 */
		public void openAndComment(int c,String comment) throws IOException{
			if(insideString){
				throw new IllegalStateException();
			}
			if(c != '{' && c != '['){
				throw new IllegalArgumentException();
			}
			out.write(c);
			tabs++;
			if(comment == null){
				out.write("\r\n");
				writeTabs();
			}else{
				out.write('\t');
				writeComment(comment);
			}
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
						tabs++;
					}else if(c == '}' || c == ']'){
						tabs--;
					}else{
						out.write(c);
					}
					out.write("\r\n");
					writeTabs();
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
		public void writeComment(String comment) throws IOException{
			if(insideString){
				throw new IllegalStateException();
			}
			if(comment != null){
				int index = comment.indexOf('\n');
				int lastIndex = comment.length()-1;
				if(index < 0 || index == lastIndex){//Single line comment.
					if(!comment.startsWith("//")){
						if(comment.startsWith("/")){
							out.write('/');
						}else{
							out.write("// ");
						}
					}
					out.write(comment);
					if(index < 0){
						if(comment.endsWith("\r")){
							out.write('\n');
						}else{
							out.write("\r\n");
						}
					}
					writeTabs();
					return;
				}
				lastIndex--;
				index = comment.indexOf("*/");
				if(index < 0 || index == lastIndex){//Multi-line comment.
					if(!comment.startsWith("/*")){
						if(comment.startsWith("*")){
							out.write('/');
						}else{
							out.write("/* ");
						}
					}
					lastIndex = comment.length();
					int s = 0,i;
					while((i = comment.indexOf('\n',s)) >= 0){
						i++;
						out.write(comment, s, i-s);
						writeTabs();
						out.write(" * ");
						s = i;
						i = comment.indexOf('\n',s);
					}
					out.write(comment, s, lastIndex-s);
					if(index < 0){
						if(comment.endsWith("*")){
							out.write("/\r\n");
						}else{
							out.write("*/\r\n");
						}
					}else{
						out.write("\r\n");
					}
					writeTabs();
					return;
				}
				throw new IOException();
			}
		}
	}
	/**
	 * A Commentator is special cased in the writeMap method to write an additional comment ahead of 
	 * each key-value pair. Comments are ignored by the parsing algorithms.
	 * @author balintgergely
	 */
	public static class Commentator extends PrettyWriter{
		protected BiFunction<? super Map<? super String,Object>,? super String,String> commentator;
		public Commentator(Writer out,BiFunction<? super Map<? super String,Object>,? super String,String> cmt) {
			super(out);
			this.commentator = cmt;
		}
	}
	public static boolean isImmutable(Map<?,?> obj){
		return (obj instanceof Mirror.DescendingMap ? ((Mirror.DescendingMap<?,?>)obj).mirror : obj) instanceof Immutable;
	}
	public static boolean isImmutable(Iterable<?> obj){
		return (obj instanceof Mirror.DescendingSet ? ((Mirror.DescendingSet<?>)obj).mirror : obj) instanceof Immutable;
	}
}
