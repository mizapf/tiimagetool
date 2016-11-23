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
import de.mizapf.timt.util.TIFiles;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.util.LZW;
import de.mizapf.timt.basic.BasicLine;

public class TFile extends Element {

	// ================================================
	// Information from the FIB
	protected int			m_nRecordLength;
	protected byte			m_byFlags;
	protected int			m_nRecordsPerSector;
	protected int			m_nAllocatedSectors;
	protected int			m_nEOFOffset;
	protected int			m_nNumberOfRecords;
	protected Time			m_tUpdate;
	protected Interval[] 	m_aCluster;
	protected int[] 		m_anFIBSector;  // required for deleting
	protected int			m_nFDIRAU;
	protected boolean		m_bL3Flaw;
	
	public final static byte VARIABLE = (byte)0x80;
	public final static byte EMULATE = (byte)0x20;
	public final static byte MODIFIED = (byte)0x10;
	public final static byte PROTECTED = (byte)0x08;
	public final static byte INTERNAL = (byte)0x02;
	public final static byte PROGRAM = (byte)0x01;
	
	public final static int T_DISFIX = 0;
	public final static int T_DISVAR = 1;
	public final static int T_INTFIX = 2;
	public final static int T_INTVAR = 3;
	public final static int T_PROGRAM = 4;
	public final static int T_EMULATE = 5;
	
	// ================================================

	protected final static int NAMELEN = 15;
	protected final static int SIZELEN = 5;
	protected final static int TYPELEN = 10;
	protected final static int RECLEN = 8;
	protected final static int PROLEN = 3;
	protected final static int CRELEN = 20;
	protected final static int UPDLEN = 18;
		
	/** Also accessed by ImageManager. */
	public TFile(byte[] abyTif) throws ImageException {
		TIFiles tif = new TIFiles(abyTif);
		m_sName = tif.getTFIName();
		m_nRecordLength = tif.getRecordLength();
		m_byFlags = tif.getFlags();
		m_nRecordsPerSector = tif.getRecordsPerSector();
		m_nEOFOffset = tif.getEOFOffset();
		m_nNumberOfRecords = tif.getNumberOfRecords();
		m_tCreation = tif.getCreationTime();
		m_tUpdate = tif.getUpdateTime();
		m_nAllocatedSectors = TIFiles.getTotalNumberOfSectors(abyTif);
		checkL3();		
		m_dirParent = null;
		m_bProtected = false;
	}
		
	TFile(Volume vol, Sector sectFile, Directory dirParent) throws IOException, ImageException {
		byte[] aby = sectFile.getBytes();
		m_sName = Utilities.getString10(aby, 0);
		m_nRecordLength = (Utilities.getInt16(aby, 0x0a)<<8) + (aby[0x11] & 0xff);
		m_byFlags = aby[0x0c];
		m_nRecordsPerSector = aby[0x0d] & 0xff;
		m_nAllocatedSectors = Utilities.getInt16(aby, 0x0e);
		m_nEOFOffset = aby[0x10] & 0xff;
		m_nNumberOfRecords = Utilities.getInt16rev(aby, 0x12);
		if (!vol.isFloppyImage() && !vol.isCF7Image()) {
			m_nAllocatedSectors += ((aby[0x26] & 0xf0)<<12);
			m_nNumberOfRecords += ((aby[0x26] & 0x0f)<<16);
			m_nFDIRAU = Utilities.getInt16(aby, 0x24);
		}
		m_tCreation = new Time(aby, 0x14);
		m_tUpdate = new Time(aby, 0x18);
		m_anFIBSector = getAllFIB(vol, sectFile);
		m_aCluster = getAllClusters(vol, m_anFIBSector);
		m_dirParent = dirParent;
		m_bProtected = false;
		checkL3();		
	}
	
	/** Used for archive files. */
	protected TFile() {
		m_bProtected = false;
	}

	private void checkL3() throws ImageException {
		m_bL3Flaw = false;
		if (hasFixedRecordLength()) {
			if (getRecordCount()==0) {
				if (m_nAllocatedSectors > 0) {
					System.err.print(getVolume().getImageName() + ": File " + getName() + " is a fixed data file, but has 0 record count. ");
					m_nNumberOfRecords = m_nRecordsPerSector * m_nAllocatedSectors;
					System.err.println("Assuming record count = " + m_nNumberOfRecords + " from sectors = " +  m_nAllocatedSectors + " and records/sector = " + m_nRecordsPerSector + ".");
				}
			}
			else {
				int nMinRec = m_nRecordsPerSector * (m_nAllocatedSectors-1) + 1;
				if (nMinRec < 0) nMinRec = 0;
				int nMaxRec = m_nRecordsPerSector * m_nAllocatedSectors;
				if (getRecordCount() < nMinRec || getRecordCount() > nMaxRec) {
					int nTry = ((getRecordCount() & 0xff) << 8) | ((getRecordCount()>>8) & 0xff); 
					// Check whether this is a little-endian error 
					if (nTry < nMinRec || nTry > nMaxRec) {
						// No, treat it as corrupt
						System.err.println(getVolume().getImageName() + ": File " + getPathname() + " has unplausible record count: " + getRecordCount() + " (0x" + Utilities.toHex(getRecordCount(), 4) + ")");
						System.err.println("  Minimum record count = " + nMinRec + ", maximum record count = " + nMaxRec);
						m_nNumberOfRecords = m_nRecordsPerSector * m_nAllocatedSectors;
						System.err.println("  Assuming record count = " + m_nNumberOfRecords + " (0x" + Utilities.toHex(m_nNumberOfRecords, 4) + ") from sectors = " +  m_nAllocatedSectors + " and records/sector = " + m_nRecordsPerSector + ".");
					}
					else {
						// Yes, swap it
						System.err.println(getVolume().getImageName() + ": File " + getName() + " has swapped L3 record count");
						m_nNumberOfRecords = nTry;
						m_bL3Flaw = true;
					}
				}
			}
			if (m_nNumberOfRecords > 1000000) throw new ImageException("Disk file system damaged; file descriptor record corrupt");
		}
	}
	
