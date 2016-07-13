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

public class Assembler {
	
	public static final boolean DEBUG = false;
	
	byte[] m_Code;
	static Command[] command; 
	private Location m_Counter;

	public final static int INFTY = 100000;
	
	public final static int ABSOLUTE = 1;
	// public final static int RELOCATABLE = 2;
	public final static int PRELOC = 3;
	public final static int DRELOC= 4;
	public final static int CRELOC= 5;
	
	static int[] mask = { 0x0000, 0xf000, 0xff00, 0xfc00, 0xfc00, 0xff00, 0xffc0,
		0xffff, 0xfff0, 0xfc00, 0xfff0, 0xffc0 };

	final static int T_REG = 0;
	final static int T_REGIND = 1;
	final static int T_SYM = 2;
	final static int T_INDEXED = 3;
	final static int T_REGINDINC = 4;
	final static int T_IMM = 5;
	final static int T_COUNT = 6;
	final static int T_JUMP = 7;
	final static int T_DATA = 8;
	final static int T_IMMREL = 9;
	final static int T_BYTE = 10;
	final static int T_TEXT = 11;
	
	/* Pseudo commands and assembler directives. */
	final static int C_RT = 0x10000;
	final static int C_NOP = 0x10001;
	final static int C_BSS = 0x10002;
	final static int C_END = 0x10003;
	final static int C_AORG = 0x10004;
	final static int C_RORG = 0x10005;
	final static int C_IDT = 0x10006;
	final static int C_DATA = 0x10007;
	final static int C_DEF = 0x10008;
	final static int C_REF = 0x10009;
	final static int C_BLANK = 0x1000a;
	final static int C_DSEG = 0x1000b;
	final static int C_DEND = 0x1000c;
	final static int C_CSEG = 0x1000d;
	final static int C_CEND = 0x1000e;
	final static int C_TEXT = 0x1000f;
	final static int C_BYTE = 0x10010;
	
	public static void debug(Object s) {
		if (DEBUG) System.out.println(s);
	}
					
	static {
		List<Command> lst = new ArrayList<Command>();
		lst.add(new Command("LST ", 0x0080,11));  lst.add(new Command("LWP ", 0x0090,11));
		lst.add(new Command("DIVS", 0x0180,10));  lst.add(new Command("MPYS", 0x01C0,10));
		lst.add(new Command("LI  ", 0x0200, 8));  lst.add(new Command("AI  ", 0x0220, 8));
		lst.add(new Command("ANDI", 0x0240, 8));  lst.add(new Command("ORI ", 0x0260, 8));
		lst.add(new Command("CI  ", 0x0280, 8));  lst.add(new Command("STWP", 0x02A0, 8));
		lst.add(new Command("STST", 0x02C0, 8));  lst.add(new Command("LWPI", 0x02E0, 8));
		lst.add(new Command("LIMI", 0x0300, 8));  lst.add(new Command("IDLE", 0x0340, 7));
		lst.add(new Command("RSET", 0x0360, 7));  lst.add(new Command("RTWP", 0x0380, 7));
		lst.add(new Command("CKON", 0x03A0, 7));  lst.add(new Command("CKOF", 0x03C0, 7));
		lst.add(new Command("LREX", 0x03E0, 7));  lst.add(new Command("BLWP", 0x0400, 6));
		lst.add(new Command("B   ", 0x0440, 6));  lst.add(new Command("X   ", 0x0480, 6));
		lst.add(new Command("CLR ", 0x04C0, 6));  lst.add(new Command("NEG ", 0x0500, 6));
		lst.add(new Command("INV ", 0x0540, 6));  lst.add(new Command("INC ", 0x0580, 6));
		lst.add(new Command("INCT", 0x05C0, 6));  lst.add(new Command("DEC ", 0x0600, 6));
		lst.add(new Command("DECT", 0x0640, 6));  lst.add(new Command("BL  ", 0x0680, 6));
		lst.add(new Command("SWPB", 0x06C0, 6));  lst.add(new Command("SETO", 0x0700, 6));
		lst.add(new Command("ABS ", 0x0740, 6));  lst.add(new Command("SRA ", 0x0800, 5));
		lst.add(new Command("SRL ", 0x0900, 5));  lst.add(new Command("SLA ", 0x0A00, 5));
		lst.add(new Command("SRC ", 0x0B00, 5));  lst.add(new Command("JMP ", 0x1000, 2));
		lst.add(new Command("JLT ", 0x1100, 2));  lst.add(new Command("JLE ", 0x1200, 2));
		lst.add(new Command("JEQ ", 0x1300, 2));  lst.add(new Command("JHE ", 0x1400, 2));
		lst.add(new Command("JGT ", 0x1500, 2));  lst.add(new Command("JNE ", 0x1600, 2));
		lst.add(new Command("JNC ", 0x1700, 2));  lst.add(new Command("JOC ", 0x1800, 2));
		lst.add(new Command("JNO ", 0x1900, 2));  lst.add(new Command("JL  ", 0x1A00, 2));
		lst.add(new Command("JH  ", 0x1B00, 2));  lst.add(new Command("JOP ", 0x1C00, 2));
		lst.add(new Command("SBO ", 0x1D00, 2));  lst.add(new Command("SBZ ", 0x1E00, 2));
		lst.add(new Command("TB  ", 0x1F00, 2));  lst.add(new Command("COC ", 0x2000, 3));
		lst.add(new Command("CZC ", 0x2400, 3));  lst.add(new Command("XOR ", 0x2800, 3));
		lst.add(new Command("XOP ", 0x2C00, 9));  lst.add(new Command("LDCR", 0x3000, 4));
		lst.add(new Command("STCR", 0x3400, 4));  lst.add(new Command("MPY ", 0x3800, 9));
		lst.add(new Command("DIV ", 0x3C00, 9));  lst.add(new Command("SZC ", 0x4000, 1));
		lst.add(new Command("SZCB", 0x5000, 1));  lst.add(new Command("S   ", 0x6000, 1));
		lst.add(new Command("SB  ", 0x7000, 1));  lst.add(new Command("C   ", 0x8000, 1));
		lst.add(new Command("CB  ", 0x9000, 1));  lst.add(new Command("A   ", 0xA000, 1));
		lst.add(new Command("AB  ", 0xB000, 1));  lst.add(new Command("MOV ", 0xC000, 1));
		lst.add(new Command("MOVB", 0xD000, 1));  lst.add(new Command("SOC ", 0xE000, 1));
		lst.add(new Command("SOCB", 0xF000, 1));  lst.add(new Command("SOCB", 0xFFFF, 1));
		
		command = lst.toArray(new Command[0]);
	}
	
