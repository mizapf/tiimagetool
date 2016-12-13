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

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import javax.swing.JFileChooser;
import java.awt.Dimension;
import java.awt.Cursor;
import javax.swing.JOptionPane;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.ui.ImageFileFilter;

public class CHDRawAction extends Activity {

	public String getMenuName() {
		return imagetool.langstr("ExtractRaw"); 
	}
	
	public String getActionName() {
		return "EXPORTCHD";
	}
	
	public void go() {
		CHDRawDialog expdialog = new CHDRawDialog(m_parent, imagetool);
		expdialog.createGui(imagetool.boldFont);
		expdialog.setVisible(true);
		
		boolean bOK = false;
		
		if (expdialog.confirmed()) {
			if (!expdialog.validSelection()) {
				JOptionPane.showMessageDialog(m_parent, imagetool.langstr("MissingRaw"), imagetool.langstr("ConversionError"), JOptionPane.ERROR_MESSAGE);		
			}
			else {
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				MessCHDFormat source = expdialog.getCHD();
				File fileTarget = expdialog.getTargetFile();	
				
				try {
					DataOutputStream dos = new DataOutputStream(new FileOutputStream(fileTarget));
					int nBytes = source.getCylinders() * source.getHeads() * source.getSectorsPerTrack() * source.getSectorLength();
					int nLength = 0;
					// System.out.println("Hunks = " + source.getHunkCount());
					for (int i=0; i < source.getHunkCount(); i++) {
						// System.out.println("copy hunk " + i);
						byte[] abyHunk = source.getHunkContents(i);
						nLength = (nBytes > abyHunk.length)? abyHunk.length : nBytes;
						dos.write(abyHunk, 0, nLength);
						nBytes = nBytes - abyHunk.length;
					}
					// System.out.println("Last hunk written " + nLength + " bytes"); 
					dos.close();
					bOK = true;
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(m_parent, ix.getMessage(), imagetool.langstr("ExtractError"), JOptionPane.ERROR_MESSAGE);		
				}
				catch (FileNotFoundException fnfx) {
					JOptionPane.showMessageDialog(m_parent, imagetool.langstr("TargetNotFound") + fnfx.getMessage(), imagetool.langstr("ExtractError"), JOptionPane.ERROR_MESSAGE);		
				}
				catch (IOException iox) {
					iox.printStackTrace();
					JOptionPane.showMessageDialog(m_parent, imagetool.langstr("ExtractIOError") + iox.getClass().getName(), imagetool.langstr("ExtractError"), JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		if (bOK) JOptionPane.showMessageDialog(m_parent, String.format(imagetool.langstr("Completed"), imagetool.langstr("Extraction")), imagetool.langstr("Extract"), JOptionPane.INFORMATION_MESSAGE);
	}
}
