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

import java.io.*;
import java.util.*;
import de.mizapf.timt.util.TIFiles;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;

public class Volume {

	public final static int ROOT_SECTOR = 0;
	public final static int SECTOR_LENGTH=0x100;   

	public final static int MAXAU = 0xf800;

	public final static int SCSI = 1;
	public final static int HFDC = 2;
	public final static int FLOPPY = 3;
	public final static int CF7 = 4;
	
	public final static String PARENTDIR = "..";
	
	// Image format
	ImageFormat m_Image;

	long		m_nLastMod;
	
	// Information from the VIB
	String 		m_sVolumeName;
	int 		m_nType = FLOPPY;
	int 		m_nTotalSectors = 0;
	int 		m_nSectorsPerTrack = 0;
	AllocationMap m_allocMap;
	Directory 	m_dirRoot = null;
	int 		m_nHeads = 0;

	// HD-specific
	int 		m_nReservedAUs = 0;
	int 		m_nStepSpeed = 0;
	int 		m_nReducedWriteCurrent = 0;
	int 		m_nSectorsPerAU = 0;
	boolean 	m_bBufferedStep = false;
	int 		m_nWritePrecomp;
	int			m_nAUEmulate;
	Time		m_tCreation;
	
	// Floppy-specific
	int 		m_nTracksPerSide = 0;
	int 		m_nDensity = 0;
	boolean		m_bProtection = false;
	
	boolean		m_cf7Inconsistency;
	
	private String m_sImageFileName;
	
	public Volume(String sFile, boolean bCheck) throws FileNotFoundException, IOException, ImageException {

		Sector sector0 = null;
		byte[] abySect0 = null;
		int number = -1;
		m_cf7Inconsistency = false;
		
		// Check whether we have a number at the end of the name
		int volnumpos = sFile.lastIndexOf("#");
		if (volnumpos > 0 && volnumpos < sFile.length()-1) {
			try {
				number = Integer.parseInt(sFile.substring(volnumpos+1));
				sFile = sFile.substring(0, volnumpos);
			}
			catch (NumberFormatException nfx) {
				// Did not work, so what. 
			}
		}
				
		// Get the image format
		if (number > -1) {
			// We have a CF7
			ImageFormat format = ImageFormat.getImageFormat(sFile);
			m_Image = ((CF7ImageFormat)format).getSubvolume(number);
		}
		else {
			m_Image = ImageFormat.getImageFormat(sFile);
		}
		
		m_nLastMod = m_Image.getLastModifiedTime();
		
		m_sImageFileName = sFile;
		
		sector0 = readSector(0);
		abySect0 = sector0.getBytes();

		if (m_Image instanceof SectorDumpFormat || m_Image instanceof TrackDumpFormat || m_Image instanceof HFEFormat) {
			m_nType = FLOPPY;
			if (!hasFloppyVib(abySect0) && bCheck) throw new MissingHeaderException();  

			// TODO: Check with image
			m_nTotalSectors = Utilities.getInt16(abySect0, 0x0a);
			
			m_nHeads = abySect0[0x12] & 0xff;
			m_nTracksPerSide = abySect0[0x11] & 0xff;
			m_nDensity = abySect0[0x13] & 0xff;		

			if (m_Image instanceof CF7VolumeFormat) {
				m_nType = CF7;
				// There may be inconsistencies with CF7 volumes.
				// TODO: This should be checked; maybe offer to fix the volume?
			}
		
			m_nSectorsPerAU = (int)(m_nTotalSectors/1601) + 1;
			m_bProtection = (abySect0[0x10]=='P');
			m_nReservedAUs = 0x21;
			
			m_allocMap = new AllocationMap(m_nTotalSectors / m_nSectorsPerAU, m_nSectorsPerAU, true);
			m_allocMap.setMapFromBitfield(abySect0, 0x38, 0);
			
			if (m_nDensity != m_Image.getDensity()) {
				System.err.println(String.format(TIImageTool.langstr("VolumeDensityMismatch"), m_nDensity, m_Image.getDensity())); 
			}
		}
		else {
			if ((abySect0[0x10] & 0x0f)==0) m_nType = SCSI;
			else m_nType = HFDC;
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
			
			// Create allocation map
			m_allocMap = new AllocationMap(nTotalAU, m_nSectorsPerAU, false);
			// Sectors 1-31 constitute the AM
			for (int i=1; i <= 1 + nTotalAU/2048; i++) {
				byte[] abySect = readSector(i).getBytes();
				m_allocMap.setMapFromBitfield(abySect, 0, (i-1)*2048);
			}			
		}
		m_nSectorsPerTrack = abySect0[0x0c] & 0xff;
		
		m_sVolumeName = Utilities.getString10(abySect0, 0);
		
		if (m_nType==FLOPPY || m_nType==CF7)
			m_dirRoot = new Directory(this, sector0);  // used for floppy
		else 
			m_dirRoot = new Directory(this, sector0, null);
	}
	
