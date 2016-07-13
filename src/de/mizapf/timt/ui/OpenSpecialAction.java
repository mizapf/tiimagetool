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
import javax.swing.*;
import java.io.*;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.Cursor;

/**
	Open special is intended to open a device like a Compact Flash card and
	to directly operate on it. Under Linux, this works quite well, at least
	if you are privileged to access the device file /dev/sdX, where X is some
	number. 
	
	Under Windows, things seem to be more complicated, and maybe unfeasible in
	Java. Despite some discussions like
	
	* http://stackoverflow.com/questions/2108313/how-to-access-specific-raw-data-on-disk-from-java
	* http://stackoverflow.com/questions/22161967/accessing-windows-disks-directly-with-java-nio

	it seems impossible to get writing access to the device, even when run
	as Administrator. Reading is possible, though.

	Solution:
	
	When the first write operation on this volume is performed, TIImageTool
	will pop up a dialog to inform that the change cannot be committed to the
	device but will be written to a copy of the device contents that will
	now be created.
	
	Alternatively: (preferred)
	
	The "open special" menu item will be replaced by a "Create image from device".
	This will yield a new *.raw file that can be opened as an image. Later,
	it can be written back by an external tool.
	
	Also, a new menu item "New -> CF7 image" will be available.
*/


public class OpenSpecialAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_F;
	}

	public String getMenuName() {
		return "Open device...";
	}
	
	public String getActionName() {
		return "OPENSPECIAL";
	}
		
	private void open(String sSelectedDevice) {
		
		// At this point we have a selected file.		
				
//		imagetool.setInsertMenuEnabled(false);
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		try {
			Volume vol = imagetool.getAlreadyOpenedVolume(sSelectedDevice);
			if (vol==null) vol = new Volume(sSelectedDevice);
			
			Directory root = vol.getRootDirectory();	
			
			// Check for MaxAU bug and set the available menu options
			if (vol.isSCSIImage()) {
				//					imagetool.setHDConvEnabled(true, false);
				int nChecked = Directory.checkDIB(root, false);
				if (nChecked < 0) {
					int doCheck = JOptionPane.showConfirmDialog(m_parent, 
						"This SCSI image shows the MaxAU bug. Shall this image be repaired?", "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					if (doCheck == JOptionPane.OK_OPTION) {
						nChecked = Directory.checkDIB(root, true);
						if (nChecked < 0)
						{
							JOptionPane.showMessageDialog(m_parent, 
								"Checked " + (-nChecked) + " directories and fixed MaxAu bug.", "Fix MaxAU", JOptionPane.INFORMATION_MESSAGE);
						}
						else {
							if (nChecked==0) 
							JOptionPane.showMessageDialog(m_parent, 
								"No directories found.", "Fix MaxAU", JOptionPane.INFORMATION_MESSAGE);
							else 
							JOptionPane.showMessageDialog(m_parent, 
								"Checked " + nChecked + " directories; no problems found.", "Fix MaxAU",JOptionPane.INFORMATION_MESSAGE);
						}
					}
					else {
						JOptionPane.showMessageDialog(m_parent, 
							"Before this image can be converted to HFDC it must be fixed.", "Warning", JOptionPane.INFORMATION_MESSAGE);							
						//							imagetool.setHDConvEnabled(false, false);
					}
				}
			}		
			imagetool.addDirectoryView(root);	
		}
		catch (FileExistsException fxx) {
			JOptionPane.showMessageDialog(m_parent, "Cannot open an image more than once.", "Illegal operation", JOptionPane.ERROR_MESSAGE);				
		}
		catch (ImageException ix) {
			JOptionPane.showMessageDialog(m_parent, ix.getMessage(), "Image error", JOptionPane.ERROR_MESSAGE); 			
		}
		catch (EOFException eofx) {
			JOptionPane.showMessageDialog(m_parent, "Defect or missing file system: " + eofx.getMessage(), "Error opening file", JOptionPane.ERROR_MESSAGE);
		}
		catch (FileNotFoundException fnfx) {
			JOptionPane.showMessageDialog(m_parent, "File not found: " + fnfx.getMessage(), "Error opening file", JOptionPane.ERROR_MESSAGE); 
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_parent, iox.getClass().getName() + " (" + iox.getMessage() + ")", "Error reading file", JOptionPane.ERROR_MESSAGE);
			iox.printStackTrace();
		}
		catch (ProtectedException px) {
			JOptionPane.showMessageDialog(m_parent, "Error during fix: " + px.getMessage(), "Write error", JOptionPane.ERROR_MESSAGE); 
		}

		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	// Used from the Menu
	public void go() {
		String sRet = JOptionPane.showInputDialog(m_parent, "Enter a file or drive specifier (like e:)");
		if (sRet != null) {
			if (sRet.endsWith(":")) sRet = "\\\\.\\" + sRet;
			open(sRet);
		}
	}
}
