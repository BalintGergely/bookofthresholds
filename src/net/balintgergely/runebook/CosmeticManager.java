package net.balintgergely.runebook;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.balintgergely.runebook.AssetManager.RoleIcon;
import net.balintgergely.sutil.BackgroundPanel;
import net.balintgergely.sutil.BareIconButton;
import net.balintgergely.sutil.ListComboBoxModel;
import net.balintgergely.util.JSList;
import net.balintgergely.util.JSMap;

//We set loadouts via lol-loadouts/v4/loadouts/scope/account

public class CosmeticManager extends BookOfThresholds.Module implements TreeModel{
	public static final String	C_ROLES = "role",
								C_CLASS = "class",
								C_CHAMPION = "champion",
								C_SKIN = "skin";
	private Entry root = new Entry();
	private EventListenerList listenerList = new EventListenerList();
	public CosmeticManager(BookOfThresholds bot){
		super(bot);
	}
	@Override
	void init() {
		AssetManager assets = main.getAssetManager();
		BackgroundPanel panel = new BackgroundPanel(assets.shóma);
		panel.setLayout(new GridBagLayout());
		JTree tree = new JTree(this);
		tree.setRootVisible(true);
		tree.setOpaque(false);
		{GridBagConstraints con = new GridBagConstraints();
			con.gridx = 0;
			con.gridy = 0;
			{
				roles = new BareIconButton[5];
				JPanel roleSubpanel = new JPanel(null,false);
				roleSubpanel.setOpaque(false);
				roleSubpanel.setLayout(new BoxLayout(roleSubpanel,BoxLayout.X_AXIS));
				for(int i = 0;i < 5;i++){
					Color color;
					switch(i){
					case 0:color = new Color(0xC152DC);break;
					case 1:color = new Color(0xDA9435);break;
					case 2:color = new Color(0x098DA9);break;
					case 3:color = new Color(0xB9CA40);break;
					case 4:color = new Color(0xE086A2);break;
					default:color = Color.BLACK;
					}
					BareIconButton bt = new BareIconButton(assets.new RoleIcon((byte)(1 << i),color),Color.WHITE);
					bt.setMaximumSize(new Dimension(24,24));
					bt.setModel(new ToggleButtonModel());
					bt.setForeground(Color.BLACK);
					bt.setToolTipText(assets.getRoleName(i));
					roleSubpanel.add(bt);
				}
				append(panel, con, "Roles", roleSubpanel);
			}
			append(panel, con, "Tag", tagBox = new JComboBox<>(new ListComboBoxModel<>(assets.tagList)));
			append(panel, con, "Champion", championBox = new JComboBox<>(new ListComboBoxModel<>(assets.championsSortedByName)));
			championBox.setRenderer(new BareIconButton(null,Color.BLACK));
			con.gridx += 2;
			con.gridy = 0;
			con.gridheight = GridBagConstraints.REMAINDER;
			con.weightx = 1;
			con.fill = GridBagConstraints.BOTH;
			JScrollPane sc = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			panel.add(sc,con);
			sc.setOpaque(false);
			sc.getViewport().setOpaque(false);
			//championBox.setOpaque(true);
		}
		super.add(panel);
		super.pack();
		super.setLocationRelativeTo(null);
	}
	private static JLabel append(JPanel listPanel,GridBagConstraints con,String name,Component value){
		JLabel label = new JLabel(name);
		label.setOpaque(false);
		listPanel.add(label,con);
		con.gridx++;
		listPanel.add(value,con);
		con.gridx--;
		con.gridy++;
		return label;
	}
	private BareIconButton[] roles;
	private JComboBox<String> tagBox;
	private JComboBox<Champion> championBox;
	private static class Entry{
		String conditionType;
		JSList conditionValues = JSList.EMPTY_LIST;
		List<Entry> children = new Vector<>();
		Map<String,JSMap> loadout = Collections.EMPTY_MAP;
		private TreePath path;
		private Entry(TreePath parentPath){
			path = parentPath.pathByAddingChild(this);
		}
		private Entry(){
			path = new TreePath(this);
		}
		@Override
		public String toString(){
			return path.getParentPath() == null ? "Root" : conditionType+" "+conditionValues;
		}
	}
	@Override
	public Object getRoot() {
		return root;
	}
	@Override
	public Object getChild(Object parent, int index) {
		return ((Entry)parent).children.get(index);
	}
	@Override
	public int getChildCount(Object parent) {
		return ((Entry)parent).children.size();
	}
	@Override
	public boolean isLeaf(Object node) {
		return false;
	}
	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {}
	@Override
	public int getIndexOfChild(Object parent, Object child) {
		return ((Entry)parent).children.indexOf(child);
	}
	@Override
	public void addTreeModelListener(TreeModelListener l) {
		listenerList.add(TreeModelListener.class, l);
	}
	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		listenerList.remove(TreeModelListener.class, l);
	}
}
