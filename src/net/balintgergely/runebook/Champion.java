package net.balintgergely.runebook;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;

import javax.swing.Icon;

import net.balintgergely.util.NavigableList;

public class Champion implements Icon,Comparable<Champion>{
	private final String id;
	public final Number key;
	public final List<String> tags;
	private BufferedImage image;
	private String name;
	private int index = -1;
	Champion(String id,String name,Number key,BufferedImage image,List<String> tags){
		this.id = id;
		this.key = key;
		this.image = image;
		this.name = name;
		this.tags = tags;
	}
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		g.drawImage(image, x, y, 24, 24, null);
	}
	public void paintIcon(Component c, Graphics g, int x, int y, int w, int h){
		g.drawImage(image, x, y, w, h, null);
	}
	void setIndex(int index){
		if(this.index >= 0){
			throw new IllegalStateException();
		}
		this.index = index;
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
	public int hashCode() {
		return key.hashCode();
	}
	@Override
	public int compareTo(Champion o) {
		return id.compareTo(o.id);
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
		Variant(Champion other,String formId,BufferedImage image){
			super(other.id+formId,other.name,other.key,image,other.tags);
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
	public static class ChampionList extends NavigableList<Champion> implements Comparator<Champion>{
		ChampionList(Champion[] championData){
			super(championData);
			for(int i = 0;i < championData.length;i++){
				championData[i].setIndex(i);
			}
		}
		@Override
		public Comparator<? super Champion> comparator() {
			return this;
		}
		@Override
		public int indexOf(Object o) {
			return o instanceof Champion ? ((Champion)o).index : -1;
		}
		@Override
		protected int seek(Champion element, int min, int max, boolean lower, boolean inclusive) {
			int index = element.index;
			if(inclusive){
				return index;
			}else if(lower){
				return index-1;
			}else{
				index++;
				return index >= data.length ? -1 : index;
			}
		}
		@Override
		public int compare(Champion o1, Champion o2) {
			return o1.index-o2.index;
		}
	}
}
