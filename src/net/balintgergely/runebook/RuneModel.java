package net.balintgergely.runebook;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.ImageIcon;

import net.balintgergely.util.ArrayIterator;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;

public final class RuneModel extends AbstractList<RuneModel.Path>{
	private static final String STAT_MODS_ROUTE = "perk-images/StatMods/";
	private static final int[][] STAT_MODEL = {
			{5,3,4},
			{5,1,2},
			{0,1,2}
	};
	public static final int PATH_SIZE = 32,KEYSTONE_SIZE = 48,RUNESTONE_SIZE = 32,STAT_MOD_SIZE = 24;
	public final int statModPermutations = 27;//3*3*3
	public final long totalPermutationCount;
	private Path[] paths;
	private Statstone[] statStones;
	/**
	 * A map of all stones contained in this model.
	 */
	public final Map<Integer,Stone> stoneMap;
	/**
	 * The foundation rune. Contains no stones, but can be forged into any other rune.
	 */
	public final Rune foundation;
	public RuneModel(JSList model,Function<String,BufferedImage> ip,PropertyResourceBundle bundle){
		paths = new Path[model.size()];
		if(paths.length < 2){
			throw new IllegalArgumentException();
		}
		HashMap<Integer,Stone> fm = new HashMap<>();
		final BufferedImage replacement;
		if(ip == null){
			replacement = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			ip = s -> replacement;
		}else{
			replacement = null;
		}
		for(byte i = 0;i < paths.length;i++){
			ptp(replacement, i, model, ip, fm);
		}
		statStones = new Statstone[]{//Icon image			Name		Id	SA	MIN	MAX
				sts(0, ip, "StatModsHealthScalingIcon.png", bundle.getString("statHp"),	5001, 2, 2, 0),
				sts(1, ip, "StatModsArmorIcon.png",			bundle.getString("statAr"),	5002, 1, 2, 1),
				sts(2, ip, "StatModsMagicResIcon.png",		bundle.getString("statMr"),	5003, 1, 2, 2),
				sts(3, ip, "StatModsAttackSpeedIcon.png",	bundle.getString("statAs"),	5005, 0, 0, 1),
				sts(4, ip, "StatModsCDRScalingIcon.png",	bundle.getString("statCd"),	5007, 0, 0, 2),
				sts(5, ip, "StatModsAdaptiveForceIcon.png",	bundle.getString("statAf"),	5008, 0, 1, 0),
		};
		for(Statstone st : statStones){
			fm.put(st.id, st);
		}
		stoneMap = Collections.unmodifiableMap(fm);//Faster.
		long tpc = 0;
		for(Path pt : paths){
			tpc += pt.totalPermutationCount;
		}
		this.totalPermutationCount = tpc;
		foundation = new Rune(this);
	}
	private Path ptp(BufferedImage replacement,byte order,JSList parseList,Function<String,BufferedImage> imageProvider,Map<Integer,Stone> sm){
		Path pt = paths[order];
		if(pt == null){
			JSMap pathModel = parseList.getJSMap(order);
			BufferedImage img = imageProvider.apply(pathModel.getString("icon"));
			pt = new Path(order, this, parseList, pathModel, imageProvider, img, img == replacement ? null : AssetManager.averagePixels(img), sm);
			sm.put(pt.id, pt);
		}
		return pt;
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
	public Path pathForKey(String name){
		for(Path pt : paths){
			if(pt.key.equalsIgnoreCase(name)){
				return pt;
			}
		}
		return null;
	}
	@SuppressWarnings("static-method")
	public int getStatSlotCount(){
		return STAT_MODEL.length;
	}
	@SuppressWarnings("static-method")
	public int getStatSlotLength(int slot){
		return STAT_MODEL[slot].length;
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
		public final long totalPermutationCount;
		/**
		 * The number of different permutations this path has if it is selected as the secondary path.
		 */
		public final long secondaryPermutationCount;
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
		public Path(
				byte order,
				RuneModel rm,
				JSList parseList,
				JSMap model,
				Function<String,BufferedImage> imageProvider,
				BufferedImage img,
				Color color,
				Map<Integer,Stone> stoneMap){
			super(order,img,model.getString("name"),model.getInt("id"));
			long pp = rm.statModPermutations;
			{
				this.model = rm;
				this.key = model.getString("key");
				JSList sl = model.getJSList("slots");
				slots = new Runestone[sl.size()][];
				for(int i = 0;i < slots.length;i++){
					JSList slist = sl.getJSMap(i).getJSList("runes");
					Runestone[] sb = new Runestone[slist.size()];
					pp *= sb.length;
					for(byte n = 0;n < sb.length;n++){
						JSMap stone = slist.getJSMap(n);
						Runestone rs = new Runestone(n,
								this,
								resize(i == 0 ? KEYSTONE_SIZE : RUNESTONE_SIZE,imageProvider.apply(stone.getString("icon"))),
								stone.getString("name"),
								stone.getString("longDesc"),
								stone.getInt("id"),i);
						sb[n] = rs;
						stoneMap.put(rs.id, rs);
					}
					slots[i] = sb;
				}
				long secondaryPermutations = 0;
				for(int a = 1;a < slots.length;a++){
					for(int b = a+1;b < slots.length;b++){
						secondaryPermutations += slots[a].length*slots[b].length;
					}
				}
				secondaryPermutationCount = secondaryPermutations;
				this.color = color;
				rm.paths[order] = this;
			}
			long tpp = 0;
			for(byte i = 0;i < rm.paths.length;i++){
				if(i != order){
					Path pt = rm.ptp(img, i, parseList, imageProvider, stoneMap);
					tpp += pp*pt.secondaryPermutationCount;
				}
			}
			this.totalPermutationCount = tpp;
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
		return img == null ? null : img.getScaledInstance(dim, dim, Image.SCALE_SMOOTH);
	}
	public Rune parseRune(JSMap map){
		JSList perks = JSON.toJSList(map.peek("selectedPerkIds"));
		ArrayList<Stone> stoneList = new ArrayList<>(perks.size());
		Path primaryPath = (Path)stoneMap.get(map.peekInt("primaryStyleId"));
		Path secondaryPath = (Path)stoneMap.get(map.peekInt("subStyleId"));
		int len = perks.size();
		for(int i = 0;i < len;i++){
			stoneList.add(stoneMap.get(perks.getInt(i)));
		}
		return new Rune(this, primaryPath, secondaryPath, stoneList);
	}
	public Rune parseRune(String data){
		data = data.toLowerCase(Locale.ROOT);
		TreeMap<Integer,Stone> stones = new TreeMap<>();
		System.out.println(stoneMap.containsKey(5005));
		for(Stone st : stoneMap.values()){//Link stones to their index of occurrence.
			String word = st.getDescription().toLowerCase(Locale.ROOT);
			if(st.id == 5005){
				System.out.println("Gotcha");
			}
			int index = data.indexOf(word);
			int length = word.length();
			while(index >= 0){
				stones.putIfAbsent(index, st);
				index = data.indexOf(word, index+length);
			}
		}
		return new Rune(this, null, null, stones.values());
	}
	@Override
	public Spliterator<Path> spliterator() {
		return new ArrayIterator<>(paths);
	}
	@Override
	public void forEach(Consumer<? super Path> action) {
		for(Path pt : paths){
			action.accept(pt);
		}
	}
	@Override
	public Path get(int index) {
		return paths[index];
	}
	@Override
	public int indexOf(Object o) {
		if(o instanceof Path){
			Path p = (Path)o;
			if(p.model == this){
				return p.order;
			}
		}
		return -1;
	}
	@Override
	public int lastIndexOf(Object o) {
		if(o instanceof Path){
			Path p = (Path)o;
			if(p.model == this){
				return p.order;
			}
		}
		return -1;
	}
	@Override
	public Iterator<Path> iterator() {
		return new ArrayIterator<>(paths);
	}
	@Override
	public ListIterator<Path> listIterator() {
		return new ArrayIterator<>(paths);
	}
	@Override
	public ListIterator<Path> listIterator(int index) {
		return new ArrayIterator<>(paths, index);
	}
	@Override
	public boolean equals(Object o) {
		return this == o;
	}
	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
	@Override
	public int size() {
		return paths.length;
	}
	@Override
	public boolean isEmpty() {
		return false;
	}
	@Override
	public boolean contains(Object o) {
		return o instanceof Path && ((Path)o).model == this;
	}
}