	private static int[] getAllFIB(Volume vol, Sector sect) throws IOException, ImageException {
		ArrayList<Integer> lst = new ArrayList<Integer>();
		lst.add(sect.getNumber());
		
		int nAUSize = vol.getAUSize();
		int nNextFibAU = 0;
		byte[] aby = sect.getBytes();
		
		do {
			if (!vol.isFloppyImage() && !vol.isCF7Image()) {
				nNextFibAU = Utilities.getInt16(aby, 0x20);
				if (nNextFibAU != 0) {
					int nOffset = (aby[0x27] & 0x0f);
					int nSectorNumber = nNextFibAU * nAUSize + nOffset;
					sect = vol.readSector(nSectorNumber);
					aby = sect.getBytes(); 
					lst.add(nSectorNumber);
				}
			} 
		}
		while (nNextFibAU!=0);
		
		int[] anFib = new int[lst.size()];
		int i=0;
		Iterator<Integer> it = lst.iterator();
		while (it.hasNext()) anFib[i++] = it.next();
		
		return anFib;
	}
	
	private Interval[] getAllClusters(Volume vol, int[] anFIBSector) throws IOException, ImageException {
		List<Interval> lstSum = new ArrayList<Interval>();
		for (int i : anFIBSector) {
			Sector sect = vol.readSector(i);
			byte[] aby = sect.getBytes();
			lstSum.addAll(getDataChainPointerBlockList(vol, aby));			
		}
	
		Interval[] acluster = new Interval[lstSum.size()];
		lstSum.toArray(acluster);
		return acluster;
	}
	
	/** Delivers the intervals of the data chain. The intervals reference
		sectors, not AUs. */
	private static List<Interval> getDataChainPointerBlockList(Volume vol, byte[] aby) {
		int nPos;
		List<Interval> lst = new ArrayList<Interval>(); 
		int nStartValue = 0;
		int nEndValue = 0;
		int nLastOffset = 0;
		
		if (vol.isFloppyImage() || vol.isCF7Image()) {
			//	28	   29	  30	 31		32	33	34	35 36
			//	n2n1   m1n3	  m3m2
			//	for N = 0 n3 n2 n1
			//		M = 0 m3 m2 m1
		
			// N is the start sector
			// M is the highest number of the sector from the beginning of the
			//	  file to be retrieved from this chain (starting with 0)
			//
			// NOTE: N references the AU for media bigger than 720 KiB
			//		 M is always sector-oriented
			// 1440 KiB, AUsize=4
			//	80 30 03 = 080/033; sector=200, length=33 (34 sectors!)
			//	8D 30 00 = 08D/003; sector=234, length=3 (4 sectors!)
			//
			// Example: 0421/02, 0585/04
			//	 start at 0421, read three sectors (0, 1, 2)
			//	 continue at 0585, read two more (3, 4)
			
			// 23 c0 01 => 023 -> 01c (+023 = 3f) 
//			  System.out.print(getFileName() + ": intervals = { ");			   
			nPos = 28;
			do {
				nStartValue = ((aby[nPos+1]&0x0f)<<8) + (aby[nPos]&0xff);
				int nOffset = ((aby[nPos+2]&0xff)<<4) + ((aby[nPos+1]&0xf0)>>4);

				int nEffectiveAUSize = (vol.getTotalSectors()>4095)? vol.getAUSize() : 1;
				nStartValue = nStartValue * nEffectiveAUSize;
				
				nEndValue = nStartValue + nOffset - nLastOffset;
				nLastOffset = nOffset+1;
		
				if (nStartValue!=0 && nEndValue!=0) {
					Interval in =  new Interval(nStartValue, nEndValue);
//					  System.out.print("[" + in.start + "," + in.end + "] ");
					lst.add(in);
					nPos = nPos + 3;
				}
			}
			while (nPos < 255 && nStartValue!=0 && nEndValue!=0);			 
//			  System.out.println("}");
		}
		else {
			// first AU, last AU (including)
			
			//	40	41	42	43	44	45 46  47	48
			// a11 a12 b11 b12 a21 a22 b21 b22	0
			
			nPos = 40;
			do {
				nStartValue = Utilities.getInt16(aby, nPos) * vol.getAUSize();
				nEndValue = (Utilities.getInt16(aby, nPos+2)+1) * vol.getAUSize() - 1; 
				
				if (nStartValue!=0 && nEndValue!=0) {
					lst.add(new Interval(nStartValue, nEndValue));
					nPos = nPos + 4;
				}
			}
			while (nPos < 254 && nStartValue!=0 && nEndValue!=0);
		}
		return lst;		   
	}
	
	/** Called from Directory.moveinFile/renameElement/writeFDIR. */
	public int getFIBLocation() {
		return m_anFIBSector[0];
	}

	/** Called from from ImageCheck and Directory.deleteFile */
	public int[] getFIBLocations() {
		return m_anFIBSector;
	}
	
