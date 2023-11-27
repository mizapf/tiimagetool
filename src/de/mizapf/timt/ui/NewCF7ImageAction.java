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
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class NewCF7ImageAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("CompleteCF7") + "...";
	}
	
	public String getActionName() {
		return "NEWCF7IMG";
	}
	
	public int getKeyCode() {
		return KeyEvent.VK_7;
	}
	
	public void go() {
		NewCF7Dialog newimagedia = new NewCF7Dialog(m_parent);

		newimagedia.createGui(imagetool.boldFont);
		newimagedia.setVisible(true);

		int nSizeKiB = 0;
		String sSize = newimagedia.getCapacity();
		try {
			nSizeKiB = Integer.parseInt(sSize) * 1024;
		}
		catch (NumberFormatException nfx) {
			JOptionPane.showMessageDialog(m_parent, String.format(TIImageTool.langstr("ParseError"), sSize), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
			return;
 		}
 		
 		int nVolumes = nSizeKiB / 800;
		
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		boolean bOK = false;
		
		if (newimagedia.confirmed()) {		
			
			JFileChooser jfc = null;
			if (imagetool.getSourceDirectory("image")!=null) {
				jfc = new JFileChooser(imagetool.getSourceDirectory("image"));
			}
			else jfc = new JFileChooser();
			
			// only files, no multiple selection
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			jfc.setMultiSelectionEnabled(false);

			ImageFileFilter im = new ImageFileFilter(settings.getPropertyString(imagetool.IMGSUF));
			
			jfc.addChoosableFileFilter(im);
			jfc.setFileFilter(im);	
			
			Dimension dim = settings.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			
			// open it now
			int nReturn = jfc.showSaveDialog(m_parent);
			
			File selectedFile = null;
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				File select = jfc.getSelectedFile();
				if (select != null) {
					selectedFile = new File(ImageFormat.getSuffixedName(select.getAbsolutePath(), ImageFormat.CF7));
				}
			}
			else {
				selectedFile = null;
			}

			byte[] volume = new byte[800*1024];
						
			try {
				if (selectedFile != null) {	
					
					// Overwrite?
					if (selectedFile.exists()) {		
						int nRet = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("ExistsOverwrite"), TIImageTool.langstr("NewImageTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if (nRet == JOptionPane.NO_OPTION) {
							m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
							return;
						}
						selectedFile.delete();
					}
					
					FileOutputStream fos = new FileOutputStream(selectedFile);
					Interval[] intv = newimagedia.getIntervals();
					if (intv.length > 0) {					
						
						String names = newimagedia.getVolumeNames();
						if (names.length() < 10) {
							for (int i=1; i <= nVolumes; i++) {
								for (int j=0; j < intv.length; j++) {
									if (intv[j].contains(i)) setupVolume(i, volume, names);
									else clearVolume(i, volume);
								}
								fos.write(volume);
							}
							// Write the rest
							int nRest = (nSizeKiB - nVolumes*800)*1024;
							if (nRest > 0) {
								byte[] rest = new byte[nRest];
								fos.write(rest);
							}
							fos.close();
							bOK = true;
						}
						else {
							JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("Name10"), TIImageTool.langstr("FormatCF"), JOptionPane.ERROR_MESSAGE);
						}
					}
					else {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("NoVolSpec"), TIImageTool.langstr("FormatCF"), JOptionPane.ERROR_MESSAGE);
					}
				}
			}
			catch (IOException iox) {
				iox.printStackTrace();
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("CF7WriteError"), TIImageTool.langstr("FormatCF"), JOptionPane.ERROR_MESSAGE);
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		if (bOK) JOptionPane.showMessageDialog(m_parent, String.format(TIImageTool.langstr("Completed"), TIImageTool.langstr("FormatCF")), TIImageTool.langstr("FormatCF"), JOptionPane.INFORMATION_MESSAGE);
	}
	
	private void setupVolume(int num, byte[] volume, String name) {
		Interval intv = getVariable(name);
		String sName = name;
		
		if (intv.start != -1) {
			// xxxxxx ## xx
			//   pref    tail
			StringBuilder sbName = new StringBuilder();	
			String pref = name.substring(0, intv.start);
			String number = String.valueOf(num);
			sbName.append(pref);
			
			if (intv.start == intv.end) {
				sbName.append(number);	
			}
			else {
				String tail = name.substring(intv.end+1);
				int len = number.length();
				int digits = intv.end-intv.start+1;
				if (len > digits) sbName.append(number.substring(len-digits));
				else {
					String nulls = "0000000000";
					sbName.append(nulls.substring(0, digits-len));
					sbName.append(number);
				}
				sbName.append(tail);
			}
			sName = sbName.toString();
		}
/*
	00000000: 5600 4f00 4c00 3100 2000 2000 2000 2000  V.O.L.1. . . . .
	00000010: 2000 2000 0600 4000 2000 4400 5300 4b00   . ...@. .D.S.K.
	00000020: 2000 2800 0100 0100 0000 0000 0000 0000   .(.............

	00000070: 0300 0000 0000 0000 0000 0000 0000 0000  ................
*/

		for (int i=0; i < 10; i++) {
			volume[i*2] = (byte)((i < sName.length())? sName.charAt(i) : ' ');
		}
		volume[0x14] = (byte)0x06;
		volume[0x16] = (byte)0x40;
		volume[0x18] = (byte)0x20;
		volume[0x1a] = (byte)0x44;
		volume[0x1c] = (byte)0x53;
		volume[0x1e] = (byte)0x4b;
		volume[0x20] = (byte)0x20;
		volume[0x22] = (byte)0x28;
		volume[0x24] = (byte)0x01;
		volume[0x26] = (byte)0x01;
		volume[0x70] = (byte)0x03;
	}

	private void clearVolume(int num, byte[] volume) {
		for (int i=0; i < 0x70; i+=2) {
			volume[i] = (byte)0x00;
		}
	}
	
	private Interval getVariable(String names) {
		// Determine the variable position in the string
		int posh1 = -1;
		int posh2 = -1;
		if (names.length()>0) {
			int i = 0;
			while (i < names.length() && (posh1 == -1)) {
				if (names.charAt(i) == '#') {
					posh1 = i;
				}
				i++;
			}
			
			if (posh1 > -1) {
				while (i < names.length() && (posh2 == -1)) {
					if (names.charAt(i) != '#') {
						posh2 = i-1;
					}
					i++;
				}
				
				if (posh2 == -1) posh2 = names.length()-1;
			}
			
			if (names.indexOf("#", posh2+1) == -1) {
				return new Interval(posh1, posh2);
			}
		}
		return null;
	}
}
