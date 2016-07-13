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
    
    Copyright 2013 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import java.awt.Cursor;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;

import javax.swing.JOptionPane;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.*;
import de.mizapf.timt.TIImageTool;

public class InstallGenOSAction extends Activity {

	private final static String SYSTEMSYS = "/ti/util/system_sys_redist.tfi";
	private final static String LOADSYS = "/ti/util/load_sys_redist.tfi";
	private final static String AUTOEXEC = "/ti/util/autoexec.txt";
	
	public String getMenuName() {
		return "Install Geneve OS";
	}
	
	public String getActionName() {
		return "INSTALLGOS";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		
		// Caution: the next line will unselect all windows, and therefore,
		// dvCurrent will be null
		int nCheck = JOptionPane.showConfirmDialog(m_parent, 
			"You are about to install Geneve OS on the currently open image.\nExisting files will not be changed. Continue?", "Install GenOS", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);

		if (dvCurrent == null) {
			JOptionPane.showMessageDialog(m_parent, "BUG: Current view is null", "Import error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		Directory dirCurrent = dvCurrent.getDirectory();
		DirectoryPanel dp = dvCurrent.getPanel();

		Volume vol = dvCurrent.getVolume();
		Directory dirRoot = vol.getRootDirectory();

		if (nCheck == JOptionPane.OK_OPTION) {
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			Volume volTarget = dirCurrent.getVolume();
			try {
				volTarget.reopenForWrite();
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent, "Cannot open image file for writing", "Import error", JOptionPane.ERROR_MESSAGE); 				
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}
			
						
			try {
				// SYSTEM/SYS
				installFile(SYSTEMSYS, null, dirRoot, dvCurrent);
				
				// LOAD/SYS
				if (vol.isFloppyImage()) {
					dirCurrent = dirRoot;
				}
				else {
					if (dirRoot.hasSubdirectory("DSK1")) {
						dirCurrent = dirRoot.getSubdirectory("DSK1");
					}
					else {
						// create a DSK1 directory
						dirCurrent = dirRoot.createSubdirectory("DSK1", false);
					}
				}
				installFile(LOADSYS, null, dirCurrent, dvCurrent);

				// AUTOEXEC
				// Import content
				installFile(AUTOEXEC, "AUTOEXEC", dirRoot, dvCurrent); 				
			}
			catch (IllegalOperationException iox) {
				JOptionPane.showMessageDialog(m_parent, "Could not create the DSK1 directory", "Import error", JOptionPane.ERROR_MESSAGE);
			}
			catch (InvalidNameException ix) {
				ix.printStackTrace();
			} 			
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(m_parent, "Volume may be corrupt; cannot install Geneve OS: " + ix.getMessage(), "Import error", JOptionPane.ERROR_MESSAGE);				
			}			
			catch (ProtectedException ix) {
				JOptionPane.showMessageDialog(m_parent, "Volume is protected; cannot install Geneve OS", "Import error", JOptionPane.ERROR_MESSAGE); 
			}			
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent, "IO error when copying Geneve OS on image: " + iox.getClass().getName(), "Import error", JOptionPane.ERROR_MESSAGE); 
			}
			
			try {
				volTarget.reopenForRead();
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent, "Cannot open image file for reading", "Import error", JOptionPane.ERROR_MESSAGE); 				
			}
			
			try {
				imagetool.reloadVolume(vol);
				imagetool.refreshPanel(vol);
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(m_parent, "Could not re-open image file: " + e.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE); 
			}

			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	private void installFile(String sFilename, String sTargetName, Directory where, DirectoryView dvCurrent) throws IOException, ProtectedException, ImageException {
		byte[] abyTif = null;
		InputStream isFile = getClass().getResourceAsStream(sFilename);
		if (isFile == null) {
			JOptionPane.showMessageDialog(m_parent, sFilename + " not found in JAR file; maybe the JAR file is corrupt.", "Import error", JOptionPane.ERROR_MESSAGE); 
			return;
		}
		DataInputStream dis = new DataInputStream(isFile);
		abyTif = new byte[dis.available()];
		dis.readFully(abyTif);

		// Before importing, save the "ignore TIFILES name" flag because
		// we rely on the TIFILES name in this case
		boolean nameMode = imagetool.getPropertyBoolean(TIImageTool.KEEPNAME);
		imagetool.setProperty(TIImageTool.KEEPNAME, "false");
		
		if (sTargetName != null) {
			String sAutoexec = new String(abyTif);
			// Convert it to a DV80 file 
			String[] lines = sAutoexec.split("\n");
			byte[] sectors = TFile.textToSectors(lines);
			// Hand it over to TIFiles
			abyTif = TIFiles.createTfi(sectors, sTargetName, (byte)0x90, (byte)80, 0);
		}
		
		try {
			imagetool.putTIFileIntoImage(where, dvCurrent, abyTif, "somefile");
		}
		catch (FileExistsException fx) {
			JOptionPane.showMessageDialog(m_parent, "File " + fx.getMessage() + " already exists in directory; not imported", "Import error", JOptionPane.ERROR_MESSAGE); 
		}
		catch (InvalidNameException inx) {
			JOptionPane.showMessageDialog(m_parent, "Invalid name: " + inx.getMessage() + "; not imported", "Import error", JOptionPane.ERROR_MESSAGE); 
		}

		imagetool.setProperty(TIImageTool.KEEPNAME, nameMode? "true" : "false");

		dis.close();		
	}
}
