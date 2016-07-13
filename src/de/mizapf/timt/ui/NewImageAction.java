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
		boolean bTDF = false;
		if (newimage.confirmed()) {
			try {
				byte[] abyImage = Volume.createImage(newimage.getDiskName(), !newimage.formatImage(), newimage.sectorDump(), 
					newimage.getSides(), newimage.getDensity(), newimage.getTrackCount());
				
				bTDF = !newimage.sectorDump();			
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
				java.io.File file = null;
				if (nReturn == JFileChooser.APPROVE_OPTION) {
					imagetool.setProperty(TIImageTool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
					file = jfc.getSelectedFile();
					int nSuffixPos = file.getName().indexOf(".");
					if (nSuffixPos==-1 || nSuffixPos == file.getName().length()-1) { 
						if (bTDF) {
							if (!file.getName().endsWith(".dtk")) {
								file = new java.io.File(file.getAbsolutePath() + ".dtk");
							}
						}
						else {	
							if (!file.getName().endsWith(".dsk")) {
								file = new java.io.File(file.getAbsolutePath() + ".dsk");
							}
						}
					}
					
					if (file.exists()) {		
						int nRet = JOptionPane.showConfirmDialog(m_parent, "Image file already exists. Overwrite?", "New image", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
						if (nRet == JOptionPane.NO_OPTION) return;
					}
					
					Volume vol = imagetool.getAlreadyOpenedVolume(file.getAbsolutePath());
					if (vol != null) {
						JOptionPane.showMessageDialog(m_parent, "Volume with same file name already opened", "Illegal operation", JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					imagetool.setSourceDirectory(file.getParentFile(), "image");
					
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(abyImage);
					fos.close();		
				}
				
				if (!newimage.formatImage()) {
					JOptionPane.showMessageDialog(m_parent, 
						"This image is unformatted; you must format it in an emulator first\nbefore you can use it here.", "Unformatted", JOptionPane.OK_OPTION);
				}
				else { 
					if (file != null) {
						Volume vol = new Volume(file.getAbsolutePath());
						Directory root = vol.getRootDirectory();					
						imagetool.addDirectoryView(root);
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
