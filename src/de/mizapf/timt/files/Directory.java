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
	along with TIImageTool.	 If not, see <http://www.gnu.org/licenses/>.
	
	Copyright 2011 Michael Zapf
	www.mizapf.de
	
****************************************************************************/

package de.mizapf.timt.files;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.*;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.util.TIFiles;
import de.mizapf.timt.util.InternalException;

import de.mizapf.timt.TIImageTool;

public class Directory extends Element {
	
	public final static String PARENTDIR = "..";
	
	public static Directory parent = new Directory(PARENTDIR);
	
	Directory[] m_Subdirs;
	TFile[]		m_Files;

	Volume		m_Volume;
	
	/** Needed for checkDIB. Some SCSI images may have bad AU counts. */ 
	boolean m_bBadAUCount;
		
	// ==========================================================
	// Data from the DDR
	
	/** Location of file index. */
	int 	m_nFileIndexSector;

	/** Location of the directory descriptor of this directory. Always 0
	    for floppy and CF7. */
	int		m_nDDRSector;
	
	// ==========================================================
	private final static int NAMELEN = 15;
	private final static int SIZELEN = 5;
	private final static int TYPELEN = 10;
	private final static int RECLEN = 8;
	private final static int PROLEN = 3;
	private final static int CRELEN = 20;
	private final static int UPDLEN = 18;

	/** Called by recursion and by Volume.const#HD. */
	Directory(Volume vol, Sector vibddr, Directory dirParent) throws IOException, ImageException {
		m_Volume = vol;
		// System.out.println(Utilities.hexdump(vibddr.getData()));
		TreeSet<TFile> files = new TreeSet<TFile>();
		TreeSet<Directory> subdirs = new TreeSet<Directory>();

		if (dirParent != null) m_sName = Utilities.getString10(vibddr.getData(), 0);
		m_nFileIndexSector = Utilities.getInt16(vibddr.getData(), 0x18) * vol.getAUSize();
		if (m_nFileIndexSector == 0) throw new ImageException(TIImageTool.langstr("ImageUnknown"));
		
		m_nDDRSector = vibddr.getNumber();
		m_tCreation = new Time(vibddr.getData(), 0x12);
		m_dirParent = dirParent;
		setContainingDirectory(dirParent);
		// Create files
		Sector sectFiles = vol.readSector(m_nFileIndexSector);
		int bad = 0;
		// System.out.println("Directory " + getFullPathname());
		// if (m_dirParent == null) System.out.println("> " + System.currentTimeMillis());
		for (int nFile : getFilePointers(sectFiles, vol.getAUSize())) {
			try {
				Sector sectFile = vol.readSector(nFile);
				TFile file = new TFile(vol, sectFile, this);
				// if (file.getAllocatedSectors()==0) System.err.println("Warning: File " + file.getPathname() + " has no contents");
				files.add(file);
				// System.out.println("File " + file.getName());
			}
			catch (ImageException ix) {
				bad++;
				System.err.println(ix.getMessage());
				if (bad > 10) {
					String name = m_sName;
					if (dirParent == null)
						throw new ImageException(TIImageTool.langstr("DirectoryFailedRoot"));
					else 
						throw new ImageException(String.format(TIImageTool.langstr("DirectoryFailedOther"), name));
				}
			}
		}
		
		int fcount = vibddr.getData()[0x16] & 0xff;
		if (files.size() != fcount)
			System.out.println("Warning (" + m_sName + "): File count = " + fcount + ", but found " + files.size() + " files"); 
		
		// if (m_dirParent == null) System.out.println("| " + System.currentTimeMillis());
		// Create directories
		for (int nDir : getDirPointers(vibddr, vol.getAUSize())) {
			// System.out.println("- " + System.currentTimeMillis());
			Sector ddr = vol.readSector(nDir);
			// Recurse
			subdirs.add(new Directory(vol, ddr, this));
		}
		int nMaxAU = Utilities.getInt16(vibddr.getData(), 0x0a);
		m_bBadAUCount = (nMaxAU > TFileSystem.MAXAU);
		
		m_Subdirs = new Directory[subdirs.size()];
		subdirs.toArray(m_Subdirs);

		int dcount = vibddr.getData()[0x17] & 0xff;
		if (subdirs.size() != dcount)
			System.out.println("Warning (" + m_sName + "): Dir count = " + dcount + ", but found " + subdirs.size() + " subdirs"); 

		m_Files = new TFile[files.size()];
		files.toArray(m_Files);
		// if (m_dirParent == null) System.out.println("< " + System.currentTimeMillis());
	}

	/** Creates a parent directory link (".."). */
	public Directory(String name) {
		m_sName = name;
	}
	
	public boolean isParentLink() {
		return m_sName.equals(PARENTDIR);
	}
	
