package net.balintgergely.runebook;

import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

class BuildTransferHandler extends TransferHandler {
	private static final DataFlavor PRIVATE_FLAVOR = new DataFlavor(BuildTransferable.class, "Rune Book Page");
	private static final DataFlavor[] SUPPORTED =  new DataFlavor[]{PRIVATE_FLAVOR,DataFlavor.imageFlavor,DataFlavor.stringFlavor};
	private static final long serialVersionUID = 1L;
	private final BookOfThresholds bot;
	private BuildIcon buildIcon = new BuildIcon(false, null);
	BuildTransferHandler(BookOfThresholds bot){
		this.bot = bot;
	}
	@Override
	public boolean importData(TransferSupport supp) {
		Component comp = supp.getComponent();
		Transferable t = supp.getTransferable();
		if(!canImport(comp)){
			return false;
		}
		Build bld;
		BuildTransferable tr = null;
		try{
			if(t.isDataFlavorSupported(PRIVATE_FLAVOR)){
				tr = (BuildTransferable)t.getTransferData(PRIVATE_FLAVOR);
				bld = tr.build;
			}else if(t.isDataFlavorSupported(DataFlavor.stringFlavor)){
				String str = (String)t.getTransferData(DataFlavor.stringFlavor);
				Rune rn;
				try{
					rn = Mobafire.resolveLink(bot.getAssetManager().runeModel, str);
				}catch(Throwable x){
					return false;//Eat
				}
				bld = new Build("", null, rn, (byte)0, System.currentTimeMillis());
			}else{
				return false;
			}
		}catch(Throwable e) {
			e.printStackTrace();
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
		if(comp instanceof LargeBuildPanel){
			bot.applyBuild(bld);
			return true;
		}
		return false;
	}
	@Override
	public boolean canImport(TransferSupport supp) {
		return	canImport(supp.getComponent()) ||
				supp.isDataFlavorSupported(PRIVATE_FLAVOR) ||
				supp.isDataFlavorSupported(DataFlavor.stringFlavor);
	}
	public boolean canImport(Component comp){
		if(comp instanceof JList){
			ListModel<?> md = ((JList<?>)comp).getModel();
			if(md instanceof BuildListModel){
				return true;
			}
		}
		return comp instanceof LargeBuildPanel;
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
			Build bld = ((BuildTransferable)t).build;
			buildIcon.setGridVariant((bld.getRoles() | 0x20) == 0);
			buildIcon.setBuild(bld);
			return buildIcon;
		}
		return null;
	}
	@Override
	protected Transferable createTransferable(JComponent c) {
		BuildTransferable tr;
		Point pt = c.getMousePosition();
		if(c instanceof JList){
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
			tr = new BuildTransferable(c,(Build)model.getElementAt(index));
		}else if(c instanceof LargeBuildPanel){
			tr = new BuildTransferable(c, bot.createBuild());
		}else{
			return null;
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
			return image == null ? image = BuildIcon.toImage(bot.getAssetManager(),build) : image;
		}
		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if(flavor.equals(DataFlavor.stringFlavor)){
				return Mobafire.toURL(build.getRune());
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
