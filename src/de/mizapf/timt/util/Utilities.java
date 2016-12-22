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

package de.mizapf.timt.util;
import java.nio.charset.*;
import de.mizapf.timt.files.Time;
import de.mizapf.timt.files.FormatException;
import java.io.*;
import de.mizapf.timt.TIImageTool;

public class Utilities {
	
	public static void main(String[] arg) {
		TIImageTool.localize();
		if (arg.length < 2) {
			System.err.println(TIImageTool.langstr("UtilUsage"));
			return;
		}
		if (arg[0].equalsIgnoreCase("crc16")) {
			try {
				DataInputStream dis = new DataInputStream(new FileInputStream(arg[1]));
				byte[] content = new byte[dis.available()];
				dis.readFully(content);
				System.out.println(arg[1] + ": crc16=" + toHex(crc16_get(content, 0, content.length),4));
			}
			catch (IOException iox) {
				iox.printStackTrace();
			}		
		}
		else {
			System.err.println(TIImageTool.langstr("UtilUnknownComm"));
		}		
	}
	
	public static int getInt16(byte[] aby, int nOffset) {
		return (((aby[nOffset]<<8)&0xff00)+((aby[nOffset+1])&0x00ff)); 
	}
	
	public static int getInt16rev(byte[] aby, int nOffset) {
		return (((aby[nOffset+1]<<8)&0xff00)+((aby[nOffset])&0x00ff)); 
	}

	public static void setInt16(byte[] aby, int nOffset, int nValue) {
		aby[nOffset] = (byte)((nValue>>8)&0x00ff);
		aby[nOffset+1] = (byte)(nValue & 0x00ff);
	}
	
	public static String getString10(byte[] aby, int nOffset) {
		String sName = null;
		byte[] abyCopy = new byte[10];
		for (int i=0; i < 10; i++) {
			if (isPrintable((char)aby[i+nOffset])) abyCopy[i] = aby[i+nOffset];
			else {
				// System.err.println("Unprintable character " + toHex(aby[i+nOffset],2) + " replaced by '.'");
				abyCopy[i] = '.';
			}
		}
		
		try {
			sName = new String(abyCopy, 0, 10, Charset.forName("ISO-8859-1"));
		}
		catch (UnsupportedCharsetException ux) {
			ux.printStackTrace();
			sName = new String(abyCopy, 0, 10);
		}
		catch (StringIndexOutOfBoundsException six) {
			six.printStackTrace();
			sName = "*unknown**";
		} 
		return sName.trim();
	}
	
	public static void setString(byte[] aby, int nOffset, String s, int nLength) {
		byte[] abyString = s.getBytes();
		int j = 0;
		for (int i=nOffset; i < nOffset + nLength; i++) {
			if (j < s.length()) aby[i] = abyString[j++];
			else aby[i] = (byte)0x20;
		}
	}
	
	public static void setTime(byte[] aby, int nOffset, Time time) {
		System.arraycopy(time.getBytes(), 0, aby, nOffset, 4);
	}
	
	public static String toHex(int value, int length) {
		return toHex(value, length, false);
	}
	
	public static String toHex(int value, int length, boolean bUppercase) {
		String HEX = "0123456789abcdef0123456789ABCDEF";
		char[] out = new char[length];
		int offset = bUppercase? 16 : 0; 
		
		for (int i=0; i < length; i++) {
			out[length-i-1] = HEX.charAt(offset + (value & 0x0f));
			value >>= 4;			
		}
		return new String(out);
	}
	
	/** If more than 20% of the string are characters outside plain ASCII,
	    this is probably no printable text. */
	public static boolean checkForText(String s) {
		int unprintable = 0;
		for (int i=0; i < s.length(); i++) {
			if (s.charAt(i) < 32 || s.charAt(i) > 127) unprintable++; 
		}
		return (unprintable * 5 < s.length());
	}
	
	public static boolean checkForText(byte[] aby) {
		return checkForText(aby, aby.length);
	}
	
	public static boolean checkForText(byte[] aby, int len) {
		int unprintable = 0;
		int ch = 0;
		for (int i=0; i < len; i++) {
			ch = aby[i] & 0xff;
			if (ch != 0x0a && ch != 0x0d && (ch < 32 || ch > 127)) unprintable++; 
		}
		return (unprintable * 5 < len);
	}
	
