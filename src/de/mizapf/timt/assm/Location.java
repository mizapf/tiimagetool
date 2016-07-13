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

class Location implements Cloneable {
	int m_nValue;
	int m_nAddressType;
	int m_nSegment;
	
	List<String> m_lstLabels; 
	
	Location(int nType) {
		m_nAddressType = nType;
		m_nValue = 0;
		m_lstLabels = null;
		m_nSegment = 0;
	}
	
	Location(int nValue, int nSegment) {
		m_nAddressType = (nSegment==0)? Assembler.DRELOC : Assembler.CRELOC;
		m_nValue = nValue;
		m_lstLabels = null;
		m_nSegment = nSegment;			
	}
	
	Location(int nType, int nValue, int nSegment) {
		m_nAddressType = nType;
		m_nValue = nValue;
		m_lstLabels = null;
		m_nSegment = nSegment;			
	}
	
	Location getLocationAfter(int nOffset) {
	    return new Location(m_nAddressType, m_nValue+nOffset, m_nSegment);
	}
	
	/*
	    Formats: 
	    absolute: Xaaaa, 0xaaaa, >aaaa, aaaa
	    preloc: Raaaa
	    creloc: Saaaa or Sbaaaa
	    dreloc: Taaaa
	*/
	static Location getInstance(String sText) throws FormatException {
	    int nValue = 0;
	    int nAddressType = 0;
	    int nSegment = 0;
	    try {
	        if (sText.length()==4) {
	            nValue = Integer.parseInt(sText, 16);
	            nAddressType = Assembler.ABSOLUTE;
	        }
	        else {
	            switch (sText.charAt(0)) {
	            case 'X':
	            case '>':
	                nValue = Integer.parseInt(sText.substring(1,5), 16);
	                nAddressType = Assembler.ABSOLUTE;
	                break;
                case '0':
                    if (sText.charAt(1)=='x') {
                       nValue = Integer.parseInt(sText.substring(2,6), 16);
                       nAddressType = Assembler.ABSOLUTE;
                    }
                    else throw new FormatException(sText, "invalid prefix");
	                break;
	            case 'R':
	                nValue = Integer.parseInt(sText.substring(1,5), 16);
	                nAddressType = Assembler.PRELOC;
	                break;
	            case 'S':
	                if (sText.length()>5) {
	                    nSegment = (int)(sText.charAt(1))-'A'+2;
	                    nValue = Integer.parseInt(sText.substring(2,6), 16);
	                }
	                else {
	                    nSegment = 1;  // blank
	                    nValue = Integer.parseInt(sText.substring(1,5), 16);
	                }
	                nAddressType = Assembler.CRELOC;
	                break;
	            case 'T':
	                nValue = Integer.parseInt(sText.substring(1,5), 16);
	                nAddressType = Assembler.PRELOC;
	                break;
	            default:
	                throw new FormatException(sText, "invalid prefix");
	            }
	        }
	        return new Location(nAddressType, nValue, nSegment);
	    }
	    catch (NumberFormatException nfx) {
	        throw new FormatException(sText, "invalid format");
	    }
	}
	
	int getAddressingType() {
		return m_nAddressType;
	}
	
	void setSegment(int nSegment) {
		m_nSegment = nSegment;
	}
	
	int getSegment() {
		return m_nSegment;
	}
	
	void add(int offset) {
		m_nValue += offset;
	}
	
	void setValue(int addr) {
		m_nValue = addr;
	}
	
	int getValue() {
		return m_nValue;
	}
	
	boolean isHigherOrEqual(Location cnt) {
	    if (getAddressingType() != cnt.getAddressingType()) return false;
	    if (getSegment() != cnt.getSegment()) return false;
	    return (m_nValue >= cnt.getValue());
	}

	boolean isLowerOrEqual(Location cnt) {
	    if (getAddressingType() != cnt.getAddressingType()) return false;
	    if (getSegment() != cnt.getSegment()) return false;
	    return (m_nValue <= cnt.getValue());
	}
	
	void setFrom(Location ct) {
		m_nValue = ct.getValue();
		m_nAddressType = ct.getAddressingType();
		m_nSegment = ct.getSegment();
	}
	
	boolean isSubsequentTo(Location ct, int nLastLen) {
		return ((ct.getValue() == (m_nValue - nLastLen)) && (ct.getAddressingType()==m_nAddressType));  
	}
	
	public String getAddressWithOffset(int nOffset) {
		return Symbol.asLocation(m_nValue + nOffset, m_nAddressType, m_nSegment, null, false, 0);
	}
	
	public String toString() {
		return getAddressWithOffset(0);
	}
	
	public String toAddressLabel(boolean bPlain) {
		return Symbol.asLocation(m_nValue, m_nAddressType, m_nSegment, bPlain? null:"X" , bPlain, 0);
	}

	public void addLabel(String sLabel) {
		if (m_lstLabels==null) m_lstLabels = new ArrayList<String>();
		for (String lab : m_lstLabels) {
			if (lab.equals(sLabel)) return;
		}
		m_lstLabels.add(sLabel);
	}
	
	public boolean hasLabel() {
		return m_lstLabels!=null;
	}
	
	public String[] getLabels() {
		return m_lstLabels.toArray(new String[m_lstLabels.size()]);
	}
	
	public Object clone() {
		Location newc = null;
		try {
			newc = (Location)super.clone();
			newc.setValue(m_nValue);
		}
		catch (CloneNotSupportedException cnsx) {
			cnsx.printStackTrace();
		}
		return newc;
	}
	
	public boolean equals(Object obj) {
		Location other = (Location)obj;
		if ((other.m_nValue != this.m_nValue) || (other.getAddressingType() != this.getAddressingType())) return false;
		if (m_nAddressType != Assembler.CRELOC) return true;
		else return (other.m_nSegment == this.m_nSegment);
	}
	
	public int diffTo(Symbol sym) {
		if (sym.getMode() != m_nAddressType) return Assembler.INFTY;
		if (m_nAddressType==Assembler.CRELOC && m_nSegment != sym.getSegment()) return Assembler.INFTY; 
		return getValue() - sym.getValue();
	}
}
