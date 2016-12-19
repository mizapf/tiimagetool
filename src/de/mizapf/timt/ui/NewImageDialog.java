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

class NewImageDialog extends ToolDialog {
	
	JTextField 		m_tfName;
	JComboBox<String> 		m_jcType;
	JRadioButton 	m_jrFormatted;
	JRadioButton 	m_jrBlank;
	JRadioButton 	m_jrSingle;
	JRadioButton 	m_jrDouble;
	JComboBox<String> 		m_jcDensity;
	JRadioButton	m_jrTrack40;
	JRadioButton 	m_jrTrack80;
	
	public final static String[] suffix = { ".dsk", ".dtk", ".hfe" };
	
	NewImageDialog(JFrame owner) {
		super(owner, TIImageTool.langstr("NewImageFloppy"));
	}
	
/*
	| 	Create new floppy image										|

		Disk name			EMPTY________
		Image type			|v Sector Dump|
		Disk will be		* formatted			o blank
		Sides				o Single			* Double
		Density				|v Double|
		Tracks				* 40				o 80
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	public void createGui(Font font) {
		prepareGui();
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("NewImageColumn"));

		m_tfName = putTextField(this,  TIImageTool.langstr("VolumeName"), "EMPTY", nColumnWidth, 100); 
		
		String[] asFormat = { TIImageTool.langstr("SectorDump"), TIImageTool.langstr("TrackDump"), TIImageTool.langstr("HFEImage") };
		m_jcType = putComboBox(this, TIImageTool.langstr("ImageType"), asFormat, 0, nColumnWidth);
		
		int[] anFormat = { 100, 100 };
		
		String[] asDoFormat = { TIImageTool.langstr("Formatted"), TIImageTool.langstr("Blank") };
		JRadioButton[] arb2 = putRadioButtons(this, TIImageTool.langstr("NewImageWillBe"), nColumnWidth, asDoFormat, anFormat, 0);
		m_jrFormatted = arb2[0];
		m_jrBlank = arb2[1];

		String[] asSides = { TIImageTool.langstr("SingleSided"), TIImageTool.langstr("DoubleSided") };
		JRadioButton[] arb3 = putRadioButtons(this, TIImageTool.langstr("NewImageSides"), nColumnWidth, asSides, anFormat, 1);
		m_jrSingle = arb3[0];
		m_jrDouble = arb3[1];

		String[] asOptions = { TIImageTool.langstr("SingleDensity"), TIImageTool.langstr("DoubleDensity"), TIImageTool.langstr("HighDensity"), TIImageTool.langstr("UltraDensity") };
		m_jcDensity = putComboBox(this, TIImageTool.langstr("NewImageDensity"), asOptions, 1, nColumnWidth);
		
		String[] asTracks = { "40", "80" };
		JRadioButton[] arb4 = putRadioButtons(this, TIImageTool.langstr("NewImageTracks"), nColumnWidth, asTracks, anFormat, 0);
		m_jrTrack40 = arb4[0];
		m_jrTrack80 = arb4[1];

		add(Box.createVerticalGlue());

		addButtons();
	}
	
	int getTrackCount() {
		return m_jrTrack40.isSelected()? 40:80;
	}
	
	int getDensity() {
		switch (m_jcDensity.getSelectedIndex()) {
			case 0: return ImageFormat.SINGLE_DENSITY;
			case 1: return ImageFormat.DOUBLE_DENSITY;
			case 2: return ImageFormat.HIGH_DENSITY;
			case 3: return ImageFormat.ULTRA_DENSITY;
		}				
		return -1;
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
	
	int getImageType() {
		switch (m_jcType.getSelectedIndex()) {
			case 0: return ImageFormat.SECTORDUMP;
			case 1: return ImageFormat.TRACKDUMP;
			case 2: return ImageFormat.HFE;
		}
		return -1;
	}
	
	String getImageTypeSuffix() {
		return suffix[m_jcType.getSelectedIndex()];
	}
}