	public boolean isFragmented() {
		return m_aCluster.length>1;
	}
	
	public byte getFlags() {
		return m_byFlags;
	}
	
	public Time getUpdateTime() {
		return m_tUpdate;
	}
	
	public int getAllocatedSectors() {
		return m_nAllocatedSectors;
	}

	public int getEOFOffset() {
		return m_nEOFOffset;
	}
	
	public int getRecordsPerSector() {
		return m_nRecordsPerSector;
	}
	
	public int getRecordLength() {
		return m_nRecordLength;
	}
		
	public static boolean validName(String sName) {
		if (sName==null) return false;
		byte[] abyName = sName.getBytes();
		for (int i=0; i < abyName.length; i++) {
			if (abyName[i] < 0x20 || abyName[i] > 0x7f) return false;
		}
		if (sName.indexOf(".")!=-1) return false;
		return true;
	}
	
	public int getUsedSectors() {
		return m_nAllocatedSectors;
	}
	
	/** Returns the number of sectors for the content and for the FIBs. */
	public int getAllRequiredSectors(int nAUSize) {
		int nTotal = getUsedSectors();
		if ((nAUSize > 1) && ((nTotal %  nAUSize)!=0)) nTotal = ((nTotal / nAUSize)+1) * nAUSize;
		// Add all FIBs, but only those that occupy position 0 in the AU
		// All other positions are already allocated via position 0.
		for (int i=0; i < m_anFIBSector.length; i++) {
			if ((nAUSize == 1) || ((m_anFIBSector[i] % nAUSize)==0)) nTotal = nTotal + nAUSize;
		}
		return nTotal;
	}
	
	public int getRecordCount() {
		return m_nNumberOfRecords;
	}
		
	void setClusters(Interval[] clusters) {
		m_aCluster = clusters;
	}
	
	/** Called from Directory.moveinFile/insertFile/renameFile. */
	void writeFIB(int nSectorNumber, int nFDIRSector) throws ProtectedException, IOException, ImageException {
		byte[] aFibNew = new byte[256];

		Volume vol = getVolume();
		
		// Set the new FIB
		m_anFIBSector = new int[1];
		m_anFIBSector[0] = nSectorNumber; 
		
		// Set the name
		// Fill with spaces
		Arrays.fill(aFibNew, 0, 10, (byte)0x20);
		byte[] abyName = getName().getBytes();
		System.arraycopy(abyName, 0, aFibNew, 0, abyName.length);

		// Set the extended record length. TIFILES does not support this.
		Utilities.setInt16(aFibNew, 0x0a, 0);

		// Flags
		aFibNew[0x0c] = getFlags();

		// Rec/sec
		aFibNew[0x0d] = (byte)getRecordsPerSector();

		// Number of sectors
		Utilities.setInt16(aFibNew, 0x0e, getAllocatedSectors() & 0xffff);

		// EOF offset
		aFibNew[0x10] = (byte)getEOFOffset();

		// Logical rec len
		aFibNew[0x11] = (byte)(getRecordLength() & 0x00ff);

		// Level 3
		aFibNew[0x12] = (byte)(getRecordCount() & 0x00ff);
		aFibNew[0x13] = (byte)((getRecordCount() & 0xff00)>>8);
		
		// Date/Time of creation / update
		System.arraycopy(m_tCreation.getBytes(), 0, aFibNew, 0x14, 4);
		System.arraycopy(m_tUpdate.getBytes(), 0, aFibNew, 0x18, 4);

		if (vol.isFloppyImage() || vol.isCF7Image()) {
			// Floppy version

			// clear all
			Arrays.fill(aFibNew, 0x1c, 0x100, (byte)0x00);
			
			// Data chain pointers
			int nlast = -1;
			for (int i=0; i < m_aCluster.length; i++) {
				int n = m_aCluster[i].end - m_aCluster[i].start + nlast + 1;
				int nStart = m_aCluster[i].start;
				if (vol.getTotalSectors() > 4095) nStart = nStart / vol.getAUSize();
				aFibNew[0x1c + i*3+0] = (byte)(nStart & 0x00ff);
				aFibNew[0x1c + i*3+1] = (byte)(((nStart & 0x0f00)>>8)|((n&0x000f)<<4));
				aFibNew[0x1c + i*3+2] = (byte)((n & 0x0ff0)>>4);
				nlast = n;
			}
		}
		else {
			// HD version

			// Complete the fields
			aFibNew[0x1c] = (byte)'F';
			aFibNew[0x1d] = (byte)'I';

			// Previous FDR 
			Utilities.setInt16(aFibNew, 0x1e, 0);

			// Next FDR
			Utilities.setInt16(aFibNew, 0x20, 0);

			// Link to FDRIndex
			Utilities.setInt16(aFibNew, 0x24, nFDIRSector / vol.getAUSize());

			// Extended information is 0, since TIFILES does not support long files
			Utilities.setInt16(aFibNew, 0x26, 0);
			
			// For HD images the Data chain consists of AU start/end pairs
			int nAUsFromHere = 0;
			for (int i=0; i < m_aCluster.length; i++) {
//				System.out.println("Interval " + i + ": [" + aint[i].start + ", " + aint[i].end + "]");
				nAUsFromHere += (m_aCluster[i].end - m_aCluster[i].start)/vol.getAUSize() + 1; 
				aFibNew[0x28 + i*4] = (byte)(((m_aCluster[i].start / vol.getAUSize())>>8) & 0xff);
				aFibNew[0x29 + i*4] = (byte)((m_aCluster[i].start / vol.getAUSize()) & 0xff);
				aFibNew[0x2a + i*4] = (byte)(((m_aCluster[i].end / vol.getAUSize())>>8) & 0xff);
				aFibNew[0x2b + i*4] = (byte)((m_aCluster[i].end / vol.getAUSize()) & 0xff);
			}

			// #AU of the file allocated by this FIB		
			Utilities.setInt16(aFibNew, 0x22, nAUsFromHere);
			
		}
		// write the new FIB (always sector number)
		vol.writeSector(nSectorNumber, aFibNew);
	}
	
