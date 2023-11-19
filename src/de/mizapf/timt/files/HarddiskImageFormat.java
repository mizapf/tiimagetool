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
import de.mizapf.timt.util.NotImplementedException;

import de.mizapf.timt.util.Utilities;

public abstract class HarddiskImageFormat extends FileImageFormat implements PartitionedStorage {
	
	/** Used to determine the type. */
	protected byte[] m_sector0;
	
	protected int m_nCylinders;  
	protected int m_nHeads;
	protected int m_nSectorsPerTrack;
	protected int m_nSectorSize;
		
/*	public final static int HFDC = 1;	// ss=256
	public final static int SCSI = 2;	// ss=512, no partitions
	public final static int IDE = 3;	// ss=512, may have partitions
	*/
	private int m_nHDType;
		
	protected HarddiskImageFormat(String sImageName) throws FileNotFoundException, IOException, ImageException {
		super(sImageName);
		m_nActivePartition = -1;
	}

	protected HarddiskImageFormat(String sImageName, FormatParameters params) throws FileNotFoundException, IOException, ImageException {
		super(sImageName, params);
		m_nSectorsPerTrack = params.sectors;
		m_nCylinders = params.cylinders;		
		m_nHeads = params.heads;
		m_nActivePartition = -1;
		System.out.println("hier2");
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

	/** SCSI and IDE have 512 bytes physically, but the TI file system always sees
		256 byte sectors. */
	int getSectorSize() {
		return m_nSectorSize;
	}

	public HarddiskFileSystem getFileSystem(byte[] vibmap) {
		int nHDType = 0;
		HarddiskFileSystem fs = null;
		
		if (m_partition != null) {
			// We have a partitioned IDE image
			fs = new IDEFileSystem();
		}
		else {
			// Sectors/track or write precomp == 0 and heads == 1 -> SCSI
			// It may be sufficient if any of these is true
			if (((vibmap[12] == 0) || (vibmap[17] == 0)) && ((vibmap[16] & 0x0f)==0))
				fs = new SCSIFileSystem();
			else
				fs = new HFDCFileSystem();
		}
		fs.setImage(this);
		fs.configure(vibmap);
		return fs;
	}
	
	public int getHDType() {
		return m_nHDType;
	}	
	
	protected int getPartitionSectorOffset() {
		if (m_nActivePartition == -1) return 0;
		return m_partition[m_nActivePartition].offset;
	}
	
	/*
	    18-1b: Offset 1st partition
	    1c-1f: #sectors 1st partition
	    20-23: Offset 2nd partition
	    24-27: #sectors 2nd partition
	    28-2b: Offset 3rd partition
	    2c-2f: #sectors 3rd partition
	    30-33: Offset 4th partition
	    34-37: #sectors 4th partition
	    
	    offset = 0 -> unused
	    
    */
    // Der Partitionsoffset muss vor jedem readSector/writeSector addiert werden
    // Er darf nicht erst in getFUNumberFromSector addiert werden, sonst wird
    // der erwartete Sektor nicht gefunden
    
	public void setupPartitionTable() throws ImageException, IOException {
		m_partition = new Partition[4];
		byte[] sect0 = readSector(0).getData();
		
		for (int i=0; i < 4; i++) {
			int partsect = Utilities.getInt32be(sect0, 0x18 + i*8) * 2;
			int lensect = Utilities.getInt32be(sect0, 0x20 + i*8) * 2;
			// System.out.println(i + ": partsect=" + partsect);

			if (partsect != 0) {
				Sector vib = readSector(partsect);
				String sName = Utilities.getString10(vib.getData(), 0);
			//	System.out.println(sName);
				m_partition[i] = new Partition(i, partsect, lensect, sName); 
			}
		}
	}
	
	public boolean isPartitioned() throws ImageException, IOException {
		if (m_partition != null) return true;
		
		byte[] vib = readSector(0).getData();
		System.out.println(Utilities.hexdump(vib));
		return (vib[0x0e] == 'P' && vib[0x0f] == 'T' && vib[0x0a] == (byte)0xff && vib[0x0b] == (byte)0xff);
	}
}
