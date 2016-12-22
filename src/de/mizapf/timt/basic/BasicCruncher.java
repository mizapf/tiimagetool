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
    
    Copyright 2015 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.basic;
import de.mizapf.timt.util.*;
import de.mizapf.timt.files.TFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.ArrayList;
import java.nio.charset.Charset;
import de.mizapf.timt.TIImageTool;
import static de.mizapf.timt.basic.CrunchException.*;

public class BasicCruncher {
	
	int m_inloc = 0;
	String m_line = null;
	int m_linenumber;
	int m_version;
	
	int m_count = 0;
	
	int m_textLine = 0;
	
	public static final int NORMAL = 1;
	public static final int MERGE = 2;
	public static final int LONG = 3;
	
	public static final int NOLINENUMBER = -1;
	public static final int BADLINENUMBER = -2;
	public static final byte EOL = (byte)0xff;
	
	public static void main(String[] arg) {
		TIImageTool.localize();
		BasicCruncher bc = new BasicCruncher();
		String[] basicline = null;
		
		if (arg.length < 2) {
			System.err.println(TIImageTool.langstr("BasicCrunchUsage"));
			return;
		}
		try {
			int version = Integer.parseInt(arg[0]);
			BufferedReader br = new BufferedReader(new FileReader(arg[1]));
			ArrayList<String> lines = new ArrayList<String>();
			while (br.ready()) {
				String line = br.readLine();
				lines.add(line);
			}
			br.close();
			
			basicline = new String[lines.size()];
			lines.toArray(basicline);
			
			byte[] aby = bc.textToBasic(basicline, "UNNAMED", version, NORMAL, false);
			
			System.out.println(Utilities.hexdump(aby));
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
		catch (CrunchException cx) {
			System.err.println(String.format(TIImageTool.langstr("BasicCrunchError"), cx.getReason(), cx.line));
		}
	}
		
	public static boolean contentLooksLikeBasic(byte[] content, boolean verbose) {
		return textLooksLikeBasic(new String(content, Charset.forName("ISO-8859-1")), verbose);
	}
	
	public static boolean textLooksLikeBasic(String s, boolean verbose) {
		BasicCruncher bc = new BasicCruncher();
		String split = Utilities.getSeparator(s);
	
		String[] aline = s.split(split);
		// Take the first 10 lines to check
		try {
			for (int i=0; i < 10 && i < aline.length; i++) {
				BasicLine bl = bc.crunch(aline[i], BasicLine.EX_BASIC);
			}
		}
		catch (CrunchException cx) {
			if (verbose) System.err.println(String.format(TIImageTool.langstr("BasicCrunchTest"), cx.getReason(), cx.line, cx.pos)); 
			// Try again for TI BASIC
			try {
				for (int i=0; i < 10 && i < aline.length; i++) {
					BasicLine bl = bc.crunch(aline[i], BasicLine.TI_BASIC);
				}
			}
			catch (CrunchException c1x) {
				// System.err.println(c1x.getReason() + ", line " + c1x.line + ", column " + c1x.pos);
				return false;
			}
		}
		return true;
	}
	
	/*
		The first *entered* Basic line is located at the top of the memory.
		The order in memory does not depend on the line number.
		The order is implied by the line number table.
		New lines are always added at the bottom, and the line number table 
		is moved to the new end.
	*/
	public byte[] textToBasic(String[] aline, String filename, int version, int saveFormat, boolean prot) throws CrunchException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LinkedList<BasicLine> program = new LinkedList<BasicLine>();
		LinkedList<BasicLine> lnt = new LinkedList<BasicLine>();
				
		int memtop = (saveFormat==LONG)? 0xffe7 : 0x37d7;
		int address = memtop+2;
		int prglen = 0;
		
		byte[] abyTif = null;
		
		m_textLine = 0;
		
		for (int i=0; i < aline.length; i++) {
			m_textLine++;
			BasicLine cmdline = crunch(aline[i], version);
			if (cmdline.linenumber==0) continue; // filter out comment lines
			int len = cmdline.content.length;
			address = address - len - 2;
			cmdline.setAddress(address);
			program.add(cmdline);
			
			prglen += (len + 2);
			
			int index = -1;
			
			for (int j=0; j < lnt.size() && index < 0; j++) {
				BasicLine entry = lnt.get(j);
				if (entry.linenumber >= cmdline.linenumber) {
					index = j;
				}
			}
			if (index == -1) lnt.add(cmdline);
			else lnt.add(index, cmdline);
		}
		
		m_count = lnt.size();
		
		int endlnt = memtop - prglen;
		int startlnt = endlnt - 4 * lnt.size() + 1;
		int check = (startlnt ^ endlnt) & 0xffff;
		if (prot) check = (-check) & 0xffff;
	/*
PRG: 
0e2f 0a98 37d7
0e4a 0aaf 37d7
0e43 0aa4 37d7
0e3c 0a99 37d7

I/V254: 
d2a7 d63e ffe7
d29e d645 ffe7
	*/
	
		if (saveFormat == NORMAL) {
			// Header
			writeInt(baos, check);
			writeInt(baos, endlnt);
			writeInt(baos, startlnt);
			writeInt(baos, memtop);
			
			// LNT
			for (int i = lnt.size()-1; i >=0; i--) {
				BasicLine bl = lnt.get(i);
				writeInt(baos, bl.linenumber);
				writeInt(baos, bl.address);
			}
			
			for (int i=program.size()-1; i >=0; i--) {
				BasicLine bl = program.get(i);
				baos.write(bl.content.length +1);
				baos.write(bl.content);
				baos.write(0x00);			
			}
			
			byte[] prgbytes = baos.toByteArray();
			if (prgbytes.length > 11584) throw new CrunchException(TOOLONG, "", 0, 0, 0);
			
			abyTif = TIFiles.createTfi(prgbytes, filename, TFile.PROGRAM, 0, 0);
		}
		else {
			if (saveFormat == MERGE) {
				TIFiles mergefile = new TIFiles(filename, TFile.T_DISVAR, 163);
				for (int i=0; i < lnt.size(); i++) {
					BasicLine bl = lnt.get(i);
					byte[] record = new byte[bl.content.length + 3];
					record[0] = (byte)((bl.linenumber >> 8)&0xff);
					record[1] = (byte)(bl.linenumber & 0xff);
					System.arraycopy(bl.content, 0, record, 2, bl.content.length);
					record[bl.content.length + 2] = (byte)0x00;
					mergefile.writeRecord(record);
				}
				byte[] endofprg = { (byte)0xff, (byte)0xff };
				mergefile.writeRecord(endofprg);
				abyTif = mergefile.closeAndGetBytes(false, false);
			}
			else {
				if (saveFormat == LONG) {
					TIFiles longfile = new TIFiles(filename, TFile.T_INTVAR, 254);
					ByteArrayOutputStream baosRecord = new ByteArrayOutputStream();
					
					// Format: ABCD startLNT endLNT (start^end) memtop
					writeInt(baosRecord, 0xabcd);
					writeInt(baosRecord, startlnt);
					writeInt(baosRecord, endlnt);
					writeInt(baosRecord, check);
					writeInt(baosRecord, memtop);
					longfile.writeRecord(baosRecord.toByteArray());					
					
					// LNT
					baosRecord.reset();
					for (int i = lnt.size()-1; i >=0; i--) {
						BasicLine bl = lnt.get(i);
						writeInt(baosRecord, bl.linenumber);
						writeInt(baosRecord, bl.address);
					}
					
					// Now add the lines
					for (int i=program.size()-1; i >=0; i--) {
						BasicLine bl = program.get(i);
						baosRecord.write(bl.content.length +1);
						baosRecord.write(bl.content);
						baosRecord.write(0x00);			
					}
					
					byte[] prgbytes = baosRecord.toByteArray();
					byte[] rec = new byte[254];
					
					int pos = 0;
					int len = prgbytes.length;
					while (len > 254) {
						System.arraycopy(prgbytes, pos, rec, 0, 254);
						longfile.writeRecord(rec);
						len -= 254;
						pos += 254;
					}
					if (len > 0) {
						rec = new byte[len];
						System.arraycopy(prgbytes, pos, rec, 0, len);
						longfile.writeRecord(rec);
					}
					abyTif = longfile.closeAndGetBytes(false, false);
					// If the BASIC lines amount to less than 244 bytes, the
					// LONG header and the lines fit into one sector, which
					// is not allowed for the LONG format. Accordingly, we are
					// save with more than 256 bytes, which need two sectors,
					// plus another sector for the LONG header, plus 0x80 bytes
					// for the TIFILES header.
					if (abyTif.length < 0x380) throw new CrunchException(TOOSHORT, "", 0, 0, 0);
				}
			}
		}
		
		return abyTif;
	}
	
