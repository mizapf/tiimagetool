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

class ExportDialog extends ToolDialog implements ActionListener {
	
	JTextField 		m_tfSuffix;
	JCheckBox		m_chbLower;
	JTextField		m_tfSSubst;	
	JTextField		m_tfSubst;	
	
	ExportDialog(JFrame owner) {
		super(owner, "Export parameters");
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
		int nColumnWidth = fm.stringWidth("Each of these characters");
		int nFullWidth = fm.stringWidth("Every non-printable character will be translated to =XX,");
		int nFieldWidth = fm.stringWidth("XXXXX");
		
		add(Box.createVerticalStrut(10));		

		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		putTextLine(this, "Choose the rules for creating file names", 0);
		add(Box.createVerticalStrut(10));		

		m_tfSuffix = putTextField(this, "File name suffix", ".tfi", nColumnWidth, nFieldWidth);
		m_chbLower = putCheckBox(this, "Convert to lowercase", true, nColumnWidth);
		add(Box.createVerticalStrut(10));
		
		putTextLine(this, "Character replacement", 0);
		add(Box.createVerticalStrut(10));		

		int separ = translate.indexOf(" ");
		String fromList = translate.substring(0, separ);
		String toList = translate.substring(separ+1);		
		
		m_tfSSubst = putTextField(this, "Each of these characters", "", nColumnWidth, nFieldWidth);
		m_tfSSubst.setFont(mono);
		m_tfSSubst.setText(fromList);

		m_tfSubst = putTextField(this, "shall be mapped to", "", nColumnWidth, nFieldWidth); 
		m_tfSubst.setFont(mono);
		m_tfSubst.setText(toList);
		
		add(Box.createVerticalStrut(10));		
		putTextLine(this, "Use ? as target character to delete the character.", 0);
		add(Box.createVerticalStrut(10));

		putTextLine(this, "Every non-printable character will be translated to ", 0);
		putTextLine(this, "a character sequence of the form =xx.", 0);
		
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
