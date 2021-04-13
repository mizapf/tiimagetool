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
	
	/** Cached sectors of the current track (Read cache). */
	Sector[] m_buffsector;
	protected byte[] m_abyBuffer;
	
	/** Write cache. */
	SectorCache m_cache;
	
	/** Current location */
	Location m_locCurrent;
	
	byte[] m_abyEmpty;
	
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
	
	protected int m_nCurrentTrack; // CHD and RawHD

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
	protected int m_codeRate = 1;

	protected boolean m_bFromFile;
	
	protected FormatCodec m_codec;

	protected ImageFormat(RandomAccessFile rafile, String sImageName) throws IOException, ImageException {
		TFileSystem fs = determineFileSystem(rafile);
		init(rafile, sImageName, fs);
		m_bFromFile = true;
	}
	
	/** Newly created; no image file yet. */
	protected ImageFormat(RandomAccessFile rafile, String sImageName, TFileSystem fs) throws IOException, ImageException {
		init(rafile, sImageName, fs);
		m_bFromFile = false;
	}
	
	final void init(RandomAccessFile rafile, String sImageName, TFileSystem fs) {
		m_ImageFile = rafile;
		m_sImageName = sImageName;
		m_nSectorLength = Volume.SECTOR_LENGTH;
		m_nCurrentTrack = NOTRACK;
		m_bWriteThrough = false;
		setGeometry((FloppyFileSystem)fs);
		m_fs = fs;
	}
	
	// Called from Volume.createFloppyImage (needed by subclass contructors)
	protected ImageFormat() {
		m_bWriteThrough = false;
		m_bFromFile = false;
	}
	
	protected ImageFormat(File newfile) throws FileNotFoundException {
		m_ImageFile = new RandomAccessFile(newfile, "rw");
		m_bFromFile = false;
	}
	
	void setSectorCache(SectorCache cache) {
		m_cache = cache;
		m_codec.setFillPattern(cache.getFillPattern());
	}
	
	protected void writeThrough(boolean bWriteTh) {
		m_bWriteThrough = bWriteTh;
	}
	
	/** Needed from SectorEditFrame. */
	public int getTotalSectors() {
		return m_nTotalSectors;
	}
	
	/** Reads a sector.
		@throws ImageException if the sector cannot be found.
	*/
	public Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		if (nSectorNumber > 10000) throw new ImageException(String.format(TIImageTool.langstr("BadSectorNumber"), nSectorNumber)); 
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		m_locCurrent = ffs.lbaToChs(nSectorNumber);
		int index = getBufferIndex(m_locCurrent);
		// System.out.println("Sector " + nSectorNumber + ": index=" + index);
		m_codec.loadBuffer(index);
		Sector sect = m_codec.readSector(m_locCurrent, nSectorNumber);
		if (sect == null)
			throw new ImageException(String.format(TIImageTool.langstr("SectorNotFound"), nSectorNumber));			
		return sect;
	}

	int getDensity() {
		return m_nDensity;
	}
	
	/** Returns the type of the format. */
	abstract int getFormatType();
	
	int getImageType() {
		return NOTYPE;
	}
	
	void close() throws IOException {
	//	prepareImageFile();
	//	flush();
		m_ImageFile.close();
	}
	
	void setFileSystem(TFileSystem fs) {
		m_fs = fs;
	}
	
	public TFileSystem getFileSystem() {
		return m_fs;
	}
	
	long getLastModifiedTime() {
		java.io.File file = new java.io.File(m_sImageName);
		return file.lastModified();
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

	/** Used for new images. Creates the image file.  */
	public static ImageFormat getImageFormat(String sFile, int type, TFileSystem fs) throws FileNotFoundException, IOException, ImageException {
		RandomAccessFile raFile = new RandomAccessFile(sFile, "rw");
		
		switch (type) {
		case SECTORDUMP:
			return new SectorDumpFormat(raFile, sFile, fs);
		case TRACKDUMP:
			return new TrackDumpFormat(raFile, sFile, fs);
		case HFE:
			return new HFEFormat(raFile, sFile, fs);
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

	abstract TFileSystem determineFileSystem(RandomAccessFile rafile) throws IOException, ImageException;

	final void setGeometry(FloppyFileSystem ffs) {
		// Calculate length
		m_nHeads = ffs.getHeads();
		m_nCylinders = ffs.getTracksPerSide();
		m_nSectorsPerTrack = ffs.getTotalSectors() / (m_nHeads * ffs.getTracksPerSide());
		m_nDensity = ffs.getDensity();
		m_encoding = (m_nSectorsPerTrack < 16)? FM : MFM;
		System.out.println("Cylinders = " + m_nCylinders + ", heads = " + m_nHeads + ", sectors = " +  m_nSectorsPerTrack
			+ ", density = " +  m_nDensity + ", encoding = " + ((m_encoding==0)? "FM" : "MFM"));
	}
	
	void setupBuffers(String sImageName, boolean bInitial) {
		System.out.println("FIXME: Implement setupBuffers");
	}
	
	boolean bufferNeedsFlush(int cyl, int head) {
		return m_currentCylinder != cyl && m_currentHead != head;
	}

	/** Saves all modified sectors to the image file. This may be overridden
		by subclasses, e.g. HFEFormat.
		@param bDoSave Save also when there are no changes. Needed when saving to
		a new image.
	*/
	void saveImage(boolean bDoSave) throws IOException, ImageException {
		for (int i=0; i < m_fs.getTotalSectors(); i++) {
			// System.out.println("Writing sector " + i);
			// Read the sector from the cache and get null on miss
			Sector sect = m_cache.read(i, false);
			
			m_locCurrent = m_fs.lbaToChs(i);
			
			// Determine if the sector is already in the codec; if not, flush
			// the codec and load the appropriate buffer
			int index = getBufferIndex(m_locCurrent);
			// System.out.println("Sector " + i + ": index=" + index);
			m_codec.loadBuffer(index);
			if (bDoSave) m_codec.touch();
			if (sect != null) m_codec.writeSector(m_locCurrent, sect.getBytes());
			// writeToBuffer(sect);
		}
		m_codec.flush();
		m_codec.setInitial(false);
		m_cache.setCommitted(true);
		// setCommitted(true);
		m_cache.wipe();
	}
	
	/** Overridden by subclasses if required. */
	int getFullTrackLength(int nTrackLength) {
		return nTrackLength;
	}
	
	/** Overridden by subclasses if required. Normally, cylinder and head
	    must match. */
	boolean bufferLoaded(int cyl, int head) {
		return (m_currentCylinder == cyl && m_currentHead == head);
	}
	
	abstract int getBufferIndex(Location loc);
	
	abstract String getDumpFormatName();
		
	abstract void prepareImageFile() throws FileNotFoundException, IOException;
		
	int checkCRC(boolean fix, boolean reset) throws IOException {	
		return NONE;
	}
	
	/** Write a single sector to the buffer. Load the buffer if required. */
	void writeToBuffer(Sector sect) throws IOException, ImageException {
		System.out.println("FIXME: Implement writeToBuffer");
	}
	
	void setCommitted(boolean bCommit) {
		m_bFromFile = bCommit;
	}
	
	/** Needed for formatting a track. */
	byte[] getFillPattern() {
		return m_cache.getFillPattern();
	}
}
