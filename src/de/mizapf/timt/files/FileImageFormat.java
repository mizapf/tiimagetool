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
    
    Copyright 2021 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.files;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.*;
import java.lang.reflect.*;

public abstract class FileImageFormat extends ImageFormat {
	
	RandomAccessFile m_file;
	int m_nCurrentFormatUnit;
	boolean m_bInitial;
	String m_sFileName;
	long m_nLength;

	FormatParameters m_format;

	// Partition support
	int m_nPartitions;
	protected int m_nActivePartition;
	Partition[] m_partition;
	
	/** Existing format. */
	protected FileImageFormat(String sFileName) throws FileNotFoundException, IOException {
		m_sFileName = sFileName;
		m_file = new RandomAccessFile(sFileName, "r");
		m_nLength = m_file.length();
		System.out.println("Opening image " + sFileName + " for reading"); 
		m_nCurrentFormatUnit = NONE;
		m_bInitial = false;
		m_writeCache.setName(getShortImageName());
	}
	
	/** New format. */
	protected FileImageFormat(String sFileName, FormatParameters param) throws FileNotFoundException, IOException {
		m_sFileName = sFileName;
		m_file = new RandomAccessFile(sFileName, "rw");
		System.out.println("Opening image " + sFileName + " for writing"); 
		m_nLength = m_file.length();
		m_nCurrentFormatUnit = NONE;
		m_bInitial = true;
		m_writeCache.setName(getShortImageName());
		m_nTotalSectors = param.getTotalSectors();
	}
	
	protected FileImageFormat() {
		super();
	}
	
	protected void loadFormatUnit(int funum) throws ImageException, IOException {
		if (funum < 0) throw new InternalException("Wrong format unit number: " + funum);
		// Flush the current format unit to the image
		if (m_nCurrentFormatUnit != funum) {
			if (m_nCurrentFormatUnit != NONE) {
				if (m_bDirty) {
					try {
						// System.out.println("Write back format unit " + m_nCurrentFormatUnit + " on " + m_sFileName);
						writeCurrentFormatUnit();
					}
					catch (ProtectedException px) {
						System.err.println(TIImageTool.langstr("ImageFWP"));
					}
				} /*
				else {
					System.out.println("Evict format unit " + m_nCurrentFormatUnit);
				} */
			}
			// System.out.println("Load format unit " + funum + " from file");
			byte[] abyFU = new byte[getFormatUnitLength(funum)];
			m_codec.setBuffer(abyFU);
			m_nCurrentFormatUnit = funum;
			if (m_bInitial) {
				// System.out.println("Create FU " + funum);
				m_codec.prepareNewFormatUnit(funum, getFormatUnitParameters());
			}
			else {
				long pos = getFormatUnitPosition(funum);
				if (pos >= 0) {
					m_file.seek(pos);
					m_file.readFully(abyFU);
				}
				else 
					m_codec.loadEmptyFormatUnit();
				// System.out.println(Utilities.hexdump(abyFU));
			}
			m_codec.decode();
		} 
		else {
			// System.out.println("Format unit " + funum + " in memory");
		} 
	}
	
	/** Reads a sector.
		@throws ImageException if the sector cannot be found.
	*/
	public Sector readSector(int nSectorNumber) throws ImageException, IOException {
		Sector sect = null;
		// System.out.println("read Sector " + nSectorNumber);
		// For very early accesses, in particular with RawHDFormat
		if ((nSectorNumber == 0) && (getFormatUnitLength(0)<=0)) {
			// System.out.println("Read sector 0 directly");
			sect = readSector0();
		}
		if (sect != null) 
			return sect;

		sect = m_writeCache.read(nSectorNumber);
		
		if (sect == null) {
			// Otherwise, determine the format unit of this sector
			// System.out.println("nSecNum = " + nSectorNumber);
			int funum = getFUNumberFromSector(nSectorNumber); // throws ImageException
			loadFormatUnit(funum);
				
			ImageSector isect = findSector(nSectorNumber); // throws ImageException
			if (isect == null) throw new ImageException(String.format(TIImageTool.langstr("SectorNotFound"), nSectorNumber));
			sect = isect;
			// System.out.println(Utilities.hexdump(sect.getData()));
		}
		return sect;	
	}
	
	/** Writes a sector.
	*/
	public void writeSector(Sector sect) {
		m_writeCache.write(sect);
	}	
	
	/** Write back the sector. This means to load the respective format unit,
	    possibly writing back the last format unit first, then modifying
	    the sector contents. The format unit will be written lazily, i.e.
	    at the end, or when another format unit is loaded.
	*/
	void writeBack(Sector sect) throws ImageException, IOException, ProtectedException {
		// Determine format unit
		int secnum = sect.getNumber();
		int funum = getFUNumberFromSector(secnum); // throws ImageException
		loadFormatUnit(funum);
		m_nCurrentFormatUnit = funum;		
		ImageSector isect = findSector(secnum);
		if (isect == null) throw new ImageException(String.format(TIImageTool.langstr("SectorNotFound"), secnum));
		isect.modify(sect.getData());	
		// if (!m_bDirty) System.out.println("Set dirty on " + m_sFileName + ", FU " + m_nCurrentFormatUnit);
		m_bDirty = true;
	}
	
