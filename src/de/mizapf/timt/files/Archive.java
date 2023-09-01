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
import java.util.*;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.util.TIFiles;
import java.io.FileNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import de.mizapf.timt.util.LZW;
import de.mizapf.timt.TIImageTool;

/*
ARK format


0x00 - 0x09: File name       --> TIFILES: 0x10-0x19
0x0a: Flags                  -->          0x0a
0x0b: #rec/sec               -->          0x0b
0x0c-0x0d: total #sect       -->          0x08-0x09
0x0e: EOF offset             -->          0x0c
0x0f: reclen                 -->          0x0d
0x10-0x11: #L3               -->          0x0e-0x0f

All file contents sequentially stored; must get the offset for each file from 
the sector count: content_offset = (#directory_sectors + sum(#total_sect_previous_files))*256

Example:

000000: 2d 52 45 41 44 4d 45 20 20 20 80 03 00 06 83 50     -README   .....P
000010: 06 00 51 44 45 20 20 20 20 20 20 20 11 00 00 31     ..QDE       ...1
000020: 5e 00 00 00 51 44 45 5f 44 4f 43 20 20 20 80 03     ^...QDE_DOC   ..
000030: 00 48 b9 50 48 00 51 44 45 5f 48 53 20 20 20 20     .H.PH.QDE_HS
000040: 90 03 00 07 86 50 07 00 00 00 00 00 00 00 00 00     .....P..........
...
0000e0: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000f0: 00 00 00 00 00 00 00 00 00 00 00 00 45 4e 44 21     ............END!

000100: 38 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20     8
000110: 20 20 20 20 20 20 20 20 20 51 44 45 20 76 33 2e              QDE v3.
000120: 33 20 52 65 6c 65 61 73 65 20 4e 6f 74 65 73 20     3 Release Notes
000130: 20 39 34 2f 30 34 2f 30 36 00 3f 20 20 20 20 20      94/04/06.?

000700: 00 46 30 58 04 00 02 03 00 00 13 01 06 93 04 60     .F0X...........`
000710: 2a 1a 00 00 00 00 00 00 00 00 00 00 00 00 00 00     *...............

003800: 00 00 4c 20 20 51 44 45 20 20 20 20 20 20 20 20     ..L  QDE
003810: 20 20 20 20 20 20 20 20 20 20 20 20 20 20 54 65                   Te

008000: 2f 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20     /
008010: 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 51                    Q
008020: 44 45 20 46 75 6e 63 74 69 6f 6e 20 4b 65 79 73     DE Function Keys




content_offset(QDE_DOC) = (1 + 6 + 49) * 0x100 = 0x3800

End of directory: Sector ending with "END!"

ARK format:

LZW-compressed

Directory = ((DirEntry)*14 (0x00)*4)* (DirEntry)+ (0x00)* "END!"  

DirEntry = see above, 18 bytes

14 entries per sector (@ 18 bytes)
4 bytes 0x00 at end

Directory length = multiple of 256 bytes

FileContent = (SectorContent)+
FileContent length = multiple of 256 bytes

*/

public class Archive extends Directory {

	byte[] m_abyContent;
	
	/** Used to buffer the new content until it can be committed to the medium. */ 
	byte[] m_abyOldContent;
	
	TFile m_fBase;
	
	boolean m_bCompressed;
	
	class Entry {
		String name;
		int sectors;
		int offset;
		
		Entry(String sName, int nSectors) {
			name = sName;
			sectors = nSectors;
			offset = 0;
		}
		
		void setOffset(int nOffset) {
			offset = nOffset;
		}
	}
	
