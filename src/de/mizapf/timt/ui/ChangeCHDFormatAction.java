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

import java.awt.event.KeyEvent;
import java.awt.Dimension;
import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import de.mizapf.timt.files.*;
import java.awt.Cursor;
import java.io.FileOutputStream;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;

public class ChangeCHDFormatAction extends Activity {

	public String getMenuName() {
		return imagetool.langstr("ChangeCHD");
	}
	
	public String getActionName() {
		return "CHANGECHD";
	}
	
	public void go() {
		java.io.File selectedfile = null;
		
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
			ifsource = ImageFormat.getImageFormat(selectedfile.getAbsolutePath());
			if (!(ifsource instanceof MessCHDFormat)) {
				JOptionPane.showMessageDialog(m_parent, imagetool.langstr("NotCHD"), imagetool.langstr("InvalidFormat"), JOptionPane.ERROR_MESSAGE);				
				return;
			}
			source = (MessCHDFormat)ifsource;
		}
		catch (FileNotFoundException fnfx) {
			JOptionPane.showMessageDialog(m_parent, imagetool.langstr("FileNotFoundUnexp"), imagetool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
			return;
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_parent, imagetool.langstr("IOError") + ": " + iox.getClass().getName(), imagetool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
			return;
		}
		catch (ImageException ix) {
			JOptionPane.showMessageDialog(m_parent, imagetool.langstr("ImageError") + ": " + ix.getMessage(), imagetool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
			return;
		}		
		
		boolean bDone = false;
		int nNewFormat = 0;
		while (!bDone) {
			ChangeCHDFormatDialog changeDialog = new ChangeCHDFormatDialog(m_parent);
			changeDialog.createGui(imagetool.boldFont);
			changeDialog.setFileName(sImageFile);
			changeDialog.setSourceVersion(source.getVersion());
			changeDialog.setVisible(true);
			
			bDone = true;
			if (changeDialog.confirmed()) {
				nNewFormat = changeDialog.getNewFormat();
				if (nNewFormat < source.getVersion()) {
					int nRet = JOptionPane.showConfirmDialog(m_parent, String.format(imagetool.langstr("SureToDowngrade"), nNewFormat), imagetool.langstr("ConvertCHDVersion"), JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
					if (nRet != JOptionPane.OK_OPTION) bDone = false; 
				}
				else {
					if (nNewFormat == source.getVersion()) {
						JOptionPane.showMessageDialog(m_parent, imagetool.langstr("ConvertNoEffect"), imagetool.langstr("ConvertCHDVersion"), JOptionPane.WARNING_MESSAGE); 
						bDone = false;
					}
				}
			}
			else return;
		}
		
		// Now we know what to do.
		// Create a new image in the given version.
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		java.io.File fileTarget = null;
		FormatParameters parm = new FormatParameters(source.getCylinders(), source.getHeads(), source.getSectorsPerTrack(), source.getSectorLength(), nNewFormat);
		try {
			byte[] abyNewImage = MessCHDFormat.createEmptyCHDImage(parm);
			
			JFileChooser jfc1 = new JFileChooser();
			Dimension dim1 = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim1!=null) jfc1.setPreferredSize(dim1);
			
			ImageFileFilter im1 = new ImageFileFilter();
			jfc1.addChoosableFileFilter(im1);
			jfc1.setFileFilter(im1);
			
			int nReturn1 = jfc1.showSaveDialog(m_parent);
			if (nReturn1 == JFileChooser.APPROVE_OPTION) {
				imagetool.setProperty(TIImageTool.FILEDIALOG, jfc1.getWidth() + "x" + jfc1.getHeight());
				fileTarget = jfc1.getSelectedFile();
				int nSuffixPos = fileTarget.getName().indexOf(".");
				if (nSuffixPos==-1 || nSuffixPos == fileTarget.getName().length()-1) { 
					if (!fileTarget.getName().endsWith(".hd") && !fileTarget.getName().endsWith(".chd")) {
						fileTarget = new java.io.File(fileTarget.getAbsolutePath() + ".hd");
					}
				}
				
				imagetool.setSourceDirectory(fileTarget.getParentFile(),"image");
				
				FileOutputStream fos = new FileOutputStream(fileTarget);
				fos.write(abyNewImage);
				fos.close();
			}
		}
		catch (IllegalOperationException iox) {	
			JOptionPane.showMessageDialog(m_parent, iox.getMessage(), imagetool.langstr("ConvertError"), JOptionPane.ERROR_MESSAGE);		
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_parent, imagetool.langstr("IOError") + " " + imagetool.langstr("WhileNewImage") + ": " + iox.getClass().getName(), imagetool.langstr("ConvertError"), JOptionPane.ERROR_MESSAGE);		
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		
		// Open the target image
		// and for each hunk read in the source, write the hunk into the target
		boolean bOK = false;
		try {
			MessCHDFormat target = (MessCHDFormat)ImageFormat.getImageFormat(fileTarget.getAbsolutePath());
			target.reopenForWrite();
			// System.out.println("Hunks = " + source.getHunkCount());
			for (int i=0; i < source.getHunkCount(); i++) {
				// System.out.println("copy hunk " + i);
				byte[] abyHunk = source.getHunkContents(i);
				target.writeHunkContents(abyHunk, i);
			}
			bOK = true;			
		}
		catch (ImageException ix) {
			JOptionPane.showMessageDialog(m_parent, ix.getMessage(), imagetool.langstr("ConvertError"), JOptionPane.ERROR_MESSAGE);		
		}
		catch (FileNotFoundException fnfx) {
			JOptionPane.showMessageDialog(m_parent, imagetool.langstr("TargetNotFoundUnexp"), imagetool.langstr("ConvertError"), JOptionPane.ERROR_MESSAGE);		
		}
		catch (IOException iox) {
			iox.printStackTrace();
			JOptionPane.showMessageDialog(m_parent,  imagetool.langstr("IOError") + " " + imagetool.langstr("WhileCopyContents") + ": " , imagetool.langstr("ConvertError"), JOptionPane.ERROR_MESSAGE);
		}
		
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		if (bOK) JOptionPane.showMessageDialog(m_parent, String.format(imagetool.langstr("Completed"), imagetool.langstr("Conversion")), imagetool.langstr("ConvertCHDVersion"), JOptionPane.INFORMATION_MESSAGE);
	}
}
