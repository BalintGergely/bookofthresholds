package net.balintgergely.runebook;

import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.util.Enumeration;

import javax.swing.ButtonModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import net.balintgergely.runebook.LCUManager.LCUSummonerManager;

public class TraderAssistant extends JFrame{
	private static final long serialVersionUID = 1L;
	final ButtonModel callerModel;
	private final LCUSummonerManager tableModel;
	TraderAssistant(AssetManager assets,LCUManager client){
		MasteryIcon masteryIcon = new MasteryIcon(assets.iconSprites);
		JLabel rendererLabel = new JLabel();
		rendererLabel.setDoubleBuffered(false);
		rendererLabel.setBorder(null);
		//rendererLabel.setForeground(Color.WHITE);
		rendererLabel.setHorizontalAlignment(SwingConstants.LEFT);
		tableModel = client.championsManager;
		TableRowSorter<LCUSummonerManager> tableRowSorter = new TableRowSorter<>(tableModel);
		tableRowSorter.setSortsOnUpdates(true);
		JTable table = new JTable(tableModel);
		table.setRowSorter(tableRowSorter);
		table.setBorder(null);
		table.setOpaque(false);
		table.getTableHeader().setReorderingAllowed(false);
		table.setRowHeight(24);
		table.setAutoCreateRowSorter(false);
		TableCellRenderer rendererForChampions = (JTable t, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) -> {
					Champion champion = (Champion)value;
					rendererLabel.setText(champion.getName());
					rendererLabel.setIcon(champion);
					rendererLabel.setBorder(null);
					return rendererLabel;
		};
		table.setDefaultRenderer(Number.class, (JTable t, Object value, boolean isSelected, boolean hasFocus,
					int row, int column) -> {
					if(value == null){
						rendererLabel.setIcon(null);
					}else{
						masteryIcon.setStatusToDisplay(((Number)value).intValue());
						rendererLabel.setIcon(masteryIcon);
					}
					rendererLabel.setText(null);
					rendererLabel.setBorder(null);
					return rendererLabel;
		});
		table.setDefaultRenderer(Champion.class, rendererForChampions);
		int maximumWidth = 0;
		for(Champion champion : assets.championsOrdered){
			int width = rendererForChampions.getTableCellRendererComponent(null, champion, false, false, 0, 0).getPreferredSize().width;
			if(width > maximumWidth){
				maximumWidth = width;
			}
		}
		tableRowSorter.setComparator(0, tableModel);
		TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(maximumWidth);
		columnModel.getColumn(0).setHeaderValue(assets.z.getString("champion"));
		for(int i = 1;i < tableModel.getColumnCount();i++){
			tableRowSorter.setComparator(i, masteryIcon);
		}
		table.setAutoCreateColumnsFromModel(false);
		tableModel.addChangeListener((ChangeEvent e) -> {
			Enumeration<TableColumn> columns = columnModel.getColumns();
			while(columns.hasMoreElements()){
				TableColumn column = columns.nextElement();
				int index = column.getModelIndex();
				if(index > 0){
					column.setHeaderValue(tableModel.getColumnName(index));
				}
			}
			table.getTableHeader().repaint();
		});
		table.setShowGrid(false);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setIntercellSpacing(new Dimension(0, 0));
		JScrollPane sp = new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setBorder(null);
		sp.setOpaque(false);
		sp.getViewport().setOpaque(false);
		super.add(sp);
		super.pack();
		super.setLocationRelativeTo(null);
		super.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.callerModel = new JToggleButton.ToggleButtonModel();
		callerModel.addChangeListener(this::stateChanged);
	}
	private void stateChanged(ChangeEvent e){
		tableModel.setDetailed(callerModel.isEnabled());
		super.setVisible(callerModel.isSelected());
	}
	@Override
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			callerModel.setSelected(false);
		}
	}
}
