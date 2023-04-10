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

import javax.swing.JOptionPane;
import java.io.IOException;
import de.mizapf.timt.files.*;
import java.awt.Cursor;
import java.io.EOFException;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;

public class ViewTextAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("ViewText");
	}
	
	public String getActionName() {
		return "VIEWTEXT";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		Volume vol = dvCurrent.getVolume();

		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		// Caution: Only open text files as text 
		for (Element selected : dvCurrent.getSelectedEntries()) {
			if (selected instanceof TFile) {
				try {
					byte[] content = ((TFile)selected).getRecordContent();
					boolean showit = Utilities.checkForText(content, content.length);
					if (!showit) {
						int nRet = JOptionPane.showConfirmDialog(dvCurrent.getFrame(), TIImageTool.langstr("LotUnprint"), TIImageTool.langstr("Attention"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						showit = (nRet == JOptionPane.YES_OPTION); 
					}					
					if (showit) {
						String sContent = Utilities.sanitizeBytes(content, imagetool.getPropertyString(imagetool.ESCAPE), imagetool.getPropertyBoolean(TIImageTool.VERBOSE));
						imagetool.showTextContent(selected.getName(), sContent);  
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
				catch (FormatException fx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("Error") + ": " + fx.toString(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
				}
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
