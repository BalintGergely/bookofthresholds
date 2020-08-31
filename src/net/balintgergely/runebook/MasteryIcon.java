package net.balintgergely.runebook;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Comparator;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import static net.balintgergely.runebook.LCUManager.*;

public class MasteryIcon implements Icon,Comparator<Number>{
	public static void main(String[] atgs) throws Throwable{
		MasteryIcon icon = new MasteryIcon(ImageIO.read(MasteryIcon.class.getResourceAsStream("icons.png")));
		Integer[] array = new Integer[0x1000];
		for(int i = 0;i < 0x1000;i++){
			array[i] = i;
		}
		//Arrays.sort(array,icon);
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
		JFrame frame = new JFrame("Mastery Icon Test");
		JLabel label = new JLabel(icon);
		label.setOpaque(false);
		JList<Integer> list = new JList<>(array);
		list.setBackground(Color.BLACK);
		list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		list.setVisibleRowCount(0);
		list.setCellRenderer((ListCellRenderer<Integer>)(JList<? extends Integer> ls, Integer value, int index, boolean isSelected, boolean cellHasFocus) -> {
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
	private BufferedImage locked,medal,unlocked,green,brown,red,purple,blue,button,ban,cpick,circle,hover;
	private int statusToDisplay;
	public int getStatusToDisplay() {
		return statusToDisplay;
	}
	public void setStatusToDisplay(int statusToDisplay) {
		this.statusToDisplay = statusToDisplay;
	}
	@Override
	public int compare(Number a,Number b){
		int t1 = a.intValue(),t2 = b.intValue();
		int m1 = (t1 & MASTERY_MASK),m2 = (t2 & MASTERY_MASK);
		t1 ^= t2;
		if((t1 & HOVERED) != 0){
			return (t2 & HOVERED)-1;
		}
		if(m1 != m2){
			return m2-m1;
		}
		if((t1 & KNOWN_OWNED) != 0){
			return (t2 & KNOWN_OWNED)-1;
		}
		if((t2 & KNOWN_OWNED) == 0 && (t1 & FREE_ROTATION) != 0){
			return (t2 & FREE_ROTATION)-1;
		}
		if((t1 & MASTERY_CHEST) != 0){
			return (t2 & MASTERY_CHEST)-1;
		}
		return 0;
	}
	private static BufferedImage clip(BufferedImage img,int x,int y,int w,int h){
		return img.getSubimage(96+x, y, w, h);
	}
	MasteryIcon(BufferedImage am){
		locked =	clip(am,	6,	6,	12, 12);
		medal =		clip(am,	6,	30,	12, 12);
		unlocked =	clip(am,	6,	54,	12, 12);
		green =		clip(am,	6,	78,	12, 12);
		button =	clip(am,	48,	108,12,	12);
		ban =		clip(am,	48,	96,	12,	12);
		cpick =		clip(am,	60,	96,	12,	12);
		brown =		clip(am,	20,	5,	52,	14);
		red =		clip(am,	20,	29, 52, 14);
		purple =	clip(am,	20,	53, 52, 14);
		blue =		clip(am,	20,	77, 52, 14);
		circle =	clip(am,	0,	96,	24,	24);
		hover =		clip(am,	24,	96,	24,	24);
	}
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		BufferedImage ik;
		int buttonCount = (statusToDisplay & MASTERY_MASK);
		switch(buttonCount){
		case 0:ik = null;buttonCount = 0;break;
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
			g.drawImage(button, x+22+k*10, y+6, null);
		}
		if((statusToDisplay & HOVERED) != 0){
			g.drawImage(hover, x, y, null);
		}
		g.drawImage(circle, x, y, null);
		if((statusToDisplay & KNOWN_OWNED) != 0){
			ik = green;
		}else if((statusToDisplay & FREE_ROTATION) != 0){
			ik = unlocked;
		}else if((statusToDisplay & KNOWN_NOT_OWNED) != 0){
			ik = locked;
		}else if((statusToDisplay & MASTERY_CHEST) >= 5){
			ik = medal;
		}else{
			ik = null;
		}
		if(ik != null){
			g.drawImage(ik, x+6, y+6, null);
		}
		if(ik != locked){
			if((statusToDisplay & BANNED) != 0){
				g.drawImage(ban, x+6, y+6, null);
			}else if((statusToDisplay & PICKED_BY_ENEMY) != 0){
				g.drawImage(cpick, x+6, y+6, null);
			}
		}
	}
	@Override
	public int getIconWidth() {
		return 72;
	}
	@Override
	public int getIconHeight() {
		return 24;
	}
}
