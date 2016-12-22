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

package de.mizapf.timt.conn;
import javax.swing.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.*;

import de.mizapf.timt.TIImageTool;

public class ProgressView extends JDialog implements ActionListener {
	
	String m_sText;
	JLabel m_jlBlockSize;
	JLabel m_jlChecksum;	
	JLabel m_jlBytes;
	JLabel m_jlStatus;
	JFrame m_frmMain;
	JButton m_btnStop;
	int m_nColumnWidth;
	boolean m_bStop;
	
	public ProgressView(String sTitle, JFrame frmMain) {
		super(frmMain, sTitle, false);
		m_frmMain = frmMain;
	}
	
/*
	| 	XModem upload/download progress										|

		Block size 				128
		Integrity check 		Checksum / CRC16
		
		Bytes read / written	1234
		Status					OK / Error
			
				+---------------+	
				|	Abort/close	|
				+---------------+   
*/	
	public void createGui(Font font) {
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		setFont(font);

		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		m_nColumnWidth = fm.stringWidth(TIImageTool.langstr("ProgressViewColumn"));
		
		int nHeight = getHeight(font, "B") * 12 / 10;
		add(Box.createHorizontalStrut(300));		
		
		m_bStop = false;
		
		m_jlBlockSize = new JLabel();
		setBlockSize(0);
		
		m_jlBytes = new JLabel();
		setTransferredBytes(0);
		
		m_jlChecksum = new JLabel();
		setUseCRC16(false);
		
		m_jlStatus = new JLabel();
		setStatus("-");
		
		m_btnStop = new JButton("");
		setButtonText(TIImageTool.langstr("Abort"));
		
		add(Box.createVerticalStrut(10));		
		createLine(TIImageTool.langstr("ProgressViewBlockSize"), nHeight, m_jlBlockSize, font);
		createLine(TIImageTool.langstr("ProgressViewIntegrity"), nHeight, m_jlChecksum, font);
		add(Box.createVerticalStrut(10));		

		createLine(TIImageTool.langstr("ProgressViewTrans"), nHeight, m_jlBytes, font);
		createLine(TIImageTool.langstr("ProgressViewStatus"), nHeight, m_jlStatus, font);
		
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

	private void createLine(String sText, int nHeight, JComponent jc, Font font) {
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(10));	
		JLabel jl = new JLabel(sText, SwingConstants.LEFT);
		jl.setPreferredSize(new Dimension(m_nColumnWidth, nHeight));
		jl.setFont(font);
		System.out.println(font);
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
	
	void setBlockSize(int nValue) {
		if (nValue==0) m_jlBlockSize.setText("-");
		else m_jlBlockSize.setText(String.valueOf(nValue));
	}
	
	void setUseCRC16(boolean bUsed) {
		if (bUsed) m_jlChecksum.setText("CRC16");
		else m_jlChecksum.setText(TIImageTool.langstr("ProgressViewChecksum"));
	}
	
	void setTransferredBytes(int nAmount) {
		m_jlBytes.setText(String.valueOf(nAmount));
	}
	
	void setStatus(String sText) {
		m_jlStatus.setText(sText);
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
