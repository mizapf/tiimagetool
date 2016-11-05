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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.EOFException;
import java.util.ArrayList;
import java.io.File;

import de.mizapf.timt.util.Utilities;

class TrackDumpFormat extends ImageFormat {

	int m_value;

	static final int[][] tdfgeometry = { { 130120, 1, 40, 9, 1 }, { 260240, 2, 40, 9, 1 },
		{ 274880, 1, 40, 18, 2 }, { 549760, 2, 40, 18, 2 }, 
		{ 1039360, 2, 40, 36, 3 }, { 1099520, 2, 80, 18, 2 },
		{ 2078720, 2, 80, 36, 3 } };

	static int vote(RandomAccessFile fileSystem) throws IOException {
		
		// File system size must be less than 3 MB
		if (fileSystem.length()==0 || fileSystem.length() > 3000000) return 0;
		
		for (int i=0; i < tdfgeometry.length; i++) {
			if (fileSystem.length() == tdfgeometry[i][0]) {
				return 100;
			}
		}
		return 0;		
	}
	
	TrackDumpFormat(RandomAccessFile filesystem, String sImageName) throws IOException, ImageException {
		super(filesystem, sImageName);
	}
	
	TrackDumpFormat() {
	}

	@Override	
	void setGeometry(boolean bSpecial) throws IOException, ImageException {
		m_FileSystem.seek(0);
		byte[] bheader = new byte[60];
		m_FileSystem.readFully(bheader);
		
		// Determine the positions and lengths of the tracks (see HFEFormat)
		
		int tracklen = 0;
		m_encoding = FM;		
		int i=16;
		while (i<23) {
			if (bheader[i++] != 0) break;
		}
		i--;
		if (i==22 && bheader[i] == (byte)0xfe) {
			tracklen = 3253;
		}
		
		i=40;
		while (i<51) {
			if (bheader[i++] != 0) break;
		}
		i--;
		if (i==50 && bheader[i] == (byte)0xa1) {
			m_encoding = MFM;
			tracklen = 6872;
		}
		
		m_nHeads = 2;
		if (tracklen==0) throw new ImageException("Unknown or corrupted format");
		
		int cylinders = (int)((m_FileSystem.length()/tracklen)/2);
		if (cylinders < 30) {
			System.err.println("One-sided format; not complying to standard");
			m_nHeads = 1;
			cylinders = cylinders*2;
		}
		
		m_trackpos = new int[cylinders*2];
		m_tracklen = new int[cylinders*2];
		
		int pos = 0;
		
		for (int j=0; j < cylinders; j++) {
			m_trackpos[j] = pos;
			m_tracklen[j] = tracklen;
			pos += tracklen;
		}
		for (int j=2*cylinders-1; j >= cylinders; j--) {
			m_trackpos[j] = pos;
			m_tracklen[j] = tracklen;
			pos += tracklen;
		}
		
		m_nCylinders = cylinders;
		m_nSectorsByFormat = NONE;
		m_currentCylinder = NONE;
		m_currentTrack = NONE;
		m_currentHead = NONE;
		m_positionInTrack = 0;	
	}

	public String getDumpFormatName() {
		return "track";
	}
	
