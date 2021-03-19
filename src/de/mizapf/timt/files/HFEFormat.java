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
import java.io.IOException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;

/** The HFEFormat class implements the HFE format that is used for the Lotharek
	floppy emulator.
	
	Format definition according to the official document:
	
	[ Header |  LUT  | (Track)* ]	
	
	File header (0x0000 - 0x01FF, 512 bytes)
	----------------------------------------
	
    typedef struct picfileformatheader_
    {
       unsigned char HEADERSIGNATURE[8];        // “HXCPICFE”
       unsigned char formatrevision;            // Revision 0
       unsigned char number_of_track;           // Number of track in the file
       unsigned char number_of_side;            // Number of valid side (Not used by the emulator)
       unsigned char track_encoding;            // Track Encoding mode
                                                // (Used for the write support - Please see the list above)
       unsigned short bitRate;                  // Bitrate in Kbit/s. Ex : 250=250000bits/s
                                                // Max value : 500
       unsigned short floppyRPM;                // Rotation per minute (Not used by the emulator)
       unsigned char floppyinterfacemode;       // Floppy interface mode. (Please see the list above.)
       unsigned char dnu;                       // Free
       unsigned short track_list_offset;        // Offset of the track list LUT in block of 512 bytes
                                                // (Ex: 1=0x200)
       unsigned char write_allowed;             // The Floppy image is write protected ?
       unsigned char single_step;               // 0xFF : Single Step – 0x00 Double Step mode
       unsigned char track0s0_altencoding;      // 0x00 : Use an alternate track_encoding for track 0 Side 0
       unsigned char track0s0_encoding;         // alternate track_encoding for track 0 Side 0
       unsigned char track0s1_altencoding;      // 0x00 : Use an alternate track_encoding for track 0 Side 1
       unsigned char track0s1_encoding;         // alternate track_encoding for track 0 Side 1
    } picfileformatheader;
	   
	   
	 floppyintefacemodes are defined below as *_MODE
	 track_encodings are defined below as *_ENCODING
	 
	 track0s0_encoding is only valid when track0s0_altencoding==0xff
	 track0s1_encoding is only valid when track0s1_altencoding==0xff
	  
	 Track offset lookup table (at 0x0200)
	 -------------------------------------
	 
     typedef struct pictrack_
     {
         unsigned short offset;               // Offset of the track data in block of 512 bytes (Ex: 2=0x400)
         unsigned short track_len;            // Length of the track data in byte.
     } pictrack;
   
	 This table has a size of  number_of_track*4 bytes.
	   
	 Track data
	 ----------
	 
	 (first possible occurance at 0x0400)
	 
	 Each track is encoded in a sequence of cell levels which are represented
	 by bits in the data.
	 
	 +--------+--------+--------+--------+---- ........ ---+--------+--------+
	 | Head 0 | Head 1 | Head 0 | Head 1 | Hea ........  1 | Head 0 | Head 1 |
	 +--------+--------+--------+--------+---- ........ ---+--------+--------+
	 |     Block 0     |     Block 1     |                 |    Block n-1    | 
	 
	 Each block (Head 0 + Head 1) is 0x200 bytes long, with 0x100 bytes for
	 each head. Block n-1 may be partially filled, e.g. with 64 bytes for 
	 head 0 and 64 bytes for head 1. The contents for head 1 in block n-1
	 start at offxet 0x100 nevertheless:
	   
	 +--------+--------+
	 |]]]]  0 |]]]]  1 |
	 +--------+--------+
     |    Block n-1    |	  
	   
	 
     Each byte in the track data is a sequence of cell sample levels 
     according to the sample rate. Bit order is little endian:
     
     Bits
     7 6 5 4 3 2 1 0   15 14 13 12 11 10 9 8   23 22 21 20 19 18 17 16
	   
	 0-bits indicate no change, 1-bits indicate flux level change.
	 
	 To encode the byte 0x4e in MFM at 250 kbit/s, the following data bytes
	 are used:
	 
	 Byte:          0  1  0  0  1  1  1  0
	 MDM encoding: 10 01 00 10 01 01 01 00
	 Reversed order: 0010 1010 0100 1001     = 2a 49
	
     Disks that were sampled with a rate of 250 kbit/s with FM recording will
     have two sample bits per cell. The second bit should always be 0, since
     there are no changes at a rate higher than 125 kbit/s.
     
     It is possible that these intermediate bits are at even positions (0, 2, ...)
     so that the read process may have to be advanced by one sample.	
*/

public class HFEFormat extends ImageFormat {
	
	/** Taken from the official documentation. */
	class HFEHeader {
		
		public final static int ISOIBM_MFM_ENCODING = 0x00;
		public final static int AMIGA_MFM_ENCODING = 0x01;
		public final static int ISOIBM_FM_ENCODING = 0x02;
		public final static int EMU_FM_ENCODING = 0x03;
		public final static int UNKNOWN_ENCODING = 0x04;	
		
		public String[] encoding = { "ISOIBM_MFM_ENCODING",
			"AMIGA_MFM_ENCODING", "ISOIBM_FM_ENCODING", "EMU_FM_ENCODING",
		"UNKNOWN_ENCODING" };

		public final static int IBMPC_DD_FLOPPYMODE = 0x00;
		public final static int IBMPC_HD_FLOPPYMODE = 0x01;
		public final static int ATARIST_DD_FLOPPYMODE = 0x02;
		public final static int ATARIST_HD_FLOPPYMODE = 0x03;
		public final static int AMIGA_DD_FLOPPYMODE = 0x04;
		public final static int AMIGA_HD_FLOPPYMODE = 0x05;
		public final static int CPC_DD_FLOPPYMODE = 0x06;
		public final static int GENERIC_SHUGART_DD_FLOPPYMODE = 0x07;
		public final static int IBMPC_ED_FLOPPYMODE = 0x08;
		public final static int MSX2_DD_FLOPPYMODE = 0x09;
		public final static int C64_DD_FLOPPYMODE = 0x0a;
		public final static int EMU_SHUGART_FLOPPYMODE = 0x0b;
		public final static int S950_DD_FLOPPYMODE = 0x0c;
		public final static int S950_HD_FLOPPYMODE = 0x0d;
		public final static int DISABLE_FLOPPYMODE = 0xfe;
		
		public String[] mode = { "IBMPC_DD_FLOPPYMODE", "IBMPC_HD_FLOPPYMODE",
			"ATARIST_DD_FLOPPYMODE", "ATARIST_HD_FLOPPYMODE", 
			"AMIGA_DD_FLOPPYMODE", "AMIGA_HD_FLOPPYMODE",
			"CPC_DD_FLOPPYMODE", "GENERIC_SHUGART_DD_FLOPPYMODE",
			"IBMPC_ED_FLOPPYMODE", "MSX2_DD_FLOPPYMODE",
			"C64_DD_FLOPPYMODE", "EMU_SHUGART_FLOPPYMODE",
			"S950_DD_FLOPPYMODE", "S950_HD_FLOPPYMODE",
			"DISABLE_FLOPPYMODE" };
		
		String signature;
		int formatrevision;
		int number_of_track;
		int number_of_side;
		int track_encoding;
		int bitRate;
		int floppyRPM;
		int floppyinterfacemode;
		int dnu;
		int track_list_offset;
		boolean write_allowed;
		boolean single_step;
		boolean track0s0_altencoding;
		int track0s0_encoding;
		boolean track0s1_altencoding;
		int track0s1_encoding;
		byte[] m_bheader;
		
