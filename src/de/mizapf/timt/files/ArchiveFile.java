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
    
    Copyright 2011 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.files;

import java.util.*;
import java.io.*;
import de.mizapf.timt.TIImageTool;

public class ArchiveFile extends TFile {

	private byte[] m_abyArchive;
	int m_nOffset = 0;

	/** Used in archives. */
	public ArchiveFile(byte[] abyArchive, int nOffset, String sFileName, int nRecordLength, byte byFlags, int nRecordsPerSector, int nEOFOffset, int nNumberOfRecords, int nSectors, Archive dirContaining) {
		m_abyArchive = abyArchive;
		m_sName = sFileName;
		m_nRecordLength = nRecordLength;
		m_byFlags = byFlags;
		m_nRecordsPerSector = nRecordsPerSector;
		m_nEOFOffset = nEOFOffset;
		m_nNumberOfRecords = nNumberOfRecords;
		m_nAllocatedSectors = nSectors;
		m_nOffset = nOffset;
		m_dirParent = dirContaining;
		
		byte[] nulltime = new byte[4];
		m_tCreation = new Time(nulltime); 
		m_tUpdate = new Time(nulltime); 
	}
	
	/** Retrieves the content as the sector content. The file is not truncated according to EOF. */
	public byte[] getSectorContent() throws ImageException {
		try {
			byte[] abyReturn = new byte[m_nAllocatedSectors*Volume.SECTOR_LENGTH];
			System.arraycopy(m_abyArchive, m_nOffset, abyReturn, 0, abyReturn.length);
			return abyReturn;
		}
		catch (ArrayIndexOutOfBoundsException e) {
			throw new ImageException(TIImageTool.langstr("ArchiveTruncated"));
		}
	}
		
	public int getAllRequiredSectors(int nAUSize) {
		return getUsedSectors();
	}
	
	public boolean isFragmented() {
		return false;
	}

	public boolean isProtected() {
		return m_bProtected;
	}
}
