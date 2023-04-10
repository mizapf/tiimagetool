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
// import java.io.FileOutputStream;
// import java.io.EOFException;
import java.io.IOException;
// import java.io.FileNotFoundException;
// import java.util.ArrayList;
// import java.io.File;
import de.mizapf.timt.TIImageTool;

public class CF7ImageFormat extends SectorDumpFormat implements PartitionedStorage {
	
	int m_nPartitions;
	int m_nActivePartition;
	
	static int vote(String sFile) throws IOException {
		return 0;
	}
	
	public CF7ImageFormat(String sImageName) throws IOException, ImageException {
		super(sImageName);
	}
	
	public String getFormatName() {
		return TIImageTool.langstr("CF7ImageType");
	}
	
	public int partitionCount() {
		return m_nPartitions;
	}
	
	public void setPartition(int part) {
		m_nActivePartition = part;
	}
	
	public String getPartitionName(int part) {
		return "FIXME";
	}
}

