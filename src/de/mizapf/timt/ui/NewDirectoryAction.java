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
import java.awt.font.*;
import java.awt.Graphics2D;
import java.awt.FontMetrics;

import java.awt.Cursor;
import java.awt.Dimension;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class NewDirectoryAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("NewDirectory") + "...";
	}
	
	public String getActionName() {
		return "NEWDIR";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		DirectoryPanel dp = dvCurrent.getPanel();

		Volume vol = dvCurrent.getVolume();
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		NewElementDialog dirdia = new NewElementDialog(dvCurrent.getFrame(), TIImageTool.langstr("NewDirectoryPrompt"), "NEWDIR", null);
		dirdia.createGui();
		dirdia.setVisible(true);
		boolean ok = true;

		if (dirdia.confirmed()) {	
			String sName = dirdia.getElementName();
			if (vol.isCF7Volume()) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("CF7NoDirectory"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
				ok = false;
			}
			else {
				if (vol.isFloppyImage()) {
					if (!dirCurrent.isRootDirectory()) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("NewDirectoryOnlyRoot"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
						ok = false;
					}
					if (vol.getRootDirectory().getDirectories().length>2) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("FloppyDirectoryOnly3"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
						ok = false;
					}
				}
			}
			
			if (ok && !Directory.validName(sName)) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("InvalidDirectoryName"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
				ok = false;
			}

			if (ok) {
				try {
					dirCurrent.createSubdirectory(sName);
					vol.nextGeneration(true);
				}
				catch (ImageFullException ifx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("NoSpaceLeft"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
				}
				catch (FileExistsException fx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ElementExists"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);					
				}
				catch (Exception ex) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + ex.getClass().getName(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
				imagetool.refreshPanel(vol);			
			}
		}
		
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
