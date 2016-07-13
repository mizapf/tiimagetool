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

/*
	Labels can be used in DEF
	Any symbol can be REFed (including immediate values)
	REF (-> location_counter)* -> 0000 abs
*/
class DisassembledLine {
	String 			m_sCommand;
	int 			m_nOpcode;
	Location 		m_address;
	boolean 		m_bReferenced;
	LineArgument[] 	m_aarg;
	int 			m_nType;
	boolean			m_bLabels;
	boolean        m_bShowData;
	boolean        m_bShowLocation;
	
	boolean			m_bPlain;
	
	final static int DL_NORMAL = 0;
	final static int DL_BRANCH = 1;
	final static int DL_DATA = 2;
	final static int DL_PSEUDO = 3;
	final static int DL_JUMP = 4;
	final static int DL_DIRECTIVE = 5;
	final static int DL_TEXT = 6;	
	final static int DL_BYTE = 7;	
		
	int m_nMode; // PSEG, DSEG, CSEG

	private static void debug(Object s) {
		if (Assembler.DEBUG) System.out.println(s);
	}
	
	DisassembledLine(Location count, String sCommand, int nOpcode, LineArgument arg1, LineArgument arg2, int nShowLocation) {
		if (nOpcode == Assembler.C_RT || nOpcode == Assembler.C_NOP) m_nType = DL_PSEUDO;
		else m_nType = DL_NORMAL;
		
		m_aarg = new LineArgument[2];  
		m_sCommand = sCommand;
		m_nOpcode = nOpcode;
		m_address = (Location)count.clone();
		m_bReferenced = false;
		m_aarg[0] = arg1;
		m_aarg[1] = arg2;
		debug("== location=" + m_address + ", opcode=" + Utilities.toHex(nOpcode,5) + ", type = " + m_nType + ", arg1=" + m_aarg[0] + ", arg2=" + m_aarg[1]);
		m_bLabels = true;
		m_bPlain = false;
		m_bShowLocation = ((nShowLocation & Hint.OPT_SHOWLOC)!=0);
		m_bShowData = ((nShowLocation & Hint.OPT_SHOWDATA)!=0);
	}		
	
	DisassembledLine(Location count, int nCommand, LineArgument arg1) {
	    this(count, nCommand, arg1, 0);
	}
	
	/** Constructor for assembler directives. */
	DisassembledLine(Location count, int nCommand, LineArgument arg1, int nShowLocation) {
		m_bShowLocation = ((nShowLocation & Hint.OPT_SHOWLOC)!=0);
		m_bShowData = ((nShowLocation & Hint.OPT_SHOWDATA)!=0);
		m_bReferenced = false;
		m_bLabels = true;
		m_aarg = new LineArgument[2];  
		m_bPlain = false;
		m_aarg[0] = arg1;
		m_aarg[1] = null;
		debug("location=" + count + ", arg1=" + m_aarg[0]);				
        if (count!=null) m_address = (Location)count.clone();
		
		if (nCommand == Assembler.C_DATA) {
			m_nType = DL_DATA;
		}
		else {
		    if (nCommand == Assembler.C_TEXT) {
		        m_nType = DL_TEXT;
		    }
		    else {
		        if (nCommand == Assembler.C_BYTE) {
		            m_nType = DL_BYTE;
		        }
		        else {
		            m_nType = DL_DIRECTIVE;
		            m_nOpcode = nCommand;
		            if ((m_nOpcode == Assembler.C_AORG) || (m_nOpcode == Assembler.C_RORG) || (m_nOpcode == Assembler.C_DEF)
		                || (m_nOpcode == Assembler.C_REF) || (m_nOpcode == Assembler.C_IDT) || (m_nOpcode == Assembler.C_BLANK)
		            || (m_nOpcode == Assembler.C_DSEG) || (m_nOpcode == Assembler.C_DEND) || (m_nOpcode == Assembler.C_CSEG)
		            || (m_nOpcode == Assembler.C_CEND) 
		            )
		            m_bLabels = false;
		        }
		    }
		}
	}
	
