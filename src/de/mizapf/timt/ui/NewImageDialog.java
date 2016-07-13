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

class NewImageDialog extends ToolDialog {
	
	JTextField 		m_tfName;
	JRadioButton 	m_jrSDF;
	JRadioButton 	m_jrTDF;
	JRadioButton 	m_jrFormatted;
	JRadioButton 	m_jrBlank;
	JRadioButton 	m_jrSingle;
	JRadioButton 	m_jrDouble;
	JComboBox<String> 		m_jcDensity;
	JRadioButton	m_jrTrack40;
	JRadioButton 	m_jrTrack80;
	
	NewImageDialog(JFrame owner) {
		super(owner, "Create new floppy image");
	}
	
/*
	| 	Create new floppy image										|

		Disk name			EMPTY________
		Image type			* Sector Dump 		o Track Dump
		Disk will be		* formatted			o blank
		Sides				o Single			* Double
		Density				|v Double|
		Tracks				* 40				o 80
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	public void createGui() {
		prepareGui();

		m_tfName = putTextField(this, "Disk name", "EMPTY", 100, 100); 
		
		String[] asFormat = { "Sector dump", "Track dump" };
		int[] anFormat = { 100, 100 };

		JRadioButton[] arb1 = putRadioButtons(this, "Image type", 100, asFormat, anFormat, 0);
		m_jrSDF = arb1[0];
		m_jrTDF = arb1[1];

		String[] asDoFormat = { "formatted", "blank" };
		JRadioButton[] arb2 = putRadioButtons(this, "Disk will be", 100, asDoFormat, anFormat, 0);
		m_jrFormatted = arb2[0];
		m_jrBlank = arb2[0];

		String[] asSides = { "single", "double" };
		JRadioButton[] arb3 = putRadioButtons(this, "Sides", 100, asSides, anFormat, 1);
		m_jrSingle = arb3[0];
		m_jrDouble = arb3[1];

		String[] asOptions = { "Single", "Double", "High", "Ultra" };
		m_jcDensity = putComboBox(this, "Density", asOptions, 1, 100);
		
		String[] asTracks = { "40", "80" };
		JRadioButton[] arb4 = putRadioButtons(this, "Tracks", 100, asTracks, anFormat, 0);
		m_jrTrack40 = arb4[0];
		m_jrTrack80 = arb4[1];

		add(Box.createVerticalGlue());

		addButtons();
	}
	
	int getTrackCount() {
		return m_jrTrack40.isSelected()? 40:80;
	}
	
	int getDensity() {
		return (m_jcDensity.getSelectedIndex());
	}
	
	int getSides() {
		return m_jrSingle.isSelected()? 1:2;
	}
	
	boolean formatImage() {
		return m_jrFormatted.isSelected();
	}
	
	String getDiskName() {
		return m_tfName.getText();
	}
	
	boolean sectorDump() {
		return m_jrSDF.isSelected();
	}
}