	/** Does this file have a swapped L3 count? */
	public boolean hasSwappedL3Count() {
		return m_bL3Flaw;
	}
	
	/** Rewrite the FIB. This is done to swap the L3 values (which have been swapped already on loading). **/
	public void rewriteFIB() throws ProtectedException, IOException, ImageException {
		Volume vol = getVolume();
		int fdir = (vol.isFloppyImage() || vol.isCF7Image())? 1 : (m_nFDIRAU * vol.getAUSize());
		m_bL3Flaw = false;
		writeFIB(m_anFIBSector[0], fdir); 
	}
	
	// File Size Type Length Protection Created Updated
	public String toFormattedString() {
		String sPattern = "%1$-10s %2$4d %3$-7s %4$6d %5$1s %6$1s %7$20s %8$20s";
		int nSize = 0;
		if (isProgram()) nSize = getProgramLength();
		else nSize = m_nRecordLength;
		
		String sProt = " ";
		if ((m_byFlags & PROTECTED)!=0) sProt = "P";
		String sFrag = " ";
		if (isFragmented()) sFrag = "F";
		return String.format(sPattern, getName(), getUsedSectors(), typeToString(), 
			nSize, sProt, sFrag, m_tCreation.toString(), m_tUpdate.toString());
	}

	/** Retrieves the content as the sector content. The file is not truncated according to EOF. */
	public byte[] getSectorContent() throws IOException, ImageException {
		if (getUsedSectors() == 0) return new byte[0];

		Volume vol = getVolume();
		byte[] aby = new byte[getUsedSectors() * Volume.SECTOR_LENGTH];
		int nAUSize = vol.getAUSize();
		
		int nSectorsInLastAU = (nAUSize==1)? 1 : (getUsedSectors() % nAUSize);
		if (nSectorsInLastAU==0) nSectorsInLastAU = nAUSize;
		
		int nSectorInCluster = 0;
		int nClusterPointer = 0;
		int nRead = 0;
		int nSector = 0;
		Interval current = null;
		
		if (m_aCluster.length==0) {
			throw new ImageException("File " + getPathname() + " has no content");
		}

		for (int i=0; i < getUsedSectors(); i++) {
			
			current = m_aCluster[nClusterPointer]; 
			nSector = current.start + nSectorInCluster;
				
			if (nClusterPointer==m_aCluster.length-1)	{ // last cluster
				if (vol.isFloppyImage() || vol.isCF7Image()) {
					// Interval bounds are precise
					if (nSector == current.end + 1) throw new ImageException("Cannot find next sector for file " + getName());
				}
				else {
					if (nSector == (current.end - nAUSize + nSectorsInLastAU + 1)) {
						// System.out.println("sector = " + nSector  + ", sectors in last au = " + m_nSectorsInLastAU + ", current.end = " + current.end + ", last cluster and last sector");
						throw new ImageException("Cannot find next sector for file " + getName() + ", " + (getUsedSectors() - nRead) + " sectors not found in clusters");
					}
				}
			}
			// xxxxxxxx xxxxxxxx xxxxxxxx xxx.....
			// 3 in last, ausize=8
			// interval = [0,31] => last_sec = 26 (end-(ausize-inlast))
			
			if (nSector == (current.end + 1)) {
				//			  System.out.println("Next chain\n");
				nClusterPointer++;
				nSectorInCluster = 0; 
				nSector = m_aCluster[nClusterPointer].start; 
			}
			nSectorInCluster++;

			nRead++;
			Sector sect = vol.readSector(nSector);
			System.arraycopy(sect.getBytes(), 0, aby, i*256, 256);
		}
		// System.out.println(Utilities.hexdump(0, 0, aby, aby.length, false));
		return aby;
	}
	
	public int getPhysicalSectorNumber(int nLogicalSector) throws FormatException, ImageException {
		if (getUsedSectors() == 0) throw new FormatException(getName(), "is an empty file");

		Volume vol = getVolume();
		int nAUSize = vol.getAUSize();
		
		int nSectorsInLastAU = (nAUSize==1)? 1 : (getUsedSectors() % nAUSize);
		if (nSectorsInLastAU==0) nSectorsInLastAU = nAUSize;
		
		int nSectorInCluster = 0;
		int nClusterPointer = 0;
		int nSector = 0;
		Interval current = null;
		
		if (m_aCluster.length==0) {
			throw new ImageException("File has no content");
		}

		for (int i=0; i < getUsedSectors(); i++) {
			
			current = m_aCluster[nClusterPointer]; 
			nSector = current.start + nSectorInCluster;
				
			if (nClusterPointer==m_aCluster.length-1)	{ // last cluster
				if (vol.isFloppyImage() || vol.isCF7Image()) {
					// Interval bounds are precise
					if (nSector == current.end + 1) throw new ImageException("Cannot find next sector for file " + getName());
				}
				else {
					if (nSector == (current.end - nAUSize + nSectorsInLastAU + 1)) {
						// System.out.println("sector = " + nSector  + ", sectors in last au = " + m_nSectorsInLastAU + ", current.end = " + current.end + ", last cluster and last sector");
						throw new ImageException("Cannot find next sector for file " + getName() + ", " + (getUsedSectors() - i) + " sectors not found in clusters");
					}
				}
			}
			// xxxxxxxx xxxxxxxx xxxxxxxx xxx.....
			// 3 in last, ausize=8
			// interval = [0,31] => last_sec = 26 (end-(ausize-inlast))
			
			if (nSector == (current.end + 1)) {
				//			  System.out.println("Next chain\n");
				nClusterPointer++;
				nSectorInCluster = 0; 
				nSector = m_aCluster[nClusterPointer].start; 
			}
			nSectorInCluster++;

			if (i == nLogicalSector) return nSector;
		}
		throw new FormatException(getName(), "Logical sector " + nLogicalSector + " not found");
	}
	
