package net.balintgergely.runebook;

import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

import net.balintgergely.sutil.HybridListModel;

class BuildTransferHandler extends TransferHandler {
	private static final DataFlavor PRIVATE_FLAVOR = new DataFlavor(BuildTransferable.class, "Rune Book Page");
	private static final DataFlavor[] SUPPORTED =  new DataFlavor[]{PRIVATE_FLAVOR,DataFlavor.imageFlavor,DataFlavor.stringFlavor};
	private static final long serialVersionUID = 1L;
	private final RuneBook bot;
	BuildTransferHandler(RuneBook bot){
		this.bot = bot;
	}
	@Override
	public boolean importData(TransferSupport supp) {
		Component comp = supp.getComponent();
		Transferable t = supp.getTransferable();
		Build bld;
		BuildTransferable tr = null;
		try{
			if(t.isDataFlavorSupported(PRIVATE_FLAVOR)){
				tr = (BuildTransferable)t.getTransferData(PRIVATE_FLAVOR);
				bld = tr.build;
			}else if(t.isDataFlavorSupported(DataFlavor.stringFlavor)){
				String str = (String)t.getTransferData(DataFlavor.stringFlavor);
				bld = bot.main.getAssetManager().exportManager.resolveBuild(str);
			}else{
				return false;
			}
		}catch(Throwable e) {
			e.printStackTrace();
			return false;
		}
		if(bld == null){
			return false;
		}
		if(comp instanceof JList){
			JList<?> list = (JList<?>)comp;
			BuildListModel lstmd = (BuildListModel)list.getModel();
			int dropIndex;
			JList.DropLocation dl = supp.isDrop() ? (JList.DropLocation)supp.getDropLocation() : null;
			if(dl != null){
				dropIndex = dl.getIndex();
			}else{
				dropIndex = lstmd.getMinSelectionIndex();
			}
			if(dropIndex < 0){
				dropIndex = lstmd.getSize();
			}
			if(tr == null){
				lstmd.insertBuild(bld, dropIndex);
			}else{
				if(tr.source == comp){
					lstmd.moveBuild(bld, dropIndex);
					tr.source = null;
				}else{
					lstmd.insertBuild(new Build(bld), dropIndex);
				}
			}
			return true;
		}
		bot.applyBuild(bld);
		return true;
	}
	@Override
	public boolean canImport(TransferSupport supp) {
		return	canImport(supp.getComponent()) ||
				supp.isDataFlavorSupported(PRIVATE_FLAVOR) ||
				supp.isDataFlavorSupported(DataFlavor.stringFlavor);
	}
	public boolean canImport(Component comp){
		return true;
	}
	@Override
	public int getSourceActions(JComponent c) {
		if(c instanceof JList){
			ListModel<?> md = ((JList<?>)c).getModel();
			if(md instanceof HybridListModel){
				return COPY;
			}
		}
		if(c instanceof LargeBuildPanel){
			return COPY;
		}
		return NONE;
	}
	@Override
	public Icon getVisualRepresentation(Transferable t) {
		if(t instanceof BuildTransferable){
			return new ImageIcon(((BuildTransferable)t).toImage());
		}
		return null;
	}
	@Override
	protected Transferable createTransferable(JComponent c) {
		BuildTransferable tr;
		if(c instanceof JList){
			Point pt = c.getMousePosition();
			JList<?> list = ((JList<?>)c);
			ListModel<?> model = list.getModel();
			ListSelectionModel slm = list.getSelectionModel();
			int index = slm.getMinSelectionIndex();
			if(index < 0){
				return null;
			}
			if(pt != null){
				Rectangle rct = list.getCellBounds(index,index);
				pt.x -= rct.x;
				pt.y -= rct.y;
				super.setDragImageOffset(pt);
			}
			Build bld = (Build)model.getElementAt(index);
			if(bld == null){
				return null;
			}
			tr = new BuildTransferable(c,bld);
		}else if(c instanceof LargeBuildPanel){
			tr = new BuildTransferable(c, bot.createBuild());
		}else{
			Container ct = c.getParent();
			if(ct instanceof JComponent){
				return createTransferable((JComponent)ct);
			}else{
				return null;
			}
		}
		super.setDragImage(tr.toImage());
		return tr;
	}
	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {}
	private class BuildTransferable implements Transferable{
		private Build build;
		private JComponent source;
		private BufferedImage image;
		private BuildTransferable(JComponent source,Build build) {
			this.source = source;
			this.build = build;
		}
		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return SUPPORTED.clone();
		}
		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			for(DataFlavor f : SUPPORTED){
				if(flavor.equals(f)){
					return true;
				}
			}
			return false;
		}
		private Image toImage(){
			return image == null ? image = BuildIcon.toImage(bot.main.getAssetManager(),build) : image;
		}
		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if(flavor.equals(DataFlavor.stringFlavor)){
				return ExportManager.toMobafireURL(build.getRune());
			}
			if(flavor.equals(DataFlavor.imageFlavor)){
				return toImage();
			}
			if(flavor.equals(PRIVATE_FLAVOR)){
				return this;
			}
			throw new UnsupportedFlavorException(flavor);
		}
	}
}
