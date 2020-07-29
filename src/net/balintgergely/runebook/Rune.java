package net.balintgergely.runebook;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;

import net.balintgergely.runebook.RuneModel.Path;
import net.balintgergely.runebook.RuneModel.Runestone;
import net.balintgergely.runebook.RuneModel.Statstone;
import net.balintgergely.runebook.RuneModel.Stone;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;

public final class Rune implements Comparator<Stone>{
	public final Path primaryPath;
	public final Path secondaryPath;
	public final List<Stone> stoneList;
	public Rune(Stone... paramStones){
		this(Arrays.asList(paramStones));
	}
	public Rune(Collection<Stone> stoneCollection){
		RuneModel model = null;
		Path alphaPath = null,bravoPath = null;
		int inAlpha = 0,inBravo = 0,inStat = 0;
		int count = 0;
		Stone[] stoneArray = new Stone[stoneCollection.size()];
		for(Stone stone : stoneCollection){
			stoneArray[count++] = stone;
			if(stone instanceof Runestone){
				Path pt = ((Runestone)stone).path;
				if(model == null){
					model = pt.model;
				}else if(model != pt.model){
					throw new IllegalArgumentException("Model mismatch!");
				}
				if(inAlpha == 0){
					alphaPath = pt;
					inAlpha = 1;
				}else if(pt == alphaPath){
					inAlpha++;
				}else if(inBravo == 0){
					bravoPath = pt;
					inBravo = 1;
				}else if(pt == bravoPath){
					inBravo++;
				}else{
					throw new IllegalArgumentException("Third path not allowed!");
				}
			}else if(stone instanceof Statstone){
				RuneModel lm = ((Statstone)stone).model;
				if(model == null){
					model = lm;
				}else if(model != lm){
					throw new IllegalArgumentException("Model mismatch!");
				}
				inStat++;
			}else{
				throw new IllegalArgumentException("Unsupported stone type!");
			}
		}
		if(count != stoneArray.length){
			throw new ConcurrentModificationException();
		}
		if(inAlpha < inBravo){
			primaryPath = bravoPath;
			secondaryPath = alphaPath;
			int k = inAlpha;
			inAlpha = inBravo;
			inBravo = k;
			alphaPath = primaryPath;
			bravoPath = secondaryPath;
		}else{
			primaryPath = alphaPath;
			secondaryPath = bravoPath;
		}
		int offset = alphaPath.getSlotCount();
		int stones = model.getStatSlotCount();
		if(model != bravoPath.model || inAlpha != offset || inBravo != 2 || inStat != stones){
			throw new IllegalArgumentException("Model mismatch!");
		}
		Arrays.sort(stoneArray, this);
		offset += 2;
		a: for(int i = 0;i < stones;i++){
			int stonesInSlot = model.getStatSlotLength(i);
			Stone stone = stoneArray[i+offset];
			for(int k = 0;k < stonesInSlot;k++){
				if(model.getStatstone(i, k) == stone){
					continue a;
				}
			}
			throw new IllegalArgumentException("Illegal stat stone config");
		}
		stoneList = List.of(stoneArray);
	}
	public int statStoneOffset(){
		return stoneList.size()-primaryPath.model.getStatSlotCount();
	}
	public Runestone getKeystone(){
		return (Runestone)stoneList.get(0);
	}
	public JSMap toJSMap(){
		JSList perkJSList = new JSList(stoneList.size());
		for(Stone perk : stoneList){
			perkJSList.add(perk.id);
		}
		return new JSMap().put("selectedPerkIds",perkJSList,"primaryStyleId",primaryPath.id,"subStyleId",secondaryPath.id);
	}
	@Override
	public int hashCode() {
		return stoneList.hashCode();
	}
	@Override
	public boolean equals(Object that){
		if(that instanceof Rune){
			List<Stone> lst = ((Rune)that).stoneList;
			int len = lst.size();
			if(len == stoneList.size()){
				for(int i = 0;i < len;i++){
					if(stoneList.get(i) != lst.get(i)){
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	@Override
	public int compare(Stone o1, Stone o2) {
		if(o1 instanceof Runestone){
			if(o2 instanceof Runestone){
				Runestone s1 = (Runestone)o1;
				Runestone s2 = (Runestone)o2;
				if(s1.path == s2.path){
					switch(Integer.compare(s1.slot, s2.slot)){
					case -1:return -1;
					case 1:return 1;
					case 0:throw new IllegalArgumentException();//We are bound to find one of these if they exist.
					}
				}else{
					return s1.path == primaryPath ? -1 : 1;
				}
			}
			return -1;
		}else if(o2 instanceof Runestone){
			return 1;
		}else{
			return Integer.compare(((Statstone)o1).sortingAssist, ((Statstone)o2).sortingAssist);
		}
	}
}
