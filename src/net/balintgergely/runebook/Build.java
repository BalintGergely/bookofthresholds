package net.balintgergely.runebook;

import java.util.Objects;

public class Build implements Cloneable{
	private Champion champion;
	private byte roles;
	private Rune rune;
	private String name;
	private long timestamp;
	public Build(String name,Champion champion,Rune rune,byte roles,long timestamp){
		this.name = Objects.requireNonNull(name);
		this.roles = roles;
		this.rune = Objects.requireNonNull(rune);
		this.champion = champion;
		this.timestamp = timestamp;
	}
	public Build(Build that){
		this.name = that.name;
		this.roles = that.roles;
		this.rune = that.rune;
		this.champion = that.champion;
		this.timestamp = that.timestamp;
	}
	public Champion getChampion(){
		return champion;
	}
	public void setChampion(Champion ch){
		champion = ch;
	}
	public byte getRoles(){
		return roles;
	}
	public void setRoles(byte byt){
		roles = byt;
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
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp){
		this.timestamp = timestamp;
	}
	@Override
	public Build clone() throws CloneNotSupportedException{
		return (Build)super.clone();
	}
}