	private void writeInt(ByteArrayOutputStream baos, int value) throws IOException {
		baos.write((value >> 8)&0xff);
		baos.write(value &0xff);
	}
	
	public int getLineCount() {
		return m_count;
	}
	
/*
00 13 
37 b9 
37 aa 
37 d7 

37aa: 00 28 37 bb 
37ae: 00 1e 37 be
37b2: 00 14 37 c2 
37b6: 00 0a 37 cc

37ba: 02 8b 00 
37bd: 03 96 49 00 
37c1: 09 9c c7 05 48 41 4c 4c 4f 00 
37cb: 0c 8c 49 be c8 01 31 b1 c8 02 31 30 00
*/
	
	/** Creates a new BASIC line from a text. 
		@param line Source code
		@param version BASIC version: TIBASIC, EXBASIC, OTHERBASIC.
		@return New crunched BASIC line.
	*/		
	public BasicLine crunch(String line, int version) throws CrunchException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
		m_line = line;
		// Get the line number. There is not necessarily a whitespace behind.
		int pos = 0;
		m_linenumber = 0;
		int state = 0;
		int token = 0;
		int previous_token = 0;
		int startpos = 0;
		boolean ok = false;
		boolean alreadyNumVar = false;
		
		m_version = version;
		
		m_inloc = 0;
		skipSpace();
		if (currentChar()>='0' && currentChar()<='9') {
			m_linenumber = getLineNumber();
		}
		else {
			m_linenumber = 0;
			// throw new CrunchException(NOLINENO, "", 0, 0);
		}

