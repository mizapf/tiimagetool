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

class SectorDumpFormat extends ImageFormat {

	static final int[][] sdfgeometry = { 
		{ 92160, 1, 40, 9, 1 }, 
		{ 184320, 2, 40, 9, 1 },
	    { 327680, 2, 40, 16, 2 },
		{ 368640, 2, 40, 18, 2 }, 
		{ 655360, 2, 80, 16, 2 }, 
		{ 737280, 2, 80, 18, 2 }, 
		{ 1474560, 2, 80, 36, 3 }, 
	    { 2949120, 2, 80, 72, 4 } };
		
	static int vote(RandomAccessFile fileSystem) throws IOException {
		long nLength = fileSystem.length();
		
		// File system size must be less than 3 MB
		if (nLength==0 || nLength > 3000000) return 0;
		
		int nVal = 100;
		
		if (((nLength / 256) % 10)==3) nLength -= 768;
		if ((nLength % Volume.SECTOR_LENGTH) != 0) nVal = 10;

		for (int i=0; i < sdfgeometry.length; i++) {
			if (nLength == sdfgeometry[i][0]) {
				return nVal;
			}
		}
		return 0;		
	}

	SectorDumpFormat(RandomAccessFile filesystem, String sImageName, int nSectorLength) throws IOException, ImageException {
		super(filesystem, sImageName, nSectorLength);
	}
	
	public String getDumpFormatName() {
		return "sector";
	}
	
	private int readTrack(int nSectorNumber) throws IOException {
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
		
		if (nSectorNumber >= m_nTotalSectors)  throw new EOFException("Sector " + nSectorNumber + " beyond image size");
		
		offset[TRACK] = readTrack(nSectorNumber);

		nSectorNumber = nSectorNumber % m_nSectorsPerTrack;
		offset[SECTOR] = nSectorNumber * Volume.SECTOR_LENGTH;
	}
	
	void setGeometry(boolean bSpecial) throws ImageException, IOException {
		int format = -1;
		long nLength = m_FileSystem.length();
		if (((nLength / 256) % 10)==3) nLength -= 768;

		for (int i=0; i < sdfgeometry.length; i++) {
			if (nLength == sdfgeometry[i][0]) {
				format = i;
				break;
			}
		}
		if (format==-1) throw new ImageException("Unknown image format (invalid length " + m_FileSystem.length() + ")");
			
		m_nHeads = sdfgeometry[format][1];
		m_nCylinders = sdfgeometry[format][2];
		m_nSectorsPerTrack = sdfgeometry[format][3];
		m_nDensity = sdfgeometry[format][4];
		
		m_nTrackLength = Volume.SECTOR_LENGTH * m_nSectorsPerTrack;		
		
		m_nTotalSectors = m_nHeads * m_nCylinders * m_nSectorsPerTrack;
	}
	
	void writeSector(int nNumber, byte[] abySector, boolean bFM, boolean bNeedReopen) throws IOException, ImageException {
		try {
			int[] offset = new int[2];
			getOffset(nNumber, offset);
			// Get the absolute offset to the sector
			// Add the track offset to the sector offset
			if (bNeedReopen) reopenForWrite();
			m_FileSystem.seek(offset[TRACK] + offset[SECTOR]);
			m_FileSystem.write(abySector);
			if (bNeedReopen) reopenForRead();
		}
		catch (EOFException eofx) {
			eofx.printStackTrace();
			throw new EOFException("Sector " + nNumber + " beyond image size");
		}
	}
}

