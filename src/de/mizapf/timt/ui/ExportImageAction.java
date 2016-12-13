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

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Properties;

public class ExportImageAction extends Activity {

	public String getMenuName() {
		return imagetool.langstr("ExportImage");
	}
	
	public String getActionName() {
		return "EXPORTIMAGE";
	}
		
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		DirectoryPanel dp = dvCurrent.getPanel();
		
		Volume vol = dvCurrent.getVolume();
		Directory dirCurrent = vol.getRootDirectory();
		
		JFileChooser jfc = null;
		if (imagetool.getSourceDirectory("export")!=null) jfc = new JFileChooser(imagetool.getSourceDirectory("impexp"));
		else jfc = new JFileChooser();
		Dimension dim = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);

		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int nReturn = jfc.showSaveDialog(m_parent);
		if (nReturn == jfc.CANCEL_OPTION) return;
		
		ExportDialog expparm = new ExportDialog(m_parent);
		expparm.createGui(imagetool.boldFont, imagetool.getPropertyString(imagetool.CONVERT), Font.decode(imagetool.contentFont));
		expparm.setVisible(true);
		
		if (expparm.confirmed()) {
			imagetool.setProperty(imagetool.CONVERT, expparm.getSubstSource() + " " + expparm.getSubstTarget());		
			imagetool.setProperty(TIImageTool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());

			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				imagetool.exportDirectory(dirCurrent, jfc.getSelectedFile(), null, false);
				JOptionPane.showMessageDialog(m_parent, String.format(TIImageTool.langstr("Completed"), TIImageTool.langstr("Export")), TIImageTool.langstr("Export"), JOptionPane.INFORMATION_MESSAGE);				
			}
			catch (ImageException ix) { 
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ExportError"), JOptionPane.ERROR_MESSAGE);
			}
			catch (FileNotFoundException fnfx) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("FileFolderNotFound") + ": " + fnfx.getMessage(), TIImageTool.langstr("ExportError"), JOptionPane.ERROR_MESSAGE);
			}
			catch (IOException iox) { 
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ExportError"), JOptionPane.ERROR_MESSAGE);
			}
			catch (ReplaceTableException rx) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ReplaceMismatch"), TIImageTool.langstr("ExportError"), JOptionPane.ERROR_MESSAGE);
			}
			catch (InvalidNameException ix) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("InvalidName"), TIImageTool.langstr("ExportError"), JOptionPane.ERROR_MESSAGE);
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
