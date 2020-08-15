package net.balintgergely.runebook;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.jar.Attributes.Name;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
/**
 * This class encapsulates the self-updating logic of RuneBook.
 * Upon call of the update() method, it generates a separate Updater.jar file
 * with only this class file as the main class. It is ran within the current context
 * and the current JVM is terminated.<br>
 * The main method within this class handles the logic of
 * downloading the updated .jar file. Once it is downloaded with a separate temporary name,
 * the original file is deleted and replaced with it. Then it is launched with the parameter
 * parameter "-finishUpdate" followed by the absolute path to Updater.jar.<br>
 * Finally it is the responsibility of the updated application to call
 * <code>finishUpdate(String)</code> with the parameter path in order to clean up the old Updater.jar.
 * @author balintgergely
 */
public class Updater {private Updater() {}
	private static final Class<Updater> CLASS = Updater.class;
	public static void main(String[] atgs) throws Throwable{
		final Updater referenceHolder = new Updater();
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Throwable t){
			t.printStackTrace();
		}
		File currentFile = new File(CLASS.getProtectionDomain().getCodeSource().getLocation().toURI());
		URI uri = new URI(atgs[0]);
		File targetFile = new File(atgs[1]);
		String localeCode = atgs[2];
		String windowTitle = atgs[3];
		String windowText = atgs[4];
		File tempFile = new File(targetFile+".temp");
		if(!(tempFile.exists() || tempFile.createNewFile())){
			System.err.println("Could not create temp file.");
			return;
		}
		try{
			Locale.setDefault(Locale.forLanguageTag(localeCode));
		}catch(Throwable t){
			t.printStackTrace();
		}
		HttpClient client = HttpClient.newBuilder().build();
		CompletableFuture<HttpResponse<InputStream>> future = client.sendAsync(HttpRequest.newBuilder(uri).GET().build(), BodyHandlers.ofInputStream());
		DefaultBoundedRangeModel progressBarModel = new DefaultBoundedRangeModel(0, 0, 0, 1);
		EventQueue.invokeLater(() -> {
			JProgressBar progressBar = new JProgressBar(progressBarModel);
			referenceHolder.progressBar = progressBar;
			JFrame frame = new JFrame(windowTitle);
			referenceHolder.frame = frame;
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.setLocale(Locale.getDefault());
			JLabel label = new JLabel(windowText);
			frame.add(label,BorderLayout.PAGE_START);
			label.setBorder(new LineBorder(new Color(0,true), 5));
			progressBar.setIndeterminate(true);
			frame.add(progressBar,BorderLayout.CENTER);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
		Runnable updateRunnable = () -> progressBarModel.setValue(referenceHolder.progressValue);
		try{
			HttpResponse<InputStream> response = future.join();
			if(response.statusCode()/100 != 2){
				return;
			}
			DataInputStream inputStream = new DataInputStream(response.body());
			String lengthString = response.headers().firstValue("Content-Length").orElse(null);
			if(lengthString != null){
				try{
					int totalLength = Integer.parseInt(lengthString);
					EventQueue.invokeLater(() -> {
						progressBarModel.setMaximum(totalLength+1);//Let's not display 100% until we are actually done.
						referenceHolder.progressBar.setIndeterminate(false);
					});
				}catch(Throwable th){
					lengthString = null;
					th.printStackTrace();
				}
			}
			try(FileOutputStream fileOut = new FileOutputStream(tempFile)){
				byte[] buffer = new byte[0x10000];
				int len;
				while((len = inputStream.read(buffer)) >= 0){
					fileOut.write(buffer,0,len);
					referenceHolder.progressValue += len;
					if(lengthString != null){
						EventQueue.invokeLater(updateRunnable);
					}
				}
				fileOut.flush();
			}
			if(targetFile.exists() && !targetFile.delete()){
				System.err.println("Could not delete previous file.");
				return;
			}
			if(!tempFile.renameTo(targetFile)){
				System.err.println("Could not rename the target file.");
				return;
			}
			if(lengthString != null){//This is the point where we are technically done.
				referenceHolder.progressValue++;
				EventQueue.invokeLater(updateRunnable);
			}
			runAndExit(targetFile,"-finishUpdate",currentFile.getAbsolutePath());
		}finally{
			EventQueue.invokeLater(() -> referenceHolder.frame.dispose());
			System.exit(1);
		}
	}
	JFrame frame;
	JProgressBar progressBar;
	volatile int progressValue;
	private static final String SHORT_NAME = CLASS.getSimpleName();
	/**
	 * Updates the current jar file in which Updater.class is located from the specified source url.
	 * This procedure involves generating a new jar file containing the updater code,
	 * running it, exiting the current runtime and restarting it.
	 * @return Never.
	 * @param sourceURI The uri to download the updated file from
	 * @param locale The locale code
	 * @param windowTitle The title of the window
	 * @param windowDescription The description of the window
	 */
	@SuppressWarnings("javadoc")
	static void update(String sourceURI,String locale,String windowTitle,String windowDescription) throws Throwable{
		File file = new File(CLASS.getProtectionDomain().getCodeSource().getLocation().toURI());
		File updater = new File(file.getParentFile(),SHORT_NAME+".jar");
		try(InputStream updaterFileResource = CLASS.getResourceAsStream(SHORT_NAME+".class")){
			String updaterEntryName = CLASS.getName();
			ZipEntry updaterEntry = new ZipEntry(updaterEntryName.replace('.', '/')+".class");
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
			manifest.getMainAttributes().put(Name.CLASS_PATH, ".");
			manifest.getMainAttributes().put(Name.MAIN_CLASS, updaterEntryName);
			try(JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(updater),manifest)){
				jarOut.putNextEntry(updaterEntry);
				updaterFileResource.transferTo(jarOut);
				jarOut.closeEntry();
				jarOut.finish();
			}
		}
		runAndExit(updater, sourceURI, file.getAbsolutePath(), locale, windowTitle, windowDescription);
	}
	static void finishUpdate(String nextParam){
		new File(nextParam).delete();
	}
	private static void runAndExit(File file,String... params) throws IOException{
		Runtime runtime = Runtime.getRuntime();
		String[] command = new String[3+params.length];
		command[0] = "java";
		command[1] = "-jar";
		command[2] = file.getAbsolutePath();
		System.arraycopy(params, 0, command, 3, params.length);
		runtime.exec(command);
		runtime.exit(0);
	}
}
