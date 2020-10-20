package net.balintgergely.runebook;

import java.util.Arrays;
import java.util.Objects;

import net.balintgergely.util.NavigableList;
import net.balintgergely.util.NavigableList.StringList;
import net.balintgergely.util.OrdinalCollection;
import net.balintgergely.util.OrdinalMap;
import net.balintgergely.util.OrdinalSet;

public class Build implements Cloneable,OrdinalMap<String,Object>{
	private Champion champion;
	private byte flags;
	private Rune rune;
	private String name;
	private long order;
	public Build(String name,Champion champion,Rune rune,byte roles,long timestamp){
		this.name = Objects.requireNonNull(name);
		this.flags = roles;
		this.rune = Objects.requireNonNull(rune);
		this.champion = champion;
		this.order = timestamp;
	}
	public Build(Build that){
		this.name = that.name;
		this.flags = that.flags;
		this.rune = that.rune;
		this.champion = that.champion;
		this.order = that.order;
	}
	public Champion getChampion(){
		return champion;
	}
	public void setChampion(Champion ch){
		champion = ch;
	}
	public byte getFlags(){
		return flags;
	}
	public void setFlags(byte byt){
		flags = byt;
	}
	public Rune getRune(){
		return rune;
	}
	public void setRune(Rune rn){
		rune = Objects.requireNonNull(rn);
	}
	public String getName(){
		return name;
	}
	public void setName(String name){
		this.name = Objects.requireNonNull(name);
	}
	public long getOrder() {
		return order;
	}
	public void setOrder(long order){
		this.order = order;
	}
	@Override
	public Build clone() throws CloneNotSupportedException{
		return (Build)super.clone();
	}
	@Override
	public OrdinalSet<String> keySet() {
		return KEYS;
	}
	@Override
	public OrdinalCollection<Object> values() {
		throw new UnsupportedOperationException();
	}
	public static final StringList KEYS = new NavigableList.StringList(
Arrays.asList("champion","name","primaryStyleId","subStyleId","roles","selectedPerkIds","order"));
	@Override
	public String getKey(int index) {
		return KEYS.get(index);
	}
	@Override
	public Object getValue(int index) {
		return get(KEYS.get(index));
	}
	@Override
	public Object get(Object key) {
		switch((String)key){
		case "champion":return champion;
		case "name":return name;
		case "primaryStyleId":return rune == null ? null : rune.primaryPath;
		case "subStyleId":return rune == null ? null : rune.secondaryPath;
		case "roles":return flags;
		case "selectedPerkIds":return rune;
		case "order":return order;
		default:return null;
		}
	}
}
