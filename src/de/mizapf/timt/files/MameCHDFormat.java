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


/*
    V4 header:

    [  0] char   tag[8];        // 'MComprHD'
    [  8] UINT32 length;        // length of header (including tag and length fields)
    [ 12] UINT32 version;       // drive format version
    [ 16] UINT32 flags;         // flags (see below)
    [ 20] UINT32 compression;   // compression type
    [ 24] UINT32 totalhunks;    // total # of hunks represented
    [ 28] UINT64 logicalbytes;  // logical size of the data (in bytes)
    [ 36] UINT64 metaoffset;    // offset to the first blob of metadata
    [ 44] UINT32 hunkbytes;     // number of bytes per hunk
    [ 48] UINT8  sha1[20];      // combined raw+meta SHA1
    [ 68] UINT8  parentsha1[20];// combined raw+meta SHA1 of parent
    [ 88] UINT8  rawsha1[20];   // raw data SHA1
    [108] (V4 header length)

    Flags are the same as V1

    Compression types:
        CHDCOMPRESSION_NONE = 0
        CHDCOMPRESSION_ZLIB = 1
        CHDCOMPRESSION_ZLIB_PLUS = 2
        CHDCOMPRESSION_AV = 3

    Map format is the same as V3
    V3 map format:

    [  0] UINT64 offset;        // starting offset within the file
    [  8] UINT32 crc32;         // 32-bit CRC of the uncompressed data
    [ 12] UINT16 length_lo;     // lower 16 bits of length
    [ 14] UINT8 length_hi;      // upper 8 bits of length
    [ 15] UINT8 flags;          // flags, indicating compression info
    
    =========================================================================

    V5 header:

    [  0] char   tag[8];        // 'MComprHD'
    [  8] UINT32 length;        // length of header (including tag and length fields)
    [ 12] UINT32 version;       // drive format version
    [ 16] UINT32 compressors[4];// which custom compressors are used?
    [ 32] UINT64 logicalbytes;  // logical size of the data (in bytes)
    [ 40] UINT64 mapoffset;     // offset to the map
    [ 48] UINT64 metaoffset;    // offset to the first blob of metadata
    [ 56] UINT32 hunkbytes;     // number of bytes per hunk (512k maximum)
    [ 60] UINT32 unitbytes;     // number of bytes per unit within each hunk
    [ 64] UINT8  rawsha1[20];   // raw data SHA1
    [ 84] UINT8  sha1[20];      // combined raw+meta SHA1
    [104] UINT8  parentsha1[20];// combined raw+meta SHA1 of parent
    [124] (V5 header length)

    If parentsha1 != 0, we have a parent (no need for flags)
    If compressors[0] == 0, we are uncompressed (including maps)

    V5 uncompressed map format:

    [  0] UINT32 offset;        // starting offset div by hunk size

    Metadata format:
    
    [  0] char   tag[4]
    [  4] UINT8  flags
    [  5] UINT24 length
    [  8] UINT64 next
    [ 16] UINT8  data[length]
    
0000000: 4d43 6f6d 7072 4844 0000 007c 0000 0005  MComprHD...|....
0000010: 0000 0000 0000 0000 0000 0000 0000 0000  ................
0000020: 0000 0000 0f80 0000 0000 0000 0000 007c  ...............|
0000030: 0000 0000 0003 e07c 0000 1000 0000 0100  .......|........
0000040: 0000 0000 0000 0000 0000 0000 0000 0000  ................
0000050: 0000 0000 0000 0000 0000 0000 0000 0000  ................
0000060: 0000 0000 0000 0000 0000 0000 0000 0000  ................
0000070: 0000 0000 0000 0000 0000 0000 0000 003f  ...............?
0000080: 0000 0000 0000 0040 0000 0000 0000 0041  .......@.......A
0000090: 0000 0042 0000 0043 0000 0044 0000 0045  ...B...C...D...E
00000a0: 0000 0046 0000 0047 0000 0048 0000 0049  ...F...G...H...I
00000b0: 0000 004a 0000 004b 0000 004c 0000 004d  ...J...K...L...M
00000c0: 0000 004e 0000 004f 0000 0050 0000 0051  ...N...O...P...Q
00000d0: 0000 0052 0000 0053 0000 0054 0000 0055  ...R...S...T...U
00000e0: 0000 0056 0000 0057 0000 0058 0000 0059  ...V...W...X...Y
00000f0: 0000 005a 0000 005b 0000 005c 0000 005d  ...Z...[...\...]
0000100: 0000 005e 0000 005f 0000 0060 0000 0061  ...^..._...`...a
0000110: 0000 0062 0000 0063 0000 0064 0000 0065  ...b...c...d...e
0000120: 0000 0066 0000 0067 0000 0068 0000 0069  ...f...g...h...i
0000130: 0000 006a 0000 006b 0000 006c 0000 006d  ...j...k...l...m
0000140: 0000 006e 0000 006f 0000 0070 0000 0071  ...n...o...p...q
0000150: 0000 0072 0000 0073 0000 0074 0000 0075  ...r...s...t...u
0000160: 0000 0076 0000 0077 0000 0078 0000 0079  ...v...w...x...y
0000170: 0000 007a 0000 007b 0000 007c 0000 007d  ...z...{...|...}

...

003f000: 4241 434b 5550 2020 2020 f800 2064 013a  BACKUP    .. d.:
003f010: ff1d 0000 0000 3114 0004 0000 0705 0005  ......1.........
003f020: 0007 0009 000b 000d 000f 0011 0013 0015  ................
003f030: 0017 0019 001b 001d 070c 001f 0021 0023  .............!.#
003f040: 0025 0027 0000 0000 0000 0000 0000 0000  .%.'............
003f050: 0000 0000 0000 0000 0000 0000 0000 0000  ................
003f060: 0000 0000 0000 0000 0000 0000 0000 0000  ................
003f070: 0000 0000 0000 0000 0000 0000 0000 0000  ................
003f080: 0000 0000 0000 0000 0000 0000 0000 0000  ................
003f090: 0000 0000 0000 0000 0000 0000 0000 0000  ................
003f0a0: 0000 0000 0000 0000 0000 0000 0000 0000  ................
003f0b0: 0000 0000 0000 0000 0000 0000 0000 0000  ................
003f0c0: 0000 0000 0000 0000 0000 0000 0000 0000  ................
003f0d0: 0000 0000 0000 0000 0000 0000 0000 0000  ................
003f0e0: 0000 0000 0000 0000 0000 0000 0000 0000  ................
003f0f0: 0000 0000 0000 0000 0000 0000 0000 0000  ................
003f100: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f110: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f120: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f130: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f140: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f150: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f160: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f170: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f180: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f190: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f1a0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f1b0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f1c0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f1d0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f1e0: ffff ffff ffff ffff ffff ffff ffff ffff  ................
003f1f0: ffff ffff ffff fa00 0000 0000 0000 0000  ................

*/

