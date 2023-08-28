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
import de.mizapf.timt.files.HarddiskFileSystem;
import de.mizapf.timt.files.Time;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;

class NewHDImageDialog extends ToolDialog implements ActionListener, FocusListener {
	
	JTextField 		m_tfName;
	JTextField 		m_tfSize;

	JCheckBox		m_chbAdvanced;	

	JTextField		m_tfCylinders;
	JTextField		m_tfHeads;
	JTextField		m_tfSectors;

	JComboBox<String> m_jcDrive;
	JComboBox<String> m_jcSize;
	
	JPanel			m_jpAU;
	JPanel			m_jpExpert;
	JPanel			m_jpExpertPanel;

	JTextField 		m_tfReserved;
	JTextField		m_tfPrecomp;
	JTextField		m_tfReduced;
	JTextField		m_tfStep;
	JCheckBox		m_chbBuffered;	
	
	int[][] geometries = { {615, 2, 32}, {615, 4, 32}, {820, 6, 32} };
	
	/** Constructor
		@param owner Frame which contains this dialog
	*/
	NewHDImageDialog(JFrame owner) {
		super(owner, TIImageTool.langstr("Image.CreateNewHDImage"));
	}

/*
		| 	Create new hard disk image										|

		Volume name			[EMPTY________]
		Type                (*) MFM    ( ) SCSI
		
		Drive				[v generic SCSI | generic MFM | ST213 MFM | ST225 MFM | ST251 MFM ]
		Size				[auto for STxxx]
		AU size				1|2|4|8|16 (fixed)
		
		// generic
		Capacity (MiB)		[32] (auto)
		
		// not for SCSI
		Cylinders			[615] (auto)
		Heads				[4]   (auto)
		Sectors/track		[32]  (auto)
		
		// expert options  (dep on type)
		Reserved AUs		[2048] (auto)
		Write precomp		[464] (auto)
		Reduced current		[464] (auto)
		Step rate			[1] (auto)
		Buffered step		[x] (auto)

				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+       	+-----------+
				
		AU sizes: max 63488 AUs
		
		<= 15.5 MiB: 1 
		<= 31   MiB: 2
		<= 62   MiB: 4
		<= 124  MiB: 8
		<= 248  MiB: 16
		Fixed sizes: 16, 32, 64, 128, 256
*/
	public void createGui(Font font) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("NewHDColumn"));
		int nFieldWidth = fm.stringWidth("XXXXXXXXXXXX");
		add(vspace(50));
		m_tfName = putTextField(this, TIImageTool.langstr("VolumeName"), "HARDDISK", nColumnWidth, nFieldWidth);
		add(vspace(50));
		
		String[] asDrive = { TIImageTool.langstr("Generic") + " SCSI", TIImageTool.langstr("Generic") + " MFM", "ST-213 MFM", "ST-225 MFM", "ST-251 MFM"  };
		m_jcDrive = putComboBox(this, TIImageTool.langstr("Drive"), asDrive, 0, nColumnWidth);
		m_jcDrive.addActionListener(this);
		add(vspace(50));
		
		m_tfSize = putTextField(this, TIImageTool.langstr("Capacity"), "248", nColumnWidth, nFieldWidth);
		m_tfSize.addActionListener(this);	
		m_tfSize.addFocusListener(this);		
	
		m_chbAdvanced = putCheckBox(this, TIImageTool.langstr("NewHDAdvancedOptions"), false, nColumnWidth);
		m_chbAdvanced.addActionListener(this);
		m_chbAdvanced.setSelected(false);
		m_chbAdvanced.setEnabled(true);
		
		m_jpExpertPanel = new JPanel();
		m_jpExpertPanel.setLayout(new BoxLayout(m_jpExpertPanel, BoxLayout.Y_AXIS));
		
		m_jpExpert = new JPanel();
		m_jpExpert.setLayout(new BoxLayout(m_jpExpert, BoxLayout.Y_AXIS));
		
		m_tfCylinders = putTextField(m_jpExpert, TIImageTool.langstr("Cylinders"), "0", nColumnWidth, nFieldWidth);
		m_tfCylinders.addActionListener(this);		
		m_tfCylinders.addFocusListener(this);		
		
		m_tfHeads = putTextField(m_jpExpert, TIImageTool.langstr("Heads"), "0", nColumnWidth, nFieldWidth);
		m_tfHeads.addActionListener(this);
		m_tfHeads.addFocusListener(this);		

		m_tfSectors = putTextField(m_jpExpert, TIImageTool.langstr("SectorsPerTrack"), "0", nColumnWidth, nFieldWidth);
		m_tfSectors.addActionListener(this);
		m_tfSectors.addFocusListener(this);		
		
		m_tfPrecomp = putTextField(m_jpExpert, TIImageTool.langstr("WritePC"), "464", nColumnWidth, nFieldWidth); 
		m_tfReduced = putTextField(m_jpExpert, TIImageTool.langstr("ReducedWC"), "464", nColumnWidth, nFieldWidth); 
		m_tfStep = putTextField(m_jpExpert, TIImageTool.langstr("StepSpeed"), "1", nColumnWidth, nFieldWidth);	
		m_chbBuffered = putCheckBox(m_jpExpert, TIImageTool.langstr("BufferedStep"), true, nColumnWidth);

		m_jpAU = new JPanel();
		m_jpAU.setLayout(new BoxLayout(m_jpAU, BoxLayout.Y_AXIS));
		m_tfReserved = putTextField(m_jpAU, TIImageTool.langstr("NewHDReservedAUs"), "0", nColumnWidth, nFieldWidth); 
		
		add(vspace(10));
		add(m_jpExpertPanel);
		
		add(Box.createVerticalGlue());
		setReserved();
		addButtons();
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==m_jcDrive) {
			setSizeActive(m_jcDrive.getSelectedIndex());
			setReserved();
			// m_chbAdvanced.setEnabled(m_jcDrive.getSelectedIndex()>=1);
		}
		if (ae.getSource()==m_chbAdvanced) {
			setExpertActive(m_chbAdvanced.isSelected(), m_jcDrive.getSelectedIndex());
		}
		if (ae.getSource()==m_tfSize) {
			setGeomAndCheck();
		}
		if (ae.getSource()==m_tfCylinders || ae.getSource() == m_tfHeads || ae.getSource() == m_tfSectors) {
			setSizeAndCheck();
		}
		super.actionPerformed(ae);
	}
	
	private void setGeomAndCheck() {
		try {
			int size = Integer.parseInt(m_tfSize.getText());
			setGeometry(calculateGeometry(size));
			setReserved();
		}
		catch (NumberFormatException nfx) {
			m_tfSize.setText(String.valueOf(248));
			setGeometry(calculateGeometry(Integer.parseInt(m_tfSize.getText())));
		}
	}
	
	private void setSizeAndCheck() {
		int cylinders = 1984;
		int heads = 16;
		int sectors = 32;
		int size = 248;
		try {
			cylinders = Integer.parseInt(m_tfCylinders.getText());
			heads = Integer.parseInt(m_tfHeads.getText());
			sectors = Integer.parseInt(m_tfSectors.getText());
			size = calculateSize(cylinders, heads, sectors);
			updateExpert();
		}
		catch (NumberFormatException nfx) {
			size = 248;
		}
		m_tfSize.setText(String.valueOf(size));
		setReserved();
	}
	
	private void setSizeActive(int index) {
		int size[] = { 248, 248, 10, 20, 40 };
		m_tfSize.setText(String.valueOf(size[index]));
		m_tfSize.setEnabled(index < 2);
		if (index >= 2) {
			setGeometry(geometries[index-2]);
		}
		else {
			setGeomAndCheck();
		}
		setGeometryEnabled(index<2);
		setExpertActive(m_chbAdvanced.isSelected(), index);
		updateExpert();
	}
	
	private void setGeometry(int[] value) {
		m_tfCylinders.setText(String.valueOf(value[0]));
		m_tfHeads.setText(String.valueOf(value[1]));
		m_tfSectors.setText(String.valueOf(value[2]));
	}
		
	private void setGeometryEnabled(boolean bActive) {
		m_tfCylinders.setEnabled(bActive);
		m_tfHeads.setEnabled(bActive);
		m_tfSectors.setEnabled(bActive);
	}
	
	private void setExpertActive(boolean bActive, int type) {
		if (bActive) {
			m_jpExpertPanel.add(m_jpAU);
			if (type > 0) m_jpExpertPanel.add(m_jpExpert);
			else m_jpExpertPanel.remove(m_jpExpert);
		}
		else {
			m_jpExpertPanel.remove(m_jpAU);
			if (type > 0) m_jpExpertPanel.remove(m_jpExpert);
		}
		repaint();
		pack();
	}
	
	private int[] calculateGeometry(int nMiB) { 
		int[] values = new int[3];
		values[2] = 32;
		int nTotalSectors = nMiB * 4096;
		int nTracks = nTotalSectors / 32;
		// max sect = 1015808, tracks=31744
		// Must get below 2048 cylinders and 16 heads
		int heads = ((nTracks + 4095) / 4096) * 2;
		values[1] = heads;
		values[0] = nTracks / heads;
		return values;	
	}
	
	private int calculateSize(int cylinders, int heads, int sectors) {
		return cylinders * heads * sectors / 4096;
	}
	
	public void focusGained(FocusEvent fe) {
	}

	public void focusLost(FocusEvent fe) {
		if (fe.getSource()==m_tfSize) {
			setGeomAndCheck();
			setReserved();
		}
		if (fe.getSource()==m_tfCylinders || fe.getSource() == m_tfHeads || fe.getSource() == m_tfSectors) {
			setSizeAndCheck();
		}
	}
	
	private void setReserved() {
		int sizeMB = Integer.parseInt(m_tfSize.getText());
		
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
		m_tfReserved.setText(String.valueOf(res));
		
		// SCSI: max=0xe6*0x40 = 14720  (bei 248 MiB)
	}
	
	private void updateExpert() {
/*		
		min res=512 with 2 heads
		max res=16383 with 16 heads
		
		Calculation for reserved AUs:
		res = 2 ^ (10 + sizeMiB/16)
		
		value is stored in file system as reserved/64 -> max = 255*64 = 16320
		
		precomp: 0x27 -> 624 (/16)
		redwc:   0x43 -> 540 (/8)
		reserved: 0x40 -> 4096 (/64)
		*/
		
		
		int cyl = Integer.parseInt(m_tfCylinders.getText());
		
		// MDM5 calculates 75% of the cylinders, rounded up to the next 16-multiple
		int pc_red = ((cyl * 3 / 4) + 15) & 0xfff0; 
		
		m_tfPrecomp.setText(String.valueOf(pc_red));
		m_tfReduced.setText(String.valueOf(pc_red));
	}
	
	FormatParameters getParameters() {
		FormatParameters params = new FormatParameters(getDiskName(), "className", true);
		params.setHD(Time.createNow(), getAUSize(), getReserved(), getDriveType());
		if (getDriveType()==HarddiskFileSystem.MFM) {
			params.setCHS(getCylinders(), getHeads(), getSectors());
			params.setMFM(getStepRate(), getReduced(), getPrecomp(), getBuffered());
		}
		return params;
	}
	
	String getDiskName() {
		return m_tfName.getText();
	}
	
	int getAUSize() {
		int nTotalSize = Integer.parseInt(m_tfSize.getText());
		int nAUSize = (nTotalSize*2)/31;
		if (nAUSize > 8) return 16;
		if (nAUSize > 4) return 8;
		if (nAUSize > 2) return 4;
		if (nAUSize > 1) return 2;
		return 1;
	}
	
	int getReserved() {
		return Integer.parseInt(m_tfReserved.getText());
	}
	
	int getDriveType() {
		int index = m_jcDrive.getSelectedIndex();
		return (index==0)? HarddiskFileSystem.SCSI : HarddiskFileSystem.MFM;
	}
	
	int getCylinders() {
		return Integer.parseInt(m_tfCylinders.getText());
	}

	int getHeads() {
		return Integer.parseInt(m_tfHeads.getText());
	}
	
	int getSectors() {
		return Integer.parseInt(m_tfSectors.getText());
	}
	
	int getPrecomp() {
		return Integer.parseInt(m_tfPrecomp.getText());
	}

	int getReduced() {
		return Integer.parseInt(m_tfReduced.getText());
	}
	
	int getStepRate() {
		return Integer.parseInt(m_tfStep.getText());
	}
	
	boolean getBuffered() {
		return m_chbBuffered.isSelected();
	}
}
	
// 10416 byte/track -> max 32 sect/track (@313 byte)
//
// ST-213: cyl=615, h=2		-> 12.2 MiB (unform.), 10 MB (form.)
// ST-225: cyl=615, h=4		-> 24.4, 20 MB
// ST-251: cyl=820, h=6		-> 48.8 MiB, 40 MB
//

// 3600 RPM -> 16.66 ms per turn, 5 Mbit/s -> 83333 bit = 10416.25 byte
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

/*
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
		FormatParameters param = new FormatParameters(getVolumeName(), getFormatClass(), 
		// toBeFormatted() 
		true);
		return param;
	}
}

*/

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