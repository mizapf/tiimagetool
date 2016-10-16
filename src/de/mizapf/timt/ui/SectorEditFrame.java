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
	
	JButton m_btnStart;
	JButton m_btnBack;
	JButton m_btnNext;
	JButton m_btnEnd;
	
	private static final String FONTED = Font.MONOSPACED;

	// Use this as a cache.
	HashMap<Integer,Sector> m_sectormap;
		
	JTextArea m_jep;
	ImageFormat m_image;
	
	JTextField m_tfSector;
	
	private static final String SAVEAS = "saveas";
	private static final String WRITE = "write";
	private static final String CLOSE = "close"; 

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
		
		m_btnStart = new JButton("|<<");
		m_btnStart.addActionListener(this);
		m_btnBack = new JButton("<");
		m_btnBack.addActionListener(this);
		m_btnNext = new JButton(">");
		m_btnNext.addActionListener(this);
		m_btnEnd = new JButton(">>|");
		m_btnEnd.addActionListener(this);

		firstLine.add(jlSect);
		firstLine.add(Box.createHorizontalStrut(10));
		firstLine.add(m_tfSector);
		firstLine.add(Box.createHorizontalStrut(20));
		firstLine.add(m_btnStart);
		firstLine.add(m_btnBack);
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
		
		JLabel[] jlByteContent = new JLabel[256];
		JLabel[] jlAsciiContent = new JLabel[256];
		
		Font mono = Font.decode(m_app.contentFont);
		Font byteFont = mono.deriveFont((float)(mono.getSize() * 1.5));
		for (int i=0; i < 256; i++) {
			jlByteContent[i] = new JLabel("00");
			jlAsciiContent[i] = new JLabel(".");
			jpBytes.add(jlByteContent[i]);
			jpAscii.add(jlAsciiContent[i]);
			jlByteContent[i].setFont(byteFont);
			jlAsciiContent[i].setFont(byteFont);
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
			for (int i = 0; i < 256; i++) {
				jlByteContent[i].setText(Utilities.toHex(sect0[i],2));
				if (sect0[i] > 31 && sect0[i] < 127) ch = (char)sect0[i];
				else ch = '.';
				jlAsciiContent[i].setText(String.valueOf(ch));
			}
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
			}
		}
	}
}
