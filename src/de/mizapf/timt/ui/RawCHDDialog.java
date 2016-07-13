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
    
    Copyright 2013 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import de.mizapf.timt.files.FormatParameters;
import de.mizapf.timt.files.Time;

class RawCHDDialog extends ToolDialog {

	JLabel				m_jlRawFile;
	JComboBox<String>	m_jcCHDVersion;
	JCheckBox			m_chbFillZero;	
	
	JLabel				m_jlFileSystem;
	JLabel				m_jlCylinders;
	JLabel				m_jlHeads;
	JLabel				m_jlSectorsPerTrack;
	JLabel				m_jlSectorLength;
	
	RawCHDDialog(JFrame owner) {
		super(owner, "Create a new CHD image file from raw contents");
	}
	
/*
	| 	Import raw										|

		Create a new CHD image file from raw contents.
	
		Raw file:       				  [...]
		Target CHD version: 			  [v 5]
		Fill unallocated space with zeros [x]
		
		Target parameters:
		File system:					  ...
		Cylinders:						  ...
		Heads:							  ...
		Sectors per track:                ...
		Sector length:                    ...

		Next: Select the file name for the target CHD image.
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
				
*/	
	public void createGui(Font font) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth("Fill unallocated space with zerosXXXX");

		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));		

		m_jlRawFile = putLabel(this, "Raw file", "-", nColumnWidth);
		
		String[] asOptions = { "4", "5" };
		m_jcCHDVersion = putComboBox(this, "Target CHD version", asOptions, 1, nColumnWidth);
		m_chbFillZero = putCheckBox(this, "Fill unallocated space with zeros", true, nColumnWidth);
		add(Box.createVerticalStrut(15));
		
		putTextLine(this, "Target parameters:", 300);
		add(Box.createVerticalStrut(5));
		m_jlFileSystem = putLabel(this, "File system", "-",nColumnWidth);
		m_jlCylinders = putLabel(this, "Cylinders", "-", nColumnWidth);
		m_jlHeads = putLabel(this, "Heads", "-", nColumnWidth);
		m_jlSectorsPerTrack = putLabel(this, "Sectors per track", "-", nColumnWidth);
		m_jlSectorLength = putLabel(this, "Sector length", "-", nColumnWidth);
		add(Box.createVerticalStrut(10));

		putTextLine(this, "Next: Select a file name for the target CHD image.", 300); 
		
		add(Box.createVerticalStrut(10));
		
		addButtons();
	}
	
	void setRawFile(String sFileName) {
		if (sFileName == null) sFileName = "-"; 
		m_jlRawFile.setText(sFileName);		
	}
	
	void setFileSystem(String sFS) {
		if (sFS == null) sFS = "-"; 
		m_jlFileSystem.setText(sFS);
	}
	
	void setCylinders(int nCylinders) {
		m_jlCylinders.setText(String.valueOf(nCylinders));
	}
	
	void setHeads(int nHeads) {
		m_jlHeads.setText(String.valueOf(nHeads));
	}
	
	void setSectorsPerTrack(int nSectors) {
		m_jlSectorsPerTrack.setText(String.valueOf(nSectors));
	}	
	
	void setSectorLength(int nSectors) {
		m_jlSectorLength.setText(String.valueOf(nSectors));
	}	

	boolean fillWithZeros() {
		return m_chbFillZero.isSelected();
	}
	
	int getCHDVersion() {
		int[] aVer = { 4, 5 };
		return aVer[m_jcCHDVersion.getSelectedIndex()];
	}
}