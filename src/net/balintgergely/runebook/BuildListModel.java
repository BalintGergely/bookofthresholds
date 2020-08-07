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
	private static final Comparator<Build> BY_TIMESTAMP = (Build a,Build b) -> Long.compare(b.getTimestamp(), a.getTimestamp());
	private static final long serialVersionUID = 1L;
	public final List<Build> publicList;
	private volatile boolean changeFlag;
	private List<Build> fullList;
	private List<Build> list;
	public BuildListModel(Collection<Build> initials) {
		fullList = new CopyOnWriteArrayList<>(initials);
		fullList.sort(BY_TIMESTAMP);
		publicList = Collections.unmodifiableList(fullList);
		list = new ArrayList<Build>(fullList);
	}
	@Override
	public int getSize() {
		return list.size();
	}
	@Override
	public Build getElementAt(int index) {
		return list.get(index);
	}
	public Build getSelectedElement() {
		return selectedIndex >= 0 ? list.get(selectedIndex) : null;
	}
	public void insertBuild(Build build,int index){
		antiRecurse = true;
		try{
			if(fullList.add(build)){
				try{
					list.add(index, build);
					fireIntervalAdded(this, index, index);
					moveSelection(index);
					fireStateChanged();
				}finally{
					changeFlag = true;
					synchronized(fullList){
						fullList.notifyAll();
					}
				}
			}
		}finally{
			antiRecurse = false;
		}
	}
	public void moveBuild(Build build,int index){
		antiRecurse = true;
		try{
			if(fullList.contains(build)){
				int dex = list.indexOf(build);
				if(dex >= 0){
					if(dex == index || dex == index-1){
						return;
					}
					list.remove(dex);
					if(index > dex){
						index--;
					}
				}
				list.add(index, build);
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
				try{
					int index = list.indexOf(build);
					if(index >= 0){
						list.remove(index);
						fireIntervalRemoved(this, index, index);
						if(selectedIndex == index){
							moveSelection(-1);
						}
						fireStateChanged();
					}
				}finally{
					changeFlag = true;
					synchronized(fullList){
						fullList.notifyAll();
					}
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
				try{
					int location = list.indexOf(b);
					if(location >= 0){
						list.remove(location);
						fireIntervalRemoved(this, location, location);
						moveSelection(-1);
						fireStateChanged();
					}
				}finally{
					changeFlag = true;
					synchronized(fullList){
						fullList.notifyAll();
					}
				}
			}
		}finally{
			antiRecurse = false;
		}
	}
	public void buildChanged(Build build){
		antiRecurse = true;
		try{
			int i = list.indexOf(build);
			if(i >= 0){
				try{
					fireContentsChanged(this, i, i);
					fireStateChanged();
				}finally{
					changeFlag = true;
					synchronized(fullList){
						fullList.notifyAll();
					}
				}
			}
		}finally{
			antiRecurse = false;
		}
	}
	public void awaitChange() throws InterruptedException{
		synchronized(fullList){
			fullList.wait();
		}
	}
	public void awaitChange(long timeoutMillis) throws InterruptedException{
		synchronized(fullList){
			fullList.wait(timeoutMillis);
		}
	}
	public void awaitChange(long timeoutMillis,int nanos) throws InterruptedException{
		synchronized(fullList){
			fullList.wait(timeoutMillis,nanos);
		}
	}
	public void notifyChange(){
		synchronized(fullList){
			fullList.notifyAll();
		}
	}
	void clearChangeFlag(){
		changeFlag = false;
	}
	boolean getChangeFlag(){
		return changeFlag;
	}
}
