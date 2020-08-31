package net.balintgergely.runebook;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * This class manages the full list of Builds as well as (in the future) filtered and sorted subsets of it.
 * Acts as both a list model and a list selection model in single selection mode. The selection can not be altered
 * by any list event listeners registered to this class.
 */
@SuppressWarnings("unused")
public class BuildListModel extends HybridListModel<Build>{
	private static final Comparator<Build> BY_ORDER = (Build a,Build b) -> Long.compare(a.getOrder(), b.getOrder());
	private static final long serialVersionUID = 1L;
	public final List<Build> publicList;
	private CopyOnWriteArrayList<Build> fullList;
	private List<Build> list;
	private Comparator<Build> sortingOrder = BY_ORDER;
	public BuildListModel(Collection<Build> initials) {
		fullList = new CopyOnWriteArrayList<>(initials);
		fullList.sort(BY_ORDER);
		int size = fullList.size();
		for(int i = 0;i < size;i++){
			fullList.get(i).setOrder(i);
		}
		publicList = Collections.unmodifiableList(fullList);
		list = new ArrayList<Build>(fullList);
	}
	@Override
	public int getSize() {
		return list.size()+1;
	}
	@Override
	public Build getElementAt(int index) {
		return index == list.size() ? null : list.get(index);
	}
	private void sweepUpdateOrder(int fromIndex,int toIndex){
		if(sortingOrder == BY_ORDER){
			toIndex = Math.min(toIndex, list.size()-1);
			while(fromIndex <= toIndex){
				list.get(fromIndex).setOrder(fromIndex);
				fromIndex++;
			}
		}
	}
	public int getPreferredInsertLocation(){
		return (selectedIndex == list.size()) ? selectedIndex : 0;
	}
	public void sortByOrder(){
		if(sortingOrder != BY_ORDER){
			list.sort(sortingOrder = BY_ORDER);
			fireContentsChanged(this, 0, list.size()-1);
		}
	}
	public void sortForChampion(Object ch){
		list.sort(sortingOrder = (a,b) -> {
			if(a.getChampion() == ch){
				if(b.getChampion() == ch){
					return 0;
				}else{
					return -1;
				}
			}else if(b.getChampion() == ch){
				return 1;
			}else{
				return 0;
			}
		});
		fireContentsChanged(this, 0, list.size()-1);
	}
	private int findProperIndex(int index,Build build){
		if(sortingOrder == BY_ORDER){
			return Math.min(index,list.size());
		}else if(list.isEmpty()){
			return 0;
		}else{
			int minIndex = 0,maxIndex = list.size()-1;
			while(true){//Binary search to identify a minimum and a maximum index.
				int midIndex = (minIndex+maxIndex) >>> 1;
				int cmp = sortingOrder.compare(list.get(midIndex),build);
				if(cmp < 0){
					minIndex = midIndex+1;
				}else if(cmp > 0){
					maxIndex = midIndex-1;
				}else{
					if(index < midIndex){
						if(index < minIndex){
							index = minIndex;
						}
						while(index < midIndex && sortingOrder.compare(list.get(index),build) < 0){
							index++;
						}
					}else if(index > midIndex){
						if(index > maxIndex){
							index = maxIndex+1;
						}
						midIndex++;
						while(index > midIndex && sortingOrder.compare(list.get(index-1),build) > 0){
							index--;
						}
					}
					return index;
				}
				if(minIndex > maxIndex){
					return minIndex;//Exactly one place to insert to.
				}
			}
		}
	}
	public void insertBuild(Build build,int index){
		antiRecurse = true;
		try{
			if(fullList.addIfAbsent(build)){
				index = findProperIndex(index, build);
				list.add(index, build);
				sweepUpdateOrder(index, Integer.MAX_VALUE);
				fireIntervalAdded(this, index, index);
				moveSelection(index);
				fireStateChanged();
			}
		}finally{
			antiRecurse = false;
		}
	}
	public void moveBuild(Build build,int index){
		antiRecurse = true;
		try{
			if(fullList.contains(build)){
				index = findProperIndex(index, build);
				int dex = list.indexOf(build);
				if(dex >= 0){
					if(dex == index || dex == index-1){
						return;
					}
					list.remove(dex);
					if(index > dex){
						index--;
					}
				}else{
					dex = Integer.MAX_VALUE;
				}
				list.add(index, build);
				sweepUpdateOrder(Math.min(index, dex), Math.max(index, dex));
				if(dex >= 0){
					fireContentsChanged(this, index, dex);
				}else{
					fireIntervalAdded(this, index, index);
				}
				moveSelection(index);
				fireStateChanged();
			}
		}finally{
			antiRecurse = false;
		}
	}
	public void removeBuild(Object build){
		antiRecurse = true;
		try{
			if(fullList.remove(build)){
				int index = list.indexOf(build);
				if(index >= 0){
					list.remove(index);
					sweepUpdateOrder(index,Integer.MAX_VALUE);
					fireIntervalRemoved(this, index, index);
					if(selectedIndex == index){
						moveSelection(-1);
					}
					fireStateChanged();
				}
			}
		}finally{
			antiRecurse = false;
		}
	}
	public void removeSelectedBuild(){
		antiRecurse = true;
		try{
			Build b = getSelectedElement();
			if(fullList.remove(b)){
				int location = list.indexOf(b);
				if(location >= 0){
					list.remove(location);
					fireIntervalRemoved(this, location, location);
					moveSelection(-1);
					fireStateChanged();
				}
			}
		}finally{
			antiRecurse = false;
		}
	}
	public void buildChanged(Build build){
		antiRecurse = true;
		try{
			if(fullList.contains(build)){
				int i = list.indexOf(build);
				if(i >= 0){
					try{
						fireContentsChanged(this, i, i);
					}finally{
						synchronized(fullList){
							fullList.notifyAll();
						}
					}
				}
				fireStateChanged();
			}
		}finally{
			antiRecurse = false;
		}
	}
}
