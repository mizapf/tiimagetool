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

/** Represents a SCSI harddisk file system.

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
public class SCSIFileSystem extends HarddiskFileSystem {

	@Override
	byte[] createVIBContents() {
		byte[] abyNewVIB = new byte[SECTOR_LENGTH];
		Utilities.setString(abyNewVIB, 0, getVolumeName(), 10);

		Utilities.setInt16(abyNewVIB, 0x0a, m_nFSTotalSectors/m_nFSSectorsPerAU);
		abyNewVIB[0x0d] = (byte)((m_nReservedAUs>>6) & 0xff);
		Utilities.setTime(abyNewVIB, 0x12, m_tCreation);

		abyNewVIB[0x0c] = (byte)0;
		abyNewVIB[0x0e] = (byte)0;
		abyNewVIB[0x0f] = (byte)0;
		abyNewVIB[0x10] = (byte)(((m_nFSSectorsPerAU-1)<<4) & 0xff);
		abyNewVIB[0x11] = (byte)0;
		abyNewVIB[0x1a] = (byte)0;
		abyNewVIB[0x1b] = (byte)0;
		for (int i=0x1c; i < 0x100; i++) abyNewVIB[i] = (byte)0;

		if (m_dirRoot == null) {
			abyNewVIB[0x16] = (byte)0;
			abyNewVIB[0x17] = (byte)0;
			Utilities.setInt16(abyNewVIB, 0x18, getAUNumber(64));
		}
		else {		
			abyNewVIB[0x16] = (byte)(m_dirRoot.getFiles().length & 0xff);
			abyNewVIB[0x17] = (byte)(m_dirRoot.getDirectories().length & 0xff);
			Utilities.setInt16(abyNewVIB, 0x18, getAUNumber(m_dirRoot.getFileIndexSector()));
			int j=0x1c;
			Directory[] dirs = m_dirRoot.getDirectories();
			for (int i=0; i < dirs.length; i++) {
				Utilities.setInt16(abyNewVIB, j, dirs[i].getDDRSector() / m_nFSSectorsPerAU);
				j=j+2;
			}
		}		
		return abyNewVIB;
	}
	
	SCSIFileSystem() {
		// System.out.println("SCSI file system");
	}
	
	SCSIFileSystem(FormatParameters param) {
		super(param);
		// System.out.println("SCSI file system");
	}
	
	void setGeometry(FormatParameters param) {
		m_nFSTotalSectors = param.getTotalSectors();
		m_nFSSectorsPerAU = param.auSize;
		m_nReservedAUs = param.reservedAUs;
		m_tCreation = param.time;
	}

	/** Try to load the VIB and get the logical geometry. 
	*/
	@Override
	public void configure(byte[] vibmap) {
		// System.out.println(Utilities.hexdump(vib));		
		configureCommon(vibmap);
		
		// SCSI does not define "tracks" but only linear sector numbers
		// We define the format units as 32 sectors (for the RawHDFormat; the
		// MameCHDFormat has its own notion of format units)
		((HarddiskImageFormat)m_Image).setFormatUnitLength(32 * TFileSystem.SECTOR_LENGTH);

		// System.out.println("nTotal = " + m_nFSTotalSectors + ", AU size = " + m_nFSSectorsPerAU);	
	}
	
	@Override
	FormatParameters getParams() {
		FormatParameters param = new FormatParameters(m_sName, false);
		param.setTotal(m_nFSTotalSectors);
		param.setFormatUnitSectors(32);
		param.setHD(m_tCreation, m_nFSSectorsPerAU, m_nReservedAUs, HarddiskFileSystem.SCSI);

		return param;
	}
}
