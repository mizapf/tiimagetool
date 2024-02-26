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
    
    Copyright 2023 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.util.InternalException;

/* Partition table:

   00-09: Disk name (default: *TI99FSPT*")
   0a-0b: FFFF 	    
   0c-0d: 0000
   0e-0f: "PT"
   10-13: 0000 0000
   14-17: Total number of 512-sectors (BE)
   18-1b: Offset 1st partition    (BE)
   1c-1f: #sectors 1st partition  (BE)
   20-23: Offset 2nd partition
   24-27: #sectors 2nd partition
   28-2b: Offset 3rd partition
   2c-2f: #sectors 3rd partition
   30-33: Offset 4th partition
   34-37: #sectors 4th partition
   38-fd: 00
   fe-ff: 5aa5  
   
   total number = 2048*totalsize
   Offset1 = 00000001 (*512)
   size1 = 2048*size1mib
   offset2 = offset1 + size1
   size2 = 2048*size2mib
   offset3 = offset2 + size2
   size3 = 2048*size3mib
   offset4 = offset3 + size3
   size4 = 2048*size4mib
   
   Partition:
   00-09: Volume name                      name[i]
   0a-0b: Total number of AUs              size[i] * 
   0c:    Reserved                         00
   0d:    Reserved AUs                     64
   0e-0f: Unused                           0000
   10-11: Sec/AU-1(4), 0(12)                              7000
   12-15: Time/date of creation            Time.createNow
   16:    #files                           00
   17:    #dirs                            00
   18-19: AU of FDIR                       64 / ausize    
   1a-1b: AU of DSK1 emulation file        0000
   1C-FF: subdirs                          00 ... 00
   100..: Allocation table                                111111111100...  

   

   offset == 0 -> unused
   
   
*/

