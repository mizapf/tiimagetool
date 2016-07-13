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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.font.*;

class ImportContentDialog extends ToolDialog {
	
	JTextField 		m_tfFileName;
	JComboBox<String>	m_jcFormat;
	JComboBox<String>		m_jcTabs;
	boolean			m_bHasTabs;
	boolean			m_bHasSpecial;
	Set<Character> 	m_Special;
	String[]		m_asTranslation;
	JTextField		m_tfTrans;
	String			m_sCharList;
	
	ImportContentDialog(JFrame owner, boolean bHasTabs, Set<Character> special) {
		super(owner, "Import content parameters");
		m_bHasTabs = bHasTabs;
		m_Special = special;
	}
	
/*
	| 	Import content parameters									|
		
		File name [...]
		File format will be DIS/VAR 80
		Replace TABs v [ keep | 4 5 6 7 8 spaces ]
		
			+-------+			+-----------+
			|	OK	|			|	Cancel	|
			+-------+           +-----------+
*/	
	void createGui() {
		prepareGui();
		int nLabelWidth = determineFieldWidth("Special characters:");
		
		m_tfFileName = putTextField(this, "File name", "", nLabelWidth, 0);

		String[] asFormatOptions = { "DIS/VAR 80" };
		m_jcFormat = putComboBox(this, "File format", asFormatOptions, 0, nLabelWidth);

		if (m_bHasTabs) {
			String[] asOptions = { "keep", "4 spaces", "5 spaces", "6 spaces", "7 spaces", "8 spaces" };
			m_jcTabs = putComboBox(this, "Replace TABs", asOptions, 5, nLabelWidth);
		}
		
		if (m_Special.size()>0) {
			Box box4a = new Box(BoxLayout.X_AXIS);
			box4a.add(Box.createHorizontalStrut(10));
			StringBuilder sb = new StringBuilder();
			StringBuilder sb1 = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for (char ch:m_Special) {
				sb2.append(ch);
				if (sb.length()>0) sb.append(",");
				sb.append(ch);
				if (sb1.length()>0) sb1.append(", ");
				sb1.append(ch).append(" (").append((int)ch).append(")");
			}
			m_sCharList = sb2.toString();
			
			add(Box.createVerticalStrut(10));
			
			putLabel(this, "Special characters:", sb1.toString(), nLabelWidth);
			m_tfTrans = putTextField(this, "Translations:", sb.toString(), nLabelWidth, 100);
		}
		addButtons();	
	}
		
	Map<Character,String> getTranslations() {
		Map<Character,String> chToStr = new HashMap<Character,String>();
		if (m_Special.size()>0) {
			int j=0; 
			for (int i=0; i < m_sCharList.length(); i++) {
				char ch = m_sCharList.charAt(i);
				chToStr.put(ch, String.valueOf(ch));
			}
			String[] asEntry = m_tfTrans.getText().split(",");
			for (int i=0; i < asEntry.length && i < m_sCharList.length(); i++) {
				chToStr.put(m_sCharList.charAt(i), asEntry[i].trim());
			}
		}
		return chToStr;
	}
	
	String getTabTrans() {
		String sTab = "        ";
		if (!m_bHasTabs) return "\t";
		if (m_jcTabs.getSelectedIndex()==0) return "\t";
		else return sTab.substring(0, m_jcTabs.getSelectedIndex()+3);
	}
	
	String getFileName() {
		return m_tfFileName.getText().trim();
	}
}
