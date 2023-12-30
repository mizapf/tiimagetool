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

import java.awt.Cursor;
import java.io.*;
import java.util.*;

import java.awt.Dimension;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.*;
import de.mizapf.timt.TIImageTool;

public class EditCF7Action extends Activity {
	
	RandomAccessFile m_file;
	byte[] m_volume;
	
	public String getMenuName() {
		return TIImageTool.langstr("Menu.CF7.Edit") + "...";
	}
	
	public String getActionName() {
		return "EDITCF";
	}
	
	public void open(File selectedFile) {
		boolean bOK = false;
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			String sAbsFile = null;
			sAbsFile = selectedFile.getAbsolutePath();
			// ============== Open the image	
			FileImageFormat image = (FileImageFormat)ImageFormat.getImageFormat(sAbsFile); // throws ImageExc if unknown
			if (image instanceof CF7ImageFormat) {
				
				if (imagetool.hasAlreadyOpenedVolume(sAbsFile)) {
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("Image.CF7.AlreadyOpen"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
				}				
				else {
					Partition[] part = ((CF7ImageFormat)image).getPartitionTable();
					int nLastPart = part[part.length-1].getNumber();
					
					m_file = new RandomAccessFile(sAbsFile, "rw");
					m_volume = new byte[800*1024];
					
					String[] partName = new String[nLastPart+1];
					
					int j = 0;
					for (int i=0; i < partName.length; i++) {
						if (i == part[j].getNumber()) {
							partName[i] = part[j].getName();
							j++;
						}
						else
							partName[i] = null;
					}
					
					EditCF7Dialog ecd = new EditCF7Dialog(m_parent, partName);
					
					ecd.setSettings(settings);
					ecd.createGui(imagetool.boldFont);
					ecd.setVisible(true);
					
					if (ecd.confirmed()) {
						String[] partNameNew = ecd.getPartitionNames();
						
						// Read the sectors of the partitions and check whether they
						// are formatted (and unformat them)
						int len = partNameNew.length;
						if (partName.length > len) len = partName.length;
						
						for (int i=0; i < len; i++) {
							if (i < partNameNew.length) {
								if (i < partName.length) {
									if (partNameNew[i].equals("---")) {
										if (partName[i] != null) {
											removeVolume(i);
										}
									}
									else {
										if (partName[i].equals("---")) {
											formatVolume(i, partNameNew[i]);
										}
										else {
											if (!partNameNew[i].equals(partName[i])) {
												renameVolume(i, partNameNew[i]);
											}
										}
									}
								}
								else
									formatVolume(i, partNameNew[i]);
							}
							else removeVolume(i);
						}						
						bOK = true;
					}
				}
			}			
			else {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("Image.CF7.WrongFormat"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
			}
		}
		catch (FileNotFoundException fnfx) {
			JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("FileNotFound") + ": "  + fnfx.getMessage(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE); 
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName() + " (" + iox.getMessage() + ")", TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
			iox.printStackTrace();
		}
		catch (ImageException ix) {
			ix.printStackTrace();
			JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
		}

		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		if (bOK) JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("Dialog.CF7.EditDone"), TIImageTool.langstr("Title.CF7.Edit"), JOptionPane.INFORMATION_MESSAGE);
	}
	
	public void go() {					
		File selectedfile = null;
		
		JFileChooser jfc = null;
		if (imagetool.getSourceDirectory("image")!=null) {
			jfc = new JFileChooser(imagetool.getSourceDirectory("image"));
		}
		else jfc = new JFileChooser();
		
		Dimension dim = settings.getPropertyDim(imagetool.FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);
		ImageFileFilter im = new ImageFileFilter(settings.getPropertyString(imagetool.IMGSUF));
		jfc.addChoosableFileFilter(im);
		jfc.setFileFilter(im);
		jfc.setMultiSelectionEnabled(false);
		
		int nReturn = jfc.showOpenDialog(m_parent);

		if (nReturn == JFileChooser.APPROVE_OPTION) {
			selectedfile = jfc.getSelectedFile();
			java.io.File filePar = selectedfile.getParentFile();
			imagetool.setSourceDirectory(filePar, "image");
			settings.put(imagetool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
		}
		else return;
		
		open(selectedfile);
	}
	
	private void formatVolume(int i, String sNewName) throws IOException {
		m_file.seek(i * m_volume.length);    // each volume is 800K in size (every second byte used)
		NewCF7ImageAction.setupVolume(i, m_volume, sNewName, false); 

		// Write the new volume
		m_file.write(m_volume);
		
		System.out.println("Volume " + i + " formatted");
	}
	
	private void renameVolume(int i, String sNewName) throws IOException {
		// Read the volume
		byte[] name = new byte[20];
		m_file.seek(i * m_volume.length);    // each volume is 800K in size (every second byte used)
		
		NewCF7ImageAction.setupVolume(i, name, sNewName, true); 
		// Write the new header
		m_file.write(name);
		System.out.println("Volume " + i + " renamed");
	}
	
	private void removeVolume(int i) throws IOException {
		m_file.seek(i * m_volume.length);    // each volume is 800K in size (every second byte used)
		
		NewCF7ImageAction.clearVolume(i, m_volume); 
		// Write the new header
		m_file.write(m_volume);
		
		System.out.println("Volume " + i + " removed");
	}
}
