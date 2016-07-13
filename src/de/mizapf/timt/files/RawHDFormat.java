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

class RawHDFormat extends ImageFormat {

	private final static int BLOCKSIZE = 512;
	
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
		return "sector";
	}
	
	RawHDFormat(RandomAccessFile filesystem, String sImageName, int nSectorLength) throws IOException, ImageException {
		super(filesystem, sImageName, nSectorLength);
		m_nDensity = 0;
	}
	
	private int readTrack(int nSectorNumber) throws IOException {
		// TODO: NPX for SCSI
		int nTrackNumber = (nSectorNumber / m_nSectorsPerTrack);			
		int nTrackOffset = nTrackNumber * m_nTrackLength; 

		if (nTrackNumber != m_nCurrentTrack) {
			m_FileSystem.seek(nTrackOffset);
			m_FileSystem.readFully(m_abyTrack);
			m_nCurrentTrack = nTrackNumber;
		}
		return nTrackOffset;
	}	

	void getOffset(int nSectorNumber, int[] offset) throws IOException, ImageException {
		int nTrackOffset = 0;
		int nTrack = 0;
		
		if (nSectorNumber >= m_nTotalSectors) throw new EOFException("Sector " + nSectorNumber + " beyond image size");
		
		offset[TRACK] = readTrack(nSectorNumber);

		nSectorNumber = nSectorNumber % m_nSectorsPerTrack;
		offset[SECTOR] = nSectorNumber * Volume.SECTOR_LENGTH;
	}
	
	void setGeometry(boolean bSpecial) throws ImageException, IOException {
		// We have to read the values from the image itself. 
		// This is not really safe, but there is no other way.

		// Read the start of the image
		byte[] abyStart = new byte[BLOCKSIZE];
		m_FileSystem.seek(0);
		m_FileSystem.readFully(abyStart);
		
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
			m_nTotalSectors = (int)(m_FileSystem.length() / Volume.SECTOR_LENGTH);
			m_nCylinders = (int)((m_FileSystem.length() / m_nTrackLength) / m_nHeads);
		}
		
		if (m_nTotalSectors > Volume.MAXAU * nSectorsPerAU)
			m_nTotalSectors = Volume.MAXAU * nSectorsPerAU;
	}
	
	/** For Windows systems and access to the physical device we must
		adjust to block boundaries. */
	void writeSector(int nNumber, byte[] abySector, boolean bFM, boolean bNeedReopen) throws IOException, ImageException {
		// System.out.println("Writing sector " + nNumber);
		try {
			int[] offset = new int[2];
			
			// Find the start sector of this track
			int nStartSector = (nNumber / m_nSectorsPerTrack) * m_nSectorsPerTrack;
			getOffset(nStartSector, offset);
			
			if (offset[SECTOR] != 0) {
				throw new IOException("Could not find block boundary for track.");
			}

			// Copy the new sector into the track image first
			// We will then write the complete block containing this sector
			int nSectOff = nNumber - nStartSector;
			int nSectorsPerBlock = BLOCKSIZE / Volume.SECTOR_LENGTH;
			int nBlockOff = (nSectOff / nSectorsPerBlock)*BLOCKSIZE;
			byte[] abyNewBlock = new byte[BLOCKSIZE];
			int nPos = (nNumber / nSectorsPerBlock)*BLOCKSIZE;
/*
			System.out.println("Writing sector " + nNumber);
			System.out.println("Sectors per track: " + m_nSectorsPerTrack);
			System.out.println("Start sector of this track is " +nStartSector);
			System.out.println("Sector offset is " + nSectOff);
			System.out.println("Sectors per block: " + nSectorsPerBlock);
			System.out.println("Block offset: " + nBlockOff);
			System.out.println("Track offset: " + offset[TRACK]);
			System.out.println("Block location: " + nPos);
*/
			// Copy the new sector contents into the track
			System.arraycopy(abySector, 0, m_abyTrack, nSectOff * Volume.SECTOR_LENGTH, Volume.SECTOR_LENGTH);

			// Copy the block in the track into the buffer to be written
			System.arraycopy(m_abyTrack, nBlockOff, abyNewBlock, 0, BLOCKSIZE);

			if (bNeedReopen) reopenForWrite();
			m_FileSystem.seek(nPos);
			m_FileSystem.write(abyNewBlock);
			if (bNeedReopen) reopenForRead();
		}
		catch (EOFException eofx) {
			throw new EOFException("Sector " + nNumber + " beyond image size");
		}
	}
}

