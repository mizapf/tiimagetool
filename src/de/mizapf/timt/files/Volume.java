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
import de.mizapf.timt.util.*;
import de.mizapf.timt.TIImageTool;

public class Volume {

	/** Image format */
	ImageFormat m_Image;
	
	/** File system */
	TFileSystem m_FileSystem;

	// long		m_nLastMod;		
	// private String m_sImageFileName;
			
	/** Called when opening an image file. */
/*	public Volume(String sFile) throws FileNotFoundException, IOException, ImageException {
		this(sFile, null);
	} */

	// Indicates whether this is a read-only image
	private boolean m_bReadOnly;
	
	/** Create the volume from the given file name (existing file).
		@param sFile File name of the image file.
	*/
	public Volume(ImageFormat image, TFileSystem fs) throws FileNotFoundException, IOException, MissingHeaderException, ImageException {

		m_Image = image;
		m_bReadOnly = false;

		if (fs == null) {
			throw new InternalException("** FileSystem is null");
		}
		m_FileSystem = fs;

		// Check whether the image is read-only
		try {
			image.reopenForWrite();
		}
		catch (ProtectedException px) {
			System.err.println("Volume is read-only");
			m_bReadOnly = true;
		}
		image.reopenForRead();

		buildTree();
		
		image.setStartGeneration();
	}
	
	/** Newly created volume. */
	public Volume(TFileSystem fs, int unnamedNumber) {
		m_FileSystem = fs;
		
		Directory dirRoot = new Directory(this, fs.getRootFDIR());
		fs.setRootDirectory(dirRoot);

		m_Image = new MemoryImageFormat("unsaved", unnamedNumber);
		
		Sector[] initsec = fs.createInitSectors();
		try {
			for (Sector s : initsec) {
				writeSector(s);
			}
		}
		catch (ProtectedException px) {
			System.err.println("Internal error: Volume is write-protected, although it is a memory image");
		}	
	}
	
	public void buildTree() throws ImageException, IOException {
		Sector sector0 = m_Image.readSector(0);
		// System.out.println(Utilities.hexdump(sector0.getData()));
		Directory dirRoot = null;
		
		if (m_FileSystem instanceof FloppyFileSystem)
			dirRoot = new Directory(this, sector0);  // used for floppy
		else
			dirRoot = new Directory(this, sector0, null);   // used for HD

		m_FileSystem.setRootDirectory(dirRoot);
	}
	
	public boolean isReadOnly() {
		return m_bReadOnly;
	}
	
