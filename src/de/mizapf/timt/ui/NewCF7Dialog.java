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

class NewCF7Dialog extends ToolDialog {
	
	JTextField 		m_tfVolumeName;
	boolean   m_fullImage;
	
	NewCF7Dialog(JFrame owner, boolean fullImage) {
		super(owner, "Create new external CF7 volume");
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
		int nColumnWidth = fm.stringWidth("Volume name");

		if (m_fullImage) {
			putTextLine(this, "If you want to create a new image for a CF7 card,", 0);
			add(Box.createVerticalStrut(10));
			putTextLine(this, "!please use the functions in the \"Utility\" menu.", 0);
			add(Box.createVerticalStrut(10));
			
			putMultiTextLine(this, 
				  "Short explanation: Compact Flash cards sometimes offer less than the exact amount of\n"
				+ "memory as printed on their casing, so you may get into trouble when copying the image\n"
				+ "on the CF card.");
			add(Box.createVerticalStrut(10));
			putMultiTextLine(this,
				  "Hence, you should use the \"Read Compact Flash Card\" function first, which produces\n"
				+ "an image file, then format the volumes as desired with \"Format CF7\". This way, you\n"
				+ "will get an image that suits your CF card; copy it to the CF card with \"Write Compact\n"
				+ "Flash Card\" finally.");
		}
		else {
			putMultiTextLine(this,
				   "This is a single volume for a CF7-formatted card. You can copy its contents into a\n"
				 + "CF7 image by using the usual file views (copy/paste or drag-and-drop) or by using the\n"
				 + "tool dsk2cf.exe for your CF7 device. A volumes has 1600 sectors and resembles a\n"
				 + "floppy disk image in sector dump format.");
			add(Box.createVerticalStrut(10));
			putTextLine(this, "!You can work on CF7 images in TIImageTool directly.", 0);
			add(Box.createVerticalStrut(10));
			putTextLine(this, "You do not need an external volume file unless you want to store it as a separate file.", 0);
			add(Box.createVerticalStrut(10));
			putTextLine(this, "See also the \"Utility\" menu.", 0);
			add(Box.createVerticalStrut(10));
			m_tfVolumeName = putTextField(this, "Volume name", "", nColumnWidth, 0);
		}
		add(Box.createVerticalStrut(10));
		add(Box.createVerticalGlue());

		addButtons(m_fullImage);
	}
	
	String getDiskName() {
		return m_tfVolumeName.getText();
	}
	
	String getImageTypeSuffix() {
		return ".dsk";
	}
}