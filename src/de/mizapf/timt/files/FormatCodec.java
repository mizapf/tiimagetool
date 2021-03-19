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
import de.mizapf.timt.util.Utilities;

import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;


/** The FormatCodec buffers a set of sectors read from the image or to 
	be written to the image. In particular, it contains a lookup table for
	the positions and lengths of the buffered blocks.
	
	Since the image formats may differ in the kind of block, the codec works
	with the appropriate size of the structure as defined by the subclass
	(buffering a track, a cylinder, or a hunk).
*/	
abstract class FormatCodec {
	
	// Image of the buffer
	protected byte[] m_abyBuffer;
	
	// Empty pattern
	protected byte[] m_abyFill; 
	
	/** Initial creation. There is no image file yet. */
	boolean m_bInitial;
	
	boolean m_bBufferChanged;
	String m_sImageName;	
	int m_nCurrentIndex;
	
	protected int m_nPositionInBuffer;
	
	private final static int NONE = -1;
	
	/** The sectors in this buffer as appearing on the medium. */
	protected List<ImageSector> m_buffsector;
	
	/** Position of the buffer on the image. Indexed by the number of the 
		buffer (track, cylinder, hunk).	*/
	protected int[] m_bufferpos;
		
	/** Length of the buffer on the image. */
	protected int[] m_bufferlen;
	
	/** Reference to the image file. */
	private RandomAccessFile m_ImageFile; 
		
	class ImageSector {
		byte[] m_content;
		byte[] m_crc;
		int m_crcvalue;
		int m_initcrc;
		Location m_loc;
		int m_nImagePosition;
		
		ImageSector(Location loc, byte[] content, byte mark, boolean mfm, int pos) {
			m_content = new byte[TFileSystem.SECTOR_LENGTH];
			m_crc = new byte[2];
			
			byte[] head = { (byte)0xa1, (byte)0xa1, (byte)0xa1, (byte)0xfb };
			head[3] = mark;
			
			int start = mfm? 0 : 3;
			m_initcrc = Utilities.crc16_get(head, start, 4-start, 0); 

			setData(content);
			m_loc = loc;
			m_nImagePosition = pos;
		}		
		
		void setData(byte[] content) {
			System.arraycopy(content, 0, m_content, 0, content.length);
			m_crcvalue = Utilities.crc16_get(m_content, 0, m_content.length, m_initcrc);	
			m_crc[0] = (byte)((m_crcvalue>>8)&0xff);
			m_crc[1] = (byte)(m_crcvalue&0xff);
		}
		
		Location getLocation() {
			return m_loc;
		}
		
		int getImagePosition() {
			return m_nImagePosition;
		}
		
		/** Delivers the head, the content, and the crc. */
		byte[] getData() {
			return m_content;
		}
		
		byte[] getCRCBytes() {
			return m_crc;
		}		
		
		int getCRC() {
			return m_crcvalue;
		}
	}
	
	FormatCodec(String sFile, boolean bInitial) {
		this(null, sFile, bInitial);
	}

	FormatCodec(RandomAccessFile rafile, String sFile, boolean bInitial) {
	//	Thread.currentThread().dumpStack();
		m_ImageFile = rafile; 
		System.out.println("sFile = " + sFile);
		m_sImageName = sFile;
		m_nCurrentIndex = NONE;
		m_buffsector = new LinkedList<ImageSector>();
		m_bInitial = bInitial;
	}

	
	void setInitial(boolean init) {
		m_bInitial = init;
	}
	
	/** Find the index of the sector in the buffer. 
		@param loc CHS location
	*/
	private ImageSector findSector(Location loc) {
		for (ImageSector isect : m_buffsector) {
			if (isect.getLocation().equals(loc)) return isect;
		}
		return null;
	}
	
	void setFile(RandomAccessFile rafile) {
		System.out.println("File set");
		if (rafile == null) System.out.println("is null");
		m_ImageFile = rafile; 
	}
	
