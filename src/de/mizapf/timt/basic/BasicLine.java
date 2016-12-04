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
package de.mizapf.timt.basic;

import de.mizapf.timt.util.Utilities;
import java.io.ByteArrayOutputStream;
import de.mizapf.timt.files.FormatException;

public class BasicLine {
	int linenumber;
	byte[] content;
	int address;
	
	static String[] basicToken;
	static int[] tokenType;

	final static String QUOTED = "§QUOTED";
	final static String UNQUOTED = "§UNQUOTED";
	final static String LINENO = "§LINENO";

	private final static int STNORMAL = 0;
	private final static int STQUOTED = 1;
	private final static int STUNQUOTED = 2;
	private final static int STQUOTED_CONT = 3;
	private final static int STUNQUOTED_CONT = 4;

	private final static int STLINENUMBER = 5;
	private final static int STLINENUMBER_CONT = 6;

	public static final int TI_BASIC = 0x01;
	public static final int EX_BASIC = 0x02;	
	public static final int ALL_BASIC = TI_BASIC | EX_BASIC;
	public static final int HASLINENO = 0x04;
	public static final int DIRECT = 0x08;

	public static final int JUNCTOR = 0x10;
	public static final int FUNCTION = 0x20;
	public static final int PAREN = 0x40;	
	public static final int NOSPACE = 0x80;	
	
	private static final int NAME = 0x10;