	public static String getSeparator(String s) {
		int pos = -1;
		int countcr = 0;
		int countlf = 0;
		
		while (pos < s.length()) {
			pos = s.indexOf("\n", pos+1);
			if (pos == -1) break;
			if (pos >= 0) countlf++;
			if (pos > 0 && s.charAt(pos-1)=='\r') countcr++;
		}
		// If more than 80% are CRLF, return CRLF, else LF
		if (countcr * 10 > countlf * 8) return "\r\n";
		return "\n";
	}
	
	public static String sanitizeText(byte[] strbyte, String sEscape, boolean log) throws FormatException {
		if (sEscape.length() < 1 || sEscape.length() > 2) {
			throw new FormatException("", TIImageTool.langstr("UtilInvalidEsc"));
		}

		char chEscape = sEscape.charAt(0);
		boolean bEscape = (sEscape.length() == 2);
		boolean bUnprintable = false;
		
		if (bEscape && sEscape.charAt(1) != '%') {
			throw new FormatException("", TIImageTool.langstr("UtilInvalidEsc"));
		}

		StringBuilder sb = new StringBuilder();
		for (int i=0; i < strbyte.length; i++) {
			char c = (char)strbyte[i];
			if (c == chEscape) sb.append(c).append(c);
			else {
				if (isPrintable(c)) sb.append(c);
				else {
					if (log) System.out.println(String.format(TIImageTool.langstr("UtilUnprint"), i, Utilities.toHex(strbyte[i],2)));
					bUnprintable = true;
					if (bEscape) sb.append(chEscape).append(Utilities.toHex(strbyte[i], 2));
					else sb.append(chEscape);
				}
			}				
		}
		return sb.toString();
	}
		
	public static String sanitizeChar(byte strbyte, String sEscape) throws FormatException {
		if (sEscape.length() < 1 || sEscape.length() > 2) {
			throw new FormatException("", TIImageTool.langstr("UtilInvalidEsc"));
		}

		char chEscape = sEscape.charAt(0);
		boolean bEscape = (sEscape.length() == 2);
		boolean bUnprintable = false;
		
		if (bEscape && sEscape.charAt(1) != '%') {
			throw new FormatException("", TIImageTool.langstr("UtilInvalidEsc"));
		}

		StringBuilder sb = new StringBuilder();
		char c = (char)strbyte;
		if (c == chEscape) sb.append(c).append(c);
		else {
			if (isPrintable(c)) sb.append(c);
			else {
				if (bEscape) sb.append(chEscape).append(Utilities.toHex(strbyte, 2));
				else sb.append(chEscape);
			}
		}				
		return sb.toString();
	}
	
	public static boolean isPrintable(char c) {
		if (c>=32 && c <=126) return true;
		if (c == 13 || c == 10 || c == 9) return true;
		return false;
	}
	
	public static String hexdump(int nStart, int nOffset, byte[] content, int nLength, boolean bOnlyValues, int nBias) {
		StringBuilder sb = new StringBuilder();
		int nPos = nOffset;
		while (nPos < nLength+nOffset) {
			int nByteLength = sb.length();
			if (!bOnlyValues) sb.append(toHex(nPos-nOffset+nStart,6)).append(": ");
			for (int i=0; i < 16 && i < content.length-nPos; i++) {
				sb.append(toHex(content[nPos+i],2)).append(" ");
			}
			int nUsed = sb.length()-nByteLength;
			if (!bOnlyValues) {
				for (int i=0; i < 60-nUsed; i++) {
					sb.append(" ");
				}
				for (int i=0; i < 16 && i < content.length-nPos; i++) {
					char ch = (char)((content[nPos+i] - (byte)(nBias&0xff))&0xff);
					if (ch>=32 && ch<=126)
						sb.append(ch);
					else
						sb.append(".");
				}
			}
			nPos += 16;
			sb.append("\n");
			
		}
		return sb.toString();
	}

	public static String hexdump(int nStart, int nOffset, byte[] content, int nLength, boolean bOnlyValues) {
		return hexdump(nStart, nOffset, content, nLength, bOnlyValues, 0);
	}
	
	public static String hexdump(byte[] content) {
		return hexdump(0, 0, content, content.length, false, 0);
	}
	
	public static String byteToHex(byte[] content, int nLength) {
		StringBuilder sb = new StringBuilder();
		int nPos = 0;
		while (nPos < nLength) {
			int nByteLength = sb.length();
			sb.append(toHex(content[nPos],2)).append(" ");
			nPos++;
		}
		return sb.toString();		
	}
	
	public static int crc16_get(byte aby[], int nOffset, int nLength) {
		return crc16_get(aby, nOffset, nLength, 0xffff);
	}