	/** We return the cached sector.
	*/
	@Override
	public Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		if (nSectorNumber > 10000) throw new ImageException("Bad sector number: " + nSectorNumber); 
		int secindex = readTrack(nSectorNumber);
		if (secindex != NONE) {
			// System.out.println("sector " + nSectorNumber);
			// System.out.println(m_sector[secindex]);
			return m_sector[secindex];
		}
		else throw new ImageException("Sector " + nSectorNumber + " not found");
	}

	@Override
	public void writeSector(int nSectorNumber, byte[] abySector) throws IOException, ImageException {
		int secindex = readTrack(nSectorNumber);
		if (secindex == NONE) throw new ImageException("Sector " + nSectorNumber + " not found");
		// Write the new data
		// Don't forget to clone the bytes!
		byte[] bNewcontent = new byte[256];
		System.arraycopy(abySector, 0, bNewcontent, 0, 256);
		m_sector[secindex].setData(bNewcontent);
		// System.out.println("Writing sector " + nSectorNumber);
		// System.out.println(m_sector[secindex]);
		// System.out.println("CRC = " + Utilities.toHex(m_sector[secindex].getCrc(),4));
	}
	
	/** Scan a track. 
	    @param nSectorNumber Sector that is about to be read.
	    @return Index of the sector in the sector cache (or NONE)
	*/
	@Override
	int readTrack(int nSectorNumber) throws IOException, ImageException {
		int secindex = -1;
		
		Location loc = null;
		if (nSectorNumber != 0) {
			loc = getLocation(nSectorNumber);
		}
		else {
			loc = new Location(0, 0, 0, 0);
		}
		
		// Do we have that track already?
		if (m_currentCylinder == loc.cylinder && m_currentHead == loc.head) {
			for (int i=0; i < m_sector.length; i++) {
				if (m_sector[i].getNumber()==nSectorNumber) return i;
			}
			return NONE;
		}
		else {
			// Write back the last track
			flush();
		}
		
		m_currentTrack = loc.track;
		m_currentCylinder = loc.cylinder;
		m_currentHead = loc.head;

		m_abyTrack = new byte[m_tracklen[loc.track]];
		
		m_FileSystem.seek(m_trackpos[loc.track]);
		m_FileSystem.readFully(m_abyTrack);
		
		// Reset to start
		m_positionInTrack = 0;
		
		ArrayList<Sector> sectors = new ArrayList<Sector>();

		int count=0;
		byte[] bSector = null;
		byte[] bHeader = new byte[4];
		int initcrc = 0;
		
		while (m_positionInTrack < m_tracklen[loc.track]) {
			if (nextIDAMFound()) {
				// For MFM we have an ident byte to consume
				if (m_encoding==MFM) {
					initcrc = 0xb230;
				}
				else initcrc = 0xef21;

				bHeader[0] = (byte)readBits(8);
				bHeader[1] = (byte)readBits(8);
				bHeader[2] = (byte)readBits(8);
				bHeader[3] = (byte)readBits(8);
				
				if ((bHeader[2]&0xff) == loc.sector) secindex = count;
				int crch = readBits(16);
				int crcc = Utilities.crc16_get(bHeader, 0, 4, initcrc);
				if (crch != 0xf7f7 && crch != crcc) System.out.println("Bad header CRC at (" + bHeader[0] + "," + bHeader[1] + "," + bHeader[2]+ "): Expected " + Utilities.toHex(crcc, 4) + ", got " + Utilities.toHex(crch, 4));
				
				if (nextDAMFound()) {
					int mark = m_value;  // the DAM for FM and MFM
					if (m_encoding==MFM) {
						initcrc = (mark==0xfb)? 0xe295 : 0xd2f6;   // f8 is the "deleted" mark
					}
					else {
						initcrc = (mark==0xfb)? 0xbf84 : 0x8fe7;
					}
					
					int pos = m_positionInTrack;
					bSector = new byte[256];
					readBits(bSector);
					int crcd = readBits(16);
					Sector sect = new Sector(loc.track*m_nSectorsByFormat + bHeader[2], bSector, pos, initcrc, mark); 
					sectors.add(sect);
					//System.out.println("Sector " + sect.getNumber()  + ": Data CRC = " + Utilities.toHex(sect.getCrc(),4) + " (expected " +  Utilities.toHex(crcd, 4) + ")");
					if (crcd != 0xf7f7 && crcd != sect.getCrc()) System.out.println("Bad data CRC at sector " + sect.getNumber() + ": Expected " + Utilities.toHex(sect.getCrc(), 4) + ", got " + Utilities.toHex(crcd,4));
					// System.out.println("loaded sector " + sect.getNumber() + " at track " + loc.track);
					// System.out.println(sect);
					count++;
				}
			}	
		}
//		System.out.println("Found " + count + " sectors");
		m_sector = sectors.toArray(new Sector[count]);
		m_nSectorsByFormat = count;	
		
		if (count == 0) throw new ImageException("No sectors found on track");
		
		// Determine density
		if (m_nDensity==0) {
			if (m_encoding==FM) m_nDensity = 1;
			else {
				if (count <= 18) m_nDensity = 2;
				else if (count <= 36) m_nDensity = 3;
				else m_nDensity = 4;
			}
		}

		// Now we know the last sector	
		if (m_nTotalSectors == 0) m_nTotalSectors = m_nSectorsByFormat * m_nCylinders * m_nHeads;

		return secindex;
	}
	
	private boolean nextIDAMFound() {
	//	System.out.println("Searching next IDAM");
		int idampos = 22;
		int period = 334;
		
		if (m_encoding==MFM) {
			idampos = 53;
			period = 340;
		}

		while ((((m_positionInTrack - idampos) % period) != 0) && m_positionInTrack < m_tracklen[m_currentTrack]) {
			m_positionInTrack++;
		}
		
		boolean found = false;
		if (m_positionInTrack < m_tracklen[m_currentTrack] && m_abyTrack[m_positionInTrack]==(byte)0xfe) found = true;
		
		// if (found) System.out.println("IDAM found at " + m_positionInTrack);
		m_positionInTrack++;
		return found;
	}
	
	private boolean nextDAMFound() {
		// System.out.println("Searching next DAM");
		int dampos = 46;
		int period = 334;
		
		if (m_encoding==MFM) {
			dampos = 97;
			period = 340;
		}

		while ((((m_positionInTrack - dampos) % period) != 0) && m_positionInTrack < m_tracklen[m_currentTrack]) {
			m_positionInTrack++;
		}

		m_value = 0;
		boolean found = false;

		if (m_positionInTrack < m_tracklen[m_currentTrack]) {
			m_value = m_abyTrack[m_positionInTrack++] & 0xff;
			if (m_value==0xfb || m_value == 0xf8) found = true;
		}
		
		// if (found) System.out.println("DAM found at " + m_positionInTrack);
		return found;
	}
	
	/** Get the number of bits from the cell level sequence. 
	*/
	private int readBits(int number) {
		int value = 0;
		if (m_positionInTrack < m_tracklen[m_currentTrack]) {
			value = (m_abyTrack[m_positionInTrack++] & 0xff);
		}
			
		if (number == 16) {
			if (m_positionInTrack < m_tracklen[m_currentTrack]) {
				value = (value << 8) | (m_abyTrack[m_positionInTrack++] & 0xff);
			}
		}
		return value;
	}
	
	private void readBits(byte[] buffer) {
		int actnumber = buffer.length;
		if (m_positionInTrack + actnumber > m_abyTrack.length) 
			actnumber = m_abyTrack.length - m_positionInTrack;
		System.arraycopy(m_abyTrack, m_positionInTrack, buffer, 0, actnumber);
		m_positionInTrack += actnumber;
	}
	
	/** Write the number of bits into the track.
	*/
	private void writeBits(int value, int number) {
		int val1 = value;
		if (number==16) {
			val1 = (value >> 8);
		}

		if (m_positionInTrack < m_tracklen[m_currentTrack]) {
			m_abyTrack[m_positionInTrack++] = (byte)(val1 & 0xff);
		}
		
		if (number==16) {
			if (m_positionInTrack < m_tracklen[m_currentTrack]) {
				m_abyTrack[m_positionInTrack++] = (byte)(value & 0xff);
			}
		}
	}
	
	private void writeBits(byte[] bytes, int number) {
		int actnumber = number/8;
		if (m_positionInTrack + actnumber > m_abyTrack.length) 
			actnumber = m_abyTrack.length - m_positionInTrack;
		
		System.arraycopy(bytes, 0, m_abyTrack, m_positionInTrack, actnumber);
		m_positionInTrack += actnumber;
	}
	
	public void flush() throws IOException {
		boolean trackchanged = false;
		if (m_currentTrack == NONE) return;
		for (int i=0; i < m_sector.length; i++) {
			if (m_sector[i].changed()) {
				trackchanged = true;
				m_positionInTrack = m_sector[i].getPosition();
				writeBits(m_sector[i].getBytes(), Volume.SECTOR_LENGTH * 8);
				writeBits(m_sector[i].getCrc(), 16);
			}
		}
		if (trackchanged) {
			// Write back the whole track (both sides; leave something to optimize)
			m_FileSystem = new RandomAccessFile(m_sImageName, "rw");		
			// System.out.println("writing track " + m_currentTrack + " at position " + m_trackpos[m_currentTrack]);
			m_FileSystem.seek(m_trackpos[m_currentTrack]);
			m_FileSystem.write(m_abyTrack);
			m_FileSystem = new RandomAccessFile(m_sImageName, "r");		
		}
	}

	@Override
	public void reopenForWrite() throws IOException {
		// Don't do anything here
	}
	
	@Override
	public void reopenForRead() throws IOException {
		// Don't do anything here
	}	
	// ===========================================================
	// Formatting
	// ===========================================================
	@Override
	void formatTrack(int cylinder, int head, int seccount, int density, int[] gap) {
		int gapval0 = 0x4e;
		int gapval1 = 0x4e;
		m_positionInTrack = 0;
		
		// Start number
		int sector = 0;
		int initcrc = 0;
		
		byte[] bHeader = new byte[4];
		
		if (density==SINGLE_DENSITY) {
			gapval0 = 0x00;
			gapval1 = 0xff;
			sector = (cylinder * 6) % 9;
		}

		// GAP 0
		// System.out.println("Write GAP 0 for cylinder " + cylinder + ", head " + head);
		for (int i=0; i < gap[0]; i++) writeBits(gapval0,8);
		
		// Write all sectors
		for (int i=0; i < seccount; i++) {
			// System.out.println("Sector " + i);
			// Write sector		
			// Sync gap
			for (int k=0; k < gap[1]; k++) writeBits(0x00,8);

			initcrc = 0xef21;
			if (density != SINGLE_DENSITY) {			
				writeBits(0xa1,8);
				writeBits(0xa1,8);
				writeBits(0xa1,8);
				initcrc = 0xb230;
			}
			writeBits(0xfe,8);

			bHeader[0] = (byte)cylinder;
			bHeader[1] = (byte)head;
			bHeader[2] = (byte)sector;
			bHeader[3] = (byte)0x01;					
			for (byte b : bHeader) writeBits(b, 8);
			int crcc = Utilities.crc16_get(bHeader, 0, 4, initcrc);
			writeBits(crcc, 16);			
			
			// Gap2
			for (int k=0; k < gap[2]; k++) writeBits(gapval1,8);
			// Sync
			for (int k=0; k < gap[3]; k++) writeBits(0x00,8);
			
			// DAM
			if (density!=SINGLE_DENSITY) {
				writeBits(0xa1,8);
				writeBits(0xa1,8);
				writeBits(0xa1,8);
			}
			writeBits(0xfb, 8);

			// Sector content
			for (int k=0; k < 256; k++) writeBits(0xe5,8);
			
			// CRC
			writeBits((density==SINGLE_DENSITY)? 0xa40c : 0x7827,16);
			
			// GAP3
			for (int k=0; k < gap[4]; k++) writeBits(gapval1,8);

			// Next sector
			if (density==SINGLE_DENSITY)
				sector = (sector + 7) % 9;
			else
				sector = (sector + 11) % 18;
		}	
		// GAP4
		// System.out.println("Write GAP 4");
		for (int k=0; k < gap[5]; k++) {
			// System.out.println("k = " + k); 
			writeBits(gapval1,8);
		}
	}
	
	public void createEmptyImage(File newfile, int sides, int density, int cylinders, int sectors, boolean format) throws ImageException, FileNotFoundException, IOException {
		
		if (density != SINGLE_DENSITY && density != DOUBLE_DENSITY) 
			throw new ImageException("Density not supported by this floppy image format");
		
		if (sides != 2)
			throw new ImageException("TDF (PC99) images are double-sided");
		
		int tracklen = (density==SINGLE_DENSITY)? 3253 : 6872;
		int pos = 0;

		m_trackpos = new int[cylinders*2];
		m_tracklen = new int[cylinders*2];
		
		for (int j=0; j < cylinders; j++) {
			m_trackpos[j] = pos;
			m_tracklen[j] = tracklen;
			pos += tracklen;
		}
		for (int j=2*cylinders-1; j >= cylinders; j--) {
			m_trackpos[j] = pos;
			m_tracklen[j] = tracklen;
			pos += tracklen;
		}
		
		int[] gapsd = { 16, 6, 11, 6, 45, 231 };  
		int[] gapdd = { 40, 10, 22, 12, 24, 712 };
		
		m_abyTrack = new byte[tracklen];

		// Allocate bytes in memory. We will write the array to the file at the end.
		// Note that PC99 format always allocates space for two heads
		byte[] image = new byte[m_abyTrack.length * cylinders * 2];

		if (format) {
			for (int cyl = 0; cyl < cylinders; cyl++) {
			
				for (int head=0; head < 2; head++) {
					m_positionInTrack = 0;
					formatTrack(cyl, head, sectors, density, (density==SINGLE_DENSITY)? gapsd : gapdd);
				
					// Copy the track into the image
					int track = (head==0)? cyl : 2*cylinders-1-cyl; 
					System.arraycopy(m_abyTrack, 0, image, m_trackpos[track], m_tracklen[track]);
				}
			}
		}
		
		// Write the resulting image
		FileOutputStream fos = new FileOutputStream(newfile);
		fos.write(image, 0, image.length);
		fos.close();
	}

	@Override
	int checkCRC(boolean fix, boolean reset) throws IOException {	
		
		int count=0;
	
		if (fix) {
			m_FileSystem.close();
			m_FileSystem = new RandomAccessFile(m_sImageName, "rw");
			// Flush the last track
			m_FileSystem.seek(m_trackpos[m_currentTrack]);
			m_FileSystem.write(m_abyTrack);
		}
		
		for (int cylinder=0; cylinder < m_nCylinders; cylinder++) {
			for (int head=0; head<2; head++) {

				int track = (head==0)? cylinder : (2*m_nCylinders-1 - cylinder);	
				m_currentTrack = track;
				m_currentCylinder = cylinder;
				m_currentHead = head;

				m_abyTrack = new byte[m_tracklen[track]];
		
				m_FileSystem.seek(m_trackpos[track]);
				m_FileSystem.readFully(m_abyTrack);
				
				// Reset to start
				m_positionInTrack = 0;
		
				byte[] bSector = null;
				byte[] bHeader = new byte[4];
				int initcrc = 0;
				int actualcrc = 0; 
				boolean changed = false;
				
				while (m_positionInTrack < m_tracklen[track]) {
					if (nextIDAMFound()) {
						// For MFM we have an ident byte to consume
						if (m_encoding==MFM) {
							initcrc = 0xb230;
						}
						else initcrc = 0xef21;
						
						bHeader[0] = (byte)readBits(8);
						bHeader[1] = (byte)readBits(8);
						bHeader[2] = (byte)readBits(8);
						bHeader[3] = (byte)readBits(8);
						
						int position = m_positionInTrack;
						int crch = readBits(16);
						
						actualcrc = Utilities.crc16_get(bHeader, 0, 4, initcrc);
						if (crch != 0xf7f7 && crch != actualcrc) {
							if (fix) {
								m_positionInTrack = position;
								writeBits(reset? 0xf7f7 : actualcrc, 16);
								changed = true;
							}
							else {
								System.out.println("Bad header CRC at (" + bHeader[0] + "," + bHeader[1] + "," + bHeader[2]+ "): Expected " + Utilities.toHex(actualcrc, 4) + ", got " + Utilities.toHex(crch, 4));
							}
							count++;
						}
						
						if (nextDAMFound()) {
							int mark = m_value;  // the DAM for FM and MFM
							if (m_encoding==MFM) {
								initcrc = (mark==0xfb)? 0xe295 : 0xd2f6;   // f8 is the "deleted" mark
							}
							else {
								initcrc = (mark==0xfb)? 0xbf84 : 0x8fe7;
							}
							
							int pos = m_positionInTrack;
							bSector = new byte[256];
							readBits(bSector);
							position = m_positionInTrack;
							int crcd = readBits(16);
							actualcrc = Utilities.crc16_get(bSector, 0, bSector.length, initcrc);
							
							if (crcd != 0xf7f7 && crcd != actualcrc) {
								if (fix) {
									m_positionInTrack = position;
									writeBits(reset? 0xf7f7 : actualcrc, 16);
									changed = true;
								}
								else {
									System.out.println("Bad data CRC at (" + bHeader[0] + "," + bHeader[1] + "," + bHeader[2]+ "): Expected " + Utilities.toHex(actualcrc, 4) + ", got " + Utilities.toHex(crcd, 4));
								}
								count++;
							}
						}
					}
				}
				// System.out.println("writing track " + m_currentTrack + " at position " + m_trackpos[m_currentTrack]);
				if (changed) {
					m_FileSystem.seek(m_trackpos[m_currentTrack]);
					m_FileSystem.write(m_abyTrack);
				}
			}
		}
		if (fix) {
			m_FileSystem.close();
			m_FileSystem = new RandomAccessFile(m_sImageName, "r");
		}
		return count;
	}	
}
