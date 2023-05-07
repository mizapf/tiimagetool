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
import de.mizapf.timt.TIImageTool;

class Location {
	int cylinder;
	int head;
	int sector;
	
	Location(int cyl, int hd, int sec) {
		cylinder = cyl;
		head = hd;
		sector = sec;
	}
	
	Location(byte[] abyHeader) {
		cylinder = (int)abyHeader[0];
		head = (int)abyHeader[1];
		sector = (int)abyHeader[2];
	}

	public boolean equals(Object obj) {
		if (obj instanceof Location) {
			Location other = (Location)obj;
			return ((cylinder == other.cylinder) &&
					(head == other.head) &&
					(sector == other.sector));
		}
		return false;
	}
	
	public String toString() {
		return String.format(TIImageTool.langstr("LocationString"), cylinder, head, sector);
	}
}
