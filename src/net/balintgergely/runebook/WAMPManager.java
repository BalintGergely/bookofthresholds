package net.balintgergely.runebook;

import java.io.CharArrayReader;
import java.net.URI;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;

public class WAMPManager implements WebSocket.Listener{
	private BiConsumer<WAMPManager,WebSocket> updateListener;
	private Map<String,Consumer<Object>> listenerMap;
	private CompletableFuture<WebSocket> stage;
	private volatile WebSocket currentWebSocket;
	public WAMPManager(BiConsumer<WAMPManager,WebSocket> updateListener,Map<String,Consumer<Object>> eventListeners){
		this.updateListener = updateListener;
		this.listenerMap = eventListeners;
	}
	public synchronized CompletableFuture<WebSocket> open(WebSocket.Builder builder,URI uri){
		if(stage != null){
			throw new IllegalStateException();
		}
		CompletableFuture<WebSocket> st = builder.buildAsync(uri, this).thenCompose(this::handleOpen);
		if(!st.isCompletedExceptionally()){
			stage = st;
		}
		return st;
	}
	private CompletableFuture<WebSocket> handleOpen(WebSocket webSocket){
		synchronized(this){
			currentWebSocket = webSocket;
		}
		return subscribeChain(webSocket).handle((ws,th) -> {
			if(th == null){
				ws.request(1);
				updateListener.accept(this, ws);
				return ws;
			}else{
				synchronized(WAMPManager.this){
					if(currentWebSocket == webSocket){
						stage = null;
						currentWebSocket = null;
					}
				}
				throw new CancellationException();
			}
		});
	}
	@Override
	public void onOpen(WebSocket webSocket) {
		System.out.println("WAMPManager: open");
	}
	private CompletableFuture<WebSocket> subscribeChain(WebSocket webSocket){
		Iterator<String> itr = listenerMap.keySet().iterator();
		CompletableFuture<WebSocket> cs;
		if(itr.hasNext()){
			cs = webSocket.sendText(nextOf(itr), true);
			if(itr.hasNext()){
				AtomicReference<Function<WebSocket,CompletableFuture<WebSocket>>> arf = new AtomicReference<>();
				arf.set(ws -> {
					CompletableFuture<WebSocket> cs0 = ws.sendText(nextOf(itr), true);
					return itr.hasNext() ? cs0.thenCompose(arf.get()) : cs0;
				});
				return cs.thenCompose(arf.get());
			}
			return cs;
		}else{
			return CompletableFuture.completedFuture(webSocket);
		}
	}
	private static String nextOf(Iterator<String> itr){
		return "[5,"+JSON.quote(itr.next())+"]";
	}
	public synchronized CompletableFuture<WebSocket> subscribeAgain(){
		return stage == null ? null : (stage = stage.thenCompose(this::subscribeChain));
	}
	public synchronized CompletableFuture<WebSocket> getCurrentStage(){
		return stage;
	}
	public synchronized CompletableFuture<WebSocket> close(){
		if(stage == null){
			return null;
		}else{
			return stage = stage.thenCompose(ws -> ws.sendClose(1000, ""));
		}
	}
	public synchronized CompletableFuture<WebSocket> softAbort(WebSocket toClose){
		boolean doUpdate;
		CompletableFuture<WebSocket> finalStage = null;
		synchronized(this){
			if(doUpdate = currentWebSocket == toClose){
				charBufferLength = 0;
				finalStage = stage.thenCompose(ws -> ws.sendClose(1000, ""));
				stage = null;
				currentWebSocket = null;
			}
		}
		if(doUpdate){
			updateListener.accept(this, null);
		}
		return finalStage;
	}
	char[] charBuffer;
	int charBufferLength;
	private void append(CharSequence data){
		int len = data.length();
		int newLength = charBufferLength+len;
		char[] b = charBuffer;
		if(b == null){
			charBuffer = b = new char[len];
		}else if(b.length < newLength){
			b = new char[newLength];
			System.arraycopy(charBuffer, 0, b, 0, charBufferLength);
			charBuffer = b;
		}
		if(data instanceof CharBuffer){
			CharBuffer cbuf = (CharBuffer)data;
			cbuf.get(cbuf.position(), b, charBufferLength, len);
		}else if(data instanceof String){
			String str = (String)data;
			str.getChars(0, str.length(), b, charBufferLength);
		}else{
			int off = charBufferLength;
			for(int i = 0;i < len;i++){
				b[off+i] = data.charAt(i);
			}
		}
		a: if(charBufferLength == 0){
			for(int i = 0;i < newLength;i++){
				if(!Character.isWhitespace(b[i])){
					break a;
				}
			}
			return;
		}
		charBufferLength = newLength;
	}
	@Override
	public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		JSList list = null;
		synchronized(this){
			if(currentWebSocket == webSocket){
				append(data);
				if(last && charBufferLength != 0){
					list = new JSList(3);
					try{
						JSON.readList(list.list, new CharArrayReader(charBuffer,0,charBufferLength));
					}catch(Exception e){
						System.err.println(new String(charBuffer, 0, charBufferLength));
						list = null;
					}finally{
						charBufferLength = 0;
					}
				}
				webSocket.request(1);
			}
		}
		if(list != null && list.peekInt(0) == 8){
			String key = list.peekString(1);
			if(key != null){
				Consumer<Object> cns = listenerMap.get(key);
				JSMap mp = JSON.toJSMap(list.peek(2));
				if(cns != null && mp.map.containsKey("data")){
					try{
						cns.accept(mp.map.get("data"));//Can be mapped to null.
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}
	@Override
	public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
		if(currentWebSocket == webSocket){
			webSocket.request(1);
		}
		return null;
	}
	@Override
	public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
		if(currentWebSocket == webSocket){
			webSocket.request(1);
		}
		return null;
	}
	@Override
	public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
		if(currentWebSocket == webSocket){
			webSocket.request(1);
		}
		return null;
	}
	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		System.out.println("WAMPManager: close");
		boolean doUpdate;
		synchronized(this){
			if(doUpdate = currentWebSocket == webSocket){
				charBufferLength = 0;
				stage = null;
				currentWebSocket = null;
			}
		}
		if(doUpdate){
			updateListener.accept(this, null);
		}
		return null;
	}
	@Override
	public void onError(WebSocket webSocket, Throwable error) {
		System.out.println("WAMPManager: error");
		boolean doUpdate;
		synchronized(this){
			if(doUpdate = currentWebSocket == webSocket){
				stage = null;
				currentWebSocket = null;
			}
		}
		if(doUpdate){
			updateListener.accept(this, null);
		}
		error.printStackTrace();
	}
}
