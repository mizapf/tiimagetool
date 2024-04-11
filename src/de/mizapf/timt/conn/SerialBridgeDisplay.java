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

package de.mizapf.timt.conn;

import java.io.*;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;
import java.awt.*;
import java.awt.font.*;
import javax.swing.*; 

import java.awt.event.*;

/** Creates a socket/serial bridge. This bridge is useful for use in the MESS
emulator. */

/*

			
	| 	Serial bridge 									|
			
		Parameters
		Bit rate				xxxxx
		Data bits				x
		Parity					x
		Stop bits				x
		
		Lines
		Data in  	(LED) green		Data out    (LED) red		
		Break in 	(LED) green		Break out	(LED) red
		
		CTS			(LED) green		RTS			(LED) red						
		DSR			(LED) green		DTR			(LED) red		
		DCD			(LED) green		
		RI			(LED) green
		
		Framing error	(LED) green
		Parity error	(LED) green

			+-----------+			
			|	Abort	|			
			+-----------+

					
		Overrun does not occur (bytes are always downloaded in time)

*/

public class SerialBridgeDisplay implements ActionListener, WindowListener {
	
	JFrame m_frmDisplay;
	int m_nColumnWidth;
	int m_nFontHeight;
	
	SerialDisplayLED m_LedDataIn;
	SerialDisplayLED m_LedDataOut;
	SerialDisplayLED m_LedBreakIn;
	SerialDisplayLED m_LedBreakOut;

	SerialDisplayLED m_LedCTS;
	SerialDisplayLED m_LedDSR;
	SerialDisplayLED m_LedDCD;
	SerialDisplayLED m_LedRI;
	
	SerialDisplayLED m_LedRTS;
	SerialDisplayLED m_LedDTR;

	SerialDisplayLED m_LedFramingError;
	SerialDisplayLED m_LedParityError;
	
	JButton m_btnAbort;

	SerialBridge m_Bridge;
	
	JLabel m_jlBitRate;
	JLabel m_jlDataBits;
	JLabel m_jlParity;
	JLabel m_jlStopBits;
	
	final static int NONE = 0;
	final static int ODD = 1;
	final static int EVEN = 2;
	final static int MARK = 3;
	final static int SPACE = 4;
	final static int UNDEF = 5;
	
	final static int DELAY = 100;
	
	public SerialBridgeDisplay() {
		m_frmDisplay = new JFrame(TIImageTool.langstr("SBDisplayTitle"));
		Font fnt = TIImageTool.dialogFont;
		createGui(fnt);
	}

	public JFrame getFrame() {
		return m_frmDisplay;
	}
	
	public void setBridge(SerialBridge sb) {
		m_Bridge = sb;
	}

