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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import net.balintgergely.util.JSON;
/**
 * Interfaces with and stores data locally from the data dragon api.
 * @author balintgergely
 *
 */
public class DataDragon {
	public static final BodyHandler<String> STRING_BODY_HANDLER = BodyHandlers.ofString();
	public static final BodyHandler<InputStream> STREAM_BODY_HANDLER = BodyHandlers.ofInputStream();
	private static final String BASE_URI = "http://ddragon.leagueoflegends.com/cdn/";
	private Map<String,Object> cache = new HashMap<>();
	private Map<String,Object> data = new ConcurrentHashMap<>();
	private HttpClient httpClient;
	private File file;
	public DataDragon(File cacheFile,HttpClient httpClient){
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
						object = JSON.freeze(JSON.readObject(new InputStreamReader(input)));
					}
					if(object != null){
						cache.put(name, object);
					}
				}
			}catch(Throwable th){
				th.printStackTrace();
			}
		}
	}
	private Object fetchInternal(String path){
		Object c = cache.get(path);
		if(c != null){
			return c;
		}
		try{
			if(path.endsWith(".png")){
				try(InputStream input = httpClient.send(httpRequest(path),STREAM_BODY_HANDLER).body()){
					return ImageIO.read(input);
				}
			}else if(path.endsWith(".json")){
				return JSON.freeze(JSON.parse(httpClient.send(httpRequest(path),STRING_BODY_HANDLER).body()));
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
			return HttpRequest.newBuilder().GET().uri(new URI(BASE_URI+path)).build();
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
	 * Preloads a bunch of objects more efficiently.
	 */
	public void batchPreload(Iterable<String> itr){
		HashMap<String,CompletableFuture<?>> dedup = new HashMap<>();
		for(final String str : itr){
			if(!(data.containsKey(str) || cache.containsKey(str) || dedup.containsKey(str))){
				if(str.endsWith(".png")){
					dedup.put(str,httpClient.sendAsync(httpRequest(str), STREAM_BODY_HANDLER).thenAccept((HttpResponse<InputStream> response) -> {
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
					dedup.put(str,httpClient.sendAsync(httpRequest(str), STRING_BODY_HANDLER).thenAccept((HttpResponse<String> response) -> {
						data.put(str, JSON.parse(response.body()));
					}).toCompletableFuture());
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
	public boolean finish() throws IOException{
		httpClient = null;
		a: if(!file.exists()){
			file.createNewFile();
		}else{
			for(String str : data.keySet()){
				if(cache.remove(str) == null){
					break a;//New image.
				}
			}
			if(cache.isEmpty()){
				return false;//No new images, no unused images
			}//Found unused images that we now remove.
		}
		try(ZipOutputStream output = new ZipOutputStream(new FileOutputStream(file))){
			OutputStreamWriter writer = new OutputStreamWriter(output,StandardCharsets.UTF_8);
			for(Map.Entry<String, Object> entry : data.entrySet()){
				output.putNextEntry(new ZipEntry(entry.getKey()));
				Object obj = entry.getValue();
				if(obj instanceof BufferedImage){
					ImageIO.write((BufferedImage)obj, "png", output);
				}else{
					JSON.write(obj,writer);
					writer.flush();
				}
				output.flush();
				output.closeEntry();
			}
			output.finish();
		}
		return true;
	}
}
