package net.balintgergely.sutil;

import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ListComboBoxModel<E> extends AbstractListModel<E> implements ComboBoxModel<E>{
	private static final long serialVersionUID = 1L;
	private List<E> list;
	private E selectedElement = null;
	public ListComboBoxModel(List<E> lst) {
		this.list = lst;
	}
	public void setList(List<E> lst,boolean selectFirstIfNecessary){
		List<E> old = list;
		this.list = lst;
		if(!old.isEmpty())fireIntervalRemoved(this, 0, old.size()-1);
		if(!lst.isEmpty())fireIntervalAdded(this, 0, lst.size()-1);
		if(selectedElement == null || !lst.contains(selectedElement)){
			setSelectedItem(selectFirstIfNecessary && !lst.isEmpty() ? lst.get(0) : null);
		}
	}
	@Override
	public int getSize() {
		return list.size();
	}
	@Override
	public E getElementAt(int index) {
		return list.get(index);
	}
	@SuppressWarnings("unchecked")
	@Override
	public void setSelectedItem(Object anItem) {
		if(selectedElement != anItem){
			selectedElement = (E)anItem;
			fireContentsChanged(this, -1, -1);
			fireStateChanged();
		}
	}
	@Override
	public E getSelectedItem() {
		return selectedElement;
	}
	public void addChangeListener(ChangeListener ls){
		listenerList.add(ChangeListener.class, ls);
	}
	public void removeChangeListener(ChangeListener ls){
		listenerList.remove(ChangeListener.class, ls);
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
