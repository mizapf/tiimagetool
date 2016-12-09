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

import javax.swing.JOptionPane;
import java.io.IOException;
import de.mizapf.timt.files.*;
import java.awt.Cursor;
import java.io.EOFException;
import java.io.FileNotFoundException;
import de.mizapf.timt.util.Utilities;

public class ViewImageAction extends Activity {

	public String getMenuName() {
		return imagetool.langstr("ViewImage");
	}
	
	public String getActionName() {
		return "VIEWIMAGE";
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
					showPicture((TFile)selected, vol, dvCurrent);
				}
				catch (IOException iox) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Error reading file: " + iox.getClass().getName(), "Read error", JOptionPane.ERROR_MESSAGE); 
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Image error: " + ix.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE); 
				}
				catch (FormatException fx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), fx.toString(), "Read error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	void showPicture(TFile sel, Volume vol, DirectoryView dvCurrent) throws FormatException, IOException, ImageException {
		byte[] abyPattern = null;
		byte[] abyColor = null;
		byte[] abyContent = null;
		int nRecCount = 0;
		int nType = 0;
		Directory dirCurrent = dvCurrent.getDirectory();

		if (sel.isProgram()) {
			String sName1 = sel.getName();
			if (sName1.endsWith("_P")) {
				abyPattern = sel.getRawContent();
				String sColorTable = sName1.substring(0, sName1.length()-1) + "C";
				try {
					TFile col = dirCurrent.getFile(sColorTable);
					abyColor = col.getRawContent();
				}
				catch (FileNotFoundException fnfx) {
					System.err.println("Could not find companion color file: " + fnfx.getMessage());
					// Create a black/white color table
					abyColor = new byte[6144];
					for (int i=0; i < abyColor.length; i++) {
						abyColor[i] = (byte)0x1f;
					}
				}							
			}
			else {
				if (sName1.endsWith("_C")) {
					abyColor = sel.getRawContent();									
					String sPatternTable = sName1.substring(0, sName1.length()-1) + "P";
					try {
						TFile pat = dirCurrent.getFile(sPatternTable);
						abyPattern = pat.getRawContent();
					}
					catch (FileNotFoundException fnfx) {
						System.err.println("Could not find companion pattern file: " + fnfx.getMessage());
						// Create a checkered pattern
						abyPattern = new byte[6144];
						for (int i=0; i < abyColor.length; i+=2) {
							abyPattern[i] = (byte)0xaa;
							abyPattern[i+1] = (byte)0x55;
						}
					}							
				}
				else {
					abyPattern = sel.getRawContent();
					abyColor = new byte[6144];
					for (int i=0; i < abyColor.length; i++) {
						abyColor[i] = (byte)0x1f;
					}
				}
			}
			// Combine both files
			abyContent = new byte[12288];
			System.arraycopy(abyPattern, 0, abyContent, 0, 6144);
			System.arraycopy(abyColor, 0, abyContent, 6144, 6144);
			nRecCount = 0;
		}
		else {
			abyContent = sel.getRecordContent();
		}
		try {
			ImageFrame ifr = new ImageFrame(sel.getName(), abyContent, sel.getRecordLength(), imagetool);
			ifr.showImage(null, 640);
		}
		catch (FormatException fx) {
			try {
//				JOptionPane.showMessageDialog(dvCurrent.getFrame(), sel.getName() + ": Not an image file; showing plain dump", "Read error", JOptionPane.INFORMATION_MESSAGE);
				byte[] content = sel.getRawContent();
				imagetool.showTextContent(sel.getName(), Utilities.hexdump(0, 0, content, content.length, false));
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
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Error when rendering the image", "Read error", JOptionPane.ERROR_MESSAGE);			
		}
	}
}
