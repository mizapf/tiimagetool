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
import java.util.ArrayList;

import de.mizapf.timt.util.InternalException;
import de.mizapf.timt.TIImageTool;

public class AllocationMap implements Cloneable {

	private byte[] m_abyMap;
	private int m_nLength;
	private int m_nAUSize;
	private boolean m_bFloppy;
	
	static final int EVENLONGER = -1; 
	static final int NOTFOUND = -1; 
	
	/** Creates a new allocation map. 
		@param nAU Number of allocation units
		@param nAUSize Length of AU
		@param bFloppy FLoppy allocation maps are little-endian
	*/
	public AllocationMap(int nAU, int nAUSize, boolean bFloppy, byte[] bitfield) {
		// System.out.println("New allocmap, size = " + (nAU+7)/8);
		if (bFloppy) {
			// m_abyMap = new byte[(nAU+7)/8];
			m_abyMap = new byte[200];

			// Fill bits with 1 after the last allocatable AU
			if (nAU < 1600) {
				m_abyMap[nAU/8] = (byte)((0xff << (nAU%8)) & 0xff);
			}
			for (int i = nAU/8+1; i < m_abyMap.length; i++) {
				m_abyMap[i] = (byte)0xff;
			}
		}
		else {
			// Allocate full sectors (256*8 bits)
			if (nAU > 0xf800) throw new InternalException("Excess AU number: " + nAU);
			// int nSectors = (nAU+2047)/2048;
			// m_abyMap = new byte[nSectors * TFileSystem.SECTOR_LENGTH];
			m_abyMap = new byte[31 * TFileSystem.SECTOR_LENGTH];
			
			// Fill bits with 1 after the last allocatable AU
			if (nAU < 0xf800) {
				m_abyMap[nAU/8] = (byte)((0xff >> (nAU%8)) & 0xff);
				for (int i = nAU/8+1; i < m_abyMap.length; i++) {
					m_abyMap[i] = (byte)0xff;
				}
			}
			// System.out.println("Allocation map size = 31 sectors");
		}
		m_nLength = nAU;
		m_nAUSize = nAUSize;
		m_bFloppy = bFloppy;
		
		// Note that the LSB represents the first AU, and the MSB is seven
		// AUs later (going from right to left for each byte)
		//
		// 00000000 00000000 1111111 11111111
		// 76543210 fedcba98 7654321 fedcba98
		//
		// Freshly formatted disks should have a 0x03 as the first allocated bits
		// (sector 0, sector 1)
		//
		if (bitfield != null) {
			int nOffset = m_bFloppy? 0x38 : TFileSystem.SECTOR_LENGTH;  // Start at byte 0x38 for floppies and at sector 1 for hard disks
			int nLength = bitfield.length - nOffset;
			if (nLength > m_abyMap.length) nLength = m_abyMap.length;
			// System.out.println("aby.length = " + vibmap.length + ", nOffset = " + nOffset + ", map.length = " + m_abyMap.length + ", length = " + nLength);
			System.arraycopy(bitfield, nOffset, m_abyMap, 0, nLength);
		}
	}

	public Object clone() {
		try {
			// System.out.println("Cloning allocmap");
			AllocationMap clonedMap = (AllocationMap)super.clone();
			System.arraycopy(m_abyMap, 0, clonedMap.m_abyMap, 0, m_abyMap.length);
			return clonedMap;
		}
		catch (CloneNotSupportedException cnsx) {
			cnsx.printStackTrace();
			return null;
		}
	}

	public byte[] toBitField() {
		return m_abyMap;
	}
	
	public int getAUSize() {
		return m_nAUSize;
	}
	
	public int getMaxAU() {
		return m_nLength;
	}

	public void allocate(int nSector) {
		allocateAU(nSector / m_nAUSize);
	}
	
