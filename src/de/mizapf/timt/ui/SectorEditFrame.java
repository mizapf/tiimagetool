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

public class SectorEditFrame extends JFrame implements ActionListener, WindowListener {

	JMenuBar m_mbar; 
	JMenu m_mFile;
	JMenuItem m_iSaveAs;
	JMenuItem m_iRevert;
	JMenuItem m_iRevertAll;
	JMenuItem m_iWrite;
	JMenuItem m_iClose;
	TIImageTool m_app;
	JComponent m_head;
	JPanel m_jp;
	
	JButton m_btnBegin;
	JButton m_btnPrev;
	JButton m_btnNext;
	JButton m_btnEnd;
	
	int m_lastValue;
	int m_newValue;
	
	JLabel[] m_jlByteContent;
	JLabel[] m_jlAsciiContent;
	
	int m_lastOffset = -1;
	boolean m_lastAscii = false;
	
	int m_currentSector = 0;
	
	// byte[] m_content;
	Sector m_sector;
	
	boolean m_firstDigit;
	
	boolean m_thereAreChanges=false;
	
	private static final String FONTED = Font.MONOSPACED;

	// Use this as a cache.
	HashMap<Integer,Sector> m_sectormap;
		
	JTextArea m_jep;
	ImageFormat m_image;
	
	JTextField m_tfSector;
	
	int m_lastSector;
	
	String m_imageName;
	
	private static final String SAVEAS = "saveas";
	private static final String WRITE = "write";
	private static final String REVERT = "revert";
	private static final String REVERTALL = "revertall";
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
	
	class ByteListener implements MouseListener, KeyListener {
		
		SectorEditFrame m_sef;
		int m_offset;
		JLabel m_jl;
		
		ByteListener(SectorEditFrame sef, JLabel jl, int offset) {
			m_sef = sef;
			m_offset = offset;
			m_jl = jl;
		}
		
		public void mouseClicked(MouseEvent ae) {
			// Do this in the SEF
			// Unfocus other label
			m_sef.select(m_offset, false);
//			m_jl.requestFocus();
//			m_jl.setBackground(Color.YELLOW);
		}

		public void mouseEntered(MouseEvent act) { }
		public void mouseExited(MouseEvent act) { }
		public void mousePressed(MouseEvent act) { }
		public void mouseReleased(MouseEvent act) { }
		
		public void keyPressed(KeyEvent ke) {	}
		public void keyReleased(KeyEvent ke) {	}
		public void keyTyped(KeyEvent ke) {
			char ch = ke.getKeyChar();
			switch (ch) {
			case 10:
			case 13: m_sef.returnPressed(false); break;
			case 27: m_sef.escapePressed(false); break;
			default: m_sef.keyPressed(ch, false); break;
			}
		}
	}
		
	class AsciiListener implements MouseListener, KeyListener {
		
		SectorEditFrame m_sef;
		int m_offset;
		JLabel m_jl;
		
		AsciiListener(SectorEditFrame sef, JLabel jl, int offset) {
			m_sef = sef;
			m_offset = offset;
			m_jl = jl;
		}
		
		public void mouseClicked(MouseEvent ae) {
			m_sef.select(m_offset, true);
		}

		public void mouseEntered(MouseEvent act) { }
		public void mouseExited(MouseEvent act) { }
		public void mousePressed(MouseEvent act) { }
		public void mouseReleased(MouseEvent act) { }
		
		public void keyPressed(KeyEvent ke) {	}
		public void keyReleased(KeyEvent ke) {	}
		public void keyTyped(KeyEvent ke) {
			char ch = ke.getKeyChar();
			switch (ch) {
			case 10:
			case 13: m_sef.returnPressed(true); break;
			case 27: m_sef.escapePressed(true); break;
			default: m_sef.keyPressed(ch, true); break;
			}
		}
	}
	
