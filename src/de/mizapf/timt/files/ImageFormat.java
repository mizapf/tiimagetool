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
import de.mizapf.timt.util.Utilities;

public abstract class ImageFormat  {

	RandomAccessFile m_FileSystem;

	String m_sImageName;
	
	protected int m_nCylinders;
	protected int m_nHeads;
	protected int m_nSectorsPerTrack;
	protected int m_nSectorLength;

	protected int m_nTrackLength;
	
	protected int m_nTotalSectors;
	
	protected int m_nDensity; // specific for floppies
	
	protected byte[] m_abyTrack;
	protected int m_nCurrentTrack; 

	protected static final int TRACK = 0;
	protected static final int SECTOR = 1;

	protected final static int NOTRACK = -1;

	protected ImageFormat(RandomAccessFile filesystem, String sImageName, int nSectorLength) throws IOException, ImageException {
		m_FileSystem = filesystem;
		m_sImageName = sImageName;
		setGeometry(Utilities.isRawDevice(sImageName));
		m_nCurrentTrack = NOTRACK;
		m_abyTrack = new byte[m_nTrackLength];
		m_nSectorLength = nSectorLength;
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
	
	Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		byte[] abySector = new byte[m_nSectorLength];
		// Get sector offset in track
		//			System.out.println("Read sector " + nSectorNumber);
		int[] offset = new int[2];
		getOffset(nSectorNumber, offset);
		System.arraycopy(m_abyTrack, offset[SECTOR], abySector, 0, m_nSectorLength);
		return new Sector(nSectorNumber, abySector);
	}
	
	abstract void writeSector(int nNumber, byte[] abySector, boolean bFM, boolean bNeedReopen) throws IOException, ImageException;
	
	int getDensity() {
		return m_nDensity;
	}
	
	void close() throws IOException {
		m_FileSystem.close();
	}
	
	long getLastModifiedTime() {
		java.io.File file = new java.io.File(m_sImageName);
		return file.lastModified();
	}
	
	public static ImageFormat getImageFormat(String sFile, int nSectorLength) throws FileNotFoundException, IOException, ImageException {
		RandomAccessFile fileSystem = new RandomAccessFile(sFile, "r");
		
		if (Utilities.isRawDevice(sFile)) {
			return new RawHDFormat(fileSystem, sFile, nSectorLength);
		}
		
		if (fileSystem.length()==0) throw new ImageException("Empty image");
		
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
		throw new ImageException("Unknown format or image corrupted");
	}

	public void reopenForWrite() throws IOException {
		if (m_FileSystem != null) m_FileSystem.close();
		m_FileSystem = new RandomAccessFile(m_sImageName, "rw");		
	}
	
	public void reopenForRead() throws IOException {
		if (m_FileSystem != null) m_FileSystem.close();
		m_FileSystem = new RandomAccessFile(m_sImageName, "r");		
	}

	abstract void setGeometry(boolean bSpecial) throws IOException, ImageException;
			
	abstract void getOffset(int nSectorNumber, int[] offset) throws IOException, ImageException;
	
	abstract String getDumpFormatName();
}
