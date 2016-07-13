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
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.JOptionPane;
import java.util.Arrays;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class RawCHDAction extends Activity {

	public String getMenuName() {
		return "Import raw into CHD ...";
	}
	
	public String getActionName() {
		return "IMPORTRAW";
	}
	
	public void go() {
		java.io.File selectedfile = null;
		JOptionPane.showMessageDialog(m_parent, "First step: Select a raw file as input.", "Import", JOptionPane.INFORMATION_MESSAGE);
		
		JFileChooser jfc = null;
		if (imagetool.getSourceDirectory("image")!=null) {
			jfc = new JFileChooser(imagetool.getSourceDirectory("image"));
		}
		else jfc = new JFileChooser();
		
		Dimension dim = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);
		jfc.setMultiSelectionEnabled(false);
		
		int nReturn = jfc.showOpenDialog(m_parent);
		
		if (nReturn == JFileChooser.APPROVE_OPTION) {
			selectedfile = jfc.getSelectedFile();
			java.io.File filePar = selectedfile.getParentFile();
			if (!filePar.getName().equals(".")) imagetool.setSourceDirectory(filePar, "image");  // sets the property only for non-UNC paths
			imagetool.setProperty(TIImageTool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
		}
		else return;
		
		RawCHDDialog dialog = new RawCHDDialog(m_parent);
		dialog.createGui(imagetool.boldFont);
		dialog.setRawFile(selectedfile.getName());

		// Read the Volume Information Block
		
		int nCylinders = 0;
		int nHeads = 0;
		int nSectorsPerTrack = 0;
		int nSectorLength =0;
		boolean bSCSI = false;
		
		// Read VIB and allocation map
		byte[] abyHead = new byte[32*256];
		try {
			DataInputStream dis = new DataInputStream(new FileInputStream(selectedfile));
			dis.readFully(abyHead);
			dis.close();
		}
		catch (FileNotFoundException fx) {
			JOptionPane.showMessageDialog(m_parent, "File not found; has it been removed in the meantime?", "Read error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		catch (IOException iox) {
			JOptionPane.showMessageDialog(m_parent, "IO error when reading the raw file: " + iox.getClass().getName(), "Read error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		if (Volume.hasFloppyVib(abyHead)) {
			JOptionPane.showMessageDialog(m_parent, "You cannot import a floppy image into a CHD.", "Import error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// File system
		if (Volume.hasSCSIVib(abyHead)) {
			bSCSI = true;
		}
		dialog.setFileSystem(bSCSI? "SCSI" : "HFDC");

		// Geometry
		int nTotalAU = Utilities.getInt16(abyHead, 10);
		int nAUsize = ((abyHead[16]>>4)&0x0f) + 1;
		nSectorLength = 256;
		int nTotalSectors = nTotalAU * nAUsize;
				
		// For HFDC we take the values as found in the VIB
		if (!bSCSI) {
			nHeads = (abyHead[16] & 0x0f) + 1; 
			nSectorsPerTrack = abyHead[12] & 0xff;
			if (nHeads < 1 || nHeads > 16) {
				JOptionPane.showMessageDialog(m_parent, "Invalid raw data; head count must be 1..16, not " + nHeads, "Import error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (nSectorsPerTrack < 32) {
				JOptionPane.showMessageDialog(m_parent, "Number of sectors per track (" + nSectorsPerTrack + ") may be invalid; possibly no raw image data.", "Import error", JOptionPane.ERROR_MESSAGE);
				return;
			}	
			// we should again increase the number of total sectors because
			// this can be a converted SCSI image
			nTotalSectors = ((nTotalSectors + 511) / 512) * 512;
			
			nCylinders = nTotalSectors / nHeads / nSectorsPerTrack;
		}
		else {
			// For SCSI we have to guess some reasonable values.
			// These values are required for the "physical layer"; SCSI itself
			// provides a logical layer with linear block addressing.
			
			// Heads = 1 .. 16
			// Sectors/Track = 1 .. 255
			// Cylinders = total sectors / (Heads * Sectors/Track)
			// Cylinders = 1 .. 2048

			// Also: SCSI drives may show a number of sectors which are not
			// a product of cylinders, heads, and sectors, supposedly because of
			// an internal bad sector map.
			// We should round up to multiples of 512 sectors.
		
			nTotalSectors = ((nTotalSectors + 511) / 512) * 512;
			nSectorsPerTrack = 32; // assume this is always the case
			
			nCylinders = Utilities.guessGeometry(nSectorsPerTrack, nTotalSectors);
			if (nCylinders != -1) {
				nHeads = nTotalSectors / nSectorsPerTrack / nCylinders;
			}
			else {
				JOptionPane.showMessageDialog(m_parent, "Cannot determine geometry for SCSI image.", "Import error", JOptionPane.ERROR_MESSAGE);				
				return;
			}
		}	

		if (nAUsize != 1 && nAUsize != 2 && nAUsize != 4 && nAUsize != 8 && nAUsize != 16) {
			JOptionPane.showMessageDialog(m_parent, "Unplausible AU size: " + nAUsize + ". Should be 1,2,4,8, or 16.", "Import error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		
		dialog.setHeads(nHeads);
		dialog.setSectorsPerTrack(nSectorsPerTrack);
		dialog.setCylinders(nCylinders);
		dialog.setSectorLength(nSectorLength);
		dialog.setVisible(true);

		if (dialog.getCHDVersion()<5) {
			int nCheck1 = JOptionPane.showConfirmDialog(m_parent, "Are you sure you want to create an image with CHD version 4?\nThis is a deprecated format that is not accepted anymore in current MESS releases.", "Import as v4 image", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (nCheck1==JOptionPane.CANCEL_OPTION) {
				return;
			}
		}
		
		java.io.File fileTarget = null;
		if (dialog.confirmed()) {
			FormatParameters parm = new FormatParameters(nCylinders, nHeads, nSectorsPerTrack, nSectorLength, dialog.getCHDVersion());
			try {
				byte[] abyNewImage = MessCHDFormat.createEmptyCHDImage(parm);
				
				JFileChooser jfc1 = new JFileChooser();
				Dimension dim1 = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
				if (dim1!=null) jfc1.setPreferredSize(dim1);
				
				ImageFileFilter im1 = new ImageFileFilter();
				jfc1.addChoosableFileFilter(im1);
				jfc1.setFileFilter(im1);
				
				int nReturn1 = jfc1.showSaveDialog(m_parent);
				if (nReturn1 == JFileChooser.APPROVE_OPTION) {
					imagetool.setProperty(TIImageTool.FILEDIALOG, jfc1.getWidth() + "x" + jfc1.getHeight());
					fileTarget = jfc1.getSelectedFile();
					int nSuffixPos = fileTarget.getName().indexOf(".");
					if (nSuffixPos==-1 || nSuffixPos == fileTarget.getName().length()-1) { 
						if (!fileTarget.getName().endsWith(".hd") && !fileTarget.getName().endsWith(".chd")) {
							fileTarget = new java.io.File(fileTarget.getAbsolutePath() + ".hd");
						}
					}
					
					imagetool.setSourceDirectory(fileTarget.getParentFile(), "image");
					
					FileOutputStream fos = new FileOutputStream(fileTarget);
					fos.write(abyNewImage);
					fos.close();
				}
			}
			catch (IllegalOperationException iox) {	
				JOptionPane.showMessageDialog(m_parent, iox.getMessage(), "Import error", JOptionPane.ERROR_MESSAGE);		
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent, "IO error while importing new image: " + iox.getClass().getName(), "Conversion error", JOptionPane.ERROR_MESSAGE);		
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}

			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			// Open the target image
			// and for each hunksize of bytes read in the source, write the hunk into the target				
			boolean bOK = false;
			int nHunkNumber = 0;
			try {
				MessCHDFormat target = (MessCHDFormat)ImageFormat.getImageFormat(fileTarget.getAbsolutePath(), nSectorLength);
				target.reopenForWrite();
				
				// We have the allocation still from above
				AllocationMap alloc = new AllocationMap(nTotalAU, nAUsize, false);
				alloc.setMapFromBitfield(abyHead, 256, 0);
					
				DataInputStream dis = new DataInputStream(new FileInputStream(selectedfile));
				byte[] hunk = new byte[4096]; // hunk size
				byte[] aubytes = new byte[nAUsize * nSectorLength];
				int nAUPerHunk = 4096 / (nAUsize*nSectorLength); 
				
				// As the AU size is 1,2,4,8, or 16, and the sector size is 256,
				// a hunk always contains a multiple of AUs.
				
				for (int au=0; au < nTotalAU; au++) {
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
					if ((au==nTotalAU-1) || ((au % nAUPerHunk)==(nAUPerHunk-1))) {
						// System.out.println("Write hunk " + nHunkNumber + " after AU " + au);
						target.writeHunkContents(hunk, nHunkNumber++);
						// System.out.println(Utilities.hexdump(0, 0, hunk, hunk.length, false));
					}
				}				
				bOK = true;			
			}
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(m_parent, ix.getMessage(), "Conversion error", JOptionPane.ERROR_MESSAGE);		
			}
			catch (FileNotFoundException fnfx) {
				JOptionPane.showMessageDialog(m_parent, "Target file not found, seems to have disappeared.", "Conversion error", JOptionPane.ERROR_MESSAGE);		
			}
			catch (IOException iox) {
				iox.printStackTrace();
				JOptionPane.showMessageDialog(m_parent, "IO error while copying contents: " + iox.getClass().getName(), "Conversion error", JOptionPane.ERROR_MESSAGE);
			}
			
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			if (bOK) JOptionPane.showMessageDialog(m_parent, "Import completed successfully", "Import raw data", JOptionPane.INFORMATION_MESSAGE);
			if (bSCSI) JOptionPane.showMessageDialog(m_parent, "Warning: This image has a SCSI file system. Make sure that your emulator supports it.", "Import raw data", JOptionPane.INFORMATION_MESSAGE); 
		}
	}
}
