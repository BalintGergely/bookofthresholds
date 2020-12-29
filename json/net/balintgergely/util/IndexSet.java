package net.balintgergely.util;

import java.util.PrimitiveIterator;
import java.util.Spliterator;

public interface IndexSet {
	/**
	 * @return A new iterator of all indices this set contains.
	 */
	public PrimitiveIterator.OfInt indexIterator();
	/**
	 * @return A new spliterator of all indices this set contains.
	 */
	public Spliterator.OfInt indexSpliterator();
	/**
	 * @return The first index this set contains or -1 if this set is empty.
	 */
	public int firstIndex();
	/**
	 * @return The first index this set does not contain or -1 if this set is full.
	 */
	//public int firstUnsetIndex();
	/**
	 * @return The last index this set contains or -1 if this set is empty.
	 */
	public int lastIndex();
	/**
	 * @return The last index this set does not contain or -1 if this set is full.
	 */
	//public int lastUnsetIndex();
	/**
	 * @return Tests whether or not this set currently contains the specified index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	public boolean contains(int index);
	/**
	 * Adds the specified index to this set.
	 * @return true if the index was added, false if this set already contained the specified index.
	 */
	public boolean add(int index);
	public void clear();
	/**
	 * Removes the specified index from this set.
	 * @return Undefined.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	public Object remove(int index);
	public int size();
	public boolean isEmpty();
	public int length();
}
