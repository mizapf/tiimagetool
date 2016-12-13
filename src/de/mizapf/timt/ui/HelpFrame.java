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
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.JFrame;
import javax.swing.event.HyperlinkListener;
import javax.swing.JViewport;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Font;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Container;
import javax.swing.event.HyperlinkEvent;

import de.mizapf.timt.TIImageTool;

public class HelpFrame extends JFrame implements HyperlinkListener {

	private static final String FONTHLP = Font.SANS_SERIF;
	
	JEditorPane m_jep;
		
	HelpFrame() {
		super(TIImageTool.langstr("HelpTitle"));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	void createGui(Component jpn) {
		jpn.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		String sText = null;
		
		Container cntEditor = getContentPane();
		Class thisClass = getClass();
		InputStream is = thisClass.getResourceAsStream("help.html");
		if (is==null) {
			sText = "\n*** " + TIImageTool.langstr("HelpNotFound") + " ***";
			m_jep = new JEditorPane("text/plain", sText);
		}
		else {
			try {
				DataInputStream dis = new DataInputStream(is);
				byte[] abyHelp = new byte[dis.available()];
				dis.readFully(abyHelp);
				sText = new String(abyHelp);
				m_jep = new JEditorPane("text/html; charset=UTF-8", sText);
				m_jep.addHyperlinkListener(this);
			}
			catch (IOException iox) {
				sText = TIImageTool.langstr("IOError");
				m_jep = new JEditorPane("text/plain", sText);
			}
		}
		
		
		m_jep.setEditable(false);
		m_jep.setFont(Font.decode(FONTHLP));
		JScrollPane jp = new JScrollPane(m_jep);
		cntEditor.add(jp);

//		int scrollMode = JViewport.SIMPLE_SCROLL_MODE;
		int scrollMode = JViewport.BACKINGSTORE_SCROLL_MODE;
//		int scrollMode = JViewport.BLIT_SCROLL_MODE;      // default, distorts output
		jp.getViewport().setScrollMode(scrollMode);
		
		jp.setPreferredSize(new Dimension(800,600));
		m_jep.setCaretPosition(0);
		setLocationByPlatform(true);
		setVisible(true);
		pack();		
		jpn.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
			String sDescr = e.getDescription();
			if (sDescr.startsWith("#")) {
				m_jep.scrollToReference(sDescr.substring(1));
			}
		}
	}
}
