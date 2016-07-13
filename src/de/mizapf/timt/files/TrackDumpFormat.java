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
import java.io.IOException;
import java.io.EOFException;

import de.mizapf.timt.util.Utilities;

class TrackDumpFormat extends ImageFormat {

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
	
	TrackDumpFormat(RandomAccessFile filesystem, String sImageName, int nSectorLength) throws IOException, ImageException {
		super(filesystem, sImageName, nSectorLength);
	}

	public String getDumpFormatName() {
		return "track";
	}
	
	private int readTrack(int nSectorNumber) throws IOException {
		int nTrackNumber = (nSectorNumber / m_nSectorsPerTrack);
			
		if (nTrackNumber >= m_nCylinders) {
			// 40 -> 79
			// 79 -> 40
			nTrackNumber = (3 * m_nCylinders - 1) - nTrackNumber;
		}

		int nTrackOffset = nTrackNumber * m_nTrackLength; 

		if (nTrackNumber != m_nCurrentTrack) {
//			System.out.println("need to read at " + nTrackOffset);
			m_FileSystem.seek(nTrackOffset);
			m_FileSystem.readFully(m_abyTrack);
			m_nCurrentTrack = nTrackNumber;
		}
		return nTrackOffset;
	}
	
	void getOffset(int nSectorNumber, int[] offset) throws IOException, ImageException {
		int nTrackOffset = 0;
		int nTrack = 0;
		
		if (nSectorNumber >= m_nTotalSectors)  throw new EOFException("Sector " + nSectorNumber + " beyond image size");
		
		offset[TRACK] = readTrack(nSectorNumber);

		int nStart = 0;
		int nCount = 0;
		int nStep = 0;
		int nIDAMOffset = 0;
		int nDAMOffset = 0;
		
		int nSectorID = nSectorNumber % m_nSectorsPerTrack;
		
		if (m_nDensity == 1) {
			nIDAMOffset = 5;
			nDAMOffset = 30;
			nStep = (Volume.SECTOR_LENGTH + 78);
			// find 22 x 0 at the track start (remember the strange format of the TI controller)
			for (int i=0; i < 100; i++) {
				if (m_abyTrack[i] == 0) nCount++;
				else nCount = 0;
				if (nCount==22 && m_abyTrack[i+1]==(byte)0xfe) { 
					nStart = i-4;
					break;
				}
			}
		}
		else {
			nIDAMOffset = 13;
			nDAMOffset = 58;
			nStep = (Volume.SECTOR_LENGTH + 84);
			// find 40 x 4e at the track start (remember the strange format of the TI controller)
			for (int i=0; i < 100; i++) {
				if (m_abyTrack[i] == (byte)0x4e) nCount++;
				else nCount = 0;
				if (nCount==40 && m_abyTrack[i+14]==(byte)0xfe) { 
					nStart = i+1;
					break;
				}
			}
		}
		
		if (nStart == 0) throw new ImageException("Invalid image: Lead-in not found");
		for (int i=0; i < m_nSectorsPerTrack; i++) {
			// Find the sector in the track
			if (m_abyTrack[nStart + i*nStep + nIDAMOffset + 3] == nSectorID) {
				int nOff = nStart + i*nStep + nDAMOffset;
				offset[SECTOR] = nOff;
				return;
			}
		}
		throw new ImageException("Invalid image: Sector " + nSectorNumber + " (ID " + nSectorID + ") not found in track");
	}
	
	void setGeometry(boolean bSpecial) throws ImageException, IOException {
		int format = -1;
		for (int i=0; i < tdfgeometry.length; i++) {
			if (m_FileSystem.length() == tdfgeometry[i][0]) {
				format = i;
			}
		}
		if (format==-1) throw new ImageException("Unknown image format (invalid length " + m_FileSystem.length() + ")");
			
		// We need to know the number of sectors per track
		// so that we can calculate the offset of the sector from the 
		// start of the file
		// Doing it the easy way: We assume that the image length is a well-known size.

		m_nHeads = tdfgeometry[format][1];
		m_nCylinders = tdfgeometry[format][2];
		m_nSectorsPerTrack = tdfgeometry[format][3];
		m_nDensity = tdfgeometry[format][4];

		if (m_nDensity == 1) m_nTrackLength = 16 + m_nSectorsPerTrack * (Volume.SECTOR_LENGTH + 78) + 231;
		else m_nTrackLength = 40 + m_nSectorsPerTrack * (Volume.SECTOR_LENGTH + 84) + 712;

		m_nTotalSectors = m_nHeads * m_nCylinders * m_nSectorsPerTrack;
		
		if (m_nTrackLength * m_nHeads * m_nCylinders != m_FileSystem.length()) {
			throw new ImageException("Unsupported image format: invalid track length");
		}
	}
	
	void writeSector(int nNumber, byte[] abySector, boolean bFM, boolean bNeedReopen) throws IOException, ImageException {
		try {
			int[] offset = new int[2];
			getOffset(nNumber, offset);

			// We need to recalculate the CRC; for this purpose we copy the
			// sector contents into a longer array and add the DAM to the 
			// beginning (FM: 0xfe, MFM: 0xa1 0xa1 0xa1 0xfe) 
			// and keep some space for the CRC at the end
			byte[] abySector1 = new byte[Volume.SECTOR_LENGTH+6];
			System.arraycopy(abySector, 0, abySector1, 4, Volume.SECTOR_LENGTH);
			abySector1[0] = (byte)0xa1;
			abySector1[1] = (byte)0xa1;
			abySector1[2] = (byte)0xa1;
			abySector1[3] = (byte)0xfb;
			
			int nStart = (bFM)? 3 : 0;
			int nLen = (bFM)? Volume.SECTOR_LENGTH+1 : Volume.SECTOR_LENGTH+4;
			
			int crc = Utilities.crc16_get(abySector1, nStart, nLen);
			abySector1[Volume.SECTOR_LENGTH+4] = (byte)((crc >> 8)&0xff);
			abySector1[Volume.SECTOR_LENGTH+5] = (byte)(crc & 0xff);

			System.arraycopy(abySector1, 4, m_abyTrack, offset[SECTOR], Volume.SECTOR_LENGTH+2);

			if (bNeedReopen) reopenForWrite();
			m_FileSystem.seek(offset[TRACK] + offset[SECTOR]);

			for (int i=0; i < Volume.SECTOR_LENGTH+2; i++)
				m_FileSystem.write(abySector1[i+4]);

			if (bNeedReopen) reopenForRead();
		}
		catch (EOFException eofx) {
			eofx.printStackTrace();
			throw new EOFException("Sector " + nNumber + " beyond image size");
		}
	}
}