public class NewIDEImageAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_I;
	}
	
	public String getMenuName() {
		return TIImageTool.langstr("Menu.IDE.New") + "...";
	}
	
	public String getActionName() {
		return "NEWIDE";
	}
	
	public void go() {
		NewIDEImageDialog newimage = new NewIDEImageDialog(m_parent);

		try {
			newimage.createGui(imagetool.boldFont);
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		newimage.setVisible(true);
		
		if (newimage.confirmed()) {
			// System.out.println("New partitions: ");
			String[] asPart = newimage.getPartitionNames();
			int[] anSize = newimage.getPartitionSizes();
			// for (int i=0; i < asPart.length; i++) {
			// 	System.out.println(asPart[i] + ": " + anSize[i]);
			// }
			
			SaveAsImageDialog sd = new SaveAsImageDialog(m_parent, ImageFormat.MEMORY, false);
			
			sd.createGui(imagetool.boldFont);
			sd.setVisible(true);
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			if (sd.confirmed()) {		
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
						selectedFile = new File(ImageFormat.getSuffixedName(select.getAbsolutePath(), sd.getImageType()));
					}
				}
				else {
					selectedFile = null;
				}
				
				if (selectedFile != null) {
					// System.out.println("Save new IDE image");
					if (selectedFile.exists()) {		
						int nRet = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("ExistsOverwrite"), TIImageTool.langstr("NewImageTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if (nRet == JOptionPane.NO_OPTION) {
							m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
							return;
						}
						selectedFile.delete();
					}
					
					MemoryImageFormat mif = new MemoryImageFormat("idepart", 0, true);
					
					byte[] parttable = new byte[TFileSystem.SECTOR_LENGTH];
					Utilities.setString(parttable, 0, "*TI99FSPT*", 10);
					Utilities.setInt16(parttable, 0x0a, 0xffff);
					Utilities.setInt16(parttable, 0x0c, 0x0000);
					Utilities.setString(parttable, 0x0e, "PT", 2);
					Utilities.setInt32be(parttable, 0x10, 0x00000000);
					
					int sectors512 = newimage.getCapacity() * 2048;
					Utilities.setInt32be(parttable, 0x14, sectors512);
					
					int offset = 1;
					int size = 0;
					
					int[] secoff = new int[4];
					
					for (int i=0; i < anSize.length; i++) {
						size = newimage.getParameters(i).totalsectors / 2;
						Utilities.setInt32be(parttable, 0x18 + i*8, offset);
						Utilities.setInt32be(parttable, 0x1c + i*8, size);
						secoff[i] = offset;
						offset += size; 
					}
					
					for (int i = 0x18 + 8*anSize.length; i < 254; i++) {
						parttable[i] = (byte)0;
					}
					Utilities.setInt16(parttable, 0xfe, 0x5aa5);
						
					// System.out.println(Utilities.hexdump(parttable));
					// Write the partition table
					mif.writeSector(new Sector(0, parttable));
					
					// Sector 1 is empty (second half of the first 512-byte sector)
					byte[] empty = new byte[TFileSystem.SECTOR_LENGTH];
					for (int i=0; i < empty.length; i++)
						empty[i] = (byte)0x00;
					
					mif.writeSector(new Sector(1, empty));
					
					for (int i=0; i < anSize.length; i++) {
						FormatParameters param = newimage.getParameters(i);
						IDEFileSystem fs = new IDEFileSystem(param);
						byte[] initsec = fs.createInitArray();
						// System.out.println(Utilities.hexdump(initsec));
						
						int startSector = secoff[i] * 2;
						
						for (int j=0; j < initsec.length/TFileSystem.SECTOR_LENGTH; j++) {
							Sector sec = new Sector(startSector + j, initsec, j * TFileSystem.SECTOR_LENGTH);
							mif.writeSector(sec);
						}					
					}
					
					mif.nextGeneration(true);
				
					// New FileImageFormat
					
					FormatParameters newparams = new FormatParameters("IDE", false);
					newparams.type = HarddiskFileSystem.IDE;
					newparams.totalsectors = 2*sectors512;
					newparams.formatUnitSectors = 32;
					
					try {
						FileImageFormat newFileImage = (FileImageFormat)ImageFormat.getImageFormat(selectedFile.getAbsolutePath(), sd.getImageType(), newparams);
						if (newFileImage == null) {
							throw new InternalException(TIImageTool.langstr("ImageUnknown") + ": " + ImageFormat.suffix[sd.getImageType()]);
						}						
						newFileImage.saveImageFromOld(mif);

						JOptionPane.showMessageDialog(m_parent, String.format(TIImageTool.langstr("Completed"), TIImageTool.langstr("Formatting")), TIImageTool.langstr("Title.IDE.CreateNew"), JOptionPane.INFORMATION_MESSAGE);
						
					}
					catch (InternalException e) {
						e.printStackTrace();
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("InternalError") + ": " + e.getMessage(), TIImageTool.langstr("InternalError"), JOptionPane.ERROR_MESSAGE);
					}
					catch (IOException iox) {
						iox.printStackTrace();
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("WriteError"), JOptionPane.ERROR_MESSAGE);
					}
					catch (ImageException ix) {
						ix.printStackTrace();
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ImageError"), JOptionPane.ERROR_MESSAGE);				
					}				
				}
			}
			
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
}
/* Example for 40 MiB disk  (PART1: 25 MiB, PART2: 15 MiB)

00000000: 2a54 4939 3946 5350 542a ffff 0000 5054  *TI99FSPT*....PT
00000010: 0000 0000 0001 4000 0000 0001 0000 c800  ......@.........
00000020: 0000 c801 0000 77ff 0000 0000 0000 0000  ......w.........
00000030: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
000000f0: 0000 0000 0000 0000 0000 0000 0000 5aa5  ..............Z.
00000100: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
00000200: 5041 5254 3120 2020 2020 c800 0064 0000  PART1     ...d..
00000210: 1000 0000 0000 0000 0020 0000 0000 0000  ......... ......
00000220: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
00000300: ffff ffff 8000 0000 0000 0000 0000 0000  ................
00000310: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
00001c00: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001c10: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001c20: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001c30: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001c40: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001c50: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001c60: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001c70: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001c80: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001c90: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001ca0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001cb0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001cc0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001cd0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001ce0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001cf0: ffff ffff ffff ffff ffff ffff ffff ffff  ................

00001d00: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001d10: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001d20: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001d30: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001d40: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001d50: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001d60: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001d70: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001d80: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001d90: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001da0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001db0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001dc0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001dd0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001de0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001df0: ffff ffff ffff ffff ffff ffff ffff ffff  ................

00001e00: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001e10: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001e20: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001e30: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001e40: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001e50: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001e60: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001e70: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001e80: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001e90: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001ea0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001eb0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001ec0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001ed0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001ee0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001ef0: ffff ffff ffff ffff ffff ffff ffff ffff  ................

00001f00: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001f10: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001f20: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001f30: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001f40: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001f50: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001f60: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001f70: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001f80: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001f90: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001fa0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001fb0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001fc0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001fd0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001fe0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00001ff0: ffff ffff ffff ffff ffff ffff ffff ffff  ................

00002000: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002010: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002020: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002030: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002040: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002050: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002060: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002070: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002080: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002090: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000020a0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000020b0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000020c0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000020d0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000020e0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000020f0: ffff ffff ffff ffff ffff ffff ffff ffff  ................

00002100: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002110: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002120: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002130: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002140: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002150: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002160: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002170: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002180: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00002190: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000021a0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000021b0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000021c0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000021d0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000021e0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000021f0: ffff ffff ffff ffff ffff ffff ffff ffff  ................

00002200: 5041 5254 3120 2020 2020 c800 0064 0000  PART1     ...d..
00002210: 1000 0000 0000 0000 0020 0000 0000 0000  ......... ......
00002220: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
00002300: ffff ffff 8000 0000 0000 0000 0000 0000  ................
00002310: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
00003c00: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003c10: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003c20: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003c30: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003c40: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003c50: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003c60: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003c70: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003c80: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003c90: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003ca0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003cb0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003cc0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003cd0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003ce0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003cf0: ffff ffff ffff ffff ffff ffff ffff ffff  ................

00003d00: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003d10: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003d20: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003d30: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003d40: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003d50: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003d60: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003d70: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003d80: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003d90: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003da0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003db0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003dc0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003dd0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003de0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003df0: ffff ffff ffff ffff ffff ffff ffff ffff  ................

00003e00: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003e10: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003e20: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003e30: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003e40: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003e50: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003e60: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003e70: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003e80: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003e90: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003ea0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003eb0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003ec0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003ed0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003ee0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003ef0: ffff ffff ffff ffff ffff ffff ffff ffff  ................

00003f00: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003f10: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003f20: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003f30: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003f40: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003f50: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003f60: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003f70: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003f80: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003f90: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003fa0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003fb0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003fc0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003fd0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003fe0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00003ff0: ffff ffff ffff ffff ffff ffff ffff ffff  ................

00004000: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004010: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004020: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004030: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004040: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004050: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004060: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004070: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004080: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004090: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000040a0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000040b0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000040c0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000040d0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000040e0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000040f0: ffff ffff ffff ffff ffff ffff ffff ffff  ................

00004100: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004110: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004120: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004130: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004140: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004150: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004160: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004170: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004180: ffff ffff ffff ffff ffff ffff ffff ffff  ................
00004190: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000041a0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000041b0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000041c0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000041d0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000041e0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
000041f0: ffff ffff ffff ffff ffff ffff ffff ffff  ................

00004200: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
01900200: 5041 5254 3220 2020 2020 effe 0064 0000  PART2     ...d..
01900210: 0000 0000 0000 0000 0040 0000 0000 0000  .........@......
01900220: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
01900300: ffff ffff ffff ffff 8000 0000 0000 0000  ................
01900310: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
019020f0: 0000 0000 0000 0000 0000 0000 0000 0003  ................
01902100: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01902110: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01902120: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01902130: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01902140: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01902150: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01902160: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01902170: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01902180: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01902190: ffff ffff ffff ffff ffff ffff ffff ffff  ................
019021a0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
019021b0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
019021c0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
019021d0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
019021e0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
019021f0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01902200: 5041 5254 3220 2020 2020 effe 0064 0000  PART2     ...d..
01902210: 0000 0000 0000 0000 0040 0000 0000 0000  .........@......
01902220: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
01902300: ffff ffff ffff ffff 8000 0000 0000 0000  ................
01902310: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
019040f0: 0000 0000 0000 0000 0000 0000 0000 0003  ................
01904100: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01904110: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01904120: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01904130: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01904140: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01904150: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01904160: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01904170: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01904180: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01904190: ffff ffff ffff ffff ffff ffff ffff ffff  ................
019041a0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
019041b0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
019041c0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
019041d0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
019041e0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
019041f0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
01904200: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
027ffff0: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*/