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
import java.awt.event.*;
import javax.swing.event.*;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

class ChangeCHDFormatDialog extends ToolDialog {

	JLabel 				m_jlSourceImage;
	JLabel				m_jlCurrentFormat;
	JComboBox<String>	m_jcCHDVersion;
	
	ChangeCHDFormatDialog(JFrame owner) {
		super(owner, TIImageTool.langstr("ConvertCHDVersion"));
	}
	
/*
	| 	Change CHD Format										|

		Convert a CHD image file to another CHD version.
	
		Image file:       [...]
		Current version:  xxx
		
		Convert to version:      [v 5]
	
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	public void createGui(Font font) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("ChangeCHDColumn"));

		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		add(Box.createVerticalStrut(10));		
		add(Box.createHorizontalStrut(300));		

		putTextLine(this, "!" + TIImageTool.langstr("ConvertTitle"), 0);

		add(Box.createVerticalStrut(10));
		
		m_jlSourceImage = putLabel(this, TIImageTool.langstr("ImageFile"), "-", nColumnWidth);  
		m_jlCurrentFormat = putLabel(this, TIImageTool.langstr("CurrentVersion"), "-", nColumnWidth);

		add(Box.createVerticalStrut(10));

		String[] asOptio = { "4", "5" };
		m_jcCHDVersion = putComboBox(this, TIImageTool.langstr("ConvertTo"), asOptio, 1, nColumnWidth);
		
		add(Box.createVerticalStrut(10));
		add(Box.createVerticalGlue());

		addButtons();
	}
		
	void setFileName(String sFileName) {
		m_jlSourceImage.setText(sFileName);
	}
	
	void setSourceVersion(int nVersion) {
		m_jlCurrentFormat.setText(String.valueOf(nVersion));
	}
	
	int getNewFormat() {
		return Integer.parseInt((String)m_jcCHDVersion.getSelectedItem());
	}
}