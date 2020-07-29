package net.balintgergely.runebook;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

public class Champion implements Icon{
	public final String id;
	private BufferedImage image;
	private String name;
	//private JSMap data;
	public Champion(String id,String name,BufferedImage sprite,int x,int y,int w,int h){
		this.id = id;
		this.image = sprite.getSubimage(x, y, w, h);
		this.name = name;
		//this.data = data;
	}
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		g.drawImage(image, x, y, 24, 24, c);
	}
	public void paintIcon(Component c, Graphics g, int x, int y, int w, int h){
		g.drawImage(image, x, y, w, h, c);
	}
	@Override
	public int getIconWidth() {
		return 24;
	}
	@Override
	public int getIconHeight() {
		return 24;
	}
	@Override
	public String toString(){
		return name;
	}
}
