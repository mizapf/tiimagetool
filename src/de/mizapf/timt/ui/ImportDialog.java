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
import java.nio.charset.Charset;
import de.mizapf.timt.files.TFile;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.ui.ToolDialog;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.basic.*;

public class ImportDialog extends ToolDialog {
	
	JTextField 		m_tfFileName;
	JCheckBox		m_chbDontAsk;
	
	JCheckBox		m_chbProtected;
	JRadioButton	m_rbtOpt1;
	JRadioButton	m_rbtOpt2;
	JRadioButton	m_rbtOpt3;
	
	JPanel			m_jpExtendedPanel;
	JPanel			m_jpOther;
	JPanel			m_jpBasic;
	
	String 			m_sSuggested;
	String			m_sGivenName;
	
	boolean			m_bMulti;
	int 			m_nMode;
	
	JComboBox<String>		m_jcFormat;
	JComboBox<String>		m_jcVersion;
	JTextField		m_tfRecLen;
//	JTextField		m_tfL3Count;
	
	JTextArea		m_taPreview;
	
	int				m_nPresetFormat;
	int				m_nPresetRecLen;
	int				m_nPresetRecCount;
	
	byte[]			m_abyContent;
	
	boolean m_bSet = false;
	
	String			m_sInfoTitle;
	
	public static final int NORMAL = 1;
	public static final int TEXTONLY = 2;
	public static final int NONAME = 3;
	public static final int NOHEADER = 4;
	public static final int BINARY = 5;
	public static final int BASIC = 6;
	
	ImportDialog(JFrame owner, String sGivenName, String sSuggested, boolean bMulti, int nMode) {
		super(owner, "Import parameters");
		m_sGivenName = sGivenName;
		m_sSuggested = sSuggested;
		m_bMulti = bMulti;
		m_nMode = nMode;
		m_sInfoTitle = "File has no TIFILES header";
	}

	public ImportDialog(JFrame owner, String sSuggested, boolean bMulti, int nMode) {
		super(owner, "Import parameters");
		m_sGivenName = null;
		m_sSuggested = sSuggested;
		m_bMulti = bMulti;
		m_nMode = nMode;
		m_sInfoTitle = "File has no TIFILES header";
	}
	
	void setInfoTitle(String sInfo) {
		m_sInfoTitle = sInfo;
	}
	
/*
	| 	Import Parameters									|
		
		File name is missing in the TIFILES file
		
		Use this file name    [FILENAME]
		Guess each file name  [x]

			+-------+			+-----------+
			|	OK	|			|	Cancel	|
			+-------+           +-----------+
*/	

/*
	| 	Import Parameters									|
		
		File name             [FILENAME]
		Save as               [Simple text / BASIC]

		Basic version         [v TI BASIC / Extended Basic]
		Protected             [ ]
		Save format           [PROGRAM / MERGE / LONG]
		-----------
		Format                [DIS/VAR
		
		
			+-------+			+-----------+
			|	OK	|			|	Cancel	|
			+-------+           +-----------+
*/	

