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
import de.mizapf.timt.util.Utilities;

public abstract class ImageFormat  {

	RandomAccessFile m_FileSystem;

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
	
	public final static int SINGLE_DENSITY = 0;
	public final static int DOUBLE_DENSITY = 1;
	public final static int HIGH_DENSITY = 2;
	public final static int ULTRA_DENSITY = 3;
	
	protected int m_nCylinders;
	protected int m_nHeads;
	protected int m_nSectorsPerTrack;
	protected int m_nSectorLength;

	protected int m_nTrackLength;
	
	protected int m_nDensity; // specific for floppies
	
	protected byte[] m_abyTrack;
	protected int m_nCurrentTrack; 

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

	protected ImageFormat(RandomAccessFile filesystem, String sImageName, int nSectorLength) throws IOException, ImageException {
		m_FileSystem = filesystem;
		m_sImageName = sImageName;
		setGeometry(false /*Utilities.isRawDevice(sImageName)*/);
		m_nCurrentTrack = NOTRACK;
		m_abyTrack = new byte[m_nTrackLength];
		m_nSectorLength = nSectorLength;
	}
	
	protected ImageFormat() {
	}
	
	protected ImageFormat(File newfile) throws FileNotFoundException {
		m_FileSystem = new RandomAccessFile(newfile, "rw");
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
	
	abstract Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException;
	
	abstract void writeSector(int nNumber, byte[] abySector, boolean bNeedReopen) throws IOException, ImageException;
	
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
	
	public static ImageFormat getImageFormat(String sFile, int nSectorLength) throws FileNotFoundException, IOException, ImageException {
		RandomAccessFile fileSystem = new RandomAccessFile(sFile, "r");
		
/*		if (Utilities.isRawDevice(sFile)) {
			return new RawHDFormat(fileSystem, sFile, nSectorLength);
		} */
		
		if (fileSystem.length()==0) throw new ImageException("Empty image");
		
		if (CF7VolumeFormat.vote(fileSystem) > 50) {
			return new CF7VolumeFormat(fileSystem, sFile, nSectorLength);
		}
		if (SectorDumpFormat.vote(fileSystem) > 50) {
			return new SectorDumpFormat(fileSystem, sFile, nSectorLength);
		}
		
		if (TrackDumpFormat.vote(fileSystem) > 50) {
			return new TrackDumpFormat(fileSystem, sFile, nSectorLength);
		}
		
		if (RawHDFormat.vote(fileSystem) > 50) {
			return new RawHDFormat(fileSystem, sFile, nSectorLength);
		}

		if (MessCHDFormat.vote(fileSystem) > 50) {
			return new MessCHDFormat(fileSystem, sFile, nSectorLength);
		}

		if (HFEFormat.vote(fileSystem) > 50) {
			return new HFEFormat(fileSystem, sFile, nSectorLength);
		}
				
		throw new ImageException(sFile + ": Unknown format or image corrupted");
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
		int sector = nSectorNumber % m_nSectorsByFormat;
		
		int track = nSectorNumber / m_nSectorsByFormat;
		int cylinder = track;
		int head = 0;
		
		if (track >= m_nCylinders) {
			// Next head
			cylinder = 2 * m_nCylinders - 1 - track;
			head++;
		}
		if (cylinder < 0) throw new ImageException("Image defines more than two heads");

		Location loc = new Location(cylinder, head, sector, track);
		
		// System.out.println("sector " + nSectorNumber + " in " + loc);
		return loc;

		// System.out.println("Sector " + nSectorNumber + " is in track " + loc.track + ", cyl=" + loc.cylinder + ", head=" + loc.head);
		//System.out.println("m_header.number_of_track = " + m_header.number_of_track + ", nSectorNumber = " + nSectorNumber);
	}	
	
	abstract int readTrack(int nSectorNumber) throws IOException, ImageException;
	
	abstract String getDumpFormatName();
	
	abstract void flush() throws IOException;
	
	abstract void createEmptyImage(File newfile, int sides, int density, int tracks, boolean format) throws ImageException, FileNotFoundException, IOException;
	
	int checkCRC(boolean fix, boolean reset) throws IOException {	
		return NONE;
	}
}
