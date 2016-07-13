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
import java.util.*;
import java.awt.font.*;

class NewDirectoryDialog extends ToolDialog {
	
	JTextField 		m_tfFileName;
	
	NewDirectoryDialog(JFrame owner) {
		super(owner, "New directory");
	}
	
/*
	| 	New directory             								|
		
		Directory name [ ... ]
		
			+-------+			+-----------+
			|	OK	|			|	Cancel	|
			+-------+           +-----------+
*/	
	void createGui(Font font) {
		prepareGui();
		determineWidth("Directory name:");
		
		m_tfFileName = new JTextField();
		m_tfFileName.setText("");
		addLine("Directory name", m_tfFileName);
		addButtons();	
	}
		
	String getDirName() {
		return m_tfFileName.getText().trim();
	}
}