		try {
			// TODO: Check for end of input
			while (!endOfLine()) {
				previous_token = token;
				skipSpace();
				String symbol = null;
				// System.out.println("Single char = " + Utilities.toHex((int)currentChar(),2));
				if (currentChar() == (char)EOL) break;
				if (currentChar() >= '0') {
					if (currentChar() <= '9') {
						symbol = getFloatNumber();
						if (alreadyNumVar) throw new CrunchException(MULTIPLE_VAR_NUM, symbol, m_linenumber, m_inloc, m_textLine);
						alreadyNumVar = true;
						baos.write(0xc8); // UNQUOTED
						baos.write((byte)symbol.length());
						baos.write(symbol.getBytes());	
					}
					else {
						// Higher than '9'
						if (isValidForSymbol(currentChar())) {
							symbol = getUnquotedString();
							// Try to identify token
							token = 0;
							for (int i=0; i < 256 && token == 0; i++) {
								// if (symbol.equalsIgnoreCase(BasicLine.basicToken[i]) && supported(i)) {
								if (matches(symbol, i)) {
									if ((BasicLine.tokenType[i] & BasicLine.DIRECT) != 0) {
										System.err.println(ILLEGAL_IN_PRG);
										throw new CrunchException(ILLEGAL_IN_PRG, symbol, m_linenumber, m_inloc, m_textLine);
									}
									token = i;
								}
							}
							
							if (token != 0) {
								if (token == 0x93) dataLine(token, baos);
								else {
									if (token == 0x9a) remLine(token, baos);
									else {
										if (token == 0x9d) callSubLine(token, baos); 
										else {
											if (token == 0xa1 && supported(token)) callSubLine(token, baos); 
											else {
												if ((BasicLine.tokenType[token] & BasicLine.HASLINENO)!=0) {
													try {
														lineNumberList(token, previous_token, baos);
													}
													catch (CrunchException cx) { }
												}
												else {
													baos.write(token);
												}
											}
										}
									}
								}
								alreadyNumVar = false;
							}
							else {
								// not reserved; is a variable
								if (alreadyNumVar) throw new CrunchException(MULTIPLE_VAR_NUM, symbol, m_linenumber, m_inloc, m_textLine);
								alreadyNumVar = true;
								// baos.write(0xc8); // UNQUOTED
								// baos.write((byte)symbol.length());
								baos.write(symbol.getBytes());	
							}
						}
						else {
							alreadyNumVar = false;
							// Test for single char token and '::'
							token = getSingleCharToken();
							if (token == 0) {
								throw new CrunchException(UNRECOGNIZED, symbol, m_linenumber, m_inloc, m_textLine);
							}
							else baos.write(token);
						}
					}
				}
				else {
					// below '0'
					if (currentChar() == '!' && supported(0x83)) {
						remLine(0x83, baos);
					}
					else {
						if (currentChar() == '.') {
							symbol = getFloatNumber();
							if (alreadyNumVar) throw new CrunchException(MULTIPLE_VAR_NUM, symbol, m_linenumber, m_inloc, m_textLine);
							alreadyNumVar = true;
							baos.write(0xc8); // UNQUOTED
							baos.write((byte)symbol.length());
							baos.write(symbol.getBytes());	
						}
						else {
							alreadyNumVar = false;
							if (currentChar() == '"') {
								// Quoted string
								symbol = getQuotedString();
								baos.write(0xc7); // QUOTED
								baos.write((byte)symbol.length());
								baos.write(symbol.getBytes());	
							}
							else {
								// below '0', but neither '.' nor '"'
								// Test for single char token 
								token = getSingleCharToken();
								if (token==0) {
									throw new CrunchException(UNRECOGNIZED, symbol, m_linenumber, m_inloc, m_textLine);
								}
								else baos.write(token);
							}
						}
					}
				}
			}
			baos.flush();
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
		BasicLine bl = new BasicLine(m_linenumber, baos.toByteArray());
		if (m_linenumber==0 && bl.content.length != 0 && bl.content[0]!=(byte)0x83 && bl.content[0]!=(byte)0x9a)
			throw new CrunchException(NOLINENO, "", 0, 0, m_textLine);
		return bl;
	}
	