	/** Saves all changed sectors to the image. No format change. */
	public void saveImage() throws ImageException, IOException, ProtectedException {
		reopenForWrite();
		for (int i=0; i < getTotalSectors(); i++) {
			Sector sect = m_writeCache.read(i);
			if (sect != null) {
				// System.out.println("Write back sector "  + sect.getNumber());  // #%
				writeBack(sect);
			}
		}
		// Write back the last format unit, which has not yet been committed 
		// System.out.println("Write back current format unit at end");
		writeCurrentFormatUnit();
		
		m_writeCache.setCheckpoint();
		m_writeCache.nextGeneration(true);
		reopenForRead();
	}
	
	/** Save from the old image to this image. 
	    This is done by reading a sector from the old source image
	    and writing it to this target image for each sector from 0 to max.
	    By this method, new images do not support any copy protection.
	*/
	public void saveImageFromOld(ImageFormat imgOld) throws ImageException, IOException {
		m_nCurrentFormatUnit = NONE;
		try {
			reopenForWrite();
			System.out.println("Write back " + getTotalSectors() + " sectors");
			for (int i=0; i < getTotalSectors(); i++) {
				Sector sect = imgOld.readSector(i);
				// System.out.println("Write back sector "  + sect.getNumber());  // #%
				writeBack(sect);
			}
			// Write back the last format unit, which has not yet been committed 
			writeCurrentFormatUnit();
			
			m_writeCache.setCheckpoint();
			m_writeCache.nextGeneration(true);
			m_bInitial = false;
			reopenForRead();
		}
		catch (ProtectedException px) {
			px.printStackTrace();  // should not happen
		}
	}	
	
	/** Writes back the current format unit. */
	void writeCurrentFormatUnit() throws IOException, ProtectedException {
		m_codec.encode();
		prepareFormatUnitWrite();
		
		long offset = getFormatUnitPosition(m_nCurrentFormatUnit);
		if (offset >= 0) {
			// System.out.println("write format unit " + m_nCurrentFormatUnit + " at position " + offset);  // #%
			// reopenForWrite();
			m_file.seek(offset);
			m_file.write(m_codec.getFormatUnitBuffer());
			// System.out.println(Utilities.hexdump(m_codec.getFormatUnitBuffer()));
			// reopenForRead();
		}
		else {
			// Maybe the format unit was filled with zeros, and nothing has changed
			// System.out.println("Not writing format unit " + m_nCurrentFormatUnit);
		}
		// if (m_bDirty) System.out.println("Clean dirty on " + m_sFileName + ", FU " + m_nCurrentFormatUnit);
		m_bDirty = false;
	}
	
	public void reopenForWrite() throws IOException, ProtectedException {
		try {
			if (m_file != null) m_file.close();
			// System.out.println("reopenForWrite(" + m_sFileName + ")");
			m_file = new RandomAccessFile(m_sFileName, "rw");
		}
		catch (FileNotFoundException fnfx) {
			throw new ProtectedException(m_sFileName, ProtectedException.IMAGE);
		}
	}
	
	public void reopenForRead() throws IOException {
		if (m_file != null) m_file.close();
		// System.out.println("reopenForRead(" + m_sFileName + ")");
		m_file = new RandomAccessFile(m_sFileName, "r");		
	}
	
	/** Gets the format unit number from the linear sector number. */
	abstract int getFUNumberFromSector(int nSectorNumber) throws ImageException;	

	/** Gets the offset in the file of the requested format unit. */
	abstract long getFormatUnitPosition(int number);

	/** Gets the length of the format unit. */
	abstract int getFormatUnitLength(int number);

	/** Finds the sector in the format unit. */
	// Each FIB is read twice: for the file name, and for the file contents
	ImageSector findSector(int number) throws ImageException {
		// System.out.println("find " + number);
		for (ImageSector is : m_codec.getDecodedSectors()) {
			// System.out.println("- number " + is.getNumber());
			if (is.getNumber() == number) {
				return is;
			}
		}
		return null;
	}
	
	abstract void prepareNewImage(FormatParameters param) throws IOException, ImageException;
	
	abstract FormatUnitParameters getFormatUnitParameters();
	
	@Override
	public String getImageName() {
		return m_sFileName;
	}
	
	@Override
	public String getShortImageName() {
		return m_sFileName.substring(m_sFileName.lastIndexOf(java.io.File.separator)+java.io.File.separator.length());
	}
	
	Sector readSector0() throws IOException {
		return null;
	}
	
	void prepareFormatUnitWrite() throws IOException {
		// Do nothing by default
	}
	
	/** May be overridden by subclasses. */
	void setFormatUnitLength(int len) {
		// Do nothing by default
	}
	
	abstract public boolean isPartitioned() throws ImageException, IOException;
	
	public Partition[] getPartitionTable() {
		return m_partition;
	}

	public int getActivePartition() {
		return m_nActivePartition;
	}
		
	public void setPartition(int part) {
		// System.out.println("Selected partition " + (part+1));
		m_nActivePartition = part;
		m_nCurrentFormatUnit = NONE;
	}
}
