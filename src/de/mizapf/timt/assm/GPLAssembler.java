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
	
	Copyright 2013 Michael Zapf
	www.mizapf.de
	
****************************************************************************/

package de.mizapf.timt.assm;
import java.util.*;
import java.io.*;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.FormatException;

public class GPLAssembler {

	int		m_nPos;
	byte[]	m_abyCode;
	
	int		m_nFMTaddress;
	
	static	Command[] command; 
	static	int[] mask = { 0, 0xfd, 0xff, 0xff, 
						0xe0, 0xff, 0xe0, 0xff, 0xfc };
		
	static {
		List<Command> lst = new ArrayList<Command>();
		lst.add(new Command("RTN  ", 0x00, 3));
		lst.add(new Command("RTNC ", 0x01, 3));
		lst.add(new Command("RAND ", 0x02, 2));
		lst.add(new Command("SCAN ", 0x03, 3));
		lst.add(new Command("BACK ", 0x04, 2));
		lst.add(new Command("B    ", 0x05, 2));
		lst.add(new Command("CALL ", 0x06, 2));
		lst.add(new Command("ALL  ", 0x07, 2));
		lst.add(new Command("FMT  ", 0x08, 7));
		lst.add(new Command("H    ", 0x09, 3));
		lst.add(new Command("GT   ", 0x0a, 3));
		lst.add(new Command("EXIT ", 0x0b, 3));
		lst.add(new Command("CARRY", 0x0c, 3));
		lst.add(new Command("OVF  ", 0x0d, 3));
		lst.add(new Command("PARSE", 0x0e, 2));
		lst.add(new Command("XML  ", 0x0f, 2));
		lst.add(new Command("CONT ", 0x10, 3));
		lst.add(new Command("EXEC ", 0x11, 3));
		lst.add(new Command("RTNB ", 0x12, 3));
		lst.add(new Command("RTGR ", 0x13, 3));
		lst.add(new Command("XGPL ", 0x14, 8));
		lst.add(new Command("XGPL ", 0x18, 8));
		lst.add(new Command("XGPL ", 0x1c, 8));
		lst.add(new Command("MOVE ", 0x20, 6));
		lst.add(new Command("BR   ", 0x40, 4));
		lst.add(new Command("BS   ", 0x60, 4));
		lst.add(new Command("ABS  ", 0x80, 5));
		lst.add(new Command("DABS ", 0x81, 5));
		lst.add(new Command("NEG  ", 0x82, 5));
		lst.add(new Command("DNEG ", 0x83, 5));
		lst.add(new Command("INV  ", 0x84, 5));
		lst.add(new Command("DINV ", 0x85, 5));
		lst.add(new Command("CLR  ", 0x86, 5));
		lst.add(new Command("DCLR ", 0x87, 5));
		lst.add(new Command("FETCH", 0x88, 5));
		lst.add(new Command("FETCH", 0x89, 5));
		lst.add(new Command("CASE ", 0x8a, 5));
		lst.add(new Command("DCASE", 0x8b, 5));
		lst.add(new Command("PUSH ", 0x8c, 5));
		lst.add(new Command("PUSH ", 0x8d, 5));
		lst.add(new Command("CZ   ", 0x8e, 5));
		lst.add(new Command("DCZ  ", 0x8f, 5));
		lst.add(new Command("INC  ", 0x90, 5));
		lst.add(new Command("DINC ", 0x91, 5));
		lst.add(new Command("DEC  ", 0x92, 5));
		lst.add(new Command("DDEC ", 0x93, 5));
		lst.add(new Command("INCT ", 0x94, 5));
		lst.add(new Command("DINCT", 0x95, 5));
		lst.add(new Command("DECT ", 0x96, 5));
		lst.add(new Command("DDECT", 0x97, 5));
		lst.add(new Command("XGPL ", 0x98, 8));
		lst.add(new Command("XGPL ", 0x9c, 8));
		lst.add(new Command("ADD  ", 0xa0, 1));
		lst.add(new Command("DADD ", 0xa1, 1));
		lst.add(new Command("SUB  ", 0xa4, 1));
		lst.add(new Command("DSUB ", 0xa5, 1));
		lst.add(new Command("MUL  ", 0xa8, 1));
		lst.add(new Command("DMUL ", 0xa9, 1));
		lst.add(new Command("DIV  ", 0xac, 1));
		lst.add(new Command("DDIV ", 0xad, 1));
		lst.add(new Command("AND  ", 0xb0, 1));
		lst.add(new Command("DAND ", 0xb1, 1));
		lst.add(new Command("OR   ", 0xb4, 1));
		lst.add(new Command("DOR  ", 0xb5, 1));
		lst.add(new Command("XOR  ", 0xb8, 1));
		lst.add(new Command("DXOR ", 0xb9, 1));
		lst.add(new Command("ST   ", 0xbc, 1));
		lst.add(new Command("DST  ", 0xbd, 1));
		lst.add(new Command("EX   ", 0xc0, 1));
		lst.add(new Command("DEX  ", 0xc1, 1));
		lst.add(new Command("CH   ", 0xc4, 1));
		lst.add(new Command("DCH  ", 0xc5, 1));
		lst.add(new Command("CHE  ", 0xc8, 1));
		lst.add(new Command("DCHE ", 0xc9, 1));
		lst.add(new Command("CGT  ", 0xcc, 1));
		lst.add(new Command("DCGT ", 0xcd, 1));
		lst.add(new Command("CGE  ", 0xd0, 1));
		lst.add(new Command("DCGE ", 0xd1, 1));
		lst.add(new Command("CEQ  ", 0xd4, 1));
		lst.add(new Command("DCEQ ", 0xd5, 1));
		lst.add(new Command("CLOG ", 0xd8, 1));
		lst.add(new Command("DCLOG", 0xd9, 1));
		lst.add(new Command("SRA  ", 0xdc, 1));
		lst.add(new Command("DSRA ", 0xdd, 1));
		lst.add(new Command("SLL  ", 0xe0, 1));
		lst.add(new Command("DSLL ", 0xe1, 1));
		lst.add(new Command("SRL  ", 0xe4, 1));
		lst.add(new Command("DSRL ", 0xe5, 1));
		lst.add(new Command("SRC  ", 0xe8, 1));
		lst.add(new Command("DSRC ", 0xe9, 1));
		lst.add(new Command("COINC", 0xec, 1));
		lst.add(new Command("COINC", 0xed, 1));
		lst.add(new Command("I/O  ", 0xf4, 1));
		lst.add(new Command("DI/O ", 0xf5, 1)); // I/O
		lst.add(new Command("SWGR ", 0xf8, 1));
		lst.add(new Command("SWGR ", 0xf9, 1));
		lst.add(new Command("XGPL ", 0xf0, 8));
		lst.add(new Command("XGPL ", 0xfc, 8));

		command = lst.toArray(new Command[0]);
	}
	
