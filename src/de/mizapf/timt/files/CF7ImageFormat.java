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

package de.mizapf.timt.files;

import java.io.RandomAccessFile;
// import java.io.FileOutputStream;
// import java.io.EOFException;
import java.io.IOException;
// import java.io.FileNotFoundException;
// import java.util.ArrayList;
import de.mizapf.timt.util.Utilities;

import java.io.File;
import java.io.FileInputStream;
import de.mizapf.timt.TIImageTool;

public class CF7ImageFormat extends SectorDumpFormat implements PartitionedStorage {
	
	int m_nPartitions;
	private int m_nActivePartition;
	Partition[] m_partition;
	
	static int vote(String sFile) throws IOException {
		int vote = 0;

		// System.out.println("vote CF7");
		File fl = new File(sFile);
		long nLength = fl.length();
		
		// File system size must be less than 3 MB
		if (nLength==0 || nLength < 409600) return 0;
		
		// If it is smaller than 10 MB, it must be a multiple of 2*400 KiB.
		if (nLength < 10000000) {
			if ((nLength % 819200)!=0) {
				// System.out.println("wrong length");
				return 0;
			}
		}
		
		// If it is longer, it may have any length because CF cards may have
		// a variable length

		// Test whether we have a DSK signature in at least one of the first
		// three partitions
		FileInputStream fis = new FileInputStream(sFile);
		int nReadlen = (nLength < 2457600)? (int)nLength : 2457600;
		
		byte[] aby = new byte[nReadlen];
		
		// Read the beginning
		int nReadBytes = 0;
		for (int i=0; i < nReadlen; i++) {
			int ch = fis.read();
			if (ch == -1) {
				break;
			}
			nReadBytes++;
			aby[i] = (byte)ch;
		}
		fis.close();
		
		boolean bFound = false;
		for (int i=0; i < 3; i++)  {
			if ((i*819200+0x1e) > nReadBytes) break;
			if ((aby[(i*819200)+0x1a] == 'D') && (aby[(i*819200)+0x1c] == 'S') || (aby[(i*819200)+0x1e] == 'K')) {
				bFound = true;
				break;
			}
		}
		if (bFound){
			// System.out.println("is a CF7 image");
			return 100;
		}
		// System.out.println("is not a CF7 image");		
		return 0;			
	}
	
	/** Codec for reading / writing CF7 */
	class CF7Codec extends FormatCodec {
		void decode() {
		}
		
		void encode() {
		}				
		
		void prepareNewFormatUnit(int funum, FormatUnitParameters t) {
		}
	}
	
	public CF7ImageFormat(String sImageName) throws IOException, ImageException {
		super(sImageName);
	}
	
	public String getFormatName() {
		return TIImageTool.langstr("CF7ImageType");
	}
	
	public void setPartition(int part) {
		m_nActivePartition = part;
	}
	
	public String getPartitionName(int part) {
		return "FIXME";
	}
	
	// FIXME: Called by OpenImageAction
	public void setupPartitionTable() {
		List<Partition> plist = new LinkedList<Partition>();

		setFormatUnitLength(32 * TFileSystem.SECTOR_LENGTH);
		byte[] sect0 = readSector(0).getData();
		
		
		
		for (int i=0; i < 4; i++) {
			int partsect = Utilities.getInt32be(sect0, 0x18 + i*8) * 2;
			int lensect = Utilities.getInt32be(sect0, 0x20 + i*8) * 2;
			// System.out.println(i + ": partsect=" + partsect);

			if (partsect != 0) {
				Sector vib = readSector(partsect);
				String sName = Utilities.getString10(vib.getData(), 0);
			//	System.out.println(sName);
				m_partition[i] = new Partition(i, partsect, lensect, sName); 
			}
		}
		
	}
	
	public int getActivePartition() {
		return -1;
	}
	
	static int getImageTypeStatic() {
		return CF7;
	}
}

/*
00000000: 5600 4f00 4c00 3100 2000 2000 2000 2000  V.O.L.1. . . . .
00000010: 2000 2000 0600 4000 1400 4400 5300 4b00   . ...@...D.S.K.
00000020: 2000 2800 0200 0200 0000 0000 0000 0000   .(.............
00000030: 0000 0000 0000 0000 0000 0000 0000 0000  ................
00000040: 0000 0000 0000 0000 0000 0000 0000 0000  ................

repeated every 0x64000

*/