	public Assembler(byte[] abyCode) {
		m_Code = abyCode;
	}
	
	private int lookupCommand(int opcode) {
		boolean bFound = false;
		int index = 0;
		while (index < command.length && !bFound) {
			if ((opcode & mask[command[index].format]) == command[index].opcode) {
				bFound = true;
			}
			else index++;
		}
		if (!bFound) return -1;
		return index;
	}
	
	private LineArgument createArgument(int type, int reg, Symbol sym) {
		LineArgument arg = null;
		
		switch (type) {
		case 0:
			arg = new LineArgument(T_REG, reg);
			break;
		case 1:
			arg = new LineArgument(T_REGIND, reg);
			break;
		case 2:
			if (sym==null) return null;
			arg = new LineArgument(sym, reg);
			break;
		case 3:
			arg =  new LineArgument(T_REGINDINC, reg);
			break;
		}
		return arg;
	}
	
	public DisassembledLine disassembleLine(Location location, TocEntity ent, TocEntity enta1, TocEntity enta2, int nShowDataLocs) {
		Symbol s1 = ent.getSymbol();
		Symbol s2 = (enta1 != null)? enta1.getSymbol() : null;
		Symbol s3 = (enta2 != null)? enta2.getSymbol() : null;
		return disassembleLine(location, s1, s2, s3, nShowDataLocs);
	}
	
	public DisassembledLine disassembleLine(Location location, Symbol sym1, Symbol sym2, Symbol sym3, int nShowDataLoc) {

		int ts = -1;
		int td = -1;
		
		int displacement = 0;
		
		LineArgument arg1 = null;
		LineArgument arg2 = null;
		
		boolean bInvalid = false;
		boolean bBranch = false;
		boolean bJump = false;
		
		// It is not strictly illegal to have a relocatable symbol as an opcode
		// but this would mean a different opcode whereever the program is loaded,
		// and in this case we assume this is a data line.
		if (!sym1.isAbsolute()) {
			debug("Commands must be absolute data");
			return null;
		}
		
		int opcode = sym1.getValue();
		int index = lookupCommand(opcode);
		boolean bFound = (index != -1);
		
		if (!bFound) {
			debug("Command " + Utilities.toHex(opcode, 4) + " not found");
			return null;
		}
		//debug("Format " + command[index].format);
			
		switch (command[index].format) {					
		case 1: /* 2-arg gen addr */
			// ts/td = 00  - Rx
			//         01  - *Rx
			//         10  - @pos / @pos(Rx)
			//         11  - *Rx+ 
			ts = ((opcode>>4)&0x0003);
			arg1 = createArgument(ts, opcode & 0x000f, sym2);
			arg2 = createArgument((opcode>>10)&0x0003, (opcode>>6) & 0x000f, (ts==2)? sym3 : sym2);
			if (arg1==null || arg2==null) bInvalid = true;
			break;
			
		case 2: 
			displacement = opcode & 0x00ff;
			if (opcode < 0x1d00) {
				// Jump commands
				// Jump target is the current address + displacement (absolute or program-reloc)
				if (displacement > 0x007f) displacement = displacement | 0xff00;
				arg1 = new LineArgument(new Symbol(location.getAddressingType(), (location.getValue() + 2*(displacement + 1))&0xffff), 0);
				arg1.setAddressingType(T_JUMP);
				bJump = true;
			}
			else {
				// CRU single-bit commands
				if (displacement > 0x007f) displacement = -256 + displacement;
				arg1 = new LineArgument(T_COUNT, displacement);
			}
			break;
			
		case 3:
			arg1 = createArgument((opcode>>4)&0x0003, opcode & 0x000f, sym2);
			if (arg1==null) bInvalid = true;
			arg2 = new LineArgument(T_REG, (opcode>>6)&0x000f);
			break;
			
		case 4:
			arg1 = createArgument((opcode>>4)&0x0003, opcode & 0x000f, sym2);
			if (arg1==null) bInvalid = true;
			arg2 = new LineArgument(T_COUNT, (opcode>>6)&0x000f);
			break;
			
		case 5:
			arg1 = new LineArgument(T_REG, opcode & 0x000f);
			arg2 = new LineArgument(T_COUNT, (opcode>>4)&0x000f);
			break;
			
		case 6: 
			if (opcode > 0x077F) bInvalid = true;
			else {
				ts = ((opcode>>4)&0x0003);
				arg1 = createArgument(ts, opcode & 0x000f, sym2);
				if (ts==2 && (command[index].opcode == 0x0400 || command[index].opcode == 0x0440 || command[index].opcode == 0x0680)) bBranch = true;
			}
			if (arg1==null) bInvalid = true;
			break;
			
		case 7: 
			if ((opcode & 0x001f)!=0) bInvalid = true;
			break;
			
		case 8: 
			if ((opcode & 0x0010)!=0) bInvalid = true;
			else {
				if (opcode < 0x02e0) {
					ts = 0;
					arg1 = new LineArgument(T_REG, opcode&0x000f);
					if (opcode < 0x02a0) { // not STWP / STST
						arg2 = new LineArgument(sym2);
						arg2.setAddressingType(T_IMM);
					}
					if (sym2==null) bInvalid = true;
				}
				else {
					// LWPI/LIMI
					if ((opcode & 0x000f)!=0) bInvalid = true;
					else {
						arg1 = new LineArgument(sym2);
						arg1.setAddressingType(T_IMM);
					} 
					if (sym2==null) bInvalid = true;
				}
			}
			break;
			
		case 9:  // DIV, MUL, XOP
			arg1 = createArgument((opcode>>4)&0x0003, opcode & 0x000f, sym2);
			arg2 = new LineArgument(T_REG, (opcode>>6)&0x000f);
			// XOPs have a count
			if ((opcode & 0x2c00)==0x2c00) arg2.setAddressingType(T_COUNT);		
			if (arg1==null) bInvalid = true;
			break;
			
		case 10:  // DIVS, MPYS
			arg1 = createArgument((opcode>>4)&0x0003, opcode & 0x000f, sym2);
			if (arg1==null) bInvalid = true;
			break;
			
		case 11:  // LST, LWP
			if (opcode > 0x009f) bInvalid = true;
			else {
				arg1 = new LineArgument(T_REG, opcode&0x000f);
			}
			break;
		}
		
		if (bInvalid) return null;

		DisassembledLine line = null;
		if (opcode == 0x045b) {
			line = new DisassembledLine(location, "RT", C_RT, null, null, nShowDataLoc);
		}
		else {
			if (opcode == 0x1000) {
				line = new DisassembledLine(location, "NOP", C_NOP, null, null, nShowDataLoc);
			}
			else {
				line = new DisassembledLine(location, command[index].name, opcode, arg1, arg2, nShowDataLoc);
			}
		}
		if (bBranch) line.setBranchToAddressCommand();
		if (bJump) line.setJumpToAddressCommand();

		return line;
	}
	
