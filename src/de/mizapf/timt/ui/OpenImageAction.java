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
			try {
				// ============== Open the image			
				ImageFormat image = ImageFormat.getImageFormat(sAbsFile); // throws ImageExc if unknown
				
				byte[] vibmap = null;
				TFileSystem fs = null;	
				
				if (image instanceof FloppyImageFormat) {
					vibmap = image.readSector(0).getData();
					
					int check = FloppyFileSystem.checkFormat(vibmap);
					
					System.out.println("Format check: " + TFileSystem.getFormatCheckText(check));
					if (check != TFileSystem.GOOD) {
						int doCheckSig = JOptionPane.YES_OPTION;
						if ((check & TFileSystem.NO_SIG)!=0) {
							doCheckSig = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("OpenImageNoDSK") + ". " + TIImageTool.langstr("Ask.openanyway"),
								TIImageTool.langstr("Warning"), JOptionPane.YES_NO_OPTION);
						}
						if ((check & TFileSystem.BAD_GEOMETRY)!=0) {
							doCheckSig = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("Format.badgeometry") + ". " + TIImageTool.langstr("Ask.openanyway"), 
								TIImageTool.langstr("Warning"), JOptionPane.YES_NO_OPTION);
						}
						if (doCheckSig == JOptionPane.NO_OPTION) {
							continue;
						}
					}						
					fs = ((FloppyImageFormat)image).getFileSystem(vibmap);
					((FloppyFileSystem)fs).configure(vibmap);
					((FloppyFileSystem)fs).setupAllocationMap(vibmap);
				}
				
				if (image instanceof HarddiskImageFormat) {
					HarddiskImageFormat hif = (HarddiskImageFormat)image;
					vibmap = image.readSector(0).getData();						
					int check = HarddiskFileSystem.checkFormat(vibmap);
					
					System.out.println("Format check: " + TFileSystem.getFormatCheckText(check));
					if (check != TFileSystem.GOOD) {
						// When we have partitions, open the partition selection
						if ((check & TFileSystem.PARTITIONED)!=0) {
							hif.setupPartitionTable();
							PartitionSelectionDialog psd = new PartitionSelectionDialog(m_parent, hif.getPartitionTable());
							psd.createGui(imagetool.boldFont);
							psd.setVisible(true);
							if (psd.confirmed()) {
								int selPart = psd.getSelectedNumber()-1;
								hif.setPartition(selPart);
								// Do not open the partition if another partition of 
								// the same image is open! This needs further investigation,
								// especially for CHD (see sector cache)
								
								// Sectors are relative inside partitions, i.e. sector 0
								// in partition 1 is in another format unit than sector 0
								// in partition 2.
								
								// Although the sectors in the partitions do not overlap,
								// the hunk map is shared. When two partitions have
								// updates that create new hunks, this may lead to a race condition.
								
								// This means that the same sector cache must be used
								
								// However, this in turn means that saving the cache would
								// commit the changes of all open partitions: Two tabs
								// opened with two different partitions, both modified;
								// doing a Save would require all changes of both partitions
								// to be written.
								if (imagetool.hasAlreadyOpenedVolume(sAbsFile)) {
									Volume vol1 = imagetool.getAlreadyOpenedVolume(sAbsFile);
									if (vol1.getPartitionNumber() != selPart) {
										JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("Image.OtherPartitionOpen"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
										continue;
									}
								}
								// Have to read the VIB from the selected partition
								vibmap = image.readSector(0).getData();
							}
							else continue;
						}
						int doCheckSig = JOptionPane.YES_OPTION;
						if ((check & TFileSystem.BAD_GEOMETRY)!=0) {
							doCheckSig = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("Format.badgeometry") + ". " + TIImageTool.langstr("Ask.openanyway"), 
								TIImageTool.langstr("Warning"), JOptionPane.YES_NO_OPTION);
						}
						if ((check & TFileSystem.BAD_AUCOUNT)!=0) {
							doCheckSig = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("Format.badaucount") + ". " + TIImageTool.langstr("Ask.openanyway"), 
								TIImageTool.langstr("Warning"), JOptionPane.YES_NO_OPTION);
						}
						if (doCheckSig == JOptionPane.NO_OPTION) {
							continue;
						}
					}
					
					fs = hif.getFileSystem(vibmap);
					((HarddiskFileSystem)fs).setupAllocationMap(image.getContent(0, 31));
				}
				
				if (imagetool.hasAlreadyOpenedVolume(sAbsFile)) {
					System.out.println("Already open");
					vol = imagetool.getAlreadyOpenedVolume(sAbsFile);
					imagetool.addDirectoryView(vol.getRootDirectory());
				}
				else {
					try {
						vol = new Volume(image, fs);					
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
			}
			catch (ImageException ix) {
				ix.printStackTrace();
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
			}
			catch (EOFException eofx) {
				// TODO: Close open image
				eofx.printStackTrace();
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("OpenImageDefect") + ": " + eofx.getMessage(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
			}
			catch (FileNotFoundException fnfx) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("FileNotFound") + ": "  + fnfx.getMessage(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE); 
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName() + " (" + iox.getMessage() + ")", TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
				iox.printStackTrace();
			}
			catch (NumberFormatException nfx) {
				JOptionPane.showMessageDialog(m_parent, String.format(TIImageTool.langstr("ParseError"), nfx.getMessage()), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
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
