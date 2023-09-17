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
    
    Copyright 2020 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import java.io.*;

import java.awt.event.KeyEvent;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.files.ImageFormat;
import de.mizapf.timt.files.FormatParameters;

public class SaveAsImageDialog  extends ToolDialog {
	
	JRadioButton[] m_rbt;
	int m_nProposedType;
	boolean m_bFloppy;

	int[] m_types = null;
	int[] floppytypes = { ImageFormat.SECTORDUMP, ImageFormat.TRACKDUMP, ImageFormat.HFE };
	int[] hdtypes = { ImageFormat.CHD, ImageFormat.RAWHD }; 
	
/*
        | Save floppy image                            |
        
			Image type			* Sector Dump (normal DSK)
			                    O Track Dump (PC99-compatible)
			                    O HFE (Lotharek/GoTek)
			
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	
	SaveAsImageDialog(JFrame owner, int nProposedType, boolean bFloppy) {
		super(owner, TIImageTool.langstr("Save"));
		m_nProposedType = nProposedType;
		m_bFloppy = bFloppy;
	}
	
	public void createGui(Font font) {
		prepareGui();	
		int nColumnWidth = determineFieldWidth(TIImageTool.langstr("ImageType"));

		String[] asFor;
		JRadioButton[] arb = null;
		int index = 0;
		
		if (m_bFloppy) {
			asFor = new String[3];
			asFor[0] = TIImageTool.langstr("SectorDump");
			asFor[1] = TIImageTool.langstr("TrackDump");
			asFor[2] = TIImageTool.langstr("HFEImage");
			m_rbt = putRadioButtons(this, TIImageTool.langstr("ImageType"), nColumnWidth, asFor, null, 1);
			m_types = floppytypes;
		}
		else {
			asFor = new String[2];
			asFor[0] = TIImageTool.langstr("CHDImageType");
			asFor[1] = TIImageTool.langstr("RAWType");			
			m_rbt = putRadioButtons(this, TIImageTool.langstr("ImageType"), nColumnWidth, asFor, null, 1);
			m_types = hdtypes;
		}
			
		if (m_nProposedType != -1) {
			for (index = 0; index < asFor.length; index++) {
				if (m_types[index] == m_nProposedType) break;
			}
			if (index == asFor.length) {				
				index = 0;
				// System.out.println("Invalid type proposed: " + m_nProposedType);
			}
		}
		else {
			System.out.println("New image, no proposed type");
		}
	
		m_rbt[index].setSelected(true);
		add(Box.createVerticalGlue());
		addButtons();	
	}
	
	int getImageType() {
		for (int i=0; i < m_types.length; i++)
			if (m_rbt[i].isSelected()) return m_types[i];
		return -1;
	}
}

