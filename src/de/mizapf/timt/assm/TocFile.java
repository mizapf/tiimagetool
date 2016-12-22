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

import de.mizapf.timt.TIImageTool;

class TocFile {

	private static void debug(Object s) {
		if (Assembler.DEBUG) System.out.println(s);
	}
	
	byte[] m_abyCode;
	TocEntity[] m_aEntity;	
	int m_nTOCpos = 0;
	boolean m_bCompressed = false;
	
	Map<String,Symbol> m_mapBranch;
	
	private int m_nPosition = 0;
	
	TocFile(byte[] code) {
		m_abyCode = code;
		
		int nType = 0;
		boolean bDone = false;
		m_nTOCpos = 0;
		List<TocEntity> list = new ArrayList<TocEntity>();
		int nVal = 0;
		int nSegment = 0;
		TocEntity ent = null;
		String sVal = null;
		
		debug(Utilities.hexdump(0, 0, m_abyCode, m_abyCode.length, false)); 
		
		while (!bDone) {
			debug(Utilities.toHex(m_nTOCpos, 4) + ": ");
			byte tag = m_abyCode[m_nTOCpos++];
			
			ent = null;
			switch (tag) {
			case 0x01:
				m_bCompressed = true;
				nVal = getWord(); 
				sVal = getString(8, true);
				ent = new TocEntity(TocEntity.IDENT, Assembler.ABSOLUTE, 0, nVal, sVal);
				break;
			case '0':
				m_bCompressed = false;
				nVal = getWord(); 
				sVal = getString(8, true);
				ent = new TocEntity(TocEntity.IDENT, Assembler.ABSOLUTE, 0, nVal, sVal);
				break;
			case '1':
				ent = new TocEntity(TocEntity.ENTRY, Assembler.ABSOLUTE, 0, getWord(), null);
				break;
			case '2':
				ent = new TocEntity(TocEntity.ENTRY, Assembler.PRELOC, 0, getWord(), null);
				break;
			case '3':
				ent = new TocEntity(TocEntity.REF, Assembler.PRELOC, 0, getWord(), getString(6, true));
				break;
			case '4':
				ent = new TocEntity(TocEntity.REF, Assembler.ABSOLUTE, 0, getWord(), getString(6, true));
				break;
			case '5':
				ent = new TocEntity(TocEntity.DEF, Assembler.PRELOC, 0, getWord(), getString(6, true));
				break;
			case '6':
				ent = new TocEntity(TocEntity.DEF, Assembler.ABSOLUTE, 0, getWord(), getString(6, true));
				break;
			case '7': // Checksum
				ent = new TocEntity(TocEntity.CHECKSUM, Assembler.ABSOLUTE, 0, getWord(), null);
				break;
			case '8':
				debug("Checksum ignore; value = " + Utilities.toHex(getWord(), 4));
				break;
			case '9':
				nVal = getWord(); 
				ent = new TocEntity(TocEntity.ADDRESS, Assembler.ABSOLUTE, 0, nVal, null);
				break;
			case 'A': // relocatable address
				nVal = getWord();
				ent = new TocEntity(TocEntity.ADDRESS, Assembler.PRELOC, 0, nVal, null);
				break;
			case 'B': // Absolute data
				ent = new TocEntity(TocEntity.DATA, Assembler.ABSOLUTE, 0, getWord(), null);
				break;
			case 'C': // Relocatable data
				ent = new TocEntity(TocEntity.DATA, Assembler.PRELOC, 0, getWord(), null);
				break;
			case 'D':
				debug("Load bias (unsupp) = " + Utilities.toHex(getWord(), 4, false));
				break;
			case 'E':
				nVal = getWord();
				nSegment = getWord();
				System.err.println(TIImageTool.langstr("TocUnsupported") + " REF offset " + Utilities.toHex(nVal, 4, false) + ", ref " + nSegment); 
				break;
			case 'F': // Next record
				debug("Next rec");
				m_nTOCpos = m_nTOCpos + (80 - m_nTOCpos % 80);
				break;
			case 'M':
				nVal = getWord();
				sVal = getString(6, true);
				nSegment = getWord();
				ent = new TocEntity(TocEntity.SEG, 0, nSegment, nVal, sVal);
				break;
			case 'N':
				nVal = getWord();
				nSegment = getWord();
				ent = new TocEntity(TocEntity.DATA, Assembler.CRELOC, nSegment, nVal, null);
				break;
			case 'P': // CSEG address
				nVal = getWord();
				nSegment = getWord();
				ent = new TocEntity(TocEntity.ADDRESS, Assembler.CRELOC, nSegment, nVal, null);
				break;
			case 'S': // DSEG address
				nVal = getWord();
				ent = new TocEntity(TocEntity.ADDRESS, Assembler.DRELOC, 0, nVal, null);
				break;
			case 'T':
				nVal = getWord();
				ent = new TocEntity(TocEntity.DATA, Assembler.DRELOC, 0, nVal, null);
				break;
			case 'V':
				nVal = getWord();
				sVal = getString(6, true);
				System.err.println(TIImageTool.langstr("TocUnsupported") + " SREF " + sVal);
				break;
			case 'W':
				nVal = getWord();
				sVal = getString(6, true);
				nSegment = getWord();					
				ent = new TocEntity(TocEntity.DEF, (nSegment==0)? Assembler.DRELOC : Assembler.CRELOC, nSegment, nVal, sVal);
				break;
			case 'X':
				nVal = getWord();
				sVal = getString(6, true);
				nSegment = getWord();
				ent = new TocEntity(TocEntity.REF, (nSegment==0)? Assembler.DRELOC : Assembler.CRELOC, nSegment, nVal, sVal);
				break;
				
			case ':': // End of file
				ent = new TocEntity(TocEntity.END, Assembler.PRELOC, 0, 0, null);
				bDone = true;
				break;
				
			default:
				System.err.println(TIImageTool.langstr("TocUnsupported") + ": " + ((char)tag));
				break;
			}
			
			if (ent!=null) {
				list.add(ent);
				debug(ent);
			}
		}
		
		m_aEntity = list.toArray(new TocEntity[list.size()]);
		debug("List length = " + m_aEntity.length);
	}
	