	public static void main(String[] arg) {
		byte[] content = null;
		int nAddress = 0;
		int nOffset = 0;

		if (arg.length<2) {
			System.err.println("de.mizapf.timt.assm.GPLAssembler <file> <startaddr> <offset> | direct <byte> ...");
			return;
		}
		
		if (arg[0].equalsIgnoreCase("direct")) {
			content = new byte[arg.length-1];
			for (int i=1; i < arg.length; i++) {
				content[i-1] = (byte)Integer.parseInt(arg[i], 16);
			}
		}
		else {
			try {
				nAddress = Integer.parseInt(arg[1], 16);
				nOffset = Integer.parseInt(arg[2], 16);
				DataInputStream dis = new DataInputStream(new FileInputStream(arg[0]));
				content = new byte[dis.available()];
				dis.readFully(content);
				dis.close();
			}
			catch (NumberFormatException nfx) {
				System.err.println("Start address must be a hex number: " + arg[1]);
				content = null;
			}
			catch (FileNotFoundException fnfx) {
				System.err.println("File not found: " + fnfx);
				content = null;
			}
			catch (IOException iox) {
				iox.printStackTrace();
				content = null;
			}
		}
		if (content != null) {
			GPLAssembler gasm = new GPLAssembler();
			System.out.println(gasm.disassemble(content, nOffset, nAddress, content.length-nOffset, new Hint[0], true, 0));
		}
	}
		
