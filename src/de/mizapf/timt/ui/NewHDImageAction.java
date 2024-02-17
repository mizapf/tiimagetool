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
import java.io.*;
import java.awt.Dimension;
import java.awt.event.KeyEvent;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.NotImplementedException;

/*
Contents of a new HD image (example of a 20 MiB hd, 615-4-32)

CHD container (after initial creation)

0000000: 4d43 6f6d 7072 4844 0000 007c 0000 0005  MComprHD...|....
0000010: 0000 0000 0000 0000 0000 0000 0000 0000  ................
0000020: 0000 0000 0133 8000 0000 0000 0000 007c  .....3.........|
0000030: 0000 0000 0000 4d5c 0000 1000 0000 0100  ......M\........

0000040: 0000 0000 0000 0000 0000 0000 0000 0000  (rawsha1)
0000050: 0000 0000 
0000054: 0000 0000 0000 0000 0000 0000 0000 0000  (sha1)
0000064: 0000 0000 
0000068: 0000 0000 0000 0000 0000 0000 0000 0000  (parentsha1)
0000078: 0000 0000

000007c: 0000 0000  
0000080: 0000 0000 0000 0000 0000 0000 0000 0000  ................
*
0004d50: 0000 0000 0000 0000 0000 0000 

Map length = #hunks * 4 (4 bytes/hunk)

0004d5c: 4744 4444 0100 0021 0000 0000 0000 0000  GDDD   (flags=CHD_MDFLAGS_CHECKSUM (01) | len=0x21 (3 Bytes) | 8*0x00)
0004d6c: 4359 4c53 3a36 3135 2c48 4541 4453 3a34  CYLS:615,HEADS:4
0004d7c: 2c53 4543 533a 3332 2c42 5053 3a32 3536  ,SECS:32,BPS:256
0004d8c: 00                                       .

MComprHD
length=0x0000007c
version=0x00000005
compressors=4*0x00000000
logicalbytes=0x0000000001338000
mapoffset=0x000000000000007c
metaoffset=0x0000000000004d5c   = mapoffset + #hunks*mapentrysize (4)
hunkbytes=0x00001000   (MESS default)
unitbytes=0x00000100   (TI fixed default)
rawsha1=20*0x00
sha1=20*0x00
parentsha1=20*0x00



Raw content:

000000 41 42 43 44 45 46 20 20 20 20 99 c0 20 20 01 3a
000010 13 1d a7 6a 19 92 00 00 00 20 00 00 00 00 00 00
000020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
...
0000f0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

Volume name = "ABCDEF    "
Total number of AU = 0x99c0 (39360, i.e. 78720 sectors with 2 s/au)
Sectors/track = 0x20
Reserved AUs = 0x20 (* 0x40 = 2048)
Step rate = 0x01
Reduced write curr = 0x3a (*8 = 464)
Sect per AU = 0x1 (+1 = 2)
Heads = 0x3 (+1 = 4)
Buffered = 0
Precomp = 0x1d (*16 = 464)
Date/time = a76a 1992
Files = 0x00
Subdirs = 0x00
AU_FDIR = 0x0020
DSK1 = 0x0000
Subdirs = 00..00

Allocation bitmap

000100 ff ff ff ff 80 00 00 00 00 00 00 00 00 00 00 00
000110 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
001ff0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

Occupied AUs: 0 ... 32 

Copy of VIB
002000 4e 45 55 45 48 44 20 20 20 20 99 c0 20 20 01 3a
002010 13 1d a7 6a 19 92 00 00 00 20 00 00 00 00 00 00
002020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
0020f0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

Copy of allocation bitmap
002100 ff ff ff ff 80 00 00 00 00 00 00 00 00 00 00 00
002110 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
0040f0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
004100 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5
*
1338000

Allocation: 65 sectors (33 AUs)

=====================================================================

000000 41 42 43 44 45 46 20 20 20 20 a0 00 20 20 01 3c
000010 13 1e 73 e1 1a 2d 01 00 00 20 00 00 00 00 00 00
000020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
000100 ff ff ff ff c0 00 00 00 00 00 00 00 00 00 00 00
000110 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
000200 80 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
000210 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
002000 41 42 43 44 45 46 20 20 20 20 a0 00 20 20 01 3c
002010 13 1e 73 e1 1a 2d 00 00 00 20 00 00 00 00 00 00
002020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
002100 ff ff ff ff 80 00 00 00 00 00 00 00 00 00 00 00
002110 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
004000 00 21 00 00 00 00 00 00 00 00 00 00 00 00 00 00
004010 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
004100 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5
*
004200 54 45 53 54 46 49 4c 45 20 20 00 00 90 03 00 01
004210 12 50 01 00 73 f3 1a 2d 73 f3 1a 2d 46 49 00 00
004220 00 00 00 01 00 20 00 00 08 00 08 00 00 00 00 00
004230 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
004300 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5
*
100000 11 44 49 45 53 20 49 53 54 20 45 49 4e 20 54 45
100010 53 54 ff e1 1a 2d 01 00 00 20 00 00 00 00 00 00
100020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
100100 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5
*
1400000

FDIR at 004000
FDR at 004200
File contents at 100000 (AU 0x0800 = res * 0x40)

Allocation: 34 AUs at start, 1 AU later

michael@bellatrix:~/Entwicklung/tigeneve> od -Ax -tx1 testhard640_2048.raw 
000000 41 42 43 44 45 46 20 20 20 20 a0 00 20 30 01 3c
000010 13 1e 02 4d 1a 2d 00 00 00 20 00 00 00 00 00 00
000020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
000100 ff ff ff ff 80 00 00 00 00 00 00 00 00 00 00 00
000110 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
002000 41 42 43 44 45 46 20 20 20 20 a0 00 20 30 01 3c
002010 13 1e 02 4d 1a 2d 00 00 00 20 00 00 00 00 00 00
002020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
002100 ff ff ff ff 80 00 00 00 00 00 00 00 00 00 00 00
002110 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
004000 00 21 00 00 00 00 00 00 00 00 00 00 00 00 00 00
004010 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
004100 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5

Reserved = 0x30 (* 0x40 = 3072)
Reduced = 0x3c
Precomp = 0x1e
AU_FDIR = 0x0020

FDIR at 004000
FDR at 004200 (AU 0x21)
File contents at 180000 (AU 0x0c00 = res * 0x40)


000000-000009: Volume name (char[10]) = "ABCDEF    "
00000a-00000b: Total AU (int16) = a000

=================================================================

40 MiB (4 sect/AU, 1 file)

000000 41 42 43 44 45 46 20 20 20 20 a0 00 20 40 01 3c
000010 37 1e 63 64 1a 2d 01 00 00 10 00 00 00 00 00 00
000020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
000100 ff ff c0 00 00 00 00 00 00 00 00 00 00 00 00 00
000110 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
000300 80 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
000310 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
002000 41 42 43 44 45 46 20 20 20 20 a0 00 20 40 01 3c
002010 37 1e 63 64 1a 2d 00 00 00 10 00 00 00 00 00 00
002020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
002100 ff ff 80 00 00 00 00 00 00 00 00 00 00 00 00 00
002110 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
004000 00 11 00 00 00 00 00 00 00 00 00 00 00 00 00 00
004010 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
004100 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5
*
004400 54 45 53 54 46 49 4c 45 20 20 00 00 90 03 00 01
004410 12 50 01 00 63 90 1a 2d 63 90 1a 2d 46 49 00 00
004420 00 00 00 01 00 10 00 00 10 00 10 00 00 00 00 00
004430 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
004500 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5
*
400000 11 44 49 45 53 20 49 53 54 20 45 49 4e 20 54 45
400010 53 54 ff 64 1a 2d 01 00 00 10 00 00 00 00 00 00
400020 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
*
400100 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5 d7 a5

Reserved = 0x40 (* 0x40 = 3072)
Reduced = 0x3c
Precomp = 0x1e
AU_FDIR = 0x0010

FDIR at 004000 (AU 0x0010)
FDR at 004400 (AU 0x0011)
File contents at 400000 (AU 0x1000 = res * 0x40)


000000-000009: Volume name (char[10]) = "ABCDEF    "
00000a-00000b: Total AU (int16) = a000

Allocation: 65 sectors (17 AUs)

=========================================

Sector 0: VIB
Sector 1: 1-bits, # = (65 + (ausize-1))/ausize
Sector 2 - 31: 0
Sector 32 = copy of 0
Sector 33 - 63: copy of 1-31
Sector 64: 0 


*/