	public void setBranchTable(Map<String,Symbol> table) {
		m_mapBranch = table;
	}
	
	public void setPosition(int nPos) {
		m_nPosition = nPos;
	}
	
	public TocEntity getCurrentEntity() {
		if (m_nPosition >= m_aEntity.length) return null;
		return m_aEntity[m_nPosition];
	}
	
	public TocEntity getNextDataEntity(Location count, int i, Hint[] hint) {
		int nArg = 0;
		debug("Get arg " + i);
		
		if (m_nPosition + nArg > m_aEntity.length) return null;
		int nPos = m_nPosition+1;
		
		while (nPos < m_aEntity.length) {
			debug("  checking " + m_aEntity[nPos]);
			if (m_aEntity[nPos].isOfKind(TocEntity.DATA)) {
				nArg++;
				if (nArg==i) {
					// We do not return a data entity as an argument 
					// if there is a branch to it
					String sLoc = count.getAddressWithOffset(i*2);
					if (m_mapBranch.containsKey(sLoc)) {
						debug("  found data, but there is a branch to this location " + sLoc + "; don't use as an argument");
						return null;
					}
					for (int k=0; k < hint.length; k++) {
						if (hint[k].definesReferenceFor(count)) {
						    debug("   found data, but this has been explicitly referenced; don't use as an argument");
						    return null;
						}
					}
					return m_aEntity[nPos];
				}
			}
			else {
				if (m_aEntity[nPos].isOfKind(TocEntity.ADDRESS)) {
					Location cnt = m_aEntity[nPos].getLocation();
					if (!cnt.isSubsequentTo(count, 2 + nArg*2)) {   // counter, last length
						debug("  not subsequent (entity is at " + cnt +", expected to be after " + count + "), return null");
						return null;
					}
				}
			}
			nPos++;
		}
		return null;
	}
	
	public void advance(int i) {
		m_nPosition += i;
	}
	
	public void advanceDataWords(int i) {
		// We are still pointing to the current command. Advance by
		// the number of arguments plus the command 
		debug("advance by "  + i + " words\n");
		m_nPosition++;
		i--;
		while (i>0) {
			if (m_aEntity[m_nPosition].isOfKind(TocEntity.DATA)) i--; 
			m_nPosition++;
		}
	}
	
	private int getWord() {
		String sHex = "0123456789ABCDEF";
		int nValue = 0;
		if (m_bCompressed) {
			nValue = ((m_abyCode[m_nTOCpos] << 8) & 0xff00) | (m_abyCode[m_nTOCpos+1] & 0xff);
			m_nTOCpos += 2;
		}
		else {
			for (int i=0; i < 4; i++) {
				nValue = (nValue << 4) | sHex.indexOf(m_abyCode[m_nTOCpos++]);
			}
		}
		return nValue;
	}
	
	private String getString(int len, boolean bTrim) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < len; i++) sb.append((char)m_abyCode[m_nTOCpos++]);
		String sRet = sb.toString();
		if (bTrim) return sRet = sRet.trim();
		return sRet;
	}
	
	public TocEntity getLast(int nType) {
		for (int i = m_aEntity.length-1; i >=0; i--) {
			if (m_aEntity[i].isOfKind(nType)) {
				return m_aEntity[i];
			}
		}
		return null;
	}
	
	public TocEntity[] getAll(int nType) {
		List<TocEntity> list = new ArrayList<TocEntity>();
		for (int i=0; i < m_aEntity.length; i++) {
			if (m_aEntity[i].isOfKind(nType)) {
				list.add(m_aEntity[i]);
			}			
		}
		TocEntity[] res = list.toArray(new TocEntity[list.size()]);
		return res;
	}
	
	public String[] getStrings(TocEntity[] aent, int nAt, int nLength) {
		int nLen = nLength;
		if (nLen > aent.length - nAt) nLen = aent.length - nAt;
		String[] as = new String[nLen];
		for (int i=0; i < nLen; i++) as[i] = aent[i + nAt].getStringValue();
		return as;
	}
}

