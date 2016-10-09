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
import java.awt.Cursor;
import java.awt.Dimension;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;
import java.awt.Font;

import de.mizapf.timt.util.*;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class SaveTFIAction extends Activity {

	public String getMenuName() {
		return "Save as TIFILES ...";
	}
	
	public String getActionName() {
		return "SAVETFI";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		DirectoryPanel dp = dvCurrent.getPanel();
		Volume volCurrent = dvCurrent.getVolume();

		List<Element> selected = dvCurrent.getSelectedEntries();
		
		// Only if we have a single export
		if (selected.size()==1) {
			Iterator<Element> it = selected.iterator();
			Element el = it.next();
			if (el instanceof TFile) {
				// Single file only
				try {
					imagetool.saveToDisk(TIFiles.createFromFile((TFile)el).toByteArray(), true);
				}
				catch (IOException iox) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), iox.getClass().getName(), "Error saving file", JOptionPane.ERROR_MESSAGE); 
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Image error: " + ix.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE); 
				}
				return;
			}
		}

		// We are here because there is more than one selection, or because this is a directory
		JFileChooser jfc = null;
		int nReturn = 0;

		// Set the last import/export directory
		if (imagetool.getSourceDirectory("impexp")!=null) jfc = new JFileChooser(imagetool.getSourceDirectory("impexp"));
		else jfc = new JFileChooser();
		Dimension dim = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);

		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		nReturn = jfc.showSaveDialog(dvCurrent.getFrame());
		if (nReturn == jfc.CANCEL_OPTION) return;
		
		// Not cancelled
		ExportDialog expparm = new ExportDialog(dvCurrent.getFrame());
		expparm.createGui(imagetool.boldFont, imagetool.getPropertyString(imagetool.CONVERT, "/\\*_ __x."), Font.decode(imagetool.contentFont));

		expparm.setVisible(true);
		if (expparm.confirmed()) {
			// Save the dimensions of the file dialog
			imagetool.setProperty(imagetool.CONVERT, expparm.getSubstSource() + " " + expparm.getSubstTarget());		
			imagetool.setProperty(TIImageTool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				// Do a recursive export
				imagetool.exportDirectory(dirCurrent, jfc.getSelectedFile(), dvCurrent.getSelectedEntries(), false);

				// JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Export completed sucessfully", "Export files", JOptionPane.INFORMATION_MESSAGE);				
			}
			catch (ImageException ix) { 
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), ix.getMessage(), "Error getting file", JOptionPane.ERROR_MESSAGE);
			}
			catch (FileNotFoundException fnfx) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), fnfx.getMessage(), "File or folder not found", JOptionPane.ERROR_MESSAGE);
			}
			catch (IOException iox) { 
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), iox.getClass().getName(), "Error exporting file", JOptionPane.ERROR_MESSAGE);
			}
			catch (ReplaceTableException rx) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Replacement list mismatch, must have same number of characters.", "Export problem", JOptionPane.ERROR_MESSAGE);
			}
			catch (InvalidNameException ix) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), "File name contains a character that cannot be used for the exported file.", "Export problem", JOptionPane.ERROR_MESSAGE);
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
