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
	protected int m_nVibCheck;

	FormatParameters m_format;

	protected FileImageFormat(String sFileName) throws FileNotFoundException {
		this(sFileName, true);
	}
	
	protected FileImageFormat(String sFileName, boolean bRead) throws FileNotFoundException {
		m_sFileName = sFileName;
		m_file = new RandomAccessFile(sFileName, bRead? "r" : "rw");
		System.out.println("Opening image " + sFileName + ", read = " + bRead); 
		m_nCurrentFormatUnit = NONE;
		m_bInitial = !bRead;
		m_writeCache.setName(getShortImageName());
		if (m_bInitial) prepareNewImage();
	}
	
	protected void loadFormatUnit(int funum) throws ImageException, IOException {
		if (funum < 0) throw new InternalException("Wrong format unit number: " + funum);
		// Flush the current format unit to the image
		if (m_nCurrentFormatUnit != funum) {
			if (m_nCurrentFormatUnit != NONE && m_bDirty) {
				try {
					writeCurrentFormatUnit();
				}
				catch (ProtectedException px) {
					System.err.println(TIImageTool.langstr("ImageFWP"));
				}
			}
			
			byte[] abyFU = new byte[getFormatUnitLength(funum)];
			long offset = getFormatUnitPosition(funum);
			// System.out.println("offset = " + offset + ", funum = " + funum);
			if (m_bInitial) {
				m_codec.prepareNewFormatUnit(funum, abyFU, getFillPattern());
			}
			else {
				m_file.seek(offset);
				m_file.readFully(abyFU);
				// System.out.println(Utilities.hexdump(abyFU));
			}
			m_nCurrentFormatUnit = funum;
			m_bDirty = false;
			m_codec.setBuffer(abyFU);
			m_codec.decode();
		} /*
		else {
			System.out.println("Format unit " + funum + " in memory");
		} */
	}
	
	/** Reads a sector.
		@throws ImageException if the sector cannot be found.
	*/
	public Sector readSector(int nSectorNumber) throws ImageException, IOException {
		// If there is a write cache, try to get the sector from there
		// MemoryImageFormats and FloppyImageFormats always have a write cache		
		Sector sect = null;
		if (m_writeCache != null) {
			sect = m_writeCache.read(nSectorNumber);
		}

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
	public void writeSector(Sector sect) throws ImageException, IOException, ProtectedException {
		// If there is a write cache, write the sector to the write cache.
		if (m_writeCache != null) {
			m_writeCache.write(sect);
		}
		else {
			// Otherwise, write on image
			writeBack(sect);
		}
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
		// System.out.println("writeBack " + secnum);

		m_nCurrentFormatUnit = funum;		
		ImageSector isect = findSector(secnum);
		if (isect == null) throw new ImageException(String.format(TIImageTool.langstr("SectorNotFound"), secnum));
		isect.modify(sect.getData());	
		m_bDirty = true;
	}
	
	/** Saves all changed sectors to the image. No format change. */
	public void saveImage() throws ImageException, IOException, ProtectedException {
		for (int i=0; i < m_fs.getTotalSectors(); i++) {
			Sector sect = m_writeCache.read(i);
			if (sect != null) {
				// System.out.println("Write back sector "  + sect.getNumber());  // #%
				writeBack(sect);
			}
		}
		// Write back the last format unit, which has not yet been committed 
		writeCurrentFormatUnit();
		
		m_writeCache.setCheckpoint();
	}
	
	/** Save from the old image to this image. 
	    This is done by reading a sector from the old source image
	    and writing it to this target image for each sector from 0 to max.
	    By this method, new images do not support any copy protection.
	*/
	public void saveImageFromOld(ImageFormat imgOld) throws ImageException, IOException {
		try {
			for (int i=0; i < m_fs.getTotalSectors(); i++) {
				Sector sect = imgOld.readSector(i);
				// System.out.println("Write back sector "  + sect.getNumber());  // #%
				writeBack(sect);
			}
			// Write back the last format unit, which has not yet been committed 
			writeCurrentFormatUnit();
			
			m_writeCache.setCheckpoint();
			m_bInitial = false;
		}
		catch (ProtectedException px) {
			px.printStackTrace();  // should not happen
		}
	}	
	
	/** Writes back the current format unit. */
	void writeCurrentFormatUnit() throws IOException, ProtectedException {
		m_codec.encode();
		long offset = getFormatUnitPosition(m_nCurrentFormatUnit);
		System.out.println("write format unit " + m_nCurrentFormatUnit + " at position " + offset);  // #%
		reopenForWrite();
		m_file.seek(offset);
		m_file.write(m_codec.getFormatUnitBuffer());
		reopenForRead();
	}
	
	public void reopenForWrite() throws IOException, ProtectedException {
		try {
			if (m_file != null) m_file.close();
			m_file = new RandomAccessFile(m_sFileName, "rw");
		}
		catch (FileNotFoundException fnfx) {
			throw new ProtectedException(m_sFileName, ProtectedException.IMAGE);
		}
	}
	
	public void reopenForRead() throws IOException {
		if (m_file != null) m_file.close();
		m_file = new RandomAccessFile(m_sFileName, "r");		
	}
		
	public int getFormatCheck() {
		return m_nVibCheck;
	}
		
	/** Gets the format unit number from the linear sector number. */
	abstract int getFUNumberFromSector(int nSectorNumber) throws ImageException;	

	/** Gets the offset in the file of the requested format unit. */
	abstract long getFormatUnitPosition(int number);

	/** Gets the length of the format unit. */
	abstract int getFormatUnitLength(int number);

	/** Finds the sector in the format unit. */
	abstract ImageSector findSector(int number) throws ImageException;
	
	abstract void prepareNewImage();
	
	@Override
	public String getImageName() {
		return m_sFileName;
	}
	
	@Override
	public String getShortImageName() {
		return m_sFileName.substring(m_sFileName.lastIndexOf(java.io.File.separator)+java.io.File.separator.length());
	}
}
