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
    
    Copyright 2021 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.files;
import java.io.IOException;
import java.io.FileNotFoundException;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.*;

public abstract class FloppyImageFormat extends FileImageFormat {

	/** Sector count according to the sectors found on a track. */
	private int m_nCountedSectors;  
		
	/** Sectors per track according to the image format. */
	int m_nTracks;
	
	/** Sectors per track according to the image format. */
	int m_nSides;

	/** Sectors per track, determined from the file size or metadata. */
	int m_nSectorsPerTrack;
	
	/** Format index */
	protected int m_nFormatIndex;	
	
	/** Used for new images. Creates the image file.  */
/*	public static ImageFormat getImageFormat(String sFile, int type, TFileSystem fs) throws FileNotFoundException, IOException, ImageException {
		
		switch (type) {
		case SECTORDUMP:
			return new SectorDumpFormat(sFile);
		case TRACKDUMP:
			return new TrackDumpFormat(sFile);
		case HFE:
			return new HFEFormat(sFile);
		}
		return null;
	}
	*/
	protected FloppyImageFormat(String sImageName) throws FileNotFoundException, IOException, ImageException {
		super(sImageName);
	}
	
	protected FloppyImageFormat(String sImageName, boolean bRead) throws FileNotFoundException, IOException, ImageException {
		super(sImageName, bRead);
	}
	
	int getCountedSectorsPerTrack() {
		return m_nCountedSectors;
	}
	
	/** Must be overridden by formats like SectorDumpFormat which cannot rely on this. */
	abstract int getSectorsPerTrack();
	
	/** May be overridden by formats like SectorDumpFormat which cannot rely on this. */
	public int getTracks() {
		return m_nTracks;
	}
	
	/** May be overridden by formats like SectorDumpFormat which cannot rely on this. */
	public int getSides() {
		return m_nSides;
	}
	
	/** Delivers the position on the image file by track and sector.
		Result is returned as a Location instance.
	*/
	Location lbaToChs(int nSectorNumber) throws ImageException {
		// Now we should know the sector count, so we can calculate the track and
		// sector. The track number is counted from head 0, cylinder 0 ... max,
		// then head 1, cylinder max ... 0.
		
		// For sector 0 we do not need the geometry.
		if (nSectorNumber == 0) {
			return new Location(0, 0, 0);
		}
		
		if (m_nTotalSectors != 0 && nSectorNumber >= m_nTotalSectors)  {
			// Thread.currentThread().dumpStack();
			System.out.println("total sectors = " + m_nTotalSectors + ", secno = " + nSectorNumber);
			throw new ImageException(String.format(TIImageTool.langstr("ImageSectorHigh"), m_nTotalSectors));
		}					
		int sector = nSectorNumber % getSectorsPerTrack();
		int lintrack = nSectorNumber / getSectorsPerTrack();
		
		// track is linearly counted over both sides, cylinder is one side only
		// track is the logical count (TI file system), cylinder is physical count
		int cylinder = trackToCyl(getTracks(), lintrack);
		int head = trackToHead(getTracks(), lintrack);

		// System.out.println("total=" + m_nTotalSectors + ", sec=" + nSectorNumber + ", secbyform=" + getCountedSectors() + ", track=" + track + ", m_nCylinders=" + m_nCylinders + ", cylinder=" + cylinder);
		
		if (cylinder < 0) throw new ImageException(TIImageTool.langstr("ImageInvalidHeads"));
		
		Location loc = new Location(cylinder, head, sector);
		// System.out.println("lba2chs(" + nSectorNumber + ") = " + loc);

		return loc;
	}
	
	void setVolumeInformation() throws ImageException, IOException {

		// Use UNSET for the next accesses until we set the header
		m_nVibCheck = FloppyFileSystem.UNSET;
		
		Sector sector0 = readSector(0);	
		try {
			m_fs.setVolumeName(Utilities.getString10(sector0.getData(), 0));
		}
		catch (InvalidNameException inx) {
			m_fs.setVolumeName0("UNNAMED");
		}
						
		m_nVibCheck = setupGeometry(sector0.getData());
		setupAllocationMap();

/*		for (int i=0; i < m_nTotalSectors; i++) {
			try {
				Sector test = readSector(i);
				System.out.println(Utilities.hexdump(test.getData()));
			}
			catch (ImageException icx) {
				System.out.println("Sector " + i + " not found");
			}
		} */
	}
	
	int chsToLba(Location loc) {
		// We assume the usual sector ordering on floppies
		int lintrack;
		if (loc.head > 0) {
			lintrack = 2 * getTracks() - loc.cylinder - 1;
		}
		else {
			lintrack = loc.cylinder;
			if (lintrack == 0) return loc.sector;
		}
		return lintrack * getSectorsPerTrack() + loc.sector;	
	}

	static int trackToCyl(int totalcyl, int track) {
		return (track < totalcyl)? track : (2*totalcyl - 1 - track);
	}

	static int trackToHead(int totalcyl, int track) {
		return (track < totalcyl)? 0 : 1;
	}
	
	/** Find the image sector by the linear sector number. */
	// Each FIB is read twice: for the file name, and for the file contents
	@Override
	ImageSector findSector(int number) throws ImageException {
		for (ImageSector is : m_codec.getDecodedSectors()) {
			if (is.getNumber() == number) {
				return is;
			}
		}
		return null;
	}
	
	/** Format units are tracks in this format. */
	long getFormatUnitPosition(int funum) {
		return funum * getFormatUnitLength(funum);
	}	
	
	protected int setupGeometry(byte[] sec0) {
		return ((FloppyFileSystem)m_fs).getGeometryFromVIB(sec0);
	}
	
	protected void setupAllocationMap() throws ImageException, IOException {
		Sector sector0 = readSector(0);	
		((FloppyFileSystem)m_fs).setupAllocationMap(sector0.getData());
	}
}