		HFEHeader(byte[] bytes) {
			m_bheader = bytes;
			readHeader();
		}
		
		HFEHeader(FloppyFileSystem ffs) {
			this(ffs.getTracksPerSide(), ffs.getHeads(), ffs.getDensity());
		}
		
		private void readHeader() {
			byte[] bytes = m_bheader;
			signature = new String(bytes, 0, 8);
			formatrevision = bytes[8] & 0xff;
			number_of_track = bytes[9] & 0xff;
			number_of_side = bytes[10] & 0xff;
			track_encoding = bytes[11] & 0xff;
			bitRate = Utilities.getInt16rev(bytes, 12);
			floppyRPM = Utilities.getInt16rev(bytes, 14);
			floppyinterfacemode = bytes[16] & 0xff;
			dnu = bytes[17] & 0xff;
			track_list_offset = Utilities.getInt16rev(bytes, 18);
			write_allowed = (bytes[20]!=((byte)0x00));
			single_step = (bytes[21]!=((byte)0x00));
			track0s0_altencoding = (bytes[22]==((byte)0x00));
			track0s0_encoding = bytes[23] & 0xff;
			track0s1_altencoding = (bytes[24]==((byte)0x00));
			track0s1_encoding = bytes[25] & 0xff;
			
			m_nHeads = number_of_side;
		}		
		
		// No localized because this is only used in a commented output operation
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Revision = ").append(formatrevision);
			sb.append("\nTracks = ").append(number_of_track);
			sb.append("\nSides = ").append(number_of_side);
			sb.append("\nEncoding = ");
			if (track_encoding < 4) sb.append(encoding[track_encoding]);
			else sb.append(encoding[UNKNOWN_ENCODING]);
			sb.append("\nBit rate = ").append(bitRate).append(" kbps");
			if (floppyRPM != 0) sb.append("\nFloppy RPM = ").append(floppyRPM);
			sb.append("\nFloppy interface mode = ");
			if (floppyinterfacemode != 0xfe) sb.append(mode[floppyinterfacemode]);
			else sb.append(mode[0x0e]);
			sb.append("\nTrack tables at offset = ").append(track_list_offset*512);
			sb.append("\nWrite allowed = ").append(write_allowed);
			sb.append("\nSingle steps = ").append(single_step);
			if (track0s0_altencoding) sb.append("\nAlternate encoding for track 0 side 0 = ").append(track0s0_encoding);
			if (track0s1_altencoding) sb.append("\nAlternate encoding for track 0 side 1 = ").append(track0s1_encoding);
			return sb.toString();			
		}
		
		// Cannot use static in inner class...
		public HFEHeader(int tracks, int sides, int density) {
			byte[] newheader = new byte[512];
			for (int i=0; i < 8; i++) newheader[i] = (byte)("HXCPICFE".charAt(i)); 
			newheader[8] = (byte)0;
			newheader[9] = (byte)tracks;
			newheader[10] = (byte)sides;
			newheader[11] = (byte)((density == FloppyFileSystem.SINGLE_DENSITY)? ISOIBM_FM_ENCODING : ISOIBM_MFM_ENCODING); 
			newheader[12] = (byte)0xfa;
			newheader[13] = (byte)0x00;
			newheader[14] = (byte)0x00;
			newheader[15] = (byte)0x00;
			newheader[16] = (byte)GENERIC_SHUGART_DD_FLOPPYMODE;
			newheader[17] = (byte)0x00;
			newheader[18] = (byte)0x01;
			newheader[19] = (byte)0x00;
			newheader[20] = (byte)0xff;  // write allowed
			newheader[21] = (byte)0xff;  // single step
			newheader[22] = (byte)0xff;
			newheader[23] = (byte)0xff;
			newheader[24] = (byte)0xff;
			newheader[25] = (byte)0xff;
			for (int i=26; i < 512; i++) newheader[i] = (byte)0xff;
			m_bheader = newheader;
			readHeader();
		}
		
