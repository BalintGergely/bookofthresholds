package net.balintgergely.runebook;

import java.util.ArrayList;

import net.balintgergely.runebook.RuneModel.Path;
import net.balintgergely.runebook.RuneModel.Runestone;
import net.balintgergely.runebook.RuneModel.Stone;
import net.balintgergely.runebook.RuneModel.Statstone;

public final class Mobafire {private Mobafire(){}
	/**
	 * Oh my god why does this exist and why does this make zero sense?
	 * Why could they not have used the original system?
	 * This is not connected to the actual rune ids.
	 * Some ids are mysteriously skipped...?
	 */
	private static final int[] ID_MAP = new int[]{
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
	private static final int[] STAT_ID_MAP = new int[]{
			1,5008,//???????
			2,5005,//???
			3,5007,//????
			4,5002,//??
			5,5003,//??
			6,5001,//?????
	};
	/**
	 * Converts the specified mobafire rune builder url to a list of rune stones.
	 */
	public static Rune resolveLink(RuneModel model,String url){
		int splix = url.lastIndexOf("rune=");
		if(splix >= 0){
			url = url.substring(splix+5);
		}
		String[] parts = url.split(":");
		int[] currentMapping = ID_MAP;
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
						case "resolve":Path pt = model.pathForName(str);
							if(pt != null){
								if(keystoneMissing){
									alpha = bravo;
								}
								bravo = pt;
							}
							currentMapping = ID_MAP;continue main;
						case "shards":currentMapping = STAT_ID_MAP;continue main;
						default:continue main;
						}
					}
				}
				if(len < 3){
					int value = Integer.parseInt(str);
					for(int i = 0;i < currentMapping.length;i += 2){
						if(currentMapping[i] == value){
							Stone st = model.fullMap.get(currentMapping[i+1]);
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
		return new Rune(model, alpha, bravo, stoneList);
	}
	public static String toURL(Rune rune){
		StringBuilder builder = new StringBuilder("https://www.mobafire.com/league-of-legends/rune-page-planner#&rune=");
		Path pt = rune.primaryPath;
		int index = 0;
		int limit = rune.stoneList.size();
		for(int i = 0;i < 2;i++){
			if(pt != null){
				if(i == 1){
					builder.append("::");
				}
				builder.append(pt.key);
				b: while(index < limit){
					Stone st = rune.stoneList.get(index);
					if(st instanceof Runestone){
						Runestone rs = (Runestone)st;
						if(rs.path == pt){
							int id = rs.id;
							for(int iter = 1;iter < ID_MAP.length;iter += 2){
								if(ID_MAP[iter] == id){
									builder.append(':');
									builder.append(ID_MAP[iter-1]);
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
				Statstone st = (Statstone)rune.stoneList.get(index);
				int id = st.id;
				for(int iter = 1;iter < ID_MAP.length;iter += 2){
					if(STAT_ID_MAP[iter] == id){
						builder.append(':');
						builder.append(ID_MAP[iter-1]);
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
