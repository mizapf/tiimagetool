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

public class Directory extends Element {
	
	public static Directory parent = new Directory("..");
	
	Directory[] m_Subdirs;
	TFile[]		m_Files;

	Volume		m_Volume;
	
	/** Needed for checkDIB. Some SCSI images may have bad AU counts. */ 
	boolean m_bBadAUCount;
		
	// ==========================================================
	// Data from the DDR
	
	/** Location of file index. */
	int 	m_nFileIndexSector;

	/** Location of the directory descriptor of this directory. */
	int		m_nDDRSector;
	
	// ==========================================================
	private final static int NAMELEN = 15;
	private final static int SIZELEN = 5;
	private final static int TYPELEN = 10;
	private final static int RECLEN = 8;
	private final static int PROLEN = 3;
	private final static int CRELEN = 20;
	private final static int UPDLEN = 18;

	Directory(Volume vol, Sector sect, Directory dirParent) throws IOException, ImageException {
		m_Volume = vol;

		TreeSet<TFile> files = new TreeSet<TFile>();
		TreeSet<Directory> subdirs = new TreeSet<Directory>();

		if (dirParent != null) m_sName = Utilities.getString10(sect.getBytes(), 0);
		m_nFileIndexSector = Utilities.getInt16(sect.getBytes(), 0x18) * vol.getAUSize();
		m_nDDRSector = sect.getNumber();
		m_tCreation = new Time(sect.getBytes(), 0x12);
		m_dirParent = dirParent;
		setContainingDirectory(dirParent);
		// Create files
		Sector sectFiles = vol.readSector(m_nFileIndexSector);
		int bad = 0;
		for (int nFile : getFilePointers(sectFiles, vol.getAUSize())) {
			try {
				Sector sectFile = vol.readSector(nFile);
				TFile file = new TFile(vol, sectFile, this);
				// if (file.getAllocatedSectors()==0) System.err.println("Warning: File " + file.getPathname() + " has no contents");
				files.add(file);
			}
			catch (ImageException ix) {
				bad++;
				System.err.println("Failed to add file at sector " + nFile);
				if (bad > 10) {
					String name = m_sName;
					if (dirParent == null) name = "root";
					throw new ImageException("Failed to read directory " + name + ": too many errors");
				}
			}
		}
		// Create directories
		for (int nDir : getDirPointers(sect, vol.getAUSize())) {
			Sector sectSubdir = vol.readSector(nDir);
			subdirs.add(new Directory(vol, sectSubdir, this));
		}
		int nMaxAU = Utilities.getInt16(sect.getBytes(), 0x0a);
		m_bBadAUCount = (nMaxAU > Volume.MAXAU);
		
		m_Subdirs = new Directory[subdirs.size()];
		subdirs.toArray(m_Subdirs);
		
		m_Files = new TFile[files.size()];
		files.toArray(m_Files);
	}

	/** Creates a parent directory link (".."). */
	public Directory(String name) {
		m_sName = name;
	}
	
	public boolean isParentLink() {
		return m_sName.equals("..");
	}
	
