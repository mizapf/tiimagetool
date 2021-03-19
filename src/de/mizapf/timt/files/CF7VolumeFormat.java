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
import java.io.EOFException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.io.File;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;

class CF7VolumeFormat extends SectorDumpFormat {

	/** Indicates whether we look at a separate volume file (DSK), extracted
	    from the CF7 image, or at a volume inside the CF7 image. */
	boolean m_separate;
	
	/** Required for volumes inside the CF7 image. */ 
	int m_volumeNumber; 
	
	/** Required for volumes inside the CF7 image. */
	byte[] m_rawVolume;
	
	static int vote(RandomAccessFile fileSystem) throws IOException {
		long nLength = fileSystem.length();
		return (nLength == 409600)? 100 : 0;
	}
	
	CF7VolumeFormat() {
	}

	CF7VolumeFormat(RandomAccessFile filesystem, String sImageName) throws IOException, ImageException {
		super(filesystem, sImageName);
		m_separate = true;
	}

	CF7VolumeFormat(RandomAccessFile filesystem, String sImageName, int volnumber) throws IOException, ImageException {
		super(filesystem, sImageName);
		m_separate = false;
		m_volumeNumber = volnumber;
		int volsize = 1600 * 256 * 2;
		m_rawVolume = new byte[volsize];
		m_ImageFile.seek(volnumber * volsize);
		m_ImageFile.readFully(m_rawVolume);
		// We do not keep the filesystem open in this case.
		m_ImageFile.close();
		
/*		byte[] test = new byte[1600*256];
		for (int i=0; i < test.length; i++) test[i] = m_rawVolume[i*2];
		System.out.println(Utilities.hexdump(0, 0, test, 1024, false)); */
	}

	public String getDumpFormatName() {
		return m_separate? TIImageTool.langstr("CF7VolumeType") : TIImageTool.langstr("CF7VolumeType") + " #" + (m_volumeNumber+1);
	}
	
	@Override
	int getFormatType() {
		return FLOPPY_FORMAT; 
	}
	
	void readBufferFromImage(byte[] content, int offset) throws IOException {
		if (m_separate) {
			m_ImageFile.seek(offset);
			m_ImageFile.readFully(m_abyBuffer);
		}
		else {
			int pos = offset*2;
			for (int i=0; i < content.length; i++) {
				content[i] = m_rawVolume[pos];
				pos += 2;
			}
			//System.out.println(Utilities.hexdump(0, 0, content, 256, false));
		}
	}

	void writeOnImage() throws IOException {
		if (m_separate) {		
			// Write back the whole track
			m_ImageFile = new RandomAccessFile(m_sImageName, "rw");		
			m_ImageFile.seek(m_bufferpos[m_currentTrack]);
			m_ImageFile.write(m_abyBuffer);
			m_ImageFile = new RandomAccessFile(m_sImageName, "r");		
		}
		else {
			// Write into the cache
			int pos = m_bufferpos[m_currentTrack] * 2;
			for (int i=0; i < m_abyBuffer.length; i++) {
				m_rawVolume[pos + i*2] = m_abyBuffer[i];
			}
			// Write through (only the track) 
			m_ImageFile = new RandomAccessFile(m_sImageName, "rw");		
			m_ImageFile.seek(m_volumeNumber * (1600*256*2) + pos);
			m_ImageFile.write(m_rawVolume, pos, m_abyBuffer.length * 2);
		}
	}

/*	@Override	
	void setGeometryAndCodec(boolean bSpecial) throws IOException, ImageException {	
		m_nHeads = 2;
		m_nCylinders = 40;
		m_nSectorsPerTrack = 20;

		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		ffs.setCountedSectors(20);
		m_nSectorLength = 256;
		m_encoding = FloppyFileSystem.DOUBLE_DENSITY;

		int tracklen = Volume.SECTOR_LENGTH * m_nSectorsPerTrack;

		m_bufferpos = new int[m_nCylinders*2];
		m_bufferlen1 = new int[m_nCylinders*2];
		
		int pos = 0;
		for (int j=0; j < m_nCylinders*2; j++) {
			m_bufferpos[j] = pos;
			m_bufferlen1[j] = tracklen;
			pos += tracklen;
		}		
		
		m_maxSector = 1600;
	}
	*/
}