	private String getOperand(boolean bImmediate, boolean bWord) {
		StringBuilder sb = new StringBuilder();
		int nOperand = m_abyCode[m_nPos++] & 0xff;
		int nAddress = 0;

		if (bImmediate) {
			if (bWord) {
				nOperand = (nOperand << 8) | (m_abyCode[m_nPos++] & 0xff);
			}	
			sb.append(">").append(Utilities.toHex(nOperand, bWord? 4 : 2, true));
			return sb.toString();
		}
		
		if ((nOperand & 0x80)==0) {
			// Format 1
			nAddress = (nOperand & 0x7f) | 0x8300;
			sb.append("@>").append(Utilities.toHex(nAddress, 4, true));
		}
		else {
			boolean bVideo = ((nOperand & 0x20)!=0);
			boolean bIndirect = ((nOperand & 0x10)!=0);

			if ((nOperand & 0x0f)!=0x0f) {
				// Format 2 or 3
				nAddress = ((nOperand & 0x0f)<<8) | (m_abyCode[m_nPos++] & 0xff);
			}
			else {
				// Format 4 or 5
				nAddress = (m_abyCode[m_nPos++]&0xff) << 8;
				nAddress |= m_abyCode[m_nPos++] & 0xff;
			}
			if (!bVideo || bIndirect) nAddress = (nAddress + 0x8300) & 0xffff;
			if (bVideo) sb.append("VDP");
			sb.append(bIndirect? "*>" : "@>");
			sb.append(Utilities.toHex(nAddress, 4, true));
			
			if ((nOperand & 0x40)==0x40) {
				// Format 3 or 5
				nAddress = (m_abyCode[m_nPos++]&0xff) | 0x8300;
				sb.append("(@>").append(Utilities.toHex(nAddress, 4, true)).append(")");
			}
		}
		return sb.toString();
	}
	
	private String getText(int nLength) {
		StringBuilder sb = new StringBuilder();
		boolean bStartedBytes = false;
		boolean bStartedString = false;
		int by;
		for (int i=0; i < nLength; i++) {
			by = m_abyCode[m_nPos++] & 0xff;
			if ((by < 0x20) || (by > 0x7e) || (by == 0x22)) {
				if (bStartedString) sb.append("\"");
				if (sb.length()>0) sb.append(",");
				sb.append(">").append(Utilities.toHex(by,2, true));
				bStartedBytes = true;
				bStartedString = false;
			}
			else {
				if (bStartedBytes) sb.append(",");
				if (!bStartedString) sb.append("\"");
				sb.append((char)by);
				bStartedString = true;
				bStartedBytes = false;
			}
		}
		if (bStartedString) sb.append("\"");
		return sb.toString();
	}
	
