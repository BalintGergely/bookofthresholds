package net.balintgergely.util;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.Spliterator;
import java.util.function.Predicate;

public class SimpleOrdinalCollection<E> extends AbstractCollection<E> implements OrdinalCollection<E>{
	private E[] data;
	private IndexSet base;
	private int off;
	@SuppressWarnings("unchecked")
	public SimpleOrdinalCollection(IndexSet base,Class<? super E> type) {
		this.data = (E[])Array.newInstance(type, base.length());
		this.base = base;
		this.off = 0;
	}
	SimpleOrdinalCollection(SimpleOrdinalCollection<E> s,IndexSet b,int off) {
		this.data = s.data;
		this.base = b;
		this.off = s.off+off;
	}
	@Override
	public boolean contains(Object o) {
		PrimitiveIterator.OfInt indices = base.indexIterator();
		if(o == null){
			while(indices.hasNext()){
				if(data[off+indices.nextInt()] == null){
					return true;
				}
			}
		}else{
			while(indices.hasNext()){
				if(o.equals(data[off+indices.nextInt()])){
					return true;
				}
			}
		}
		return false;
	}
	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean remove(Object o) {
		PrimitiveIterator.OfInt indices = base.indexIterator();
		if(o == null){
			while(indices.hasNext()){
				if(data[off+indices.nextInt()] == null){
					indices.remove();
					return true;
				}
			}
		}else{
			while(indices.hasNext()){
				if(o.equals(data[off+indices.nextInt()])){
					indices.remove();
					return true;
				}
			}
		}
		return false;
	}
	@Override
	public boolean removeAll(Collection<?> c) {
		return removeImpl(c,true);
	}
	@Override
	public boolean retainAll(Collection<?> c) {
		return removeImpl(c,false);
	}
	private boolean removeImpl(Collection<?> c,boolean complement){
		boolean mod = false;
		PrimitiveIterator.OfInt indices = base.indexIterator();
		while(indices.hasNext()){
			if(c.contains(data[off+indices.nextInt()]) == complement){
				indices.remove();
				mod = true;
			}
		}
		return mod;
	}
	@Override
	public boolean removeIf(Predicate<? super E> p){
		boolean mod = false;
		PrimitiveIterator.OfInt indices = base.indexIterator();
		while(indices.hasNext()){
			if(p.test(data[off+indices.nextInt()])){
				indices.remove();
				mod = true;
			}
		}
		return mod;
	}
	@Override
	public void clear() {
		base.clear();
		Arrays.fill(data, off, off+base.length(), null);
	}
	@Override
	public E get(int index) {
		return base.contains(index) ? data[off+index] : null;
	}
	@Override
	public int firstIndex() {
		return base.firstIndex();
	}
	@Override
	public int lastIndex() {
		return base.lastIndex();
	}
	@Override
	public int size() {
		return base.size();
	}
	@Override
	public boolean isEmpty() {
		return base.isEmpty();
	}
	@Override
	public boolean contains(int index) {
		return base.contains(index);
	}
	@Override
	public E set(int index,E element) {
		E old = base.add(index) ? null : data[off+index];
		data[off+index] = element;
		return old;
	}
	@Override
	public E remove(int index) {
		if(base.contains(index)){
			base.remove(index);
			index += off;
			E old = data[index];
			data[index] = null;
			return old;
		}
		return null;
	}
	@Override
	public OfInt indexIterator() {
		return base.indexIterator();
	}
	@Override
	public Spliterator.OfInt indexSpliterator() {
		return base.indexSpliterator();
	}
	@Override
	public int length() {
		return base.length();
	}
	@Override
	public boolean add(int index) {
		throw new UnsupportedOperationException();
	}
	@Override
	public Iterator<E> iterator() {
		return OrdinalCollection.super.iterator();
	}
}
