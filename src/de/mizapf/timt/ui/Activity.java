/****************************************************************************
    This file is part of TIImageTool.

    TIImageTool is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    TIImageTool is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with TIImageTool.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright 2011 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import java.awt.event.InputEvent;
import de.mizapf.timt.TIImageTool;

abstract public class Activity {
	
	protected JFrame m_parent;
	protected TIImageTool imagetool;
	
	protected final static String NAME_ABORT = ".abort";
	protected final static String NAME_SKIP = ".skip";
	
	public void setLinks(TIImageTool timt, JFrame parent) {
		m_parent = parent;
		imagetool = timt;
	}
	
	public abstract String getMenuName();
	public abstract String getActionName();
	
	public abstract void go();

	public int getKeyCode() {
		return 0;
	}

	public int getModifier() {
		return InputEvent.CTRL_DOWN_MASK;
	}
	
	/** Called from PasteAction. */
	protected String getAlternativeName(boolean bFile, String sName, JFrame frame) {
		NewNameDialog namedialog = new NewNameDialog(frame, bFile, sName);
		namedialog.createGui();
		namedialog.setVisible(true);
		if (namedialog.ok()) {
			return namedialog.getFileName();
		}
		if (namedialog.skipped()) return NAME_SKIP;
		return NAME_ABORT; 
	}
}
