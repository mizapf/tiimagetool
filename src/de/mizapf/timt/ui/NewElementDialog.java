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
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.font.*;

import de.mizapf.timt.TIImageTool;

class NewElementDialog extends ToolDialog {
	
	JTextField	m_tfElementName;
	String 		m_prompt;
	String 		m_default;
	String		m_hint;
	
	NewElementDialog(JFrame owner, String prompt, String def, String hint) {
		super(owner, TIImageTool.langstr("NewElementTitle"));
		m_prompt = prompt;
		m_default = def;
		m_hint = hint;
	}
	
/*
	| 	XXX             								|
		
		XXXXX [ ... ]
		
		XXXXXXXXXXXXXXXXXXXXXXX
		
			+-------+			+-----------+
			|	OK	|			|	Cancel	|
			+-------+           +-----------+
*/	
	void createGui() {
		prepareGui();
		int nLabelWidth = determineFieldWidth(m_prompt);

		m_tfElementName = putTextField(this, m_prompt, "", nLabelWidth, 0);
		m_tfElementName.setText(m_default);
		add(Box.createVerticalStrut(10));
		if (m_hint != null) {	
			putTextLine(this, m_hint, 0);
			add(Box.createVerticalStrut(10));
		}
		else {
			add(Box.createHorizontalStrut(300));			
		}
		m_tfElementName.addKeyListener(this);
		addButtons();	
	}
		
	String getElementName() {
		return m_tfElementName.getText().trim();
	}
}