	static {
		basicToken = new String[256];
		tokenType = new int[256];

		for (int i=10; i < 32; i++) {
			basicToken[i] = null;
			tokenType[i] = 0;
		}
		
		for (int i=32; i < 128; i++) basicToken[i] = String.valueOf((char)i);
		
		basicToken[0x00] = "NEW";				tokenType[0x00] = ALL_BASIC | DIRECT;
		basicToken[0x01] = "CON|TINUE";			tokenType[0x01] = ALL_BASIC | DIRECT;
		basicToken[0x02] = "LIST";				tokenType[0x02] = ALL_BASIC | DIRECT;
		basicToken[0x03] = "BYE";				tokenType[0x03] = ALL_BASIC | DIRECT;
		basicToken[0x04] = "NUM|BER";			tokenType[0x04] = ALL_BASIC | DIRECT;
		basicToken[0x05] = "OLD";				tokenType[0x05] = ALL_BASIC | DIRECT;
		basicToken[0x06] = "RES|EQUENCE";		tokenType[0x06] = ALL_BASIC | DIRECT;
		basicToken[0x07] = "SAVE";				tokenType[0x07] = ALL_BASIC | DIRECT;
		basicToken[0x08] = "MERGE";				tokenType[0x08] = EX_BASIC | DIRECT;
		basicToken[0x09] = "EDIT";				tokenType[0x00] = TI_BASIC | DIRECT;

		basicToken[0x80] = null;				tokenType[0x80] = 0;
		basicToken[0x81] = "ELSE";				tokenType[0x81] = ALL_BASIC | HASLINENO;
		basicToken[0x82] = "::";				tokenType[0x82] = EX_BASIC;
		basicToken[0x83] = "!";					tokenType[0x83] = EX_BASIC | NOSPACE;
		basicToken[0x84] = "IF";				tokenType[0x84] = ALL_BASIC;
		basicToken[0x85] = "GO";				tokenType[0x85] = ALL_BASIC;
		basicToken[0x86] = "GOTO";				tokenType[0x86] = ALL_BASIC | HASLINENO;
		basicToken[0x87] = "GOSUB";				tokenType[0x87] = ALL_BASIC | HASLINENO;
		basicToken[0x88] = "RETURN";			tokenType[0x88] = ALL_BASIC;
		basicToken[0x89] = "DEF";				tokenType[0x89] = ALL_BASIC;
		basicToken[0x8a] = "DIM";				tokenType[0x8a] = ALL_BASIC;
		basicToken[0x8b] = "END";				tokenType[0x8b] = ALL_BASIC;
		basicToken[0x8c] = "FOR";				tokenType[0x8c] = ALL_BASIC;
		basicToken[0x8d] = "LET";				tokenType[0x8d] = ALL_BASIC;
		basicToken[0x8e] = "BREAK";				tokenType[0x8e] = ALL_BASIC | HASLINENO;
		basicToken[0x8f] = "UNBREAK";			tokenType[0x8f] = ALL_BASIC | HASLINENO;
		basicToken[0x90] = "TRACE";				tokenType[0x90] = ALL_BASIC;
		basicToken[0x91] = "UNTRACE";			tokenType[0x91] = ALL_BASIC;
		basicToken[0x92] = "INPUT";				tokenType[0x92] = ALL_BASIC;
		basicToken[0x93] = "DATA";				tokenType[0x93] = ALL_BASIC;
		basicToken[0x94] = "RESTORE";			tokenType[0x94] = ALL_BASIC | HASLINENO;
		basicToken[0x95] = "RANDOMIZE";			tokenType[0x95] = ALL_BASIC;
		basicToken[0x96] = "NEXT";				tokenType[0x96] = ALL_BASIC;
		basicToken[0x97] = "READ";				tokenType[0x97] = ALL_BASIC;
		basicToken[0x98] = "STOP";				tokenType[0x98] = ALL_BASIC;
		basicToken[0x99] = "DELETE";			tokenType[0x99] = ALL_BASIC;
		basicToken[0x9a] = "REM";				tokenType[0x9a] = ALL_BASIC | NOSPACE;
		basicToken[0x9b] = "ON";				tokenType[0x9b] = ALL_BASIC;
		basicToken[0x9c] = "PRINT";				tokenType[0x9c] = ALL_BASIC;
		basicToken[0x9d] = "CALL";				tokenType[0x9d] = ALL_BASIC;
		basicToken[0x9e] = "OPTION";			tokenType[0x9e] = ALL_BASIC;
		basicToken[0x9f] = "OPEN";				tokenType[0x9f] = ALL_BASIC;
		basicToken[0xa0] = "CLOSE";				tokenType[0xa0] = ALL_BASIC;
		basicToken[0xa1] = "SUB";				tokenType[0xa1] = ALL_BASIC | HASLINENO;
		basicToken[0xa2] = "DISPLAY";			tokenType[0xa2] = ALL_BASIC;
		basicToken[0xa3] = "IMAGE";				tokenType[0xa3] = EX_BASIC;
		basicToken[0xa4] = "ACCEPT";			tokenType[0xa4] = EX_BASIC;
		basicToken[0xa5] = "ERROR";				tokenType[0xa5] = EX_BASIC;
		basicToken[0xa6] = "WARNING";			tokenType[0xa6] = EX_BASIC;
		basicToken[0xa7] = "SUBEXIT";			tokenType[0xa7] = EX_BASIC;
		basicToken[0xa8] = "SUBEND";			tokenType[0xa8] = EX_BASIC;
		basicToken[0xa9] = "RUN";				tokenType[0xa9] = EX_BASIC;
		basicToken[0xaa] = "LINPUT";			tokenType[0xaa] = EX_BASIC;
		basicToken[0xab] = null;				tokenType[0xab] = 0;
		basicToken[0xac] = null;				tokenType[0xac] = 0;
		basicToken[0xad] = null;				tokenType[0xad] = 0;
		basicToken[0xae] = null;				tokenType[0xae] = 0;
		basicToken[0xaf] = null;				tokenType[0xaf] = 0;
		basicToken[0xb0] = "THEN";				tokenType[0xb0] = ALL_BASIC | HASLINENO;
		basicToken[0xb1] = "TO";				tokenType[0xb1] = ALL_BASIC | HASLINENO;
		basicToken[0xb2] = "STEP";				tokenType[0xb2] = ALL_BASIC;
		basicToken[0xb3] = ",";					tokenType[0xb3] = ALL_BASIC | JUNCTOR;
		basicToken[0xb4] = ";";					tokenType[0xb4] = ALL_BASIC | JUNCTOR;
		basicToken[0xb5] = ":";					tokenType[0xb5] = ALL_BASIC | JUNCTOR;
		basicToken[0xb6] = ")";					tokenType[0xb6] = ALL_BASIC | PAREN;
		basicToken[0xb7] = "(";					tokenType[0xb7] = ALL_BASIC | PAREN;
		basicToken[0xb8] = "&";					tokenType[0xb8] = ALL_BASIC | JUNCTOR;
		basicToken[0xb9] = null;				tokenType[0xb9] = 0;
		basicToken[0xba] = "OR";				tokenType[0xba] = EX_BASIC;
		basicToken[0xbb] = "AND";				tokenType[0xbb] = EX_BASIC;
		basicToken[0xbc] = "XOR";				tokenType[0xbc] = EX_BASIC;
		basicToken[0xbd] = "NOT";				tokenType[0xbd] = EX_BASIC;
		basicToken[0xbe] = "=";					tokenType[0xbe] = ALL_BASIC | JUNCTOR;
		basicToken[0xbf] = "<";					tokenType[0xbf] = ALL_BASIC | JUNCTOR;
		basicToken[0xc0] = ">";					tokenType[0xc0] = ALL_BASIC | JUNCTOR;
		basicToken[0xc1] = "+";					tokenType[0xc1] = ALL_BASIC | JUNCTOR;
		basicToken[0xc2] = "-";					tokenType[0xc2] = ALL_BASIC | JUNCTOR;
		basicToken[0xc3] = "*";					tokenType[0xc3] = ALL_BASIC | JUNCTOR;
		basicToken[0xc4] = "/";					tokenType[0xc4] = ALL_BASIC | JUNCTOR;
		basicToken[0xc5] = "^";					tokenType[0xc5] = ALL_BASIC | JUNCTOR;
		basicToken[0xc6] = null;				tokenType[0xc6] = 0;
		basicToken[0xc7] = QUOTED;				tokenType[0xc7] = ALL_BASIC;
		basicToken[0xc8] = UNQUOTED;			tokenType[0xc8] = ALL_BASIC;
		basicToken[0xc9] = LINENO;				tokenType[0xc9] = ALL_BASIC;
		basicToken[0xca] = "EOF";				tokenType[0xca] = ALL_BASIC | FUNCTION;
		basicToken[0xcb] = "ABS";				tokenType[0xcb] = ALL_BASIC | FUNCTION;
		basicToken[0xcc] = "ATN";				tokenType[0xcc] = ALL_BASIC | FUNCTION;
		basicToken[0xcd] = "COS";				tokenType[0xcd] = ALL_BASIC | FUNCTION;
		basicToken[0xce] = "EXP";				tokenType[0xce] = ALL_BASIC | FUNCTION;
		basicToken[0xcf] = "INT";				tokenType[0xcf] = ALL_BASIC | FUNCTION;
		basicToken[0xd0] = "LOG";				tokenType[0xd0] = ALL_BASIC | FUNCTION;
		basicToken[0xd1] = "SGN";				tokenType[0xd1] = ALL_BASIC | FUNCTION;
		basicToken[0xd2] = "SIN";				tokenType[0xd2] = ALL_BASIC | FUNCTION;
		basicToken[0xd3] = "SQR";				tokenType[0xd3] = ALL_BASIC | FUNCTION;
		basicToken[0xd4] = "TAN";				tokenType[0xd4] = ALL_BASIC | FUNCTION;
		basicToken[0xd5] = "LEN";				tokenType[0xd5] = ALL_BASIC | FUNCTION;
		basicToken[0xd6] = "CHR$";				tokenType[0xd6] = ALL_BASIC | FUNCTION;
		basicToken[0xd7] = "RND";				tokenType[0xd7] = ALL_BASIC | FUNCTION;
		basicToken[0xd8] = "SEG$";				tokenType[0xd8] = ALL_BASIC | FUNCTION;
		basicToken[0xd9] = "POS";				tokenType[0xd9] = ALL_BASIC | FUNCTION;
		basicToken[0xda] = "VAL";				tokenType[0xda] = ALL_BASIC | FUNCTION;
		basicToken[0xdb] = "STR$";				tokenType[0xdb] = ALL_BASIC | FUNCTION;
		basicToken[0xdc] = "ASC";				tokenType[0xdc] = ALL_BASIC | FUNCTION;
		basicToken[0xdd] = "PI";				tokenType[0xdd] = EX_BASIC | FUNCTION;
		basicToken[0xde] = "REC";				tokenType[0xde] = ALL_BASIC | FUNCTION;
		basicToken[0xdf] = "MAX";				tokenType[0xdf] = EX_BASIC | FUNCTION;
		basicToken[0xe0] = "MIN";				tokenType[0xe0] = EX_BASIC | FUNCTION;
		basicToken[0xe1] = "RPT$";				tokenType[0xe1] = EX_BASIC | FUNCTION;
		basicToken[0xe2] = null;				tokenType[0xe2] = 0;
		basicToken[0xe3] = null;				tokenType[0xe3] = 0;
		basicToken[0xe4] = null;				tokenType[0xe4] = 0;
		basicToken[0xe5] = null;				tokenType[0xe5] = 0;
		basicToken[0xe6] = null;				tokenType[0xe6] = 0;
		basicToken[0xe7] = null;				tokenType[0xe7] = 0;
		basicToken[0xe8] = "NUMERIC";			tokenType[0xe8] = EX_BASIC;
		basicToken[0xe9] = "DIGIT";				tokenType[0xe9] = EX_BASIC;
		basicToken[0xea] = "UALPHA";			tokenType[0xea] = EX_BASIC;
		basicToken[0xeb] = "SIZE";				tokenType[0xeb] = EX_BASIC | FUNCTION;
		basicToken[0xec] = "ALL";				tokenType[0xec] = EX_BASIC;
		basicToken[0xed] = "USING";				tokenType[0xed] = EX_BASIC;
		basicToken[0xee] = "BEEP";				tokenType[0xee] = EX_BASIC;
		basicToken[0xef] = "ERASE";				tokenType[0xef] = EX_BASIC;
		basicToken[0xf0] = "AT";				tokenType[0xf0] = EX_BASIC | FUNCTION;
		basicToken[0xf1] = "BASE";				tokenType[0xf1] = ALL_BASIC;
		basicToken[0xf2] = null;				tokenType[0xf2] = 0;
		basicToken[0xf3] = "VARIABLE";			tokenType[0xf3] = ALL_BASIC;
		basicToken[0xf4] = "RELATIVE";			tokenType[0xf4] = ALL_BASIC;
		basicToken[0xf5] = "INTERNAL";			tokenType[0xf5] = ALL_BASIC;
		basicToken[0xf6] = "SEQUENTIAL";		tokenType[0xf6] = ALL_BASIC;
		basicToken[0xf7] = "OUTPUT";			tokenType[0xf7] = ALL_BASIC;
		basicToken[0xf8] = "UPDATE";			tokenType[0xf8] = ALL_BASIC;
		basicToken[0xf9] = "APPEND";			tokenType[0xf9] = ALL_BASIC;
		basicToken[0xfa] = "FIXED";				tokenType[0xfa] = ALL_BASIC;
		basicToken[0xfb] = "PERMANENT";			tokenType[0xfb] = ALL_BASIC;
		basicToken[0xfc] = "TAB";				tokenType[0xfc] = ALL_BASIC;
		basicToken[0xfd] = "#";					tokenType[0xfd] = ALL_BASIC | NOSPACE;
		basicToken[0xfe] = "VALIDATE";			tokenType[0xfe] = ALL_BASIC;
		basicToken[0xff] = null;				tokenType[0xff] = 0;
	}
		