	/** Reads a sector.
		@throws ImageException if the sector cannot be found.
		@throws ProtectedException if the image is write-protected so that the write back fails
	*/
	public Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		// System.out.println("readSector " + nSectorNumber);
		return m_Image.readSector(nSectorNumber);
	}
		
	// Called from Directory, TFile, this
	void writeSector(Sector sect) throws ProtectedException {
		if (isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));
		m_Image.writeSector(sect);
	}

	public void close() throws IOException {
		// If there is no image file, changes may be lost
		if (m_Image != null) m_Image.close();
	}
	
	public boolean equals(Object other) {
		boolean res = false;
		if (other==this) res = true;
		else {
			if (other!=null) { 
				if (other instanceof Volume) {
					String imageName = getImageName();
					if (imageName!=null) {
						res = imageName.equals(((Volume)other).getImageName());
					}
				}
			}
		}
		return res;
	}
	
	public String getImageName() {
		return m_Image.getImageName();
	} 

	public int getPartitionNumber() {
		if (m_Image != null) {
			if (isHarddiskImage()) {
				return ((HarddiskImageFormat)m_Image).getActivePartition();
			}
		}
		return -1;
	}
	
	public Directory traverse(String[] path) throws ImageException {
		Directory dir = m_FileSystem.getRootDirectory();
		for (String nextdir : path) 
			dir = dir.enterDirectory(nextdir);
		return dir;		
	}
	
	public int getImageType() {
		if (m_Image == null) return ImageFormat.MEMORY;
		// System.out.println("git: " + m_Image.getClass().getName() + ", type = " + m_Image.getImageType());
		return m_Image.getImageType();
	}
		
	public String getModShortImageName() {
		return m_Image.getModShortImageName();
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
		return m_FileSystem.getAUNumber(nSectorNumber);
	}

	public void writeAllocationMap() throws ProtectedException {
		// System.out.println("Write allocation map");
		// Thread.currentThread().dumpStack();

		Sector[] alloc = m_FileSystem.createAllocationMapSectors();
		
		// For floppy file systems, this rewrites the VIB
		for (Sector sect : alloc) {
			if (sect != null) writeSector(sect);
		}
	}
	
	public void readAllocationMap() throws IOException, ImageException {
		// System.out.println("Read allocation map");
		Interval allSect = m_FileSystem.getAllocationInterval();
		byte[] amap = m_Image.getContent(allSect.start, allSect.end);
		m_FileSystem.setupAllocationMap(amap);
	}
	
	public boolean isHarddiskImage() {
		return (m_FileSystem instanceof HarddiskFileSystem); 
	}
	
	public boolean isFloppyImage() {
		// if (!(m_FileSystem instanceof FloppyFileSystem)) return false;	
		// return !((FloppyFileSystem)m_FileSystem).isCF7();
		return m_FileSystem instanceof FloppyFileSystem;
	}
	
	public boolean isSCSIImage() {
		return (m_FileSystem instanceof SCSIFileSystem);
	}
	
	public boolean isHFDCImage() {
		return (m_FileSystem instanceof HFDCFileSystem);
	}

	public boolean isIDEImage() {
		return (m_FileSystem instanceof IDEFileSystem);
	}
	
	public boolean isCF7Volume() {
		// if (!(m_FileSystem instanceof FloppyFileSystem)) return false;	
		// return ((FloppyFileSystem)m_FileSystem).isCF7();
		return false;
	}

	public String getName() {
		return m_FileSystem.getVolumeName();
	}
	
	public String getDeviceName() {
		if (m_FileSystem instanceof FloppyFileSystem) return "DSK1";
		else {
			if (m_FileSystem instanceof IDEFileSystem) {
				int part = ((HarddiskImageFormat)m_Image).getActivePartition() + 1;
				return "IDE" + part;
			}
			else { 
				if (m_FileSystem instanceof SCSIFileSystem) return "SCS1";
				else return "HDS1";
			}
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
	
	// From CommandShell and DirectoryPanel
	public int getTracksPerSide() {
		return ((FloppyFileSystem)m_FileSystem).getTracksPerSide();
	}
	
	// From TFile
	public int getAUEmulateSector() {
		if (m_FileSystem instanceof HFDCFileSystem)
			return ((HFDCFileSystem)m_FileSystem).getAUEmulateSector();
		else
			return 0;
	}
	
	// From ToggleEmulateAction
	public void toggleEmulateFlag(int nSector) throws ImageException, ProtectedException {
		((HFDCFileSystem)m_FileSystem).toggleEmulateFlag(nSector);
		updateVIB();
	}
	
	// From convert action
	public boolean isCHDImage() {
		return m_Image instanceof MameCHDFormat;
	}
	
	public String dumpFormat() {
		return m_Image.getFormatName();
	}
	
	public String getFloppyFormatName() {
		return ((FloppyFileSystem)m_FileSystem).getFloppyFormatName();
	}
	
	public void renameVolume(String newName) throws IOException, ImageException, ProtectedException, InvalidNameException {
		m_FileSystem.setVolumeName(newName);
		updateVIB();
	}
	
	public void updateVIB() throws ProtectedException {
		// Write the VIB
		// System.out.println("updateVIB");
		writeSector(new Sector(0, m_FileSystem.createVIBContents()));
	}
	
	public void fixCF7Geometry() {
		((FloppyFileSystem)m_FileSystem).setGeometry(40, 2, 20);
	}
	
	// Called from Actions and Directory
	public void nextGeneration(boolean bNew) {
		m_Image.nextGeneration(bNew);
	}

	public boolean redoPossible() {
		return m_Image.redoPossible();
	}
	
	// Called from SaveImageAction
	/** Save all modified sectors to the image. A sector is modified 
		when the cache has an entry of it, or when the cache has never been
		committed. That means that on the first invocation, all sectors
		are written.
	*/

	/** Write back the changed sectors. No image format change.
	    Never called for MemoryImageFormat.
	*/
	public void saveImage() throws IOException, ImageException, ProtectedException {
		((FileImageFormat)m_Image).saveImage();
	}
	
	/** Write all sectors to the new image. */
	public void saveNewImage(FileImageFormat newImage) throws FileNotFoundException, IOException, ImageException {
		// Get the format (includes preparing the image) 
		//newImage.setFileSystem(m_FileSystem);
		// System.out.println("Save new image");
		newImage.saveImageFromOld(m_Image);
		m_FileSystem.setImage(newImage);
		
		m_Image = newImage;
		System.out.println(newImage.getClass().getName());
	}
		
	public boolean isMemoryImage() {
		return m_Image instanceof MemoryImageFormat;
	}
	
	public boolean isModified() {
		return m_Image.cacheHasUnsavedEntries();
	}
	
	public void undoAction() throws ImageException, IOException {
		m_Image.previousGeneration();
		buildTree();
		readAllocationMap();   // restore the previous allocation map
	}
	
	public void redoAction() throws ImageException, IOException {
		nextGeneration(false);
		buildTree();
		readAllocationMap();   // restore the previous allocation map
	}
	
	public FormatParameters getFormatParams() {
		return m_FileSystem.getParams();
	}
	
	/** Returns the number of sectors that are allocated to maintain the
	 	file system. */
	public int getSysAllocated() {
		return m_FileSystem.getSysAllocated();
	}
}
