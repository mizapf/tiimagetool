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
import java.awt.*;

import javax.swing.*;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.*;
import de.mizapf.timt.TIImageTool;
import java.util.StringTokenizer;
import java.util.ArrayList;

/* Problem: Debian/Ubuntu removed the gksu command without replacement.
   Workaround could be a xterm -e sudo dd ...; chown ...

   ProcessBuilder pb = new ProcessBuilder("xterm", "-e", "sudo sh -c \"touch testfile; chown michael testfile\"");

   Since 3.0: Excluded from code base. CF7 is not used that much anymore since
   TIPI is available, and it is a mess to find out whether the required tools
   are available.
*/

public class ReadCFCardAction extends Activity {
	
	public String getMenuName() {
		return TIImageTool.langstr("ReadCF") + "...";
	}
	
	public String getActionName() {
		return "READCF";
	}
	
	public void go() {
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		ReadWriteCFDialog rwd = new ReadWriteCFDialog(m_parent, imagetool, true);

		rwd.setSettings(settings);
		rwd.createGui(imagetool.boldFont);
		rwd.setVisible(true);
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
