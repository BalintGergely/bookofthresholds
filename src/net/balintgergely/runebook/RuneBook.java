package net.balintgergely.runebook;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.Objects;

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
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;

import net.balintgergely.sutil.BackgroundPanel;
import net.balintgergely.sutil.BareIconButton;
import net.balintgergely.sutil.ListComboBoxModel;

public class RuneBook extends BookOfThresholds.Module{
	private static final long serialVersionUID = 1L;
	private JTextField nameField;
	private JButton /*saveButton,*/eraseButton,completeButton,exportButton,clearSortingButton;
	BuildListModel buildListModel;
	private LargeBuildPanel buildPanel;
	private JComboBox<Champion> championSorterBox;
	private Rune currentRune;
	private JPanel mainPanel;
	RuneBook(BookOfThresholds main){
		super(main);
		AssetManager assets = main.getAssetManager();
		super.setTitle(assets.z.getString("window")+" "+BookOfThresholds.VERSION);
		super.setIconImage(assets.image(0, 3));
	}
	@Override
	void init(){
		this.buildListModel = main.getBuildListModel();
		AssetManager assets = main.getAssetManager();
		LCUManager client = main.getLCUManager();
		mainPanel = new BackgroundPanel(assets.book);
		mainPanel.setLayout(new GridBagLayout());
		mainPanel.setPreferredSize(new Dimension(1200, 675));//  3/4 the size of the image
		super.add(mainPanel);
		buildListModel.addChangeListener(this::rbStateChanged);
		super.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		BuildTransferHandler transferer = new BuildTransferHandler(this);
		{GridBagConstraints con = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0,0,0,0), 0, 0);
			con.fill = GridBagConstraints.BOTH;
			exportButton = toolButton(5,4,"EXPORT",
					assets.z.getString(client == null ? "exportCant" : "exportCan"),
					false);
			Border	basicBuildBorder = new LineBorder(new Color(0,true), 2, true),
					selectedBuildBorder = new LineBorder(Color.WHITE, 2, true);
			BuildIcon rendererBuildIcon = new BuildIcon(false, assets);
			JLabel rendererLabel = new JLabel(rendererBuildIcon);
			rendererLabel.setDoubleBuffered(false);
			rendererLabel.setBorder(basicBuildBorder);
			rendererLabel.setHorizontalAlignment(SwingConstants.LEFT);
			if(client != null){
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
				LCUPerksManager perkManager = main.getLCUModule(LCUPerksManager.class);
				JList<Build> list = new JList<>(perkManager);
				list.setTransferHandler(transferer);
				list.setDragEnabled(true);
				list.setOpaque(false);
				list.setSelectionModel(perkManager);
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
				toolBar.add(completeButton = toolButton(4,4,"FIX",assets.z.getString("fix"),true));
				toolBar.add(nameField = new JTextField(16));
				toolBar.add(eraseButton = toolButton(3,4,"ERASE",assets.z.getString("erase"),false));
				toolBar.add(/*saveButton = */toolButton(2,4,"SAVE",assets.z.getString("save"),true));
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
				buildPanel.model.addChangeListener(this::rbStateChanged);
				currentRune = buildPanel.getRune();
				mainPanel.add(buildPanel,con);
			}
			con.gridy = 2;con.weighty = 0;
			{
				JButton aboutButton = new JButton("<html>"+assets.z.getString("aboutLine0")+"<br>"
						+ assets.z.getString("aboutLine1")+" "+BookOfThresholds.GITHUB+"</html>");
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
				championSorterBox = new JComboBox<>(new ListComboBoxModel<>(assets.championList));
				//championBox.setOpaque(false);
				championSorterBox.setSelectedItem(null);
				championSorterBox.setRenderer(new BareIconButton(null,Color.BLACK));
				championSorterBox.setMaximumSize(championSorterBox.getPreferredSize());
				if(client != null){
					TraderAssistant dd = main.getModule(TraderAssistant.class);
					if(dd != null){
						mainViewToolBar.add(dd.createModuleButton(new JToggleButton()));
					}
					LCUSummonerManager summonerManager = main.getLCUModule(LCUSummonerManager.class);
					final ActionListener btAct = (ActionEvent e) -> championSorterBox.setSelectedItem(((JButton)e.getSource()).getIcon());
					final JButton[] prefButtonList = new JButton[5];
					final Champion[] prefList = new Champion[5];
					for(int i = 0;i < 5;i++){
						JButton bt = new BareIconButton(null,Color.WHITE);
						bt.setMaximumSize(new Dimension(24, 24));
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
							button.setIcon(prefList[index]);
							button.setVisible(true);
							index++;
						}
						while(index < 5){
							prefButtonList[index].setVisible(false);
							index++;
						}
					});
				}
				mainViewToolBar.add(championSorterBox);
				clearSortingButton = toolButton(6, 4, "ORDER", assets.z.getString("revertOrder"), true);
				championSorterBox.addItemListener((ItemEvent e) -> {
					if(e.getStateChange() == ItemEvent.SELECTED){
						buildListModel.sortForChampion(e.getItem());
						clearSortingButton.setVisible(true);
					}
				});
				clearSortingButton.addActionListener((ActionEvent e) -> {
					championSorterBox.setSelectedIndex(-1);
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
				JLabel newBuildLabel = new JLabel(new ImageIcon(assets.image(1, 4)));
				newBuildLabel.setHorizontalTextPosition(SwingConstants.CENTER);
				newBuildLabel.setVerticalTextPosition(SwingConstants.TOP);
				newBuildLabel.setForeground(Color.WHITE);
				String nblt = assets.z.getString("newRune");
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
	}
	@Override
	public void paint(Graphics g) {
		g.drawImage(main.getAssetManager().book, 0, 0, null);
		super.paintComponents(g);
	}
	private JButton toolButton(int imgx,int imgy,String action,String toolTip,boolean enabled){
		JButton button = new JButton(new ImageIcon(main.getAssetManager().image(imgx, imgy)));
		button.setEnabled(enabled);
		button.setToolTipText(toolTip);
		button.setActionCommand(action);
		button.addActionListener(this::actionPerformed);
		return button;
	}
	private void rbStateChanged(ChangeEvent e){
		if(e.getSource() == buildListModel){
			if(buildListModel.isBeingModified()){
				main.setDataChangeFlag();
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
		case "ORDER":{
			championSorterBox.setSelectedIndex(-1);
			clearSortingButton.setVisible(false);
			buildListModel.sortByOrder();
		}break;
		case "SAVE":{
				String name = nameField.getText();
				Champion champion = buildPanel.getChampion();
				byte roles = buildPanel.getSelectedRoles();
				Build build = buildListModel.getSelectedElement();
				if(build != null && 
						(!Objects.equals(champion, build.getChampion())//Champion changed.
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
				AssetManager assetManager = main.getAssetManager();
				String name = nameField.getText();
				if(name == null || name.isBlank()){
					Champion target = buildPanel.getChampion();
					name = (target == null ? "" : target.getName()+" ")+assetManager.z.getString("rune");
				}
				main.getLCUManager().getModule(LCUPerksManager.class).exportRune(currentRune, name).thenAcceptAsync(
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
				Desktop.getDesktop().browse(new URI(BookOfThresholds.GITHUB));
			}catch(Exception e1){
				e1.printStackTrace();
			}
		}
	}
}