		public byte[] getBytes() {
			return m_bheader;
		}
	}
	
	HFEHeader m_header;	

	/** Codec for reading / writing HFE */
	class HFECodec extends FormatCodec {
		
		boolean m_mfm = false;
		int m_codeRate;
		int m_lastDataBit;
		int m_currentHead;
		boolean m_first = true;		
		
		HFECodec(String sFile, boolean mfm, int coderate) {
			super(sFile, false);
			m_mfm = mfm;
			m_codeRate = coderate;
		}		
		
		// Called when a file is opened
		HFECodec(String sFile) {
			// FIXME: mfm, coderate?
			super(sFile, false);
		}
		
		/** From the sequence of ImageSectors, produce a buffer content. */
		void encodeBuffer() {
			m_lastDataBit = 0;
			m_currentHead = 0;
			// We already have the track in the buffer; just need to write
			// the sector contents into them
			for (ImageSector sect : m_buffsector) {
				m_nPositionInBuffer = sect.getImagePosition();
				m_currentHead = sect.getLocation().head;
				writeBits(sect.getData());
				writeBits(sect.getCRCBytes());
			}
		}
		
		/** Produces a list of ImageSectors. Delivers the sectors per track. */
		int decodeBuffer() {
			m_buffsector.clear(); 
			int count = 0;
			int initcrc = 0;
			byte[] abyHeader = new byte[4];
			byte[] abySector;

			// Do it for both heads
			for (int head = 0; head < 2; head++) {
				m_currentHead = head;
				m_positionInBuffer = 0;
			
				while (m_positionInBuffer < m_abyBuffer.length*8) {
					boolean foundIDAM = searchIDAM();
					if (foundIDAM) {
						// For MFM we have an ident byte to consume
						if (m_mfm) {
							initcrc = 0xb230;
							readBits(8);
						}
						else initcrc = 0xef21;
						// Read the header
						abyHeader[0] = (byte)readBits(8);
						abyHeader[1] = (byte)readBits(8);
						abyHeader[2] = (byte)readBits(8);
						abyHeader[3] = (byte)readBits(8);
						// and the header CRC
						int crch = readBits(16);
						// and check against the calculated CRC
						int crcc = Utilities.crc16_get(abyHeader, 0, 4, initcrc);
						if (crch != crcc) System.out.println(String.format(TIImageTool.langstr("BadHeaderCRC"), abyHeader[0], abyHeader[1], abyHeader[2], Utilities.toHex(crcc, 4), Utilities.toHex(crch, 4)));
						// FIXME: We should abandon this sector when the CRC is bad
						
						boolean foundDAM = searchDAM();
						if (foundDAM) {
							int mark = m_value;
							if (m_mfm) {
								initcrc = (mark==0xfb)? 0xe295 : 0xd2f6;   // f8 is the "deleted" mark
							}
							else {
								initcrc = (mark==0xfb)? 0xbf84 : 0x8fe7;
							}
							int pos = m_positionInBuffer; // right after the DAM, first cell of the contents
							
							// Create a new ImageSector
							abySector = new byte[ImageFormat.SECTOR_LENGTH];
							for (int i=0; i < ImageFormat.SECTOR_LENGTH; i++) {
								abySector[i] = (byte)readBits(8);
							}
							// Read the CRC
							int crcd = readBits(16);
							ImageSector sect = new ImageSector(new Location(abyHeader), abySector, (byte)mark, m_mfm, pos);
							// Check against the calculated value						
							// FIXME: Sector number
							if (crcd != sect.getCRC()) System.out.println(String.format(TIImageTool.langstr("BadDataCRC"), 9999, Utilities.toHex(sect.getCRC(),4), Utilities.toHex(crcd, 4)));
							m_buffsector.add(sect);
						}
					}
				}
				// How many sectors did we find?
				// Only count for head 0
				if (count==0) count = m_buffsector.size();
			}
			return count;
		}
		
		/** Creates a new empty buffer. For HFE this means a cylinder. */ 
		void createEmptyBuffer(int buffernum) {
			int cylinder = buffernum;
			int[] gaps;
			System.out.println("Create cyl = " + cylinder);
			int[] gapsd = {  3, 26, 11, 28, 227, 6 };
			int[] gapdd18 = { 80, 50, 22, 13, 136, 12 };
			int[] gapdd16 = { 80, 50, 22, 50, 206, 12 };
			m_abyBuffer = new byte[getFullTrackLength(m_bufferlen[cylinder])];
			
			FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
			int density = ffs.getDensity();
			if (density == FloppyFileSystem.SINGLE_DENSITY) gaps = gapsd;
			else {
				if (density == FloppyFileSystem.DOUBLE_DENSITY_16) gaps = gapdd16;
				else gaps = gapdd18;
			}	
			// Create both tracks of the cylinder
			formatTrack(cylinder, 0, ffs.getSectorsPerTrack(), density, gaps);
			formatTrack(cylinder, 1, ffs.getSectorsPerTrack(), density, gaps);
		}
		
		void writeBits(int value, int number) {
			int mask = 1 << (number-1);
			for (int i=0; i < number; i++) {
				writeNextBit((value & mask)!=0);
				mask >>= 1;
			}
		}
		
		void writeBits(byte[] seq) {
			for (int i=0; i < seq.length; i++) {
				writeBits(seq[i], 8);
			}
		}
		
		private void writeNextBit(boolean databit) {
			boolean clock = !m_mfm || (m_lastDataBit==0 && databit==false);
			
			writeNextCell(clock);
			if (m_codeRate == 4) writeNextCell(false);  // ignore the next cell
			writeNextCell(databit);   // Data bit
			m_lastDataBit = databit? 1 : 0;
			if (m_codeRate == 4) writeNextCell(false);  // ignore the next cell
		}
		
		private void writeNextCell(boolean set) {
			int position = m_positionInBuffer / 8; 
			if (position >= m_abyBuffer.length) position =  m_abyBuffer.length - 1;
			
			int block = position / 256;
			int offset = position % 256;		
			int actPosition = block*512 + m_currentHead * 256 + offset;
			
			int bit = 1 << (m_positionInBuffer % 8);
			
			if (set) m_abyBuffer[actPosition] |= bit;
			else m_abyBuffer[actPosition] &= ~bit;
			m_positionInBuffer++;
		}
		
		/** Get the number of bits from the cell level sequence. 
		*/
		private int readBits(int number) {
			m_value = 0;
			for (int i=0; i < number; i++) {
				m_value <<= 1;
				m_value |= getNextBit();
			}
			return m_value;
		}
		
		/** Get the next bit from the cell level sequence.
		*/
		private int getNextBit() {
			int value = 0;
			// FM images are shifted by one cell position
			if (m_first && (m_codeRate == 4)) getNextCell(); 
			int level = getNextCell();
			m_shiftRegister = ((m_shiftRegister << 1) | level) & 0xffff;
			if (m_codeRate == 4) getNextCell();  // ignore the next cell
			value = getNextCell(); 
			m_shiftRegister = ((m_shiftRegister << 1) | value) & 0xffff;
			if (m_codeRate == 4) getNextCell();  // ignore the next cell
			return value;
		}
		
		/** Gets the next cell. This depends on the currently selected head. */
		private int getNextCell() {
			if ((m_positionInBuffer % 8)==0) {
				int position = m_positionInBuffer / 8; 
				// Consider the interleave of both sides every 0x100 bytes
				// 0000   0100    0200    0300    0400   0500   ...
				// C0H0   C0H1    C1H0    C1H1    C2H0   C2H1   ...
				
				int block = position / 256;
				int offset = position % 256;		
				int actPosition = block*512 + m_currentHead * 256 + offset;
				if (actPosition >= m_abyBuffer.length) {
					m_positionInBuffer = m_cells;
					m_currentGroup = 0;
				}
				else {
					m_currentGroup = (byte)((m_abyBuffer[actPosition])&0xff);
				}
			}
			int value = m_currentGroup & 1;
			m_currentGroup >>= 1;
			//		System.out.println("current cellpos = " + m_positionInBuffer + ", value = " + value);
			m_positionInBuffer++;
			m_first = false;
			return value; 		
		}
		
		private boolean searchIDAM() {
			//		System.out.println("Searching next IDAM");
			int marks = 0;
			int am = 0;
			
			if (m_mfm) {
				marks = 3;
				am = 0x4489; // A1
			}
			else {
				marks = 1;
				am = 0xf57e; // FE
			}
			
			while (m_positionInBuffer < m_cells && marks != 0) {
				getNextBit();
				if (m_shiftRegister == am) marks--;
			}
			//		System.out.println("Next IDAM found: " + (marks==0));		
			return (marks == 0);
		}
	
		private boolean searchDAM() {
			m_value = 0;
			int marks = 1;
			if (m_mfm) {
				marks = 3;
				while (m_positionInBuffer < m_cells && marks != 0) {
					getNextBit();
					if (m_shiftRegister == 0x4489) marks--;  // A1
					if (marks==0) m_value = readBits(8);  // read the ident field
				}
			}
			else {
				while (m_positionInBuffer < m_cells) {
					m_value = ((m_value << 1 ) | getNextBit())&0xff;
					if ((m_shiftRegister & 0xfffa) == 0xf56a) return true;
				}
			}
			return (marks == 0);
		}
		
		private void formatTrack(int cylinder, int head, int seccount, int density, int[] gap) {
			System.out.println("Creating new track on cylinder " + cylinder + ", head " + head);
			m_lastDataBit = 0;
			int gapval = 0x4e;
			m_positionInBuffer = 0;
			
			int sync = gap[5];
			
			// Start number
			int sector = 0;
			int initcrc = 0;
			
			byte[] abyEmpty = getFillPattern();
			
			byte[] abyHeader = new byte[4];
			m_currentHead = head;
			
			if (density == FloppyFileSystem.SINGLE_DENSITY) {
				if (m_codeRate==4) writeNextCell(false); // Write a first empty cell		
				gapval = 0xff;
				sector = (cylinder * 6) % 9;
			}
			
			// Gap 0
			for (int i=0; i < gap[0]; i++) writeBits(gapval,8);
			// Sync
			for (int i=0; i < sync; i++) writeBits(0x00,8);
			
			if (density == FloppyFileSystem.SINGLE_DENSITY) {
				writePattern(0xf77a); // write IXAM
			}
			else {
				writePattern(0x5224); 
				writePattern(0x5224); 
				writePattern(0x5224);
				writeBits(0xfc, 8);
			}
			
			// Gap 1
			for (int i=0; i < gap[1]; i++) writeBits(gapval,8);
			
			// Write all sectors
			for (int i=0; i < seccount; i++) {
				// Write sector		
				// Sync 
				for (int k=0; k < sync; k++) writeBits(0x00,8);
				
				if (density == FloppyFileSystem.SINGLE_DENSITY) {			
					// IDAM
					writePattern(0xf57e);
					initcrc = 0xef21;
				}
				else {
					writePattern(0x4489); 
					writePattern(0x4489); 
					writePattern(0x4489); 
					writeBits(0xfe, 8);
					initcrc = 0xb230;
				}
				
				System.out.println("Write c=" + cylinder + ", h=" + head + ", s=" + sector + ", track len = " + m_abyBuffer.length);
				abyHeader[0] = (byte)cylinder;
				abyHeader[1] = (byte)head;
				abyHeader[2] = (byte)sector;
				abyHeader[3] = (byte)0x01;					
				for (byte b : abyHeader) writeBits(b, 8);
				int crcc = Utilities.crc16_get(abyHeader, 0, 4, initcrc);
				writeBits(crcc, 16);
				// Gap2
				for (int k=0; k < gap[2]; k++) writeBits(gapval, 8);
				// Sync
				for (int k=0; k < sync; k++) writeBits(0x00, 8);
				
				// DAM
				if (density == FloppyFileSystem.SINGLE_DENSITY) {
					writePattern(0xf56f); 
				}
				else {
					writePattern(0x4489); 
					writePattern(0x4489); 
					writePattern(0x4489); 
					writeBits(0xfb, 8);
				}
				
				// Sector content
				// All sectors are filled with the empty pattern
				for (int k=0; k < 256; k++) writeBits(abyEmpty[k], 8);
				initcrc = (m_header.track_encoding==HFEHeader.ISOIBM_MFM_ENCODING)? 0xe295 : 0xbf84;
				
				int crcd = Utilities.crc16_get(abyEmpty, 0, 256, initcrc);
				
				// CRC
				writeBits(crcd, 16);
				
				// GAP3
				for (int k=0; k < gap[3]; k++) writeBits(gapval,8);
				
				// Next sector
				if (density == FloppyFileSystem.SINGLE_DENSITY)
					sector = (sector + 7) % 9;
				else
					// FIXME: 16-sector track
					sector = (sector + 11) % 18;
			}	
			// GAP4
			System.out.println("Write GAP 4 at " + m_positionInBuffer);
			for (int k=0; k < gap[4]; k++) {
				// System.out.println("k = " + k); 
				writeBits(gapval,8);
			}
		}
	}
	
	byte m_currentGroup;
	
