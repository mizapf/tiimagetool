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

/** Represents a TI file system. */	
public abstract class TFileSystem {

	// General information from the VIB
	String m_sName;
	int	m_nTotalSectors = 0;
	
	/** Sector count according to the header. */
	int	m_nSectorsPerTrack = 0;
	int m_nHeads = 0;
	int m_nSectorsPerAU;
	int m_nReservedAUs;
	int m_nCylinders;

	Directory m_dirRoot;
	
	protected boolean m_bWriteCached;
	
	public final static int SECTOR_LENGTH=0x100;   
	public final static int MAXAU = 0xf800;  // from Directory and RawHDFormat
	static final int NONE = -1;

	// Sector allocation map
	AllocationMap m_allocMap;
	
	public TFileSystem() {
		m_bWriteCached = false;
	}

	void setRootDirectory(Directory root) {
		m_dirRoot = root;
	}
	
	Directory getRootDirectory() {
		return m_dirRoot;
	}
		
	abstract void setupFromFile(byte[] sector0, byte[] abyAlloc, boolean bCheck) throws MissingHeaderException, ImageException;

	public String getName() {
		return m_sName;
	}
	
	void setName(String newName) throws InvalidNameException {
		if (newName == null || newName.length()==0 || newName.length()>10) throw new InvalidNameException(TIImageTool.langstr("VolumeNameConstr"));
		if (newName.indexOf(".")!=-1) throw new InvalidNameException(TIImageTool.langstr("VolumeNamePeriod"));
		m_sName = newName;
	}
	
	public int getCylinders() {
		return m_nCylinders;
	}
	
	public int getSectors() {
		return m_nSectorsPerTrack;
	}
	
	public int getSectorLength() {
		return SECTOR_LENGTH;
	}
	
	public int getSectorsPerAU() {
		return m_nSectorsPerAU;
	}

	public int getHeads() {
		return m_nHeads;
	}
	
	public int getReservedAUs() {
		return m_nReservedAUs;
	}
	
	public int getTotalSectors() {
		return m_nTotalSectors;
	}
	
	public boolean isProtected() {
		return false;
	}
	
	public boolean isWriteCached() {	
		return m_bWriteCached;
	}
	
	abstract Location lbaToChs(int nSectorNumber) throws ImageException;
	abstract int chsToLba(int cylinder, int head, int sector);
	
	static boolean hasFloppyVib(byte[] abySect) {
		return (abySect[13]=='D' && abySect[14]=='S' && abySect[15]=='K');	
	}
	
	public int toAU(int nSectorNumber) {
		return nSectorNumber / m_nSectorsPerAU;
	}
	
	abstract Sector[] initialize(FormatParameters param);
	
	abstract int getAllocMapStart();
	abstract int getAllocMapEnd();

	abstract Sector[] getAllocationMapSectors(); 

	abstract byte[] createVIB();
	
	public Interval[] findFreeSpace(int nSectors, int nStarting) {
		List<Interval> intList = new LinkedList<Interval>();
		AllocationMap allocMap = (AllocationMap)m_allocMap.clone();
		Interval intnew;
		
		int nStartSector = 0;
//		System.out.println("find free space for " + nSectors + " sectors, starting from sector " + nStarting);

		if (m_nSectorsPerAU > 1) {
			if ((nSectors % m_nSectorsPerAU)!=0) nSectors = ((nSectors/m_nSectorsPerAU)+1)*m_nSectorsPerAU;
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
	
	public int getAllRequiredSectors(int nAUSize) {
		// Sector 0 is already used for the root directory
		// Allocation map is included in sector 0 for floppies
	
		int nAllocMapSectors = ((m_allocMap.getMaxAU()/8)-1) / SECTOR_LENGTH + 1; 
		
		// Round up to AU size
		// Starts with sector 1, so we must ignore the first AU
		// sector 1 .. ausize-1
		
		nAllocMapSectors = nAllocMapSectors - (nAUSize-1);
		
		int nAllocMapAU = ((nAllocMapSectors-1) / nAUSize) + 1;
		return nAllocMapAU;
	}
	
	public int getAllocatedSectorCount() {
		return m_allocMap.countAllocated() * m_nSectorsPerAU;
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
