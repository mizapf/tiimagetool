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
import java.util.ArrayList;

import de.mizapf.timt.TIImageTool;

public class SearchResultFrame extends JFrame implements ActionListener {

	JMenuBar m_mbar; 
	JMenu m_mFile;
	JMenuItem m_iSaveAs;
	JMenuItem m_iClose;
	TIImageTool m_app;
	JComponent m_head;
	JPanel m_jp;
	
	int m_nLeft;
	int m_nRight;
	int m_nHeight;
	
	SearchResult[] m_content;
	
	JTextArea m_jep;
	
	private static final String SAVEAS = "saveas"; 
	private static final String CLOSE = "close"; 

	class ClickListener implements MouseListener {
		// ===================================================================
		//   MouseListener
		// ================================================================
		
		String imagefile;
		OpenImageAction imageact;
		
		ClickListener(String image, Activity act) {
			imagefile = image;
			imageact = (OpenImageAction)act;
		}
		
		public void mouseEntered(MouseEvent act) { }
		public void mouseExited(MouseEvent act) { }
		public void mousePressed(MouseEvent act) { 	}
		public void mouseReleased(MouseEvent act) { }
		
		// Click occured on panel.
		public void mouseClicked(MouseEvent act) {
			imageact.openLine(new File(imagefile));
		}		
	}	
	
	public SearchResultFrame(String sFile, TIImageTool app) {
		super(sFile);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		m_app = app;
		m_app.registerFrame(this);
	}

	public void createGui(SearchResult[] content, Font fontName, Dimension di) {	

		m_mbar = new JMenuBar();
		m_mFile = new JMenu(TIImageTool.langstr("SearchResultMenu"));
		m_mbar.add(m_mFile);
		m_iSaveAs = new JMenuItem(TIImageTool.langstr("SaveAs") + "...");
		m_iSaveAs.setActionCommand(SAVEAS);
		m_iSaveAs.addActionListener(this);
		m_mFile.add(m_iSaveAs);

		m_iClose = new JMenuItem(TIImageTool.langstr("Close"));
		m_iClose.setActionCommand(CLOSE);
		m_iClose.addActionListener(this);
		m_mFile.add(m_iClose);
		setJMenuBar(m_mbar);	

		Container cntView = getContentPane();
		cntView.setLayout(new BoxLayout(cntView, BoxLayout.Y_AXIS));
		
		m_jp = new JPanel();
		m_jp.setLayout(new BoxLayout(m_jp, BoxLayout.Y_AXIS));
		
		int line = 0;
		
		Color[] color = new Color[3];
		color[0] = new Color(180,200,220);
		color[1] = new Color(220,230,240);
		color[2] = new Color(230,241,252);
		
		int nLeft = 0;
		int nRight = 0;
		
		for (int i=0; i < content.length; i++) {
			if (content[i].path.length() > nLeft) nLeft = content[i].path.length();
			if (content[i].image.length() > nRight) nRight = content[i].image.length();
		}
	
		m_nLeft = m_app.getColumnWidth(nLeft) + TIImageTool.dialogHeight;
		m_nRight = m_app.getColumnWidth(nRight) + TIImageTool.dialogHeight;
		
		cntView.add(createLine(TIImageTool.langstr("SearchResultCol1"), TIImageTool.langstr("SearchResultCol2"), color[0], true));
		
		String lastVolume = null;
	
		for (int i=0; i < content.length; i++) {		
			String pathtext = "";		
			if (lastVolume != content[i].image) {
				pathtext = lastVolume = content[i].image;
				line++;
			}
			Box box2 = createLine(content[i].path, pathtext, color[(line&1)+1], false); 
			m_jp.add(box2);
			box2.addMouseListener(new ClickListener(content[i].image, m_app.getActivity("OPENIMAGE")));
		}
		
		m_content = content;
		
		m_jp.add(Box.createVerticalGlue());
		
		JScrollPane scp = new JScrollPane(m_jp);
		
		scp.setPreferredSize(di);
		
		cntView.add(scp);
		m_jp.setBackground(DirectoryPanel.NORM);
		toFront();
	}
	
	Box createLine(String left, String right, Color col, boolean title) {

		Box box1 = new Box(BoxLayout.X_AXIS);
		
		int nLineHeight = (int)(TIImageTool.dialogHeight * 1.3);
		
		// Left part
		box1.add(Box.createHorizontalStrut(TIImageTool.dialogHeight/2));
		
		JLabel jlPath = new JLabel(left, SwingConstants.LEFT);
		jlPath.setMinimumSize(new Dimension(m_nLeft, nLineHeight));
		jlPath.setPreferredSize(new Dimension(m_nLeft, nLineHeight));
		jlPath.setMaximumSize(new Dimension(m_nLeft, nLineHeight));
		
		jlPath.setFont(title? TIImageTool.boldFont : TIImageTool.dialogFont);
		box1.add(jlPath);
		
		// Space
		box1.add(Box.createHorizontalStrut(TIImageTool.dialogHeight));
		
		// Right part
		JLabel jlImage = new JLabel(right, SwingConstants.LEFT);
		jlImage.setFont(title? TIImageTool.boldFont : TIImageTool.dialogFont);
		box1.add(jlImage);
		
		box1.add(Box.createHorizontalGlue());

		box1.setMinimumSize(new Dimension(m_nRight, nLineHeight));
		box1.setMaximumSize(new Dimension(Short.MAX_VALUE, nLineHeight));
		box1.setOpaque(true);
		box1.setBackground(col);
		return box1;		
	}
		
	void terminate() {
		dispose();
	}
	
	void append(String s) {
		m_jep.append(s);
	}
	
	String createTextOutput() {
		StringBuilder sb = new StringBuilder();
		
		int width1 = 0;
		for (SearchResult res : m_content) {
			if (width1 < res.path.length()) width1 = res.path.length();
		}
		
		for (SearchResult res : m_content) {
			sb.append(res.path);
			for (int i=0; i < width1-res.path.length(); i++) sb.append(" ");
			sb.append(" --- ").append(res.image).append("\n");
		}
		if (sb.length()==0) sb.append("-- " + TIImageTool.langstr("SearchResultNothing") + " --");
		return sb.toString();
	}
	
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand()==SAVEAS) {
			JFileChooser jfc = new JFileChooser();
			
			int nReturn = jfc.showSaveDialog(this);
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				try {
					java.io.File file = jfc.getSelectedFile();
					
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
			if (ae.getActionCommand()==CLOSE) {
				m_app.closeFrame(this);
			}
		}
	}
}
