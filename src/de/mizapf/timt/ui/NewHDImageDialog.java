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
import de.mizapf.timt.TIImageTool;

class NewHDImageDialog extends ToolDialog implements ActionListener, FocusListener {
	
	// TODO: Add standard hard disk layouts
	
	JTextField 		m_tfName;
	JComboBox<String> m_jcDrive;
	JTextField		m_tfCapacity;

	
	JTextField		m_tfCylinders;
	JTextField		m_tfHeads;
	JTextField		m_tfSectors;

	JComboBox<String> m_jcSectorLength;
//	JComboBox<String> m_jcCHDVersion;

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
	
	int m_nType;
	
	/** Constructor
		@param owner Frame which contains this dialog
		@param type HarddiskImageFormat.{SCSI|IDE|HFDC}
	*/
	NewHDImageDialog(JFrame owner, int type) {
		super(owner, TIImageTool.langstr("NewImageTitle"));
		m_nType = type;
	}
	
/*


Default values:
	Total sectors	reserved		precomp ~ cylinders/22 * 16
	< 19712			512				reduced ~ cylinders/22 * 16
	< 65536			1024
	< 131072		2048
	< 196608		4096
	< 327680		8192
	more			16384


              	+---------------+
		New > 	| Floppy image  | -> memory image
				| CF7 image     | -> save to disk / open dialog - new - overwrite? 
				| IDE image     | -> open dialog / new / overwrite?
				| SCSI image    | -> save to disk
				| HFDC image    | -> save to disk
				+---------------+
				
				
				
				
		| 	Create new HFDC disk image										|

		Volume name			[EMPTY________]
		Drive				[v ST213 | ST225 | ST251 | generic]
		
		Capacity (MiB)		[32] (auto)
		Reserved AUs		[2048] (auto)

		Cylinders			[615] (auto)
		Heads				[4]   (auto)
		Sectors/track		[32]  (auto)
		Write precomp		[464] (auto)
		Reduced current		[464] (auto)
		Step rate			[1] (auto)
		Buffered step		[x] (auto)

				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+

				
		| 	Create new SCSI disk image										|

		Volume name			[EMPTY________]
		
		Size (MiB)			[32]
		Reserved AUs		[2048] (auto)

				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+

				
		| 	Create new IDE disk image										|

		Volume name			[EMPTY________]
		
		Size (MiB)			[32]
		Reserved AUs		[2048] (auto)
		
		Create as partition [ ]

				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
		
*/	
	public void createGui(Font font) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("NewHDColumn"));
		int nFieldWidth = fm.stringWidth("XXXXXXXXXXXX");
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(vspace(50));		

		m_tfName = putTextField(this, TIImageTool.langstr("VolumeName"), "HARDDISK", nColumnWidth, nFieldWidth);
		add(vspace(50));		
		
		String[] asDrive = { TIImageTool.langstr("Generic"), "ST-213", "ST-225", "ST-251"  };
		m_jcDrive = putComboBox(this, TIImageTool.langstr("Drive"), asDrive, 0, nColumnWidth);
		m_jcDrive.addActionListener(this);
		add(vspace(50));		

		m_tfCapacity = putTextField(this, TIImageTool.langstr("Capacity"), "0", nColumnWidth, nFieldWidth); 
		add(vspace(50));		
				
		m_tfCylinders = putTextField(this, TIImageTool.langstr("Cylinders"), "615", nColumnWidth, nFieldWidth);
		m_tfCylinders.addFocusListener(this);		
		
		m_tfHeads = putTextField(this, TIImageTool.langstr("Heads"), "4", nColumnWidth, nFieldWidth);
		m_tfHeads.addFocusListener(this);

		m_tfSectors = putTextField(this, TIImageTool.langstr("SectorsPerTrack"), "32", nColumnWidth, nFieldWidth);
		m_tfSectors.addFocusListener(this);
		
		m_tfReserved = putTextField(this, TIImageTool.langstr("NewHDReservedAUs"), "2048", nColumnWidth, nFieldWidth); 
		m_tfPrecomp = putTextField(this, TIImageTool.langstr("WritePC"), "464", nColumnWidth, nFieldWidth); 
		m_tfReduced = putTextField(this, TIImageTool.langstr("ReducedWC"), "464", nColumnWidth, nFieldWidth); 
		m_tfStep = putTextField(this, TIImageTool.langstr("StepSpeed"), "1", nColumnWidth, nFieldWidth);	

		m_chbBuffered = putCheckBox(this, TIImageTool.langstr("BufferedStep"), true, nColumnWidth);
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
	
	String getFormatClass() {
		return "NoSuchClass";
	}
	
	public FormatParameters getFormatParameters() {
		FormatParameters param = new FormatParameters(getVolumeName(), getFormatClass(), /* toBeFormatted() */ true);
		return param;
	}
}

/*
Alternativen:
Oder?

IDE: Neue IDE-Partitionstabelle; direkt speichern
IDE: Bestehende Datei öffnen; anbieten, Partitionen zu ändern, zu löschen, zu öffnen oder neu anzulegen

Oder?

		Create as 			[v partition | single image]  -> File dialog
		
Oder
              	+--------------------+
		New > 	| Floppy disk image  | -> memory image
				| CF7 image          | -> save to disk
				| CF7 partition      | -> open dialog / new / overwrite?
				| IDE partition table| -> save to disk
				| IDE partition      | -> open dialog / new / overwrite?
				| SCSI image         | -> save to disk
				| IDE image          | -> save to disk
				| HFDC image         | -> save to disk
				+--------------------+
				

*/