	/** Disassemble memory dump. We only have absolute addresses and absolute data. */	
	public String disassembleMemoryImage(int nStartOffset, int nStartAddress, int nLength, Hint[] hint, int nShowDataLoc) {
		StringBuilder sb = new StringBuilder();
					
		int nOffset = nStartOffset;
		int nDatalines = 0;

		// Allocate an absolute counter
		Location aCounter = new Location(ABSOLUTE);
		
		aCounter.setValue(nStartAddress);
		m_Counter = aCounter; // Absolute counter 
		
		DisassembledLine dl = null;
		boolean bData = false;
		
		Symbol val1, val2, val3;
		List<DisassembledLine> lines = new ArrayList<DisassembledLine>();

		Set<Hint> dataareas = new HashSet<Hint>();  // collects all areas of data lines
		Set<Hint> textareas = new HashSet<Hint>();  // collects all areas of text lines
		
		for (Hint hu : hint) {
		    if (hu.getKind()==Hint.DATA) dataareas.add(hu);
		    if (hu.getKind()==Hint.TEXT) textareas.add(hu);
		}
		
		StringBuilder sbText = new StringBuilder();
		Location cntLast = null;

		try {
			while (nOffset < nLength+nStartOffset-1) {
				bData = false;		
				
				// Check whether we are in a text area
				// We will start a new TEXT at the beginning of an area
				
				// This is easier than in the tagged object code variant
				// because we can advance in single byte steps
				boolean bTextArea = false;
				for (Hint htext : textareas) {
				    if (htext.contains(m_Counter)) {
				        if (cntLast == null) {
				            debug("Set TEXT counter to " + m_Counter);
				            cntLast = (Location)m_Counter.clone();
				        }

				        int by1 = m_Code[nOffset] & 0xff;
				        debug("Trying to add a character to TEXT: " + Utilities.toHex(by1,2) + "; buffer length " + sbText.length());  
        
				        // Is it the start of the text region?
				        // Is the line already full?
				        // Have we encountered a non-printable character?				        
				        if (htext.getStart().equals(m_Counter) || sbText.length() == 40 || by1 < 32 || by1 > 126) {
				            // Terminate the last TEXT line
				            if (sbText.length()>0) {
				                lines.add(createNewTextLine(sbText, cntLast, nShowDataLoc));
				            }
				        }
				        
				        if (by1 >= 32 && by1 <= 126) {
				            // Byte is printable → put it in the TEXT line
				            sbText.append((char)by1);
				        }
				        else {
				            // Byte is not printable → use a BYTE line 
				            lines.add(createNewByteLine(by1, nShowDataLoc));
				            cntLast = null;
				        }
				        bTextArea = true;
				        break;
				    }
				}
				if (bTextArea) {
				    nOffset++;
				    m_Counter.add(1);
				    continue;					            
				}
				
				if (sbText.length()>0) {
				    // Flush the TEXT line
				    debug("There is a TEXT line to be written; location is " + cntLast);
				    lines.add(createNewTextLine(sbText, cntLast, nShowDataLoc));
				}
			    cntLast = null;

				// Align to word boundary
				if ((m_Counter.getValue() & 1) != 0) {
				    int by1 = m_Code[nOffset] & 0xff;
				    lines.add(createNewByteLine(by1, nShowDataLoc));
				    nOffset++;
				    m_Counter.add(1);
				    continue;					            
				}
				
				// Check if our counter is somewhere in a data area
				for (Hint hdat : dataareas) {
				    if (hdat.contains(m_Counter)) {
				        // System.out.println(m_Counter + ": have it in " + hdat);
				        bData = true;
				    }
				}
				
				// The opcode must be an absolute symbol
				val1 = new Symbol(Utilities.getInt16(m_Code, nOffset));
				
				dl = null;
				if (!bData && nDatalines==0) {
					// Get the (possible) arguments
					int nval2 = 0;
					int nval3 = 0;
					if (nOffset+2 < m_Code.length) nval2 = Utilities.getInt16(m_Code, nOffset+2);
					if (nOffset+4 < m_Code.length) nval3 = Utilities.getInt16(m_Code, nOffset+4);
					
					// Create them as absolute symbols
					val2 = new Symbol(nval2);
					val3 = new Symbol(nval3);

					dl = disassembleLine(m_Counter, val1, val2, val3, nShowDataLoc);
				}

				if (dl==null) {
					// not found or invalid or data line
					LineArgument la = new LineArgument(val1);
					la.setAddressingType(T_DATA);
					dl = new DisassembledLine(m_Counter, C_DATA, la); // creates DATA line
					if (nDatalines>0) nDatalines--;
				}
				else {
					if (dl.isBranchToAddressCommand()) {
						// Special handling: If the address is known for a 
						// subroutine that expects datalines, let the 
						// disassembler keep the contents unchanged
						for (int j=0; j < hint.length; j++) {
							Location add = dl.getLineArgument(0).getLocation();
							if (hint[j].definesParamsFor(add)) nDatalines = hint[j].getParamCount();
						}
					}
					else {
						if (dl.isJump()) dl.getLineArgument(0).setJumpPrefix(">");
					}
				}
				
				lines.add(dl);
				dl.setReferenced();
				dl.setDumpAddressMode(true);
				nOffset += dl.getLength();
				m_Counter.add(dl.getLength());
			}
		}
		catch (ArrayIndexOutOfBoundsException aax) {
			debug("TIImageTool (disassembler): Clipped end or beyond program code");
		}
		
		for (DisassembledLine line : lines) {
			if (sb.length()>0) sb.append("\n");
			sb.append(line.toString());
		}
		return sb.toString();
	}
	
