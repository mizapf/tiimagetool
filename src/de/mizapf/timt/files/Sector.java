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

public class Sector implements Cloneable {
	byte[] m_content = null;

	/** Linear number of the sector in the file system. */
	int m_nNumber;

	/** Generation of this sector. */
	int m_generation;

	/** Creates a new sector instance with a private content. */
	public Sector(int nNumber, byte[] abySector) {
		this(nNumber, abySector, 0);
	}
	
	public Sector(int nNumber, byte[] abyFormatUnit, int offset) {
		m_content = new byte[TFileSystem.SECTOR_LENGTH];
		System.arraycopy(abyFormatUnit, offset, m_content, 0, m_content.length);
		m_nNumber = nNumber;
		m_generation = 0;
	}

	public Object clone() {
		byte[] content = new byte[m_content.length];
		System.arraycopy(m_content, 0, content, 0, m_content.length);
		Sector sect = new Sector(m_nNumber, content);
		sect.setGeneration(m_generation);
		return sect;
	}
	
	public void setGeneration(int gen) {
		m_generation = gen;
	}
	
	public byte[] getData() {
		return m_content;
	}
	
	public int getNumber() {
		return m_nNumber;
	}
	
	public void modify(byte[] abyNewContent) {
		System.arraycopy(abyNewContent, 0, m_content, 0, abyNewContent.length);
	}
	
	public int getGeneration() {
		return m_generation;
	}
}