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
    
    Copyright 2023 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import de.mizapf.timt.files.ImageFormat;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.files.FormatParameters;
import de.mizapf.timt.files.HarddiskFileSystem;
import de.mizapf.timt.files.Time;

class NewIDEImageDialog extends ToolDialog implements ActionListener, FocusListener {
	
	JTextField 		m_tfCapacity;
	JTextField[]	m_atfName;	

	JTextField[]	m_atfSize;
		
	int 			m_nFullCapacity;
	int[] 			m_anSize;

	String[]		m_asName;
	
	NewIDEImageDialog(JFrame owner) {
		super(owner, TIImageTool.langstr("Title.IDE.CreateNew"));
		m_atfName = new JTextField[8];
		m_atfSize = new JTextField[8];
		m_anSize = new int[8];
	}
	
/*				
		| 	Create new partitioned IDE image										|
	
	    Partitions can be created from 1 to 8, but without gaps. For partitions
	    beyond 4 the IDE DSR must be updated to release 1.6.

		Full capacity          [...]
	
		Number	Name	Size 
		1       [...]   [...]
		2       [...]   [...]
		3       [...]   [...]
		4       [...]   [...]
		5       [...]   [...]
		6       [...]   [...]
		7       [...]   [...]
		8       [...]   [...]
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+			+-----------+
				
		Completion rules:
		- If the capacity is less than the sum in the size fields, update the
		  capacity
		- If the capacity is higher than the sum in the size fields, suggest the
		  difference for the next partition if more than 10 MiB
		- If a new size or capacity is entered, check the above rules
		- When Return is pressed, move forward from name to size, and from size
		  to the next name, check the above rules
		- When the focus is lost, check the above rules
		- When an empty name is entered, clear all following fields (you can 
		  still click cancel)
		- When an invalid size is entered, restore the previous entry
		- When size 0 is entered, clear the name and all following fields
	    - When an invalid name is entered, restore the previous entry
	    - When an invalid capacity is entered, fill it with the sum of the sizes
	      or 200
	    - Start with a capacity of 200
*/	
	public void createGui(Font font) {
		prepareGui();
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = determineMaxWidth(font, TIImageTool.langstr("Image.IDE.FullCapacity"), TIImageTool.langstr("VolumeName"));

		int nColumnWidth1 = getBoldWidth(TIImageTool.langstr("Dialog.IDE.Partition"));
		int nColumnWidth2 = getColumnWidth(10);
		int nColumnWidth3 = getBoldWidth(TIImageTool.langstr("Capacity"));
	
		int nFieldWidth = fm.stringWidth("XXXXXXXXXXXX");

		putMultiTextLine(this, TIImageTool.langstr("Dialog.IDE.Header"));
		putMultiTextLine(this, TIImageTool.langstr("Dialog.IDE.Upgrade"));
		putMultiTextLine(this, TIImageTool.langstr("Dialog.IDE.HeaderUnpart"));
		add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));
		
