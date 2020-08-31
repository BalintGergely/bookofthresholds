package net.balintgergely.runebook;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

public class Champion implements Icon,Comparable<Champion>{
	public final String id;
	public final int key;
	private BufferedImage image;
	private String name;
	//private JSMap data;
	public Champion(String id,String name,int key,BufferedImage sprite,int x,int y,int w,int h){
		this.id = id;
		this.key = key;
		this.image = sprite.getSubimage(x, y, w, h);
		this.name = name;
		//this.data = data;
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
}
