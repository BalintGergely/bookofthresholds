package net.balintgergely.runebook;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Comparator;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import net.balintgergely.runebook.AssetManager.RoleIcon;
import net.balintgergely.runebook.LCUSummonerManager.Summoner;
import net.balintgergely.sutil.BackgroundPanel;
import net.balintgergely.sutil.BareIconButton;

public class TraderAssistant extends BookOfThresholds.Module{
	private static final long serialVersionUID = 1L;
	private LCUSummonerManager tableModel;
	TraderAssistant(BookOfThresholds main) {
		super(main);
		AssetManager assets = main.getAssetManager();
		super.setTitle(assets.z.getString("tradeAssistWindow"));
		super.setIconImage(assets.image(1, 3));
	}
	@Override
	public void init(){
		AssetManager assets = main.getAssetManager();
		MasteryIcon masteryIcon = new MasteryIcon(assets.icons);
		RoleIcon roleIcon = assets.new RoleIcon(AssetManager.SUPPORT);
		BareIconButton renderer = new BareIconButton(null, Color.WHITE);
		renderer.setIconTextGap(0);
		renderer.setHoverColor(new Color(0x50000000,true));
		tableModel = main.getLCUModule(LCUSummonerManager.class);
		TableRowSorter<LCUSummonerManager> tableRowSorter = new TableRowSorter<>(tableModel);
		tableRowSorter.setSortsOnUpdates(true);
		JTable table = new JTable(tableModel);
		table.setRowSorter(tableRowSorter);
		table.setBorder(null);
		table.setOpaque(false);
		table.getTableHeader().setReorderingAllowed(false);
		table.setRowHeight(24);
		table.setAutoCreateRowSorter(false);
		Color allyColor = new Color(0x92A0FF),enemyColor = new Color(0xFF4E50);
		TableCellRenderer tableCellRenderer = (JTable t, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) -> {
				Icon icon;
				String text;
				boolean armed;
				Color color;
				if(row < 0){
					color = Color.WHITE;
					armed = false;
					if(column < 2){
						icon = null;
						text = (String)value;
					}else{
						Summoner sm = tableModel.getSummoner(column-2);
						if(sm != null){
							roleIcon.setRoles(sm.getPosition());
							icon = roleIcon;
							text = sm.toString();
							roleIcon.setColor(sm.isEnemy() ? enemyColor : allyColor);
						}else{
							roleIcon.setRoles((byte)0);
							icon = roleIcon;
							text = null;
							roleIcon.setColor(color);
						}
					}
				}else{
					switch(column){
					case 0:
						armed = true;
						icon = (Icon)value;text = ((Champion)value).getName();
						color = Color.WHITE;
						break;
					case 1:
						if(value instanceof Summoner){
							armed = false;
							roleIcon.setRoles(((Summoner)value).getPosition());
							icon = roleIcon;
							text = value.toString();
							color = ((Summoner)value).isEnemy() ? enemyColor : allyColor;
							roleIcon.setColor(color);
							break;
						}//$FALL-THROUGH$
					default:
						color = Color.WHITE;
						armed = false;
						if(value != null){
							masteryIcon.setStatusToDisplay(((Number)value).intValue());
							icon = masteryIcon;
						}else{
							icon = null;
						}
						text = null;
					}
				}
				renderer.setForeground(color);
				renderer.setIcon(icon);
				renderer.setText(text);
				renderer.getModel().setArmed(armed);
				//renderer.getModel().setRollover(isSelected);
				return renderer;
		};
		tableRowSorter.setComparator(0, Comparator.naturalOrder());
		table.setDefaultRenderer(Object.class, tableCellRenderer);
		int maximumWidth = 0;
		for(Champion champion : assets.championList){
			int width = tableCellRenderer.getTableCellRendererComponent(null, champion, false, false, 0, 0).getPreferredSize().width;
			if(width > maximumWidth){
				maximumWidth = width;
			}
		}
		tableRowSorter.setComparator(1, LCUSummonerManager.SUMMONER_PRIORITY);
		TableColumnModel columnModel = table.getColumnModel();
		DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
		headerRenderer.setOpaque(false);
		headerRenderer.setForeground(Color.WHITE);
		columnModel.getColumn(0).setPreferredWidth(maximumWidth);
		columnModel.getColumn(0).setHeaderValue(assets.z.getString("champion"));
		columnModel.getColumn(1).setPreferredWidth(maximumWidth);
		columnModel.getColumn(1).setHeaderValue(assets.z.getString("summoner"));
		for(int i = 2;i < tableModel.getColumnCount();i++){
			tableRowSorter.setComparator(i, Comparator.reverseOrder());
			columnModel.getColumn(i).setHeaderRenderer(tableCellRenderer);
			columnModel.getColumn(i).setPreferredWidth(72);
		}
		table.getTableHeader().setBackground(Color.DARK_GRAY);
		table.getTableHeader().setDefaultRenderer(tableCellRenderer);
		table.setAutoCreateColumnsFromModel(false);
		tableModel.addChangeListener((ChangeEvent e) -> {
			table.getTableHeader().repaint();
		});
		table.setShowGrid(false);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setIntercellSpacing(new Dimension(0, 0));
		JScrollPane sp = new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setBorder(null);
		sp.setOpaque(false);
		sp.getViewport().setOpaque(false);
		BackgroundPanel bck = new BackgroundPanel(assets.petricite);
		bck.setLayout(new BorderLayout());
		bck.add(sp);
		super.add(bck);
		super.pack();
		super.setSize(super.getWidth()+200,super.getHeight());
		super.setLocationRelativeTo(null);
		super.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	}
	@Override
	void stateChanged(ChangeEvent e){
		super.stateChanged(e);
		tableModel.setDetailed(callerModel.isEnabled());
	}
}
