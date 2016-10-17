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

public class SectorEditFrame extends JFrame implements ActionListener {

	JMenuBar m_mbar; 
	JMenu m_mFile;
	JMenuItem m_iSaveAs;
	JMenuItem m_iWrite;
	JMenuItem m_iClose;
	TIImageTool m_app;
	JComponent m_head;
	JPanel m_jp;
	
	JButton m_btnBegin;
	JButton m_btnPrev;
	JButton m_btnNext;
	JButton m_btnEnd;
	
	JLabel[] m_jlByteContent;
	JLabel[] m_jlAsciiContent;
	
	int m_currentSector = 0;
	
	private static final String FONTED = Font.MONOSPACED;

	// Use this as a cache.
	HashMap<Integer,Sector> m_sectormap;
		
	JTextArea m_jep;
	ImageFormat m_image;
	
	JTextField m_tfSector;
	
	private static final String SAVEAS = "saveas";
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
			catch (NumberFormatException nfx) {
				nfx.printStackTrace();
			}
			catch (IOException iox) {
				iox.printStackTrace();
			}
			catch (ImageException ix) {
				ix.printStackTrace();
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
			m_jl.requestFocus();
			m_jl.setBackground(Color.YELLOW);
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
			case 13: System.out.println("RETURN"); break;
			case 27: System.out.println("ESC"); break;
			default: System.out.println((int)ch);
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
			// Do this in the SEF
			// Unfocus other label
			m_jl.requestFocus();
			m_jl.setBackground(Color.YELLOW);
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
			case 13: System.out.println("RETURN"); break;
			case 27: System.out.println("ESC"); break;
			default: System.out.println((int)ch);
			}
		}
	}

	public SectorEditFrame(String name, ImageFormat image, TIImageTool app) {
		super(name);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		m_app = app;
		m_app.registerFrame(this);
		m_image = image;
		m_sectormap = new HashMap<Integer,Sector>();
	}

	public void createGui(String sFontName) {	

		m_mbar = new JMenuBar();
		m_mFile = new JMenu("File");
		m_mbar.add(m_mFile);
		m_iSaveAs = new JMenuItem("Save as text ...");
		m_iSaveAs.setActionCommand(SAVEAS);
		m_iSaveAs.addActionListener(this);
		m_mFile.add(m_iSaveAs);

		m_iWrite = new JMenuItem("Save changes to image");
		m_iWrite.setActionCommand(WRITE);
		m_iWrite.addActionListener(this);
		m_mFile.add(m_iWrite);

		m_iClose = new JMenuItem("Close");
		m_iClose.setActionCommand(CLOSE);
		m_iClose.addActionListener(this);
		m_mFile.add(m_iClose);
		setJMenuBar(m_mbar);	
		
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
		int nLabelWidth = fm.stringWidth("Sector");
		JLabel jlSect = new JLabel("Sector");
		m_tfSector = new JTextField("0");
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
		
		Font mono = Font.decode(m_app.contentFont);
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
			char ch;
			byte[] sect0 = getSector(0);
			showContent(sect0);
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
	
	byte[] getSector(int i) throws IOException, ImageException {
		Sector sect = m_sectormap.get(i);
		if (sect==null) {
			sect = m_image.readSector(i);
			m_sectormap.put(new Integer(i), sect);
		}
		return sect.getBytes();
	}
	
	void showContent(byte[] content) {
		char ch = 0;
		for (int i = 0; i < 256; i++) {
			m_jlByteContent[i].setText(Utilities.toHex(content[i],2));
			if (content[i] > 31 && content[i] < 127) ch = (char)content[i];
			else ch = '.';
			m_jlAsciiContent[i].setText(String.valueOf(ch));
		}
	}
	
	void goToSector(int i) throws IOException, ImageException {
		m_currentSector = i;
		showContent(getSector(i));
	}
	
	Box createLine(String left, String right, Color col, boolean title) {

		Box box1 = new Box(BoxLayout.X_AXIS);
/*		
		// Left part
		box1.add(Box.createHorizontalStrut(10));
		
		JLabel jlPath = new JLabel(left, SwingConstants.LEFT);
		jlPath.setMinimumSize(new Dimension(200, 20));
		jlPath.setPreferredSize(new Dimension(200, 20));
		jlPath.setMaximumSize(new Dimension(300, 20));
		
		jlPath.setFont(title? TIImageTool.boldFont : TIImageTool.dialogFont);
		box1.add(jlPath);
		
		// Space
		box1.add(Box.createHorizontalStrut(30));
		
		// Right part
		JLabel jlImage = new JLabel(right, SwingConstants.LEFT);
		jlImage.setFont(title? TIImageTool.boldFont : TIImageTool.dialogFont);
		box1.add(jlImage);
		
		box1.add(Box.createHorizontalGlue());

		box1.setMinimumSize(new Dimension(400, 20));
		box1.setMaximumSize(new Dimension(Short.MAX_VALUE, TIImageTool.plainHeight+2));
		box1.setOpaque(true);
		box1.setBackground(col);
		*/
		return box1;		
	}
		
	void terminate() {
		dispose();
	}
	
	String createTextOutput() {
		StringBuilder sb = new StringBuilder();
		sb.append("-- Nothing found --");
		return sb.toString();
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
					JOptionPane.showMessageDialog(this, "Error", "IOException: " + iox.getClass().getName(), JOptionPane.ERROR_MESSAGE); 
					return;
				}
			}
		}
		else {
			if (ae.getActionCommand()==CLOSE) {
				m_app.closeFrame(this);
			}
			else {
				if (ae.getActionCommand()==WRITE) {
					// Write back
				}
				else {
					try {
						int last = m_currentSector;
						if (ae.getActionCommand()==BEGIN) {
							m_currentSector = 0;
						}
						else {
							if (ae.getActionCommand()==PREV) {
								if (m_currentSector > 0) m_currentSector--;
								showContent(getSector(m_currentSector));
							}
							else {
								if (ae.getActionCommand()==NEXT) {
									m_currentSector++;
									showContent(getSector(m_currentSector));
								}
								else {
									if (ae.getActionCommand()==END) {
										System.out.println("Last sector");
									}
								}
							}
						}
						if (last != m_currentSector) {
							m_tfSector.setText(String.valueOf(m_currentSector));
							showContent(getSector(m_currentSector));
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
