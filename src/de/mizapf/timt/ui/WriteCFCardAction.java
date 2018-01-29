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
    
    MacOS additions by Henrik Wedekind 2016

****************************************************************************/
package de.mizapf.timt.ui;

import java.awt.Cursor;
import java.io.File;
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
		return TIImageTool.langstr("WriteCF") + "...";
	}
	
	public String getActionName() {
		return "WRITECF";
	}
	
	public void go() {
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		Runtime runtime = Runtime.getRuntime();
		
		// Find missing commands
		if (imagetool.getOperatingSystem()==TIImageTool.WINDOWS) {
			String ddpath = imagetool.getPropertyString(TIImageTool.DDPATH);
			if (ddpath == null) ddpath = "";
			if (ddpath.length() > 0) {
				// Test whether the path is still valid
				File ddfile = new File(ddpath);
				if (!ddfile.exists()) {
					ddpath = "";
					imagetool.setProperty(TIImageTool.DDPATH, ddpath);
				}
			}
			
			// Path is not valid; let's search for the proper path
			if (ddpath.length()==0) {
				FindCFUtilDialog findd = new FindCFUtilDialog(m_parent, imagetool);
				findd.createGui(imagetool.boldFont);
				findd.setVisible(true);
				
				if (!findd.confirmed()) {
					m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					return;
				}
				else {
					ddpath = findd.getDDPath();
					if (ddpath == null) ddpath = "";
					imagetool.setProperty(TIImageTool.DDPATH, ddpath);
				}
				ddpath = imagetool.getPropertyString(TIImageTool.DDPATH);
			}
			
			if (ddpath.length()==0) {
				JOptionPane.showMessageDialog(m_parent, "No DD.EXE found. This is required for CF operations.", TIImageTool.langstr("ReadCFTitle"), JOptionPane.ERROR_MESSAGE);				
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));				
				return;
			}
		}
		else {
			// If we're on MAC/UNIX, check whether the chown/dd(/gksu/kdesu) tools were found
			if (!imagetool.allToolsFound()) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ReadWriteCFNoUtils"), TIImageTool.langstr("ReadCFTitle"), JOptionPane.ERROR_MESSAGE);				
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));				
				return;
			}
		}
				
		// Continue

		ReadWriteCFDialog rwd = new ReadWriteCFDialog(m_parent, imagetool, false);

		rwd.createGui(imagetool.boldFont);
		rwd.setVisible(true);

		if (rwd.confirmed()) {
			String[] commands = rwd.getCommandLine();
			if (commands == null || commands.length < 3) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("AbortCommand"), TIImageTool.langstr("WriteCFTitle"), JOptionPane.ERROR_MESSAGE);
			} 
			else {
				try {
					long startTime = System.currentTimeMillis();
					// for (String s: commands) System.out.println("command = " + s);
					Process p = runtime.exec(commands, null, null); 

					WaitForCF7Dialog wd = new WaitForCF7Dialog(m_parent, imagetool, false);
					wd.createGui(imagetool.boldFont);
					wd.setVisible(true);
					p.waitFor();
					wd.dispose();

					long endTime = System.currentTimeMillis();
					int exit = p.exitValue();
					if (exit == 0) {
						if (endTime - startTime < 5000) {
							// If it takes less than 5 secs, this obviously failed. 
							// Looks like an ugly hack, but this should be a safe 
							// way to find it out for different OS
							JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("WriteCFFailed"), TIImageTool.langstr("WriteCFTitle"), JOptionPane.ERROR_MESSAGE);
						}
						else {
							JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("WriteCFSuccess"), TIImageTool.langstr("WriteCFTitle"), JOptionPane.INFORMATION_MESSAGE);
						}
					}
					else {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("WriteCFFailed"), TIImageTool.langstr("WriteCFTitle"), JOptionPane.ERROR_MESSAGE);
					}
				}
				catch (IOException iox) {
					// iox.printStackTrace();
					// Linux: java.io.IOException: Cannot run program "xxx": error=2, Datei oder Verzeichnis nicht gefunden
					// Windows: java.io.IOException: Cannot run program "xxx": CreateProcess error=2, Das System kann die angegebene Datei nicht finden
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("DDFailed"), TIImageTool.langstr("WriteCFTitle"), JOptionPane.ERROR_MESSAGE);
				}
				catch (InterruptedException ix) {
					ix.printStackTrace();
				}
			}
		}

		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
