package net.balintgergely.runebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.TreeMap;

import net.balintgergely.util.ArrayListView;
import net.balintgergely.util.JSConvertible;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;

public final class RuneModel extends ArrayListView<RuneModel.Path>{
	private static final String STAT_MODS_ROUTE = "perk-images/StatMods/";
	private static final int[][] STAT_MODEL = {
			{5,3,4},
			{5,1,2},
			{0,1,2}
	};
	public final int statModPermutations = 27;//3*3*3
	public final long totalPermutationCount;
	private Statstone[] statStones;
	/**
	 * A map of all stones contained in this model.
	 */
	public final Map<Integer,Stone> stoneMap;
	/**
	 * The foundation rune. Contains no stones, but can be forged into any other rune.
	 */
	public final Rune foundation;
	public RuneModel(JSList model,PropertyResourceBundle bundle){
		super(new Path[model.size()]);
		if(data.length < 2){
			throw new IllegalArgumentException();
		}
		HashMap<Integer,Stone> fm = new HashMap<>();
		for(byte i = 0;i < data.length;i++){
			ptp(i, model, fm);
		}
		statStones = new Statstone[]{//Icon image			Name		Id	SA	MIN	MAX
				sts(0, "StatModsHealthScalingIcon.png", bundle.getString("statHp"),	5001, 2, 2, 0),
				sts(1, "StatModsArmorIcon.png",			bundle.getString("statAr"),	5002, 1, 2, 1),
				sts(2, "StatModsMagicResIcon.png",		bundle.getString("statMr"),	5003, 1, 2, 2),
				sts(3, "StatModsAttackSpeedIcon.png",	bundle.getString("statAs"),	5005, 0, 0, 1),
				sts(4, "StatModsCDRScalingIcon.png",	bundle.getString("statCd"),	5007, 0, 0, 2),
				sts(5, "StatModsAdaptiveForceIcon.png",	bundle.getString("statAf"),	5008, 0, 1, 0),
		};
		for(Statstone st : statStones){
			fm.put(st.id, st);
		}
		stoneMap = Collections.unmodifiableMap(fm);//Faster.
		long tpc = 0;
		for(Path pt : data){
			tpc += pt.totalPermutationCount;
		}
		this.totalPermutationCount = tpc;
		foundation = new Rune(this);
	}
	private Path ptp(byte order,JSList parseList,Map<Integer,Stone> sm){
		Path pt = data[order];
		if(pt == null){
			JSMap pathModel = parseList.getJSMap(order);
			pt = new Path(order, this, parseList, pathModel, sm);
			sm.put(pt.id, pt);
		}
		return pt;
	}
	private Statstone sts(int statId,String imageName,String name,int id,int leastSlot,int mostSlot,int index){
		return new Statstone(
				statId,
				this,
				STAT_MODS_ROUTE+imageName,
				name, id, leastSlot, mostSlot, index);
	}
	public Path pathForKey(String name){
		for(Path pt : data){
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
	public static abstract class Stone implements JSConvertible{
		public final String imageRoute;
		public final String name;
		public final int id;
		public final byte order;
		private Stone(byte order,String imageRoute,String name,int id){
			this.imageRoute = imageRoute;
			this.name = name;
			this.id = id;
			this.order = order;
		}
		@Override
		public Object convert() {
			return Integer.valueOf(id);
		}
	}
	public static class Path extends Stone{
		public final RuneModel model;
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
		public Path(
				byte order,
				RuneModel rm,
				JSList parseList,
				JSMap model,
				Map<Integer,Stone> stoneMap){
			super(order,model.getString("icon"),model.getString("name"),model.getInt("id"));
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
								stone.getString("icon"),
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
				rm.data[order] = this;
			}
			long tpp = 0;
			for(byte i = 0;i < rm.data.length;i++){
				if(i != order){
					Path pt = rm.ptp(i, parseList, stoneMap);
					tpp += pp*pt.secondaryPermutationCount;
				}
			}
			this.totalPermutationCount = tpp;
		}
	}
	public static class Runestone extends Stone{
		//private static final Pattern uikitPattern = Pattern.compile('')
		public final Path path;
		public final int slot;
		public final String description;
		private Runestone(byte order,Path path,String imageRoute,String name,String description,int id,int slot){
			super(order,imageRoute,name.toUpperCase(),id);
			this.path = path;
			this.slot = slot;
			this.description = "<html><h2>"+name+"</h2>"+description+"</html>";
		}
	}
	public static class Statstone extends Stone{
		public final RuneModel model;
		public final byte minSlot;
		public final byte maxSlot;
		public final byte statId;
		private Statstone(int statId,RuneModel model,String imageRoute,String name,int id,int leastSlot,int mostSlot,int index){
			super((byte)index,imageRoute,name,id);
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
	public Rune parseRune(JSMap map){
		JSList perks = JSON.toJSList(map.peek("selectedPerkIds"));
		ArrayList<Stone> stoneList = new ArrayList<>(perks.size());
		Path primaryPath = (Path)stoneMap.get(map.peekInt("primaryStyleId"));
		Path secondaryPath = (Path)stoneMap.get(map.peekInt("subStyleId"));
		int len = perks.size();
		for(int i = 0;i < len;i++){
			stoneList.add(stoneMap.get(perks.getInt(i)));
		}
		return Rune.ofStones(this, primaryPath, secondaryPath, stoneList);
	}
	public Rune parseRune(String str){
		str = str.toLowerCase(Locale.ROOT);
		TreeMap<Integer,Stone> stones = new TreeMap<>();
		for(Stone st : stoneMap.values()){//Link stones to their index of occurrence.
			String word = st.name.toLowerCase(Locale.ROOT);
			int index = str.indexOf(word);
			int length = word.length();
			while(index >= 0){
				stones.putIfAbsent(index, st);
				index = str.indexOf(word, index+length);
			}
		}
		return Rune.ofStones(this, null, null, stones.values());
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
		return indexOf(o);
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
	public boolean contains(Object o) {
		return o instanceof Path && ((Path)o).model == this;
	}
}
