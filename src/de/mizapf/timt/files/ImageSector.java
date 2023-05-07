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
	
	Copyright 2016 Michael Zapf
	www.mizapf.de
	
****************************************************************************/

package de.mizapf.timt.files;
import de.mizapf.timt.util.Utilities;

import java.io.*;

/** Contains the sector content as well as the CRC values and the 
	location of the sector in the image. Instances of this class are used in the 
	read cache in the ImageFormat class and are created by the FormatCodec.
*/

public class ImageSector extends Sector {

	byte[] m_crc;    
	int m_crcvalue;    // Content CRC
	int m_initcrc;
	Location m_loc;
	int m_nFormatUnitPosition;
		
	ImageSector(int number, byte[] content) {
		super(number, content);
	}
	
	ImageSector(int number, byte[] content, int offset) {
		super(number, content);
		setPosition(offset);
	}
	
	ImageSector(int number, byte[] content, byte mark, boolean mfm, int pos) {
		this(number, content, pos);
		initCrc(mark, mfm);
		calculateCrc();
	}

	void setLocation(Location loc) {
		m_loc = loc;
	}

	/** Position of the first byte of this sector's content. */
	void setPosition(int pos) {
		m_nFormatUnitPosition = pos;
	}
		
	int getPosition() {
		return m_nFormatUnitPosition;
	}
	
	void initCrc(byte mark, boolean mfm) {
		byte[] head = { (byte)0xa1, (byte)0xa1, (byte)0xa1, (byte)0xfb };
		head[3] = mark;
		int start = mfm? 0 : 3;
		m_initcrc = Utilities.crc16_get(head, start, 4-start, 0xffff); 	
	}
	
	void calculateCrc() {
		m_crc = new byte[2];
		m_crcvalue = Utilities.crc16_get(m_content, 0, m_content.length, m_initcrc);	
		m_crc[0] = (byte)((m_crcvalue>>8)&0xff);
		m_crc[1] = (byte)(m_crcvalue&0xff);
	}
	
	int getCRC() {
		return m_crcvalue;
	}
		
	/** Sets the data and calculates the CRC. */
	public void modify(byte[] content) {
		super.modify(content);
		calculateCrc();
	}
		
	Location getLocation() {
		return m_loc;
	}
}