public class NewHDImageAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_M;
	}
	
	public String getMenuName() {
		return TIImageTool.langstr("HarddiskImage") + "...";
	}
	
	public String getActionName() {
		return "NEWHDIMAGE";
	}
	
	public void go() {
		boolean bValid = false;
		while (!bValid) {
			NewHDImageDialog newimagedialog = new NewHDImageDialog(m_parent);
			newimagedialog.createGui(imagetool.boldFont);
			newimagedialog.setVisible(true);
			if (newimagedialog.confirmed()) {
				FormatParameters parm = newimagedialog.getParameters();
				if (parm.isHFDC()) {
					if (parm.cylinders < 100 || parm.cylinders > 2048) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("Image.NewHD.InvalidCylinderCount"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
						continue;
					}
					if (parm.heads < 1 || parm.heads > 16) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("Image.NewHD.InvalidHeadCount"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
						continue;
					}
					
					if (parm.sectors <= 8 || parm.sectors > 256) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("Image.NewHD.InvalidSectorCount"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
						continue;
					}
				}
				int size = 0;
				if (parm.isHFDC()) {
					size = (parm.cylinders * parm.heads * parm.sectors)/4096;  
				}
				else {
					size = newimagedialog.getCapacity();
				}
				if (size > 248) {
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("Image.NewHD.TooBig"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
					continue;
				}
				bValid = true;

				HarddiskFileSystem hfs = HarddiskFileSystem.getInstance(parm);
				Volume newVolume = null;
			
				newVolume = new Volume(hfs, imagetool.nextUnnamedIndex());
				Directory root = newVolume.getRootDirectory();					
				imagetool.addDirectoryView(root);
				newVolume.nextGeneration(true);
			}
			else break;
		}
	}
}
