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

package de.mizapf.timt.files;

import java.io.RandomAccessFile;
import java.io.EOFException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

import de.mizapf.timt.TIImageTool;

class RawHDFormat extends ImageFormat {

	private final static int BLOCKSIZE = 512;
	private int m_nTotalSectors;
	
	static int vote(RandomAccessFile fileSystem) throws IOException, EOFException {
		
		// File system size must be bigger than 3 MB
		if (fileSystem.length()==0 || fileSystem.length() < 3000000) return 0;
		
		byte[] chd = "MComprHD".getBytes();
		boolean isChd = true;
			
		// Read start of file
		byte[] abyStart = new byte[16];
		fileSystem.seek(0);
		fileSystem.readFully(abyStart);
		
		for (int i=0; i < chd.length; i++) {
			if (chd[i]!=abyStart[i]) isChd = false;
		}
		if (isChd) return 0;

		if ((fileSystem.length() % Volume.SECTOR_LENGTH) != 0) return 10;
		
		return 100;		
	}

	public String getDumpFormatName() {
		return TIImageTool.langstr("RAWType");
	}

	@Override
	int getFormatType() {
		return HD_FORMAT; 
	}
	
	RawHDFormat(RandomAccessFile filesystem, String sImageName) throws IOException, ImageException {
		super(filesystem, sImageName);
		m_nDensity = 0;
		writeThrough(true);
	}
	
	@Override
	int locateInBuffer(int nSectorNumber) throws IOException {
		// TODO: NPX for SCSI
		int nTrackNumber = (nSectorNumber / m_nSectorsPerTrack);			
		int nTrackOffset = nTrackNumber * m_nTrackLength; 

		if (nTrackNumber != m_nCurrentTrack) {
			m_ImageFile.seek(nTrackOffset);
			m_ImageFile.readFully(m_abyBuffer);
			m_nCurrentTrack = nTrackNumber;
		}
		return nTrackOffset;
	}	

	void getOffset(int nSectorNumber, int[] offset) throws IOException, ImageException {
		int nTrackOffset = 0;
		int nTrack = 0;
		
		if (nSectorNumber >= m_nTotalSectors) throw new EOFException(String.format(TIImageTool.langstr("ImageBeyond"), nSectorNumber));
		
		offset[TRACK] = locateInBuffer(nSectorNumber);

		nSectorNumber = nSectorNumber % m_nSectorsPerTrack;
		offset[SECTOR] = nSectorNumber * Volume.SECTOR_LENGTH;
	}
	
	void setGeometryAndCodec(boolean bSpecial) throws ImageException, IOException {
		// We have to read the values from the image itself. 
		// This is not really safe, but there is no other way.

		// Read the start of the image
		byte[] abyStart = new byte[BLOCKSIZE];
		m_ImageFile.seek(0);
		m_ImageFile.readFully(abyStart);
		
		m_nSectorsPerTrack = abyStart[0x0c]&0xff;
		if (m_nSectorsPerTrack == 0) m_nSectorsPerTrack = 32;
		
		m_nHeads = (abyStart[0x10]&0x0f)+1;
		m_nTrackLength = Volume.SECTOR_LENGTH * m_nSectorsPerTrack;		
		int nSectorsPerAU = ((abyStart[0x10]>>4)&0x0f)+1;
		int nTotalAU = 	((abyStart[0x0a]<<8) | (abyStart[0x0b])) & 0xffff;
		
		if (bSpecial) {
			m_nTotalSectors = nSectorsPerAU * nTotalAU;
			m_nCylinders = m_nTotalSectors / m_nSectorsPerTrack;
		}
		else {
			m_nTotalSectors = (int)(m_ImageFile.length() / Volume.SECTOR_LENGTH);
			m_nCylinders = (int)((m_ImageFile.length() / m_nTrackLength) / m_nHeads);
		}
		
		if (m_nTotalSectors > TFileSystem.MAXAU * nSectorsPerAU)
			m_nTotalSectors = TFileSystem.MAXAU * nSectorsPerAU;
	}
	
	@Override
	void createBuffer(int cylinder, int head, int track) {
	}
	
	@Override
	int loadBuffer(Location loc) {
		return 0;
	}
	
	@Override
	TFileSystem determineFileSystem(RandomAccessFile rafile) throws IOException, ImageException {
		return null;
	}
	
	@Override
	public Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		byte[] abySector = new byte[m_nSectorLength];
		// Get sector offset in track
		// System.out.println("Read sector " + nSectorNumber);
		int[] offset = new int[2];
		getOffset(nSectorNumber, offset);
		System.arraycopy(m_abyBuffer, offset[SECTOR], abySector, 0, m_nSectorLength);
		return new Sector(nSectorNumber, abySector);
	}

	/** For Windows systems and access to the physical device we must
		adjust to block boundaries. */
	@Override
	public void writeSector(Sector sect) throws IOException, ImageException {
		// System.out.println("Writing sector " + sect.getNumber());
		try {
			int[] offset = new int[2];
			
			// Find the start sector of this track
			int nStartSector = (sect.getNumber() / m_nSectorsPerTrack) * m_nSectorsPerTrack;
			getOffset(nStartSector, offset);
			
			if (offset[SECTOR] != 0) {
				throw new IOException(TIImageTool.langstr("RawHDNoBlock"));
			}

			// Copy the new sector into the track image first
			// We will then write the complete block containing this sector
			int nSectOff = sect.getNumber() - nStartSector;
			int nSectorsPerBlock = BLOCKSIZE / Volume.SECTOR_LENGTH;
			int nBlockOff = (nSectOff / nSectorsPerBlock)*BLOCKSIZE;
			byte[] abyNewBlock = new byte[BLOCKSIZE];
			int nPos = (sect.getNumber() / nSectorsPerBlock)*BLOCKSIZE;
/*
			System.out.println("Writing sector " + sect.getNumber());
			System.out.println("Sectors per track: " + m_nSectorsPerTrack);
			System.out.println("Start sector of this track is " +nStartSector);
			System.out.println("Sector offset is " + nSectOff);
			System.out.println("Sectors per block: " + nSectorsPerBlock);
			System.out.println("Block offset: " + nBlockOff);
			System.out.println("Track offset: " + offset[TRACK]);
			System.out.println("Block location: " + nPos);
*/
			// Copy the new sector contents into the track
			System.arraycopy(sect.getBytes(), 0, m_abyBuffer, nSectOff * Volume.SECTOR_LENGTH, Volume.SECTOR_LENGTH);

			// Copy the block in the track into the buffer to be written
			System.arraycopy(m_abyBuffer, nBlockOff, abyNewBlock, 0, BLOCKSIZE);
			
			m_ImageFile.seek(nPos);
			m_ImageFile.write(abyNewBlock);
		}
		catch (EOFException eofx) {
			throw new EOFException(String.format(TIImageTool.langstr("ImageBeyond"), sect.getNumber()));
		}
	}
	
	public void flush() {
		// Do nothing.
	}
	
	// Not needed
	void createEmptyImage(File newfile, int sides, int density, int tracks, int sectors, boolean format) throws FileNotFoundException, IOException {
	}
	
	// Not needed
	void formatTrack(int cylinder, int head, int seccount, int density, int[] gap) {
	}
	
	/** Write a header. */
	@Override
	void prepareImageFile() {
		System.out.println("FIXME: prepareImageFile in RawHDFormat");
	}
		
	// FIXME
	@Override
	int getBufferIndex(Location loc) {
		return NONE;
	}
}

