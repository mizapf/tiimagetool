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
	
	int[] m_bufferpos;
	int[] m_bufferlen1;
	int m_positionInBuffer;

	boolean m_bBufferChanged;
	
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
		m_ImageFile = rafile;
		m_sImageName = sImageName;
		m_nSectorLength = Volume.SECTOR_LENGTH;
		setGeometryAndCodec(false /*Utilities.isRawDevice(sImageName)*/);
		m_codec.setFile(rafile);
		m_nCurrentTrack = NOTRACK;
		m_bWriteThrough = false;
		m_bFromFile = true;
	}
	
	/** Newly created; no image file yet. */
	protected ImageFormat(RandomAccessFile rafile, String sImageName, TFileSystem fs) throws IOException, ImageException {
		m_fs = fs;
		m_ImageFile = rafile;
		m_sImageName = sImageName;
		m_nSectorLength = Volume.SECTOR_LENGTH;
		setGeometryAndCodec(sImageName, fs, true);
		m_codec.setFile(rafile);
		m_nCurrentTrack = NOTRACK;
		// System.out.println("Track length = " + m_nTrackLength + ", class = " + this.getClass().getName());
		// m_abyBuffer = new byte[m_nTrackLength];
		m_bWriteThrough = false;
		m_bFromFile = false;
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
		System.out.println("Sector " + nSectorNumber + ": index=" + index);
		m_codec.loadBuffer(index);
		Sector sect = m_codec.readSector(m_locCurrent, nSectorNumber);
		if (sect == null)
			throw new ImageException(String.format(TIImageTool.langstr("SectorNotFound"), nSectorNumber));			
		return sect;
	}

	/** Reads a sector from the medium. */
	// public abstract Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException;
	
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

	/** Write a sector to the buffer. Also sets the mark and position in the
		argument sector. */
	public void writeSector(Sector sect) throws IOException, ImageException {
		int secindex = locateInBuffer(sect.getNumber());
		if (secindex == NONE) throw new ImageException(String.format(TIImageTool.langstr("SectorNotFound"), sect.getNumber()));
		// Write the new data
		// Don't forget to clone the bytes!
		byte[] abyNewcontent = new byte[256];
		System.arraycopy(sect.getBytes(), 0, abyNewcontent, 0, 256);
		
		// Write the sector contents into the read cache
		// so that the track image can be created on flush
		System.out.println("Writing sector " + sect.getNumber());
		m_buffsector[secindex].setData(abyNewcontent);
		sect.setMark(m_buffsector[secindex].getMark());
		sect.setPosition(m_buffsector[secindex].getPosition());
		// System.out.println(m_buffsector[secindex]);
		// System.out.println("CRC = " + Utilities.toHex(m_buffsector[secindex].getCrc(),4));
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
	abstract void setGeometryAndCodec(boolean bSpecial) throws IOException, ImageException;

	abstract void setGeometryAndCodec(String sImageName, TFileSystem fs, boolean bInitial);

	final void setBasicParams(FloppyFileSystem ffs) {
		// Calculate length
		m_nHeads = ffs.getHeads();
		m_nCylinders = ffs.getTracksPerSide();
		m_nSectorsPerTrack = ffs.getTotalSectors() / (m_nHeads * ffs.getTracksPerSide());
		m_nDensity = ffs.getDensity();
		m_encoding = (m_nSectorsPerTrack < 16)? FM : MFM;
	}
	
	void setupBuffers(String sImageName, boolean bInitial) {
		System.out.println("FIXME: Implement setupBuffers");
	}
	
	/** Read a track, given the number of a sector within that track.
		@param nSectorNumber LBA number of the sector.
		@return Index of the desired sector within its track.
	*/
	abstract int loadBuffer(Location loc) throws IOException, ImageException;

	/** If the image file does not yet exist. */
	abstract void createBuffer(int cylinder, int head, int track);
	
	/** Common part of the readBuffer method. Read the block into the buffer
		that contains the specified sector. For a newly created file system, 
		create the buffer.
	*/
	int locateInBuffer(int nSectorNumber) throws ImageException, IOException {
		System.out.println("readBuffer (for sector " + nSectorNumber + ")");
		Location loc = null;
		FloppyFileSystem ffs = (FloppyFileSystem)m_fs;
		// System.out.println("Read track of sector " + nSectorNumber);
		if (nSectorNumber != 0) {
			
			if (ffs.getCountedSectors() == NONE) {
				// We do not know the number of sectors yet. Read track 0.
				System.out.println("Need to read track 0 first");
				locateInBuffer(0);
			}
		
			loc = ffs.lbaToChs(nSectorNumber);
		}
		else {
			loc = new Location(0, 0, 0, 0);
		}
		
		// Do we have that buffer already?
		if (bufferLoaded(loc.cylinder, loc.head)) {
			// Iterate over the sectors and return its index in the list.
			for (int i=0; i < m_buffsector.length; i++) {
				if (m_buffsector[i].getNumber()==nSectorNumber) return i;
			}
			return NONE;
		}
		
		// So the buffer is not the right one
		if (!m_bFromFile) {
			System.out.println("Need to create track " + loc.track);
		}
		else {
			System.out.println("Need to load cylinder " + loc.cylinder + ", head " + loc.head);
		}
		prepareImageFile();
		
		// Write back the last buffer
		if (bufferNeedsFlush(loc.cylinder, loc.track))
			flush();

		// We need the track - either from disk or newly created. 

		m_currentTrack = loc.track; // Required for readBufferFromImage

		if (m_bFromFile) {
			// We need to read the whole cylinder.
			System.out.println("Read track " + loc.track + " from position " + m_bufferpos[loc.track]);
			readBufferFromImage(m_bufferpos[loc.track]);
		}
		else {
			// CreateBuffer needs the old cylinder number
			createBuffer(loc.cylinder, loc.head, loc.track);
		}

		m_currentCylinder = loc.cylinder;
		m_currentHead = loc.head;
		
		// Only interesting for HFEFormat
		m_cells = m_abyBuffer.length * 4;  // All bits for either head
				
		// Reset to start
		m_positionInBuffer = 0;
		
		// Now load the track
		int secindex = -1;
		secindex = loadBuffer(loc);

		// Return the position of the desired sector in this track
		return secindex;		
	}
	
	boolean bufferNeedsFlush(int cyl, int head) {
		return m_currentCylinder != cyl && m_currentHead != head;
	}

	/** Overridden by CF7Format. */
	void readBufferFromImage(int offset) throws IOException {
		m_abyBuffer = new byte[getFullTrackLength(m_bufferlen1[m_currentTrack])];
		m_ImageFile.seek(offset);
		m_ImageFile.readFully(m_abyBuffer);
	}
	
	/** Saves all modified sectors to the image file. This may be overridden
		by subclasses, e.g. HFEFormat.
	*/
	void saveImage() throws IOException, ImageException {
		for (int i=0; i < m_fs.getTotalSectors(); i++) {
			System.out.println("Writing sector " + i);
			// Read the sector from the cache and get null on miss
			Sector sect = m_cache.read(i, false);
			
			m_locCurrent = m_fs.lbaToChs(i);
			
			// Determine if the sector is already in the codec; if not, flush
			// the codec and load the appropriate buffer
			int index = getBufferIndex(m_locCurrent);
			System.out.println("Sector " + i + ": index=" + index);
			m_codec.loadBuffer(index);
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
	
	abstract void formatTrack(int cylinder, int head, int sectors, int density, int[] gap);
	
	abstract String getDumpFormatName();
	
	public abstract void flush() throws IOException;
	
	abstract void prepareImageFile() throws FileNotFoundException, IOException;
	
	abstract void createEmptyImage(File newfile, int sides, int density, int tracks, int sectors, boolean format) throws ImageException, FileNotFoundException, IOException;
	
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
	
	public static void createFloppyImage(File newImageFile, String volumeName, int type, int sides, int density, int tracks, boolean format) throws IOException, ImageException {
		System.out.println("FIXME: Called legacy function ImageFormat.createFloppyImage");
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
	}
	
	/** Needed for formatting a track. */
	byte[] getFillPattern() {
		return m_cache.getFillPattern();
	}
}
