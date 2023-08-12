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

    Flags:
        0x00000001 - set if this drive has a parent
        0x00000002 - set if this drive allows writes
        
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
    
    
    Header[108]; Map[16*totalhunks]; Metadata; Hunks   
    Hunks may start at any offset
    
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
    [  5] UINT24 length (+1 for null termination)
    [  8] UINT64 next
    [ 16] UINT8  data[length]
    
    Header - Map - Metadata - Hunks   
    
    New metadata entries may be added at the end, so the next hunk may require padding
    
    
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

Hunks:
- Must have a length of a multiple of 256
- Sectors are arranged in linear order
- Sectors are stored without header (only contents)
*/

package de.mizapf.timt.files;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.zip.CRC32;
import java.util.Arrays;

import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.*;

public class MameCHDFormat extends HarddiskImageFormat {

	int m_nVersion;	        // CHD version
	
	int m_nHeaderLength;   	// CHD header length
	int m_nHunkLength;
	long m_nMapOffset;
	int m_nHunkCount;
	long m_nHunkOffset;
	byte m_byHunkFlags;
	long m_nLogicalSize;
	long m_nMetaOffset;
	int m_nFlags;
	long m_nAppendOffset;
	
	byte[] m_header;
	
	byte[] m_hunkmap;
	
	final static int MAPENTRYSIZEv4 = 16;	
	final static int MAPENTRYSIZEv5 = 4;	

	final static int METALENGTH = 16;	
	final static long EMPTY = -1;

	final static int HUNKLENGTH = 4096;
	
	final static int CRCIMG = 0;
	final static int CRCCALC = 1;

	static int vote(String sFile) throws FileNotFoundException, IOException {
		
		FileInputStream fis = new FileInputStream(sFile);
		
		// File system size must be bigger than 3 MB
		// No, not with unexpanded CHD
//		if (fileSystem.length()==0 || fileSystem.length() < 3000000) return 0;
		
		byte[] chd = "MComprHD".getBytes();
		int vote = 100;
		
		// Read start of file
		for (int i=0; (i < chd.length) && (vote > 0); i++) {
			int ch = fis.read();
			if (ch != chd[i]) vote = 0; 
		}
		fis.close();
		return vote;
	}
	
	class CHDCodec extends FormatCodec {

		void decode() {
			throw new NotImplementedException("CHDCodec");	
		}

		void encode() {
			throw new NotImplementedException("CHDCodec");	
		}
		
		void prepareNewFormatUnit(int funum, TrackFormatParameters t) {
			throw new NotImplementedException("CHDCodec");	
		}
	}

