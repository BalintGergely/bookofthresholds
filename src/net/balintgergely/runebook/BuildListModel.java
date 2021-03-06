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

import net.balintgergely.sutil.HybridListModel;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;

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
	public BuildListModel(AssetManager assetManager,JSList source) {
		list = new ArrayList<>(source.size());
		for(Object obj : source){
			try{
				JSMap bld = JSON.toJSMap(obj);
				String name = bld.peekString("name");
				String champ = bld.peekString("champion");
				Champion champion = champ == null ? null : assetManager.championsById.get(champ);
				byte roles = bld.peekByte("roles");
				list.add(new Build(name, champion, assetManager.runeModel.parseRune(bld), roles,
						bld.peekLong("order")));
			}catch(Throwable t){
				t.printStackTrace();
			}
		}
		list.sort(BY_ORDER);
		int size = list.size();
		for(int i = 0;i < size;i++){
			list.get(i).setOrder(i);
		}
		fullList = new CopyOnWriteArrayList<>(list);
		publicList = Collections.unmodifiableList(fullList);
		list = new ArrayList<Build>(fullList);
	}
	@Override
	public int getSize() {
		return sortingOrder == BY_ORDER ? (list.size()+1) : list.size();
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
			Build bld = selectedIndex < 0 ? null : list.get(selectedIndex);
			list.sort(sortingOrder = BY_ORDER);
			if(bld != null){
				silentlySetSelection(list.indexOf(bld));
			}
			fireContentsChanged(this, 0, list.size()-1);
			fireIntervalAdded(this, list.size(), list.size());
		}
	}
	public void sortForChampion(Object ch){
		Build bld = selectedIndex < 0 || selectedIndex >= list.size() ? null : list.get(selectedIndex);
		Object oldOrder = sortingOrder;
		list.sort(sortingOrder = (a,b) -> {
			if(ch.equals(a.getChampion())){
				if(ch.equals(b.getChampion())){
					return 0;
				}else{
					return -1;
				}
			}else if(ch.equals(b.getChampion())){
				return 1;
			}else{
				return 0;
			}
		});
		fireContentsChanged(this, 0, list.size()-1);
		if(bld != null){
			silentlySetSelection(list.indexOf(bld));
		}else{
			silentlySetSelection(-1);
		}
		if(oldOrder == BY_ORDER){
			fireIntervalRemoved(this, list.size(), list.size());
		}
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
