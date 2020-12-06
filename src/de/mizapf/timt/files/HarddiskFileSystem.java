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

/** Represents a TI harddisk file system. */	
public class HarddiskFileSystem extends TFileSystem {

	// HD-specific
	int 		m_nReservedAUs = 0;
	int 		m_nStepSpeed = 0;
	int 		m_nReducedWriteCurrent = 0;
	boolean 	m_bBufferedStep = false;
	int 		m_nWritePrecomp;
	int			m_nAUEmulate;
	Time		m_tCreation;
	
	boolean		m_bSCSI = false;
	
	public HarddiskFileSystem() {
	}
	
	void setParams(int sectors, int speed, int current, int heads, boolean buff, int precomp) {
		// This is really sectors per track, which means that on hard disks with
		// 32 sectors per track and 16 sectors per AU, we have two AUs per track.
		// Max values: 1984 tracks, 16 heads, 2 AU per track -> 63488 AUs
		m_nSectorsPerTrack = sectors;
		m_nStepSpeed = speed;
		m_nReducedWriteCurrent = current;
		m_nHeads = heads;
		m_bBufferedStep = buff;
		m_nWritePrecomp = precomp;
	}
		
	Sector[] initialize(FormatParameters param) {
		return null;
	}
	
	boolean isSCSI() {
		return m_bSCSI;
	}
	
	void setSCSI(boolean bSCSI) {
		m_bSCSI = bSCSI;
	}
	
	int getAUEmulateSector() {
		return m_nAUEmulate * m_nSectorsPerAU;
	}
	
	void toggleEmulateFlag(int nSector) {
		if (getAUEmulateSector()==nSector) m_nAUEmulate = 0;
		else m_nAUEmulate = nSector / m_nSectorsPerAU;
	}
	
	@Override
	void setupFromFile(byte[] abySect0, byte[] abyAllocMap, boolean bCheck) throws MissingHeaderException, ImageException {	

		m_bSCSI = ((abySect0[0x10] & 0x0f)==0);
		
		if (hasFloppyVib(abySect0)) throw new ImageException(TIImageTool.langstr("VolumeUnexpFloppyVIB"));
		
		m_nStepSpeed = abySect0[0x0e] & 0xff;
		m_nReducedWriteCurrent = abySect0[0x0f] & 0xff;
		m_nSectorsPerAU = ((abySect0[0x10]>>4)&0x0f)+1;
		m_nHeads = (abySect0[0x10]&0x0f)+1;
		m_bBufferedStep = ((abySect0[0x11] & 0x80)==0x80);
		m_nWritePrecomp = abySect0[0x11] & 0x7f;
		m_tCreation = new Time(abySect0, 0x12);
		m_nAUEmulate = Utilities.getInt16(abySect0, 0x1a);		
		
		m_nReservedAUs = ((abySect0[0x0d] & 0xff) << 6);
		if (m_nReservedAUs == 0) {
			System.err.println(TIImageTool.langstr("VolumeNoReservedAU"));
			m_nReservedAUs = 2048;
		}	
		
		// TODO: Check with information from image
		int nTotalAU = Utilities.getInt16(abySect0, 0x0a);
		m_nTotalSectors = nTotalAU * m_nSectorsPerAU;
		m_nSectorsPerTrack = abySect0[0x0c] & 0xff;
	
		// Create allocation map
		m_allocMap = new AllocationMap(nTotalAU, m_nSectorsPerAU, false);		
		m_allocMap.setMapFromBitfield(abyAllocMap, getAllocMapStart() % SECTOR_LENGTH, 0);	
		m_sName = Utilities.getString10(abySect0, 0);
	}
	
	@Override
	int getAllocMapStart() {
		// Sector 0, bytes 0x38 - 0xff
		// Max 1600 sectors
		return 1*SECTOR_LENGTH + 0;		
	}
	
	@Override
	int getAllocMapEnd() {
		// Sector 0, bytes 0x38 - 0xff
		// Max 1600 sectors
		return 32*SECTOR_LENGTH - 1; 		
	}

	@Override
	Sector[] getAllocationMapSectors() {
		Sector[] list = new Sector[(getAllocMapEnd()+1-getAllocMapStart())/SECTOR_LENGTH];
		byte[] bitmap = m_allocMap.toBitField();
		byte[] sectorcont = new byte[SECTOR_LENGTH];
		
		int secno = getAllocMapStart() / SECTOR_LENGTH;   // Sector 1
		int pos = 0;
		int i = 0;
		while (pos < bitmap.length) {
			System.arraycopy(bitmap, pos, sectorcont, 0, SECTOR_LENGTH);
			list[i++] = new Sector(secno++, sectorcont);
			pos += SECTOR_LENGTH;
		}
		return list;
	}
	
	@Override
	byte[] createVIB() {
		byte[] abyNewVIB = new byte[SECTOR_LENGTH];
		Utilities.setString(abyNewVIB, 0, getName(), 10);

		Utilities.setInt16(abyNewVIB, 0x0a, m_nTotalSectors/m_nSectorsPerAU);
		abyNewVIB[0x0d] = (byte)((m_nReservedAUs>>6) & 0xff);
		Utilities.setTime(abyNewVIB, 0x12, m_tCreation);
		abyNewVIB[0x16] = (byte)(m_dirRoot.getFiles().length & 0xff);
		abyNewVIB[0x17] = (byte)(m_dirRoot.getDirectories().length & 0xff);
		Utilities.setInt16(abyNewVIB, 0x18, toAU(m_dirRoot.getFileIndexSector()));
		
		if (!m_bSCSI) {
			abyNewVIB[0x0c] = (byte)m_nSectorsPerTrack;
			abyNewVIB[0x0e] = (byte)m_nStepSpeed;
			abyNewVIB[0x0f] = (byte)m_nReducedWriteCurrent;
			abyNewVIB[0x10] = (byte)((((m_nSectorsPerAU-1)<<4)|(m_nHeads-1)) & 0xff);
			abyNewVIB[0x11] = (byte)(((m_bBufferedStep? 0x80 : 0x00) | m_nWritePrecomp) & 0xff);
			Utilities.setInt16(abyNewVIB, 0x1a, m_nAUEmulate);
		}
		else {
			abyNewVIB[0x0c] = (byte)0;
			abyNewVIB[0x0e] = (byte)0;
			abyNewVIB[0x0f] = (byte)0;
			abyNewVIB[0x10] = (byte)(((m_nSectorsPerAU-1)<<4) & 0xff);
			abyNewVIB[0x11] = (byte)0;
			abyNewVIB[0x1a] = (byte)0;
			abyNewVIB[0x1b] = (byte)0;
		}
		
		int j=0x1c;
		Directory[] dirs = m_dirRoot.getDirectories();
		for (int i=0x1c; i < 0x100; i++) abyNewVIB[i] = (byte)0;
		for (int i=0; i < dirs.length; i++) {
			Utilities.setInt16(abyNewVIB, j, dirs[i].getDDRSector() / m_nSectorsPerAU);
			j=j+2;
		}
		return abyNewVIB;
	}
}
