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

import java.awt.*;
import java.io.*;

import javax.swing.*;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.*;
import de.mizapf.timt.TIImageTool;

public class SectorEditAction extends Activity {
	
	public String getMenuName() {
		return "Edit sectors ...";
	}
	
	public String getActionName() {
		return "SECEDIT";
	}
	
	public void go() {
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		// Present an open dialog like the one for opening images	
		File selectedFile = null;	
		JFileChooser jfc = null;
		if (imagetool.getSourceDirectory("image")!=null) {
			jfc = new JFileChooser(imagetool.getSourceDirectory("image"));
		}
		else jfc = new JFileChooser();
		
		Dimension dim = imagetool.getPropertyDim(imagetool.FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);
		ImageFileFilter im = new ImageFileFilter();
		jfc.addChoosableFileFilter(im);
		jfc.setFileFilter(im);
		jfc.setMultiSelectionEnabled(false);
		
		int nReturn = jfc.showOpenDialog(m_parent);

		if (nReturn != JFileChooser.APPROVE_OPTION) {
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
			
		selectedFile = jfc.getSelectedFile();
		// We do not change the properties for images this time.
		try {
			String sAbsFile = selectedFile.getAbsolutePath();
			if (imagetool.getAlreadyOpenedVolume(sAbsFile) != null) {
				JOptionPane.showMessageDialog(m_parent, "Cannot edit sectors of an open image. Close all of its tabs first.", "Open error", JOptionPane.ERROR_MESSAGE); 
			}
			else {
				ImageFormat iform = ImageFormat.getImageFormat(sAbsFile);
				java.io.File filePar = selectedFile.getParentFile();
				imagetool.setSourceDirectory(filePar, "image");
				imagetool.showSectorEditor(selectedFile.getName(), iform);
			}
		}
		catch (FileNotFoundException fnfx) {
			JOptionPane.showMessageDialog(m_parent, "File not found: " + selectedFile.getName(), "Open error", JOptionPane.ERROR_MESSAGE); 
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_parent, "Error when opening " + selectedFile.getName() + ":" + iox.getClass().getName(), "Open error", JOptionPane.ERROR_MESSAGE); 
		}
		catch (ImageException ix) {
			JOptionPane.showMessageDialog(m_parent, "Cannot open image " + selectedFile.getName() + ":" + ix.getMessage(), "Open error", JOptionPane.ERROR_MESSAGE); 
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
