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
import java.io.*;
import java.awt.Dimension;
import java.awt.event.KeyEvent;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class NewCF7ImageAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("CompleteCF7") + "...";
	}
	
	public String getActionName() {
		return "NEWCF7IMG";
	}
	
	public void go() {
		NewCF7Dialog newimagedia = new NewCF7Dialog(m_parent, true);

		try {
			newimagedia.createGui(imagetool.boldFont);
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		newimagedia.setVisible(true);
	}
}
