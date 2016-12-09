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

import java.awt.Cursor;
import java.io.*;
import java.util.*;

import javax.swing.JOptionPane;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.*;
import de.mizapf.timt.TIImageTool;

public class FormatCFCardAction extends Activity {
	
	public String getMenuName() {
		return imagetool.langstr("FormatCF");
	}
	
	public String getActionName() {
		return "FORMATCF";
	}
	
	public void go() {
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		FormatCFDialog fcfd = new FormatCFDialog(m_parent, imagetool);

		fcfd.createGui(imagetool.boldFont);
		fcfd.setVisible(true);

		if (fcfd.confirmed()) {
			try {
				String path = fcfd.getImagePath();
				if (path == null) {
					JOptionPane.showMessageDialog(m_parent, "No image file selected.", "Format CF", JOptionPane.ERROR_MESSAGE);
				}
				else {
					RandomAccessFile raf = new RandomAccessFile(path, "rw");
					
					Interval[] intv = fcfd.getIntervals();
					String names = fcfd.getVolumeNames();
					
					// Check for validity
					if (intv.length > 0) {					
						if (names.length() < 10) {
							// Check for formatted volume
							ArrayList<Integer> listFormatted = new ArrayList<Integer>();
							for (int i=0; i < intv.length; i++) {
								for (int j=intv[i].start; j <= intv[i].end; j++) {
									if (alreadyFormatted(raf, j)) listFormatted.add(j);
								}
							}			
							
							boolean stop = false;
							if (listFormatted.size() > 0) {
								StringBuilder sb = new StringBuilder();
								sb.append("There are already formatted volumes: ");
								int count = 1;			
								int maxlen = listFormatted.size();					
								for (Integer i : listFormatted) {
									if (count>1) sb.append(", ");
									sb.append(i);
									count++;
									if (count>10) { 
										sb.append(" ... (and more)"); 
										break;
									}
								}
								sb.append(". Continue formatting?");
								int nRet = JOptionPane.showConfirmDialog(m_parent, sb.toString(), "Format CF", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
								if (nRet == JOptionPane.NO_OPTION) stop = true;
							}
							
							if (!stop) {
								Interval var = getVariable(names);
								if (var == null) {
									JOptionPane.showMessageDialog(m_parent, "Invalid volume name format: " + names, "Format CF", JOptionPane.ERROR_MESSAGE);
								}
								else {
									for (int i=0; i < intv.length; i++) {
										for (int j=intv[i].start; j <= intv[i].end; j++) {
											String sName = createName(names, j, var.start, var.end);
											formatVolume(j, sName, raf);
										}
									}
									JOptionPane.showMessageDialog(m_parent, "Formatting completed successfully.", "Format CF", JOptionPane.INFORMATION_MESSAGE);
								}
							}
						}
						else {
							JOptionPane.showMessageDialog(m_parent, "Volume names must be 10 characters or less.", "Format CF", JOptionPane.ERROR_MESSAGE);
						}
					}
					else {
						JOptionPane.showMessageDialog(m_parent, "No volumes specified for formatting.", "Format CF", JOptionPane.ERROR_MESSAGE);
					}
					raf.close();	
				}
			}
			catch (FileNotFoundException fnfx) {
				JOptionPane.showMessageDialog(m_parent, "Cannot find a CF7 image with this file name.", "Format CF", JOptionPane.ERROR_MESSAGE);
			}
			catch (IOException iox) {
				iox.printStackTrace();
				JOptionPane.showMessageDialog(m_parent, "Error while writing a volume to this CF7 image.", "Format CF", JOptionPane.ERROR_MESSAGE);
			}
			catch (NumberFormatException nfx) {
				JOptionPane.showMessageDialog(m_parent, nfx.getMessage() , "Format CF", JOptionPane.ERROR_MESSAGE);
			}
		}

		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
	
	private boolean alreadyFormatted(RandomAccessFile raf, int volnum) throws IOException {
		byte[] header = new byte[0x20];
		raf.seek((volnum-1) * 512 * 1600);
		raf.readFully(header);
		return (header[0x1a] == 'D' && header[0x1c] == 'S' && header[0x1e] == 'K'); 
	}
	
	private String createName(String name, int num, int start, int end) {
		if (start == -1) return name;
		StringBuilder sName = new StringBuilder();
		
		String pref = name.substring(0, start);
		String tail = name.substring(end+1);
		boolean flex = (start == end);
		String number = String.valueOf(num);
		
		sName.append(pref);
		if (flex) sName.append(number);
		else {
			int len = number.length();
			int digits = end-start+1;
			if (len > digits) sName.append(number.substring(len-digits));
			else {
				String nulls = "0000000000";
				sName.append(nulls.substring(0, digits-len));
				sName.append(number);				
			}
		}
		sName.append(tail);
		return sName.toString();
	}
	
	private void formatVolume(int num, String sName, RandomAccessFile raf) throws IOException {
		// System.out.println("Formatting volume " + num + " with name " + sName);
		
		// Create an empty image
		byte[] image = new byte[512 * 1600];
		for (int i=0; i < image.length; i++) image[i] = (byte)0x00;
		
		// Write name
		for (int i=0; i < 10; i++) {
			if (i < sName.length()) image[i*2] = sName.getBytes()[i];
			else image[i*2] = (byte)0x20;
		}
	
		int sectorsPerTrack = 20;
		int sides = 2;
		int tracks = 40;
		int nsectors = sides * tracks * sectorsPerTrack;
		int density = ImageFormat.DOUBLE_DENSITY;
		
		image[0x14] = (byte)(nsectors >> 8);
		image[0x16] = (byte)(nsectors % 256);
		image[0x18] = (byte)sectorsPerTrack;
		image[0x1a] = 'D';
		image[0x1c] = 'S';
		image[0x1e] = 'K';
		image[0x20] = (byte)0x20;
		image[0x22] = (byte)tracks;
		image[0x24] = (byte)sides;
		image[0x26] = (byte)(density+1);

		// Allocation of sector 0 and sector 1		
		image[0x38*2] = (byte)0x03;
		// Sector 1 and all other sectors are empty		
		
		// Write this image to the file
		raf.seek((num-1) * 512 * 1600);
		raf.write(image);	
	}
}
