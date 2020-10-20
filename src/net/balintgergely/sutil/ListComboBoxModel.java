package net.balintgergely.sutil;

import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

public class ListComboBoxModel<E> extends AbstractListModel<E> implements ComboBoxModel<E>{
	private static final long serialVersionUID = 1L;
	private List<E> list;
	private Object selectedElement = null;
	public ListComboBoxModel(List<E> lst) {
		this.list = lst;
	}
	@Override
	public int getSize() {
		return list.size();
	}
	@Override
	public E getElementAt(int index) {
		return list.get(index);
	}
	@Override
	public void setSelectedItem(Object anItem) {
		selectedElement = anItem;
		fireContentsChanged(this, -1, -1);
	}
	@Override
	public Object getSelectedItem() {
		return selectedElement;
	}

}
