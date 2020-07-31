package net.balintgergely.runebook;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;

import net.balintgergely.runebook.RuneModel.Path;
import net.balintgergely.runebook.RuneModel.Stone;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;

public class BookOfThresholds extends JFrame{
	private static final long serialVersionUID = 1L;
	private static final File SAVE_FILE = new File("runeBook.json");
	private static final Executor EVENT_QUEUE = EventQueue::invokeLater;
	private static final String GITHUB = "https://github.com/BalintGergely/bookofthresholds";
	public static void main(String[] atgs) throws Throwable{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Throwable t){}//Eat
		
		HttpClient client;
		DataDragon dragon;
		AssetManager assets;
		System.out.println("Initializing Nexus...");
		try{
			client = HttpClient.newBuilder().sslContext(ClientManager.makeContext()).build();
			System.out.println("Confronting Data Dragon...");
			dragon = new DataDragon(new File("dataDragon.zip"), client);
			assets = new AssetManager(dragon, "10.15.1");
			System.out.println("Securing Cloud to Earth...");
			if(dragon.finish()){
				System.out.println("File updated.");
			}else{
				System.out.println("File not updated.");
			}
		}catch(Throwable t){
			t.printStackTrace();
			JOptionPane.showMessageDialog(null, t.getMessage(), "Failed to start Rune Book of Thresholds", JOptionPane.ERROR_MESSAGE);
			return;
		}
		CompletableFuture<BookOfThresholds> cmpl = new CompletableFuture<>();
		{
			ClientManager clm;
			try{
				clm = new ClientManager(assets,client);
			}catch(Throwable th){
				clm = null;
				JOptionPane.showMessageDialog(null, th.getMessage()+"\r\nRune Book of Thresholds will not be able to export runes.");
			}
			ArrayList<Build> buildList = new ArrayList<Build>(0);
			System.out.println("Loading saved runes...");
			if(SAVE_FILE.exists()){
				try(FileReader reader = new FileReader(SAVE_FILE,StandardCharsets.UTF_8)){
					JSMap dataMap = JSON.asJSMap(JSON.readObject(reader), true);
					JSList buildLst = dataMap.getJSList("builds");
					buildList.ensureCapacity(buildLst.size());
					for(Object obj : buildLst){
						try{
							JSMap bld = JSON.asJSMap(obj, true);
							String name = bld.peekString("name");
							String champ = bld.peekString("champion");
							Champion champion = champ == null ? null : assets.champions.get(champ);
							byte roles = bld.peekByte("roles");
							JSList perks = bld.getJSList("selectedPerkIds");
							ArrayList<Stone> stoneList = new ArrayList<>();
							Path primaryPath = (Path)assets.runeModel.fullMap.get(bld.peekInt("primaryStyleId"));
							Path secondaryPath = (Path)assets.runeModel.fullMap.get(bld.peekInt("subStyleId"));
							int len = perks.size();
							for(int i = 0;i < len;i++){
								stoneList.add(assets.runeModel.fullMap.get(perks.getInt(i)));
							}
							buildList.add(new Build(name, champion, new Rune(assets.runeModel,primaryPath,secondaryPath,stoneList), roles,
									bld.peekLong("timestamp")));
						}catch(Throwable t){
							t.printStackTrace();
						}
					}
				}catch(Throwable t){
					t.printStackTrace();
				}
			}
			System.out.println("Initializing window...");
			final ClientManager clientManager = clm;
			EventQueue.invokeAndWait(() -> {
				try{
					cmpl.complete(new BookOfThresholds(assets, clientManager, buildList));
				}catch(Throwable e){
					cmpl.completeExceptionally(e);
				}
			});
		}
		BookOfThresholds runeBook = cmpl.get();
		System.out.println("Done. Book, meet new friend!");
		long nextSaveTime = 0;
		while(true){
			synchronized(runeBook.buildListModel){
				while(!(runeBook.buildListChanged || runeBook.shutdownInitiated)){
					runeBook.buildListModel.wait();
				}
			}
			if(runeBook.buildListChanged){
				long currentTime = System.currentTimeMillis();
				if(currentTime < nextSaveTime && !runeBook.shutdownInitiated){
					synchronized(runeBook.buildListModel){
						while((currentTime = System.currentTimeMillis()) < nextSaveTime && !runeBook.shutdownInitiated){
							runeBook.buildListModel.wait(nextSaveTime-currentTime);
						}
					}
				}
				if(!SAVE_FILE.exists()){
					SAVE_FILE.createNewFile();
				}
				List<Build> bls = runeBook.buildListModel.publicList;
				JSList buildList = new JSList(bls.size());
				runeBook.buildListChanged = false;
				for(Build build : bls){
					JSMap mp = build.getRune().toJSMap().put(
							"name",build.getName(),
							"roles",build.getRoles(),
							"timestamp",build.timestamp);
					Champion ch = build.getChampion();
					if(ch != null){
						mp.put("champion",ch.id);
					}
					buildList.add(mp);
				}
				JSMap dataMap = new JSMap(Map.of("builds",buildList));
				try(Writer writer = new JSON.PrettyWriter(new FileWriter(SAVE_FILE,StandardCharsets.UTF_8))){
					JSON.write(dataMap, writer);
					writer.flush();
				}catch(Throwable t){
					t.printStackTrace();
				}
				nextSaveTime = currentTime+10000;
			}else if(runeBook.shutdownInitiated){
				System.exit(0);
			}
		}
	}
	private volatile boolean buildListChanged = false, shutdownInitiated = false;
	private AssetManager assetManager;
	private ClientManager clientManager;
	private JTextField nameField;
	private JButton saveButton,eraseButton,completeButton,exportButton;
	private BuildListModel buildListModel;
	private Build currentBuild;
	private LargeBuildPanel buildPanel;
	private Rune currentRune;
	private BookOfThresholds(AssetManager assets,ClientManager client,ArrayList<Build> builds){
		super(assets.z.getString("window"));
		super.setIconImage(assets.windowIcon);
		this.assetManager = assets;
		this.clientManager = client;
		this.buildListModel = new BuildListModel(builds);
		JPanel mainPanel = new JPanel(new BorderLayout()) {
			private static final long serialVersionUID = 1L;
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				int width = getWidth();
				int height = getHeight();
				BufferedImage image = assetManager.background;
				int imgheight = image.getHeight();
				int imgwidth = image.getWidth();
				double	widthRatio = width/(double)imgwidth,
						heightRatio = height/(double)imgheight;
				if(widthRatio < heightRatio){
					imgwidth = (int)Math.round(imgwidth*heightRatio);
				}else{
					imgwidth = width;
				}
				if(widthRatio > heightRatio){
					imgheight = (int)Math.round(imgheight*widthRatio);
				}else{
					imgheight = height;
				}
				g.drawImage(assetManager.background, (width-imgwidth)/2, (height-imgheight)/2, imgwidth, imgheight, null);
			}
		};
		mainPanel.setPreferredSize(new Dimension(1200, 675));//  3/4 the size of the image
		super.add(mainPanel);
		buildListModel.addListSelectionListener(this::valueChanged);
		super.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		{

			JList<Build> list = new JList<>(buildListModel);
			list.setOpaque(false);
			list.setBackground(new Color(0,true));
			list.setSelectionModel(buildListModel);
			list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
			list.setVisibleRowCount(0);
			list.setCellRenderer(new BuildRenderer(assetManager));
			list.setDragEnabled(true);
			JScrollPane sp = new JScrollPane(list,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			sp.setBorder(null);
			sp.setOpaque(false);
			sp.getViewport().setOpaque(false);
			mainPanel.add(sp,BorderLayout.CENTER);
		}
		{
			JPanel sidePanel = new JPanel(new BorderLayout());
			sidePanel.setOpaque(false);
			mainPanel.add(sidePanel,BorderLayout.LINE_START);
			buildPanel = new LargeBuildPanel(assets);
			buildPanel.model.addChangeListener(this::stateChanged);
			currentRune = buildPanel.getRune();
			sidePanel.add(buildPanel,BorderLayout.CENTER);
			JToolBar buildOptionsToolBar = new JToolBar();
			buildOptionsToolBar.setFloatable(false);
			buildOptionsToolBar.setOpaque(false);
			buildOptionsToolBar.setBorder(null);
			sidePanel.add(buildOptionsToolBar,BorderLayout.PAGE_START);
			{
				buildOptionsToolBar.add(exportButton = toolButton(3,4,"EXPORT",
						assets.z.getString(client == null ? "exportCant" : "exportCan"),
						false,true));
				exportButton.setVisible(false);
				buildOptionsToolBar.add(completeButton = toolButton(2,4,"FIX",assets.z.getString("fix"),true,true));
				buildOptionsToolBar.add(nameField = new JTextField(16));
				buildOptionsToolBar.add(eraseButton = toolButton(1,4,"ERASE",assets.z.getString("erase"),false,false));
				buildOptionsToolBar.add(saveButton = toolButton(0,4,"SAVE",assets.z.getString("save"),true,false));
			}
			{
				JButton aboutButton = new JButton("<html>"+assets.z.getString("aboutLine0")+"<br>"
						+ assets.z.getString("aboutLine1")+" "+GITHUB+"</html>");
				aboutButton.setActionCommand("ABOUT");
				aboutButton.setOpaque(false);
				aboutButton.addActionListener(this::actionPerformed);
				sidePanel.add(aboutButton,BorderLayout.PAGE_END);
				//TODO Add mobafire rune link import/export via the Mobafire class.
			}
		}
		super.pack();
		super.setLocationRelativeTo(null);
		super.setVisible(true);
	}
	@Override
	public void paint(Graphics g) {
		g.drawImage(assetManager.background, 0, 0, null);
		super.paintComponents(g);
	}
	@Override
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			shutdownInitiated = true;
			synchronized(buildListModel){
				buildListModel.notifyAll();
			}
		}
	}
	private JButton toolButton(int imgx,int imgy,String action,String toolTip,boolean enabled,boolean opaque){
		JButton button = new JButton(new ImageIcon(assetManager.iconSprites.getSubimage(imgx*24, imgy*24, 24, 24)));
		button.setOpaque(opaque);
		button.setEnabled(enabled);
		button.setToolTipText(toolTip);
		button.setActionCommand(action);
		button.addActionListener(this::actionPerformed);
		return button;
	}
	private void valueChanged(ListSelectionEvent e){
		if(e.getValueIsAdjusting()){
			return;
		}
		Build selectedElement = buildListModel.getSelectedElement();
		if(currentBuild != selectedElement){
			currentBuild = selectedElement;
			if(selectedElement != null){
				nameField.setText(selectedElement.getName());
				buildPanel.setChampion(selectedElement.getChampion());
				buildPanel.setSelectedRoles(selectedElement.getRoles());
				buildPanel.setRune(currentRune = selectedElement.getRune());
			}
		}
		eraseButton.setEnabled(selectedElement != null);
	}
	private void stateChanged(ChangeEvent e){
		currentRune = buildPanel.getRune();
		boolean runeComplete = currentRune.isComplete;
		exportButton.setEnabled(clientManager != null && runeComplete);
		exportButton.setVisible(runeComplete);
		completeButton.setVisible(!runeComplete);
	}
	private void actionPerformed(ActionEvent e){
		switch(e.getActionCommand()){
		case "FIX":
			if(!currentRune.isComplete){
				buildPanel.setRune(currentRune = currentRune.fix());
			}
			break;
		case "ERASE":{
				buildListModel.removeSelectedBuild();
				buildListChanged = true;
				synchronized(buildListModel){
					buildListModel.notifyAll();
				}
			}break;
		case "SAVE":{
				String name = nameField.getText();
				Champion champion = buildPanel.getChampion();
				byte roles = buildPanel.getSelectedRoles();
				Build build = currentBuild;
				if(build != null && 
						(champion != build.getChampion()//Champion changed.
								||(
										!(Objects.equals(name, build.getName()) && roles == build.getRoles()) &&//Either name or roles changed
										!build.getRune().equals(currentRune)//Rune changed
								)
						)
				){
					//If champion is changed, that definitely means we should make a new build. However...
					//It makes no sense to change the name or the roles without changing the runes AND saving it as a new build.
					//It makes no sense to change the runes without changing the roles or the name AND saving it as a new build.
					//Therefore, we only make a new build if what changes is: (champion || ((name || roles) && runes))
					//Example of changing only runes and name: "Aggressive" top Gnar. "Defensive" top Gnar.
					//Example of changing only runes and role: "Any name" top Gnar. "Any name" bot Gnar. (Yes, seriously.)
					//This is because the name and the roles can be treated as a very similar labelling tool. You can use only one of them.
					build = null;
				}
				if(build == null){
					currentBuild = build = new Build(name, champion, currentRune, roles, System.currentTimeMillis());
					buildListModel.insertBuild(build);
				}else{
					build.setChampion(champion);
					build.setName(name);
					build.setRoles(roles);
					build.setRune(currentRune);
					build.timestamp = System.currentTimeMillis();
					buildListModel.buildChanged(build);
				}
				buildListChanged = true;
				synchronized(buildListModel){
					buildListModel.notifyAll();
				}
			}break;
		case "EXPORT":
			if(currentRune.isComplete){
				if(currentBuild != null){
					currentBuild.timestamp = System.currentTimeMillis();
				}
				clientManager.exportRune(currentRune, nameField.getText()).thenAcceptAsync(
						(String str) -> {
						if(str != null){
							JOptionPane.showMessageDialog(this, str, assetManager.z.getString("message"), JOptionPane.ERROR_MESSAGE);
						}
						}, EVENT_QUEUE);
			}break;
		case "ABOUT":
			try{
				Desktop.getDesktop().browse(new URI(GITHUB));
			}catch(Exception e1){
				e1.printStackTrace();
			}
		}
	}
}
