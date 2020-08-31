package net.balintgergely.runebook;

import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.HttpRequest.BodyPublisher;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;
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
import javax.swing.table.AbstractTableModel;

import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;
import net.balintgergely.util.JSONBodyPublishing;
import net.balintgergely.util.JSONBodySubscriber;

public class LCUManager {
	/**
	 * State space:
	 * Mastery 0,1,2,3,4: Not owned
	 * 
	 */
	public static final int		MASTERY_MASK =		0x7,
								MASTERY_CHEST =		0x8,
								KNOWN_OWNED =		0x10,
								KNOWN_NOT_OWNED =	0x20,
								HOVERED	=			0x40,
								
								FREE_ROTATION =		0x100,
								BANNED =			0x200,
								OWNED_LOCALLY =		0x400,
								PICKED_BY_ENEMY =	0x800,
								
								UNAVAILABLE =		0xA00;
	
	private static final int	GS_FREE_ROTATION =	0x1,
								GS_BANNED =			0x2,
								GS_OWNED =			0x4,
								GS_PICKED_BY_ENEMY =0x8,
								
								GS_UNAVAILABLE =	0xA;
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
	public static final int STATE_NOT_KNOWN = 0,
							STATE_NOT_RUNNING = 1,
							STATE_CONNECTING = 2,
							STATE_CONNECTED = 3,
							STATE_ERROR = 4;
	private static <E> E bodyOf(HttpResponse<E> response){
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
	};
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
	private static int indexOfChampion(List<? extends Champion> championList,int key){
		int min = 0;
		int max = championList.size() - 1;
		while (min <= max) {
			int mid = (min + max) >>> 1;
			int midKey = championList.get(mid).key;
			if(midKey < key){
			    min = mid + 1;
			}else if(midKey > key){
			    max = mid - 1;
			}else{
			    return mid;
			}
		}
		return -1;
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
	private final HttpClient client;
	private final String address;
	private final WAMPManager wamp;
	final LCUPerksManager perkManager;
	final LCUSummonerManager championsManager;
	private volatile CompletableFuture<WebSocket> shortState;
	private volatile Function<String,HttpRequest.Builder> conref;
	private volatile boolean manuallyClosed = false;
	private Thread lcuSeekerThread = new Thread(this::seekerRun,"LCU-Seeker-Thread");
	{lcuSeekerThread.setDaemon(true);}
	LCUManager(HttpClient client){
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
		perkManager = new LCUPerksManager();
		championsManager = new LCUSummonerManager();
		wamp = new WAMPManager(this::wampUpdate, Map.of(
				"OnJsonApiEvent_lol-perks_v1_pages",perkManager::perksChanged,
				"OnJsonApiEvent_lol-perks_v1_inventory",perkManager::inventoryChanged,
				"OnJsonApiEvent_lol-champ-select_v1_session",championsManager::sessionChanged,
				"OnJsonApiEvent_lol-champions_v1_owned-champions-minimal",championsManager::championsChanged,
				"OnJsonApiEvent_lol-lobby_v2_lobby",championsManager::lobbyChanged
		));
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
		return CompletableFuture.allOf(
			simpleGet("lol-perks/v1/pages").thenAccept(perkManager::perksChanged),
			simpleGet("lol-perks/v1/inventory").thenAccept(perkManager::inventoryChanged),
			simpleGet("lol-champions/v1/owned-champions-minimal").thenAccept(championsManager::championsChanged),
			client.sendAsync(conref.apply("lol-lobby/v2/lobby").GET().build(),
			JSONBodySubscriber.HANDLE_UTF8).thenAccept((HttpResponse<Object> response) -> {
				if(response.statusCode()/100 == 2){
					championsManager.lobbyChanged(response.body());
				}else{
					championsManager.lobbyChanged(null);
				}
			}),
			client.sendAsync(conref.apply("lol-champ-select/v1/session").GET().build(),
			JSONBodySubscriber.HANDLE_UTF8).thenAccept((HttpResponse<Object> response) -> {
				if(response.statusCode()/100 == 2){
					championsManager.sessionChanged(response.body());
				}else{
					championsManager.sessionChanged(null);
				}
			})
		);
	}
	private synchronized CompletableFuture<WebSocket> tryConnect(){
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
			{
				String authString = "Basic "+Base64.getEncoder().encodeToString(("riot:"+password).getBytes());
				HttpRequest.Builder builder = HttpRequest.newBuilder().header("Authorization", authString);
				String base = "https://"+address+":"+port+"/";
				conref = (String str) -> builder.copy().uri(uri(base+str));
			}
			String authString = "Basic "+Base64.getEncoder().encodeToString(("riot:"+password).getBytes());
			shortState = simpleGet("lol-summoner/v1/current-summoner")
				.thenAccept(championsManager::updateCurrentSummoner)
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
	private synchronized CompletableFuture<?> refreshConnection(CompletableFuture<?> stageToRefresh){
		if(shortState != stageToRefresh || (shortState != null && shortState.isCompletedExceptionally())){
			return shortState;
		}
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
	/**
	 * LCUPerksManager is responsible for maintaining a local image of the perks stored by the LCU.
	 * @author balintgergely
	 */
	public class LCUPerksManager extends HybridListModel<Build>{
		private static final long serialVersionUID = 1L;
		private ArrayList<Build> buildList = new ArrayList<>();
		private HashMap<Number,Build> buildMap = new HashMap<>();
		private RuneModel model;
		private JSList perkList;
		private int ownedPageCount = -1;
		private LCUPerksManager(){}
		@Override
		public Build getElementAt(int index) {
			return buildList.get(index);
		}
		@Override
		public int getSize() {
			return buildList.size();
		}
		public void setRuneModel(RuneModel model){
			this.model = model;
			JSList pr = perkList;
			if(pr != null){
				EventQueue.invokeLater(() -> update(pr));
			}
		}
		public CompletableFuture<Object> exportRune(Rune rune,String name){
			BodyPublisher runeExport = JSONBodyPublishing.publish(rune.toJSMap().put("name", name, "current", Boolean.TRUE));
			CompletableFuture<?> strf = tryConnect();
			CompletableFuture<Object> response = strf.thenCompose(o -> exportRuneInternal(runeExport,name));
			return response.exceptionallyCompose(t ->
				refreshConnection(strf).thenCompose(o1 -> exportRuneInternal(runeExport,name))
			);
		}
		private CompletableFuture<Object> exportRuneInternal(BodyPublisher bp,String name){
			JSList runeList = perkList;
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
				exportRequest = conref.apply("lol-perks/v1/pages").POST(bp).build();
			}else{
				int pageId = pageToReplace.getInt("id");
				exportRequest = conref.apply("lol-perks/v1/pages/"+pageId).PUT(bp).build();
			}
			return client.sendAsync(exportRequest, JSONBodySubscriber.HANDLE_UTF8).thenApply(LCUManager::bodyOf);
		}
		private void inventoryChanged(Object o){
			int pc = JSON.toJSMap(o).peekInt("ownedPageCount",-1);
			if(pc >= 0){
				ownedPageCount = pc;
			}
		}
		private void perksChanged(Object o){
			if(o instanceof JSList){
				JSList pl = (JSList)o;
				this.perkList = pl;
				EventQueue.invokeLater(() -> update(pl));
			}
		}
		private void update(JSList builds){
			if(model == null){
				return;
			}
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
							boolean editable = m.peekBoolean("isEditable");
							Rune rn = model.parseRune(m);
							Build bld = buildMap.get(id);
							if(bld == null){
								buildMap.put(id, bld = new Build(name, null, rn, editable ? (byte)0x80 : (byte)0xC0, timestamp));
							}else{
								if(bld == selectedBuild){
									indexOfSelectedBuild = buildList.size();
								}
								bld.setName(name);
								bld.setOrder(timestamp);
								bld.setRune(rn);
								bld.setFlags((byte)((bld.getFlags() & 0x3F) | (editable ? 0x80 : 0xC0)));
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
	private static class Summoner{
		private byte[] rt;
		private String name;
		private final int summonerId;
		private int hoveredChampionIndex = -1;
		private boolean isDetailed = false;
		private boolean isLockedIn = false;
		private boolean isExcludedFromFreeRotation = false;
		private int pghl = 0;
		private Summoner(int id){
			this.summonerId = id;
		}
		private boolean pghlEqualsZero(){
			return pghl == 0;
		}
	}
	/**
	 * LCUSummonerManager maintains a small database of summoners encountered in-game
	 * as part of a premade group or teammates with drafting. It is responsible for
	 * fetching and storing their relationships with champions like mastery or what champions
	 * do they own. This class has two modes: Detailed and non-detailed. In detailed mode
	 * the class makes additional client callbacks to fill out information not received through events.
	 * In non-detailed mode it does not.
	 */
	public class LCUSummonerManager extends AbstractTableModel implements Comparator<Champion>{
		private static final long serialVersionUID = 1L;
		/*
		 * Information about who owns which champions is difficult to get. For the current summoner
		 * we are able to just fetch the list of owned champions. For other summoners it is trickier.
		 * In order to present a fine table of possible trades we do the following:
		 * 
		 * -Mark the champions part of the weekly free rotation as available to summoners above level 10.
		 * -For each summoner, mark all champions on at least mastery level 5 or with the chest granted flag as owned.
		 * -If a summoner hovers any champion not part of the weekly free rotation, we know they own that champion.
		 */
		private List<Champion> championList;
		private byte[] globalStates;
		private boolean detailedMode;
		private JSMap localSummonerData = JSMap.EMPTY_MAP;
		private JSMap sessionData;
		private JSList championsData;
		private JSMap lobbyData = JSMap.EMPTY_MAP;
		private HashMap<Integer,Summoner> summonerCache = new HashMap<>();
		/**
		 * We have exactly 10 slots. First N slots are always the last active team of size N.
		 */
		private Summoner[] myTeam = new Summoner[10];
		private Summoner mySummoner;
		private int sessionId = Integer.MIN_VALUE;
		private int pghlProgress;
		private int teamSize;
		private boolean pghl(Summoner sm,int index0){
			sm.pghl = ++pghlProgress;
			Summoner sm0 = myTeam[index0];
			if(sm0 != sm){
				for(int s = 0;s < myTeam.length;s++){
					if(myTeam[s] == sm || myTeam[s] == null){
						myTeam[s] = sm0;
						sm0 = null;
					}
				}
				int index1 = teamSize;
				if(sm0 != null && index1 < myTeam.length){
					int minIndex = -1;
					int minPghl = sm0 == mySummoner ? Integer.MAX_VALUE : sm0.pghl;
					while(index1 < myTeam.length){
						Summoner sm1 = myTeam[index1];
						if(sm1.pghl < minPghl){
							minPghl = sm1.pghl;
							minIndex = index1;
						}
						index1++;
					}
					if(minIndex >= 0){
						myTeam[minIndex] = sm0;
					}else if(sm0 != mySummoner){
						sm0.pghl = 0;
						sm0.isDetailed = false;
					}
				}
				myTeam[index0] = sm;
				if(detailedMode){
					sm.isDetailed = true;
					fetchMastery(sm.summonerId);
					if(sm.name == null){
						fetchSummonerData(sm.summonerId);
					}
				}
				return true;
			}
			return false;
		}
		public void setChampionList(List<Champion> champions){
			if(championList == null){
				int length = champions.size();
				championList = champions;
				globalStates = new byte[length];
				updateCurrentSummoner();
				lobbyChanged();
				championsChanged();
				sessionChanged();
			}else{
				throw new IllegalStateException("Champion list already set!");
			}
		}
		public void setDetailed(boolean enabled){
			if(enabled != detailedMode){
				if(enabled){
					if(championList == null){
						throw new IllegalStateException("Model not ready!");
					}
					detailedMode = true;
					for(Summoner sm : myTeam){
						if(sm != null){
							if(!sm.isDetailed){
								sm.isDetailed = true;
								fetchMastery(sm.summonerId);
								if(sm.name == null){
									fetchSummonerData(sm.summonerId);
								}
							}
						}
					}
				}else{
					detailedMode = false;
				}
			}
		}
		public int getNPreferredChampions(int[] array){
			int index = 0;
			if(array.length == index || mySummoner == null){
				return index;
			}
			boolean imbl = mySummoner.isExcludedFromFreeRotation;
			if(mySummoner.hoveredChampionIndex >= 0){
				array[index++] = mySummoner.hoveredChampionIndex;
				if(array.length == index){
					return index;
				}
			}
			for(int i = 0;i < teamSize;i++){
				Summoner sm = myTeam[i];
				if(sm != null && sm != mySummoner){
					int hoverIndex = sm.hoveredChampionIndex;
					if(hoverIndex >= 0 && (
							 ((mySummoner.rt[hoverIndex] & KNOWN_OWNED) != 0) ||
							!(imbl || (globalStates[hoverIndex] & GS_FREE_ROTATION) == 0)
					)){
						array[index++] = hoverIndex;
						if(array.length == index){
							return index;
						}
					}
				}
			}
			return index;
		}
		public int getTeamSize(){
			return teamSize;
		}
		private void updateCurrentSummoner(Object obj){
			localSummonerData = JSON.toJSMap(obj);
			EventQueue.invokeLater(this::updateCurrentSummoner);
		}
		private void updateCurrentSummoner(){
			JSMap jsm = localSummonerData;
			int id = jsm.peekInt("summonerId");
			if(id != 0 && (mySummoner == null || mySummoner.summonerId != id)){
				mySummoner = summonerCache.computeIfAbsent(id, Summoner::new);
				if(mySummoner.pghl == 0){
					mySummoner.pghl = 1;
				}
				mySummoner.name = jsm.getString("displayName");
				mySummoner.isExcludedFromFreeRotation = (jsm.getInt("summonerLevel") <= 10);
			}
			if(mySummoner != null && championList != null && mySummoner.rt == null){
				mySummoner.rt = new byte[globalStates.length];
			}
		}
		private void championsChanged(Object obj){
			championsData = JSON.asJSList(obj);
			EventQueue.invokeLater(this::championsChanged);
		}
		private void championsChanged(){
			if(championList != null){
				JSList chd = championsData;
				if(chd != null){
					for(int i = 0;i < globalStates.length;i++){
						globalStates[i] &= GS_BANNED;
					}
					for(Object champion : chd){
						JSMap championData = JSON.asJSMap(champion);
						Integer id = championData.getInt("id");
						int index = indexOfChampion(championList, id);
						if(index >= 0){
							if(championData.getBoolean("freeToPlay")){
								globalStates[index] |= GS_FREE_ROTATION;
							}
							JSMap ownership = JSON.toJSMap(championData.peek("ownership"));
							if(ownership.peekBoolean("owned") || JSON.toJSMap(ownership.peek("rental")).peekBoolean("rented")){
								globalStates[index] |= GS_OWNED;
							}
						}
					}
					if(mySummoner != null){
						for(int i = 0;i < globalStates.length;i++){
							if((globalStates[i] & GS_OWNED) == 0){
								mySummoner.rt[i] = (byte)((mySummoner.rt[i] & ~KNOWN_OWNED) | KNOWN_NOT_OWNED);
							}else{
								mySummoner.rt[i] = (byte)((mySummoner.rt[i] & ~KNOWN_NOT_OWNED) | KNOWN_OWNED);
							}
						}
					}
					fireTableRowsUpdated(0, globalStates.length-1);
				}
			}
		}
		private void lobbyChanged(Object obj){
			if(obj instanceof JSMap){
				lobbyData = (JSMap)obj;
				EventQueue.invokeLater(this::lobbyChanged);
			}
		}
		private void lobbyChanged(){
			if(sessionId == Integer.MIN_VALUE){
				JSList lst = JSON.toJSList(lobbyData.peek("members"));
				teamSize = lst.size();
				boolean pghlChanged = false;
				for(int i = 0;i < teamSize;i++){
					JSMap summonerData = JSON.toJSMap(lst.get(i));
					int id = summonerData.peekInt("summonerId");
					if(id != 0){
						Summoner sm = summonerCache.computeIfAbsent(id, Summoner::new);
						String name = summonerData.peekString("summonerName");
						if(name != null){
							sm.name = name;
							pghlChanged = true;
						}
						if(pghl(sm, i)){
							pghlChanged = true;
						}
					}
				}
				if(pghlChanged){
					fireTableRowsUpdated();
					fireStateChanged();
				}
			}
		}
		private void sessionChanged(Object obj){
			sessionData = obj == null ? null : JSON.asJSMap(obj);
			EventQueue.invokeLater(this::sessionChanged);
		}
		private void sessionChanged(){
			if(championList != null){
				JSMap sessionLocal = sessionData;
				if(sessionLocal == null){
					sessionId = Integer.MIN_VALUE;
				}else{
					int gameId = sessionLocal.peekInt("gameId");
					boolean pghlChanged = false;
					boolean rowsChanged = false;
					JSList team = sessionLocal.getJSList("myTeam");
					int ts = team.size();
					if(ts > 0){
						teamSize = ts;
					}
					if(gameId != sessionId){
						for(int i = 0;i < globalStates.length;i++){
							globalStates[i] &= ~GS_UNAVAILABLE;
						}
						sessionId = gameId;
					}
					for(int summonerIndex = 0;summonerIndex < ts;summonerIndex++){
						JSMap summonerData = team.getJSMap(summonerIndex);
						int id = summonerData.peekInt("summonerId");
						if(id != 0){
							Summoner summoner = summonerCache.get(id);
							if(summoner == null){
								summonerCache.put(id,summoner = new Summoner(id));
								summoner.rt = new byte[globalStates.length];
							}
							if(pghl(summoner,summonerIndex)){
								pghlChanged = true;
							}
							int championId = summonerData.peekInt("championId");
							int champion = -1;
							if(championId > 0){
								champion = indexOfChampion(championList, championId);
							}
							if(champion < 0){
								champion = indexOfChampion(championList, championId = summonerData.peekInt("championPickIntent"));
							}
							if(champion < 0){
								champion = -1;
							}
							if(summoner.hoveredChampionIndex != champion){
								summoner.hoveredChampionIndex = champion;
								if(champion >= 0){
									if(summoner != mySummoner && (globalStates[champion] & GS_FREE_ROTATION) == 0){
										summoner.rt[champion] |= KNOWN_OWNED;
									}
								}
								rowsChanged = true;
							}
						}
					}
					for(Object actions : sessionLocal.getJSList("actions")){
						for(Object action : JSON.toJSList(actions)){
							JSMap actionData = JSON.toJSMap(action);
							if(actionData.peekBoolean("completed")){
								int dex = indexOfChampion(championList, actionData.peekInt("championId"));
								if(dex >= 0)switch(actionData.peekString("type","")){
								case "ban":
									globalStates[dex] |= GS_BANNED;
									break;
								case "pick":
									if(!actionData.peekBoolean("isAllyAction",true)){
										globalStates[dex] |= GS_PICKED_BY_ENEMY;
									}
									break;
								}
							}
						}
					}
					summonerCache.values().removeIf(Summoner::pghlEqualsZero);
					if(pghlChanged || rowsChanged){
						fireTableRowsUpdated();
					}
					if(pghlChanged){
						fireStateChanged();
					}
				}
			}
		}
		private void fetchMastery(int summonerId){
			client.sendAsync(conref.apply("lol-collections/v1/inventories/"+summonerId+"/champion-mastery").GET().build(),
					JSONBodySubscriber.HANDLE_UTF8)
					.thenAcceptAsync(this::feedMastery, EVENT_QUEUE);
		}
		private void feedMastery(HttpResponse<Object> data){
			if(data.statusCode()/100 == 2){
				Object body = data.body();
				for(Object obj : JSON.toJSList(body)){
					JSMap championData = JSON.toJSMap(obj);
					int index = indexOfChampion(championList, championData.peekInt("championId"));
					Summoner summoner = summonerCache.get(championData.peekInt("playerId"));
					if(index >= 0 && summoner != null){
						int level = championData.peekInt("championLevel",-1);
						boolean chestGranted = championData.peekBoolean("chestGranted");
						int relation = summoner.rt[index];
						if(level >= 0 && level <= 7){
							relation = (relation & ~MASTERY_MASK) | level;
						}
						if(chestGranted){
							relation |= MASTERY_CHEST;
						}
						summoner.rt[index] = (byte)relation;
					}
				}
				fireTableRowsUpdated();
			}
		}
		private void fetchSummonerData(int summonerId){
			client.sendAsync(conref.apply("lol-summoner/v1/summoners/"+summonerId).GET().build(),
					JSONBodySubscriber.HANDLE_UTF8)
					.thenAcceptAsync(this::feedSummonerData, EVENT_QUEUE);
		}
		private void feedSummonerData(HttpResponse<Object> data){
			if(data.statusCode()/100 == 2){
				JSMap map = JSON.toJSMap(data.body());
				int id = map.peekInt("summonerId");
				if(id != 0){
					Summoner summoner = summonerCache.get(id);
					if(summoner != null){
						String name = map.peekString("displayName");
						if(name == null){
							name = map.peekString("internalName");
							if(name == null){
								return;
							}
						}
						summoner.name = name;
						summoner.isExcludedFromFreeRotation = map.peekInt("summonerLevel",11) <= 10;
						fireStateChanged();
					}
				}
			}
		}
		/**
		 * Very inefficient comparator. Too bad Java does not let us sort primitives in a non-numerical order.
		 * Comparator fallback order
		 * -NOT Banned by anyone or picked by non-ally
		 * -Hovered
		 * -Known owned
		 * -Free rotating champion
		 * -Probably owned (mastery chest & on at least mastery level 5)
		 * -Highest mastery
		 * -Alphabetical order
		 */
		@Override
		public int compare(Champion c1, Champion c2) {
			int o1 = c1.key,o2 = c2.key;
			int t1 = globalStates[o1] << 8,t2 = globalStates[o2] << 8,m1 = 0,m2 = 0;
			if((t1 & UNAVAILABLE) != 0){
				if((t2 & UNAVAILABLE) != 0){
					return 0;
				}else{
					return 1;
				}
			}else if((t2 & UNAVAILABLE) != 0){
				return -1;
			}
			for(int i = 0;i < teamSize;i++){
				Summoner sm = myTeam[i];
				if(sm.isLockedIn){//Ignore summoners who locked in, unless one of the champions is being locked in.
					if(sm.hoveredChampionIndex == o1){
						return o1 == o2 ? 0 : -1;//Locked in champions are sorted by their team order.
					}else if(sm.hoveredChampionIndex == o2){
						return 1;
					}
				}else{
					if(sm.hoveredChampionIndex == o1){
						t1 |= HOVERED;
					}else if(sm.hoveredChampionIndex == o2){
						t2 |= HOVERED;
					}
					int s1 = sm.rt[o1];
					int s2 = sm.rt[o2];
					t1 |= (s1 ^ MASTERY_MASK);
					t2 |= (s2 ^ MASTERY_MASK);
					s1 &= MASTERY_MASK;
					s2 &= MASTERY_MASK;
					m1 += s1*s1;
					m2 += s2*s2;
					if(s1 > 4){
						t1 |= MASTERY_CHEST;
					}
					if(s2 > 4){
						t2 |= MASTERY_CHEST;
					}
				}
			}
			t1 ^= t2;
			if((t1 & HOVERED) != 0){
				return (t2 & HOVERED)-1;
			}
			if((t1 & KNOWN_OWNED) != 0){
				return (t2 & KNOWN_OWNED)-1;
			}
			if((t1 & MASTERY_CHEST) != 0){
				return (t2 & MASTERY_CHEST)-1;
			}
			if((t2 & KNOWN_OWNED) == 0 && (t1 & FREE_ROTATION) != 0){
				return (t1 & FREE_ROTATION)-1;
			}
			if(m1 != m2){
				return m2-m1;
			}
			return c1.compareTo(c2);
		}
		@Override
		public int getRowCount() {
			return globalStates.length;
		}
		@Override
		public int getColumnCount() {
			return myTeam.length+1;
		}
		@Override
		public String getColumnName(int columnIndex) {
			if(columnIndex == 0){
				return "";
			}else{
				Summoner sm = myTeam[columnIndex-1];
				return sm == null ? null : sm.name;
			}
		}
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 0 ? Champion.class : Short.class;
		}
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if(columnIndex == 0){
				return championList.get(rowIndex);
			}else{
				Summoner summoner = myTeam[columnIndex-1];
				if(summoner == null){
					return null;
				}
				int value =		(summoner.rt[rowIndex]) |
								(globalStates[rowIndex] << 8) |
								(summoner.hoveredChampionIndex == rowIndex ? HOVERED : 0);
				if(summoner.isExcludedFromFreeRotation){
					value &= ~FREE_ROTATION;
				}
				return Short.valueOf((short)value);
			}
		}
		public void addChangeListener(ChangeListener x) {
			listenerList.add(ChangeListener.class, x);
		}
		public void removeChangeListener(ChangeListener x) {
			listenerList.remove(ChangeListener.class, x);
		}
	    private ChangeEvent che;
	    protected void fireStateChanged(){
			Object[] listeners = listenerList.getListenerList();
			for(int i = listeners.length - 2; i >= 0; i -= 2){
				if(listeners[i] == ChangeListener.class){
					if(che == null){
						che = new ChangeEvent(this);
					}
					((ChangeListener)listeners[i+1]).stateChanged(che);
				}
			}
	    }
	    protected void fireTableRowsUpdated(){
	    	fireTableRowsUpdated(0, globalStates.length-1);
	    }
	}
}
