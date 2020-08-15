package net.balintgergely.runebook;

import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.CancellationException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;
import net.balintgergely.util.JSONBodyPublishing;
import net.balintgergely.util.JSONBodySubscriber;

public class ClientManager implements WebSocket.Listener{
	/*
	 * I honestly... I tried to despaghettify this thing but I realized that
	 * it is not that terrible and doing so would be just overengineering.
	 * It is not nice but it works, it can be understood if you look hard enough
	 * and it is elegant.
	 */
	private static class ClientBuildListModel extends HybridListModel<Build>{
		private static final long serialVersionUID = 1L;
		private ArrayList<Build> buildList = new ArrayList<>();
		private HashMap<Number,Build> buildMap = new HashMap<>();
		private final AssetManager assets;
		private ClientBuildListModel(JSList initialList,AssetManager manager){
			this.assets = manager;
			if(initialList != null){
				update(initialList);
			}
		}
		@Override
		public Build getElementAt(int index) {
			return buildList.get(index);
		}
		@Override
		public int getSize() {
			return buildList.size();
		}
		private void update(JSList builds){
			antiRecurse = true;
			try{
				Build selectedBuild = selectedIndex >= 0 ? buildList.get(selectedIndex) : null;
				int previousSize = buildList.size();
				buildList.clear();
				int indexOfSelectedBuild = -1;
				for(Object obj : builds){
					JSMap m = JSON.asJSMap(obj, false);
					if(m != null){
						try{
							Number id = m.getNumber("id");
							String name = m.peekString("name","");
							long timestamp = m.peekLong("lastModified", 0);
							Rune rn = assets.runeModel.parseRune(m);
							Build bld = buildMap.get(id);
							if(bld == null){
								buildMap.put(id, bld = new Build(name, null, rn, (byte)0x80, timestamp));
							}else{
								if(bld == selectedBuild){
									indexOfSelectedBuild = buildList.size();
								}
								bld.setName(name);
								bld.setTimestamp(timestamp);
								bld.setRune(rn);
							}
							if(m.peekBoolean("current")){
								selectedBuild = bld;
								indexOfSelectedBuild = buildList.size();
							}
							buildList.add(bld);
						}catch(Exception e){
							e.printStackTrace();
						}
					}
				}
				fireContentsChanged(this, 0, Math.max(previousSize, buildList.size()));
				super.moveSelection(indexOfSelectedBuild);
				super.fireStateChanged();
			}finally{
				antiRecurse = false;
			}
		}
	}
	/**
	 * An Executor corresponding to the AWT Event Queue. Used to pass execution to the AWT Dispatch Thread via CompletableFuture async methods.
	 */
	public static final Executor EVENT_QUEUE = EventQueue::invokeLater;
	/**
	 * Well here it is. The one and only crypto certificate to the League Client.
	 */
	private static final String CERTIFICATE = "-----BEGIN CERTIFICATE-----\r\n" +
			"MIIEIDCCAwgCCQDJC+QAdVx4UDANBgkqhkiG9w0BAQUFADCB0TELMAkGA1UEBhMC\r\n" + 
			"VVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFTATBgNVBAcTDFNhbnRhIE1vbmljYTET\r\n" + 
			"MBEGA1UEChMKUmlvdCBHYW1lczEdMBsGA1UECxMUTG9MIEdhbWUgRW5naW5lZXJp\r\n" + 
			"bmcxMzAxBgNVBAMTKkxvTCBHYW1lIEVuZ2luZWVyaW5nIENlcnRpZmljYXRlIEF1\r\n" + 
			"dGhvcml0eTEtMCsGCSqGSIb3DQEJARYeZ2FtZXRlY2hub2xvZ2llc0ByaW90Z2Ft\r\n" + 
			"ZXMuY29tMB4XDTEzMTIwNDAwNDgzOVoXDTQzMTEyNzAwNDgzOVowgdExCzAJBgNV\r\n" + 
			"BAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRUwEwYDVQQHEwxTYW50YSBNb25p\r\n" + 
			"Y2ExEzARBgNVBAoTClJpb3QgR2FtZXMxHTAbBgNVBAsTFExvTCBHYW1lIEVuZ2lu\r\n" + 
			"ZWVyaW5nMTMwMQYDVQQDEypMb0wgR2FtZSBFbmdpbmVlcmluZyBDZXJ0aWZpY2F0\r\n" + 
			"ZSBBdXRob3JpdHkxLTArBgkqhkiG9w0BCQEWHmdhbWV0ZWNobm9sb2dpZXNAcmlv\r\n" + 
			"dGdhbWVzLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKoJemF/\r\n" + 
			"6PNG3GRJGbjzImTdOo1OJRDI7noRwJgDqkaJFkwv0X8aPUGbZSUzUO23cQcCgpYj\r\n" + 
			"21ygzKu5dtCN2EcQVVpNtyPuM2V4eEGr1woodzALtufL3Nlyh6g5jKKuDIfeUBHv\r\n" + 
			"JNyQf2h3Uha16lnrXmz9o9wsX/jf+jUAljBJqsMeACOpXfuZy+YKUCxSPOZaYTLC\r\n" + 
			"y+0GQfiT431pJHBQlrXAUwzOmaJPQ7M6mLfsnpHibSkxUfMfHROaYCZ/sbWKl3lr\r\n" + 
			"ZA9DbwaKKfS1Iw0ucAeDudyuqb4JntGU/W0aboKA0c3YB02mxAM4oDnqseuKV/CX\r\n" + 
			"8SQAiaXnYotuNXMCAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAf3KPmddqEqqC8iLs\r\n" + 
			"lcd0euC4F5+USp9YsrZ3WuOzHqVxTtX3hR1scdlDXNvrsebQZUqwGdZGMS16ln3k\r\n" + 
			"WObw7BbhU89tDNCN7Lt/IjT4MGRYRE+TmRc5EeIXxHkQ78bQqbmAI3GsW+7kJsoO\r\n" + 
			"q3DdeE+M+BUJrhWorsAQCgUyZO166SAtKXKLIcxa+ddC49NvMQPJyzm3V+2b1roP\r\n" + 
			"SvD2WV8gRYUnGmy/N0+u6ANq5EsbhZ548zZc+BI4upsWChTLyxt2RxR7+uGlS1+5\r\n" + 
			"EcGfKZ+g024k/J32XP4hdho7WYAS2xMiV83CfLR/MNi8oSMaVQTdKD8cpgiWJk3L\r\n" + 
			"XWehWA==\r\n-----END CERTIFICATE-----\r\n";
	//private static final BodyPublisher EMPTY = BodyPublishers.ofString("{}", StandardCharsets.UTF_8);
	private static final Throwable LOL_NOT_RUNNING = new Throwable("League of Legends is not running.",null,false,false){
		private static final long serialVersionUID = 1L;
		@Override
		public Throwable initCause(Throwable cause) {
			return this;
		}
	};
	private static final Pattern	PASSWORD_PATTERN = Pattern.compile("--remoting-auth-token=(?<group>[^ \\\"]+)"),
									PORT_PATTERN = Pattern.compile("--app-port=(?<group>\\d+)");
	private static final BodyHandler<String> STRING_BODY_HANDLER = BodyHandlers.ofString();
	private static final BodyHandler<Void> DISCARD = BodyHandlers.discarding();
	private static <E> E bodyOf(HttpResponse<E> response){
		if(response.statusCode()/100 != 2){
			throw new RuntimeException(response.uri()+" Received status code: "+response.statusCode());
		}
		return response.body();
	}
	private static final CompletionStage<Object> COMPLETED_STAGE = CompletableFuture.completedStage(null);
	public static final int STATE_NOT_KNOWN = 0,
							STATE_NOT_RUNNING = 1,
							STATE_CONNECTING = 2,
							STATE_CONNECTED = 3,
							STATE_ERROR = 4;
	/*
	 * Notable events:
	 * "OnJsonApiEvent_lol-perks_v1_pages" Fired when rune pages change.
	 * "OnJsonApiEvent_lol-champ-select_v1_grid-champions" Fired in champ select when selected champion changes
	 * 
	 */
	public static void main(String[] atgs) throws Throwable{
		HttpClient client = HttpClient.newBuilder().sslContext(ClientManager.makeContext()).build();
		ClientManager manager = new ClientManager(client);
		manager.startSeekerThread();
		System.out.println("Done.");
		try(Scanner sc = new Scanner(System.in)){
			sc.nextLine();
		}
	}
	/**
	 * Creates an SSLContext that supports both LCU among other normal certificates.
	 */
	public static SSLContext makeContext() throws GeneralSecurityException, IOException{
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());//Step 1: Get default factory.
		tmf.init((KeyStore)null);//Step 2: Get the factory to load the default keystore
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());//Step 3: Make our own keystore
		keyStore.load(null);//Step 4: Load 'nothing' into our keystore.
		for(TrustManager trustManager : tmf.getTrustManagers()){//Step 5: Fetch the default keys.
			if(trustManager instanceof X509TrustManager){
				for(X509Certificate cert : ((X509TrustManager)trustManager).getAcceptedIssuers()){
					keyStore.setCertificateEntry(Integer.toString(System.identityHashCode(cert)), cert);
				}
			}
		}
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		keyStore.setCertificateEntry("L0L", cf.generateCertificate(new ByteArrayInputStream(CERTIFICATE.getBytes())));
		tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(keyStore);
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());//Yaaaaay
		return sslContext;
	}
	/**
	 * Constructs a URI object converting it's syntax exceptions to runtime exception.
	 */
	private static URI uri(String uri){
		try{
			return new URI(uri);
		}catch(URISyntaxException e){
			throw new RuntimeException(e);
		}
	}
	HttpClient client;
	private final String address;
	private volatile Runnable changeListener;
	private volatile Function<String,HttpRequest.Builder> conref;
	private volatile CompletableFuture<Object> localeString;
	private volatile int ownedPageCount;
	private volatile JSList pageList;
	private volatile ClientBuildListModel pageListModel;
	private final ProcessBuilder ppFetcher;
	/**
	 * <li>null & exceptional: Not connected.
	 * <li>Incomplete: Connection attempt in progress.
	 * <li>Complete: Connected
	 */
	private AtomicReference<CompletableFuture<WebSocket>> state = new AtomicReference<>();
	private Thread seekerThread = new Thread(() -> {
		while(true){
			try{
				CompletableFuture<?> ft = tryConnect(false);
				long nextTime = System.currentTimeMillis()+10000;
				while(!ft.isDone()){
					try{
						ft.get();
					}catch(CancellationException | ExecutionException t){}
				}//ft must be done.
				if(!ft.isCompletedExceptionally()){
					while(state.get() == ft){
						LockSupport.park();//We are connected. Wait until we are not.
					}
				}
				while(System.currentTimeMillis() < nextTime){
					LockSupport.parkUntil(nextTime);
				}
			}catch(Throwable e){
				e.printStackTrace();
			}
		}
	}, "ClientManager.seekerThread");
	{
		seekerThread.setDaemon(true);
	}
	private StringBuilder textCollator = new StringBuilder();
	public ClientManager(HttpClient client){
		this.client = client;
		address = InetAddress.getLoopbackAddress().getHostAddress();
		String osName = System.getProperty("os.name");
		String osLowerCase = osName.toLowerCase();
		if(osLowerCase.contains("win")){
			ppFetcher = new ProcessBuilder("WMIC", "process", "where", "name='LeagueClientUx.exe'", "get", "commandLine");
		}else if(osLowerCase.contains("mac")){
			ppFetcher = new ProcessBuilder("ps", "x", "|", "grep", "'LeagueClientUx.exe'");
		}else{
			throw new Error("Connecting to the League Client on "+osName+" is not supported.");
		}
	}
	/**
	 * Closes the connection if open. Returns a CompletableFuture that completes with the 
	 * single parameter value once the close command has been sent.
	 */
	public <E> CompletableFuture<E> close(E responseObject){
		CompletableFuture<WebSocket> st = state.get();
		if(st == null || st.isCompletedExceptionally()){
			return CompletableFuture.completedFuture(responseObject);
		}else{
			return st.thenCompose(ws -> ws.isOutputClosed() ? st : ws.sendClose(1000, "")).handle((ws,th) -> {
				if(th != null){
					th.printStackTrace();
				}
				return responseObject;
			});
		}
	}
	/**
	 * Starts the automatic seeker thread.
	 */
	public void startSeekerThread(){
		seekerThread.start();
	}
	/**
	 * Exports the specified rune with the specified name into the LCU.
	 */
	@SuppressWarnings("unchecked")
	public CompletionStage<Object> exportRune(final Rune rune,final String name){
		BodyPublisher runeExport = JSONBodyPublishing.publish(rune.toJSMap().put("name", name, "current", Boolean.TRUE));
		/*
		 * Listening to page changes via WebSocket and using cached page information
		 * produces a noticeable increase in the speed of exports which is kind of important.
		 * Not to mention the nice bonus of being able to display client runes in the rune book.
		 */
		return composeExport(runeExport, name, (CompletableFuture<WebSocket>)tryConnect(false));
	}
	volatile boolean gotRetry;
	/**
	 * Composes the export operation.
	 */
	private CompletableFuture<Object> composeExport(BodyPublisher runeExport,String name,CompletableFuture<WebSocket> ws){
		return ws.thenCompose(o -> {
			JSList runeList = pageList;
			JSMap pageToReplace = null;
			int pageLimit = ownedPageCount;
			long lastModTime = Long.MAX_VALUE;
			for(Object obj : runeList){
				JSMap runeBuild = JSON.asJSMap(obj, true);
				if(runeBuild.peekBoolean("isEditable",false)){//Ignore builtin pages
					if(name.equals(runeBuild.peekString("name"))){
						pageToReplace = runeBuild;
						pageLimit = -1;
						break;
					}
					long modTime = runeBuild.peekLong("lastModified");
					if(modTime <= lastModTime){
						pageToReplace = runeBuild;
						lastModTime = modTime;
					}
					pageLimit--;
				}
			}
			HttpRequest exportRequest;
			if(pageLimit > 0){
				exportRequest = conref.apply("lol-perks/v1/pages").POST(runeExport).build();
			}else{
				int pageId = pageToReplace.getInt("id");
				exportRequest = conref.apply("lol-perks/v1/pages/"+pageId).PUT(runeExport).build();
			}
			return client.sendAsync(exportRequest, STRING_BODY_HANDLER);
		})
		.handle((BiFunction<HttpResponse<String>,Throwable,CompletionStage<Object>>)(HttpResponse<String> response,Throwable t) -> {
			if(response != null){//Until now everything succeeded. We know this because we are not handling a throwable.
				int code = response.statusCode();
				if(code/100 != 2){
					if(gotRetry){//This might be a zombied connection. Resub to events, re-request the pages.
						gotRetry = false;
						return composeExport(runeExport,name,reInit());
					}else{
						conref = null;//This is what happens when the user spams the export button while the client is loading.
						return close(Integer.valueOf(code));
					}
				}else{
					return COMPLETED_STAGE;
				}
			}
			conref = null;
			return CompletableFuture.completedFuture(t.getLocalizedMessage());
		}).thenCompose(a -> a);
	}
	public ClientBuildListModel getListModel(AssetManager mgr){
		return pageListModel = new ClientBuildListModel(pageList,mgr);
	}
	public String getLocale() throws InterruptedException{
		try{
			CompletableFuture<Object> ft = localeString;
			if(ft != null && ft.isDone() && !ft.isCompletedExceptionally()){
				return (String)ft.get();
			}
			return (String)tryConnect(true).get();
		}catch(ExecutionException e){
			e.printStackTrace();
			return null;
		}
	}
	public boolean isConnected(){
		CompletableFuture<?> ft = state.get();
		return ft != null && ft.isDone() && !ft.isCompletedExceptionally();
	}
	public void setChangeListener(Runnable ls){
		this.changeListener = ls;
		ls.run();
	}
	private CompletableFuture<? extends Object> fetchLocale0(){
		return localeString = client.sendAsync(conref.apply("riotclient/get_region_locale").GET().build(), JSONBodySubscriber.HANDLE_UTF8)
				.thenApply(o -> JSON.toJSMap(bodyOf(o)).peekString("locale"));
	}
	private void fireChange(){
		Runnable ls = changeListener;
		if(ls != null){
			EventQueue.invokeLater(changeListener);
		}
	}
	public int getState(){
		CompletableFuture<WebSocket> ws = state.get();
		if(ws == null){
			return STATE_NOT_KNOWN;
		}else{
			if(ws.isDone()){
				try{
					ws.getNow(null);
					return STATE_CONNECTED;
				}catch(CompletionException | CancellationException e){
					Throwable x = e.getCause();
					while(x != null){
						if(x == LOL_NOT_RUNNING){
							return STATE_NOT_RUNNING;
						}
						x = x.getCause();
					}
					return STATE_ERROR;
				}
			}
			return STATE_CONNECTING;
		}
	}
	@SuppressWarnings("unchecked")
	private CompletableFuture<?> tryConnect(boolean fetchLocale){
		CompletableFuture<WebSocket> nf = null;
		CompletableFuture<WebSocket> st;
		do{
			st = state.get();
			if(st != null && !st.isCompletedExceptionally()){
				if(fetchLocale){
					CompletableFuture<Object> lcs = localeString;
					if(lcs == null){
						return fetchLocale0();
					}else if(lcs.isDone()){
						return lcs.isCompletedExceptionally() ? fetchLocale0() : lcs;
					}else{
						return lcs.exceptionallyCompose(th -> (CompletableFuture<Object>)fetchLocale0());
					}
				}
				return st;
			}
			if(nf == null){
				nf = new CompletableFuture<>();
			}
		}while(!state.compareAndSet(st, nf));
		gotRetry = true;
		try{
			String processData = new String(//To get the password and port.
				ppFetcher.start().getInputStream().readAllBytes(),StandardCharsets.UTF_8);
			Matcher pwMatcher = PASSWORD_PATTERN.matcher(processData),
					poMatcher = PORT_PATTERN.matcher(processData);
			if(!(pwMatcher.find() && poMatcher.find())){
				nf.completeExceptionally(LOL_NOT_RUNNING);
				fireChange();
				return nf;
			}
			fireChange();
			String password = pwMatcher.group("group");
			int port = Integer.parseInt(poMatcher.group("group"));
			String authString = "Basic "+Base64.getEncoder().encodeToString(("riot:"+password).getBytes());
			HttpRequest.Builder builder = HttpRequest.newBuilder().header("Authorization", authString);
			String base = "https://"+address+":"+port+"/";
				//return CompletableFuture.completedFuture(null);
			final CompletableFuture<WebSocket> newFuture = nf;
			conref = (String str) -> builder.copy().uri(uri(base+str));
			if(fetchLocale){
				nf = (CompletableFuture<WebSocket>)fetchLocale0();
			}
			CompletableFuture<WebSocket> mainFuture = 
						client.sendAsync(conref.apply("lol-summoner/v1/current-summoner").GET().build(), DISCARD)
						.thenCompose(o -> {
							bodyOf(o);
							return client.newWebSocketBuilder().header("Authorization", authString)
							.buildAsync(uri("wss://"+address+":"+port), this);
						});
						//client.newWebSocketBuilder().header("Authorization", authString)
						//.buildAsync(uri("wss://"+address+":"+port), this);			
			mainFuture = composeSubscribe(mainFuture,1).handle((WebSocket ws,Throwable th) -> {	
							if(th == null){
								newFuture.complete(ws);
							}else{
								newFuture.completeExceptionally(th);
								close(null);
							}
							return ws;
						});
			Runnable ls = changeListener;
			if(ls != null){
				mainFuture.thenRunAsync(ls, EVENT_QUEUE);
			}
		}catch(Throwable e){
			e.printStackTrace();
			nf.completeExceptionally(e);
		}
		return nf;
	}
	private CompletableFuture<WebSocket> reInit(){
		CompletableFuture<WebSocket> nf = null;
		CompletableFuture<WebSocket> st;
		do{
			st = state.get();
			if(nf == null){
				nf = new CompletableFuture<>();
			}
		}while(!state.compareAndSet(st, nf));
		final CompletableFuture<WebSocket> newFuture = nf;
		st = composeSubscribe(st, 0);
		st.handle((WebSocket ws,Throwable th) -> {	
			if(th == null){
				newFuture.complete(ws);
			}else{
				newFuture.completeExceptionally(th);
				close(null);
			}
			return ws;
		});
		return st;
	}
	private CompletableFuture<WebSocket> composeSubscribe(CompletableFuture<WebSocket> ft,int request){
		return ft.thenCompose(ws -> ws.sendText("[5, \"OnJsonApiEvent_lol-perks_v1_pages\"]", true))
				.thenCompose(ws -> ws.sendText("[5, \"OnJsonApiEvent_lol-perks_v1_inventory\"]", true))
				.thenCompose(ws -> {
					CompletableFuture<Void> pages =
							client.sendAsync(conref.apply("lol-perks/v1/pages").GET().build(), JSONBodySubscriber.HANDLE_UTF8)
							.thenApply(ClientManager::bodyOf)
							.thenAccept(this::updatePages);
					CompletableFuture<Void> inventory =
							client.sendAsync(conref.apply("lol-perks/v1/inventory").GET().build(), JSONBodySubscriber.HANDLE_UTF8)
							.thenApply(ClientManager::bodyOf)
							.thenAccept(this::updateInventory);
					return pages.thenCombine(inventory, (Object a,Object b) -> {ws.request(request);return ws;})
							.exceptionallyCompose(th -> {
								return ws.sendClose(1000, "Unable to fetch page data");
					});
				});
	}
	private void updateInventory(Object data){
		int pc = JSON.toJSMap(data).peekInt("ownedPageCount",-1);
		if(pc >= 0){
			ownedPageCount = pc;
		}
	}
	private void updatePages(Object data){
		JSList ls = JSON.asJSList(data,false);
		if(ls != null){
			pageList = ls;
			ClientBuildListModel model = pageListModel;
			if(model != null){
				EventQueue.invokeLater(() -> model.update(ls));
			}
		}
	}
	@Override
	public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
		webSocket.request(1);
		return null;
	}
	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		state.set(null);
		LockSupport.unpark(seekerThread);
		fireChange();
		return null;
	}
	@Override
	public void onError(WebSocket webSocket, Throwable error) {
		state.set(null);
		LockSupport.unpark(seekerThread);
		fireChange();
		error.printStackTrace();
	}
	@Override
	public void onOpen(WebSocket webSocket) {
		System.out.println("ClientManager: Opened");
	}
	@Override
	public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
		webSocket.request(1);
		return null;
	}
	@Override
	public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
		webSocket.request(1);
		return null;
	}
	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		try{
			textCollator.append(data);
			if(last){
				String str = textCollator.toString();
				textCollator.setLength(0);
				if(!str.isBlank()){
					JSList list = JSON.toJSList(JSON.parse(str));
					if(list.peekInt(0) == 8){
						switch(list.peekString(1,"")){
						case "OnJsonApiEvent_lol-perks_v1_pages":updatePages(list.getJSMap(2).get("data"));break;
						case "OnJsonApiEvent_lol-perks_v1_inventory":updateInventory(list.getJSMap(2).get("data"));break;
						}
					}
				}
			}
		}finally{
			webSocket.request(1);
		}
		return null;
	}
}
