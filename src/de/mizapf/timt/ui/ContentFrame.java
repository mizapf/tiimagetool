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
    
    Copyright 2011 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.ui;
import de.mizapf.timt.util.Utilities;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

import de.mizapf.timt.TIImageTool;

public class ContentFrame extends JFrame implements ActionListener {

	JMenuBar m_mbar; 
	JMenu m_mFile;
	JMenuItem m_iSaveAs;
	JMenuItem m_iClear;
	JMenuItem m_iClose;
	TIImageTool m_app;
	boolean m_withClear;
	
	String m_sContent;
	
	JTextArea m_jep;
	
	private static final String SAVEAS = "saveas"; 
	private static final String CLEAR = "clear"; 
	private static final String CLOSE = "close"; 
	
	public ContentFrame(String sFile, TIImageTool app, boolean bWithClear) {
		super(sFile);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		m_app = app;
		m_app.registerFrame(this);
		m_withClear = bWithClear;
	}

	public void createGui(String sText, String sFontName) {	
		
		String sEscape = m_app.getPropertyString(TIImageTool.ESCAPE);

		if (sEscape.length() < 1 || sEscape.length() > 2) {
			JOptionPane.showMessageDialog(this, "Invalid escape specification (see manual)", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		char chEscape = sEscape.charAt(0);
		boolean bEscape = (sEscape.length() == 2);
		boolean bUnprintable = false;
		
		if (bEscape && sEscape.charAt(1) != '%') {
			JOptionPane.showMessageDialog(this, "Invalid escape specification (see manual)", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < sText.length(); i++) {
			char c = sText.charAt(i);
			if (c==chEscape && bEscape) sb.append(c).append(c);
			else {
				if (Utilities.isPrintable(c)) sb.append(c);
				else {
					System.out.println("Unprintable character at position " + i + ": code " + Utilities.toHex(c&0xff,2));
					bUnprintable = true;
					if (bEscape) sb.append(chEscape).append(Utilities.toHex(c & 0xff, 2));
					else sb.append(chEscape);
				}
			}
		}
		
		if (bUnprintable) {
			if (bEscape)
				JOptionPane.showMessageDialog(this, "Unprintable characters have been replaced by " + chEscape + "xx (ASCII code).", "Warning", JOptionPane.WARNING_MESSAGE);
			else 
				JOptionPane.showMessageDialog(this, "Unprintable characters have been replaced by '" + sEscape + "'.", "Warning", JOptionPane.WARNING_MESSAGE);
		}
		
		m_sContent = sb.toString();
		m_mbar = new JMenuBar();

		m_mFile = new JMenu("File");
		m_mbar.add(m_mFile);
		m_iSaveAs = new JMenuItem("Save as...");
		m_iSaveAs.setActionCommand(SAVEAS);
		m_iSaveAs.addActionListener(this);
		m_mFile.add(m_iSaveAs);

		m_iClear = new JMenuItem("Clear content");
		m_iClear.setActionCommand(CLEAR);
		m_iClear.addActionListener(this);

		if (m_withClear) {
			m_mFile.add(m_iClear);			
		}
		
		m_iClose = new JMenuItem("Close");
		m_iClose.setActionCommand(CLOSE);
		m_iClose.addActionListener(this);
		m_mFile.add(m_iClose);
		setJMenuBar(m_mbar);	

		Container cntEditor = getContentPane();
		cntEditor.setLayout(new BoxLayout(cntEditor, BoxLayout.Y_AXIS));
		
		m_jep = new JTextArea(m_sContent);
		m_jep.setEditable(false);
		m_jep.setFont(Font.decode(sFontName));
		JScrollPane jp = new JScrollPane(m_jep);
		cntEditor.add(jp);
		cntEditor.setPreferredSize(new Dimension(800,600));
		m_jep.setCaretPosition(0);
		
		int scrollMode = JViewport.SIMPLE_SCROLL_MODE;
//		int scrollMode = JViewport.BACKINGSTORE_SCROLL_MODE;
//		int scrollMode = JViewport.BLIT_SCROLL_MODE;      // default, distorts output
		jp.getViewport().setScrollMode(scrollMode);
	}
	
	void terminate() {
		dispose();
	}
	
	void append(String s) {
		m_jep.append(s);
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand()==SAVEAS) {
			JFileChooser jfc = new JFileChooser();
			
			int nReturn = jfc.showSaveDialog(this);
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				try {
					java.io.File file = jfc.getSelectedFile();
					
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(m_sContent.getBytes());
					fos.close();
				}
				catch (IOException iox) {
					JOptionPane.showMessageDialog(this, "Error", "IOException: " + iox.getClass().getName(), JOptionPane.ERROR_MESSAGE); 
					return;
				}
			}
		}
		else {
			if (ae.getActionCommand()==CLOSE) {
				m_app.closeFrame(this);
			}
			else {
				if (ae.getActionCommand()==CLEAR) {
					m_app.clearLogfile();
					m_jep.setText("");
				}				
			}
		}
	}
}