	/** Returns the binary contents, clipped to the length of the file. */
	public byte[] getRawContent() throws IOException, ImageException {
		byte[] abyReturn = null;
		int nLength = 0;
		if (isProgram()) nLength = getProgramLength();
		else nLength = getByteLength();

		if (nLength == 0) System.err.println("File size of " + getName() + " is zero.");
		abyReturn = new byte[nLength];
		try {
			System.arraycopy(getSectorContent(), 0, abyReturn, 0, nLength);
		}
		catch (ArrayIndexOutOfBoundsException ax) {
			throw new ImageException("Header of file " + getName() + " possibly corrupt; file is short than expected");
		}
		return abyReturn;
	}

	public int getProgramLength() {
		int nSectors = 0;
		int nLength = 0;
		nSectors = getAllocatedSectors();
		nLength = nSectors * Volume.SECTOR_LENGTH;
		if (nLength > 0 && getEOFOffset()!=0) {
			nLength = nLength - 256 + getEOFOffset();
		}
		return nLength;
	}
	
	public int getByteLength() {
		int nSectors = 0;
		int nLength = 0;
		if (hasFixedRecordLength()) {
			/*
			int nCount = getRecordCount() / getRecordsPerSector();
			if ((nCount * getRecordsPerSector()) < getRecordCount()) nCount++;
			nSectors = nCount; 
			nLength = getRecordCount() * getRecordLength();
			*/
			nSectors = getAllocatedSectors();
			nLength = nSectors * Volume.SECTOR_LENGTH;				
		}
		else {
			nSectors = getRecordCount();
			nLength = nSectors*256;
		}
		
		if (nLength > 0 && getEOFOffset()!=0) {
			nLength = nLength - 256 + getEOFOffset();
			if (!hasFixedRecordLength() && !isProgram()) nLength++;  // add the EOF marker to the content
		}
		return nLength;
	}

	public byte[] getRecordContent() throws IOException, FormatException, ImageException {
		String sEOR = "\n";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int nPointer = 0;
		int nRecord = 0;
		boolean bDone = false;
		int nLevel3RecordCount = getRecordCount();
		int nRecordLength = getRecordLength();
		
		byte[] abyContent = null;
		
		if (hasFixedRecordLength()) {
			abyContent = getSectorContent();
			while (!bDone) {
				sEOR = null;
				try {
					//					  System.out.println("Pointer = " + nPointer + ", Length = " + nRecordLength + " #Rec = " + nRecord + " #Records = " + nNumberOfRecords);
					baos.write(abyContent, nPointer, nRecordLength);
				}
				catch (IndexOutOfBoundsException ix) {
					System.err.println("Error: abyContent.length=" + abyContent.length + ", nPointer=" + nPointer + ", recordLength=" + nRecordLength);
				}
				if (sEOR != null) baos.write(sEOR.getBytes());
				nRecord++;
				if (nRecord == getRecordsPerSector()) {
					nRecord = 0;
					nPointer = (nPointer - (nPointer%256)) + 256;
				}
				else nPointer += nRecordLength;
				nLevel3RecordCount--;
				if (nLevel3RecordCount<=0) bDone = true;
			}
		}
		else {
			abyContent = getRawContent();
			while (!bDone) {
				sEOR = "\n";
				nRecordLength = abyContent[nPointer]&0xff;
				boolean bClipped = false;
				if (nRecordLength!=0xff) {
					nPointer++;
					if (nPointer + nRecordLength >= abyContent.length) {
						System.err.println("File clipped");
						nRecordLength = abyContent.length-nPointer;
						bClipped = true;
					}							
					try {
						baos.write(abyContent, nPointer, nRecordLength);
					}
					catch (IndexOutOfBoundsException ibx) {
						System.err.println("abyContent.length = " + abyContent.length + ", Pointer = " + nPointer + ", Length = " + nRecordLength);
					}
					baos.write(sEOR.getBytes());
					nPointer += nRecordLength;
					if (bClipped) break;
				}
				else {
					nPointer = (nPointer - (nPointer%256)) + 256;
					if (nPointer >= abyContent.length) bDone = true;
				}
			}
		}
		return baos.toByteArray();
	}
	
