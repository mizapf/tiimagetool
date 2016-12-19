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
import java.io.*;
import java.awt.Dimension;
import java.awt.event.KeyEvent;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class NewCF7VolumeAction extends Activity {

	public String getMenuName() {
		return imagetool.langstr("SingleCF7");
	}
	
	public String getActionName() {
		return "NEWCF7VOL";
	}
	
	public void go() {
		NewCF7Dialog newimagedia = new NewCF7Dialog(m_parent, false);

		try {
			newimagedia.createGui(imagetool.boldFont);
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		newimagedia.setVisible(true);

		Volume newVolume = null;
		
		if (newimagedia.confirmed()) {
			// Copied from NewImageAction
			try {
				JFileChooser jfc = null;
				if (imagetool.getSourceDirectory("image")!=null) {
					jfc = new JFileChooser(imagetool.getSourceDirectory("image"));
				}
				else jfc = new JFileChooser();
				
				Dimension dim = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
				if (dim!=null) jfc.setPreferredSize(dim);
				
				ImageFileFilter im = new ImageFileFilter();
				jfc.addChoosableFileFilter(im);
				jfc.setFileFilter(im);
				
				int nReturn = jfc.showSaveDialog(m_parent);
				File file = null;
				if (nReturn == JFileChooser.APPROVE_OPTION) {
					imagetool.setProperty(TIImageTool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
					file = jfc.getSelectedFile();
					int nSuffixPos = file.getName().indexOf(".");
					if (nSuffixPos==-1 || nSuffixPos == file.getName().length()-1) {
						if (!file.getName().endsWith(newimagedia.getImageTypeSuffix())) {
							file = new File(file.getAbsolutePath() + newimagedia.getImageTypeSuffix());
						}
					}
					
					if (file.exists()) {		
						int nRet = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("ExistsOverwrite"), TIImageTool.langstr("NewVolume"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if (nRet == JOptionPane.NO_OPTION) return;
					}
					
					if (imagetool.getAlreadyOpenedVolume(file.getAbsolutePath()) != null) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("NewVolumeSameName"), TIImageTool.langstr("NewVolume"), JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					imagetool.setSourceDirectory(file.getParentFile(), "image");
					
					Volume.createFloppyImage(file, 
									newimagedia.getDiskName(),
									ImageFormat.CF7VOLUME,
									2, 
									ImageFormat.DOUBLE_DENSITY,
									40,
									true);
				
				}
				// Open it when it is initialized
				if (file != null) {
					newVolume = new Volume(file.getAbsolutePath());
					Directory root = newVolume.getRootDirectory();					
					imagetool.addDirectoryView(root);
				}
			}
			catch (FileNotFoundException fnfx) {
				fnfx.printStackTrace();
			}
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("NewVolume"), JOptionPane.ERROR_MESSAGE);				
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("NewVolume"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
