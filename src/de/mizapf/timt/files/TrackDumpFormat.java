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

import de.mizapf.timt.util.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.NotImplementedException;

class TrackDumpFormat extends FloppyImageFormat {

	/* 
	   Most of the geometries here are not compatible with PC99. However,
	   we can use them in MAME, and it may prove difficult to prevent them
	   from being created in MAME: By reformatting a medium, we may be able
	   to create a non-compatible TDF image, but it would be weird to get 
	   an error message from MAME.
	   
	   It would be better to prevent the creation of a non-compatible format
	   by UI feedback.
	*/
	// We simply do not support the other formats in TIImageTool
	static final int[][] tdfgeometry = { 
		{ 260240, 2, 40, 9, 1 },        // DSSD40, compatible to PC99
		{ 491520, 2, 40, 16, 2 },       // DSDD16
		{ 549760, 2, 40, 18, 2 },       // DSDD40, compatible to PC99
//		{ 1003520, 2, 40, 36, 3 },      // DSQD

//		{ 520480,  2, 80, 9, 1 },       
//		{ 983040,  2, 80, 16, 2 },
//		{ 1099520, 2, 80, 18, 2 },      
//		{ 2007040, 2, 80, 36, 3 } 
	};
	
/*	int fm9param[]   = { 16, 11, 45, 231, 0x00, 0xff,  6,  6 };
	int mfm16param[] = { 50, 22, 50, 206, 0x4e, 0x4e, 12, 12 };
	int mfm18param[] = { 40, 22, 24, 712, 0x4e, 0x4e, 10, 12 };
	int mfm36param[] = { 40, 22, 24, 264, 0x4e, 0x4e, 10, 12 }; */
		
	int fm9param[]   = { 0, 16, 11, 45, 231, 0x00, 0xff,  6,  6 };
	int mfm16param[] = { 0, 50, 22, 50, 206, 0x4e, 0x4e, 12, 12 };
	int mfm18param[] = { 0, 40, 22, 24, 712, 0x4e, 0x4e, 10, 12 };
	int mfm36param[] = { 0, 40, 22, 24, 264, 0x4e, 0x4e, 10, 12 };
	Object param[] = { fm9param, mfm16param, mfm18param, mfm36param };		
	
	static int vote(String sFile) throws IOException {

		File fl = new File(sFile);
		long nLength = fl.length();

		// File system size must be less than 3 MB
		if (nLength==0 || nLength > 3000000) return 0;
		
		for (int i=0; i < tdfgeometry.length; i++) {
			if (nLength == tdfgeometry[i][0]) {
				return 100;
			}
		}
		return 0;		
	}
	
	@Override
	int getImageType() {
		return getImageTypeStatic();
	}
	
	static int getImageTypeStatic() {
		return TRACKDUMP;
	}
	
	/** Codec for the Track Dump Format.
	
		Format units are numbered from outside to inside on head 0, and from
		outside to inside on head 1.
		
		funum = cyl + #tracks * head
		
		1------->
		=========[  ]==========
		0------->
	*/

	class TrackDumpCodec extends FormatCodec {
						
		Object param[] = { fm9param, mfm16param, mfm18param, mfm36param };
		
		/** Takes the buffer and creates a new sequence of decoded sectors.
			For this codec, the sectors in the format unit declare their own
			number.
			A format unit is a track for the TDF (including all metadata).
		*/
		void decode() {
			// System.out.println("m_nFormatIndex=" + m_nFormatIndex);
			TrackFormatParameters t = getTrackParameters();
			int headerpos = getFirstHeaderPos(t);
			int contpos = getFirstContentPos(t);
			int increm = getIncrement(t);

			// System.out.println("firsthead=" + bufferpos + ", gap=" + increm);
			boolean mfm = (t.sectors>10);
			// System.out.println("format index=" + m_nFormatIndex);
						
			int nFound = 0;
			m_decodedSectors.clear();
			byte[] content = new byte[TFileSystem.SECTOR_LENGTH];

			while ((nFound < t.sectors) && ((contpos+256) <= m_formatUnit.length)) {
				// m_nCurrentIndex is the current buffer number = track
				
				// TODO: What should happen if the sector number is outside of
				// the expected range for this format unit?
				// -> ImageException?
				
				// System.out.println("Found " + loc + "(curr index=" + m_nCurrentIndex + ")");		
				
				System.arraycopy(m_formatUnit, contpos, content, 0, TFileSystem.SECTOR_LENGTH);

				// System.out.println("headerpos " + headerpos);
				
				ImageSector is = new ImageSector(getLinearSectorNumber(headerpos), 
												content, (byte)0xfb, mfm, contpos);
				m_decodedSectors.add(is);			
				headerpos += increm;
				contpos += increm;
				nFound++;
			}
		}
		
