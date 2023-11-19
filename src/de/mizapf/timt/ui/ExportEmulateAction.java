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
import java.awt.Cursor;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.*;
import java.io.*;
import java.awt.Dimension;
import java.awt.event.KeyEvent; 
import de.mizapf.timt.TIImageTool;

public class ExportEmulateAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("SaveDSK") + "...";
	}
	
	public String getActionName() {
		return "SAVEEMU";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		Volume volCurrent = dvCurrent.getVolume();

		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		for (Element selected : dvCurrent.getSelectedEntries()) {
			if (selected instanceof TFile) {
				try {
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
					
					int nReturn = jfc.showSaveDialog(m_parent);
					File file = null;
					if (nReturn == JFileChooser.APPROVE_OPTION) {
						settings.put(imagetool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
						file = jfc.getSelectedFile();
						int nSuffixPos = file.getName().indexOf(".");
						if (nSuffixPos==-1 || nSuffixPos == file.getName().length()-1) {
							if (!file.getName().endsWith(".dsk")) {
								file = new File(file.getAbsolutePath() + ".dsk");
							}
						}
						
						if (file.exists()) {		
							int nRet = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("ExistsOverwrite"), TIImageTool.langstr("ExportEmu"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if (nRet == JOptionPane.NO_OPTION) return;
						}
						
						imagetool.setSourceDirectory(file.getParentFile(), "impexp");

						FileOutputStream fos = new FileOutputStream(file);
						fos.write(((TFile)selected).getRawContent());
						fos.close();
						JOptionPane.showMessageDialog(m_parent, String.format(TIImageTool.langstr("Completed"), TIImageTool.langstr("Export")), TIImageTool.langstr("ExportEmu"), JOptionPane.INFORMATION_MESSAGE);	
					}
				}
				catch (IOException iox) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ExportEmu"), JOptionPane.ERROR_MESSAGE); 
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ExportEmu"), JOptionPane.ERROR_MESSAGE); 
				}
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