//	byte[] m_bCellTrack;
	
	byte[] m_abyBufferLUT;

	int m_shiftRegister;
	int m_value;
	int m_lastDataBit;
		
	boolean m_first = true;
	boolean m_bHeaderWritten = false;
	
	static final int ENDOFTRACK = -1;
	
	// There is no RAF.getName
	HFEFormat(RandomAccessFile imagefile, String sImageName) throws IOException, ImageException {
		super(imagefile, sImageName);
		m_bHeaderWritten = true;
		m_codec = new HFECodec(sImageName);
		writeThrough(false);
	}
	
	HFEFormat(File file) throws FileNotFoundException {
		super(file);
		m_bHeaderWritten = true;
		m_codec = new HFECodec(file.getAbsolutePath());
		writeThrough(false);
	}

	HFEFormat(RandomAccessFile imagefile, String sImageName, TFileSystem fs) throws IOException, ImageException {
		super(imagefile, sImageName, fs);
		m_bHeaderWritten = false;
		m_codec = new HFECodec(sImageName);
		writeThrough(false);
	}
	
	HFEFormat() {
		m_bHeaderWritten = false;
		
		// FIXME: No codec
		// m_codec = new HFECodec();
		writeThrough(false);
	}
	
	public String getHeaderInformation() {
		return m_header.toString();
	}
	
	public String getDumpFormatName() {
		return TIImageTool.langstr("HFEImage");
	}
	
	@Override
	int getFormatType() {
		return FLOPPY_FORMAT; 
	}

	@Override
	int getImageType() {
		return HFE; 
	}
	
	@Override
	int getFullTrackLength(int nTrackLength) {
		return (nTrackLength+0x200) & 0xfe00;
	}

	/** The track is available when the cylinder was loaded from disk.
	   	When created in memory, both tracks are created, so only the cylinder
	   	matters.
	*/
	@Override
	boolean bufferLoaded(int cyl, int head) {
		return (m_currentCylinder == cyl);
	}
	
	@Override
	boolean bufferNeedsFlush(int cyl, int head) {
		return m_currentCylinder != cyl;
	}
	
	static int vote(RandomAccessFile fileSystem) throws IOException {
		int nVote = 0;
		fileSystem.seek(0);
		byte[] sig = new byte[8];
		fileSystem.readFully(sig);
		String sSignature = new String(sig);
		if (sSignature.equals("HXCPICFE")) nVote=100;
		return nVote;
	}
		
	@Override
	TFileSystem determineFileSystem(RandomAccessFile rafile) throws IOException, ImageException {
		return null;
	}
	