package de.mizapf.timt.files;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.zip.CRC32;
import java.util.Arrays;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;

public class MameCHDFormat extends ImageFormat {

	int m_nVersion;

	// We define hunks to be the tracks
	
	// This is wrong. Hunks are 4096 bytes, while tracks usually have 32*256 = 8192 bytes
	// Why does it work at all? --- By getSectorOffset, we only specify the sector number,
	// and the track length is falsely assumed to be the hunk size, so *internally* it calculates
	// a track number that is twice as high, thus we are actually located on the hunk number.
	// That is, when we read sector 50, we should expect it on track 1, offset 18*256, but
	// here we calculate hunk 3, offset 2*256.
	
	// In that sense, this is actually not an issue, because it will yield the 
	// correct result. However, we should check what happens for sector/track != 32 (possible?)
	
	// Let's keep it like this: Hunks are "logical tracks" of the CHD.
	
	int m_nCompression;
	int m_nHeaderLength;
	int m_nFlags;
	
	long m_nMapOffset;
	
	int m_nTotalHunks;
	long m_nMetaOffset;
	long m_nLogicalBytes;

	byte m_byHunkFlags;
	long m_nHunkOffset;
	
	long m_nAppendOffset;
		
	final static int METALENGTH = 16;	
	final static int MAPENTRYSIZEv4 = 16;	
	
	final static int CRCIMG = 0;
	final static int CRCCALC = 1;
		