	/** Constructor of the archive. We assume that the contents have already 
		been uncompressed. 
	*/	
	public Archive(Volume vol, String sName, Directory dirParent, byte[] content, TFile fBase, boolean bCompressed) throws IllegalOperationException {
		
		m_Volume = vol;
		m_fBase = fBase;

		TreeSet<TFile> files = new TreeSet<TFile>();
		m_Subdirs = new Directory[0];
		m_sName = sName;
		m_dirParent = dirParent;
		m_bBadAUCount = false;
		m_bCompressed = bCompressed;
		
		boolean bDone = false;
		int nPos = 0;
		String sFileName = null;
		Entry file = null;
		int nStart = 0;
		
		m_abyContent = content;
		List<Entry> entries = new ArrayList<Entry>();
		int nSector = 0;
		int nSectors = 0;
		boolean bFound = false;
		byte byFlags;
		int nRecPerSect = 0;
		int nEOFOffset = 0;
		int nRecordLength = 0;
		int nL3 = 0;
		int nDirSectors = 0;
		
		// Go through all entries
		while (!bFound && (nDirSectors < 10)) {
			if (content[nDirSectors*TFileSystem.SECTOR_LENGTH + 0xfc] == (byte)'E' 
				&& content[nDirSectors*TFileSystem.SECTOR_LENGTH + 0xfd] == (byte)'N' 
			&& content[nDirSectors*TFileSystem.SECTOR_LENGTH + 0xfe] == (byte)'D' 
			&& content[nDirSectors*TFileSystem.SECTOR_LENGTH + 0xff] == (byte)'!') bFound = true;
			else nDirSectors++;
		}
		
		nDirSectors++;
		
		if (!bFound) throw new IllegalOperationException(TIImageTool.langstr("ArchiveNot"));

		bDone = false;
		nPos = 0;
				
		nStart = nDirSectors * TFileSystem.SECTOR_LENGTH;

		nSector = 0;
		// System.out.println("nDirSectors = " +nDirSectors); 
		
		while (!bDone) {
			// System.out.println("nPos = " + nPos);
			if (content[nPos] != (byte)0 && (nSector < (nDirSectors-1) || (nPos != (nDirSectors-1)*TFileSystem.SECTOR_LENGTH + 0xfc))) {
				sFileName = Utilities.getString10(content, nPos);
				nPos += 10;
				byFlags = content[nPos++];
				nRecPerSect = (int)(content[nPos++]&0xff);
				nSectors = Utilities.getInt16(content, nPos);
				nPos += 2;
				nEOFOffset = (int)(content[nPos++] & 0xff);
				nRecordLength = (int)(content[nPos++] & 0xff);
				
				nL3 = Utilities.getInt16rev(content, nPos);
				nPos += 2;
				
				// System.out.println(sFileName);
				
				files.add(new ArchiveFile(m_abyContent, nStart, sFileName, nRecordLength, 
					byFlags, nRecPerSect, nEOFOffset, nL3, nSectors, this));

				nStart += nSectors * TFileSystem.SECTOR_LENGTH;
			}
			else {
				// System.out.println("nSector = " + nSector);
				nSector++;
				if (nSector == nDirSectors) bDone = true;
				else nPos = nSector * TFileSystem.SECTOR_LENGTH;
			}
		}
		m_Files = new TFile[files.size()];
		files.toArray(m_Files);
	}
	
	public boolean isCompressed() {
		return m_bCompressed;
	}
	
	/** Check for Archive format. */
	public static boolean hasPlainArchiveFormat(byte[] content) {
		try {
			if (content.length==0) {
				return false;
			}
			// Archive format has
			// - an even number of records
			// - a string "END!" at a position 0xfc + n*(0x100) with 0x00000000 at
			// all previous 0xfc + m*(0x100)
			// With 127 files at most, n < 10
			
			for (int i=0; i < 10; i++) {
				if (content[i*256 + 0xfc] == (byte)'E' && content[i*256 + 0xfd] == (byte)'N' 
					&& content[i*256 + 0xfe] == (byte)'D' && content[i*256 + 0xff] == (byte)'!') return true; 
				if (content[i*256 + 0xfc] != (byte)0 || content[i*256 + 0xfd] != (byte)0 
					|| content[i*256 + 0xfe] != (byte)0 || content[i*256 + 0xff] != (byte)0) return false; 
			}
			return false;
		}
		catch (ArrayIndexOutOfBoundsException ax) {
//			ax.printStackTrace();
			return false;
		}
	}
	
