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
	private static final Color //STAT_MOD_BORDER = new Color(0xA88035),
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
		boolean gridVariant = ((bld.getFlags() & 0x1F) != 0) || (bld.getChampion() != null);
		BufferedImage image = new BufferedImage(gridVariant ? G_WIDTH : F_WIDTH, gridVariant ? G_HEIGHT : F_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics gr = image.getGraphics();
		renderBuild(mgr, bld, gridVariant, gr, 0, 0);
		gr.dispose();
		return image;
	}
	private static void renderBuild(AssetManager mgr,Build build,boolean gridVariant,Graphics gr,int x,int y){
		if(build == null){
			return;
		}
		gr.setFont(new Font("Tahoma", Font.PLAIN, 11));
		String name = build.getName();
		Rune rune = build.getRune();
		int ox = x,oy = y;
		if(gridVariant){
			gr.drawImage(mgr.runeBase, x, y, null);
			y += 12;
			Champion champion = build.getChampion();
			byte roles = build.getFlags();
			if(champion != null){
				Graphics ngr = gr.create(x+72, y, 48, 48);
				ngr.setClip(new Ellipse2D.Float(2, 2, 44, 44));
				champion.paintIcon(null, ngr, 0, 0, 48, 48);
				ngr.dispose();
			}
			mgr.paintRoleIcon(gr, roles, x+48, y);
		}else{
			gr.drawImage(mgr.runeBase, x+40, y, x+160, y+12, 0, 00, 120, 12, null);
			gr.drawImage(mgr.runeBase, x+40, y+12, x+160, y+42, 0, 60, 120, 90, null);
		}
		if(rune != null){
			boolean lockFlag = (build.getFlags() & 0x40) != 0;
			Runestone keystone = rune.getKeystone();
			if(gridVariant){
				gr.setColor(GLASS_PANE_COLOR);
				gr.fillOval(x+48, y+24, 24, 24);
				if(keystone == null){
					if(rune.primaryPath != null){
						gr.fillOval(x+12, y+24, 24, 24);
						gr.drawImage(mgr.runeIcons.get(rune.primaryPath), x+12, y+24, 24, 24, null);
					}
				}else{
					gr.drawImage(mgr.runeIcons.get(keystone), x, y, 48, 48, null);
				}
				if(rune.secondaryPath != null){
					gr.drawImage(mgr.runeIcons.get(rune.secondaryPath), x+48, y+24, 24, 24, null);
				}
				x += 24;
				y += 51;
			}else{
				if(keystone == null){
					if(rune.primaryPath != null){
						gr.drawImage(mgr.runeIcons.get(rune.primaryPath), x, y, 32, 32, null);
					}
				}else{
					gr.drawImage(mgr.runeIcons.get(keystone), x-4, y-4, 48, 48, null);
				}
				if(rune.secondaryPath != null){
					gr.drawImage(mgr.runeIcons.get(rune.secondaryPath), x+20, y+16, 24, 24, null);
				}
				x += 64;
				y += 15;
			}
			Path path = rune.primaryPath;
			RuneModel model = rune.model;
			for(int p = 0;p < 3;p++){
				//gr.setColor(GLASS_PANE_COLOR);
				//gr.fillRoundRect(x-17, y-2, 33, 27, 8, 8);
				if(p == 2){
					int slots = model.getStatSlotCount();
					for(int i = 0;i < slots;i++){
						Statstone stone = (Statstone)rune.getSelectedStone(2, i);
						int stones = model.getStatSlotLength(i);
						int offset = -stones*4;
						/*if(lockFlag){
							gr.setColor(Color.DARK_GRAY);
							gr.fillRect(x-16, y+3, 32, 2);
							if(stone != null){
								gr.setColor(mgr.runeColors.get(stone));
								gr.fillOval(offset+x+stone.order*8, y, 7, 7);
							}
						}else{*/
							int searchFor = stone == null ? -1 : stone.order;
							for(int k = 0;k < stones;k++){
								gr.setColor(k == searchFor ? mgr.runeColors.get(stone) : Color.DARK_GRAY);
								if(lockFlag && k != searchFor){
									gr.fillRect(offset+x+k*8+1, y+3, 6, 2);
								}else{
									gr.fillOval(offset+x+k*8, y, 7, 7);
								}
							}
						//}
						y += 8;
					}
					y -= slots*8;
					//gr.setColor(STAT_MOD_BORDER);
				}else if(path != null){
					int slots = path.getSlotCount();
					Color pathColor = mgr.runeColors.get(path);
					for(int i = 1;i < slots;i++){
						int stones = path.getStoneCountInSlot(i);
						int offset = -stones*4;
						int searchFor = rune.getSelectedIndex(p, i);
						/*if(lockFlag){
							gr.setColor(Color.DARK_GRAY);
							gr.fillRect(x-16, y+3, 32, 2);
							if(searchFor >= 0){
								gr.setColor(pathColor);
								gr.fillOval(offset+x+searchFor*8, y, 7, 7);
							}
						}else{*/
							for(int k = 0;k < stones;k++){
								gr.setColor(k == searchFor ? pathColor : Color.DARK_GRAY);
								if(lockFlag && k != searchFor){
									gr.fillRect(offset+x+k*8+1, y+3, 6, 2);
								}else{
									gr.fillOval(offset+x+k*8, y, 7, 7);
								}
							}
						//}
						y += 8;
					}
					y -= (slots-1)*8;
					gr.setColor(pathColor);
					gr.drawRoundRect(x-17, y-2, 33, 27, 10, 10);
				}
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
