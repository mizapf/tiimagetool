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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import de.mizapf.timt.util.*;
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
       unsigned char write_allowed;             // The Floppy image is write protected?
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
   
	 This table has a size of (number_of_track*4) bytes.
	   
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
	 start at offset 0x100 nevertheless:
	   
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
	 MFM encoding: 10 01 00 10 01 01 01 00
	 Reversed order: 0010 1010 0100 1001     = 2a 49
	
     Disks that were sampled with a rate of 250 kbit/s with FM recording will
     have two sample bits per cell. The second bit should always be 0, since
     there are no changes at a rate higher than 125 kbit/s.
     
     It is possible that these intermediate bits are at even positions (0, 2, ...)
     so that the read process may have to be advanced by one sample.	
*/

public class HFEFormat extends FloppyImageFormat {
	
	HFEHeader m_header;	

	long m_cylinderpos[];
	int m_cylinderlen[];
	byte[] m_abyBufferLUT;

	
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
			this(ffs.getTracksPerSide(), ffs.getSides(), ffs.getDensity());
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
			
			// TODO: Check this?
			// m_nHeads = number_of_side;
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
		
	/** Codec for reading / writing HFE */
	class HFECodec extends FormatCodec {
		
		int m_currentSampleNumber;   // format unit length * 8
		int m_currentHead;
		boolean m_mfm;
		byte m_currentGroup;		
		int m_samplecount;
		int m_value;
		boolean m_first;		
		int m_codeRate;
		int m_shiftRegister;
		int m_lastDataBit;
		boolean m_debug = false;
		
		HFECodec(int encoding, int rate) {
			super();
			m_mfm = (encoding < HFEHeader.ISOIBM_FM_ENCODING);
			m_codeRate = rate / 125;
			if (encoding==HFEHeader.ISOIBM_FM_ENCODING) m_codeRate <<=1;
		}
		