/*	// Called when opening a file
	@Override	
	void setGeometryAndCodec(boolean bSpecial) throws IOException, ImageException {
		m_ImageFile.seek(0);
		byte[] bheader = new byte[512];
		m_ImageFile.readFully(bheader);
		m_header = new HFEHeader(bheader);
		System.out.println(m_header);
		m_nCylinders = m_header.number_of_track;
		
		m_abyBufferLUT = new byte[m_nCylinders * 4];
		m_ImageFile.seek(m_header.track_list_offset*512);
		// Read the LUT
		m_ImageFile.readFully(m_abyBufferLUT);
	
		// setupBuffers();
	}

	// Newly created.
	@Override	
	void setGeometryAndCodec(String sImageName, TFileSystem fs, boolean bInitial) {
		setBasicParams((FloppyFileSystem)fs);
		// Create a new LUT
		m_header = new HFEHeader((FloppyFileSystem)fs);
		m_codeRate = m_header.bitRate / 125;
		if (m_header.track_encoding==HFEHeader.ISOIBM_FM_ENCODING) m_codeRate <<=1; 
		m_abyBufferLUT = createLookupTable();

		setupBuffers(sImageName, bInitial);
	}
	*/
	
	void createBuffer(int cylinder, int head, int track) {
		System.out.println("Create track, cyl = " + cylinder + ", head = " + head);
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
//		int cylinder, int head, int seccount, int density, int[] gap

//		int[] gapsd = { 16, 6, 11, 45, 103+9 };  // with padding
//		int[] gapdd = { 32, 12, 22, 24, 68+16 };

		// gap0, gap1, gap2, gap3, gap4, Sync }
		int[] gapsd = {  3, 26, 11, 28, 227, 6 };
		int[] gapdd18 = { 80, 50, 22, 13, 136, 12 };
		int[] gapdd16 = { 80, 50, 22, 50, 206, 12 };

		int[] gaps = null;
		int density = ffs.getDensity();

		// We have to allocate a new array if the track is from another
		// cylinder
		if (m_currentCylinder != cylinder)	
			m_abyBuffer = new byte[getFullTrackLength(m_bufferlen1[track])];
				
		// Track allocated by locateInBuffer
		System.out.println("Track length = " + m_abyBuffer.length);
		
		if (density == FloppyFileSystem.SINGLE_DENSITY) gaps = gapsd;
		else {
			if (density == FloppyFileSystem.DOUBLE_DENSITY_16) gaps = gapdd16;
			else gaps = gapdd18;
		}
		
		// Create both tracks of the cylinder
		formatTrack(cylinder, 0, ffs.getSectorsPerTrack(), density, gaps);
		formatTrack(cylinder, 1, ffs.getSectorsPerTrack(), density, gaps);

		System.out.println("createBuffer done");
	}

	
	/*
	void setupBuffers() {
		// FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		m_encoding = (m_header.track_encoding==HFEHeader.ISOIBM_FM_ENCODING)? FM : MFM; 
		// FM/125 -> 2
		// MFM/250 -> 2
		// FM/250 -> 4
		m_codeRate = m_header.bitRate / 125;
		if (m_encoding == FM) m_codeRate <<=1; 

		// FIXME: NPX on loading
		// ffs.setCountedSectors(NONE);
		m_currentCylinder = NONE;
		m_currentHead = NONE;
		m_nDensity = (m_encoding == FM)? FloppyFileSystem.SINGLE_DENSITY : FloppyFileSystem.DOUBLE_DENSITY;
		
		m_bufferpos = new int[m_nCylinders*2];
		m_bufferlen1 = new int[m_nCylinders*2];
		for (int i=0; i < m_nCylinders; i++) {
			int opposite = 2*m_nCylinders - i - 1;
			m_bufferpos[i] = Utilities.getInt16rev(m_abyBufferLUT, i*4) * 512;
			m_bufferpos[opposite] = m_bufferpos[i]; 
			m_bufferlen1[i] = Utilities.getInt16rev(m_abyBufferLUT, i*4+2);
			m_bufferlen1[opposite] = m_bufferlen1[i];		
			System.out.println("Track " + i + " at pos " + m_bufferpos[i] + ", len " +  m_bufferlen1[i]);
		}
	}
*/
	/** Write a header. */
	@Override
	void prepareImageFile() throws FileNotFoundException, IOException {
		if (m_bHeaderWritten) return;
		
		m_ImageFile = new RandomAccessFile(m_sImageName, "rw");		
		m_ImageFile.seek(0);
		m_ImageFile.write(m_header.getBytes());  // 512 bytes
		m_ImageFile.seek(0x200);
		m_ImageFile.write(m_abyBufferLUT);
		m_ImageFile = new RandomAccessFile(m_sImageName, "r");		
		
		m_bHeaderWritten = true;
	}
	
	/** Loads a cylinder. In HFEFormat, both tracks of the cylinder must be 
		loaded, as saving and loading is always done on the base of a whole
		cylinder. The sectors of both tracks are stored in the read cache
		(m_buffsector).
	    @param nSectorNumber Sector that is about to be read.
	    @return Index of the sector in the sector cache (or NONE)
	*/
	@Override
	int loadBuffer(Location loc) throws IOException, ImageException {
		m_first = true;  // First bit ever loaded of this cylinder?
		ArrayList<Sector> sectors = new ArrayList<Sector>();

		int nSaveHead = m_currentHead;

		m_currentHead = 0;
		int savepos = m_positionInBuffer;
		
		int a = loadTrackOfCylinder(loc, sectors);
		int listlena = sectors.size();

		m_currentHead = 1;
		m_positionInBuffer = savepos;
		int b = loadTrackOfCylinder(loc, sectors) + listlena;

		m_currentHead = nSaveHead;

		m_buffsector = sectors.toArray(new Sector[0]);
		
		if (a != NONE) return a;
		if (b != NONE) return b;
		return NONE;
	}
	
	private int loadTrackOfCylinder(Location loc, ArrayList<Sector> sectors) {
		int secindex = NONE;
		
		int count=0;
		byte[] bSector = null;
		byte[] abyHeader = new byte[4];
		int initcrc = 0;

		// Debug
		/*if (m_currentCylinder==1) {
			byte[] btrack = new byte[8192];
			int i=0;
			while (m_positionInBuffer < m_bufferlen1[loc.cylinder] * 4) {
				btrack[i++] = (byte)readBits(8);
			}
			System.out.println(Utilities.hexdump(0, 0, btrack, i, false));
			m_positionInBuffer = 0;
			m_first = true;
		}*/

		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;

		while (m_positionInBuffer < m_bufferlen1[loc.cylinder] * 4) {
			if (nextIDAMFound()) {
				// For MFM we have an ident byte to consume
				if (m_header.track_encoding==HFEHeader.ISOIBM_MFM_ENCODING) {
					initcrc = 0xb230;
					readBits(8);
				}
				else initcrc = 0xef21;
				abyHeader[0] = (byte)readBits(8);
				abyHeader[1] = (byte)readBits(8);
				abyHeader[2] = (byte)readBits(8);
				abyHeader[3] = (byte)readBits(8);
				int secnum = ffs.chsToLba(abyHeader[0], abyHeader[1], abyHeader[2]);
				// System.out.println("Found sector (" + abyHeader[0] + "," + abyHeader[1] + "," + abyHeader[1] + ") = " + secnum);  
				// If the header matches, keep that index for return
				if ((abyHeader[2]&0xff) == loc.sector) secindex = count;
				
				int crch = readBits(16);
				int crcc = Utilities.crc16_get(abyHeader, 0, 4, initcrc);
				if (crch != crcc) System.out.println(String.format(TIImageTool.langstr("BadHeaderCRC"), abyHeader[0], abyHeader[1], abyHeader[2], Utilities.toHex(crcc, 4), Utilities.toHex(crch, 4)));
				
//				System.out.println("Header: (C="+ cyl + ",H=" + head + ",S=" + sector + ", cellpos=" + m_positionInBuffer + ")");
				if (nextDAMFound()) {
					int mark = m_value;  // the DAM for FM and MFM
					if (m_header.track_encoding==HFEHeader.ISOIBM_MFM_ENCODING) {
						initcrc = (mark==0xfb)? 0xe295 : 0xd2f6;   // f8 is the "deleted" mark
					}
					else {
						initcrc = (mark==0xfb)? 0xbf84 : 0x8fe7;
					}
					
					int pos = m_positionInBuffer;
					bSector = new byte[256];
					for (int i=0; i < 256; i++) {
						bSector[i] = (byte)readBits(8);
					}
					int crcd = readBits(16);
					Sector sect = new Sector(secnum, bSector);
					sect.setTrackPosition(pos, initcrc, mark);
					sectors.add(sect);
					// System.out.println("Sector " + sect.getNumber()  + ": Data CRC = " + Utilities.toHex(sect.getCrc(),4) + " (expected " +  Utilities.toHex(crcd, 4) + ")");
					if (crcd != sect.getCrc()) System.out.println(String.format(TIImageTool.langstr("BadDataCRC"), sect.getNumber(), Utilities.toHex(sect.getCrc(),4), Utilities.toHex(crcd, 4)));
					// System.out.println("loaded sector " + sect.getNumber() + " at track " + loc.track);
					// System.out.println(sect);
					count++;
				}
			}	
		}
//		System.out.println("Found " + count + " sectors");
		if (ffs.getCountedSectors()==NONE) ffs.setCountedSectors(count);	
		
		// Now we know the last sector	
		if (m_nTotalSectors == 0) m_nTotalSectors = ffs.getCountedSectors() * m_nCylinders * m_nHeads;
		
		return secindex;
	}
	
	/** Called from HFEReader. Similar to the method above, just not trying to find sectors. */
	public byte[] getTrackBytes(int cylinder, int head) throws IOException, ImageException {
		Location loc = new Location(cylinder, head, 0, 0);

		m_currentCylinder = loc.cylinder;
		m_currentHead = loc.head;

		m_abyBuffer = new byte[m_bufferlen1[loc.track]];
		m_ImageFile.seek(m_bufferpos[loc.track]);
		m_ImageFile.readFully(m_abyBuffer);
		
		m_cells = m_abyBuffer.length * 4;  // All bits for either head
			
		// Reset to start
		m_positionInBuffer = 0;
		m_first = true;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (m_positionInBuffer < m_bufferlen1[loc.cylinder] * 4) {
			baos.write(readBits(8));
		}

		return baos.toByteArray();
	}
	
	/** The HFE format has its own way of reading sectors. We return
	    the cached sector.
	*/
