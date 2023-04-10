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
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.Cursor;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.*;
import de.mizapf.timt.TIImageTool;

public class CreateArchiveAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("CreateArchive") + "...";
	}
	
	public String getActionName() {
		return "ARCHIVE";
	}
		
	// Used from the Menu
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		Volume vol = dirCurrent.getVolume();
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		String sFirstName = "FILES";
		
		for (Element selected : dvCurrent.getSelectedEntries()) {
			if (selected instanceof TFile) {
				sFirstName = selected.getName();
				break;
			}
		}
		
		ArchiveDialog ad = new ArchiveDialog(dvCurrent.getFrame());
		ad.createGui(imagetool.boldFont, sFirstName);

		ad.setVisible(true);
		Archive ark = null;

		if (ad.confirmed()) {
			try {
				vol.reopenForWrite();
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImageFWP"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE); 				
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;				
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("NotReopen"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE); 				
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}

			// Create an empty archive
			byte flags = ad.useCompression()? TFile.INTERNAL : (byte)0x00;
			byte[] abyEmpty = Archive.createEmptyArchive(ad.useCompression());

			byte[] abyTfi = TIFiles.createTfi(abyEmpty, ad.getArchiveName(), flags, 128, 0);
			TFile arkf = null;
			
			try {
				// Caution: We must not change the directory while elements
				// are selected.
				// Copy the files into an array first
				List<TIFiles> forArc = new ArrayList<TIFiles>();
				for (Element selected : dvCurrent.getSelectedEntries()) {
					if (selected instanceof TFile) {
						forArc.add(TIFiles.createFromFile((TFile)selected));
					}
				}
				TIFiles[] aforarc = new TIFiles[forArc.size()];
				forArc.toArray(aforarc);			
				
				// then add the archive
				System.out.println("Add the empty archive file " + ad.getArchiveName() + " to the current directory");
				arkf = dirCurrent.insertFile(abyTfi, null, false);
				ark = arkf.unpackArchive();
				// vol.sameGeneration();
				System.out.println("Add all selected files to the archive");
				ark.insertFiles(aforarc, null, false);
				dirCurrent.commit(false);
				System.out.println("done");
			// FIXME: Element selection must be improved (select/deselect) 
			}
			catch (InvalidNameException inx) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("ArchiveCannotPut"), inx.getMessage()), TIImageTool.langstr("ArchiveError"), JOptionPane.ERROR_MESSAGE); 	
			}
			catch (ImageFullException ifx) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ArchiveNoSpace"), TIImageTool.langstr("ArchiveError"), JOptionPane.ERROR_MESSAGE);
				// Remove ark again
				try {	
					dirCurrent.deleteFile(arkf, true);
					dirCurrent.commit(true);
				}
				catch (Exception e) {
					// Do your best, we don't expect any issue here
					e.printStackTrace();
				}
			}
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ArchiveError"), JOptionPane.ERROR_MESSAGE);
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("VolumeWP"), TIImageTool.langstr("ArchiveError"), JOptionPane.ERROR_MESSAGE);
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError"), TIImageTool.langstr("ArchiveError"), JOptionPane.ERROR_MESSAGE);
			}
			catch (FormatException fx) {
				fx.printStackTrace();
			}
			catch (IllegalOperationException ix) {
				ix.printStackTrace();
			}

			try {
				vol.reopenForRead();
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("NotReopen") + ": " + iox.getMessage(), TIImageTool.langstr("ArchiveError"), JOptionPane.ERROR_MESSAGE); 				
			}
			imagetool.refreshPanel(vol);			
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
