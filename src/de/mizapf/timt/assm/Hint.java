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
    
    Copyright 2011 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.assm;

import de.mizapf.timt.files.FormatException;
import de.mizapf.timt.util.Utilities;

import java.util.List;
import java.util.ArrayList;

import de.mizapf.timt.TIImageTool;

public class Hint {
	private int m_nKind;
	
	private Location m_locStart;
	private Location m_locEnd;
    private int m_nArgCount;	
    
    private boolean m_bBiased;
	
	private String m_sSymbol;
	
	public static final int DATA = 1;
	public static final int TEXT = 2;
	public static final int REF = 3;
	public static final int PARAM = 4;
	public static final int NOFMT = 5;
	
	public static final int OPT_SHOWLOC = 1;
	public static final int OPT_SHOWDATA = 2;

	/** Constructor for DATA and TEXT */
	Hint(int kind, Location start, Location end) {
	    m_locStart = start;
	    m_locEnd = end;
	    m_nKind = kind;
	    m_bBiased = false;
	}

	/** Constructor for PARAM */
	Hint(Location address, int params) {
	    m_sSymbol = null;
	    m_locStart = address;
	    m_nArgCount = params;
	    m_nKind = PARAM;
	}

	/** Constructor for PARAM with symbol. */
	Hint(String symbol, int params) {
	    m_locStart = null;
	    m_sSymbol = symbol;
	    m_nArgCount = params;
	    m_nKind = PARAM;
	}
	
	/** Constructor for REF */
	Hint(Location address) {
	    m_locStart = address;
	    m_nKind = REF;
	}
	
	public void setBiased(boolean bSet) {
	    m_bBiased = bSet;
	}
	
	public int getKind() {
	    return m_nKind;
	}
	
	public Location getStart() {
	    return m_locStart;
	}
	
	public Location getEnd() {
	    return m_locEnd;
	}

	public String getSymbol() {
	    return m_sSymbol;
	}	

	public boolean contains(Location loc) {
	    return (m_locStart.isLowerOrEqual(loc) && m_locEnd.isHigherOrEqual(loc)); 
	}
	
	public boolean contains(int nAddress) {
	    return ((nAddress >= getStart().getValue()) && (nAddress <= getEnd().getValue()));
	}
	
	public boolean isRange() {
	    return (m_nKind == DATA || m_nKind == TEXT || m_nKind == NOFMT);
	}
	
	public boolean isBiasedText() {
	    return m_bBiased;
	}
	
	public boolean definesParamsFor(Location add) {
	    if (m_locStart == null) return false;
	    return (m_nKind == PARAM && m_locStart.equals(add)); 
	}

	public boolean definesParamsFor(String name) {
	    if (m_sSymbol == null) return false;
	    return (m_nKind == PARAM && m_sSymbol.equals(name)); 
	}

	public boolean definesParamsFor(int nAddress) {
	    return (m_nKind == PARAM && m_locStart.getAddressingType()==Assembler.ABSOLUTE && m_locStart.getValue()==nAddress); 
	}
	
	public boolean definesReferenceFor(Location add) {
	    return (m_nKind == REF && m_locStart.equals(add));
	}
	
	public int getParamCount() {
	    return m_nArgCount;
	}
	
	public static void main(String[] arg) {
		TIImageTool.localize();
		try {
			Hint[] hint = parse(arg[0]);
			for (int i=0; i < hint.length; i++) {
				System.out.println(hint[i]);
			}
		}
		catch (FormatException fx) {
			System.out.println(fx.getMessage());
		}
	}

