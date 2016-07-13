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

import javax.swing.*;
import java.awt.*;
import java.net.*;

class SerialDisplayLED extends JPanel implements Runnable {
	
	boolean m_bOutput;
	Thread m_thrTurnOff; 
	long m_nMilliStart;
	int m_nMinOnMs;
	int m_nMaxOnMs;
	boolean m_bDone;
	long m_nRemaining;
	String m_sLabel;
	
	JLabel m_jlLED;

	static ImageIcon[] redLed;
	static ImageIcon[] greenLed;

	protected static ImageIcon createImageIcon(String sPath, String sDescription) {
		java.net.URL imgURL = de.mizapf.timt.ui.ToolDialog.class.getResource(sPath);
		if (imgURL != null) {
			// System.out.println("Pfad gefunden");
			return new ImageIcon(imgURL, sDescription);
		}
		else {
			System.err.println("Error: Image file not found: " + sPath);
			return null;
		}
	}
	
	static {
		redLed = new ImageIcon[2];
		greenLed = new ImageIcon[2];
		redLed[0] = createImageIcon("ledredoff.png", "aus");
		redLed[1] = createImageIcon("ledredon.png", "an");
		greenLed[0] = createImageIcon("ledgreenoff.png", "aus");
		greenLed[1] = createImageIcon("ledgreenon.png", "an");
	}

	SerialDisplayLED(String sLabel, boolean bOutput, int nMinOnMs, int nMaxOnMs, Dimension dim) {
		m_bOutput = bOutput;
		m_nMinOnMs = nMinOnMs;
		m_nMaxOnMs = nMaxOnMs;
		m_thrTurnOff = new Thread(this);
		m_thrTurnOff.setName("Thread_" + sLabel);
		
		setLayout(new BorderLayout());
		JLabel jl = new JLabel(sLabel, SwingConstants.LEFT);
		add(jl, BorderLayout.WEST);
		m_jlLED = new JLabel();
		setStateInt(false);
		add(m_jlLED, BorderLayout.EAST);
		setPreferredSize(dim);
		setMaximumSize(dim);
		m_thrTurnOff.start();
		m_sLabel = sLabel;
	}

	private void setStateInt(boolean bOn) {
		if (m_bOutput) m_jlLED.setIcon(bOn? redLed[1] : redLed[0]);
		else m_jlLED.setIcon(bOn? greenLed[1] : greenLed[0]);
	}
		
	synchronized void setState(boolean bOn) {
		if (bOn) {
			setStateInt(true);
			m_nMilliStart = System.currentTimeMillis();
			if (m_nMaxOnMs != 0) {
				m_nRemaining = m_nMaxOnMs;
				notify();
			}
		}
		else {
			if (m_nMaxOnMs == 0) {
				// Calculate the remaining time in milliseconds
				long nCurrent = System.currentTimeMillis();
				m_nRemaining = m_nMinOnMs - (nCurrent - m_nMilliStart);
				if (m_nRemaining < 1) m_nRemaining = 1;
				notify();
			}
		}
	}

	synchronized void terminate() {
		m_bDone = true;
		notify();
	}
	
	public void run() {
		while (!m_bDone) {
			try {
				long nSleep = 1;
				synchronized(this) {
					nSleep = m_nRemaining;
					wait();
				}
				repaint();
				m_thrTurnOff.sleep(nSleep);
				setStateInt(false);
			}
			catch (InterruptedException ix) {
				System.err.println("Interrupted");
			}
		}
	}
}
