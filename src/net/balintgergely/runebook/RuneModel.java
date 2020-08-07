package net.balintgergely.runebook;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.swing.ImageIcon;

import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;

public class RuneModel {
	private static final String STAT_MODS_ROUTE = "perk-images/StatMods/";
	private static final int[][] STAT_MODEL = {
			{5,3,4},
			{5,1,2},
			{0,1,2}
	};
	private static final int STAT_MOD_STATE_COUNT = 27;//3*3*3
	public static final int PATH_SIZE = 32,KEYSTONE_SIZE = 48,RUNESTONE_SIZE = 32,STAT_MOD_SIZE = 24;
	private Path[] paths;
	private Statstone[] statStones;
	public final Map<Integer,Stone> fullMap;
	public final Rune foundation;
	public RuneModel(JSList model,Function<String,BufferedImage> ip,AssetManager mg){
		paths = new Path[model.size()];
		HashMap<Integer,Stone> fm = new HashMap<>();
		for(byte i = 0;i < paths.length;i++){
			JSMap pathModel = model.getJSMap(i);
			BufferedImage img = ip.apply(pathModel.getString("icon"));
			Path pt = new Path(i,this,pathModel,ip,img,AssetManager.averagePixels(img),fm);
			paths[i] = pt;
			fm.put(pt.id, pt);
		}
		statStones = new Statstone[]{//Icon image			Name		Id	SA	MIN	MAX
				sts(0, ip, "StatModsHealthScalingIcon.png",	mg.z.getString("statHp"),	5001, 2, 2, 0),
				sts(1, ip, "StatModsArmorIcon.png",			mg.z.getString("statAr"),	5002, 1, 2, 1),
				sts(2, ip, "StatModsMagicResIcon.png",		mg.z.getString("statMr"),	5003, 1, 2, 2),
				sts(3, ip, "StatModsAttackSpeedIcon.png",	mg.z.getString("statAr"),	5005, 0, 0, 1),
				sts(4, ip, "StatModsCDRScalingIcon.png",	mg.z.getString("statCd"),	5007, 0, 0, 2),
				sts(5, ip, "StatModsAdaptiveForceIcon.png",	mg.z.getString("statAf"),	5008, 0, 1, 0),
		};
		for(Statstone st : statStones){
			fm.put(st.id, st);
		}
		fullMap = Collections.unmodifiableMap(fm);//Faster.
		foundation = new Rune(this, null, null, Collections.emptyList());
	}
	private Statstone sts(int statId,
			Function<String,BufferedImage> imageProvider,String imageName,String name,int id,int leastSlot,int mostSlot,int index){
		BufferedImage image = imageProvider.apply(STAT_MODS_ROUTE+imageName);
		return new Statstone(
				statId,
				this,
				resize(STAT_MOD_SIZE,image),
				AssetManager.averagePixels(image),
				name, id, leastSlot, mostSlot, index);
	}
	public int getPathCount(){
		return paths.length;
	}
	public Path getPath(int path){
		return paths[path];
	}
	public Path pathForName(String name){
		for(Path pt : paths){
			if(pt.getDescription().equalsIgnoreCase(name)){
				return pt;
			}
		}
		return null;
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
		return statStones[STAT_MODEL[slot][index]];
	}
	public static abstract class Stone extends ImageIcon{
		private static final long serialVersionUID = 1L;
		public final int id;
		public final byte order;
		private Stone(byte order,Image image,String name,int id){
			super(image,name);
			this.id = id;
			this.order = order;
		}
	}
	public static class Path extends Stone{
		public final RuneModel model;
		public final Color color;
		public final String key;
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
		public Path(byte order,RuneModel rm,JSMap model,Function<String,BufferedImage> imageProvider,Image img,Color color,Map<Integer,Stone> m){
			super(order,img,model.getString("name"),model.getInt("id"));
			this.model = rm;
			this.key = model.getString("key");
			JSList sl = model.getJSList("slots");
			slots = new Runestone[sl.size()][];
			for(int i = 0;i < slots.length;i++){
				JSList slist = sl.getJSMap(i).getJSList("runes");
				Runestone[] sb = new Runestone[slist.size()];
				for(byte n = 0;n < sb.length;n++){
					JSMap stone = slist.getJSMap(n);
					Runestone rs = new Runestone(n,
							this,
							resize(i == 0 ? KEYSTONE_SIZE : RUNESTONE_SIZE,imageProvider.apply(stone.getString("icon"))),
							stone.getString("name"),
							stone.getString("longDesc"),
							stone.getInt("id"),i);
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
		public final String description;
		private Runestone(byte order,Path path,Image image,String name,String description,int id,int slot){
			super(order,image,name.toUpperCase(),id);
			this.path = path;
			this.slot = slot;
			this.description = description;
		}
		@Override
		public String toString(){
			return "<html><h2>"+super.getDescription()+"</h2>"+description+"</html>";
		}
	}
	public static class Statstone extends Stone{
		private static final long serialVersionUID = 1L;
		public final RuneModel model;
		public final Color color;
		public final byte minSlot;
		public final byte maxSlot;
		public final byte statId;
		private Statstone(int statId,RuneModel model,Image image,Color color,String name,int id,int leastSlot,int mostSlot,int index){
			super((byte)index,image,name,id);
			this.color = color;
			this.model = model;
			this.minSlot = (byte)leastSlot;
			this.maxSlot = (byte)mostSlot;
			this.statId = (byte)statId;
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
	public Rune parseRune(JSMap map){
		JSList perks = JSON.toJSList(map.peek("selectedPerkIds"));
		ArrayList<Stone> stoneList = new ArrayList<>(perks.size());
		Path primaryPath = (Path)fullMap.get(map.peekInt("primaryStyleId"));
		Path secondaryPath = (Path)fullMap.get(map.peekInt("subStyleId"));
		int len = perks.size();
		for(int i = 0;i < len;i++){
			stoneList.add(fullMap.get(perks.getInt(i)));
		}
		return new Rune(this, primaryPath, secondaryPath, stoneList);
	}
}
