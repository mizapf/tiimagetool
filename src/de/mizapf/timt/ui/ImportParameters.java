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
    
    Copyright 2024 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

class ImportParameters {
	boolean asBasic;
	boolean asText;
	boolean asWideText;
	boolean binary;
	boolean other;
	String fileName;
	int basicVersion;
	int saveFormat;
	boolean protect;
	byte flags;
	int  recordLength;
	
	ImportParameters(boolean bBasic, boolean bText, boolean bWide,
		boolean bBinary, boolean bOther,
		String sFile, int nBasic, int nFormat, boolean bProtect, byte byFlags,
		int recLen) {
		asBasic = bBasic;
		asText = bText;
		asWideText = bWide;
		binary = bBinary;
		other = bOther;
		fileName = sFile;
		basicVersion = nBasic;
		saveFormat = nFormat;
		protect = bProtect;
		flags = byFlags;
		recordLength = recLen;
	}
	
	ImportParameters() {
		asBasic = false;
		asText = false;
		asWideText = false;
		binary = false;
		other = false;
		fileName = null;
		basicVersion = 0;
		saveFormat = 0;
		protect = false;
		flags = (byte)0;
		recordLength = 0;
	}
}