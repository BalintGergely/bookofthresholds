package net.balintgergely.runebook;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Objects;

public class Build implements Transferable{
	private static final DataFlavor DATA_FLAVOR = new DataFlavor(Build.class, "Rune Book Page");
	private Champion champion;
	private byte roles;
	private Rune rune;
	private String name;
	long timestamp;
	public Build(String name,Champion champion,Rune rune,byte roles,long timestamp){
		this.name = Objects.requireNonNull(name);
		this.roles = roles;
		this.rune = Objects.requireNonNull(rune);
		this.champion = champion;
		this.timestamp = timestamp;
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
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[]{DATA_FLAVOR};
	}
	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(DATA_FLAVOR);
	}
	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if(!flavor.equals(DATA_FLAVOR)){
			throw new UnsupportedFlavorException(flavor);
		}
		return this;
	}
}
