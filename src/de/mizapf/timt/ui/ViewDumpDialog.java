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
import java.awt.event.*;
import de.mizapf.timt.TIImageTool;

class ViewDumpDialog extends ToolDialog implements ActionListener {
	
	JCheckBox		m_chbHeader;
	JTextField		m_tfStart;	
	
	ViewDumpDialog(JFrame owner) {
		super(owner, TIImageTool.langstr("ViewPlain"));
	}
	
/*
	| 	View as Binary File									|
		
		Use header [ ]
 		Start address [0000]
		
			+-------+			+-----------+
			|	OK	|			|	Cancel	|
			+-------+           +-----------+
*/	
	void createGui(Font font, Font mono) {
		m_bSet = false;

		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("ViewDumpPrompt"));
		add(Box.createVerticalStrut(10));		

		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		m_chbHeader = putCheckBox(this, TIImageTool.langstr("ViewDumpHeader"), false, nColumnWidth);
		m_tfStart = putTextField(this, TIImageTool.langstr("ViewDumpPrompt"), "0000", nColumnWidth, 0);
		add(Box.createVerticalStrut(10));
		
		add(Box.createVerticalGlue());

		addButtons();
	}
	
	String getStartAddress() {
		return m_tfStart.getText().trim();
	}

	boolean useHeader() {
		return m_chbHeader.isSelected();
	}
}
