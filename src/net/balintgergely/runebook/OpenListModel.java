package net.balintgergely.runebook;

import java.util.List;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * This horrible thing exists because we can't keep selections tied to list elements.
 * The point of this class is to make list operations such as the removal, addition or complete altering in the order of elements
 * retain the single selected element. Call <code>beforeListChange()</code> before making changes to the list.
 * Generate list events using the corresponding methods. Finally, call <code>afterListChange()</code>.
 */
public class OpenListModel<E> extends DefaultListSelectionModel implements ListModel<E> {
	private static final long serialVersionUID = 1L;
	protected List<E> list;
	private E anchorElement;
	public OpenListModel(List<E> list) {
		super();
		super.setSelectionMode(SINGLE_SELECTION);
		this.list = list;
	}
	@Override
	public int getSize() {
		return list.size();
	}
	@Override
	public E getElementAt(int index) {
		return list.get(index);
	}
	public E getSelectedElement() {
		if(anchorElement == null){
			int index = super.getMinSelectionIndex();
			return (index >= 0 && index < list.size()) ? list.get(index) : null;
		}else{
			return anchorElement;
		}
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
	protected void beforeListChange(){
		int index = super.getMinSelectionIndex();
		if(index >= 0){
			anchorElement = list.get(index);
			super.setValueIsAdjusting(true);
		}
	}
	protected void afterListChange(){
		if(anchorElement != null){
			int index = list.indexOf(anchorElement);
			if(index >= 0){
				super.setSelectionInterval(index, index);
			}
			anchorElement = null;
			super.setValueIsAdjusting(false);
		}
	}
	@Override
	public void setValueIsAdjusting(boolean isAdjusting) {
		if(anchorElement == null){
			super.setValueIsAdjusting(isAdjusting);
		}
	}
	protected void fireContentsChanged(int index0, int index1) {
		Object[] listeners = listenerList.getListenerList();
		ListDataEvent e = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ListDataListener.class) {
				if (e == null) {
					e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index0, index1);
				}
				((ListDataListener) listeners[i + 1]).contentsChanged(e);
			}
		}
	}
	protected void fireIntervalAdded(int index0, int index1) {
		Object[] listeners = listenerList.getListenerList();
		ListDataEvent e = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ListDataListener.class) {
				if (e == null) {
					e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index0, index1);
				}
				((ListDataListener) listeners[i + 1]).intervalAdded(e);
			}
		}
	}
	protected void fireIntervalRemoved(int index0, int index1) {
		Object[] listeners = listenerList.getListenerList();
		ListDataEvent e = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ListDataListener.class) {
				if (e == null) {
					e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index0, index1);
				}
				((ListDataListener) listeners[i + 1]).intervalRemoved(e);
			}
		}
	}
}