	public String getTextContent() throws IOException, FormatException, ImageException {
		return new String(getRecordContent());
	}
	

// TODO: Drop this method in favor of TIFiles.writeRecord
	public static byte[] textToSectors(String[] asLine) {
		// ending with 0xff in sector
		// 00 is an empty line (length 0)
		
		// Lines longer than 80 characters are wrapped
		// Characters outside of 00-7f are replaced by a .
		// (should offer a replacement table)
				
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int nTotalLength = 0;
		for (String sLine:asLine) {
			int nPos = 0;
			boolean bLineDone = false;
			// Split long lines
			while (!bLineDone) {
				int nPosEnd = nPos + 80;
				if (nPosEnd > sLine.length()) nPosEnd = sLine.length();
				String sCurrentLine = sLine.substring(nPos, nPosEnd);
				if (nTotalLength + sCurrentLine.length() + 1 > 255) {
					bos.write((byte)0xff);
					nTotalLength++;
					// Write padding bytes
					for (int j=nTotalLength; j<256; j++) bos.write((byte)0);
					nTotalLength = 0;
				}					
				bos.write((byte)sCurrentLine.length());
				nTotalLength++;
				for (int i=0; i < sCurrentLine.length(); i++) {
//					if (sCurrentLine.charAt(i)>=32 && sCurrentLine.charAt(i)<= 127) {
						bos.write((byte)sCurrentLine.charAt(i));
//					}
//					else {
//						bos.write((byte)'.');
//					}
					nTotalLength++;
				}
				nPos = nPosEnd;
				if (nPos==sLine.length()) bLineDone = true;
			}
		}
		// Mark EOF. Note that we must never drop into position 0.
		bos.write((byte)0xff); 
		return bos.toByteArray();
	}

	
	public String typeToString() {
		if (isActiveEmulateFile()) return "Emulate*";   // Emulate files are always PROGRAM files
		if (isEmulateFile()) return "Emulate";          // Emulate files are always PROGRAM files
		if (isProgram()) return "Program";
		if (!hasFixedRecordLength()) {
			if (!isDisplay()) return "Int/Var";
			else return "Dis/Var";
		}
		else {
			if (!isDisplay()) return "Int/Fix";
			else return "Dis/Fix";
		}
	}

	public static byte fileTypeToFlags(int type, boolean prot, boolean mod) {
		byte ret = (byte)0xff;
		byte[] atype = { (byte)0x00, VARIABLE, INTERNAL, INTERNAL | VARIABLE, PROGRAM, EMULATE };
		
		if (type >= 0 && type <=5) ret = atype[type];
		if (prot) ret |= PROTECTED;
		if (mod) ret |= MODIFIED;		
		return ret;
	}
	
	public boolean isDisplay() {
		return ((m_byFlags & INTERNAL)==0);
	}
	
	public boolean isProgram() {
		return ((m_byFlags & PROGRAM)!=0);
	}
	
	public boolean isProtected() {
		return ((m_byFlags & PROTECTED)!=0);
	}
	
	public boolean isEmulateFile() {
		return ((m_byFlags & EMULATE)!=0);
	}
	
	public boolean isActiveEmulateFile() {
		return (isEmulateFile() && getVolume().getAUEmulateSector()==getFIBLocation());
	}
	
	public boolean isModified() {
		return ((m_byFlags & MODIFIED)!=0);
	}
	
	public boolean hasFixedRecordLength() {
		return ((m_byFlags & VARIABLE)==0) && !isProgram();
	}

	public boolean hasBasicLongFormat() {
		return (!isDisplay() && !hasFixedRecordLength() && getRecordLength()==254);
	}

	public boolean hasBasicMergeFormat() {
		return (isDisplay() && !hasFixedRecordLength() && getRecordLength()==163);
	}
	
	public boolean isTextFile() {
		return (!isProgram() && isDisplay() && !hasFixedRecordLength() && getRecordLength()==80); 
	}

	public boolean mayBePrintable() {
		return (!isProgram() && isDisplay() && !hasFixedRecordLength()); 
	}
	
	public static int flagsToType(int flags) {
		if ((flags & PROGRAM) != 0) return T_PROGRAM;
		if ((flags & VARIABLE)!=0) {
			if ((flags & INTERNAL)!=0)  return T_INTVAR;
			else return T_DISVAR;
		}
		else {
			if ((flags & INTERNAL)!=0)  return T_INTFIX;
			else return T_DISFIX;
		}			
	}
	
	public boolean isImageFile() {
		if (isProgram()) {
			if (getProgramLength()==6144) return true;	// TI ARTIST
		}
		else {
			if (isDisplay() && hasFixedRecordLength()) {
				if (getRecordLength()==128) return true;	// MyArt
				if (getRecordLength()==255) return true;	// FRACTALS
			}
		}
		return false;
	}
	
	public boolean isTaggedObjectCodeFile() {
		return (isDisplay() && hasFixedRecordLength() && getRecordLength()==80);
	}
	
	/** Delivers all intervals of this file over all FIBs of this file. 
		The intervals refer to sectors. */
	public Interval[] getAllocatedBlocks() {
		return m_aCluster;
	}
	
/*********************************************************************
	BASIC handling
	
	BASIC file format:
	
	0000: XOR LNT start/end
	0002: LNT end
	0004: LNT start
	0006: Memory top (usually 37d7)
	
**********************************************************************/

	public boolean isBasicFile() throws IOException, FormatException, ImageException {
		return isBasicFile(getRawContent());
	}
	
