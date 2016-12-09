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

import java.awt.Cursor;
import javax.swing.*;
import java.io.IOException;
import de.mizapf.timt.conn.*;

import gnu.io.*;

import static de.mizapf.timt.conn.XModem.*;

import java.io.InputStream;
import java.io.OutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.TIFiles;

public class ExportRemoteAction extends Activity {

	public String getMenuName() {
		return imagetool.langstr("SendRemote");
	}
	
	public String getActionName() {
		return "SENDREM";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		DirectoryPanel dp = dvCurrent.getPanel();
		Directory dirCurrent = dvCurrent.getDirectory();

		byte[] abyCont = null;
		try {	
			Element el = dvCurrent.getClickedElement();
			if (el instanceof TFile) {
				TIFiles tif = TIFiles.createFromFile((TFile)el);
				abyCont = tif.toByteArray();
			}
			else {
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}				
		}
		catch (ImageException ix) { 
			JOptionPane.showMessageDialog(m_parent, ix.getMessage(), "Error getting file file", JOptionPane.ERROR_MESSAGE);
			return;
		}
		catch (FileNotFoundException fnfx) {
			JOptionPane.showMessageDialog(m_parent, fnfx.getMessage(), "File or folder not found", JOptionPane.ERROR_MESSAGE);
			return;
		}
		catch (IOException iox) { 
			JOptionPane.showMessageDialog(m_parent, iox.getClass().getName(), "Error loading file", JOptionPane.ERROR_MESSAGE);
			return;
		}

		ConnectDialog cd = new ConnectDialog(m_parent);
		try {
			cd.createGui(imagetool.boldFont, true);
			cd.setVisible(true);		
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_parent, iox.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
			return;			
		}		
		if (cd.confirmed()) {
			SerialConnection sc = new SerialConnection();
			try {
				sc.initializeConnection(cd.getAdapter(), cd.getSpeed(), cd.getDatabits(), cd.getParity(), cd.getStopbits(), 10000);
			}
			catch (ConnectionException cx) {
				JOptionPane.showMessageDialog(m_parent, cx.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
				sc.close();
				return;
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent, iox.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
				sc.close();
				return;
			}
			
			ProgressView view = null;
			try {
				view = new ProgressView("XModem sending", m_parent);
				view.createGui(imagetool.boldFont);
				InputStream is = sc.getInputStream();
				OutputStream os = sc.getOutputStream();
				XModem xm = new XModem(is, os, view); 

				int nOurBlockSize = (cd.getProtocol() == XModem.XMODEM_UP_1K)? 1024:128;

				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				xm.send(abyCont, nOurBlockSize);
				
				is.close();
				os.close();
			}
			catch (ProtocolException px) {
				JOptionPane.showMessageDialog(m_parent, px.getMessage(), "Protocol error", JOptionPane.ERROR_MESSAGE);				
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent, iox.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
			}
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			view.dispose();
			sc.close();
		}		
	}
}
