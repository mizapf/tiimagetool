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
    
    Copyright 2020 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.files;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;
import java.util.ArrayList;
import java.util.Arrays;

/** Represents a TI floppy disk file system. */	
public class FloppyFileSystem extends TFileSystem {

	// Floppy-specific
	
	/** Sector count according to the sectors found on a track. */
	private int			m_nCountedSectors;  
	
	int 		m_nDensity = 0;
	boolean		m_bProtection = false;
	boolean		m_bCF7 = false;

	public final static int UNKNOWN_DENSITY = -1;
	public final static int SINGLE_DENSITY = 0;
	public final static int DOUBLE_DENSITY = 1;
	public final static int HIGH_DENSITY = 2;
	public final static int ULTRA_DENSITY = 3;
	public final static int DOUBLE_DENSITY_16 = 4;
	
	public final static int[] sectorsPerTrack = { 9, 18, 36, 72, 16 };
	public final static int[] densityCode = { 1, 2, 3, 4, 2 };
			
	public FloppyFileSystem() {
		m_bCF7 = false;
		m_bWriteCached = true;
		m_nCountedSectors = NONE;
	}

	FloppyFileSystem(int cylinders, int heads, int secpertrack, int density) {
		setGeometry(cylinders*heads*secpertrack, cylinders, heads, secpertrack, density);
	}
	
	public void setCF7(boolean isCF7) {
		m_bCF7 = isCF7;
	}
	
	public boolean isCF7() {
		return m_bCF7;
	}
		
	@Override
	void setupFromFile(byte[] abySect0, byte[] abyAlloc, boolean bCheck) throws MissingHeaderException, ImageException {
		if (!hasFloppyVib(abySect0) && bCheck) throw new MissingHeaderException();  
		
		// TODO: Check with image
		m_nTotalSectors = Utilities.getInt16(abySect0, 0x0a);
		m_nSectorsPerTrack = abySect0[0x0c] & 0xff;
		
		m_nHeads = abySect0[0x12] & 0xff;
		m_nCylinders = abySect0[0x11] & 0xff;
		int nDensity = getDensityFromCode(abySect0[0x13] & 0xff, m_nSectorsPerTrack);		
		m_nDensity = getDensityFromSectors(m_nSectorsPerTrack);
		if (m_nDensity != nDensity) {
			System.err.println("Wrong density value " + (abySect0[0x13] & 0xff) + " for sector count " + m_nSectorsPerTrack); 
		}
		//System.out.println("Density: " + m_nDensity);
		
		m_nSectorsPerAU = (int)(m_nTotalSectors/1601) + 1;
		m_bProtection = (abySect0[0x10]=='P');
		m_nReservedAUs = 0x21;
		
		m_allocMap = new AllocationMap(m_nTotalSectors / m_nSectorsPerAU, m_nSectorsPerAU, true);
		m_allocMap.setMapFromBitfield(abyAlloc, getAllocMapStart() % SECTOR_LENGTH, 0);
		
		m_sName = Utilities.getString10(abySect0, 0);
	}
	
	void setGeometry(int total, int tracks, int heads, int sectors, int density) {
		m_nTotalSectors = total;
		m_nCylinders = tracks;
		m_nHeads = heads;
		m_nSectorsPerTrack = sectors;
		m_nDensity = density;
		m_nSectorsPerAU = m_nTotalSectors/1600 + 1;
	}
	
	int getDensityFromCode(int code, int sectors) {
		if (sectors==16) return DOUBLE_DENSITY_16;
		for (int i=0; i < densityCode.length; i++) {
			if (code == densityCode[i]) return i;
		}
		return 0;	
	}

	int getDensityFromSectors(int sectors) {
		if (sectors==16) return DOUBLE_DENSITY_16;
		if ((sectors % 9) != 0) {
			System.err.println("Invalid sector count: " + sectors);
		}
		int i=0;
		while ((sectors != 9) && (sectors != 0)) { sectors >>= 1; i++; }
		return i;
	}
	
	int getDensity() {
		return m_nDensity;
	}
	
	int getDensityCode() {
		return densityCode[m_nDensity];
	}
	
	@Override
	public boolean isProtected() {
		return m_bProtection;
	}
	
	int getTracksPerSide() {
		return m_nCylinders;
	}
	
	void setCountedSectors(int sectors) {
		m_nCountedSectors = sectors;
	}

	int getCountedSectors() {
		return m_nCountedSectors;
	}
	
	static int cylToTrack(int totalcyl, int cyl, int head) {
		return (head == 0)? cyl : (2*totalcyl - 1 - cyl);
	}
	