	public boolean isBasicFile(byte[] abyContent) {
		boolean bBasic = false;
		int nLineNumber = 0;
		if (abyContent.length < 8) return false;

		if (hasBasicMergeFormat()) {
			// Check MERGE file
			// Format (plain, no EOR inserted)
			// [len lineno1 lineno2 token(len-3) 00 ]+ 02 ff ff
			// When ff appears as length, the record is terminated; go to next sector
			// Note that 00 may appear in the token field as empty string length
			int nPos = 0;
			int nOldLine = 0;
			int state = 0;
			int nLen = 0;
			
			while (nPos < abyContent.length) {
//				System.out.println(toHex(abyContent[nPos],2) + ", state = " + state + ", nLen = " + nLen);
				switch (state) {
				case 0:
					nLen = (abyContent[nPos] & 0x00ff);
					if (nLen == 255) {
						// Record end
						nPos = (nPos & 0xff00)+0xff;
						state = 0;
						break;
					}
					if (nLen > 163) {
						System.err.println("Invalid record length");
						return false;
					}
					state = 1;
					break;
				case 1: 
					nLineNumber = (abyContent[nPos] << 8) & 0xffff;
					state = 2;
					nLen--;
					break;
				case 2:
					nLineNumber = nLineNumber | (abyContent[nPos] & 0x00ff);
					if (nLen == 1 && nLineNumber == 0xffff) {
						state = 5;
						break;
					}
					if (nLineNumber <= nOldLine) {
						System.err.println("Not a merge file: bad line order");
						return false;
					}
					if (abyContent[nPos+1]==0) {
						System.err.println("Not a merge file: line empty");
						return false; // empty line
					}
					nOldLine = nLineNumber;
					state = 3;
					nLen--;
//					System.out.println("Line number = " + nLineNumber + ", still awaiting " + nLen + " bytes");
					break;
				case 3:
					if (nLen==1) state = 4;
					nLen--;
					break;
				case 4:
					nPos--;
					state = 0;
					break;
				case 5:
					if (nPos != abyContent.length-1) {
						System.err.println("Additional bytes at the end; nPos = " + nPos + ", length = " + abyContent.length);
						// return false;
					}
					break;
				}
				nPos++;
			}
			if (state != 5) {
				System.err.println("Premature end, state = " + state);
				return false;
			}
			return true;
		}

		if (isProgram()) {
			int nCheck = Utilities.getInt16(abyContent, 0);
			int nEndLNT = Utilities.getInt16(abyContent, 2);
			int nStartLNT = Utilities.getInt16(abyContent, 4);
			int nMemTop = Utilities.getInt16(abyContent, 6);
			int nLineAddress = 0;
			int nPrevLineNumber = -1;
			int nPrevAddress = 0xffff;
			int nPos = 8;
			
			int nStart = nMemTop - abyContent.length + 1;
			
			//		System.out.println("nEndLNT = " + nEndLNT + ", nStartLNT = " + nStartLNT + ", check = " + (nEndLNT ^ nStartLNT));  
			
			// Checksum is (end XOR start) of Line Number Table
			if ((nCheck & 0x8000) == 0x8000) {
				nCheck = (-nCheck & 0xffff);
				//			System.out.println("protected file, new value = " + nCheck);
			}
			if (nCheck != ((nEndLNT ^ nStartLNT)&0xffff)) return false;
			//		System.out.println("check ok");
			
			// Start of LNT must be below its end, and both must be below 
			// the VDP memory top
			if (nStartLNT >= nEndLNT || nEndLNT >= nMemTop) return false;
			//		System.out.println("LNT ok");
			
			// Last byte must be 0 (all lines are 0-terminated)
			//		if (abyContent[abyContent.length-1]!=0) return false;
			//		System.out.println("EOF ok");
			return true;
		}
		
		if (hasBasicLongFormat()) {
			// Long format from Extended Basic
			if (abyContent[0] != (byte)0x0a) return false;
			if (Utilities.getInt16(abyContent, 1) != 0xabcd) return false;
			
			int nCheck = Utilities.getInt16(abyContent, 7);
			int nEndLNT = Utilities.getInt16(abyContent, 5);
			int nStartLNT = Utilities.getInt16(abyContent, 3);
			int nMemTop = Utilities.getInt16(abyContent, 9);
			if ((nCheck & 0x8000) == 0x8000) {
				nCheck = (-nCheck & 0xffff);
			}
			if (nCheck != ((nEndLNT ^ nStartLNT)&0xffff)) return false;
			if (nStartLNT >= nEndLNT || nEndLNT >= nMemTop) return false;
			return true;
		}

		return false;
	}

	public String listBasic(int version) throws IOException, FormatException, ImageException {	
		return listBasic(getRawContent(), version);
	}
		
