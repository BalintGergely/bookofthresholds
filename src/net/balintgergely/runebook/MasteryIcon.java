package net.balintgergely.runebook;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import static net.balintgergely.runebook.LCUSummonerManager.*;

public class MasteryIcon implements Icon{
	public static void main(String[] atgs) throws Throwable{
		MasteryIcon icon = new MasteryIcon(ImageIO.read(MasteryIcon.class.getResourceAsStream("icons.png")));
		Byte[] array = new Byte[0x100];
		for(int i = 0;i < 0x100;i++){
			array[i] = (byte)i;
		}
		Arrays.sort(array);
		/*for(int a = 0;a < 0x1000;a++){
			for(int b = 0;b < 0x1000;b++){
				int resultA = icon.compare(array[a], array[b]);
				if(resultA != 0){
					if(resultA < 0){
						resultA = -1;
					}else if(resultA > 0){
						resultA = 1;
					}
					int resultB = Integer.compare(a, b);
					if(resultB < 0){
						resultB = -1;
					}else if(resultB > 0){
						resultB = 1;
					}
					if(resultA != resultB){
						throw new IllegalStateException();
					}
				}
			}
		}*/
		JLabel label = new JLabel(icon);
		JFrame frame = new JFrame("Mastery Icon Test");
		label.setOpaque(false);
		JList<Byte> list = new JList<>(array);
		list.setBackground(Color.BLACK);
		list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		list.setVisibleRowCount(0);
		list.setCellRenderer((ListCellRenderer<Byte>)(JList<? extends Byte> ls, Byte value, int index, boolean isSelected, boolean cellHasFocus) -> {
			icon.setStatusToDisplay(value);
			return label;
		});
		JScrollPane sp = new JScrollPane(list,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setBorder(null);
		sp.getViewport().setOpaque(false);
		sp.setOpaque(false);
		frame.add(sp);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	private BufferedImage white,locked,medal,unlocked,green,brown,red,purple,blue,button,ban,/*cpick,*/circle,blueHover,redHover;
	private int statusToDisplay;
	public int getStatusToDisplay() {
		return statusToDisplay;
	}
	public void setStatusToDisplay(int statusToDisplay) {
		this.statusToDisplay = statusToDisplay;
	}
	private static BufferedImage clip(BufferedImage img,int x,int y,int w,int h){
		return img.getSubimage(96+x, y, w, h);
	}
	MasteryIcon(BufferedImage am){
		white =		clip(am,	0,	0,	12,	12);
		locked =	clip(am,	0,	12,	12, 12);
		medal =		clip(am,	0,	24,	12, 12);
		unlocked =	clip(am,	0,	36,	12, 12);
		green =		clip(am,	0,	48,	12, 12);
		button =	clip(am,	14,	4,	4,	4 );
		ban =		clip(am,	0,	60,	12,	12);
		//cpick =	clip(am,	12,	60,	12,	12);
		brown =		clip(am,	20,	0,	52,	14);
		red =		clip(am,	20,	14, 52, 14);
		purple =	clip(am,	20,	28, 52, 14);
		blue =		clip(am,	20,	42, 52, 14);
		circle =	clip(am,	0,	72,	24,	24);
		blueHover =	clip(am,	24,	72,	24,	24);
		redHover =	clip(am,	48,	72, 24, 24);
	}
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		BufferedImage ik;
		int buttonCount = (statusToDisplay & MASTERY_MASK) / MASTERY_OFFSET;
		switch(buttonCount){
		case 0:ik = null;break;
		case 1:
		case 2:
		case 3:
		case 4:ik = brown;break;
		case 5:ik = red;buttonCount = 0;break;
		case 6:ik = purple;buttonCount = 0;break;
		case 7:ik = blue;buttonCount = 0;break;
		default:throw new Error();
		}
		if(ik != null){
			g.drawImage(ik, x+20, y+5, null);
		}
		for(int k = 0;k < buttonCount;k++){
			g.drawImage(button, x+26+k*10, y+10, null);
		}
		switch(statusToDisplay & MEDAL_MASK){
		case MEDAL_NOT_OWNED:ik = locked;break;
		case MEDAL_MASTERY:ik = medal;break;
		case MEDAL_FREE:ik = unlocked;break;
		case MEDAL_OWNED:ik = green;break;
		default:ik = white;break;
		}
		if(ik != null){
			g.drawImage(ik, x+6, y+6, null);
		}
		switch(statusToDisplay & STATUS_MASK){
		case STATE_NONE:
			g.drawImage(circle, x, y, null);
			break;
		case STATE_ALLY:
			g.drawImage(blueHover, x, y, null);
			break;
		case STATE_ENEMY:
			g.drawImage(redHover, x, y, null);
			break;
		case STATE_BANNED:
			g.drawImage(redHover, x, y, null);
			g.drawImage(ban, x+6, y+6, null);
			break;
		}
	}
	@Override
	public int getIconWidth() {
		return (statusToDisplay & MASTERY_MASK) == 0 ? 24 : 72;
	}
	@Override
	public int getIconHeight() {
		return 24;
	}
}
