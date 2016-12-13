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

class ArchiveDialog extends ToolDialog implements ActionListener {
	
	JTextField 		m_tfFilename;
	JCheckBox		m_chbCompression;
	
	boolean m_bSet = false;
	
	ArchiveDialog(JFrame owner) {
		super(owner, TIImageTool.langstr("CreateArchive"));
	}
	
/*
	| 	Archive Parameters									|
		
		Archive file name         [.tfi    ]
		LZW compression		      [x]
		
			+-------+			+-----------+
			|	OK	|			|	Cancel	|
			+-------+           +-----------+
*/	
	void createGui(Font font, String sFirstFilename) {
		m_bSet = false;

		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nHeight = fm.getHeight();
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("ArchiveDialogColumn"));
		int nColumnWidth1 = fm.stringWidth("XXXXXXXXXXXX");
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		add(Box.createVerticalStrut(10));		
				
		String sProposed = sFirstFilename;
		if (sProposed.length()>6) sProposed = sProposed.substring(0,6);
		m_tfFilename = putTextField(this, TIImageTool.langstr("ArchiveFileName"), sProposed + "_ARK", nColumnWidth, nColumnWidth1);
			
		m_chbCompression = putCheckBox(this, TIImageTool.langstr("LZWComp"), true, nColumnWidth);
		add(Box.createVerticalStrut(10));

		add(Box.createVerticalGlue());

		addButtons();
	}

	
	String getArchiveName() {
		return m_tfFilename.getText().trim();
	}

	boolean useCompression() {
		return m_chbCompression.isSelected();
	}
}
