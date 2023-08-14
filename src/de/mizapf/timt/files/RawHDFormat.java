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
import java.io.EOFException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.*;

/** Format for hard disk images. May be partitioned. Later. 
	Format units are tracks (like SectorDumpFormat).
*/
class RawHDFormat extends HarddiskImageFormat {

	int m_nFormatUnitLength;
	
	static int vote(String sFile) throws IOException, EOFException {
		
		File fl = new File(sFile);
		
		// File system size must be bigger than 3 MB
		if (fl.length()==0 || fl.length() < 3000000) return 0;
		
		// Is it a MAME format?
		if (MameCHDFormat.vote(sFile)==100) return 0;
		
		// We should have a multiple of the sector size
		if ((fl.length() % 256) != 0) return 10;
		
		return 100;		
	}
		
	/** Codec for the Sector Dump Format. Not much to do here. */
	class RawHDCodec extends FormatCodec {
		void decode() {
			m_decodedSectors.clear();
			int count = m_formatUnit.length / TFileSystem.SECTOR_LENGTH;
			// The current FU number is member of ImageFormat
			int startSector = m_nCurrentFormatUnit * count;
			for (int i = 0; i < count; i++) {
				m_decodedSectors.add(new ImageSector(startSector + i, m_formatUnit, i * TFileSystem.SECTOR_LENGTH));
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
	}
	
	public RawHDFormat(String sImageName) throws IOException, ImageException {
		super(sImageName);
		m_codec = new RawHDCodec();
		m_nSectorsPerTrack = -1;
		m_nFormatUnitLength = -1;
		m_nTotalSectors = (int)(m_file.length() / TFileSystem.SECTOR_LENGTH);
	}
	

	public RawHDFormat(String sImageName, FormatParameters params) throws IOException, ImageException {
		super(sImageName, params);
		m_codec = new RawHDCodec();
		m_format = params;
		setFormatUnitLength(params.getFormatUnitLength());
		prepareNewImage(params);
	}

	public String getFormatName() {
		return TIImageTool.langstr("RAWType");
	}
	
	@Override
	Sector readSector0() throws IOException {
		// Read the first 256 bytes
		byte[] sector0 = new byte[TFileSystem.SECTOR_LENGTH];
		m_file.seek(0);
		m_file.readFully(sector0);
		
		return new Sector(0, sector0);
	}
	
	int getFUNumberFromSector(int secnum) {
		if (secnum == 0) return 0;
		int count = m_nFormatUnitLength / TFileSystem.SECTOR_LENGTH;
		return secnum / count;
	}
			
	/** Format units are tracks in this format. */
	long getFormatUnitPosition(int funum) {
		return funum * getFormatUnitLength(funum);
	}	
	
	int getFormatUnitLength(int funum) {
		return m_nFormatUnitLength;
	}

	@Override
	int getImageType() {
		return getImageTypeStatic();
	}
	
	static int getImageTypeStatic() {
		return RAWHD;
	}

	/** Prepare an empty image. The raw HD format does not store anything outside
		of its format units. */
    @Override
	void prepareNewImage(FormatParameters params) {
	}
		
	@Override
	FormatUnitParameters getFormatUnitParameters() {
		// System.out.println("FU param: sectors = " + getFormatUnitLength(0)/TFileSystem.SECTOR_LENGTH);
		return new FormatUnitParameters(getFormatUnitLength(0)/TFileSystem.SECTOR_LENGTH, getFillPattern());
	}
		
	static String checkFormatCompatibility(FormatParameters params) {
		return null; 
	}
	
	@Override
	void setFormatUnitLength(int len) {
		m_nFormatUnitLength = len; 
	}
}