	public static void main(String[] arg) {
		TIImageTool.localize();
		try {
			byte[] abyNewImage = createEmptyCHDImage(new FormatParameters("TEST", 640, 4, 32, 256, 2048, 1, 480, 2, false, 480, Time.createNow(), true, true,5));
			if (arg.length > 0) {
				FileOutputStream fos = new FileOutputStream(arg[0]);
				fos.write(abyNewImage);
				fos.close();
			}
			else
				System.out.println(Utilities.hexdump(0, 0, abyNewImage, abyNewImage.length, false)); 
		}
		catch (IllegalOperationException ix) {
			ix.printStackTrace();
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
	}
	
	public String getDumpFormatName() {
		return TIImageTool.langstr("CHDImageType");
	}
	
	static int findValue(byte[] aby, String sToken, int nStart, int nEnd) {
		String sTok = sToken + ":";
		byte[] abyToken = sTok.getBytes();
		int i = nStart;
		int nMatch = 0;
		boolean bFound = false;
		int nResult = 0;
		while (i < nEnd && !bFound) {
			if (aby[i] == abyToken[nMatch]) nMatch++;
			else nMatch = 0;
			i++;
			if (nMatch == abyToken.length) {
				bFound = true;
				boolean bIsNumber = true;
				while (bIsNumber) {
					if (aby[i] < 0x30 || aby[i] > 0x39) bIsNumber = false;
					else nResult = nResult * 10 + (aby[i++]-0x30);
				}
			}
		}
		return nResult;
	}
	
	static int vote(RandomAccessFile fileSystem) throws IOException, EOFException {
		// File system size must be bigger than 3 MB
		// No, not with unexpanded CHD
//		if (fileSystem.length()==0 || fileSystem.length() < 3000000) return 0;
		
		byte[] chd = "MComprHD".getBytes();
		boolean isChd = true;
			
		// Read start of file
		byte[] abyStart = new byte[1024];
		fileSystem.seek(0);
		fileSystem.readFully(abyStart);
		
		for (int i=0; i < chd.length; i++) {
			if (chd[i]!=abyStart[i]) return 0;
		}
		return 100;
	}

	public MameCHDFormat(RandomAccessFile filesystem, String sImageName) throws IOException, ImageException {
		super(filesystem, sImageName);
		m_nDensity = 0;
		writeThrough(true);
	}

	@Override
	protected int locateInBuffer(int nSectorNumber) throws IOException {
		return 0;
	}	
	
	public int getVersion() {
		return m_nVersion;
	}
	
	public int getHunkCount() {
		return m_nTotalHunks;
	}
	
	void setGeometryAndCodec(boolean bSpecial) throws ImageException, IOException {
		byte[] abyStart = new byte[1024];
		m_ImageFile.seek(0);
		m_ImageFile.readFully(abyStart);

		m_nHeaderLength = Utilities.getInt32be(abyStart, 8);
		m_nVersion = Utilities.getInt32be(abyStart, 12);
		
		if (m_nVersion < 4) throw new ImageException(String.format(TIImageTool.langstr("MameCHDTooLow"), m_nVersion));
		if (m_nVersion > 5) throw new ImageException(String.format(TIImageTool.langstr("MameCHDTooHigh"), m_nVersion));
		if (m_nVersion == 4) {
			m_nFlags = Utilities.getInt32be(abyStart, 16);
			
			m_nCompression = Utilities.getInt32be(abyStart, 20);
			if (m_nCompression != 0) throw new ImageException(TIImageTool.langstr("MameCHDCompressed")); 		
			
			m_nTotalHunks   = Utilities.getInt32be(abyStart, 24);
			m_nLogicalBytes = Utilities.getInt64be(abyStart, 28);
			m_nMetaOffset   = Utilities.getInt64be(abyStart, 36);
			m_nTrackLength  = Utilities.getInt32be(abyStart, 44);
			
			m_abyBuffer = new byte[m_nTrackLength];
			m_nCurrentTrack = NOTRACK;
		}
		if (m_nVersion == 5) {	
			
			m_nCompression = Utilities.getInt32be(abyStart, 16);
			if (m_nCompression != 0) throw new ImageException(TIImageTool.langstr("MameCHDCompressed")); 		
			
			m_nMapOffset    = Utilities.getInt64be(abyStart, 40);
			m_nLogicalBytes = Utilities.getInt64be(abyStart, 32);
			m_nMetaOffset   = Utilities.getInt64be(abyStart, 48);
			m_nTrackLength  = Utilities.getInt32be(abyStart, 56);   // actually, hunk length
			
			m_abyBuffer = new byte[m_nTrackLength];
			m_nCurrentTrack = NOTRACK;
			m_nTotalHunks = (int)m_nLogicalBytes / m_nTrackLength;
		}
		
		// Find metadata of the device
		long nOffset = m_nMetaOffset;
		int nMetaLength = 0;
		boolean bFound = false;
		int nMetaFlags = 0;
		byte[] abyMeta = new byte[METALENGTH];
		
		while (!bFound && nOffset!=0) {
			m_ImageFile.seek(nOffset);
			m_ImageFile.readFully(abyMeta);
			int nMetaTag = Utilities.getInt32be(abyMeta, 0);
			nMetaLength = Utilities.getInt32be(abyMeta, 4);
			nMetaFlags = nMetaLength>>24;
			nMetaLength = nMetaLength & 0x00ffffff;
			
			// See MAME, file chd.h
			if (nMetaTag == 0x47444444) {
				bFound = true;
			}
			else { 
				nOffset = Utilities.getInt64be(abyMeta, 8);
			}
		}
		if (!bFound) throw new ImageException(TIImageTool.langstr("MameCHDNoMetadata"));

		// Read the metadata
		m_ImageFile.seek(nOffset + METALENGTH);
		byte[] abyMetadata = new byte[nMetaLength];
		m_ImageFile.readFully(abyMetadata);
		m_nCylinders = parseValue(abyMetadata, "CYLS", nMetaLength);
		m_nHeads = parseValue(abyMetadata, "HEADS", nMetaLength);
		m_nSectorsPerTrack = parseValue(abyMetadata, "SECS", nMetaLength);

		int nBytes = parseValue(abyMetadata, "BPS", nMetaLength);
		if (nBytes != Volume.SECTOR_LENGTH) throw new ImageException(String.format(TIImageTool.langstr("MameCHDSectorSize"), nBytes, Volume.SECTOR_LENGTH));
		m_nTotalSectors = m_nCylinders * m_nHeads * m_nSectorsPerTrack;

		// Find out where the next position is for adding hunks
		// Metadata at the end may need to be skipped
		// Follow the linked list of metadata blobs
		byte[] metadata = new byte[16];			
		long nextOffset = m_nMetaOffset;
		m_nAppendOffset = 1; 
		
//		System.out.println("MetaOffset = " + Utilities.toHex((int)m_nMetaOffset, 8));	
		
		while (nextOffset != 0 && nextOffset < m_nLogicalBytes) {
//			System.out.println(Utilities.toHex((int)nextOffset, 8));
			m_ImageFile.seek(nextOffset);
			m_ImageFile.readFully(metadata);
			
			// Round up to the next hunk boundary
			m_nAppendOffset = ((nextOffset + (m_nTrackLength - 1)) / m_nTrackLength) * m_nTrackLength;
			nextOffset = Utilities.getInt64be(metadata, 8);
		}
//		System.out.println("append offset = " + Utilities.toHex((int)m_nAppendOffset, 8));					
		// TODO: Check with format in VIB?
	}
		
	@Override
	void createBuffer(int cylinder, int head, int track) {
	}
	
	@Override
	int loadBuffer(Location loc) {
		return 0;
	}

	private int parseValue(byte[] aby, String sToken, int nEnd) {
		String sTok = sToken + ":";
		byte[] abyToken = sTok.getBytes();
		int i = 0;
		int nMatch = 0;
		boolean bFound = false;
		int nResult = 0;
		while (i < nEnd && !bFound) {
			if (aby[i] == abyToken[nMatch]) nMatch++;
			else nMatch = 0;
			i++;
			if (nMatch == abyToken.length) {
				bFound = true;
				boolean bIsNumber = true;
				while (bIsNumber) {
					if (aby[i] < 0x30 || aby[i] > 0x39) bIsNumber = false;
					else nResult = nResult * 10 + (aby[i++]-0x30);
				}
			}
		}
		return nResult;
	}
	
	private void readHunk(int nHunk) throws IOException, ImageException {
		if (m_nVersion == 4) {
			m_ImageFile.seek(m_nHeaderLength + nHunk * MAPENTRYSIZEv4);
			byte[] abyMap = new byte[16];
			m_ImageFile.readFully(abyMap);
			m_nHunkOffset = Utilities.getInt64be(abyMap, 0);
			
			int crci = Utilities.getInt32be(abyMap, 8);
			int nLengthHunk = Utilities.getInt16(abyMap, 12) + (abyMap[14]<<16);
			if (nLengthHunk != m_nTrackLength) throw new ImageException(String.format(TIImageTool.langstr("MameCHDVary"), nLengthHunk, m_nTrackLength));
			m_byHunkFlags = abyMap[15];
			m_ImageFile.seek(m_nHunkOffset);
			m_ImageFile.readFully(m_abyBuffer);
			CRC32 crcc = new CRC32();
			crcc.update(m_abyBuffer);
			int crca = (int)(crcc.getValue() & 0xffffffff);
			if (crca != crci) throw new ImageException(String.format(TIImageTool.langstr("MameCHDBadCRC"), Utilities.toHex(crca,8), Utilities.toHex(crci,8)));
		}
		if (m_nVersion == 5) {
			m_ImageFile.seek(m_nMapOffset + nHunk * 4);
			byte[] abyMap = new byte[4];
			m_ImageFile.readFully(abyMap);
			m_nHunkOffset = Utilities.getInt32be(abyMap, 0) * m_nTrackLength;
			if (m_nHunkOffset == 0) {
				// System.out.println("Hunk " + nHunk + " is empty");
				Arrays.fill(m_abyBuffer, (byte)0);
			}
			else {
				int nLengthHunk = m_nTrackLength;
				m_ImageFile.seek(m_nHunkOffset);
				m_ImageFile.readFully(m_abyBuffer);
			}
		}		
		m_nCurrentTrack = nHunk;
	}
	
	/** Used for converting CHD versions. */
	public byte[] getHunkContents(int nHunk) throws IOException, ImageException {
		readHunk(nHunk);
		return m_abyBuffer;
	}

	/** Used for converting CHD versions and for initializing the file system. */
	public void writeHunkContents(byte[] abyTrack, int nHunkNumber) throws IOException {
		m_nCurrentTrack = nHunkNumber;
		System.arraycopy(abyTrack, 0, m_abyBuffer, 0, m_abyBuffer.length);
		writeCurrentHunk(false);
	}
	
	@Override
	public Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		byte[] abySector = new byte[m_nSectorLength];
		// Get sector offset in track
		//			System.out.println("Read sector " + nSectorNumber);

		if (nSectorNumber >= m_nTotalSectors) throw new ImageException(String.format(TIImageTool.langstr("ImageSectorHigh"), m_nTotalSectors));
		int secoff = getSectorOffset(nSectorNumber);
		System.arraycopy(m_abyBuffer, secoff, abySector, 0, m_nSectorLength);
		return new Sector(nSectorNumber, abySector);
	}
	
