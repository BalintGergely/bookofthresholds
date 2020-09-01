package net.balintgergely.runebook;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JButton;

public class BareIconButton extends JButton{
	private static final long serialVersionUID = 1L;
	public BareIconButton(Icon icon){
		super(icon);
		super.setRolloverEnabled(true);
		super.setForeground(Color.WHITE);
		super.setOpaque(false);
	}
	@Override
	public void paint(Graphics gr){
		int width = getWidth(),height = getHeight();
		boolean postFill = false;
		if(model.isArmed()){
			gr.setColor(new Color(0x50404040,true));
			gr.fillRect(0, 0, width, height);
		}else if(!(postFill = (model.isPressed() || model.isRollover())) && isFocusOwner()){
			gr.setColor(new Color(0x50C0C0C0,true));
			gr.fillRect(0, 0, width, height);
		}
		Icon ic = super.getIcon();
		if(ic != null){
			ic.paintIcon(this, gr, 0, 0);
		}
		if(postFill){
			gr.setColor(new Color(0x50C0C0C0,true));
			gr.fillRect(0, 0, width, height);
		}
		if(model.isSelected()){
			gr.setColor(getForeground());
			gr.drawRect(0, 0, width-1, height-1);
		}
	}
}
