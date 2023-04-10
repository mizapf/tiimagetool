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

public class HFEFormat extends FloppyImageFormat {
	
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
		
	HFEHeader m_header;	
	long m_cylinderpos[];
	int m_cylinderlen[];
	
	/** Codec for reading / writing HFE */
	class HFECodec extends FormatCodec {
		
		void decode() {
			throw new NotImplementedException("HFECodec");	
		}
		
		void encode() {
			throw new NotImplementedException("HFECodec");	
		}
		
		void prepareNewFormatUnit(int number, byte[] buffer) {
			throw new NotImplementedException("HFECodec");	
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
	}
	
	public String getFormatName() {
		return TIImageTool.langstr("HFEImage");
	}
	
	/** Find the image sector by the linear sector number. */
	@Override
	ImageSector findSector(int number) throws ImageException {
		// Calculate the CHS location
		Location loc = lbaToChs(number);
		
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
		return lbaToChs(number).cylinder;
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
		throw new NotImplementedException("HFE:SPT");
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
		
		m_cells = m_abyBuffer.length * 4;  // All bits for either head
			
		// Reset to start
		m_positionInBuffer = 0;
		m_first = true;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (m_positionInBuffer < m_bufferlen1[loc.cylinder] * 4) {
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