	/** Builds a new floppy root directory. Called from Volume.const.
		@param vol Volume where this directory is located
		@param sect Sector which contains the information of the root directory (usually 0)	
	*/
	Directory(Volume vol, Sector sect) throws IOException, ImageException {
		m_Volume = vol;
		m_dirParent = null;

		TreeSet<TFile> files = new TreeSet<TFile>();
		TreeSet<Directory> subdirs = new TreeSet<Directory>();
		setContainingDirectory(null);
		
		// Floppy root directory
		m_nFileIndexSector = 1;
		
		for (int nDir = 0; nDir < 3; nDir++) {
			if (Utilities.getInt16(sect.getData(), nDir*0x0c + 0x1e)!=0) {
				subdirs.add(new Directory(vol, sect, this, nDir));
			}
		}
		// Create files
		int bad = 0;
		Sector sectFiles = vol.readSector(m_nFileIndexSector);
		for (int nFile : getFilePointers(sectFiles, 1)) {
			// FDIR in floppies always uses sector numbers
			Sector sectFile = vol.readSector(nFile);
			try {
				TFile file = new TFile(vol, sectFile, this);
				files.add(file);
			}
			catch (ImageException ix) {
				bad++;
				System.err.println(ix.getMessage());
				if (bad > 10) {
					throw new ImageException(TIImageTool.langstr("DirectoryDamaged"));
				}
			}	
		}
		m_Subdirs = new Directory[subdirs.size()];
		subdirs.toArray(m_Subdirs);
		
		m_Files = new TFile[files.size()];
		files.toArray(m_Files);		
	}

	/** Builds a new empty harddisk root directory. Called from Volume.const.
		@param vol Volume where this directory is located
		@param inx Sector number of FDIR
	*/
	Directory(Volume vol, int idx) {
		m_Volume = vol;
		m_dirParent = null;
		setContainingDirectory(null);
		
		// Floppy root directory
		m_nFileIndexSector = idx;
		
		m_Subdirs = new Directory[0];
		m_Files = new TFile[0];
	}
	
	/** Builds a new floppy subdirectory. Called from Directory.const.
		@param vol Volume which contains this directory
		@param sect Sector where this directory is specified (always 0 for floppy)
		@param dirParent Parent directory (always root)
		@param nDirIndex Number of directory (0-3)
	*/
	private Directory(Volume vol, Sector sect, Directory dirParent, int nDirIndex) throws IOException, ImageException {
		m_Volume = vol;
		TreeSet<TFile> files = new TreeSet<TFile>();

		m_tCreation = new Time();
		m_nFileIndexSector = Utilities.getInt16(sect.getData(), nDirIndex*0x0c + 0x1e);
		m_sName = Utilities.getString10(sect.getData(), nDirIndex*0x0c + 0x14);
		m_dirParent = dirParent;
		m_nDDRSector = 0;
		setContainingDirectory(dirParent);

		// Create files
		Sector sectFiles = vol.readSector(m_nFileIndexSector);
		for (int nFile : getFilePointers(sectFiles, 1)) {
			// FDIR in floppies always uses sector numbers
			Sector sectFile = vol.readSector(nFile);
			TFile file = new TFile(vol, sectFile, this);
			files.add(file);
		}
		m_bBadAUCount = false;

		m_Subdirs = new Directory[0];
		
		m_Files = new TFile[files.size()];
		files.toArray(m_Files);		
	}
	
	/** Creates a new blank floppy subdirectory. Called from createSubdirecory. */
	private Directory(Volume vol, String sName, int nFDIRSector, Directory dirParent) {
		this(vol, sName, 0, nFDIRSector, dirParent);
		m_tCreation = new Time(); // cannot save time, so just don't show it at all
	}

	/** Creates a new blank HD subdirectory. Called from createSubdirecory. */
	private Directory(Volume vol, String sName, int nDDRSector, int nFDIRSector, Directory dirParent) {
		m_Volume = vol;
		m_Files = new TFile[0];
		m_Subdirs = new Directory[0];
		m_tCreation = Time.createNow();
		m_nFileIndexSector = nFDIRSector;
		m_sName = sName;
		m_dirParent = dirParent;
		m_nDDRSector = nDDRSector;
		m_bBadAUCount = false;
		// No subdirectories		
	}
	
	/** Constructor for an empty directory. Required by Archive.const. */
	protected Directory() {
	}
		
	/** If negative, errors were found. Called from OpenImageAction#SCSI */
/*	public static int checkDIB(Directory dir, boolean bWrite) throws IOException, ImageException, ProtectedException  {
		boolean bFound = false;
		int nCount = 1;
		if (dir.hasInvalidAUCount()) {
//			System.err.println("Warning: Directory Information Block of directory " + dir.getName() 
//				+ " contains an invalid MAXAU value (larger than the maximum). Setting the value to the maximum value: " + Volume.MAXAU);
			bFound = true;
			if (bWrite) {
				// Fix the DDR
				dir.m_bBadAUCount = false;
				dir.writeDDR();
//				System.out.println("Fixed directory");
			}
		}
		Directory[] adir = dir.getDirectories();
		for (int i=0; i < adir.length; i++) nCount += checkDIB(adir[i], bWrite);
		if (bFound) nCount = -nCount;
		return nCount;
	}
	*/
	public static boolean validName(String sName) {
		if (sName==null) return false;
		if (sName.length()>10) return false;
		if (sName.indexOf(".")!=-1) return false;
		
		byte[] abyName = sName.getBytes();
		for (int i=0; i < abyName.length; i++) {
			if (abyName[i] < 0x20 || abyName[i] > 0x7f) return false;
		}
		return true;
	}
	