		void decode() {
			
			int initcrc = 0;
			byte[] abyHeader = new byte[4];
			byte[] abySector;
			m_decodedSectors.clear();
			
			// System.out.println("Format unit number " + m_nCurrentFormatUnit + ", length=" + m_formatUnit.length);
			m_samplecount = m_formatUnit.length * 8;  // Each byte encodes 8 cells; all bits for either head
			// We count all cells, even for the oversampling of FM. In that case,
			// a data bit takes four cells.
						
			// System.out.println("mfm = " + m_mfm);
			// System.out.println("coderate = " + m_codeRate);
			// System.out.println("cells = " + m_samplecount);
			
			for (m_currentHead = 0; m_currentHead < 2; m_currentHead++) {
				m_first = true;
				m_currentSampleNumber = 0;
			
				while (m_currentSampleNumber < m_samplecount) {
					// System.out.println(m_currentSampleNumber);
					boolean foundIDAM = searchIDAM();
					if (foundIDAM) {
						initcrc = (m_mfm)? 0xb230 : 0xef21;

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
						// TODO: Discuss what to do when things are not quite ok
						
						boolean foundDAM = searchDAM();
						if (foundDAM) {					
							int mark = m_value;
							int pos = m_currentSampleNumber; // right after the DAM, first cell of the contents
							
							// Create a new ImageSector
							abySector = new byte[TFileSystem.SECTOR_LENGTH];
							for (int i=0; i < TFileSystem.SECTOR_LENGTH; i++) {
								abySector[i] = (byte)readBits(8);
							}
							// Read the CRC
							int crcd = readBits(16);
							// System.out.println("Found sector " + new Location(abyHeader) + ", pos=" + pos);
							// System.out.println(Utilities.hexdump(abySector));
							Location loc = new Location(abyHeader);
							ImageSector sect = new ImageSector(chsToLba(loc), abySector, (byte)mark, m_mfm, pos);
							sect.setLocation(loc);
							// Check against the calculated value						
							if (crcd != sect.getCRC()) System.out.println(String.format(TIImageTool.langstr("BadDataCRC"), chsToLba(loc), Utilities.toHex(sect.getCRC(),4), Utilities.toHex(crcd, 4)));
							// else System.out.println("Good data CRC = " + Utilities.toHex(crcd, 4));
							
							m_decodedSectors.add(sect);
						}
					}
					else {
						// TODO: What if we have different sector counts?
						if (m_nSectorsPerTrack == -1) m_nSectorsPerTrack = m_decodedSectors.size();
						break;
					}
				}
				// System.out.println("Next head " + (m_currentHead+1));
			}
			System.out.println("Found " + m_decodedSectors.size() + " sectors in format unit " + m_nCurrentFormatUnit);
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
		
		/** Get the next bit from the cell level sequence.	*/
		private int getNextBit() {
			int clock = getNextCell();
			if (clock < 0) return -1;
			
			return getNextCell(); 
		}
		
		/** Gets the next cell. This depends on the currently selected head.  
		    When we are at the end of this format unit, any further read will
		    stay at the last bit. */
		private int getNextCell() {
			int value = 0;
			if (m_first) {
				while (value == 0) {
					value = getNextSample();
					if (value < 0) return -1;
				}
				m_first = false;
			}
			else 
				value = getNextSample();

			if (m_codeRate == 4) getNextSample();
			
			m_shiftRegister = ((m_shiftRegister << 1) | value) & 0xffff;					
			return value; 		
		}
		
		private int getNextSample() {
			if ((m_currentSampleNumber % 8)==0) {			
				int position = m_currentSampleNumber / 8; 
				// Consider the interleave of both sides every 0x100 bytes
				// 0000   0100    0200    0300    0400   0500   ...
				// C0H0   C0H1    C1H0    C1H1    C2H0   C2H1   ...
				
				int block = position / 256;
				int offset = position % 256;		
				int actPosition = block*512 + m_currentHead * 256 + offset;
				if (actPosition >= m_formatUnit.length) {			
					// Go to the last position for this head
					// actPosition = m_formatUnit.length - (1-m_currentHead) * 256 - 1;
					// m_currentSampleNumber = (m_formatUnit.length / 2) * 8 - 1;
					return -1;
				}
				
				m_currentGroup = (byte)((m_formatUnit[actPosition])&0xff);
			}
			int value = m_currentGroup & 1;
			m_currentGroup >>= 1;
			// System.out.println("current cellpos = " + m_currentSampleNumber + ", value = " + value);
			m_currentSampleNumber++;
			return value;
		}
			
		private boolean searchIDAM() {
			return searchMark(false);
		}
		
		private boolean searchDAM() {
			return searchMark(true);
		}
		
		private boolean searchMark(boolean dam) {
			m_value = 0;
			int marks = 1;
			int bit = 0;
			int val = 0;
			int mark = 0;
			int mask = 0;

			if (m_mfm) {
				if (dam) {
					mark = 0xf8;
					mask = 0xfc;
				}
				else {
					mark = 0xfe;
					mask = 0xff;
				}
				
				marks = 3;
				while (marks > 0) {
					bit = getNextCell();
					if (bit < 0) return false;
					if (m_shiftRegister == 0x4489) marks--;  // A1
					
					if (marks == 0) {
						m_value = readBits(8);  // read the ident field
						if ((m_value & mask) != mark) marks = 3; // Not the expected mark
					}
				}
				// System.out.println(Utilities.toHex(m_value,4));
			}
			else {
				if (dam) {
					mark = 0xf56a;
					mask = 0xfffa;
				}
				else {
					mark = 0xf57e;
					mask = 0xffff;
				}
				
				while (marks > 0) {
					bit = getNextCell();
					if (bit < 0) return false;
					// Valid DAMs are 1111 0101 0110 1010  = f56a
					//                1111 0101 0110 1011  = f56b
					//                1111 0101 0110 1110  = f56e
					//                1111 0101 0110 1111  = f56f
					if ((m_shiftRegister & mask) == mark) marks--;
				}
				
				// Get the value
				m_value = 0;
				val = m_shiftRegister; // .d.d.d.d.d.d.d.d
				int setbit = 0x0100;
				for (int i=0; i < 8; i++) {
					if ((val & 1)!=0) m_value |= setbit;
					val >>= 2;
					m_value >>= 1;
				}
			}
			return true;
		}
		
		void encode() {
			boolean ok = true;
			for (ImageSector isect : m_decodedSectors) {
				int pos = isect.getPosition();
				m_currentSampleNumber = pos;
				m_currentHead = isect.getLocation().head;
				// m_debug = (isect.getNumber()==0);

				// System.out.println("writing sector " + isect.getNumber() + ", pos=" + pos);
				isect.startStream();
				
				for (int i=0; i < isect.getData().length + 2; i++) {
					int val = isect.nextByte();
					ok = writeBits(val, 8);
					if (!ok) break;
				}
			}
			if (!ok) System.out.println("End of track encountered during write");
		}
		
		void prepareNewFormatUnit(int number, byte[] buffer) {
			throw new NotImplementedException("HFECodec");	
		}
		
		/** Writes the last n bits of the value. Starts with the leftmost bit. */ 
		private boolean writeBits(int value, int number) {
			int mask = 1 << (number-1);
			boolean ok = true;
			for (int i=0; i < number; i++) {
				int bit = ((value & mask)!=0)? 1 : 0;
				ok = writeNextBit(bit);
				if (!ok) break;
				mask >>= 1;
			}
			return ok;
		}
		
		private boolean writeNextBit(int databit) {
			int clock = 1;
			
			if ((m_header.track_encoding == HFEHeader.ISOIBM_MFM_ENCODING)
				&& ((m_lastDataBit == 1) || (databit == 1)))
				clock = 0; 
			
			boolean ok = writeNextCell(clock);     // Clock bit
			if (!ok) return false;
			writeNextCell(databit);   // Data bit
			if (!ok) return false;
			m_lastDataBit = databit;
			return true;
		}

		private boolean writeNextCell(int value) {
			// if (m_debug) System.out.print(value);
			boolean ok = writeNextSample(value);
			if (!ok) return false;
			if (m_codeRate == 4) ok = writeNextSample(0);  // no change for the next cell
			return ok;
		}
		
		private boolean writeNextSample(int level) {
			// For each track, sample 0 starts at a byte boundary
			// if (m_debug) System.out.print(level);
			int position = m_currentSampleNumber / 8; 
			
			int block = position / 256;
			int offset = position % 256;		
			int actPosition = block*512 + m_currentHead * 256 + offset;

			if (actPosition >= m_formatUnit.length) {
				return false;
			}		
			
			level &= 1;
			int bit = 1 << (m_currentSampleNumber % 8);
			
			if (level == 1) 
				m_formatUnit[actPosition] |= bit;
			else 
				m_formatUnit[actPosition] &= ~bit;

			m_currentSampleNumber++;
			return true;
		}
	}
	
