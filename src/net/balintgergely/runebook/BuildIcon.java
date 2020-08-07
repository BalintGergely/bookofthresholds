package net.balintgergely.runebook;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

import net.balintgergely.runebook.RuneModel.Path;
import net.balintgergely.runebook.RuneModel.Runestone;
import net.balintgergely.runebook.RuneModel.Statstone;

public class BuildIcon implements Icon{
	private static final int G_WIDTH = 120,G_HEIGHT = 90,F_WIDTH = 160,F_HEIGHT = 42;
	private static final Color STAT_MOD_BORDER = new Color(0xA88035),
								GLASS_PANE_COLOR = new Color(0x50000000,true);
	private final AssetManager assetManager;
	private boolean gridVariant;
	private Build build;
	public BuildIcon(boolean useGridVariant,AssetManager assetManager){
		this.gridVariant = useGridVariant;
		this.assetManager = assetManager;
	}
	public boolean isGridVariant() {
		return this.gridVariant;
	}
	public void setGridVariant(boolean gridVariant) {
		this.gridVariant = gridVariant;
	}
	public Build getBuild() {
		return this.build;
	}
	public void setBuild(Build build) {
		this.build = build;
	}
	public static BufferedImage toImage(AssetManager mgr,Build bld){
		boolean gridVariant = ((bld.getRoles() & 0x1F) != 0) || (bld.getChampion() != null);
		BufferedImage image = new BufferedImage(gridVariant ? G_WIDTH : F_WIDTH, gridVariant ? G_HEIGHT : F_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics gr = image.getGraphics();
		renderBuild(mgr, bld, gridVariant, gr, 0, 0);
		gr.dispose();
		return image;
	}
	public static void renderBuild(AssetManager mgr,Build build,boolean gridVariant,Graphics gr,int x,int y){
		if(build == null){
			return;
		}
		gr.setFont(new Font("Tahoma", Font.PLAIN, 11));
		String name = build.getName();
		Rune rune = build.getRune();
		int ox = x,oy = y;
		if(gridVariant){
			y += 12;
			Champion champion = build.getChampion();
			byte roles = build.getRoles();
			if(champion != null){
				Graphics ngr = gr.create(x+72, y, 48, 48);
				ngr.setClip(new Ellipse2D.Float(2, 2, 44, 44));
				champion.paintIcon(null, ngr, 0, 0, 48, 48);
				ngr.dispose();
			}
			mgr.paintRoleIcon(gr, roles, x+48, y);
		}
		if(rune != null){
			Runestone keystone = rune.getKeystone();
			if(gridVariant){
				gr.setColor(GLASS_PANE_COLOR);
				gr.fillOval(x+48, y+24, 24, 24);
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
			}else{
				if(keystone == null){
					if(rune.primaryPath != null){
						gr.drawImage(rune.primaryPath.getImage(), x, y, 32, 32, null);
					}
				}else{
					gr.drawImage(keystone.getImage(), x-4, y-4, 48, 48, null);
				}
				if(rune.secondaryPath != null){
					gr.drawImage(rune.secondaryPath.getImage(), x+20, y+16, 24, 24, null);
				}
				x += 64;
				y += 14;
			}
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
		{
			FontMetrics metrics = gr.getFontMetrics();
			int width = metrics.stringWidth(name);
			int xp = (G_WIDTH - width) / 2;
			if(!gridVariant){
				xp += 40;
				if(xp+width > F_WIDTH){
					xp = F_WIDTH-width;
				}
			}
			gr.setColor(GLASS_PANE_COLOR);
			gr.fillRoundRect(ox+xp-1, oy, width+2, 12, 3, 3);
			gr.setColor(Color.WHITE);
			gr.drawString(name, ox+xp, oy+10);
		}
	}
	@Override
	public void paintIcon(Component c, Graphics gr, int x, int y) {
		renderBuild(assetManager, build, gridVariant, gr, x, y);
	}
	@Override
	public int getIconWidth() {
		return gridVariant ? G_WIDTH : F_WIDTH;
	}
	@Override
	public int getIconHeight() {
		return gridVariant ? G_HEIGHT : F_HEIGHT;
	}
}