	public Directory getParentDirectory() {
		if (m_dirParent == null) {
			return this;
		}
		else return m_dirParent;
	}
		
	public Directory getSubdirectory(String sSubdir) throws FileNotFoundException {
		for (Directory dir : m_Subdirs) {
			if (dir.getName().equals(sSubdir)) return dir; 
		}
		throw new FileNotFoundException(sSubdir);
	}
	
	public boolean hasSubdirectory(String sSubdir) {
		if (sSubdir.equals(PARENTDIR) && m_dirParent != null) return true;
		for (Directory dir : m_Subdirs) {
			if (dir.getName().equals(sSubdir)) return true; 
		}
		return false;
	}
	
	/** Called from constructors. */
	private static int[] getFilePointers(Sector fdir, int nAUSize) {
		int[] an = new int[127];
		int i = 0;
		for (i=0; i < an.length; i++) {
			an[i] = Utilities.getInt16(fdir.getData(), 2*i) * nAUSize;
			if (an[i] == 0) break;
		}
		int[] anRet = new int[i];
		System.arraycopy(an, 0, anRet, 0, i);
		return anRet;
	}
	
	/** Called from constructor. */
	private static int[] getDirPointers(Sector sect, int nAUSize) {
		int[] an = new int[114];
		int i = 0;
		for (i=0; i < an.length; i++) {
			an[i] = Utilities.getInt16(sect.getData(), 0x1c + 2*i) * nAUSize;
			if (an[i] == 0) break;
		}
		int[] anRet = new int[i];
		System.arraycopy(an, 0, anRet, 0, i);
		return anRet;		
	}
	
	public int getDDRSector() {	// needed by ImageCheck, also by insertFile
		return m_nDDRSector;
	}

	@Override
	public Volume getVolume() {
		return m_Volume;
	}
		
	public int getAllRequiredSectors(int nAUSize) {
		int nReqAU = 1;
		// if this is the root directory of the floppy, the file index is in
		// *sector* 1 (not AU). If the AU size is bigger, no additional sectors
		// are used as it is contained in the AU of sector 0.

		if (m_Volume.isFloppyImage() || m_Volume.isCF7Volume()) {
			if (isRootDirectory() && nAUSize==1) nReqAU = 2;
			// not root -> no separate DDR
			// root but AUsize > 1 -> included in AU 0 
		}
		else {
			// root -> DDR included in VIB
			if (!isRootDirectory()) nReqAU = 2;
		}
		return nReqAU * nAUSize;
	}

	public boolean isRootDirectory() {
		return m_dirParent == null;
	}
	
	public int getFileIndexSector() {
		return m_nFileIndexSector;
	}
	
	public boolean hasInvalidAUCount() {
		return m_bBadAUCount;
	}
	
	public Directory[] getDirectories() {
		return m_Subdirs;
	}
	
	public Directory enterDirectory(String sDir) throws ImageException {
		for (Directory d : m_Subdirs) {
			if (d.getName().equals(sDir)) return d;
		}
		throw new ImageException(String.format(TIImageTool.langstr("VolumeDirNotFound"), sDir));
	}
	
	public TFile getFile(String sFile) throws FileNotFoundException {
		for (TFile file : m_Files) {
			if (file.getName().equals(sFile)) return file; 
		}
		throw new FileNotFoundException(sFile);
	}
	
	public TFile[] getFiles() {
		return m_Files;
	}
	
	public String getFullPathname() {
		if (isRootDirectory()) return "";
		return m_dirParent.getFullPathname() + "." + getName();
	}
	
	public boolean equals(Object other) {
		if (other == null) return false;
		if (!(other instanceof Directory)) return false;
		
		Directory dother = (Directory)other;
		return (getFullPathname().equals(dother.getFullPathname()) && m_Volume.getImageName().equals(dother.getVolume().getImageName())); 
	}
	
	// File Size Type Length Protection Created Updated
	public String toFormattedString() {
		String sPattern = "%1$-10s %2$4d %3$-8s %4$6d    %5$-20s";
		return String.format(sPattern, getName(), m_Volume.getAUSize(), "Dir", m_Volume.getAUSize()*256, getCreationTime().toString());
	}
	
	public TFile insertFile(byte[] abyTif, String sNewFilename, boolean bNextGen) throws InvalidNameException, ImageFullException, ProtectedException, ImageException {
		return insertFile(abyTif, sNewFilename, bNextGen, false);
	}
	
