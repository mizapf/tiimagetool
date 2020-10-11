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
import java.io.EOFException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.Arrays;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.util.GenCounter;
import de.mizapf.timt.TIImageTool;

public abstract class ImageFormat  {

	RandomAccessFile m_FileSystem;
	
	/** Cached sectors of this image. */
	SectorCache m_cache;
	
	/** Cached sectors of the current track. */
	Sector[] m_sector;
	int[] m_trackpos;
	int[] m_tracklen;
	int m_positionInTrack;

	int m_encoding;
	
	String m_sImageName;
	static final int NONE = -1;
	
	protected final static int FM = 0;
	protected final static int MFM = 1;
	
	public final static int SECTORDUMP = 0;
	public final static int TRACKDUMP = 1;
	public final static int HFE = 2;
	public final static int CF7VOLUME = 3;
	
	public final static int SINGLE_DENSITY = 0;
	public final static int DOUBLE_DENSITY = 1;
	public final static int HIGH_DENSITY = 2;
	public final static int ULTRA_DENSITY = 3;
	
	public final static int SECTOR_LENGTH = 256;
	
	protected int m_nCylinders;
	protected int m_nHeads;
	protected int m_nSectorsPerTrack;
	protected int m_nSectorLength;

	protected int m_nTrackLength;
	
	protected int m_nDensity; // specific for floppies
	
	protected byte[] m_abyTrack;
	protected int m_nCurrentTrack; 

	protected int m_nTotalSectors;
	
	protected boolean m_bWriteThrough;
	protected boolean m_bDirty;
	
	protected static final int TRACK = 0;
	protected static final int SECTOR = 1;
	protected static final int HEAD = 2;
	protected static final int CYLINDER = 3;

	protected final static int NOTRACK = -1;

	protected int m_currentCylinder = 0;
	protected int m_currentTrack = 0;
	protected int m_currentHead = 0;
	protected int m_nSectorsByFormat;
	protected int m_codeRate = 1;


	protected ImageFormat(RandomAccessFile filesystem, String sImageName, GenCounter gen) throws IOException, ImageException {
		m_FileSystem = filesystem;
		m_sImageName = sImageName;
		m_nSectorLength = Volume.SECTOR_LENGTH;
		setGeometry(false /*Utilities.isRawDevice(sImageName)*/);
		m_nCurrentTrack = NOTRACK;
		m_abyTrack = new byte[m_nTrackLength];
		m_cache = new SectorCache(gen);
		m_bWriteThrough = false;
		m_bDirty = false;
	}
	
	// Called from Volume.createFloppyImage (needed by subclass contructors)
	protected ImageFormat() {
		m_bWriteThrough = false;
		m_bDirty = false;
	}
	
	protected ImageFormat(File newfile) throws FileNotFoundException {
		m_FileSystem = new RandomAccessFile(newfile, "rw");
		m_bDirty = false;
	}
	
	protected void writeThrough(boolean bWriteTh) {
		m_bWriteThrough = bWriteTh;
	}
	
	public int getCylinders() {
		return m_nCylinders;
	}

	public int getHeads() {
		return m_nHeads;
	}

	public int getSectorsPerTrack() {
		return m_nSectorsPerTrack;
	}

	public int getSectorLength() {
		return m_nSectorLength;
	}
	
	public int getTotalSectors() {
		return m_nTotalSectors;
	}
	
