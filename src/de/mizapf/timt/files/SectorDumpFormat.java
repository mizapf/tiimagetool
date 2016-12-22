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

class SectorDumpFormat extends ImageFormat {

	int m_maxSector;
	
	static final int[][] sdfgeometry = { 
		{ 92160, 1, 40, 9 }, 
		{ 184320, 2, 40, 9 },
	    { 327680, 2, 40, 16 },
		{ 368640, 2, 40, 18 }, 
		{ 655360, 2, 80, 16 }, 
		{ 737280, 2, 80, 18 }, 
		{ 1474560, 2, 80, 36 }, 
	    { 2949120, 2, 80, 72 } };
		
	static int vote(RandomAccessFile fileSystem) throws IOException {
		long nLength = fileSystem.length();
		
		// File system size must be less than 3 MB
		if (nLength==0 || nLength > 3000000) return 0;
		
		int nVal = 100;
		
		if (((nLength / 256) % 10)==3) nLength -= 768;
		if ((nLength % Volume.SECTOR_LENGTH) != 0) nVal = 10;

		for (int i=0; i < sdfgeometry.length; i++) {
			if (nLength == sdfgeometry[i][0]) {
				return nVal;
			}
		}
		return 0;		
	}

	SectorDumpFormat(RandomAccessFile filesystem, String sImageName) throws IOException, ImageException {
		super(filesystem, sImageName);
		m_nSectorsByFormat = NONE;
		m_currentCylinder = NONE;
		m_currentTrack = NONE;
		m_currentHead = NONE;
		m_positionInTrack = 0;
	}

	SectorDumpFormat() {
	}

	public String getDumpFormatName() {
		return TIImageTool.langstr("SectorDump");
	}
	
	@Override	
	void setGeometry(boolean bSpecial) throws IOException, ImageException {
		long nLength = m_FileSystem.length();
		if (((nLength / 256) % 10)==3) nLength -= 768;
		
		int format = NONE;
		
		for (int i=0; i < sdfgeometry.length; i++) {
			if (nLength == sdfgeometry[i][0]) {
				format = i;
				break;
			}
		}
		if (format==-1) throw new ImageException(TIImageTool.langstr("SectorDumpInvLength") + ": " + m_FileSystem.length());
			
		m_nHeads = sdfgeometry[format][1];
		m_nCylinders = sdfgeometry[format][2];
		m_nDensity = 0;
		
		int tracklen = Volume.SECTOR_LENGTH * sdfgeometry[format][3];

		m_trackpos = new int[m_nCylinders*2];
		m_tracklen = new int[m_nCylinders*2];
		
		int pos = 0;
		for (int j=0; j < m_nCylinders*2; j++) {
			m_trackpos[j] = pos;
			m_tracklen[j] = tracklen;
			pos += tracklen;
		}
		
		m_encoding = (sdfgeometry[format][3]==9)? FM : MFM; 
		m_maxSector = 11520;
	}
	
	/** Overridden by CF7Format. */
	void readFromImage(byte[] content, int offset) throws IOException {
		m_FileSystem.seek(offset);
		m_FileSystem.readFully(m_abyTrack);
	}

	/** Overridden by CF7Format. */	
	void writeOnImage() throws IOException {
		// Write back the whole track
		m_FileSystem = new RandomAccessFile(m_sImageName, "rw");		
		m_FileSystem.seek(m_trackpos[m_currentTrack]);
		m_FileSystem.write(m_abyTrack);
		m_FileSystem = new RandomAccessFile(m_sImageName, "r");		
	}
	
	@Override
	int readTrack(int nSectorNumber) throws IOException, ImageException {
		int secindex = -1;

		Location loc = null;
		if (nSectorNumber != 0) {
			loc = getLocation(nSectorNumber);
		}
		else {
			loc = new Location(0, 0, 0, 0);
		}
		
		// Do we have that track already?
		if (m_currentCylinder == loc.cylinder && m_currentHead == loc.head) {
			for (int i=0; i < m_sector.length; i++) {
				if (m_sector[i].getNumber()==nSectorNumber) return i;
			}
			return NONE;
		}
		else {
			// Write back the last track
			flush();
		}
		
		m_currentTrack = loc.track;
		m_currentCylinder = loc.cylinder;
		m_currentHead = loc.head;

		m_abyTrack = new byte[m_tracklen[loc.track]];
		readFromImage(m_abyTrack, m_trackpos[loc.track]);
		
		// Reset to start
		m_positionInTrack = 0;
		
		ArrayList<Sector> sectors = new ArrayList<Sector>();

		int count=0;
		byte[] bSector = null;
		byte[] bHeader = new byte[4];
		int initcrc = 0;
		int sector = 0;
		
		// System.out.println("Track length = " + m_tracklen[loc.track] + ", track " + loc.track);
		
		while (m_positionInTrack < m_tracklen[loc.track]) {
			// System.out.println("Position in track = " + m_positionInTrack +", tracklen = " + m_tracklen[loc.track]);
			if (nextIDAMFound()) {
				if (sector == loc.sector) secindex = count;
				if (nextDAMFound()) {
					int pos = m_positionInTrack;
					bSector = new byte[Volume.SECTOR_LENGTH];
					System.arraycopy(m_abyTrack, m_positionInTrack, bSector, 0, Volume.SECTOR_LENGTH);
					m_positionInTrack += Volume.SECTOR_LENGTH;
					
					Sector sect = new Sector(loc.track*m_nSectorsByFormat + sector, bSector, pos, 0xffff, 0xfb); 
					sectors.add(sect);
					// System.out.println("loaded sector " + sect.getNumber() + " at track " + loc.track);
					// System.out.println(sect);
					sector++;
					count++;
				}
			}	
		}
//		System.out.println("Found " + count + " sectors");
		m_sector = sectors.toArray(new Sector[count]);
		m_nSectorsByFormat = count;	
		
		// TODO: Does not make too much sense for sector dump, but we should still
		// try to unify the readTrack method of the floppy formats 
		if (count == 0) throw new ImageException(TIImageTool.langstr("NoSectorsFound"));
		
		// Determine density
		if (m_nDensity==0) {
			if (m_encoding==FM) m_nDensity = 1;
			else {
				if (count <= 20) m_nDensity = 2;
				else if (count <= 40) m_nDensity = 3;
				else m_nDensity = 4;
			}
		}
		
		// Now we know the last sector	
		if (m_nTotalSectors == 0) m_nTotalSectors = m_nSectorsByFormat * m_nCylinders * m_nHeads;

		return secindex;
	}	
	