	public static byte[] createEmptyArchive(boolean bCompressed) {
		byte[] abyContent = new byte[256];
		for (int i=0; i < 252; i++) abyContent[i] = (byte)0x00;
		abyContent[252] = (byte)'E';
		abyContent[253] = (byte)'N';
		abyContent[254] = (byte)'D';
		abyContent[255] = (byte)'!';
		
		byte[] content = abyContent;
		if (bCompressed) {
			LZW lzw = new LZW(abyContent);
			content = lzw.compress();
		}
		
		return content;
	}
	
	@Override
	public TFile insertFile(byte[] abyTif, String sNewFilename, boolean bReopen) throws ProtectedException, IOException, InvalidNameException, ImageFullException, ImageException, FileExistsException {
		if (isProtected()) throw new ProtectedException(TIImageTool.langstr("ArchiveProtected"));

		if (m_Files.length>=127) {
			throw new ImageFullException(TIImageTool.langstr("ArchiveNoSpace"));
		}

		int nSectors = 0;

		// Create a File
		TFile fileNew = new TFile(abyTif);
		
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

		// Already there?
		sContName = sContName.trim();
		for (TFile file:m_Files) {
			if (file.getName().trim().equals(sContName))
				throw new FileExistsException(sContName);
		}
		
		if (!TFile.validName(sContName)) throw new InvalidNameException(sContName); 

		// System.out.println("Insert a new file " + sContName); 
		ArchiveFile afNew = new ArchiveFile(abyTif, 128, sContName, fileNew.getRecordLength(), fileNew.getFlags(), 
			fileNew.getRecordsPerSector(), fileNew.getEOFOffset(), fileNew.getRecordCount(), fileNew.getAllocatedSectors(), this);

		// Add the file to this archive; gets sorted automatically
		addToList(afNew);

		// For archives, we now have to rebuild the byte array
		m_abyOldContent = m_abyContent;
		// Directory dirParent = getContainingDirectory();
		m_abyContent = rebuild();

		// fileNew.checkArchiveFormat();
		// System.out.println(Utilities.hexdump(0, 0, m_abyContent, m_abyContent.length, false));
		
/*		// This is done in commit
	 	try {
			m_fBase = dirParent.updateFile(m_fBase, m_abyContent, (m_abyContent.length/256)*2, bReopen);
		}
		catch (ImageFullException ifx) {
			// Image is full; revert to previous state
			// The file is already deleted from the image; we need to re-insert it
			removeFromList(afNew);
			// System.out.println("Reverting to earlier state of " + m_fBase.getName()); 
			byte[] abyTfiOld = TIFiles.createTfi(m_abyOldContent, m_fBase.getName(), 
				m_fBase.getFlags(), m_fBase.getRecordLength(), (m_abyOldContent.length/256)*2); 	

			m_fBase = dirParent.insertFile(abyTfiOld, null, true);
			throw ifx;
		} */
		return afNew;
	}
	
	/** Used when creating an archive. This is more efficient since it 
		compresses the archive only after adding all files. 
		Called from CreateArchiveAction only.
	*/		
	public void insertFiles(TIFiles[] files, String sNewFilename, boolean bReopen) throws ProtectedException, IOException, InvalidNameException, ImageFullException, ImageException, FileExistsException {
		if (isProtected()) throw new ProtectedException(TIImageTool.langstr("ArchiveProtected"));

		if (m_Files.length>=127) {
			throw new ImageFullException(TIImageTool.langstr("ArchiveNoSpace"));
		}

		int nSectors = 0;

		// Add all files here before compressing
		for (TIFiles tifile : files) {
			byte[] abyTif = tifile.toByteArray();
			TFile fileNew = new TFile(abyTif);
	
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
			
			// Already there?
			sContName = sContName.trim();
			for (TFile file:m_Files) {
				if (file.getName().trim().equals(sContName))
					throw new FileExistsException(sContName);
			}
			
			if (!TFile.validName(sContName)) throw new InvalidNameException(sContName); 
			
			// System.out.println("Insert a new file " + sContName); 
			ArchiveFile afNew = new ArchiveFile(abyTif, 128, sContName, fileNew.getRecordLength(), fileNew.getFlags(), 
				fileNew.getRecordsPerSector(), fileNew.getEOFOffset(), fileNew.getRecordCount(), fileNew.getAllocatedSectors(), this);
			
			// Add the file to this archive; gets sorted automatically
			addToList(afNew);			
			fileNew.checkArchiveFormat();
		}
		
		// For archives, we now have to rebuild the byte array
		// m_abyOldContent = m_abyContent;
		// Directory dirParent = getContainingDirectory();
		// Compression happens here
		// m_abyContent = rebuild();

		// System.out.println(Utilities.hexdump(0, 0, m_abyContent, m_abyContent.length, false));
		// m_fBase = dirParent.updateFile(m_fBase, m_abyContent, (m_abyContent.length/256)*2, bReopen);
		commit(bReopen);
	}	
	
