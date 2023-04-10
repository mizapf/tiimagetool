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

// Can a partitioned format also be a memory image format?
// Basically not.
// Partitioned formats cannot be opened as such, only their partitions

// Can a new partition be saved to a partitioned format?
// Probably not. How are partitions created then?

public class MemoryImageFormat extends ImageFormat {
	
	byte[] m_empty;
	
	// Used for displaying still unnamed volumes
	private int m_nUnnamedIndex;

	MemoryImageFormat(String sImageName, int number) throws FileNotFoundException {
		// super(sImageName);
		m_empty = new byte[TFileSystem.SECTOR_LENGTH];
		m_nUnnamedIndex = number;
		m_writeCache.setName(sImageName + number);
	}
	
	public String getFormatName() {
		return TIImageTool.langstr("Unsaved");
	}
	
	/** Create an empty sector content with the given pattern. */
	void setEmptySector(byte[] pattern) {
		int j = 0;
		for (int i=0; i < TFileSystem.SECTOR_LENGTH; i++) {
			m_empty[i] = pattern[j++];
			if (j > pattern.length) j=0;
		}
	}

	/** Read a sector. The MemoryImageFormat always uses a write cache. */
	public Sector readSector(int nSectorNumber) {
		Sector sect = null;
		sect = m_writeCache.read(nSectorNumber);

		if (sect == null) {
			// Create an empty sector
			sect = new Sector(nSectorNumber, m_empty);
		}
		return sect;	
	}
	
	/** Write a sector. The MemoryImageFormat always uses a write cache.
	*/
	@Override
	public void writeSector(Sector sect) {
		m_writeCache.write(sect);
	}	
	
	@Override
	int getImageType() {
		return MEMORY; 
	}
	
	/** Don't do anything for reopening. */
	@Override
	public void reopenForWrite() {
	}
	
	/** Don't do anything for reopening. */
	@Override
	public void reopenForRead() {
	}
	
	@Override
	public String getImageName() {
		return getShortImageName();
	}
	
	@Override 
	public String getShortImageName() {
		return TIImageTool.langstr("Unnamed") + m_nUnnamedIndex;
	}
}
