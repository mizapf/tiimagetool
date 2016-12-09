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

import de.mizapf.timt.assm.Hint;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.FormatException;

class DisassParamDialog extends ToolDialog implements ActionListener {
	
	JTextField 		m_tfOffset;
	JTextField 		m_tfStart;
	JTextField 		m_tfLength;
	JTextArea		m_taParameter;	
	JCheckBox		m_chbSkipAddresses;
	JCheckBox		m_chbShowAddresses;
	JCheckBox		m_chbShowData;
	JCheckBox		m_chbSymbolic;
	String			m_sSkip;	
	byte[]			m_abyFile;
	boolean			m_bGPL;
	boolean			m_bTagged;
	
	DisassParamDialog(JFrame owner, byte[] abyFile, boolean bGPL) {
		super(owner, "Disassembler parameters");
		m_abyFile = abyFile;
		m_bGPL = bGPL;
		m_bTagged = false;
	}
	
	void setType(boolean bPrg) {
		if (!bPrg) m_bTagged = true;
	}
	
/*
	| 	Disassembler Parameters									|

		Head of file: .... .... .... .... .... .... .... ....
		
		File offset:   [ 0000 ]
		Start address: [ 0000 ]
		Length:        [ 0000 ]
		
		Enter branch addresses and number of data words to be skipped
		as address:words (like a080:2)
		+------------------------------------------+
		| b73a:2                                   |
		| caa2:3                                   |
		+------------------------------------------+
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	void createGui(Font font) {
		m_bSet = false;

		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth("Skip invalid GROM addresses");
		int nColumnWidth1 = fm.stringWidth("XXXXXX");
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		add(Box.createVerticalStrut(10));		

		if (m_bGPL) {
			putTextLine(this, "!Parameters for GPL disassembly", 0);
		}
		else {
			putTextLine(this, "!Parameters for TMS99xx disassembly", 0);
		}

		add(Box.createVerticalStrut(10));		
		
		putLabel(this, "Head of file", Utilities.hexdump(0, 0, m_abyFile, 16, true), nColumnWidth);
				
		int start = Utilities.getInt16(m_abyFile, 4);
		int length = Utilities.getInt16(m_abyFile, 2)-6;
		int offset = 6;

		if (m_bGPL) {
			if (m_abyFile[0]==(byte)0xaa) {
				start = 0x0000;
				length = m_abyFile.length;
				offset = 0;
			}
		}
		
		if (m_bTagged) {
			putLabel(this, "Type", "Tagged object code", nColumnWidth);
			add(Box.createVerticalStrut(10));		
		}
		else {	
			putLabel(this, "Type", "Memory dump", nColumnWidth);
			add(Box.createVerticalStrut(10));		
			m_chbSymbolic = putCheckBox(this, "Symbolic disassembly", false, nColumnWidth);			
			add(Box.createVerticalStrut(10));		
			m_tfOffset = putTextField(this, "File offset (hex)", Utilities.toHex(offset, 4), nColumnWidth, nColumnWidth1);
			m_tfStart = putTextField(this, "Start address (hex)", Utilities.toHex(start, 4), nColumnWidth, nColumnWidth1);
			m_tfLength = putTextField(this, "Length (hex)", Utilities.toHex(length, 4), nColumnWidth, nColumnWidth1);
			
			if (m_bGPL) {
				m_chbSkipAddresses = putCheckBox(this, "Skip invalid GROM addresses", true, nColumnWidth);
			}
		}

		m_chbShowAddresses = putCheckBox(this, "Show locations at line end", false, nColumnWidth);
		m_chbShowData = putCheckBox(this, "Show raw data at line end", false, nColumnWidth);
		add(Box.createVerticalStrut(10));		

		putTextLine(this, "!Disassembler hints", 0);
		add(Box.createVerticalStrut(10));
		
		putTextLine(this, "You can specify which parts of the code are data or text areas.", 0);
		add(Box.createVerticalStrut(10));

		putTextLine(this, "data(from,to): Locations which should be interpreted as DATA", 0);
		putTextLine(this, "text(from,to): Locations which should be interpreted as TEXT/BYTE", 0);

		if (m_bGPL) {
			putTextLine(this, "btext(from,to): Locations which should be interpreted as BTEXT/BYTE", 0);
			putTextLine(this, "param(proc,count): CALL to this location is followed by a fixed number of parameter bytes", 0);
			putTextLine(this, "nofmt(from,to): Inhibit FMT disassembly in this area", 0);
			add(Box.createVerticalStrut(10));
		}
		else {
			putTextLine(this, "param(proc,count): BL/BLWP to this location is followed by a fixed number of data words", 0);
			if (m_bTagged) {
				putTextLine(this, "ref(location): This location is referenced as a data source or destination or a jump/branch target", 0);
			    add(Box.createVerticalStrut(10));
			    putMultiTextLine(this, "Locations can be specified as absolute (837c, >837C, 0x837c, X837C), as program-relocatable (R0010),\nas common-relocatable (S0123), or as data-relocatable (T0028).");
			    add(Box.createVerticalStrut(10));
			    putTextLine(this, "A branch target can be specified as a location or by its name, like param(R0418,2) or param(\"XMLLNK\",1).", 0);
			}
		}
		add(Box.createVerticalStrut(10));

		putTextLine(this, "Separate specifications by spaces, commas, or newlines.",0);
		add(Box.createVerticalStrut(10));
		
		Box box7 = new Box(BoxLayout.X_AXIS);
		m_taParameter = new JTextArea(10,20);
		m_taParameter.setLineWrap(true);
		box7.add(Box.createHorizontalStrut(10));		
		addField(box7, m_taParameter, 0, 100, 0);
		box7.add(Box.createHorizontalStrut(10));		
		add(box7);

		add(Box.createVerticalGlue());

		addButtons();
	}
		
	int getStartAddress() {
		if (m_tfStart==null) return 0;
		return Integer.parseInt(m_tfStart.getText(), 16);
	}
	
	boolean symbolic() {
		if (m_chbSymbolic==null) return false;
		else return m_chbSymbolic.isSelected();
	}
	
	int getOffset() {
		if (m_tfOffset==null) return 0;
		return Integer.parseInt(m_tfOffset.getText(), 16);
	}
	
	int getLength() {
		if (m_tfLength==null) return 0;
		return Integer.parseInt(m_tfLength.getText(), 16);
	}
	
	boolean skipInvalidAddresses() {
		if (m_chbSkipAddresses==null) return false;
		return m_chbSkipAddresses.isSelected();
	}
	
	int showDataLoc() {
	    int nValue = 0;
	    if (m_chbShowAddresses!=null) {
	        if (m_chbShowAddresses.isSelected()) nValue |= Hint.OPT_SHOWLOC;
	    }

	    if (m_chbShowData!=null) {
	        if (m_chbShowData.isSelected()) nValue |= Hint.OPT_SHOWDATA;
	    }
		return nValue;
	}
	
	Hint[] getHint() throws FormatException {
		if (m_taParameter.getText().length()==0) return new Hint[0];
		else return Hint.parse(m_taParameter.getText()); 
	}
	
	void setParams(String sParam) {
		int start = 0;
		int length = 0;
		int offset = 0;
		boolean noinv = false;
		String sSkip = null;
		
		if (sParam == null) {
			m_taParameter.setText("");
			start = Utilities.getInt16(m_abyFile, 4);
			length = Utilities.getInt16(m_abyFile, 2)-6;
			offset = 6;
		}
		else {
			String[] as = sParam.split("#");
			try {
				start = Integer.parseInt(as[0], 16);
				offset = Integer.parseInt(as[1], 16);
				length = Integer.parseInt(as[2], 16);
				noinv = (Integer.parseInt(as[3])!=0);
				if (as.length>4) sSkip = as[4];
				else sSkip = "";
			}
			catch (NumberFormatException nfx) {
				start = 0;
				offset = 0;
				length = 0;
				noinv = false;
				System.err.println("Invalid disassembler parameters: " + sParam);
			}
			if (m_tfStart != null) m_tfStart.setText(Utilities.toHex(start,4));
			if (m_tfOffset != null) m_tfOffset.setText(Utilities.toHex(offset, 4));
			if (m_tfLength != null) m_tfLength.setText(Utilities.toHex(length, 4));
			if (m_chbSkipAddresses != null) m_chbSkipAddresses.setSelected(noinv);
			m_taParameter.setText(sSkip);
		}
	}
}
