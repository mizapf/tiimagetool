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

import de.mizapf.timt.files.*;

public class ImportRemoteAction extends Activity {

	public String getMenuName() {
		return imagetool.langstr("ImpRemote");
	}
	
	public String getActionName() {
		return "IMPORTREM";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		DirectoryPanel dp = dvCurrent.getPanel();

		Volume volTarget = dirCurrent.getVolume();
		
		ConnectDialog cd = new ConnectDialog(dvCurrent.getFrame());
		byte[] abyTif = null;
		try {
			cd.createGui(imagetool.boldFont, false);
			cd.setVisible(true);		
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), iox.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
			return;			
		}		
		if (cd.confirmed()) {
			SerialConnection sc = new SerialConnection();
			try {
				sc.initializeConnection(cd.getAdapter(), cd.getSpeed(), cd.getDatabits(), cd.getParity(), cd.getStopbits(), 10000);
			}
			catch (ConnectionException cx) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), cx.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
				sc.close();
				return;
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), iox.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
				sc.close();
				return;
			}
			
			ProgressView view = null;

			try {
				view = new ProgressView("XModem receiving", m_parent);
				view.createGui(imagetool.boldFont);
				InputStream is = sc.getInputStream();
				OutputStream os = sc.getOutputStream();
				XModem xm = new XModem(is, os, view); 

				boolean bUseCrc = (cd.getProtocol() == XMODEM_DOWN_CRC);

				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				abyTif = xm.receive(bUseCrc);
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				
				view.dispose();
				is.close();
				os.close();
				sc.close();
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), iox.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				view.dispose();
				sc.close();
				return;
			}
			
			if (abyTif==null || abyTif.length==0) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), "No data received", "Connection error", JOptionPane.ERROR_MESSAGE);
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				view.dispose();
				return;
			}				
			
			// Now insert abyTif as done with "Insert File"
			try {
				volTarget.reopenForWrite();
				imagetool.putTIFileIntoImage(dirCurrent, dvCurrent, abyTif, "unnamed");
				volTarget.reopenForRead();
			}
			catch (java.io.FileNotFoundException fnfx) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Failed to open directory for writing", "File save error", JOptionPane.ERROR_MESSAGE);
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), "Write error", JOptionPane.ERROR_MESSAGE); 
			}
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Could not insert file into image: " + ix.getMessage(), "File save error", JOptionPane.ERROR_MESSAGE);
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Could not insert file into image: " + iox.getMessage(), "File save error", JOptionPane.ERROR_MESSAGE);
			}
/*			catch (FormatException fx) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), fx.getMessage(), "Import error", JOptionPane.ERROR_MESSAGE); 
			}
*/			catch (InvalidNameException ix) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Invalid name: " + ix.getMessage(), "Invalid name", JOptionPane.ERROR_MESSAGE); 
			}
			imagetool.refreshPanel(volTarget);			
		}
	}
}
