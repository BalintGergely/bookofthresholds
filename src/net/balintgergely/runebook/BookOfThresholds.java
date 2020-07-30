package net.balintgergely.runebook;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
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

import net.balintgergely.runebook.RuneModel.Stone;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;

public class BookOfThresholds extends JFrame{
	private static final long serialVersionUID = 1L;
	private static final File SAVE_FILE = new File("runeBook.json");
	private static final Executor EVENT_QUEUE = EventQueue::invokeLater;
	public static void main(String[] atgs) throws Throwable{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Throwable t){}//Eat
		HttpClient client;
		DataDragon dragon;
		AssetManager assets;
		try{
			client = HttpClient.newBuilder().sslContext(ClientManager.makeContext()).build();
			dragon = new DataDragon(new File("dataDragon.zip"), client);
			assets = new AssetManager(dragon, "10.15.1");
			dragon.finish();
		}catch(Throwable t){
			t.printStackTrace();
			JOptionPane.showMessageDialog(null, t.getMessage(), "Failed to start Book of Thresholds", JOptionPane.ERROR_MESSAGE);
			return;
		}
		CompletableFuture<BookOfThresholds> cmpl = new CompletableFuture<>();
		{
			ClientManager clm;
			try{
				clm = new ClientManager(client);
			}catch(Throwable th){
				clm = null;
				JOptionPane.showMessageDialog(null, th.getMessage()+"\r\nRune Book of Thresholds will not be able to export runes.");
			}
			ArrayList<Build> buildList = new ArrayList<Build>(0);
			if(SAVE_FILE.exists()){
				try(FileReader reader = new FileReader(SAVE_FILE,StandardCharsets.UTF_8)){
					JSMap dataMap = JSON.asJSMap(JSON.readObject(reader), true);
					JSList buildLst = dataMap.getJSList("builds");
					buildList.ensureCapacity(buildLst.size());
					for(Object obj : buildLst){
						try{
							JSMap bld = JSON.asJSMap(obj, true);
							String name = bld.peekString("name");
							Champion champion = assets.champions.get(bld.peekString("champion"));
							byte roles = bld.peekByte("roles");
							JSList perks = bld.getJSList("selectedPerkIds");
							ArrayList<Stone> stoneList = new ArrayList<>();
							int len = perks.size();
							for(int i = 0;i < len;i++){
								stoneList.add(assets.runeModel.fullMap.get(perks.getInt(i)));
							}
							buildList.add(new Build(name, champion, new Rune(stoneList), roles));
						}catch(Throwable t){
							t.printStackTrace();
						}
					}
				}catch(Throwable t){
					t.printStackTrace();
				}
			}
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
		long nextSaveTime = 0;
		while(true){
			synchronized(runeBook.buildList){
				while(!(runeBook.buildListChanged || runeBook.shutdownInitiated)){
					runeBook.buildList.wait();
				}
			}
			if(runeBook.buildListChanged){
				long currentTime = System.currentTimeMillis();
				if(currentTime < nextSaveTime && !runeBook.shutdownInitiated){
					synchronized(runeBook.buildList){
						while((currentTime = System.currentTimeMillis()) < nextSaveTime && !runeBook.shutdownInitiated){
							runeBook.buildList.wait(nextSaveTime-currentTime);
						}
					}
				}
				if(!SAVE_FILE.exists()){
					SAVE_FILE.createNewFile();
				}
				JSList buildList = new JSList(runeBook.buildList.size());
				runeBook.buildListChanged = false;
				for(Build build : runeBook.buildList){
					buildList.add(build.getRune().toJSMap().put(
							"name",build.getName(),
							"champion",build.getChampion().id,
							"roles",build.getRoles()));
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
	private OpenListModel<Build> buildListModel;
	private CopyOnWriteArrayList<Build> buildList;
	private Build currentBuild;
	private LargeBuildPanel buildPanel;
	private Rune currentRune;
	private BookOfThresholds(AssetManager assets,ClientManager client,ArrayList<Build> builds){
		super("The Rune Book of Thresholds");
		super.setIconImage(assets.windowIcon);
		this.assetManager = assets;
		this.clientManager = client;
		this.buildListModel = new OpenListModel<>(builds);
		this.buildList = new CopyOnWriteArrayList<>(builds);
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
		super.add(mainPanel);
		buildListModel.addListSelectionListener(this::valueChanged);
		super.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		{

			JList<Build> list = new JList<>(buildListModel);
			list.setPreferredSize(new Dimension(380, 200));
			list.setOpaque(false);
			list.setBackground(new Color(0,true));
			list.setSelectionModel(buildListModel);
			list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
			list.setVisibleRowCount(0);
			list.setCellRenderer(new BuildRenderer(assetManager));
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
			sidePanel.add(buildPanel,BorderLayout.CENTER);
			JToolBar buildOptionsToolBar = new JToolBar();
			buildOptionsToolBar.setFloatable(false);
			sidePanel.add(buildOptionsToolBar,BorderLayout.PAGE_START);
			{
				JButton aboutButton = new JButton("About");
				aboutButton.setActionCommand("ABOUT");
				aboutButton.addActionListener(this::actionPerformed);
				buildOptionsToolBar.add(aboutButton);
				buildOptionsToolBar.add(exportButton = toolButton(3,4,"EXPORT","Export configuration to League of Legends",false));
				buildOptionsToolBar.add(completeButton = toolButton(2,4,"FIX","Autocomplete configuration",true));
				buildOptionsToolBar.add(nameField = new JTextField(16));
				buildOptionsToolBar.add(eraseButton = toolButton(1,4,"ERASE","Erase current configuration",false));
				buildOptionsToolBar.add(saveButton = toolButton(0,4,"SAVE","Save current configuration",false));
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
			synchronized(buildList){
				buildList.notifyAll();
			}
		}
	}
	private JButton toolButton(int imgx,int imgy,String action,String toolTip,boolean enabled){
		JButton button = new JButton(new ImageIcon(assetManager.iconSprites.getSubimage(imgx*24, imgy*24, 24, 24)));
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
		currentRune = buildPanel.getRune(false);
		boolean runeComplete = currentRune != null;
		saveButton.setEnabled(runeComplete);
		exportButton.setEnabled(clientManager != null && runeComplete);
		completeButton.setEnabled(!runeComplete);
	}
	private void actionPerformed(ActionEvent e){
		switch(e.getActionCommand()){
		case "FIX":
			currentRune = buildPanel.model.getRune(true);
			break;
		case "ERASE":{
				Build build = buildListModel.getSelectedElement();
				if(build != null){
					int index = buildListModel.getMinSelectionIndex();
					buildListModel.clearSelection();
					buildListModel.list.remove(build);
					buildListModel.fireIntervalRemoved(index, index);
					buildList.remove(build);
					buildListChanged = true;
					synchronized(buildList){
						buildList.notifyAll();
					}
				}
			}break;
		case "SAVE":{
				Rune rune = buildPanel.getRune(false);
				if(rune == null){
					return;
				}
				String name = nameField.getText();
				Champion champion = buildPanel.getChampion();
				byte roles = buildPanel.getSelectedRoles();
				Build build = currentBuild;
				if(build != null && 
						(champion != build.getChampion()//Champion changed.
								||(
										!(Objects.equals(name, build.getName()) && roles == build.getRoles()) &&//Either name or roles changed
										!build.getRune().equals(rune)//Rune changed
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
					currentBuild = build = new Build(name, champion, rune, roles);
					buildList.add(build);
					int bindex = buildListModel.list.size();
					buildListModel.list.add(build);
					buildListModel.fireIntervalAdded(bindex, bindex);
					buildListModel.addSelectionInterval(bindex, bindex);
				}else{
					buildListModel.beforeListChange();
					build.setChampion(champion);
					build.setName(name);
					build.setRoles(roles);
					build.setRune(rune);
					int index = buildListModel.getMinSelectionIndex();
					buildListModel.fireContentsChanged(index, index);
					buildListModel.afterListChange();
				}
				buildListChanged = true;
				synchronized(buildList){
					buildList.notifyAll();
				}
			}break;
		case "EXPORT":
			if(currentRune != null){
				clientManager.exportRune(currentRune, nameField.getText()).thenAcceptAsync(
						(String str) -> {
						if(str != null){
							JOptionPane.showMessageDialog(this, str);
						}
						}, EVENT_QUEUE);
			}break;
		case "ABOUT":
			JOptionPane.showMessageDialog(null, "Rune Book for League of Legends by Bálint János Gergely.\r\n"
					+ "Get updates at: github.com/BalintGergely/bookofthresholds");
		}
	}
}
