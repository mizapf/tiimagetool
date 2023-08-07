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
import java.util.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.InternalException;

/** Represents a TI file system.
	Every file system manages the medium as a collection of sectors.

	We assume that every file system has a volume information block (VIB) with
	a volume name and a total number of sectors.
	
	A file system has 
	- a name (volume name)
	- a total number of sectors
	- a map of allocated sectors
	- a size for allocation units
	- a link to file or directory entries (root directory)
	
	The general file system does not have to deal with cylinders, heads, or
	number of sectors in a track.
	
*/	
public abstract class TFileSystem {

	public final static int SECTOR_LENGTH=0x100;   
	public final static int MAXAU = 0xf800;  // from Directory and RawHDFormat

	// Check values
	public final static int GOOD = 0;
	public final static int NO_SIG = 1;
	public final static int SIZE_MISMATCH = 2;
	public final static int BAD_GEOMETRY = 4;
	public final static int WRONG_DENSITY = 8;
	public final static int UNSET = 65535;
	
	/** Volume name. */
	protected String m_sName;
	
	/** Size of the file system. */
	protected int m_nTotalSectors = 0;

	/** Reserved AUs. */
	protected int m_nReservedAUs;
	
	/** Root directory. */
	protected Directory m_dirRoot;
	
	/** Sector allocation map. */
	protected AllocationMap m_allocMap;

	public TFileSystem() {
	}

	public TFileSystem(int reserved) {
		this(-1, reserved);
	}

	public TFileSystem(int total, int reserved) {
		m_nTotalSectors = total;
		m_nReservedAUs = reserved;
	}
	
	protected void setVolumeName(String newName) throws InvalidNameException {
		if (newName == null || newName.length()==0 || newName.length()>10) throw new InvalidNameException(TIImageTool.langstr("VolumeNameConstr"));
		if (newName.indexOf(".")!=-1) throw new InvalidNameException(TIImageTool.langstr("VolumeNamePeriod"));
		m_sName = newName;
	}
	
	void setVolumeName0(String newName) {
		m_sName = newName;
	}

	public String getVolumeName() {
		return m_sName;
	}

	void setRootDirectory(Directory root) {
		m_dirRoot = root;
	}
	
	Directory getRootDirectory() {
		return m_dirRoot;
	}
	
	public int getTotalSectors() {
		return m_nTotalSectors;
	}
	
	public int getAUNumber(int nSectorNumber) {
		return nSectorNumber / getSectorsPerAU();
	}
	
	public int getReservedAUs() {
		return m_nReservedAUs;
	}

	abstract FormatParameters getParams();
	
	abstract int getSectorsPerAU();
	abstract byte[] createVIBContents();
	abstract Sector[] createInitSectors();	 // initialize
	abstract Sector[] createAllocationMapSectors(); 
	abstract boolean isProtected();
	abstract int getAllocMapStart();
	abstract int getAllocMapEnd();
	
	abstract void setupAllocationMap(byte[] map);
	
	/* 
		Allocation handling
	*/
	