	public Volume(String sFile) throws FileNotFoundException, IOException, ImageException {
		this(sFile, true);
	}
	
	public Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		return m_Image.readSector(nSectorNumber);
	}
	
	public void writeSector(int nNumber, byte[] abySector) throws ProtectedException, IOException, ImageException {
		if (isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));
//		long time = m_Image.getLastModifiedTime();
		// System.out.println("time = " + time + ", last mod = " + m_nLastMod);
//		if (m_nLastMod < time) throw new ProtectedException("Volume has changed on disk; cannot write. Image will be closed.");
		m_Image.writeSector(nNumber, abySector);
//		m_nLastMod = m_Image.getLastModifiedTime();
	}
	
	public void setGeometry(int total, int tracks, int heads, int sectors, int density) {
		m_nTotalSectors = total;
		m_nTracksPerSide = tracks;
		m_nHeads = heads;
		m_nSectorsPerTrack = sectors;
		m_nDensity = density;
	}
	
	public boolean hasCf7Inconsistency() {
		return m_cf7Inconsistency;
	}
	
	public void reopenForWrite() throws IOException {
		m_Image.reopenForWrite();
	}
	
	public void reopenForRead() throws IOException {
		m_Image.reopenForRead();
	}

	public void close() throws IOException {
		m_Image.close();
	}
	
	public boolean equals(Object other) {
		if (other instanceof Volume) {
			return getImageName().equals(((Volume)other).getImageName());
		}
		return false;
	}
	
	public String getImageName() {
		return m_sImageFileName;
	}
	
	public String getShortImageName() {
		return m_sImageFileName.substring(m_sImageFileName.lastIndexOf(java.io.File.separator)+java.io.File.separator.length());
	}
		
	public static boolean hasFloppyVib(byte[] abySect) {
		return (abySect[13]=='D' && abySect[14]=='S' && abySect[15]=='K');	
	}

	public static boolean hasSCSIVib(byte[] abySect) {
		return (abySect[14]==(byte)0x00 && abySect[15]==(byte)0x00 && abySect[17]==(byte)0x00); 
	}	

	public int getAUSize() {
		return m_nSectorsPerAU;
	}

	public int getReservedAUs() {
		return m_nReservedAUs;
	}
		
	public int getTotalAUs() {
		return m_nTotalSectors / m_nSectorsPerAU;
	}
	
	public boolean isProtected() {
		return m_bProtection;
	}
	
	public int toAU(int nSectorNumber) {
		return nSectorNumber / m_nSectorsPerAU;
	}

	public void saveAllocationMap() throws IOException, ImageException, ProtectedException {
		if (m_nType==FLOPPY || m_nType==CF7) {
			// read sector 0 and paste map into locations
			byte[] abySect0 = readSector(0).getBytes();
			byte[] bitmap = m_allocMap.toBitField();
			System.arraycopy(bitmap, 0, abySect0, 0x38, bitmap.length);
			writeSector(0, abySect0);
		}
		else {
			// create new contents for sectors 1-31
			int nTotalAU = m_nTotalSectors / m_nSectorsPerAU;
			byte[] bitmap = m_allocMap.toBitField();
			byte[] sector = new byte[SECTOR_LENGTH];
			for (int i=1; i <= 1 + nTotalAU/2048; i++) {
				int nLength = SECTOR_LENGTH;
				if ((i-1)*256 + nLength > bitmap.length) {
					nLength = bitmap.length - (i-1)*256;
				}
				System.arraycopy(bitmap, (i-1)*256, sector, 0, nLength);
				writeSector(i, sector);
			}
		}
	}
	
	public boolean isFloppyImage() {
		return m_nType == FLOPPY;
	}
	
	public boolean isSCSIImage() {
		return m_nType == SCSI;
	}
	
	public boolean isHFDCImage() {
		return m_nType == HFDC;
	}
	
	public boolean isCF7Volume() {
		return m_nType == CF7;
	}

	public String getName() {
		return m_sVolumeName;
	}
	
	public String getDeviceName() {
		if (isFloppyImage() || isCF7Volume()) return "DSK1";
		else {
			if (isSCSIImage()) return "SCS1";
			else return "HDS1";
		}
	}
	
	public Directory getRootDirectory() {
		return m_dirRoot;
	}

	public TFile getFileByPath(String sArgument) throws FileNotFoundException {
		// We assume that the path is built by path.path.path.file
		// "Dot" is difficult to specify.
		String[] asArg = sArgument.split("\\x2e");
		StringBuffer sbDir = new StringBuffer();

		// We need to descent to the given directory
		Directory dirCurrent = getRootDirectory();
		for (int i=0; i < asArg.length-1; i++) {
			Directory[] aDir = dirCurrent.getDirectories();
			boolean bFound = false;
			for (int j=0; j < aDir.length; j++) {
				if (aDir[j].getName().equals(asArg[i])) {
					dirCurrent = aDir[j];
					bFound = true;
					break;
				}
			}
			if (!bFound) throw new FileNotFoundException(String.format(TIImageTool.langstr("VolumeDirNotFound"), asArg[i]));
		}
		String sFile = asArg[asArg.length-1];

		TFile[] aFile = dirCurrent.getFiles();

		for (int i=0; i < aFile.length; i++) {
			if (aFile[i].getName().equals(sFile)) return aFile[i];
		}
		throw new FileNotFoundException(String.format(TIImageTool.langstr("VolumeFileNotFound"), sFile));
	}
		
	// -----------------------------------------------------

	/** Returns a sequence of intervals of AUs which can hold a file of that
		size. */
	public Interval[] findFreeSpace(int nSectors, int nStarting) {
		List<Interval> intList = new LinkedList<Interval>();
		AllocationMap allocMap = (AllocationMap)m_allocMap.clone();
		Interval intnew;
		
		int nStartSector = 0;
//		System.out.println("find free space for " + nSectors + " sectors, starting from sector " + nStarting);

		if (getAUSize()>1) {
			if ((nSectors % getAUSize())!=0) nSectors = ((nSectors/getAUSize())+1)*getAUSize();
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

	public int getSystemAllocatedSectors() {
		if (isFloppyImage()) return 2;
		else return 0;
	}
	
	public int getAllRequiredSectors(int nAUSize) {
		// Sector 0 is already used for the root directory
		// Allocation map is included in sector 0 for floppies
		if (isFloppyImage() || isCF7Volume()) return 0;
		
		int nAllocMapSectors = ((m_allocMap.getMaxAU()/8)-1) / SECTOR_LENGTH + 1; 
		
		// Round up to AU size
		// Starts with sector 1, so we must ignore the first AU
		// sector 1 .. ausize-1
		
		nAllocMapSectors = nAllocMapSectors - (nAUSize-1);
		
		int nAllocMapAU = ((nAllocMapSectors-1) / nAUSize) + 1;
		return nAllocMapAU;
	}

	public int getAllocatedSectorCount() {
		return m_allocMap.countAllocated() * getAUSize();
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
	
	/** Also called by TIImageTool.
	*/
	public int getTotalSectors() {
		return m_nTotalSectors;
	}
	
	public int getTracksPerSide() {
		return m_nTracksPerSide;
	}
	
	public int getAUEmulateSector() {
		return m_nAUEmulate * m_nSectorsPerAU;
	}
	
	public void toggleEmulateFlag(int nSector) throws IOException, ImageException, ProtectedException {
		if (getAUEmulateSector()==nSector) m_nAUEmulate = 0;
		else m_nAUEmulate = nSector / m_nSectorsPerAU;
		update();
	}
	
	public boolean isCHDImage() {
		return m_Image instanceof MameCHDFormat;
	}
	
	public String dumpFormat() {
		return m_Image.getDumpFormatName();
	}
	
	public String getFloppyFormat() {
		StringBuilder sb = new StringBuilder();
		sb.append((m_nHeads==2)? "DS" : "SS");
		switch (m_nDensity) {
		case 0:
		case 1:
			sb.append("S"); break;
		case 2: 
			sb.append("D"); break;
		case 3:
			sb.append("H"); break;
		case 4:
			sb.append("U"); break;
		default:
			return TIImageTool.langstr("Invalid");
		}		
		sb.append("D");
		return sb.toString();
	}
	
	public void convert(int sectors, int speed, int current, int heads, boolean buff, int precomp) {
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
	
	public void setType(int nType) {
		m_nType = nType;
	}
	
	public void renameVolume(String newName) throws IOException, ImageException, ProtectedException, InvalidNameException {
		if (newName == null || newName.length()==0 || newName.length()>10) throw new InvalidNameException(TIImageTool.langstr("VolumeNameConstr"));
		if (newName.indexOf(".")!=-1) throw new InvalidNameException(TIImageTool.langstr("VolumeNamePeriod"));
	
		m_sVolumeName = newName;
		reopenForWrite();
		update();
		reopenForRead();
	}
	
	public byte[] createVIB() throws IOException, ImageException, ProtectedException {
		// Create a new VIB
		byte[] abyNewVIB = new byte[256];

		Utilities.setString(abyNewVIB, 0, getName(), 10);
		
		if (m_nType==FLOPPY || m_nType==CF7) {
			Utilities.setInt16(abyNewVIB, 0x0a, m_nTotalSectors);
			abyNewVIB[0x0c] = (byte)(m_nSectorsPerTrack & 0xff);
			Utilities.setString(abyNewVIB, 0x0d, "DSK", 3);
			abyNewVIB[0x10] = m_bProtection? (byte)'P' : (byte)' ';
			abyNewVIB[0x11] = (byte)(m_nTracksPerSide & 0xff);
			abyNewVIB[0x12] = (byte)(m_nHeads & 0xff);
			abyNewVIB[0x13] = (byte)(m_nDensity & 0xff);

			if (m_nType==FLOPPY) {
				Directory[] dirs = m_dirRoot.getDirectories();
				for (int i=0; i < 3; i++) {
					if (i < dirs.length) {
						Directory sub = dirs[i];
						Utilities.setString(abyNewVIB, 0x14 + i*12, sub.getName(), 10);
						Utilities.setInt16(abyNewVIB, 0x1e + i*12, sub.getFdrSector()); 					
					}
					else {
						for (int j=0; j < 12; j++) abyNewVIB[0x14 + j + i*12] = (byte)0;
					}
				}
			}
			else {
				// Clear the DIR area for CF7
				for (int i=0x14; i<0x38; i++) abyNewVIB[i] = (byte)0x00;
			}
			byte[] map = m_allocMap.toBitField();
			for (int j=0; j < map.length; j++) {
				abyNewVIB[j+0x38] = map[j];
			}
			// Fill the rest with ff (required by TIFDC and BWG)
			for (int j=map.length; j < (256-0x38); j++) {
				abyNewVIB[j+0x38] = (byte)0xff;
			}
		}
		else {
			Utilities.setInt16(abyNewVIB, 0x0a, m_nTotalSectors/m_nSectorsPerAU);
			abyNewVIB[0x0d] = (byte)((m_nReservedAUs>>6) & 0xff);
			Utilities.setTime(abyNewVIB, 0x12, m_tCreation);
			abyNewVIB[0x16] = (byte)(m_dirRoot.getFiles().length & 0xff);
			abyNewVIB[0x17] = (byte)(m_dirRoot.getDirectories().length & 0xff);
			Utilities.setInt16(abyNewVIB, 0x18, toAU(m_dirRoot.getFdrSector()));
			
			if (m_nType==HFDC) {
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
		}
		return abyNewVIB;
	}
	
	public void update() throws IOException, ImageException, ProtectedException {
		// Write the allocation map and the VIB
		byte[] abyVIB = createVIB();
		writeSector(0, abyVIB);
		if (m_nType!=FLOPPY && m_nType!=CF7) {
			saveAllocationMap();
		}		
	}
	
/*************************** Low-level routines *****************************/
	
	public static void createFloppyImage(File newImageFile, String volumeName, int type, int sides, int density, int tracks, boolean format) throws IOException, ImageException {

		ImageFormat image = null;
		
		int sectorsPerTrack = 9 << density;

		switch (type) {
		case ImageFormat.SECTORDUMP:
			image = new SectorDumpFormat();
			break;
		case ImageFormat.TRACKDUMP:
			image = new TrackDumpFormat();
			break;
		case ImageFormat.HFE:
			image = new HFEFormat();
			break;
		case ImageFormat.CF7VOLUME:
			image = new CF7VolumeFormat();
			sectorsPerTrack = 20;
			break;
		}
		
		image.createEmptyImage(newImageFile, sides, density, tracks, sectorsPerTrack, format);		
		
		if (format) {
			
			// Load it and write sectors 0 and 1
			image = ImageFormat.getImageFormat(newImageFile.getAbsolutePath());
			
			// Sector 0
			byte[] sector0 = new byte[SECTOR_LENGTH];
			
			Arrays.fill(sector0, 0, 10, (byte)' ');
			System.arraycopy(volumeName.getBytes(), 0, sector0, 0, volumeName.getBytes().length);
			
			int nsectors = sides * tracks * sectorsPerTrack;
			sector0[10] = (byte)(nsectors >> 8);
			sector0[11] = (byte)(nsectors % 256);
			sector0[12] = (byte)sectorsPerTrack;
			sector0[13] = 'D';
			sector0[14] = 'S';
			sector0[15] = 'K';
			sector0[16] = (byte)0x20;
			sector0[17] = (byte)tracks;
			sector0[18] = (byte)sides;
			sector0[19] = (byte)(density+1);
			for (int i=0x14; i < 0x38; i++) sector0[i] = (byte)0;
			for (int i=0x38; i < 0x100; i++) sector0[i] = (byte)0xff;
			
			// Allocation bitmap
			AllocationMap am = new AllocationMap(nsectors);
			am.allocate(0);
			if (am.getAUSize()==1) am.allocate(1);
			
			byte[] abyMap = am.toBitField();
			System.arraycopy(abyMap, 0, sector0, 0x38, abyMap.length);
			
			// Sector 1
			byte[] sector1 = new byte[SECTOR_LENGTH];
			Arrays.fill(sector1, 0, SECTOR_LENGTH, (byte)0x00);
			
			image.writeSector(0, sector0);
			image.writeSector(1, sector1);
			image.close();
		}
	}
	
	/** Check CRC errors in Track Dump Format. */
	public int checkCRC(boolean fix, boolean reset) throws IOException
	{
		return m_Image.checkCRC(fix, reset);
	}
	
	public void scsi2hfdc(int sectors, int speed, int current, int heads, int buff, int precomp) throws IOException, ImageException, ProtectedException {
//		Directory root = image.getRootDirectory();
		// Correct invalid MaxAU entries
//		int nChecked = checkDIB(root, true);
//		if (nChecked < 0) nChecked = -nChecked;
//		System.out.println("Checked " + nChecked + " directories");
		
		// Set disk parameters
		// Need to know
		//	 Sectors per track
		//	 Number of heads
		convert(sectors, speed, current, heads, buff!=0, precomp);
		setType(HFDC);
		reopenForWrite();
		byte[] abyVIB = createVIB();
		writeSector(0, abyVIB);
		reopenForRead();
	}
	
	public void hfdc2scsi() throws IOException, ImageException, ProtectedException {
		// Set disk parameters
		// Need to know
		//	 Sectors per track
		//	 Number of heads
		convert(0, 0, 0, 1, false, 0);
		setType(SCSI);
		reopenForWrite();
		byte[] abyVIB = createVIB();
		writeSector(0, abyVIB);
		reopenForRead();
	}
}
