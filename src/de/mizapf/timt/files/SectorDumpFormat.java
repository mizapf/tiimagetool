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
import de.mizapf.timt.util.NotImplementedException;

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

	class SectorDumpCodec extends FormatCodec {
		SectorDumpCodec(String sFile, boolean bInitial) {
			super(sFile, bInitial);
		}	
		
		SectorDumpCodec(RandomAccessFile rafile, String sFile, boolean bInitial) {
			super(rafile, sFile, bInitial);
		}
		
		void writeBits(byte[] seq) {
			throw new NotImplementedException("writeBits");
		}
		
		void createEmptyBuffer(int buffernum) {
			System.out.println("createEmptyBuffer " + buffernum);
			// Thread.currentThread().dumpStack();
			m_abyBuffer = new byte[m_bufferlen[buffernum]];
			for (int i=0; i < m_nSectorsPerTrack; i++) {
				System.arraycopy(m_abyFill, 0, m_abyBuffer, i * 256, 256);
			}
		}
		
		int decodeBuffer() {
			System.out.println("decodeBuffer");
			m_buffsector.clear();
			int count =  m_fs.getSectors();
			if (count==0) {
				// We do not know how many sectors we'll get
				count = m_abyBuffer.length / 256;
			}
			byte[] content = new byte[256];
			for (int i = 0; i < count; i++) {
				Location loc = new Location(m_locCurrent.cylinder, m_locCurrent.head, i, m_locCurrent.track);
				System.arraycopy(m_abyBuffer, i * 256, content, 0, 256);
				m_buffsector.add(new ImageSector(loc, content, (byte)0xfb, count>10, i*256));
			}
			return count;	
		}

		void encodeBuffer() {
			System.out.println("encodeBuffer");
			// Thread.currentThread().dumpStack();
			for (ImageSector isect : m_buffsector) {
				System.arraycopy(isect.getData(), 0, m_abyBuffer, isect.getLocation().sector * 256, 256);
			}
		}
	}

	/** Loaded image. Called from getImageFormat. */
	SectorDumpFormat(RandomAccessFile rafile, String sImageName) throws IOException, ImageException {
		super(rafile, sImageName);
		m_codec = new SectorDumpCodec(rafile, sImageName, false);
		setupBuffers(sImageName, false);
		writeThrough(false);
	}

	/** Newly created image. */
	SectorDumpFormat(RandomAccessFile rafile, String sImageName, TFileSystem fs) throws IOException, ImageException {
		super(rafile, sImageName, fs);
		m_codec = new SectorDumpCodec(rafile, sImageName, true);
		setupBuffers(sImageName, true);
		writeThrough(false);
	}
	
	// Called from ImageFormat.createFloppyImage
	SectorDumpFormat() {
		writeThrough(false);
	}

	public String getDumpFormatName() {
		return TIImageTool.langstr("SectorDump");
	}

	/** Write a header. Nothing to do here. */
	@Override
	void prepareImageFile() {
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
	TFileSystem determineFileSystem(RandomAccessFile rafile) throws IOException, ImageException {
		long nLength = rafile.length();
		if (((nLength / 256) % 10)==3) nLength -= 768;
		
		int format = NONE;
		
		for (int i=0; i < sdfgeometry.length; i++) {
			if (nLength == sdfgeometry[i][0]) {
				format = i;
				break;
			}
		}
		if (format==-1) throw new ImageException(TIImageTool.langstr("SectorDumpInvLength") + ": " + m_ImageFile.length());
			
		FloppyFileSystem ffs = new FloppyFileSystem(
			sdfgeometry[format][2],  // cyl
			sdfgeometry[format][1],  // head
			sdfgeometry[format][3],  // sect
			FloppyFileSystem.UNKNOWN_DENSITY);  // dens
		return ffs;
	}
			
	@Override
	void setupBuffers(String sImageName, boolean bInitial) {
		// Calculate length	
		int tracklen = SECTOR_LENGTH * m_nSectorsPerTrack;
		int[] bufferpos = new int[m_nCylinders*2];
		int[] bufferlen = new int[m_nCylinders*2];
		
		int pos = 0;
		for (int j=0; j < m_nCylinders*2; j++) {
			bufferpos[j] = pos;
			bufferlen[j] = tracklen;
			pos += tracklen;
		}
		m_maxSector = 11520;
		
		m_codec.setBufferParams(bufferpos, bufferlen);
	}
		
	@Override
	int loadBuffer(Location loc) throws IOException, ImageException {
		
		ArrayList<Sector> sectors = new ArrayList<Sector>();

		int count=0;
		byte[] bSector = null;
		byte[] bHeader = new byte[4];
		int initcrc = 0;
		int sector = 0;
		int secindex = -1;
	
		// System.out.println("Track length = " + m_bufferlen1[loc.track] + ", track " + loc.track);
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		
		if (ffs == null) System.err.println("File system is still null!");
		
		while (m_positionInBuffer < m_bufferlen1[loc.track]) {
			// System.out.println("Position in track = " + m_positionInBuffer +", tracklen = " + m_bufferlen1[loc.track]);
			if (nextIDAMFound()) {
				if (sector == loc.sector) secindex = count;
				if (nextDAMFound()) {
					int pos = m_positionInBuffer;
					bSector = new byte[Volume.SECTOR_LENGTH];
					System.arraycopy(m_abyBuffer, m_positionInBuffer, bSector, 0, Volume.SECTOR_LENGTH);
					m_positionInBuffer += Volume.SECTOR_LENGTH;
					
					Sector sect = new Sector(loc.track*ffs.getCountedSectors() + sector, bSector);
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
		m_buffsector = sectors.toArray(new Sector[count]);
		ffs.setCountedSectors(count);	
		
		// TODO: Does not make too much sense for sector dump, but we should still
		// try to unify the readBuffer method of the floppy formats 
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
		if (m_nTotalSectors == 0) m_nTotalSectors = ffs.getCountedSectors() * m_nCylinders * m_nHeads;

		return secindex;
	}	
	
	/** Create a new track in m_abyBuffer when the image has not yet been written. 
		Current cylinder and head are stored in the member variables.
	*/
	@Override
	void createBuffer(int cylinder, int head, int track) {
		// Sector content
		System.out.println("Create new track " + track);
		m_abyBuffer = new byte[getFullTrackLength(m_bufferlen1[track])];
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		for (int secno = 0; secno < ffs.getSectorsPerTrack(); secno++) {
			writeBits(m_abyEmpty, 256*8);
		}	
	}
	
	/** The sector dump format only contains the sector content, so we always
		find the next IDAM until the end. */
	private boolean nextIDAMFound() {
		return (m_positionInBuffer <= m_bufferlen1[m_currentTrack]-Volume.SECTOR_LENGTH);
	}
	
	private boolean nextDAMFound() {
		return (m_positionInBuffer <= m_bufferlen1[m_currentTrack]-Volume.SECTOR_LENGTH);
	}

	/** We return the cached sector.
	*/
/*	@Override
	public Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		if (nSectorNumber >= m_maxSector) throw new ImageException(String.format(TIImageTool.langstr("BadSectorNumber"), nSectorNumber)); 
		int secindex = locateInBuffer(nSectorNumber);
		if (secindex != NONE) {
			return m_buffsector[secindex];
		}
		else throw new ImageException(String.format(TIImageTool.langstr("SectorNotFound"), nSectorNumber));
	} */

	private void writeBits(byte[] bytes, int number) {
		int actnumber = number/8;
		if (m_positionInBuffer + actnumber > m_abyBuffer.length) 
			actnumber = m_abyBuffer.length - m_positionInBuffer;
		System.arraycopy(bytes, 0, m_abyBuffer, m_positionInBuffer, actnumber);
		m_positionInBuffer += actnumber;
	}

	public void flush() throws IOException {
		System.out.println("LEGACY function");
		boolean trackchanged = false;
		if (m_currentTrack == NONE) {
			System.out.println("No track loaded yet");
			return;
		}
		for (int i=0; i < m_buffsector.length; i++) {
			if (m_buffsector[i].changed()) {
				trackchanged = true;
				byte[] bSectorContent = m_buffsector[i].getBytes();
				m_positionInBuffer = m_buffsector[i].getPosition();
				writeBits(bSectorContent, Volume.SECTOR_LENGTH * 8);
			}
		}
		if (trackchanged) {
			// Write back the whole track
			writeOnImage();
		}
	}

	/** Overridden by CF7Format. */	
	void writeOnImage() throws IOException {
		// Write back the whole track
		m_ImageFile = new RandomAccessFile(m_sImageName, "rw");		
		m_ImageFile.seek(m_bufferpos[m_currentTrack]);
		m_ImageFile.write(m_abyBuffer);
		m_ImageFile = new RandomAccessFile(m_sImageName, "r");		
	}
	
	@Override
	void writeToBuffer(Sector sect) throws IOException, ImageException {
		writeSector(sect);
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
		
		System.out.println("FIXME: Legacy function createEmptyImage");
		int tracklen = sectors * 256;
		int pos = 0;

		m_bufferpos = new int[cylinders*sides];
		m_bufferlen1 = new int[cylinders*sides];
		
		for (int j=0; j < cylinders*sides; j++) {
			m_bufferpos[j] = pos;
			m_bufferlen1[j] = tracklen;
			pos += tracklen;
		}
		
		m_abyBuffer = new byte[tracklen];

		// Allocate bytes in memory. We will write the array to the file at the end.
		byte[] image = new byte[m_abyBuffer.length * cylinders * sides];

		if (format) {
			for (int cyl = 0; cyl < cylinders; cyl++) {
				for (int head=0; head < sides; head++) {
					m_positionInBuffer = 0;
					formatTrack(cyl, head, sectors, density, null);
				
					// Copy the track into the image
					int track = (head==0)? cyl : 2*cylinders-1-cyl; 
					System.arraycopy(m_abyBuffer, 0, image, m_bufferpos[track], m_bufferlen1[track]);
				}
			}
		}
		
		// Write the resulting image
		FileOutputStream fos = new FileOutputStream(newfile);
		fos.write(image, 0, image.length);
		fos.close();
	}
	
	/** Return the track number as the buffer index. */
	@Override
	int getBufferIndex(Location loc) {
		return loc.track;
	}
}

