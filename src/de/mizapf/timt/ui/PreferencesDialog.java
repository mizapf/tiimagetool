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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import de.mizapf.timt.assm.Hint;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.FormatException;
import de.mizapf.timt.TIImageTool;

class PreferencesDialog extends ToolDialog implements ActionListener {
					
	TIImageTool m_app;
	
	Map<String,JComponent> m_entries;
	int m_fieldWidth;
	
	JTabbedPane m_tabs;
	String[] m_lang;
	
	PreferencesDialog(JFrame owner, TIImageTool app) {
		super(owner, TIImageTool.langstr("PreferencesTitle"));
		m_app = app;
		m_lang = app.getLanguages();
	}
	
	
/*
	| 	Preferences								|
	
		Name of pref:   [ value ]
		Name of pref:   [ value ]
		Name of pref:   [ value ]
		Name of pref:   [ value ]
		Name of pref:   [ value ]
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	void createGui(Font font) {
		m_bSet = false;

		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);

		// Sample text
		m_nColumnWidth = fm.stringWidth(TIImageTool.langstr("PreferencesColumn"));
		
		// Sample text
		m_fieldWidth = fm.stringWidth("javax.swing.plaf.metal.MetalLookAndFeel");

		m_entries = new HashMap<String,JComponent>();

		m_tabs = new JTabbedPane();
		m_tabs.addTab(TIImageTool.langstr("PreferencesGeneral"), createTab("general"));
		m_tabs.addTab(TIImageTool.langstr("PreferencesPaths"), createTab("paths"));
		m_tabs.addTab(TIImageTool.langstr("PreferencesCFCard"), createTab("cfcard"));
		m_tabs.addTab(TIImageTool.langstr("PreferencesOutput"), createTab("output"));
		m_tabs.addTab(TIImageTool.langstr("PreferencesImporting"), createTab("import"));
		m_tabs.addTab(TIImageTool.langstr("PreferencesExporting"), createTab("export"));
		
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));
		add(m_tabs);
		add(Box.createVerticalStrut(10));
		addButtons();
	}
	
	private JPanel createTab(String category) {
		java.util.List<String> prefs = m_app.getPreferences(category);
		JComponent jc = null;		
		String value = null;
		JPanel tabPanel = new JPanel();
		tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.Y_AXIS));
		boolean isWindows = System.getProperty("os.name").startsWith("Windows");
		
		tabPanel.add(Box.createVerticalStrut(10));	

		int i=0;
		int index = 0;
		
		for (String s : prefs) {
			String name = m_app.getPreferenceLabel(s);
			char type = m_app.getPreferenceType(s);
			switch (type) {
			case 'c':
				value = m_app.getPropertyString(s);
				try {
					index = Integer.parseInt(value);
				}
				catch (NumberFormatException nfx) {
					// Do not localize this
					System.err.println("Invalid language parameter; invalid index: " + value);
					index = 0;
				}
				if (index > m_lang.length-1) {
					System.err.println("Invalid language parameter; invalid index: " + index);
					index = m_lang.length-1;
				}
				jc = putComboBox(tabPanel, name, m_lang, index, m_nColumnWidth);
				m_entries.put(s, jc);
				break;
			case 's':
				value = m_app.getPropertyString(s);
				jc = putTextField(tabPanel, name, value, m_nColumnWidth, m_fieldWidth);
				m_entries.put(s, jc);
				break;
			case 'b':
				boolean selected = m_app.getPropertyBoolean(s);
				jc = putCheckBox(tabPanel, name, selected, m_nColumnWidth);
				m_entries.put(s, jc);
				break;					
			case 'p':
				value = m_app.getPropertyString(s);
				jc = putTextField(tabPanel, name, value, m_nColumnWidth, m_fieldWidth);
				m_entries.put(s, jc);
				break;
			case 'u':
				if (!isWindows) {
					value = m_app.getPropertyString(s);
					jc = putTextField(tabPanel, name, value, m_nColumnWidth, m_fieldWidth);
					m_entries.put(s, jc);
				}
				break;
			default:
				System.err.println(TIImageTool.langstr("PreferencesUnknown") + ": " + type);
				break;
			}
		}
		tabPanel.add(Box.createVerticalStrut(10));
		tabPanel.add(Box.createVerticalGlue());	

		return tabPanel;
	}
	
	Map<String,JComponent> getSettings() {
		return m_entries;
	}
}
