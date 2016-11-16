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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;

import javax.swing.JOptionPane;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.*;
import de.mizapf.timt.TIImageTool;

public class WriteCFCardAction extends Activity {
	
	public String getMenuName() {
		return "Write CF7 card ...";
	}
	
	public String getActionName() {
		return "WRITECF";
	}
	
	public void go() {
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//		JOptionPane.showMessageDialog(m_parent, "Not implemented", "Read CF", JOptionPane.ERROR_MESSAGE);

		Runtime runtime = Runtime.getRuntime();
		
		boolean isWindows = System.getProperty("os.name").startsWith("Windows");
		
		// Do not pack dd.exe into the TIMT distribution but tell the user
		// where to find it
				
		ReadWriteCFDialog rwd = new ReadWriteCFDialog(m_parent, imagetool, isWindows, false);

		rwd.createGui(imagetool.boldFont);
		rwd.setVisible(true);

		if (rwd.confirmed()) {
			String[] commands = rwd.getCommandLine();	
			try {
				for (String s: commands) System.out.println("command = " + s);
				Process p = runtime.exec(commands, null, null); 
				p.waitFor();
				int exit = p.exitValue();
				if (exit == 0) {
					JOptionPane.showMessageDialog(m_parent, "CF card written successfully.", "CF card writing", JOptionPane.INFORMATION_MESSAGE);
				}
				else {
					JOptionPane.showMessageDialog(m_parent, "Could not write the CF card. Maybe the path was wrong.", "CF card writing", JOptionPane.ERROR_MESSAGE);
				}
			}
			catch (IOException iox) {
				// Linux: java.io.IOException: Cannot run program "xxx": error=2, Datei oder Verzeichnis nicht gefunden
				// Windows: java.io.IOException: Cannot run program "xxx": CreateProcess error=2, Das System kann die angegebene Datei nicht finden
				JOptionPane.showMessageDialog(m_parent, "Cannot run device dump command; please check the command path and whether it is installed at all.", "Error executing CF card reading", JOptionPane.ERROR_MESSAGE);
			}
			catch (InterruptedException ix) {
				ix.printStackTrace();
			}
		}

		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