	boolean matches(String s, int i) {
		if (BasicLine.basicToken[i] == null) return false;
		if (!supported(i)) return false;
		int pos = BasicLine.basicToken[i].indexOf('|');
		String test = null;
		String full = null;
		if (pos == -1) test = BasicLine.basicToken[i];
		else {
			test = BasicLine.basicToken[i].substring(0, pos-1);
			full = test + BasicLine.basicToken[i].substring(pos+1);
		}
		
		if (s.equalsIgnoreCase(test)) return true;
		if (full != null && s.equalsIgnoreCase(full)) return true;
		
		return false;
	}
	
	private int getSingleCharToken() {
		String symbol = String.valueOf(currentChar());
		int token = 0;
		for (int i=0; i < 256 && token == 0; i++) {
			if (BasicLine.basicToken[i] != null && supported(i) && BasicLine.basicToken[i].length()==1) {
				if (BasicLine.basicToken[i].equals(symbol)) {
					token = i;
				}
			}
		}
		// Check for double ::
		if (m_version != BasicLine.TI_BASIC) {
			if (token == 0xb5 && nextChar() == ':') {
				token = 0x82;
				advance(1);
			}
		}
		if (token != 0) advance(1);
		return token;
	}
	
	private void dataLine(int token, ByteArrayOutputStream baos) throws CrunchException, IOException {
		baos.write(token);
		String symbol;
		while (!endOfLine()) {
			skipSpace();
			String datael = null;
			if (currentChar()==',') {
				baos.write(0xb3);
				advance(1);
			}
			else {
				if (currentChar()=='"') {
					symbol = getQuotedString();
					baos.write(0xc7); // QUOTED
				}
				else {
					symbol = getUnquotedDataString();
					baos.write(0xc8); // UNQUOTED
				}
				baos.write((byte)symbol.length());
				baos.write(symbol.getBytes());	
			}
		}
	}
	
	private void remLine(int token, ByteArrayOutputStream baos) throws IOException {
		baos.write(token);
		if (token != 0x9a) advance(1);
		while (!endOfLine()) {
			baos.write(currentChar());
			advance(1);
		}
	}
	
	private void callSubLine(int token, ByteArrayOutputStream baos) throws CrunchException, IOException {
		baos.write(token);
		skipSpace();
		if (endOfLine()) throw new CrunchException(NOSUB, "", token, m_inloc, m_textLine);
		String symbol = getUnquotedString();
		skipSpace();
		baos.write(0xc8); // UNQUOTED
		baos.write((byte)symbol.length());
		baos.write(symbol.getBytes());
	}
	
