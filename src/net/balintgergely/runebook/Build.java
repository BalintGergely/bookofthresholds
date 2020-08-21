package net.balintgergely.runebook;

import java.util.Objects;

public class Build implements Cloneable{
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
}
