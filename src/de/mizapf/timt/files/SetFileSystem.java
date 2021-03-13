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
import de.mizapf.timt.util.NotImplementedException;

/** Represents a file system containing one or more subvolumes. Only used 
    with CF7 images. 
*/	
public class SetFileSystem extends TFileSystem {
	
	public SetFileSystem() {
	}

	Sector[] initialize(FormatParameters param) {
		return null;
	}
		
	@Override
	void setupFromFile(byte[] abySect0, byte[] abyAllocMap, boolean bCheck) throws MissingHeaderException, ImageException {
	}

	@Override
	int getAllocMapStart() {
		return 0;
	}

	@Override
	int getAllocMapEnd() {
		return 0; 		
	}

	@Override
	Sector[] getAllocationMapSectors() {
		return null;
	}

	@Override
	byte[] createVIB() {
		return null;
	}
	
	@Override
	Location lbaToChs(int nSectorNumber) throws ImageException {
		throw new NotImplementedException("lbaToChs");
	}
	
	@Override
	int chsToLba(int cylinder, int head, int sector) {
		throw new NotImplementedException("chsToLba");
	}
}
