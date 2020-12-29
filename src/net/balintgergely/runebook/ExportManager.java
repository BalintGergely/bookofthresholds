package net.balintgergely.runebook;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.balintgergely.runebook.Rune.RuneException;
import net.balintgergely.runebook.RuneModel.Path;
import net.balintgergely.runebook.RuneModel.Runestone;
import net.balintgergely.runebook.RuneModel.Stone;
import net.balintgergely.util.OrdinalMap;
import net.balintgergely.util.SimpleOrdinalMap;
import net.balintgergely.runebook.RuneModel.Statstone;

public final class ExportManager {
	/**
	 * You can Ctrl+A then Ctrl+C a guide in Mobafire and this pattern will recognize it.
	 */
	//private static final Pattern PAGE_PATTERN =
//Pattern.compile("^(?<champion>[a-zA-Z ]*) BUILD GUIDE.*^RUNES:(?<name>[^\\n]*)?$(?<build>.*)^SPELLS:",Pattern.DOTALL | Pattern.MULTILINE);
	/**
	 * Mobafire rune builder links are recognized by this pattern.
	 */
	private static final Pattern MOBAFIRE_RUNE_PLANNER = Pattern.compile("#&rune=(?<code>[:0-9a-zA-Z]*)",Pattern.MULTILINE);
	/**
	 * A string comparator prioritizing longer strings over shorter ones.
	 */
	private static final Comparator<String> LONG_STRING_FIRST =
			Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder());
	/**
	 * Oh my god why does this exist and why does this make zero sense?
	 * Why could they not have used the original system?
	 * This is not connected to the actual rune ids.
	 * Some ids are mysteriously skipped...?
	 */
	private static final int[] MOBAFIRE_ID_MAP = new int[]{
			//Precision Keystones
			1,8005,
			2,8008,
			3,8021,
			//Domination Keystones
			4,8112,
			5,8124,
			6,8128,
			//Sorcery Keystones
			7,8214,
			8,8229,
			9,8230,
			//Resolve Keystones
			10,8437,
			11,8465,
			12,8439,
			//Inspiration Keystones
			13,8351,
			14,8360,
			//15?
			//Precision
			16,9101,
			17,9111,
			18,8009,
			19,9104,
			20,9105,
			21,9103,
			22,8014,
			23,8017,
			24,8299,
			//Domination
			25,8136,
			26,8120,
			27,8138,
			28,8126,
			29,8139,
			30,8143,
			31,8135,
			32,8134,
			33,8105,
			//Sorcery
			34,8224,
			35,8226,
			//36?
			37,8210,
			38,8234,
			39,8233,
			40,8237,
			41,8232,
			42,8236,
			//Resolve
			43,8242,
			44,8446,
			45,8463,
			//46?
			//47?
			48,8429,
			49,8451,
			50,8453,
			51,8444,
			//Inspiration
			52,8306,
			53,8345,
			54,8313,
			55,8304,
			56,8321,
			57,8316,
			58,8347,
			59,8410,
			//Extras: Why do these have higher ids than the above ones?
			//60-65?
			66,8473,//Bone Plating
			67,8352,//Time Warp Tonic
			//68?
			69,8010,//Conqueror
			70,8275,//Nimbus Cloak
			71,9923,//Hail of Blades
			72,8106,//Ultimate Hunter
			73,8401,//Shield Bash
			74,8358,//Prototype Omnistone
	};
	/**
	 * &#$@%!!! Why can't you people use THE ACTUAL ORDERING DEFINED BY RIOT GAMES?!?!?!?!?!?!
	 * This one below is sooooo messed up...
	 */
	private static final int[] MOBAFIRE_STAT_ID_MAP = new int[]{
			1,5008,//???????
			2,5005,//???
			3,5007,//????
			4,5002,//??
			5,5003,//??
			6,5001,//?????
	};
	AssetManager mgr;
	OrdinalMap<String,Object> enWordList;
	OrdinalMap<String,Object> natWordList;
	String[][] roleData = new String[5][];
	ExportManager(AssetManager mgr){
		this.mgr = mgr;
		HashMap<String,Object> temp = new HashMap<>(mgr.championList.size()+mgr.runeModel.stoneMap.size());
		for(Champion champ : mgr.championList){
			temp.put(champ.getName().toLowerCase(mgr.locale),champ);
		}
		for(Stone stone : mgr.runeModel.stoneMap.values()){
			temp.put(stone.name.toLowerCase(mgr.locale),stone);
		}
		boolean isEnglish = mgr.englishRuneModel == mgr.runeModel;
		HashSet<String> rn = new HashSet<>();
		for(int i = 0;i < 5;i++){
			switch(1 << i){
			case AssetManager.TOP:rn.add("top");break;
			case AssetManager.MIDDLE:rn.add("mid");break;
			case AssetManager.BOTTOM:rn.add("bot");break;
			case AssetManager.JUNGLE:rn.add("jungle");break;
			case AssetManager.SUPPORT:rn.add("support");break;
			}
			rn.add(mgr.getRoleName(i).toLowerCase(mgr.locale));
			roleData[i] = rn.toArray(new String[rn.size()]);
			rn.clear();
		}
		natWordList = SimpleOrdinalMap.copyOf(temp, LONG_STRING_FIRST, String.class);
		if(isEnglish){
			enWordList = null;
		}else{
			temp.clear();
			for(Map.Entry<String, Champion> champ : mgr.championsByEnglishName.entrySet()){
				temp.put(champ.getKey().toLowerCase(Locale.ENGLISH), champ.getValue());
			}
			for(Stone stone : mgr.englishRuneModel.stoneMap.values()){
				temp.put(stone.name.toLowerCase(Locale.ENGLISH),mgr.runeModel.stoneMap.get(stone.id));
			}
			enWordList = SimpleOrdinalMap.copyOf(temp, LONG_STRING_FIRST, String.class);
		}
	}
	/**
	 * Converts the specified mobafire rune builder url to a list of rune stones.
	 */
	public Build resolveBuild(String data){
		Matcher mt;
		if((mt = MOBAFIRE_RUNE_PLANNER.matcher(data)).find()){
			String[] parts = mt.group("code").split(":");
			int[] currentMapping = MOBAFIRE_ID_MAP;
			ArrayList<Stone> stoneList = new ArrayList<>();
			boolean keystoneMissing = true;
			Path alpha = null,bravo = null;
			main: for(String str : parts){
				if(!str.isBlank()){
					str = str.toLowerCase().strip();
					int len = str.length();
					for(int i = 0;i < len;i++){
						if("0123456789".indexOf(str.charAt(i)) < 0){
							switch(str){
							case "precision":
							case "sorcery":
							case "domination":
							case "inspiration":
							case "resolve":Path pt = mgr.runeModel.pathForKey(str);
								if(pt != null){
									if(keystoneMissing){
										alpha = bravo;
									}
									bravo = pt;
								}
								currentMapping = MOBAFIRE_ID_MAP;continue main;
							case "shards":currentMapping = MOBAFIRE_STAT_ID_MAP;continue main;
							default:continue main;
							}
						}
					}
					if(len < 3){
						int value = Integer.parseInt(str);
						for(int i = 0;i < currentMapping.length;i += 2){
							if(currentMapping[i] == value){
								Stone st = mgr.runeModel.stoneMap.get(currentMapping[i+1]);
								if(st != null){
									if(st instanceof Runestone && ((Runestone)st).slot == 0){
										keystoneMissing = false;
										alpha = ((Runestone)st).path;
									}
									stoneList.add(st);
								}
							}
						}
					}
				}
			}
			return new Build("Mobafire Import", null, Rune.ofStones(mgr.runeModel, alpha, bravo, stoneList), (byte)0, System.currentTimeMillis());
		}else{
			String natData = data.toLowerCase(mgr.locale);
			BitSet cache = new BitSet(natData.length());
			TreeMap<Integer,Object> stoneList = stoneSeek(natData, natWordList, cache);
			Champion champion = championSeek(natData, natWordList);
			if(enWordList != null){
				String enData = data.toLowerCase(Locale.ENGLISH);
				cache.clear();
				TreeMap<Integer,Object> enStoneList = stoneSeek(enData, enWordList, cache);
				if(stoneList.size() < enStoneList.size()){
					stoneList = enStoneList;
					Champion ch = championSeek(enData, enWordList);
					if(ch != null){
						champion = ch;
					}
					natData = enData;
				}
			}
			Rune rune = null;
			do{
				try{
					rune = Rune.ofStones(mgr.runeModel, null, null, stoneList.values());
				}catch(RuneException e){
					Iterator<Object> obj = e.stoneA instanceof Statstone ?
							stoneList.descendingMap().values().iterator() : stoneList.values().iterator();
					a:{
						while(obj.hasNext()){
							Object o = obj.next();
							if(e.stoneA == o || e.stoneB == o){
								obj.remove();
								break a;
							}
						}
						stoneList.pollFirstEntry();
					}
				}
			}while(rune == null);
			return new Build("", champion, rune, roleSeek(natData), System.currentTimeMillis());
		}
	}
	private static TreeMap<Integer,Object> stoneSeek(String str,OrdinalMap<String,Object> map,BitSet cache){
		TreeMap<Integer,Object> stoneList = new TreeMap<>();
		int size = map.size();
		for(int i = 0;i < size;i++){
			Object value = map.getValue(i);
			if(value instanceof Stone){
				String name = map.getKey(i);
				int index = 0,len = name.length();
				while((index = str.indexOf(name, index)) >= 0){
					int sb = cache.nextSetBit(index);
					if(sb < 0 || sb >= index+len){
						stoneList.put(index, value);
						cache.set(index, index+len);
					}
					index += len;
				}
			}
		}
		return stoneList;
	}
	private static Champion championSeek(String str,OrdinalMap<String,Object> map){
		int size = map.size();
		int minIndex = Integer.MAX_VALUE;
		Champion champion = null;
		for(int i = 0;i < size;i++){
			Object value = map.getValue(i);
			if(value instanceof Champion){
				String name = map.getKey(i);
				int index = str.indexOf(name);
				if(index >= 0 && minIndex > index){
					champion = (Champion)value;
					minIndex = index;
				}
			}
		}
		return champion;
	}
	private byte roleSeek(String str){
		byte cumulative = 0;
		for(int i = 0;i < 5;i++){
			a: for(String keyword : roleData[i]){
				if(str.contains(keyword)){
					cumulative |= (1 << i);
					break a;
				}
			}
		}
		return cumulative;
	}
	public static String toMobafireURL(Rune rune){
		StringBuilder builder = new StringBuilder("https://www.mobafire.com/league-of-legends/rune-page-planner#&rune=");
		Path pt = rune.primaryPath;
		int index = 0;
		int limit = rune.size();
		for(int i = 0;i < 2;i++){
			if(pt != null){
				if(i == 1){
					builder.append("::");
				}
				builder.append(pt.key);
				b: while(index < limit){
					Stone st = rune.get(index);
					if(st instanceof Runestone){
						Runestone rs = (Runestone)st;
						if(rs.path == pt){
							int id = rs.id;
							for(int iter = 1;iter < MOBAFIRE_ID_MAP.length;iter += 2){
								if(MOBAFIRE_ID_MAP[iter] == id){
									builder.append(':');
									builder.append(MOBAFIRE_ID_MAP[iter-1]);
									index++;
									continue b;
								}
							}
							throw new IllegalArgumentException("Unable to resolve rune.");
						}
					}
					break b;
				}
			}
			pt = rune.secondaryPath;
		}
		if(index < limit){
			builder.append(":::Shards");
			a: while(index < limit){
				Statstone st = (Statstone)rune.get(index);
				int id = st.id;
				for(int iter = 1;iter < MOBAFIRE_ID_MAP.length;iter += 2){
					if(MOBAFIRE_STAT_ID_MAP[iter] == id){
						builder.append(':');
						builder.append(MOBAFIRE_ID_MAP[iter-1]);
						index++;
						continue a;
					}
				}
				throw new IllegalArgumentException("Unable to resolve rune.");
			}
		}
		return builder.toString();
	}
}