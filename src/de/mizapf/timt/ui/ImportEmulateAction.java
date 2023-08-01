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
		return TIImageTool.langstr("ImportEmulate") + "...";
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
		Dimension dim = settings.getPropertyDim(TIImageTool.FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);

		jfc.setMultiSelectionEnabled(true);
		int nReturn = jfc.showOpenDialog(dvCurrent.getFrame());

		if (nReturn == JFileChooser.APPROVE_OPTION) {
			settings.put(TIImageTool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
			java.io.File[] afile = jfc.getSelectedFiles();
			imagetool.setSourceDirectory(afile[0].getParentFile(), "impexp");
			
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			Volume volTarget = dirCurrent.getVolume();

			try {
				volTarget.reopenForWrite();
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImageFWP"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE); 				
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}

			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 				
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
						if (vol==null) {
							ImageFormat image = ImageFormat.determineImageFormat(sAbsFile);
							vol = new Volume(image);
						}
					}
					catch (MissingHeaderException mx) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImportEmulateNoSig"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
						continue;
					}
					catch (ImageException ix) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
						continue;
					}
					
					if (!vol.isFloppyImage()) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImportEmulateOnlyFloppy"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
						continue;
					}
					
					// Copy the sector contents into the byte array
					byte[] content = new byte[vol.getTotalSectors() * TFileSystem.SECTOR_LENGTH];
					for (int i=0; i < vol.getTotalSectors(); i++) {
						Sector sect = vol.readSector(i);
						System.arraycopy(sect.getData(), 0, content, i * TFileSystem.SECTOR_LENGTH, TFileSystem.SECTOR_LENGTH);
					}
			
					try {
						String defName = imagefile.getName().toUpperCase();
						int perpos = defName.lastIndexOf(".");
						if (perpos != -1) defName = defName.substring(0,perpos);						
						NewElementDialog impdia = new NewElementDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImportEmulateName"), defName, TIImageTool.langstr("ImportEmulateHint"));
						impdia.createGui();
						impdia.setVisible(true);
						if (impdia.confirmed()) {							
							byte[] abyTif = TIFiles.createTfi(content, impdia.getElementName(), (byte)(TFile.EMULATE | TFile.PROGRAM), 0, 0);
							dirCurrent.insertFile(abyTif, null, false);
						}
					}
					catch (ProtectedException px) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					}
					catch (FileExistsException fx) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("ImportFileExists"), fx.getMessage()), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					}
					catch (EOFException ex) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), ex.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
						ex.printStackTrace();
					}
					catch (InvalidNameException ix) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("InvalidFileName") + ": " + ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					}
				}
				// Exceptions from new Volume
				catch (FileNotFoundException fnfx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("FileNotFound") + ": " + fnfx.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
					bOK = false;
				}
				catch (IOException iox) {
					iox.printStackTrace();
					bOK = false;
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
				}
				// from readSector
				catch (ImageException ix) {
					bOK = false;
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					break;
				}
			} 
			try {
				volTarget.reopenForRead();
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("NotReopen"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 				
			}			
			imagetool.refreshPanel(volTarget);			
			if (bOK && afile.length>1) JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("Completed"), TIImageTool.langstr("Import")), TIImageTool.langstr("ImportFiles"), JOptionPane.INFORMATION_MESSAGE);

			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
}
