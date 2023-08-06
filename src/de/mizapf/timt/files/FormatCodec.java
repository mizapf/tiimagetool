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
	be written to the image. Subclasses may contain a lookup table for
	the positions and lengths of the buffered blocks.
	
	Since the image formats may differ in the kind of block, the codec works
	with the appropriate size of the structure as defined by the subclass
	(buffering a track, a cylinder, or a hunk).
*/	
abstract class FormatCodec {
	
	byte[] m_formatUnit;

	/** The sectors in this buffer as appearing on the medium. */
	protected List<ImageSector> m_decodedSectors;
	
	FormatCodec() {
		m_formatUnit = null;
		m_decodedSectors = new LinkedList<ImageSector>();
	}
	
	void setBuffer(byte[] buf) {
		m_formatUnit = buf;
	}
	
	ImageSector[] getDecodedSectors() {
		return m_decodedSectors.toArray(new ImageSector[0]);
	}	
	
	byte[] getFormatUnitBuffer() {
		return m_formatUnit;
	}
	
	/** Takes the buffer and creates a new sequence of decoded sectors. */
	abstract void decode();
	
	/** Takes the decoded sectors and creates a new buffer. */
	abstract void encode();
	
	/** Creates a new format unit from scratch. m_formatUnit must have been allocated. */
	abstract void prepareNewFormatUnit(int funum, TrackFormatParameters param);
}	
