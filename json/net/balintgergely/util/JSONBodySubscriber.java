package net.balintgergely.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
/**
 * A body subscriber that parses a single JSON object from a single Subscription.
 * Is itself a CompletableFuture that cancels the subscriber from the cancel method.
 * @author balintgergely
 */
public class JSONBodySubscriber extends CompletableFuture<Object> implements BodySubscriber<Object>{
	private static final VarHandle SUBSCRIPTION;
	static{
		Lookup lk = MethodHandles.lookup();
		try{
			SUBSCRIPTION = lk.findVarHandle(JSONBodySubscriber.class, "s", Object.class);
		}catch(NoSuchFieldException | IllegalAccessException e){
			throw new ExceptionInInitializerError(e);
		}
	}
	public static final BodyHandler<Object> HANDLE_UTF8 = (ResponseInfo info) -> new JSONBodySubscriber(StandardCharsets.UTF_8);
	private final ArrayList<InputStream> bufferList = new ArrayList<>();
	private Charset charset;
	/**
	 * Subscription value.
	 * <li><code>null</code> if no subscription
	 * <li><code>this</code> if completed or is about to be
	 * <li><code>bufferList</code> if cancelled or is about to be
	 * <li>Otherwise the Subscription.
	 */
	private volatile Object s = null;
	public JSONBodySubscriber(Charset charset){
		this.charset = charset;
	}
	@Override
	public void onSubscribe(Subscription subscription) {
		if(SUBSCRIPTION.compareAndSet(this,null,subscription)){
			subscription.request(Long.MAX_VALUE);
			return;
		}
		subscription.cancel();
	}
	@SuppressWarnings("resource")
	@Override
	public void onNext(List<ByteBuffer> item) {
		if(s != bufferList){
			bufferList.ensureCapacity(bufferList.size()+item.size());
			for(ByteBuffer buffer : item){
				bufferList.add(new ByteBufferInputStream(buffer));
			}
		}
	}
	@Override
	public void onError(Throwable throwable) {
		bufferList.clear();
		super.completeExceptionally(throwable);
		s = this;//Ignore being cancelled. We have an alt result.
	}
	@Override
	@SuppressWarnings("resource")
	public void onComplete() {
		Object o = s;//Else we are being cancelled.
		if(o != bufferList && SUBSCRIPTION.compareAndSet(this,o,this)){
			try{
				super.complete(//This is probably as satisfying as using java.util.stream.
						JSON.readObject(
								new InputStreamReader(
										new SequenceInputStream(
												Collections.enumeration(bufferList)),
										charset)
								)
						);
			}catch(Throwable t){
				super.completeExceptionally(t);
			}
		}
	}
	@Override
	public CompletionStage<Object> getBody() {
		return this;
	}
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		Object o;
		do{
			o = s;
			if(o == this){
				return super.isCancelled();
			}
			if(o == bufferList){
				return super.cancel(true);
			}
		}while(!SUBSCRIPTION.compareAndSet(this,o,bufferList));
		boolean isCancelled = super.cancel(true);
		if(isCancelled && o != null){
			((Subscription)o).cancel();
		}
		return isCancelled;
	}
	@Override
	public boolean complete(Object value) {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean completeExceptionally(Throwable ex) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void obtrudeValue(Object value) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void obtrudeException(Throwable ex) {
		throw new UnsupportedOperationException();
	}
	@Override
	public CompletableFuture<Object> completeAsync(Supplier<? extends Object> supplier, Executor executor) {
		throw new UnsupportedOperationException();
	}
	@Override
	public CompletableFuture<Object> completeAsync(Supplier<? extends Object> supplier) {
		throw new UnsupportedOperationException();
	}
	@Override
	public CompletableFuture<Object> completeOnTimeout(Object value, long timeout, TimeUnit unit) {
		throw new UnsupportedOperationException();
	}
}
