package net.balintgergely.runebook;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import net.balintgergely.util.JSON;
import net.balintgergely.util.JSON.PrettyWriter;
import net.balintgergely.util.JSONBodyPublishing;
import net.balintgergely.util.JSONBodySubscriber;

public class LCUManager {
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
	/**
	 * Patterns.
	 */
	private static final Pattern	PASSWORD_PATTERN = Pattern.compile("--remoting-auth-token=(?<password>[^ \\\"]+)"),
									PORT_PATTERN = Pattern.compile("--app-port=(?<port>\\d+)");
	private static final CompletionStage<String> NULL_STRING_STAGE = CompletableFuture.completedStage(null);
	public static final int STATE_NOT_KNOWN = 0,
							STATE_NOT_RUNNING = 1,
							STATE_CONNECTING = 2,
							STATE_CONNECTED = 3,
							STATE_ERROR = 4;
	static <E> E bodyOf(HttpResponse<E> response){
		if(response.statusCode()/100 != 2){
			throw new RuntimeException(response.uri()+" Received status code: "+response.statusCode());
		}
		return response.body();
	}
	private static final Throwable LOL_NOT_RUNNING = new Throwable("League of Legends is not running.",null,false,false){
		private static final long serialVersionUID = 1L;
		@Override
		public Throwable initCause(Throwable cause) {
			return this;
		}
	};///lol-loadouts/v4/loadouts/scope/account
	@SuppressWarnings("resource")
	public static void main(String[] atgs) throws Throwable{
		ProcessBuilder 
		ppFetcher = new ProcessBuilder("WMIC", "process", "where", "name='LeagueClientUx.exe'", "get", "commandLine");
		String address = InetAddress.getLoopbackAddress().getHostAddress();
		String processData = new String(ppFetcher.start().getInputStream().readAllBytes(),StandardCharsets.UTF_8);
		Matcher pwMatcher = PASSWORD_PATTERN.matcher(processData),
				poMatcher = PORT_PATTERN.matcher(processData);
		if(!(pwMatcher.find() && poMatcher.find())){
			System.out.println("Couldn't find League");
		}
		String password = pwMatcher.group("password");
		String port = poMatcher.group("port");
		HttpClient httpClient = HttpClient.newBuilder().sslContext(LCUManager.makeContext()).build();
		Function<String,HttpRequest.Builder> conref;
		{
			String authString = "Basic "+Base64.getEncoder().encodeToString(("riot:"+password).getBytes());
			HttpRequest.Builder builder = HttpRequest.newBuilder().header("Authorization", authString);
			String base = "https://"+address+":"+port+"/";
			conref = (String str) -> builder.copy().uri(uri(base+str));
			System.out.println(base);
			System.out.println("riot:"+password);//3159,3477
		}// lol-game-data/assets/v1/summoner-emotes.json XXX THIS IS IT!!!!!!
		//To get chroma names use this: GET /lol-store/v1/catalog
		//PrettyWriter outputWriter = new PrettyWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
		PrettyWriter outputWriter = new PrettyWriter(new FileWriter("lcu.output"));
		try(BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8))){
			String call;
			main: while((call = inputReader.readLine()) != null){
				if(!call.isBlank()){
					String[] pair = call.split(" ",2);
					if(pair.length < 2){
						switch(call.toLowerCase()){
						case "end":break main;
						}
					}else{
						pair[0] = pair[0].toUpperCase();
						if(pair[1].startsWith("/")){
							pair[1] = pair[1].substring(1);
						}
						HttpRequest.Builder builder = conref.apply(pair[1]);
						switch(pair[0]){
						case "GET":builder.GET();break;
						case "DELETE":builder.DELETE();break;
						default:System.out.println("Body:");
							builder.method(pair[0],JSONBodyPublishing.publish(JSON.readObject(inputReader)));break;
						}
						HttpResponse<InputStream> response = httpClient.send(builder.build(),BodyHandlers.ofInputStream());
						System.out.println("Status code: "+response.statusCode());
						try(InputStreamReader input = new InputStreamReader(response.body(), StandardCharsets.UTF_8)){
							input.transferTo(outputWriter);
						}
						outputWriter.flush();
						System.out.println();
					}
				}
			}
			
		}
		/*{//Code to test emotes.
			HttpResponse<Object> response = 
		httpClient.send(builder.copy().uri(uri(base+"lol-summoner/v1/current-summoner")).GET().build(), JSONBodySubscriber.HANDLE_UTF8);
			System.out.println(response.statusCode());
			PrettyWriter writer = new PrettyWriter(new OutputStreamWriter(System.out));
			JSON.write(response.body(), writer);
			writer.flush();
		}*/
	}
	public static Function<String,HttpRequest.Builder> independentConref() throws IOException{
		ProcessBuilder 
		ppFetcher = new ProcessBuilder("WMIC", "process", "where", "name='LeagueClientUx.exe'", "get", "commandLine");
		String address = InetAddress.getLoopbackAddress().getHostAddress();
		String processData = new String(ppFetcher.start().getInputStream().readAllBytes(),StandardCharsets.UTF_8);
		Matcher pwMatcher = PASSWORD_PATTERN.matcher(processData),
				poMatcher = PORT_PATTERN.matcher(processData);
		if(!(pwMatcher.find() && poMatcher.find())){
			System.out.println("Couldn't find League");
		}
		String password = pwMatcher.group("password");
		String port = poMatcher.group("port");
		String authString = "Basic "+Base64.getEncoder().encodeToString(("riot:"+password).getBytes());
		HttpRequest.Builder builder = HttpRequest.newBuilder().header("Authorization", authString);
		String base = "https://"+address+":"+port+"/";
		return (String str) -> builder.copy().uri(uri(base+str));
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
	private EventListenerList listenerList = new EventListenerList();
	private ChangeEvent changeEvent = new ChangeEvent(this);
	private final ProcessBuilder ppFetcher;
	final HttpClient client;
	private final String address;
	private WAMPManager wamp;
	private volatile CompletableFuture<WebSocket> shortState;
	volatile Function<String,HttpRequest.Builder> conref;
	private volatile boolean manuallyClosed = false;
	private Thread lcuSeekerThread = new Thread(this::seekerRun,"LCU-Seeker-Thread");
	private Map<LCUDataRoute,BiConsumer<String,Object>> routeMap = new HashMap<>();
	private Map<Class<? extends LCUModule>,LCUModule> modules = new HashMap<>();
	private Consumer<Object> consumerForSummoner = a -> {},consumerForChampionData = a -> {};
	{lcuSeekerThread.setDaemon(true);}
	LCUManager(HttpClient client){
		this.client = client;
		address = InetAddress.getLoopbackAddress().getHostAddress();
		String osName = System.getProperty("os.name");
		String osLowerCase = osName.toLowerCase();
		if(osLowerCase.contains("win")){//WMIC process where name='LeagueClientUx.exe' get commandLine
			ppFetcher = new ProcessBuilder("WMIC", "process", "where", "name='LeagueClientUx.exe'", "get", "commandLine");
		}else if(osLowerCase.contains("mac")){
			ppFetcher = new ProcessBuilder("ps", "x", "|", "grep", "'LeagueClientUx.exe'");
		}else{
			throw new Error("Connecting to the League Client on "+osName+" is not supported.");
		}
		wamp = new WAMPManager(this::wampUpdate);
	}
	public <E extends LCUModule> E initModule(Constructor<E> cns,Object extra){
		Class<E> cl = cns.getDeclaringClass();
		@SuppressWarnings("unchecked")
		E e = (E)modules.get(cl);
		if(e == null){
			if(wamp.isListenerMapSet()){
				throw new IllegalStateException();
			}
			try {
				e = cns.newInstance(this,extra);
			}catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				throw new RuntimeException(e1);
			}
			modules.put(cl, e);
		}
		return e;
	}
	public boolean isInitialized(){
		return wamp.isListenerMapSet();
	}
	synchronized void finishInitialization(){
		if(wamp.isListenerMapSet()){
			throw new IllegalStateException();
		}
		LCUSummonerManager mgr = getModule(LCUSummonerManager.class);
		if(mgr != null){
			consumerForSummoner = mgr::updateCurrentSummoner;
			consumerForChampionData = mgr::championsChanged;
		}
		modules = Collections.unmodifiableMap(modules);
		for(LCUModule md : modules.values()){
			routeMap.putAll(md.getDataRoutes());
		}
		Map<String,BiConsumer<String,Object>> eventMap = new HashMap<>();
		for(Map.Entry<LCUDataRoute, BiConsumer<String,Object>> entry : routeMap.entrySet()){
			LCUDataRoute rt = entry.getKey();
			eventMap.put(rt.event, entry.getValue());
		}
		wamp.setListenerMap(eventMap);
		eventMap = null;
		if(shortState != null && !shortState.isCompletedExceptionally()){
			shortState = shortState
				.thenCompose(webSocket ->
					simpleGet("lol-champions/v1/owned-champions-minimal")
					.thenAccept(consumerForChampionData)
					.thenCompose(o -> simpleGet("lol-summoner/v1/current-summoner"))
					.thenAccept(consumerForSummoner).thenCompose((a) -> fetchAllData())
					.handle((a,t) -> {
						if(t != null){
							wamp.softAbort(webSocket);
							t.printStackTrace();
							throw new CancellationException();
						}
						return webSocket;
					}
				)
			);
		}
	}
	@SuppressWarnings("unchecked")
	public <E extends LCUModule> E getModule(Class<E> type){
		return (E)modules.get(type);
	}
	private void seekerRun(){
		while(true){
			try{
				while(manuallyClosed){
					LockSupport.park();
				}
				CompletableFuture<?> ft = tryConnect();
				long nextTime = System.currentTimeMillis()+10000;
				while(!ft.isDone()){
					try{
						ft.get();
					}catch(CancellationException | ExecutionException t){}
				}//ft must be done.
				if(!ft.isCompletedExceptionally()){
					while(wamp.getCurrentStage() != null){
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
	}
	public void startSeekerThread(){
		if(!wamp.isListenerMapSet()){
			throw new IllegalStateException();
		}
		lcuSeekerThread.start();
	}
	public <E> CompletableFuture<E> close(E response){
		manuallyClosed = true;
		CompletableFuture<?> wcl = wamp.close();
		if(wcl == null){
			return CompletableFuture.completedFuture(response);
		}else{
			return wcl.handle((a,b) -> response);
		}
	}
	private synchronized CompletableFuture<Void> fetchAllData(){
		CompletableFuture<?>[] futures = new CompletableFuture<?>[routeMap.size()];
		int index = 0;
		for(Map.Entry<LCUDataRoute, BiConsumer<String,Object>> entry : routeMap.entrySet()){
			LCUDataRoute route = entry.getKey();
			BiConsumer<String,Object> recipient = entry.getValue();
			if(route.mayNotExist){
				futures[index++] = client.sendAsync(conref.apply(route.route).GET().build(), JSONBodySubscriber.HANDLE_UTF8)
				.thenAccept((HttpResponse<Object> response) -> {
					if(response.statusCode()/100 == 2){
						recipient.accept(null,response.body());
					}else{
						recipient.accept(null,null);
					}
				});
			}else{
				futures[index++] = (CompletableFuture<?>)NULL_STRING_STAGE.thenAcceptBoth(simpleGet(route.route), recipient);
			}
		}
		return CompletableFuture.allOf(futures);
	}
	public String fetchLocale(){
		tryConnect();
		if(conref != null){
			try{
				return client.sendAsync(conref.apply("riotclient/get_region_locale").GET().build(), JSONBodySubscriber.HANDLE_UTF8)
				.thenApply(o -> JSON.toJSMap(bodyOf(o)).peekString("locale")).get();
			}catch(Throwable t){
				t.printStackTrace();
			}
		}
		return null;
	}
	synchronized CompletableFuture<WebSocket> tryConnect(){
		if(shortState != null){
			if(shortState.isCompletedExceptionally()){
				shortState = null;
			}else{
				return shortState;
			}
		}
		try{
			String processData = new String(ppFetcher.start().getInputStream().readAllBytes(),StandardCharsets.UTF_8);
			Matcher pwMatcher = PASSWORD_PATTERN.matcher(processData),
					poMatcher = PORT_PATTERN.matcher(processData);
			if(!(pwMatcher.find() && poMatcher.find())){
				EventQueue.invokeLater(this::fireChange);
				return shortState = CompletableFuture.failedFuture(LOL_NOT_RUNNING);
			}
			String password = pwMatcher.group("password");
			String port = poMatcher.group("port");
			String authString = "Basic "+Base64.getEncoder().encodeToString(("riot:"+password).getBytes());
			{
				HttpRequest.Builder builder = HttpRequest.newBuilder().header("Authorization", authString);
				String base = "https://"+address+":"+port+"/";
				conref = (String str) -> builder.copy().uri(uri(base+str));
			}
			shortState = simpleGet("lol-champions/v1/owned-champions-minimal")
				.thenAccept(consumerForChampionData)
				.thenCompose(o -> simpleGet("lol-summoner/v1/current-summoner"))
				.thenAccept(consumerForSummoner)
				.thenCompose(o -> wamp.open(client.newWebSocketBuilder().header("Authorization", authString),uri("wss://"+address+":"+port)))
				.thenCompose(webSocket -> fetchAllData().handle((a,t) -> {
				if(t != null){
					wamp.softAbort(webSocket);
					t.printStackTrace();
					throw new CancellationException();
				}
				return webSocket;
			}));
			EventQueue.invokeLater(this::fireChange);
			if(shortState != null){
				shortState.whenCompleteAsync((a,b) -> this.fireChange(), EVENT_QUEUE);
			}
			return shortState;
		}catch(Throwable t){
			return shortState = CompletableFuture.failedFuture(t);
		}finally{
			if(manuallyClosed){
				manuallyClosed = false;
				LockSupport.unpark(lcuSeekerThread);
			}
		}
	}
	synchronized CompletableFuture<?> refreshConnection(CompletableFuture<?> stageToRefresh){
		if(shortState != stageToRefresh || (shortState != null && shortState.isCompletedExceptionally())){
			return shortState;
		}
		Objects.requireNonNull(wamp);
		return shortState = shortState.thenCompose(w -> wamp.subscribeAgain().thenCompose(o -> fetchAllData()).handle((k,t) -> {
			if(t != null){
				wamp.softAbort(w);
				t.printStackTrace();
				throw new CancellationException();
			}
			return w;
		}));
	}
	private synchronized void wampUpdate(WAMPManager manager,WebSocket webSocket){
		if(webSocket == null){
			shortState = null;
			LockSupport.unpark(lcuSeekerThread);
		}
		EventQueue.invokeLater(this::fireChange);
	}
	private CompletableFuture<Object> simpleGet(String route){
		return client.sendAsync(conref.apply(route).GET().build(),JSONBodySubscriber.HANDLE_UTF8).thenApply(LCUManager::bodyOf);
	}
	private void fireChange(){
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i -= 2){
			if(listeners[i] == ChangeListener.class){
				((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
			}
		}
	}
	public void addChangeListener(ChangeListener x) {
		listenerList.add(ChangeListener.class, x);
	}
	public void removeChangeListener(ChangeListener x) {
		listenerList.remove(ChangeListener.class, x);
	}
	public int getState(){
		CompletableFuture<WebSocket> ws = shortState;
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
	public static class LCUDataRoute{
		public final String route;
		public final String event;
		public final boolean mayNotExist;
		public LCUDataRoute(String route,String event,boolean mayNotExist){
			this.route = route;
			this.event = event;
			this.mayNotExist = mayNotExist;
		}
		@Override
		public int hashCode(){
			return route.hashCode() ^ event.hashCode();
		}
		@Override
		public boolean equals(Object that){
			if(that instanceof LCUDataRoute){
				LCUDataRoute rt = (LCUDataRoute)that;
				return mayNotExist == rt.mayNotExist && route.equals(rt.route) && event.equals(rt.event);
			}
			return false;
		}
	}
	interface LCUModule{
		Map<LCUDataRoute,BiConsumer<String,Object>> getDataRoutes();
	}
}