	public MameCHDFormat(String sImageName) throws FileNotFoundException, IOException, ImageException {
		super(sImageName);
		m_file = new RandomAccessFile(sImageName, "r");
		m_bInitial = false;		
		
		int nCompression;
		
		// Get the header length at pos 8
		byte[] abyHeadlen = new byte[4];
		m_file.seek(8);
		m_file.readFully(abyHeadlen);
		m_nHeaderLength = Utilities.getInt32be(abyHeadlen, 0);
		
		// Get space for header
		m_header = new byte[m_nHeaderLength];
		
		// Read the header
		m_file.seek(0);
		m_file.readFully(m_header);
		
		// Read important data
		m_nVersion = Utilities.getInt32be(m_header, 12);
	
		switch (m_nVersion) {
		case 4: 
			m_nHunkLength = Utilities.getInt32be(m_header, 44);
			m_nMapOffset = m_nHeaderLength;

			m_nHunkCount = Utilities.getInt32be(m_header, 24);
			m_hunkmap = new byte[m_nHunkCount * MAPENTRYSIZEv4];
			
			m_nFlags = Utilities.getInt32be(m_header, 16);
			
			nCompression = Utilities.getInt32be(m_header, 20);
			if (nCompression != 0) throw new ImageException(TIImageTool.langstr("MameCHDCompressed")); 		
			
			m_nLogicalSize = Utilities.getInt64be(m_header, 28);
			m_nMetaOffset  = Utilities.getInt64be(m_header, 36);		
			break;
			
		case 5:
			m_nHunkLength = Utilities.getInt32be(m_header, 56);
			m_nMapOffset = Utilities.getInt64be(m_header, 40);
			
			m_nLogicalSize = Utilities.getInt64be(m_header, 32);
			m_nHunkCount = (int)(m_nLogicalSize / m_nHunkLength);
			m_hunkmap = new byte[m_nHunkCount * MAPENTRYSIZEv5];
			
			nCompression = Utilities.getInt32be(m_header, 16);
			if (nCompression != 0) throw new ImageException(TIImageTool.langstr("MameCHDCompressed")); 		
			m_nMetaOffset   = Utilities.getInt64be(m_header, 48);

			m_nHunkCount = (int)m_nLogicalSize / m_nHunkLength;			
			break;
			
		default:
			if (m_nVersion < 4)
				throw new ImageException(String.format(TIImageTool.langstr("MameCHDTooLow"), m_nVersion));
			else
				throw new ImageException(String.format(TIImageTool.langstr("MameCHDTooHigh"), m_nVersion));
			
		}

		// Read the map
		m_file.seek(m_nMapOffset);
		m_file.readFully(m_hunkmap);
		
		// Find metadata of the device
		long nOffset = m_nMetaOffset;
		int nMetaLength = 0;
		boolean bFound = false;
		int nMetaFlags = 0;
		byte[] abyMeta = new byte[METALENGTH];
		
		// Find GDDD entry
		while (!bFound && nOffset!=0) {
			m_file.seek(nOffset);
			m_file.readFully(abyMeta);
			int nMetaTag = Utilities.getInt32be(abyMeta, 0);
			nMetaLength = Utilities.getInt32be(abyMeta, 4);
			nMetaFlags = (nMetaLength>>24)&0xff;
			nMetaLength = nMetaLength & 0x00ffffff;
			
			// See MAME, file chd.h
			if (nMetaTag == 0x47444444) {    // GDDD
				bFound = true;
			}
			else { 
				// Next entry
				nOffset = Utilities.getInt64be(abyMeta, 8);
			}
		}
		if (!bFound) throw new ImageException(TIImageTool.langstr("MameCHDNoMetadata"));

		// Read the GDDD metadata entry 
		m_file.seek(nOffset + METALENGTH);
		byte[] abyMetadata = new byte[nMetaLength];
		m_file.readFully(abyMetadata);
		
		// Members of HarddiskImageFormat
		m_nCylinders = parseValue(abyMetadata, "CYLS", nMetaLength);
		m_nHeads = parseValue(abyMetadata, "HEADS", nMetaLength);
		m_nSectorsPerTrack = parseValue(abyMetadata, "SECS", nMetaLength);

		// IDE/SCSI have 512 bytes (low-level)
		m_nSectorSize = parseValue(abyMetadata, "BPS", nMetaLength);

		int nTotalSectors = m_nCylinders * m_nHeads * m_nSectorsPerTrack;
	
		// Find out where the next position is for adding hunks
		// Metadata at the end may need to be skipped
		// Follow the linked list of metadata blobs
		long nextOffset = m_nMetaOffset;
		m_nAppendOffset = 1; 
		
//		System.out.println("MetaOffset = " + Utilities.toHex((int)m_nMetaOffset, 8));	
		
		while (nextOffset != 0 && nextOffset < m_nLogicalSize) {
//			System.out.println(Utilities.toHex((int)nextOffset, 8));
			m_file.seek(nextOffset);
			m_file.readFully(abyMeta);
			
			// Round up to the next hunk boundary
			m_nAppendOffset = ((nextOffset + (m_nHunkLength - 1)) / m_nHunkLength) * m_nHunkLength;
			nextOffset = Utilities.getInt64be(abyMeta, 8);
		}
		
		// Get the contents of hunk 0; copy the first 512 bytes
		checkFormat(readHunk(0));
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
	
	public String getFormatName() {
		return TIImageTool.langstr("HFEImage");
	}
	
	/** Find the image sector by the linear sector number. */
	@Override
	ImageSector findSector(int number) throws ImageException {
		
		for (ImageSector is : m_codec.getDecodedSectors()) {
			if (is.getNumber() == number) return is;
		}
		return null;
	}
	
	long getFormatUnitPosition(int funum) {
		byte[] abyMap;
		long pos = 0;
		if (m_nVersion == 4) {
			pos = Utilities.getInt64be(m_hunkmap, funum * MAPENTRYSIZEv4);
		}
		else {
			pos = Utilities.getInt32be(m_hunkmap, funum * MAPENTRYSIZEv5) * m_nHunkLength;
			if (pos==0) pos = EMPTY;
		}
		return pos;
	}
	
	/** Format units are hunks in this format. Sectors are arranged linearly 
	    from 0 to the maximum number. */
	int getFUNumberFromSector(int number) throws ImageException {
		return number * TFileSystem.SECTOR_LENGTH / m_nHunkLength;
	}
		
	int getFormatUnitLength(int number) {
		return m_nHunkLength;	
	}

	/** Reads a hunk. This does not change the buffer. */
	public byte[] readHunk(int nHunk) throws IOException, ImageException {
		byte[] abyBuffer = new byte[m_nHunkLength];
		
		if (nHunk > m_nHunkCount)
			throw new ImageException(String.format(TIImageTool.langstr("MameCHDInvalidHunk")));
		
		int pos = 0;
		long nHunkOffset = 0;
		int crci = 0;
		int nLengthHunk = 0;
		
		switch (m_nVersion) {
		case 4:
			// Read the pointer from the hunkmap
			pos = nHunk * MAPENTRYSIZEv4;		
			nHunkOffset = Utilities.getInt64be(m_hunkmap, nHunk * MAPENTRYSIZEv4);
			crci = Utilities.getInt32be(m_hunkmap, pos+8);
			nLengthHunk = Utilities.getInt16(m_hunkmap, pos+12) + (m_hunkmap[pos+14]<<16);
			
			if (nLengthHunk != m_nHunkLength) 
				throw new ImageException(String.format(TIImageTool.langstr("MameCHDVary"), nLengthHunk, m_nHunkLength));
			
			m_file.seek(nHunkOffset);
			m_file.readFully(abyBuffer);
			
			// Check CRC
			CRC32 crcc = new CRC32();
			crcc.update(abyBuffer);
			
			int crca = (int)(crcc.getValue() & 0xffffffff);
			if (crca != crci) 
				throw new ImageException(String.format(TIImageTool.langstr("MameCHDBadCRC"), Utilities.toHex(crca,8), Utilities.toHex(crci,8)));
			break;
			
		case 5:
			pos = nHunk * MAPENTRYSIZEv5;	
			nHunkOffset = Utilities.getInt32be(m_hunkmap, pos) * m_nHunkLength;
			
			if (nHunkOffset == 0) {
				// System.out.println("Hunk " + nHunk + " is empty");
				Arrays.fill(abyBuffer, (byte)0);
			}
			else {
				m_file.seek(nHunkOffset);
				m_file.readFully(abyBuffer);
			}
			break;
		
		default:
			 throw new ImageException(String.format(TIImageTool.langstr("MameCHDTooHigh"), m_nVersion));
		}
		
		return abyBuffer;
	}
	
	public int getHunkCount() {
		return m_nHunkCount;
	}
		
	public int getVersion() {
		return m_nVersion;
	}
	
	@Override
	int getImageType() {
		return getImageTypeStatic();
	}
	
	static int getImageTypeStatic() {
		return CHD;
	}
	
	/** Writes a hunk. This does not change the buffer. */
	public void writeHunk(byte[] abyHunk, int nHunkNumber) throws IOException, ImageException {

		int pos = 0;
		long nHunkOffset = 0;
		
		switch (m_nVersion) {
		case 4:
			CRC32 crcc = new CRC32();
			crcc.update(abyHunk);
			int crca = (int)(crcc.getValue() & 0xffffffff);
			pos = nHunkNumber * MAPENTRYSIZEv4;

			// Change the CRC in the map entry
			m_hunkmap[pos + 8] = (byte)((crca >> 24)&0x000000ff);
			m_hunkmap[pos + 9] = (byte)((crca >> 16)&0x000000ff);
			m_hunkmap[pos + 10] = (byte)((crca >> 8)&0x000000ff);
			m_hunkmap[pos + 11] = (byte)(crca&0x000000ff);

			// Hunk length does not change
			
//			System.out.println("seek to " + Utilities.toHex(m_nHeaderLength + m_nCurrentTrack * MAPENTRYSIZEv4, 8));
			nHunkOffset = Utilities.getInt64be(m_hunkmap, pos); 

			// Write back the map entry with the update CRC
			m_file.seek(m_nMapOffset + pos);
			m_file.write(m_hunkmap, pos, MAPENTRYSIZEv4);
			
			break;
		case 5:
			pos = nHunkNumber * MAPENTRYSIZEv5;
			nHunkOffset = Utilities.getInt32be(m_hunkmap, pos) * m_nHunkLength;

			// If the offset is 0, the hunk is filled with zeros
			if (nHunkOffset == 0) {
				
				boolean bNull = true;
				// Check if the new hunk is full of 0. In that case do not allocate a new hunk.
				for (int j=0; (j < abyHunk.length) && bNull; j++) {
					if (abyHunk[j] != (byte)0x00) bNull = false;
				}
				
				// If the new hunk is still filled with 0, do not change anything
				if (bNull) {
					// System.out.println("Not writing hunk " + m_nCurrentTrack + " because it is filled with 0");
					return;
				}
				
				// else we have to append a new record to the end
				// System.out.println("Hunk " + m_nCurrentTrack + " was empty before, have to append after end of CHD image");
				nHunkOffset = m_file.length();

					// If the file is shorter than the next hunk offset, use that offset
				if (nHunkOffset < m_nAppendOffset) nHunkOffset = m_nAppendOffset;
				
				if ((nHunkOffset % m_nHunkLength)!=0) 
					throw new EOFException(TIImageTool.langstr("MameCHDInvalidHunk") + ": " + Long.toHexString(nHunkOffset)); 
				
				// Put the hunk number in the map entry
				int nHunkNum = (int)(nHunkOffset / m_nHunkLength);
				m_hunkmap[pos + 0] = (byte)((nHunkNum >> 24)&0xff);
				m_hunkmap[pos + 1] = (byte)((nHunkNum >> 16)&0xff);
				m_hunkmap[pos + 2] = (byte)((nHunkNum >> 8)&0xff);
				m_hunkmap[pos + 3] = (byte)(nHunkNum & 0xff);
				
				// Write back the new map entry 
				m_file.seek(m_nMapOffset + pos);
				m_file.write(m_hunkmap, pos, MAPENTRYSIZEv5);
			}

			break;
			
		default:
			 throw new ImageException(String.format(TIImageTool.langstr("MameCHDTooHigh"), m_nVersion));
		}
		
//		System.out.println("seek to " + Utilities.toHex((int)m_nHunkOffset, 8));
		// Write the actual hunk
		m_file.seek(nHunkOffset);
		m_file.write(abyHunk);
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
	
	private static byte[] createMetaEntry(String sMetaLabel, String sEntry) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeBytes(sMetaLabel);
			
			int nMetaLength = sEntry.length() + 1;
			byte byFlags = 0x01;
			dos.writeInt((byFlags<<24) | (nMetaLength & 0x00ffffff));
			dos.writeLong(0);
			dos.writeBytes(sEntry);
			dos.write(0);
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
		return baos.toByteArray();
	}
	
	private static void linkMetaEntry(byte[] entry, long pos) {
		for (int i=0; i < 7; i++) {
			entry[15-i] = (byte)(pos & 0xff);
			pos = pos >> 8;
		}
	}

	/** Create a new empty CHD image. */
	public static byte[] createEmptyCHDImageV5(FormatParameters parm) throws IllegalOperationException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeBytes("MComprHD");
		int nHeaderLength = 0x7c;
		int nSectorLength = (parm.type==HFDC)? 256:512;
		
		long nLogicalSize = parm.cylinders * parm.heads * parm.sectors * nSectorLength; 

		long nHunkCount = nLogicalSize / HUNKLENGTH;
		// Round up
		if ((nLogicalSize % HUNKLENGTH)!=0) nHunkCount++;

		int nMapEntrySize = 0;
		long nMapOffset = 0;
		long nMetaOffset = 0;

		dos.writeInt(nHeaderLength);
		dos.writeInt(parm.chdVersion);
		int nCompressors = 0;
		for (int j=0; j < 4; j++) dos.writeInt(nCompressors);
		
		dos.writeLong(nLogicalSize);
		nMapOffset = (long)nHeaderLength;
		dos.writeLong((long)nMapOffset);
		// Hunks are always 4096 bytes long 
		
		nMapEntrySize = MAPENTRYSIZEv5;
		nMetaOffset = nMapOffset + nHunkCount * MAPENTRYSIZEv5;
		dos.writeLong(nMetaOffset);
		dos.writeInt(HUNKLENGTH);
		dos.writeInt(nSectorLength);
		// clean the hash values, not going to use them 
		for (int i=0; i < 60; i++) dos.write(0x00);
		
		// Map: All hunks are filled with zeros
		for (int i=0; i < nHunkCount; i++) dos.writeInt(0x00000000);
		
		int wpcom = parm.writePrecompensation;
		int rwc = parm.reducedWriteCurrent;
		
		int gap1 = 16;
		int gap2 = 3;
		int gap3 = 18;
		int sync = 13;
		int hlen = 5;
		int ecc = -1;
		int interl = 4;
		int hskew = 0;
		int cskew = 0;

		// GDDI IL:x,CSKEW:x,HSKEW:x,WPCOM:x,RWC:x
		// GDDI GAP1:x,GAP2:x,GAP3:x,SYNC:x,HLEN:x,ECC:x
		
		// Default values: IL=4, HSKEW=CSKEW=0, WPCOM=RWC=-1 (unused)
		// GAP1=16, GAP2=3, GAP3=18, SYNC=13, HLEN=5, ECC=-1
		
		StringBuilder sb = new StringBuilder();
		sb.append("GAP1:").append(gap1).append(",GAP2:").append(gap2);
		sb.append(",GAP3:").append(gap3).append(",SYNC:").append(sync);
		sb.append(",HLEN:").append(hlen).append(",ECC:").append(ecc);
		
		byte[] abyMetaEntry3 = createMetaEntry("GDDI", sb.toString());

		sb = new StringBuilder();
		sb.append("IL:").append(interl).append(",CSKEW:").append(cskew);
		sb.append(",HSKEW:").append(hskew).append(",WPCOM:").append(wpcom);
		sb.append(",RWC:").append(rwc);

		byte[] abyMetaEntry2 = createMetaEntry("GDDI", sb.toString());
		
		sb = new StringBuilder();
		sb.append("CYLS:").append(parm.cylinders).append(",HEADS:").append(parm.heads);
		sb.append(",SECS:").append(parm.sectors).append(",BPS:").append(nSectorLength);
	
		byte[] abyMetaEntry1 = createMetaEntry("GDDD", sb.toString());
		
		linkMetaEntry(abyMetaEntry1, nMetaOffset + abyMetaEntry1.length);
		linkMetaEntry(abyMetaEntry2, nMetaOffset + abyMetaEntry1.length + abyMetaEntry2.length);
		linkMetaEntry(abyMetaEntry3, 0);
	
		dos.write(abyMetaEntry1);
		dos.write(abyMetaEntry2);
		dos.write(abyMetaEntry3);
		
		long nMetaEnd = nMetaOffset + abyMetaEntry1.length + abyMetaEntry2.length + abyMetaEntry3.length;

		while ((nMetaEnd % HUNKLENGTH)!=0) {
			// We have to pad
			dos.write(0);
			nMetaEnd++;
		}
		
		return baos.toByteArray();
	}
	
	/** Create a new CHD image version 4. This is not supported anymore. */
	public static byte[] createEmptyCHDImageV4(FormatParameters parm) throws IllegalOperationException, IOException {
		throw new IllegalOperationException(TIImageTool.langstr("MameCHDVersion"));
	}
	
	/** Prepare an empty image.  */
    @Override
	void prepareNewImage(FormatParameters params) {
		throw new NotImplementedException("MameCHDFormat");
	}
	
	@Override
	TrackFormatParameters getTrackParameters() {
		throw new NotImplementedException("MameCHDFormat");
	}
	
	static String checkFormatCompatibility(FormatParameters params) {
		return null; 
	}
}


