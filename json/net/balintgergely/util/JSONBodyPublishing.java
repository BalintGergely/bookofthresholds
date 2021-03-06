package net.balintgergely.util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
/**
 * Semi-static class for efficiently publishing JSON Objects over HTTP protocol.
 * @author balintgergely
 */
public class JSONBodyPublishing implements Subscription{
	public static BodyPublisher publish(Object obj){
		LinkedList<ByteBuffer> list = new LinkedList<>();
		try(OutputStreamWriter writer = new OutputStreamWriter(new ByteBufferOutputStream(list::add),StandardCharsets.UTF_8)){
			JSON.write(obj, writer);
		}catch(IOException e){
			throw new UncheckedIOException(e);
		}
		long totalLength = 0;
		for(ByteBuffer buffer : list){
			totalLength += buffer.remaining();
		}
		return BodyPublishers.fromPublisher(s -> s.onSubscribe(new JSONBodyPublishing(list.iterator(), s)),totalLength);
	}
	private Iterator<ByteBuffer> itr;
	private Subscriber<? super ByteBuffer> sb;
	private JSONBodyPublishing(Iterator<ByteBuffer> itr,Subscriber<? super ByteBuffer> subscriber) {
		this.itr = itr;
		this.sb = subscriber;
	}
	@Override
	public void request(long n) {
		while(itr.hasNext()){
			if(n == 0 || sb == null){
				return;
			}
			sb.onNext(itr.next().asReadOnlyBuffer());
			n--;
		}
		sb.onComplete();
		sb = null;
	}
	@Override
	public void cancel() {
		sb = null;
	}
}
