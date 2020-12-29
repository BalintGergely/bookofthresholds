package net.balintgergely.runebook;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;
import net.balintgergely.util.JSON.Commentator;
import net.balintgergely.util.JSONBodySubscriber;

public class EmoteDatabase {
	private static Pattern OBVIL = Pattern.compile(
			"/lol-game-data/assets/ASSETS/Loadouts/SummonerEmotes/Champions/(.+)/");
	private static Image readImage(HttpResponse<InputStream> input){
		try{
			BufferedImage image = ImageIO.read(input.body());
			if(image == null){
				return null;
			}else{
				return image.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	@SuppressWarnings("cast")
	public static void main(String[] atgs) throws Throwable{
		HttpClient httpClient = HttpClient.newBuilder().sslContext(LCUManager.makeContext()).build();
		Function<String,HttpRequest.Builder> conref = LCUManager.independentConref();
		JSList emoteList = JSON.toJSList(
				httpClient.send
				(conref.apply(
						"lol-game-data/assets/v1/summoner-emotes.json").GET().build(),
						JSONBodySubscriber.HANDLE_UTF8).body());
		@SuppressWarnings("unchecked")
		CompletableFuture<Image>[] futures = (CompletableFuture<Image>[])new CompletableFuture<?>[emoteList.size()];
		for(int i = 0;i < futures.length;i++){
			futures[i] = httpClient.sendAsync(
					conref.apply(emoteList.getJSMap(i).getString("inventoryIcon").substring(1)).GET().build(),
					BodyHandlers.ofInputStream())
				.thenApply(EmoteDatabase::readImage);
		}
		ArrayList<JSMap> skinList = new ArrayList<>(0x1000);
		JSON.asJSMap(httpClient.send(HttpRequest.newBuilder(
				new URI("https://ddragon.leagueoflegends.com/cdn/10.21.1/data/hu_HU/championFull.json")).GET().build(),
				JSONBodySubscriber.HANDLE_UTF8).body()).getJSMap("data").map.entrySet().parallelStream()
					.flatMap(a -> ((JSMap)a.getValue()).getJSList("skins").list.parallelStream()
						.map(JSON::asJSMap)
						.peek(b -> b.map.replace("name", "default", a.getKey())))
					.forEachOrdered(skinList::add);
		HashMap<Object,Integer> idEmoteMap = new HashMap<>();
		HashMap<Object,JSMap> idSkinMap = new HashMap<>();
		HashMap<String,JSMap> nameSkinMap = new HashMap<>();
		Vector<String> skinNameList = new Vector<>(skinList.size());
		Image[] imageArray = new Image[futures.length];
		JList<?>[] comboBoxArray = new JList<?>[futures.length];
		for(int i = 0;i < futures.length;i++){
			imageArray[i] = futures[i].get();
			idEmoteMap.put(emoteList.getJSMap(i).get("id"), i);
		}
		for(int i = 0;i < skinList.size();i++){
			JSMap mp = skinList.get(i);
			String name = mp.getString("name");
			idSkinMap.put(mp.get("id"), mp);
			nameSkinMap.put(name, mp);
			skinNameList.add(name);
		}
		skinNameList.sort(Comparator.naturalOrder());
		EventQueue.invokeAndWait(() -> {
			JPanel panel = new JPanel(new GridLayout(0, 10));
			panel.setMaximumSize(new Dimension(1000, Integer.MAX_VALUE));
			for(int i = 0;i < imageArray.length;i++){
				JPanel subPanel = new JPanel(new BorderLayout());
				JSMap emoteData = emoteList.getJSMap(i);
				JLabel label = new JLabel("<html>"+emoteData.getString("name")+"</html>");
				label.setHorizontalTextPosition(JLabel.CENTER);
				label.setVerticalTextPosition(JLabel.BOTTOM);
				String iconString = emoteData.getString("inventoryIcon");
				label.setToolTipText(iconString);
				Image image = imageArray[i];
				if(image == null){
					System.out.println("Image at index "+i+" is null!");
				}else{
					label.setIcon(new ImageIcon(image));
				}
				label.setPreferredSize(new Dimension(150, 130));
				JList<String> selectionList = new JList<>(skinNameList);
				//comboBox.setEditable(true);
				Matcher matcher = OBVIL.matcher(iconString);
				//boolean automatic = false;
				JScrollPane listPane = new JScrollPane(selectionList,
						JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
						JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				listPane.setPreferredSize(new Dimension(150, 60));
				if(matcher.find()){
					String gp = matcher.group(1);
					if(skinNameList.contains(gp)){
						//automatic = true;
						selectionList.setSelectedValue(gp, true);
					}
				}
				
				comboBoxArray[i] = selectionList;
				subPanel.add(label,BorderLayout.CENTER);
				subPanel.add(listPane,BorderLayout.PAGE_END);
				panel.add(subPanel);
			}
			JFrame mainFrame = new JFrame("Default Emote Editor");
			mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			mainFrame.add(new JScrollPane(panel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
			mainFrame.pack();
			mainFrame.setLocationRelativeTo(null);
			mainFrame.setVisible(true);
		});
		File targetFile = new File("defaultEmotes.json");
		BiFunction<Object,Object,String> commentator = (a,b) -> {
			try{
				if(b != null){
					Object value = ((List<?>)a).get((int)b);
					Integer mp = idEmoteMap.get(value);
					if(mp != null){
						return emoteList.getJSMap(mp).getString("name");
					}
					JSMap sk = idSkinMap.get(value);
					if(sk != null){
						return sk.getString("name");
					}
				}
			}catch(Throwable t){
				t.printStackTrace();
			}
			return null;
		};
		boolean hasLoaded = false;
		try(Scanner sc = new Scanner(System.in)){
			while(sc.hasNextLine()){
				switch(sc.nextLine()){
				case "save":{
					if(!hasLoaded){
						System.out.println("Not yet loaded!");
						break;
					}
					JSList collector = new JSList(comboBoxArray.length*2);
					for(int i = 0;i < comboBoxArray.length;i++){
						List<?> lst = comboBoxArray[i].getSelectedValuesList();
						if(!lst.isEmpty()){
							collector.add((Object)emoteList.getJSMap(i).get("id"));
							JSList aci;
							if(lst.size() == 1){
								aci = collector;
							}else{
								aci = new JSList(lst.size());
								collector.add(aci);
							}
							for(Object o : lst){
								aci.add((Object)nameSkinMap.get(o).get("id"));
							}
						}
					}
					try(Commentator wrt = new Commentator(new FileWriter(targetFile, StandardCharsets.UTF_8),commentator)){
						JSON.write(collector, wrt);
					}
					System.out.println("Save done.");
					}break;
				case "load":{
						JSList collector = new JSList(comboBoxArray.length*2);
						try(FileReader rd = new FileReader(targetFile, StandardCharsets.UTF_8)){
							JSON.readList(collector.list, rd);
						}
						for(JList<?> lxt : comboBoxArray){
							lxt.clearSelection();
						}
						for(int i = 0;i < collector.size();i += 2){
							int emoteIndex = idEmoteMap.get(collector.get(i));
							Object values = collector.get(i+1);
							if(values instanceof JSList){
								List<?> lst = ((JSList)values).list;
								int[] indices = new int[lst.size()];
								for(int k = 0;k < indices.length;k++){
									indices[k] = skinNameList.indexOf(idSkinMap.get(lst.get(k)).get("name"));
								}
								comboBoxArray[emoteIndex].setSelectedIndices(indices);
							}else{
								comboBoxArray[emoteIndex].setSelectedValue(idSkinMap.get(values).get("name"),true);
							}
						}
						System.out.println("Load done.");
						hasLoaded = true;
					}break;
				}
			}
		}
	}
}
