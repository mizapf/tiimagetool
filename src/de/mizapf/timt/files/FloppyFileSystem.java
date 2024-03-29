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

import java.io.IOException;

/** Represents a TI floppy disk file system. 
	The floppy file system itself does not access the sectors. This is done
	by the image format.
*/	
public class FloppyFileSystem extends TFileSystem {
	
	public final static int UNKNOWN_DENSITY = -1;
	public final static int SINGLE_DENSITY = 0;
	public final static int DOUBLE_DENSITY = 1;
	public final static int HIGH_DENSITY = 2;
	public final static int ULTRA_DENSITY = 3;
	public final static int DOUBLE_DENSITY_16 = 4;

	// Floppy-specific
	boolean		m_bProtection = false;

	// As read from the VIB
	protected int m_nFSCylinders;
	protected int m_nFSHeads;
	protected int m_nFSSectorsPerTrack;
	protected int m_nFSDensity;
	
	/** Values used in the VIB for the density options. */
	public final static int[] densityCode = { 1, 2, 3, 4, 2 };
	public final static int[] sectorsPerTrack = { 9, 18, 36, 72, 16 };

	public FloppyFileSystem() {
	}
	
	/** Create a new empty file system */
	public FloppyFileSystem(FormatParameters param) {
		// Floppy parameters
		m_bProtection = false;
		m_sName = param.name;
		m_nReservedAUs = 0x21;

		// SectorsPerTrack is unset (-1) when creating a new image
		// Cause: NewFloppyImageDialog does not define the sector count
		setGeometry(param.cylinders, param.heads, param.sectors);

		setupAllocationMap(null);

		// Allocates the AUs of sectors 0 and 1
		// For an AU size > 1, the second operation has no effect
		m_allocMap.allocate(0);
		m_allocMap.allocate(1);
	}
	
	@Override
	boolean isProtected() {
		return m_bProtection;
	}
	
	int getSectorsPerTrack() {
		return m_nFSSectorsPerTrack;
	}
	
	int getTracksPerSide() {
		return m_nFSCylinders;
	}
	
	int getSides() {
		return m_nFSHeads;
	}
	
	int getDensity() {
		return m_nFSDensity;
	}
	
	/** For a floppy file system, sectors 0 and 1 are allocated for the file
	    system. If the AU size is greater than 1, both are located in the same
	    AU. We have to deliver the sector count.
	*/
	@Override
	int getSysAllocated() {
		int ausize = getSectorsPerAU();
		if (ausize > 1) return ausize;   // only one AU
		return 2;   // Two AUs of size 1
	}
	
	FormatParameters getParams() {
		FormatParameters param = new FormatParameters(m_sName, false);
		param.setCHS(m_nFSCylinders, m_nFSHeads, m_nFSSectorsPerTrack);
		return param;
	}

	void setGeometry(int tracks, int heads, int sectors) {
		m_nFSCylinders = tracks;
		m_nFSHeads = heads;
		m_nFSSectorsPerTrack = sectors;
		m_nFSTotalSectors = heads * tracks * sectors;
		m_nFSDensity = getDensityFromSectors(sectors);
	}

	public static int getSectorsFromDensity(int dens) {
		return sectorsPerTrack[dens];
	}
	
