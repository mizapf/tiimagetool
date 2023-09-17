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

import java.util.Arrays;

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

	public static final int MFM = 0;
	public static final int SCSI = 1;
	public static final int IDE = 2;
	
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
	
	public HarddiskFileSystem() {
	}
	
	public HarddiskFileSystem(FormatParameters param) {
		m_sName = param.name;
		// System.out.println(m_sName);
		setGeometry(param);
		// System.out.println("total sectors = " + param.getTotalSectors());
		// System.out.println("AU size = " + param.auSize);
		m_allocMap = new AllocationMap(param.getTotalSectors() / param.auSize, param.auSize, false);
		m_allocMap.allocate(new Interval(0,31));
		m_allocMap.allocate(new Interval(32,63));   // Backup
		m_allocMap.allocate(64 / param.auSize);   // FDIR (AU)
	}
	
	public static HarddiskFileSystem getInstance(FormatParameters parm) {
		if (parm.isHFDC()) {
			return new HFDCFileSystem(parm);
		}
		else {
			return new SCSIFileSystem(parm);
		}
	}

	abstract void setGeometry(FormatParameters param);
	
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
	int getRootFDIR() {
		return 64;
	}
	
	@Override
	Sector[] createAllocationMapSectors() {
		// System.out.println("alloc end = " + getAllocMapEnd() + ", alloc start = " + getAllocMapStart());
		Sector[] list = new Sector[(getAllocMapEnd()+1-getAllocMapStart())/SECTOR_LENGTH];
		byte[] bitmap = m_allocMap.toBitField();
		// System.out.println("bitmap.length = " + bitmap.length);
		byte[] sectorcont = new byte[SECTOR_LENGTH];
		
		int secno = getAllocMapStart() / SECTOR_LENGTH;   // Sector 1
		int pos = 0;
		int i = 0;
		while (pos < bitmap.length) {
			// System.out.println("sector = " + secno);
			System.arraycopy(bitmap, pos, sectorcont, 0, SECTOR_LENGTH);
			list[i++] = new Sector(secno++, sectorcont);
			pos += SECTOR_LENGTH;
		}
		// System.out.println("allocation map = " + list.length + " sectors");
		return list;
	}
	
	@Override
	Sector[] createInitSectors() {
		byte[] vib = createVIBContents();
		Sector[] slist = new Sector[65];   // original data in 0-31, backup in 32-63, FDIR in 64
		Sector[] allsec = createAllocationMapSectors();

		slist[0] = new Sector(0, createVIBContents());
		
		byte[] empty = new byte[SECTOR_LENGTH];
		Arrays.fill(empty, 0, SECTOR_LENGTH, (byte)0x00);

		for (int i=0; i < allsec.length; i++) {
			if (allsec[i] != null)
				slist[i+1] = (Sector)allsec[i].clone();
			else
				slist[i+1] = new Sector(i+1, empty);
		}
		
		// Backup
		for (int i=0; i < 32; i++) {
			if (slist[i] != null) 
				slist[i + 32] = new Sector(i+32, slist[i].getData());
		}
		
		// Sector 64 (FDIR) is empty
		slist[64] = new Sector(64, empty);
		
		return slist;
	}

	public byte[] createInitArray() {
		Sector[] sect = createInitSectors();
		byte[] initarray = new byte[65 * TFileSystem.SECTOR_LENGTH];
		for (int i=0; i < sect.length; i++) {
			System.arraycopy(sect[i].getData(), 0, initarray, i*TFileSystem.SECTOR_LENGTH, TFileSystem.SECTOR_LENGTH);
		}
		return initarray;
	}
	
	public int getTotalSectors() {
		return m_nFSTotalSectors;
	}
	
	/** For a harddisk file system, sectors 0-64 are allocated for the file 
	    system (0: VIB, 1-31: Alloc, 32: VIB(copy), 33-63: Alloc(copy), 64: FDIR(root))
	*/
	@Override
	int getSysAllocated() {
		int ausize = getSectorsPerAU();
		return (65 + (ausize-1))/ausize * ausize;  // round up
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
		
		if (vib[0x0e] == 'P' && vib[0x0f] == 'T' && vib[0x0a] == (byte)0xff && vib[0x0b] == (byte)0xff) {
			// We have a partitioned IDE image; the other values are irrelevant
			ret = PARTITIONED;		
		}
		
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
		
		// If there is a "WIN" signature, guess the number of reserved AUs
		if ((vibmap[13] != 'W') || (vibmap[14] != 'I') || (vibmap[15] != 'N')) {
			m_nReservedAUs = (vibmap[13] & 0xff) << 6;
		}
		else {
			m_nReservedAUs = (m_nFSTotalSectors / 96) & 0xffc0;
			if (m_nReservedAUs > 16320) m_nReservedAUs = 16320;
			if (m_nReservedAUs < 512) m_nReservedAUs = 512;
		}
	}
		
	@Override
	public void setupAllocationMap(byte[] vibmap) {
		m_allocMap = new AllocationMap(getTotalSectors()/getSectorsPerAU(), getSectorsPerAU(), false);
		m_allocMap.setMapFromBitfield(vibmap);
	}
}