	static int trackToCyl(int totalcyl, int track) {
		return (track < totalcyl)? track : (2*totalcyl - 1 - track);
	}

	static int trackToHead(int totalcyl, int track) {
		return (track < totalcyl)? 0 : 1;
	}

	/** Delivers the position on the image file by track and sector.
		Result is returned as a Location instance.
		Called by the floppy image types only.
	*/
	@Override
	Location lbaToChs(int nSectorNumber) throws ImageException {
		// Now we should know the sector count, so we can calculate the track and
		// sector. The track number is counted from head 0, cylinder 0 ... max,
		// then head 1, cylinder max ... 0.
		
		// The sector offset is redefined here as the sector number in the track
		
		if (m_nTotalSectors != 0 && nSectorNumber >= m_nTotalSectors) 
			throw new ImageException(String.format(TIImageTool.langstr("ImageSectorHigh"), m_nTotalSectors));
		
		int sector = 0;
		int track = 0;
		int cylinder = track;
		int head = 0;
		
		// For sector 0 we do not need the geometry.
		if (nSectorNumber == 0) {
			return new Location(0, 0, 0, 0);
		}
		else {
			sector = nSectorNumber % m_nSectorsPerTrack;
			track = nSectorNumber / m_nSectorsPerTrack;
		}
		
		// track is linearly counted over both sides, cylinder is one side only
		// track is the logical count (TI file system), cylinder is physical count
		cylinder = trackToCyl(m_nCylinders, track);
		head = trackToHead(m_nCylinders, track);
				
/*		if ((m_nSectorsPerTrack < 1) || (getCountedSectors() < 1)) {
			Thread.currentThread().dumpStack();
			throw new ImageException("BUG: m_nSectorsPerTrack = " + m_nSectorsPerTrack + ", sectorsByFormat = " + getCountedSectors());
		} */
		// System.out.println("total=" + m_nTotalSectors + ", sec=" + nSectorNumber + ", secbyform=" + getCountedSectors() + ", track=" + track + ", m_nCylinders=" + m_nCylinders + ", cylinder=" + cylinder);
		
		if (cylinder < 0) throw new ImageException(TIImageTool.langstr("ImageInvalidHeads"));

		Location loc = new Location(cylinder, head, sector, track);
		
		// System.out.println("sector " + nSectorNumber + " in " + loc);
		return loc;

		// System.out.println("Sector " + nSectorNumber + " is in track " + loc.track + ", cyl=" + loc.cylinder + ", head=" + loc.head);
		//System.out.println("m_header.number_of_track = " + m_header.number_of_track + ", nSectorNumber = " + nSectorNumber);
	}
	
	@Override
	int chsToLba(int cylinder, int head, int sector) {
		int seclba = 0;
		if (head==0) return cylinder*getSectors() + sector;
		else return (2*m_nCylinders - 1 - cylinder) * getSectors() + sector;
	}
		
	// Sector 0, bytes 0x38 - 0xff
	// Max 1600 sectors
	@Override
	int getAllocMapStart() {
		return 0x38;		
	}
	
	@Override
	int getAllocMapEnd() {
		return SECTOR_LENGTH - 1; 		
	}
	
	@Override
	Sector[] getAllocationMapSectors() {
		Sector[] list = new Sector[1];	
		list[0] = new Sector(0, createVIB());
		return list;
	}
	
	@Override
	byte[] createVIB() {
		// Create a new VIB
		byte[] abyNewVIB = new byte[SECTOR_LENGTH];

		Utilities.setString(abyNewVIB, 0, getName(), 10);
		
		Utilities.setInt16(abyNewVIB, 0x0a, m_nTotalSectors);
		abyNewVIB[0x0c] = (byte)(m_nSectorsPerTrack & 0xff);
		Utilities.setString(abyNewVIB, 0x0d, "DSK", 3);
		abyNewVIB[0x10] = m_bProtection? (byte)'P' : (byte)' ';
		abyNewVIB[0x11] = (byte)(m_nCylinders & 0xff);
		abyNewVIB[0x12] = (byte)(m_nHeads & 0xff);
		abyNewVIB[0x13] = (byte)(getDensityCode() & 0xff);
		
		if (!m_bCF7) {
			Directory[] dirs = m_dirRoot.getDirectories();
			for (int i=0; i < 3; i++) {
				if (i < dirs.length) {
					Directory sub = dirs[i];
					Utilities.setString(abyNewVIB, 0x14 + i*12, sub.getName(), 10);
					Utilities.setInt16(abyNewVIB, 0x1e + i*12, sub.getFileIndexSector()); 					
				}
				else {
					// No directory
					for (int j=0; j < 12; j++) abyNewVIB[0x14 + j + i*12] = (byte)0;
				}
			}
		}
		else {
			// Clear the DIR area for CF7
			for (int i=0x14; i<0x38; i++) abyNewVIB[i] = (byte)0x00;
		}
		byte[] map = m_allocMap.toBitField();
		for (int j=0; j < map.length; j++) {
			abyNewVIB[j+0x38] = map[j];
		}
		// Fill the rest with ff (required by TIFDC and BWG)
		for (int j=map.length; j < (256-0x38); j++) {
			abyNewVIB[j+0x38] = (byte)0xff;
		}
		
		return abyNewVIB;
	}
	