	private DisassembledLine createNewTextLine(StringBuilder sbText, Location cntLast, int nShowDataLocs) {
	    DisassembledLine dl = null;
	    LineArgument la = new LineArgument(sbText.toString(), true);
	    sbText.delete(0, sbText.length());
	    la.setAddressingType(T_TEXT);
	    dl = new DisassembledLine(null, C_TEXT, la, nShowDataLocs);
	    dl.setReferenced();
	    dl.setLocation(cntLast);
	    // For the next line
	    cntLast.setFrom(m_Counter);
	    dl.setDumpAddressMode(true);
	    return dl;
	}
	
	private DisassembledLine createNewByteLine(int by1, int nShowDataLoc) {
	    DisassembledLine dl = null;
	    LineArgument la = new LineArgument(ABSOLUTE, by1 & 0xff);
	    la.setAddressingType(T_BYTE);
	    dl = new DisassembledLine(null, C_BYTE, la, nShowDataLoc);
	    dl.setReferenced();
	    dl.setLocation(m_Counter);
	    dl.setDumpAddressMode(true);
	    return dl;
	}
	
// ======================================================================
	
	/** Tagged object code handling. */
					
	public String disassembleTaggedObjectCode(int nOffset, int nStart, int nLength, Hint[] hint, int nShowDataLocs) throws FormatException {
		StringBuilder sb = new StringBuilder();
		StringBuilder sb1 = null;
		String indent = "          ";

		int nPos = 0;
		TocEntity e1, e2, e3;
		Symbol s1, s2, s3;
		DisassembledLine dl = null;
		DisassembledLine dlLast = null;
		int nValue = 0;
		int nLastLen = 2;
		boolean bLinesOK = false;
		boolean bEndOfFile = false;
		LineArgument la1 = null;

		// Prepare TocFile
		TocFile tocFile = null;		
		try {
			tocFile = new TocFile(m_Code);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			throw new FormatException("Disassemble", "End tag missing, or file is not tagged object code");
		}
				
		LinkedList<DisassembledLine> lines = new LinkedList<DisassembledLine>();

		// =====================================================================
		//  Segments
		// =====================================================================
		
		TocEntity[] segment = tocFile.getAll(TocEntity.SEG);
		int nHighest = -1;
		// Find highest segment id
		for (int i=0; i < segment.length; i++) {
			if (nHighest < segment[i].getSegment()) nHighest = segment[i].getSegment();
		}
		String[] asSegment = new String[nHighest+1];

		for (int i=0; i < segment.length; i++) {
			asSegment[segment[i].getSegment()] = segment[i].getStringValue();
		}
		
		// Allocate an array of counters for each segment, indexed by the segment id
		Location[] aSegmentCounter = new Location[asSegment.length];
		for (int i=0; i < asSegment.length; i++) {
			debug(asSegment[i]);
			if (asSegment[i] != null) aSegmentCounter[i] = new Location(0, i);
		}
		
		// Allocate an absolute counter
		Location aCounter = new Location(ABSOLUTE);
		
		// Allocate a program-relocatable counter
		Location pCounter = new Location(PRELOC);
				
		// =====================================================================
		//  File header
		// =====================================================================

		// 1. Search IDT
		TocEntity ident = tocFile.getLast(TocEntity.IDENT);
		if (ident != null && ident.getStringValue().trim().length()!=0) {
			la1 = new LineArgument(ident.getStringValue(), true);
			dl = new DisassembledLine(m_Counter, C_IDT, la1);
			lines.add(dl);
		}
	
		// 2. Construct DEF lines with max 5 entries each
		TocEntity[] deftable = tocFile.getAll(TocEntity.DEF);
		for (int i=0; i < deftable.length; i+=5) {
			la1 = new LineArgument(tocFile.getStrings(deftable, i, 5));
			dl = new DisassembledLine(m_Counter, C_DEF, la1);
			lines.add(dl);
		}
		
		// 3. Search REF
		TocEntity[] reftable = tocFile.getAll(TocEntity.REF);
		for (int i=0; i < reftable.length; i+=5) {
			la1 = new LineArgument(tocFile.getStrings(reftable, i, 5));
			lines.add(new DisassembledLine(m_Counter, C_REF, la1));
		}
	
		addBlank(lines);

		// =====================================================================
		// 	File body
		// =====================================================================

		LinkedList<DisassembledLine> bodylines = null;
		TocEntity ent = null;
		TocEntity enta1 = null;
		TocEntity enta2 = null;
		
		Map<String,Symbol> symboltable = new HashMap<String,Symbol>();
		Map<String,Symbol> branchtable = new HashMap<String,Symbol>();
		Set<Hint> dataareas = new HashSet<Hint>();  // collects all areas of data lines
		Set<Hint> textareas = new HashSet<Hint>();  // collects all areas of text lines
		
		for (Hint hu : hint) {
		    if (hu.getKind()==Hint.DATA) dataareas.add(hu);
		    if (hu.getKind()==Hint.TEXT) textareas.add(hu);
		}
		
		int nPass = 2;
		
		while (nPass > 0) {
			boolean bFirstInBlock = true;
			bodylines = new LinkedList<DisassembledLine>();
			tocFile.setPosition(0);
			tocFile.setBranchTable(branchtable);
			
			debug("===================");
			bEndOfFile = false;

			// Initialize to program-relocatable
			pCounter.setValue(0);
			m_Counter = pCounter;
			StringBuilder sbText = new StringBuilder();
			boolean bTextLine = false;
			Location cntLast = null;
			
			while (!bEndOfFile) {
				ent = tocFile.getCurrentEntity();
				boolean bData = false;
				if (ent!=null) {
					debug("current entity = " + ent);
					if (ent.isOfKind(TocEntity.DATA)) {

					    // ===============================================
					    // TEXT area handling
					    // ===============================================

					    // Check whether the current location is within a text area
					    // When the current entity is not printable, create a 
					    // BYTE line
					    
					    // Only makes sense for absolute data
					    if (ent.hasAddressingType(ABSOLUTE)) {
					        // We have two bytes to check; both bytes are within
					        // the text area or they are not
					        boolean bTextArea = false;
					        for (Hint htext : textareas) {
					            if (htext.contains(m_Counter)) {
					                if (cntLast == null) {
					                    debug("Set TEXT counter to " + m_Counter);
					                    cntLast = (Location)m_Counter.clone();
					                }
					                bTextArea = true;
					                int by1 = (ent.getSymbol().getValue() >> 8) & 0xff;
					                int by2 = ent.getSymbol().getValue() & 0xff;
					                debug("Trying to add characters to TEXT: " + Utilities.toHex(by1,2) + ", " + Utilities.toHex(by2,2) + "; buffer length " + sbText.length());  
					                
					                int by = by1;
					                for (int k=0; k < 2; k++) {
					                    DisassembledLine dl1 = handleTextArea(sbText, by, symboltable, nShowDataLocs);
					                    
					                    if (dl1 != null) {
					                        if (dl1.isTextLine()) {
					                            // The TEXT line has been closed
					                            // because it was full, or the current position is referenced, or the 
					                            // current byte is not printable
					                            // System.out.println("Closing the TEXT line");
					                            dl1.setLocation(cntLast);
					                            checkReferenced(dl1, symboltable, deftable);
					                            bodylines.add(dl1);
					                            cntLast.setFrom(m_Counter);
					                            debug("TEXT line = " + dl1);
					                            
					                            // Call it again; sbText has been reset, so it cannot be a text line again
					                            debug("Byte = " + Utilities.toHex(by,2));
					                            dl1 = handleTextArea(sbText, by, symboltable, nShowDataLocs);
					                            if (dl1 != null) {
					                                // Now this is a BYTE line for sure
					                                dl1.setLocation(m_Counter);
					                                checkReferenced(dl1, symboltable, deftable);
					                                bodylines.add(dl1);
					                                cntLast = null;
					                            }
					                        }
					                        else {
					                            // BYTE line
					                            debug("BYTE line at " + m_Counter);
					                            dl1.setLocation(m_Counter);
					                            checkReferenced(dl1, symboltable, deftable);
					                            bodylines.add(dl1);
					                            cntLast = null;
					                        }
					                    }
					                    by = by2;
					                    m_Counter.add(1);
					                }
					                tocFile.advanceDataWords(1);
					                break;
					            }
					        }
					        if (bTextArea) {
					            continue;					            
					        }
					    }
					    
					    if (sbText.length()>0) {
					        // Flush the TEXT line
					        debug("There is a TEXT line to be written; location is " + cntLast);
					        DisassembledLine dl2 = handleTextArea(sbText, (byte)0, symboltable, nShowDataLocs);
					        // System.out.println("cntLast is " + cntLast);
	                        dl2.setLocation(cntLast);
	                        checkReferenced(dl2, symboltable, deftable);
	                        bodylines.add(dl2);
		                    continue;
					    }
					    
					    // ===============================================
					    // DATA area handling
   					    // ===============================================

					    // Check whether the current location is a data location
						for (Hint hdat : dataareas) {
						    if (hdat.contains(m_Counter)) {
						        // System.out.println(m_Counter + ": have it in " + hdat);
						        bData = true;
						        dl = null;
						        break;
						    }
						}
						
						if (!bData) {
						    enta1 = tocFile.getNextDataEntity(m_Counter, 1, hint);
						    enta2 = tocFile.getNextDataEntity(m_Counter, 2, hint);
						    
						    // enta1 and enta2 may be null if there are no subsequent data items
						    // hence, disassembly may fail, and we return DATA lines
						    debug("possible argument entities: " + enta1 + ", " + enta2);
						    dl = disassembleLine(m_Counter, ent, enta1, enta2, nShowDataLocs);
						}
						
						if (dl == null) {
							// not found or invalid or data line
							LineArgument la = new LineArgument(ent.getSymbol());
							la.setAddressingType(T_DATA);
							dl = new DisassembledLine(m_Counter, C_DATA, la); // creates DATA line						
						}

						// At this point we have a properly disassembled line
						
						boolean bBlankLine = checkReferences(dl, deftable, bodylines, symboltable);
					    if (bFirstInBlock) dl.setReferenced();
						bFirstInBlock = false;

						debug("line consumes " + dl.getLength() + " bytes");

						if (bBlankLine) addBlank(bodylines);
						bodylines.add(dl);

						// Advance the pointer in the TOC file by the number of words
						// which are occupied by the line
						tocFile.advanceDataWords(dl.getLength()/2);
						// Advance the counter
						m_Counter.add(dl.getLength());
					}
					else {
					    // ===============================================
					    // Address pointer handling
   					    // ===============================================

						// Besides data elements we have address elements.
						// The checksums are ignored, as well as any other kind
						// of tag
						if (ent.isOfKind(TocEntity.ADDRESS)) {
							Location ct = ent.getLocation();
							// If the address specification does not match the current
							// counter, we have a BSS area or even a mode change
							if (!ct.equals(m_Counter)) {
							    
							    // Is there still an open text line?
							    if (sbText.length()>0) {
							        // Flush the TEXT line
							        DisassembledLine dl2 = handleTextArea(sbText, (byte)0, symboltable, nShowDataLocs);
							        dl2.setLocation(cntLast);
							        checkReferenced(dl2, symboltable, deftable);
							        bodylines.add(dl2);
							        continue;
							    }
					    						    
								debug("Address change. Expected = " + m_Counter + ", new = " + ct);
								bFirstInBlock = true;
								if ((m_Counter.getAddressingType() == ct.getAddressingType()) && (m_Counter.getSegment() == ct.getSegment())) {
									debug("Addressing mode unchanged");
									
									// =======================================================
									//    Address spec, same mode 
									
									// Use a BSS line
									int nBssSize = ct.getValue() - m_Counter.getValue();
									if (nBssSize > 0 && nBssSize < 16384) {
										la1 = new LineArgument(T_COUNT, nBssSize);
										dl = new DisassembledLine(m_Counter, C_BSS, la1, nShowDataLocs);
										// Is that BSS line referenced?
										// if (symboltable.containsKey(m_Counter.toString())) dl.setReferenced();
										// All BSS lines should be referenced
										dl.setReferenced();
										// Is there a DEF for this BSS?
										for (TocEntity def : deftable) {
											if (def.getLocation().equals(m_Counter)) {
												dl.setReferenced();
												dl.addLabel(def.getStringValue());
												addBlank(bodylines);
											}
										}
									}
									else {
										// This is a negative or an implausible high offset; let's use AORG (abs) or RORG
										if (m_Counter.getAddressingType()==ABSOLUTE) {
											la1 = new LineArgument(T_IMM, ct.getValue());
											dl = new DisassembledLine(m_Counter, C_AORG, la1);
										}
										else {
											// Use RORG
											la1 = new LineArgument(T_IMMREL, nBssSize);
											dl = new DisassembledLine(m_Counter, C_RORG, la1);
										}
									}
								}
								else {
									debug("Addressing mode changed");
									// =======================================================
									//    Different mode 
									
									// First close all currently open segments
									if (m_Counter.getAddressingType()==CRELOC) {
										// addBlank(bodylines);
										bodylines.add(new DisassembledLine(m_Counter, C_CEND, null));
									}
									else {
										if (m_Counter.getAddressingType()==DRELOC) {
											// addBlank(bodylines);
											bodylines.add(new DisassembledLine(m_Counter, C_DEND, null));
										}										
									}

									addBlank(bodylines);
									if (ct.getAddressingType()==ABSOLUTE) {
										m_Counter = aCounter;
										la1 = new LineArgument(T_IMM, ct.getValue());
										dl = new DisassembledLine(m_Counter, C_AORG, la1);
									}
									else {
										if (ct.getAddressingType()==PRELOC) {
											if (m_Counter.getAddressingType()==ABSOLUTE) {
												dl = new DisassembledLine(ct, C_RORG, null);
												// TODO: Check for address skew
												debug("RORG");
											}
											else dl = null;
											m_Counter = pCounter;
										}
										else {
											m_Counter = aSegmentCounter[ct.getSegment()];
											if (ct.getSegment()==0) {
												debug("DSEG");
												dl = new DisassembledLine(m_Counter, C_DSEG, null);
											}
											else {
												if (ct.getSegment()==1) {
													debug("CSEG");
													la1 = null;
												}								
												else {
													debug("CSEG '" + asSegment[ct.getSegment()] + "'");
													la1 = new LineArgument(asSegment[ct.getSegment()], true);
												}
												dl = new DisassembledLine(m_Counter, C_CSEG, la1);
											}
										}										
									}	
								}
								if (dl != null) bodylines.add(dl);
								m_Counter.setFrom(ct);								
							}
						}						
						tocFile.advance(1);
					}
				}
				else bEndOfFile = true;
			}
			// File has been read to its end
			
			// Terminate the last open CSEG or DSEG
			if (m_Counter.getAddressingType()==CRELOC) {
				// addBlank(bodylines);
				bodylines.add(new DisassembledLine(m_Counter, C_CEND, null));
			}
			else {
				if (m_Counter.getAddressingType()==DRELOC) {
					// addBlank(bodylines);
					bodylines.add(new DisassembledLine(m_Counter, C_DEND, null));
				}										
			}
			
			// Blank line
			addBlank(bodylines);
			
			// Add END with or without entry
			TocEntity entry = tocFile.getLast(TocEntity.ENTRY);
			if (entry!=null) {
				// Check for DEFined labels
				for (TocEntity def : deftable) {
					if (def.getLocation().equals(entry.getLocation())) {
						entry.getSymbol().setName(def.getStringValue());
						break;
					}
				}
					
				LineArgument la = new LineArgument(entry.getSymbol());
				la.setAddressingType(T_JUMP);
				dl = new DisassembledLine(m_Counter, C_END, la);
				bodylines.add(dl);
				symboltable.put(entry.getSymbol().toString(""), entry.getSymbol());
			}
			else {
				entry = tocFile.getLast(TocEntity.END);
				dl = new DisassembledLine(m_Counter, C_END, null);
				bodylines.add(dl);
			}			

			// Do we have DEFs for the END?
			for (TocEntity def : deftable) {
				if (def.getLocation().equals(m_Counter)) {
					dl.setReferenced();
					dl.addLabel(def.getStringValue());
				}
			}
			
			resolveRefs(bodylines, reftable);
			
			// Collect all argument symbols for the next pass
			int nDataWords = 0;
			for (DisassembledLine d : bodylines) {
				if (d.isDirective()) continue;
				la1 = d.getLineArgument(0);
				
				if (la1!=null) {
					Symbol sym = la1.getSymbol();
					if (sym!=null) {
						// Check if we have a DEF for this symbol
						// If available, set the symbol's name from the DEF so
						// we get branch commands to DEF'd names
						for (TocEntity def : deftable) {
							if (def.getLocation().toString().equals(sym.toString())) {
								sym.setName(def.getStringValue());
							}
						}
						
						// If the symbol is REF'd, it cannot be defined in this
						// file. Otherwise, we store the symbol in the branchtable
						if (!sym.isReferenced()) {
							symboltable.put(sym.toString(), sym);
							if ((d.isBranchToAddressCommand() || d.isJumpToAddressCommand()) && !sym.isAbsolute()) {
								// debug("branch " + sym);
								branchtable.put(sym.toString(), sym);
							}
						}
					}
				}
				la1 = d.getLineArgument(1);
				if (la1!=null) {
					Symbol sym = la1.getSymbol();
					symboltable.put(sym.toString(), sym);
				}
				
				// Check for line arguments
				if (d.isBranchToAddressCommand()) {
				    for (int i=0; i < hint.length; i++) {
				        LineArgument la = d.getLineArgument(0);
				        String sName = la.getSymbol().getName();
				        int nWords = 0;
				        if (sName != null) {
				            // Branch to a symbol with a name
				            if (hint[i].definesParamsFor(sName)) {
				                Location loc = d.getLocation();
				                Hint hnew = new Hint(Hint.DATA, loc.getLocationAfter(4), loc.getLocationAfter(4+hint[i].getParamCount()*2-1));
				                dataareas.add(hnew);
				            }
				        }
				        else {
				            // Branch to a symbol with a name
				            if (hint[i].definesParamsFor(la.getSymbol().toLocation())) {
				                Location loc = d.getLocation();			                
				                Hint hnew = new Hint(Hint.DATA, loc.getLocationAfter(4), loc.getLocationAfter(4+hint[i].getParamCount()*2-1));
				                dataareas.add(hnew);				                
				            }
				        }
				    }
				}
			}
			
			debug(symboltable);
			debug(branchtable);
			
			nPass--;
		}
		
		// Add the body lines to the list
		if (bodylines != null) {
			
			// Now tidy up. We look for remaining symbols in the symboltable that
			// do not have a corresponding label and replace them by a sum of the 
			// previous label and an offset
			
			for (DisassembledLine dld : bodylines) {
				debug("Checking " + dld);
				if (dld.isDirective() && !dld.isBSS()) continue;
				la1 = dld.getLineArgument(0);
				if (la1!=null && la1.hasSymbol()) {
					Symbol sym = la1.getSymbol();
					// Not interested in REF'd symbols
					if (!sym.isReferenced()) {
						searchLocation(sym, bodylines);
					}
				}
				la1 = dld.getLineArgument(1);
				if (la1!=null && la1.hasSymbol()) {
					Symbol sym = la1.getSymbol();
					if (!sym.isReferenced()) {
						searchLocation(sym, bodylines);
					}
				}
			}
			
			for (DisassembledLine dld : bodylines) {
				lines.add(dld);
			}
		}

		// Print out the list of disassembled lines
		for (DisassembledLine line : lines) {
			if (sb.length()>0) sb.append("\n");
			sb.append(line.toString());
		}

		return sb.toString(); 
	}
	
