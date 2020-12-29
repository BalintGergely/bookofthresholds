package net.balintgergely.runebook;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.balintgergely.runebook.Champion.Skin;
import net.balintgergely.runebook.LCUManager.LCUDataRoute;
import net.balintgergely.runebook.LCUManager.LCUModule;
import net.balintgergely.sutil.BackgroundPanel;
import net.balintgergely.sutil.BareIconButton;
import net.balintgergely.sutil.ListComboBoxModel;
import net.balintgergely.util.JSMap;
import net.balintgergely.util.JSON;
import net.balintgergely.util.SimpleOrdinalMap;
import net.balintgergely.util.NavigableList.StringList;
import net.balintgergely.util.OrdinalMap;

//We set loadouts via lol-loadouts/v4/loadouts/scope/account

public class CosmeticManager extends BookOfThresholds.Module implements TreeModel{
	private static final long serialVersionUID = 1L;
	/**
	 * List of tags we do not ignore. Emote slots are self-explanatory.
	 * Companion is the little legend in Howling Abyss and Teamfight Tactics. (Settings shared by lcu.)
	 * Ward skin is the ward used in Summoner's Rift.
	 * 
	 * A fact all three kinds share is the existence of a null value.
	 * null emote means an empty slot without an emote.
	 * null ward means the default ward and ward modell will instead depend on whether it's a trinket, support or control ward.
	 * null companion is the River Sprite who does not appear in howling abyss.
	 */
	public static final StringList LOADOUT_TAG_LIST = new StringList(
			"EMOTES_START","EMOTES_ACE","EMOTES_FIRST_BLOOD","EMOTES_VICTORY",
			"EMOTES_WHEEL_CENTER","EMOTES_WHEEL_LEFT","EMOTES_WHEEL_LOWER","EMOTES_WHEEL_RIGHT","EMOTES_WHEEL_UPPER",
			"COMPANION_SLOT","WARD_SKIN_SLOT");
	public static final String	C_ROLES = "role",
								C_CLASS = "class",
								C_CHAMPION = "champion",
								C_SKIN = "skin";
	private Entry<Object> root = new Entry<>();
	//private Map<Champion,OrdinalSet<Skin>> ownedSkinSet;
	private EventListenerList listenerList = new EventListenerList();
	public CosmeticManager(BookOfThresholds bot){
		super(bot);
	}
	@Override
	void init() {
		main.getLCUModule(LCUCosmeticModule.class);
		AssetManager assets = main.getAssetManager();
		//Map<String,List<Champion>> filteredChampions = new SimpleOrdinalMap<String, List<Champion>>(assets.listOfTags,List.class);
		/*Function<String,List<Champion>> filterListGenerator = (String a) -> {
			ArrayList<Champion> lst = new ArrayList<>(40);
			for(Champion chmp : assets.championList){
				if(chmp.tags.contains(a)){
					lst.add(chmp);
				}
			}
			return lst;
		};*/
		BackgroundPanel panel = new BackgroundPanel(assets.shóma);
		panel.setLayout(new GridBagLayout());
		JTree tree = new JTree(this);
		tree.setRootVisible(true);
		tree.setOpaque(false);
		treeSelectionModel = tree.getSelectionModel();
		treeSelectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		treeSelectionModel.addTreeSelectionListener(this::treeSelectionChanged);
		{GridBagConstraints con = new GridBagConstraints();
			con.gridx = 0;
			con.gridy = 0;
			con.anchor = GridBagConstraints.NORTH;
			{roleCondition = new Condition<>("Role");
				BareIconButton[] roles = new BareIconButton[5];
				roleCondition.setLayout(new BoxLayout(roleCondition,BoxLayout.X_AXIS));
				roleCondition.readUi = () -> {
					byte collect = 0;
					for(int i = 0;i < 5;i++){
						if(roles[i].isSelected()){
							collect |= (1 << i);
						}
					}
					return (collect == 0 || collect == 0x1F) ? null : Byte.valueOf(collect);
				};
				roleCondition.writeUi = b -> {
					byte value = b == null ? 0 : b.byteValue();
					for(int i = 0;i < 5;i++){
						roles[i].setSelected((value & (1 << i)) != 0);
					}
				};
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
					roles[i] = bt;
					bt.setMaximumSize(new Dimension(24,24));
					bt.setModel(new ToggleButtonModel());
					AtomicBoolean knownSelected = new AtomicBoolean();
					bt.addChangeListener((ChangeEvent e) -> {
						boolean t = bt.isSelected();
						if(knownSelected.getAndSet(t) != t){
							updateTreeSelection();
						}
					});
					bt.setForeground(Color.BLACK);
					bt.setToolTipText(assets.getRoleName(i));
					roleCondition.add(bt);
				}
				panel.add(roleCondition,con);
				con.gridy++;
			}
			ListComboBoxModel<String> tagListModel = new ListComboBoxModel<>(assets.listOfTags);
			ListComboBoxModel<Champion> championListModel = new ListComboBoxModel<>(assets.championList);
			ListComboBoxModel<Skin> skinListModel = new ListComboBoxModel<Champion.Skin>(List.of());
			tagListModel.addChangeListener((ChangeEvent e) -> {
				updateTreeSelection();
			});
			championListModel.addChangeListener((ChangeEvent e) -> {
				Champion champion = championListModel.getSelectedItem();
				if(champion == null){
					tagListModel.setList(assets.listOfTags, false);
					skinListModel.setList(List.of(), false);
					skinCondition.setVisible(false);
				}else{
					tagListModel.setList(champion.tags, true);
					skinListModel.setList(assets.championSkins.get(champion), true);
					skinCondition.setVisible(true);
				}
				updateTreeSelection();
			});
			{tagCondition = new Condition<>("Tag");
				tagCondition.setLayout(new BorderLayout());
				tagCondition.readUi = tagListModel::getSelectedItem;
				tagCondition.writeUi = tagListModel::setSelectedItem;
				JComboBox<String> tagBox = new JComboBox<>(tagListModel);
				tagBox.setPreferredSize(tagBox.getPreferredSize());
				tagCondition.add(tagBox);
				panel.add(tagCondition,con);
				con.gridy++;
			}
			{championCondition = new Condition<>("Champion");
				championCondition.setLayout(new BorderLayout());
				championCondition.readUi = championListModel::getSelectedItem;
				championCondition.writeUi = championListModel::setSelectedItem;
				JComboBox<Champion> championBox = new JComboBox<>(championListModel);
				championBox.setRenderer(new BareIconButton(null,Color.BLACK));
				championBox.setPreferredSize(championBox.getPreferredSize());
				championCondition.add(championBox);
				panel.add(championCondition,con);
				con.gridy++;
			}
			{skinCondition = new Condition<>("Skin");
				skinCondition.setLayout(new BorderLayout());
				skinCondition.readUi = skinListModel::getSelectedItem;
				skinCondition.writeUi = skinListModel::setSelectedItem;
				JComboBox<Skin> skinBox = new JComboBox<>(skinListModel);
				skinCondition.add(skinBox);
				skinCondition.setVisible(false);
				panel.add(skinCondition,con);
			}
			con.gridx += 1;
			con.gridy = 0;
			con.gridheight = GridBagConstraints.REMAINDER;
			con.weightx = 1;
			con.fill = GridBagConstraints.BOTH;
			JScrollPane sc = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			panel.add(sc,con);
			sc.setOpaque(false);
			sc.setBorder(null);
			sc.getViewport().setOpaque(false);
			//championBox.setOpaque(true);
		}
		listOfConditions = List.of(roleCondition,tagCondition,championCondition,skinCondition);
		updateTreeSelection();
		super.add(panel);
		super.pack();
		super.setLocationRelativeTo(null);
	}
	private Condition<Byte> roleCondition;
	private Condition<String> tagCondition;
	private Condition<Champion> championCondition;
	private Condition<Skin> skinCondition;
	private List<Condition<?>> listOfConditions;
	private TreeSelectionModel treeSelectionModel;
	private class Entry<E>{
		Condition<E> condition;
		E conditionValue;
		List<Entry<?>> children = new Vector<>();
		final OrdinalMap<String,Object> loadout;
		private TreePath path;
		private Entry(TreePath parentPath){
			path = parentPath.pathByAddingChild(this);
			loadout = new SimpleOrdinalMap<>(LOADOUT_TAG_LIST,Object.class);
		}
		private Entry(){
			path = new TreePath(this);
			loadout = OrdinalMap.emptyMap();
		}
		@Override
		public String toString(){
			return path.getParentPath() == null ? "Root" : condition+" "+conditionValue;
		}
	}
	private class Condition<E> extends JPanel{
		private static final long serialVersionUID = 1L;
		private String name;
		private Supplier<E> readUi;
		private Consumer<E> writeUi;
		Condition(String name){
			super(null,false);
			super.setOpaque(false);
			this.name = name;
		}
		@Override
		public String toString(){
			return name;
		}
	}
	boolean antiRecurse = true;
	private void updateTreeSelection(){
		if(antiRecurse) try{antiRecurse = false;
			Entry<?> currentEntry = root;
			
			a: for(int i = 0;i < listOfConditions.size();i++){
				Condition<?> cnd = listOfConditions.get(i);
				Object value = cnd.readUi.get();
				if(value != null){
					for(Entry<?> sb : currentEntry.children){
						if(sb.condition == cnd && value.equals(sb.conditionValue)){
							currentEntry = sb;
							continue a;
						}
					}
					currentEntry = null;
					break a;
				}
			}
			treeSelectionModel.setSelectionPath(currentEntry == null ? null : currentEntry.path);
		}finally{antiRecurse = true;}
	}
	private <E> Entry<?> generateCurrentEntry(){
		assert antiRecurse; try{antiRecurse = false;
			Entry<?> currentEntry = root;
			Entry<?> newEntry = null;
			int newIndex = -1;
			a: for(int i = 0;i < listOfConditions.size();i++){
				@SuppressWarnings("unchecked")
				Condition<E> cnd = (Condition<E>)listOfConditions.get(i);
				E value = cnd.readUi.get();
				if(value != null){
					for(Entry<?> sb : currentEntry.children){
						if(sb.condition == cnd && value.equals(sb.conditionValue)){
							currentEntry = sb;
							continue a;
						}
					}
					Entry<E> sb = new Entry<>(currentEntry.path);
					sb.condition = cnd;
					sb.conditionValue = value;
					if(newEntry == null){
						newEntry = sb;
						newIndex = currentEntry.children.size();
					}
					currentEntry.children.add(sb);
					currentEntry = sb;
				}
			}
			if(newEntry != null){
				fireTreeNodesInserted(this, newEntry.path.getParentPath(), new int[]{newIndex}, new Object[]{newEntry});
			}
			treeSelectionModel.setSelectionPath(currentEntry.path);
			return currentEntry;
		}finally{antiRecurse = true;}
	}
	@Override
	public Object getRoot() {
		return root;
	}
	@Override
	public Object getChild(Object parent, int index) {
		return ((Entry<?>)parent).children.get(index);
	}
	@Override
	public int getChildCount(Object parent) {
		return ((Entry<?>)parent).children.size();
	}
	@Override
	public boolean isLeaf(Object node) {
		return false;
	}
	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {}
	@Override
	public int getIndexOfChild(Object parent, Object child) {
		return ((Entry<?>)parent).children.indexOf(child);
	}
	@Override
	public void addTreeModelListener(TreeModelListener l) {
		listenerList.add(TreeModelListener.class, l);
	}
	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		listenerList.remove(TreeModelListener.class, l);
	}
	private void treeSelectionChanged(TreeSelectionEvent e){
		TreePath pt = e.getNewLeadSelectionPath();
		if(pt != null){
			int index = listOfConditions.size();
			while(index > 0){
				Entry<?> entry = (Entry<?>)pt.getLastPathComponent();
				index--;
				@SuppressWarnings("unchecked")
				Condition<Object> condition = (Condition<Object>)listOfConditions.get(index);
				if(entry.condition == condition){
					condition.writeUi.accept(entry.conditionValue);
					pt = pt.getParentPath();
					if(pt == null){
						while(index > 0){
							index--;
							listOfConditions.get(index).writeUi.accept(null);
						}
						return;
					}
				}else{
					condition.writeUi.accept(null);
				}
			}
		}
	}
	private void loadoutUpdated(String eventType,Object loadout){
		JSMap loadoutData = JSON.toJSList(loadout).peekJSMap(0);
		if(loadoutData != null){
			JSMap trueLoadout = JSON.toJSMap(loadoutData.peek("loadout"));
			EventQueue.invokeLater(() -> {
				Entry<?> entry = generateCurrentEntry();
				entry.loadout.putAllKeys(trueLoadout.map);
			});
		}
	}
	static class LCUCosmeticModule implements LCUModule{
		//private LCUManager theManager;
		private CosmeticManager theModule;
		LCUCosmeticModule(LCUManager manager,CosmeticManager theModule){
			//this.theManager = manager;
			this.theModule = theModule;
		}
		@Override
		public Map<LCUDataRoute, BiConsumer<String, Object>> getDataRoutes() {
			return Map.of(
					new LCUDataRoute("lol-loadouts/v4/loadouts/scope/account", "OnJsonApiEvent_lol-loadouts_v4_loadouts", false),
					theModule::loadoutUpdated
					);
		}
	}
	protected void fireTreeNodesInserted(Object source, TreePath path, int[] childIndices,Object[] children) {
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TreeModelListener.class) {
				if (e == null){
					e = new TreeModelEvent(source, path, childIndices, children);
				}
				((TreeModelListener)listeners[i+1]).treeNodesInserted(e);
			}
		}
	}
	protected void fireTreeStructureChanged(Object source, TreePath path) {
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TreeModelListener.class) {
				if (e == null){
					e = new TreeModelEvent(source, path, null,null);
				}
				((TreeModelListener)listeners[i+1]).treeStructureChanged(e);
			}
		}
	}
}