	public String getFloppyFormatName() {
		StringBuilder sb = new StringBuilder();
		sb.append((getSides()==2)? "DS" : "SS");
		switch (getDensity()) {
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

	int getSectorsPerAU() {
		int total = getTotalSectors();
		if (total < 1600) return 1;
		else {
			if (total < 3200) return 2;
			else {
				if (total < 6400) return 4;
				else return 8;
			}
		}
	}
		
	public int getTotalSectors() {
		return m_nFSTotalSectors;
	}

	private static int getDensityFromCode(int code, int sectors) {
		if (sectors==16) return DOUBLE_DENSITY_16;
		for (int i=0; i < densityCode.length; i++) {
			if (code == densityCode[i]) return i;
		}
		return 0;	
	}

	@Override
	int getRootFDIR() {
		return 1;
	}
	
	private static int getDensityFromSectors(int sectors) {
		if (sectors==16) return DOUBLE_DENSITY_16;
		if ((sectors % 9) != 0) {
			System.err.println("Invalid sector count: " + sectors);
		}
		int i=0;
		while ((sectors != 9) && (sectors != 0)) { sectors >>= 1; i++; }
		return i;
	}
	
	@Override
	int getAllocMapStart() {
		// Sector 0, bytes 0x38 - 0xff
		// Max 1600 sectors
		return 0*SECTOR_LENGTH + 38;		
	}
	
	@Override
	int getAllocMapEnd() {
		// Sector 0, bytes 0x38 - 0xff
		// Max 1600 sectors
		return 1*SECTOR_LENGTH - 1; 		
	}
	
	/** Create the contents of the Volume Information Block. */
	@Override
	byte[] createVIBContents() {
		// Create a new VIB
		// System.out.println("Building VIB from root directory " + m_dirRoot);
		byte[] abyNewVIB = new byte[SECTOR_LENGTH];

		Utilities.setString(abyNewVIB, 0, getVolumeName(), 10);
		
		Utilities.setInt16(abyNewVIB, 0x0a, getTotalSectors());
		abyNewVIB[0x0c] = (byte)(getSectorsPerTrack() & 0xff);
		Utilities.setString(abyNewVIB, 0x0d, "DSK", 3);
		abyNewVIB[0x10] = m_bProtection? (byte)'P' : (byte)' ';
		abyNewVIB[0x11] = (byte)(m_nFSCylinders & 0xff);
		abyNewVIB[0x12] = (byte)(m_nFSHeads & 0xff);
		abyNewVIB[0x13] = (byte)(getDensityCode() & 0xff);
		
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

		byte[] map = m_allocMap.toBitField();
		// System.out.println("Map length = " + map.length);
		for (int j=0; j < map.length; j++) {
			abyNewVIB[j+0x38] = map[j];
		}

		// Fill the rest with ff (required by TIFDC and BWG)
		/* for (int j=map.length; j < (256-0x38); j++) {
			abyNewVIB[j+0x38] = (byte)0xff;
		}	*/
		return abyNewVIB;
	}
	
	/** Create an empty file system. This means a new sector 0 and sector 1. 
		This only occurs for newly created images, so we do not need to correct
		the image format.
		It may also happen for new partitions, but this does not affect the 
		image format either.
	*/
	@Override
	Sector[] createInitSectors() {
	
		Sector[] slist = new Sector[2];
		
		// General parameters (TFileSystem)
		slist[0] = new Sector(0, createVIBContents());
	
		byte[] empty = new byte[SECTOR_LENGTH];
		Arrays.fill(empty, 0, SECTOR_LENGTH, (byte)0x00);
		
		// Sector 1 (FDIR) is empty
		slist[1] = new Sector(1, empty);
		
		return slist;
	}	
	
	int getDensityCode() {
		return densityCode[m_nFSDensity];
	}
	
	static boolean hasFloppySignature(byte[] abySect) {
		return (abySect[13]=='D' && abySect[14]=='S' && abySect[15]=='K');	
	}
		
	@Override
	Sector[] createAllocationMapSectors() {
		Sector[] list = new Sector[1];	
		list[0] = new Sector(0, createVIBContents());
		return list;
	}
		
	public static int checkFormat(byte[] vib) throws IOException {
		int ret = GOOD;
		
		if (!hasFloppySignature(vib))
			ret |= NO_SIG;
		
		// Get the tracks, sectors, and sides
		int nTotalSectors = Utilities.getInt16(vib, 0x0a);
				
		// System.out.println("nTotal = " + m_nTotalSectors + ", FStotal = " + m_nFSTotalSectors);
		
		// No checks for CF7 volumes
		if (nTotalSectors == 1600)
			return ret;
		
		int nSectorsPerTrack = vib[0x0c] & 0xff;
		int nHeads = vib[0x12] & 0xff;
		int nCylinders = vib[0x11] & 0xff;
		
		if ((nSectorsPerTrack * nHeads * nCylinders) != nTotalSectors)
			ret |= BAD_GEOMETRY;
		
		// What about density?
		int nDensity = getDensityFromCode(vib[0x13] & 0xff, nSectorsPerTrack);		
		int nFSDensity = getDensityFromSectors(nSectorsPerTrack);
		
		if (nDensity != nFSDensity) {
			System.err.println("Wrong density value " + (vib[0x13] & 0xff) + " for sector count " + nSectorsPerTrack); 
			ret |= WRONG_DENSITY;
		}
		return ret;
	}

	public void configure(byte[] vib) {

		// Get the tracks, sectors, and sides
		m_nFSTotalSectors = Utilities.getInt16(vib, 0x0a);
		
		// Is the sector count correct?
		int nTotal = ((FloppyImageFormat)m_Image).getTotalSectors();
		
		// System.out.println("nTotal = " + nTotal + ", FStotal = " + m_nFSTotalSectors);
		
		if (nTotal == -1) {
			// System.out.println("Setting total sectors from file system");	
			m_Image.setTotalSectors(m_nFSTotalSectors);
		}

		m_nFSSectorsPerTrack = vib[0x0c] & 0xff;
		m_nFSHeads = vib[0x12] & 0xff;
		m_nFSCylinders = vib[0x11] & 0xff;
		m_nFSDensity = getDensityFromSectors(m_nFSSectorsPerTrack);

		m_nReservedAUs = 0x21;
		
		try {
			setVolumeName(Utilities.getString10(vib, 0));
		}
		catch (InvalidNameException inx) {
			setVolumeName0("UNNAMED");
		}
	}
	
	@Override
	public void setupAllocationMap(byte[] bitfield) {
		m_allocMap = new AllocationMap(getTotalSectors() / getSectorsPerAU(), getSectorsPerAU(), true, bitfield);
	}	
	
	@Override
	public Interval getAllocationInterval() {
		return new Interval(0, 0);
	}
}