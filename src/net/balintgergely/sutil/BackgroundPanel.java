package net.balintgergely.sutil;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JPanel;

public class BackgroundPanel extends JPanel{
	private static final long serialVersionUID = 1L;
	public BackgroundPanel(Image background){
		super(null,false);
		this.fullImage = background;
	}
	private Image fullImage;
	private Image temp;
	@Override
	protected void paintComponent(Graphics g) {
		int width = getWidth();
		int height = getHeight();
		int imgheight = fullImage.getHeight(null);
		int imgwidth = fullImage.getWidth(null);
		double	widthRatio = width/(double)imgwidth,
				heightRatio = height/(double)imgheight;
		if(widthRatio < heightRatio){
			imgwidth = (int)Math.round(imgwidth*heightRatio);
		}else{
			imgwidth = width;
		}
		if(widthRatio > heightRatio){
			imgheight = (int)Math.round(imgheight*widthRatio);
		}else{
			imgheight = height;
		}
		if(temp == null || temp.getWidth(null) != imgwidth || temp.getHeight(null) != imgheight){
			temp = createImage(imgwidth, imgheight);
			Graphics gr = temp.getGraphics();
			gr.drawImage(fullImage, 0, 0, imgwidth, imgheight, null);
			gr.dispose();
		}
		g.drawImage(temp, (width-imgwidth)/2, (height-imgheight)/2, imgwidth, imgheight, null);
	}
}