	public void createGui() {
		prepareGui();

		int nLabelWidth = determineFieldWidth("Guess each file name");
		int nValueWidth = determineFieldWidth("DIS/VAR 80 text file");
		
		add(Box.createVerticalStrut(10));
		if (m_sGivenName != null) {
			putTextLine(this, "File has TIFILES header, but the file name is not valid.", 0);
			add(Box.createVerticalStrut(10));
			
			putTextLine(this, "File name: " + m_sGivenName, 0);
			StringBuilder sb = new StringBuilder();
			for (int i=0; i < 10; i++) {
				sb.append(" ");
				sb.append(Utilities.toHex(m_sGivenName.charAt(i), 2));
			}
			add(Box.createVerticalStrut(10));
			putTextLine(this, "Hex values: " + sb.toString(), 0);
			add(Box.createVerticalStrut(10));

			m_tfFileName = putTextField(this, "Use this file name", m_sSuggested, nLabelWidth, 0);
			add(Box.createVerticalStrut(10));
						
			if (m_bMulti) {
				m_chbDontAsk = putCheckBox(this, "Guess each file name", false, nLabelWidth);
			}			
		}
		else {
			if (m_nMode==NONAME) {
				putTextLine(this, "File has TIFILES header, but no name defined.", 0);
				add(Box.createVerticalStrut(10));
								
				m_tfFileName = putTextField(this, "Use this file name", m_sSuggested, nLabelWidth, 0);
				
				if (m_bMulti) {
					m_chbDontAsk = putCheckBox(this, "Guess each file name", false, nLabelWidth);
				}
			}
			else {
				setMinimumSize(new Dimension(nLabelWidth*3,1));
				boolean fromEditor = false;
				if (m_sSuggested == DVEditorFrame.FROMEDITOR) {
					putTextLine(this, "!Save text or BASIC from editor", 0);
					m_sSuggested = "UNNAMED";
					fromEditor = true;
				}
				else {
					putTextLine(this, "!" + m_sInfoTitle, 0);
				}
				add(Box.createVerticalStrut(10));
				m_tfFileName = putTextField(this, "File name", m_sSuggested, nLabelWidth, 0);

				// File may be text ... or not
			
				// Container where, String sLabel, int nLabelWidth, String[] asOption, int[] anWidth, int nSelected
				if (m_nMode == BINARY) {
					add(Box.createVerticalStrut(10));
					putTextLine(this, "File will be imported as PROGRAM file", 0);
				}
				else {	
					int select = 0;
					String[] asFor = null;
					JRadioButton[] arb = null;
					
					if (m_nMode == BASIC) {
						asFor = new String[3];
						asFor[0] = "DIS/VAR 80 text file";
						asFor[1] = "TI BASIC";
						asFor[2] = "Extended Basic";
						arb = putRadioButtons(this,  "Import mode", nLabelWidth, asFor, null, 1);
						m_rbtOpt1 = arb[0];
						m_rbtOpt2 = arb[1];
						m_rbtOpt3 = arb[2];
						m_rbtOpt1.addActionListener(this);
						m_rbtOpt2.addActionListener(this);
						m_rbtOpt3.addActionListener(this);
					}
					else { 
						asFor = new String[3];
						asFor[0] = "DIS/VAR 80 text file";
						asFor[1] = "Binary";
						asFor[2] = "Other format";
						arb = putRadioButtons(this,  "Import mode", nLabelWidth, asFor, null, 0);
						m_rbtOpt1 = arb[0];
						m_rbtOpt2 = arb[1];
						m_rbtOpt3 = arb[2];
						m_rbtOpt1.addActionListener(this);
						m_rbtOpt2.addActionListener(this);
						m_rbtOpt3.addActionListener(this);
						if (Utilities.checkForText(m_abyContent, m_abyContent.length)) m_rbtOpt1.setSelected(true);
						else m_rbtOpt2.setSelected(true);
					}
					
					m_jpExtendedPanel = new JPanel();
					m_jpExtendedPanel.setLayout(new BoxLayout(m_jpExtendedPanel, BoxLayout.X_AXIS));
					
					// "Other" option
					m_jpOther = new JPanel();
					m_jpOther.setLayout(new BoxLayout(m_jpOther, BoxLayout.Y_AXIS));
					m_jpOther.add(Box.createVerticalStrut(10));
					
					int nLabelWidth1 = determineFieldWidth("Record count (only for FIX)");
					int nValueWidth1 = determineFieldWidth("65536x");
					String[] asFormat = { "DIS/FIX", "DIS/VAR", "INT/FIX", "INT/VAR" };
					m_jcFormat = putComboBox(m_jpOther, "Format", asFormat, 0, nLabelWidth);
					m_tfRecLen = putTextField(m_jpOther, "Record length", "0", nLabelWidth, nValueWidth1);
//					m_tfL3Count = putTextField(m_jpOther, "Record count (only for FIX)", "0", nLabelWidth, nValueWidth1);

					// "BASIC" option
					m_jpBasic = new JPanel();
					m_jpBasic.setLayout(new BoxLayout(m_jpBasic, BoxLayout.Y_AXIS));
					m_jpBasic.add(Box.createVerticalStrut(10));
					String[] asVersion = { "Normal format", "Merge format", "Long format" };
					m_jcVersion = putComboBox(m_jpBasic, "Save in", asVersion, 0, nLabelWidth);
					m_chbProtected = putCheckBox(m_jpBasic, "LIST protection", false, nLabelWidth);
										
					add(m_jpExtendedPanel);

					if (m_nMode == BASIC) {
						setPanel(1);
					}
					
					switch(m_nPresetFormat) {
						case TFile.T_DISFIX: m_jcFormat.setSelectedIndex(0); break;
						case TFile.T_DISVAR: m_jcFormat.setSelectedIndex(1); break;
						case TFile.T_INTFIX: m_jcFormat.setSelectedIndex(2); break;
						case TFile.T_INTVAR: m_jcFormat.setSelectedIndex(3); break;
					}
					m_tfRecLen.setText(String.valueOf(m_nPresetRecLen));
//					m_tfL3Count.setText(String.valueOf(m_nPresetRecCount));
				}
				
				if (!fromEditor) {
					add(Box.createVerticalStrut(10));
					putTextLine(this, "!File contents", 0);
					add(Box.createVerticalStrut(10));
					Box box7 = new Box(BoxLayout.X_AXIS);
					m_taPreview = new JTextArea(8,25);
					box7.add(Box.createHorizontalStrut(10));		
					addField(box7, m_taPreview, 0, 100, 0);
					box7.add(Box.createHorizontalStrut(10));		
					add(box7);
					int nLength = m_abyContent.length;
					if (nLength > 128) nLength = 128;
					m_taPreview.setFont(Font.decode(TIImageTool.contentFont));
					m_taPreview.setText(Utilities.hexdump(0, 0, m_abyContent, nLength, false) + "..."); 
					m_taPreview.setEditable(false);
				}
			}
		}		
		add(Box.createVerticalStrut(10));
		add(Box.createVerticalGlue());
		addButtons();
	}
	
