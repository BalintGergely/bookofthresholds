package net.balintgergely.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
/**
 * An OutputStream that allocates and fills ByteBuffers passing them to a consumer specified at construction.
 * It has a minimum and maximum size for buffers, always allocating the least size possible.
 * @author balintgergely
 */
public class ByteBufferOutputStream extends OutputStream{
	private int minBufferSize,maxBufferSize;
	private ByteBuffer buffer;
	private boolean isDirect;
	private Consumer<ByteBuffer> bufferConsumer;
	public ByteBufferOutputStream(Consumer<ByteBuffer> consumer){
		this(consumer,0x400,0x4000,false);
	}
	public ByteBufferOutputStream(Consumer<ByteBuffer> consumer,int minBufferSize,int maxBufferSize,boolean isDirect){
		if(minBufferSize < 0 || maxBufferSize < minBufferSize || maxBufferSize < 1){
			throw new IllegalArgumentException();
		}
		this.minBufferSize = minBufferSize == 0 ? 1 : minBufferSize;
		this.maxBufferSize = maxBufferSize;
		this.bufferConsumer = consumer;
		this.isDirect = isDirect;
	}
	@Override
	public void write(int b) throws IOException {
		if(buffer == null){
			buffer = isDirect ? ByteBuffer.allocateDirect(minBufferSize) : ByteBuffer.allocate(minBufferSize);
		}
		buffer.put((byte)b);
		if(!buffer.hasRemaining()){
			bufferConsumer.accept(buffer.flip());
			buffer = null;
		}
	}
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(off < 0 || len < 0 || off+len > b.length){
			throw new IndexOutOfBoundsException();
		}
		while(len != 0){
			int cap = len;
			if(buffer == null){
				if(cap <= minBufferSize){
					buffer = isDirect ? ByteBuffer.allocateDirect(minBufferSize) : ByteBuffer.allocate(minBufferSize);
				}else{
					cap = Math.min(maxBufferSize, cap);
					buffer = isDirect ? ByteBuffer.allocateDirect(cap) : ByteBuffer.allocate(cap);
				}
			}else{
				cap = Math.min(buffer.remaining(), cap);
			}
			buffer.put(b, off, cap);
			if(!buffer.hasRemaining()){
				bufferConsumer.accept(buffer.flip());
				buffer = null;
			}
			off += cap;
			len -= cap;
		}
	}
	@Override
	public void close() throws IOException {
		if(buffer != null){
			bufferConsumer.accept(buffer.flip());
			buffer = null;
		}
	}
}
