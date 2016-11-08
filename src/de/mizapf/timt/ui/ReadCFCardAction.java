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

import java.awt.Cursor;
import java.io.*;

import javax.swing.JOptionPane;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.*;
import de.mizapf.timt.TIImageTool;

public class ReadCFCardAction extends Activity {
	
	public String getMenuName() {
		return "Read CF7 card ...";
	}
	
	public String getActionName() {
		return "READCF";
	}
	
	public void go() {
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//		JOptionPane.showMessageDialog(m_parent, "Not implemented", "Read CF", JOptionPane.ERROR_MESSAGE);

		Runtime runtime = Runtime.getRuntime();
		
		boolean isWindows = System.getProperty("os.name").startsWith("Windows");
		
		// Do not pack dd.exe into the TIMT distribution but tell the user
		// where to find it
		
		String commandString;
		if (isWindows) {
			// Windows
			commandString = "dd.exe";
		}
		else {
			// For KDE use kdesu, for Gnome use gksu
			// For OSX?
			commandString = "/usr/bin/kdesu /usr/bin/xterm";
		}
		
		// TODO: Present a dialog window with these defaults; to be saved in Properties.
		
		try {
			Process p = runtime.exec(commandString);
		}
		catch (IOException iox) {
			// Linux: java.io.IOException: Cannot run program "xxx": error=2, Datei oder Verzeichnis nicht gefunden
			// Windows: java.io.IOException: Cannot run program "xxx": CreateProcess error=2, Das System kann die angegebene Datei nicht finden
			JOptionPane.showMessageDialog(m_parent, "Cannot run device dump command; please check the command path and whether it is installed at all.", "Error executing CF card reading", JOptionPane.ERROR_MESSAGE);
		}

		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
