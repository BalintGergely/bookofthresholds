package net.balintgergely.runebook;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
//import java.awt.Graphics2D;
import java.awt.Insets;
//import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import net.balintgergely.runebook.RuneModel.Path;
import net.balintgergely.runebook.RuneModel.Runestone;
import net.balintgergely.runebook.RuneModel.Statstone;
import net.balintgergely.runebook.RuneModel.Stone;

public class BuildRenderer extends JPanel implements ListCellRenderer<Build>{
	@SuppressWarnings("hiding")
	private static final int WIDTH = 120,HEIGHT = 90;
	private static final long serialVersionUID = 1L;
	private static final Color STAT_MOD_BORDER = new Color(0xA88035),
								GLASS_PANE_COLOR = new Color(0x50000000,true);
	private final AssetManager assetManager;
	private Build build;
	public BuildRenderer(AssetManager assetManager){
		super(false);
		this.assetManager = assetManager;
		super.setBackground(Color.DARK_GRAY);
		super.setOpaque(false);
	}
	private Insets insets = new Insets(0,0,0,0);
	@Override
	public Dimension getPreferredSize(){
		super.getInsets(insets);
		Dimension dim = new Dimension(WIDTH+insets.left+insets.right, HEIGHT+insets.bottom+insets.top);
		return dim;
	}
	@Override
	public void paintComponent(Graphics gr){
		if(build == null){
			return;
		}
		String name = build.getName();
		Champion champion = build.getChampion();
		Rune rune = build.getRune();
		byte roles = build.getRoles();
		super.getInsets(insets);
		int x = insets.left,y = insets.top;
		//((Graphics2D)gr).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		//gr.setColor(Color.DARK_GRAY);
		//gr.fillRect(x, y, WIDTH, HEIGHT);
		{
			FontMetrics metrics = gr.getFontMetrics();
			int width = metrics.stringWidth(name);
			int xp = (WIDTH - width) / 2;
			gr.setColor(GLASS_PANE_COLOR);
			gr.fillRoundRect(x+xp-1, y, width+2, 12, 3, 3);
			gr.setColor(Color.WHITE);
			gr.drawString(name, x+xp, y+10);
		}
		y += 12;
		if(champion != null){
			Graphics ngr = gr.create(x+72, y, 48, 48);
			ngr.setClip(new Ellipse2D.Float(2, 2, 44, 44));
			champion.paintIcon(null, ngr, 0, 0, 48, 48);
			ngr.dispose();
		}
		assetManager.paintRoleIcon(gr, roles, x+48, y);
		if(rune != null){
			gr.setColor(GLASS_PANE_COLOR);
			gr.fillOval(x+48, y+24, 24, 24);
			gr.drawImage(rune.stoneList.get(0).getImage(), x, y, 48, 48, null);
			gr.drawImage(rune.secondaryPath.getImage(), x+48, y+24, 24, 24, null);
			x += 24;
			y += 50;
			Path path = rune.primaryPath;
			RuneModel model = path.model;
			int statStoneOffset = rune.statStoneOffset();
			for(int p = 0;p < 3;p++){
				int slots;
				gr.setColor(GLASS_PANE_COLOR);
				gr.fillRoundRect(x-17, y-2, 33, 28, 8, 8);
				switch(p){
				case 0:
				case 1:	gr.setColor(path.color);
						slots = path.getSlotCount()-1;break;
				default:gr.setColor(STAT_MOD_BORDER);
					path = null;
					slots = model.getStatSlotCount();
				}
				gr.drawRoundRect(x-17, y-2, 33, 28, 10, 10);
				for(int i = 0;i < slots;i++){
					int stones = p == 2 ? model.getStatSlotLength(i) : path.getStoneCountInSlot(i+1);
					int offset = -stones*4;
					Stone st;
					switch(p){
					case 0:st = rune.stoneList.get(1+i);break;
					case 2:st = rune.stoneList.get(statStoneOffset+i);break;
					default:st = null;
					}
					for(int k = 0;k < stones;k++){
						switch(p){
						case 0:gr.setColor(((Runestone)st).index == k ? path.color : Color.DARK_GRAY);break;
						case 1:gr.setColor(rune.stoneList.contains(path.getStone(i+1, k)) ? path.color : Color.DARK_GRAY);break;
						case 2:gr.setColor(model.getStatstone(i, k) == st ? ((Statstone)st).color : Color.DARK_GRAY);break;
						}
						gr.fillOval(offset+x+k*8, y, 7, 7);
					}
					y += 8;
				}
				path = rune.secondaryPath;
				slots = path.getSlotCount();
				x += 36;
				y -= (slots-1)*8;
			}
		}
	}
	private Border	BASIC_BORDER = new LineBorder(new Color(0,true), 2, true),
					SELECTED_BORDER = new LineBorder(Color.WHITE, 2, true);
	@Override
	public Component getListCellRendererComponent(JList<? extends Build> list, Build value, int index,
			boolean isSelected, boolean cellHasFocus) {
		super.setBorder(isSelected ? SELECTED_BORDER : BASIC_BORDER);
		//super.setBackground(cellHasFocus ? Color.DARK_GRAY : Color.BLACK);
		this.build = value;
		return this;
	}
}
