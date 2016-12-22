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
import java.io.IOException;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.*;
import java.awt.Cursor;
import java.io.EOFException;
import javax.swing.JOptionPane;
import de.mizapf.timt.TIImageTool;

public class ViewDumpAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("ViewPlain");
	}
	
	public String getActionName() {
		return "VIEWDUMP";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		Volume vol = dvCurrent.getVolume();

		String sText = TIImageTool.langstr("NoContent");
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		for (Element selected : dvCurrent.getSelectedEntries()) {
			if (selected instanceof TFile) {
				String sRet = null;
				try {
					sRet = JOptionPane.showInputDialog(dvCurrent.getFrame(), TIImageTool.langstr("ViewDumpPrompt"), "0000");
					if (sRet != null) {
						int start = Integer.parseInt(sRet, 16);	
						byte[] content = ((TFile)selected).getRawContent();
						String dump = Utilities.hexdump(start, 0, content, content.length, false, 0x00);
						imagetool.showTextContent(String.format(TIImageTool.langstr("ViewDumpContents"), selected.getName()), dump);
					}
				}
				catch (EOFException eofx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("Error") + ": " + eofx.getMessage(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 					
				}
				catch (IOException iox) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
				}
				catch (NumberFormatException nx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("ViewDumpInvalidAddress"), sRet), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE); 
				}
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