	/** Check references. */
	private boolean checkReferences(DisassembledLine dl, TocEntity[] deftable, LinkedList<DisassembledLine> bodylines, Map<String,Symbol> symboltable) {
	    // If the symbol table from the first pass 
	    // contains the current location, set the line as referenced
	    if (symboltable.containsKey(m_Counter.toString())) dl.setReferenced();
	    
	    // Check for DEFined labels
	    // Add DEF values in case they reference the location of 
	    // the current line
	    boolean bAddLine = false;
	    for (TocEntity def : deftable) {
	        if (def.getLocation().equals(m_Counter)) {
	            dl.setReferenced();
	            dl.addLabel(def.getStringValue());
	            bAddLine = true;
	        }
	    }
	    return bAddLine;
	}
	
	private void checkReferenced(DisassembledLine dl, Map<String,Symbol> symboltable, TocEntity[] deftable) {
	    Location count = dl.getLocation();
	    if (count==null) {
	    	System.err.println("Line \"" + dl + "\" has no location");
	    	return;
	    }
	    if (symboltable.containsKey(count.toString())) dl.setReferenced();
	    
	    for (TocEntity def : deftable) {
	        if (def.getLocation().equals(count)) {
	            dl.setReferenced();
	            dl.addLabel(def.getStringValue());
	        }
	    }
	}
	    
	
	/** Handle the text area. */
	private DisassembledLine handleTextArea(StringBuilder sbText, int byText, Map<String,Symbol> symboltable, int nShowDataLocs) {
	    // Is the current byte at a referenced location?
	    boolean bReferenced = symboltable.containsKey(m_Counter.toString());
	    
	    if (bReferenced || sbText.length() == 40 || byText < 32 || byText > 126) {
	        // Terminate the last TEXT line
	        if (sbText.length()>0) {
	            LineArgument la = new LineArgument(sbText.toString(), true);
	            sbText.delete(0, sbText.length());
	            la.setAddressingType(T_TEXT);
	            return new DisassembledLine(null, C_TEXT, la, nShowDataLocs);
	        }
	        // We must call this method once more
	    }
	    
	    if (byText >= 32 && byText <= 126) {
	        // First byte is printable → put it in the TEXT line
	        sbText.append((char)byText);
	        return null;
	    }
	    else {
	        LineArgument la = new LineArgument(ABSOLUTE, byText & 0xff);
	        la.setAddressingType(T_BYTE);
	        return new DisassembledLine(null, C_BYTE, la, nShowDataLocs);
	    }
	}
	
