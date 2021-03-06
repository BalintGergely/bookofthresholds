package net.balintgergely.runebook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import net.balintgergely.runebook.RuneModel.Path;
import net.balintgergely.runebook.RuneModel.Runestone;
import net.balintgergely.runebook.RuneModel.Statstone;
import net.balintgergely.runebook.RuneModel.Stone;
import net.balintgergely.util.ArrayListView;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;

public final class Rune extends ArrayListView<Stone>{
	public static final Stone[] EMPTY_STONE_ARRAY = new Stone[0];
	public final RuneModel model;
	public final Path primaryPath;
	public final Path secondaryPath;
	public final boolean isComplete;
	Rune(RuneModel model){
		super(EMPTY_STONE_ARRAY);
		this.model = model;
		this.primaryPath = null;
		this.secondaryPath = null;
		this.isComplete = false;
	}
	private Rune(RuneModel model,Path primary,Path secondary,Stone[] stones,boolean isComplete){
		super(stones);
		this.model = model;
		this.primaryPath = primary;
		this.secondaryPath = secondary;
		this.isComplete = isComplete;
	}
	public static Rune ofStones(RuneModel model,Path primaryPath,Path secondaryPath,Collection<?> stoneCollection){
		int inPrimary = 0,inSecondary = 0;
		int count = 0;
		boolean primaryPathFound = false,secondaryPathFound = false;
		boolean pathLocked = primaryPath != null || secondaryPath != null;
		Stone[] stoneArray = new Stone[stoneCollection.size()];
		for(Object stone : stoneCollection){
			if(stone instanceof Runestone){
				Runestone rst = (Runestone)stone;
				stoneArray[count++] = rst;
				Path pt = rst.path;
				if(model == null){
					model = pt.model;
				}else if(model != pt.model){
					throw new RuneException("Model mismatch!",rst,pt);
				}
				if(pt == primaryPath){
					inPrimary++;
				}else if(pt == secondaryPath){
					inSecondary++;
				}else if(primaryPath == null){
					primaryPath = pt;
					inPrimary = 1;
				}else if(secondaryPath == null){
					secondaryPath = pt;
					inSecondary = 1;
				}else{
					if(primaryPathFound || secondaryPathFound){
						throw new RuneException("Third path not allowed!",primaryPath,secondaryPath);
					}else{
						throw new RuneException("Third path not allowed!",stoneArray[0],primaryPath);
					}
				}
				if(rst.slot == 0){
					if(pt == secondaryPath){
						if(pathLocked){
							throw new RuneException("Secondary path can not have a keystone!",stoneArray[0],pt);
						}
						secondaryPath = primaryPath;
						primaryPath = pt;
						int ct = inPrimary;
						inPrimary = inSecondary;
						inSecondary = ct;
						if(primaryPathFound != secondaryPathFound){
							primaryPathFound = secondaryPathFound;
							secondaryPathFound = !primaryPathFound;
						}
					}
					pathLocked = true;//pt == primaryPath and it should stay that way.
				}
			}else if(stone instanceof Statstone){
				RuneModel lm = ((Statstone)stone).model;
				stoneArray[count++] = (Stone)stone;
				if(model == null){
					model = lm;
				}else if(model != lm){
					throw new RuneException("Model mismatch!",((Stone)stone),null);
				}
			}else if(stone instanceof Path){
				Path pt = (Path)stone;
				if(pt == primaryPath){
					if(primaryPathFound){
						throw new RuneException("Duplicate stone: "+pt.key,pt,pt);
					}
					primaryPathFound = true;
				}else if(pt == secondaryPath){
					if(secondaryPathFound){
						throw new RuneException("Duplicate stone: "+pt.key,pt,pt);
					}
					secondaryPathFound = true;
				}else if(primaryPath == null){
					primaryPath = pt;
					primaryPathFound = true;
				}else if(secondaryPath == null){
					secondaryPath = pt;
					secondaryPathFound = true;
				}else{
					throw new RuneException("Three paths not supported!",pt,pt);
				}
			}else{
				throw new RuneException("Unsupported stone type!",null,null);
			}
		}
		if(model == null){
			throw new IllegalArgumentException("Unable to resolve model!");
		}
		if(count == 0){
			stoneArray = EMPTY_STONE_ARRAY;
		}else if(count != stoneArray.length){
			stoneArray = Arrays.copyOf(stoneArray, count);
		}
		if(!pathLocked && inSecondary > 2){
			Path pt = primaryPath;
			primaryPath = secondaryPath;
			secondaryPath = pt;
			int k = inPrimary;
			inPrimary = inSecondary;
			inSecondary = k;
		}
		if(inSecondary > 2){
			Stone alpha = null,bravo = null;
			for(Stone st : stoneArray){
				if(st instanceof Runestone && ((Runestone)st).path == secondaryPath){
					if(alpha == null){
						alpha = st;
					}else{
						bravo = st;
					}
				}
			}
			throw new RuneException("Can not have more than 2 stones in secondary path!",alpha,bravo);
		}
		if((secondaryPath != null && model != secondaryPath.model) || (primaryPath != null && model != primaryPath.model)){
			throw new RuneException("Model mismatch!",primaryPath,secondaryPath);
		}
		int statSlotCount = model.getStatSlotCount();
		Rune rn = new Rune(model, primaryPath, secondaryPath, stoneArray,
primaryPath != null && inPrimary == primaryPath.getSlotCount() && inSecondary == 2 && (count-inPrimary-inSecondary) == statSlotCount);
		//Order as follows: Keystone, Primary1,Primary2,Primary3,Secondary1,Secondary2,Finally statstones ordered by their slot.
		Arrays.sort(stoneArray, rn::compare);//This will throw a slot conflict exception if multiple stones fill the same slot.
		int statSlot = 0;
		for(int i = inPrimary+inSecondary;i < count;i++){
			Statstone stone = (Statstone)stoneArray[i];
			if(stone.minSlot > statSlot){
				statSlot = stone.minSlot;
			}else if(statSlot > stone.maxSlot){
				throw new RuneException("Illegal stat stone config!",stoneArray[inPrimary+inSecondary],stone);
			}
			statSlot++;
		}
		return rn;
	}
	public static Rune ofPermutationCode(RuneModel model,long permutationCode){
		if(permutationCode < 0 || permutationCode >= model.totalPermutationCount){
			throw new IllegalArgumentException();
		}
		Path path0 = null;
		for(Path pt : model){
			if(pt.totalPermutationCount > permutationCode){
				path0 = pt;
				break;
			}
			permutationCode -= pt.totalPermutationCount;
		}
		int primarySlots = path0.getSlotCount();
		long statModRemainder = permutationCode%model.statModPermutations;
		permutationCode /= model.statModPermutations;
		long multiplier = path0.totalPermutationCount/model.statModPermutations;
		Stone[] stoneArray = new Stone[primarySlots+2+model.getStatSlotCount()];
		int slot = 0;
		for(;slot < primarySlots;slot++){
			multiplier /= path0.getStoneCountInSlot(slot);
			stoneArray[slot] = path0.getStone(slot, (int)(permutationCode/multiplier));
			permutationCode %= multiplier;
		}
		Path path1 = null;
		for(Path pt : model){
			if(pt != path0){
				if(pt.secondaryPermutationCount > permutationCode){
					path1 = pt;
					break;
				}
				permutationCode -= pt.secondaryPermutationCount;
			}
		}
		int secondarySlots = path1.getSlotCount();
		a:for(int a = 1;true;a++){//Iterate all permutations for the second path. Stop when counter reaches zero.
			int slot0Length = path1.getStoneCountInSlot(a);
			for(int b = 0;b < slot0Length;b++){
				for(int c = a+1;c < secondarySlots;c++){
					int slot1Length = path1.getStoneCountInSlot(c);
					for(int d = 0;d < slot1Length;d++){
						if(permutationCode == 0){
							stoneArray[slot++] = path1.getStone(a, b);
							stoneArray[slot++] = path1.getStone(c, d);
							break a;
						}
						permutationCode--;
					}
				}
			}
		}
		multiplier = model.statModPermutations;
		int statSlots = model.getStatSlotCount();
		for(int i = 0;i < statSlots;i++){
			multiplier /= model.getStatSlotLength(i);
			stoneArray[slot++] = model.getStatstone(i,(int)(statModRemainder/multiplier));
			statModRemainder %= multiplier;
		}
		return new Rune(model, path0, path1, stoneArray, true);
	}
	/**
	 * Creates a new rune that is accepted by the Riot simulation server and contains all
	 * stones this rune does. Returns this rune if it is already accepted.
	 */
	public Rune fix(){
		if(isComplete){
			return this;
		}
		int offset;
		Path primary = primaryPath == null ? model.get(0) : primaryPath;
		Path secondary = secondaryPath == null ? model.get(primary.order == 0 ? 1 : 0) : secondaryPath;
		offset = primary.getSlotCount();
		Stone[] stoneArray = new Stone[offset+2+model.getStatSlotCount()];
		int indexNew = 0,indexOld = 0,limitOld = data.length;
		for(;indexNew < offset;indexNew++){
			Stone candidate = indexOld == limitOld ? null : data[indexOld];
			if(candidate instanceof Runestone){
				Runestone st = ((Runestone)candidate);
				if(st.path == primary && st.slot == indexNew){
					stoneArray[indexNew] = st;
					indexOld++;
					continue;
				}
			}
			stoneArray[indexNew] = primary.getStone(indexNew, 0);
		}
		Runestone second0 = null,second1 = null;
		for(int i = 0;i < 2;i++){
			Stone candidate = indexOld == limitOld ? null : data[indexOld];
			if(candidate instanceof Runestone){
				Runestone st = (Runestone)candidate;
				if(st.path == secondaryPath){
					if(i == 0){
						second0 = st;
					}else{
						second1 = st;
					}
					indexOld++;
					continue;
				}
			}
			if(i == 0){
				second0 = secondary.getStone(1, 0);
			}else{
				second1 = secondary.getStone(second0.slot == 1 ? 2 : 1, 0);
			}
		}
		if(second0.slot > second1.slot){
			stoneArray[offset++] = second1;
			stoneArray[offset++] = second0;
		}else{
			stoneArray[offset++] = second0;
			stoneArray[offset++] = second1;
		}
		int slots = model.getStatSlotCount();
		a: for(int i = 0;i < slots;i++){
			Stone candidate = indexOld == limitOld ? null : data[indexOld];
			if(candidate instanceof Statstone){
				Statstone st = (Statstone)candidate;
				if(st.minSlot <= i){
					stoneArray[offset+i] = candidate;
					indexOld++;
					continue a;
				}
			}
			stoneArray[offset+i] = model.getStatstone(i, 0);
		}
		return new Rune(model, primary, secondary, stoneArray, true);
	}
	public long toPermutationCode(){
		if(!isComplete){
			return -1;
		}
		long permutation = 0;
		long multiplier = primaryPath.totalPermutationCount/model.statModPermutations;
		int primarySlots = primaryPath.getSlotCount();
		int secondarySlots = secondaryPath.getSlotCount();
		int slot = 0;
		for(;slot < primarySlots;slot++){
			int sl = primaryPath.getStoneCountInSlot(slot);
			permutation *= sl;
			permutation += data[slot].order;
			multiplier /= sl;
		}
		permutation *= multiplier;
		for(Path pt : model){
			if(pt == secondaryPath){
				break;
			}
			if(pt != primaryPath){
				permutation += pt.secondaryPermutationCount;
			}
		}
		Runestone ab = (Runestone)data[slot++],cd = (Runestone)data[slot++];
		a:for(int a = 1;true;a++){//Iterate all permutations for the second path. Stop when counter reaches zero.
			int slot0Length = secondaryPath.getStoneCountInSlot(a);
			for(int b = 0;b < slot0Length;b++){
				for(int c = a+1;c < secondarySlots;c++){
					int slot1Length = secondaryPath.getStoneCountInSlot(c);
					for(int d = 0;d < slot1Length;d++){
						if(ab.slot == a && ab.order == b && cd.slot == c && cd.order == d){
							break a;
						}else{
							permutation++;
						}
					}
				}
			}
		}
		int statSlots = model.getStatSlotCount();
		for(int i = 0;i < statSlots;i++){
			permutation *= model.getStatSlotLength(i);
			permutation += data[slot++].order;
		}
		for(Path pt : model){
			if(pt == primaryPath){
				break;
			}
			permutation += pt.totalPermutationCount;
		}
		return permutation;
	}
	/**
	 * Translates this rune to another rune model. Throws an exception if unable to.
	 */
	public Rune translate(RuneModel newModel){
		if(newModel == model){
			return this;
		}
		Path newPrimary = primaryPath == null ? null : (Path)Objects.requireNonNull(newModel.stoneMap.get(primaryPath.id));
		Path newSecondary = secondaryPath == null ? null : (Path)Objects.requireNonNull(newModel.stoneMap.get(secondaryPath.id));
		ArrayList<Stone> newStoneList = new ArrayList<>(data.length);
		for(Stone st : data){
			newStoneList.add(Objects.requireNonNull(newModel.stoneMap.get(st.id)));
		}
		return ofStones(newModel, newPrimary, newSecondary, newStoneList);
	}
	public int statStoneOffset(){
		int i = data.length;
		while(i > 0){
			i--;
			if(data[i] instanceof Runestone){
				return i+1;
			}
		}
		return 0;
	}
	public Stone getSelectedStone(int path,int slot){
		int searchIndex = slot;
		switch(path){
		case 1:if(primaryPath != null)searchIndex += primaryPath.getSlotCount();//$FALL-THROUGH$
		case 0:
			Path p = path == 0 ? primaryPath : secondaryPath;
			if(p == null){
				return null;
			}
			if(searchIndex >= data.length){
				searchIndex = data.length-1;
			}
			while(searchIndex >= 0){
				Stone stone = data[searchIndex];
				if(stone instanceof Runestone){
					Runestone st = (Runestone)stone;
					if(st.path == p){
						if(st.slot == slot){
							return st;
						}
						if(st.slot < slot){
							return null;
						}
					}else if(path == 1){
						return null;
					}
				}
				searchIndex--;
			}
			return null;
		case 2:
			int offset = statStoneOffset();
			int limit = data.length;
			int currentSlot = 0;
			while(offset < limit){
				Statstone st = (Statstone)data[offset];
				if(st.minSlot > currentSlot){
					currentSlot = st.minSlot;
				}
				if(currentSlot == slot){
					return st;
				}
				currentSlot++;
				if(currentSlot > slot){
					return null;
				}
				offset++;
			}
			return null;
		default:throw new IllegalArgumentException();
		}
	}
	public int getSelectedIndex(int path,int slot){
		Stone st = getSelectedStone(path, slot);
		return st == null ? -1 : st.order;
	}
	public Runestone getKeystone(){
		if(data == EMPTY_STONE_ARRAY){
			return null;
		}
		Stone st = data[0];
		if(st instanceof Runestone && ((Runestone)st).slot == 0){
			return (Runestone)st;
		}else{
			return null;
		}
	}
	public JSMap toJSMap(){
		JSList perkJSList = new JSList(data.length);
		for(Stone perk : data){
			perkJSList.add(perk.id);
		}
		JSMap map = new JSMap();
		map.put("selectedPerkIds",perkJSList);
		if(primaryPath != null){
			map.put("primaryStyleId",primaryPath.id);
		}
		if(secondaryPath != null){
			map.put("subStyleId",secondaryPath.id);
		}
		return map;
	}
	@Override
	public int hashCode() {
		return Arrays.hashCode(data);
	}
	@Override
	public boolean equals(Object that){
		if(that instanceof Rune){
			return Arrays.deepEquals(data, ((Rune)that).data);
		}
		return false;
	}
	private int compare(Stone o1, Stone o2) {
		if(o1 instanceof Runestone){
			if(o2 instanceof Runestone){
				Runestone s1 = (Runestone)o1;
				Runestone s2 = (Runestone)o2;
				if(s1.path == s2.path){
					switch(Integer.compare(s1.slot, s2.slot)){
					case -1:return -1;
					case 1:return 1;
					case 0:throw new RuneException("Slot conflict!",s1,s2);//We are bound to find one of these if they exist.
					}
				}else{
					return s1.path == primaryPath ? -1 : 1;
				}
			}
			return -1;
		}else if(o2 instanceof Runestone){
			return 1;
		}else{
			Statstone s1 = (Statstone)o1;
			Statstone s2 = (Statstone)o2;
			if(s1.minSlot < s2.minSlot){
				return -1;
			}
			if(s1.minSlot > s2.minSlot){
				return 1;
			}
			if(s1.maxSlot < s2.maxSlot){
				return -1;
			}
			if(s1.maxSlot > s2.maxSlot){
				return 1;
			}
		}
		return 0;
	}
	public static class RuneException extends RuntimeException{
		private static final long serialVersionUID = 1L;
		public final Stone stoneA,stoneB;
		public RuneException(String message,Stone stoneA,Stone stoneB) {
			super(message);
			this.stoneA = stoneA;
			this.stoneB = stoneB;
		}
	}
}
