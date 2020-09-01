package net.balintgergely.runebook;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

public class Champion implements Icon,Comparable<Champion>{
	private final String id;
	public final int key;
	private BufferedImage image;
	private String name;
	public Champion(String id,String name,int key,BufferedImage image){
		this.id = id;
		this.key = key;
		this.image = image;
		this.name = name;
	}
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		g.drawImage(image, x, y, 24, 24, null);
	}
	public void paintIcon(Component c, Graphics g, int x, int y, int w, int h){
		g.drawImage(image, x, y, w, h, null);
	}
	@Override
	public int getIconWidth() {
		return 24;
	}
	@Override
	public int getIconHeight() {
		return 24;
	}
	public String getName(){
		return name;
	}
	@Override
	public String toString(){
		return id;
	}
	@Override
	public int compareTo(Champion o) {
		return id.compareTo(o.id);
	}
	@Override
	public int hashCode() {
		return key;
	}
	@Override
	public boolean equals(Object that){
		if(that instanceof Champion){
			if(that instanceof Variant){
				return that.equals(this);
			}
			return id == ((Champion)that).id;
		}
		return false;
	}
	public static class Variant extends Champion{
		public final Champion superChampion;
		public Variant(Champion other,String formId,BufferedImage image){
			super(other.id+formId,other.name,other.key,image);
			this.superChampion = other;
		}
		@Override
		public int hashCode(){
			return superChampion.hashCode();
		}
		@Override
		public boolean equals(Object that){
			return superChampion.equals(that);
		}
	}
}