	public TFile insertFile(byte[] abyTif, String sNewFilename, boolean bNextGen, boolean bOverwrite) throws InvalidNameException, ImageFullException, ProtectedException, ImageException {
		if (m_Volume.isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));

		if (m_Files.length>=127) {
			throw new ImageFullException(TIImageTool.langstr("DirectoryFull"));
		}

		int nSectors = 0;
		if (abyTif.length < 256 && abyTif.length != 128) {
			throw new ImageException(String.format(TIImageTool.langstr("DirectoryFileSmall"), sNewFilename, abyTif.length));
		}

		if (((abyTif.length - 128)&0xff) != 0) {
			throw new ImageException(String.format(TIImageTool.langstr("DirectoryInvalidTFILen"), (abyTif.length - 128)));				  
		}
		
		// Create a File
		TFile fileNew = new TFile(abyTif);
		
		if (!fileNew.isProgram()) {
			if (fileNew.hasFixedRecordLength()) {
				if (fileNew.getRecordsPerSector() * fileNew.getAllocatedSectors() < fileNew.getRecordCount()) {
					throw new ImageException(String.format(TIImageTool.langstr("DirectoryInvalidHeader"), fileNew.getAllocatedSectors(), fileNew.getRecordsPerSector(), fileNew.getRecordCount()));
				}
			}
		}
		
		// System.out.println("File is " + abyTif.length + " bytes long (with TIFILES header)");
		nSectors = (abyTif.length - 128)/TFileSystem.SECTOR_LENGTH;
		// System.out.println("Needs " + nSectors + " sectors (without FIB)");
		
		// New file name
		String sContName = sNewFilename;
		if (sNewFilename==null) {
			sContName = fileNew.getName();
			if (sContName==null) {
				throw new ImageException(TIImageTool.langstr("MissingNameInTFI"));
			}
		}
		// System.out.println("Directory.insertFile(" + sContName + ")");

		// Already there?
		sContName = sContName.trim();
		for (TFile file:m_Files) {
			if (file.getName().trim().equals(sContName)) {
				if (!bOverwrite)
					throw new FileExistsException(sContName);
				else {
					try {
						deleteFile(file, true);
					}
					catch (FileNotFoundException fnfx) {
						fnfx.printStackTrace();
						throw new InternalException("File disappeared during delete in insertFile");
					}
				}
			}
		}
		
		if (!TFile.validName(sContName)) throw new InvalidNameException(sContName); 

		// System.out.println("File name is " + sContName);
		
		fileNew.setName(sContName);
		
		// Allocate the FIB
		// Write the contents: TFile.writeContent(byte[])
		// Insert the file into the directory
		// write back the directory index
		
		// Allocate a FIB
		Interval[] aFIB = m_Volume.findFreeSpace(1, (m_Volume.isFloppyImage() || m_Volume.isCF7Volume())? 1 : 64);
		if (aFIB == null) {
			throw new ImageFullException(TIImageTool.langstr("DirectoryNoSpace") + ". " + TIImageTool.langstr("DirectoryNoSpaceEntry"));
		}
		
		// System.out.println("Allocate " + aFIB[0]);
		m_Volume.allocate(aFIB[0]);
		
		// System.out.println("New file index is " + aFIB[0].start);
		// We must set the bit before searching the space for the file

		// Allocate the contents		
		// First start after the reserved AUs; if no space was available, 
		// cannibalize the reserved AUs
		Interval[] aint = m_Volume.findFreeSpace(nSectors, m_Volume.getReservedAUs() * m_Volume.getAUSize());
		
		// Did not make it?
		if (aint == null) {
			// Give back the already allocated FIB
			m_Volume.deallocate(aFIB[0]);
			throw new ImageFullException(TIImageTool.langstr("DirectoryNoSpace") + ". " + String.format(TIImageTool.langstr("DirectoryNoSpaceSectors"), nSectors, sContName.trim())); 
		}
		
		// TODO: On hard disks this may lead to a list of FIBs
		// We do not support chained FIBs yet (wonder whether this is useful at all)
		if (aint.length > 36) { // maximum length of data chain
			// Give back the already allocated FIB
			m_Volume.deallocate(aFIB[0]);
			// Give back the already allocated intervals
			for (Interval intv:aint) m_Volume.deallocate(intv);
			throw new ImageException(TIImageTool.langstr("DirectoryExcessiveFrag"));			
		}
		
		// Seems good, we can write the file.
		// Set the directory
		fileNew.setContainingDirectory(this);
		
		// Allocate the clusters
		for (Interval intv : aint) {
			// System.out.println("Allocating " + intv);
			m_Volume.allocate(intv);
		}
		
		// Set the clusters in the file
		fileNew.setClusters(aint);
		
