package net.balintgergely.runebook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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

public class ClientManager{
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
			"XWehWA==\r\n" + "-----END CERTIFICATE-----\r\n";
	private static final BodyHandler<String> STRING_BODY_HANDLER = BodyHandlers.ofString();
	private static final Function<HttpResponse<String>,Object> TO_JSON = (HttpResponse<String> response) -> {
		String body = response.body();
		return body == null || body.isBlank() ? null : JSON.parse(response.body());
	};
	private static final Pattern	PASSWORD_PATTERN = Pattern.compile("--remoting-auth-token=(?<group>[^ \\\"]+)"),
									PORT_PATTERN = Pattern.compile("--app-port=(?<group>\\d+)");
	//private static final BodyPublisher EMPTY = BodyPublishers.ofString("{}", StandardCharsets.UTF_8);
	private static URI uri(String uri){
		try{
			return new URI(uri);
		}catch(URISyntaxException e){
			throw new RuntimeException(e);
		}
	}
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
	private final String address;
	private final ProcessBuilder ppFetcher;
	HttpClient client;
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
			throw new Error("Connecting to the League Client on "+osLowerCase+" is not supported.");
		}
	}
	private Function<String,HttpRequest.Builder> conref;
	public boolean tryConnect(){
		String password;
		int port;
		try{
			String processData = new String(//To get the password and port.
				ppFetcher.start().getInputStream().readAllBytes(),StandardCharsets.UTF_8);
			Matcher matcher = PASSWORD_PATTERN.matcher(processData);
			matcher.find();
			password = matcher.group("group");
			matcher = PORT_PATTERN.matcher(processData);
			matcher.find();
			port = Integer.parseInt(matcher.group("group"));
		}catch(Throwable e){
			//return CompletableFuture.completedFuture(e);
			e.printStackTrace();
			return false;
		}
		String authString = "Basic "+Base64.getEncoder().encodeToString(("riot:"+password).getBytes());
		HttpRequest.Builder builder = HttpRequest.newBuilder().header("Authorization", authString);
		String base = "https://"+address+":"+port+"/";
		conref = (String str) -> builder.copy().uri(uri(base+str));
			//return CompletableFuture.completedFuture(null);
			/*return client.newWebSocketBuilder().header("Authorization", authString)
					.buildAsync(uri("wss://"+address+":"+port), this)
					.handle((WebSocket ws, Throwable th) -> {
						synchronized(ClientManager.this){
							if(ws == null){
								conref.set(null);
							}
						}
						return th;
					});*/
		return true;
	}
	public CompletionStage<String> exportRune(Rune rune,String name){
		if(conref == null && !tryConnect()){
			return CompletableFuture.completedFuture("Could not connect to the League Client!");
		}
		BodyPublisher runeExport = BodyPublishers.ofString(rune.toJSMap()
				.put("name", name, "current", true)
				.toString(),StandardCharsets.UTF_8);
		CompletionStage<Object>
		inventory0 = client.sendAsync(conref.apply("lol-perks/v1/inventory").GET().build(),STRING_BODY_HANDLER).thenApply(TO_JSON),
		pages0 = client.sendAsync(conref.apply("lol-perks/v1/pages").GET().build(),STRING_BODY_HANDLER).thenApply(TO_JSON);
		return inventory0.thenCombine(pages0, (Object inventory,Object pages) -> {
			int pageLimit = JSON.asJSMap(inventory, true).getInt("ownedPageCount");
			JSList runeList = JSON.asJSList(pages, true);
			runeList.list.removeIf(o -> !JSON.asJSMap(o, true).getBoolean("isEditable"));
			JSMap pageToReplace = null;
			long lastModTime = pageLimit <= runeList.size() ? Long.MAX_VALUE : Long.MIN_VALUE;
			for(Object obj : runeList){
				JSMap runeBuild = JSON.asJSMap(obj, true);
				if(name.equals(runeBuild.peekString("name"))){
					pageToReplace = runeBuild;
					break;
				}
				long modTime = runeBuild.peekLong("lastModified");
				if(modTime <= lastModTime){
					pageToReplace = runeBuild;
					lastModTime = modTime;
				}
			}
			HttpRequest exportRequest;
			if(pageToReplace == null){
				exportRequest = conref.apply("lol-perks/v1/pages").POST(runeExport).build();
			}else{
				int pageId = pageToReplace.getInt("id");
				exportRequest = conref.apply("lol-perks/v1/pages/"+pageId).PUT(runeExport).build();
			}
			return client.sendAsync(exportRequest, STRING_BODY_HANDLER);
		}).thenCompose(o -> o).handle((HttpResponse<?> response,Throwable t) -> {
			if(response != null){
				int code = response.statusCode();
				if(code/100 != 2){
					conref = null;
					return "Error code: "+code;
				}else{
					return null;
				}
			}
			conref = null;
			return t.getLocalizedMessage();
		});
	}
/*	@Override
	public void onOpen(WebSocket webSocket) {
		System.out.println("Opened.");
	}
	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		System.out.println("Received text. Last: "+last);
		System.out.println(data);
		return null;
	}
	@Override
	public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
		System.out.println("Received bytes. Length: "+data.remaining()+" Last: "+last);
		return null;
	}
	@Override
	public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
		System.out.println("Got pinged.");
		return null;
	}
	@Override
	public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
		System.out.println("Got ponged.");
		return null;
	}
	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		conref.set(null);
		System.out.println("Closed. Status code: "+statusCode+" Stated reason:");
		System.out.println(reason);
		return null;
	}
	@Override
	public void onError(WebSocket webSocket, Throwable error) {
		conref.set(null);
		System.err.println("Error");
		error.printStackTrace();
	}*/
}
