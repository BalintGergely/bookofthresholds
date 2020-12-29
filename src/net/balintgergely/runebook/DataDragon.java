package net.balintgergely.runebook;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.imageio.ImageIO;

import net.balintgergely.sutil.ZipChannel;
import net.balintgergely.sutil.ZipChannel.ZipEntry;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;
import net.balintgergely.util.JSONBodySubscriber;

public class DataDragon implements Flushable,Closeable{
	private static HttpRequest httpRequest(String path){
		try{
			return HttpRequest.newBuilder().GET().uri(new URI(path)).build();
		}catch(URISyntaxException e){//Stupid.
			throw new RuntimeException(e);
		}
	}
	public static final BodyHandler<String> STRING_BODY_HANDLER = BodyHandlers.ofString();
	public static final BodyHandler<InputStream> STREAM_BODY_HANDLER = BodyHandlers.ofInputStream();
	public static final String LATEST_KNOWN_LOL_VERSION = "10.25.1";
	private static final String BASE_URI = "https://ddragon.leagueoflegends.com/cdn/";
	private static final String VERSION_URI = "https://ddragon.leagueoflegends.com/api/versions.json";
	public static final String MANIFEST = "manifest.json";
	public static final String LANGUAGES = "languages.json";
	private static final Pattern LOL_VERSION = Pattern.compile("([0-9]+?)(\\.[0-9]+?)*");
	private static final Class<?>[] WRAP_TYPES = new Class<?>[] {Future.class,CompletionStage.class};
	private static <E> Object wrap(CompletableFuture<E> e){
		return Proxy.newProxyInstance(Future.class.getClassLoader(), WRAP_TYPES, (Object proxy, Method method, Object[] args) -> {
			if(method.getName().equals("toCompletableFuture")){
				return e.copy();
			}
			return method.invoke(e, args);
		});
	}
	private Map<String,Object> cache = new ConcurrentHashMap<>();
	private Map<String,Object> manifest;
	//private StringList versionSet;
	private boolean manifestChanged = false;
	private JSList localeList;
	private ExecutorService executor;
	private HttpClient httpClient;
	private ZipChannel zipFile;
	public DataDragon(File cacheFile,HttpClient httpClient,ExecutorService executor,String versionOverride) throws IOException{
		this.zipFile = new ZipChannel(cacheFile, true);
		zipFile.setFileComment("You have found a hidden treasury of the almighty data dragon.\n"
				+ "With the exception of \"manifest.json\" all these files are available online "
				+ "at the following uri:\n"+BASE_URI
				+ "<full pathname here>\nTo locate a sample manifest file of a given version take one entry from \n"
				+ VERSION_URI+"\nand you shall find it at\nhttps://ddragon.leagueoflegends.com/cdn/<version>/manifest.json\n"
				+ "This file was summoned by The Rune Book of Thresholds.\n"+BookOfThresholds.GITHUB);
		this.httpClient = httpClient;
		this.executor = executor;
		CompletableFuture<JSMap> manifestFuture = (versionOverride == null ? fetchManifestStage0() : fetchManifestStage1(versionOverride));
		manifestFuture = manifestFuture.thenApply((JSMap a) -> {
			if(!(a.peek("l") instanceof String)){
				a.put("l","en_US");
			}
			a.map.replaceAll((k,b) -> JSON.freeze(b));
			manifest = a.map;
			return new JSMap(Collections.unmodifiableMap(a.map));
		});
		cache.put(MANIFEST,wrap(manifestFuture));
		cache.put(LANGUAGES,wrap(manifestFuture.thenApply((a) -> localeList)));
	}
	@SuppressWarnings("unchecked")
	public Future<Object> fetchObject(String key,String path){
		if(!path.endsWith(".json")){
			throw new IllegalArgumentException();
		}
		if(key != null){
			path = ((JSMap)manifest.get("n")).getString(key)+'/'+path;
		}
		return (Future<Object>)cache.computeIfAbsent(path, this::createObject);
	}
	public Image fetchImage(String key,String path){
		if(!path.endsWith(".png")){
			throw new IllegalArgumentException();
		}
		if(key != null){
			path = ((JSMap)manifest.get("n")).getString(key)+'/'+path;
		}
		return (Image)cache.computeIfAbsent(path, this::createDynamicImage);
	}
	@SuppressWarnings("unchecked")
	public JSMap getManifest() throws InterruptedException, ExecutionException{
		return ((Future<JSMap>)cache.get(MANIFEST)).get();
	}
	private <E> CompletableFuture<E> request(String path,BodyHandler<E> bh){
		return httpClient.sendAsync(httpRequest(BASE_URI+path), bh)
				.thenApply((h) -> {
					if(h.statusCode()/100 != 2){
						throw new RuntimeException();
					}
					return h.body();
				});
	}
	private CompletableFuture<byte[]> loadOnline(String path){
		CompletableFuture<byte[]> cf = request(path,BodyHandlers.ofByteArray());
		cf.thenAcceptAsync((bytes) -> {
			try{
				zipFile.putEntry(zipFile.new ZipEntry(path, bytes));
			}catch(IOException e){
				throw new UncheckedIOException(e);
			}
		}, executor);
		return cf;
	}
	private <E> CompletableFuture<E> loadOffline(ZipEntry entry,Function<InputStream,E> reader){
		return CompletableFuture.supplyAsync(() -> {
			try{
				CheckedInputStream cin = new CheckedInputStream(entry.getInputStream(),new CRC32());
				E obj = reader.apply(cin);
				cin.skip(Long.MAX_VALUE);
				if(((int)cin.getChecksum().getValue()) != entry.getCRC32()){
					throw new RuntimeException("CRC32 Error");
				}
				return obj;
			}catch(IOException e){
				throw new UncheckedIOException(e);
			}
		},executor);
	}
	private static final Function<InputStream,Object> READ_JSON = (input) -> {
		try{
			return JSON.readObject(new InputStreamReader(input,StandardCharsets.UTF_8));
		}catch(IOException e){
			throw new UncheckedIOException(e);
		}
	};
	private static final Function<InputStream,BufferedImage> READ_IMAGE = (input) -> {
		try{
			return ImageIO.read(input);
		}catch(IOException e){
			throw new UncheckedIOException(e);
		}
	};
	private Object createObject(String path){
		ZipEntry entry = zipFile.get(path);
		if(entry != null){
			return wrap(loadOffline(entry,READ_JSON).exceptionallyCompose((th) -> {
				if(th.getMessage().endsWith("CRC32 Error")){
					return loadOnline(path).thenApply(ByteArrayInputStream::new).thenApply(READ_JSON);
				}else{
					throw new RuntimeException(th);
				}
			}).thenApply(JSON::freeze));
		}else{
			return wrap(loadOnline(path).thenApply(ByteArrayInputStream::new).thenApply(READ_JSON).thenApply(JSON::freeze));
		}
	}
	private Object createImage(String path){
		ZipEntry entry = zipFile.get(path);
		if(entry != null){
			return loadOffline(entry, READ_IMAGE)
			.exceptionallyCompose((e) -> loadOnline(path).thenApply(ByteArrayInputStream::new).thenApply(READ_IMAGE));
		}else{
			return loadOnline(path).thenApply(ByteArrayInputStream::new).thenApply(READ_IMAGE);
		}
	}
	private Image createDynamicImage(Object data){
		return Toolkit.getDefaultToolkit().createImage(new DataDragonImageProducer(data));
	}
	public String setLocale(String locale){
		synchronized(cache){
			if(localeList.list.contains(locale)){
				manifest.put("l", locale);
				manifestChanged = true;
				return locale;
			}else{
				return (String)manifest.get("l");
			}
		}
	}
	@Override
	public void flush() throws IOException{
		synchronized(cache){
			//if(versionSet != null){
				//TODO logic to remove outdated files.
			//}
			writeManifest();
			zipFile.clearUnreachableEntries();
			zipFile.flush();
			zipFile.setAutoFlushTreshold(0x10000);
		}
	}
	private void writeManifest() throws IOException{
		assert Thread.holdsLock(cache);
		if(manifestChanged){
			class OpenBos extends ByteArrayOutputStream{
				byte[] getBuffer(){
					return buf;
				}
			}
			OpenBos bos = new OpenBos();
			try(Writer wrt = new OutputStreamWriter(bos,StandardCharsets.UTF_8)){
				JSON.write(manifest, wrt);
				wrt.flush();
			}
			zipFile.putEntry(zipFile.new ZipEntry(MANIFEST, bos.getBuffer(), 0, bos.size()));
			manifestChanged = false;
		}
	}
	@Override
	public void close() throws IOException{
		synchronized(cache){
			try(ZipChannel ch = zipFile){
				writeManifest();
				ch.clearUnreachableEntries();
			}
		}
	}
	/**
	 * Stage 0: Determine the latest lol version and invoke stage 1.
	 * @return The Manifest
	 */
	@SuppressWarnings("unchecked")
private CompletableFuture<JSMap> fetchManifestStage0(){
		return httpClient.sendAsync(httpRequest(VERSION_URI), JSONBodySubscriber.HANDLE_UTF8).handle((r,t) -> {
				String ver = null;
				if(r != null && r.statusCode()/100 == 2){
					JSList list = JSON.asJSList(r.body());
					List<String> vList = (List<String>)(List<?>)list.list;
					//versionSet = new StringList(vList);
					for(String str : vList){
						if(LOL_VERSION.matcher(str).matches() && (ver == null || BookOfThresholds.compareVersion(ver,str) < 0)){
							ver = str;//Latest version.
						}
					}
				}
				return ver;
			}).thenCompose(this::fetchManifestStage1);
	}
	/**
	 * Stage 1: If exists locally, load the current manifest and invoke stage 2.
	 * @param version The version of lol to use. null to stay on the version last used.
	 * @return The Manifest
	 */
	private CompletableFuture<JSMap> fetchManifestStage1(String version){
		ZipEntry entry = zipFile.get(MANIFEST);
		if(entry == null){
			return fetchManifestStage2(version,null);
		}else{
			return loadOffline(entry, READ_JSON).handle((a,b) -> fetchManifestStage2(version,a)).thenCompose(Function.identity());
		}
	}
	/**
	 * Stage 2: If local manifest was successfully loaded, compare the version with the 
	 * @param version The version of lol to use. null to stay on the version last used.
	 * @param localManifest The local manifest or null if none.
	 * @return The Manifest
	 */
	private CompletableFuture<JSMap> fetchManifestStage2(String version,Object localManifest){
		JSMap localManifestObject;
		if(localManifest != null){
			localManifestObject = JSON.toJSMap(localManifest);
			if(version == null || version.equals(localManifestObject.get("v"))){
				return fetchManifestStage3(localManifestObject, null);
			}
		}else{
			localManifestObject = null;
		}
		if(version == null){
			version = LATEST_KNOWN_LOL_VERSION;
		}
		return request(version+"/"+MANIFEST, JSONBodySubscriber.HANDLE_UTF8)
			.thenApply(JSON::toJSMap)
			.handle((o,t) -> fetchManifestStage3(localManifestObject, o))
			.thenCompose(Function.identity());
	}
	/**
	 * Try to fetch the language file. This method either loads it from locally or downloads it and then stores it locally.
	 * @param localManifestObject
	 * @param onlineManifestObject
	 * @return
	 */
	private CompletableFuture<JSMap> fetchManifestStage3(JSMap localManifestObject,JSMap onlineManifestObject){
		CompletableFuture<Object> localeListStage;
		if(onlineManifestObject == null) {
			ZipEntry entry = zipFile.get(LANGUAGES);
			if(entry == null){
				localeListStage = loadOnline(LANGUAGES).thenApply(ByteArrayInputStream::new).thenApply(READ_JSON);
			}else{
				localeListStage = loadOffline(entry,READ_JSON).exceptionallyCompose((th) -> {
					if(th.getMessage().endsWith("CRC32 Error")){
						return loadOnline(LANGUAGES).thenApply(ByteArrayInputStream::new).thenApply(READ_JSON);
					}else{
						throw new RuntimeException(th);
					}
				});
			}
		}else{
			ZipEntry entry = zipFile.get(LANGUAGES);
			localeListStage = loadOnline(LANGUAGES).thenApply(ByteArrayInputStream::new).thenApply(READ_JSON);
			if(entry != null){
				localeListStage = localeListStage.exceptionallyCompose((e) -> loadOffline(entry,READ_JSON));
			}
		}
		return localeListStage.handle((o,t) -> {
			localeList = JSON.freeze(JSON.toJSList(o));
			if(onlineManifestObject == null){
				localManifestObject.put("v", localManifestObject.getString("v").intern());
				return localManifestObject;
			}
			manifestChanged = true;
			if(localManifestObject != null){
				String l = localManifestObject.peekString("l");
				if(localeList.list.contains(l)){
					onlineManifestObject.put("l",l);
				}
			}
			return onlineManifestObject;
		});
	}
	/**
	 * ImageProducer implementation that loads the image from either local storage or from the data dragon
	 * on demand.
	 * @author balintgergely
	 *
	 */
	class DataDragonImageProducer implements ImageProducer{
		private volatile Object data;
		private Collection<ImageConsumer> icSet = new HashSet<>();
		private DataDragonImageProducer(Object data){
			this.data = data;
		}
		@Override
		public void addConsumer(ImageConsumer ic) {
			if(ic != null){
				Object prd;
				if(data instanceof Image){
					prd = ((Image)data).getSource();
				}else{
					synchronized(icSet){
						if(data instanceof Image){
							prd = ((Image)data).getSource();
						}else{
							icSet.add(ic);
							return;
						}
					}
				}
				((ImageProducer)prd).addConsumer(ic);
			}
		}
		@Override
		public boolean isConsumer(ImageConsumer ic) {
			Object prd;
			if(data instanceof Image){
				prd = ((Image)data).getSource();
			}else{
				synchronized(icSet){
					if(data instanceof Image){
						prd = ((Image)data).getSource();
					}else{
						return icSet.contains(ic);
					}
				}
			}
			return ((ImageProducer)prd).isConsumer(ic);
		}
		@Override
		public void removeConsumer(ImageConsumer ic) {
			Object prd;
			if(data instanceof Image){
				prd = ((Image)data).getSource();
			}else{
				synchronized(icSet){
					if(data instanceof Image){
						prd = ((Image)data).getSource();
					}else{
						icSet.remove(ic);
						return;
					}
				}
			}
			((ImageProducer)prd).removeConsumer(ic);
		}
		@SuppressWarnings("unchecked")
		@Override
		public void startProduction(ImageConsumer ic) {
			Object prd;
			if(data instanceof Image){
				prd = ((Image)data).getSource();
			}else{
				synchronized(icSet){
					if(data instanceof Image){
						prd = ((Image)data).getSource();
					}else{
						if(ic != null){
							icSet.add(ic);
						}
						if(data instanceof String){
							String pt = (String)data;
							data = ((CompletionStage<BufferedImage>)createImage(pt)).whenComplete(this::setImage);
						}
						return;
					}
				}
			}
			if(ic != null){
				((ImageProducer)prd).startProduction(ic);
			}
		}
		private void setImage(BufferedImage image,Throwable exception){
			ImageConsumer[] cnsArray = null;
			loop: while(true){
				synchronized(icSet){
					if(icSet.isEmpty()){
						data = image == null ? exception : image;
						icSet.notifyAll();
						break loop;
					}else{
						cnsArray = cnsArray == null ? icSet.toArray(ImageConsumer[]::new) : icSet.toArray(cnsArray);
						icSet.clear();
					}
				}
				if(image == null){
					for(ImageConsumer cns : cnsArray){
						if(cns == null){
							continue loop;
						}
						cns.imageComplete(ImageConsumer.IMAGEERROR);
					}
				}else{
					ImageProducer prod = image.getSource();
					for(ImageConsumer cns : cnsArray){
						if(cns == null){
							continue loop;
						}
						prod.startProduction(cns);
					}
				}
			}
		}
		public void awaitCompletion() throws InterruptedException{
			if(!(data instanceof BufferedImage)){
				synchronized(icSet){
					startProduction(null);
					do{
						if(data instanceof Throwable || data instanceof Image){
							break;
						}
						icSet.wait();
					}while(!(data instanceof Throwable || data instanceof Image));
				}
			}
		}
		@Override
		public void requestTopDownLeftRightResend(ImageConsumer ic) {
			//BufferedImage producer uses tdlr already. No need of any additional logic here.
		}
		public Color averagePixels(){
			try{
				awaitCompletion();
			}catch(InterruptedException e){
				throw new RuntimeException(e);
			}
			BufferedImage img = (BufferedImage)data;
			double redSum = 0,blueSum = 0,greenSum = 0,alphaSum = 0;
			int w = img.getWidth(),h = img.getHeight();
			for(int y = 0;y < h;y++){
				for(int x = 0;x < w;x++){
					int pix = img.getRGB(x, y);
					double alpha = (pix >>> 24) & 0xff;
					double red = (pix >>> 16) & 0xff;
					double green = (pix >>> 8) & 0xff;
					double blue = (pix) & 0xff;
					redSum += red*alpha;
					blueSum += blue*alpha;
					greenSum += green*alpha;
					alphaSum += alpha;
				}
			}
			if(alphaSum == 0){
				return new Color(0,true);
			}else{
				redSum /= alphaSum;
				blueSum /= alphaSum;
				greenSum /= alphaSum;
				int red = redSum <= 0 ? 0 : (redSum >= 255 ? 255 : (int)Math.round(redSum));
				int green = greenSum <= 0 ? 0 : (greenSum >= 255 ? 255 : (int)Math.round(greenSum));
				int blue = blueSum <= 0 ? 0 : (blueSum >= 255 ? 255 : (int)Math.round(blueSum));
				return new Color((red << 16) | (green << 8) | blue);
			}
		}
	}
}