	DisassembledLine() {
		m_aarg = new LineArgument[2];  
		m_nType = DL_DIRECTIVE;
		m_nOpcode = Assembler.C_BLANK;
		m_bLabels = false;			
		m_bPlain = false;
		m_bShowLocation = false;
		m_bShowData = false;
	}
	
	void setReferenced() {
		m_bReferenced = true;
	}
	
	void addLabel(String sLabel) {
		debug("add " + sLabel);
		m_address.addLabel(sLabel);
		m_bReferenced = true;
	}
	
	Location getLocation() {
		return m_address;
	}
	
	void setLocation(Location count) {
	    if (count!=null) m_address = (Location)count.clone(); 
	}
	
	Location getLocation(int nOffset) {
		Location ct = (Location)m_address.clone();
		ct.add(2);
		if (nOffset==2 && m_aarg[0].hasValue()) ct.add(2); 
		return ct;
	}
	
	int getLength() {
		if (m_nType == DL_DIRECTIVE) {
			if (m_nOpcode == Assembler.C_BSS) return m_aarg[0].getSymbol().getValue();
			else return 0;
		}
		if (m_nType == DL_DATA || m_nType == DL_PSEUDO) return 2;
		
		int nLength = 2;
		if (m_aarg[0] != null && (m_aarg[0].hasValue())) nLength += 2;
		if (m_aarg[1] != null && (m_aarg[1].hasValue())) nLength += 2;
		
		return nLength;
	}
	
	boolean isBranchToAddressCommand() {
		return m_nType==DL_BRANCH;
	}

	boolean isJumpToAddressCommand() {
		return m_nType==DL_BRANCH;
	}
	
	boolean isDataLine() {
		return m_nType==DL_DATA;
	}
	
	boolean isDirective() {
		return m_nType==DL_DIRECTIVE;
	}
	
	boolean isBSS() {
		return m_nOpcode == Assembler.C_BSS;
	}
	
	boolean mayHaveLabels() {
		return m_bLabels;
	}
		
	void toDataLine() {
		m_nType = DL_DATA;
		m_aarg[1] = null;
		m_bReferenced = false;
		Symbol val1 = new Symbol(m_nOpcode);
		LineArgument la = new LineArgument(val1);
		la.setAddressingType(Assembler.T_DATA);
		m_aarg[0] = la; 
	}
	
	LineArgument getLineArgument(int nPos) {
		return m_aarg[nPos];
	}
	
	boolean isJump() {
		int nComm = (m_nOpcode >> 8)&0xff; 
		return (nComm >= 0x10 && nComm <= 0x1c); 
	}
	
	void setBranchToAddressCommand() {
		if (m_nType != DL_PSEUDO) m_nType = DL_BRANCH;
	}

	void setJumpToAddressCommand() {
		if (m_nType != DL_PSEUDO) m_nType = DL_JUMP;
	}
	
	boolean isBlankLine() {
		return m_nOpcode == Assembler.C_BLANK;
	}
	
	boolean isTextLine() {
		return m_nType == DL_TEXT;
	}
	