	// ======================================================================
		
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
	
	
	public SectorEditFrame(String name, ImageFormat image, TIImageTool app) {
		super(name);
		m_imageName = name;
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		m_app = app;
		m_app.registerFrame(this);
		m_image = image;
		m_sectormap = new HashMap<Integer,Sector>();
		// m_content = new byte[Volume.SECTOR_LENGTH];
	}

	public void createGui(Font fontName) {	

		m_mbar = new JMenuBar();
		m_mFile = new JMenu(TIImageTool.langstr("File"));
		m_mbar.add(m_mFile);
		m_iSaveAs = new JMenuItem(TIImageTool.langstr("SaveAsText") + " ...");
		m_iSaveAs.setActionCommand(SAVEAS);
		m_iSaveAs.addActionListener(this);
		m_mFile.add(m_iSaveAs);

		m_iRevert = new JMenuItem(TIImageTool.langstr("SectorEditRevertCurrent"));
		m_iRevert.setActionCommand(REVERT);
		m_iRevert.addActionListener(this);
		m_mFile.add(m_iRevert);

		m_iRevertAll = new JMenuItem(TIImageTool.langstr("SectorEditRevertAll"));
		m_iRevertAll.setActionCommand(REVERTALL);
		m_iRevertAll.addActionListener(this);
		m_mFile.add(m_iRevertAll);

		m_iWrite = new JMenuItem(TIImageTool.langstr("SectorEditCommit"));
		m_iWrite.setActionCommand(WRITE);
		m_iWrite.addActionListener(this);
		m_mFile.add(m_iWrite);

		m_iClose = new JMenuItem(TIImageTool.langstr("Close"));
		m_iClose.setActionCommand(CLOSE);
		m_iClose.addActionListener(this);
		m_mFile.add(m_iClose);
		setJMenuBar(m_mbar);	
		
		addWindowListener(this);
		
		// ==================
		
/*
		+-------------------------------------+
		|  Sector:  [    0]  [<<][<][>][>>]   |
		|                                     |
		|  00    xx xx xx xx ... xx   XXXXXXXX|
		|  10    xx xx xx xx ... xx   XXXXXXXX|
		|  20    xx xx xx xx ... xx   XXXXXXXX|
		|  ..                                 |
		|  F0    xx xx xx xx ... xx   XXXXXXXX|
		|                                     |
		+-------------------------------------+
*/
		
		Container cntView = getContentPane();
		cntView.setLayout(new BoxLayout(cntView, BoxLayout.X_AXIS));

		cntView.add(Box.createHorizontalStrut(10));
		JPanel jpInner = new JPanel();
		jpInner.setLayout(new BoxLayout(jpInner, BoxLayout.Y_AXIS));
		cntView.add(jpInner);
		cntView.add(Box.createHorizontalStrut(10));
		
		Box firstLine = new Box(BoxLayout.X_AXIS);		
		
		FontMetrics fm = ((Graphics2D)(m_app.getMainFrame().getGraphics())).getFontMetrics(m_app.dialogFont);
		int nLabelWidth = fm.stringWidth(TIImageTool.langstr("Sector"));
		JLabel jlSect = new JLabel(TIImageTool.langstr("Sector"));
		jlSect.setFont(TIImageTool.dialogFont);
		m_tfSector = new JTextField("0");
		m_tfSector.setFont(TIImageTool.dialogFont);
		m_tfSector.setMinimumSize(new Dimension(100,0));
		m_tfSector.setPreferredSize(new Dimension(2*nLabelWidth,0));
		m_tfSector.setMaximumSize(new Dimension(100,2*fm.getHeight()));
		m_tfSector.addActionListener(new FieldListener(this, m_tfSector));
		
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

		firstLine.add(jlSect);
		firstLine.add(Box.createHorizontalStrut(10));
		firstLine.add(m_tfSector);
		firstLine.add(Box.createHorizontalStrut(20));
		firstLine.add(m_btnBegin);
		firstLine.add(m_btnPrev);
		firstLine.add(m_btnNext);
		firstLine.add(m_btnEnd);
		firstLine.add(Box.createHorizontalGlue());
		
		jpInner.add(Box.createVerticalStrut(10));
		jpInner.add(firstLine);
		jpInner.add(Box.createVerticalStrut(10));
		jpInner.add(Box.createVerticalGlue());

		// Create a grid of bytes
		JPanel jpBothPanels = new JPanel();
		jpBothPanels.setLayout(new BoxLayout(jpBothPanels, BoxLayout.X_AXIS));

		JPanel jpBytes = new JPanel();
		jpBytes.setLayout(new GridLayout(16,16));
		jpBytes.setBackground(Color.WHITE);
		JPanel jpAscii = new JPanel();
		jpAscii.setLayout(new GridLayout(16,16));
		jpAscii.setBackground(Color.WHITE);
		
		m_jlByteContent = new JLabel[256];
		m_jlAsciiContent = new JLabel[256];
		
		Font mono = fontName;
		Font byteFont = mono.deriveFont((float)(mono.getSize() * 1.5));
		for (int i=0; i < 256; i++) {
			m_jlByteContent[i] = new JLabel("00");
			m_jlByteContent[i].setFocusable(true);
			ByteListener bl = new ByteListener(this, m_jlByteContent[i], i);
			m_jlByteContent[i].addMouseListener(bl);
			m_jlByteContent[i].addKeyListener(bl);
			m_jlByteContent[i].setFont(byteFont);
			m_jlByteContent[i].setOpaque(true);
			m_jlByteContent[i].setBackground(Color.WHITE);

			m_jlAsciiContent[i] = new JLabel(".");
			m_jlAsciiContent[i].setFocusable(true);
			AsciiListener al = new AsciiListener(this, m_jlAsciiContent[i], i);
			m_jlAsciiContent[i].addMouseListener(al);
			m_jlAsciiContent[i].addKeyListener(al);
			m_jlAsciiContent[i].setFont(byteFont);
			m_jlAsciiContent[i].setOpaque(true);
			m_jlAsciiContent[i].setBackground(Color.WHITE);
			
			jpBytes.add(m_jlByteContent[i]);
			jpAscii.add(m_jlAsciiContent[i]);
		}
		
		FontMetrics fm1 = ((Graphics2D)(m_app.getMainFrame().getGraphics())).getFontMetrics(byteFont);

		Dimension bytefielddim = new Dimension(fm1.charWidth('0')*48, fm1.getHeight()*16);
		jpBytes.setMinimumSize(bytefielddim);
		jpBytes.setPreferredSize(bytefielddim);
		Dimension asciifielddim = new Dimension(fm1.charWidth('0')*16, fm1.getHeight()*16);
		jpAscii.setMinimumSize(asciifielddim);
		jpAscii.setPreferredSize(asciifielddim);

		try {
			getSector(0);
			m_lastSector = m_image.getTotalSectors() -1; // only known after the first read sector
			showContent();
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
		catch (ImageException ix) {
			ix.printStackTrace();
		}
		
		jpBothPanels.add(jpBytes);
		jpBothPanels.add(Box.createHorizontalStrut(10));
		jpBothPanels.add(Box.createHorizontalGlue());
		jpBothPanels.add(jpAscii);

		jpInner.add(jpBothPanels);		
		jpInner.add(Box.createVerticalStrut(10));
		toFront();
	}
	
	void getSector(int i) throws IOException, ImageException, EOFException {
		Sector sect = m_sectormap.get(i);
		if (sect==null) {
			sect = (Sector)m_image.readSector(i).clone();
			m_sectormap.put(i, sect);
		}
		m_sector = sect;
//		System.arraycopy(sect.getBytes(), 0, m_content, 0, Volume.SECTOR_LENGTH);
	}
	
	void showContent() {
		char ch = 0;
		byte[] content = m_sector.getBytes();
		for (int i = 0; i < 256; i++) {
			m_jlByteContent[i].setText(Utilities.toHex(content[i],2));
			if (content[i] > 31 && content[i] < 127) ch = (char)content[i];
			else ch = '.';
			m_jlAsciiContent[i].setText(String.valueOf(ch));
		}
	}
	
	void goToSector(int i) throws IOException, ImageException, EOFException {
		getSector(i);
		showContent();
		m_lastOffset = -1;
		clearHighlight();
		m_currentSector = i;
	}
	
	void terminate() {
		dispose();
	}
	
	String createTextOutput() {
		byte[] content = m_sector.getBytes();
		StringBuilder sb = new StringBuilder();
		sb.append(TIImageTool.langstr("ImageFile")).append(" ").append(m_imageName).append(", ");
		sb.append(TIImageTool.langstr("Sector")).append(" ").append(m_currentSector).append("\n");
		sb.append("\n");
		for (int i=0; i < 16; i++) {
			sb.append(Utilities.toHex(i*16, 2)).append(": ");
			for (int j=0; j < 16; j++) {
				sb.append(Utilities.toHex(content[i*16+j], 2)).append(" ");
			}
			sb.append("   ");
			for (int j=0; j < 16; j++) {
				char ch;
				if (content[i*16+j] > 31 && content[i*16+j] < 127) ch = (char)content[i*16+j];
				else ch = '.';
				sb.append(ch);
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	/** Remove the highlight. */
	private void clearHighlight() {
		if (m_lastAscii) {
			if (m_lastOffset != -1) {
				m_jlAsciiContent[m_lastOffset].setBackground(Color.WHITE);
			}
		}
		else {
			if (m_lastOffset != -1) {
				m_jlByteContent[m_lastOffset].setBackground(Color.WHITE);
			}			
		}		
	}
	
	void showValue(int offset) {
		byte[] content = m_sector.getBytes();
		if (offset != -1) {
			showValue(offset, content[offset]); 
		}
	}
	
	void showValue(int offset, byte val) {
		byte[] content = m_sector.getBytes();
		if (offset != -1) {
			m_jlAsciiContent[offset].setText((val > 31 && val < 127)? String.valueOf((char)val) : ".");
			m_jlByteContent[offset].setText(Utilities.toHex(content[offset],2));
		}
	}
	
	/** Clicked on a byte (left or right). */
	void select(int offset, boolean ascii) {
		byte[] content = m_sector.getBytes();
		if (offset != m_lastOffset || ascii != m_lastAscii) {
			// If we clicked on another byte, clear the last selection
			clearHighlight();
			
			// Show the last byte (remove the underscore)
			showValue(m_lastOffset);
			
			// Save the current value of this new coordinate
			m_lastValue = content[offset];
			m_lastOffset = offset;
			m_lastAscii = ascii;
			
			// Create new highlight and underscore
			if (ascii) {
				// Clicked on the right
				m_jlAsciiContent[offset].requestFocus();
				m_jlAsciiContent[offset].setBackground(Color.YELLOW);
				m_jlAsciiContent[offset].setText("_");
			}
			else {
				// Clicked on the left
				m_jlByteContent[offset].requestFocus();
				m_jlByteContent[offset].setBackground(Color.YELLOW);
				String text = Utilities.toHex(content[offset],2);
				m_jlByteContent[offset].setText("_" + text.substring(1));
				m_firstDigit = true;
				m_newValue = content[offset];
			}
		}
	}
	
	void returnPressed(boolean ascii) {
		byte[] content = m_sector.getBytes();
		if (!ascii) {
			content[m_lastOffset] = (byte)m_newValue;
		}
		clearHighlight();
		if (m_lastOffset < 255) {
			select(m_lastOffset+1, ascii);
		}
	}
	
	void escapePressed(boolean ascii) {
		clearHighlight();
		showValue(m_lastOffset, (byte)m_lastValue);
	}
	
	void keyPressed(char key, boolean ascii) {
		byte[] content = m_sector.getBytes();		
		if (ascii) {
			m_jlAsciiContent[m_lastOffset].setText(String.valueOf(key));
			m_jlByteContent[m_lastOffset].setText(Utilities.toHex(key,2));
			content[m_lastOffset] = (byte)key;
			m_sector.dirty();
			m_thereAreChanges = true;
			clearHighlight();
			if (m_lastOffset < 255) {
				select(m_lastOffset+1, ascii);
			}
		}
		else {
			int val = -1;
			if (key >= '0' && key <= '9') val = key - 48;
			else {
				if (key >= 'A' && key <= 'F') val = key - 55;
				else {
					if (key >= 'a' && key <= 'f') val = key - 87;
				}
			}			
			if (val != -1) {
				if (m_firstDigit) {
					m_firstDigit = false;
					m_newValue = (val << 4) | (m_newValue & 0xf);
					String text = Utilities.toHex(m_newValue,2);
					m_jlByteContent[m_lastOffset].setText(text.substring(0,1) + "_");
					m_sector.dirty();
					m_thereAreChanges = true;
				}
				else {
					m_newValue = (m_newValue & 0xf0) | val;
					String text = Utilities.toHex(m_newValue,2);
					m_jlByteContent[m_lastOffset].setText(text);
					content[m_lastOffset] = (byte)m_newValue;
					m_sector.dirty();
					m_thereAreChanges = true;
					clearHighlight();
					if (m_lastOffset < 255) {
						select(m_lastOffset+1, ascii);
					}
				}
			}
		}
	}

	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand()==SAVEAS) {
			JFileChooser jfc = new JFileChooser();
			
			int nReturn = jfc.showSaveDialog(this);
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				try {
					File file = jfc.getSelectedFile();
					
					FileOutputStream fos = new FileOutputStream(file);
					String scont = createTextOutput();
					fos.write(scont.getBytes());
					fos.close();
				}
				catch (IOException iox) {
					JOptionPane.showMessageDialog(this, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE); 
					return;
				}
			}
		}
		else {
			if (ae.getActionCommand()==REVERT || ae.getActionCommand()==REVERTALL) {
				escapePressed(m_lastAscii);
				if (ae.getActionCommand()==REVERT) m_sectormap.remove(m_currentSector);
				else m_sectormap.clear();
				try {
					goToSector(m_currentSector);
				}
				catch (IOException iox) {
					iox.printStackTrace();
				}
				catch (ImageException ix) {
					ix.printStackTrace();
				}
			}
			else {
				if (ae.getActionCommand()==CLOSE) {
					closeFrame();
				}
				else {
					if (ae.getActionCommand()==WRITE) {
						writeBackAll();
						m_thereAreChanges = false;
					}
					else {
						try {
							int last = m_currentSector;
							clearHighlight();
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
										if (m_currentSector < m_lastSector) m_currentSector++;
									}
									else {
										if (ae.getActionCommand()==END) {
											m_currentSector = m_lastSector;
										}
									}
								}
							}
							if (last != m_currentSector) {
								m_tfSector.setText(String.valueOf(m_currentSector));
								getSector(m_currentSector);
								showContent();
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
		}
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
	
	private void writeBackAll() {
		try {
			m_image.reopenForWrite();
			for (Integer i : m_sectormap.keySet()) {
				Sector sect = m_sectormap.get(i);
				if (sect.changed()) {
					// System.out.println("Write back sector " + i);
					m_image.writeSector(i, sect.getBytes());  // directly written through
				}
			}
			m_image.flush();
			m_image.reopenForRead();
			SectorCache.nextGen();
		}
		catch (ImageException ix) {
			// Sector not found
			ix.printStackTrace();
			JOptionPane.showMessageDialog(this, ix.getMessage(), TIImageTool.langstr("WriteError"), JOptionPane.ERROR_MESSAGE);
		}
		catch (IOException iox) {
			// More low-level
			iox.printStackTrace();
			JOptionPane.showMessageDialog(this, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("WriteError"), JOptionPane.ERROR_MESSAGE);
		}
	}
}
