package net.balintgergely.runebook;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;
import net.balintgergely.util.JSONBodySubscriber;
/**
 * Interfaces with and stores data locally from the data dragon api.
 * @author balintgergely
 *
 */
public class DataDragon {
	public static final BodyHandler<String> STRING_BODY_HANDLER = BodyHandlers.ofString();
	public static final BodyHandler<InputStream> STREAM_BODY_HANDLER = BodyHandlers.ofInputStream();
	public static final String LATEST_KNOWN_LOL_VERSION = "10.20.1";
	private static final String BASE_URI = "https://ddragon.leagueoflegends.com/cdn/";
	private static final String VERSION_URI = "https://ddragon.leagueoflegends.com/api/versions.json";
	private static final String MANIFEST = "manifest.json";
	private static final Pattern LOL_VERSIN = Pattern.compile("([0-9]+?)(\\.[0-9]+?)*");
	private Map<String,Object> cache = new HashMap<>();
	private Map<String,Object> data = new ConcurrentHashMap<>();
	private HttpClient httpClient;
	private String versionOverride;
	private File file;
	public DataDragon(File cacheFile,HttpClient httpClient,String versionOverride){
		this.file = cacheFile;
		this.httpClient = httpClient;
		if(cacheFile.exists()){
			try(ZipInputStream input = new ZipInputStream(new FileInputStream(cacheFile))){
				ZipEntry entry;
				while((entry = input.getNextEntry()) != null){
					String name = entry.getName();
					Object object = null;
					if(name.endsWith(".png")){
						object = ImageIO.read(input);
					}else if(name.endsWith(".json")){
						object = JSON.freeze(JSON.readObject(new InputStreamReader(input,StandardCharsets.UTF_8)));
					}else if(name.endsWith(".txt")){
						object = new String(input.readAllBytes(), StandardCharsets.UTF_8);
					}
					if(object != null){
						cache.put(name, object);
					}
				}
			}catch(Throwable th){
				th.printStackTrace();
			}
		}
		this.versionOverride = versionOverride;
	}
	/**
	 * Creates a custom .txt file with the specified string content.
	 * If such file already exists, overwrite.
	 */
	public void putString(String path,String value){
		if(!path.endsWith(".txt")){
			throw new IllegalArgumentException();
		}
		data.put(path,value);
	}
	private Object fetchInternal(String path){
		Object c = cache.get(path);
		if(c != null){
			return c;
		}
		try{
			if(path.endsWith(".png")){
				try(InputStream input = httpClient.send(httpRequest(BASE_URI+path),STREAM_BODY_HANDLER).body()){
					return ImageIO.read(input);
				}
			}else if(path.endsWith(".json")){
				return JSON.freeze(httpClient.send(httpRequest(BASE_URI+path),JSONBodySubscriber.HANDLE_UTF8).body());
			}else{
				return null;
			}
		}catch(IOException e){
			throw new UncheckedIOException(e);
		}catch(InterruptedException e){
			throw new RuntimeException(e);
		}
	}
	private static HttpRequest httpRequest(String path){
		try{
			return HttpRequest.newBuilder().GET().uri(new URI(path)).build();
		}catch(URISyntaxException e){//Stupid.
			throw new RuntimeException(e);
		}
	}
	/**
	 * Loads an object either from the cache or from data dragon.
	 */
	public Object fetchObject(String path){
		return data.computeIfAbsent(path, this::fetchInternal);
	}
	/**
	 * Retrieves the current manifest. The manifest contains information
	 * about which version to use of each subroute inside the data dragon.
	 * Unless the version of lol to use has been overridden, this method attempts to fetch the latest
	 * manifest.
	 */
	public JSMap getManifest(){
		return JSON.asJSMap(data.computeIfAbsent(MANIFEST, this::fetchManifestInternal));
	}
	private JSMap fetchManifestInternal(String pt){
		if(httpClient == null){
			throw new IllegalStateException();
		}
		Exception x = null;
		Object mf = null;
		trial: try{
			String version;
			if(versionOverride == null){//No overridden version. Thus we look for the latest.
				JSList versionList = JSON.asJSList(httpClient.send(httpRequest(VERSION_URI), JSONBodySubscriber.HANDLE_UTF8).body());
				version = LATEST_KNOWN_LOL_VERSION;
				for(Object obj : versionList){
					String str = JSON.asString(obj, true, null);
					if(LOL_VERSIN.matcher(str).matches() && BookOfThresholds.compareVersion(version,str) < 0){
						version = str;//Latest version.
					}
				}
			}else{
				version = versionOverride;
				Object xmf = cache.get(MANIFEST);
				if(xmf != null && version.equals(JSON.toJSMap(xmf).peekString("v"))){
					mf = xmf;
					break trial;//Found requested version in cache. Do not try to download it.
				}
			}
			try{
				mf = httpClient.send(httpRequest(BASE_URI+version+"/manifest.json"), JSONBodySubscriber.HANDLE_UTF8).body();
			}catch(IOException e){
				x = e;
			}
			if(mf == null && versionOverride != null){
				JSList versionList = JSON.asJSList(httpClient.send(httpRequest(VERSION_URI), JSONBodySubscriber.HANDLE_UTF8).body());
				version = versionOverride;
				for(Object obj : versionList){
					String str = JSON.asString(obj, true, null);
					if(LOL_VERSIN.matcher(str).matches() && str.startsWith(versionOverride) && BookOfThresholds.compareVersion(version,str) < 0){
						version = str;
					}
				}
			}
			mf = httpClient.send(httpRequest(BASE_URI+version+"/manifest.json"), JSONBodySubscriber.HANDLE_UTF8).body();
		}catch(IOException | InterruptedException e){
			mf = cache.get(MANIFEST);
			if(x != null){
				e.addSuppressed(x);
			}
			x = e;
		}
		if(!(mf instanceof JSMap || (mf = cache.get(MANIFEST)) instanceof JSMap)){
			throw new RuntimeException(x);
		}
		return (JSMap)mf;
	}
	/**
	 * Preloads a bunch of objects more efficiently.
	 */
	public void batchPreload(Iterable<String> itr){
		HashMap<String,CompletableFuture<?>> dedup = new HashMap<>();
		for(final String str : itr){
			if(!(data.containsKey(str) || cache.containsKey(str) || dedup.containsKey(str))){
				if(str.endsWith(".png")){
					dedup.put(str,httpClient.sendAsync(httpRequest(BASE_URI+str), STREAM_BODY_HANDLER).thenAccept((HttpResponse<InputStream> response) -> {
						try(InputStream input = response.body()){
							BufferedImage img = ImageIO.read(input);
							if(img == null){
								System.err.println("Could not resolve route: "+str);
							}else{
								data.put(str, img);
							}
						}catch(IOException e){
							throw new UncheckedIOException(e);
						}
					}).toCompletableFuture());
				}else if(str.endsWith(".json")){
					dedup.put(str,httpClient.sendAsync(httpRequest(BASE_URI+str), JSONBodySubscriber.HANDLE_UTF8)
							.thenAccept((HttpResponse<Object> response) -> data.put(str, response.body())).toCompletableFuture());
				}
			}
		}
		try{
			for(CompletableFuture<?> ft : dedup.values()){
				ft.get();
			}
		}catch(InterruptedException | ExecutionException e){
			throw new RuntimeException(e);
		}
	}
	/**
	 * Finishes loading. No more data fetching will take place and the cache file will be written if changed.
	 */
	@SuppressWarnings("resource")
	public boolean finish() throws IOException{
		httpClient = null;
		a: if(!file.exists()){
			file.createNewFile();
		}else{
			for(Map.Entry<String,Object> entry : data.entrySet()){
				if(!entry.getValue().equals(cache.remove(entry.getKey()))){//All types use short circuiting via "this == that"
					break a;//New file.
				}
			}
			if(cache.isEmpty()){
				return false;//No new images, no unused images
			}//Found unused images that we now remove.
		}
		try(ZipOutputStream output = new ZipOutputStream(new FileOutputStream(file))){
			OutputStreamWriter writer = new OutputStreamWriter(output,StandardCharsets.UTF_8);
			for(Map.Entry<String,Object> entry : data.entrySet()){
				String path = entry.getKey();
				output.putNextEntry(new ZipEntry(path));
				Object obj = entry.getValue();
				if(path.endsWith(".png")){
					ImageIO.write((BufferedImage)obj, "png", output);
				}else if(path.endsWith(".json")){
					JSON.write(obj,writer);
					writer.flush();
				}else if(path.endsWith(".txt")){
					writer.write((String)obj);
					writer.flush();
				}else{
					throw new Error();//Not supposed to happen.
				}
				output.flush();
				output.closeEntry();
			}
			output.finish();
		}
		return true;
	}
}
