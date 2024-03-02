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
    
    Copyright 2024 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import de.mizapf.timt.TIImageTool;

class InstallGenOSDialog extends ToolDialog {

	TIImageTool imagetool;
	JFrame m_parent;
	JComboBox<String> m_jcEprom;
	JComboBox<String> m_jcFloppy;
	JTextField m_tfPath;
	boolean m_bFloppy;
	
	InstallGenOSDialog(JFrame owner, TIImageTool timt, boolean bFloppy) {
		super(owner, TIImageTool.langstr("InstallGenOSTitle"));
		imagetool = timt;
		m_parent = owner;
		m_bFloppy = bFloppy; 
	}	
	
/*
	| 	Install GeneveOS						            |
		
	    You are about to install all files from the specified directory
	    on the currently selected image. The directory must at least contain the 
	    SYSTEM-SYS and loader files. 
	    
	    The suitable loader for the image type (Floppy, HFDC, IDE, SCSI) is 
	    automatically copied and placed in the proper target directory as
	    required by the boot EPROM.
	    
	    Source directory (see preferences): [ ... ]
	    
	    Geneve/Genmod boot EPROM: [ 0.98 ]
	    
	    Floppy controller: [ HFDC / non-HFDC ]
	    
			+-------+	+-----------+
			|	OK	|	|	Cancel  |
			+-------+   +-----------+
*/	
	void createGui(Font font) {
		prepareGui();
		String[] texts = { "InstallGenOSPath", "FileFormat" };
		int nColumnWidth = determineMaxWidth(texts);
		putTextLine(this, "!" + TIImageTool.langstr("InstallGenOSTitle"), 0);
		add(Box.createVerticalStrut(20));
	
		putMultiTextLine(this, TIImageTool.langstr("InstallGenOSExp1"));
		add(Box.createVerticalStrut(10));
		putMultiTextLine(this, TIImageTool.langstr("InstallGenOSExp2"));
		add(Box.createVerticalStrut(10));
		putMultiTextLine(this, TIImageTool.langstr("InstallGenOSExp3"));
		add(Box.createVerticalStrut(20));		
		
		JTextField tf = putTextField(this, TIImageTool.langstr("InstallGenOSPath"), settings.getPropertyString(TIImageTool.GENOSPATH), nColumnWidth, 0); 
		tf.setEditable(false);
		
		String[] asEprom = { "0.98", "1.00", "2.00" };
		m_jcEprom = putComboBox(this, TIImageTool.langstr("InstallGenOSEprom"), asEprom, 0, nColumnWidth);
		add(Box.createVerticalStrut(10));

		String[] asFloppy = { TIImageTool.langstr("InstallGenOSHFDC"), TIImageTool.langstr("InstallGenOSOther") };
		if (m_bFloppy) {
			m_jcFloppy = putComboBox(this, TIImageTool.langstr("InstallGenOSController"), asFloppy, 0, nColumnWidth);		
		}
		
		add(Box.createVerticalStrut(10));
		add(Box.createVerticalGlue());
		addButtons();
	}
	
	int getEpromVersion() {
		int[] version = { InstallGenOSAction.E098, InstallGenOSAction.E100, InstallGenOSAction.E200 };
		return version[m_jcEprom.getSelectedIndex()];
	}
	
	int getController() {
		if (m_bFloppy) {
			int[] version = { InstallGenOSAction.HFDC, InstallGenOSAction.OTHER };
			return version[m_jcFloppy.getSelectedIndex()];
		}
		return 0;
	}
}