	/** Reads a sector.
		@throws ImageException if the sector cannot be found.
	*/
	public final Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		Sector sect = m_cache.read(nSectorNumber);
		if (sect == null) sect = readSectorFromImage(nSectorNumber);
		return sect;
	}
	
	/** Reads a sector from the medium. */
	public abstract Sector readSectorFromImage(int nSectorNumber) throws EOFException, IOException, ImageException;
	
	/** Write a sector. Is public for SectorEditFrame. */
	public final void writeSector(Sector sect) throws IOException, ImageException {
		if (m_bWriteThrough) {
			writeSectorToImage(sect.getNumber(), sect.getBytes());
		}
		else {
			m_bDirty = true;
			m_cache.write(sect);
		}
	}

	/** Write a sector to the medium. */
	public abstract void writeSectorToImage(int nNumber, byte[] abySector) throws IOException, ImageException;
	
	int getDensity() {
		return m_nDensity;
	}
	
	void close() throws IOException {
		flush();
		m_FileSystem.close();
	}
	
	long getLastModifiedTime() {
		java.io.File file = new java.io.File(m_sImageName);
		return file.lastModified();
	}
	
	void writeBack() {
	}
	
	boolean isDirty() {
		return m_bDirty;
	}
	
	// Also called from SectorEditFrame
	public void nextGeneration() {
		m_cache.nextGeneration();
	}
	
	void sameGeneration() {
		m_cache.sameGeneration();
	}
	
	public int getGeneration() {
		return m_cache.getGeneration();
	}

	public void setGeneration(int gen) {
		m_cache.setGeneration(gen);
	}
	
	public static ImageFormat getImageFormat(String sFile, GenCounter gen) throws FileNotFoundException, IOException, ImageException {
		RandomAccessFile fileSystem = new RandomAccessFile(sFile, "r");
		
/*		if (Utilities.isRawDevice(sFile)) {
			return new RawHDFormat(fileSystem, sFile, nSectorLength);
		} */
		
		if (fileSystem.length()==0) throw new ImageException(TIImageTool.langstr("ImageEmpty"));
		
		if (CF7VolumeFormat.vote(fileSystem) > 50) {
			return new CF7VolumeFormat(fileSystem, sFile, gen);
		}
		if (CF7ImageFormat.vote(fileSystem) > 50) {
			return new CF7ImageFormat(fileSystem, sFile, gen);
		}
		if (SectorDumpFormat.vote(fileSystem) > 50) {
			return new SectorDumpFormat(fileSystem, sFile, gen);
		}
		
		if (TrackDumpFormat.vote(fileSystem) > 50) {
			return new TrackDumpFormat(fileSystem, sFile, gen);
		}
		
		if (RawHDFormat.vote(fileSystem) > 50) {
			return new RawHDFormat(fileSystem, sFile, gen);
		}

		if (MameCHDFormat.vote(fileSystem) > 50) {
			return new MameCHDFormat(fileSystem, sFile, gen);
		}

		if (HFEFormat.vote(fileSystem) > 50) {
			return new HFEFormat(fileSystem, sFile, gen);
		}
				
		throw new ImageException(sFile + ": " + TIImageTool.langstr("ImageUnknown"));
	}

	public void reopenForWrite() throws IOException {
		if (m_FileSystem != null) m_FileSystem.close();
		m_FileSystem = new RandomAccessFile(m_sImageName, "rw");		
	}
	
	public void reopenForRead() throws IOException {
		if (m_FileSystem != null) m_FileSystem.close();
		m_FileSystem = new RandomAccessFile(m_sImageName, "r");		
	}

	/** Set some parameters for this image, according to the format and the
	    give file. Any kinds of members may be initialized here; at least, the
	    following members must be set:
	    - m_nSectorsByFormat
	    - m_nCylinders
	    - m_nHeads
	    - m_nSectorLength
	    - m_nTrackLength
	    - m_nDensity
	    - m_nTotalSectors
	    @param bSpecial image is a special file (raw device content).
	*/
	abstract void setGeometry(boolean bSpecial) throws IOException, ImageException;
			
	/** Delivers the position on the image file by track and sector.
		Result is returned as a Location instance.
		Called by the floppy image types only.
	*/
	Location getLocation(int nSectorNumber) throws IOException, ImageException {
		if (m_nSectorsByFormat == NONE) {
			// We do not know the number of sectors yet. Read track 0.
			readTrack(0);
		}
		// Now we should know the sector count, so we can calculate the track and
		// sector. The track number is counted from head 0, cylinder 0 ... max,
		// then head 1, cylinder max ... 0.
		
		// The sector offset is redefined here as the sector number in the track
		
		if (m_nTotalSectors != 0 && nSectorNumber > m_nTotalSectors) 
			throw new ImageException(String.format(TIImageTool.langstr("ImageSectorHigh"), m_nTotalSectors));
		
		int sector = nSectorNumber % m_nSectorsByFormat;
		
		int track = nSectorNumber / m_nSectorsByFormat;
		int cylinder = track;
		int head = 0;
		
		if (track >= m_nCylinders) {
			// Next head
			cylinder = 2 * m_nCylinders - 1 - track;
			head++;
		}
		if (cylinder < 0) throw new ImageException(TIImageTool.langstr("ImageInvalidHeads"));

		Location loc = new Location(cylinder, head, sector, track);
		
		// System.out.println("sector " + nSectorNumber + " in " + loc);
		return loc;

		// System.out.println("Sector " + nSectorNumber + " is in track " + loc.track + ", cyl=" + loc.cylinder + ", head=" + loc.head);
		//System.out.println("m_header.number_of_track = " + m_header.number_of_track + ", nSectorNumber = " + nSectorNumber);
	}	
	
	/** Read a track, given the number of a sector within that track.
		@param nSectorNumber LBA number of the sector.
		@return Index of the desired sector within its track.
	*/
	abstract int readTrack(int nSectorNumber) throws IOException, ImageException;
	
	abstract void formatTrack(int cylinder, int head, int sectors, int density, int[] gap);
	
	abstract String getDumpFormatName();
	
	public abstract void flush() throws IOException;
	
	abstract void createEmptyImage(File newfile, int sides, int density, int tracks, int sectors, boolean format) throws ImageException, FileNotFoundException, IOException;
	
	int checkCRC(boolean fix, boolean reset) throws IOException {	
		return NONE;
	}
	
		
	public static void createFloppyImage(File newImageFile, String volumeName, int type, int sides, int density, int tracks, boolean format, GenCounter gen) throws IOException, ImageException {

		ImageFormat image = null;
		
		int sectorsPerTrack = 9 << density;

		switch (type) {
		case SECTORDUMP:
			image = new SectorDumpFormat();
			break;
		case TRACKDUMP:
			image = new TrackDumpFormat();
			break;
		case HFE:
			image = new HFEFormat();
			break;
		case CF7VOLUME:
			image = new CF7VolumeFormat();
			sectorsPerTrack = 20;
			break;
		}
		
		image.createEmptyImage(newImageFile, sides, density, tracks, sectorsPerTrack, format);		
		
		if (format) {
			
			// Load it and write sectors 0 and 1
			image = ImageFormat.getImageFormat(newImageFile.getAbsolutePath(), gen);
			
			// Sector 0
			byte[] sector0 = new byte[SECTOR_LENGTH];
			
			Arrays.fill(sector0, 0, 10, (byte)' ');
			System.arraycopy(volumeName.getBytes(), 0, sector0, 0, volumeName.getBytes().length);
			
			int nsectors = sides * tracks * sectorsPerTrack;
			sector0[10] = (byte)(nsectors >> 8);
			sector0[11] = (byte)(nsectors % 256);
			sector0[12] = (byte)sectorsPerTrack;
			sector0[13] = 'D';
			sector0[14] = 'S';
			sector0[15] = 'K';
			sector0[16] = (byte)0x20;
			sector0[17] = (byte)tracks;
			sector0[18] = (byte)sides;
			sector0[19] = (byte)(density+1);
			for (int i=0x14; i < 0x38; i++) sector0[i] = (byte)0;
			for (int i=0x38; i < 0x100; i++) sector0[i] = (byte)0xff;
			
			// Allocation bitmap
			AllocationMap am = new AllocationMap(nsectors);
			am.allocate(0);
			if (am.getAUSize()==1) am.allocate(1);
			
			byte[] abyMap = am.toBitField();
			System.arraycopy(abyMap, 0, sector0, 0x38, abyMap.length);
			
			// Sector 1
			byte[] sector1 = new byte[SECTOR_LENGTH];
			Arrays.fill(sector1, 0, SECTOR_LENGTH, (byte)0x00);
			
			image.writeSector(new Sector(0, sector0));
			image.writeSector(new Sector(1, sector1));
			image.close();
		}
	}
	
}
