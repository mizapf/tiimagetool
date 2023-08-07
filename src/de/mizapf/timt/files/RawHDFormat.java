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
			int count = m_formatUnit.length / TFileSystem.SECTOR_LENGTH;
			
			// The current FU number is member of ImageFormat
			int startSector = m_nCurrentFormatUnit * getSectorsPerTrack();
			for (int i = 0; i < count; i++) {
				m_decodedSectors.add(new ImageSector(startSector + i, m_formatUnit, i * TFileSystem.SECTOR_LENGTH));
			}
		}
		
		void encode() {
			throw new NotImplementedException("RawHDCodec");	
		}
		
		void prepareNewFormatUnit(int funum, TrackFormatParameters t) {
			throw new NotImplementedException("RawHDCodec");	
		}
	}
	
	public RawHDFormat(String sImageName) throws IOException, ImageException {
		super(sImageName);
		
/*		m_file = new RandomAccessFile(sImageName, "r");
		m_bInitial = false;
		
		// Read the first 512 bytes
		byte[] sector0 = new byte[512];
		m_file.seek(0);
		m_file.readFully(sector0);
		checkFormat(sector0);
		*/
		m_codec = new RawHDCodec();
		// HIER: Geht so nicht, wir müssen erst den Typ herausfinden (SCSI, IDE, HFDC) 
		// m_fs = new HarddiskFileSystem();   
		setVolumeInformation();
	}
	
	public String getFormatName() {
		return TIImageTool.langstr("RAWType");
	}
	
	@Override
	int getSectorsPerTrack() {
		// We have to get the sectors per track from the file system
		return ((HarddiskFileSystem)m_fs).getSectorsPerTrack();
	}
	
	/** Find the image sector by the linear sector number. */
	@Override
	ImageSector findSector(int number) throws ImageException {
		for (ImageSector is : m_codec.getDecodedSectors()) {
			if (is.getNumber() == number) return is;
		}
		return null;
	}
	
	int getFUNumberFromSector(int secnum) {
		return secnum / getSectorsPerTrack();
	}
			
	/** Format units are tracks in this format. */
	long getFormatUnitPosition(int funum) {
		return funum * getFormatUnitLength(funum);
	}	
	
	int getFormatUnitLength(int funum) {
		return getSectorsPerTrack() * TFileSystem.SECTOR_LENGTH; 
	}

	@Override
	int getImageType() {
		return getImageTypeStatic();
	}
	
	static int getImageTypeStatic() {
		return RAWHD;
	}

	/** Prepare an empty image.  */
    @Override
	void prepareNewImage(FormatParameters params) {
		throw new NotImplementedException("RawHDFormat");
	}
		
	@Override
	TrackFormatParameters getTrackParameters() {
		throw new NotImplementedException("RawHDFormat");
	}
		
	static String checkFormatCompatibility(FormatParameters params) {
		return null; 
	}
}