		// Create and write new FIB
		// if (bNextGen) m_Volume.reopenForWrite();
		byte[] aFibNew = fileNew.createFIB(aFIB[0].start, getFileIndexSector());  // FDIR sector only for HD
		m_Volume.writeSector(new Sector(aFIB[0].start, aFibNew));
		
		// and its contents
		writeFileContents(m_Volume, aint, abyTif);

		// Add the file to this directory; gets sorted automatically
		addToList(fileNew);
		
		return fileNew;
	}
	
	private void writeFileContents(Volume vol, Interval[] aCluster, byte[] abyFile) throws ProtectedException, ImageException {
		int offset = 0;
		
		int nNetLength = abyFile.length - 128;
		int nSectors = nNetLength / TFileSystem.SECTOR_LENGTH;

		if (nSectors * TFileSystem.SECTOR_LENGTH < nNetLength) {
			nSectors = nSectors+1;
		}

		byte[] contents = new byte[TFileSystem.SECTOR_LENGTH * nSectors];
		System.arraycopy(abyFile, 128, contents, 0, abyFile.length-128);
		
		byte[] aby = new byte[TFileSystem.SECTOR_LENGTH];
		
		for (Interval intv : aCluster) {
			// System.out.println(intv);
			for (int nSect = intv.start; nSect <= intv.end; nSect++) {
				if (offset > contents.length - TFileSystem.SECTOR_LENGTH) break;
				try {
					System.arraycopy(contents, offset, aby, 0, TFileSystem.SECTOR_LENGTH);
				}
				catch (IndexOutOfBoundsException e) {
					System.out.println(String.format(TIImageTool.langstr("DirectoryWritingFailed"), getName(), intv.toString(), nSect, contents.length, offset));
				}
				vol.writeSector(new Sector(nSect, aby));
				offset += TFileSystem.SECTOR_LENGTH;
			}
		}
		if (offset < contents.length) throw new ImageException(String.format(TIImageTool.langstr("DirectoryNoSpaceBug"), offset, contents.length));
	}
	
	/** Removes a file.
		@param bRemoveFromList if false, keeps the list unchanged. This is important when an outer loop iterates over the list.
	*/
	public void deleteFile(TFile file, boolean bRemoveFromList) throws FileNotFoundException, ImageException, ProtectedException {
		if (m_Volume.isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));
		if (!containsInList(file)) throw new FileNotFoundException(file.getName());
		// Release FIBs (only if at start of AU)
		// FIB locations are sectors (not AUs)
		for (int nSect :  file.getFIBLocations()) {
			if ((nSect % m_Volume.getAUSize())==0) {
				// deallocates the complete AU
				// System.out.println("Deallocate " + nSect);
				m_Volume.deallocate(new Interval(nSect, nSect));
			}
		}

		// Is this an EMULATE file?
		if (file.isActiveEmulateFile()) {
			m_Volume.toggleEmulateFlag(file.getFIBLocation());
		}
		
		// Remove file from directory
		if (bRemoveFromList) removeFromList(file);
		// Remember to call commit!

		// Release allocated sectors
		try {
			for (Interval cluster : file.getAllocatedBlocks()) {
				// System.out.println("Deallocate " + cluster);
				m_Volume.deallocate(cluster);
			}
		}
		catch (IndexOutOfBoundsException ix) {
			throw new ImageException(TIImageTool.langstr("DirectoryEntryCorrupt"));
		}		
		// m_Volume.nextGeneration();
	}
	
	/** Called from PasteAction, only for sourceVol == targetVol. */
	public void moveoutFile(TFile file) throws ProtectedException, FileNotFoundException, IllegalOperationException  {
		if (m_Volume.isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));
		if (!containsInList(file)) throw new FileNotFoundException(file.getName());
		// System.out.println("moveout d " + file.getName());
		removeFromList(file);
	}
	
	/** Called from PasteAction, only for sourceVol == targetVol. */
	public void moveinFile(TFile file) throws ProtectedException, FileExistsException, IOException, ImageException, IllegalOperationException  {
		if (m_Volume.isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));
		if (containsInList(file)) throw new FileExistsException(file.getName());
		// System.out.println("movein d " + file.getName());
		addToList(file);
		file.setContainingDirectory(this);
		byte[] aFibNew = file.createFIB(file.getFIBLocation(), getFileIndexSector());
		m_Volume.writeSector(new Sector(file.getFIBLocation(), aFibNew));
	}
	
	/** Called from PasteAction. */
	public void moveoutDir(Directory dir) throws ProtectedException, FileNotFoundException, IllegalOperationException  {
		if (m_Volume.isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));
		if (!containsInList(dir)) throw new FileNotFoundException(dir.getName());
		removeFromList(dir);
	}

	/** Called from PasteAction. */
	public void moveinDir(Directory dir) throws ProtectedException, FileExistsException, IOException, ImageException, IllegalOperationException  {
		if (m_Volume.isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));
		// Should be prevented before, or the directory will get lost by moveoutDir
		if (m_Volume.isFloppyImage()) throw new ImageException(TIImageTool.langstr("PasteNotDirSelf"));
		if (m_Volume.isCF7Volume()) throw new ImageException(TIImageTool.langstr("PasteNotDirIntoCF7"));
		if (containsInList(dir)) throw new FileExistsException(dir.getName());
		addToList(dir);
		dir.setContainingDirectory(this);
	}
	
	// Called by Actions. Only the Archive.commit may throw an IOException
	public void commit(boolean bFinal) throws ImageException, IOException, ProtectedException {
		// Update directory descriptor record
		// System.out.println("Directory " + getName() + " commit: final=" + bFinal);
		// Thread.currentThread().dumpStack();
		writeDDR();
		writeFDIR();
		if (m_Volume.isHarddiskImage() && isRootDirectory()) {
			m_Volume.updateVIB();
		}
		// VIB of floppy is updated by updating the allocation map
		m_Volume.writeAllocationMap();  // Commit on the source may change its allocation (move), but should not advance the generation
		if (bFinal) {
			m_Volume.nextGeneration();
		}
		// System.out.println("Directory commit done");
	}
	
	/** Creates a new subdirectory. 
	*/
	public Directory createSubdirectory(String sName, boolean bNextGen) throws ProtectedException, InvalidNameException, FileExistsException, ImageFullException, ImageException, IOException, IllegalOperationException {
		if (m_Volume.isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));
		if (!validName(sName)) throw new InvalidNameException(sName);
		
		for (Directory dir : m_Subdirs) {
			if (dir.getName().equals(sName)) throw new FileExistsException(sName); 
		}
		
		Directory dirNew = null;
		if (m_Volume.isFloppyImage() || m_Volume.isCF7Volume()) {
			if (!isRootDirectory()) throw new IllegalOperationException(TIImageTool.langstr("NewDirectoryOnlyRoot"));
			if (m_Subdirs.length>2) throw new IllegalOperationException(TIImageTool.langstr("FloppyDirectoryOnly3"));

			// Allocate a new FDIR
			Interval[] aFDIR = m_Volume.findFreeSpace(1, 0x21);
			if (aFDIR == null) {
				throw new ImageFullException(TIImageTool.langstr("DirectoryNoSpace") + ". " + TIImageTool.langstr("DirectoryNoSpaceEntry"));
			}
			m_Volume.allocate(aFDIR[0]);
			dirNew = new Directory(m_Volume, sName, aFDIR[0].start, this);
		}
		else {
			// Allocate a new DDR
			Interval[] aDDR = m_Volume.findFreeSpace(1, 64);
			if (aDDR == null) {
				throw new ImageFullException(TIImageTool.langstr("DirectoryNoSpace") + ". " + TIImageTool.langstr("DirectoryNoSpaceDir"));
			}
			m_Volume.allocate(aDDR[0]);
			
			// Allocate a new FDIR
			Interval[] aFDIR = m_Volume.findFreeSpace(1, 64);
			if (aFDIR == null) {
				m_Volume.deallocate(aDDR[0]);
				throw new ImageFullException(TIImageTool.langstr("DirectoryNoSpace") + ". " + TIImageTool.langstr("DirectoryNoSpaceTable"));
			}
			m_Volume.allocate(aFDIR[0]);
			dirNew = new Directory(m_Volume, sName, aDDR[0].start, aFDIR[0].start, this);
		}

		addToList(dirNew);
		
		// Create the file index for the new directory
		dirNew.writeFDIR();

		// Create new DDR (no effect for floppies)
		dirNew.writeDDR();
		
		if (m_Volume.isFloppyImage() || m_Volume.isCF7Volume()) {
			// Update the VIB (new dir entry and allocation map)
			m_Volume.updateVIB();
		}
		else {
			// Rewrite this directory's DDR (new dir entry)
			if (isRootDirectory()) {
				// The VIB is the DDR of the root directory
				m_Volume.updateVIB();
			}
			
			writeDDR(); // no effect for root dir

			// Update the allocation map
			m_Volume.writeAllocationMap();
		}

		return dirNew;
	}
	
	protected void deleteDirectory(Directory dir, boolean bRecurse) throws ProtectedException, FileNotFoundException, FormatException, ImageException, IllegalOperationException {
//		System.out.println("Deleting directory " + dir.getName());
		if (m_Volume.isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));

		if (!containsInList(dir)) throw new FileNotFoundException(dir.getName());
		if (dir.isRootDirectory()) throw new FormatException(dir.getName(), TIImageTool.langstr("DirectoryRootNotDelete")); 

		Directory[] asubdir = dir.getDirectories();
		TFile[] afile = dir.getFiles();
		
		// If the directory is not empty, deletion must be recursive
		if ((asubdir.length > 0 || afile.length > 0) && !bRecurse) throw new FormatException(dir.getName(), TIImageTool.langstr("DirectoryNotEmpty"));  

		// Recurse
		for (int i=0; i < asubdir.length; i++) {
			dir.deleteDirectory(asubdir[i], bRecurse);
		}

		for (int i=0; i < afile.length; i++) {
			dir.deleteFile(afile[i], false);
		}
		
		// No more files or subdirectories in dir. Now delete dir itself.
		m_Volume.deallocate(new Interval(dir.getFileIndexSector(), dir.getFileIndexSector()));
		
		// Floppies and CF7 have directory entries in the VIB; no sector to free
		if (!m_Volume.isFloppyImage() && !m_Volume.isCF7Volume()) {
			m_Volume.deallocate(new Interval(dir.getDDRSector(), dir.getDDRSector()));
		}
		// TODO: rewrite VIB and alloc
	}
	
	/** Called from DeleteAction and PasteAction. */
	public void delDir(Directory dir, boolean bRecurse) throws ProtectedException, FileNotFoundException, IOException, ImageException, FormatException, IllegalOperationException  {
		// Remove from volume
		deleteDirectory(dir, bRecurse);
		// Remove from internal list
		removeFromList(dir);
	}
	
	protected void addToList(TFile newfile) {
		TFile[] aold = m_Files;
		m_Files = new TFile[aold.length+1];
		int poso = 0;
		int posn = 0;
		boolean found = false;
		while (posn < m_Files.length) {
			if (poso == aold.length || ((newfile.compareTo(aold[poso]) < 0) && !found)) {
				found = true;
				m_Files[posn++] = newfile;
			}
			if (posn < m_Files.length) m_Files[posn++] = aold[poso++];
		}
	}
	
	private void addToList(Directory newdir) {
		Directory[] aold = m_Subdirs;
		m_Subdirs = new Directory[aold.length+1];
		int poso = 0;
		int posn = 0;
		boolean found = false;
		while (posn < m_Subdirs.length) {
			if (poso == aold.length || ((newdir.compareTo(aold[poso]) < 0) && !found)) {
				found = true;
				m_Subdirs[posn++] = newdir;
			}
			if (posn < m_Subdirs.length) m_Subdirs[posn++] = aold[poso++];
		}
	}
	
	protected boolean containsInList(TFile file) {
		for (int i=0; i < m_Files.length; i++) {
			if (file.getName().equals(m_Files[i].getName())) return true;
		}
		return false;
	}
	
	private boolean containsInList(Directory dir) {
		for (int i=0; i < m_Subdirs.length; i++) {
			if (dir.equals(m_Subdirs[i])) return true;
		}
		return false;
	}
	
	/** Removes the file from the list of files, but not on the disk. */ 
	protected void removeFromList(TFile delfile) {
		TFile[] aold = m_Files;
		m_Files = new TFile[aold.length-1];
		int poso = 0;
		int posn = 0;
		while (poso < aold.length) {
			if (aold[poso].getName().equals(delfile.getName())) poso++;
			else m_Files[posn++] = aold[poso++];
		}
	}

	/** Removes the directory from the list of directories, but not on the disk. */ 
	private void removeFromList(Directory deldir) {
		// System.out.println("Removing " + deldir.getName() + " from dir " + this);
		Directory[] aold = m_Subdirs;
		m_Subdirs = new Directory[aold.length-1];
		int poso = 0;
		int posn = 0;
		while (poso < aold.length) {
			if (aold[poso].getName().equals(deldir.getName())) poso++;
			else m_Subdirs[posn++] = aold[poso++];
		}
	}
	
	// =========================================================================
	
	public void renameElement(Element el, String sName) throws FileExistsException, InvalidNameException, IOException, ImageException, ProtectedException {
		sName = sName.trim();
		// m_Volume.reopenForWrite();
		if (el instanceof TFile) {
			if (!TFile.validName(sName)) throw new InvalidNameException(sName);
			TFile file = (TFile)el;
			
			for (int i=0; i < m_Files.length; i++) {
				if (m_Files[i].getName().equals(sName)) throw new FileExistsException(sName);
			}
			removeFromList(file);
			file.setName(sName);
			addToList(file);
			// Change the file's FIB
			byte[] aFibNew = file.createFIB(file.getFIBLocation(), getFileIndexSector());
			m_Volume.writeSector(new Sector(file.getFIBLocation(), aFibNew));
			// Write the new file index record (order may have changed)
			writeFDIR();
		}
		else {
			if (!Directory.validName(sName)) throw new InvalidNameException(sName);
			Directory dir = (Directory)el;

			for (int i=0; i < m_Subdirs.length; i++) {
				if (m_Subdirs[i].getName().equals(sName)) throw new FileExistsException(sName);
			}

			removeFromList(dir);
			dir.setName(sName);
			addToList(dir);

			// Write the DDR of this directory
			if (m_Volume.isFloppyImage() || m_Volume.isCF7Volume()) {
				// Create a new VIB with updated directories
				m_Volume.updateVIB();
			}
			else {
				// Write the DDR of the renamed directory (name changed)
				dir.writeDDR();
				// Update this directory's DDR (order may have changed)
				writeDDR();
			}
		}
		// m_Volume.reopenForRead();
	}

	// =========================================================================
	
	/** Only called by Archives for updating the archive file in this directory.
		1. Deletes the file 
		2. Inserts the new version.
		If the new version is too big, inserts the old version again.
		TODO: Remove this reinsert.
	*/
	protected TFile updateFile(TFile file, byte[] abySectorContent, int nNewL3, boolean bNextGen) throws ImageException, IOException, InvalidNameException, ProtectedException {
		// Keep the old file as a TIFiles image
		byte[] abyTfiNew = TIFiles.createTfi(abySectorContent, file.getName(), file.getFlags(), file.getRecordLength(), nNewL3);

		// This should not change the FDIR (same name)
		System.out.print("Overwriting file " + file.getName());
		if (isRootDirectory()) System.out.println(" in root directory");
		else System.out.println(" in " + getName() + ", class " + getClass().getName());
		TFile fNew = null;
		try {
			fNew = insertFile(abyTfiNew, null, bNextGen, true);
			commit(bNextGen);  // may recurse
		}
		catch (ImageFullException ifx) {
			// Restore old version
			// FIXME: This is not needed anymore. However, needs a proper Undo implementation.
			TIFiles tfiOld = TIFiles.createFromFile(file);  // throws IOX
			fNew = insertFile(tfiOld.toByteArray(), null, bNextGen);
			throw ifx;
		}
		return fNew;
	}

	// =========================================================================
	
	/** The FDIR is the list of sectors of the FIBs of the files in this directory.
		It is sector 1 on floppy disks for the root directory.
	*/
	// TODO: Move to Volume, then to FileSystem
	private void writeFDIR() throws ImageException, ProtectedException {
		byte[] abyNew = new byte[256];
		Arrays.fill(abyNew, 0, 0x100, (byte)0x00);

		int nDiv = (m_Volume.isFloppyImage() || m_Volume.isCF7Volume())? 1 : m_Volume.getAUSize();
		int i=0;
		
		for (TFile file:m_Files) {
			Utilities.setInt16(abyNew, i, file.getFIBLocation()/nDiv);
			i = i+2;
		}
		
		if (!m_Volume.isFloppyImage() && !m_Volume.isCF7Volume()) 
			Utilities.setInt16(abyNew, 254, m_nDDRSector / m_Volume.getAUSize());
		
		// System.out.println("Writing the index record at " + m_nFileIndexSector);
		m_Volume.writeSector(new Sector(m_nFileIndexSector, abyNew));		
	}
	
	/** Writes a new directory descriptor record. */
	// TODO: Move to Volume, then to FileSystem
	private void writeDDR() throws ImageException, ProtectedException {
		byte[] aDDRNew = null;
		int nSector = 0;
		
		if (m_Volume.isFloppyImage() || m_Volume.isCF7Volume()) {
			// Nothing changes
			return;
		}
		
		if (m_dirParent != null) {	
			// Set the name
			// Fill with spaces
			
			nSector = m_nDDRSector;
			aDDRNew = new byte[256];
			Arrays.fill(aDDRNew, 0, 10, (byte)0x20);
			
			byte[] abyName = getName().getBytes();
			System.arraycopy(abyName, 0, aDDRNew, 0, abyName.length);
			
			// Set the number of AUs (HFDC only). Copy from Volume Information Block
			if (!m_Volume.isSCSIImage()) Utilities.setInt16(aDDRNew, 0x0a, m_Volume.getTotalAUs());
			
			// Set ID
			aDDRNew[0x0d] = (byte)'D';
			aDDRNew[0x0e] = (byte)'I';
			aDDRNew[0x0f] = (byte)'R';
			
			// 0x10,11 unused		
			
			// Creation
			System.arraycopy(m_tCreation.getBytes(), 0, aDDRNew, 0x12, 4);
						
			// Pointer to FDIR
			Utilities.setInt16(aDDRNew, 0x18, m_Volume.toAU(m_nFileIndexSector));
			
			// Pointer to parent directory
			Utilities.setInt16(aDDRNew, 0x1a, m_Volume.toAU(m_dirParent.getDDRSector()));
		
			// Number of files and subdirectories
			aDDRNew[0x16] = (byte)m_Files.length;
			aDDRNew[0x17] = (byte)m_Subdirs.length;
			System.out.println("(" + getName() + ") files = " + m_Files.length);
			
			// Subdirectories
			Arrays.fill(aDDRNew, 0x1c, 0x100, (byte)0);
			int i = 0;
			for (Directory subdir : m_Subdirs) {
				Utilities.setInt16(aDDRNew, 0x1c + i, m_Volume.toAU(subdir.getDDRSector()));
				i = i + 2;
			}
			
			// write the new DDR
			m_Volume.writeSector(new Sector(nSector, aDDRNew));
		}
		// else: Root directory of HD: will be written on next update
	}
}
