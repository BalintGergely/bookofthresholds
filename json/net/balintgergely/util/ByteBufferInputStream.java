package net.balintgergely.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
/**
 * An input stream bridging methods to a ByteBuffer.
 * <li>Read methods read as much bytes from the buffer as possible, and return -1 if there are no more bytes to read.
 * <li><code>available()</code> returns <code>buffer.remaining()</code>
 * <li><code>skip()</code> skips bytes using <code>buffer.position()</code>
 * <li><code>mark(int)</code> calls <code>buffer.mark()</code>
 * <li><code>reset()</code> calls <code>buffer.reset()</code><br>
 * Note that the single constructor of this class does not duplicate the internal buffer.
 * Every method call uses and updates the state of the buffer passed through the constructor.
 * @author balintgergely
 */
public class ByteBufferInputStream extends InputStream{
	protected ByteBuffer buffer;
	public ByteBufferInputStream(ByteBuffer buffer) {
		this.buffer = buffer;
	}
	@Override
	public int read() throws IOException {
		try{
			if(buffer.hasRemaining()){
				return buffer.get() & 0xff;
			}
			return -1;
		}catch(Exception e){
			throw new IOException(e);
		}
	}
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if(off < 0 || len < 0 || off+len > b.length){
			throw new IllegalArgumentException();
		}
		if(len == 0){
			return 0;
		}else{
			try{
				len = Math.min(len,buffer.remaining());
				buffer.get(b, off, len);
			}catch(Exception e){
				throw new IOException(e);
			}
			return len == 0 ? -1 : len;
		}
	}
	@Override
	public byte[] readAllBytes() throws IOException {
		try{
			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			return bytes;
		}catch(Exception | OutOfMemoryError e){
			throw new IOException(e);
		}
	}
	@Override
	public int readNBytes(byte[] b, int off, int len) throws IOException {
		return read(b, off, len);
	}
	@Override
	public long skip(long n) throws IOException {
		try{
			if(n < 0 || n > buffer.remaining()){
				n = buffer.remaining();
			}
			buffer.position(buffer.position()+(int)n);
			return n;
		}catch(Exception e){
			throw new IOException();
		}
	}
	@Override
	public int available() throws IOException {
		try{
			return buffer.remaining();
		}catch(Exception e){
			throw new IOException(e);
		}
	}
	@Override
	public void mark(int readlimit) {
		buffer.mark();
	}
	@Override
	public void reset() throws IOException {
		try{
			buffer.reset();
		}catch(Exception e){
			throw new IOException(e);
		}
	}
	@Override
	public boolean markSupported() {
		return true;
	}
}