	@Override
	public String getFullPathname() {
		return m_dirParent.getFullPathname() + "." + getName() + " (" + TIImageTool.langstr("ArchiveIndicator") + ")";
	}
	
	@Override
	public void deleteFile(TFile file, boolean bRemoveFromList) throws ProtectedException, FileNotFoundException {
		if (m_Volume.isProtected()) throw new ProtectedException(TIImageTool.langstr("ArchiveProtected"));
		if (!containsInList(file)) throw new FileNotFoundException(file.getName());

		// Remove file from directory
		if (bRemoveFromList) removeFromList(file);
		// Remember to call commit!
	}

	@Override
	public void commit(boolean bReopen) throws IOException, ImageException, ProtectedException {
		// For archives, we now have to rebuild the byte array
		System.out.println("Archive commit");
		m_abyOldContent = m_abyContent;
		Directory dirParent = getContainingDirectory();
		System.out.println("Rebuild");
		m_abyContent = rebuild();

		// System.out.println(Utilities.hexdump(0, 0, m_abyContent, m_abyContent.length, false));
		try {
			System.out.println("Update file");
			m_fBase = dirParent.updateFile(m_fBase, m_abyContent, (m_abyContent.length/256)*2, bReopen);
			
			// From Directory.commit
			// if (bReopen) m_Volume.reopenForWrite();
			// writeDDR();
		
			// Update file index
			// writeFDIR();

			// m_Volume.updateVIB();
			// m_Volume.updateAlloc();
			// if (bReopen) m_Volume.reopenForRead();
//			m_Volume.nextGeneration();
		}
		catch (ImageFullException ifx) {
			// Image is full; revert to previous state
			ifx.printStackTrace();
			throw new ImageException(TIImageTool.langstr("ArchiveUnexp"));
		}
		catch (InvalidNameException inx) {
			inx.printStackTrace();
			throw new ImageException(TIImageTool.langstr("ArchiveUnexp"));
		}
		System.out.println("Archive commit done");
	}

