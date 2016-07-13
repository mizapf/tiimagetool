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
import de.mizapf.timt.files.FormatParameters;
import de.mizapf.timt.files.Time;

class CHDRawDialog extends ToolDialog {

	JLabel 				m_jlImageFile;
	JLabel				m_jlExportSize;
	
	CHDRawDialog(JFrame owner) {
		super(owner, "Extract raw contents from a CHD image");
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
		int nColumnWidth = fm.stringWidth("Export size will be XXX");
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));		

		m_jlImageFile = putLabel(this, "Image file", "-", nColumnWidth);
		m_jlExportSize = putLabel(this, "Export size will be ", "0", nColumnWidth);

		add(Box.createVerticalStrut(10));	

		putTextLine(this, "Next: Select the name of a file where to store the contents.", 400);

		addButtons();
	}
	
	void setImageFile(String sFileName) {
		if (sFileName == null) sFileName = "-"; 
		m_jlImageFile.setText(sFileName);
	}
	
	void setImageSize(String sSize) {
		m_jlExportSize.setText(sSize);		
	}
}