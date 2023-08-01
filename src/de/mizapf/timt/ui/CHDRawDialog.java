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
	
	MameCHDFormat		m_image;
	File				m_targetFile;
	
	private final static int FROM = 1;
	private final static int TO = 2;
	
	CHDRawDialog(JFrame owner, TIImageTool timt) {
		super(owner, timt.langstr("ExtractRawLong"));
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
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("CHDRawColumn"));
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));		

		putTextLine(this, "!" + TIImageTool.langstr("ExtractTitle"), 0);
		add(Box.createVerticalStrut(10));
		putMultiTextLine(this, TIImageTool.langstr("ExtractText"));
		add(Box.createVerticalStrut(10));
		
		String fileprompt = TIImageTool.langstr("FromCHD");
		String rawprompt = TIImageTool.langstr("ToImage");
		m_tfImageFile = new JTextField(TIImageTool.langstr("ClickToSelect"));
		m_tfRawFile = new JTextField(TIImageTool.langstr("ClickToSelect"));

		addChoiceLine(nColumnWidth, fileprompt, FILELINE, FROM, m_tfImageFile, 32);
		add(Box.createVerticalStrut(10));
		addChoiceLine(nColumnWidth, rawprompt, FILELINE, TO, m_tfRawFile, 32);
		add(Box.createVerticalStrut(10));

		m_jlExportSize = putLabel(this, TIImageTool.langstr("ExportSize"), "0", nColumnWidth);
				
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
			
			Dimension dim = settings.getPropertyDim(imagetool.FILEDIALOG);
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
				settings.put(imagetool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
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
			
			Dimension dim = settings.getPropertyDim(imagetool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			jfc.setMultiSelectionEnabled(false);
			
			int nReturn = jfc.showSaveDialog(m_frmMain);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				m_targetFile = jfc.getSelectedFile();
				m_tfRawFile.setText(m_targetFile.getAbsolutePath());
				m_validTarget = true;
				File filePar = m_targetFile.getParentFile();
				if (!filePar.getName().equals(".")) imagetool.setSourceDirectory(filePar, "image");  // sets the property only for non-UNC paths
				settings.put(imagetool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
			}
		}
	}
	
	private boolean checkCHD(File selectedfile) {
		String sImageFile = selectedfile.getName();
		ImageFormat ifsource = null;
		MameCHDFormat source = null;
		try {
			ifsource = ImageFormat.determineImageFormat(selectedfile.getAbsolutePath());
			if (!(ifsource instanceof MameCHDFormat)) {
				JOptionPane.showMessageDialog(m_frmMain, TIImageTool.langstr("NotCHD"), TIImageTool.langstr("InvalidFormat"), JOptionPane.ERROR_MESSAGE);				
				return false;
			}
			m_image = (MameCHDFormat)ifsource;
		}
		catch (FileNotFoundException fnfx) {
			JOptionPane.showMessageDialog(m_frmMain, TIImageTool.langstr("InputNotFound") + ": " + fnfx.getMessage(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
			return false;
		}
		catch (EOFException ex) {
			JOptionPane.showMessageDialog(m_frmMain, TIImageTool.langstr("NotCHD"), TIImageTool.langstr("InvalidFormat"), JOptionPane.ERROR_MESSAGE);				
			return false;
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_frmMain, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
			return false;
		}
		catch (ImageException ix) {
			JOptionPane.showMessageDialog(m_frmMain, TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
			return false;
		}		
		
		// Calculate the size
		int nSize = m_image.getHunkCount() * 0x1000;
		m_jlExportSize.setText(String.valueOf(nSize) + " " + TIImageTool.langstr("Bytes"));
		return true;
	}
	
	MameCHDFormat getCHD() {
		return m_image;
	}
	
	File getTargetFile() {
		return m_targetFile;
	}
}