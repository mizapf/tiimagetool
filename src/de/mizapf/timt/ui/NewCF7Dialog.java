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

class NewCF7Dialog extends ToolDialog {
	
	JTextField 		m_tfVolumeName;
	boolean   m_fullImage;
	
	NewCF7Dialog(JFrame owner, boolean fullImage) {
		super(owner, TIImageTool.langstr("NewCF7Title"));
		m_fullImage = fullImage;
	}
	
/*
	| 	Create new external CF7 volume										|

		This is a single volume for a CF7-formatted card. You can copy
		its contents into a CF7 image by using the usual file views
		(copy/paste or drag-and-drop) or by using the tool dsk2cf.exe
		for your CF7 device. A volumes has 1600 sectors and resembles a
		disk image in sector dump format.
		
		Note that you can work on CF7 images in TIImageTool directly. You 
		do not need an external volume file unless you want to store it
		as a separate file.
		
		See also the Utilities menu.
		
		Volume name			EMPTY________
	
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	public void createGui(Font font) {
		prepareGui();

		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("NewCF7Column"));

		if (m_fullImage) {
			putTextLine(this, TIImageTool.langstr("NewCF7Hint1"), 0);
			add(Box.createVerticalStrut(10));
			putTextLine(this, "!" + TIImageTool.langstr("NewCF7Hint2"), 0);
			add(Box.createVerticalStrut(10));
			
			putMultiTextLine(this, TIImageTool.langstr("NewCF7Hint3")); 
			add(Box.createVerticalStrut(10));
			putMultiTextLine(this, TIImageTool.langstr("NewCF7Hint4"));
		}
		else {
			putMultiTextLine(this, TIImageTool.langstr("NewCF7Hint5"));
			add(Box.createVerticalStrut(10));
			putTextLine(this, "!" + TIImageTool.langstr("NewCF7Hint6"), 0);
			add(Box.createVerticalStrut(10));
			putTextLine(this, TIImageTool.langstr("NewCF7Hint7"), 0);
			add(Box.createVerticalStrut(10));
			putTextLine(this, TIImageTool.langstr("NewCF7Hint8"), 0);
			add(Box.createVerticalStrut(10));
			m_tfVolumeName = putTextField(this, TIImageTool.langstr("VolumeName"), "", nColumnWidth, 0);
		}
		add(Box.createVerticalStrut(10));
		add(Box.createVerticalGlue());

		addButtons(m_fullImage? ONLY_OK : OK_AND_CANCEL);
	}
	
	String getDiskName() {
		return m_tfVolumeName.getText();
	}
	
	String getImageTypeSuffix() {
		return ".dsk";
	}
}