	void setFillPattern(byte[] fill) {
		m_abyFill = fill;
	}
	
	/** Fills the buffer from the image. The number must be calculated in the
		ImageFormat subclass.
		@param nNumber Number of this buffer 
	*/
	void loadBuffer(int nNumber) throws IOException {
		if (nNumber == m_nCurrentIndex) {
			// System.out.println("Buffer already loaded");
			return;
		}
		// New buffer
		flush();
		System.out.println("Load buffer " + nNumber);
		
//		System.out.println("Buffer length = " + m_bufferlen[nNumber]);
		m_abyBuffer = new byte[m_bufferlen[nNumber]];
		if (m_bInitial) {
			// Newly created
			createEmptyBuffer(nNumber);
		}
		else {
			// Read from image
			m_ImageFile.seek(m_bufferpos[nNumber]);
			m_ImageFile.readFully(m_abyBuffer);
		}
		m_nCurrentIndex = nNumber;
		// Parse the buffer
		int foundSectorsPerTrack = decodeBuffer();
		// 
		m_bBufferChanged = false;
	}
	
	/** Write the buffer back to the image. Creates the image file if it
		does not exist.
		@param nNumber Number of this buffer
	*/
	void writeBuffer(int nNumber) throws FileNotFoundException, IOException {
		encodeBuffer();
		System.out.println("Write back buffer " + nNumber + " to file " + m_sImageName + " at position " + Utilities.toHex(m_bufferpos[nNumber], 6));
		m_ImageFile = new RandomAccessFile(m_sImageName, "rw");		
		m_ImageFile.seek(m_bufferpos[nNumber]);
		m_ImageFile.write(m_abyBuffer);
		m_ImageFile = new RandomAccessFile(m_sImageName, "r");	
		m_bBufferChanged = false;
	}
	
	/** Produce the byte sequence from the sectors. */
	abstract void encodeBuffer();

	/** Find the sectors and their positions in the buffered bytes. */
	abstract int decodeBuffer();
	
	/** Updates the ImageSector by the contents of the passed Sector. */
	final void writeSector(Location loc, byte[] content) {
		m_bBufferChanged = true;
		ImageSector imgsec = findSector(loc);
		if (imgsec != null) {
			imgsec.setData(content);
		}
		else System.out.println("*** Sector " + loc + " not found");
	}
	
	Sector readSector(Location loc, int nNumber) {
		ImageSector is = findSector(loc);
		if (is != null) {
			return new Sector(nNumber, is.getData());
		}
		return null;
	}
	
	void setBufferParams(int[] pos, int[] len) {
		m_bufferpos = pos;
		m_bufferlen = len;
	}
	
	/*
	Use the same references?
	1. Existing image: Loaded sectors will appear by reference in the versioning
	   cache as the original sectors. Changing their content will also change the
	   history. -> readSector must deliver a clone.
	   
	2. No image: New sectors do not contain any image information until the
	   image is created.
	   
	Result: Do not pass Sector instances to the versioning cache. Also, Sectors
	should not contain other information than their number and data.	
	
	We need Sector instances here in order to find the sectors again (by
	number)
	
	Subclass ImageSector?
	*/
	
	/** Encodes all buffered sectors to be written on the image and writes
	    them on the image. */
	void flush() throws FileNotFoundException, IOException {
		if (m_nCurrentIndex == NONE) {
			// System.out.println("flush: Undefined buffer index");
			return;
		}
		if (m_bBufferChanged) {
			writeBuffer(m_nCurrentIndex);
		}
		else System.out.println("No changes for buffer " + m_nCurrentIndex);		
	}
	
	/** Creates an empty buffer (cylinder, track, hunk). */
	abstract void createEmptyBuffer(int buffernum);
	
	void writeBits(byte[] seq) {
		System.out.println("FIXME: Implement writeBits");
		Thread.currentThread().dumpStack();
	}
}	