	@Override
	public int getAllRequiredSectors(int nAUSize) {
		return 0;	
	}
		
	public String getFloppyFormatName() {
		StringBuilder sb = new StringBuilder();
		sb.append((m_nHeads==2)? "DS" : "SS");
		switch (m_nDensity) {
		case 0:
			sb.append("S"); break;
		case 1: 
		case 4:
			sb.append("D"); break;
		case 2:
			sb.append("H"); break;
		case 3:
			sb.append("U"); break;
		default:
			return TIImageTool.langstr("Invalid");
		}		
		sb.append("D");
		return sb.toString();
	}
	
	/** Create an empty file system. */
	@Override
	Sector[] initialize(FormatParameters param) {
	
		ArrayList<Sector> list = new ArrayList<Sector>();
		
		// Get parameters
		m_sName = param.name;
		m_nTotalSectors = param.heads * param.cylinders * param.sectorsPerTrack;
		m_bProtection = param.protect;
		m_nCylinders = param.cylinders;
		m_nHeads = param.heads;
		m_nSectorsPerAU = m_nTotalSectors/1600 + 1;
		m_nSectorsPerTrack = param.sectorsPerTrack;
		m_nDensity = param.density;
		
		m_allocMap = new AllocationMap(m_nTotalSectors);
		m_allocMap.allocate(0);
		m_allocMap.allocate(1);
		list.add(new Sector(0, createVIB()));
	
		byte[] empty = new byte[SECTOR_LENGTH];
		Arrays.fill(empty, 0, SECTOR_LENGTH, (byte)0x00);
		
		// Sector 1 (FDIR) is empty
		list.add(new Sector(1, empty));
				
		Sector[] slist = new Sector[list.size()];
		list.toArray(slist);
		return slist;
	}
		
	public int getSectorsPerTrack() {
		return m_nTotalSectors / (m_nCylinders * m_nHeads);
	}
		
/*		// Sector 0
		byte[] sector0 = new byte[SECTOR_LENGTH];
		
		// Put name
		Arrays.fill(sector0, 0, 10, (byte)' ');
		byte[] abyName = param.name.getBytes();
		System.arraycopy(abyName, 0, sector0, 0, abyName.length);
		
		int nsectors = param.heads * param.cylinders * param.sectorsPerTrack;
		sector0[10] = (byte)(nsectors >> 8);
		sector0[11] = (byte)(nsectors % 256);
		sector0[12] = (byte)sectorsPerTrack;
		sector0[13] = 'D';
		sector0[14] = 'S';
		sector0[15] = 'K';
		sector0[16] = (byte)0x20;
		sector0[17] = (byte)param.cylinders;
		sector0[18] = (byte)param.heads;
		sector0[19] = (byte)(param.density+1);
		for (int i=0x14; i < 0x38; i++) sector0[i] = (byte)0;
		for (int i=0x38; i < 0x100; i++) sector0[i] = (byte)0xff;
		
		// Allocation bitmap
		m_allocMap = new AllocationMap(nsectors);
		m_allocMap.allocate(0);
		if (m_allocMap.getAUSize()==1) m_allocMap.allocate(1);
		
		byte[] abyMap = m_allocMap.toBitField();
		System.arraycopy(abyMap, 0, sector0, 0x38, abyMap.length);
		
		// Sector 1
		byte[] sector1 = new byte[SECTOR_LENGTH];
		Arrays.fill(sector1, 0, SECTOR_LENGTH, (byte)0x00);
		
		image.writeSector(new Sector(0, sector0));
		image.writeSector(new Sector(1, sector1));
		image.close();
	}
	*/
}

/*		if (m_Image instanceof CF7VolumeFormat) {
			m_nType = CF7;
			// There may be inconsistencies with CF7 volumes.
			// TODO: This should be checked; maybe offer to fix the volume?
		}
*/		

