package net.balintgergely.runebook;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.Icon;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;

import net.balintgergely.runebook.Champion.Variant;
import net.balintgergely.runebook.RuneButtonGroup.RuneButtonModel;
import net.balintgergely.runebook.RuneModel.Runestone;
import net.balintgergely.runebook.RuneModel.Stone;
import net.balintgergely.sutil.BareIconButton;
import net.balintgergely.sutil.ListComboBoxModel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class LargeBuildPanel extends JPanel implements MouseListener,FocusListener{
	private static final boolean VERTICAL = true,HORIZONTAL = false;
	private static final byte	TYPE_PRIMARY_PATH = 0,
								TYPE_SECONDARY_PATH = 1,
								TYPE_KEYSTONE = 2,
								TYPE_RUNESTONE = 3,
								TYPE_STAT_MOD = 4;
	private static final long serialVersionUID = 1L;
	private IdentityHashMap<Stone, Image> grayscaleIconVariants = new IdentityHashMap<>();
	private static final Color TT_BACKGROUND = new Color(0xFF010A13),TT_FOREGROUND = new Color(0xFFCEC6B7);
	private static final Border TT_BORDER = new CompoundBorder(new LineBorder(new Color(0xFF00080E),1), new LineBorder(new Color(0xFF463714), 2));
	private AssetManager assets;
	private Function<Stone, Image> getGrayscaleImage = (Stone a) -> {
		Image img = assets.runeIcons.get(a);
		if(img != null){
			RGBImageFilter filter = new RGBImageFilter(){
				@Override
				public int filterRGB(int x, int y, int rgb) {
					int all = 
							((rgb >>> 16) & 0xff)
							+((rgb >>> 8) & 0xff)
							+((rgb) & 0xff);
					all /= 3;
					return (rgb & 0xff000000) | (all << 16) | (all << 8) | all;
				}
			};
			return createImage(new FilteredImageSource(img.getSource(), filter));
		}
		return null;
	};
	private JComboBox<Champion> championBox;
	private JLabel descriptionLabel;
	private JTextField codeField;
	private BareIconButton variantButton;
	RuneButtonGroup model;
	private BareIconButton[] roles;
	private List<Variant> variantList;
	private int variantIndex;
	public void setRune(Rune rn){
		model.setRune(rn);
	}
	public Rune getRune(){
		return model.getRune();
	}
	public Champion getChampion(){
		return (Champion)variantButton.getIcon();
	}
	public void setChampion(Champion ch){
		championBox.setSelectedItem(ch);
		if(variantList != null){
			variantIndex = ch instanceof Variant ? variantList.indexOf(ch) : -1;
			variantButton.setIcon(ch);
		}
	}
	public byte getSelectedRoles(){
		byte bits = 0;
		for(int i = 0;i < 5;i++){
			if(roles[i].isSelected()){
				bits |= (1 << i);
			}
		}
		return bits;
	}
	public void setSelectedRoles(byte bits){
		for(int i = 0;i < 5;i++){
			roles[i].setSelected((bits & (1 << i)) != 0);
		}
	}
	public LargeBuildPanel(AssetManager assetManager){
		super(null, false);
		this.assets = assetManager;
		super.setOpaque(false);
		super.setBackground(new Color(0,true));
		model = new RuneButtonGroup(assetManager.runeModel);
		GroupLayout g = new GroupLayout(this);
		super.setLayout(g);
		g.setHonorsVisibility(false);
		GLG mainGroup = new GLG(g, VERTICAL, Alignment.LEADING, false);
		GLG buildGroup0 = new GLG(g, HORIZONTAL, Alignment.CENTER, false);
		//GLG buildGroup1 = new GLG(g, HORIZONTAL, Alignment.CENTER, false);
		mainGroup.add(buildGroup0);
		//mainGroup.add(buildGroup1);
		{
			championBox = new JComboBox<>(new ListComboBoxModel<>(assets.championsSortedByName));
			//championBox.setOpaque(false);
			championBox.setSelectedItem(null);
			championBox.setRenderer(new BareIconButton(null,Color.BLACK));
			championBox.addItemListener((ItemEvent e) -> {
				if(e.getStateChange() == ItemEvent.DESELECTED){
					variantButton.setVisible(false);
					variantButton.setIcon(null);
					variantIndex = -1;
				}
				if(e.getStateChange() == ItemEvent.SELECTED){
					Object item = e.getItem();
					variantButton.setIcon((Icon)item);
					variantList = assetManager.championVariants.get(item);
					if(variantList != null){
						variantButton.setVisible(true);
					}
				}
			});
			championBox.setMaximumSize(championBox.getPreferredSize());
			buildGroup0.add(championBox);
			buildGroup0.add(variantButton = new BareIconButton(null,Color.WHITE),24,24);
			variantButton.setVisible(false);
			variantButton.addActionListener((ActionEvent e) -> {
				if(variantList != null){
					variantIndex++;
					if(variantIndex >= variantList.size()){
						variantIndex = -1;
						variantButton.setIcon((Champion)championBox.getSelectedItem());
					}else{
						variantButton.setIcon(variantList.get(variantIndex));
					}
				}
			});
			g.setHonorsVisibility(variantButton, false);
			super.add(championBox);
		}
		{
			roles = new BareIconButton[5];
			for(int i = 0;i < 5;i++){
				BareIconButton bt = new BareIconButton(assetManager.new RoleIcon((byte)(1 << i)),Color.WHITE);
				bt.setMaximumSize(new Dimension(24,24));
				bt.setModel(new ToggleButtonModel());
				bt.setToolTipText(assets.getRoleName(i));
				super.add(bt);
				buildGroup0.add(roles[i] = bt,24,24);
			}
		}
		buildGroup0.hor.addGap(0, 10, 10);
		{
			buildGroup0.add(codeField = new JTextField(5));
			codeField.setMaximumSize(codeField.getPreferredSize());
			codeField.setOpaque(false);
			codeField.setForeground(Color.WHITE);
			Border completeBorder = codeField.getBorder();
			Border incorrectBorder = new LineBorder(Color.RED, 1);
			codeField.setBorder(null);
			codeField.setCaretColor(Color.WHITE);
			codeField.setToolTipText(assetManager.z.getString("permtt"));
			super.add(codeField);
			model.addChangeListener((ChangeEvent e) -> {
				Rune rune = model.getRune();
				if(rune.isComplete){
					codeField.setBorder(completeBorder);
					codeField.setText(Long.toString(rune.toPermutationCode(), 36).toUpperCase());
				}else{
					codeField.setBorder(null);
					codeField.setText("");
				}
			});
			codeField.addActionListener((ActionEvent e) -> {
				String str = codeField.getText();
				try{
					model.setRune(Rune.ofPermutationCode(model.runeModel,Long.parseLong(str, 36)));
					codeField.setBorder(completeBorder);
				}catch(Throwable t){
					codeField.setBorder(incorrectBorder);
				}
			});
		}
		GLG meatGroup = new GLG(g, HORIZONTAL, Alignment.CENTER, false);
		GLG leftGroup = new GLG(g, VERTICAL, Alignment.CENTER, false);
		GLG rightGroup = new GLG(g, VERTICAL, Alignment.CENTER, false);
		{
			Group topGroup = g.createParallelGroup();//TOP VERTICAL GROUP
			mainGroup.ver.addGroup(topGroup);
			Group topLeftGroup = g.createSequentialGroup();
			Group topRightGroup = g.createSequentialGroup();
			leftGroup.hor.addGroup(topLeftGroup);
			rightGroup.hor.addGroup(topRightGroup);
			for(RuneButtonModel md : model.primaryPathModels){
				RuneButton bt = new RuneButton(md, TYPE_PRIMARY_PATH);
				topGroup.addComponent(bt);
				topLeftGroup.addComponent(bt);
			}
			meatGroup.add(leftGroup);
			meatGroup.hor.addGap(24);
			meatGroup.add(rightGroup);
			mainGroup.add(meatGroup);
			GLG ksRow1 = new GLG(g, HORIZONTAL, Alignment.CENTER, false);
			GLG ksRow2 = new GLG(g, HORIZONTAL, Alignment.CENTER, false);
			boolean k = false;
			for(RuneButtonModel md : model.keystoneModels){
				(k ? ksRow2 : ksRow1).add(new RuneButton(md, TYPE_KEYSTONE));
				k = !k;
			}
			leftGroup.add(ksRow1);
			leftGroup.add(ksRow2);
			for(List<RuneButtonModel> lst : model.primarySlotModels){
				rowOfButtons(g, leftGroup, lst, TYPE_RUNESTONE, true);
			}
			boolean first = true;
			for(RuneButtonModel md : model.secondaryPathModels){
				RuneButton bt = new RuneButton(md, TYPE_SECONDARY_PATH);
				topGroup.addComponent(bt);
				topRightGroup.addComponent(bt);
				if(first){
					g.setHonorsVisibility(bt,true);
					first = false;
				}
			}
			leftGroup.hor.addGap(152);
			for(List<RuneButtonModel> lst : model.secondarySlotModels){
				rowOfButtons(g, rightGroup, lst, TYPE_RUNESTONE, true);
			}
			rightGroup.hor.addGap(152);
			for(List<RuneButtonModel> lst : model.statSlotModels){
				rowOfButtons(g, rightGroup, lst, TYPE_STAT_MOD, false);
			}
		}
		{
			mainGroup.add(descriptionLabel = new JLabel());
			descriptionLabel.setForeground(TT_FOREGROUND);
			descriptionLabel.setBackground(TT_BACKGROUND);
			descriptionLabel.setOpaque(true);
			descriptionLabel.setBorder(TT_BORDER);
			descriptionLabel.setPreferredSize(new Dimension(366, 100));
			descriptionLabel.setVisible(false);
		}
		g.setHorizontalGroup(mainGroup.hor);
		g.setVerticalGroup(mainGroup.ver);
		g.setHonorsVisibility(true);
		super.setPreferredSize(new Dimension(366,500));
	}
	private void rowOfButtons(GroupLayout groupLayout,GLG group,List<RuneButtonModel> models,byte type,boolean markFirst){
		GLG gr1 = new GLG(groupLayout, HORIZONTAL, Alignment.CENTER, false);
		for(RuneButtonModel md : models){
			RuneButton bt = new RuneButton(md, type);
			gr1.add(bt);
			if(markFirst){
				groupLayout.setHonorsVisibility(bt, false);
				markFirst = false;
			}
		}
		group.add(gr1);
	}
	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {
		descriptionLabel.setText(e.getSource().toString());
		descriptionLabel.setVisible(true);
	}
	@Override
	public void mouseExited(MouseEvent e) {
	}
	@Override
	public void focusGained(FocusEvent e) {
		descriptionLabel.setText(e.getSource().toString());
		descriptionLabel.setVisible(true);
	}
	@Override
	public void focusLost(FocusEvent e) {
	}
	public class RuneButton extends JToggleButton{
		private static final long serialVersionUID = 1L;
		private final byte type;
		private Stone stone;
		private Color color = Color.WHITE;
		RuneButton(RuneButtonModel model,byte type){
			super();
			super.setModel(model);
			super.setOpaque(false);
			super.setRolloverEnabled(true);
			this.type = type;
			updateOnModel(model);
			if(type == TYPE_KEYSTONE || type == TYPE_RUNESTONE){
				super.addMouseListener(LargeBuildPanel.this);
				super.addFocusListener(LargeBuildPanel.this);
			}else{
				super.setToolTipText(model.getStone().name);
			}
			LargeBuildPanel.this.add(this);
		}
		@Override
		public JToolTip createToolTip(){
			JToolTip tt = super.createToolTip();
			tt.setBackground(TT_BACKGROUND);
			tt.setForeground(TT_FOREGROUND);
			tt.setBorder(TT_BORDER);
			return tt;
		}
		@Override
		protected void fireStateChanged() {
			super.fireStateChanged();
			updateOnModel((RuneButtonModel)super.getModel());
		}
		private void updateOnModel(RuneButtonModel md){
			stone = (type == TYPE_SECONDARY_PATH || type == TYPE_STAT_MOD) && !md.isEnabled() ? null : md.getStone();
			if(stone == null){
				super.setVisible(false);
			}else{
				if(type != TYPE_STAT_MOD){
					color = assets.runeColors.get(stone);
				}
				super.setVisible(true);
			}
		}
		private int getPreferredDim(){
			switch(type){
			case TYPE_KEYSTONE:return AssetManager.KEYSTONE_SIZE;
			case TYPE_PRIMARY_PATH:
			case TYPE_SECONDARY_PATH:
			case TYPE_RUNESTONE:return 38;
			case TYPE_STAT_MOD:return 32;
			default:return 0;
			}
		}
		@Override
		public Dimension getMinimumSize(){
			return getPreferredSize();
		}
		@Override
		public Dimension getMaximumSize(){
			return getPreferredSize();
		}
		@Override
		public Dimension getPreferredSize(){
			int size = getPreferredDim();
			return new Dimension(size,size);
		}
		@Override
		public void paint(Graphics graphics){
			Graphics2D gr = (Graphics2D)graphics;
			if(stone != null){
				boolean selected = super.getModel().isSelected();
				boolean rollover = super.getModel().isRollover();
				boolean armed = super.getModel().isArmed();
				Image image = (armed || !rollover) && !selected ?
								grayscaleIconVariants.computeIfAbsent(stone, getGrayscaleImage) :
								assets.runeIcons.get(stone);
				int size = getPreferredDim();
				int offset = (size-image.getWidth(null))/2;
				if(selected || armed){
					gr.setColor(color);
					gr.setStroke(new BasicStroke(3));
					gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					if(size < 36){
						gr.drawOval(2, 2, size-4, size-4);
					}else{
						int coffset = (size-34)/2;
						gr.drawOval(coffset, coffset, 33, 33);
					}
				}
				gr.drawImage(image, offset, offset, null);
			}
		}
		@Override
		public String toString(){
			return stone instanceof Runestone ? ((Runestone)stone).description : null;
		}
	}
	private static class GLG{
		Group hor;
		Group ver;
		GLG(GroupLayout group,boolean direction,Alignment alignment,boolean resizable){
			if(direction){//VERTICAL
				ver = group.createSequentialGroup();
				hor = group.createParallelGroup(alignment, resizable);
			}else{//HORIZONTAL
				ver = group.createParallelGroup(alignment, resizable);
				hor = group.createSequentialGroup();
			}
		}
		void add(GLG g){
			ver.addGroup(g.ver);
			hor.addGroup(g.hor);
		}
		void add(Component c){
			ver.addComponent(c);
			hor.addComponent(c);
		}
		void add(Component c,int width,int height){
			ver.addComponent(c,height,height,height);
			hor.addComponent(c,width,width,width);
		}
	}
}
