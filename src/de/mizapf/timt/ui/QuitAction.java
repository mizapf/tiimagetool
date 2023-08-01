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
import java.awt.event.KeyEvent;
import de.mizapf.timt.TIImageTool;

public class QuitAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_Q;
	}
	
	public String getMenuName() {
		return TIImageTool.langstr("Exit");
	}
	
	public String getActionName() {
		return "QUIT";
	}
	
	public void go() {
		if (imagetool.hasUnsavedChanges()) {
			int nRet = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("UnsavedChanges") + ". " + TIImageTool.langstr("ReallyQuit"), TIImageTool.langstr("Leaving"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (nRet == JOptionPane.NO_OPTION) return;
		}
		m_parent.dispose();
		imagetool.terminate();
	}
}
