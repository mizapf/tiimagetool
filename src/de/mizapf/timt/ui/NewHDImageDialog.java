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
import javax.swing.event.*;
import de.mizapf.timt.files.FormatParameters;
import de.mizapf.timt.files.Time;
import de.mizapf.timt.util.Utilities;

class NewHDImageDialog extends ToolDialog implements ActionListener, FocusListener {
	
	JTextField 		m_tfName;
	JTextField		m_tfCylinders;
	JTextField		m_tfHeads;
	JTextField		m_tfSectors;
	JComboBox<String> m_jcSectorLength;
	JComboBox<String> m_jcCHDVersion;

	JLabel			m_jlSize;
	
	JCheckBox		m_chbFormat;	
	JCheckBox		m_chbAdvanced;	

	JPanel			m_jpAdvanced;
	JPanel			m_jpAdvancedFrame;
	
	JTextField 		m_tfReserved;
	JTextField		m_tfPrecomp;
	JTextField		m_tfReduced;
	JTextField		m_tfStep;
	JComboBox<String> m_jcFilesystem;
	JCheckBox		m_chbBuffered;	
	
	NewHDImageDialog(JFrame owner) {
		super(owner, "Create new hard disk image");
	}
	
/*
	| 	Create new hard disk image										|

		Volume name			[EMPTY________]
		Cylinders           [615]
		Heads               [4]
		Sectors/track       [32]
		Sector length       [v 256]
		MESS CHD version    [v 5]
	
		Format              [x]
		Advanced options    [x]

		Reserved AUs        [2048]
		Write precomp       [464]
		Reduced current     [464]
		Step rate           [1]
		File system			[v HFDC]
		Buffered step		[x]
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+

Default values:
	Total sectors	reserved		precomp ~ cylinders/22 * 16
	< 19712			512				reduced ~ cylinders/22 * 16
	< 65536			1024
	< 131072		2048
	< 196608		4096
	< 327680		8192
	more			16384
	
				
*/	
	public void createGui(Font font) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth("Hard disk size will be 1234 MiB");
		int nFieldWidth = fm.stringWidth("XXXXXXXXXXXX");

		add(Box.createVerticalStrut(10));		

		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		m_tfName = putTextField(this, "Volume name",  "HARDDISK", nColumnWidth, nFieldWidth);
		
		m_tfCylinders = putTextField(this, "Cylinders", "615", nColumnWidth, nFieldWidth);
		m_tfCylinders.addFocusListener(this);		
		
		m_tfHeads = putTextField(this, "Heads", "4", nColumnWidth, nFieldWidth);
		m_tfHeads.addFocusListener(this);

		m_tfSectors = putTextField(this, "Sectors per track", "32", nColumnWidth, nFieldWidth);
		m_tfSectors.addFocusListener(this);
		
		String[] asOpti = { "256", "512" };
		m_jcSectorLength = putComboBox(this, "Sector length", asOpti, 0, nColumnWidth);

		String[] asOptio = { "4", "5" };
		m_jcCHDVersion = putComboBox(this, "MESS CHD version", asOptio, 1, nColumnWidth);
		
		m_chbFormat = putCheckBox(this, "Format hard disk", true, nColumnWidth);
		m_chbFormat.addActionListener(this);

		m_chbAdvanced = putCheckBox(this, "Advanced options", false, nColumnWidth);
		m_chbAdvanced.addActionListener(this);

		m_jpAdvancedFrame = new JPanel();

		m_jpAdvanced = new JPanel();
		m_jpAdvanced.setLayout(new BoxLayout(m_jpAdvanced, BoxLayout.Y_AXIS));

		// add(Box.createVerticalStrut(10));

		m_tfReserved = putTextField(m_jpAdvanced, "Reserved AUs", "2048", nColumnWidth, nFieldWidth); 
		m_tfPrecomp = putTextField(m_jpAdvanced, "Write precompensation", "464", nColumnWidth, nFieldWidth); 
		m_tfReduced = putTextField(m_jpAdvanced, "Reduced write current", "464", nColumnWidth, nFieldWidth); 
		m_tfStep = putTextField(m_jpAdvanced, "Step rate", "1", nColumnWidth, nFieldWidth);	

