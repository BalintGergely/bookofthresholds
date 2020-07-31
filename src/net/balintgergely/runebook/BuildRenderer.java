package net.balintgergely.runebook;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.geom.Ellipse2D;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import net.balintgergely.runebook.RuneModel.Path;
import net.balintgergely.runebook.RuneModel.Runestone;
import net.balintgergely.runebook.RuneModel.Statstone;

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
			Runestone keystone = rune.getKeystone();
			if(keystone == null){
				if(rune.primaryPath != null){
					gr.fillOval(x+12, y+24, 24, 24);
					gr.drawImage(rune.primaryPath.getImage(), x+12, y+24, 24, 24, null);
				}
			}else{
				gr.drawImage(keystone.getImage(), x, y, 48, 48, null);
			}
			if(rune.secondaryPath != null){
				gr.drawImage(rune.secondaryPath.getImage(), x+48, y+24, 24, 24, null);
			}
			x += 24;
			y += 50;
			Path path = rune.primaryPath;
			RuneModel model = rune.model;
			for(int p = 0;p < 3;p++){
				gr.setColor(GLASS_PANE_COLOR);
				gr.fillRoundRect(x-17, y-2, 33, 28, 8, 8);
				if(p == 2){
					int slots = model.getStatSlotCount();
					for(int i = 0;i < slots;i++){
						Statstone stone = (Statstone)rune.getSelectedStone(2, i);
						int stones = model.getStatSlotLength(i);
						int offset = -stones*4;
						int searchFor = stone == null ? -1 : stone.order;
						for(int k = 0;k < stones;k++){
							gr.setColor(k == searchFor ? stone.color : Color.DARK_GRAY);
							gr.fillOval(offset+x+k*8, y, 7, 7);
						}
						y += 8;
					}
					y -= slots*8;
					gr.setColor(STAT_MOD_BORDER);
				}else if(path != null){
					int slots = path.getSlotCount();
					for(int i = 1;i < slots;i++){
						int stones = path.getStoneCountInSlot(i);
						int offset = -stones*4;
						int searchFor = rune.getSelectedIndex(p, i);
						for(int k = 0;k < stones;k++){
							gr.setColor(k == searchFor ? path.color : Color.DARK_GRAY);
							gr.fillOval(offset+x+k*8, y, 7, 7);
						}
						y += 8;
					}
					y -= (slots-1)*8;
					gr.setColor(path.color);
				}
				gr.drawRoundRect(x-17, y-2, 33, 28, 10, 10);
				path = rune.secondaryPath;
				x += 36;
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