	/** Called from PasteAction, only for sourceVol == targetVol. Moving out of an archive always means to 
		delete it from the archive.*/
	@Override
	public void moveoutFile(TFile file) throws ProtectedException, FileNotFoundException, IllegalOperationException {
		if (m_Volume.isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));
		// System.out.println("moveout a " + file.getName());
		deleteFile(file, true);
	}
	
	/** Called from PasteAction, only for sourceVol == targetVol. Moving into an archive always means to 
		insert it into the archive. */
	@Override
	public void moveinFile(TFile file) throws ProtectedException, FileExistsException, IOException, ImageException, IllegalOperationException {
		if (m_Volume.isProtected()) throw new ProtectedException(TIImageTool.langstr("VolumeWP"));
		if (containsInList(file)) throw new FileExistsException(file.getName());
		// System.out.println("movein a " + file.getName());
		TIFiles tfiNew = TIFiles.createFromFile(file);
		try {
			insertFile(tfiNew.toByteArray(), null, false);
		}
		catch (InvalidNameException inx) {
			inx.printStackTrace();
		}
	}
	
	@Override
	public void renameElement(Element el, String sName) throws FileExistsException, InvalidNameException, IOException, ImageException, ProtectedException {
		sName = sName.trim();
		// m_Volume.reopenForWrite();
		if (!TFile.validName(sName)) throw new InvalidNameException(sName);
		
		for (int i=0; i < m_Files.length; i++) {
			if (m_Files[i].getName().equals(sName)) throw new FileExistsException(sName);
		}

		// OK so far, now remove the file and add it again
		ArchiveFile file = (ArchiveFile)el;
		removeFromList(file);
		file.setName(sName);
		addToList(file);
		commit(false);
		
		// m_Volume.reopenForRead();
	}
	
	/** Called from PasteAction. */
	@Override
	public void moveoutDir(Directory dir) throws ProtectedException, FileNotFoundException, IllegalOperationException  {
		throw new IllegalOperationException(TIImageTool.langstr("ArchiveNotMoveOut"));
	}

	/** Called from PasteAction. */
	@Override
	public void moveinDir(Directory dir) throws ProtectedException, FileExistsException, IOException, ImageException, IllegalOperationException  {
		throw new IllegalOperationException(TIImageTool.langstr("ArchiveNotMoveIn"));
	}
	
	@Override
	public Directory createSubdirectory(String sName, boolean bReopen) throws ProtectedException, InvalidNameException, FileExistsException, ImageFullException, ImageException, IOException, IllegalOperationException {
		throw new IllegalOperationException(TIImageTool.langstr("ArchiveNotCreateDir"));
	}

	@Override
	protected void deleteDirectory(Directory dir, boolean bRecurse) throws ProtectedException, FileNotFoundException, IOException, ImageException, FormatException, IllegalOperationException {
		throw new IllegalOperationException(TIImageTool.langstr("ArchiveNotDelDir"));
	}
	
	@Override
	public void delDir(Directory dir, boolean bRecurse) throws ProtectedException, FileNotFoundException, IOException, ImageException, FormatException, IllegalOperationException {
		throw new IllegalOperationException(TIImageTool.langstr("ArchiveNotDelDir"));
	}
	
	/** Recreates the archive when its contents have changed. If the archive
		does not fit on the medium after the change, it must be reverted.
		@throws IOException for File#getSectorContent
	*/
	private byte[] rebuild() throws ImageException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int nPos = 0;
		boolean bPadding = false;
		for (TFile f : m_Files) {
			int nSectors = f.getAllocatedSectors();
			int nL3 = f.getRecordCount();
			byte[] name = f.getName().getBytes(); 

			if (bPadding) {
				baos.write((byte)0);
				baos.write((byte)0);
				baos.write((byte)0);
				baos.write((byte)0);
				nPos += 4;
				bPadding = false;
			}

			baos.write(name, 0, name.length);
			for (int j=name.length; j < 10; j++) baos.write((byte)0x20);	// pad with spaces
			baos.write(f.getFlags());
			baos.write(f.getRecordsPerSector());
			baos.write((byte)((nSectors >> 8)&0xff));
			baos.write((byte)(nSectors&0xff));
			baos.write((byte)f.getEOFOffset());
			baos.write((byte)f.getRecordLength());
			baos.write((byte)(nL3 & 0xff));
			baos.write((byte)((nL3 >> 8)&0xff));	// #L3 is little-endian
			nPos = nPos + 18;
			if ((nPos % 256) == 252) bPadding = true;
		}
		
		while ((nPos++ % 256) != 252) baos.write((byte)0x00);
		baos.write((byte)'E');
		baos.write((byte)'N');
		baos.write((byte)'D');
		baos.write((byte)'!');
		
		for (TFile f : m_Files) {
			// System.out.println("f.name = " + f.getName() + ", class = " + f.getClass().getName());
			byte[] aby = ((ArchiveFile)f).getSectorContent();
			baos.write(aby, 0, aby.length);
		}
		
		byte[] abyUncomp = baos.toByteArray();
		byte[] content = abyUncomp;
		if (isCompressed()) {
			LZW lzw = new LZW(abyUncomp);
			content = lzw.compress();
		}
		return content;
	}
}
