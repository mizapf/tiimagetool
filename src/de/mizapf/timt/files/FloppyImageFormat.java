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
	// private int m_nCountedSectors;  
		
	/** Sectors per track according to the image format. */
	int m_nTracks;
	
	/** Sectors per track according to the image format. */
	int m_nSides;

	/** Sectors per track, determined from the file size or metadata. */
	int m_nSectorsPerTrack;
	
	protected FloppyImageFormat(String sImageName) throws FileNotFoundException, IOException, ImageException {
		super(sImageName);
	}
	
	protected FloppyImageFormat(String sImageName, FormatParameters params) throws FileNotFoundException, IOException, ImageException {
		super(sImageName, params);
		m_nSectorsPerTrack = params.sectors;
		m_nTracks = params.cylinders;		
		m_nSides = params.heads;		
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
	static Location lbaToChs(int nSectorNumber, int tracks, int nSecPerTrack) throws ImageException {
		// Now we should know the sector count, so we can calculate the track and
		// sector. The track number is counted from head 0, cylinder 0 ... max,
		// then head 1, cylinder max ... 0.
		
		// For sector 0 we do not need the geometry.
		if (nSectorNumber == 0) {
			return new Location(0, 0, 0);
		}
		
/*		if (m_nTotalSectors != 0 && nSectorNumber >= m_nTotalSectors)  {
			// Thread.currentThread().dumpStack();
			System.out.println("total sectors = " + m_nTotalSectors + ", secno = " + nSectorNumber);
			throw new ImageException(String.format(TIImageTool.langstr("ImageSectorHigh"), m_nTotalSectors));
		} */					
		int sector = nSectorNumber % nSecPerTrack;
		int lintrack = nSectorNumber / nSecPerTrack;
		
		// track is linearly counted over both sides, cylinder is one side only
		// track is the logical count (TI file system), cylinder is physical count
		int cylinder = trackToCyl(tracks, lintrack);
		int head = trackToHead(tracks, lintrack);
		
		// System.out.println("secnum=" + nSectorNumber + ", sec/track=" + nSecPerTrack + " -> cyl=" + cylinder + ", head=" + head + ", sector=" + sector + ", tracks=" + tracks + ", lintrack=" + lintrack); 

		// System.out.println("total=" + m_nTotalSectors + ", sec=" + nSectorNumber + ", secbyform=" + getCountedSectors() + ", track=" + track + ", m_nCylinders=" + m_nCylinders + ", cylinder=" + cylinder);
		// System.out.println("m_nTotalSectors=" + m_nTotalSectors);
		
		if (cylinder < 0) throw new ImageException(TIImageTool.langstr("ImageInvalidHeads"));
		
		Location loc = new Location(cylinder, head, sector);
		// System.out.println("lba2chs(" + nSectorNumber + ") = " + loc);

		return loc;
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

	/** Format units are tracks in this format. */
	long getFormatUnitPosition(int funum) {
		return funum * getFormatUnitLength(funum);
	}	
	
	public FloppyFileSystem getFileSystem(byte[] sector0) {
		FloppyFileSystem fs = null; 
		if ((sector0[10] == 0x06) && (sector0[11] == 0x40)) {
			fs = new CF7VolumeFileSystem();
		}
		else
			fs = new FloppyFileSystem();
		
		fs.setImage(this);
		// System.out.println(Utilities.hexdump(sector0));
		fs.configure(sector0);
		return fs;
	}
}
