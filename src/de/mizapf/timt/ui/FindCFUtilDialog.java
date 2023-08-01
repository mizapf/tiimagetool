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

import javax.swing.*;
import de.mizapf.timt.TIImageTool;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import de.mizapf.timt.util.FileFinder;

import static de.mizapf.timt.TIImageTool.WINDOWS;
import static de.mizapf.timt.TIImageTool.MACOS;
import static de.mizapf.timt.TIImageTool.UNIX;

class FindCFUtilDialog extends ToolDialog {

	TIImageTool imagetool;
	JFrame m_parent;
	private final static int DD = 3;
	JTextField m_tfddpath;
	String m_ddpath;
			
	FindCFUtilDialog(JFrame owner, TIImageTool timt) {
		super(owner, TIImageTool.langstr("FindCFUtils"));
		imagetool = timt;
		m_parent = owner;
	}	
	
/*
	| 	Find CF utilities								|

		Please provide the path to the DD.EXE tool. You may have to download 
		and install it first, since it is not part of the standard Windows 
		operating system.
		
		Click on the search icon to set the start point where to search for
		the program.
	
		Path to the DD.EXE program [Search] ___/usr/bin/dd_____ 
				
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	public void createGui(Font font) {

		prepareGui();

		// ======================
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("ReadwriteCFColumn"));

		putTextLine(this, "!" + TIImageTool.langstr("FindCFUtils"), 0);
		add(Box.createVerticalStrut(10));
		putMultiTextLine(this, TIImageTool.langstr("FindCFPath"));
		add(Box.createVerticalStrut(10));
		String ddprompt = TIImageTool.langstr("ReadWriteCFDD");
		
		m_tfddpath = new JTextField(settings.getPropertyString(imagetool.DDPATH));
		addSearchLine(nColumnWidth, ddprompt, m_tfddpath);
		
		add(Box.createVerticalGlue());
		addButtons();		
	}

	void updateText(String s) {
		m_tfddpath.setText(s);
	}
		
	String getDDPath() {
		return m_ddpath;
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		JFileChooser jfc = null;
		if (ae.getSource()==m_btnOK) {
			/* 
			 * TODO: Check if command line has content and check if output file is set. Disable OK button if not.
			 */
			m_bSet = true;
			dispose();
		}
		if (ae.getSource()==m_btnCancel) {
			m_bSet = false;
			dispose();
		}
		if (ae.getActionCommand().equals("AUTOSEARCH")) {
			// Open a FileOpenDialog
			try {
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				jfc = new JFileChooser();
				jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				File dummy_file = new File(new File("C:\\").getCanonicalPath());
				jfc.setCurrentDirectory(dummy_file);
				jfc.changeToParentDirectory();
				int nReturn = jfc.showOpenDialog(m_parent);
				if (nReturn == JFileChooser.APPROVE_OPTION) {
					updateText(TIImageTool.langstr("FindCFSearching") + "...");
					File file = jfc.getSelectedFile();
					String ddpath = file.getAbsolutePath();
					File ddfl = new File(ddpath);
					if (ddfl.isFile()) {
						if (ddfl.exists() && ddpath.endsWith("dd.exe")) {
							updateText(ddpath);
							m_ddpath = ddpath;
						}
						else {
							updateText(TIImageTool.langstr("FindCFInvalid"));
						}
					}
					else {
						FileFinder ff = new FileFinder(ddpath);
						String path = ff.find("dd.exe");
						if (path != null) {
							updateText(path);
							m_ddpath = path;
						}
						else updateText(TIImageTool.langstr("FindCFNotFound"));
					}					
				}				
			}
			catch (IOException iox) {
				iox.printStackTrace();
			}
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
}

