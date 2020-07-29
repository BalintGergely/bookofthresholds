package net.balintgergely.runebook;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.balintgergely.runebook.RuneButtonGroup.RuneButtonModel;
import net.balintgergely.runebook.RuneModel.Path;
import net.balintgergely.runebook.RuneModel.Runestone;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class LargeBuildPanel extends JPanel{
	private static final boolean VERTICAL = true,HORIZONTAL = false;
									
	private static final byte	TYPE_PRIMARY_PATH = 0,
								TYPE_SECONDARY_PATH = 1,
								TYPE_KEYSTONE = 2,
								TYPE_RUNESTONE = 3,
								TYPE_STAT_MOD = 4;
	private static final long serialVersionUID = 1L;
	private IdentityHashMap<Image, Image> grayscaleIconVariants = new IdentityHashMap<>();
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
	RuneButtonGroup model;
	private RoleDisplayButton[] roles;
	public void setRune(Rune rn){
		model.setRune(rn);
	}
	public Rune getRune(boolean fix){
		return model.getRune(fix);
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
		model = new RuneButtonGroup(assetManager.runeModel);
		GroupLayout g = new GroupLayout(this);
		super.setLayout(g);
		g.setHonorsVisibility(false);
		GLG mainGroup = new GLG(g, VERTICAL, Alignment.CENTER, false);
		GLG buildGroup0 = new GLG(g, HORIZONTAL, Alignment.CENTER, false);
		//GLG buildGroup1 = new GLG(g, HORIZONTAL, Alignment.CENTER, false);
		mainGroup.add(buildGroup0);
		//mainGroup.add(buildGroup1);
		{
			Collection<Champion> championList = assetManager.champions.values();
			championBox = new JComboBox<>(championList.toArray(new Champion[championList.size()+1]));
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
			        	rendererLabel.setText(value == null ? "" : value.toString());
			        	rendererLabel.setBackground(isSelected ? Color.WHITE : Color.LIGHT_GRAY);
			        	return rendererLabel;
			        });
			buildGroup0.add(championBox);
		}
		{
			roles = new RoleDisplayButton[5];
			for(int i = 0;i < 5;i++){
				buildGroup0.add(roles[i] = new RoleDisplayButton(assetManager,(byte)(1 << i)));
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
		for(List<RuneButtonModel> lst : model.secondarySlotModels){
			rowOfButtons(g, rightGroup, lst, TYPE_RUNESTONE, true);
		}
		for(List<RuneButtonModel> lst : model.statSlotModels){
			rowOfButtons(g, rightGroup, lst, TYPE_STAT_MOD, false);
		}
		g.setHorizontalGroup(mainGroup.hor);
		g.setVerticalGroup(mainGroup.ver);
		//rightGroup.hor.addGap(0);
		Dimension dim = super.getPreferredSize();
		super.setPreferredSize(dim);
		mainGroup.hor.addGap(dim.width);
		g.setHonorsVisibility(true);
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
				super.setVisible(false);
			}else{
				super.setIcon(ic);
				super.setToolTipText(ic.toString());
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
