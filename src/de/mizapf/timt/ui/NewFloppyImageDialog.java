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

import de.mizapf.timt.files.ImageFormat;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.files.FormatParameters;
import de.mizapf.timt.files.FloppyFileSystem;

class NewFloppyImageDialog extends ToolDialog {
	
	JTextField 		m_tfName;
	JTextField		m_tfFill;

	JRadioButton 	m_jrSingle;
	JRadioButton 	m_jrDouble;

	JComboBox<String> m_jcDensity;

	JRadioButton	m_jrTrack40;
	JRadioButton 	m_jrTrack80;
		
	NewFloppyImageDialog(JFrame owner) {
		super(owner, TIImageTool.langstr("NewImageFloppy"));
	}
	
/*		
	| 	Create new floppy image										|

		Volume name			    EMPTY________
		Sides				    o Single			* Double
		Density				    |v Double|
		Tracks				    * 40				o 80
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+

*/	
	public void createGui(Font font) {
		prepareGui();
	
		int nColumnWidth = determineFieldWidth(TIImageTool.langstr("NewImageFill"));
		int rbutwidth = determineFieldWidth(TIImageTool.langstr("NewImageRadioColumn"));
		int[] anFormat = new int[2];
		anFormat[0] = rbutwidth;
		anFormat[1] = rbutwidth;

		// Volume name [ ... ]
		m_tfName = putTextField(this,  TIImageTool.langstr("VolumeName"), "EMPTY", nColumnWidth, 0); 
		add(vspace(80));		
		
		// Sides
		String[] asSides = { TIImageTool.langstr("SingleSided"), TIImageTool.langstr("DoubleSided") };
		JRadioButton[] arb3 = putRadioButtons(this, TIImageTool.langstr("NewImageSides"), nColumnWidth, asSides, anFormat, 1);
		m_jrSingle = arb3[0];
		m_jrDouble = arb3[1];
		add(vspace(40));	
		
		// Density
		String[] asOptions = { 	TIImageTool.langstr("SingleDensity"), 
								TIImageTool.langstr("DoubleDensity"), 
								TIImageTool.langstr("HighDensity"), 
								TIImageTool.langstr("UltraDensity"),
								TIImageTool.langstr("DoubleDensity16")
		};
		m_jcDensity = putComboBox(this, TIImageTool.langstr("NewImageDensity"), asOptions, 1, nColumnWidth);
		add(vspace(40));	

		// Tracks
		String[] asTracks = { "40", "80" };
		JRadioButton[] arb4 = putRadioButtons(this, TIImageTool.langstr("NewImageTracks"), nColumnWidth, asTracks, anFormat, 0);
		m_jrTrack40 = arb4[0];
		m_jrTrack80 = arb4[1];
		
		add(Box.createVerticalGlue());
		m_tfName.addKeyListener(this);

		addButtons();
	}
	
	int getTrackCount() {
		return m_jrTrack40.isSelected()? 40:80;
	}
	
	int getDensity() {
		switch (m_jcDensity.getSelectedIndex()) {
			case 0: return FloppyFileSystem.SINGLE_DENSITY;
			case 1: return FloppyFileSystem.DOUBLE_DENSITY;
			case 2: return FloppyFileSystem.HIGH_DENSITY;
			case 3: return FloppyFileSystem.ULTRA_DENSITY;
			case 4: return FloppyFileSystem.DOUBLE_DENSITY_16;
		}				
		return -1;
	}
	
	int getSides() {
		return m_jrSingle.isSelected()? 1:2;
	}
	
	String getDiskName() {
		return m_tfName.getText();
	}
	
	FormatParameters getParameters() {
		FormatParameters params = new FormatParameters(getDiskName(), true);
		params.setCHS(getTrackCount(), getSides(), FloppyFileSystem.getSectorsFromDensity(getDensity())); 
		return params;
	}
}