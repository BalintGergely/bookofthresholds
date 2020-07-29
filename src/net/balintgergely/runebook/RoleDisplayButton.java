package net.balintgergely.runebook;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JToggleButton;

public class RoleDisplayButton extends JToggleButton{
	private static final long serialVersionUID = 1L;
	private byte roleToDisplay;
	private AssetManager assetManager;
	public RoleDisplayButton(AssetManager assetManager,byte roleToDisplay){
		this.assetManager = assetManager;
		super.setRolloverEnabled(true);
		super.setForeground(Color.WHITE);
		super.setOpaque(false);
		Dimension size = new Dimension(24, 24);
		super.setMinimumSize(size);
		super.setMaximumSize(size);
		super.setPreferredSize(size);
		this.roleToDisplay = roleToDisplay;
	}
	@Override
	public void paint(Graphics gr){
		Color backgroundColor = null;
		if(model.isArmed()){
			backgroundColor = new Color(0x50404040,true);
		}else if(model.isPressed() || model.isRollover() || isFocusOwner()){
			backgroundColor = new Color(0x50C0C0C0,true);
		}
		if(backgroundColor != null){
			gr.setColor(backgroundColor);
			gr.fillRect(0, 0, 24, 24);
		}
		if(model.isSelected()){
			gr.setColor(getForeground());
			gr.drawRect(0, 0, 23, 23);
		}
		assetManager.paintRoleIcon(gr, roleToDisplay, 0, 0);
	}
}
