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
import de.mizapf.timt.TIImageTool;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

class FormatCFDialog extends ToolDialog {

	TIImageTool imagetool;
	JFrame m_parent;
	JTextField m_tfImageFile;
	JTextField m_tfSelection;
	JTextField m_tfNames;
	JLabel m_jlNumber;
	
	FormatCFDialog(JFrame owner, TIImageTool timt) {
		super(owner, "Format CF card");
		imagetool = timt;
		m_parent = owner;
	}	
	
/*
	| 	Format volumes for a CF7 card								|
	
	    This tool modifies an image file that must be written to the CF7 card 
	    with "Write to CF card" afterwards. You have to read the image file
	    from the CF card first, even when it is not yet a CF7-formatted card.
	    
	    Please specify the numbers of the volumes to be formatted. If the
	    volumes are already formatted, they will lose their contents. 
	    
	    You can enter the numbers as a comma-separated list of single numbers or
	    intervals (like 0-2,5,7,10-20). The first volume has number 0. It must
	    be CF7-formatted, or the image format will not be correctly detected.
	    
	    Note that all changes are applied to the image file only. You must
	    write back the image to the CF card later.

		CF7 Image name   _________   [btn]
		
		Maximum number of volumes   _____________  (filled in automatically for existing file)
		
		Volumes to be formatted _________________
		
		Volume names (add "%" for variable width number, or "%%...%" for fixed width number): ____________ 
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	public void createGui(Font font) {

		prepareGui();

		// ======================
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth("Maximum number of volumes");

		putTextLine(this, "!Formatting volumes on a CF7 card", 0);
		add(Box.createVerticalStrut(10));
		putTextLine(this, "This tool modifies an image file that must be written to the CF7 card with \"Write to CF card\" afterwards.", 0);
		putTextLine(this, "You have to read the image file from the CF card first, even when it is not yet a CF7-formatted card.", 0);
		add(Box.createVerticalStrut(10));
		putTextLine(this, "Please specify the numbers of the volumes to be formatted. If the volumes are already formatted, they will",0);
		putTextLine(this, "lose their contents.", 0);
		add(Box.createVerticalStrut(10));
		putTextLine(this, "You can enter the numbers as a comma-separated list of single numbers or intervals (like 0-2,5,7,10-20).", 0);
		putTextLine(this, "The first volume has number 0. It must be CF7-formatted, or the image format will not be correctly detected.", 0);
		add(Box.createVerticalStrut(10));
		putTextLine(this, "Note that all changes are applied to the image file only. You must write back the image to the CF card later.", 0);

		add(Box.createVerticalStrut(30));
		String fileprompt = "File name for CF image";
		m_tfImageFile = new JTextField("click to select");
		addChoiceLine(nColumnWidth, fileprompt, FILELINE, m_tfImageFile, 32);
		add(Box.createVerticalStrut(10));
		
		m_jlNumber = putLabel(this, "Maximum volume number", "-", nColumnWidth);
		
		m_tfSelection = putTextField(this, "Volumes to be formatted", "", nColumnWidth, 0);
		add(Box.createVerticalStrut(10));
		
		putTextLine(this, "For the volume names you can specify a number field using ##...# with the name: VOL### will create names", 0);
		putTextLine(this, "VOL000 for volume 0, VOL001 for volume 1. A single # will create sufficiently many digits for the number.", 0);
		add(Box.createVerticalStrut(10));
		
		m_tfNames = putTextField(this, "Volume names", "", nColumnWidth, 0);
		
		add(Box.createVerticalGlue());
		addButtons();		
	}
}
