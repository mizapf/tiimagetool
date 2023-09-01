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
    
    Copyright 2016 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import de.mizapf.timt.files.*;
import javax.swing.*;
import java.io.*;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.Cursor;
import java.util.List;
import de.mizapf.timt.TIImageTool;

public class ToggleEmulateAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("ToggleEmulate");
	}
	
	public String getActionName() {
		return "EMULATE";
	}
		
	// Used from the Menu
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));	
		Element elFirst = dvCurrent.getClickedElement();
		try {
			if (elFirst instanceof TFile) {
				Volume vol = dvCurrent.getVolume();
//				vol.reopenForWrite();		
				vol.toggleEmulateFlag(((TFile)elFirst).getFIBLocation());
//				vol.reopenForRead();
			}
			else {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ToggleEmulateBug") + ": " + elFirst.getName(), TIImageTool.langstr("InternalError"), JOptionPane.ERROR_MESSAGE); 		
			}
			dvCurrent.refreshAll();
		}
		catch (ProtectedException px) {
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
		}
		catch (ImageException ix) {
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
			ix.printStackTrace();
		}
		
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
