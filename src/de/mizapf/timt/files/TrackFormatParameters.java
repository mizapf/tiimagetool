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
    
    Copyright 2023 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.files;

public class TrackFormatParameters {
	boolean mfm;
	int sectors;
	int gap0;
	int gap1;
	int gap2;
	int gap3;
	int gap4;
	int gapbyte;
	int gap1byte;
	int sync;
	int sync1;
	byte[] fillpattern;
	
	TrackFormatParameters(int[] param, int sec, byte[] fill) {
		mfm = (sec > 9);
		sectors = sec;
		gap0 = param[0];
		gap1 = param[1];
		gap2 = param[2];
		gap3 = param[3];
		gap4 = param[4];
		gapbyte = param[5];
		gap1byte = param[6];
		sync = param[7];
		sync1 = param[8];
		fillpattern = fill;
	}
}
