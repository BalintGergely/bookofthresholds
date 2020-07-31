package net.balintgergely.runebook;

import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ButtonGroup;
import javax.swing.DefaultButtonModel;
import javax.swing.ImageIcon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.balintgergely.runebook.RuneModel.Path;
import net.balintgergely.runebook.RuneModel.Stone;
/**
 * Implements the button model logic used for the runes.
 * @author balintgergely
 *
 */
public class RuneButtonGroup{
	private CopyOnWriteArrayList<ChangeListener> listenerList = new CopyOnWriteArrayList<>();
	public final RuneModel runeModel;
	public final List<RuneButtonModel> primaryPathModels,secondaryPathModels,keystoneModels;
	public final List<List<RuneButtonModel>> primarySlotModels,secondarySlotModels,statSlotModels;
	public RuneButtonGroup(RuneModel model){
		this.runeModel = model;
		int pathCount = model.getPathCount();
		if(pathCount == 0){
			primaryPathModels = List.of();
			secondaryPathModels = List.of();
			primarySlotModels = List.of();
			secondarySlotModels = List.of();
			keystoneModels = List.of();
		}else{
			RuneButtonModel[] ppm = new RuneButtonModel[pathCount],spm = new RuneButtonModel[pathCount];
			int[] slotModel = null;
			for(int i = 0;i < pathCount;i++){
				Path p = model.getPath(i);
				int slots = p.getSlotCount();
				if(slotModel == null){
					slotModel = new int[slots];
				}else if(slotModel.length < slots){
					slotModel = Arrays.copyOf(slotModel, slots);
				}
				for(int s = 0;s < slots;s++){
					slotModel[s] = Math.max(slotModel[s], p.getStoneCountInSlot(s));
				}
				PathButtonModel pbm = new PathButtonModel(p);
				ppm[i] = pbm;
				spm[i] = pbm.alt;
			}
			primaryPathModels = List.of(ppm);
			secondaryPathModels = List.of(spm);
			if(slotModel.length == 0){
				keystoneModels = List.of();
				primarySlotModels = List.of();
				secondarySlotModels = List.of();
			}else{
				int keys = slotModel[0];
				RuneButtonModel[] ksl = new RuneButtonModel[keys];
				for(int i = 0;i < keys;i++){
					ksl[i] = new PrimarySlotButtonModel(0, i);
				}
				keystoneModels = List.of(ksl);
				@SuppressWarnings("unchecked")
				List<RuneButtonModel>[] psl = (List<RuneButtonModel>[]) new List<?>[slotModel.length-1];
				@SuppressWarnings("unchecked")
				List<RuneButtonModel>[] ssl = (List<RuneButtonModel>[]) new List<?>[slotModel.length-1];
				for(int s = 1;s < slotModel.length;s++){
					int slot = slotModel[s];
					RuneButtonModel[] pil = new RuneButtonModel[slot];
					RuneButtonModel[] sil = new RuneButtonModel[slot];
					for(int i = 0;i < slot;i++){
						pil[i] = new PrimarySlotButtonModel(s, i);
						sil[i] = new SecondarySlotButtonModel(s, i);
					}
					psl[s-1] = List.of(pil);
					ssl[s-1] = List.of(sil);
				}
				primarySlotModels = List.of(psl);
				secondarySlotModels = List.of(ssl);
				primaryPathElements = new PrimarySlotButtonModel[slotModel.length];
			}
		}
		int statSlotCount = model.getStatSlotCount();
		@SuppressWarnings("unchecked")
		List<RuneButtonModel>[] msl = (List<RuneButtonModel>[]) new List<?>[statSlotCount];
		for(int s = 0;s < statSlotCount;s++){
			int slot = model.getStatSlotLength(s);
			RuneButtonModel[] mil =  new RuneButtonModel[slot];
			for(int i = 0;i < slot;i++){
				mil[i] = new StatModButtonModel(s, i);
			}
			msl[s] = List.of(mil);
		}
		statSlotModels = List.of(msl);
		statModElements = new StatModButtonModel[statSlotCount];
	}
	public void setRune(Rune rune){
		if(rune.model != runeModel){
			throw new IllegalArgumentException();
		}
		if(secondaryPath != null){
			secondaryPath.deselect();
			pathChanged(false);
		}
		if(rune.primaryPath == null && primaryPath != null){
			primaryPath.deselect();
			pathChanged(true);
		}
		if(rune.primaryPath != null){
			primaryPathModels.get(rune.primaryPath.order).setSelected(true);
		}
		if(rune.secondaryPath != null){
			secondaryPathModels.get(rune.secondaryPath.order).setSelected(true);
			if(rune.primaryPath == null){
				for(List<RuneButtonModel> grp : statSlotModels){
					for(RuneButtonModel g : grp){
						g.fireBridge();
					}
				}
			}
		}
		int slots = primaryPathElements.length;
		for(int i = 0;i < slots;i++){
			int index = rune.getSelectedIndex(0, i);
			if(index < 0){
				if(primaryPathElements[i] != null){
					primaryPathElements[i].deselect();
				}
			}else{
				if(i == 0){
					keystoneModels.get(index).setSelected(true);
				}else{
					primarySlotModels.get(i-1).get(index).setSelected(true);
				}
			}
		}
		for(int i = 1;i < slots;i++){
			int index = rune.getSelectedIndex(1, i);
			if(index >= 0){
				secondarySlotModels.get(i-1).get(index).setSelected(true);
			}
		}
		slots = runeModel.getStatSlotCount();
		for(int i = 0;i < slots;i++){
			int index = rune.getSelectedIndex(2, i);
			if(index < 0){
				if(statModElements[i] != null){
					statModElements[i].deselect();
				}
			}else{
				statSlotModels.get(i).get(index).setSelected(true);
			}
		}
	}
	public Rune getRune(){
		ArrayList<Stone> stoneList = new ArrayList<>(9);
		Path primary;
		if(primaryPath == null){
			primary = null;
		}else{
			primary = primaryPath.path;
			int slots = primary.getSlotCount();
			for(int i = 0;i < slots;i++){
				PrimarySlotButtonModel md = primaryPathElements[i];
				if(md != null){
					stoneList.add(primary.getStone(i, md.index));
				}
			}
		}
		Path secondary;
		if(secondaryPath == null){
			secondary = null;
		}else{
			secondary = secondaryPath.path;
			if(secondA != null){
				stoneList.add(secondary.getStone(secondA.slot, secondA.index));
			}
			if(secondB != null){
				stoneList.add(secondary.getStone(secondB.slot, secondB.index));
			}
		}
		int slots = runeModel.getStatSlotCount();
		for(int i = 0;i < slots;i++){
			StatModButtonModel md = statModElements[i];
			if(md != null){
				stoneList.add(runeModel.getStatstone(i,md.index));
			}
		}
		return new Rune(runeModel,primary,secondary,stoneList);
	}
	public void addChangeListener(ChangeListener ls){
		listenerList.add(ls);
	}
	public void removeChangeListener(ChangeListener ls){
		listenerList.remove(ls);
	}
	private ChangeEvent che;
	private void fireGlobalStateChanged(){
		for(ChangeListener ls : listenerList){
			if(che == null){
				che = new ChangeEvent(this);
			}
			ls.stateChanged(che);
		}
	}
	private void pathChanged(boolean isPrimary){
		if(isPrimary){
			Arrays.fill(primaryPathElements, null);
			int len = keystoneModels.size();
			for(int i = 0;i < len;i++){
				keystoneModels.get(i).parentPathChanged();
			}
			len = secondaryPathModels.size();
			for(int i = 0;i < len;i++){
				secondaryPathModels.get(i).fireBridge();
			}
			if(primaryPath == null){
				for(List<RuneButtonModel> grp : statSlotModels){
					for(RuneButtonModel g : grp){
						g.fireBridge();
					}
				}
			}
		}else{
			secondA = null;
			secondB = null;
		}
		List<List<RuneButtonModel>> pathList = isPrimary ? primarySlotModels : secondarySlotModels;
		int len = pathList.size();
		for(int i = 0;i < len;i++){
			List<RuneButtonModel> mdl = pathList.get(i);
			int mdlen = mdl.size();
			for(int k = 0;k < mdlen;k++){
				mdl.get(k).parentPathChanged();
			}
		}
	}
	private PathButtonModel primaryPath,secondaryPath;
	public static abstract class RuneButtonModel extends DefaultButtonModel{
		private static final long serialVersionUID = 1L;
		private RuneButtonModel(){}
		@Override
		public void setEnabled(boolean b) {
		}
		@Override
		public void setGroup(ButtonGroup group) {
			throw new UnsupportedOperationException();
		}
		private void parentPathChanged() {//Used only on Primary & Secondary rune buttons.
			if((stateMask & SELECTED) != 0){
				stateMask &= ~SELECTED;//Deselects internally only. Caller cleared the arrays.
				fireItemStateChanged(new ItemEvent(this,ItemEvent.ITEM_STATE_CHANGED,this,ItemEvent.DESELECTED));
			}
			fireStateChanged();//State changed event is always reported since the icon is guaranteed to have changed at this point.
		}
        @Override
		public void setPressed(boolean b) {
        	if(!isEnabled()){
        		return;
        	}
			if(isPressed() && isArmed() && !b) {
				setSelected(true);
			}
			super.setPressed(b);
        }
        private void fireBridge(){
        	super.fireStateChanged();
        }
		public abstract ImageIcon getIcon();
	}
	private class PathButtonModel extends RuneButtonModel{
		private static final long serialVersionUID = 1L;
		private final boolean forPrimaryPath;
		private final Path path;
		private final PathButtonModel alt;
		public PathButtonModel(Path path) {
			this.path = path;
			this.forPrimaryPath = true;
			this.alt = new PathButtonModel(path, this);
		}
		private PathButtonModel(Path path,PathButtonModel alt){
			this.path = path;
			this.forPrimaryPath = false;
			this.alt = alt;
		}
		@Override
		public boolean isEnabled() {
			return forPrimaryPath || (primaryPath != null && primaryPath != alt) || secondaryPath == this;
		}
		private void deselect(){
			if(forPrimaryPath){
				primaryPath = null;
			}else{
				secondaryPath = null;
			}
			super.setSelected(false);
			if(forPrimaryPath){
				alt.fireStateChanged();
			}
		}
		@Override
		public void setSelected(boolean b) {
			if(b){
				if(forPrimaryPath){
					if(primaryPath != this){
						boolean firstSelection = primaryPath == null;
						if(secondaryPath != null && secondaryPath == alt){
							alt.deselect();
							pathChanged(false);
						}
						if(!firstSelection){
							primaryPath.deselect();
						}
						primaryPath = this;
						super.setSelected(true);
						alt.fireStateChanged();
						pathChanged(true);
						fireGlobalStateChanged();
						if(firstSelection){
							for(List<RuneButtonModel> grp : statSlotModels){
								for(RuneButtonModel g : grp){
									g.fireBridge();
								}
							}
						}
					}
				}else{
					if(primaryPath != alt && secondaryPath != this){
						if(secondaryPath != null){
							secondaryPath.deselect();
						}
						secondaryPath = this;
						super.setSelected(true);
						pathChanged(false);
						fireGlobalStateChanged();
					}
				}
			}
		}
		@Override
		public ImageIcon getIcon() {
			return path;
		}
	}
	private PrimarySlotButtonModel[] primaryPathElements;
	private class PrimarySlotButtonModel extends RuneButtonModel{
		private static final long serialVersionUID = 1L;
		private final int slot;
		private final int index;
		public PrimarySlotButtonModel(int slot,int index) {
			this.slot = slot;
			this.index = index;
		}
		@Override
		public boolean isEnabled() {
			PathButtonModel pimp = primaryPath;
			return pimp != null && pimp.path.isValidStone(slot, index);
		}
		private void deselect(){
			primaryPathElements[slot] = null;
			super.setSelected(false);
		}
		@Override
		public void setSelected(boolean b) {
			PathButtonModel pimp = primaryPath;
			if(b && pimp != null && pimp.path.isValidStone(slot, index) && primaryPathElements[slot] != this){
				if(primaryPathElements[slot] != null){
					primaryPathElements[slot].deselect();
				}
				primaryPathElements[slot] = this;
				super.setSelected(true);
				fireGlobalStateChanged();
			}
		}
		@Override
		public ImageIcon getIcon() {
			PathButtonModel pt = primaryPath;
			if(pt != null && pt.path.isValidStone(slot, index)){
				return pt.path.getStone(slot, index);
			}
			return null;
		}
	}
	private SecondarySlotButtonModel secondA,secondB;
	private class SecondarySlotButtonModel extends RuneButtonModel{
		private static final long serialVersionUID = 1L;
		private final int slot;
		private final int index;
		public SecondarySlotButtonModel(int slot,int index) {
			this.slot = slot;
			this.index = index;
		}
		@Override
		public boolean isEnabled() {
			PathButtonModel secd = secondaryPath;
			return secd != null && secd.path.isValidStone(slot, index);
		}
		private void deselect(){
			if(secondA == this){
				secondA = null;
			}
			if(secondB == this){
				secondB = null;
			}
			super.setSelected(false);
		}
		@Override
		public void setSelected(boolean b) {
			PathButtonModel secd = secondaryPath;
			if(b && secd != null && secd.path.isValidStone(slot, index) && secondA != this && secondB != this){
				d:{
					if(secondA != null){
						if(secondA.slot == slot){
							secondA.deselect();//A has the same slot. Deselect A and ignore B.
							break d;//A is null and B does not have the same slot.
						}
					}else if(secondB != null){
						if(secondB.slot == slot){
							secondB.deselect();//A is null and B has the same slot. Deselect B.
						}
						break d;//A is null and B does not have the same slot.
					}
					if(secondB != null){
						secondB.deselect();//Both A and B non-null, A does not have the same slot. Deselect B.
					}
					secondB = secondA;//Move A to B.
				}
				secondA = this;
				super.setSelected(true);
				fireGlobalStateChanged();
			}
		}
		@Override
		public ImageIcon getIcon() {
			PathButtonModel pt = secondaryPath;
			if(pt != null && pt.path.isValidStone(slot, index)){
				return pt.path.getStone(slot, index);
			}
			return null;
		}
	}
	private StatModButtonModel[] statModElements;
	private class StatModButtonModel extends RuneButtonModel{
		private static final long serialVersionUID = 1L;
		private final int slot;
		private final int index;
		public StatModButtonModel(int slot,int index) {
			this.slot = slot;
			this.index = index;
		}
		@Override
		public boolean isEnabled() {
			return primaryPath != null || secondaryPath != null;
		}
		private void deselect(){
			statModElements[slot] = null;
			super.setSelected(false);
		}
		@Override
		public void setSelected(boolean b) {
			if(b && statModElements[slot] != this){
				if(statModElements[slot] != null){
					statModElements[slot].deselect();
				}
				statModElements[slot] = this;
				super.setSelected(true);
				fireGlobalStateChanged();
			}
		}
		@Override
		public ImageIcon getIcon() {
			return runeModel.getStatstone(slot, index);
		}
	}
}
