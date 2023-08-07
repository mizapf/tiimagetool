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
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.NotImplementedException;
import de.mizapf.timt.util.Utilities;

public class OpenImageAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_O;
	}

	public String getMenuName() {
		return TIImageTool.langstr("OpenImage") + "...";
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
			sAbsFile = imagefile.getAbsolutePath();
			Volume vol = null;
			// Do we already have that image?
			if (imagetool.hasAlreadyOpenedVolume(sAbsFile)) {
				vol = imagetool.getAlreadyOpenedVolume(sAbsFile);
				imagetool.addDirectoryView(vol.getRootDirectory());
			}
			else {
				
				try {
					
					// ============== Open the image			
					ImageFormat image = ImageFormat.determineImageFormat(sAbsFile); // throws ImageExc if unknown
							
					if (image instanceof FloppyImageFormat) {
						int check = ((FloppyImageFormat)image).getFormatCheck();
						System.out.println("Format check: " + FloppyFileSystem.getFormatCheckText(check));
						if ((check & TFileSystem.NO_SIG)!=0) {
							int doCheckSig = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("OpenImageNoDSK"), TIImageTool.langstr("Warning"), JOptionPane.YES_NO_OPTION);
							if (doCheckSig == JOptionPane.NO_OPTION) {
								continue;
							}
						}
					}
					
					// Do we have a partitioned image?
					if (image instanceof PartitionedStorage) {
						if (((PartitionedStorage)image).partitionCount()>0)
							throw new NotImplementedException("PartitionedStorage");
						// Check CF7 inconsistency
					}
					
					try {
						vol = new Volume(image);					
					}
					catch (MissingHeaderException mx) {
						// FIXME: Not used
						// No DSK signature
						int doCheck1 = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("OpenImageNoDSK"), TIImageTool.langstr("Warning"), JOptionPane.YES_NO_OPTION);
						if (doCheck1 == JOptionPane.YES_OPTION) {
							vol = new Volume(image);
						}
						// Be graceful, do as much as possible
						else continue;
					}
					catch (ImageException ix) {
						JOptionPane.showMessageDialog(m_parent, ix.getMessage(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
						continue;
					}
					
					if (vol.isReadOnly()) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImageFWP"), TIImageTool.langstr("Warning"), JOptionPane.WARNING_MESSAGE);
					}
					
					Directory root = vol.getRootDirectory();	
					// ============== 
					
					// Check for MaxAU bug in SCSI image and set the available menu options
					
					// Add a tab and show the root directory.
					imagetool.addDirectoryView(root);
					image.setCheckpoint();
					vol.nextGeneration();
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
				}
				catch (EOFException eofx) {
					// TODO: Close open image
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("OpenImageDefect") + ": " + eofx.getMessage(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
				}
				catch (FileNotFoundException fnfx) {
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("FileNotFound") + ": "  + fnfx.getMessage(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE); 
				}
				catch (IOException iox) {
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName() + " (" + iox.getMessage() + ")", TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
					iox.printStackTrace();
				}				
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
		
		Dimension dim = settings.getPropertyDim(imagetool.FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);
		ImageFileFilter im = new ImageFileFilter();
		jfc.addChoosableFileFilter(im);
		jfc.setFileFilter(im);
		jfc.setMultiSelectionEnabled(true);
		
		int nReturn = jfc.showOpenDialog(m_parent);

		if (nReturn == JFileChooser.APPROVE_OPTION) {
			selectedfiles = jfc.getSelectedFiles();
			java.io.File filePar = selectedfiles[0].getParentFile();
			imagetool.setSourceDirectory(filePar, "image");
			settings.put(imagetool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
		}
		else return;
		
		open(selectedfiles);
	}
}
