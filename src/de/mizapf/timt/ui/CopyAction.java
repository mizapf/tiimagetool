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
import de.mizapf.timt.TIImageTool;

public class CopyAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_C;
	}

	public String getMenuName() {
		return TIImageTool.langstr("Copy");
	}
	
	public String getActionName() {
		return "COPY";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

//		System.out.println("Copy action");
//		imagetool.setClipboard(new DDData(dvCurrent.getSelectedEntries(), dvCurrent));
		Action copy = TransferHandler.getCopyAction();
		copy.actionPerformed(new ActionEvent(dvCurrent.getPanel().getLinkToJList(), ActionEvent.ACTION_PERFORMED, null));

		imagetool.markForCut(false);
		imagetool.setClipboardLoaded(true);
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