	public BasicLine(int nLineNo, byte[] abyParsed) {
		linenumber = nLineNo;
		content = abyParsed;
	}
	
	public void setAddress(int addr) {
		address = addr;
	}
		
	public String list(int version, String escape) {
		StringBuilder sb = new StringBuilder();
		int state = STNORMAL;
		boolean bCommand = false;
		String sLastToken = "  ";
		int nThisToken = 0;
		int nLastToken = 0;
		sb.append(linenumber);
		
		int nCurrentByte = 0;
		int nEndString = 0;
		int nLineNo = 0;
		
		char c = 0;
		for (int i=0; i < content.length; i++) {
			nCurrentByte = content[i] & 0xff;
			switch (state) {

			case STQUOTED:
				nEndString = i + nCurrentByte;  // position of the closing quote
				if (needsSpace(nThisToken, nLastToken, version)) sb.append(" ");
				sb.append("\"");

				// Null string?
				if (nCurrentByte == 0) { 
					sb.append("\"");
					state = STNORMAL;
				}
				else state = STQUOTED_CONT;
				break;

			case STUNQUOTED:
				nEndString = i + nCurrentByte; 
				
				if (needsSpace(nThisToken, nLastToken, version)) sb.append(" ");
				// Null string?
				if (nCurrentByte == 0) 	state = STNORMAL;
				else state = STUNQUOTED_CONT;
				break;

			case STQUOTED_CONT:
				c = (char)content[i];
				if (c=='"') sb.append("\""); // double the quote
				try {
					sb.append(Utilities.sanitizeChar(content[i], escape));  
				}
				catch (FormatException fx) {
					if (content[i] < 32 || content[i] > 126) sb.append("<").append(Utilities.toHex(content[i],2)).append(">");
					else sb.append(c);
				}

				if (i == nEndString) {
					sb.append("\"");
					state = STNORMAL;
				}
				break;

			case STUNQUOTED_CONT:
				c = (char)content[i];
				try {
					sb.append(Utilities.sanitizeChar(content[i], escape));  
				}
				catch (FormatException fx) {
					if (content[i] < 32 || content[i] > 126) sb.append("<").append(Utilities.toHex(content[i],2)).append(">");
					else sb.append(c);
				}
				if (i == nEndString) {
					state = STNORMAL;
				}
				break;

			case STLINENUMBER:
				nLineNo = nCurrentByte << 8;
				state = STLINENUMBER_CONT;
				break;

			case STLINENUMBER_CONT:
				nLineNo = nLineNo + nCurrentByte;
				if (needsSpace(nThisToken, nLastToken, version)) sb.append(" ");
				sb.append(nLineNo);
				nLastToken = nThisToken;
				state = STNORMAL;
				break;
				
			case STNORMAL:
				if (nCurrentByte >= 0x20 && nCurrentByte <= 0x7f) {
					// Plain character
					nLastToken = nThisToken;
					nThisToken = NAME;
				}
				else {
					nLastToken = nThisToken;
					nThisToken = nCurrentByte;
				}

				String sToken = basicToken[nCurrentByte];			
				if (sToken == null) sb.append("<").append(Utilities.toHex(content[i],2)).append(">");
				else {
					if (sToken == UNQUOTED) {
						state = STUNQUOTED;
					}
					else {
						if (sToken == QUOTED) {
							state = STQUOTED;
						}
						else {
							if (sToken == LINENO) {
								state = STLINENUMBER;
							}
							else {								 
								if (needsSpace(nThisToken, nLastToken, version)) sb.append(" ");
								sb.append(sToken);
							}
						}
					}
				}
			}
		}
		return sb.toString();
	}
	
