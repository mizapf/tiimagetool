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
    
    Copyright 2013 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.JFileChooser;
import java.awt.Dimension;
import java.awt.Cursor;
import java.io.*;
import javax.swing.JOptionPane;
import java.util.Arrays;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class RawCHDAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("ImportRaw") + "...";
	}
	
	public String getActionName() {
		return "IMPORTRAW";
	}
	
	public void go() {
	
		RawCHDDialog dialog = new RawCHDDialog(m_parent, imagetool);
		dialog.createGui(imagetool.boldFont);
		dialog.setVisible(true);
		
		File fileTarget = null;
		int chdVersion = 5;
		
		if (dialog.confirmed()) {
			if (!dialog.hasValidSettings()) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("RawCHDMissingRaw"), TIImageTool.langstr("ConversionError"), JOptionPane.ERROR_MESSAGE);		
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}
			FormatParameters parm = new FormatParameters(dialog.getCylinders(), dialog.getHeads(), 
				dialog.getSectorsPerTrack(), dialog.getSectorLength(), chdVersion);
			try {
				fileTarget = dialog.getTargetCHD();
				FileOutputStream fos = new FileOutputStream(fileTarget);
				fos.write(MameCHDFormat.createEmptyCHDImage(parm));
				fos.close();
			}
			catch (IllegalOperationException iox) {	
				JOptionPane.showMessageDialog(m_parent, iox.getMessage(),  TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);		
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent,  TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ConversionError"), JOptionPane.ERROR_MESSAGE);		
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}

			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			// Open the target image
			// and for each hunksize of bytes read in the source, write the hunk into the target				
			boolean bOK = false;
			int nHunkNumber = 0;
			try {
				MameCHDFormat target = (MameCHDFormat)ImageFormat.getImageFormat(fileTarget.getAbsolutePath());
				target.reopenForWrite();
				
				// We have the allocation still from above
				AllocationMap alloc = new AllocationMap(dialog.getTotalAU(), dialog.getAUSize(), false);
				alloc.setMapFromBitfield(dialog.getHeader(), 256, 0);
					
				DataInputStream dis = new DataInputStream(new FileInputStream(dialog.getRawFile()));
				byte[] hunk = new byte[4096]; // hunk size
				byte[] aubytes = new byte[dialog.getAUSize() * dialog.getSectorLength()];
				int nAUPerHunk = 4096 / (dialog.getAUSize() * dialog.getSectorLength()); 
				
				// As the AU size is 1,2,4,8, or 16, and the sector size is 256,
				// a hunk always contains a multiple of AUs.
				
				for (int au=0; au < dialog.getTotalAU(); au++) {
					// System.out.println("Reading AU " + au + ", allocated = " + alloc.hasAllocated(au));
					dis.readFully(aubytes);
					// System.out.println(Utilities.hexdump(0, 0, aubytes, aubytes.length, false));
					if (!alloc.hasAllocated(au) && dialog.fillWithZeros()) {
						Arrays.fill(aubytes, (byte)0);
					}
					System.arraycopy(aubytes, 0, hunk, aubytes.length * (au % nAUPerHunk), aubytes.length);
					// If we are at the last AU of the whole image or
					// at the last AU of the current hunk, write the hunk
					// to the target
					if ((au==dialog.getTotalAU()-1) || ((au % nAUPerHunk)==(nAUPerHunk-1))) {
						// System.out.println("Write hunk " + nHunkNumber + " after AU " + au);
						target.writeHunkContents(hunk, nHunkNumber++);
						// System.out.println(Utilities.hexdump(0, 0, hunk, hunk.length, false));
					}
				}				
				bOK = true;			
			}
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(m_parent, ix.getMessage(), TIImageTool.langstr("ConversionError"), JOptionPane.ERROR_MESSAGE);		
			}
			catch (FileNotFoundException fnfx) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("FileNotFoundUnexp"), TIImageTool.langstr("ConversionError"), JOptionPane.ERROR_MESSAGE);		
			}
			catch (IOException iox) {
				iox.printStackTrace();
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ConversionError"), JOptionPane.ERROR_MESSAGE);
			}
			
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			if (bOK) JOptionPane.showMessageDialog(m_parent, String.format(TIImageTool.langstr("Completed"), TIImageTool.langstr("Conversion")), TIImageTool.langstr("Conversion"), JOptionPane.INFORMATION_MESSAGE);
			if (dialog.isSCSI()) JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("RawCHDSCSIWarn"), TIImageTool.langstr("Conversion"), JOptionPane.WARNING_MESSAGE); 
		}
	}
}
