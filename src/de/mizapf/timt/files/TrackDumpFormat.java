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
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.NotImplementedException;

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
	
	int[] gapsd = { 16, 6, 11, 6, 45, 231 };  
	int[] gapdd = { 40, 10, 22, 12, 24, 712 };
	
	TrackDumpFormat(RandomAccessFile filesystem, String sImageName) throws IOException, ImageException {
		super(filesystem, sImageName);
		writeThrough(false);
	}
	
	TrackDumpFormat() {
		writeThrough(false);
	}

	TrackDumpFormat(RandomAccessFile filesystem, String sImageName, TFileSystem fs) throws IOException, ImageException {
		super(filesystem, sImageName, fs);
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		ffs.setCountedSectors(NONE);
		m_currentCylinder = NONE;
		m_currentTrack = NONE;
		m_currentHead = NONE;
		m_positionInBuffer = 0;
		writeThrough(false);
	}
		
	/** Write a header. Nothing to do here. */
	@Override
	void prepareImageFile() {
	}
	
	@Override
	int getFormatType() {
		return FLOPPY_FORMAT; 
	}

	@Override
	int getImageType() {
		return TRACKDUMP; 
	}

	@Override	
	void setGeometryAndCodec(boolean bSpecial) throws IOException, ImageException {
		m_ImageFile.seek(0);
		byte[] bheader = new byte[60];
		m_ImageFile.readFully(bheader);
		
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
		if (tracklen==0) throw new ImageException(TIImageTool.langstr("ImageUnknown"));
		
		m_nCylinders = (int)((m_ImageFile.length()/tracklen)/m_nHeads);
		 
		if (m_nCylinders < 30) {
			// Warn that this is not compatible
			System.err.println(TIImageTool.langstr("TrackDump1Sided"));
			m_nHeads = 1;
			m_nCylinders = (int)(m_ImageFile.length()/tracklen);
		}
		
		m_nDensity = (m_encoding == FM)? FloppyFileSystem.SINGLE_DENSITY : FloppyFileSystem.DOUBLE_DENSITY;
		
		setupBuffers("unnamed", false);
	}

	/** Newly created. */
	@Override	
	void setGeometryAndCodec(String sImageName, TFileSystem fs, boolean bInitial) {
		setBasicParams((FloppyFileSystem)fs);	
		setupBuffers(sImageName, bInitial);
	}

	@Override
	void setupBuffers(String sImageName, boolean bInitial) {
		int pos = 0;
		int tracklen = (m_encoding==FM)? 3253 : 6872;
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		
		if (m_nHeads == 1) {
			// Warn that this is not compatible
			System.err.println(TIImageTool.langstr("TrackDump1Sided"));
		}
		
		m_bufferpos = new int[m_nCylinders*2];
		m_bufferlen1 = new int[m_nCylinders*2];

		for (int j=0; j < m_nCylinders; j++) {
			m_bufferpos[j] = pos;
			m_bufferlen1[j] = tracklen;
			pos += tracklen;
		}
		for (int j=2*m_nCylinders-1; j >= m_nCylinders; j--) {
			m_bufferpos[j] = pos;
			m_bufferlen1[j] = tracklen;
			pos += tracklen;
		}
		ffs.setCountedSectors(NONE);
		m_currentCylinder = NONE;
		m_currentTrack = NONE;
		m_currentHead = NONE;
		m_positionInBuffer = 0;	
	}
	
	public String getDumpFormatName() {
		return TIImageTool.langstr("TrackDump");
	}
	
	/** We return the cached sector.
	*/
	@Override
	public Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		if (nSectorNumber > 10000) throw new ImageException(String.format(TIImageTool.langstr("BadSectorNumber"), nSectorNumber)); 
		int secindex = locateInBuffer(nSectorNumber);
		if (secindex != NONE) {
			// System.out.println("sector " + nSectorNumber);
			// System.out.println(m_buffsector[secindex]);
			return m_buffsector[secindex];
		}
		else throw new ImageException(String.format(TIImageTool.langstr("SectorNotFound"), nSectorNumber));
	}

	/** Create a new track in m_abyBuffer when the image has not yet been written. 
		Current cylinder and head are stored in the member variables.
	*/
	@Override
	void createBuffer(int cylinder, int head, int track) {
		System.out.println("Create new track " + track);
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		m_abyBuffer = new byte[getFullTrackLength(m_bufferlen1[track])];
//		int cylinder, int head, int seccount, int density, int[] gap
		formatTrack(cylinder, head, ffs.getSectorsPerTrack(), ffs.getDensity(), (m_encoding==FM)? gapsd : gapdd );
	}
	

	/** Scan a track. 
	    @param nSectorNumber Sector that is about to be read.
	    @return Index of the sector in the sector cache (or NONE)
	*/
	@Override
	int loadBuffer(Location loc) throws IOException, ImageException {
		
		ArrayList<Sector> sectors = new ArrayList<Sector>();

		int count=0;
		byte[] bSector = null;
		byte[] bHeader = new byte[4];
		int secindex = -1;
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		
		while (m_positionInBuffer < m_bufferlen1[loc.track]) {
			if (nextIDAMFound()) {
				// For MFM we have an ident byte to consume
				bHeader[0] = (byte)readBits(8);
				bHeader[1] = (byte)readBits(8);
				bHeader[2] = (byte)readBits(8);
				bHeader[3] = (byte)readBits(8);
				
				if ((bHeader[2]&0xff) == loc.sector) secindex = count;
				int crch = readBits(16);
				if (crch != 0xf7f7) System.out.println(String.format(TIImageTool.langstr("BadHeaderCRC"), bHeader[0], bHeader[1], bHeader[2], Utilities.toHex(0xf7f7, 4), Utilities.toHex(crch, 4)));
									
				if (nextDAMFound()) {
					int mark = m_value;  // the DAM for FM and MFM				
					int pos = m_positionInBuffer;
					bSector = new byte[256];
					readBits(bSector);
					int crcd = readBits(16);
					Sector sect = new Sector(loc.track*ffs.getCountedSectors() + bHeader[2], bSector);
					sect.setTrackPosition(pos, 0xffff, mark); 
					sectors.add(sect);
					//System.out.println("Sector " + sect.getNumber()  + ": Data CRC = " + Utilities.toHex(sect.getCrc(),4) + " (expected " +  Utilities.toHex(crcd, 4) + ")");
					if (crcd != 0xf7f7) System.out.println(String.format(TIImageTool.langstr("BadDataCRC"), sect.getNumber(), Utilities.toHex(0xf7f7,4), Utilities.toHex(crcd, 4)));
					// System.out.println("loaded sector " + sect.getNumber() + " at track " + loc.track);
					// System.out.println(sect);
					count++;
				}
			}	
		}
//		System.out.println("Found " + count + " sectors");
		m_buffsector = sectors.toArray(new Sector[count]);
		ffs.setCountedSectors(count);	
		
		if (count == 0) throw new ImageException(TIImageTool.langstr("NoSectorsFound"));
		
		// Now we know the last sector	
		if (m_nTotalSectors == 0) m_nTotalSectors = ffs.getCountedSectors() * m_nCylinders * m_nHeads;

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

		while ((((m_positionInBuffer - idampos) % period) != 0) && m_positionInBuffer < m_bufferlen1[m_currentTrack]) {
			m_positionInBuffer++;
		}
		
		boolean found = false;
		if (m_positionInBuffer < m_bufferlen1[m_currentTrack] && m_abyBuffer[m_positionInBuffer]==(byte)0xfe) found = true;
		
		// if (found) System.out.println("IDAM found at " + m_positionInBuffer);
		m_positionInBuffer++;
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

		while ((((m_positionInBuffer - dampos) % period) != 0) && m_positionInBuffer < m_bufferlen1[m_currentTrack]) {
			m_positionInBuffer++;
		}

		m_value = 0;
		boolean found = false;

		if (m_positionInBuffer < m_bufferlen1[m_currentTrack]) {
			m_value = m_abyBuffer[m_positionInBuffer++] & 0xff;
			if (m_value==0xfb || m_value == 0xf8) found = true;
		}
		
		// if (found) System.out.println("DAM found at " + m_positionInBuffer);
		return found;
	}
	
	/** Get the number of bits from the cell level sequence. 
	*/
	private int readBits(int number) {
		int value = 0;
		if (m_positionInBuffer < m_bufferlen1[m_currentTrack]) {
			value = (m_abyBuffer[m_positionInBuffer++] & 0xff);
		}
			
		if (number == 16) {
			if (m_positionInBuffer < m_bufferlen1[m_currentTrack]) {
				value = (value << 8) | (m_abyBuffer[m_positionInBuffer++] & 0xff);
			}
		}
		return value;
	}
	
	private void readBits(byte[] buffer) {
		int actnumber = buffer.length;
		if (m_positionInBuffer + actnumber > m_abyBuffer.length) 
			actnumber = m_abyBuffer.length - m_positionInBuffer;
		System.arraycopy(m_abyBuffer, m_positionInBuffer, buffer, 0, actnumber);
		m_positionInBuffer += actnumber;
	}
	
	/** Write the number of bits into the track.
	*/
	private void writeBits(int value, int number) {
		int val1 = value;
		if (number==16) {
			val1 = (value >> 8);
		}

		if (m_positionInBuffer < m_bufferlen1[m_currentTrack]) {
			m_abyBuffer[m_positionInBuffer++] = (byte)(val1 & 0xff);
		}
		
		if (number==16) {
			if (m_positionInBuffer < m_bufferlen1[m_currentTrack]) {
				m_abyBuffer[m_positionInBuffer++] = (byte)(value & 0xff);
			}
		}
	}
	
	private void writeBits(byte[] bytes, int number) {
		int actnumber = number/8;
		if (m_positionInBuffer + actnumber > m_abyBuffer.length) 
			actnumber = m_abyBuffer.length - m_positionInBuffer;
		
		System.arraycopy(bytes, 0, m_abyBuffer, m_positionInBuffer, actnumber);
		m_positionInBuffer += actnumber;
	}
	
	public void flush() throws IOException {
		boolean trackchanged = false;
		if (m_currentTrack == NONE) return;
		for (int i=0; i < m_buffsector.length; i++) {
			if (m_buffsector[i].changed()) {
				trackchanged = true;
				byte[] bSectorContent = m_buffsector[i].getBytes();
				m_positionInBuffer = m_buffsector[i].getPosition();
				writeBits(bSectorContent, Volume.SECTOR_LENGTH * 8);
				writeBits(0xf7f7, 16);
			}
		}
		if (trackchanged) {
			// Write back the whole track (both sides; leave something to optimize)
			m_ImageFile = new RandomAccessFile(m_sImageName, "rw");		
			// System.out.println("writing track " + m_currentTrack + " at position " + m_bufferpos[m_currentTrack]);
			m_ImageFile.seek(m_bufferpos[m_currentTrack]);
			m_ImageFile.write(m_abyBuffer);
			m_ImageFile = new RandomAccessFile(m_sImageName, "r");		
		}
	}

	@Override
	void writeToBuffer(Sector sect) throws IOException, ImageException {
		writeSector(sect);
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
		m_positionInBuffer = 0;
		
		// Start number
		int sector = 0;
		
		byte[] bHeader = new byte[4];
		
		if (density == FloppyFileSystem.SINGLE_DENSITY) {
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

			if (density != FloppyFileSystem.SINGLE_DENSITY) {			
				writeBits(0xa1,8);
				writeBits(0xa1,8);
				writeBits(0xa1,8);
			}
			writeBits(0xfe,8);

			bHeader[0] = (byte)cylinder;
			bHeader[1] = (byte)head;
			bHeader[2] = (byte)sector;
			bHeader[3] = (byte)0x01;					
			for (byte b : bHeader) writeBits(b, 8);
			writeBits(0xf7f7, 16);			
			
			// Gap2
			for (int k=0; k < gap[2]; k++) writeBits(gapval1,8);
			// Sync
			for (int k=0; k < gap[3]; k++) writeBits(0x00,8);
			
			// DAM
			if (density != FloppyFileSystem.SINGLE_DENSITY) {
				writeBits(0xa1,8);
				writeBits(0xa1,8);
				writeBits(0xa1,8);
			}
			writeBits(0xfb, 8);

			// Sector content (Empty)
			writeBits(m_abyEmpty, 256*8);
			
			// CRC
			writeBits(0xf7f7, 16);
			
			// GAP3
			for (int k=0; k < gap[4]; k++) writeBits(gapval1,8);

			// Next sector
			if (density == FloppyFileSystem.SINGLE_DENSITY)
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
		
		System.out.println("FIXME: Legacy function createEmptyImage");
		
		if (density != FloppyFileSystem.SINGLE_DENSITY && density != FloppyFileSystem.DOUBLE_DENSITY) 
			throw new ImageException(String.format(TIImageTool.langstr("TrackDumpInvalidDensity"), density));
		
		if (sides != 2)
			throw new ImageException(TIImageTool.langstr("TrackDump1Sided"));
		
		int tracklen = (density == FloppyFileSystem.SINGLE_DENSITY)? 3253 : 6872;
		int pos = 0;

		m_bufferpos = new int[cylinders*2];
		m_bufferlen1 = new int[cylinders*2];
		
		for (int j=0; j < cylinders; j++) {
			m_bufferpos[j] = pos;
			m_bufferlen1[j] = tracklen;
			pos += tracklen;
		}
		for (int j=2*cylinders-1; j >= cylinders; j--) {
			m_bufferpos[j] = pos;
			m_bufferlen1[j] = tracklen;
			pos += tracklen;
		}

		m_abyBuffer = new byte[tracklen];

		// Allocate bytes in memory. We will write the array to the file at the end.
		// Note that PC99 format always allocates space for two heads
		byte[] image = new byte[m_abyBuffer.length * cylinders * 2];

		if (format) {
			for (int cyl = 0; cyl < cylinders; cyl++) {
			
				for (int head=0; head < 2; head++) {
					m_positionInBuffer = 0;
					formatTrack(cyl, head, sectors, density, (density == FloppyFileSystem.SINGLE_DENSITY)? gapsd : gapdd);
				
					// Copy the track into the image
					int track = (head==0)? cyl : 2*cylinders-1-cyl; 
					System.arraycopy(m_abyBuffer, 0, image, m_bufferpos[track], m_bufferlen1[track]);
				}
			}
		}
		
		// Write the resulting image
		FileOutputStream fos = new FileOutputStream(newfile);
		fos.write(image, 0, image.length);
		fos.close();
	}

	/*
		Check the CRC in the TDF. Actually, only F7F7 is accepted by the format.
	*/
	@Override
	int checkCRC(boolean fix, boolean reset) throws IOException {	
		
		int count=0;
	
		if (fix) {
			m_ImageFile.close();
			m_ImageFile = new RandomAccessFile(m_sImageName, "rw");
			// Flush the last track
			m_ImageFile.seek(m_bufferpos[m_currentTrack]);
			m_ImageFile.write(m_abyBuffer);
		}
		
		for (int cylinder=0; cylinder < m_nCylinders; cylinder++) {
			for (int head=0; head<2; head++) {

				int track = (head==0)? cylinder : (2*m_nCylinders-1 - cylinder);	
				m_currentTrack = track;
				m_currentCylinder = cylinder;
				m_currentHead = head;

				m_abyBuffer = new byte[m_bufferlen1[track]];
		
				m_ImageFile.seek(m_bufferpos[track]);
				m_ImageFile.readFully(m_abyBuffer);
				
				// Reset to start
				m_positionInBuffer = 0;
		
				byte[] bSector = null;
				byte[] bHeader = new byte[4];
				boolean changed = false;
				
				while (m_positionInBuffer < m_bufferlen1[track]) {
					if (nextIDAMFound()) {
						// For MFM we have an ident byte to consume						
						bHeader[0] = (byte)readBits(8);
						bHeader[1] = (byte)readBits(8);
						bHeader[2] = (byte)readBits(8);
						bHeader[3] = (byte)readBits(8);
						
						int position = m_positionInBuffer;
						int crch = readBits(16);
						
						if (crch != 0xf7f7) {
							if (fix) {
								m_positionInBuffer = position;
								writeBits(0xf7f7, 16);
								changed = true;
							}
							else {
								System.out.println(String.format(TIImageTool.langstr("BadHeaderCRC"), bHeader[0], bHeader[1], bHeader[2], Utilities.toHex(0xf7f7, 4), Utilities.toHex(crch, 4)));
							}
							count++;
						}
						
						if (nextDAMFound()) {
							int mark = m_value;  // the DAM for FM and MFM
							int pos = m_positionInBuffer;
							bSector = new byte[256];
							readBits(bSector);
							position = m_positionInBuffer;
							int crcd = readBits(16);
							
							if (crcd != 0xf7f7) {
								if (fix) {
									m_positionInBuffer = position;
									writeBits(0xf7f7, 16);
									changed = true;
								}
								else {
									System.out.println(String.format(TIImageTool.langstr("BadDataCRC1"), bHeader[0], bHeader[1], bHeader[2], Utilities.toHex(0xf7f7, 4), Utilities.toHex(crcd, 4)));
								}
								count++;
							}
						}
					}
				}
				// System.out.println("writing track " + m_currentTrack + " at position " + m_bufferpos[m_currentTrack]);
				if (changed) {
					m_ImageFile.seek(m_bufferpos[m_currentTrack]);
					m_ImageFile.write(m_abyBuffer);
				}
			}
		}
		if (fix) {
			m_ImageFile.close();
			m_ImageFile = new RandomAccessFile(m_sImageName, "r");
		}
		return count;
	}	
		
	@Override
	int getBufferIndex(Location loc) {
		throw new NotImplementedException("getBufferIndex");
	}
}