	@Override
	public void writeSector(Sector sect) throws IOException, ImageException {
		try {
			int secoff = getSectorOffset(sect.getNumber());
			System.arraycopy(sect.getBytes(), 0, m_abyBuffer, secoff, Volume.SECTOR_LENGTH);
			writeCurrentHunk(false);
		}
		catch (EOFException eofx) {
			eofx.printStackTrace();
			throw new EOFException(String.format(TIImageTool.langstr("ImageBeyond"), sect.getNumber()));
		}
	}
	
	void writeCurrentHunk(boolean bNeedReopen) throws IOException {
		if (m_nVersion == 4) {
			CRC32 crcc = new CRC32();
			crcc.update(m_abyBuffer);
			int crca = (int)(crcc.getValue() & 0xffffffff);

			m_ImageFile.seek(m_nHeaderLength + m_nCurrentTrack * MAPENTRYSIZEv4);
			byte[] abyMap = new byte[16];
			m_ImageFile.readFully(abyMap);
			m_nHunkOffset = Utilities.getInt64be(abyMap, 0);
			
			long nHunkOff = m_nHunkOffset;
			for (int i=7; i >=0; i--) {
				abyMap[i] = (byte)(nHunkOff & 0xff);
				nHunkOff = nHunkOff >> 8;
			}
			
			abyMap[8] = (byte)((crca >> 24)&0x000000ff);
			abyMap[9] = (byte)((crca >> 16)&0x000000ff);
			abyMap[10] = (byte)((crca >> 8)&0x000000ff);
			abyMap[11] = (byte)(crca&0x000000ff);
			
			Utilities.setInt16(abyMap, 12, 0x1000);
			abyMap[14] = (byte)0x00;
			abyMap[15] = m_byHunkFlags;
		
			if (bNeedReopen) reopenForWrite();
//			System.out.println("seek to " + Utilities.toHex(m_nHeaderLength + m_nCurrentTrack * MAPENTRYSIZEv4, 8));
			m_ImageFile.seek(m_nHeaderLength + m_nCurrentTrack * MAPENTRYSIZEv4);
			m_ImageFile.write(abyMap);
		}
		else
		{
			long nMapPos = m_nMapOffset + m_nCurrentTrack * 4;
			m_ImageFile.seek(nMapPos);
			byte[] abyMap = new byte[4];
			m_ImageFile.readFully(abyMap);	
			int nHunkNumber = Utilities.getInt32be(abyMap, 0);
			
			m_nHunkOffset = nHunkNumber * m_nTrackLength;
			if (nHunkNumber == 0) {
				boolean bNull = true;
				// Check if the hunk is full of 0. In that case do not allocate a new hunk.
				for (int j=0; (j < m_abyBuffer.length) && bNull; j++) {
					if (m_abyBuffer[j] != (byte)0x00) bNull = false;
				}
				// Do not change anything
				if (bNull) {
					// System.out.println("Not writing hunk " + m_nCurrentTrack + " because it is filled with 0");
					return;
				}
				// System.out.println("Hunk " + m_nCurrentTrack + " was empty before, have to append after end of CHD image");
				m_nHunkOffset = m_ImageFile.length();
				
				// If the file is shorter than the next hunk offset, use that offset
				if (m_nHunkOffset < m_nAppendOffset) m_nHunkOffset = m_nAppendOffset;
				
				nHunkNumber = (int)(m_nHunkOffset / m_nTrackLength);
				// System.out.println("nHunkNumber = " + nHunkNumber + ", m_nHunkOffset =  " + m_nHunkOffset + ", m_nTrackLength = " + m_nTrackLength);
				if ((nHunkNumber * m_nTrackLength) != m_nHunkOffset) throw new EOFException(TIImageTool.langstr("MameCHDInvalidHunk") + ": " + Long.toHexString(m_nHunkOffset)); 

				abyMap[0] = (byte)((nHunkNumber >> 24)&0xff);
				abyMap[1] = (byte)((nHunkNumber >> 16)&0xff);
				abyMap[2] = (byte)((nHunkNumber >> 8)&0xff);
				abyMap[3] = (byte)(nHunkNumber & 0xff);
				
				if (bNeedReopen) reopenForWrite();
				m_ImageFile.seek(nMapPos);
				m_ImageFile.write(abyMap);
			}
		}
		
//		System.out.println("seek to " + Utilities.toHex((int)m_nHunkOffset, 8));
		m_ImageFile.seek(m_nHunkOffset);
		m_ImageFile.write(m_abyBuffer);
		if (bNeedReopen) reopenForRead();
	}
	
