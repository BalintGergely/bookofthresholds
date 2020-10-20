package net.balintgergely.sutil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import net.balintgergely.runebook.Champion;

public class BareIconButton extends JButton implements ListCellRenderer<Champion>{
	private static final Color GLASS_PANE_COLOR = new Color(0x50C0C0C0,true);
	private static final long serialVersionUID = 1L;
	public BareIconButton(Icon ic,Color foreground){
		super(ic);
		super.setRolloverEnabled(true);
		super.setForeground(foreground);
		super.setOpaque(false);
		super.setFont(new Font("Tahoma", Font.BOLD, 12));
	}
	private Color hoverColor = GLASS_PANE_COLOR;
	public void setHoverColor(Color hc){
		this.hoverColor = hc;
	}
	public Color getHoverColor(){
		return hoverColor;
	}
	@Override
	public void setFont(Font font){
		//Something, SOMETHING In Java Combo Box UI just grabs the cell renderer and tries to set the font. A cunning but futile attempt.
		//Oh btw it also sets the background. Got to keep that in mind.
	}
	@Override
	public Dimension getPreferredSize(){
		String text = getText();
		Icon icon = getIcon();
		if(text == null || text.isBlank()){
			if(icon == null){
				return new Dimension();
			}else{
				return new Dimension(icon.getIconWidth(), icon.getIconHeight());
			}
		}else{
			FontMetrics metrics = getFontMetrics(getFont());
			if(icon == null){
				return new Dimension(metrics.stringWidth(text),metrics.getHeight());
			}else{
				return new Dimension(metrics.stringWidth(text)+getIconTextGap()+icon.getIconWidth(),Math.max(metrics.getHeight(), icon.getIconHeight()));
			}
		}
	}
	@Override
	public void paintComponent(Graphics gr){
		((Graphics2D)gr).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int width = getWidth(),height = getHeight();
		boolean postFill = false;
		if(model.isArmed()){
			gr.setColor(hoverColor);
			gr.fillRect(0, 0, width, height);
		}else if(!(postFill = (model.isPressed() || model.isRollover())) && isFocusOwner()){
			gr.setColor(hoverColor);
			gr.fillRect(0, 0, width, height);
		}
		Icon ic = super.getIcon();
		int offset = 0;
		if(ic != null){
			offset = ic.getIconWidth()+getIconTextGap();
			ic.paintIcon(this, gr, 0, 0);
		}
		if(postFill){
			gr.setColor(hoverColor);
			gr.fillRect(0, 0, width, height);
		}
		gr.setColor(getForeground());
		if(model.isSelected()){
			gr.drawRect(0, 0, width-1, height-1);
		}
		String text = getText();
		if(text != null){
			FontMetrics fm = gr.getFontMetrics();
			gr.drawString(text, offset, (height+fm.getHeight())/2-fm.getDescent());
		}
	}
	@Override
	public Component getListCellRendererComponent(
			JList<? extends Champion> list,
			Champion champion,
			int index,
			boolean isSelected,
			boolean cellHasFocus){
				if(list != null){
					list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
					list.setVisibleRowCount(0);
				}
				setIcon(champion);
				setText(index >= 0 || champion == null? "" : champion.getName());
				isSelected &= index >= 0;
				getModel().setPressed(isSelected);
				return this;
			}
}