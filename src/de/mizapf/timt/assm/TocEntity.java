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

class TocEntity {
	
	// Kinds
	public final static int IDENT = 1;
	public final static int ENTRY = 2;
	public final static int REF = 3;
	public final static int DEF = 4;
	public final static int CHECKSUM = 5;
	public final static int ADDRESS = 6;
	public final static int DATA = 7;
	public final static int BSS = 8;
	public final static int SEG = 9;
	public final static int END = 10;
	
	private int 	m_nValue;
	private String 	m_sValue;
	private int 	m_nAddressType;
	private int 	m_nLocation;
	private int 	m_nKind;
	private Symbol 	m_sym;
	private int 	m_nSegment;
	
	private boolean m_bReferenced;
	
	private Location m_Location;
	
	TocEntity() {
		m_bReferenced = false;
		m_nSegment = 0;
	}
	
	/**
	@param nKind IDENT, ..., END
	@param nType ABSOLUTE, PRELOC, CRELOC, DRELOC
	*/
	TocEntity(int nKind, int nAddressType, int nSegment, int nVal, String sValue) {
		m_nAddressType = nAddressType;
		m_nValue = nVal;
		m_nKind = nKind;
		m_sValue = sValue;
		m_bReferenced = false;
		m_sym = new Symbol(nAddressType, nVal);
		m_sym.setSegment(nSegment);
		m_nSegment = nSegment;
		m_Location = new Location(nAddressType, m_nValue, m_nSegment);
	}
	
	void setSegment(int nSeg) {
		m_nSegment = nSeg;
	}
	
	int getKind() {
		return m_nKind;
	}
	
	int getSegment() {
		return m_nSegment;
	}
	
	void setReferenced() {
		m_bReferenced = true;
	}
	
	Location getLocation() {
		return m_Location;
	}
	
	boolean isOfKind(int kind) {
		return m_nKind == kind;
	}
	
	boolean hasAddressingType(int type) {
		return m_nAddressType == type;
	}
	
	String getStringValue() {
		return m_sValue;
	}
	
	Symbol getSymbol() {
		return m_sym;
	}
	
	private String getQualifier() {
		StringBuilder sb = new StringBuilder(" ");
		if (m_nAddressType == Assembler.ABSOLUTE) sb.append("(abs)");
		else {
			if (m_nAddressType == Assembler.PRELOC) sb.append("(pseg)");
			else {
				if (m_nAddressType == Assembler.CRELOC) sb.append("(cseg ").append(m_nSegment).append(")");
				else  sb.append("(dseg)");
			}
		}
		return sb.toString(); 
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		// if (m_bReferenced) sb.append(m_Location.toString()).append("  ");
		// else sb.append("       ");
		switch (m_nKind) {
		case IDENT:
			sb.append("IDT  '").append(m_sValue).append("'");
			break;
		case ENTRY:
			sb.append("Entry ");
			sb.append(Utilities.toHex(m_nValue,4));
			sb.append(getQualifier());	
			break;
		case CHECKSUM:
			sb.append("Checksum = ").append(Utilities.toHex(m_nValue,4));
			break;
		case ADDRESS:
			sb.append("Address ").append(Utilities.toHex(m_nValue,4));
			sb.append(getQualifier());
			break;
		case DATA:
			sb.append("Data ").append(Utilities.toHex(m_nValue,4));
			sb.append(getQualifier());
			break;
		case DEF:
			sb.append("Definition ");
			sb.append(m_sValue).append(" = ").append(Utilities.toHex(m_nValue,4));
			sb.append(getQualifier());		
			break;
		case REF:
			sb.append("Reference ");
			sb.append(m_sValue).append(" = ").append(Utilities.toHex(m_nValue,4));
			sb.append(getQualifier());
			break;
		case SEG:
			if (m_nSegment==0) sb.append("Data segment");
			else {
				sb.append("Common segment");
				if (m_nValue>1) sb.append(" '").append(m_sValue).append("'");
				sb.append(", id=").append(m_nSegment); 
			}
			sb.append(", length=").append(Utilities.toHex(m_nValue, 4));
			break;
		case END:
			sb.append("END");
			break;
		}
		return sb.toString();
	}
}