	void setDumpAddressMode(boolean bPlain) {
		m_bPlain = bPlain;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		int nWidth = 0;
		
		// Label(s)
		if (m_bReferenced) {
			if (m_address.hasLabel()) {
				for (String sLabel : m_address.getLabels()) {
					if (sb.length()>0) sb.append("\n");
					sb.append(sLabel);
					nWidth = sLabel.length();
				}
			}
			else {
				sb.append(m_address.toAddressLabel(m_bPlain));
				nWidth = sb.length();
			}
		}
		int nDiff = sb.length() - nWidth;
		
		// Tab to command
		// Note that if we have multiple labels, the sb length is not
		// only the column of the last line
		for (int i=nWidth; i < 10; i++) sb.append(" ");
		
		String sCommand = null;
		// Command
		switch (m_nType) {
		case DL_NORMAL:
		case DL_BRANCH:
		case DL_JUMP:
			sCommand = m_sCommand;
			break;
		case DL_DATA:
			sCommand = "DATA";
			break;
		case DL_TEXT:
			sCommand = "TEXT";
			break;
		case DL_BYTE:
			sCommand = "BYTE";
			break;
		case DL_PSEUDO:
			if (m_nOpcode == Assembler.C_RT) sCommand = "RT  ";
			else {
				if (m_nOpcode == Assembler.C_NOP) sCommand = "NOP ";
				else sCommand = "<unknown>";
			}
			break;
		case DL_DIRECTIVE:
			
			switch (m_nOpcode) {
			case Assembler.C_BSS:
				sCommand = "BSS ";
				break;
			case Assembler.C_END:
				sCommand = "END ";
				break;
			case Assembler.C_AORG:
				sCommand = "AORG";
				break;
			case Assembler.C_RORG:
				sCommand = "RORG";
				break;
			case Assembler.C_IDT:
				sCommand = "IDT ";
				break;
			case Assembler.C_DEF: 
				sCommand = "DEF ";
				break;
			case Assembler.C_REF:
				sCommand = "REF ";
				break;
			case Assembler.C_BLANK:
				sCommand = "";
				break;
			case Assembler.C_DSEG:
				sCommand = "DSEG";
				break;
			case Assembler.C_DEND:
				sCommand = "DEND";
				break;
			case Assembler.C_CSEG:
				sCommand = "CSEG";
				break;
			case Assembler.C_CEND:
				sCommand = "CEND";
				break;
				
			default:
				sCommand = "<unknown>";
				break;
			}
			break;
		}
		
		sb.append(sCommand);
		
		switch (m_nType) {
		case DL_BYTE:
		    sb.append(" >").append(Utilities.toHex(m_aarg[0].getSymbol().getValue(), 2, true));
		    break;
		case DL_TEXT:
		    sb.append(" ").append(m_aarg[0].toString());
		    break;
		case DL_PSEUDO:
		    // Tab to raw values
		    if (m_bShowData) {
		        for (int i = sb.length() - nDiff; i < 58; i++) sb.append(" ");
		        if (m_nOpcode == Assembler.C_RT) sb.append(Utilities.toHex(0x045b, 4, true));
		        else {
		            if (m_nOpcode == Assembler.C_NOP) sb.append(Utilities.toHex(0x1000, 4, true));
		        }
		    }
			break;
		case DL_NORMAL:
		case DL_BRANCH:
		case DL_JUMP:
		case DL_DATA:
		    if (m_aarg[0] != null) sb.append(" ").append(m_aarg[0].toString());
		    if (m_aarg[1] != null) sb.append(",").append(m_aarg[1].toString());
		    // Tab to raw values
			if (m_bShowData) {
			    for (int i = sb.length() - nDiff; i < 58; i++) sb.append(" ");
			    // Opcode
			    if (m_aarg[0] != null && m_aarg[0].getAddressingType()==Assembler.T_DATA) sb.append(m_aarg[0].toValue());
			    else {
			        sb.append(Utilities.toHex(m_nOpcode,4, true));
			        if (m_aarg[0] != null && m_aarg[0].hasValue()) sb.append(" ").append(m_aarg[0].toValue());
			        if (m_aarg[1] != null && m_aarg[1].hasValue()) sb.append(" ").append(m_aarg[1].toValue());
			    }
			}
			break;
		case DL_DIRECTIVE:
		    if (m_aarg[0] != null) sb.append(" ").append(m_aarg[0].toString());
		    break;		    
		default:
		    sb.append("<unknown>");
		    break;
		}
		
		if (m_bShowLocation) {
		    for (int i = sb.length() - nDiff; i < 73; i++) sb.append(" ");
		    sb.append(": ").append(m_address);
		}
			
		// Argument values (or null)
		return sb.toString();
	}
}

