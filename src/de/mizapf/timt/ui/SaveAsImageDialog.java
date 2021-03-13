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
import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import java.io.*;

import java.awt.event.KeyEvent;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.files.ImageFormat;

public class SaveAsImageDialog  extends ToolDialog {
	
	JRadioButton[] m_rbt;
	int m_nProposedType;
	
/*
        | Save floppy image                            |
        
			Image type			* Sector Dump (normal DSK)
			                    O Track Dump (PC99-compatible)
			                    O HFE (Lotharek/GoTek)
			
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	
	SaveAsImageDialog(JFrame owner, int nProposedType) {
		super(owner, TIImageTool.langstr("Save"));
		m_nProposedType = nProposedType;
	}
	
	public void createGui(Font font) {
		prepareGui();	
		int nColumnWidth = determineFieldWidth(TIImageTool.langstr("ImageType"));

		String[] asFor = new String[3];
		JRadioButton[] arb = null;

		asFor[0] = TIImageTool.langstr("SectorDump");
		asFor[1] = TIImageTool.langstr("TrackDump");
		asFor[2] = TIImageTool.langstr("HFEImage");
		m_rbt = putRadioButtons(this, TIImageTool.langstr("ImageType"), nColumnWidth, asFor, null, 1);
			
		if (m_nProposedType != -1) {
			System.out.println("Proposed type: " + m_nProposedType);
			m_rbt[m_nProposedType].setSelected(true);
		}
		else {
			System.out.println("New image, no proposed type");
			m_rbt[0].setSelected(true);
		}
		
		add(Box.createVerticalGlue());
		addButtons();	
	}
	
	int getImageType() {
		int itype[] = { ImageFormat.SECTORDUMP, ImageFormat.TRACKDUMP, ImageFormat.HFE };
		for (int i=0; i < 3; i++)
			if (m_rbt[i].isSelected()) return itype[i];
		return -1;
	}
	
	/*	
	JTextField 		m_tfName;
	JComboBox<String> 		m_jcType;
	JButton m_btnFileChooser;
	JTextField m_tfPath;
	File m_selectedFile;
	TIImageTool imagetool;
	JFrame m_parent;
	String m_sProposedName;
	
	public final static String[] suffix = { ".dsk", ".dtk", ".hfe" };
	
	SaveAsImageDialog(JFrame owner, TIImageTool timt, String sProposedName) {
		super(owner, TIImageTool.langstr("Save"));
		imagetool = timt;
		m_parent = owner;
		m_sProposedName = sProposedName;
	}
	
	public void createGui(Font font) {
		prepareGui();	
		//
		int nColumnWidth = getColumnWidth(25);

		String[] asFormat = { TIImageTool.langstr("SectorDump"), TIImageTool.langstr("TrackDump"), TIImageTool.langstr("HFEImage") };
		m_jcType = putComboBox(this, TIImageTool.langstr("ImageType"), asFormat, 0, nColumnWidth);
		
		JComponent[] comp = new JComponent[2];
		Box pathSelect = createPathSelectBox(comp, TIImageTool.langstr("FileName"), 
										getSuffixedName(m_sProposedName), nColumnWidth); 

		m_btnFileChooser = (JButton)comp[0];
		m_tfPath = (JTextField)comp[1];
		m_btnFileChooser.addActionListener(this);
		add(pathSelect);
		
		add(Box.createVerticalGlue());

		addButtons();		
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==m_btnOK) {
			m_bSet = true;
			dispose();
		}
		if (ae.getSource()==m_btnCancel) {
			m_bSet = false;
			dispose();
		}
		if (ae.getSource()==m_btnFileChooser) {
			JFileChooser jfc = null;
			if (imagetool.getSourceDirectory("image")!=null) {
				jfc = new JFileChooser(imagetool.getSourceDirectory("image"));
			}
			else jfc = new JFileChooser();
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			jfc.setMultiSelectionEnabled(false);

			ImageFileFilter im = new ImageFileFilter();
			jfc.addChoosableFileFilter(im);
			jfc.setFileFilter(im);	
			
			Dimension dim = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			
			int nReturn = jfc.showOpenDialog(m_parent);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				File select = jfc.getSelectedFile();
				if (select != null) {
					m_selectedFile = new File(getSuffixedName(select.getAbsolutePath()));
					m_tfPath.setText(m_selectedFile.getAbsolutePath());
				}
				else {
					m_tfPath.setText("");
				}
			}
			else {
				m_selectedFile = null;
			}
		}

	}
	
	String getImageTypeSuffix() {
		return suffix[m_jcType.getSelectedIndex()];
	}
	
	File getSelectedFile() {
		if (m_selectedFile == null) {
			m_selectedFile = new File(getSuffixedName(m_sProposedName));
		}
		return m_selectedFile;
	}
	
	String getSuffixedName(String sName) {
		int nSuffixPos = sName.indexOf(".");
		if (nSuffixPos==-1 || nSuffixPos == sName.length()-1) {
			if (!sName.endsWith(getImageTypeSuffix())) {
				sName = sName + getImageTypeSuffix();
			}
		}
		return sName;
	}
	
	int getImageType() {
		switch (m_jcType.getSelectedIndex()) {
			case 0: return ImageFormat.SECTORDUMP;
			case 1: return ImageFormat.TRACKDUMP;
			case 2: return ImageFormat.HFE;
		}
		return -1;	
	}
	*/
}

/*
        | Save floppy image                            |
        
			Image type			|v Sector Dump|
			File name           [icon] [ path/empty1.dsk ]
			
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	

