package net.balintgergely.runebook;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;

import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;
import net.balintgergely.util.JSON.Commentator;
import net.balintgergely.util.JSONBodySubscriber;

public class BookOfThresholds{
	static final int compareVersion(String a,String b){
		if(a.equals(b)){
			return 0;
		}
		a = a.strip();
		b = b.strip();
		int aIndex = 0,bIndex = 0;
		while(true){
			int	aNextIndex = a.indexOf('.', aIndex),
				bNextIndex = b.indexOf('.', bIndex);
			if(aNextIndex < 0){
				aNextIndex = a.length();
			}
			if(bNextIndex < 0){
				bNextIndex = b.length();
			}
			int av = Integer.parseInt(a.substring(aIndex,aNextIndex)),
				bv = Integer.parseInt(b.substring(bIndex,bNextIndex));
			if(av < bv){
				return -1;
			}
			if(bv < av){
				return 1;
			}
			aIndex = aNextIndex+1;
			bIndex = bNextIndex+1;
			if(aIndex >= a.length()){
				if(bIndex >= b.length()){
					return 0;
				}
				return -1;
			}
			if(bIndex >= b.length()){
				return 0;
			}
		}
	}
	private static final File SAVE_FILE = new File("runeBook.json"),TEMP_SAVE_FILE = new File("runeBook.json.temp");
	static final String GITHUB = "https://balintgergely.github.io/bookofthresholds";
	static final String VERSION = "7.0.0";
	public static void main(String[] atgs) throws Throwable{xxxxx:{//Breaking this block = System.exit(1);
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Throwable t){}//Eat
		int argIndex = 0;
		String doFinishUpdate = null;
		boolean updateOnly = false,hasSetOutput = false;
		argReadLoop: while(argIndex < atgs.length){
			String argument = atgs[argIndex++];
			switch(argument){
			case "-finishUpdate"://Required by the Updater class. We must supply the additional parameter to it.
				if(doFinishUpdate == null && argIndex < atgs.length){
					doFinishUpdate = String.valueOf(atgs[argIndex++]);
					continue argReadLoop;
				}
				break;
			case "-update"://Forces us to perform an update.
				if(!updateOnly){
					updateOnly = true;
					continue argReadLoop;
				}
				break;
			case "-logFile"://Redirects system output and errors to log files.
				if(!hasSetOutput){
					File outFile,errFile;
					for(int logIndex = 0;
						(outFile = new File("out"+logIndex+".log")).exists() ||
						(errFile = new File("err"+logIndex+".log")).exists();
						logIndex++){}
					PrintStream outStream = new PrintStream(new FileOutputStream(outFile), true, StandardCharsets.UTF_8);
					PrintStream errStream = new PrintStream(new FileOutputStream(errFile), true, StandardCharsets.UTF_8);
					System.setOut(outStream);
					System.setErr(errStream);
					Runtime.getRuntime().addShutdownHook(new Thread(() -> {
						try(outStream){
							errStream.close();
						}
					},"File log closer hook"));
					hasSetOutput = true;
					continue argReadLoop;
				}
			}
			System.out.println("Ignored: "+argument);
		}
		if(TEMP_SAVE_FILE.createNewFile()){
			TEMP_SAVE_FILE.deleteOnExit();
		}else if(doFinishUpdate == null){
			TEMP_SAVE_FILE.delete();
			updateOnly = true;
		}
		final BookOfThresholds main = new BookOfThresholds();
		System.out.println("Initializing Nexus...");
		CompletableFuture<HttpResponse<Object>> versionCheckFuture = null;
		try{
			versionCheckFuture = main.initClient(doFinishUpdate == null);
			if(updateOnly){//In an extreme case where some of the logic below is messed up, we bypass it.
				break xxxxx;//This forces execution to proceed to the finally block onwards that contains update logic.
			}
			System.out.println("Confronting Data Dragon...");
			main.getDataDragon();
			System.out.println("Fetching Runeterran dictionary...");
			main.initSubsystems();
			System.out.println("Securing Cloud to Earth...");
			if(main.dataDragon.finish()){
				System.out.println("File updated.");
			}else{
				System.out.println("File not updated.");
			}
			System.out.println("Using Data Dragon version "+main.dataDragon.getManifest().getString("v"));
		}catch(Throwable t){
			t.printStackTrace();
			JOptionPane.showMessageDialog(null, t.getMessage(), "Failed to start Rune Book of Thresholds", JOptionPane.ERROR_MESSAGE);
			break xxxxx;
		}finally{
			if(versionCheckFuture != null){
				try{
					main.postCheckUpdate(updateOnly ? versionCheckFuture.get() : versionCheckFuture.get(5, TimeUnit.SECONDS),updateOnly);
				}catch(Throwable th){
					th.printStackTrace();
				}
			}
		}
		if(main.lcuManager == null){
			JOptionPane.showMessageDialog(null, "Rune Book of Thresholds will not be able to export runes.");
		}
		main.initModules(-1);//XXX Set module index here.
		if(doFinishUpdate != null){
			Updater.finishUpdate(doFinishUpdate);
		}
		System.out.println("We can store "+main.assetManager.runeModel.totalPermutationCount+" different runes!");
		main.mainLoop();
	}System.exit(1);}
	private AssetManager assetManager;
	AssetManager getAssetManager(){
		return assetManager;
	}
	private DataDragon dataDragon;
	DataDragon getDataDragon(){
		if(dataDragon == null){
			dataDragon = new DataDragon(new File("dataDragon.zip"), httpClient, dataMap.peekString("lolVersion"));
		}
		return dataDragon;
	}
	private HttpClient httpClient;
	HttpClient getHttpClient(){
		return httpClient;
	}
	private LCUManager lcuManager;
	LCUManager getLCUManager(){
		return lcuManager;
	}
	private BuildListModel buildList;
	BuildListModel getBuildListModel(){
		if(buildList == null){
			buildList = new BuildListModel(assetManager, JSON.toJSList(dataMap.peek("builds")));
			dataMap.map.put("builds",buildList.publicList);
		}
		return buildList;
	}
	<E extends LCUManager.LCUModule> E getLCUModule(Class<E> type){
		if(!lcuManager.isInitialized()){
			try{
				if(type == LCUPerksManager.class){
					return lcuManager.initModule(type.getDeclaredConstructor(LCUManager.class,RuneModel.class), getAssetManager().runeModel);
				}
				if(type == LCUSummonerManager.class){
					return lcuManager.initModule(type.getDeclaredConstructor(LCUManager.class,AssetManager.class), getAssetManager());
				}
			}catch(NoSuchMethodException | SecurityException e){
				throw new RuntimeException(e);
			}
		}
		return lcuManager.getModule(type);
	}
	private Module[] moduleArray;
	private Module mainModule;
	private JSMap dataMap;
	private boolean dataChangeFlag,isCustomLocale;
	private String localeString;
	private BookOfThresholds(){
		if(SAVE_FILE.exists()){
			JSMap d;
			try(FileReader reader = new FileReader(SAVE_FILE,StandardCharsets.UTF_8)){
				d = JSON.toJSMap(JSON.readObject(reader));
			}catch(Throwable t){
				d = new JSMap();
				t.printStackTrace();
			}
			dataMap = d;
		}else{
			dataMap = new JSMap();
		}
		dataMap.map.putIfAbsent("locale", null);
		dataMap.map.putIfAbsent("lolVersion", null);
		localeString = dataMap.peekString("language");
		isCustomLocale = localeString != null;
	}
	private CompletableFuture<HttpResponse<Object>> initClient(boolean checkUpdates) throws Throwable{
		httpClient = HttpClient.newBuilder().sslContext(LCUManager.makeContext()).build();
		return checkUpdates ? httpClient.sendAsync(HttpRequest.newBuilder(new URI(GITHUB+"/version.json")).GET().build(),
				JSONBodySubscriber.HANDLE_UTF8) : null;
	}
	private void initSubsystems(){
		try{
			lcuManager = new LCUManager(httpClient);
			if(localeString == null){
				localeString = lcuManager.fetchLocale();
			}
		}catch(Throwable th){
			th.printStackTrace();
		}
		try{
			assetManager = new AssetManager(getDataDragon(), localeString, isCustomLocale);
		}catch(IOException e){
			throw new UncheckedIOException(e);
		}
		JOptionPane.setDefaultLocale(assetManager.locale);
	}
	private void postCheckUpdate(HttpResponse<Object> response,boolean isBeingForced) throws Throwable{
		if(response.statusCode()/100 == 2){
			JSMap versionMap = JSON.toJSMap(response.body());
			if(compareVersion(VERSION,versionMap.peekString("version", "0")) < 0){
				if(assetManager == null){//We might be on a bugged version. Try to recover without asking.
					System.out.println("Update in progress. "+(isBeingForced ? "Requested by user." : "Triggered by loading error."));
					Updater.update(versionMap.getString("download"),
							Locale.getDefault().toLanguageTag(), "NoooNOOO!", "We can fix this!");
					System.exit(1);
				}else{
					switch(JOptionPane.showConfirmDialog(null,
							assetManager.z.getObject("update"),
							assetManager.z.getString("window"),
							JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE)){
					case JOptionPane.NO_OPTION:break;
					case JOptionPane.YES_OPTION:
						Updater.update(versionMap.getString("download"),
								assetManager.locale.toLanguageTag(), assetManager.z.getString("window"), assetManager.z.getString("updating"));
						//$FALL-THROUGH$
					default:System.exit(0);
					}
				}
			}else if(isBeingForced){
				System.out.println("Rune Book appears to be on the latest version.");
			}else{
				System.out.println("Version check successfull.");
			}
		}else if(isBeingForced){
			System.out.println("Got an unexpected status code: "+response.statusCode());
		}
	}
	private void initModules(int singleModuleMode) throws Throwable{
		EventQueue.invokeAndWait(() -> {
			ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
			try{
				moduleArray = new Module[]{
					new RuneBook(this),
					new TraderAssistant(this),
					//new CosmeticManager(this)
				};
				if(singleModuleMode >= 0){
					moduleArray = new Module[]{moduleArray[singleModuleMode]};
				}
				mainModule = moduleArray[0];
				for(Module md : moduleArray){
					md.init();
				}
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		});
	}
	public <E extends Module> E getModule(Class<E> clazz){
		for(Module md : moduleArray){
			if(clazz.isInstance(md)){
				return clazz.cast(md);
			}
		}
		return null;
	}
	private void mainLoop() throws Throwable{
		lcuManager.finishInitialization();
		lcuManager.startSeekerThread();
		EventQueue.invokeAndWait(() -> mainModule.callerModel.setSelected(true));
		long nextSaveTime = 0;
		CompletableFuture<String> shutdown = null;
		BiFunction<Map<?,?>,String,String> commentFunction = (a,b) -> {
			if(a == dataMap.map && b != null){
				switch(b){
				case "builds":return
"The list of builds.";
				case "locale":return
"The localization for Rune Book. Can be one of: "+assetManager.listOfLocales;
				case "lolVersion":return
"If non-null, overrides the version of lol to use in Rune Book.\nThe following is guaranteed to work: \""+DataDragon.LATEST_KNOWN_LOL_VERSION+"\"";
				}
			}
			return null;
		};
		System.gc();//The AssetManager generates tons and tons of garbage. Best point to gc at.
		while(true){
			synchronized(this){
				while(!(dataChangeFlag || isShutdown())){
					wait();
				}
			}
			boolean sh = isShutdown();
			if(sh){
				System.out.println("Shutdown initiated.");
				if(lcuManager == null){
					shutdown = CompletableFuture.completedFuture("What am I even running on?");
				}else{
					shutdown = lcuManager.close("Close command sent to client.");
				}
			}
			if(dataChangeFlag){
				long currentTime = System.currentTimeMillis();
				if(currentTime < nextSaveTime && !sh){
					synchronized(this){
						while((currentTime = System.currentTimeMillis()) < nextSaveTime && !isShutdown()){
							wait(nextSaveTime-currentTime);
						}
					}
				}
				if(!SAVE_FILE.exists()){
					SAVE_FILE.createNewFile();
				}
				dataChangeFlag = false;
				boolean success = false;
				try(Commentator writer = new JSON.Commentator(new FileWriter(TEMP_SAVE_FILE,StandardCharsets.UTF_8),commentFunction)){
					JSON.write(dataMap, writer);
					writer.flush();
					writer.close();
					success = true;
				}catch(Throwable t){
					System.err.println(shutdown == null ? "Ouch. Will see you in the next attempt." : "No. No! We can fix this!");
					t.printStackTrace();
				}
				if(success && SAVE_FILE.delete()){
					TEMP_SAVE_FILE.renameTo(SAVE_FILE);
				}
				nextSaveTime = currentTime+10000;
			}else if(shutdown != null){
				try{//Give the client 5 seconds to close our connection.
					System.out.println(shutdown.get(5, TimeUnit.SECONDS));
				}catch(Throwable t){
					t.printStackTrace();
				}
				System.out.println("Goodbye.");
				System.exit(0);
			}
		}
	}
	synchronized void setDataChangeFlag(){
		dataChangeFlag = true;
		notifyAll();
	}
	private boolean isShutdown(){
		for(Module md : moduleArray){
			if(md.isModuleOpen()){
				return false;
			}
		}
		return true;
	}
	static abstract class Module extends JFrame{
		private static final long serialVersionUID = 1L;
		ButtonModel callerModel = new JToggleButton.ToggleButtonModel();
		{
			callerModel.addChangeListener(this::stateChanged);
		}
		private ImageIcon icon = new ImageIcon();
		protected final BookOfThresholds main;
		Module(BookOfThresholds main){
			this.main = main;
		}
		abstract void init();
		@Override
		protected void processWindowEvent(WindowEvent e) {
			super.processWindowEvent(e);
			if (e.getID() == WindowEvent.WINDOW_CLOSING) {
				callerModel.setSelected(false);
			}
		}
		@Override
		public void setIconImage(Image icon){
			this.icon.setImage(icon);
			super.setIconImage(icon);
		}
		void stateChanged(ChangeEvent e){
			if(callerModel.isSelected()){
				super.setVisible(true);
			}else{
				super.dispose();
				synchronized(main){
					main.notifyAll();
				}
			}
		}
		boolean isModuleOpen(){
			return callerModel.isSelected();
		}
		public <B extends AbstractButton> B createModuleButton(B ab){
			ab.setModel(callerModel);
			ab.setIcon(icon);
			return ab;
		}
	}
}
