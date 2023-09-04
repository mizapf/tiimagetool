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
import java.awt.*;
import java.awt.event.KeyEvent;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class NewIDEImageAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_I;
	}
	
	public String getMenuName() {
		return TIImageTool.langstr("Menu.IDE.New") + "...";
	}
	
	public String getActionName() {
		return "NEWIDE";
	}
	
	public void go() {
		NewIDEImageDialog newimage = new NewIDEImageDialog(m_parent);

		try {
			newimage.createGui(imagetool.boldFont);
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		newimage.setVisible(true);
		
		if (newimage.confirmed()) {
			System.out.println("New partitions: ");
			String[] asPart = newimage.getPartitionNames();
			int[] anSize = newimage.getPartitionSizes();
			for (int i=0; i < asPart.length; i++) {
				System.out.println(asPart[i] + ": " + anSize[i]);
			}
			
			SaveAsImageDialog sd = new SaveAsImageDialog(m_parent, ImageFormat.MEMORY, false);
			
			sd.createGui(imagetool.boldFont);
			sd.setVisible(true);
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			if (sd.confirmed()) {		
				JFileChooser jfc = null;
				if (imagetool.getSourceDirectory("image")!=null) {
					jfc = new JFileChooser(imagetool.getSourceDirectory("image"));
				}
				else jfc = new JFileChooser();
				
				// only files, no multiple selection
				jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				jfc.setMultiSelectionEnabled(false);
				
				ImageFileFilter im = new ImageFileFilter();
				jfc.addChoosableFileFilter(im);
				jfc.setFileFilter(im);	
				
				Dimension dim = settings.getPropertyDim(TIImageTool.FILEDIALOG);
				if (dim!=null) jfc.setPreferredSize(dim);
				
				// open it now
				int nReturn = jfc.showSaveDialog(m_parent);
				
				// New FileImageFormat
				
				// Create a MemoryImageFormat
				// Write the sectors to memory
				// newImage.saveImageFromOld(MemoryImage);
				
			}
			
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
}
