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

import javax.swing.*;
import de.mizapf.timt.TIImageTool;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;

class SearchDialog extends ToolDialog {

	JCheckBox m_chbRegex;
	JCheckBox m_chbSubdir;
	JCheckBox m_chbArchives;
	JTextField m_tfSearchString;
	JRadioButton m_jrSearchFile;
	JRadioButton m_jrSearchContent;
	JRadioButton m_jrSearchDate;
	JButton m_btnFileChooser;
	TIImageTool imagetool;
	JFrame m_parent;
	JTextField m_tfPath;
	File[] m_selectedFiles;
	JTextField m_tfValidExtensions;
	JTextField m_tfMaxHits;
		
	SearchDialog(JFrame owner, TIImageTool timt) {
		super(owner, TIImageTool.langstr("SearchTitle"));
		imagetool = timt;
		m_parent = owner;
	}	
	
	public void createGui(Font font) {
/*
	| 	Search files										|

		Search text			___________
		Regular expression [ ]
		
		Search file names    (x)   file contents ( )   file dates ( ) 
		
		Path to search     [.....] [btn]
		Search subdirectories ( )
	    Include archives in image [ ]
		Limit to extensions [...]
		
		Maximum hits [...]
				
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
		prepareGui();
	//	FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = getColumnWidth(30);
		
		m_tfSearchString = putTextField(this, TIImageTool.langstr("SearchText"), "", nColumnWidth, 0); 

		m_chbRegex = putCheckBox(this, TIImageTool.langstr("SearchRegex"), false, nColumnWidth);
		add(Box.createVerticalStrut(10));
		
		int[] anFormat = { 100, 100, 100 };
		
		String[] asDoSearch = { TIImageTool.langstr("SearchNames"), TIImageTool.langstr("SearchContent"), TIImageTool.langstr("SearchDate") };
		JRadioButton[] arb2 = putRadioButtons(this, TIImageTool.langstr("SearchFor"), nColumnWidth, asDoSearch, anFormat, 0);
		m_jrSearchFile = arb2[0];
		m_jrSearchContent = arb2[1];
		m_jrSearchDate = arb2[2];

		m_chbArchives = putCheckBox(this, TIImageTool.langstr("SearchArchives"), false, nColumnWidth);		

		//
		JComponent[] comp = new JComponent[2];
		Box pathSelect = createPathSelectBox(comp, TIImageTool.langstr("SearchPath"), 
										TIImageTool.langstr("ClickToSelect"),
										nColumnWidth); 

		m_btnFileChooser = (JButton)comp[0];
		m_tfPath = (JTextField)comp[1];
		m_btnFileChooser.addActionListener(this);
		add(pathSelect);
			
		m_tfValidExtensions = putTextField(this, TIImageTool.langstr("SearchLimit"), ".dsk,.dtk,.hfe,.hd", nColumnWidth, 0); 
		m_chbSubdir = putCheckBox(this, TIImageTool.langstr("SearchSubdir"), false, nColumnWidth);
		m_tfMaxHits = putTextField(this, TIImageTool.langstr("SearchMax"), "1000", nColumnWidth, 0); 
		
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
			jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			jfc.setMultiSelectionEnabled(true);
			
			Dimension dim = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			
			int nReturn = jfc.showOpenDialog(m_parent);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				m_selectedFiles = jfc.getSelectedFiles();
				if (m_selectedFiles.length > 0) {
					StringBuilder sb = new StringBuilder();
					for (File f : m_selectedFiles) {
						sb.append(f.getName());
						if (f.isDirectory()) sb.append("/");
						sb.append(";");
					}
					sb.deleteCharAt(sb.length()-1);
					m_tfPath.setText(sb.toString());
				}
				else {
					System.out.println(TIImageTool.langstr("SearchNoFilesDir"));
					m_tfPath.setText("");
				}
			}
			else {
				m_selectedFiles = null;
			}
		}
	}
	
	String getSearchString() {
		return m_tfSearchString.getText();
	}
	
	boolean isRegex() {
		return m_chbRegex.isSelected();
	}
	
	boolean searchInsideFiles() {
		return m_jrSearchContent.isSelected();
	}

	boolean searchFileNames() {
		return m_jrSearchFile.isSelected();
	}

	boolean searchFileDate() {
		return m_jrSearchDate.isSelected();
	}

	File[] getSelectedFiles() {
		return m_selectedFiles;
	}
	
	boolean searchSubdirectories() {
		return m_chbSubdir.isSelected();
	}

	int getMaximumHits() throws NumberFormatException {
		return Integer.parseInt(m_tfMaxHits.getText());
	}
	
	boolean searchArchives() {
		return m_chbArchives.isSelected();
	}
	
	String[] getValidExtensions() {
		String sValid = m_tfValidExtensions.getText();
		String[] ext = null;
		ArrayList<String> list = new ArrayList<String>();
		
		if (sValid != null && sValid.length()>0) {
			ext = sValid.split(",");
		}
		if (ext != null) {
			for (int i=0; i < ext.length; i++) {
				String extn = ext[i].trim();
				if (extn.length()>0) list.add(extn);
			}
		}
		String[] res = new String[list.size()];
		list.toArray(res);
		return res;
	}	
}
