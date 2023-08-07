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
import java.io.RandomAccessFile;

import de.mizapf.timt.util.Utilities;

public abstract class HarddiskImageFormat extends FileImageFormat implements PartitionedStorage {
	
	/** Used to determine the type. */
	protected byte[] m_sector0;
	
	protected int m_nCylinders;  
	protected int m_nHeads;
	protected int m_nSectorsPerTrack;
	protected int m_nSectorSize;
	
	protected int m_nPartitions;
	protected int m_nActivePartition;
	
	public final static int HFDC = 1;	// ss=256
	public final static int SCSI = 2;	// ss=512, no partitions
	public final static int IDE = 3;	// ss=512, may have partitions
	
	private int m_nHDType;
	
	protected HarddiskImageFormat(String sImageName) throws FileNotFoundException, IOException, ImageException {
		super(sImageName);
	}

	/** May be overridden by formats like RawHDFormat which cannot rely on this. */
	int getSectorsPerTrack() {
		return m_nSectorsPerTrack;
	}
	
	/** May be overridden by formats like RawHDFormat which cannot rely on this. */
	int getCylinders() {
		return m_nCylinders;
	}
	
	/** May be overridden by formats like RawHDFormat which cannot rely on this. */
	int getHeads() {
		return m_nHeads;
	}

	/** SCSI and IDE have 512 bytes. */
	int getSectorSize() {
		return m_nSectorSize;
	}

	/*
		00-09: Disk name (default: *TI99FSPT*)
		0a-0b: Total number of AUs                             FFFF
		0c-0d: 0000
		0e-0f: "PT"
		10-13: 0000 0000                                       (sectors 512 bytes)
		14-17: Total #sectors (4 bytes)                        000b4000 = 360 MiB
		18-1b: Offset 1st partition       (sectors@512)        00000001
		1c-1f: #sectors 1st partition                          0002e000 = 92 MiB
		20-23: Offset 2nd partition                            0002e001
		24-27: #sectors 2nd partition                          0002e000 = 92 MiB
		28-2b: Offset 3rd partition                            0005c001
		2c-2f: #sectors 3rd partition                          0002e000 = 92 MiB
		30-33: Offset 4th partition                            0008a001
		34-37: #sectors 4th partition                          00029fff = 84 MiB
		fe-ff: 5AA5
		
		Undefined partition: offset = 0
	*/
	void checkFormat(byte[] sector0) {
		m_sector0 = sector0;
		m_nPartitions = 0;
		
		if (sector0[14] == 'P' && sector0[15] == 'T' 
			&& sector0[254] == (byte)0x5a && sector0[255] == (byte)0xa5) {
			// We have a partitioned IDE image
			m_nHDType = IDE;
			
			// Count the partitions
			for (int i=0; i < 4; i++) {
				if (Utilities.getInt32be(sector0, 0x18 + i*8)!=0)
					m_nPartitions++;
			}
			
		}
		else {
			if (getSectorSize()==256)
				m_nHDType = HFDC;
			else
				// There is no difference between SCSI and unpartitioned IDE
				m_nHDType = SCSI;
		}
	}
	
	public int getHDType() {
		return m_nHDType;
	}	
	
	public int partitionCount() {
		return m_nPartitions;
	}
	
	public void setPartition(int part) {
		m_nActivePartition = part;
	}
		
	public String getPartitionName(int part) {
		if (part > m_nPartitions) return null;
		return "FIXME";	
	}
	
	protected void setupAllocationMap() throws ImageException, IOException {
		byte[] allocMap = new byte[31*TFileSystem.SECTOR_LENGTH];
		for (int i=1; i < 32; i++) {
			Sector sect = readSector(i);
			System.arraycopy(sect.getData(), 0, allocMap, (i-1) * TFileSystem.SECTOR_LENGTH, TFileSystem.SECTOR_LENGTH);
		}
				
		((HarddiskFileSystem)m_fs).setupAllocationMap(allocMap);
	}

	protected int setupGeometry(byte[] sec0) {
		return ((HarddiskFileSystem)m_fs).deriveGeometryFromVIB(sec0);
	}
	
	protected void setVolumeInformation() throws ImageException, IOException {
		// System.out.println("setVolumeInfo");

		Sector sector0 = readSector(0);	
		// System.out.println(Utilities.hexdump(sector0.getData()));
		
		try {
			m_fs.setVolumeName(Utilities.getString10(sector0.getData(), 0));
		}
		catch (InvalidNameException inx) {
			m_fs.setVolumeName0("UNNAMED");
		}
						
		checkFormat(sector0.getData());
		setupGeometry(sector0.getData());
		setupAllocationMap();
	}
}
