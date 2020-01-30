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
import de.mizapf.timt.TIImageTool;

public class CF7ImageFormat extends ImageFormat {

	String[] m_volume;
	int m_maxVolumes = 0;
	
	static int vote(RandomAccessFile fileSystem) throws IOException {
		if (fileSystem.length() < 409600*2) return 0;  // maximum size?

		byte[] header = new byte[1024];
		fileSystem.seek(0);
		fileSystem.readFully(header);

		// Check for the DSK signature in the first volume
		// Note that only every second byte is used in the CF7 format
		if (header[0x1a] == 'D' && header[0x1c] == 'S' && header[0x1e] == 'K') return 70;
		else return 0;
	}
	
	public CF7ImageFormat(RandomAccessFile filesystem, String sImageName) throws IOException, ImageException {
		super(filesystem, sImageName);
		
		// Find volumes
		byte[] header = new byte[32];
		int offset = 0;
		int volumesize = 1600 * 256;
		int voln = 0;
		
		ArrayList<String> volumeList = new ArrayList<String>();
		
		m_maxVolumes = (int)(filesystem.length() / (409600*2));
		
		while (offset < filesystem.length()) {
			filesystem.seek(offset);
			filesystem.readFully(header);
			
			if (header[0x1a] == 'D' && header[0x1c] == 'S' && header[0x1e] == 'K') {
				StringBuilder sb = new StringBuilder();
				for (int i=0; i < 10; i++) {
					char c = (char)header[i*2];
					if (c < 32 || c > 126) c = '.';
					sb.append(c);
				}
				volumeList.add(voln + ":" + sb.toString());
			}		
			voln++;
			offset += volumesize * 2;
		}
		
		m_volume = new String[volumeList.size()];
		volumeList.toArray(m_volume);
	}

	public String getDumpFormatName() {
		return TIImageTool.langstr("CF7ImageType");
	}
	
	public String[] getVolumes() {
		return m_volume;
	}
	
	CF7VolumeFormat getSubvolume(int number) throws IOException, ImageException {
		return new CF7VolumeFormat(m_FileSystem, m_sImageName, number);
	}
	
	@Override	
	void setGeometry(boolean bSpecial) throws IOException, ImageException {	
	}
	
	@Override
	int readTrack(int nSectorNumber) throws IOException, ImageException {
		return 0;
	}

	@Override
	void formatTrack(int cylinder, int head, int sectors, int density, int[] gap) {
	}	
	
	@Override
	public void flush() throws IOException {
	}
	
	@Override
	void createEmptyImage(File newfile, int sides, int density, int tracks, int sectors, boolean format) throws ImageException, FileNotFoundException, IOException {
	}
	
	@Override
	public void reopenForWrite() throws IOException {
	}
	
	@Override
	public void reopenForRead() throws IOException {
	}

	@Override
	public Sector readSectorFromImage(int nSectorNumber) throws EOFException, IOException, ImageException {
		return null;
	}
	
	@Override
	public void writeSectorToImage(int nNumber, byte[] abySector) throws IOException, ImageException {
	}
}

