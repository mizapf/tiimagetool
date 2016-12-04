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
import java.awt.Cursor;
import java.io.EOFException;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.basic.BasicLine;


public class ListAction extends Activity {

	public String getMenuName() {
		return "List BASIC program";
	}
	
	public String getActionName() {
		return "LISTBASIC";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		DirectoryPanel dp = dvCurrent.getPanel();

		String sText = "no content or no BASIC file";			
		Volume vol = dvCurrent.getVolume();
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		String escape = imagetool.getPropertyString(TIImageTool.ESCAPE);

		for (Element selected : dvCurrent.getSelectedEntries()) {
			if (selected instanceof TFile) {
				try {
					if (((TFile)selected).isBasicFile()) {
						sText = ((TFile)selected).listBasic((imagetool.getPropertyBoolean(TIImageTool.BASICVER)==true)? BasicLine.EX_BASIC : BasicLine.TI_BASIC, escape);
					}
					
					imagetool.showTextContent(selected.getName(), sText);  
				}
				catch (EOFException eofx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Error: " + eofx.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE); 					
				}
				catch (IOException iox) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), iox.getClass().getName(), "Error opening file", JOptionPane.ERROR_MESSAGE); 
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Image error: " + ix.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE); 
				}
				catch (FormatException fx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), fx.toString(), "Read error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
