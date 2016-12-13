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
import de.mizapf.timt.TIImageTool;

class ExportDialog extends ToolDialog implements ActionListener {
	
	JTextField 		m_tfSuffix;
	JCheckBox		m_chbLower;
	JTextField		m_tfSSubst;	
	JTextField		m_tfSubst;	
	
	ExportDialog(JFrame owner) {
		super(owner, TIImageTool.langstr("ExportTitle"));
	}
	
/*
	| 	Export Parameters									|
		
		Choose the rules for creating file names
		
		File name suffix          [.tfi    ]
		Convert to lowercase      [x]
		
		Character replacement
		Each of these characters [/\*_]
		shall be mapped to  [__x.]
		Use ? as target character to delete the character.
		
		Every non-printable character will be translated to 
		a character sequence of the form =xx.
		
			+-------+			+-----------+
			|	OK	|			|	Cancel	|
			+-------+           +-----------+
*/	
	void createGui(Font font, String translate, Font mono) {
		m_bSet = false;

		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("ExportColumn"));
		add(Box.createVerticalStrut(10));		

		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		putTextLine(this, "!" + TIImageTool.langstr("ExportChoose"), 0);
		add(Box.createVerticalStrut(10));		

		m_tfSuffix = putTextField(this, TIImageTool.langstr("ExportSuffix"), ".tfi", nColumnWidth, 0);
		m_chbLower = putCheckBox(this, TIImageTool.langstr("ExportLower"), true, nColumnWidth);
		add(Box.createVerticalStrut(10));
		
		int separ = translate.indexOf(" ");
		String fromList = translate.substring(0, separ);
		String toList = translate.substring(separ+1);		
		
		m_tfSSubst = putTextField(this, TIImageTool.langstr("ExportReplFrom"), "", nColumnWidth, 0);
		m_tfSSubst.setFont(mono);
		m_tfSSubst.setText(fromList);

		m_tfSubst = putTextField(this, TIImageTool.langstr("ExportReplTo"), "", nColumnWidth, 0); 
		m_tfSubst.setFont(mono);
		m_tfSubst.setText(toList);
		
		add(Box.createVerticalStrut(10));		
		putTextLine(this, TIImageTool.langstr("ExportUse"), 0);
		add(Box.createVerticalStrut(10));

		putMultiTextLine(this, TIImageTool.langstr("ExportEvery"));
		
		add(Box.createVerticalStrut(10));
		add(Box.createVerticalGlue());

		addButtons();
	}
	
	String getSuffix() {
		return m_tfSuffix.getText().trim();
	}

	boolean convertToLower() {
		return m_chbLower.isSelected();
	}
	
	String getSubstSource() {
		return m_tfSSubst.getText().trim();
	}
	
	String getSubstTarget() {
		return m_tfSubst.getText().trim();
	}
}
