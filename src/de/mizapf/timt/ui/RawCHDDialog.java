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
import java.io.*;
import java.awt.event.*;
import javax.swing.event.*;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.Utilities;

class RawCHDDialog extends ToolDialog {

	JTextField			m_tfRawFile;
	JTextField			m_tfImageFile;
//	JComboBox<String>	m_jcCHDVersion;
	JCheckBox			m_chbFillZero;	
	TIImageTool			imagetool;
	JLabel				m_jlFileSystem;
	JLabel				m_jlCylinders;
	JLabel				m_jlHeads;
	JLabel				m_jlSectorsPerTrack;
	JLabel				m_jlSectorLength;
	File				m_rawFile;
	File				m_targetCHD;
	private final static int FROM = 1;
	private final static int TO = 2;
	boolean				m_validRaw;
	boolean				m_validCHD;
	
	
	int					m_cylinders;
	int					m_heads;
	int					m_sectorsPerTrack;
	int					m_sectorLength;
	int					m_auSize;
	int					m_totalAU;
	byte[]				m_abyHead;
	boolean				m_scsi;
	
	RawCHDDialog(JFrame owner, TIImageTool timt) {
		super(owner, "Create a new CHD image file from raw contents");
		imagetool = timt;
	}
	
/*
	| 	Import raw										|

		Create a new CHD image file from raw contents.
	
		File name for raw contents (from)		[...]
		File name for CHD image (to)   			[...]
		Fill unallocated space with zeros 		[x]
		
		Target parameters:
		File system:					  ...
		Cylinders:						  ...
		Heads:							  ...
		Sectors per track:                ...
		Sector length:                    ...
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
				
*/	
	public void createGui(Font font) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth("Fill unallocated space with zerosXXXX");
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));		

		putTextLine(this, "!Creating a CHD file from a raw sector dump", 0);
		add(Box.createVerticalStrut(10));
		putTextLine(this, "This function creates a new CHD image (MAME hard disk format) from a sector dump file.", 0);
		
		putTextLine(this, "The sector dump file is supposed to hold the contents of all sectors from 0 to the last", 0);
		putTextLine(this, "sector with no metadata. You have to define these metadata below.", 0);
		add(Box.createVerticalStrut(10));
		
		String rawprompt = "File name for raw contents (from)";
		String fileprompt = "File name for CHD image (to)";
		m_tfRawFile = new JTextField("click to select");
		m_tfImageFile = new JTextField("click to select");

		addChoiceLine(nColumnWidth, rawprompt, FILELINE, FROM, m_tfRawFile, 32);
		add(Box.createVerticalStrut(10));
		addChoiceLine(nColumnWidth, fileprompt, FILELINE, TO, m_tfImageFile, 32);
		add(Box.createVerticalStrut(10));
		
		// String[] asOptions = { "4", "5" };
		// m_jcCHDVersion = putComboBox(this, "Target CHD version", asOptions, 1, nColumnWidth);
		m_chbFillZero = putCheckBox(this, "Fill unallocated space with zeros", true, nColumnWidth);
		add(Box.createVerticalStrut(15));
		
		putTextLine(this, "Target parameters:", 300);
		add(Box.createVerticalStrut(5));
		m_jlFileSystem = putLabel(this, "File system", "-",nColumnWidth);
		m_jlCylinders = putLabel(this, "Cylinders", "-", nColumnWidth);
		m_jlHeads = putLabel(this, "Heads", "-", nColumnWidth);
		m_jlSectorsPerTrack = putLabel(this, "Sectors per track", "-", nColumnWidth);
		m_jlSectorLength = putLabel(this, "Sector length", "-", nColumnWidth);
		add(Box.createVerticalStrut(10));
		
		add(Box.createVerticalStrut(10));
		
		m_validRaw = false;
		m_validCHD = false;
		
		addButtons();
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		JFileChooser jfc = null;
		if (ae.getSource()==m_btnOK) {
			m_bSet = true;
			dispose();
		}
		if (ae.getSource()==m_btnCancel) {
			m_bSet = false;
			dispose();
		}
		
		if (ae.getActionCommand().equals(String.valueOf(FROM))) {
			File lastPath = imagetool.getSourceDirectory("image");
			if (lastPath!=null) {
				jfc = new JFileChooser(lastPath);
			}
			else jfc = new JFileChooser();
			
			Dimension dim = imagetool.getPropertyDim(imagetool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			jfc.setMultiSelectionEnabled(false);
			
			int nReturn = jfc.showOpenDialog(m_frmMain);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				m_validRaw = false;
				m_rawFile = jfc.getSelectedFile();
				File filePar = m_rawFile.getParentFile();
				if (!filePar.getName().equals(".")) imagetool.setSourceDirectory(filePar, "image");  // sets the property only for non-UNC paths
				imagetool.setProperty(imagetool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
				m_tfRawFile.setText(m_rawFile.getAbsolutePath());
				
				m_validRaw = fillFields(m_rawFile);
			}
		}
		
		if (ae.getActionCommand().equals(String.valueOf(TO))) {
			File lastPath = imagetool.getSourceDirectory("image");
			if (lastPath!=null) {
				jfc = new JFileChooser(lastPath);
			}
			else jfc = new JFileChooser();
			
			Dimension dim = imagetool.getPropertyDim(imagetool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			jfc.setMultiSelectionEnabled(false);
			ImageFileFilter im = new ImageFileFilter();
			im.setOnlyHD();
			jfc.addChoosableFileFilter(im);
			jfc.setFileFilter(im);
			
			int nReturn = jfc.showSaveDialog(m_frmMain);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				m_validCHD = false;
				m_targetCHD = jfc.getSelectedFile();
				
				int nSuffixPos = m_targetCHD.getName().indexOf(".");
				if (nSuffixPos==-1 || nSuffixPos == m_targetCHD.getName().length()-1) { 
					if (!m_targetCHD.getName().endsWith(".hd") && !m_targetCHD.getName().endsWith(".chd")) {
						m_targetCHD = new File(m_targetCHD.getAbsolutePath() + ".hd");
					}
				}
				
				m_tfImageFile.setText(m_targetCHD.getAbsolutePath());
				File filePar = m_targetCHD.getParentFile();
				if (!filePar.getName().equals(".")) imagetool.setSourceDirectory(filePar, "image");  // sets the property only for non-UNC paths
				imagetool.setProperty(imagetool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
				m_validCHD = true;
			}
		}
	}
	
	boolean hasValidSettings() {
		return m_validRaw && m_validCHD;
	}
	
	private boolean fillFields(File selectedFile) {
		
		// Read the Volume Information Block
		
		m_cylinders = 0;
		
		// Read VIB and allocation map
		m_abyHead = new byte[32*256];
		try {
			DataInputStream dis = new DataInputStream(new FileInputStream(selectedFile));
			dis.readFully(m_abyHead);
			dis.close();
		}
		catch (FileNotFoundException fx) {
			JOptionPane.showMessageDialog(m_frmMain, "File not found; has it been removed in the meantime?", "Read error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_frmMain, "IO error when reading the raw file: " + iox.getClass().getName(), "Read error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		if (Volume.hasFloppyVib(m_abyHead)) {
			JOptionPane.showMessageDialog(m_frmMain, "You cannot import a floppy image into a CHD.", "Import error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		// File system
		if (Volume.hasSCSIVib(m_abyHead)) {
			m_scsi = true;
		}
		setFileSystem(m_scsi? "SCSI" : "HFDC");

		// Geometry
		m_totalAU = Utilities.getInt16(m_abyHead, 10);
		m_auSize = ((m_abyHead[16]>>4)&0x0f) + 1;
		m_sectorLength = 256;
		int nTotalSectors = m_totalAU * m_auSize;
				
		// For HFDC we take the values as found in the VIB
		if (!m_scsi) {
			m_heads = (m_abyHead[16] & 0x0f) + 1; 
			m_sectorsPerTrack = m_abyHead[12] & 0xff;
			if (m_heads < 1 || m_heads > 16) {
				JOptionPane.showMessageDialog(m_frmMain, "Invalid raw data; head count must be 1..16, not " + m_heads, "Import error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			if (m_sectorsPerTrack < 32) {
				JOptionPane.showMessageDialog(m_frmMain, "Number of sectors per track (" + m_sectorsPerTrack + ") may be invalid; possibly no raw image data.", "Import error", JOptionPane.ERROR_MESSAGE);
				return false;
			}	
			// we should again increase the number of total sectors because
			// this can be a converted SCSI image
			// MZ: No, don't do this, or an export of a HFDC-formatted drive
			// will yield 1 more cylinder
			// nTotalSectors = ((nTotalSectors + 511) / 512) * 512;
			
			m_cylinders = nTotalSectors / m_heads / m_sectorsPerTrack;
		}
		else {
			// For SCSI we have to guess some reasonable values.
			// These values are required for the "physical layer"; SCSI itself
			// provides a logical layer with linear block addressing.
			
			// Heads = 1 .. 16
			// Sectors/Track = 1 .. 255
			// Cylinders = total sectors / (Heads * Sectors/Track)
			// Cylinders = 1 .. 2048

			// Also: SCSI drives may show a number of sectors which are not
			// a product of cylinders, heads, and sectors, supposedly because of
			// an internal bad sector map.
			// We should round up to multiples of 512 sectors.
		
			nTotalSectors = ((nTotalSectors + 511) / 512) * 512;
			m_sectorsPerTrack = 32; // assume this is always the case
			
			m_cylinders = Utilities.guessGeometry(m_sectorsPerTrack, nTotalSectors);
			if (m_cylinders != -1) {
				m_heads = nTotalSectors / m_sectorsPerTrack / m_cylinders;
			}
			else {
				JOptionPane.showMessageDialog(m_frmMain, "Cannot determine geometry for SCSI image.", "Import error", JOptionPane.ERROR_MESSAGE);				
				return false;
			}
		}	

		if (m_auSize != 1 && m_auSize != 2 && m_auSize != 4 && m_auSize != 8 && m_auSize != 16) {
			JOptionPane.showMessageDialog(m_frmMain, "Unplausible AU size: " + m_auSize + ". Should be 1,2,4,8, or 16.", "Import error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		m_jlHeads.setText(String.valueOf(m_heads));
		m_jlSectorsPerTrack.setText(String.valueOf(m_sectorsPerTrack));
		m_jlCylinders.setText(String.valueOf(m_cylinders));
		m_jlSectorLength.setText(String.valueOf(m_sectorLength));
		return true;
	}
	
	File getTargetCHD() {
		return m_targetCHD;
	}
	
	File getRawFile() {
		return m_rawFile;
	}		
	
	void setFileSystem(String sFS) {
		if (sFS == null) sFS = "-"; 
		m_jlFileSystem.setText(sFS);
	}
	
	int getCylinders() {
		return m_cylinders;
	}

	int getHeads() {
		return m_heads;
	}

	int getSectorsPerTrack() {
		return m_sectorsPerTrack;
	}
	
	int getSectorLength() {
		return m_sectorLength;
	}
	
	int getAUSize() {
		return m_auSize;
	}
	
	int getTotalAU() {
		return m_totalAU;
	}

	byte[] getHeader() {
		return m_abyHead;
	}
	
	boolean fillWithZeros() {
		return m_chbFillZero.isSelected();
	}
	
	boolean isSCSI() {
		return m_scsi;
	}
}