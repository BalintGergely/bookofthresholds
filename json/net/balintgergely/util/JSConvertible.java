package net.balintgergely.util;
/**
 * Instances of this interface are replaced by calling their 'convert()' method by JSON write and freeze methods.
 * @author balintgergely
 */
public interface JSConvertible{
	public Object convert();
}
