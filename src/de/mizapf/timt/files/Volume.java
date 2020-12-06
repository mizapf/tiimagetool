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

	public final static int SECTOR_LENGTH=0x100;   

	/** Image format */
	ImageFormat m_Image;
	
	/** File system */
	TFileSystem m_FileSystem;
	
	/** Cached sectors of this volume. */
	SectorCache m_cache;

	// long		m_nLastMod;
			
	private String m_sImageFileName;
	
	// Used for displaying still unnamed volumes
	private int m_nUnnamedIndex;
	
	// Fill pattern of empty sectors
	private byte[] m_abyEmpty;
	
	/** Create the volume from the given file name. 
		@param sFile File name of the image file.
		@param bCheck if true, check for the DSK signature in the floppy image
	*/
	public Volume(String sFile, boolean bCheck) throws FileNotFoundException, IOException, ImageException {

		m_Image = null;
		
		// Check whether we have a number at the end of the name
		// This would be a subvolume of a CF7 image file
		int volnumpos = sFile.lastIndexOf("#");
		if (volnumpos > 0 && volnumpos < sFile.length()-1) {
			try {
				sFile = sFile.substring(0, volnumpos);
				int number = Integer.parseInt(sFile.substring(volnumpos+1));
				ImageFormat format = ImageFormat.getImageFormat(sFile);
				m_Image = ((CF7ImageFormat)format).getSubvolume(number);
				// We continue with a CF7VolumeFormat
			}
			catch (NumberFormatException nfx) {
				// Did not work, so what. 
			}
		}
				
		// Determine the file system; throws an ImageException when unknown
		if (m_Image == null) m_Image = ImageFormat.getImageFormat(sFile);
		
		// When we are here, the format could be determined
		if (m_Image.getFormatType() == ImageFormat.FLOPPY_FORMAT) {
			m_FileSystem = new FloppyFileSystem();
		}
		else {
			m_FileSystem = new HarddiskFileSystem();
		}
		
		if (m_FileSystem.isWriteCached()) {
			m_cache = new SectorCache();
			m_cache.setCommitted(true);
		}

		else m_cache = null;
		
		// m_nLastMod = m_Image.getLastModifiedTime();
		m_sImageFileName = sFile;
		
		// Read sector 0 
		Sector sector0 = readSector(0);
		byte[] abySect0 = sector0.getBytes();

		// Read the allocation map
		// Sectors 1-31 constitute the AM for hard disks, sector 0 for floppies
		ByteArrayOutputStream baosAlloc = new ByteArrayOutputStream();
		int allocStart = m_FileSystem.getAllocMapStart();
		int allocEnd = m_FileSystem.getAllocMapEnd();
		
		for (int sect = allocStart/SECTOR_LENGTH; sect <= allocEnd/SECTOR_LENGTH; sect++) {
			baosAlloc.write(readSector(sect).getBytes());
		}
		byte[] abyAlloc = baosAlloc.toByteArray();	

		// Set up the file system
		m_FileSystem.setupFromFile(abySect0, abyAlloc, bCheck);
		
		Directory dirRoot = null;
		if (m_FileSystem instanceof FloppyFileSystem) {
			int fsDensity = ((FloppyFileSystem)m_FileSystem).getDensity();
			if (fsDensity != m_Image.getDensity()) {
				System.err.println(String.format(TIImageTool.langstr("VolumeDensityMismatch"), fsDensity, m_Image.getDensity())); 
			}
			dirRoot = new Directory(this, sector0);  // used for floppy
		}
		else {
			dirRoot = new Directory(this, sector0, null);
		}
		
		m_FileSystem.setRootDirectory(dirRoot);
	}
	
	/** Create a new empty volume. */
	public Volume(TFileSystem fs, FormatParameters param, int number) throws IOException, ImageException {
		m_FileSystem = fs;
		if (m_FileSystem.isWriteCached()) {
			m_cache = new SectorCache();
			m_cache.setCommitted(false);
		}
		else m_cache = null;
		
		Directory dirRoot = new Directory(this);
		m_FileSystem.setRootDirectory(dirRoot);
		
		m_nUnnamedIndex = number;
		
		Sector[] initsec = fs.initialize(param);
		try {
			for (Sector s : initsec) {
				writeSector(s);
			}
		}
		catch (ProtectedException px) {
			System.err.println("Internal error: Volume is write-protected");
		}	
	}
	
	public Volume(String sFile) throws FileNotFoundException, IOException, ImageException {
		this(sFile, true);
	}
	
	public void setFillPattern(byte[] empty) {
		m_abyEmpty = empty;
	}
	
	/** Reads a sector.
		@throws ImageException if the sector cannot be found.
	*/
	public Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		Sector sect = m_cache.read(nSectorNumber);
		if (sect == null) {
			if (m_Image != null) {
				// We have an image file
				sect = m_Image.readSector(nSectorNumber);
			}
			else {
				// Create a new blank sector
				sect = new Sector(nSectorNumber, m_abyEmpty);
			}
		}
		return sect;
	}
		
	// Called from Directory, TFile, this
	void writeSector(Sector sect) throws ProtectedException, IOException, ImageException {
		if (isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));

		if (m_cache != null) {
			m_cache.write(sect);
		}
		else {
			m_Image.writeSector(sect.getNumber(), sect.getBytes());
		}
		
		//		long time = m_Image.getLastModifiedTime();
		// System.out.println("time = " + time + ", last mod = " + m_nLastMod);