	/** Sectors are always numbered from 0..max, i.e. we have LBA. */
	int getSectorOffset(int nSectorNumber) throws IOException, ImageException {
		// Determine hunk
		int nHunk = nSectorNumber * Volume.SECTOR_LENGTH / m_nTrackLength;
		if (m_nCurrentTrack != nHunk) readHunk(nHunk);
		return (nSectorNumber * Volume.SECTOR_LENGTH) % m_nTrackLength;
	}
	
	/** Create a new CHD image. */
	public static byte[] createEmptyCHDImage(FormatParameters parm) throws IllegalOperationException, IOException {
		switch (parm.chdVersion) {
		case 4:
			return createEmptyCHDImageV4(parm);
		case 5:
			return createEmptyCHDImageV5(parm);
		default:
			throw new IllegalOperationException(TIImageTool.langstr("MameCHDVersion"));		
		}
	}

	/** Create a new CHD image. */
	public static byte[] createEmptyCHDImageV5(FormatParameters parm) throws IllegalOperationException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeBytes("MComprHD");
		int nHeaderLength = 0x7c;
		long nLogicalBytes = 0;
		nLogicalBytes = parm.cylinders * parm.heads * parm.sectorsPerTrack * parm.sectorLength; 
		int nHunkBytes = 0x1000;
		long nHunks = nLogicalBytes / nHunkBytes;
		int nCompressors = 0;
		int nMapEntrySize = 0;
		long nMapOffset = 0;
		int nCompInfo = 0x0002; // for creating V4 map
		