	public void allocateAU(int nUnit) {
		if (nUnit < m_nLength) {
			if (m_bFloppy) {
				m_abyMap[nUnit/8] |= (1 << (nUnit%8));			
			}
			else {
				m_abyMap[nUnit/8] |= ((0x80 >> (nUnit%8))&0xff);		
			}
			//		System.out.println("Allocate " + nUnit);
		}
		else 
			throw new IndexOutOfBoundsException(String.valueOf(nUnit));
	}
	
	/** Allocates the smallest set of AUs to contain the interval. 
		@param it Interval (referring to sectors)
	*/
	public void allocate(Interval it) {
		// System.out.println("Allocate interval (sectors) = [" + it.start + ", "+ it.end + "]");
		int nStartAU = it.start/m_nAUSize;
		int nEndAU = it.end/m_nAUSize;
		for (int i=nStartAU; i <= nEndAU; i++) allocateAU(i);		
	}
	
	public void deallocate(int nUnit) throws IndexOutOfBoundsException {
		if (nUnit < m_nLength) {
			if (m_bFloppy) {
				m_abyMap[nUnit/8] &= ~(1<<(nUnit%8));	
			}
			else {
				m_abyMap[nUnit/8] &= ~((0x80 >> (nUnit%8))&0xff);		
			}
		}
		else  
			throw new IndexOutOfBoundsException(String.valueOf(nUnit));
	}

	/** Deallocates the smallest set of AUs to contain the interval. 
		@param it Interval (referring to sectors)
	*/
	public void deallocate(Interval it) throws IndexOutOfBoundsException {
		int nStartAU = it.start/m_nAUSize;
		int nEndAU = it.end/m_nAUSize;
		for (int i=nStartAU; i <= nEndAU; i++) deallocate(i);		
	}
	
	public boolean hasAllocated(int nUnit) {
		if (nUnit < m_nLength) {
			if (m_bFloppy) {
				return ((m_abyMap[nUnit/8] & (1<<(nUnit%8)))!=0);
			}
			else {
				//			System.out.println("nUnit = " + nUnit + ", 0x80>>= " + ((0x80>>(nUnit%8))&0xff) + ", m_abyMap[nUnit/8] = " + m_abyMap[nUnit/8]);
				//			System.exit(1);
				return ((m_abyMap[nUnit/8] & ((0x80>>(nUnit%8))&0xff)) !=0);
			}
		}
		else
			throw new IndexOutOfBoundsException(TIImageTool.langstr("AllocMapInvalidAU") + ": " + String.valueOf(nUnit));
	}
	
	int getNextFreeAUAfter(int nAU) {
		for (int i=nAU+1; i < m_nLength; i++) {
			if (!hasAllocated(i)) return i;
		}
		return NOTFOUND;
	}
	
	/** Return next AU position that is allocated. If this is farther away
		than we need, return -1. If we reach the end of the medium, return the
		highest AU number + 1.
		@param nAU First AU that can be used (supposed to be free)
		@param nNumberAU Number of AUs that we need
		@return EVENLONGER if this gap is larger than required; the size of the gap when it is too small; 
		m_nLength if we run over the end of the medium (pretending that higher AUs are allocated)
	*/
	int getNextAllocatedAUAfter(int nAU, int nNumberAU) {
		int i = nAU + 1;
		int nEnd = i + nNumberAU;

		while ((i < nEnd) && (i < m_nLength)) {
			if (hasAllocated(i)) return i;
			i++;
		}
		if (i==m_nLength) return m_nLength;
		return EVENLONGER;
	}
	
	public Integer[] getUnallocatedLocations(Interval inv) {
		ArrayList<Integer> vct = new ArrayList<Integer>();
		for (int i=inv.start; i<=inv.end; i++) {
			if (!hasAllocated(i / m_nAUSize)) {
				vct.add(Integer.valueOf(i));
			}
		}
		Integer[] result = vct.toArray(new Integer[vct.size()]);
		return result;
	}	
	
	int countAllocated() {
		int sum = 0;
		for (int i=0; i < m_nLength; i++) {
			if (hasAllocated(i)) sum++;
		}
		return sum;
	}
}
