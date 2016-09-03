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
import javax.swing.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.*;

public class SearchProgressView extends JDialog implements ActionListener {
	
	JLabel m_jlCurrentFilename;
	JLabel m_jlMatch;	
	JFrame m_frmMain;
	JButton m_btnStop;

	int m_nColumnWidth;
	boolean m_bStop;
	
	public SearchProgressView(String sTitle, JFrame frmMain) {
		super(frmMain, sTitle, false);
		m_frmMain = frmMain;
	}
	
/*
	| 	Searching										|

		Current file:     [...]
		Results found:    xxxx
			
				+---------------+	
				|	Abort/close	|
				+---------------+   
*/	
	public void createGui(Font font) {
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		setFont(font);

		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		m_nColumnWidth = fm.stringWidth("Current fileXXXXX");
		
		int nHeight = getHeight(font, "B");
		
		m_bStop = false;
		
		m_jlCurrentFilename = new JLabel();
		m_jlCurrentFilename.setMinimumSize(new Dimension(m_nColumnWidth*3, 25));
		m_jlCurrentFilename.setPreferredSize(new Dimension(m_nColumnWidth*3, 25));
		setFilename("");
		
		m_jlMatch = new JLabel();
		setResultCount(0);
		
		m_btnStop = new JButton("");
		setButtonText("Abort");
		
		add(Box.createVerticalStrut(10));		
		createLine("Current file", nHeight, m_jlCurrentFilename);
		add(Box.createVerticalStrut(10));		

		createLine("Matches found", nHeight, m_jlMatch);
	
		add(Box.createVerticalStrut(10));		
	
		Box box = new Box(BoxLayout.X_AXIS);		
		m_btnStop.addActionListener(this);
		m_btnStop.setPreferredSize(new Dimension(100, 25));
		box.add(m_btnStop);
		add(box);		
		add(Box.createVerticalStrut(10));		
		
		pack();
		setLocationRelativeTo(getParent());
	}

	private void createLine(String sText, int nHeight, JComponent jc) {
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(10));	
		JLabel jl = new JLabel(sText, SwingConstants.LEFT);
		jl.setPreferredSize(new Dimension(m_nColumnWidth, nHeight));
		box.add(jl);
		box.add(Box.createHorizontalStrut(20));
		box.add(jc);
		box.add(Box.createHorizontalStrut(10));
		box.add(Box.createHorizontalGlue());
		add(box);		
	}
	
	int getHeight(Font font, String sSample) {
		FontRenderContext frc = ((Graphics2D)(m_frmMain.getGraphics())).getFontRenderContext();
		LineMetrics lm = font.getLineMetrics(sSample, 0, 2, frc);
		return (int)Math.ceil(lm.getHeight()*1.03);
	}
	
	void setResultCount(int nValue) {
		m_jlMatch.setText(String.valueOf(nValue));
	}
	

	void setFilename(String sText) {
		int len = sText.length();
		if (len>30) sText = "..." + sText.substring(len-30);
		m_jlCurrentFilename.setText(sText);
	}
	
	void setButtonText(String sText) {
		m_btnStop.setText(sText);
	}
	
	public boolean stopRequested() {
		return m_bStop;
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==m_btnStop) {
			m_bStop = true;
		}
	}
}
