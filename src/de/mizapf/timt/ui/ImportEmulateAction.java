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

import javax.swing.*;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.EOFException;
import java.awt.Dimension;
import java.awt.Cursor;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.TIFiles;

public class ImportEmulateAction extends Activity {

	public String getMenuName() {
		return "Import as emulate files...";
	}
	
	public String getActionName() {
		return "IMPORTEMU";
	}
	
	public void go() {
		boolean bOK = true;
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		DirectoryPanel dp = dvCurrent.getPanel();

		JFileChooser jfc = null;
		if (imagetool.getSourceDirectory("impexp")!=null) jfc = new JFileChooser(imagetool.getSourceDirectory("impexp"));
		else jfc = new JFileChooser();
		Dimension dim = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);

		jfc.setMultiSelectionEnabled(true);
		int nReturn = jfc.showOpenDialog(dvCurrent.getFrame());

		if (nReturn == JFileChooser.APPROVE_OPTION) {
			imagetool.setProperty(TIImageTool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
			java.io.File[] afile = jfc.getSelectedFiles();
			imagetool.setSourceDirectory(afile[0].getParentFile(), "impexp");
			
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			Volume volTarget = dirCurrent.getVolume();

			try {
				volTarget.reopenForWrite();
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Cannot open image file for writing", "Import error", JOptionPane.ERROR_MESSAGE); 				
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}

			boolean bGuess = false; 
			for (java.io.File imagefile:afile) {
				String sAbsFile = null;
				try {
					sAbsFile = imagefile.getAbsolutePath();
					Volume vol = null;
					try {
						vol = imagetool.getAlreadyOpenedVolume(sAbsFile);
						if (vol==null) vol = new Volume(sAbsFile);
					}
					catch (MissingHeaderException mx) {
						JOptionPane.showMessageDialog(m_parent, "Image file does not have a floppy signature (DSK).", "Image error", JOptionPane.ERROR_MESSAGE);
						continue;
					}
					catch (ImageException ix) {
						JOptionPane.showMessageDialog(m_parent, ix.getMessage(), "Image error", JOptionPane.ERROR_MESSAGE);
						continue;
					}
					
					if (!vol.isFloppyImage()) {
						JOptionPane.showMessageDialog(m_parent, "Only floppy images can be used as EMULATE files; " + imagefile.getAbsolutePath() + " is not a floppy image.", "Image error", JOptionPane.ERROR_MESSAGE);
						continue;
					}
					
					// Copy the sector contents into the byte array
					byte[] content = new byte[vol.getTotalSectors()*vol.SECTOR_LENGTH];
					for (int i=0; i < vol.getTotalSectors(); i++) {
						Sector sect = vol.readSector(i);
						System.arraycopy(sect.getBytes(), 0, content, i*vol.SECTOR_LENGTH, vol.SECTOR_LENGTH);
					}
			
					try {
						String defName = imagefile.getName().toUpperCase();
						int perpos = defName.lastIndexOf(".");
						if (perpos != -1) defName = defName.substring(0,perpos);						
						NewElementDialog impdia = new NewElementDialog(dvCurrent.getFrame(), "New emulate file", defName, "Use the context menu to activate the EMULATE file.");
						impdia.createGui();
						impdia.setVisible(true);
						if (impdia.confirmed()) {							
							byte[] abyTif = TIFiles.createTfi(content, impdia.getElementName(), (byte)(TFile.EMULATE | TFile.PROGRAM), 0, 0);
							dirCurrent.insertFile(abyTif, null, false);
						}
					}
					catch (ProtectedException px) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), "Write error", JOptionPane.ERROR_MESSAGE); 
					}
					catch (FileExistsException fx) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "File " + fx.getMessage() + " already exists in directory; not imported", "Import error", JOptionPane.ERROR_MESSAGE); 
					}
					catch (EOFException ex) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), ex.getMessage(), "Import error", JOptionPane.ERROR_MESSAGE); 
						ex.printStackTrace();
					}
					catch (InvalidNameException ix) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Invalid name: " + ix.getMessage(), "Invalid name", JOptionPane.ERROR_MESSAGE); 
					}
				}
				// Exceptions from new Volume
				catch (FileNotFoundException fnfx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "File not found: " + fnfx.getMessage(), "Not found", JOptionPane.ERROR_MESSAGE);
					bOK = false;
				}
				catch (IOException iox) {
					iox.printStackTrace();
					bOK = false;
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Error reading file: " + iox.getClass().getName(), "Read error", JOptionPane.ERROR_MESSAGE); 
				}
				// from readSector
				catch (ImageException ix) {
					bOK = false;
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), ix.getMessage(), "Image error", JOptionPane.ERROR_MESSAGE); 
					break;
				}
			} 
			try {
				volTarget.reopenForRead();
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Cannot re-open image file for reading", "Import error", JOptionPane.ERROR_MESSAGE); 				
			}			
			imagetool.refreshPanel(volTarget);			
			if (bOK && afile.length>1) JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Import completed sucessfully", "Import files", JOptionPane.INFORMATION_MESSAGE);

			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
}