	private void searchLocation(Symbol sym, LinkedList<DisassembledLine> bodylines) {
		DisassembledLine dlBest = null;
		int nDist = INFTY;
		Location addr = null;
		debug("Searching a label for " + sym);
		for (DisassembledLine dld : bodylines) {
			// Go through all lines and search the closest label
			// that is below the symbol's value
			if (!dld.mayHaveLabels()) continue;
			addr = dld.getLocation();
			if (addr==null) {
				System.err.println("Line \"" + dld + "\" has no location");
				continue;
			}

			debug(addr + ":" + dld);
			// If we match, break
			int nDiff = -addr.diffTo(sym); 
			if (nDiff==0) {
				debug("Match: counter " + addr + " for symbol " + sym);
				nDist = 0;
				dlBest = dld;
				break;
			}
			else {
				if (nDiff > 0) {
					debug("Diff is " + nDiff);
					if (nDiff < nDist) {
						dlBest = dld;
						nDist = nDiff;
					}
				}
				else {
					debug("Diff is " + nDiff);
				}
			}
		}
		if (nDist < INFTY) {
			if (dlBest != null) {
				debug("Best match at line " + dlBest);
				dlBest.setReferenced();
			}
			sym.setOffset(nDist);
		}
	}


	private void addBlank(LinkedList<DisassembledLine> lines) {
		if (lines.size()==0) return;
		DisassembledLine dl = lines.getLast();
		if (!dl.isBlankLine()) lines.add(new DisassembledLine());
	}

	
	/** Adds labels to the symbols that are referenced in a REF chain.
		As a result, the lines now show the referenced text symbols.
		@param lines List of disassembled lines.
		@param reftable List of REF entities from the TOC file		
	*/
	private void resolveRefs(List<DisassembledLine> lines, TocEntity[] reftable) {
		LineArgument la = null;
		for (TocEntity ref : reftable) {
			Location rc = ref.getLocation();
			// Going backwards
			for (int i=lines.size()-1; i >=0; i--) {
				DisassembledLine line = lines.get(i);

				// Directives do not use references
				if (line.isDirective()) continue;

				// Data lines only have one argument but no command
				if (line.isDataLine()) {
					Location addr = line.getLocation();
					la = line.getLineArgument(0);
					if (addr.equals(rc)) {
						Symbol sym = la.getSymbol();
						// debug("Compare " + addr + " with " + rc + ", symbol = " + sym);
						// Set the symbol name
						sym.setName(ref.getStringValue());
						rc = new Location(sym.getMode(), sym.getValue(), sym.getSegment());
						// debug("new counter = " + rc);
						continue;
					}
				}
				// Check the second argument
				la = line.getLineArgument(1);
				if (la!=null) {
					if (la.hasValue()) {
						Location addr = line.getLocation(2);
						if (addr.equals(rc)) {
							Symbol sym = la.getSymbol();
							// debug("Compare " + addr + " with " + rc + ", symbol = " + sym);
							sym.setName(ref.getStringValue());
							sym.setReferenced(true);
							// Follow the chain; take the value from this line
							rc = new Location(sym.getMode(), sym.getValue(), sym.getSegment());
							// debug("new counter = " + rc);
						}
						// else debug("no match, loc2=" + addr);
					}
					// else debug("arg2 has no symbol");
				}
				// else debug("No two args");
				if (rc.getValue()==0 && rc.getAddressingType()==ABSOLUTE) {
					// debug("Chain end");
					break;
				}
				
				// Check first argument
				la = line.getLineArgument(0);
				if (la != null) {
					// debug("1 argument: " + line);
					if (la.hasValue()) {
						Location addr = line.getLocation(1);
						if (addr.equals(rc)) {
							Symbol sym = la.getSymbol();
							// debug("Compare " + addr + " with " + rc + ", symbol = " + sym);
							sym.setName(ref.getStringValue());
							sym.setReferenced(true);
							rc = new Location(sym.getMode(), sym.getValue(), sym.getSegment());
							// debug("new counter = " + rc);
						}
						// else debug("no match, loc1=" + addr);
					}
					// else debug("arg1 has no symbol");
				}
			}
		}
	}
}