	/* New hints
		DATA area:     data(fromAddr,toAddr)
		TEXT area:     text(fromAddr,toAddr)
		Referenced:    ref(addr)
		Params:        param(addr,words), param(symbol,words)
		No FMT:        nofmt(fromAddr,toAddr)  ONLY GPL
		
		addr = Location value; from/to must be from the same scope
		symbol needs quotes
		
		Examples:

        All four examples are equivalent:		
		data(XC000,XC100)
		data(>C000,>C100)
		data(0xC000,0xC100)
		data(c000,c100)
		
		param("XMLLNK",1)
		param(R046A,2)
		data(R0000,R0010)
		ref(R010a)
		text(T0004,T000A)
		ref(SA0008)
		
		Invalid:
		data(R0000,0010)
	*/

	
    public static Hint[] parse(String sHintLine) throws FormatException {
        // Try to find the positions of the parameters
        // Have a look at opening and closing parentheses
        int nPosOpen = -1;
        int nPosLast = -1;
        String sArg1 = null;
        String sArg2 = null;
        String sSymbol = null;
        
        List<Hint> list = new ArrayList<Hint>();
        
        for (int i=0; i < sHintLine.length(); i++) {
            if (sHintLine.charAt(i)=='(') {
                if (nPosOpen != -1) throw new FormatException(TIImageTool.langstr("Hints"), TIImageTool.langstr("HintsDoubleOpen"));
                nPosOpen = i;
            }
            if (sHintLine.charAt(i)==')') {
                if (nPosOpen == -1) throw new FormatException(TIImageTool.langstr("Hints"), TIImageTool.langstr("HintsIllegalClose"));
                String sArg = sHintLine.substring(nPosOpen+1, i).trim();
                String sKind = sHintLine.substring(nPosLast+1, nPosOpen).trim();
                int nCommaPos = sArg.indexOf(",");
                if (nCommaPos!=-1) {
                    sArg1 = sArg.substring(0, nCommaPos).trim();
                    sArg2 = sArg.substring(nCommaPos+1, sArg.length()).trim();
                }
                else {
                    sArg1 = sArg;
                    sArg2 = null;
                }
                
                if (sKind.equalsIgnoreCase("param")) {
                    try {
                        if (sArg1.charAt(0)=='\"') {
                            if (sArg.charAt(sArg1.length()-1)=='\"') {
                                sSymbol = sArg1.substring(1, sArg1.length()-1);
                            }
                            else {
                                throw new FormatException(sArg, TIImageTool.langstr("HintsUnmatched") + ": " + sArg1);
                            }
                            list.add(new Hint(sSymbol, Integer.parseInt(sArg2)));
                        }
                        else {
                            list.add(new Hint(Location.getInstance(sArg1), Integer.parseInt(sArg2)));
                        }
                    }
                    catch (NumberFormatException nfx) {
                        throw new FormatException(TIImageTool.langstr("Hints"), String.format(TIImageTool.langstr("ParseError"), sArg2));
                    }
                }
                else {
                    if (sKind.equalsIgnoreCase("data")) {
                        list.add(new Hint(DATA, Location.getInstance(sArg1), Location.getInstance(sArg2)));
                    }
                    else {
                        if (sKind.equalsIgnoreCase("text")) {
                            list.add(new Hint(TEXT, Location.getInstance(sArg1), Location.getInstance(sArg2)));
                        }
                        else {
                            if (sKind.equalsIgnoreCase("btext")) {
                                Hint hint = new Hint(TEXT, Location.getInstance(sArg1), Location.getInstance(sArg2));
                                hint.setBiased(true);
                                list.add(hint);
                            }
                            else {
                                if (sKind.equalsIgnoreCase("ref")) {
                                    list.add(new Hint(Location.getInstance(sArg1)));
                                }
                                else {
                                    if (sKind.equalsIgnoreCase("nofmt")) {
                                        list.add(new Hint(NOFMT, Location.getInstance(sArg1), Location.getInstance(sArg2)));
                                    }
                                    else {
                                        throw new FormatException(TIImageTool.langstr("Hints"), TIImageTool.langstr("HintsUnknown") + " '" + sKind + "'");
                                    }
                                }
                            }
                        }
                    }
                }
                nPosOpen = -1;
                nPosLast = i+1;
            }
        }
        if (nPosOpen != -1) throw new FormatException(TIImageTool.langstr("Hints"), TIImageTool.langstr("HintsNotClosed"));

        Hint[] ah = list.toArray(new Hint[0]);
        return ah;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        switch (m_nKind) {
        case DATA:
            sb.append("data(").append(m_locStart).append(",").append(m_locEnd).append(")");
            break;
        case TEXT:
            sb.append(m_bBiased? "btext(" : "text(").append(m_locStart).append(",").append(m_locEnd).append(")");
            break;
        case PARAM:
            if (m_sSymbol != null) {
                sb.append("param(\"").append(m_sSymbol).append("\",").append(m_nArgCount).append(")");
            }
            else {
                sb.append("param(").append(m_locStart).append(",").append(m_nArgCount).append(")");
            }
            break;
        case REF:
            sb.append("ref(").append(m_locStart).append(")");
            break;
        case NOFMT:
            sb.append("nofmt(").append(m_locStart).append(",").append(m_locEnd).append(")");
        }
        return sb.toString();
    }
}
