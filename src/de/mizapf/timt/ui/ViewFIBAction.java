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
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.*;
import java.awt.Cursor;
import java.io.EOFException;
import javax.swing.JOptionPane;

public class ViewFIBAction extends Activity {

	public String getMenuName() {
		return "View file information block";
	}
	
	public String getActionName() {
		return "VIEWFIB";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		Volume vol = dvCurrent.getVolume();

		String sText = "no content";
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		for (Element selected : dvCurrent.getSelectedEntries()) {
			if (selected instanceof TFile) {
				try {
					int[] anFIB = ((TFile)selected).getFIBLocations();
					byte[] content = new byte[anFIB.length * Volume.SECTOR_LENGTH];
					for (int i=0; i < anFIB.length; i++) {
						System.arraycopy(vol.readSector(anFIB[i]).getBytes(), 0, content, i * Volume.SECTOR_LENGTH, Volume.SECTOR_LENGTH);
					}
					StringBuilder sb = new StringBuilder();
					
					sb.append("====== Plain dump ======\n\n");
					sb.append(Utilities.hexdump(0, 0, content, content.length, false));
					
					// Analyse the contents
					if (vol.isFloppyImage()) sb.append("\nFloppy image");
					else {
						if (vol.isCF7Image()) sb.append("\nCF7 image");
						else sb.append("\nHard disk image");
					}
					sb.append("\n");
					
					TFile myfile = (TFile)selected;
					sb.append("\nFile name = ").append(Utilities.getString10(content, 0));
					sb.append("\nExtended rec len = ").append(Utilities.getInt16(content, 0x0a));
					sb.append("\nStatus flags = 0x").append(Utilities.toHex(content[0x0c],2)).append(" (");
					if ((content[0x0c]&0x01)!=0) {
						sb.append("PROGRAM");
					}
					else {
						if ((content[0x0c]&0x80)!=0) sb.append("VARIABLE"); else sb.append("FIXED");
						if ((content[0x0c]&0x02)!=0) sb.append(" INTERNAL"); else sb.append(" DISPLAY");
					}
					if ((content[0x0c]&0x20)!=0) sb.append(" EMULATE");
					if ((content[0x0c]&0x10)!=0) sb.append(" MODIFIED");
					if ((content[0x0c]&0x08)!=0) sb.append(" PROTECTED");			
					sb.append(")");
					
					sb.append("\nNumber of records per sector = ").append(content[0x0d] & 0xff);
					sb.append("\nNumber of allocated sectors = ").append(Utilities.getInt16(content, 0x0e));
					sb.append("\nEOF offset = ").append(content[0x10] & 0xff);
					sb.append("\nLogical record length = ").append(content[0x11] & 0xff);
					int l3 = (content[0x12] << 8) | (content[0x13] & 0xff);
					sb.append("\nLevel 3 record count (little endian) = 0x").append(Utilities.toHex(l3,4)).append(" = ").append(Utilities.getInt16rev(content, 0x12));

					sb.append("\nCreation time = ");
					if (Utilities.getInt32be(content, 0x14)==0) {
						sb.append("undefined");
					}
					else {
						Time cr = new Time(content, 0x14);
						sb.append(cr.toString());
					}
					sb.append("\nUpdate time = ");
					if (Utilities.getInt32be(content, 0x18)==0) {
						sb.append("undefined");
					}
					else {
						Time cr = new Time(content, 0x18);
						sb.append(cr.toString());
					}
					Interval[] blocks = myfile.getAllocatedBlocks();
					sb.append("\nAllocated sectors = ");
					if (blocks.length==0) sb.append("none");
					else {
						for (Interval in : blocks) sb.append(in.toString()).append(" ");
					}
					
					if (!vol.isFloppyImage() && !vol.isCF7Image()) {
						sb.append("\nPrevious FDR = ").append(Utilities.getInt16(content, 0x1e));
						sb.append("\nNext FDR = ").append(Utilities.getInt16(content, 0x20));
						sb.append("\nAllocated AU for this FDR = ").append(Utilities.getInt16(content, 0x22));
						sb.append("\nLocation of FDIR = ").append(Utilities.getInt16(content, 0x24));
						int ext = (content[0x26] << 8) | (content[0x27] & 0xff);
						sb.append("\nExtended information = 0x").append(Utilities.toHex(ext,4));
					}
					
					imagetool.showTextContent(selected.getName(), sb.toString());
				}
				catch (EOFException eofx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Error: " + eofx.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE); 					
				}
				catch (IOException iox) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Error reading file: " + iox.getClass().getName(), "Read error", JOptionPane.ERROR_MESSAGE); 
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Image error: " + ix.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