	public String listBasic(byte[] content, int version) throws ImageException {
		List<BasicLine> basicprog = new ArrayList<BasicLine>();
			
		int nLineNo = 0;
	
		if (hasBasicMergeFormat()) {
			int nPos = 0;
			int nLineLen = 0;
			// Format (plain, no EOR inserted)
			// [len lineno1 lineno2 token(len-3) 00]+ 02 ff ff
			while (nLineNo != 0xffff && nPos < content.length-2) {
				nLineLen = content[nPos] & 0xff;
				if (nLineLen == 0x00ff) {
					nPos = (nPos & 0xff00) + 0x0100;
					continue;
				}
				nLineNo = Utilities.getInt16(content, nPos+1);
				if (nLineNo != 0xffff) {
					byte[] abyLine = new byte[nLineLen-3];
//					System.out.println("line = " + nLineNo + ", nPos = " + nPos  + ", len = " + (nLineLen-3));
					System.arraycopy(content, nPos + 3, abyLine, 0, nLineLen-3);
					basicprog.add(new BasicLine(nLineNo, abyLine));
					nPos = nPos + nLineLen + 1;
				}
			}
		}

		try {
			if (hasBasicLongFormat()) {
				// read all records and create a new byte array which matches 
				// the PROGRAM format			
				boolean bStillRecords = true;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				// Write header
				// Check = cont[07] cont[08]  -> cont[00] cont[01] 
				// endlt = cont[05] cont[06]  -> cont[02] cont[03]
				// startlt = cont[03] cont[04]  -> cont[04] cont[05]
				// memtop = cont[09] cont[0a]  -> cont[06] cont[07]
				baos.write(content[7]);
				baos.write(content[8]);
				baos.write(content[5]);
				baos.write(content[6]);
				baos.write(content[3]);
				baos.write(content[4]);
				baos.write(content[9]);
				baos.write(content[10]);
				int nPos = 0x100;
				int nRec = 1;
				while (nRec < m_nNumberOfRecords) {
					int nLength = content[nPos++]&0xff;
					// System.out.println("Length = " + nLength);
					// System.out.println(Utilities.hexdump(0, nPos,content, nLength + 1, false)); 
					for (int i=0; i < nLength; i++) baos.write(content[nPos++]);
					if (nLength!=0 && (content[nPos]&0xff) != 0xff) {
						throw new ImageException("Missing record end in record " + nRec);
					}
					nRec++;
					nPos++;
				}
				content = baos.toByteArray();
			}
		}
		catch (ArrayIndexOutOfBoundsException ax) {
			throw new ImageException("File cannot be listed as BASIC, possibly corrupted, or unsupported BASIC version.");
		}
		
		try {
			if (hasBasicLongFormat() || isProgram()) {
				// Create Line number table
				int nEndLNT = Utilities.getInt16(content, 2);
				int nStartLNT = Utilities.getInt16(content, 4);
				int nMemTop = Utilities.getInt16(content, 6);
				
				boolean bNeedSpace = false;
				
				int nPos = 8;
				int nStart = nStartLNT - nPos;
				//		System.out.println("nPos = " + nPos + ", len = " + toHex(content.length,4) + ", nStartLNT = " + toHex(nStartLNT,4));
				while (nPos < nEndLNT - nStart) {
					nLineNo = Utilities.getInt16(content, nPos);
					// Address must be decreased by one, as the pointer points to the
					// first byte after the length byte 
					int nAddress = Utilities.getInt16(content, nPos+2);
					//			if (nAddress-nStart < 0) {
					//				System.err.println("fehler: nstart=" +toHex(nStart,4) + ", nAdd=" + toHex(nAddress,4) + ", line = " + nLineNo); 
					//			}
					//			else System.err.println("ok: nstart=" +toHex(nStart,4) + ", nAdd=" + toHex(nAddress,4)+ ", line = " + nLineNo);
					int nLineLen = (content[nAddress - nStart - 1] - 1)& 0xff;
					byte[] abyLine = new byte[nLineLen];
					System.arraycopy(content, nAddress - nStart, abyLine, 0, nLineLen);
					basicprog.add(0, new BasicLine(nLineNo, abyLine));
					nPos += 4;
				}
			}
		}
		catch (ArrayIndexOutOfBoundsException ax) {
			throw new ImageException("File cannot be listed as BASIC, possibly corrupted.");
		}

		StringBuilder sb = new StringBuilder();
				
		for (BasicLine line:basicprog) {
			sb.append(line.list(version)).append("\n");
		}		
		
		return sb.toString();
	}
	
	/** Check for Util format. We need the vol reference because we need to look at the contents. */
	public boolean hasUtilFormat() throws IOException, FormatException, ImageException {
		if (!isProgram()) return false;
		byte[] content = getRawContent();
		int nLengthHead = Utilities.getInt16(content, 2);
		return ((content.length==nLengthHead || content.length==nLengthHead+6) && 
			(content[0] == (byte)0xff || content[0] == (byte)0x00) && 
			(content[1] == (byte)0xff || content[1] == (byte)0x00 || content[1] == (byte)0x09 || content[1] == (byte)0x0a));
	}
	
	/** Check for Archive format. */
	public boolean hasArchiveFormat() {
		try {
			if (!hasFixedRecordLength() || getRecordLength()!=128 || isProgram()) return false;
			byte[] content = getRawContent();

			if (Archive.hasPlainArchiveFormat(content)) return true;
			
			if (!isDisplay() && content.length>0) {
				if (content[0] == (byte)0x80) return true;
			}
			return false;
		}
		catch (ArrayIndexOutOfBoundsException ax) {
			ax.printStackTrace();
			System.err.println("ArrayIndexOutOfBoundsException for " + getName());
			return false;
		}
		catch (IOException iox) {
			return false;
		}
		catch (ImageException ix) {
			return false;
		}
	}
	
	public Archive unpackArchive() throws IllegalOperationException, FormatException, IOException, ImageException {
		if (!hasArchiveFormat()) throw new IllegalOperationException("Not an archive");
		boolean bCompressed = false;
		
		byte[] content = getRawContent();
		try {
			if (!isDisplay()) {
				LZW lzw = new LZW(content);
				content = lzw.uncompress();
				bCompressed = true;
			}
		}
		catch (ArrayIndexOutOfBoundsException ax) {
			throw new ImageException("Broken archive");
		}
		return new Archive(getVolume(), m_sName, getContainingDirectory(), content, this, bCompressed);
	}
}