	static int vote(String sFile) throws FileNotFoundException, IOException {
		File fl = new File(sFile);
		
		byte[] hxc = "HXCPICFE".getBytes();
		boolean isHxc = true;
			
		// Read start of file
		FileInputStream fis = new FileInputStream(fl);
		for (int i=0; i < hxc.length; i++) {
			int ch = fis.read();
			if ((char)ch != hxc[i]) return 0;
		}
		return 100;
	}
	
	public HFEFormat(String sImageName) throws IOException, ImageException {
		super(sImageName);
		
		// Read the header
		m_file.seek(0);
		byte[] fhead = new byte[512];
		m_file.readFully(fhead);

		// The format cannot say anything about the track organisation
		m_nSectorsPerTrack = -1;

		m_header = new HFEHeader(fhead);
		m_nTracks = m_header.number_of_track;
		m_nSides = m_header.number_of_side;
		
		m_codec = new HFECodec(m_header.track_encoding, m_header.bitRate);
		
		// Populate the lookup table
		readTrackLookupTables();
			
		// We do not know the total sector count yet
		m_fs = new FloppyFileSystem();
		setVolumeInformation();
	}
	
	public String getFormatName() {
		return TIImageTool.langstr("HFEImage");
	}
	
	private void readTrackLookupTables() throws IOException {
				
		m_abyBufferLUT = new byte[m_nTracks * 4];
		m_file.seek(m_header.track_list_offset*512);  // given in multiples of 0x200
		m_file.readFully(m_abyBufferLUT);  // Read the LUT
		
		m_cylinderpos = new long[m_nTracks];
		m_cylinderlen = new int[m_nTracks];
		
		for (int i=0; i < m_nTracks; i++) {
			m_cylinderpos[i] = Utilities.getInt16rev(m_abyBufferLUT, i*4) * 512;
			m_cylinderlen[i] = Utilities.getInt16rev(m_abyBufferLUT, i*4+2);
			// System.out.println("Cylinder " + i + " at pos " + m_cylinderpos[i] + ", len " +  m_cylinderlen[i]);
		}
	}
	