	void createGui(Font font) {
		m_frmDisplay.addWindowListener(this);
		
		Container cnt = m_frmDisplay.getContentPane();
		cnt.setLayout(new BoxLayout(cnt, BoxLayout.Y_AXIS));
		
		m_frmDisplay.setVisible(true);
		m_frmDisplay.setFont(font);
		
		// m_nFontHeight = 15;
		Graphics2D g2d = (Graphics2D)m_frmDisplay.getGraphics();
		FontRenderContext frc = g2d.getFontRenderContext();
		LineMetrics lm = font.getLineMetrics("B", 0, 2, frc);
		m_nFontHeight = (int)Math.ceil(lm.getHeight()*1.2);

		FontMetrics fm = ((Graphics2D)(m_frmDisplay.getGraphics())).getFontMetrics(font);
		m_nColumnWidth = fm.stringWidth(TIImageTool.langstr("SBDisplayColumn"))+40;
				
		// System.out.println("font height: " + m_nFontHeight + ", width: " + m_nColumnWidth);		
		Dimension dimPref = new Dimension(m_nColumnWidth, m_nFontHeight);

		m_LedDataIn = new SerialDisplayLED(TIImageTool.langstr("SBDisplayDataIn"), false, 100, 0, dimPref, font);
		m_LedDataOut = new SerialDisplayLED(TIImageTool.langstr("SBDisplayDataOut"), true, 100, 0, dimPref, font);
		m_LedBreakIn = new SerialDisplayLED(TIImageTool.langstr("SBDisplayBreakIn"), false, 0, 100, dimPref, font);
		m_LedBreakOut = new SerialDisplayLED(TIImageTool.langstr("SBDisplayBreakOut"), true, 100, 0, dimPref, font);
		m_LedRTS = new SerialDisplayLED("RTS", true, 100, 0, dimPref, font);
		m_LedCTS = new SerialDisplayLED("CTS", false, 100, 0, dimPref, font);
		m_LedDTR = new SerialDisplayLED("DTR", true, 100, 0, dimPref, font);
		m_LedDSR = new SerialDisplayLED("DSR", false, 100, 0, dimPref, font);
		m_LedDCD = new SerialDisplayLED("DCD", false, 100, 0, dimPref, font);
		m_LedRI = new SerialDisplayLED("RI", false, 100, 0, dimPref, font);
		m_LedFramingError = new SerialDisplayLED(TIImageTool.langstr("SBDisplayFrameError"), false, 0, 100, dimPref, font);
		m_LedParityError = new SerialDisplayLED(TIImageTool.langstr("SBDisplayParityError"), false, 0, 100, dimPref, font);

		m_jlBitRate = new JLabel();
		m_jlParity = new JLabel();
		m_jlDataBits = new JLabel();		
		m_jlStopBits = new JLabel();		
		
		setSpeed(0);
		setDataBits(0);
		setParity(UNDEF);
		setStopBits(UNDEF);	
		
		m_frmDisplay.add(Box.createVerticalStrut(10));
		addLine(TIImageTool.langstr("SBDisplayLineSpeed"), m_jlBitRate, font); 
		addLine(TIImageTool.langstr("SBDisplayDataBits"), m_jlDataBits, font); 
		addLine(TIImageTool.langstr("SBDisplayParity"), m_jlParity, font); 
		addLine(TIImageTool.langstr("SBDisplayStopBits"), m_jlStopBits, font); 
		m_frmDisplay.add(Box.createVerticalStrut(10));
		
		add2Led(m_LedDataIn, m_LedDataOut);
		add2Led(m_LedBreakIn, m_LedBreakOut);
		m_frmDisplay.add(Box.createVerticalStrut(10));	
		add2Led(m_LedCTS, m_LedRTS);
		add2Led(m_LedDSR, m_LedDTR);
		add2Led(m_LedDCD, null);
		add2Led(m_LedRI, null);
		m_frmDisplay.add(Box.createVerticalStrut(10));		
		add2Led(m_LedFramingError, null);
		add2Led(m_LedParityError, null);
		m_frmDisplay.add(Box.createVerticalStrut(10));	
		m_frmDisplay.add(Box.createVerticalGlue());
		
		Box box7 = new Box(BoxLayout.X_AXIS);		
		m_btnAbort = new JButton(TIImageTool.langstr("Abort"));
		m_btnAbort.addActionListener(this);
//		m_btnAbort.setPreferredSize(new Dimension(100, 25));
		box7.add(m_btnAbort);
		m_frmDisplay.add(box7);		
		m_frmDisplay.add(Box.createVerticalStrut(10));			
		m_frmDisplay.pack();	
	}

	private void addLine(String sLabel, JComponent jc, Font font) {
		Box box1 = new Box(BoxLayout.X_AXIS);
		box1.add(Box.createHorizontalStrut(10));
		JLabel jl = new JLabel(sLabel, SwingConstants.LEFT);
		jl.setFont(font);
		jl.setPreferredSize(new Dimension(m_nColumnWidth, m_nFontHeight));
		box1.add(jl);
		jc.setPreferredSize(new Dimension(m_nColumnWidth, m_nFontHeight)); 
		box1.add(jc);
		box1.add(Box.createHorizontalGlue());
		m_frmDisplay.add(box1);
		m_frmDisplay.add(Box.createVerticalStrut(2));	
	}
	
