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

import de.mizapf.timt.util.ImageCheck;

public class OpenImageAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_O;
	}

	public String getMenuName() {
		return "Open...";
	}
	
	public String getActionName() {
		return "OPENIMAGE";
	}
		
	/** Used by the command line. */
	public void openLine(java.io.File file) {
		java.io.File[] files = new java.io.File[1];
		files[0] = file;
		open(files);
	}

	protected void open(java.io.File[] selectedfiles) {
		
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		for (java.io.File imagefile : selectedfiles) {
			String sAbsFile = null;
			try {
				sAbsFile = imagefile.getAbsolutePath();
				Volume vol = null;

				// ============== Open the image
				
				// Do we have a CF7 image?
				ImageFormat image = ImageFormat.getImageFormat(sAbsFile);
				if (image instanceof CF7ImageFormat) {
					// Find out how many volumes we have
					
					CF7ImageFormat cf7format = (CF7ImageFormat)image;
					String[] volumes = cf7format.getVolumes();

					// Ask the user which volume to open
					CF7VolumeSelection select = new CF7VolumeSelection(m_parent);
					select.setContent(volumes);
					select.createGui();
					select.setVisible(true);

					if (select.confirmed()) {
						int number = 0;
						try {
							number = select.getNumber();
							// We're doing some tricks with the name so that
							// the existing infrastructure gets along with it
							sAbsFile = sAbsFile + "#" + number;
						}
						catch (NumberFormatException nx) {
							JOptionPane.showMessageDialog(m_parent, "Invalid volume number", "Open error", JOptionPane.ERROR_MESSAGE);
							continue;
						}
						if (number < 0) {
							JOptionPane.showMessageDialog(m_parent, "Volume number cannot be negative", "Open error", JOptionPane.ERROR_MESSAGE);
							continue;
						}
					}
					else {
						// System.out.println("not confirmed");
						continue;
					}
				}

				try {
					vol = imagetool.getAlreadyOpenedVolume(sAbsFile);
					if (vol==null) vol = new Volume(sAbsFile);
					int[] geom = new int[5];
					if (vol.isCF7Image() && ImageCheck.checkCF7Inconsistency(vol, geom)==true) {
						JOptionPane.showMessageDialog(m_parent, "Inconsistent file system. You should run a file system check (Utilities).", "Open error", JOptionPane.WARNING_MESSAGE);
					}
				}
				catch (MissingHeaderException mx) {
					int doCheck1 = JOptionPane.showConfirmDialog(m_parent, 
						"Image file has floppy size, but no floppy signature (DSK). Open anyway?", "Warning", JOptionPane.YES_NO_OPTION);
					if (doCheck1 == JOptionPane.YES_OPTION) {
						vol = new Volume(sAbsFile, false);
					}
					// Be graceful, do as much as possible
					else continue;
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(m_parent, ix.getMessage(), "Image error", JOptionPane.ERROR_MESSAGE);
					continue;
				}

				Directory root = vol.getRootDirectory();	
				// ============== 
				
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
				
				// Add a tab and show the root directory.
				imagetool.addDirectoryView(root);
				
			}
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
		java.io.File[] selectedfiles = null;
		
		JFileChooser jfc = null;
		if (imagetool.getSourceDirectory("image")!=null) {
			jfc = new JFileChooser(imagetool.getSourceDirectory("image"));
		}
		else jfc = new JFileChooser();
		
		Dimension dim = imagetool.getPropertyDim(imagetool.FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);
		ImageFileFilter im = new ImageFileFilter();
		jfc.addChoosableFileFilter(im);
		jfc.setFileFilter(im);
		jfc.setMultiSelectionEnabled(true);
		
		int nReturn = jfc.showOpenDialog(m_parent);

		if (nReturn == JFileChooser.APPROVE_OPTION) {
			selectedfiles = jfc.getSelectedFiles();
			java.io.File filePar = selectedfiles[0].getParentFile();
			if (!filePar.getName().equals(".")) imagetool.setSourceDirectory(filePar, "image");  // sets the property only for non-UNC paths
			imagetool.setProperty(imagetool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
		}
		else return;
		
		open(selectedfiles);
	}
}
