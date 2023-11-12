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
import de.mizapf.timt.util.*;

class SectorDumpFormat extends FloppyImageFormat {

	private final static int NONE = -1;	
	
	static final int[][] sdfgeometry = {    // Alternatives
		{ 92160, 1, 40, 9 },           // SSSD40                            1*1*40
		{ 163840, 1, 40, 16 },			// SSDD16
		{ 184320, 2, 40, 9 },          // DSSD40, SSDD40, SSSD80            2*1*40, 1*2*40, 1*1*80
	    { 327680, 2, 40, 16 },         // DSDD16                         
		{ 368640, 2, 40, 18 },         // DSDD40, DSSD80, SSDD80, SSQD40    2*2*40, 2*1*80, 1*2*80, 1*4*40
		{ 737280, 2, 80, 18 },         // DSDD80, DSQD40, SSUD40, SSQD80    2*2*80, 2*4*40, 1*8*40, 1*4*80
		{ 1474560, 2, 80, 36 },        // DSQD80, DSUD40, SSUD80            2*4*80, 2*8*40, 1*8*80
	    { 2949120, 2, 80, 72 },        // DSUD80                            2*8*80
		{ 409600, 2, 40, 20 }			// CF7 volume
	};

	static int vote(String sFile) throws IOException {
		
		File fl = new File(sFile);
		long nLength = fl.length();
		
		// File system size must be less than 3 MB
		if (nLength==0 || nLength > 3000000) return 0;
		
		int nVal = 100;
		
		if (((nLength / 256) % 10)==3) nLength -= 768;
		if ((nLength % 256) != 0) nVal = 10;

		for (int i=0; i < sdfgeometry.length; i++) {
			if (nLength == sdfgeometry[i][0]) {
				return nVal;
			}
		}
		return 0;		
	}
	
	@Override
	int getImageType() {
		return getImageTypeStatic();
	}
	
	static int getImageTypeStatic() {
		return SECTORDUMP;
	}
	
	/** Codec for the Sector Dump Format. Not much to do here. */
	class SectorDumpCodec extends FormatCodec {
		void decode() {
			// TODO: Try to unify with TrackDumpCodec
			
			// The current FU number is member of ImageFormat
			// A format unit is a track
			int count = getSectorsPerTrack();
			int headerpos = getFirstHeaderPos();
			int contpos = getFirstContentPos();
			int increm = getIncrement(); 

			boolean mfm = (count>10);
			
			int nFound = 0;
			m_decodedSectors.clear();
			while ((nFound < count) && ((contpos+256) <= m_formatUnit.length)) {
				ImageSector is = new ImageSector(getLinearSectorNumber(headerpos), 
					m_formatUnit, (byte)0xfb, mfm, contpos);
				m_decodedSectors.add(is);
				headerpos += increm;
				contpos += increm;
				nFound++;
			}
		}
		
		void encode() {
			for (ImageSector sect : m_decodedSectors) {
				System.arraycopy(sect.getData(), 0, m_formatUnit, sect.getPosition(), TFileSystem.SECTOR_LENGTH);  
			}
		}
		
		void prepareNewFormatUnit(int funum, FormatUnitParameters t) {
			// Only add the fill pattern
			int k = 0;
			int m = t.fillpattern.length;
			for (int i=0; i < m_formatUnit.length; i++) {
				m_formatUnit[i] = t.fillpattern[k % m];
				k = (k+1) % TFileSystem.SECTOR_LENGTH;
			}
		}
		
		private int getIncrement() {
			return TFileSystem.SECTOR_LENGTH;
		}
		
		private int getFirstHeaderPos() {
			return 0;
		}
		
		private int getFirstContentPos() {
			return 0;
		}
		
		int getLinearSectorNumber(int bufferpos) {
			// The first sector is always the first in the FU
			int start = m_nCurrentFormatUnit * getSectorsPerTrack();
			return start + bufferpos / getIncrement();
		}
	}
	
	/** Loaded image. Called from getImageFormat. */
	public SectorDumpFormat(String sImageName) throws IOException, ImageException {
		super(sImageName);
		
		m_codec = new SectorDumpCodec();
		
		long nLength = m_nLength;
		if (((m_nLength / 256) % 10)==3) nLength -= 768;

		int format = NONE;
		for (int i=0; i < sdfgeometry.length; i++) {
			if (nLength == sdfgeometry[i][0]) {
				format = i;
				break;
			}
		}
				
		if (format==NONE) throw new ImageException(TIImageTool.langstr("SectorDumpInvLength") + ": " + nLength);
					
		// Sizes according to the image file (not VIB)
		m_nSectorsPerTrack = sdfgeometry[format][3];
		m_nTracks = sdfgeometry[format][2];
		m_nSides = sdfgeometry[format][1];
		m_nTotalSectors = (int)(nLength / TFileSystem.SECTOR_LENGTH);
	}
	
	public SectorDumpFormat(String sFileName, FormatParameters params) throws FileNotFoundException, IOException, ImageException {
		super(sFileName, params);
		m_codec = new SectorDumpCodec();
		m_format = params;
		prepareNewImage(params);
	}
	
	int getSectorsPerTrack() {
		return m_nSectorsPerTrack;
	}
	
	public String getFormatName() {
		return TIImageTool.langstr("SectorDump");
	}
		
	int getFUNumberFromSector(int secnum) {
		if (secnum == 0) return 0;
		return secnum / getSectorsPerTrack();
	}
	
	int getFormatUnitLength(int funum) {
		return getSectorsPerTrack() * FloppyFileSystem.SECTOR_LENGTH;
	}
	
	FormatUnitParameters getFormatUnitParameters() {
		return new FormatUnitParameters(getSectorsPerTrack(), getFillPattern());
	}
	
	/** Prepare an empty image. The SectorDumpFormat has no additional data
		outside of its format units. */
    @Override
	void prepareNewImage(FormatParameters params) {
	}

	static String checkFormatCompatibility(FormatParameters params) {
		if (params.sectors > 18) return TIImageTool.langstr("Format.unsupported");
		return null; 
	}
	
	Sector readSector0() throws IOException {
		byte[] sect0 = new byte[TFileSystem.SECTOR_LENGTH];
		m_file.seek(0);
		m_file.readFully(sect0);
		return new Sector(0, sect0);
	}
	
	public boolean isPartitioned() {
		return false;
	}
}

