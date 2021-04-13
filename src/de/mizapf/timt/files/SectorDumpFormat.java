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
import java.io.FileOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.io.File;

import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.NotImplementedException;

class SectorDumpFormat extends ImageFormat {

	int m_maxSector;
	
	static final int[][] sdfgeometry = { 
		{ 92160, 1, 40, 9 }, 
		{ 184320, 2, 40, 9 },
	    { 327680, 2, 40, 16 },
		{ 368640, 2, 40, 18 }, 
		{ 655360, 2, 80, 16 }, 
		{ 737280, 2, 80, 18 }, 
		{ 1474560, 2, 80, 36 }, 
	    { 2949120, 2, 80, 72 } };
		
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

	class SectorDumpCodec extends FormatCodec {
		SectorDumpCodec(String sFile, boolean bInitial) {
			super(sFile, bInitial);
		}	
		
		SectorDumpCodec(RandomAccessFile rafile, String sFile, boolean bInitial) {
			super(rafile, sFile, bInitial);
		}
				
		void createEmptyBuffer(int buffernum) {
			// System.out.println("createEmptyBuffer " + buffernum);
			// Thread.currentThread().dumpStack();
			m_abyBuffer = new byte[m_bufferlen[buffernum]];
			for (int i=0; i < m_nSectorsPerTrack; i++) {
				System.arraycopy(m_abyFill, 0, m_abyBuffer, i * 256, 256);
			}
		}
		
		int decodeBuffer() {
			// System.out.println("decodeBuffer");
			m_buffsector.clear();
			int count =  m_fs.getSectors();
			if (count==0) {
				// We do not know how many sectors we'll get
				count = m_abyBuffer.length / 256;
			}
			byte[] content = new byte[256];
			for (int i = 0; i < count; i++) {
				Location loc = new Location(m_locCurrent.cylinder, m_locCurrent.head, i, m_locCurrent.track);
				System.arraycopy(m_abyBuffer, i * 256, content, 0, 256);
				m_buffsector.add(new ImageSector(loc, content, (byte)0xfb, count>10, i*256));
			}
			return count;	
		}
	}

	/** Loaded image. Called from getImageFormat. */
	SectorDumpFormat(RandomAccessFile rafile, String sImageName) throws IOException, ImageException {
		super(rafile, sImageName);
		m_codec = new SectorDumpCodec(rafile, sImageName, false);
		setupBuffers(sImageName, false);
		writeThrough(false);
	}

	/** Newly created image. */
	SectorDumpFormat(RandomAccessFile rafile, String sImageName, TFileSystem fs) throws IOException, ImageException {
		super(rafile, sImageName, fs);
		m_codec = new SectorDumpCodec(rafile, sImageName, true);
		setupBuffers(sImageName, true);
		writeThrough(false);
	}
	
	// Called from ImageFormat.createFloppyImage
	SectorDumpFormat() {
		writeThrough(false);
	}

	public String getDumpFormatName() {
		return TIImageTool.langstr("SectorDump");
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
		return SECTORDUMP; 
	}
	
	@Override
	TFileSystem determineFileSystem(RandomAccessFile rafile) throws IOException, ImageException {
		long nLength = rafile.length();
		if (((nLength / 256) % 10)==3) nLength -= 768;
		
		int format = NONE;
		
		for (int i=0; i < sdfgeometry.length; i++) {
			if (nLength == sdfgeometry[i][0]) {
				format = i;
				break;
			}
		}
		if (format==-1) throw new ImageException(TIImageTool.langstr("SectorDumpInvLength") + ": " + m_ImageFile.length());
			
		FloppyFileSystem ffs = new FloppyFileSystem(
			sdfgeometry[format][2],  // cyl
			sdfgeometry[format][1],  // head
			sdfgeometry[format][3],  // sect
			FloppyFileSystem.UNKNOWN_DENSITY);  // dens
		return ffs;
	}
			
	@Override
	void setupBuffers(String sImageName, boolean bInitial) {
		// Calculate length	
		int tracklen = SECTOR_LENGTH * m_nSectorsPerTrack;
		int[] bufferpos = new int[m_nCylinders*2];
		int[] bufferlen = new int[m_nCylinders*2];
		
		int pos = 0;
		for (int j=0; j < m_nCylinders*2; j++) {
			bufferpos[j] = pos;
			bufferlen[j] = tracklen;
			pos += tracklen;
		}
		m_maxSector = 11520;
		
		m_codec.setBufferParams(bufferpos, bufferlen);
	}
	
	/** Return the track number as the buffer index. */
	@Override
	int getBufferIndex(Location loc) {
		return loc.track;
	}
	
	@Override
	public void reopenForWrite() throws IOException {
		// Don't do anything here
	}
	
	@Override
	public void reopenForRead() throws IOException {
		// Don't do anything here
	}	
}