	public static int crc16_get(byte aby[], int nOffset, int nLength, int init) {
		// Big-endian, x^16+x^12+x^5+1 = (1) 0001 0000 0010 0001 = 0x1021
		int rem = init;
		int n = 16;
		// A popular variant complements rem here
		int i;
		int j;
		for (i=0; i < nLength; i++) {
			rem = (rem ^ (aby[i+nOffset] << (n-8)));
			for (j=0; j <= 7; j++)	{
				if ((rem & 0x8000) == 0x8000) {
					rem = ((rem << 1) ^ 0x1021);
				}
				else {
					rem = (rem << 1);
				}
			}
		}
		// A popular variant complements rem here
		return (rem & 0xffff);
	}
	
	public static int getInt32be(byte[] aby, int nOffset) {
		return   ((aby[nOffset+3]&0x000000ff)
			   | ((aby[nOffset+2]<<8)&0x0000ff00)
			   | ((aby[nOffset+1]<<16)&0x00ff0000)
			   | ((aby[nOffset+0]<<24)&0xff000000)); 
	}
	
	public static int getInt32le(byte[] aby, int nOffset) {
		return   ((aby[nOffset+0]&0x000000ff)
			   | ((aby[nOffset+1]<<8)&0x0000ff00)
			   | ((aby[nOffset+2]<<16)&0x00ff0000)
			   | ((aby[nOffset+3]<<24)&0xff000000)); 
	}

	public static long getInt64be(byte[] aby, int nOffset) {
		return   ((aby[nOffset+7]&0x00000000000000ffl)
			   | ((aby[nOffset+6]<<8)&0x000000000000ff00l)
			   | ((aby[nOffset+5]<<16)&0x0000000000ff0000l)
			   | ((aby[nOffset+4]<<24)&0x00000000ff000000l) 
			   | ((aby[nOffset+3]<<32)&0x000000ff00000000l)
			   | ((aby[nOffset+2]<<40)&0x0000ff0000000000l)
			   | ((aby[nOffset+1]<<48)&0x00ff000000000000l)
			   | ((aby[nOffset+0]<<56)&0xff00000000000000l)); 
	}

	public static String buildString(String s, int nCount) {
		StringBuilder sb = new StringBuilder(s.length() * nCount);
		for (int i=0; i < nCount; i++) sb.append(s);
		return sb.toString();
	}
	
	public static String twochar(int n) {
		StringBuffer sb = new StringBuffer();
		if (n<10) sb.append("0");
		sb.append(String.valueOf(n));
		return sb.toString();
	}	
	
/*	public static boolean isRawDevice(String sName) {
		boolean bTest = sName.toLowerCase().startsWith("\\\\");
		return bTest;
	} */
	
	public static int[] guessAdvanced(int nCyl, int nHead, int nSect) {
		int[] result = new int[2];
		int nRes = ((nCyl * nHead * nSect)/24576 + 1)*512;
		if (nRes < 512) nRes = 512;
		if (nRes >= 16384) nRes = 16383;

		result[0] = nRes;
		result[1] = (nCyl / 22) * 16; // nPre
		return result;
	}
	
	public static int guessGeometry(int nSectorsPerTrack, int nTotalSectors) {
		boolean bFound = false;
		int nHeads = 0;
		int nCylinders = 0;		

		while (!bFound && nHeads < 16) {
			nHeads = nHeads + 2;
			nCylinders = nTotalSectors / nHeads / nSectorsPerTrack;
			if ((nCylinders > 1) && (nCylinders < 2049) && (nCylinders * nHeads * nSectorsPerTrack == nTotalSectors)) {
				bFound = true;
			}
		}
		if (!bFound) return -1;
		else return nCylinders;
	}
	
	public static boolean isTextFile(byte[] aby) {
		for (int i=0; i < aby.length; i++) {
			if ((aby[i]<(byte)0x20 && aby[i] != (byte)0x0a && aby[i] != (byte)0x0d && aby[i] != (byte)0x1a) || aby[i]>(byte)126) {
				System.out.println(TIImageTool.langstr("UtilNonText") + ": " + Utilities.toHex(aby[i],2));
				return false;
			}
		}
		return true;
	}
	
	public static String getHash(byte[] aby) {
		// Create a simple hash
		long value = 0;
		for (int i=0; i < aby.length; i++) {
			value = (value << 8) | ((value>>56) & 0xff);
			value ^= aby[i];
		}
		int high = (int)((value >> 32) & 0xffffffff);
		int low = (int)(value & 0xffffffff);
		StringBuilder sb = new StringBuilder();
		sb.append(toHex(high, 8)).append(toHex(low, 8));
		return sb.toString();
	}
}