	private String doFormat(int nStart, int nShowDataLoc) {
		/* We introduce another source code interpretation here, as the one
			used in some listings is hard to read, and the "official" variant
			in GPL source code from TI contains high-level structures which
			we cannot reproduce either. */
		StringBuilder sb = new StringBuilder();
		StringBuilder line = new StringBuilder();
		int nDepth = 0;
		int nCount = 0;
		int nByte = 0;
		int nAddress;
		int nPrevPos = m_nPos;
				
		/*

		0x xx..xx	PRINTH "..." (len=x)
		1x xx..xx	PRINTH "..." (len=x+10)
		2x xx..xx	PRINTV "..." (len=x)
		3x xx..xx	PRINTV "..." (len=x+10)
		4x bb		PRINTH x+1 TIMES >bb
		5x bb		PRINTH x+11 TIMES >bb
		6x bb		PRINTV x+1 TIMES >bb
		7x bb		PRINTV x+11 TIMES >bb
		8x			RIGHT x
		9x			RIGHT x+10
		ax			DOWN x
		bx			DOWN x+10
		cx			FOR x+1 TIMES DO
		dx			FOR x+11 TIMES DO
		ex aa (aa)	PRINTH x+1 FROM @>aaaa
		fx aa (aa)	PRINTH >11+x FROM @>aaaa  (x < 0b)
		fb (aa aa)	END (REPEAT FROM GROM@>aaaa) 
		fc xx		BIAS=>xx
		fd aa (aa)	BIAS=(@>aaaa)
		fe xx		ROW=>xx
		ff xx		COL=>xx
		*/
		sb.append('\n');
		while (nDepth >= 0) {
			line = new StringBuilder();
			line.append(Utilities.toHex(m_nPos + nStart, 4, true)).append(": ");
			int nCode = m_abyCode[m_nPos++];
			nCount = (nCode & 0x1f)+1;
			
			for (int i=0; i <=nDepth; i++) line.append("...");
			
			switch ((nCode >> 4) & 0x0f) {
			case 0: 
			case 1:
				// PRINTH direct
				line.append("PRINTH ").append(getText(nCount));
				break;
			case 2: 
			case 3:
				// PRINTV direct
				line.append("PRINTV ").append(getText(nCount));
				break;
			case 4:
			case 5:
				line.append("PRINTH ").append(nCount).append(" TIMES >");
				nByte = m_abyCode[m_nPos++] & 0xff;
				line.append(Utilities.toHex(nByte, 2, true));
				break;
			case 6:
			case 7:
				line.append("PRINTV ").append(nCount).append(" TIMES >");
				nByte = m_abyCode[m_nPos++] & 0xff;
				line.append(Utilities.toHex(nByte, 2, true));
				break;
			case 8:
			case 9:
				line.append("RIGHT ").append(nCount);
				break;
			case 10:
			case 11:
				line.append("DOWN ").append(nCount);
				break;
			case 12:
			case 13:
				line.append("FOR ").append(nCount).append(" TIMES DO");
				nDepth++;
				break;
			case 14:
			case 15:
				if (nCount < 0x1c) {  // count is one less
					line.append("PRINTH ").append(nCount).append(" BYTES FROM ").append(getOperand(false, false));
				}
				else {
					if (nCount == 0x1c) {
						line.delete(line.length()-3, line.length());
						nDepth--;
						if (nDepth >= 0) m_nPos += 2;
						if (nDepth == 0) m_nFMTaddress = -1;
						line.append("END");
					}
					else {
						if (nCount == 0x1d) {
							nByte = m_abyCode[m_nPos++] & 0xff;
							line.append("BIAS >");
							line.append(Utilities.toHex(nByte,2, true));
						}
						else {
							if (nCount == 0x1e) {
								line.append("BIAS ");
								line.append(getOperand(false, false));
							}
							else {
								if (nCount == 0x1f) line.append("ROW ");
								else line.append("COL ");
								nByte = m_abyCode[m_nPos++] & 0xff;
								line.append(nByte);
							}
						}
					}
				}
				break;
			}
			if ((nShowDataLoc & Hint.OPT_SHOWDATA)!=0) {
				for (int j=line.length(); j < 65; j++) line.append(" ");
				
				if ((m_nPos - nPrevPos) > 10) {
					for (int i=0; i < 8; i++) {
						line.append(Utilities.toHex(m_abyCode[i+nPrevPos],2, true)).append(" ");
					}
					line.append("[...] ");
					for (int i=(m_nPos - nPrevPos)-2 ; i < (m_nPos - nPrevPos); i++) {
						line.append(Utilities.toHex(m_abyCode[i+nPrevPos],2, true)).append(" ");
					}
				}
				else {
					for (int i=0; i < m_nPos - nPrevPos; i++) {
						line.append(Utilities.toHex(m_abyCode[i+nPrevPos],2, true)).append(" ");
					}
				}
			}
			nPrevPos = m_nPos;
			sb.append(line.toString());
			if (nDepth >= 0) sb.append('\n');
		}
		return sb.toString();
	}
	
