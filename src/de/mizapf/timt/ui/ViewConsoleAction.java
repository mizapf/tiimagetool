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
    
    Copyright 2015 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.JOptionPane;
import java.io.IOException;
import de.mizapf.timt.files.*;
import java.awt.Cursor;
import java.io.EOFException;
import de.mizapf.timt.util.Utilities;

public class ViewConsoleAction extends Activity {

	public String getMenuName() {
		return "View Console output";
	}
	
	public String getActionName() {
		return "VIEWCONS";
	}
	
	public void go() {
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		imagetool.showConsoleContent();  
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