		String[] asOptions = { "HFDC", "SCSI" };
		m_jcFilesystem = putComboBox(m_jpAdvanced, "File system", asOptions, 0, nColumnWidth);

		m_chbBuffered = putCheckBox(m_jpAdvanced, "Buffered step", false, nColumnWidth);
		add(m_jpAdvancedFrame);
		add(Box.createVerticalStrut(10));

		add(Box.createVerticalGlue());

		addButtons();
		updateFields();
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==m_chbAdvanced || ae.getSource()==m_chbFormat) {
			setActive(m_chbAdvanced.isSelected() && m_chbFormat.isSelected());
		}
		super.actionPerformed(ae);
	}
	
	public void focusGained(FocusEvent fe) {
		updateFields();
	}

	public void focusLost(FocusEvent fe) {
		updateFields();
	}
	
	private void setActive(boolean bActive) {
		if (bActive) {
			m_jpAdvancedFrame.add(m_jpAdvanced);
		}
		else {
			m_jpAdvancedFrame.remove(m_jpAdvanced);
		}
		repaint();
		pack();
	}
	
	private void updateFields() {
		try {
			int nCyl = Integer.parseInt(m_tfCylinders.getText());
			int nHead = Integer.parseInt(m_tfHeads.getText());
			int nSect = Integer.parseInt(m_tfSectors.getText());
			
			int[] param = Utilities.guessAdvanced(nCyl, nHead, nSect);
			
			m_tfReserved.setText(String.valueOf(param[0]));  // nRes
			m_tfPrecomp.setText(String.valueOf(param[1]));  // nPre
			m_tfReduced.setText(String.valueOf(param[1]));  // nPre
		}
		catch (NumberFormatException nfx) {
			m_tfReserved.setText("0");
			m_tfPrecomp.setText("0");
			m_tfReduced.setText("0");
		}
	}
	
	public String getVolumeName() {
		return m_tfName.getText();
	}
	
	public int getCylinders() {
		try {
			return Integer.parseInt(m_tfCylinders.getText());
		}
		catch (NumberFormatException nfx) {
			return -1;
		}
	}
	
	public int getHeads() {
		try {
			return Integer.parseInt(m_tfHeads.getText());
		}
		catch (NumberFormatException nfx) {
			return -1;
		}
	}
	
	public int getSectors() {
		try {
			return Integer.parseInt(m_tfSectors.getText());
		}
		catch (NumberFormatException nfx) {
			return -1;
		}
	}

	public int getSectorLength() {
		int[] aVer = { 256, 512 };
		return aVer[m_jcSectorLength.getSelectedIndex()];
	}

	public boolean formatImage() {
		return m_chbFormat.isSelected();
	}
	
	public boolean forHfdc() {
		return m_jcFilesystem.getSelectedIndex()==0;
	}
	
	public int getReserved() {
		try {
			return Integer.parseInt(m_tfReserved.getText());
		}
		catch (NumberFormatException nfx) {
			return -1;
		}
	}

	public int getWritePrecompensation() {
		try {
			return Integer.parseInt(m_tfPrecomp.getText());
		}
		catch (NumberFormatException nfx) {
			return -1;
		}
	}

	public int getReducedWriteCurrent() {
		try {
			return Integer.parseInt(m_tfReduced.getText());
		}
		catch (NumberFormatException nfx) {
			return -1;
		}
	}

	public int getStepRate() {
		try {
			return Integer.parseInt(m_tfStep.getText());
		}
		catch (NumberFormatException nfx) {
			return -1;
		}
	}
	
	public boolean getBufferedStep() {
		return m_chbBuffered.isSelected();
	}
	
	public int getCHDVersion() {
		int[] aVer = { 4, 5 };
		return aVer[m_jcCHDVersion.getSelectedIndex()];
	}
	
	public FormatParameters getFormatParameters() {
		return new FormatParameters(getVolumeName(), getCylinders(), getHeads(), 
			getSectors(), getSectorLength(), getReserved(), getStepRate(), 
			getReducedWriteCurrent(), 0, getBufferedStep(), 
			getWritePrecompensation(), Time.createNow(), formatImage(), 
			forHfdc(), getCHDVersion());
	}
}