//		add(vspace(120));
		// Max capacity is 4*248 = 992 MiB
		m_tfCapacity = putTextField(this,  TIImageTool.langstr("Image.IDE.FullCapacity"), "200", nColumnWidth, nFieldWidth); 
		add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));
		
		//		add(vspace(80));		
		m_tfCapacity.addActionListener(this);	
		m_tfCapacity.addFocusListener(this);	

		JPanel jp = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
				
		// Create table title
		jp.add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));
		Box title = new Box(BoxLayout.X_AXIS);
		title.add(Box.createHorizontalStrut(TIImageTool.dialogHeight/2));
		JLabel jlt1 = new JLabel(TIImageTool.langstr("Dialog.IDE.Partition"), SwingConstants.LEFT);
		jlt1.setFont(TIImageTool.boldDialogFont);
		addField(title, jlt1,  nColumnWidth1, TIImageTool.dialogHeight,  0);
		
		JLabel jlt2 = new JLabel(TIImageTool.langstr("Dialog.IDE.Name"), SwingConstants.LEFT);
		jlt2.setFont(TIImageTool.boldDialogFont);
		addField(title, jlt2,  nColumnWidth2, TIImageTool.dialogHeight,  0);
		
		JLabel jlt3 = new JLabel(TIImageTool.langstr("Capacity"), SwingConstants.LEFT);
		jlt3.setFont(TIImageTool.boldDialogFont);
		addField(title, jlt3,  nColumnWidth3, TIImageTool.dialogHeight,  0);
		
		jp.add(title);
		
		for (int i=1; i <=8; i++) {
			jp.add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));

			JTextField[] atf = putTextFieldN(jp, String.valueOf(i), 2, nColumnWidth1, nFieldWidth);

			m_atfName[i-1] = atf[0];
			m_atfSize[i-1] = atf[1];

			m_atfName[i-1].addActionListener(this);
			m_atfName[i-1].addFocusListener(this);

			m_atfSize[i-1].addActionListener(this);	
			m_atfSize[i-1].addFocusListener(this);
		}
		
		JScrollPane jsp = new JScrollPane(jp);
		add(jsp);

		add(Box.createVerticalGlue());

		m_asName = new String[8];
		for (int i=0; i < 8; i++) m_asName[i] = "";
		m_nFullCapacity = 200;

		m_tfCapacity.addKeyListener(this);
		m_atfName[0].setText("PART1");
		changeName(0, "PART1");
		
		addButtons();
	}
	
	private int calcFullCapacity(int skip) {
		int capa = 0;
		for (int i=0; i < 8; i++) {
			if (i != skip) {
				if (isValidName(m_atfName[i].getText()) && isValidSize(m_atfSize[i].getText())) {
					capa += m_anSize[i];
				}
				else break;
			}
		}
		return capa;
	}
	
	private boolean isValidName(String sName) {
		if (sName == null) return false;
		if ((sName.length() < 1) || (sName.length() > 10)) return false;
		for (int i=0; i < sName.length(); i++) {
			if ((sName.charAt(i) <= 32) || (sName.charAt(i) > 126) || (sName.charAt(i) == '.')) return false;
		}
		return true;
	}
	
	private boolean isValidSize(String sValue) {
		int value = 0;
		try {
			value = Integer.parseInt(sValue);
		}
		catch (NumberFormatException nfx) {
			return false;
		}
		return ((value >=0 ) && (value < 249));
	}
	
	private void changeName(int i, String s) {
//		System.out.println("changeName(" + i + ", " + "\"" + s + "\")");
		if (s.trim().length()==0) {
			m_atfSize[i].setText("");
			m_anSize[i] = 0;
			if (i < 7) {
				m_atfName[i+1].setText("");
				changeName(i+1, "");
				m_atfSize[i+1].setText("");
				m_anSize[i+1] = 0;			
			}
		}
		else {
			if (isValidName(s)) {
				int count = getDefinedPartitions();
				if (i <= count) {
					m_asName[i] = s;
					int remain = m_nFullCapacity;
					if (getDefinedPartitions() > 0)
						remain = m_nFullCapacity - calcFullCapacity(i);
				
					if (remain >= 10) {
						if (remain > 248) remain = 248; 
						m_atfSize[i].setText(String.valueOf(remain));
						changeSize(i, String.valueOf(remain));
					}
				}
				else {
					m_atfName[i].setText("");
					changeName(i, "");
				}
			}
			else m_atfName[i].setText(m_asName[i]);  // restore old entry
		}
	}
	
	private void changeSize(int i, String s) {
//		System.out.println("changeSize(" + i + ", " + "\"" + s + "\")");
		int size = 0;
		if (isValidSize(s)) {
			size = Integer.parseInt(s);
		}
		else {
			if ((m_asName[i].length() > 0) && (m_anSize[i] > 0))
				m_atfSize[i].setText(String.valueOf(m_anSize[i]));
			else
				changeSize(i, "0");
			return;
		}
		if (size == 0) {
			m_atfName[i].setText("");
			changeName(i, "");
		}
		else {
			m_anSize[i] = size;
			int nCap = calcFullCapacity(-1);
			if (nCap > m_nFullCapacity) {
				m_nFullCapacity = nCap;
				m_tfCapacity.setText(String.valueOf(m_nFullCapacity));
			}
		}
	}
	
	// Only if there is a name, the partition is valid, and the value of the size is shown
	// Triggered when the Return key is pressed one of the text fields
	public void actionPerformed(ActionEvent ae) {

		if (ae.getSource()==m_tfCapacity) {
			try {
				m_nFullCapacity = Integer.parseInt(m_tfCapacity.getText());
			}
			catch (NumberFormatException nfx) {
				m_tfCapacity.setText(String.valueOf(calcFullCapacity(-1)));
			}
		}
		
		for (int i=0; i < 8; i++) {
			if (ae.getSource()==m_atfName[i]) {
				// Changes are done by the focus listener
				m_atfSize[i].grabFocus();
			}
			else {
				if (ae.getSource()==m_atfSize[i]) {
					// Changes are done by the focus listener
					m_atfName[(i+1)%8].grabFocus();
				}
			}
		}
				
		super.actionPerformed(ae);
	}
		
	// Triggered when the dialog (and any of its fields) is selected
	public void focusGained(FocusEvent fe) {
		// System.out.println("Focus gained");
	}

	public int getCapacity() {
		return m_nFullCapacity;
	}
	
	public void focusLost(FocusEvent fe) {
		// System.out.println("Focus lost");
		if (fe.getSource()==m_tfCapacity) {
			try {
				m_nFullCapacity = Integer.parseInt(m_tfCapacity.getText());
			}
			catch (NumberFormatException nfx) {
				m_tfCapacity.setText(String.valueOf(calcFullCapacity(-1)));
			}
		}

		for (int i=0; i < 8; i++) {
			if (fe.getSource()==m_atfName[i]) {
				changeName(i, m_atfName[i].getText());
				// System.out.println("Left field name " + (i+1));
			}
			else {
				if (fe.getSource()==m_atfSize[i]) {
					changeSize(i, m_atfSize[i].getText());
					// System.out.println("Left field size " + (i+1));
				}
			}
		}
	}
	
	private int getDefinedPartitions() {
		for (int i=0; i < 8; i++) {
			if (m_atfName[i].getText().trim().length()==0) return i;
		}
		return 8;
	}
	
	FormatParameters getParameters(int part) {
		FormatParameters params = new FormatParameters(m_asName[part], true);
		params.setHD(Time.createNow(), getAUSize(part), getReserved(m_anSize[part]), HarddiskFileSystem.SCSI);
		
		int count = getDefinedPartitions();
		int i = m_anSize[part] * 4096;
		
		// One sector (512 bytes) contains the partition table; take it from the size
		// of the last partition
		if (part == count-1) 
			i = i-2;  
			
		params.setTotal(i);
		return params;
	}
	
	int getAUSize(int i) {
		// Max AU = 63488 = 31 * 2048
		// Sectors = cap * 4096
		
		// AU size = sectors / max = cap * 4096 / (31 * 2048)
		//          = cap * 2 / 31
		double nAUSize = (m_anSize[i]*2)/31.0;
		if (nAUSize > 8) return 16;
		if (nAUSize > 4) return 8;
		if (nAUSize > 2) return 4;
		if (nAUSize > 1) return 2;
		return 1;
	}
	
	private int getReserved(int sizeMB) {
	
		int res = 0;
		// For small drives, double the reserved AUs every 16 MiB
		if (sizeMB < 50) {
			res = 2 << (sizeMB/16 + 9);
		}
		else {
			// For bigger drives, increase the reserved AUs linearly up to FF
			res = ((sizeMB-50)*2/3 + 128) * 64;
		}
		
		if (res > 16320) res = 16320;
		if (res < 512) res = 512;
		return res;
		
		// SCSI: max=0xe6*0x40 = 14720  (bei 248 MiB)
	}
	
	int[] getPartitionSizes() {
		int[] psize = new int[getDefinedPartitions()];
		for (int i=0; i < psize.length; i++) {
			psize[i] = m_anSize[i];
		}
		
		return psize; 
	}
}