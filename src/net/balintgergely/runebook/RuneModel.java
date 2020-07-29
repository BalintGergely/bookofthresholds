package net.balintgergely.runebook;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.swing.ImageIcon;

import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;

public class RuneModel {
	private static final String STAT_MODS_ROUTE = "perk-images/StatMods/";
	private static final String[] STAT_ICONS = {
			"StatModsHealthScalingIcon.png",
			"StatModsArmorIcon.png",
			"StatModsMagicResIcon.png",
			"StatModsAttackSpeedIcon.png",
			"StatModsCDRScalingIcon.png",
			"StatModsAdaptiveForceIcon.png",
	};
	private static final String[] STAT_LOCALE = {
			"Health",
			"Armor",
			"Magic Resistance",
			"Attack Speed",
			"Cooldown Reduction",
			"Adaptive Force",
	};
	private static final int[] STAT_IDS = {
			5001,//0 Health
			5002,//1 Armor
			5003,//2 Magic resist
			5005,//3 Attack Speed
			5007,//4 CDR
			5008,//5 Adaptive force
	};
	private static final int[] SORTING_ASSIST = {
			3,//0 Health
			2,//1 Armor
			2,//2 Magic resist
			0,//3 Attack Speed
			0,//4 CDR
			1,//5 Adaptive force
	};
	private static final int[][] STAT_MODEL = {
			{5,3,4},
			{5,1,2},
			{0,1,2}
	};
	private static final int STAT_MOD_STATE_COUNT = 27;//3*3*3
	public static final int PATH_SIZE = 32,KEYSTONE_SIZE = 48,RUNESTONE_SIZE = 32,STAT_MOD_SIZE = 24;
	private Path[] paths;
	private Statstone[] statIcons;
	public final Map<Integer,Stone> fullMap;
	public RuneModel(JSList model,Function<String,BufferedImage> imageProvider){
		paths = new Path[model.size()];
		HashMap<Integer,Stone> fm = new HashMap<>();
		for(int i = 0;i < paths.length;i++){
			JSMap pathModel = model.getJSMap(i);
			BufferedImage img = imageProvider.apply(pathModel.getString("icon"));
			Path pt = new Path(i,this,pathModel,imageProvider,img,AssetManager.averagePixels(img),fm);
			paths[i] = pt;
			fm.put(pt.id, pt);
		}
		statIcons = new Statstone[STAT_ICONS.length];
		for(int i = 0;i < STAT_ICONS.length;i++){
			BufferedImage img = imageProvider.apply(STAT_MODS_ROUTE+STAT_ICONS[i]);
			int id = STAT_IDS[i];
			Statstone sts = new Statstone(this, resize(STAT_MOD_SIZE,img), STAT_LOCALE[i], AssetManager.averagePixels(img), id, SORTING_ASSIST[i]);
			statIcons[i] = sts;
			fm.put(id, sts);
		}
		fullMap = Map.copyOf(fm);
	}
	public int getPathCount(){
		return paths.length;
	}
	public Path getPath(int path){
		return paths[path];
	}
	public int getStatSlotCount(){
		return STAT_MODEL.length;
	}
	public int getStatSlotLength(int slot){
		return STAT_MODEL[slot].length;
	}
	public int getStatModStateCount(){
		return STAT_MOD_STATE_COUNT;
	}
	public Statstone getStatstone(int slot,int index){
		return statIcons[STAT_MODEL[slot][index]];
	}
	public static abstract class Stone extends ImageIcon{
		private static final long serialVersionUID = 1L;
		public final int id;
		private Stone(Image image,String name,int id){
			super(image,name);
			this.id = id;
		}
	}
	public static class Path extends Stone{
		public final int order;
		public final RuneModel model;
		public final Color color;
		public int getSlotCount(){
			return slots.length;
		}
		public int getStoneCountInSlot(int slot){
			return slots[slot].length;
		}
		public Runestone getStone(int slot,int index){
			return slots[slot][index];
		}
		public boolean isValidStone(int slot,int index){
			return slot < slots.length && index < slots[slot].length;
		}
		private Runestone[][] slots;
		private static final long serialVersionUID = 1L;
		public Path(int order,RuneModel rm,JSMap model,Function<String,BufferedImage> imageProvider,Image img,Color color,Map<Integer,Stone> m){
			super(img,model.getString("name"),model.getInt("id"));
			this.order = order;
			this.model = rm;
			JSList sl = model.getJSList("slots");
			slots = new Runestone[sl.size()][];
			for(int i = 0;i < slots.length;i++){
				JSList slist = sl.getJSMap(i).getJSList("runes");
				Runestone[] sb = new Runestone[slist.size()];
				for(int n = 0;n < sb.length;n++){
					JSMap stone = slist.getJSMap(n);
					Runestone rs = new Runestone(n,this,
resize(i == 0 ? KEYSTONE_SIZE : RUNESTONE_SIZE,imageProvider.apply(stone.getString("icon"))),stone.getString("name"),stone.getInt("id"),i);
					sb[n] = rs;
					m.put(rs.id, rs);
				}
				slots[i] = sb;
			}
			this.color = color;
		}
	}
	public static class Runestone extends Stone{
		private static final long serialVersionUID = 1L;
		public final Path path;
		public final int slot;
		public final int index;
		private Runestone(int index,Path path,Image image,String name,int id,int slot){
			super(image,name,id);
			this.index = index;
			this.path = path;
			this.slot = slot;
		}
	}
	public static class Statstone extends Stone{
		private static final long serialVersionUID = 1L;
		public final RuneModel model;
		public final Color color;
		public final int sortingAssist;
		private Statstone(RuneModel model,Image image,String name,Color color,int id,int sortingAssist){
			super(image,name,id);
			this.color = color;
			this.model = model;
			this.sortingAssist = sortingAssist;
		}
		public int indexOfInSlot(int slot){
			switch(id){
			case 5001:
			case 5008:return 0;
			case 5002:
			case 5005:return 1;
			case 5003:
			case 5007:return 2;
			}
			return -1;
		}
	}
	private static Image resize(int dim,Image img){
		return img.getScaledInstance(dim, dim, Image.SCALE_SMOOTH);
	}
}