	public void actionPerformed(ActionEvent ae) {
		super.actionPerformed(ae);
		if (ae.getSource()==m_rbtOpt1) {
			m_jcFormat.setSelectedIndex(1);
			m_tfRecLen.setText("80");
			setPanel(0);
		}
		else {
			if (ae.getSource() == m_rbtOpt2) {
				setPanel((m_nMode==BASIC)? 1:2);
			}
			else {
				if (ae.getSource() == m_rbtOpt3) {
					setPanel((m_nMode==BASIC)? 3:4);
				}
			}
		}
	}
	
	private void setPanel(int option) {
		switch (option) {
			case 0:  // text only
			case 1:  // TI Basic
			case 2:  // binary
				m_jpExtendedPanel.removeAll();
				break;
			case 3:  // Extended Basic
				// m_jpExtendedPanel.add(Box.createHorizontalStrut(10));
				m_jpExtendedPanel.add(m_jpBasic);
				m_jpExtendedPanel.add(Box.createHorizontalGlue());
				break;
			case 4:  // other
				// m_jpExtendedPanel.add(Box.createHorizontalStrut(10));
				m_jpExtendedPanel.add(m_jpOther);
				m_jpExtendedPanel.add(Box.createHorizontalGlue());
				setRecordLength(128);
				setRecordCount((m_abyContent.length+127)/128);
				m_tfRecLen.setText(String.valueOf(m_nPresetRecLen));
//				m_tfL3Count.setText(String.valueOf(m_nPresetRecCount));
				break;
		}
		repaint();
		pack();
	}
	
	public String getFileName() {
		return m_tfFileName.getText().trim();
	}
	
	public void setContent(byte[] aby) {
		m_abyContent = aby;
	}

	boolean importAsText() {
		return m_rbtOpt1.isSelected() || (m_nMode!=BASIC && m_rbtOpt3.isSelected());
	}
	
	boolean importAsBasic() {
		return (m_nMode==BASIC && (m_rbtOpt2.isSelected() || m_rbtOpt3.isSelected()));
	}
	
	boolean importAsBinary() {
		return (m_nMode!=BASIC && (m_rbtOpt2.isSelected()));
	}

	boolean importAsFixed() {
		return (m_nMode!=BASIC && m_rbtOpt3.isSelected() && ((getFlags() & 0x80) == 0));
	}
	
	int getBasicVersion() {
		if (m_rbtOpt2.isSelected()) return BasicLine.TI_BASIC;
		if (m_rbtOpt3.isSelected()) return BasicLine.EX_BASIC;
		return 0;
	}

	int getSaveFormat() {
		int[] ver = { BasicCruncher.NORMAL, BasicCruncher.MERGE, BasicCruncher.LONG };
		return ver[m_jcVersion.getSelectedIndex()];
	}

	boolean getProtected() {
		return m_chbProtected.isSelected();
	}
	
	byte getFlags() {
		byte[] aby = { (byte)0x00, (byte)0x80, (byte)0x02, (byte)0x82, (byte)0x01 };
		if (m_rbtOpt1.isSelected()) return aby[1];
		return aby[m_jcFormat.getSelectedIndex()];
	}

	int getRecordLength() {
		if (m_rbtOpt1.isSelected()) return 80;
		try {
			return Integer.parseInt(m_tfRecLen.getText());
		}
		catch (NumberFormatException nbfx) {
			return -1;
		}
	}
/*	
	int getRecordCount() {
		try {
			return Integer.parseInt(m_tfL3Count.getText());
		}
		catch (NumberFormatException nbfx) {
			return -1;
		}
	}	
*/	
	boolean guessTheRest() {
		if (!m_bMulti) return false;
		return m_chbDontAsk.isSelected();
	}
	
	void setRecordLength(int nLength) {
		m_nPresetRecLen = nLength;
	}

	void setRecordCount(int nCount) {
		m_nPresetRecCount = nCount;
	}	
	
	void setFormat(int nFormat) {
		m_nPresetFormat = nFormat;
	}
}
