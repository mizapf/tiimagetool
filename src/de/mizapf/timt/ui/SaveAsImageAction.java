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
    
    Copyright 2020 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import java.io.*;
import java.awt.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.files.Volume;
import de.mizapf.timt.files.ImageFormat;
import de.mizapf.timt.files.FileImageFormat;
import de.mizapf.timt.files.FormatParameters;
import de.mizapf.timt.files.ImageException;
import de.mizapf.timt.util.InternalException;

public class SaveAsImageAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("SaveAs") + "...";
	}
	
	public String getActionName() {
		return "SAVEAS";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Volume vol = dvCurrent.getVolume();
		saveAs(vol);
	}
		
	public void saveAs(Volume vol) {

		// Query the image type
		// Preset the last type
		SaveAsImageDialog sd = new SaveAsImageDialog(m_parent, vol.getImageType(), vol.isFloppyImage());

		sd.createGui(imagetool.boldFont);
		sd.setVisible(true);
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		if (sd.confirmed()) {		
			String sWarn = ImageFormat.checkFormatCompatibility(vol.getFormatParams(), sd.getImageType());
			if (sWarn != null) {
				if (sWarn.startsWith("!")) {
					JOptionPane.showMessageDialog(m_parent, sWarn.substring(1), TIImageTool.langstr("ImageError"), JOptionPane.ERROR_MESSAGE);
					m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					return;
				}
				else {
					JOptionPane.showMessageDialog(m_parent, sWarn, TIImageTool.langstr("Warning"), JOptionPane.WARNING_MESSAGE);
				}
			}
			
			if (vol.isIDEImage()) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("Image.IDE.SaveAs"), TIImageTool.langstr("Warning"), JOptionPane.WARNING_MESSAGE);
			}

			JFileChooser jfc = null;
			if (imagetool.getSourceDirectory("image")!=null) {
				jfc = new JFileChooser(imagetool.getSourceDirectory("image"));
			}
			else jfc = new JFileChooser();
			
			// only files, no multiple selection
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			jfc.setMultiSelectionEnabled(false);

			ImageFileFilter im = new ImageFileFilter(settings.getPropertyString(imagetool.IMGSUF));

			jfc.addChoosableFileFilter(im);
			jfc.setFileFilter(im);	
			
			Dimension dim = settings.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			
			// open it now
			int nReturn = jfc.showSaveDialog(m_parent);
			
			File selectedFile = null;
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				File select = jfc.getSelectedFile();
				if (select != null) {
					selectedFile = new File(ImageFormat.getSuffixedName(select.getAbsolutePath(), sd.getImageType()));
				}
			}
			else {
				selectedFile = null;
			}
			
			if (selectedFile != null) {
				
				FileImageFormat newImage = null;
				
				// Overwrite?
				if (selectedFile.exists()) {		
					int nRet = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("ExistsOverwrite"), TIImageTool.langstr("NewImageTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (nRet == JOptionPane.NO_OPTION) {
						m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						return;
					}
					selectedFile.delete();
				}
				
				// Ready to save
				// System.out.println("Save as " + selectedFile.getAbsolutePath());
				
				try {
					newImage = (FileImageFormat)ImageFormat.getImageFormat(selectedFile.getAbsolutePath(), sd.getImageType(), vol.getFormatParams());
					if (newImage == null) {
						throw new InternalException(TIImageTool.langstr("ImageUnknown") + ": " + ImageFormat.suffix[sd.getImageType()]);
					}
					
					vol.saveNewImage(newImage);
					imagetool.addRecent(selectedFile.getAbsolutePath());
					imagetool.refreshAllViews();
					
					java.io.File filePar = selectedFile.getParentFile();
					imagetool.setSourceDirectory(filePar, "image");
				}
				catch (InternalException e) {
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("InternalError") + ": " + e.getMessage(), TIImageTool.langstr("InternalError"), JOptionPane.ERROR_MESSAGE);
				}
				catch (IOException iox) {
					iox.printStackTrace();
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("WriteError"), JOptionPane.ERROR_MESSAGE);
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ImageError"), JOptionPane.ERROR_MESSAGE);				
				}
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		// else we cancelled the save after the type selection		
	}
}

// NOTE: Only saves the open IDE partition as a single image.

/*
		
TODO: Either disallow to overwrite the origin file
or cache all sectors (no RandomAccessFile used)
This will lose all special information, though.
Error message: Cannot change the format of the origin file. Choose a new file name.

Format change: Recreation of the track without using original information
Save as: Will not preserve any metadata but recreate them. If those data shall
be kept, use Save or copy the image file and work on it.

Should be part of dialog window.

"Saving to a new file will not preserve non-standard track information as used 
for copy protections"

Cannot overwrite 

*/
