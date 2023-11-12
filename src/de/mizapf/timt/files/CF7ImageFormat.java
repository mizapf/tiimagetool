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

// import java.io.FileOutputStream;
// import java.io.EOFException;

// import java.io.FileNotFoundException;
// import java.util.ArrayList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.List;
import java.util.LinkedList;

import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.Utilities;

public class CF7ImageFormat extends FileImageFormat implements PartitionedStorage {
		
	static int vote(String sFile) throws IOException {
		int vote = 0;

		// System.out.println("vote CF7");
		File fl = new File(sFile);
		long nLength = fl.length();
		
		// File system size must be less than 3 MB
		if (nLength==0 || nLength < 409600) return 0;
		
		// If it is smaller than 10 MB, it must be a multiple of 2*400 KiB.
		// If it is longer, it may span the whole size of the CF card which
		// need not be a multple of 2*400 KiB.
		if (nLength < 10000000) {
			if ((nLength % 819200)!=0) {
				// System.out.println("wrong length");
				return 0;
			}
		}

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
			System.out.println("is a CF7 image");
			return 100;
		}
		System.out.println("is not a CF7 image");		
		return 0;			
	}
	
	/** Codec for reading / writing CF7.
		Geometry: 20 sect/track
		          40 tracks
		           2 sides
		      double density
		        1600 sectors
		        
		Format units are one track (20 sectors)
	*/
	class CF7Codec extends FormatCodec {
		void decode() {
			m_decodedSectors.clear();
			
			int j = 0;
			int pos = 0;
			int number = 0;
			int count = 20;

			// System.out.println("decode format unit " + m_nCurrentFormatUnit + ", length " + m_formatUnit.length );	
			byte[] abySector = new byte[TFileSystem.SECTOR_LENGTH];
			int startSector = m_nCurrentFormatUnit * count - getPartitionSectorOffset();
			
			for (int i=0; i < m_formatUnit.length; i+=2) {
				abySector[j++] = m_formatUnit[i];

				if (j == TFileSystem.SECTOR_LENGTH) {
					ImageSector is = new ImageSector(startSector + number, abySector);
					is.setPosition(pos);
					// System.out.println("Found sector " + is.getNumber());
					// System.out.println(Utilities.hexdump(is.getData()));
					m_decodedSectors.add(is);
					j = 0;
					number++;
				}
			}
		}
		
		void encode() {
		}				
		
		void prepareNewFormatUnit(int funum, FormatUnitParameters t) {
		}
	}
	
	public CF7ImageFormat(String sImageName) throws IOException, ImageException {
		super(sImageName);
		m_codec = new CF7Codec();
		setupPartitionTable();
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
	
	/** Find partitions. Each partition is exactly 400 KiB long (1600 sectors).
	    We only consider those partitions that have a DSK signature.
	*/
	public void setupPartitionTable() {
		List<Partition> plist = new LinkedList<Partition>();

		setFormatUnitLength(20 * 2 * TFileSystem.SECTOR_LENGTH);

		int maxsect = (int)(m_nLength / TFileSystem.SECTOR_LENGTH);
		
		int partsect = 0;
		int lensect = 1600;
		int nFail = 3;
		int nNoName = 1;
		
		while (partsect < maxsect-1600) {
			// System.out.print("Check sector " + partsect + ": ");
			try {
				Sector vib = readSector(partsect);
				byte[] abyVib = vib.getData();
				String sName = Utilities.getString10(abyVib, 0);
				boolean bValid = false;
				
				// Check whether there is a name or a DSK signature
				// If not, skip that partition
				if (sName.trim().length()==0) sName = String.format("NONAME%04d", nNoName++);
				if ((abyVib[13] == 'D') && (abyVib[13] == 'S') && (abyVib[13] == 'K')) {
					bValid = true;
				}
				else {
					if (!sName.contains("unknown") && !sName.contains(".")) {
						bValid = true;
					}
				}
				if (bValid) {
					// System.out.println("found partition " + sName);
					plist.add(new Partition(partsect/1600, partsect, lensect, sName));
				}
				else {
					// System.out.println("no");
					if (nFail==0) break;
					nFail--;
				}
			}
			catch (ImageException ix) {
				ix.printStackTrace();
			}
			catch (IOException iox) {
				iox.printStackTrace();
			}

			partsect += 1600;
		}
		
		m_partition = plist.toArray(new Partition[0]);
		// System.out.println("Partition number = " + m_partition.length);		
	}
	
	protected int getPartitionSectorOffset() {
		if (m_nActivePartition == -1) return 0;
		return m_nActivePartition*1600;
	}
	
	public int getActivePartition() {
		return -1;
	}
	
	static int getImageTypeStatic() {
		return CF7;
	}
	
	FormatUnitParameters getFormatUnitParameters() {
		byte[] fill = { 0x00 };
		return new FormatUnitParameters(20, fill);
	}
	
	/** Prepare an empty image. The CF7Format has no additional data
		outside of its format units. */
    @Override
	void prepareNewImage(FormatParameters params) {
		System.out.println("CF7: prepareNewImage");
	}
	
	int getFormatUnitLength(int funum) {
		return 20 * TFileSystem.SECTOR_LENGTH * 2;
	}
	
	/** Format units are tracks in this format. */
	long getFormatUnitPosition(int funum) {
		return funum * getFormatUnitLength(funum);
	}	
	
	int getFUNumberFromSector(int secnum) {
		secnum += getPartitionSectorOffset();
	
		if (secnum == 0) return 0;
		int count = getFormatUnitLength(secnum) / (TFileSystem.SECTOR_LENGTH*2);
		return secnum / count;
	}
	
	@Override
	int getImageType() {
		return getImageTypeStatic();
	}
	
	@Override
	public boolean isPartitioned() {
		return true;
	}
	
	public CF7VolumeFileSystem getFileSystem(byte[] sector0) {
		CF7VolumeFileSystem fs = new CF7VolumeFileSystem();
		fs.setImage(this);
		// System.out.println(Utilities.hexdump(sector0));
		fs.configure(sector0);
		return fs;
	}
}

/*
00000000: 5600 4f00 4c00 3100 2000 2000 2000 2000  V.O.L.1. . . . .
00000010: 2000 2000 0600 4000 1400 4400 5300 4b00   . ...@...D.S.K.
00000020: 2000 2800 0200 0200 0000 0000 0000 0000   .(.............
00000030: 0000 0000 0000 0000 0000 0000 0000 0000  ................
00000040: 0000 0000 0000 0000 0000 0000 0000 0000  ................

repeated every 0x64000

00-09  Name
0a-0b  1600
0c     20
0d-0f  "DSK"
10     " " / "P"
11     40
12     2
13     2
14-    00

*/

