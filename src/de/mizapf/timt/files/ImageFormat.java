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
import de.mizapf.timt.TIImageTool;

public abstract class ImageFormat  {

	RandomAccessFile m_ImageFile;
	
	TFileSystem m_fs;
	
	/** Cached sectors of the current track. */
	Sector[] m_sector;
	
	byte[] m_abyEmpty;
	
	int[] m_trackpos;
	int[] m_tracklen;
	int m_positionInTrack;

	int m_encoding;
	int m_cells;

	String m_sImageName;
	static final int NONE = -1;
	
	protected final static int FM = 0;
	protected final static int MFM = 1;
	
	public final static int NOTYPE = -1;
	public final static int SECTORDUMP = 0;
	public final static int TRACKDUMP = 1;
	public final static int HFE = 2;
	public final static int CF7VOLUME = 3;
	
	public final static String[] suffix = { "dsk", "dtk", "hfe" };
	
	// Types
	public final static int FLOPPY_FORMAT = 0;
	public final static int HD_FORMAT = 1;
	public final static int SET_FORMAT = 2;

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

	protected boolean m_bFormatCommitted;

	protected ImageFormat(RandomAccessFile filesystem, String sImageName) throws IOException, ImageException {
		m_ImageFile = filesystem;
		m_sImageName = sImageName;
		m_nSectorLength = Volume.SECTOR_LENGTH;
		setGeometry(false /*Utilities.isRawDevice(sImageName)*/);
		m_nCurrentTrack = NOTRACK;
		m_abyTrack = new byte[m_nTrackLength];
		m_bWriteThrough = false;
		m_bFormatCommitted = true;
	}
	
	/** Newly created; no image file yet. */
	protected ImageFormat(RandomAccessFile rafile, String sImageName, TFileSystem fs) throws IOException, ImageException {
		m_fs = fs;
		m_ImageFile = rafile;
		m_sImageName = sImageName;
		m_nSectorLength = Volume.SECTOR_LENGTH;
		setGeometry(fs);
		m_nCurrentTrack = NOTRACK;
		m_abyTrack = new byte[m_nTrackLength];
		m_bWriteThrough = false;
		m_bFormatCommitted = false;
	}
	
	// Called from Volume.createFloppyImage (needed by subclass contructors)
	protected ImageFormat() {
		m_bWriteThrough = false;
		m_bFormatCommitted = false;
	}
	
