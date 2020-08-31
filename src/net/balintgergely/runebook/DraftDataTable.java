package net.balintgergely.runebook;

import java.util.Arrays;
import java.util.PrimitiveIterator;

public class DraftDataTable {
	private int[] championIdList;
	public DraftDataTable(PrimitiveIterator.OfInt intValues,int length){
		this.championIdList = new int[length];
		for(int i = 0;i < length;i++){
			championIdList[i] = intValues.nextInt();
		}
		Arrays.sort(championIdList);
	}
	public int getChampionIndex(int key){
		int min = 0;
		int max = championIdList.length-1;
		while(min <= max){
			int mid = (min + max) >>> 1;
			int midVal = championIdList[mid];
			if(midVal < key) {
				min = mid + 1;
			}else if (midVal > key){
				max = mid - 1;
			}else{
				return mid;
			}
		}
		return -1;
	}
	public class Summoner{
		public byte[] relationTable = new byte[championIdList.length];
		public int summonerId;
		public int hoveredChampionId;
		public boolean isLockedIn;
	}
}