		// Metadata
		StringBuilder sb = new StringBuilder();
		sb.append("CYLS:").append(parm.cylinders).append(",HEADS:").append(parm.heads);
		sb.append(",SECS:").append(parm.sectorsPerTrack).append(",BPS:").append(parm.sectorLength);
		int nMetaLength = sb.length() + 1;
		long nMetaOffset = 0;

		nHeaderLength = 0x7c;
		dos.writeInt(nHeaderLength);
		dos.writeInt(parm.chdVersion);
		for (int j=0; j < 4; j++) dos.writeInt(nCompressors);
		
		dos.writeLong(nLogicalBytes);
		nMapOffset = (long)nHeaderLength;
		dos.writeLong((long)nMapOffset);
		// Hunks are always 4096 bytes long 
		
		nMapEntrySize = 4;
		nMetaOffset = nMapOffset + nHunks * nMapEntrySize;
		dos.writeLong(nMetaOffset);
		dos.writeInt(nHunkBytes);
		dos.writeInt(parm.sectorLength);
		// clean the hash values, not going to use them 
		for (int i=0; i < 60; i++) dos.write(0x00);
		
		// Map
		for (int i=0; i < nHunks; i++) dos.writeInt(0x00000000);
		
		// Metadata
		dos.writeBytes("GDDD");
		
