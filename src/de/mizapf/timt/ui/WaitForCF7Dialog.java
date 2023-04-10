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

class WaitForCF7Dialog extends ToolDialog {
	
	TIImageTool imagetool;
	boolean   m_read;
	
	WaitForCF7Dialog(JFrame owner, TIImageTool timt, boolean read) {
		super(owner, TIImageTool.langstr(read? "ReadWriteCFTitleR" : "ReadWriteCFTitleW"));
		imagetool = timt;
		m_read = read;
	}
	
/*
	Inform the user about the time to wait.	
	We cannot offer a CANCEL button to kill the process because this 
	would again require root privileges.
*/	
	public void createGui(Font font) {
		prepareGui();

		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("NewCF7Column"));

		add(Box.createVerticalStrut(10));
		if (m_read) putTextLine(this, TIImageTool.langstr("ReadCFWait"), 0);
		else putTextLine(this, TIImageTool.langstr("WriteCFWait"), 0);
		add(Box.createVerticalStrut(10));
		putTextLine(this, TIImageTool.langstr("ReadWriteCFWaitLong"), 0);
		add(Box.createVerticalStrut(10));
		add(Box.createVerticalGlue());
		addButtons(NONE);
		setModal(false);
	}
}
