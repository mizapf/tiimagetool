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

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import javax.swing.JOptionPane;
import java.io.IOException;
import de.mizapf.timt.files.*;

public class RenameAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_F2;
	}

	public int getModifier() {
		return 0;
	}
	
	public String getMenuName() {
		return "Rename...";
	}
	
	public String getActionName() {
		return "RENAME";
	}
	
	public void go() {

		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		Volume volCurrent = dvCurrent.getVolume();

	//	m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		imagetool.clearClipboard();
		boolean bDone = false;
		for (Element selected : dvCurrent.getSelectedEntries()) {
			boolean bValid = false;
			while (!bValid && !bDone) {
				String sRet = JOptionPane.showInputDialog(dvCurrent.getFrame(), "Provide a new name: ", selected.getName());
				if (sRet != null) {
					try {
						bValid = true;
						dirCurrent.renameElement(selected, sRet);
					}
					catch (FileExistsException fxx) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Name already exists" , "Renaming error", JOptionPane.ERROR_MESSAGE); 
						bValid = false;
					}
					catch (InvalidNameException inx) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Invalid name" , "Renaming error", JOptionPane.ERROR_MESSAGE); 						
						bValid = false;
					}
					catch (ImageException ix) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Image error: " + ix.getMessage() , "Renaming error", JOptionPane.ERROR_MESSAGE); 						
					}
					catch (IOException iox) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Write error" , "Renaming error", JOptionPane.ERROR_MESSAGE); 						
					}
					catch (ProtectedException px) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Cannot write: " + px.getMessage(), "Write error", JOptionPane.ERROR_MESSAGE); 
					}
				}
				else bDone = true;
			}
			if (bDone) break;
		}
		imagetool.refreshPanel(volCurrent);
	//	m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