	/** Builds a new floppy root directory.
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
			if (Utilities.getInt16(sect.getBytes(), nDir*0x0c + 0x1e)!=0) {
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
				System.err.println("Failed to add file at sector " + nFile);
				if (bad > 10) {
					throw new ImageException("Failed to read floppy image: File system damaged");
				}
			}	
		}
		m_Subdirs = new Directory[subdirs.size()];
		subdirs.toArray(m_Subdirs);
		
		m_Files = new TFile[files.size()];
		files.toArray(m_Files);		
	}

	
	/** Builds a new floppy subdirectory. 
		@param vol Volume which contains this directory
		@param sect Sector where this directory is specified (always 0 for floppy)
		@param dirParent Parent directory (always root)
		@param nDirIndex Number of directory (0-3)
	*/
	private Directory(Volume vol, Sector sect, Directory dirParent, int nDirIndex) throws IOException, ImageException {
		m_Volume = vol;
		TreeSet<TFile> files = new TreeSet<TFile>();

		m_tCreation = new Time();
		m_nFileIndexSector = Utilities.getInt16(sect.getBytes(), nDirIndex*0x0c + 0x1e);
		m_sName = Utilities.getString10(sect.getBytes(), nDirIndex*0x0c + 0x14);
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
	
	/** Creates a new blank floppy subdirectory. Used for manually creating subdirectories. */
	private Directory(Volume vol, String sName, int nFDIRSector, Directory dirParent) {
		this(vol, sName, 0, nFDIRSector, dirParent);
		m_tCreation = new Time(); // cannot save time, so just don't show it at all
	}

	/** Creates a new blank HD subdirectory. Used for manually creating subdirectories. */
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
	
	/** Constructor for an empty directory. */
	protected Directory() {
	}
		
	/* If negative, errors were found */
	public static int checkDIB(Directory dir, boolean bWrite) throws IOException, ImageException, ProtectedException  {
		boolean bFound = false;
		int nCount = 1;
		if (dir.hasInvalidAUCount()) {
//			System.err.println("Warning: Directory Information Block of directory " + dir.getName() 
//				+ " contains an invalid MAXAU value (larger than the maximum). Setting the value to the maximum value: " + Volume.MAXAU);
			bFound = true;
			if (bWrite) {
				dir.fixDDR();
//				System.out.println("Fixed directory");
			}
		}
		Directory[] adir = dir.getDirectories();
		for (int i=0; i < adir.length; i++) nCount += checkDIB(adir[i], bWrite);
		if (bFound) nCount = -nCount;
		return nCount;
	}
	
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
			System.err.println("*** Called getParentDirectory for root directory. Ignoring.");
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
		if (sSubdir.equals(Volume.PARENTDIR) && m_dirParent != null) return true;
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
			an[i] = Utilities.getInt16(fdir.getBytes(), 2*i) * nAUSize;
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
			an[i] = Utilities.getInt16(sect.getBytes(), 0x1c + 2*i) * nAUSize;
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

		if (m_Volume.isFloppyImage() || m_Volume.isCF7Image()) {
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
	
	public int getFdrSector() {
		return m_nFileIndexSector;
	}
	
	public boolean hasInvalidAUCount() {
		return m_bBadAUCount;
	}
	
	public Directory[] getDirectories() {
		return m_Subdirs;
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
		String sPattern = "%1$-10s %2$4d %3$-7s %4$6d     %5$20s";
		return String.format(sPattern, getName(), m_Volume.getAUSize(), "Dir", m_Volume.getAUSize()*256, getCreationTime().toString());
	}
	
	public TFile insertFile(byte[] abyTif, String sNewFilename, boolean bReopen) throws InvalidNameException, ImageFullException, ProtectedException, ImageException, IOException {

		if (m_Volume.isProtected()) throw new ProtectedException("Volume is write-protected.");

		if (m_Files.length>=127) {
			throw new ImageFullException("Directory full.");
		}

		int nSectors = 0;
		if (abyTif.length < 256 && abyTif.length != 128) {
			throw new ImageException("File " + sNewFilename + " is too small to be a TIFILES file; it only has " + abyTif.length + " bytes");  
		}

		if (((abyTif.length - 128)&0xff) != 0) {
			throw new ImageException("TIFILES file payload has invalid length " + (abyTif.length - 128) + ": not a multiple of 256");  
		}
		
		// Create a File
		TFile fileNew = new TFile(abyTif);
		
		if (!fileNew.isProgram()) {
			if (fileNew.hasFixedRecordLength()) {
				if (fileNew.getRecordsPerSector() * fileNew.getAllocatedSectors() < fileNew.getRecordCount()) {
					throw new ImageException("File header has inconsistent data: sectors=" +fileNew.getAllocatedSectors() 
						+ ", rec/sect=" + fileNew.getRecordsPerSector() + ", but records=" + fileNew.getRecordCount()); 
				}
			}
		}
		
		// System.out.println("File is " + abyTif.length + " bytes long (with TIFILES header)");
		nSectors = (abyTif.length - 128)/Volume.SECTOR_LENGTH;
		// System.out.println("Needs " + nSectors + " sectors (without FIB)");
		
		// New file name
		String sContName = sNewFilename;
		if (sNewFilename==null) {
			sContName = fileNew.getName();
			if (sContName==null) {
				throw new ImageException("File name missing in TIFILES file. Please provide a file name.");
			}
		}

		// Already there?
		sContName = sContName.trim();
		for (TFile file:m_Files) {
			if (file.getName().trim().equals(sContName))
				throw new FileExistsException(sContName);
		}
		
		if (!TFile.validName(sContName)) throw new InvalidNameException(sContName); 

		// System.out.println("File name is " + sContName);
		
		fileNew.setName(sContName);
		
		// Allocate the FIB
		// Write the contents: TFile.writeContent(byte[])
		// Insert the file into the directory
		// write back the directory index
		
		// Allocate a FIB
		Interval[] aFIB = m_Volume.findFreeSpace(1, (m_Volume.isFloppyImage() || m_Volume.isCF7Image())? 1 : 64);
		if (aFIB == null) {
			throw new ImageFullException("No space left on volume. Cannot create file entry in directory.");
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
			throw new ImageFullException("No space left on volume. Failed to allocate " + nSectors + " sectors for " + sContName.trim() + ".");
		}
		
		// TODO: On hard disks this may lead to a list of FIBs
		// We do not support chained FIBs yet (wonder whether this is useful at all)
		if (aint.length > 36) { // maximum length of data chain
			// Give back the already allocated FIB
			m_Volume.deallocate(aFIB[0]);
			// Give back the already allocated intervals
			for (Interval intv:aint) m_Volume.deallocate(intv);
			throw new ImageException("Excessive fragmentation. Rebuild image.");			
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
		
		// Tell file to write its FIB
		if (bReopen) m_Volume.reopenForWrite();
		fileNew.writeFIB(aFIB[0].start, getFdrSector());
		
		// and its contents
		writeFileContents(m_Volume, aint, abyTif);

		// Add the file to this directory; gets sorted automatically
		addToList(fileNew);

		// Commit the new file index record
		writeFDIR();

		// and update the directory entry on the image
		writeDDR();
		
		// Write the allocation map
		m_Volume.update();
		
		if (bReopen) m_Volume.reopenForRead();
		
		return fileNew;
	}
	
	void writeFileContents(Volume vol, Interval[] aCluster, byte[] abyFile) throws ProtectedException, IOException, ImageException {
		int offset = 0;
		
		int nNetLength = abyFile.length - 128;
		int nSectors = nNetLength / Volume.SECTOR_LENGTH;

		if (nSectors * Volume.SECTOR_LENGTH < nNetLength) {
			nSectors = nSectors+1;
		}

		byte[] contents = new byte[Volume.SECTOR_LENGTH * nSectors];
		System.arraycopy(abyFile, 128, contents, 0, abyFile.length-128);
		
		byte[] aby = new byte[Volume.SECTOR_LENGTH];
		
		for (Interval intv : aCluster) {
			// System.out.println(intv);
			for (int nSect = intv.start; nSect <= intv.end; nSect++) {
				if (offset > contents.length - Volume.SECTOR_LENGTH) break;
				try {
					System.arraycopy(contents, offset, aby, 0, Volume.SECTOR_LENGTH);
				}
				catch (IndexOutOfBoundsException e) {
					System.out.println("Error when writing contents of " + getName() + ": Writing to cluster " 
						+ intv + " failed at sector " + nSect + "; contents.length = " + contents.length + ", offset = " + offset);
				}
				vol.writeSector(nSect, aby, false);
				offset += Volume.SECTOR_LENGTH;
			}
		}
		if (offset < contents.length) throw new ImageException("Bug: Not enough space allocated for file contents (offset=" + offset + ", length=" + contents.length + ")");
	}
	
	/** Removes a file.
		@param bRemoveFromList if false, keeps the list unchanged. This is important when an outer loop iterates over the list.
	*/
	public void deleteFile(TFile file, boolean bRemoveFromList) throws FileNotFoundException, IOException, ImageException, ProtectedException {
		if (m_Volume.isProtected()) throw new ProtectedException("Volume is write-protected.");
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
			throw new ImageException("File entry corrupt - cannot deallocate the sectors.\nYou should check the file system.");
		}		
	}
	
	/** Called from PasteAction, only for sourceVol == targetVol. */
	public void moveoutFile(TFile file) throws ProtectedException, FileNotFoundException, IllegalOperationException  {
		if (m_Volume.isProtected()) throw new ProtectedException("Volume is write-protected.");
		if (!containsInList(file)) throw new FileNotFoundException(file.getName());
		// System.out.println("moveout d " + file.getName());
		removeFromList(file);
	}
	
	/** Called from PasteAction, only for sourceVol == targetVol. */
	public void moveinFile(TFile file) throws ProtectedException, FileExistsException, IOException, ImageException, IllegalOperationException  {
		if (m_Volume.isProtected()) throw new ProtectedException("Volume is write-protected.");
		if (containsInList(file)) throw new FileExistsException(file.getName());
		// System.out.println("movein d " + file.getName());
		addToList(file);
		file.setContainingDirectory(this);
		file.writeFIB(file.getFIBLocation(), getFdrSector());
	}
	
	/** Called from PasteAction. */
	public void moveoutDir(Directory dir) throws ProtectedException, FileNotFoundException, IllegalOperationException  {
		if (m_Volume.isProtected()) throw new ProtectedException("Volume is write-protected.");
		if (!containsInList(dir)) throw new FileNotFoundException(dir.getName());
		removeFromList(dir);
	}

	/** Called from PasteAction. */
	public void moveinDir(Directory dir) throws ProtectedException, FileExistsException, IOException, ImageException, IllegalOperationException  {
		if (m_Volume.isProtected()) throw new ProtectedException("Volume is write-protected.");
		// Should be prevented before, or the directory will get lost by moveoutDir
		if (m_Volume.isFloppyImage()) throw new ImageException("Cannot move directories on floppies");
		if (m_Volume.isCF7Image()) throw new ImageException("Cannot move directories on CF7 images");
		if (containsInList(dir)) throw new FileExistsException(dir.getName());
		addToList(dir);
		dir.setContainingDirectory(this);
	}
	
	public void commit(boolean bReopen) throws IOException, ImageException, ProtectedException {
		// Update directory descriptor record
		if (bReopen) m_Volume.reopenForWrite();

		writeDDR();
		
		// Update file index
		writeFDIR();

		m_Volume.update();
		if (bReopen) m_Volume.reopenForRead();
	}
	
	/** Creates a new subdirectory. 
	*/
	public Directory createSubdirectory(String sName, boolean bReopen) throws ProtectedException, InvalidNameException, FileExistsException, ImageFullException, ImageException, IOException, IllegalOperationException {
		if (m_Volume.isProtected()) throw new ProtectedException("Volume is write-protected.");
		if (!validName(sName)) throw new InvalidNameException(sName);
		
		for (Directory dir : m_Subdirs) {
			if (dir.getName().equals(sName)) throw new FileExistsException(sName); 
		}
		
		Directory dirNew = null;
		if (m_Volume.isFloppyImage() || m_Volume.isCF7Image()) {
			if (!isRootDirectory()) throw new IllegalOperationException("Floppy file systems only allow for root subdirectories");
			if (m_Subdirs.length>2) throw new IllegalOperationException("Floppy file systems only allow for a maximum of 3 root subdirectories");

			// Allocate a new FDIR
			Interval[] aFDIR = m_Volume.findFreeSpace(1, 0x21);
			if (aFDIR == null) {
				throw new ImageFullException("No space left on volume. Cannot create file entry in directory.");
			}
			m_Volume.allocate(aFDIR[0]);
			dirNew = new Directory(m_Volume, sName, aFDIR[0].start, this);
		}
		else {
			// Allocate a new DDR
			Interval[] aDDR = m_Volume.findFreeSpace(1, m_Volume.getReservedAUs()*m_Volume.getAUSize());
			if (aDDR == null) {
				throw new ImageFullException("No space left on volume. Cannot create new directory descriptor.");
			}
			m_Volume.allocate(aDDR[0]);
			
			// Allocate a new FDIR
			Interval[] aFDIR = m_Volume.findFreeSpace(1, m_Volume.getReservedAUs()*m_Volume.getAUSize());
			if (aFDIR == null) {
				m_Volume.deallocate(aDDR[0]);
				throw new ImageFullException("No space left on volume. Cannot create file table for new directory.");
			}
			m_Volume.allocate(aFDIR[0]);
			dirNew = new Directory(m_Volume, sName, aDDR[0].start, aFDIR[0].start, this);
		}

		addToList(dirNew);
		if (bReopen) m_Volume.reopenForWrite();
		
		dirNew.writeDDR();
		dirNew.writeFDIR();
		
		writeDDR();
		
		m_Volume.update();
		if (bReopen) m_Volume.reopenForRead();
		return dirNew;
	}
	
	public void deleteDirectory(Directory dir, boolean bRecurse) throws ProtectedException, FileNotFoundException, IOException, ImageException, FormatException, IllegalOperationException {
//		System.out.println("Deleting directory " + dir.getName());
		if (m_Volume.isProtected()) throw new ProtectedException("Volume is write-protected.");

		if (!containsInList(dir)) throw new FileNotFoundException(dir.getName());
		if (dir.isRootDirectory()) throw new FormatException(dir.getName(), "root cannot be deleted");

		Directory[] asubdir = dir.getDirectories();
		TFile[] afile = dir.getFiles();
		
		if ((asubdir.length > 0 || afile.length > 0) && !bRecurse) throw new FormatException(dir.getName(), "not empty");  

		for (int i=0; i < asubdir.length; i++) {
			dir.deleteDirectory(asubdir[i], bRecurse);
		}

		for (int i=0; i < afile.length; i++) {
			dir.deleteFile(afile[i], false);
		}
		
		// No more files or subdirectories in dir. Now delete dir itself.
		m_Volume.deallocate(new Interval(dir.getFdrSector(), dir.getFdrSector()));
		if (!m_Volume.isFloppyImage() && !m_Volume.isCF7Image()) {
			m_Volume.deallocate(new Interval(dir.getDDRSector(), dir.getDDRSector()));
		}
	}
	
	public void delDir(Directory dir, boolean bRecurse) throws ProtectedException, FileNotFoundException, IOException, ImageException, FormatException, IllegalOperationException  {
		deleteDirectory(dir, bRecurse);
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
			if (file.equals(m_Files[i])) return true;
		}
		return false;
	}
	
	private boolean containsInList(Directory dir) {
		for (int i=0; i < m_Subdirs.length; i++) {
			if (dir.equals(m_Subdirs[i])) return true;
		}
		return false;
	}
	
	protected void removeFromList(TFile delfile) {
		TFile[] aold = m_Files;
		m_Files = new TFile[aold.length-1];
		int poso = 0;
		int posn = 0;
		while (poso < aold.length) {
			if (aold[poso].equals(delfile)) poso++;
			else m_Files[posn++] = aold[poso++];
		}
	}

	private void removeFromList(Directory deldir) {
		Directory[] aold = m_Subdirs;
		m_Subdirs = new Directory[aold.length-1];
		int poso = 0;
		int posn = 0;
		while (poso < aold.length) {
			if (aold[poso].equals(deldir)) poso++;
			else m_Subdirs[posn++] = aold[poso++];
		}
	}
	
	// =========================================================================
	
	public void renameElement(Element el, String sName) throws FileExistsException, InvalidNameException, IOException, ImageException, ProtectedException {
		sName = sName.trim();
		m_Volume.reopenForWrite();
		if (el instanceof TFile) {
			if (!TFile.validName(sName)) throw new InvalidNameException(sName);
			TFile file = (TFile)el;
			
			for (int i=0; i < m_Files.length; i++) {
				if (m_Files[i].getName().equals(sName)) throw new FileExistsException(sName);
			}
			removeFromList(file);
			file.setName(sName);
			addToList(file);
			file.writeFIB(file.getFIBLocation(), getFdrSector());
			// Commit the new file index record
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

			// Write the DDR of the renamed directory
			dir.writeDDR();
			// Write the DDR of this directory
			if (m_Volume.isFloppyImage() || m_Volume.isCF7Image()) {
				m_Volume.writeVIB();
			}
			else writeDDR();			
		}
		m_Volume.reopenForRead();
	}

	// =========================================================================
	
	protected TFile updateFile(TFile file, byte[] abySectorContent, int nNewL3, boolean bReopen) throws IOException, ImageException, InvalidNameException, ProtectedException {
		// Keep the old file as a TIFiles image
		byte[] abyTfiNew = TIFiles.createTfi(abySectorContent, file.getName(), file.getFlags(), file.getRecordLength(), nNewL3); 
		// System.out.println("Deleting old file " + file.getName());
		deleteFile(file, true);
		// System.out.println("Inserting new file");
		TFile fNew = insertFile(abyTfiNew, null, bReopen);
		return fNew;
	}

	// =========================================================================
	
	private void writeFDIR() throws IOException, ImageException, ProtectedException {
		byte[] abyNew = new byte[256];
		Arrays.fill(abyNew, 0, 0x100, (byte)0x00);

		int nDiv = (m_Volume.isFloppyImage() || m_Volume.isCF7Image())? 1 : m_Volume.getAUSize();
		int i=0;
		
		for (TFile file:m_Files) {
			Utilities.setInt16(abyNew, i, file.getFIBLocation()/nDiv);
			i = i+2;
		}
		
		if (!m_Volume.isFloppyImage() && !m_Volume.isCF7Image()) Utilities.setInt16(abyNew, 254, m_nDDRSector / m_Volume.getAUSize());
		// System.out.println("Writing the index record at " + m_nFileIndexSector);
		m_Volume.writeSector(m_nFileIndexSector, abyNew, false);		
	}
	
	/** Writes a new directory descriptor record. */
	private void writeDDR() throws IOException, ImageException, ProtectedException {
		byte[] aDDRNew = null;
		int nSector = 0;
		
		if (m_Volume.isFloppyImage() || m_Volume.isCF7Image()) {
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
			
			// Subdirectories
			Arrays.fill(aDDRNew, 0x1c, 0x100, (byte)0);
			int i = 0;
			for (Directory subdir : m_Subdirs) {
				Utilities.setInt16(aDDRNew, 0x1c + i, m_Volume.toAU(subdir.getDDRSector()));
				i = i + 2;
			}
			
			// write the new DDR
			m_Volume.writeSector(nSector, aDDRNew, false);
		}
		// else: Root directory of HD: will be written on next update
	}
	
	public void fixDDR() throws IOException, ImageException, ProtectedException {
		m_bBadAUCount = false;
		writeDDR();
	}
}
