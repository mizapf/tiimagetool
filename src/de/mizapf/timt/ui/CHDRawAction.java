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

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import javax.swing.JFileChooser;
import java.awt.Dimension;
import java.awt.Cursor;
import javax.swing.JOptionPane;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.ui.ImageFileFilter;

public class CHDRawAction extends Activity {

	public String getMenuName() {
		return "Extract raw from CHD ...";
	}
	
	public String getActionName() {
		return "EXPORTCHD";
	}
	
	public void go() {
		java.io.File selectedfile = null;
		JOptionPane.showMessageDialog(m_parent, "First step: Select a CHD file as input.", "Export", JOptionPane.INFORMATION_MESSAGE);
		
		JFileChooser jfc = null;
		if (imagetool.getSourceDirectory("image")!=null) {
			jfc = new JFileChooser(imagetool.getSourceDirectory("image"));
		}
		else jfc = new JFileChooser();
		
		Dimension dim = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);
		ImageFileFilter im = new ImageFileFilter();
		im.setOnlyHD();
		jfc.addChoosableFileFilter(im);
		jfc.setFileFilter(im);
		jfc.setMultiSelectionEnabled(false);
		
		int nReturn = jfc.showOpenDialog(m_parent);
		
		if (nReturn == JFileChooser.APPROVE_OPTION) {
			selectedfile = jfc.getSelectedFile();
			java.io.File filePar = selectedfile.getParentFile();
			if (!filePar.getName().equals(".")) imagetool.setSourceDirectory(filePar, "image");  // sets the property only for non-UNC paths
			imagetool.setProperty(TIImageTool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
		}
		else return;
		
		String sImageFile = selectedfile.getName();
		ImageFormat ifsource = null;
		MessCHDFormat source = null;
		try {
			ifsource = ImageFormat.getImageFormat(selectedfile.getAbsolutePath(), Volume.SECTOR_LENGTH);
			if (!(ifsource instanceof MessCHDFormat)) {
				JOptionPane.showMessageDialog(m_parent, "Not a MESS CHD image file.", "Invalid format error", JOptionPane.ERROR_MESSAGE);				
				return;
			}
			source = (MessCHDFormat)ifsource;
		}
		catch (FileNotFoundException fnfx) {
			JOptionPane.showMessageDialog(m_parent, "Input file not found: " + fnfx.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE); 
			return;
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_parent, "IO error: " + iox.getClass().getName(), "Read error", JOptionPane.ERROR_MESSAGE); 
			return;
		}
		catch (ImageException ix) {
			JOptionPane.showMessageDialog(m_parent, "Image error: " + ix.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE); 
			return;
		}		
		
		CHDRawDialog expdialog = new CHDRawDialog(m_parent);
		expdialog.createGui(imagetool.boldFont);
		expdialog.setImageFile(sImageFile);
		
		// Calculate the size
		int nSize = source.getHunkCount() * 0x1000;
		expdialog.setImageSize(String.valueOf(nSize) + " bytes (" + String.valueOf(nSize/1048576) + " MiB)");
		expdialog.setVisible(true);
		
		boolean bOK = false;
		java.io.File fileTarget = null;
		if (expdialog.confirmed()) {
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			JFileChooser jfc1 = new JFileChooser();
			Dimension dim1 = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim1!=null) jfc1.setPreferredSize(dim1);
						
			int nReturn1 = jfc1.showSaveDialog(m_parent);
			if (nReturn1 == JFileChooser.APPROVE_OPTION) {
				imagetool.setProperty(TIImageTool.FILEDIALOG, jfc1.getWidth() + "x" + jfc1.getHeight());
				fileTarget = jfc1.getSelectedFile();
				
				try {
					DataOutputStream dos = new DataOutputStream(new FileOutputStream(fileTarget));
					int nBytes = source.getCylinders() * source.getHeads() * source.getSectorsPerTrack() * source.getSectorLength();
					int nLength = 0;
					// System.out.println("Hunks = " + source.getHunkCount());
					for (int i=0; i < source.getHunkCount(); i++) {
						// System.out.println("copy hunk " + i);
						byte[] abyHunk = source.getHunkContents(i);
						nLength = (nBytes > abyHunk.length)? abyHunk.length : nBytes;
						dos.write(abyHunk, 0, nLength);
						nBytes = nBytes - abyHunk.length;
					}
					// System.out.println("Last hunk written " + nLength + " bytes"); 
					dos.close();
					bOK = true;
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(m_parent, ix.getMessage(), "Extract error", JOptionPane.ERROR_MESSAGE);		
				}
				catch (FileNotFoundException fnfx) {
					JOptionPane.showMessageDialog(m_parent, "Target file or directory not found: " + fnfx.getMessage(), "Extract error", JOptionPane.ERROR_MESSAGE);		
				}
				catch (IOException iox) {
					iox.printStackTrace();
					JOptionPane.showMessageDialog(m_parent, "IO error while extracting contents: " + iox.getClass().getName(), "Extract error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		if (bOK) JOptionPane.showMessageDialog(m_parent, "Extraction completed successfully", "Extract", JOptionPane.INFORMATION_MESSAGE);
	}
}