	protected ImageFormat(File newfile) throws FileNotFoundException {
		m_ImageFile = new RandomAccessFile(newfile, "rw");
		m_bFormatCommitted = false;
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
/*	public final Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		Sector sect = m_cache.read(nSectorNumber);
		if (sect == null) sect = readSectorFromImage(nSectorNumber);
		return sect;
	}
	*/
	/** Reads a sector from the medium. */
	public abstract Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException;
	
	/** Write a sector. Is public for SectorEditFrame. */
/*	public final void writeSector(Sector sect) throws IOException, ImageException {
		if (m_bWriteThrough) {
			writeSectorToImage(sect.getNumber(), sect.getBytes());
		}
		else {
			m_cache.write(sect);
		}
	}
*/

	/** Write a sector to the medium. */
	public abstract void writeSector(int nNumber, byte[] abySector) throws IOException, ImageException;
	
	int getDensity() {
		return m_nDensity;
	}
	
	/** Returns the type of the format. */
	abstract int getFormatType();
	
	int getImageType() {
		return NOTYPE;
	}
	
	void close() throws IOException {
		flush();
		m_ImageFile.close();
	}
	
	long getLastModifiedTime() {
		java.io.File file = new java.io.File(m_sImageName);
		return file.lastModified();
	}
	
	/** Indicates whether the formatting has been saved to disk already.
	    Only applicable for non-write-through media.
	*/
	boolean formatCommitted() {
		return m_bFormatCommitted;
	}
	
	public static ImageFormat getImageFormat(String sFile) throws FileNotFoundException, IOException, ImageException {
		RandomAccessFile raFile = new RandomAccessFile(sFile, "r");
		
/*		if (Utilities.isRawDevice(sFile)) {
			return new RawHDFormat(raFile, sFile, nSectorLength);
		} */
		
		if (raFile.length()==0) throw new ImageException(TIImageTool.langstr("ImageEmpty"));
		
		if (CF7VolumeFormat.vote(raFile) > 50) {
			return new CF7VolumeFormat(raFile, sFile);
		}
		if (CF7ImageFormat.vote(raFile) > 50) {
			return new CF7ImageFormat(raFile, sFile);
		}
		if (SectorDumpFormat.vote(raFile) > 50) {
			return new SectorDumpFormat(raFile, sFile);
		}
		
		if (TrackDumpFormat.vote(raFile) > 50) {
			return new TrackDumpFormat(raFile, sFile);
		}
		
		if (RawHDFormat.vote(raFile) > 50) {
			return new RawHDFormat(raFile, sFile);
		}

		if (MameCHDFormat.vote(raFile) > 50) {
			return new MameCHDFormat(raFile, sFile);
		}

		if (HFEFormat.vote(raFile) > 50) {
			return new HFEFormat(raFile, sFile);
		}
				
		throw new ImageException(sFile + ": " + TIImageTool.langstr("ImageUnknown"));
	}

	/** Used for new images. */
	public static ImageFormat getImageFormat(String sFile, int type, TFileSystem fs) throws FileNotFoundException, IOException, ImageException {
		RandomAccessFile raFile = new RandomAccessFile(sFile, "rw");
		
		switch (type) {
		case SECTORDUMP:
			return new SectorDumpFormat(raFile, sFile, fs);
		case TRACKDUMP:
			return new TrackDumpFormat(raFile, sFile, fs);
		case HFE:
			return new HFEFormat(raFile, sFile);
		}
		return null;
	}		
	
	public void reopenForWrite() throws IOException {
		if (m_ImageFile != null) m_ImageFile.close();
		m_ImageFile = new RandomAccessFile(m_sImageName, "rw");		
	}
	
	public void reopenForRead() throws IOException {
		if (m_ImageFile != null) m_ImageFile.close();
		m_ImageFile = new RandomAccessFile(m_sImageName, "r");		
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

	abstract void setGeometry(TFileSystem fs);

	
	/** Delivers the position on the image file by track and sector.
		Result is returned as a Location instance.
		Called by the floppy image types only.
	*/
	Location LBAToCHS(int nSectorNumber) throws ImageException {
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
		
		// track is linearly counted over both sides, cylinder is one side only
		// track is the logical count (TI file system), cylinder is physical count
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
	abstract int loadTrack(Location loc) throws IOException, ImageException;

	/** If the image file does not yet exist. */
	abstract void createTrack();
	
	/** Common part of the readTrack method. */
	int readTrack(int nSectorNumber) throws ImageException, IOException {
		Location loc = null;
		// System.out.println("Read track of sector " + nSectorNumber);
		if (nSectorNumber != 0) {
			
			if (m_nSectorsByFormat == NONE) {
				// We do not know the number of sectors yet. Read track 0.
				System.out.println("Need to read track 0 first");
				readTrack(0);
			}
		
			loc = LBAToCHS(nSectorNumber);
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
		
		m_currentCylinder = loc.cylinder;
		m_currentHead = loc.head;
		m_currentTrack = loc.track;
		
		// System.out.println("cyl = " + m_currentCylinder + ", head = " + m_currentHead);

		m_abyTrack = new byte[m_tracklen[loc.track]];
		if (m_bFormatCommitted) {
			System.out.println("Read track " + loc.track);
			readFromImage(m_trackpos[loc.track]);
		}
		else {
			createTrack();
		}
		
		m_cells = m_abyTrack.length * 4;  // All bits for either head
				
		// Reset to start
		m_positionInTrack = 0;
		
		int secindex = -1;
		secindex = loadTrack(loc);
		return secindex;		
	}
	
	/** Overridden by CF7Format. */
	void readFromImage(int offset) throws IOException {
		m_ImageFile.seek(offset);
		m_ImageFile.readFully(m_abyTrack);
	}
	
	abstract void formatTrack(int cylinder, int head, int sectors, int density, int[] gap);
	
	abstract String getDumpFormatName();
	
	public abstract void flush() throws IOException;
	
	abstract void createEmptyImage(File newfile, int sides, int density, int tracks, int sectors, boolean format) throws ImageException, FileNotFoundException, IOException;
	
	int checkCRC(boolean fix, boolean reset) throws IOException {	
		return NONE;
	}
	
	void writeBack(Sector sect) throws IOException, ImageException {
	}
		
	public static void createFloppyImage(File newImageFile, String volumeName, int type, int sides, int density, int tracks, boolean format) throws IOException, ImageException {

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
		
		// Save image
	}
	
	void setFillPattern(byte[] pattern) {
		m_abyEmpty = pattern;
	}
}
