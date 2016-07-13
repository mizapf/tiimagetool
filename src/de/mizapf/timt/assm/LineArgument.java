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

class LineArgument {
	int 	m_nAddrType;
	int 	m_nValue;
	String  m_sValue;
	Symbol	m_sym;
	boolean m_bQuotes;
	String[] m_asValue;
	String  m_sHexJump = "X";
	
	LineArgument(int nAddrType, int nValue) {
		m_nValue = nValue;
		m_nAddrType = nAddrType;
		m_sym = new Symbol(Assembler.ABSOLUTE, nValue);
		m_sValue = null;
		m_asValue = null;
	}
	
	LineArgument(Symbol symb) {
		this(symb, 0);
	}
	
	LineArgument(Symbol symb, int reg) {
		m_sym = symb;
		m_nValue = reg;
		m_nAddrType = (reg==0)? Assembler.T_SYM : Assembler.T_INDEXED;
		m_sValue = null;
		m_asValue = null;
	}
	
	/** Used for directives with strings as argument. */
	LineArgument(String s, boolean bQuotes) {
		m_sym = null;
		m_sValue = s;
		m_bQuotes = bQuotes;
		m_asValue = null;
	}
	
	LineArgument(String[] as) {
		m_sym = null;
		m_sValue = null;
		m_asValue = as;
	}
		
	public void setAddressingType(int nAddrType) {
		m_nAddrType = nAddrType;
	}
	
	public int getAddressingType() {
		return m_nAddrType;
	}
	
	public Symbol getSymbol() {
		return m_sym;
	}
	
	public void setJumpPrefix(String s) {
		m_sHexJump = s;
	}
	
	public Location getLocation() {
	    return m_sym.toLocation();
	}
	
	public int getArrayLength() {
		if (m_asValue == null) return 0;
		return m_asValue.length;
	}
	
	public String toValue() {
		StringBuilder sb = new StringBuilder();
		if (m_sym!=null) { 
			sb.append(Utilities.toHex(m_sym.getValue(), 4, true));
			if (m_sym.isProgramRelocatable()) sb.append("'");
			if (m_sym.isCommonRelocatable()) sb.append("+");
			if (m_sym.isDataRelocatable()) sb.append("\"");
		}
		else sb.append("<null>");
		return sb.toString();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int nVal = 0;
		
		if (m_asValue != null) {
			for (int i=0; i < m_asValue.length; i++) {
				if (sb.length()>0) sb.append(",");
				sb.append(m_asValue[i]);
			}
			return sb.toString();
		}
		
		if (m_sValue != null) {
			if (m_bQuotes) sb.append("'");
			sb.append(m_sValue);
			if (m_bQuotes) sb.append("'");
			return sb.toString();
		}
		
		switch (m_nAddrType) {
		case Assembler.T_REG:
			sb.append("R").append(m_nValue);
			break;
		case Assembler.T_REGIND:
			sb.append("*R").append(m_nValue);
			break;
		case Assembler.T_SYM:
			sb.append("@").append(m_sym.toString());
			break;
		case Assembler.T_INDEXED:
			sb.append("@").append(m_sym.toString()).append("(R").append(m_nValue).append(")");
			break;
		case Assembler.T_REGINDINC:
			sb.append("*R").append(m_nValue).append("+");
			break;
		case Assembler.T_IMM:
			if (m_sym==null) sb.append("<null>"); 
			else sb.append(m_sym.toString());
			break;
		case Assembler.T_COUNT:
			sb.append(m_nValue);
			break;
		case Assembler.T_JUMP:
			sb.append(m_sym.toString(m_sHexJump));
			break;
		case Assembler.T_DATA:
			sb.append(m_sym.toString());
			break;
		case Assembler.T_BYTE:
		    sb.append(">").append(Utilities.toHex(m_nValue, 2));
		    break;
		case Assembler.T_IMMREL:
			nVal = m_sym.getValue();
			sb.append("$");
			if (nVal < 0) sb.append("-").append(-nVal);
			else {
				if (nVal > 0) sb.append("+").append(nVal);
			}
			break;
		default:
			sb.append("<invalid>");
			break;
		}
		return sb.toString();
	}
	
	public boolean hasValue() {
		return (m_nAddrType==Assembler.T_SYM || m_nAddrType==Assembler.T_INDEXED || m_nAddrType==Assembler.T_IMM);
	}
	
	public boolean hasSymbol() {
		return ((m_sym != null && !m_sym.isAbsolute()) || m_nAddrType==Assembler.T_JUMP);
	}
}