		/** Takes the decoded sectors and creates a new buffer.
		*/
		void encode() {
			for (ImageSector sect : m_decodedSectors) {
				// System.out.println("DecSec = " + sect.getNumber() +", pos = " + sect.getPosition());
				System.arraycopy(sect.getData(), 0, m_formatUnit, sect.getPosition(), TFileSystem.SECTOR_LENGTH);
			}
		}
		
		int getLinearSectorNumber(int bufferpos) {
			Location loc = getHeader(bufferpos);
			return chsToLba(loc);
		}
		
		// New image
		//    prepare empty image (without format units)
		//       create empty format unit
		//       write sector into format unit
		
		
		/** Creates a new format unit from scratch.
			The number is the logical track number.
			
			GAP1:      00 (16)                4E (40)     (50, DSDD16)
			   Sync:   00 (6)                 00 (10)     (12, DSDD16)
			   IDAM:   FE (1)           A1A1A1FE (4)
			     Track:  xx (1)                 xx (1)
			     Head:   xx (1)                 xx (1)
			     Sector: xx (1)                 xx (1)
			     Size:   01 (1)                 01 (1)
			     CRC:  F7F7 (2)               F7F7 (2)
			   GAP2:   FF (11)                4E (22)
			   Sync:   00 (6)                 00 (12)
			   DAM:    FB (1)           A1A1A1FB (4)
			   Cont:   xx (256)               xx (256)
			   CRC:  F7F7 (2)               F7F7 (4)
			   GAP3:   FF (45)                4E (24)     (50, DSDD16)
			GAP4:      FF (231)               4E (712)    (206, DSDD16)

		*/
		void prepareNewFormatUnit(int number, TrackFormatParameters t) {
			int start = 0;			
			boolean mfm = true;
			// System.out.println("prepareNewFU(" + number + ")");
			// Skew (only for single density), else start with sector 0 for
			// each track
			if (!t.mfm) {
				start = (number * 6) % 9;
			}	
			int pos = 0;

			// Fill gap 1
			for (int i=0; i < t.gap1; i++) m_formatUnit[pos++] = (byte)t.gap1byte;
			
			int secno = start;
			for (int sect = 0; sect < t.sectors; sect++) {
				// Sync
				for (int i=0; i < t.sync1; i++) m_formatUnit[pos++] = (byte)0;
				// IDAM
				if (t.mfm)
					for (int i=0; i < 3; i++) m_formatUnit[pos++] = (byte)0xa1;		
				m_formatUnit[pos++] = (byte)0xfe;
				
				// Header
				m_formatUnit[pos++] = (byte)(number % getTracks());
				m_formatUnit[pos++] = (byte)(number / getTracks());
				m_formatUnit[pos++] = (byte)secno;	
				m_formatUnit[pos++] = (byte)1;
				m_formatUnit[pos++] = (byte)0xf7;
				m_formatUnit[pos++] = (byte)0xf7;
				
				for (int i=0; i < t.gap2; i++) m_formatUnit[pos++] = (byte)t.gapbyte;
				for (int i=0; i < t.sync; i++) m_formatUnit[pos++] = (byte)0;

				// DAM
				if (t.mfm)
					for (int i=0; i < 3; i++) m_formatUnit[pos++] = (byte)0xa1;		
				m_formatUnit[pos++] = (byte)0xfb;
				
				// Contents
				for (int i=0; i < 256; i++) {
					m_formatUnit[pos++] = t.fillpattern[i % t.fillpattern.length];
				}
				
				m_formatUnit[pos++] = (byte)0xf7;
				m_formatUnit[pos++] = (byte)0xf7;
				
				// GAP3
				for (int i=0; i < t.gap3; i++) m_formatUnit[pos++] = (byte)t.gapbyte;
				
				// Save sector position
				
				// Now increase the sector number
				if (t.sectors == 9) {
					secno = (secno + 7) % 9;
				}
				else {
					if (t.sectors == 16) {
						secno = (secno + 9) % 16;
					}
					else { 
						secno = (secno + 11) % 18;
					}
				}
			}
			// Fill gap 4
			for (int i=0; i < t.gap4; i++) m_formatUnit[pos++] = (byte)t.gapbyte;
		}
		
