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

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;

/** Obsolete */

class CF7VolumeFormat extends SectorDumpFormat {

	static int vote(String sFile) throws IOException {
		File fl = new File(sFile);
		long nLength = fl.length();
		return (nLength == 409600)? 100 : 0;
	}
	
	public CF7VolumeFormat(String sImageName) throws IOException, ImageException {
		super(sImageName);
	}

	public String getDumpFormatName() {
		// return m_standalone? TIImageTool.langstr("CF7VolumeType") : TIImageTool.langstr("CF7VolumeType") + " #" + (m_volumeNumber+1);
		return TIImageTool.langstr("CF7VolumeType");
	}
}