/*	@Override
	public Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		if (nSectorNumber > 10000) throw new ImageException(String.format(TIImageTool.langstr("BadSectorNumber"), nSectorNumber)); 
		int secindex = locateInBuffer(nSectorNumber);
		if (secindex != NONE) {
			return m_buffsector[secindex];
		}
		else throw new ImageException(String.format(TIImageTool.langstr("SectorNotFound"), nSectorNumber));
	}
*/	
	/** Write a single sector to the buffer. Load the buffer if required. */
	@Override
	public void writeToBuffer(Sector sect) throws IOException, ImageException {
		// Write the sector into m_buffsector. This may lead to flushing the
		// current buffer and loading the buffer containing this sector
		writeSector(sect);
		
		// Write the sector into the buffer
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		int secsPerSide = ffs.getCountedSectors() * ffs.getTracksPerSide();
		m_currentHead = (sect.getNumber() < secsPerSide)? 0 : 1;
		
		m_bBufferChanged = true;
		byte[] abySectorContent = sect.getBytes();
		m_lastDataBit = (sect.getMark()) & 1;
		setCellPosition(sect.getPosition());
		for (int j=0; j < 256; j++) {
			writeBits(abySectorContent[j], 8);
		}
		writeBits(sect.getCrc(), 16);
		sect.clean();
	}
		
	/** Save all sectors to the image file. Unlike the other 
		formats, HFEFormat must write all sectors of the same cylinder
		(tracks of both heads) before changing to a new cylinder.
	*/
	@Override
	void saveImage() throws IOException, ImageException {
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		System.out.println("HFEImage.saveImage");
		System.out.println("cyl=" + ffs.getTracksPerSide() + ", h=" + ffs.getHeads() + ", sect=" + ffs.getSectors());

		for (int c=0; c < ffs.getTracksPerSide(); c++) {
			// System.out.println("c=" + c);
			for (int h=0; h < ffs.getHeads(); h++) {
				// System.out.println("h=" + h);
				for (int s=0; s < ffs.getSectors(); s++) {
					int seclba = ffs.chsToLba(c, h, s);
					Sector sect = m_cache.read(seclba, true);
					writeToBuffer(sect);
				}
			}
		}
		m_cache.setCommitted(true);
		setCommitted(true);
		m_cache.wipe();
		System.out.println("done");
	}

	/** Write the track back into the image file.
	*/
	public void flush() throws IOException {
		// boolean trackchanged = false;
		if (m_currentCylinder == NONE) return;
		
		System.out.println("Flush cylinder " + m_currentCylinder);
/*
		int initcrc = (m_header.track_encoding==HFEHeader.ISOIBM_MFM_ENCODING)? 0xe295 : 0xbf84;
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;

		// m_buffsector contains all sectors of this cylinder
		// (We need a read cache for the track because the image file
		// contains cells, not bits)
				
		// for (int i=0; i < m_buffsector.length; i++) {
			// // copy the changed contents into the read cache
			// Sector sect = m_cache.read(m_buffsector[i].getNumber());
			// if (sect != null) {
				// // This automatically adjusts the CRC
				// m_buffsector[i].setData(sect.getBytes());
			// }
		// } 
		
		// Now write all changed sectors into the cylinder image
		// System.out.println("Writing back " + m_buffsector.length + " sectors");
		int secsPerSide = ffs.getCountedSectors() * ffs.getTracksPerSide();
		
		// TODO: Check with SectorCache
		// Why is the last cylinder written again on exit?
		for (int i=0; i < m_buffsector.length; i++) {
			if (m_buffsector[i].changed()) {
				System.out.println("Sector " + m_buffsector[i].getNumber() + " changed");
				m_currentHead = (m_buffsector[i].getNumber() < secsPerSide)? 0 : 1;
				// System.out.println("Current head = " + m_currentHead);
				trackchanged = true;
				// Write the cell pattern into the image
				byte[] bSectorContent = m_buffsector[i].getBytes();
				// if (m_buffsector[i].getNumber()==0) System.out.println(Utilities.hexdump(bSectorContent));
				m_lastDataBit = (m_buffsector[i].getMark()) & 1;
				setCellPosition(m_buffsector[i].getPosition());
				for (int j=0; j < 256; j++) {
					writeBits(bSectorContent[j], 8);
				}
				writeBits(m_buffsector[i].getCrc(), 16);
				m_buffsector[i].clean();
			}
		}*/
		
		if (m_bBufferChanged) {
			// Write back the whole cylinder (both sides; leave something to optimize)
			System.out.println("Write back cylinder " + m_currentCylinder + " at position " + Utilities.toHex(m_bufferpos[m_currentCylinder], 6));
			m_ImageFile = new RandomAccessFile(m_sImageName, "rw");		
			m_ImageFile.seek(m_bufferpos[m_currentCylinder]);
			m_ImageFile.write(m_abyBuffer);
			m_ImageFile = new RandomAccessFile(m_sImageName, "r");		
		}
		else System.out.println("No changes");
	}
	
	@Override
	public void reopenForWrite() throws IOException {
		// Don't do anything here
	}
	
	@Override
	public void reopenForRead() throws IOException {
		// Don't do anything here
	}
	
	/** Get the number of bits from the cell level sequence. 
	*/
	private int readBits(int number) {
		m_value = 0;
		for (int i=0; i < number; i++) {
			m_value <<= 1;
			m_value |= getNextBit();
		}
		return m_value;
	}
	
	/** Write the number of bits into the cell level sequence.
	*/
	private void writeBits(int value, int number) {
		int mask = 1 << (number-1);
		for (int i=0; i < number; i++) {
			writeNextBit((value & mask)!=0);
			mask >>= 1;
		}
	}
	
	/** Get the next bit from the cell level sequence.
	*/
	private int getNextBit() {
		int value = 0;
		// FM images are shifted by one cell position
		if (m_first && (m_codeRate == 4)) getNextCell(); 
		int level = getNextCell();
		m_shiftRegister = ((m_shiftRegister << 1) | level) & 0xffff;
		if (m_codeRate == 4) getNextCell();  // ignore the next cell
		value = getNextCell(); 
		m_shiftRegister = ((m_shiftRegister << 1) | value) & 0xffff;
		if (m_codeRate == 4) getNextCell();  // ignore the next cell
		return value;
	}
	
	private boolean nextIDAMFound() {
//		System.out.println("Searching next IDAM");
		int marks = 0;
		int am = 0;
		
		if (m_header.track_encoding==HFEHeader.ISOIBM_FM_ENCODING) {
			marks = 1;
			am = 0xf57e;
		}
		else {
			marks = 3;
			am = 0x4489;
		}
			
		while (m_positionInBuffer < m_cells && marks != 0) {
			getNextBit();
			if (m_shiftRegister == am) marks--;
		}
//		System.out.println("Next IDAM found: " + (marks==0));
		
		return (marks == 0);
	}
	
	private boolean nextDAMFound() {
		m_value = 0;
		int marks = 1;
		if (m_header.track_encoding==HFEHeader.ISOIBM_FM_ENCODING) {
			while (m_positionInBuffer < m_cells) {
				m_value = ((m_value << 1 ) | getNextBit())&0xff;
				if ((m_shiftRegister & 0xfffa) == 0xf56a) return true;
			}
		}
		else {
			marks = 3;
			while (m_positionInBuffer < m_cells && marks != 0) {
				getNextBit();
				if (m_shiftRegister == 0x4489) marks--;
				if (marks==0) m_value = readBits(8);  // read the ident field
			}
		}
		
		return (marks == 0);
	}

	/** Gets the next cell. This depends on the currently selected head. */
	private int getNextCell() {
		if (true) throw new NullPointerException();
		if ((m_positionInBuffer % 8)==0) {
			int position = m_positionInBuffer / 8; 
			// Consider the interleave of both sides every 0x100 bytes
			// 0000   0100    0200    0300    0400   0500   ...
			// C0H0   C0H1    C1H0    C1H1    C2H0   C2H1   ...
			
			int block = position / 256;
			int offset = position % 256;		
			int actPosition = block*512 + m_currentHead * 256 + offset;
			if (actPosition >= m_abyBuffer.length) {
				m_positionInBuffer = m_cells;
				m_currentGroup = 0;
			}
			else {
				m_currentGroup = (byte)((m_abyBuffer[actPosition])&0xff);
			}
		}
		int value = m_currentGroup & 1;
		m_currentGroup >>= 1;
//		System.out.println("current cellpos = " + m_positionInBuffer + ", value = " + value);
		m_positionInBuffer++;
		m_first = false;
		return value; 		
	}
	
	/** Called from flush. */
	private void setCellPosition(int pos) {
		int position = pos / 8; 
		// Consider the interleave of both sides every 0x100 bytes
		// 0000   0100    0200    0300    0400   0500   ...
		// C0H0   C0H1    C1H0    C1H1    C2H0   C2H1   ...
		
		int block = position / 256;
		int offset = position % 256;		
		int actPosition = block*512 + m_currentHead * 256 + offset;
		m_currentGroup = (byte)((m_abyBuffer[actPosition] >> (m_positionInBuffer % 8))&0xff);
		m_positionInBuffer = pos;
	}

	private void writePattern(int value) {
		for (int i=0; i < 16; i++) {
			boolean cell = ((value & 0x8000)==0x8000);
			writeNextCell(cell);
			if (m_codeRate == 4) writeNextCell(false);  // ignore the next cell
			value <<= 1;
			if ((i & 1)==1) m_lastDataBit = cell? 1 : 0;
		}
	}
		
	private void writeNextBit(boolean databit) {
		boolean clock = (m_header.track_encoding==HFEHeader.ISOIBM_FM_ENCODING) ||
				(m_lastDataBit==0 && databit==false);

		writeNextCell(clock);
		if (m_codeRate == 4) writeNextCell(false);  // ignore the next cell
		writeNextCell(databit);   // Data bit
		m_lastDataBit = databit? 1 : 0;
		if (m_codeRate == 4) writeNextCell(false);  // ignore the next cell
	}
	
	private void writeNextCell(boolean set) {
		int position = m_positionInBuffer / 8; 
		if (position >= m_abyBuffer.length) position =  m_abyBuffer.length - 1;
		
		int block = position / 256;
		int offset = position % 256;		
		int actPosition = block*512 + m_currentHead * 256 + offset;
		
		int bit = 1 << (m_positionInBuffer % 8);
		
		if (set) m_abyBuffer[actPosition] |= bit;
		else m_abyBuffer[actPosition] &= ~bit;
		m_positionInBuffer++;
	}
	
	// ===========================================================
	// Formatting
	// ===========================================================
