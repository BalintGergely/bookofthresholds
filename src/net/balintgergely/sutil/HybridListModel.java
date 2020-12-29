package net.balintgergely.sutil;

import javax.swing.AbstractListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class HybridListModel<E> extends AbstractListModel<E> implements ListSelectionModel{
	private static final long serialVersionUID = 1L;
	/**
	 * <li>selectedIndex: Currently selected index or -1
	 * <li>anchorIndex: Anchor index
	 * <li>leadIndex: Lead index
	 * <li>adjustingState: Cached selectedIndex from beginning of adjusting or -2 if not adjusting
	 */
	protected int selectedIndex = -1,anchorIndex = -1,leadIndex = -1,adjustingState = -2;
	/**
	 * Set to true before a try finally block while updating the list and firing list events.
	 * Selection can not change by HybridListModel's methods other than <code>moveSelection</code> while true.
	 */
	protected boolean antiRecurse;
	protected void moveSelection(int index){
		if(selectedIndex != index){
			int oldIndex = selectedIndex;
			selectedIndex = index;
			if(oldIndex == -1){
				fireValueChanged(index, index);
			}else if(index == -1){
				fireValueChanged(oldIndex, oldIndex);
			}else{
				fireValueChanged(Math.min(index, oldIndex),Math.max(index, oldIndex));
			}
		}
	}
	/**
	 * @return True if currently within a structurally modifying event. False otherwise.
	 */
	public boolean isBeingModified(){
		return antiRecurse;
	}
	public E getSelectedElement() {
		return selectedIndex >= 0 ? getElementAt(selectedIndex) : null;
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
	@Override
	public void setSelectionInterval(int index0, int index1) {
		if(antiRecurse){
			return;
		}
		if(index1 < 0 || index1 >= getSize()){
			throw new IllegalArgumentException();
		}
		leadIndex = index1;
		anchorIndex = index1;
		if(selectedIndex != index1){
			int oldIndex = selectedIndex;
			selectedIndex = index1;
			if(oldIndex == -1){
				fireValueChanged(index1, index1);
			}else{
				fireValueChanged(Math.min(index1, oldIndex),Math.max(index1, oldIndex));
			}
			fireStateChanged();
		}
	}
	protected void silentlySetSelection(int index){
		leadIndex = index;
		anchorIndex = index;
		if(selectedIndex != index){
			int oldIndex = selectedIndex;
			selectedIndex = index;
			if(oldIndex == -1){
				fireValueChanged(index, index);
			}else{
				fireValueChanged(Math.min(index, oldIndex),Math.max(index, oldIndex));
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
			fireStateChanged();
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
			fireStateChanged();
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
		int s = getSize();
		if(length < 0 || index < 0 || length+index > s){
			throw new IllegalArgumentException();
		}
		if(selectedIndex >= 0 && selectedIndex > index){
			int oldIndex = selectedIndex;
			if(selectedIndex+length > s){
				throw new IllegalArgumentException();
			}
			selectedIndex += length;
			fireValueChanged(oldIndex, selectedIndex);
			fireStateChanged();
		}
	}
	@Override
	public void removeIndexInterval(int index0, int index1) {
		if(antiRecurse){
			return;
		}
		int min = Math.min(index0, index1);
		int max = Math.max(index0, index1);
		if(selectedIndex >= 0 && selectedIndex >= min){
			int oldIndex = selectedIndex;
			if(selectedIndex > max){
				selectedIndex = selectedIndex-max+min;
				fireValueChanged(oldIndex, selectedIndex);
			}else{
				selectedIndex = -1;
				fireValueChanged(oldIndex, oldIndex);
			}
			fireStateChanged();
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
			fireStateChanged();
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
	public void addChangeListener(ChangeListener x) {
		listenerList.add(ChangeListener.class, x);
	}
	public void removeChangeListener(ChangeListener x) {
		listenerList.remove(ChangeListener.class, x);
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
	private ChangeEvent che;
	protected void fireStateChanged(){
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i -= 2){
			if(listeners[i] == ChangeListener.class){
				if(che == null){
					che = new ChangeEvent(this);
				}
				((ChangeListener)listeners[i+1]).stateChanged(che);
			}
		}
    }
}