// =======================================================================================================
// //////////////////////////////////////////////////////////////////////////////////////////////////////

/*

TODO:
	Editor/Assembler Disk 1: SAVE
	TEXT does not play well with ref line (ignores it)
    Line "          TEXT ' '" has no location
    
    

Thanks to Thierry Nouspikel for the remaining codes
	
0x01 / int16 byte[8] = Identifier, compressed object code
0 / int16 byte[8] = Identifier (all following int16, byte represented as hex strings)
1 / int16 = Program entry ("END XXX"), absolute address
2 / int16 = Program entry, relocatable address
3 / int16 byte[6] = REF (relocatable address, symbol)
4 / int16 byte[6] = REF (absolute address, symbol)
5 / int16 byte[6] = DEF (relocatable address, symbol)
6 / int16 byte[6] = DEF (absolute address, symbol)
7 / int16 = Checksum (value)
8 / int16 = Checksum (value, ignored)
9 / int16 = Absolute location (location counter, from AORG)
A / int16 = Program-relocatable location (location counter, RORG, default)
B / int16 = Absolute contents
C / int16 = Content, PSEG offset to be added
D / int16 = Load bias (address where to start loading, not supported)
E / int16 int16 = REF offset (not supported)
F / null = end of record
G / int16 byte[6] = Symbol table dump (not supported), PSEG
H / int16 byte[6] = Symbol table dump (not supported), AORG
I / byte[8] = Program ID (for symbol table)
J / int16 byte[6] int16 = Symbol table dump (not supported), CSEG, DSEG
K / int16 byte[6] = External macro reference (not supp)
M / int16 byte[6] int16 = Segment declaration (length in bytes, $DATA for DSEG and $<name> for CSEG '<name>', id),
                          id=0000 for DSEG, 0001 for CSEG<blank>, 0xxx for other CSEG
N / int16 int16 = Common-relocatable content, CSEG offset to be added (value, id)
P / int16 int16 = Common-relocatable location (value, cseg_id)
Q / int16 int16 = COBOL segment reference 
R / int16 int16 = Repeat count 
S / int16 =  Data-relocatable location (value)
T / int16 =  Data-relocatable content, DSEG offset to be added
U / int16 byte[6] = Load symbol
V / int16 byte[6] = Secondary reference, PSEG
W / int16 byte[6] int16 = DEF in DSEG or CSEG (location, name, segment_id)
X / int16 byte[6] int16 = REF in DSEG or CSEG (location, name, segment_id)
Y / int16 = SREF absolute
Z / int16 int16 = SREF data-relocatable, common-relocatable
: / string = End of file





	
	===================================================================
	Zum Thema PSEG/DSEG/CSEG:	

	CSEG können mehrere auftreten, jedes mit eigenem Namen; jedes CSEG bekommt
	eine neue Segmentnummer 
	
	DATA = SN 0
	BLANK = SN 1
	CSEG 'name' = SN 2 ...
	
0 0018 "DSEGTS  "
M 0014 "$DATA " 0000    DSEG '<name>' akzeptiert, aber ignoriert
M 0004 "$BLANK" 0001    <name> aus CSEG '<name>', sonst <name>=BLANK
A 0000
B FFFF
B 0201
T 0000
7 F1C4
F
A 0006
B C811
N 0000 0001
B 04E0
C 0000
S 0000
B 1234
B 4321
B 00FF
P 0000 0001
B 1122
7 F284
F
P 0002 0001
B 3344
S 0006
B 5448
B 4553
B 4520
B 4152
B 4520
B 4441
B 5441
A 000E
B 16FB
7 F215
F
A 0010
B 0202
T 0006
B 0460
C 0002
7 FA96
F
5 0002 "MZTEST"
7 FCEB
F

 99/4 ASSEMBLER
VERSION 1.2                                                  PAGE 0001
  0001                   IDT  'DSEGTS'  
  0002                   DEF  MZTEST  
  0003                
  0004 0000 FFFF  PDATA  DATA >FFFF   
  0005                
  0006 0002 0201  MZTEST LI   R1,DVAR   
       0004 0000" 
  0007 0006 C811  LOOP   MOV  *R1,@CVAR   
       0008 0000+ 
  0008 000A 04E0         CLR  @PDATA  
       000C 0000' 
  0009                
  0010 0000              DSEG   
  0011 0000 1234  DVAR   DATA >1234   
  0012 0002 4321         DATA >4321   
  0013 0004 00FF         DATA >00FF   
  0014 0006              DEND   
  0015                
  0016 0000              CSEG   
  0017 0000 1122  CVAR   DATA >1122,>3344   
       0002 3344  
  0018 0004              CEND   
  0019                
  0020 0006              DSEG   
  0021 0006   54  DVAR1  TEXT 'THESE ARE DATA'  
  0022 0014              DEND   
  0023                
  0024 000E              PSEG   
  0025 000E 16FB  PADR   JNE  LOOP  
  0026 0010 0202         LI   R2,DVAR1  
       0012 0006" 
  0027 0014 0460         B    @MZTEST   
       0016 0002' 
  0028 0018              PEND   
  0029                
  0030                   END  

99/4 ASSEMBLER
VERSION 1.2                                                  PAGE 0002
  + CVAR    0000    " DVAR    0000    " DVAR1   0006    ' LOOP    0006    
  D MZTEST  0002    ' PADR    000E    ' PDATA   0000      R0      0000    
    R1      0001      R10     000A      R11     000B      R12     000C    
    R13     000D      R14     000E      R15     000F      R2      0002    
    R3      0003      R4      0004      R5      0005      R6      0006    
    R7      0007      R8      0008      R9      0009    
  0000 ERRORS
*/
