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
		writeThrough(false);
	}

	SectorDumpFormat(RandomAccessFile filesystem, String sImageName, TFileSystem fs) throws IOException, ImageException {
		super(filesystem, sImageName, fs);
		m_nSectorsByFormat = NONE;
		m_currentCylinder = NONE;
		m_currentTrack = NONE;
		m_currentHead = NONE;
		m_positionInTrack = 0;
		writeThrough(false);
	}
	
	// Called from Volume.createFloppyImage
	SectorDumpFormat() {
		writeThrough(false);
	}

	public String getDumpFormatName() {
		return TIImageTool.langstr("SectorDump");
	}
	
	@Override
	int getFormatType() {
		return FLOPPY_FORMAT; 
	}
	
	@Override
	int getImageType() {
		return SECTORDUMP; 
	}
	
	@Override	
	void setGeometry(boolean bSpecial) throws IOException, ImageException {
		long nLength = m_ImageFile.length();
		if (((nLength / 256) % 10)==3) nLength -= 768;
		
		int format = NONE;
		
		for (int i=0; i < sdfgeometry.length; i++) {
			if (nLength == sdfgeometry[i][0]) {
				format = i;
				break;
			}
		}
		if (format==-1) throw new ImageException(TIImageTool.langstr("SectorDumpInvLength") + ": " + m_ImageFile.length());
			
		m_nHeads = sdfgeometry[format][1];
		m_nCylinders = sdfgeometry[format][2];
		m_nDensity = FloppyFileSystem.UNKNOWN_DENSITY;
		
		int nSectPerTrack = sdfgeometry[format][3];
		int tracklen = Volume.SECTOR_LENGTH * nSectPerTrack;		
		setupBuffers(tracklen);
		m_encoding = (nSectPerTrack < 16)? FM : MFM; 
		System.out.println("nSect " + nSectPerTrack);
	}
	
	/** Newly created. */
	@Override	
	void setGeometry(TFileSystem fs) {
		// Calculate length
		FloppyFileSystem ffs = (FloppyFileSystem)fs;
		
		m_nHeads = ffs.getHeads();
		m_nCylinders = ffs.getTracksPerSide();
		
		int nSectPerTrack = ffs.getTotalSectors() / (m_nHeads * ffs.getTracksPerSide());
		m_nDensity = ffs.getDensity();
		
		int tracklen = Volume.SECTOR_LENGTH * nSectPerTrack;		
		setupBuffers(tracklen);
		m_encoding = (nSectPerTrack < 16)? FM : MFM; 
	}
	
	void setupBuffers(int tracklen) {
		m_trackpos = new int[m_nCylinders*2];
		m_tracklen = new int[m_nCylinders*2];
		
		int pos = 0;
		for (int j=0; j < m_nCylinders*2; j++) {
			m_trackpos[j] = pos;
			m_tracklen[j] = tracklen;
			pos += tracklen;
		}
		m_maxSector = 11520;
	}
	
	/** Overridden by CF7Format. */	
	void writeOnImage() throws IOException {
		// Write back the whole track
		m_ImageFile = new RandomAccessFile(m_sImageName, "rw");		
		m_ImageFile.seek(m_trackpos[m_currentTrack]);
		m_ImageFile.write(m_abyTrack);
		m_ImageFile = new RandomAccessFile(m_sImageName, "r");		
	}
	
	@Override
	int loadTrack(Location loc) throws IOException, ImageException {
		
		ArrayList<Sector> sectors = new ArrayList<Sector>();

		int count=0;
		byte[] bSector = null;
		byte[] bHeader = new byte[4];
		int initcrc = 0;
		int sector = 0;
		int secindex = -1;
	
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
					
					Sector sect = new Sector(loc.track*m_nSectorsByFormat + sector, bSector);
					sect.setTrackPosition(pos, 0xffff, 0xfb); 
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
		// We have no metadata to determine the density before we read a track
		if (m_nDensity==FloppyFileSystem.UNKNOWN_DENSITY) {
			if (m_encoding==FM) m_nDensity = FloppyFileSystem.SINGLE_DENSITY;
			else {
				if (count == 16) m_nDensity = FloppyFileSystem.DOUBLE_DENSITY_16;
				else {
					if (count <= 20) m_nDensity = FloppyFileSystem.DOUBLE_DENSITY;
					else if (count <= 40) m_nDensity = FloppyFileSystem.HIGH_DENSITY;
					else m_nDensity = FloppyFileSystem.ULTRA_DENSITY;
				}
			}
		}
		System.out.println("Determined density: " + m_nDensity + ", count " + count + ", enc " + m_encoding);
		
		// Now we know the last sector (also to fix the problem of missing metadata)
		if (m_nTotalSectors == 0) m_nTotalSectors = m_nSectorsByFormat * m_nCylinders * m_nHeads;

		return secindex;
	}	
	
	/** Create a new track in m_abyTrack when the image has not yet been written. 
		Current cylinder and head are stored in the member variables.
	*/
	@Override
	void createTrack() {
		// Sector content
		System.out.println("Create new track " + m_currentTrack);
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		for (int secno = 0; secno < ffs.getSectorsPerTrack(); secno++) {
			writeBits(m_abyEmpty, 256*8);
		}	
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
	void writeBack(Sector sect) throws IOException, ImageException {
		writeSector(sect.getNumber(), sect.getBytes());
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

