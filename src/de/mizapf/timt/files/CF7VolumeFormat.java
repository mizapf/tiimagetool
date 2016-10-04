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
    
    Copyright 2016 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.files;

import java.io.RandomAccessFile;
import java.io.FileOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.io.File;

class CF7VolumeFormat extends ImageFormat {

	static int vote(RandomAccessFile fileSystem) throws IOException {
		long nLength = fileSystem.length();
		return (nLength == 409600)? 100 : 0;
	}

	CF7VolumeFormat(RandomAccessFile filesystem, String sImageName, int nSectorLength) throws IOException, ImageException {
		super(filesystem, sImageName, nSectorLength);
	}

	CF7VolumeFormat() {
	}

	public String getDumpFormatName() {
		return "CF7 volume";
	}
	
	@Override	
	void setGeometry(boolean bSpecial) throws IOException, ImageException {
		m_nSectorsByFormat = NONE;
		m_currentCylinder = NONE;
		m_currentTrack = NONE;
		m_currentHead = NONE;
		m_positionInTrack = 0;
		
		m_nHeads = 2;
		m_nCylinders = 40;
		m_nSectorsPerTrack = 20;
		m_nSectorsByFormat = 20;
		m_nSectorLength = 256;
	}
	
	@Override
	int readTrack(int nSectorNumber) throws IOException, ImageException {
		int secindex = -1;
		return secindex;
	}	
	
	/** We return the cached sector.
	*/
	@Override
	Sector readSector(int nSectorNumber) throws EOFException, IOException, ImageException {
		if (nSectorNumber >= 1600) throw new ImageException("Bad sector number: " + nSectorNumber); 
		int secindex = readTrack(nSectorNumber);
		if (secindex != NONE) {
			return m_sector[secindex];
		}
		else throw new ImageException("Sector " + nSectorNumber + " not found");
	}

	@Override
	void writeSector(int nSectorNumber, byte[] abySector, boolean bNeedReopen) throws IOException, ImageException {
	}
	
	void flush() throws IOException {
	}

	@Override
	public void reopenForWrite() throws IOException {
		// Don't do anything here
	}
	
	@Override
	public void reopenForRead() throws IOException {
		// Don't do anything here
	}	
	// ===========================================================
	// Formatting
	// ===========================================================
	
	void formatTrack(int cylinder, int head, int density, int[] gap) {
	}
	
	public void createEmptyImage(File newfile, int sides, int density, int cylinders, boolean format) throws FileNotFoundException, IOException {
		// Write the resulting image
		FileOutputStream fos = new FileOutputStream(newfile);
		// fos.write(image, 0, image.length);
		fos.close();
	}
}

