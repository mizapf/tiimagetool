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

public class OpenOneImageAction extends Activity {

	private String m_fileName;
	private int m_number;
	
	public OpenOneImageAction(String file, int number) {
		m_fileName = file;
		m_number = number;
	}
	
	public String getMenuName() {
		return m_fileName;
	}
	
	public String getActionName() {
		return "OPENIMAGE" + m_number;
	}
		
	private void open(java.io.File[] selectedfiles) {
		
		// At this point we have a selected file.		
				
//		imagetool.setInsertMenuEnabled(false);
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		for (java.io.File imagefile : selectedfiles) {
			String sAbsFile = null;
			try {
				sAbsFile = imagefile.getAbsolutePath();
				Volume vol = null;
				try {
					vol = imagetool.getAlreadyOpenedVolume(sAbsFile);
					if (vol==null) vol = new Volume(sAbsFile);
				}
				catch (ImageException ix) {
					if (ix.getMessage().equals(".HEAD")) {
						int doCheck1 = JOptionPane.showConfirmDialog(m_parent, 
							"Image file has floppy size, but missing floppy signature (DSK). Open anyway?", "Warning", JOptionPane.YES_NO_OPTION);
						if (doCheck1 == JOptionPane.YES_OPTION) {
							vol = new Volume(sAbsFile, false);
						}
						else continue;
					}
					else {
						JOptionPane.showMessageDialog(m_parent, ix.getMessage(), "Image error", JOptionPane.ERROR_MESSAGE);
						continue;
					}
				}
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
/*			catch (FileExistsException fxx) {
				JOptionPane.showMessageDialog(m_parent, "Cannot open an image more than once.", "Illegal operation", JOptionPane.ERROR_MESSAGE);				
			} */
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(m_parent, ix.getMessage(), "Image error", JOptionPane.ERROR_MESSAGE);
			}
			catch (EOFException eofx) {
				// TODO: Close open image
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

		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	// Used from the Menu
	public void go() {
		java.io.File[] selectedfiles = new java.io.File[1];
		selectedfiles[0] = new java.io.File(m_fileName);		
		open(selectedfiles);
	}
}