//		if (m_nLastMod < time) throw new ProtectedException("Volume has changed on disk; cannot write. Image will be closed.");
//		m_nLastMod = m_Image.getLastModifiedTime();
	}

	boolean isDirty() {
		return m_cache.hasEntries();
	}
	
	void commitAll() {
		if (m_FileSystem.isWriteCached()) {
			// First check whether this image has already been saved to disk
			// Otherwise we cannot read the tracks
			if (!m_Image.formatCommitted()) {
				System.err.println("FIXME: formatCommitted");
			}
			
			try {
				Integer[] list = m_cache.getSectorSequence();
				for (int sect : list) {
					Sector s = m_cache.read(sect);
					if (s == null) {
						System.err.println("Internal error: Sector " + sect + " not found");
					}
					else {
						m_Image.writeSector(sect, s.getBytes());
					}
				}		
			}
			catch (IOException iox) {
				iox.printStackTrace();
			}
			catch (ImageException ix) {
				ix.printStackTrace();
			}
		}
	}
	
	public void fixCF7Geometry() {
		((FloppyFileSystem)m_FileSystem).setGeometry(1600, 40, 2, 20, 2);
	}
			
	public void reopenForWrite() throws IOException {
		if (m_cache == null)
			m_Image.reopenForWrite();
	}
	
	public void reopenForRead() throws IOException {
		if (m_cache == null)
			m_Image.reopenForRead();
	}

	public void close() throws IOException {
		// If there is no image file, changes may be lost
		if (m_Image != null) m_Image.close();
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
		if (m_Image != null)
			return m_sImageFileName.substring(m_sImageFileName.lastIndexOf(java.io.File.separator)+java.io.File.separator.length());
		else
			return TIImageTool.langstr("Unnamed") + m_nUnnamedIndex;
	}
		
	public String getModShortImageName() {
		return isModified()? ("*" + getShortImageName()) : getShortImageName(); 
	}
	
	public static boolean hasFloppyVib(byte[] abySect) {
		return (abySect[13]=='D' && abySect[14]=='S' && abySect[15]=='K');	
	}

	public static boolean hasSCSIVib(byte[] abySect) {
		return (abySect[14]==(byte)0x00 && abySect[15]==(byte)0x00 && abySect[17]==(byte)0x00); 
	}	

	public int getAUSize() {
		return m_FileSystem.getSectorsPerAU();
	}

	public int getReservedAUs() {
		return m_FileSystem.getReservedAUs();
	}
		
	public int getTotalAUs() {
		return m_FileSystem.getTotalSectors() / m_FileSystem.getSectorsPerAU();
	}
	
	public boolean isProtected() {
		return m_FileSystem.isProtected(); 
	}
	
	public int toAU(int nSectorNumber) {
		return m_FileSystem.toAU(nSectorNumber);
	}

	public void saveAllocationMap() throws IOException, ImageException, ProtectedException {
		Sector[] alloc = m_FileSystem.getAllocationMapSectors();
		
		// For floppy file systems, this rewrites the VIB
		for (Sector sect : alloc) {
			writeSector(sect);
		}
	}
	
	public boolean isHarddiskImage() {
		return (m_FileSystem instanceof HarddiskFileSystem); 
	}
	
	public boolean isFloppyImage() {
		if (!(m_FileSystem instanceof FloppyFileSystem)) return false;	
		return !((FloppyFileSystem)m_FileSystem).isCF7();
	}
	
	public boolean isSCSIImage() {
		if (!(m_FileSystem instanceof HarddiskFileSystem)) return false;	
		return ((HarddiskFileSystem)m_FileSystem).isSCSI();
	}
	
	public boolean isHFDCImage() {
		if (!(m_FileSystem instanceof HarddiskFileSystem)) return false;	
		return !((HarddiskFileSystem)m_FileSystem).isSCSI();
	}
	
	public boolean isCF7Volume() {
		if (!(m_FileSystem instanceof FloppyFileSystem)) return false;	
		return ((FloppyFileSystem)m_FileSystem).isCF7();
	}

	public String getName() {
		return m_FileSystem.getName();
	}
	
	public String getDeviceName() {
		if (m_FileSystem instanceof FloppyFileSystem) return "DSK1";
		else {
			if (((HarddiskFileSystem)m_FileSystem).isSCSI()) return "SCS1";
			else return "HDS1";
		}
	}
	
	public Directory getRootDirectory() {
		return m_FileSystem.getRootDirectory();
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
		return m_FileSystem.findFreeSpace(nSectors, nStarting);
	}

	// From CommandShell
	public int getSystemAllocatedSectors() {
		if (isFloppyImage()) return 2;
		else return 0;
	}
	
	// From CommandShell and DirectoryPanel
	public int getAllocatedSectorCount() {
		return m_FileSystem.getAllocatedSectorCount();
	}
	
	// Called from CheckFS
	public AllocationMap getAllocationMap() {
		return m_FileSystem.getAllocationMap();
	}	 
	
	// Called from Directory and CheckFS	
	void allocate(Interval intv) {
		m_FileSystem.allocate(intv);
	}
	
	void deallocate(Interval intv) {
		m_FileSystem.deallocate(intv);
	}
	
	/** Also called by TIImageTool.
	*/
	public int getTotalSectors() {
		return m_FileSystem.getTotalSectors();
	}
	
	public int getHeads() {
		return m_FileSystem.getHeads();
	}
	
	// From CommandShell and DirectoryPanel
	public int getTracksPerSide() {
		return ((FloppyFileSystem)m_FileSystem).getTracksPerSide();
	}
	
	// From TFile
	public int getAUEmulateSector() {
		return ((HarddiskFileSystem)m_FileSystem).getAUEmulateSector();
	}
	
	// From ToggleEmulateAction
	public void toggleEmulateFlag(int nSector) throws IOException, ImageException, ProtectedException {
		((HarddiskFileSystem)m_FileSystem).toggleEmulateFlag(nSector);
		updateVIB();
	}
	
	// From convert action
	public boolean isCHDImage() {
		return m_Image instanceof MameCHDFormat;
	}
	
	public String dumpFormat() {
		if (m_Image != null)
			return m_Image.getDumpFormatName();
		else
			return "-";
	}
	
	public String getFloppyFormatName() {
		return ((FloppyFileSystem)m_FileSystem).getFloppyFormatName();
	}
	
	public void renameVolume(String newName) throws IOException, ImageException, ProtectedException, InvalidNameException {
		m_FileSystem.setName(newName);
		updateVIB();
	}
	
	public byte[] createVIB() throws IOException, ImageException, ProtectedException {
		return m_FileSystem.createVIB();
	}
	
	public void updateVIB() throws IOException, ImageException, ProtectedException {
		// Write the VIB
		writeSector(new Sector(0, createVIB()));
	}
	
	public void updateAlloc() throws IOException, ImageException, ProtectedException {
		// Write the allocation map
		saveAllocationMap();
	}
	
	public void saveImage() throws IOException, ImageException {
		for (int i=0; i < m_FileSystem.getTotalSectors(); i++) {
			Sector sect = m_cache.read(i);
			if (sect != null) {
				m_Image.writeBack(sect);
				// System.out.println(sect.getNumber());
			}
		}
		m_cache.setCommitted(true);
		m_cache.wipe();
	}
	
	public boolean isModified() {
		return isDirty();
	}
	
	public String getProposedName() {
		if (m_sImageFileName != null) return m_sImageFileName;
		return m_FileSystem.getName().toLowerCase();
	}
	
	public int getImageType() {
		if (m_Image == null) return ImageFormat.NOTYPE;
		return m_Image.getImageType();
	}
	
	public void saveNewImage(String sFileName, int type, byte[] fill) throws FileNotFoundException, IOException, ImageException {
		if (m_Image == null) {
			// The volume is new, not yet backed by an image file
			// { } ---> A
			m_sImageFileName = sFileName;
			m_Image = ImageFormat.getImageFormat(sFileName, type, m_FileSystem);
			m_Image.setFillPattern(fill);
			m_cache.setFillPattern(fill);
			saveImage();
		}
		else {
			// The image already exists; we are creating a new image
			// A ---> B
			m_sImageFileName = sFileName;
			// We need to keep this image because it will deliver the sectors that are unchanged
			ImageFormat newImage = ImageFormat.getImageFormat(sFileName, type, m_FileSystem);
			newImage.setFillPattern(fill);
			
			for (int i=0; i < m_FileSystem.getTotalSectors(); i++) {
				Sector sect = readSector(i);
				System.out.println(sect.getNumber());
				newImage.writeBack(sect);
			}
			m_cache.setCommitted(true);
			m_cache.wipe();
			m_Image = newImage;
			
			// Problem: 
			// ARC file written to image, then saved: not recognized as an ARC anymore
			// works after reopen
		}
	}
	
/*************************** Low-level routines *****************************/
		
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
		((HarddiskFileSystem)m_FileSystem).setParams(sectors, speed, current, heads, buff!=0, precomp);
		((HarddiskFileSystem)m_FileSystem).setSCSI(false);
		updateVIB();
	}
	
	public void hfdc2scsi() throws IOException, ImageException, ProtectedException {
		// Set disk parameters
		// Need to know
		//	 Sectors per track
		//	 Number of heads
		((HarddiskFileSystem)m_FileSystem).setParams(0, 0, 0, 1, false, 0);
		((HarddiskFileSystem)m_FileSystem).setSCSI(true);
		updateVIB();
	}
}
