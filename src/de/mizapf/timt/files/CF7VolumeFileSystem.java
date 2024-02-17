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

/** Represents a TI floppy disk file system for the CF7 volumes. */	
public class CF7VolumeFileSystem extends FloppyFileSystem {
	
	public CF7VolumeFileSystem() {
	}
	
	void setGeometry(int tracks, int heads, int sectors) {
		m_nFSCylinders = 40;
		m_nFSHeads = 2;
		m_nFSSectorsPerTrack = 20;
		m_nFSTotalSectors = 1600;
		m_nFSDensity = 2;
	}

	public static int getSectorsFromDensity(int dens) {
		return 20;
	}
	
	@Override
	public String getFloppyFormatName() {
		return "CF7";
	}

	@Override
	int getSectorsPerAU() {
		return 1;
	}
		
	@Override
	public int getTotalSectors() {
		return 1600;
	}

	private static int getDensityFromSectors(int sectors) {
		return 2;
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
	
	@Override
	int getDensityCode() {
		return 2;
	}
			
	@Override
	Sector[] createAllocationMapSectors() {
		Sector[] list = new Sector[1];	
		list[0] = new Sector(0, createVIBContents());
		return list;
	}
		
	@Override
	public void configure(byte[] vib) {

		// Get the tracks, sectors, and sides
		m_nFSTotalSectors = 1600;

		m_nFSSectorsPerTrack = 20;
		m_nFSHeads = 2;
		m_nFSCylinders = 40;
		m_nFSDensity = 2;

		m_nReservedAUs = 0x21;
		
		try {
			setVolumeName(Utilities.getString10(vib, 0));
		}
		catch (InvalidNameException inx) {
			setVolumeName0("UNNAMED");
		}
	}
}