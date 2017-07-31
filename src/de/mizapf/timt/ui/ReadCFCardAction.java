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
import java.io.*;

import javax.swing.JOptionPane;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.*;
import de.mizapf.timt.TIImageTool;
import java.util.StringTokenizer;
import java.util.ArrayList;

public class ReadCFCardAction extends Activity {
	
	public String getMenuName() {
		return TIImageTool.langstr("ReadCF") + "...";
	}
	
	public String getActionName() {
		return "READCF";
	}
	
	public void go() {
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		Runtime runtime = Runtime.getRuntime();

		int type =  ReadWriteCFDialog.UNIX;

		if (System.getProperty("os.name").startsWith("Windows")) type = ReadWriteCFDialog.WINDOWS;
		else if (System.getProperty("os.name").startsWith("Mac")) type = ReadWriteCFDialog.MACOS;

		ReadWriteCFDialog rwd = new ReadWriteCFDialog(m_parent, imagetool, type, true);

		rwd.createGui(imagetool.boldFont);
		rwd.setVisible(true);
		long lastMod = -1;
		
		if (rwd.confirmed()) {
			String[] commands = rwd.getCommandLine();
			if (commands == null || commands.length < 3) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("AbortCommand"), TIImageTool.langstr("ReadCFTitle"), JOptionPane.ERROR_MESSAGE);
			} 
			else {
				// for (String s: commands) System.out.println("command = " + s);
				String targetImage = rwd.getTargetImage();
				File target = new File(targetImage); 
				if (rwd.isRead() && target.exists()) {
					int nRet = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("ExistsOverwrite"), TIImageTool.langstr("ReadCFTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (nRet == JOptionPane.NO_OPTION) {
						m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						return;
					}
					else lastMod = target.lastModified();
				}
				
				try {
					Process p = runtime.exec(commands, null, null); 
					p.waitFor();
					int exit = p.exitValue();
					if (exit == 0) {
						// Check whether the image file was created
						boolean fail = false;
						if (rwd.isRead()) {
							if (!target.exists() || target.lastModified()==lastMod) {
								JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ReadCFFailed"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
								fail = true;
							}
						}
						if (!fail)
							JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ReadCFSuccess"), TIImageTool.langstr("ReadCFTitle"), JOptionPane.INFORMATION_MESSAGE);
					}
					else {
						// Only effective for the immediate command (e.g. kdesu)
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ReadCFFailed"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
					}
				}
				catch (IOException iox) {
					// iox.printStackTrace();
					// Linux: java.io.IOException: Cannot run program "xxx": error=2, Datei oder Verzeichnis nicht gefunden
					// Windows: java.io.IOException: Cannot run program "xxx": CreateProcess error=2, Das System kann die angegebene Datei nicht finden
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("DDFailed"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
				}
				catch (InterruptedException ix) {
					ix.printStackTrace();
				}
			}
		}
		

		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
