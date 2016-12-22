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
    
    Copyright 2013 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.font.*;

import de.mizapf.timt.TIImageTool;

class TranslateDialog extends ToolDialog {
	
	JComboBox<String>		m_jcFormat;
	JComboBox<String>		m_jcTabs;
	boolean			m_bHasTabs;
	boolean			m_bHasSpecial;
	String			m_special;
	String[]		m_asTranslation;
	JTextField		m_tfTrans;
	String			m_sCharList;
	
	TranslateDialog(JFrame owner, boolean bHasTabs, String special) {
		super(owner, TIImageTool.langstr("TranslateDialogTitle"));
		m_bHasTabs = bHasTabs;
		m_special = special;
	}
	
/*
	| 	Import content parameters									|
		
		Replace TABs v [ keep | 4 5 6 7 8 spaces ]
		
			+-------+			+-----------+
			|	OK	|			|	Cancel	|
			+-------+           +-----------+
*/	
	void createGui() {
		prepareGui();
		int nLabelWidth = determineFieldWidth(TIImageTool.langstr("TranslateDialogColumn"));
		
		if (m_bHasTabs) {
			String[] asOptions = new String[6];
			asOptions[0] = TIImageTool.langstr("TranslateDialogKeep");
			for (int i=4; i < 9; i++)
				asOptions[i-3] = String.format(TIImageTool.langstr("TranslateDialogSpaces"), i);
			m_jcTabs = putComboBox(this, TIImageTool.langstr("TranslateDialogTab"), asOptions, 5, nLabelWidth);
		}
			
		if (m_special.length()>0) {
			Box box4a = new Box(BoxLayout.X_AXIS);
			box4a.add(Box.createHorizontalStrut(10));
			StringBuilder sb = new StringBuilder();
			StringBuilder sb1 = new StringBuilder();
			
			for (int i=0; i < m_special.length(); i++) {
				char ch = m_special.charAt(i);
				if (sb.length()>0) sb.append(",");
				sb.append(ch);
				if (sb1.length()>0) sb1.append(", ");
				sb1.append(ch).append(" (").append((int)ch).append(")");
			}
			m_sCharList = m_special;
			
			add(Box.createVerticalStrut(10));
			
			putLabel(this, TIImageTool.langstr("TranslateDialogSpecial"), sb1.toString(), nLabelWidth);
			m_tfTrans = putTextField(this, TIImageTool.langstr("TranslateDialogReplace"), sb.toString(), nLabelWidth, 0);
		}
		addButtons();	
	}
		
	String getFromTranslations() {
		return m_special;
	}
	
	String getToTranslations() {
		if (m_tfTrans==null) return "";
		else return m_tfTrans.getText();
	}
	
	String getTabTrans() {
		String sTab = "        ";
		if (!m_bHasTabs) return "\t";
		if (m_jcTabs.getSelectedIndex()==0) return "\t";
		else return sTab.substring(0, m_jcTabs.getSelectedIndex()+3);
	}
}