		// sync1 + mark (1|4) + header + crc + gap2 + sync2 + mark (1|4) + contlen + crc + gap3  
		private int getIncrement(TrackFormatParameters t) {
			int mark = (t.sectors > 10)? 4 : 1;
			int headerlen = 4;
			int crclen = 2;
			return t.sync1 + mark + headerlen + crclen 
			    +  t.gap2 + t.sync + mark + TFileSystem.SECTOR_LENGTH + crclen 
			    +  t.gap3;
		}
		
		// gap1 + sync1 + mark (1|4)
		private int getFirstHeaderPos(TrackFormatParameters t) {
			int mark = (t.sectors > 10)? 4 : 1;
			return t.gap1 + t.sync1 + mark;
		}
		
		private int getFirstContentPos(TrackFormatParameters t) {
			int mark = (t.sectors > 10)? 4 : 1;
			int headerlen = 4;
			int crclen = 2;
				
			return getFirstHeaderPos(t) + headerlen + crclen + t.gap2 + t.sync + mark; 
		}
		
		private Location getHeader(int bufferpos) {
			return new Location(m_formatUnit[bufferpos], 
					m_formatUnit[bufferpos+1],
					m_formatUnit[bufferpos+2]);
		}
	}
	
	/** Loaded image. Called from getImageFormat. */
	public TrackDumpFormat(String sImageName) throws IOException, ImageException {
		super(sImageName);
		m_codec = new TrackDumpCodec();
		
		long nLength = m_file.length();
		m_nFormatIndex = NONE;
		for (int i=0; i < tdfgeometry.length; i++) {
			if (nLength == tdfgeometry[i][0]) {
				m_nFormatIndex = i;
				break;
			}
		}
		
		if (m_nFormatIndex==NONE) throw new ImageException(TIImageTool.langstr("InvalidFormat"));		
		System.out.println("TDF / FormatIndex = " + m_nFormatIndex);
		
		int nTotalSectors = tdfgeometry[m_nFormatIndex][1] 
						* tdfgeometry[m_nFormatIndex][2]
						* tdfgeometry[m_nFormatIndex][3];
		
		// Set the geometry from the format
		m_nTracks = tdfgeometry[m_nFormatIndex][2];
		m_nSides = tdfgeometry[m_nFormatIndex][1];
		m_nSectorsPerTrack = tdfgeometry[m_nFormatIndex][3];

		m_fs = new FloppyFileSystem(nTotalSectors);
		
		setVolumeInformation();
	}
	
	/** Called for newly created images. */
	public TrackDumpFormat(String sFileName, FormatParameters params) throws FileNotFoundException, IOException, ImageException {
		super(sFileName, params);
		m_codec = new TrackDumpCodec();
		m_format = params;
		
		m_nFormatIndex = NONE;
				
		for (int i=0; i < tdfgeometry.length; i++) {
			if (params.cylinders == tdfgeometry[i][2]
					&& params.heads == tdfgeometry[i][1]
					&& params.sectors == tdfgeometry[i][3]) {
				m_nFormatIndex = i;
				break;
			}
		}
		if (m_nFormatIndex == NONE) {
			throw new ImageException(TIImageTool.langstr("InvalidFormat"));
		}
		
		// Set the geometry from the format
		m_nTracks = tdfgeometry[m_nFormatIndex][2]; // we need this for chsToLba

		// We do not need to set a new file system because the old one will
		// be copied here
		prepareNewImage(params);
	}

	/** Prepare an empty image. The TrackDumpFormat has no additional data
		outside of its format units. */
    @Override
	void prepareNewImage(FormatParameters params) {
		System.out.println("Prepare new image; nothing to do for TrackDumpFormat");
	}
		
	public String getFormatName() {
		return TIImageTool.langstr("TrackDump");
	}
	
	int getFormatUnitLength(int funum) {
		switch (getSectorsPerTrack()) {
		case 9:
			return 3253;
		case 16:
			return 6144;
		case 18:
			return 6872;
		default:
			return 0;
		}
	}
		
	int getFUNumberFromSector(int secnum) throws ImageException {
		Location loc = lbaToChs(secnum, getTracks(), getSectorsPerTrack());
		int funum = loc.cylinder;
		if (loc.head > 0) funum += m_nTracks;
		return funum;	
	}   

	int getSectorsPerTrack() {
		return tdfgeometry[m_nFormatIndex][3]; 
	}
	
	@Override
	TrackFormatParameters getTrackParameters() {
		return new TrackFormatParameters((int[])param[m_nFormatIndex], getSectorsPerTrack(), getFillPattern());
	}
}
