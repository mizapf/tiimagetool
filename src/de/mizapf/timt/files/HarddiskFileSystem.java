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
import de.mizapf.timt.util.NotImplementedException;

import java.io.IOException;

/** Represents a TI harddisk file system.

    IDE:
    
    00000200: 4944 4548 4152 4431 2020 b800 0064 0000  IDEHARD1  ...d..
    00000210: 7000 0000 0000 0100 0008 0000 0000 0000  p...............
    ...
    000002f0: 0000 0000 0000 0000 0000 0000 0000 0000  ................
    00000300: ffc0 0000 0000 0000 0000 0000 0000 0000  ................   
    
    00-09: Volume name
    0a-0b: Total number of AUs                             B800
    0c:    Reserved                                        00
    0d:    Reserved AUs                                    64
    0e-0f: Unused                                          0000
    10-11: Sec/AU-1(4), 0(12)                              7000
    12-15: Time/date of creation                           0000 0000
    16:    #files                                          01
	17:    #dirs                                           00
	18-19: AU of FDIR                                      0008    
    1a-1b: AU of DSK1 emulation file                       0000
    1C-FF: subdirs
    100..: Allocation table                                111111111100...  

*/	
public abstract class HarddiskFileSystem extends TFileSystem {

	public static final int SCSI = 0;
	public static final int MFM = 1;
	
	// HD-specific
	Time m_tCreation;

	int m_nFiles;
	int m_nSubdirs;
	int m_nRootFD;
	
	// As read from the VIB
	protected int m_nFSCylinders;
	protected int m_nFSHeads;
	protected int m_nFSSectorsPerTrack;
	protected int m_nFSTotalSectors;
	protected int m_nFSSectorsPerAU;
	
	// From the image
	protected int m_nCylinders;
	protected int m_nHeads;
	protected int m_nSectorsPerTrack;
	
	public HarddiskFileSystem(int nTotal, byte[] vibmap) {
		super(0x21);
		m_nReservedAUs = 2048; // if new
	}

	public HarddiskFileSystem() {
		super(0x21);
		m_nReservedAUs = 2048; // if new
	}
	
	Sector[] getInitSectors(FormatParameters param) {
		return null;
	}
	
/*	public int getCylinders() {
		if (m_nCylinders != -1) 
			return m_nCylinders;
		else 
			return m_nFSCylinders;
	}

	public int getHeads() {
		if (m_nHeads != -1) 
			return m_nHeads;
		else 
			return m_nFSHeads;
	}
	
	public int getSectors() {
		if (m_nSectorsPerTrack != -1) 
			return m_nSectorsPerTrack;
		else 
			return m_nFSSectorsPerTrack;
	}
	*/


/*	public int getSectorLength() {
		return getSectors();
	}
*/
/*	int getSectorsPerTrack() {
		return m_nSectorsPerTrack;
	}
*/

	@Override
	int getSectorsPerAU() {
		return m_nFSSectorsPerAU;
	}
	
	@Override
	boolean isProtected() {
		return false;
	}
	
	@Override
	int getAllocMapStart() {
		// Sector 1, bytes 0x00 - 0xff
		return 1*SECTOR_LENGTH + 0;		
	}
	
	@Override
	int getAllocMapEnd() {
		// Sector 31, bytes 0xff
		return 32*SECTOR_LENGTH - 1; 		
	}
	
	@Override
	Sector[] createAllocationMapSectors() {
		// System.out.println("alloc end = " + getAllocMapEnd() + ", alloc start = " + getAllocMapStart());
		Sector[] list = new Sector[(getAllocMapEnd()+1-getAllocMapStart())/SECTOR_LENGTH];
		byte[] bitmap = m_allocMap.toBitField();
		System.out.println("bitmap.length = " + bitmap.length);
		byte[] sectorcont = new byte[SECTOR_LENGTH];
		
		int secno = getAllocMapStart() / SECTOR_LENGTH;   // Sector 1
		int pos = 0;
		int i = 0;
		while (pos < bitmap.length) {
			System.out.println("sector = " + secno);
			System.arraycopy(bitmap, pos, sectorcont, 0, SECTOR_LENGTH);
			list[i++] = new Sector(secno++, sectorcont);
			pos += SECTOR_LENGTH;
		}
		return list;
	}
	
	@Override
	Sector[] createInitSectors() {
		byte[] vib = createVIBContents();
		Sector[] slist = new Sector[64];   // original data in 0-31, backup in 32-63
		Sector[] allsec = createAllocationMapSectors();

		slist[0] = new Sector(0, createVIBContents());
		
		for (int i=1; i < allsec.length; i++) {
			slist[i] = (Sector)allsec[i].clone();
		}
		
		// Backup
		for (int i=0; i < slist.length/2; i++) {
			if (slist[i] != null) 
				slist[i + slist.length/2] = (Sector)slist[i].clone();
		}
		
		return slist;
	}

	public int getTotalSectors() {
		return m_nFSTotalSectors;
	}

	abstract byte[] createVIBContents();
	
	/** Try to load the VIB and get the logical geometry. 
	*/
	public abstract void configure(byte[] vib); 

	public static int checkFormat(byte[] vib) throws IOException {
		int ret = GOOD;
		
		int nFSSectorsPerAU = ((vib[0x10] >> 4) & 0x0f) + 1;
		int nFSTotalSectors = Utilities.getInt16(vib, 0x0a) * nFSSectorsPerAU;		
		
		if ((nFSSectorsPerAU > 16) || (nFSTotalSectors < 360))
			ret |= BAD_GEOMETRY;
		
		if ((nFSTotalSectors / nFSSectorsPerAU) > 0xF800)
			ret |= BAD_AUCOUNT;
		
		return ret;
	}
	
	void configureCommon(byte[] vibmap) {
		try {
			setVolumeName(Utilities.getString10(vibmap, 0));
		}
		catch (InvalidNameException inx) {
			setVolumeName0("UNNAMED");
		}
		m_nFSSectorsPerAU = ((vibmap[0x10] >> 4) & 0x0f) + 1;
		m_nFSTotalSectors = Utilities.getInt16(vibmap, 0x0a) * m_nFSSectorsPerAU;		
			
		m_tCreation = new Time(vibmap, 0x12);

		m_nFiles = vibmap[16] & 0xff;
		m_nSubdirs = vibmap[17] & 0xff;
		
		m_nRootFD = Utilities.getInt16(vibmap, 0x18);
	}
		
	@Override
	public void setupAllocationMap(byte[] vibmap) {
		m_allocMap = new AllocationMap(getTotalSectors()/getSectorsPerAU(), getSectorsPerAU(), false);
		m_allocMap.setMapFromBitfield(vibmap, TFileSystem.SECTOR_LENGTH, 0);
	}
}