	/** Disassembles the code as GPL.
		@param code GPL code from memory
		@param nOffset Offset from the beginning of the file
		@param nStart Start address. Address of the byte at nOffset. 
		@param nLength Number of bytes starting at nOffset.
		@param skip List of skip specifications.
		@param bSkipInvalid Skip invalid GROM addresses (1800-1fff etc.)
	*/
	public String disassemble(byte[] code, int nOffset, int nStart, int nLength, Hint[] hint, boolean bSkipInvalid, int nShowDataLoc) {
		StringBuilder sb = new StringBuilder();
		StringBuilder line;
		
		m_nPos = nOffset;
		boolean bFound = false;
		m_abyCode = code;
		String sSource = "";
		String sDest = "";
		int nReqBytes = 0;
		int nPrevPos = 0;
		int nOperand = 0;
		int nlen = 0;
		byte opcode;
		boolean bGRAM = false;
		int nLastCallAddr = -1;
		int nLastCallPos = -1;
		int nCurrentAddress = 0;
		m_nFMTaddress = -1;
		StringBuilder sbData = new StringBuilder();
		StringBuilder sbText = new StringBuilder();
		int nBias = 0;
		int nLastDataPos = -1;
		int nLastTextPos = -1;
		
		// Cut at the end
		if (nLength+nOffset > code.length) nLength = code.length - nOffset;
				
		try {
			while (m_nPos - nOffset + nStart < nStart + nLength) {

				nCurrentAddress = m_nPos - nOffset + nStart;

				line = new StringBuilder();
				if (bSkipInvalid && ((nCurrentAddress & 0x1800)==0x1800)) {
					// Do we need to flush a DATA or TEXT line?
					if (sbText.length() > 0) {
						sb.append(Utilities.toHex(nLastTextPos, 4, true));
						sb.append((nBias != 0)? ": BTEXT '" : ": TEXT '").append(sbText.toString()).append("'\n");
						sbText.delete(0, sbText.length());
						nLastTextPos = -1;
					}
					if (sbData.length() > 0) {
						sb.append(Utilities.toHex(nLastDataPos, 4, true));
						sb.append(": DATA  ").append(sbData.toString()).append("\n");
						sbData.delete(0, sbData.length());
						nLastDataPos = -1;
					}			

					line.append("\n-- Skip to next GROM --\n\n");
					sb.append(line);
					m_nPos = ((nCurrentAddress + 0x800)&0xe000) - nStart + nOffset;
					
					continue;
				}
				
				nPrevPos = m_nPos;
				int index = 0;
				bFound = false;
								
				boolean bText = false;
				boolean bData = false;

				for (int i=0; i < hint.length; i++) {
					if (hint[i].getKind()==Hint.TEXT && hint[i].contains(nCurrentAddress)) {
						nBias = hint[i].isBiasedText()? 0x60 : 0x00;
						if (((code[m_nPos]-nBias)&0xff) < 32 || ((code[m_nPos]-nBias)&0xff) > 126) {
							if (nLastDataPos == -1) nLastDataPos = nCurrentAddress;
//							System.out.println("TEXT contains unprintable " + Utilities.toHex(nCurrentAddress, 4));
							bData = true;
							bText = false;
						}
						else { 
							if (nLastTextPos == -1) nLastTextPos = nCurrentAddress;
//							System.out.println("TEXT contains " + Utilities.toHex(nCurrentAddress, 4));
							bData = false;
							bText = true;
						}
						break;
					}
					if (hint[i].getKind()==Hint.DATA && hint[i].contains(nCurrentAddress)) {
//						System.out.println("DATA contains " + Utilities.toHex(nCurrentAddress, 4));
						if (nLastDataPos == -1) nLastDataPos = nCurrentAddress;
						bData = true;
						bText = false;
						break;
					}
					if (hint[i].definesParamsFor(nLastCallAddr) && (nCurrentAddress < nLastCallPos + 3 + hint[i].getParamCount())) {
						// System.out.println("PARAM contains " + Utilities.toHex(nCurrentAddress, 4));
						if (nLastDataPos == -1) nLastDataPos = nCurrentAddress;
						bData = true;
						bText = false;
						break;
					}
				}

				if (bText) {
					if (sbText.length() >= 40) {
						sb.append(Utilities.toHex(nLastTextPos, 4, true));
						sb.append((nBias!=0)? ": BTEXT '" : ": TEXT '").append(sbText.toString()).append("'\n");
						sbText.delete(0, sbText.length());
						nLastTextPos = m_nPos - nOffset + nStart;
					}
					sbText.append((char)((code[m_nPos++]-nBias)&0xff));
				}
				else {
					// No more TEXT
					// We have a TEXT line left
					if (sbText.length() > 0) {
						sb.append(Utilities.toHex(nLastTextPos, 4, true));
						sb.append((nBias!=0)? ": BTEXT '" : ": TEXT '").append(sbText.toString()).append("'\n");
						sbText.delete(0, sbText.length());
						nLastTextPos = -1;
					}
				}
				
				if (bData) {
					if (sbData.length() >= 31) {
						sb.append(Utilities.toHex(nLastDataPos, 4, true));
						sb.append(": DATA  ").append(sbData.toString()).append("\n");
						sbData.delete(0, sbData.length());
						nLastDataPos = m_nPos - nOffset + nStart;
					}
					if (sbData.length() > 0) sbData.append(",");
					sbData.append('>').append(Utilities.toHex(code[m_nPos++], 2, true));
				}
				else {
					if (sbData.length() > 0) {
						sb.append(Utilities.toHex(nLastDataPos, 4, true));
						sb.append(": DATA  ").append(sbData.toString()).append("\n");
						sbData.delete(0, sbData.length());
						nLastDataPos = -1;
					}
				}
				
				if (bText || bData) continue;
				
				opcode = code[m_nPos];
				
				// Search opcode
				while (index < command.length && !bFound) {
					if ((opcode & mask[command[index].format]) == command[index].opcode) {
						bFound = true;
					}
					else index++;
				}

				// Check for nofmt
				// Replace the line by a DATA line
				boolean bNoFmt = false;
				if (command[index].format == 7) {
					for (int i=0; i < hint.length; i++) {
						if (hint[i].getKind()==Hint.NOFMT) {
							if (hint[i].contains(nCurrentAddress)) {
								sb.append(Utilities.toHex(nCurrentAddress, 4, true)).append(": DATA  ").append(">").append(Utilities.toHex(code[m_nPos],2, true)).append("\n");
								m_nPos += 1;
								bNoFmt = true;
								break;
							}
						}
					}
				}
				if (bNoFmt) continue;
				
				line.append(Utilities.toHex(nCurrentAddress, 4, true)).append(": ").append(command[index].name).append(" ");
				
				m_nPos++;
				
				switch (command[index].format) {					
				case 1: 
					sDest = getOperand(false, false);
					sSource = getOperand((opcode & 0x02)!=0, (opcode & 0x01)!=0);
					line.append(sSource).append(",").append(sDest);
					break;
				case 2: 
					nOperand = code[m_nPos++] & 0xff;
					nlen = 2;
					if (opcode==0x05 || opcode==0x06) {
						line.append("GROM@");
						nOperand = (nOperand << 8) | (code[m_nPos++] & 0xff);
						nlen = 4;
						nLastCallAddr = nOperand;
						nLastCallPos = nCurrentAddress;
//						System.out.println(Utilities.toHex(nLastCallPos,4) + ": calling " + Utilities.toHex(nLastCallAddr,4));
					}
					line.append(">").append(Utilities.toHex(nOperand, nlen, true));
					break;
				case 3: 
					// no operand
					break;
				case 4:
					nOperand = (nCurrentAddress & 0xe000) | ((opcode & 0x1f)<<8) | (code[m_nPos++] & 0xff);
					line.append("GROM@>").append(Utilities.toHex(nOperand, 4, true));
					break;
				case 5: 
					line.append(getOperand(false, false));
					break;
				case 6: 
					// MOVE
/*
Format type 6:

0 1 2 3 4 5 6 7
---------------
0 0 1 G R V C N

Number
Destination operand
source operand

G=0: Destination GRAM
   R=0: (in)direct
   R=1: indexed

G=1: Destination VRAM/RAM/VReg
   R=0: VRAM/RAM
   R=1: VReg

V=0: Source GROM/GRAM
   C=0: (in)direct
   C=1: indexed

V=1: Source VRAM/RAM
   C=0: VRAM/RAM
   C=1: <invalid>
    
*/
					// Number N
					line.append(getOperand((opcode & 0x01)!=0, true));
					line.append(" BYTES FROM ");
					
					// Destination
					if ((opcode & 0x08)!=0) {  // R
						// VDP register (G=1, R=1)
						if ((opcode & 0x10)!=0) {   // G
							sDest = "VREG>" + Utilities.toHex(code[m_nPos++], 2, true);
						}
						else {
							// (G=0, R=1; GRAM-indexed)
							nOperand = (code[m_nPos++] & 0xff)<<8;
							nOperand |= code[m_nPos++] & 0xff;
							sDest = "GRAM@>" + Utilities.toHex(nOperand, 4, true);
							nOperand = (code[m_nPos++] & 0xff) | 0x8300;
							sDest = sDest + "(@>" + Utilities.toHex(nOperand, 4, true) + ")";
						}
					}
					else {
						// G=*, R=0
						sDest = (((opcode & 0x10)==0)? "GRAM" : "") + getOperand(false, false);
					}
					
					// Source
					if ((opcode & 0x04)==0) {   // V
						// Source is GROM
						nOperand = (code[m_nPos++] & 0xff)<<8;
						nOperand |= code[m_nPos++] & 0xff;
						line.append("GROM@>").append(Utilities.toHex(nOperand,4, true));

						if ((opcode & 0x02)!=0) {   // C
							// GROM-indexed
							nOperand = (code[m_nPos++] & 0xff) | 0x8300;
							line.append("(@>").append(Utilities.toHex(nOperand, 4, true)).append(")");
						}
					}
					else {
						// Source is not GROM
						line.append(getOperand(false, false));
					}
					
					line.append(" TO ");
					line.append(sDest);					
					break;
				case 7:
					if ((nShowDataLoc & Hint.OPT_SHOWDATA)!=0) {
						for (int j=line.length(); j < 65; j++) line.append(" ");
						line.append(Utilities.toHex(0x08, 2, true));
					}
					
					m_nFMTaddress = nCurrentAddress;

					line.append(doFormat(nStart-nOffset, nShowDataLoc));
					break;
				case 8:
					// just print the opcode again
					line.append(">").append(Utilities.toHex(opcode, 2, true));
					break;
				}

				// Show the data (if not FMT or disabled)
				if (((nShowDataLoc & Hint.OPT_SHOWDATA)!=0) && command[index].format != 7) {
					line.append("    ");
					for (int j=line.length(); j < 65; j++) line.append(" ");
					for (int i=0; i < m_nPos - nPrevPos; i++) {
						line.append(Utilities.toHex(code[i+nPrevPos],2, true)).append(" ");
					}
				}
				sb.append(line).append("\n");
			}
			
			// Do we need to flush a DATA or TEXT line?
			if (sbText.length() > 0) {
				sb.append(Utilities.toHex(nLastTextPos, 4, true));
				sb.append((nBias!=0)? ": BTEXT '" : ": TEXT '").append(sbText.toString()).append("'\n");
				sbText.delete(0, sbText.length());
				nLastTextPos = -1;
			}
			if (sbData.length() > 0) {
				sb.append(Utilities.toHex(nLastDataPos, 4, true));
				sb.append(": DATA  ").append(sbData.toString()).append("\n");
				sbData.delete(0, sbData.length());
				nLastDataPos = -1;
			}			
		}
		catch (ArrayIndexOutOfBoundsException aax) {
			// System.err.println("TIImageTool (disassembler): Clipped end or beyond program code");
			if (m_nFMTaddress != -1) {
				sb.append("\n** File reached its end before the FMT command at " + Utilities.toHex(m_nFMTaddress, 4, true) + " was terminated.");
				sb.append("\n** You should try to exclude that byte from being interpreted as FMT using data or nofmt in the disassembler hints.\n");
			}
			else {
				sb.append("\n** Clipped end, or non-program data.\n");
			}
		}
		return sb.toString();
	}
}
