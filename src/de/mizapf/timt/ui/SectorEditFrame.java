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
    
    Copyright 2016 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.ui;
import de.mizapf.timt.util.Utilities;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.font.*;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class SectorEditFrame extends JFrame implements ActionListener, WindowListener, FocusListener {

	TIImageTool m_app;
	String m_imageName;
	ImageFormat m_image;
	boolean m_thereAreChanges=false;

	JMenuBar m_mbar; 
	JMenu m_mFile;
	JMenuItem m_iWrite;
	JMenuItem m_iClose;	
	JTextField[] m_atfHex;
	JTextArea m_taAsciiContent;

	JTextField m_tfSectorNumber;
	int m_currentSector = 0;
	int m_lastOffset = -1;
	int m_lastSector;
	
	Sector m_sector;

	JButton m_btnBegin;
	JButton m_btnPrev;
	JButton m_btnNext;
	JButton m_btnEnd;

	private static final String WRITE = "write";
	private static final String CLOSE = "close"; 
	private static final String BEGIN = "begin"; 
	private static final String END = "end"; 
	private static final String PREV = "prev";
	private static final String NEXT = "next";
	
	class FieldListener implements ActionListener {
		SectorEditFrame m_sef;
		JTextField m_tf;
		
		FieldListener(SectorEditFrame sef, JTextField tf) {
			m_sef = sef;
			m_tf = tf;
		}
		
		public void actionPerformed(ActionEvent ae) {
			try {
				String fieldInput = m_tf.getText().trim();
				int secno = 0;
				boolean hex = fieldInput.startsWith("0x"); 
				if (hex) secno = Integer.parseInt(fieldInput.substring(2), 16);
				else secno = Integer.parseInt(fieldInput);
				m_sef.goToSector(secno);
			}
			catch (EOFException eox) {
				JOptionPane.showMessageDialog(m_sef, TIImageTool.langstr("SectorEditInvalid"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
				m_tf.setText(String.valueOf(m_currentSector));
			}
			catch (NumberFormatException nfx) {
				nfx.printStackTrace();
				m_tf.setText(String.valueOf(m_currentSector));
			}
			catch (IOException iox) {
				iox.printStackTrace();
				m_tf.setText(String.valueOf(m_currentSector));
			}
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(m_sef, ix.getMessage(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
				m_tf.setText(String.valueOf(m_currentSector));
			}
		}
	}
	
	public SectorEditFrame(String name, ImageFormat image, TIImageTool app) {
		super(name);
		m_imageName = name;
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		m_app = app;
		m_app.registerFrame(this);
		m_image = image;
	}
	
	public void createGui(Font fontName) {
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));

		m_mbar = new JMenuBar();
		m_mFile = new JMenu(TIImageTool.langstr("File"));
		m_mbar.add(m_mFile);	

		m_iWrite = new JMenuItem(TIImageTool.langstr("CloseSave"));
		m_iWrite.setActionCommand(WRITE);
		m_iWrite.addActionListener(this);
		m_mFile.add(m_iWrite);
		
		m_iClose = new JMenuItem(TIImageTool.langstr("ExitNoSave"));
		m_iClose.setActionCommand(CLOSE);
		m_iClose.addActionListener(this);
		m_mFile.add(m_iClose);
		
		setJMenuBar(m_mbar);

		// Controls panel
		JPanel jpControls = new JPanel();
		jpControls.setLayout(new BoxLayout(jpControls, BoxLayout.X_AXIS));
		
		JLabel jlSect = new JLabel(TIImageTool.langstr("Sector"));
		jlSect.setFont(TIImageTool.dialogFont);

		m_tfSectorNumber = new JTextField("0");
		m_tfSectorNumber.setFont(TIImageTool.dialogFont);
		m_tfSectorNumber.addActionListener(new FieldListener(this, m_tfSectorNumber));
		
		m_btnBegin = new JButton("|<<");
		m_btnBegin.addActionListener(this);
		m_btnBegin.setActionCommand(BEGIN);
		m_btnPrev = new JButton("<");
		m_btnPrev.addActionListener(this);
		m_btnPrev.setActionCommand(PREV);
		m_btnNext = new JButton(">");
		m_btnNext.addActionListener(this);
		m_btnNext.setActionCommand(NEXT);
		m_btnEnd = new JButton(">>|");
		m_btnEnd.addActionListener(this);
		m_btnEnd.setActionCommand(END);
			
		jpControls.add(jlSect);
		jpControls.add(Box.createHorizontalStrut(10));
		jpControls.add(m_tfSectorNumber);
		jpControls.add(Box.createHorizontalStrut(20));
		jpControls.add(m_btnBegin);
		jpControls.add(m_btnPrev);
		jpControls.add(m_btnNext);
		jpControls.add(m_btnEnd);
		jpControls.add(Box.createHorizontalGlue());
						
		// Displays
		JPanel jpDisplays = new JPanel();
		jpDisplays.setLayout(new BoxLayout(jpDisplays, BoxLayout.X_AXIS));
		jpDisplays.add(Box.createHorizontalStrut(10));
		
		JPanel jpFirstCol = new JPanel();
		jpFirstCol.setLayout(new BoxLayout(jpFirstCol, BoxLayout.Y_AXIS));
		for (int i=0; i < 16; i++) {
			JLabel jl = new JLabel(Utilities.toHex(i*16, 2));
			jpFirstCol.add(jl);
		}

		jpDisplays.add(jpFirstCol);		
		jpDisplays.add(Box.createHorizontalStrut(20));
		
		JPanel jpHexDisplay = new JPanel();
		jpHexDisplay.setLayout(new BoxLayout(jpHexDisplay, BoxLayout.Y_AXIS));
				
		m_atfHex = new JTextField[16];
		for (int i=0; i < 16; i++) {
			m_atfHex[i] = new JTextField("");
			m_atfHex[i].setEditable(true);
			m_atfHex[i].setFont(m_app.contentFont);
			m_atfHex[i].setBorder(javax.swing.BorderFactory.createEmptyBorder());
			m_atfHex[i].addFocusListener(this);
			jpHexDisplay.add(m_atfHex[i]);	
		}
		jpDisplays.add(jpHexDisplay);
		
		jpDisplays.add(Box.createHorizontalStrut(20));
		
		m_taAsciiContent = new JTextArea("");
		m_taAsciiContent.setEditable(false);
		m_taAsciiContent.setFont(fontName);
		jpDisplays.add(m_taAsciiContent);
		jpDisplays.add(Box.createHorizontalStrut(10));

		add(jpControls);
		add(Box.createVerticalStrut(10));	

		add(jpDisplays);
		add(Box.createVerticalStrut(10));	
		
		byte[] empty = new byte[256];
		fillHexDisplay(empty);
		fillAsciiDisplay(empty);		
		
		addWindowListener(this);
		
		try {
			getSector(0);
			m_lastSector = m_image.getTotalSectors() -1; // only known after the first read sector
			if (m_lastSector < 0) m_btnEnd.setEnabled(false);
			fillHexDisplay(m_sector.getData());
			fillAsciiDisplay(m_sector.getData());
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
		catch (ImageException ix) {
			ix.printStackTrace();
		}
		m_image.nextGeneration(true);
		
		setResizable(false);
	}

	private void fillHexDisplay(byte[] content) {
		for (int i=0; i < 16; i++) {
			StringBuilder sbh = new StringBuilder();
			for (int j=0; j < 16; j++) {
				byte by = content[i*16+j];
				sbh.append(Utilities.toHex(by,2));
				if (j < 15) sbh.append(" ");
			}
			m_atfHex[i].setText(sbh.toString());
		}
	}
	
	private void fillAsciiDisplay(byte[] content) {
		StringBuilder sba = new StringBuilder();
		
		for (int i=0; i < 16; i++) {
			for (int j=0; j < 16; j++) {
				byte by = content[i*16+j];
				if (by >=32 && by <= 127) sba.append((char)by);
				else sba.append('.');
			}
			if (i < 15) {
				sba.append("\n");
			}
		}

		m_taAsciiContent.setText(sba.toString());
	}
	
	private void closeFrame() {		
		if (m_thereAreChanges) {
			int doCheck = JOptionPane.showConfirmDialog(this, TIImageTool.langstr("UnsavedChanges") + ". " + TIImageTool.langstr("ReallyClose"), TIImageTool.langstr("Attention"), JOptionPane.ERROR_MESSAGE);
			if (doCheck == JOptionPane.YES_OPTION) {
				m_app.closeFrame(this);
			}
		}
		else {
			m_app.closeFrame(this);
		}	
	}
	
	public void windowClosing(WindowEvent we) {
		closeFrame();
	}
	
	public void windowClosed(WindowEvent we) {
	}
	
	public void windowActivated(WindowEvent we) {
	}

	public void windowDeactivated(WindowEvent we) {
	}

	public void windowIconified(WindowEvent we) {
	}

	public void windowDeiconified(WindowEvent we) {
	}
	
	public void windowOpened(WindowEvent we) {
	}

	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand()==CLOSE) {
			closeFrame();
		}
		else {
			if (ae.getActionCommand()==WRITE) {
				writeBackAll();
				closeFrame();
			}
			else {
				try {
					int last = m_currentSector;
					if (ae.getActionCommand()==BEGIN) {
						m_currentSector = 0;
					}
					else {
						if (ae.getActionCommand()==PREV) {
							if (m_currentSector > 0) {
								m_currentSector--;
							}
						}
						else {
							if (ae.getActionCommand()==NEXT) {
								if ((m_lastSector < 0) || (m_currentSector < m_lastSector)) m_currentSector++;
							}
							else {
								if (ae.getActionCommand()==END) {
									if (m_lastSector > 0) 
										m_currentSector = m_lastSector;
								}
							}
						}
					}
					if (last != m_currentSector) {
						m_tfSectorNumber.setText(String.valueOf(m_currentSector));
						getSector(m_currentSector);
						fillHexDisplay(m_sector.getData());
						fillAsciiDisplay(m_sector.getData());
					}
				}
				catch (IOException iox) {
					iox.printStackTrace();
				}
				catch (ImageException ix) {
					ix.printStackTrace();
				}
			}
		}
	}

	void getSector(int i) throws IOException, ImageException {
		m_sector = m_image.readSector(i);
	}

	void goToSector(int i) throws IOException, ImageException, EOFException {
		getSector(i);
		fillHexDisplay(m_sector.getData());
		fillAsciiDisplay(m_sector.getData());
		m_lastOffset = -1;
		m_currentSector = i;
	}
	
	public void focusGained(FocusEvent fe) {
		// System.out.println("Focus gained");
	}
	
	public void focusLost(FocusEvent fe) {
		Component cmp = fe.getComponent();
		boolean updated = false;
		byte[] current = m_sector.getData();
		
		for (int i=0; i < 16; i++) {
			if (cmp == m_atfHex[i]) {
				byte[] line = parseHexContent(i);
				for (int j=0; j < 16; j++) {
					if (current[i*16 + j] != line[j]) updated = true;
					current[i*16 + j] = line[j];
				}
			}
		}
		fillAsciiDisplay(current);
		if (updated) {
			m_sector.modify(current);
			m_image.sameGeneration();
			m_image.writeSector(m_sector);
			m_image.nextGeneration(true);
		}
		// Add button "Commit"
	}
	
	
	private byte[] parseHexContent(int line) {
		String sHex = m_atfHex[line].getText();
		byte[] aby = new byte[16];
		int pos = 0;
		int val = 0;
		String hex = "0123456789ABCDEF";
		
		for (int i=0; i < sHex.length(); i++) {
			char c = sHex.charAt(i);
			if (c > 32) {
				if (c >= 96) c = (char)(c - 32);
				int digit = hex.indexOf(c);
				if (digit > -1) {
					val = (val << 4) | digit;
					pos++;
					if (pos > 32) {
						System.out.println("Clipping excess bytes");
						break;
					}
					if ((pos % 2) == 0) {
						aby[pos/2 - 1] = (byte)val;
						val = 0;
					}
				}
			}
		}

		// Clean line
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < 16; i++) {
			sb.append(Utilities.toHex(aby[i],2));
			if (i < 15) sb.append(" ");
		}
		 m_atfHex[line].setText(sb.toString());
		return aby;
	}
	
	private void writeBackAll() {
		try {
			((FileImageFormat)m_image).saveImage();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