	public Interval[] findFreeSpace(int nSectors, int nStarting) {
		List<Interval> intList = new LinkedList<Interval>();
		AllocationMap allocMap = (AllocationMap)m_allocMap.clone();
		Interval intnew;
		
		int nStartSector = 0;
//		System.out.println("find free space for " + nSectors + " sectors, starting from sector " + nStarting);

		int nSectorsPerAU = getSectorsPerAU();

		if (nSectorsPerAU > 1) {
			if ((nSectors % nSectorsPerAU)!=0) nSectors = ((nSectors/nSectorsPerAU)+1)*nSectorsPerAU;
		}
				
		// Two-Pass search: 
		// 1. Search a gap that holds as many sectors of the file
		// as possible
		// 2. Go greedy, allocate the rest
		
		int nAUSize = allocMap.getAUSize();
		int nRequiredAU = nSectors / nAUSize;
		
		int nSize = 0;
		int nEndAU = 0;
		// leave some space for the FDIRs
		int nStartAU = nStarting / nAUSize; 
		int nMaxSize = 0;
		int nMaxStart = 0;
		boolean bFirst = true;
		
		if (allocMap.getMaxAU() < 0) throw new InternalException("Negative max AU in allocation map");
		
		// First pass
		// If we hit the end of the medium, the first pass fails, and we 
		// use the second pass to try from the beginning
		while (nStartAU < allocMap.getMaxAU()) {
			// System.out.println("nStartAU = " + nStartAU);
			nStartAU = allocMap.getNextFreeAUAfter(nStartAU); 
			if (nStartAU == AllocationMap.NOTFOUND) {
				break;
			}
			
			nEndAU = allocMap.getNextAllocatedAUAfter(nStartAU, nRequiredAU);
			if (nEndAU == AllocationMap.EVENLONGER) nEndAU = nStartAU + nRequiredAU;
			// System.out.println("open="+nStartAU + ", up to (excluding) =" + nEndAU);

			nSize = nEndAU - nStartAU;

			if (nSize >= nRequiredAU) {
				nStartSector = nStartAU * nAUSize;
				 // System.out.println("Allocated area large enough; interval (sectors) = [" +nStartSector + ", " + (nStartSector + nSectors-1) + "]" );
				intList.add(new Interval(nStartSector, nStartSector + nSectors-1)); 
				// we are done; we do not need a second pass
				return intList.toArray(new Interval[1]);
			}
			// else this space does not suffice, continue search
			
			// better than last time?
			if (nSize > nMaxSize) {
				nMaxSize = nSize;
				nMaxStart = nStartAU;
			}
			nStartAU = nEndAU;
		}
		
		// System.out.println("Allocated area not large enough; required=" + nRequiredAU + ", found=" + nMaxSize);
		
		// If we are here, the largest gap was not large enough. OTOH this 
		// also means that the gap of AUs that we found can be completely 
		// filled.
		
		// Put largest chunk into list (nMaxStart, nLastMaxAUSize)
		// If nMaxStart is 0, we reached the end of the medium. Do not add anything to the list.
		if (nMaxStart != 0) {
			nStartSector = nMaxStart * nAUSize;
			// System.out.println("Allocated first part; interval (sectors) = [" +nStartSector + ", " + (nStartSector + (nMaxSize * nAUSize) - 1) + "]" );
			intnew = new Interval(nStartSector, nStartSector + (nMaxSize * nAUSize) - 1);
			intList.add(intnew); 
			allocMap.allocate(intnew);
			nRequiredAU -= nMaxSize;
			nSectors -= nMaxSize*nAUSize;
		}
		
		// Second pass
		nStartAU = 1;
		while (nStartAU < allocMap.getMaxAU()) {
			nStartAU = allocMap.getNextFreeAUAfter(nStartAU);
			if (nStartAU==-1) {
				// System.out.println("No free AU available; failed to find free space.");
				return null;
			}
			// System.out.println("next free = " + nStartAU);
			
			if (nStartAU == nMaxStart) {
				// System.out.println("no, " + nStartAU + " is already chosen");
				// Skip the largest chunk
				nStartAU += nMaxSize;
				// System.out.println("new start = " + nStartAU);
				continue;
			}
			nEndAU = allocMap.getNextAllocatedAUAfter(nStartAU, nRequiredAU);
			// System.out.println("nStartAU = " + nStartAU + ", nEndAU = " + nEndAU);
			if (nEndAU==AllocationMap.EVENLONGER) nEndAU = nStartAU + nRequiredAU;
			
			nSize = nEndAU - nStartAU;
			// System.out.println("nSize = " + nSize);
			nRequiredAU -= nSize;
			if (nRequiredAU < 0) {
				// System.err.println("nRequiredAU = " + nRequiredAU);
				nEndAU = nEndAU + nRequiredAU;
				nSize = nEndAU - nStartAU;
				nRequiredAU = 0;
			}
			// System.out.println("Allocated another part; interval (sectors) =  [" +nStartAU*nAUSize + ", " +  ((nStartAU+nSize)*nAUSize-1) + "]" );
			intnew = new Interval(nStartAU * nAUSize, (nStartAU+nSize)*nAUSize-1);
			intList.add(intnew); 
			// System.out.println("Still needed: " + nRequiredAU);
			nStartAU = nEndAU;
			if (nRequiredAU <= 0) {
				// we are done
				return intList.toArray(new Interval[intList.size()]);
			}
		}
		// If we are here, we failed in the second pass.
		return null;
	}
	
	public int getAllocatedSectorCount() {
		return m_allocMap.countAllocated() * getSectorsPerAU();
	}

	public AllocationMap getAllocationMap() {
		return m_allocMap;
	}
	
	void allocate(Interval intv) {
		m_allocMap.allocate(intv);
	}
	
	void deallocate(Interval intv) {
		m_allocMap.deallocate(intv);
	} 
}
