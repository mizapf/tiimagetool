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
    
    Copyright 2013 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.assm;
import java.util.*;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.FormatException;

/** A symbol has a value and a name. The default name for the symbol is
	the hex value as a string, starting with a ">".
	An absolute symbol has a value as given. A relocatable symbol
	has a value that depends on the location counter. 
*/
class Symbol {
	int		m_nValue;
	String 	m_sName;
	int 	m_nMode;		
	int 	m_nSegment;
	boolean m_bReferenced;
	int		m_nOffset;
	
	Symbol(int nValue) {
		this(Assembler.ABSOLUTE, nValue);
	}
	
	Symbol(int nMode, int nValue) {
		m_nMode = nMode;
		m_nValue = nValue;
		m_nSegment = 0;
		m_sName = null;
		m_bReferenced = false;
		m_nOffset = 0;
	}
	
	void setSegment(int nSegment) {
		m_nSegment = nSegment;
	}
	
	void setName(String sName) {
		m_sName = sName;
	}
	
	void setOffset(int nOffset) {
		m_nOffset = nOffset;
	}
	
	void setReferenced(boolean bRef) {
		m_bReferenced = bRef;
	}
	
	boolean isReferenced() {
		return m_bReferenced;
	}
	
	boolean isAbsolute() {
		return m_nMode==Assembler.ABSOLUTE;
	}
	
	boolean isProgramRelocatable() {
		return m_nMode==Assembler.PRELOC;
	}
	
	boolean isDataRelocatable() {
		return m_nMode==Assembler.DRELOC;
	}
	
	boolean isCommonRelocatable() {
		return m_nMode==Assembler.CRELOC;
	}
	
	int getMode() {
		return m_nMode;
	}
	
	int getSegment() {
		return m_nSegment;
	}
	
	int getValue() {
		return m_nValue;
	}
	
	String getName() {
		return m_sName;
	}
	
	public Location toLocation() {
	    return new Location(m_nMode, m_nValue, m_nSegment);
	}
	
	public String toStringValue() {
		StringBuilder sb = new StringBuilder();
		sb.append(Utilities.toHex(m_nValue, 4, true));
		if (isProgramRelocatable()) sb.append("'");
		if (isCommonRelocatable()) {
			sb.append("+");
			if (m_nSegment > 1) sb.append(m_nSegment);
		}
		if (isDataRelocatable()) sb.append("\"");
		return sb.toString();
	}
	
	public String toString() {
		if (m_sName!=null) {
			return m_sName;
		}
		return asLocation(m_nValue, m_nMode, m_nSegment, ">", false, m_nOffset);
	}
	
	public String toString(String sAbsPrefix) {
		if (m_sName!=null) {
			return m_sName;
		}
		return asLocation(m_nValue, m_nMode, m_nSegment, sAbsPrefix, false, m_nOffset);
	}
	
	public static String asLocation(int nValue, int nMode, int nSegment, String sAbsPrefix, boolean bTrailing, int nOffset) {
		StringBuilder sb = new StringBuilder();
		switch (nMode) {
		case Assembler.ABSOLUTE:
			if (sAbsPrefix != null) sb.append(sAbsPrefix);
			break;
		case Assembler.PRELOC:
			sb.append("R");
			break;
		case Assembler.DRELOC:
			sb.append("T");
			break;
		case Assembler.CRELOC:
			sb.append("S");
			if (nSegment > 1) sb.append((char)('A' + nSegment - 2));
			break;
		}
		sb.append(Utilities.toHex(nValue-nOffset, 4, true));
		
		if (nOffset != 0) {
			if (nOffset < 0) sb.append("-").append(-nOffset);
			else sb.append("+").append(nOffset);
		}
		
		if (nMode==Assembler.ABSOLUTE & bTrailing) sb.append(":");
		return sb.toString();
	}
}