	/** The sector dump format only contains the sector content, so we always
		find the next IDAM until the end. */
	private boolean nextIDAMFound() {
		return (m_positionInTrack <= m_tracklen[m_currentTrack]-Volume.SECTOR_LENGTH);
	}
	
	private boolean nextDAMFound() {
		return (m_positionInTrack <= m_tracklen[m_currentTrack]-Volume.SECTOR_LENGTH);
	}

	/** We return the cached sector.
	*/
	@Override
	public Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		if (nSectorNumber >= m_maxSector) throw new ImageException(String.format(TIImageTool.langstr("BadSectorNumber"), nSectorNumber)); 
		int secindex = readTrack(nSectorNumber);
		if (secindex != NONE) {
			return m_sector[secindex];
		}
		else throw new ImageException(String.format(TIImageTool.langstr("SectorNotFound"), nSectorNumber));
	}

	@Override
	public void writeSector(int nSectorNumber, byte[] abySector) throws IOException, ImageException {
		int secindex = readTrack(nSectorNumber);
		if (secindex == NONE) throw new ImageException(String.format(TIImageTool.langstr("SectorNotFound"), nSectorNumber));
		// Write the new data
		// Don't forget to clone the bytes!
		byte[] bNewcontent = new byte[256];
		System.arraycopy(abySector, 0, bNewcontent, 0, 256);
		m_sector[secindex].setData(bNewcontent);
	}
	
	private void writeBits(byte[] bytes, int number) {
		int actnumber = number/8;
		if (m_positionInTrack + actnumber > m_abyTrack.length) 
			actnumber = m_abyTrack.length - m_positionInTrack;
		
		System.arraycopy(bytes, 0, m_abyTrack, m_positionInTrack, actnumber);
		m_positionInTrack += actnumber;
	}

	public void flush() throws IOException {
		boolean trackchanged = false;
		if (m_currentTrack == NONE) return;
		for (int i=0; i < m_sector.length; i++) {
			if (m_sector[i].changed()) {
				trackchanged = true;
				m_positionInTrack = m_sector[i].getPosition();
				writeBits(m_sector[i].getBytes(), Volume.SECTOR_LENGTH * 8);
			}
		}
		if (trackchanged) {
			// Write back the whole track
			writeOnImage();
		}
	}

	@Override
	public void reopenForWrite() throws IOException {
		// Don't do anything here
	}
	
	@Override
	public void reopenForRead() throws IOException {
		// Don't do anything here
	}	
	// ===========================================================
	// Formatting
	// ===========================================================
	
	@Override
	void formatTrack(int cylinder, int head, int sectors, int density, int[] gap) {
		// Sector content
		byte[] sect = new byte[256];
		for (int secno = 0; secno < sectors; secno++) {
			for (int k=0; k < 256; k++) sect[k] = (byte)0xe5; 
			writeBits(sect, 256*8);
		}
	}
	
	public void createEmptyImage(File newfile, int sides, int density, int cylinders, int sectors, boolean format) throws FileNotFoundException, IOException {
		
		int tracklen = sectors * 256;
		int pos = 0;

		m_trackpos = new int[cylinders*sides];
		m_tracklen = new int[cylinders*sides];
		
		for (int j=0; j < cylinders*sides; j++) {
			m_trackpos[j] = pos;
			m_tracklen[j] = tracklen;
			pos += tracklen;
		}
		
		m_abyTrack = new byte[tracklen];

		// Allocate bytes in memory. We will write the array to the file at the end.
		byte[] image = new byte[m_abyTrack.length * cylinders * sides];

		if (format) {
			for (int cyl = 0; cyl < cylinders; cyl++) {
				for (int head=0; head < sides; head++) {
					m_positionInTrack = 0;
					formatTrack(cyl, head, sectors, density, null);
				
					// Copy the track into the image
					int track = (head==0)? cyl : 2*cylinders-1-cyl; 
					System.arraycopy(m_abyTrack, 0, image, m_trackpos[track], m_tracklen[track]);
				}
			}
		}
		
		// Write the resulting image
		FileOutputStream fos = new FileOutputStream(newfile);
		fos.write(image, 0, image.length);
		fos.close();
	}
}