		byte byFlags = 0x01;
		dos.writeInt(nMetaLength | (byFlags<<24));
		for (int i=0; i < 8; i++) dos.write(0x00);
		
		dos.writeBytes(sb.toString());
		dos.write(0x00);
		
		// Fill the rest of this area with 0 until the first hunk
		int nPadding = 4096 - (int)((nMetaOffset + 16 + nMetaLength) & 0xfff);
		for (int j=0; j < nPadding; j++) {
			dos.write(0x00);
		}
		
		return baos.toByteArray();
	}
	
	/** Create a new CHD image version 4. */
	public static byte[] createEmptyCHDImageV4(FormatParameters parm) throws IllegalOperationException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeBytes("MComprHD");
		int nHeaderLength = 0x7c;
		long nLogicalBytes = 0;
		nLogicalBytes = parm.cylinders * parm.heads * parm.sectorsPerTrack * parm.sectorLength; 
		int nHunkBytes = 0x1000;
		long nHunks = nLogicalBytes / nHunkBytes;
		int nCompressors = 0;
		int nMapEntrySize = 0;
		long nMapOffset = 0;
		int nCompInfo = 0x0002; // for creating V4 map
		
		// Metadata
		StringBuilder sb = new StringBuilder();
		sb.append("CYLS:").append(parm.cylinders).append(",HEADS:").append(parm.heads);
		sb.append(",SECS:").append(parm.sectorsPerTrack).append(",BPS:").append(parm.sectorLength);
		int nMetaLength = sb.length() + 1;
		long nMetaOffset = 0;
		
		byte[] abyEmpty = new byte[nHunkBytes];
		Arrays.fill(abyEmpty, (byte)0x00);
		
		nHeaderLength = 0x6c;
		dos.writeInt(nHeaderLength);
		dos.writeInt(parm.chdVersion);
		dos.writeInt(0x00000002);		// flags: read/write
		dos.writeInt(nCompressors);		// compression: none 
		dos.writeInt((int)nHunks);   // Total number of hunks
		dos.writeLong(nLogicalBytes);
		
		nMapOffset = (long)nHeaderLength;
		nMetaOffset = nMapOffset + nHunks * MAPENTRYSIZEv4 + 16; // "EndOfListCookie\0"
		// System.out.println("map offset = " + nMapOffset + ", hunks = " + nHunks + ", meta length = " + nMetaLength);
		
		dos.writeLong(nMetaOffset);		// meta offset
		dos.writeInt(0x1000);	// Hunk size
		// clean the hash values, not going to use them 
		for (int i=0; i < 60; i++) dos.write(0x00);
		
		// Write map
		CRC32 crcc = new CRC32();
		crcc.update(abyEmpty);
		int crca = (int)(crcc.getValue() & 0xffffffff);
		
		for (int i=0; i < nHunks; i++) {
			dos.writeLong(nMetaOffset + 16 + nMetaLength + i * nHunkBytes);  // 16 = "GDDD..." 
			dos.writeInt(crca);
			dos.writeInt(((nHunkBytes & 0xffff)<<16) | (((nHunkBytes>>16)&0xff)<<8) | nCompInfo);
		}
		dos.writeBytes("EndOfListCookie");
		dos.write(0);
		
		// Metadata
		dos.writeBytes("GDDD");
		
		byte byFlags = 0x01;
		dos.writeInt(nMetaLength | (byFlags<<24));
		for (int i=0; i < 8; i++) dos.write(0x00);
		
		dos.writeBytes(sb.toString());
		dos.write(0x00);
		
		// We first create an empty CHD
		// We must use the writeCurrentHunk function to ensure a proper CRC
		for (int i=0; i < nHunks; i++) {
			dos.write(abyEmpty);
		}
		
		return baos.toByteArray();
	}
	
	@Override
	int getFormatType() {
		return HD_FORMAT; 
	}
	
	/** Prepares the file system. This contains sectors 0-31, with 32-63 as a 
		backup, and 16 sectors filled with zeros.
		
		TODO: Do the same with a sequence of Sector
	*/
	public void initializeFileSystem(FormatParameters parm) throws IOException {

		int nHunkBytes = 0x1000;
		
		// Do it in memory first
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		// Do it twice (sectors 0-31, with a backup in 32-63)
		for (int k=0; k < 2; k++) {
			// Create the VIB
			// === name
			dos.writeBytes(parm.name);
			for (int j=parm.name.length(); j < 10; j++) dos.write(0x20);
			// === Total AUs
			int nTotalAU = (parm.cylinders * parm.heads * parm.sectorsPerTrack + (parm.auSize-1))/parm.auSize;
			dos.writeShort(nTotalAU);
			
			if (parm.forHfdc) {
				// === Sect/track
				dos.write(parm.sectorsPerTrack & 0xff);
				// === reserved AUs
				// System.out.println("res = " + parm.reservedAUs);
				dos.write((parm.reservedAUs >> 6)&0xff);
				// === Step speed
				dos.write(parm.stepRate);
				// === Reduced write current
				dos.write(parm.reducedWriteCurrent >> 3);
				// === Sect/AU-1 | Heads-1
				dos.write(((parm.auSize-1)<<4) | (parm.heads-1));
				// === Buffered step | Write precomp
				dos.write((parm.bufferedStep? 0x80:0x00) | (parm.writePrecompensation>>4));
			}
			else {
				// SCSI
				dos.write(0x00);
				// === reserved AUs
				dos.write((parm.reservedAUs >> 6)&0xff);
				dos.write(0x00);
				dos.write(0x00);
				dos.write(((parm.auSize-1)<<4));
				dos.write(0x00);
			}
			
			// === Date and time
			dos.write(parm.time.getBytes());
			// === no files, no directories yet
			dos.write(0x00);
			dos.write(0x00);
			// === AU number of FDIR for root directory; 
			// === FDIR is always in sector 64
			dos.writeShort(64 / parm.auSize);
			// === DSK1 emulation
			dos.writeShort(0x0000);
			// === Subdirectories
			for (int l=0x1c; l < 0x100; l++) dos.write(0x00); 
			
			// Write allocation table
			int nLen = 0;
			switch (parm.auSize) {
			case 1: 
				dos.writeInt(0xffffffff);
				dos.writeInt(0xffffffff);
				dos.writeInt(0x80000000);
				nLen = 12;
				break;
			case 2: 
				dos.writeInt(0xffffffff);
				dos.writeInt(0x80000000);
				nLen = 8;
				break;
			case 4:
				dos.writeInt(0xffff8000);
				nLen = 4;
				break;
			case 8:
				dos.writeInt(0xff800000);
				nLen = 4;
				break;
			case 16:
				dos.writeInt(0xf8000000);
				nLen = 4;
				break;
			}
			// null the rest of the allocation map
			for (int j=nLen; j < 31*256; j++) dos.write(0x00); 
		}
		
		// Write a hunkful of 0x00 
		// This includes the FDIR of root
		for (int j=0; j < nHunkBytes; j++) dos.write(0x00);

		// We should now have 5 hunks in the byte array.
		// 2*32 sectors + 1 hunk 
		byte[] formbytes = baos.toByteArray();
		
		// Space for one hunk
		byte[] abyPart = new byte[nHunkBytes];
		
		// Write the file system initialization hunk-by-hunk
		reopenForWrite();
		for (int i=0; i < formbytes.length / nHunkBytes; i++) {
			System.arraycopy(formbytes, i*nHunkBytes, abyPart, 0, nHunkBytes);
			writeHunkContents(abyPart, i);
		}
		reopenForRead();
	}
	
	public void flush() {
		// Do nothing.
	}
	
	// Not needed
	void createEmptyImage(File newfile, int sides, int density, int tracks, int sectors, boolean format) throws FileNotFoundException, IOException {
	}
	
	// Not needed
	void formatTrack(int cylinder, int head, int seccount, int density, int[] gap) { }
	
	
	/** Write a header. */
	@Override
	void prepareImageFile() {
		System.out.println("FIXME: prepareImageFile in MAMECHDFormat");
	}
	
	// FIXME
	@Override
	int getBufferIndex(Location loc) {
		return NONE;
	}
	
	@Override
	TFileSystem determineFileSystem(RandomAccessFile rafile) throws IOException, ImageException {
		return null;
	}
}