	/** Find the image sector by the linear sector number. */
	@Override
	ImageSector findSector(int number) throws ImageException {
		// Calculate the CHS location
		if (number >= getTotalSectors()) throw new ImageException(String.format(TIImageTool.langstr("ImageSectorHigh"), getTotalSectors()));
		if ((number != 0) && getSectorsPerTrack() < 8) throw new ImageException(String.format(TIImageTool.langstr("ImageUnknown")));
		Location loc = lbaToChs(number, getTracks(), getSectorsPerTrack());
		
		for (ImageSector is : m_codec.getDecodedSectors()) {
			if (is.getLocation().equals(loc)) return is;
		}
		return null;
	}
	
	/** The positions of the format units have already been determined by the header. */
	long getFormatUnitPosition(int funum) {
		return m_cylinderpos[funum];
	}
	
	/** Format units are cylinders (tracks for head 0 and head 1) in this format. */
	int getFUNumberFromSector(int number) throws ImageException {
		// System.out.println("funum(" + number + ") = " + lbaToChs(number).cylinder);
		// System.out.println("total=" + getTotalSectors() + ", sect=" +number);
		if ((number != 0) && getSectorsPerTrack() < 8) throw new ImageException(String.format(TIImageTool.langstr("ImageUnknown")));

		if (number >= getTotalSectors()) throw new ImageException(String.format(TIImageTool.langstr("ImageSectorHigh"), getTotalSectors()));
		return lbaToChs(number, getTracks(), getSectorsPerTrack()).cylinder;
	}	
		
	int getFormatUnitLength(int funum) {
		return m_cylinderlen[funum];	
	}
	
	public String getHeaderInformation() {
		return m_header.toString();
	}
	
	@Override
	int getImageType() {
		return getImageTypeStatic();
	}
	
	static int getImageTypeStatic() {
		return HFE;
	}

	int getSectorsPerTrack() {
		return m_nSectorsPerTrack;
	}
	
	int getTotalSectors() {
		int ts = m_fs.getTotalSectors();
		if (ts == -1) {
			ts = getSectorsPerTrack() * getTracks() * 2;
		}
		if (ts == 0) return 99999;
		return ts;
	}
	
	/** Called from HFEReader. Similar to the method above, just not trying to find sectors. */
	public byte[] getTrackBytes(int cylinder, int head) throws IOException, ImageException {
		throw new NotImplementedException("HFE");
		/*
		Location loc = new Location(cylinder, head, 0, 0);

		// TODO
		m_currentCylinder = loc.cylinder;
		m_currentHead = loc.head;

		m_abyBuffer = new byte[m_bufferlen1[loc.track]];
		m_ImageFile.seek(m_bufferpos[loc.track]);
		m_ImageFile.readFully(m_abyBuffer);
		
		m_samplecount = m_abyBuffer.length * 4;  // All bits for either head
			
		// Reset to start
		m_currentSampleNumber = 0;
		m_first = true;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (m_currentSampleNumber < m_bufferlen1[loc.cylinder] * 4) {
			baos.write(readBits(8));
		}

		return baos.toByteArray(); */
	}

	/** Prepare an empty image.  */
    @Override
	void prepareNewImage() {
		throw new NotImplementedException("HFEFormat");
	}
}