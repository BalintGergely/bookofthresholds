package net.balintgergely.runebook;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;

import net.balintgergely.runebook.LCUManager.LCUSummonerManager;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;
import net.balintgergely.util.JSONBodySubscriber;

public class BookOfThresholds extends JFrame{
	private static final int compareVersion(String a,String b){
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
	private static final long serialVersionUID = 1L;
	private static final File SAVE_FILE = new File("runeBook.json");
	private static final String GITHUB = "https://balintgergely.github.io/bookofthresholds";
	private static final String VERSION = "3.0.4";
	public static void main(String[] atgs) throws Throwable{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Throwable t){}//Eat
		int argIndex = 0;
		String doFinishUpdate = null;
		boolean updateOnly = false,hasSetOutput = false;
		argReadLoop: while(argIndex < atgs.length){
			String argument = atgs[argIndex++];
			switch(argument){
			case "-finishUpdate":
				if(doFinishUpdate == null && argIndex < atgs.length){
					doFinishUpdate = String.valueOf(atgs[argIndex++]);
					continue argReadLoop;
				}
				break;
			case "-update":
				if(!updateOnly){
					updateOnly = true;
					continue argReadLoop;
				}
				break;
			case "-logFile":
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
		HttpClient client;
		DataDragon dragon;
		AssetManager assets = null;
		LCUManager clm;
		System.out.println("Initializing Nexus...");
		CompletableFuture<HttpResponse<Object>> versionCheckFuture = null;
		try{
			client = HttpClient.newBuilder().sslContext(LCUManager.makeContext()).build();
			versionCheckFuture = doFinishUpdate != null ? null :
					client.sendAsync(HttpRequest.newBuilder(new URI(GITHUB+"/version.json")).GET().build(),
							JSONBodySubscriber.HANDLE_UTF8);
			if(updateOnly){
				return;//In an extreme case where some download logic got messed up, we can bypass it.
			}
			System.out.println("Confronting Data Dragon...");
			//In the future we may allow custom specifying the league version, or automatically fetching the latest from data dragon.
			//Just keep in mind that we do not support versions lower than "9.10.1".
			//It is not reasonable to fall back to that version, but it still has for example, Kleptomancy.
			dragon = new DataDragon(new File("dataDragon.zip"), client, "10.16.1");
			System.out.println("Fetching Runeterran dictionary...");
			String localeString;
			try{
				clm = new LCUManager(client);
				localeString = clm.fetchLocale();
			}catch(Throwable th){
				clm = null;
				localeString = null;
			}
			assets = new AssetManager(dragon, localeString);
			JOptionPane.setDefaultLocale(assets.locale);
			if(clm != null){
				clm.summonerManager.setChampionList(assets.championsByKey);
			}
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
		}finally{
			if(versionCheckFuture != null){
				try{
					HttpResponse<Object> response = updateOnly ? versionCheckFuture.get() : versionCheckFuture.get(5, TimeUnit.SECONDS);
					if(response.statusCode()/100 == 2){
						JSMap versionMap = JSON.toJSMap(response.body());
						if(compareVersion(VERSION,versionMap.peekString("version", "0")) < 0){
							if(assets == null){//We might be on a bugged version. Try to recover without asking.
								System.out.println("Update in progress. "+(updateOnly ? "Requested by user." : "Triggered by loading error."));
								Updater.update(versionMap.getString("download"),
										Locale.getDefault().toLanguageTag(), "NoooNOOO!", "We can fix this!");
								System.exit(1);
							}else{
								switch(JOptionPane.showConfirmDialog(null,
										assets.z.getObject("update"),
										assets.z.getString("window"),
										JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, new ImageIcon(assets.windowIcon))){
								case JOptionPane.NO_OPTION:break;
								case JOptionPane.YES_OPTION:
									Updater.update(versionMap.getString("download"),
											assets.locale.toLanguageTag(), assets.z.getString("window"), assets.z.getString("updating"));
									//$FALL-THROUGH$
								default:System.exit(0);
								}
							}
						}else if(updateOnly){
							System.out.println("Rune Book appears to be on the latest version.");
						}else{
							System.out.println("Version check successfull.");
						}
					}else if(updateOnly){
						System.out.println("Got an unexpected status code: "+response.statusCode());
					}
				}catch(Throwable th){
					th.printStackTrace();
				}
			}
		}
		if(clm == null){
			JOptionPane.showMessageDialog(null, "Rune Book of Thresholds will not be able to export runes.");
		}else{
			clm.perkManager.setRuneModel(assets.runeModel);
		}
		/*{//Test code for rune permutations.
			RuneModel model = assets.runeModel;
			long totalRuneCount = model.totalPermutationCount;
			System.out.println("Testing "+totalRuneCount+" permutations...");
			java.util.Set<List<RuneModel.Stone>> globalPermutationMemory = new java.util.HashSet<>((int)totalRuneCount);
			for(long a = 0;a < totalRuneCount;a++){
				if(a == 27){
					System.out.println("Mark");
				}
				Rune init = new Rune(model, a);
				long b = init.toPermutationCode();
				if(b != a){
					System.out.println("Permutation Code mismatch: "+a+" != "+b+" For rune: "+init.toJSMap());
				}
				if(!globalPermutationMemory.add(init.stoneList)){
					System.out.println("Duplicate rune detected: "+init.toJSMap());
				}
				if(a % 10000 == 0){
					System.out.println("Milestone: "+a+"/"+totalRuneCount+" "+init.toJSMap());
				}
			}//Note to self: The permutation system somehow ended up being perfect the first time. How is that possible?
			System.out.println("Encountered: "+globalPermutationMemory.size()+"/"+totalRuneCount);
			System.out.println("Bit length: "+Long.numberOfTrailingZeros(Long.highestOneBit(totalRuneCount) << 1));
			System.out.println("Base 36 string: "+Long.toString(totalRuneCount, 36));
			System.exit(0);
		}*/
		CompletableFuture<BookOfThresholds> cmpl = new CompletableFuture<>();
		{
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
							Champion champion = champ == null ? null : assets.championsById.get(champ);
							byte roles = bld.peekByte("roles");
							buildList.add(new Build(name, champion, assets.runeModel.parseRune(bld), roles,
									bld.peekLong("order")));
						}catch(Throwable t){
							t.printStackTrace();
						}
					}
				}catch(Throwable t){
					t.printStackTrace();
				}
			}
			System.out.println("Initializing window...");
			final LCUManager clientManager = clm;
			AssetManager assetProxy = assets;
			EventQueue.invokeAndWait(() -> {
				ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
				try{
					cmpl.complete(new BookOfThresholds(assetProxy, clientManager, buildList));
				}catch(Throwable e){
					cmpl.completeExceptionally(e);
				}
			});
		}
		BookOfThresholds runeBook = cmpl.get();
		if(clm != null){
			clm.startSeekerThread();
		}
		if(doFinishUpdate != null){
			Updater.finishUpdate(doFinishUpdate);
		}
		System.out.println("We can store "+assets.runeModel.totalPermutationCount+" different runes!");
		long nextSaveTime = 0;
		CompletableFuture<String> shutdown = null;
		AtomicBoolean k = runeBook.dataChangeFlag;
		while(true){
			synchronized(k){
				while(!(k.get() || runeBook.shutdownInitiated)){
					k.wait();
				}
			}
			if(runeBook.shutdownInitiated && shutdown == null){
				System.out.println("Shutdown initiated.");
				if(clm == null){
					shutdown = CompletableFuture.completedFuture("What am I even running on?");
				}else{
					shutdown = clm.close("Close command sent to client.");
				}
			}
			if(k.get()){
				long currentTime = System.currentTimeMillis();
				if(currentTime < nextSaveTime && !runeBook.shutdownInitiated){
					synchronized(k){
						while((currentTime = System.currentTimeMillis()) < nextSaveTime && !runeBook.shutdownInitiated){
							k.wait(nextSaveTime-currentTime);
						}
					}
				}
				if(!SAVE_FILE.exists()){
					SAVE_FILE.createNewFile();
				}
				k.set(false);
				List<Build> bls = runeBook.buildListModel.publicList;
				JSList buildList = new JSList(bls.size());
				for(Build build : bls){
					JSMap mp = build.getRune().toJSMap().put(
							"name",build.getName(),
							"roles",build.getFlags(),
							"order",build.getOrder());
					Champion ch = build.getChampion();
					if(ch != null){
						mp.put("champion",ch.toString());
					}
					buildList.add(mp);
				}
				JSMap dataMap = new JSMap(Map.of("builds",buildList));
				try(Writer writer = new JSON.PrettyWriter(new FileWriter(SAVE_FILE,StandardCharsets.UTF_8))){
					JSON.write(dataMap, writer);
					writer.flush();
				}catch(Throwable t){
					System.err.println(shutdown == null ? "Ouch. Will see you in the next attempt." : "No. No! We can fix this!");
					t.printStackTrace();
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
	private final AtomicBoolean dataChangeFlag = new AtomicBoolean();
	private volatile boolean shutdownInitiated = false;
	private AssetManager assetManager;
	private LCUManager clientManager;
	private JTextField nameField;
	private JButton /*saveButton,*/eraseButton,completeButton,exportButton;
	private BuildListModel buildListModel;
	private LargeBuildPanel buildPanel;
	private Rune currentRune;
	private JPanel mainPanel;
	private BookOfThresholds(AssetManager assets,LCUManager client,ArrayList<Build> builds){
		super(assets.z.getString("window")+" "+VERSION);
		super.setIconImage(assets.windowIcon);
		this.assetManager = assets;
		this.clientManager = client;
		this.buildListModel = new BuildListModel(builds);
		mainPanel = new JPanel(new GridBagLayout(),false) {
			private static final long serialVersionUID = 1L;
			private Image temp;
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				int offset = 0;//super.getComponent(0).getHeight();
				int width = getWidth();
				int height = getHeight()-offset;
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
				if(temp == null || temp.getWidth(null) != imgwidth || temp.getHeight(null) != imgheight){
					temp = createImage(imgwidth, imgheight);
					Graphics gr = temp.getGraphics();
					gr.drawImage(assetManager.background, 0, 0, imgwidth, imgheight, null);
					gr.dispose();
				}
				g.drawImage(temp, (width-imgwidth)/2, offset+(height-imgheight)/2, imgwidth, imgheight, null);
			}
		};
		mainPanel.setPreferredSize(new Dimension(1200, 675));//  3/4 the size of the image
		super.add(mainPanel);
		buildListModel.addChangeListener(this::stateChanged);
		super.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		BuildTransferHandler transferer = new BuildTransferHandler(this);
		{GridBagConstraints con = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0,0,0,0), 0, 0);
			con.fill = GridBagConstraints.BOTH;
			exportButton = toolButton(3,4,"EXPORT",
					assets.z.getString(client == null ? "exportCant" : "exportCan"),
					false);
			Border	basicBuildBorder = new LineBorder(new Color(0,true), 2, true),
					selectedBuildBorder = new LineBorder(Color.WHITE, 2, true);
			BuildIcon rendererBuildIcon = new BuildIcon(false, assets);
			JLabel rendererLabel = new JLabel(rendererBuildIcon);
			rendererLabel.setDoubleBuffered(false);
			rendererLabel.setBorder(basicBuildBorder);
			rendererLabel.setHorizontalAlignment(SwingConstants.LEFT);
			if(client == null){
				exportButton = toolButton(3,4,"EXPORT",assets.z.getString("exportCant"),false);
			}else{
				exportButton = toolButton(3,4,"EXPORT",assets.z.getString("exportCan"),false);
				JLabel statusLabel = new JLabel();
				statusLabel.setLayout(new BorderLayout());
				//statusLabel.setForeground(Color.WHITE);
				statusLabel.setOpaque(true);
				statusLabel.setHorizontalTextPosition(JLabel.CENTER);
				/*JToolBar toolBar = new JToolBar();
				toolBar.setBorder(new LineBorder(Color.LIGHT_GRAY));
				toolBar.setFloatable(false);
				toolBar.add(statusLabel);
				toolBar.add(importButton);*/
				mainPanel.add(statusLabel,con);
				Border insideBorder = new LineBorder(new Color(0, true), 2);
				Border[] stateBorders = new Border[]{
					new CompoundBorder(new LineBorder(new Color(0xAE9668), 2),insideBorder),
					new CompoundBorder(new LineBorder(new Color(0x7D89DA), 2),insideBorder),
					new CompoundBorder(new LineBorder(new Color(0x3C9AA2), 2),insideBorder),
					new CompoundBorder(new LineBorder(new Color(0x8CBF73), 2),insideBorder),
					new CompoundBorder(new LineBorder(new Color(0xC33C3D), 2),insideBorder),
				};
				ChangeListener ch = e -> {
					int state = client.getState();
					statusLabel.setText(assets.z.getString("state"+state));
					statusLabel.setToolTipText(assets.z.getString("state"+state+"tt"));
					statusLabel.setBorder(stateBorders[state]);
					exportButton.setEnabled(state == 3);
				};
				client.addChangeListener(ch);
				ch.stateChanged(null);
				con.gridy++;
				con.weighty = 1;
				con.gridheight = 0;
				JList<Build> list = new JList<>(client.perkManager);
				list.setTransferHandler(transferer);
				list.setDragEnabled(true);
				list.setOpaque(false);
				list.setSelectionModel(client.perkManager);
				list.setCellRenderer((JList<? extends Build> l, Build v, int index,
							boolean isSelected, boolean cellHasFocus) -> {
								rendererBuildIcon.setBuild(v);
								rendererBuildIcon.setGridVariant(false);
								rendererLabel.setText(null);
								rendererLabel.setIcon(rendererBuildIcon);
								rendererLabel.setOpaque(false);
								rendererLabel.setBorder(isSelected ? selectedBuildBorder : basicBuildBorder);
								return 	rendererLabel;
							});
				JScrollPane sp = new JScrollPane(list,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				sp.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
				sp.setBorder(null);
				sp.setOpaque(false);
				sp.getViewport().setOpaque(false);
				JScrollBar sb = sp.getVerticalScrollBar();
				Dimension prefSb = sb.getPreferredSize();
				Dimension prefRd = rendererLabel.getPreferredSize();
				prefRd.width += prefSb.width;
				sp.setMinimumSize(prefRd);
				mainPanel.add(sp,con);
				con.gridx++;
			}
			con.gridy = 0;con.weighty = 0;con.gridwidth = 1;con.gridheight = 1;
			{
				JToolBar toolBar = new JToolBar();
				//toolBar.setBorder(new LineBorder(Color.LIGHT_GRAY));
				toolBar.setFloatable(false);
				toolBar.add(exportButton);
				exportButton.setVisible(false);
				toolBar.add(completeButton = toolButton(2,4,"FIX",assets.z.getString("fix"),true));
				toolBar.add(nameField = new JTextField(16));
				toolBar.add(eraseButton = toolButton(1,4,"ERASE",assets.z.getString("erase"),false));
				toolBar.add(/*saveButton = */toolButton(1,3,"SAVE",assets.z.getString("save"),true));
				mainPanel.add(toolBar, con, 0);
			}
			con.gridy = 1;con.weighty = 1;
			ActionMap buildPanelActionMap;
			InputMap buildPanelInputMap;
			{
				buildPanel = new LargeBuildPanel(assets);
				buildPanel.setTransferHandler(transferer);
				buildPanel.setFocusable(true);
				buildPanelInputMap = buildPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
				buildPanelActionMap = buildPanel.getActionMap();
				buildPanelActionMap.put(TransferHandler.getCutAction().getValue(Action.NAME),TransferHandler.getCutAction());
				buildPanelActionMap.put(TransferHandler.getCopyAction().getValue(Action.NAME),TransferHandler.getCopyAction());
				buildPanelActionMap.put(TransferHandler.getPasteAction().getValue(Action.NAME),TransferHandler.getPasteAction());
				buildPanel.model.addChangeListener(this::stateChanged);
				currentRune = buildPanel.getRune();
				mainPanel.add(buildPanel,con);
			}
			con.gridy = 2;con.weighty = 0;
			{
				JButton aboutButton = new JButton("<html>"+assets.z.getString("aboutLine0")+"<br>"
						+ assets.z.getString("aboutLine1")+" "+GITHUB+"</html>");
				aboutButton.setActionCommand("ABOUT");
				aboutButton.setOpaque(false);
				aboutButton.addActionListener(this::actionPerformed);
				mainPanel.add(aboutButton,con);
			}
			con.gridx++;con.gridy = 0;con.weightx = 1;con.gridheight = 1;
			JList<Build> buildList = new JList<>(buildListModel);
			{
				JToolBar mainViewToolBar = new JToolBar();
				mainViewToolBar.setFloatable(false);
				mainPanel.add(mainViewToolBar, con, 0);
				JComboBox<Champion> championBox = new JComboBox<>(assets.getSelectableChampionList());
				//championBox.setOpaque(false);
				championBox.setSelectedItem(null);
				rendererLabel.setOpaque(true);
				championBox.setRenderer((
				        JList<? extends Champion> ls,
				        Champion value,
				        int index,
				        boolean isSelected,
				        boolean cellHasFocus) -> {
				        	rendererLabel.setIcon(value);
				        	rendererLabel.setText(cellHasFocus || value == null ? "" : value.getName());
							rendererLabel.setOpaque(true);
				        	rendererLabel.setBackground(isSelected ? Color.WHITE : Color.LIGHT_GRAY);
				        	rendererLabel.setBorder(null);
				        	return rendererLabel;
				        });
				championBox.setMaximumSize(championBox.getPreferredSize());
				if(client != null){
					JToggleButton assistantButton = new JToggleButton(assets.z.getString("tradeAssistWindow"));
					TraderAssistant assistant = new TraderAssistant(assets, client);
					assistant.setTitle(assets.z.getString("tradeAssistWindow"));
					assistant.setIconImage(assets.windowIcon);
					assistantButton.setModel(assistant.callerModel);
					mainViewToolBar.add(assistantButton);
					LCUSummonerManager summonerManager = client.summonerManager;
					final ActionListener btAct = (ActionEvent e) -> championBox.setSelectedItem(((JButton)e.getSource()).getIcon());
					final JButton[] prefButtonList = new JButton[5];
					final int[] prefList = new int[5];
					for(int i = 0;i < 5;i++){
						JButton bt = new JButton();
						mainViewToolBar.add(bt);
						bt.setVisible(false);
						bt.addActionListener(btAct);
						prefButtonList[i] = bt;
					}
					summonerManager.addTableModelListener((TableModelEvent e) -> {
						int size = summonerManager.getNPreferredChampions(prefList);
						int index = 0;
						while(index < size){
							JButton button = prefButtonList[index];
							button.setIcon(assets.championsByKey.get(prefList[index]));
							button.setVisible(true);
							index++;
						}
						while(index < 5){
							prefButtonList[index].setVisible(false);
							index++;
						}
					});
				}
				mainViewToolBar.add(championBox);
				JButton clearSortingButton = new JButton("Revert order");
				championBox.addItemListener((ItemEvent e) -> {
					if(e.getStateChange() == ItemEvent.SELECTED){
						buildListModel.sortForChampion(e.getItem());
						clearSortingButton.setVisible(true);
					}
				});
				clearSortingButton.addActionListener((ActionEvent e) -> {
					championBox.setSelectedIndex(-1);
					clearSortingButton.setVisible(false);
					buildListModel.sortByOrder();
				});
				clearSortingButton.setVisible(false);
				mainViewToolBar.add(clearSortingButton);
			}
			con.gridy++;con.gridheight = 0;
			{
				buildList.setTransferHandler(transferer);
				buildList.setDragEnabled(true);
				buildList.setDropMode(DropMode.INSERT);
				buildList.setOpaque(false);
				buildList.setSelectionModel(buildListModel);
				buildList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
				buildList.setVisibleRowCount(0);
				JLabel newBuildLabel = new JLabel(assetManager.imageIconForImage(0, 4));
				newBuildLabel.setHorizontalTextPosition(SwingConstants.CENTER);
				newBuildLabel.setVerticalTextPosition(SwingConstants.TOP);
				newBuildLabel.setForeground(Color.WHITE);
				String nblt = assetManager.z.getString("newRune");
				buildList.setCellRenderer((JList<? extends Build> l, Build v, int index,
						boolean isSelected, boolean cellHasFocus) -> {
							if(v == null){
								newBuildLabel.setText(isSelected ? nblt : "");
								newBuildLabel.setBorder(isSelected ? selectedBuildBorder : basicBuildBorder);
								return newBuildLabel;
							}
							rendererBuildIcon.setBuild(v);
							rendererBuildIcon.setGridVariant(true);
							rendererLabel.setText(null);
							rendererLabel.setIcon(rendererBuildIcon);
							rendererLabel.setOpaque(false);
							rendererLabel.setBorder(isSelected ? selectedBuildBorder : basicBuildBorder);
							return 	rendererLabel;
						});
				InputMap inputMap = buildList.getInputMap();
				for(KeyStroke ks : inputMap.allKeys()){
					Object action = inputMap.get(ks);
					if(buildPanelActionMap.get(action) != null){
						buildPanelInputMap.put(ks, action);
					}
				}
				JScrollPane sp = new JScrollPane(buildList,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				sp.setBorder(null);
				sp.setOpaque(false);
				sp.getViewport().setOpaque(false);
				mainPanel.add(sp,con);
			}
		}
		super.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				buildPanel.requestFocusInWindow();
			}
		});
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
			super.dispose();
			shutdownInitiated = true;
			synchronized(dataChangeFlag){
				dataChangeFlag.notifyAll();
			}
		}
	}
	private JButton toolButton(int imgx,int imgy,String action,String toolTip,boolean enabled){
		JButton button = new JButton(assetManager.imageIconForImage(imgx, imgy));
		button.setEnabled(enabled);
		button.setToolTipText(toolTip);
		button.setActionCommand(action);
		button.addActionListener(this::actionPerformed);
		return button;
	}
	private void stateChanged(ChangeEvent e){
		if(e.getSource() == buildListModel){
			if(buildListModel.isBeingModified()){
				synchronized(dataChangeFlag){
					dataChangeFlag.set(true);
					dataChangeFlag.notifyAll();
				}
			}
			if(buildListModel.getValueIsAdjusting()){
				return;
			}
			Build selectedElement = buildListModel.getSelectedElement();
			if(selectedElement != null){
				nameField.setText(selectedElement.getName());
				buildPanel.setChampion(selectedElement.getChampion());
				buildPanel.setSelectedRoles(selectedElement.getFlags());
				buildPanel.setRune(currentRune = selectedElement.getRune());
			}
			eraseButton.setEnabled(selectedElement != null);
		}else{
			currentRune = buildPanel.getRune();
			boolean runeComplete = currentRune.isComplete;
			exportButton.setVisible(runeComplete);
			completeButton.setVisible(!runeComplete);
		}
	}
	Build createBuild(){
		return new Build(nameField.getName(), buildPanel.getChampion(), buildPanel.getRune(), buildPanel.getSelectedRoles(), 0);
	}
	void applyBuild(Build bld){
		nameField.setText(bld.getName());
		buildPanel.setChampion(bld.getChampion());
		buildPanel.setSelectedRoles(bld.getFlags());
		buildPanel.setRune(currentRune = bld.getRune());
	}
	AssetManager getAssetManager(){
		return assetManager;
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
			}break;
		case "SAVE":{
				String name = nameField.getText();
				Champion champion = buildPanel.getChampion();
				byte roles = buildPanel.getSelectedRoles();
				Build build = buildListModel.getSelectedElement();
				if(build != null && 
						(!champion.equals(build.getChampion())//Champion changed.
								||(
										!(Objects.equals(name, build.getName()) && roles == build.getFlags()) &&//Either name or roles changed
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
					build = new Build(name, champion, currentRune, roles, System.currentTimeMillis());
					buildListModel.insertBuild(build,buildListModel.getPreferredInsertLocation());
				}else{
					build.setChampion(champion);
					build.setName(name);
					build.setFlags(roles);
					build.setRune(currentRune);
					buildListModel.buildChanged(build);
				}
			}break;
		case "EXPORT":
			if(currentRune.isComplete){
				clientManager.perkManager.exportRune(currentRune, nameField.getText()).thenAcceptAsync(
						(Object str) -> {
						if(str instanceof String){
							JOptionPane.showMessageDialog(this, 
							assetManager.z.getString((String)str), assetManager.z.getString("message"), JOptionPane.ERROR_MESSAGE);
						}
						if(str instanceof Integer){
							JOptionPane.showMessageDialog(this, 
							assetManager.z.getString("errorCode")+" "+str, assetManager.z.getString("message"), JOptionPane.ERROR_MESSAGE);
						}
					}, LCUManager.EVENT_QUEUE);
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
