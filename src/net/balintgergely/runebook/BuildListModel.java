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
public class BuildListModel extends AbstractListModel<Build> implements ListSelectionModel {
	private static final Comparator<Build> BY_TIMESTAMP = (Build a,Build b) -> Long.compare(b.timestamp, a.timestamp);
	private static final long serialVersionUID = 1L;
	public final List<Build> publicList;
	private List<Build> fullList;
	private List<Build> list;
	private int selectedIndex = -1,anchorIndex = -1,leadIndex = -1,adjustingState = -2;
	private boolean antiRecurse;
	public BuildListModel(Collection<Build> initials) {
		super();
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
	@Override
	public void addListDataListener(ListDataListener l) {
		listenerList.add(ListDataListener.class, l);
	}
	@Override
	public void removeListDataListener(ListDataListener l) {
		listenerList.remove(ListDataListener.class, l);
	}
	@Override
	public void setSelectionMode(int selectionMode) {
		throw new UnsupportedOperationException();
	}
	public void insertBuild(Build build){
		antiRecurse = true;
		try{
			if(fullList.add(build)){
				list.add(0, build);
				selectedIndex = 0;
				fireIntervalAdded(this, 0, 0);
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
					selectedIndex = -1;
					fireIntervalRemoved(this, location, location);
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
				fireContentsChanged(this, i, i);
			}
		}finally{
			antiRecurse = false;
		}
	}
	@Override
	public void setSelectionInterval(int index0, int index1) {
		if(antiRecurse){
			return;
		}
		if(index1 < 0 || index1 >= list.size()){
			throw new IllegalArgumentException();
		}
		if(selectedIndex != index1){
			int oldIndex = selectedIndex;
			selectedIndex = index1;
			if(oldIndex == -1){
				fireValueChanged(index1, index1);
			}else{
				fireValueChanged(Math.min(index0, oldIndex),Math.max(index0, oldIndex));
			}
		}
	}
	@Override
	public void addSelectionInterval(int index0, int index1) {
		setSelectionInterval(index0, index1);
	}
	@Override
	public void removeSelectionInterval(int index0, int index1) {
		if(antiRecurse){
			return;
		}
		int min = Math.min(index0, index1);
		int max = Math.max(index0, index1);
		anchorIndex = index0;
		leadIndex = index1;
		if(selectedIndex >= 0 && selectedIndex >= min && selectedIndex <= max){
			int oldIndex = selectedIndex;
			selectedIndex = -1;
			fireValueChanged(oldIndex, oldIndex);
		}
	}
	@Override
	public int getMinSelectionIndex() {
		return selectedIndex;
	}
	@Override
	public int getMaxSelectionIndex() {
		return selectedIndex;
	}
	@Override
	public boolean isSelectedIndex(int index) {
		return selectedIndex == index;
	}
	@Override
	public int getAnchorSelectionIndex() {
		return anchorIndex;
	}
	@Override
	public void setAnchorSelectionIndex(int index) {
		anchorIndex = index;
	}
	@Override
	public int getLeadSelectionIndex() {
		return leadIndex;
	}
	@Override
	public void setLeadSelectionIndex(int index) {
		leadIndex = index;
	}
	@Override
	public void clearSelection() {
		if(antiRecurse){
			return;
		}
		int oldIndex = selectedIndex;
		selectedIndex = -1;
		if(oldIndex >= 0){
			fireValueChanged(oldIndex, oldIndex);
		}
	}
	@Override
	public boolean isSelectionEmpty() {
		return selectedIndex == -1;
	}
	@Override
	public void insertIndexInterval(int index, int length, boolean before) {
		if(antiRecurse){
			return;
		}
		if(before){
			index--;
		}
		if(length < 0 || index < 0 || length+index > list.size()){
			throw new IllegalArgumentException();
		}
		if(selectedIndex >= 0 && selectedIndex > index){
			int oldIndex = selectedIndex;
			if(selectedIndex+length > list.size()){
				throw new IllegalArgumentException();
			}
			selectedIndex += length;
			fireValueChanged(oldIndex, selectedIndex);
		}
	}
	@Override
	public void removeIndexInterval(int index0, int index1) {
		if(antiRecurse){
			return;
		}
		int min = Math.min(index0, index1);
		int max = Math.max(index0, index1);
		if(min < 0 || max >= list.size()){
			throw new IllegalArgumentException();
		}
		if(selectedIndex >= 0 && selectedIndex >= min){
			int oldIndex = selectedIndex;
			if(selectedIndex > max){
				selectedIndex = selectedIndex-max+min;
				fireValueChanged(oldIndex, selectedIndex);
			}else{
				selectedIndex = -1;
				fireValueChanged(oldIndex, oldIndex);
			}
		}
	}
	@Override
	public void setValueIsAdjusting(boolean valueIsAdjusting) {
		if(valueIsAdjusting != getValueIsAdjusting()){
			if(valueIsAdjusting){
				adjustingState = selectedIndex;
			}else{
				int min = Math.min(adjustingState, selectedIndex);
				int max = Math.max(adjustingState, selectedIndex);
				adjustingState = -2;
				if(max < 0){
					return;
				}
				if(min < 0){
					min = max;
				}
				fireValueChanged(min, max);
			}
		}
	}
	@Override
	public boolean getValueIsAdjusting() {
		return adjustingState > -2;
	}
	@Override
	public int getSelectionMode() {
		return ListSelectionModel.SINGLE_SELECTION;
	}
	@Override
	public void addListSelectionListener(ListSelectionListener x) {
		listenerList.add(ListSelectionListener.class, x);
	}
	@Override
	public void removeListSelectionListener(ListSelectionListener x) {
		listenerList.remove(ListSelectionListener.class, x);
	}
    protected void fireValueChanged(int firstIndex, int lastIndex){
		Object[] listeners = listenerList.getListenerList();
		ListSelectionEvent e = null;
		for(int i = listeners.length - 2; i >= 0; i -= 2){
			if(listeners[i] == ListSelectionListener.class){
				if(e == null){
					e = new ListSelectionEvent(this, firstIndex, lastIndex, adjustingState > -1);
				}
				((ListSelectionListener)listeners[i+1]).valueChanged(e);
			}
		}
    }
}
