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
import java.io.*;
import java.awt.Dimension;
import java.awt.event.KeyEvent;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class NewImageAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_N;
	}
	
	public String getMenuName() {
		return "Floppy image...";
	}
	
	public String getActionName() {
		return "NEWIMAGE";
	}
	
	public void go() {
		NewImageDialog newimage = new NewImageDialog(m_parent);

		try {
			newimage.createGui();
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		newimage.setVisible(true);

		Volume newVolume = null;
		
		if (newimage.confirmed()) {
			
			// Sanity checks
			if (newimage.getImageType()==ImageFormat.TRACKDUMP) {
				if (newimage.getSides()==1)
				{
					JOptionPane.showMessageDialog(m_parent, "Track dump images are two-sided only.", "Create error", JOptionPane.ERROR_MESSAGE);
					return;
				}				
				if (newimage.getTrackCount()==80)
				{
					JOptionPane.showMessageDialog(m_parent, "Track dump images are defined for 40 tracks only.", "Create error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
			
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
						if (!file.getName().endsWith(newimage.getImageTypeSuffix())) {
							file = new File(file.getAbsolutePath() + newimage.getImageTypeSuffix());
						}
					}
					
					if (file.exists()) {		
						int nRet = JOptionPane.showConfirmDialog(m_parent, "Image file already exists. Overwrite?", "New image", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if (nRet == JOptionPane.NO_OPTION) return;
					}
					
					if (imagetool.getAlreadyOpenedVolume(file.getAbsolutePath()) != null) {
						JOptionPane.showMessageDialog(m_parent, "Volume with same file name already opened", "Illegal operation", JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					imagetool.setSourceDirectory(file.getParentFile(), "image");
					
					Volume.createFloppyImage(file, 
									newimage.getDiskName(),
									newimage.getImageType(),
									newimage.getSides(), 
									newimage.getDensity(),
									newimage.getTrackCount(),
									newimage.formatImage());
				
				}
				// Open it when it is initialized
				if (file != null) {
					if (newimage.formatImage()) {
						newVolume = new Volume(file.getAbsolutePath());
						Directory root = newVolume.getRootDirectory();					
						imagetool.addDirectoryView(root);
					}
					else {
						JOptionPane.showMessageDialog(m_parent, 
							"This image is unformatted; you must format it in an emulator first\nbefore you can use it here.", "Unformatted", JOptionPane.OK_OPTION);
					}
				}
			}
			catch (FileNotFoundException fnfx) {
				fnfx.printStackTrace();
			}
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(m_parent, "Error opening new image: " + ix.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE);				
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent, "Error reading file: " + iox.getClass().getName(), "Read error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