	private boolean needsSpace(int currentToken, int previousToken, int version) {
//		System.out.println("prev=" + Utilities.toHex(previousToken,2) + ", curr=" + Utilities.toHex(currentToken,2));
		if (previousToken==0) return true;

		if ((tokenType[previousToken] & NOSPACE)!=0) return false; 
		
		// Double colon
		if (version != TI_BASIC && previousToken == 0xb5 && currentToken == 0xb5) return true;

		// Inside a name
		if (currentToken == NAME) {
			if (previousToken == NAME) return false;
			if ((tokenType[previousToken] & JUNCTOR)!=0) return false;
		}
		
		// No blank after ) when there is a junctor after
		if (previousToken == 0xb6 && (tokenType[currentToken] & JUNCTOR)!=0) return false;

		// No blank after (
		if (previousToken == 0xb7) return false;
		
		// Blank before (
		if (currentToken == 0xb7 && (previousToken == 0x10 || previousToken == 0xc8 || (tokenType[previousToken] & FUNCTION)!=0 || (tokenType[previousToken] & JUNCTOR)!=0)) return false;
		if (currentToken == 0xb6) return false;

		// Functions
		if ((tokenType[currentToken] & FUNCTION)!=0) {
			if ((tokenType[previousToken] & JUNCTOR)!=0) return false;
		}

		// Comma never has a space before or after
		if (currentToken == 0xb3 || previousToken == 0xb3) return false; 
		
		// Other junctors
		if ((tokenType[currentToken] & JUNCTOR)!=0) {
			if (previousToken == NAME) return false;
			if (previousToken == 0xc7 || previousToken == 0xc8) return false;
			if ((tokenType[previousToken] & JUNCTOR)!=0) return false;
			if ((tokenType[previousToken] & FUNCTION)==0) {
				return true;
			}
			return false;
		}
		
		// Name
		if (currentToken == 0xc7 || currentToken == 0xc8) {
			if ((tokenType[previousToken] & JUNCTOR)!=0) return false;
		}
			
		return true;
	}
}