	private void add2Led(SerialDisplayLED led1, SerialDisplayLED led2) {
		Box box1 = new Box(BoxLayout.X_AXIS);
		box1.add(Box.createHorizontalStrut(10));
		box1.add(led1);
		box1.add(Box.createHorizontalGlue());
		box1.add(Box.createHorizontalStrut(20));
		if (led2 != null) {
			box1.add(led2);
		}
		else {
			box1.add(Box.createHorizontalStrut(m_nColumnWidth));
		}
		box1.add(Box.createHorizontalStrut(10));
		m_frmDisplay.add(box1);
		m_frmDisplay.add(Box.createVerticalStrut(2));	
	}
	
	private void shutdown() {
		m_LedDataIn.terminate();
		m_LedDataOut.terminate();
		m_LedBreakIn.terminate();
		m_LedBreakOut.terminate();		
		m_LedCTS.terminate();
		m_LedDSR.terminate();
		m_LedDCD.terminate();
		m_LedRI.terminate();
		m_LedRTS.terminate();
		m_LedDTR.terminate();
		m_LedFramingError.terminate();
		m_LedParityError.terminate();

		m_frmDisplay.dispose();
		m_Bridge.close();
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==m_btnAbort) {
			shutdown();
		}
	}
	
	// ======================================================================
		
	public void windowClosing(WindowEvent we) {
		shutdown();
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

	// ======================================================================
	
	void setSpeed(int nSpeed) {
		m_jlBitRate.setText(String.valueOf(nSpeed));
	}

	void setParity(int nParity) {
		switch (nParity) {
		case NONE: m_jlParity.setText("N"); break;
		case ODD: m_jlParity.setText("O"); break;
		case EVEN: m_jlParity.setText("E"); break;
		case MARK: m_jlParity.setText("M"); break;
		case SPACE: m_jlParity.setText("S"); break;
		case UNDEF: m_jlParity.setText("-"); break;
		default: m_jlParity.setText("?"); break;
		}			
	}

	void setDataBits(int nBits) {
		if (nBits==0) m_jlDataBits.setText("-");
		else m_jlDataBits.setText(String.valueOf(nBits));
	}
	
	void setStopBits(int nBits) {
		switch (nBits) {
		case 1: m_jlStopBits.setText("1"); break;
		case 2: m_jlStopBits.setText("2"); break;
		case 3: m_jlStopBits.setText("1.5"); break;
		case UNDEF: m_jlStopBits.setText("-"); break;
		default:	m_jlStopBits.setText("?"); break;
		}
	}

	// ======================================================================

	void pulseDataIn() {
		m_LedDataIn.setState(true);
		m_LedDataIn.setState(false);
	}

	void pulseDataOut() {
		m_LedDataOut.setState(true);
		m_LedDataOut.setState(false);
	}

	void setBreakIn(boolean bOn) {
		m_LedBreakIn.setState(bOn);
	}

	void setBreakOut(boolean bOn) {
		m_LedBreakOut.setState(bOn);
	}
	
	void setRTS(boolean bOn) {
		m_LedRTS.setState(bOn);
	}

	void setDCD(boolean bOn) {
		m_LedDCD.setState(bOn);
	}

	void setCTS(boolean bOn) {
		m_LedCTS.setState(bOn);
	}

	void setDTR(boolean bOn) {
		m_LedDTR.setState(bOn);
	}

	void setDSR(boolean bOn) {
		m_LedDSR.setState(bOn);
	}
	
	void setRingIndicator(boolean bOn) {
		m_LedRI.setState(bOn);
	}

	void setFrameError(boolean bOn) {
		m_LedFramingError.setState(bOn);
	}
	
	void setParityError(boolean bOn) {
		m_LedParityError.setState(bOn);
	}
}
