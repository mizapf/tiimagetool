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
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.event.*;
import de.mizapf.timt.TIImageTool;

import de.mizapf.timt.files.*;

class CHDRawDialog extends ToolDialog {

	JLabel 				m_jlImageFile;
	JLabel				m_jlExportSize;
	JTextField			m_tfImageFile;
	JTextField			m_tfRawFile;
	TIImageTool 		imagetool;
	boolean 			m_validCHD;
	boolean 			m_validTarget;
	
	MessCHDFormat		m_image;
	File				m_targetFile;
	
	private final static int FROM = 1;
	private final static int TO = 2;
	
	CHDRawDialog(JFrame owner, TIImageTool timt) {
		super(owner, "Extract raw contents from a CHD image");
		imagetool = timt;
	}
	
/*
	| 	Export raw										|

		Extract raw contents from a CHD image. 
	
		Image file:       		[...]
		Export size will be: 	....

		Next: Select the name of a file where to store the contents.
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
				
*/	
	public void createGui(Font font) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth("File name for CHD image (from)");
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));		

		putTextLine(this, "!Exporting all sectors from a CHD file", 0);
		add(Box.createVerticalStrut(10));
		putTextLine(this, "This function creates a new file that consists of the contents of all sectors", 0);
		putTextLine(this, "from sector 0 to the last sector, essentially a sector dump format for a hard disk.", 0);
		putTextLine(this, "No metadata are retained. You can edit this file with an external hex editor and", 0);
		putTextLine(this, "import it into a CHD file again later.", 0);
		add(Box.createVerticalStrut(10));
		
		String fileprompt = "File name for CHD image (from)";
		String rawprompt = "File name for raw contents (to)";
		m_tfImageFile = new JTextField("click to select");
		m_tfRawFile = new JTextField("click to select");

		addChoiceLine(nColumnWidth, fileprompt, FILELINE, FROM, m_tfImageFile, 32);
		add(Box.createVerticalStrut(10));
		addChoiceLine(nColumnWidth, rawprompt, FILELINE, TO, m_tfRawFile, 32);
		add(Box.createVerticalStrut(10));

		m_jlExportSize = putLabel(this, "Export size will be ", "0", nColumnWidth);
				
		add(Box.createVerticalStrut(10));	
		m_validCHD = false;
		m_validTarget = false;
		addButtons();
	}
	
	boolean validSelection() {
		return m_validCHD && m_validTarget;
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		JFileChooser jfc = null;
		if (ae.getSource()==m_btnOK) {
			m_bSet = true;
			dispose();
		}
		if (ae.getSource()==m_btnCancel) {
			m_bSet = false;
			dispose();
		}

		if (ae.getActionCommand().equals(String.valueOf(FROM))) {
			File lastPath = imagetool.getSourceDirectory("image");
			if (lastPath!=null) {
				jfc = new JFileChooser(lastPath);
			}
			else jfc = new JFileChooser();
			
			Dimension dim = imagetool.getPropertyDim(imagetool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			ImageFileFilter im = new ImageFileFilter();
			im.setOnlyHD();
			jfc.addChoosableFileFilter(im);
			jfc.setFileFilter(im);
			jfc.setMultiSelectionEnabled(false);
			
			int nReturn = jfc.showOpenDialog(m_frmMain);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				File file = jfc.getSelectedFile();
				File filePar = file.getParentFile();
				if (!filePar.getName().equals(".")) imagetool.setSourceDirectory(filePar, "image");  // sets the property only for non-UNC paths
				imagetool.setProperty(imagetool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
				m_validCHD = checkCHD(file);
				if (m_validCHD) m_tfImageFile.setText(file.getAbsolutePath());
			}
		}
		
		if (ae.getActionCommand().equals(String.valueOf(TO))) {
			File lastPath = imagetool.getSourceDirectory("image");
			if (lastPath!=null) {
				jfc = new JFileChooser(lastPath);
			}
			else jfc = new JFileChooser();
			
			Dimension dim = imagetool.getPropertyDim(imagetool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			jfc.setMultiSelectionEnabled(false);
			
			int nReturn = jfc.showSaveDialog(m_frmMain);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				m_targetFile = jfc.getSelectedFile();
				m_tfRawFile.setText(m_targetFile.getAbsolutePath());
				m_validTarget = true;
				File filePar = m_targetFile.getParentFile();
				if (!filePar.getName().equals(".")) imagetool.setSourceDirectory(filePar, "image");  // sets the property only for non-UNC paths
				imagetool.setProperty(imagetool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
			}
		}
	}
	
	private boolean checkCHD(File selectedfile) {
		String sImageFile = selectedfile.getName();
		ImageFormat ifsource = null;
		MessCHDFormat source = null;
		try {
			ifsource = ImageFormat.getImageFormat(selectedfile.getAbsolutePath());
			if (!(ifsource instanceof MessCHDFormat)) {
				JOptionPane.showMessageDialog(m_frmMain, "Not a MESS CHD image file.", "Invalid format error", JOptionPane.ERROR_MESSAGE);				
				return false;
			}
			m_image = (MessCHDFormat)ifsource;
		}
		catch (FileNotFoundException fnfx) {
			JOptionPane.showMessageDialog(m_frmMain, "Input file not found: " + fnfx.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE); 
			return false;
		}
		catch (EOFException ex) {
			JOptionPane.showMessageDialog(m_frmMain, "Not a MESS CHD image file.", "Invalid format error", JOptionPane.ERROR_MESSAGE);				
			return false;
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_frmMain, "IO error: " + iox.getClass().getName(), "Read error", JOptionPane.ERROR_MESSAGE); 
			return false;
		}
		catch (ImageException ix) {
			JOptionPane.showMessageDialog(m_frmMain, "Image error: " + ix.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE); 
			return false;
		}		
		
		// Calculate the size
		int nSize = m_image.getHunkCount() * 0x1000;
		m_jlExportSize.setText(String.valueOf(nSize) + " bytes");
		return true;
	}
	
	MessCHDFormat getCHD() {
		return m_image;
	}
	
	File getTargetFile() {
		return m_targetFile;
	}
}