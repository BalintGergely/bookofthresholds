package net.balintgergely.runebook;

import java.util.Objects;

public class Build {
	private Champion champion;
	private byte roles;
	private Rune rune;
	private String name;
	public Build(String name,Champion champion,Rune rune,byte roles){
		this.name = Objects.requireNonNull(name);
		this.roles = roles;
		this.rune = Objects.requireNonNull(rune);
		this.champion = champion;
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
}