/*

FM layout: 

Interleave=3 skew=6
Track 0: 0-7-5-3-1-8-6-4-2
Track 1: 6-4-2-0-7-5-3-1-8
Track 2: 3-1-8-6-4-2-0-7-5   

Full sector length = 014e
Full track length = 0c36

=================
0000 IXAM (FC)
0001 16*FF
+-------
|   0011 6*00
|   0017 IDAM  (FE)
|   0018 Header
|   001c CRC
|   001e 11*FF
|   0029 6*00
|   002f DAM  (FB)
|   0030 Content
|   0130 CRC
|   0132 45*FF   ...015e
+-------
+-------
|...
|   ...0bce
+-------
0bcf 77*FF
0c1c C0      (? may differ)
0c1d 25*00   (? may differ)
..0c35

(max 103*FF)

Including padding: (length = 0c40) GAP4+=10

=================

Total size:

80 bytes padding
SSSD40, DSSD40: 512 + 512 + 40*(4*2*3126 + 80)= 1004544
DSSD80: 512 + 512 + 80*(4*2*3126(0x61b0) + 80) = 2008064


MFM:

Interleave= 4 skew= 0
Track 0-39: 0-11-4-15-8-1-12-5-16-9-2-13-6-17-10-3-14-7

Full sector length = 0156
Full track length = 1870

=================
No IXAM

0000 32*4E
+-------
|   0020 12*00
|   002c IDAM A1A1A1
|   002f Ident FE
|   0030 Header
|   0034 CRC
|   0036 22*4E
|   004c 12*00
|   0058 DAM A1A1A1
|   005b Mark FB
|   005c Content
|   015c CRC
|   015e 24*4E   ...0175
+--------
+--------
| ...
|   ...182b
+--------
182c 68*4E   ... 186f

0000200: 0200 c061 3300 c061 6400 c061 9500 c061  ...a3..ad..a...a
0000210: c600 c061 f700 c061 2801 c061 5901 c061  ...a...a(..aY..a
0000220: 8a01 c061 bb01 c061 ec01 c061 1d02 c061  ...a...a...a...a
0000230: 4e02 c061 7f02 c061 b002 c061 e102 c061  N..a...a...a...a
0000240: 1203 c061 4303 c061 7403 c061 a503 c061  ...aC..at..a...a
0000250: d603 c061 0704 c061 3804 c061 6904 c061  ...a...a8..ai..a
0000260: 9a04 c061 cb04 c061 fc04 c061 2d05 c061  ...a...a...a-..a
0000270: 5e05 c061 8f05 c061 c005 c061 f105 c061  ^..a...a...a...a
0000280: 2206 c061 5306 c061 8406 c061 b506 c061  "..aS..a...a...a
0000290: e606 c061 1707 c061 4807 c061 7907 c061  ...a...aH..ay..a

(little-endian!)

Cylinder cell size = 2 heads/cylinder * 6256 bytes * 8bit/byte * 2cell/bit 
= 200192 bits * 1/8 byte/bit => 25024 bytes 

12512 = 0x61c0 -> pad by 0x40 bytes -> 0x6200 (starts at 0x0200 multiples)

Total size:  
DSDD40: 512 + 512 + 40*(2*2*6256(0x61c0) + 64) = 1004544
DSSD80: 512 + 512 + 80*(2*2*6256 + 64) = 2008064

Including padding: (length = 0c40) GAP4+=16

=================

*/
	@Override
	void formatTrack(int cylinder, int head, int seccount, int density, int[] gap) {
		System.out.println("Creating new track on cylinder " + cylinder + ", head " + head);
		m_lastDataBit = 0;
		int gapval = 0x4e;
		m_positionInBuffer = 0;
		
		int sync = gap[5];
		
		// Start number
		int sector = 0;
		int initcrc = 0;

		byte[] abyEmpty = getFillPattern();
		
		byte[] abyHeader = new byte[4];
		m_currentHead = head;
		
		if (density == FloppyFileSystem.SINGLE_DENSITY) {
			if (m_codeRate==4) writeNextCell(false); // Write a first empty cell		
			gapval = 0xff;
			sector = (cylinder * 6) % 9;
		}
	
		// Gap 0
		for (int i=0; i < gap[0]; i++) writeBits(gapval,8);
		// Sync
		for (int i=0; i < sync; i++) writeBits(0x00,8);
				
		if (density == FloppyFileSystem.SINGLE_DENSITY) {
			writePattern(0xf77a); // write IXAM
		}
		else {
			writePattern(0x5224); 
			writePattern(0x5224); 
			writePattern(0x5224);
 			writeBits(0xfc, 8);
		}

		// Gap 1
		for (int i=0; i < gap[1]; i++) writeBits(gapval,8);
		
		// Write all sectors
		for (int i=0; i < seccount; i++) {
			// Write sector		
			// Sync 
			for (int k=0; k < sync; k++) writeBits(0x00,8);

			if (density == FloppyFileSystem.SINGLE_DENSITY) {			
				// IDAM
				writePattern(0xf57e);
				initcrc = 0xef21;
			}
			else {
				writePattern(0x4489); 
				writePattern(0x4489); 
				writePattern(0x4489); 
				writeBits(0xfe, 8);
				initcrc = 0xb230;
			}
			
			System.out.println("Write c=" + cylinder + ", h=" + head + ", s=" + sector + ", track len = " + m_abyBuffer.length);
			abyHeader[0] = (byte)cylinder;
			abyHeader[1] = (byte)head;
			abyHeader[2] = (byte)sector;
			abyHeader[3] = (byte)0x01;					
			for (byte b : abyHeader) writeBits(b, 8);
			int crcc = Utilities.crc16_get(abyHeader, 0, 4, initcrc);
			writeBits(crcc, 16);
			// Gap2
			for (int k=0; k < gap[2]; k++) writeBits(gapval, 8);
			// Sync
			for (int k=0; k < sync; k++) writeBits(0x00, 8);
			
			// DAM
			if (density == FloppyFileSystem.SINGLE_DENSITY) {
				writePattern(0xf56f); 
			}
			else {
				writePattern(0x4489); 
				writePattern(0x4489); 
				writePattern(0x4489); 
				writeBits(0xfb, 8);
			}
			
			// Sector content
			// All sectors are filled with the empty pattern
			for (int k=0; k < 256; k++) writeBits(abyEmpty[k], 8);
			initcrc = (m_header.track_encoding==HFEHeader.ISOIBM_MFM_ENCODING)? 0xe295 : 0xbf84;
			
			int crcd = Utilities.crc16_get(abyEmpty, 0, 256, initcrc);
			
			// CRC
			writeBits(crcd, 16);

			// GAP3
			for (int k=0; k < gap[3]; k++) writeBits(gapval,8);

			// Next sector
			if (density == FloppyFileSystem.SINGLE_DENSITY)
				sector = (sector + 7) % 9;
			else
				sector = (sector + 11) % 18;
		}	
		// GAP4
		System.out.println("Write GAP 4 at " + m_positionInBuffer);
		for (int k=0; k < gap[4]; k++) {
			// System.out.println("k = " + k); 
			writeBits(gapval,8);
		}
	}
	
	/** Create a new LUT. */
	private byte[] createLookupTable() {
		// Lookup table
		byte[] tracklut = new byte[512];
		Arrays.fill(tracklut, (byte)0xff);

		int trackpos = 0x0400;	
		int tracklen = (m_nDensity == FloppyFileSystem.SINGLE_DENSITY)? 0x61b0 : 0x61c0;
		m_nTrackLength = (tracklen+0x200) & 0xfe00;
		
		for (int cyl = 0; cyl < m_nCylinders; cyl++) {
			// little-endian
			tracklut[cyl*4] = (byte)((trackpos >> 9) & 0xff);
			tracklut[cyl*4+1] = (byte)((trackpos >> 17) & 0xff);
			tracklut[cyl*4+2] = (byte)(tracklen & 0xff);
			tracklut[cyl*4+3] = (byte)((tracklen >> 8) & 0xff);

			trackpos += m_nTrackLength;
		}		
		return tracklut;
	}
	
	public void createEmptyImage(File newfile, int sides, int density, int tracks, int sectors, boolean format) throws FileNotFoundException, IOException {
		
		System.out.println("FIXME: Legacy function createEmptyImage");
		
		// Header
		m_header = new HFEHeader(tracks, sides, density);
		m_codeRate = m_header.bitRate / 125;
		if (m_header.track_encoding==HFEHeader.ISOIBM_FM_ENCODING) m_codeRate <<=1; 

		// Lookup table
		byte[] tracklut = new byte[512];
		for (int i=0; i < tracklut.length; i++) tracklut[i] = (byte)0xff;
		int trackpos = 0x0400;
		
		int tracklen = (density == FloppyFileSystem.SINGLE_DENSITY)? 0x61b0 : 0x61c0;
		
		byte[] abyHeader = new byte[4];
		int initcrc = 0;
		
		int[] gapsd = { 16, 6, 11, 45, 103+9 };  // with padding
		int[] gapdd = { 32, 12, 22, 24, 68+16 };
		
		m_abyBuffer = new byte[(tracklen+0x200) & 0xfe00];

		// Allocate bytes in memory. We will write the array to the file at the end.
		byte[] image = new byte[0x0400 + m_abyBuffer.length * tracks];

		if (format) {
			for (int cyl = 0; cyl < tracks; cyl++) {
				// little-endian
				tracklut[cyl*4] = (byte)((trackpos >> 9) & 0xff);
				tracklut[cyl*4+1] = (byte)((trackpos >> 17) & 0xff);
				tracklut[cyl*4+2] = (byte)(tracklen & 0xff);
				tracklut[cyl*4+3] = (byte)((tracklen >> 8) & 0xff);
				
				for (int head=0; head < 2; head++) {
					m_currentHead = head;
					formatTrack(cyl, head, sectors, density, (density == FloppyFileSystem.SINGLE_DENSITY)? gapsd : gapdd);
				}
				
				// Copy the track into the image
				System.arraycopy(m_abyBuffer, 0, image, trackpos, m_abyBuffer.length);
				trackpos += m_abyBuffer.length;
			}
			
			// Copy the header into the image
			System.arraycopy(m_header.getBytes(), 0, image, 0, 512);
			
			// Copy the LUT into the image
			System.arraycopy(tracklut, 0, image, 0x0200, tracklut.length);
		}
		
		// Write the resulting image
		FileOutputStream fos = new FileOutputStream(newfile);
		fos.write(image, 0, image.length);
		fos.close();
	}
	
	/** Determine the buffer index from the CHS position. */
	int getBufferIndex(Location loc) {
		return loc.cylinder;
	}
}