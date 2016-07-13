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

import de.mizapf.timt.files.*;
import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ArrayList;
import javax.swing.Action;
import javax.swing.TransferHandler;
import java.awt.event.ActionEvent;

public class CutAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_X;
	}
	
	public String getMenuName() {
		return "Cut";
	}
	
	public String getActionName() {
		return "CUT";
	}
	
	public void go() {
//		System.out.println("Cut action");
		DirectoryView dvCurrent = imagetool.getSelectedView();
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

//		imagetool.setClipboard(new DDData(dvCurrent.getSelectedEntries(), dvCurrent));
		Action cut = TransferHandler.getCutAction();
		cut.actionPerformed(new ActionEvent(dvCurrent.getPanel().getLinkToJList(), ActionEvent.ACTION_PERFORMED, null));		

		imagetool.markForCut(true);		
		imagetool.setClipboardLoaded(true);
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
