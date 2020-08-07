package net.balintgergely.runebook;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;

import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;

import net.balintgergely.runebook.RuneButtonGroup.RuneButtonModel;
import net.balintgergely.runebook.RuneModel.Path;
import net.balintgergely.runebook.RuneModel.Runestone;

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
	private static final String[] ROLE_NAMES = new String[]{"roleTop","roleMid","roleBot","roleJg","roleSp"};
	private IdentityHashMap<Image, Image> grayscaleIconVariants = new IdentityHashMap<>();
	private static final Color TT_BACKGROUND = new Color(0xFF010A13),TT_FOREGROUND = new Color(0xFFCEC6B7);
	private static final Border TT_BORDER = new CompoundBorder(new LineBorder(new Color(0xFF00080E),1), new LineBorder(new Color(0xFF463714), 2));
	private Function<Image, Image> getGrayscaleImage = (Image a) -> {
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
		return createImage(new FilteredImageSource(a.getSource(), filter));
	};
	private JComboBox<Champion> championBox;
	private JLabel descriptionLabel;
	RuneButtonGroup model;
	private RoleDisplayButton[] roles;
	public void setRune(Rune rn){
		model.setRune(rn);
	}
	public Rune getRune(){
		return model.getRune();
	}
	public Champion getChampion(){
		return (Champion)championBox.getSelectedItem();
	}
	public void setChampion(Champion ch){
		championBox.setSelectedItem(ch);
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
		super.setOpaque(false);
		super.setBackground(new Color(0,true));
		super.setFocusable(true);
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
			Collection<Champion> championList = assetManager.champions.values();
			championBox = new JComboBox<>(championList.toArray(new Champion[championList.size()+1]));
			//championBox.setOpaque(false);
			championBox.setSelectedItem(null);
			JLabel rendererLabel = new JLabel();
			rendererLabel.setOpaque(true);
			championBox.setRenderer((
			        JList<? extends Champion> list,
			        Champion value,
			        int index,
			        boolean isSelected,
			        boolean cellHasFocus) -> {
			        	rendererLabel.setIcon(value);
			        	rendererLabel.setText(cellHasFocus || value == null ? "" : value.getName());
			        	rendererLabel.setBackground(isSelected ? Color.WHITE : Color.LIGHT_GRAY);
			        	return rendererLabel;
			        });
			championBox.setMaximumSize(championBox.getPreferredSize());
			buildGroup0.add(championBox);
			super.add(championBox);
		}
		{
			roles = new RoleDisplayButton[5];
			for(int i = 0;i < 5;i++){
				RoleDisplayButton bt = new RoleDisplayButton(assetManager,(byte)(1 << i));
				bt.setToolTipText(assetManager.z.getString(ROLE_NAMES[i]));
				super.add(bt);
				buildGroup0.add(roles[i] = bt);
			}
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
				super.setToolTipText(model.getIcon().getDescription());
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
			ImageIcon ic = md.getIcon();
			if((type == TYPE_SECONDARY_PATH || type == TYPE_STAT_MOD) && !md.isEnabled()){
				ic = null;
			}
			if(ic == null){
				super.setIcon(null);
				super.setVisible(false);
			}else{
				super.setIcon(ic);
				super.setVisible(true);
			}
		}
		private int getPreferredDim(){
			switch(type){
			case TYPE_KEYSTONE:return RuneModel.KEYSTONE_SIZE;
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
			ImageIcon ic = (ImageIcon)getIcon();
			if(ic != null){
				Image id = ic.getImage();
				int size = getPreferredDim();
				int offset = (size-id.getWidth(null))/2;
				boolean selected = super.getModel().isSelected();
				boolean rollover = super.getModel().isRollover() || super.isFocusOwner();
				boolean armed = super.getModel().isArmed();
				if(selected || armed){
					Color color = Color.WHITE;
					if(ic instanceof RuneModel.Runestone){
						color = ((Runestone)ic).path.color;
					}else if(ic instanceof RuneModel.Path){
						color = ((Path)ic).color;
					}
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
				if((armed || !rollover) && !selected){
					id = grayscaleIconVariants.computeIfAbsent(id, getGrayscaleImage);
				}
				gr.drawImage(id, offset, offset, null);
			}
		}
		@Override
		public String toString(){
			Icon ic = super.getIcon();
			return ic == null ? "" : ic.toString();
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
	}
}
