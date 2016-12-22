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
package de.mizapf.timt.ui;

import javax.swing.*;
import java.io.IOException;
import java.awt.*;
import java.net.*;
import de.mizapf.timt.conn.*;
import de.mizapf.timt.TIImageTool;

public class SerialBridgeAction extends Activity implements Runnable {

	Thread m_thrRunner;
	SerialBridge m_sb;
	
	public String getMenuName() {
		return TIImageTool.langstr("SerialBridge");
	}
	
	public String getActionName() {
		return "SERBRG";
	}

	public void setup(String sSerial, String sPort) {
		SerialBridgeDisplay display = null;
		try {
			display = new SerialBridgeDisplay();
			m_sb = new SerialBridge(sSerial, Integer.parseInt(sPort), display);
			display.setBridge(m_sb);
			m_sb.create();
		}
		catch (ConnectionException cx) {
			JOptionPane.showMessageDialog(display.getFrame(), cx.getMessage(), TIImageTool.langstr("ConnectionError"), JOptionPane.ERROR_MESSAGE); 			
		}
		catch (SocketException sx) {
			JOptionPane.showMessageDialog(display.getFrame(), sx.getMessage(), TIImageTool.langstr("ConnectionError"), JOptionPane.ERROR_MESSAGE); 
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(display.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ConnectionError"), JOptionPane.ERROR_MESSAGE);
		}
		catch (NumberFormatException nfx) {
			System.err.println(TIImageTool.langstr("SBActionInvalidPort") + ": " + sPort);
		}
	}
	
	public void run() {
		imagetool.setBridgeEnabled(false);
		try {
			m_sb.create();
		}
		catch (ConnectionException cx) {
			JOptionPane.showMessageDialog(m_parent, cx.getMessage(), TIImageTool.langstr("ConnectionError"), JOptionPane.ERROR_MESSAGE); 			
		}
		catch (SocketException sx) {
			JOptionPane.showMessageDialog(m_parent, sx.getMessage(), TIImageTool.langstr("ConnectionError"), JOptionPane.ERROR_MESSAGE); 
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ConnectionError"), JOptionPane.ERROR_MESSAGE); 
		}		
		imagetool.setBridgeEnabled(true);
		// Bridge has been closed
	}
	
	public void go() {
		SBConfigDialog sbd = new SBConfigDialog(m_parent);
		try {
			sbd.createGui(TIImageTool.plainFont);
			sbd.setVisible(true);	
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("SBActionError") + ": " + iox.getClass().getName(), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE); 
			return;
		}
		
		if (sbd.confirmed()) {
			SerialBridgeDisplay display = new SerialBridgeDisplay();
			m_sb = new SerialBridge(sbd.getAdapter(), sbd.getPort(), display);
			display.setBridge(m_sb);
			
			m_thrRunner = new Thread(this);
			m_thrRunner.start();
		}
	}
}
