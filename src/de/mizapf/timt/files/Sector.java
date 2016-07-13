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
	
	Copyright 2011 Michael Zapf
	www.mizapf.de
	
****************************************************************************/

package de.mizapf.timt.files;

import java.io.*;

public class Sector {
	byte[] m_abySector = null;
	int m_nNumber = 0;
	
	public Sector(int nNumber, byte[] abySector) {
		m_nNumber = nNumber;
		m_abySector = abySector;
	}
	
	public int getNumber() {
		return m_nNumber;
	}
	
	public String toString() {
		return dumpBytes(m_abySector);
	}
	
	public static String dumpBytes(byte[] aby) {
		return dumpBytes(aby, 0, aby.length);
	}
	
	public static String dumpBytes(byte[] aby, int nStart, int nEnd) {
		StringBuffer sb = new StringBuffer();
		nStart = nStart & 0xfffffff0;
		nEnd = (nEnd-1) | 0x0000000f;
		for (int nAddress = nStart; nAddress < nEnd; nAddress+=16) {
			String sAddress = Integer.toHexString(nAddress);
			if (sAddress.length()<4) sb.append("0");
			if (sAddress.length()<3) sb.append("0");
			if (sAddress.length()<2) sb.append("0");
			sb.append(sAddress);
			sb.append(": ");
			for (int j=0; j < 16; j++) {
				String sValue = Integer.toHexString((((int)aby[nAddress+j])&0xff));
				if (sValue.length()>2) sValue = sValue.substring(0,2);
				if (sValue.length()<2) sb.append("0");
				sb.append(sValue);
				sb.append(" ");
			}
			for (int j=0; j < 16; j++) {
				if (aby[nAddress+j]>=32) sb.append((char)aby[nAddress+j]);
				else sb.append(".");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public byte[] getBytes() {
		return m_abySector;
	}
}