	private void lineNumberList(int token, int previous_token, ByteArrayOutputStream baos) throws CrunchException, IOException {
		int loc = m_inloc;
		baos.write(token);
		if (token == 0xb1 && previous_token != 0x85) return;
		advance(1);

		while (!endOfLine()) {
			skipSpace();
			int number = getLineNumber();
			if (number == 0) {
				m_inloc = loc;
				throw new CrunchException(NOLINENO, "", token, m_inloc, m_textLine);
			}
			loc = m_inloc;
			baos.write(0xc9);  // LINENO
			baos.write((number >> 8)&0xff);
			baos.write(number&0xff);
			if (currentChar()==',') {
				baos.write(0xb3);
				advance(1);
			}
		}
	}
	
	private boolean supported(int token) {
		return ((BasicLine.tokenType[token] & m_version)==m_version);
	}
	
	private void skipSpace() {
		while (!endOfLine() && m_line.charAt(m_inloc)==32) 
			advance(1); 
	}

	private boolean endOfLine() {
		return m_inloc >= m_line.length();
	}

	private char currentChar() {
		if (endOfLine()) return (char)EOL;
		return m_line.charAt(m_inloc);
	}
	
	private char nextChar() {
		if (m_inloc >= m_line.length()-1) return (char)EOL;
		return m_line.charAt(m_inloc+1);
	}
	
	private int getLineNumber() throws CrunchException {
		int number = 0;
		boolean done = false;
		int digits = 0;
		char ch = 0;
		while (!done) {
			ch = currentChar();
			if (ch >= '0' && ch <= '9' && digits < 6) { 
				number = number * 10 + (ch-48);
				digits++;
				done = advance(1);
			}
			else done = true;
		}
		if (number > 32767) throw new CrunchException(BADLINENO, String.valueOf(number), number, m_inloc, m_textLine);
		return number;
	}
	
	private boolean advance(int n) {
		m_inloc += n;
		return m_inloc >= m_line.length();
	}
	
	private void getInteger(StringBuilder sb, boolean withSign) {
		boolean done = false;
		if (withSign) {
			if (currentChar()=='+' || currentChar()=='-') {
				sb.append(currentChar());
				done = advance(1);
			}
		}
		while (!done) {
			if (currentChar()>='0' && currentChar() <='9') {
				sb.append(currentChar());
				done = advance(1);
			}
			else done = true;
		}
	}
	
	/** Return a string that represents a number. Return null if there is no number. */
	private String getFloatNumber() {
		StringBuilder sb = new StringBuilder();

		getInteger(sb, true);
		if (!endOfLine()) {
			// Fraction part
			if (currentChar()=='.') {
				sb.append('.');
				advance(1);
				if (!endOfLine()) getInteger(sb, false);
			}
			// Exponent part
			if (currentChar()=='E') {
				sb.append('E');
				advance(1);
				if (!endOfLine()) getInteger(sb, true);
			}
		}
		return sb.toString();
	}
	
	private String getQuotedString() throws CrunchException {
		boolean done = advance(1);
		StringBuilder sb = new StringBuilder();
		boolean secondQuote = false;
		
		while (!done) {
			if (currentChar()=='"') {
				done = advance(1);
				if (!done && currentChar()=='"') {
					sb.append('"');
					done = advance(1);
				}
				else {
					done = true;
					secondQuote = true;
				}
			}
			else {
				if (!done) sb.append(currentChar());
				done = advance(1);
			}
		}
				
		if (!secondQuote) throw new CrunchException(UNMATCHED_QUOTES, sb.toString(), m_linenumber, m_inloc, m_textLine);
		return sb.toString();
	}
	
	/** Unquoted strings outside of DATA lines are uppercase */
	private String getUnquotedString() {
		StringBuilder sb = new StringBuilder();
		boolean done = false;
		while (!done) {
			if (isValidForSymbol(currentChar())) {
				char ch = currentChar();
				if (ch >= 97 && ch <=122) ch -= 32;
				sb.append(ch);
				done = advance(1);
			}
			else done = true;
		}
		if (currentChar()=='$') {
			sb.append('$');
			advance(1);
		}
		return sb.toString();
	}
	
	private String getUnquotedDataString() {
		StringBuilder sb = new StringBuilder();
		boolean done = false;
		while (!done) {
			if (currentChar()!=',') {
				sb.append(currentChar());
				done = advance(1);
			}
			else done = true;
		}
		return sb.toString();
	}	
	
	private boolean isValidForSymbol(char ch) {
		if (ch >= '0' && ch <= '9') return true;
		if (ch >= 'A' && ch <= 'Z') return true;
		if (ch >= 'a' && ch <= 'z') return true;
		return ("@[\\]_".indexOf(ch)!=-1);
	}
}

