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

	/* Most of the geometries here are not compatible with PC99. However,
	   we can use them in MAME, and it may prove difficult to prevent them
	   from being created in MAME: By reformatting a medium, we may be able
	   to create a non-compatible TDF image, but it would be weird to get 
	   an error message from MAME.
	   
	   It would be better to prevent the creation of a non-compatible format
	   by UI feedback.
	*/
	static final int[][] tdfgeometry = { 
		{ 260240, 2, 40, 9, 1 },        // DSSD, compatible to PC99
		{ 491520, 2, 40, 16, 2 },      
		{ 549760, 2, 40, 18, 2 },       // DSDD, compatible to PC99
		{ 1003520, 2, 40, 36, 3 },

		{ 520480,  2, 80, 9, 1 },       
		{ 983040,  2, 80, 16, 2 },
		{ 1099520, 2, 80, 18, 2 },      
		{ 2007040, 2, 80, 36, 3 } 
	};
	
	private static final int WGAP1 = 0;
	private static final int WGAP2 = 1;
	private static final int WGAP3 = 2;
	private static final int WGAP4 = 3;
	private static final int WGAP1BYTE = 4;
	private static final int WGAPBYTE = 5;
	private static final int WSYNC1 = 6;
	private static final int WSYNC2 = 7;	
	
	// WGAP1, WGAP2, WGAP3, WGAP4, WGAP1BYTE, WGAPBYTE, WSYNC1, WSYNC2
	int fm9param[]   = { 16, 11, 45, 231, 0x00, 0xff,  6,  6 };
	int mfm16param[] = { 50, 22, 50, 206, 0x4e, 0x4e, 12, 12 };
	int mfm18param[] = { 40, 22, 24, 712, 0x4e, 0x4e, 10, 12 };
	int mfm36param[] = { 40, 22, 24, 264, 0x4e, 0x4e, 10, 12 };
	
	private int getTrackSize(int nSectorCount)
	{
		switch (nSectorCount)
		{
		case 9:
			return 3253;
		case 16:
			return 6144;
		case 18:
			return 6872;
		case 36:
			return 12544;
		}
		return 0;
	}	
	
	private int getSectorCount(int nTrackSize)
	{
		switch (nTrackSize)
		{
		case 3253:
			return 9;
		case 6144:
			return 16;
		case 6872:
			return 18;
		case 12544:
			return 36;
		}
		return 0;
	}
	
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
	
	
	// Buffer numbers are the (logical) tracks
	
	class TrackDumpCodec extends FormatCodec {
		TrackDumpCodec(String sFile, boolean bInitial) {
			super(sFile, bInitial);
		}	
		
		TrackDumpCodec(RandomAccessFile rafile, String sFile, boolean bInitial) {
			super(rafile, sFile, bInitial);
		}
				
		void createEmptyBuffer(int buffernum) {
			// System.out.println("Creating new buffer " + buffernum);
			m_nCurrentIndex =  buffernum;

			m_nPositionInBuffer = 0;
			int gap[] = null;
			byte[] bHeader = new byte[4];

			int cylinder = FloppyFileSystem.trackToCyl(m_nCylinders, buffernum);
			int head = FloppyFileSystem.trackToHead(m_nCylinders, buffernum);
			int seccount = m_nSectorsPerTrack;
			
			// System.out.println("cyl = " + cylinder + ", head  = " + head + ", seccount = " + seccount);
			
			// Start number
			int sector = 0;
			boolean mfm = true;
			
			if (seccount < 10) {
				gap = fm9param;
				sector = (cylinder * 6) % 9;
				mfm = false;
			}
			else {
				if (seccount < 18) {
					gap = mfm16param;
				}
				else {
					if (seccount < 20) {
						gap = mfm18param;
					}
					else
						gap = mfm36param;
				}
			}			
					
			// GAP 1
			// System.out.println("Write GAP 1 for cylinder " + cylinder + ", head " + head);
			for (int i=0; i < gap[WGAP1]; i++) writeBits(gap[WGAP1BYTE],8);
			
			// Write all sectors
			for (int i=0; i < seccount; i++) {
				// System.out.println("Sector " + i);
				// Write sector		
				// Sync gap
				for (int k=0; k < gap[WSYNC1]; k++) writeBits(0x00,8);
				
				if (mfm) {			
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
				for (int k=0; k < gap[WGAP2]; k++) writeBits(gap[WGAPBYTE],8);
				// Sync
				for (int k=0; k < gap[WSYNC2]; k++) writeBits(0x00,8);
				
				// DAM
				if (mfm) {
					writeBits(0xa1,8);
					writeBits(0xa1,8);
					writeBits(0xa1,8);
				}
				writeBits(0xfb, 8);
				
				// Sector content (Empty)
				writeBits(m_abyFill);
				
				// CRC
				writeBits(0xf7f7, 16);
				
				// GAP3
				for (int k=0; k < gap[WGAP3]; k++) writeBits(gap[WGAPBYTE],8);
				
				// Next sector
				if (!mfm)
					sector = (sector + 7) % 9;
				else
					sector = (sector + 11) % 18;
			}	
			// GAP4
			// System.out.println("Write GAP 4");
			for (int k=0; k < gap[WGAP4]; k++) {
				// System.out.println("k = " + k); 
				writeBits(gap[WGAPBYTE],8);
			}
			
			// System.out.println(Utilities.hexdump(m_abyBuffer));
		}		
		
		Location getHeader(int bufferpos) {
			return new Location(m_abyBuffer[bufferpos], 
					m_abyBuffer[bufferpos+1],
					m_abyBuffer[bufferpos+2],
					m_nCurrentIndex);
		}
		
		int getIncrement(int[] gap) {
			int mark = 1;
			int headerlen = 6;
			if (gap != fm9param) mark = 4;
			int headerpos = gap[WSYNC1] + mark;
			int pre = headerpos + headerlen + gap[WGAP2] + gap[WSYNC2] + mark;
			return pre + 256 + 2 + gap[WGAP3];
		}
		
		int getFirstHeaderPos(int[] gap) {
			int mark = 1;
			int headerlen = 6;
			if (gap != fm9param) mark = 4;			
			int headerpos = gap[WSYNC1] + mark;
			return gap[WGAP1] + headerpos;
		}
		
		int getFirstContentPos(int[] gap) {
			int mark = 1;
			int headerlen = 6;
			if (gap != fm9param) mark = 4;			
			return getFirstHeaderPos(gap) + headerlen + gap[WGAP2] + gap[WSYNC2] + mark; 
		}
		
		int decodeBuffer() {
			int count = 0;
			int tracklen = m_abyBuffer.length;
			int[] gap = fm9param;
			switch (tracklen) {
			case 3253:
				count = 9;
				gap = fm9param;
				break;
			case 6144:
				count = 16;
				gap = mfm16param;
				break;
			case 6872:
				count = 18;
				gap = mfm18param;
				break;
			case 12544:
				count = 36;
				gap = mfm36param;
				break;
			}
			if (count==0) System.out.println("Unknown track length: " + m_abyBuffer.length);

			int bufferpos = getFirstHeaderPos(gap);
			int secpos = getFirstContentPos(gap);
			int increm = getIncrement(gap);

			byte[] content = new byte[256];
			
			int nFound = 0;
			
			while ((nFound < count) && (secpos+256 < m_abyBuffer.length)) {
				// m_nCurrentIndex is the current buffer number = track

				Location loc = getHeader(bufferpos);
				
				// System.out.println("Found " + loc + "(curr index=" + m_nCurrentIndex + ")");
				
				System.arraycopy(m_abyBuffer, secpos, content, 0, 256);
				m_buffsector.add(new ImageSector(loc, content, (byte)0xfb, count>10, secpos));
				
				bufferpos += increm;
				secpos += increm;
				nFound++;
			}
			return m_buffsector.size();	// or return count?
		}
		
		protected void writeCRC(byte[] crc) {
			writeBits(crc);
		}
	}
	
	TrackDumpFormat(RandomAccessFile rafile, String sImageName) throws IOException, ImageException {
		super(rafile, sImageName);
		m_codec = new TrackDumpCodec(rafile, sImageName, false);
		setupBuffers(sImageName, false);
		writeThrough(false);
	}
	
	/** Newly created image. */
	TrackDumpFormat(RandomAccessFile rafile, String sImageName, TFileSystem fs) throws IOException, ImageException {
		super(rafile, sImageName, fs);
		m_codec = new TrackDumpCodec(rafile, sImageName, true);
		setupBuffers(sImageName, true);
		writeThrough(false);
	}

	TrackDumpFormat() {
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
	TFileSystem determineFileSystem(RandomAccessFile rafile) throws IOException, ImageException {
		long nLength = rafile.length();
				
		int format = NONE;
		for (int i=0; i < tdfgeometry.length; i++) {
			if (nLength == tdfgeometry[i][0]) {
				format = i;
				break;
			}
		}
		if (format==-1) throw new ImageException(TIImageTool.langstr("SectorDumpInvLength") + ": " + m_ImageFile.length());
			
		FloppyFileSystem ffs = new FloppyFileSystem(
			tdfgeometry[format][2],  // cyl
			tdfgeometry[format][1],  // head
			tdfgeometry[format][3],  // sect
			FloppyFileSystem.UNKNOWN_DENSITY);  // dens
		return ffs;	
	}

	@Override
	void setupBuffers(String sImageName, boolean bInitial) {
		int pos = 0;
		int tracklen = (m_encoding==FM)? 3253 : 6872;
		
		if (m_nHeads == 1) {
			// Warn that this is not compatible
			System.err.println(TIImageTool.langstr("TrackDump1Sided"));
		}
		
		int[] bufferpos = new int[m_nCylinders*2];
		int[] bufferlen = new int[m_nCylinders*2];

		for (int j=0; j < m_nCylinders; j++) {
			bufferpos[j] = pos;
			bufferlen[j] = tracklen;
			pos += tracklen;
		}
		for (int j=2*m_nCylinders-1; j >= m_nCylinders; j--) {
			bufferpos[j] = pos;
			bufferlen[j] = tracklen;
			pos += tracklen;
		}
		m_codec.setBufferParams(bufferpos, bufferlen);
	}
	
	public String getDumpFormatName() {
		return TIImageTool.langstr("TrackDump");
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
	
	// ======================================================================

}
