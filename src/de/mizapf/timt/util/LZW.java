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

package de.mizapf.timt.util;
import java.io.*;
import de.mizapf.timt.files.FormatException;
import de.mizapf.timt.TIImageTool;

public class LZW {

	private static final int DEBUG = 0;
	
	byte[] m_abyRaw = null;
	
	class TableEntry {
		int prefix;
		int suffix;
		
		TableEntry(int i, int j) {
			prefix = i;
			suffix = j;
		}
	}

	TableEntry[] m_aEntry;
	int m_nPosition;
	
	byte[] m_abyBuffer;
	int m_nBufLen;
	
	int m_nSavePrefix;
	
	class Bitstream {
		byte[] m_abyBuffer;
		int m_nWidth;
		int m_nPos;	// Bit position
		int m_nInitialWidth;
		
		Bitstream(byte[] abyte, int width) {
			m_abyBuffer = abyte;
			m_nInitialWidth = width;
			m_nWidth = width;
			m_nPos = 0;
		}
		
		void resetWidth() {
			m_nWidth = m_nInitialWidth;
		}
		
		void increaseWidth() {
			m_nWidth++;
		}
		
		int getWidth() {
			return m_nWidth;
		}
		
		int read() {
			// Seems overly complicated, but is not that bad: we know we never
			// read more than 16 bits at once, so we fetch three bytes (possibly
			// padding with 0x00)
			int nValue = 0;
			int nBytePos = m_nPos / 8;
			int nVal = 0;
			if (DEBUG>0) System.out.print(Utilities.toHex(nBytePos,4) + "/" + (m_nPos%8) + " = "); 

			if (nBytePos < m_abyBuffer.length) {
				nValue = (m_abyBuffer[nBytePos] & 0xff) << 16;
				if (DEBUG>0) System.out.print(Utilities.toHex(m_abyBuffer[nBytePos] & 0xff,2) + " ");
			}
			if (nBytePos+1 < m_abyBuffer.length) {
				nValue |= (m_abyBuffer[nBytePos+1] & 0xff) << 8;
				if (DEBUG>0) System.out.print(Utilities.toHex(m_abyBuffer[nBytePos+1] & 0xff,2) + " ");
			}
			if (nBytePos+2 < m_abyBuffer.length) {
				nValue |= (m_abyBuffer[nBytePos+2] & 0xff);
				if (DEBUG>0) System.out.print(Utilities.toHex(m_abyBuffer[nBytePos+2] & 0xff,2) + " ");
			}
						
			// shift left 
			nValue = (nValue << (m_nPos % 8))&0x00ffffff;
			
			// and right
			nValue = nValue >> (24 - m_nWidth);

			m_nPos += m_nWidth;
			if (DEBUG>0) System.out.println("bitpos=" + Utilities.toHex(m_nPos,4) + ", read=" + Utilities.toHex(nValue, 3) + "; ");	
			return nValue;
		}
		
		// Needed when we peek for 257
		void pushBack() {
			m_nPos -= m_nWidth;
		}
		
		boolean available() {
			return (m_nPos < m_abyBuffer.length * 8);
		}
		
		/** Writes the value, given the current width. */
		void write(int nValue) {
			int nBytePos = m_nPos / 8;
			int nBitPos = m_nPos % 8;
			int nRem = m_nWidth;
			int nVal = (nValue << (24 - m_nWidth)) >> nBitPos;
			// ----xxxx xxxxxx-- --------
			
			// Test whether the array must be expanded
			if (nBytePos >= m_abyBuffer.length - 3) {
				// Increase by 25%
				byte[] abyNew = new byte[(int)(m_abyBuffer.length * 1.25)];
				System.arraycopy(m_abyBuffer, 0, abyNew, 0, m_abyBuffer.length);
				m_abyBuffer = abyNew;
				if (DEBUG>0) System.out.println("Increase buffer to " + abyNew.length);
			}
			
			while (nRem > 0) {
				int nCount = (nRem >=8)? (8 - nBitPos) : nRem;
				m_abyBuffer[nBytePos] |= (byte)((nVal >> 16) & 0xff);
				/* System.out.print("aby[" + Utilities.toHex(nBytePos,4) + "] = " 
					+ Utilities.toHex(m_abyBuffer[nBytePos], 2) + " bitpos=" + nBitPos + "  "); */
				nVal = nVal << 8;
				m_nPos += nCount;
				nRem -= nCount;
				nBytePos = m_nPos / 8;
				nBitPos = m_nPos % 8;
			}
			// System.out.println(" bitpos = " + nBitPos);
		}
		
		int getBits() {
			return m_nPos;
		}
		
		public byte[] toByteArray() {
			// Round up to a sector size. A sector has 2048 bits
			byte[] abyResult = new byte[(m_nPos + 2047)/2048 << 8];
			System.arraycopy(m_abyBuffer, 0, abyResult, 0, (m_nPos + 7)/8);
			return abyResult;
		}
	}
	
	public static void main(String[] arg) {
		TIImageTool.localize();
		LZW lzw = null;
		if (arg.length < 3) {
			System.err.println("Usage: LZW [x|c] <file> <outfile>");
			return;
		}
		boolean bExtract = false;
		if (arg[0].equalsIgnoreCase("x")) bExtract = true;
		else {
			if (arg[0].equalsIgnoreCase("c")) bExtract = false;
			else {
				System.err.println("Specify e(x)tract or (c)ompress");
				return;
			}
		}
		try {
			DataInputStream dis = new DataInputStream(new FileInputStream(arg[1]));
			byte[] abyRaw = new byte[dis.available()];
			dis.readFully(abyRaw);
			dis.close();

			lzw = new LZW(abyRaw);
			byte[] abyNew = bExtract? lzw.uncompress() : lzw.compress();
			FileOutputStream fos = new FileOutputStream(arg[2]);
			fos.write(abyNew);
			fos.close();
		}
		catch (FileNotFoundException fnfx) {
			System.err.println("Could not find file " + arg[0]);
		}
		catch (IOException iox) {
			System.err.println("Could not load file " + arg[0]);
		}
		catch (FormatException fx) {
			System.err.println("Data error: " + arg[0]);
		}
	}
	
	public LZW(byte[] abyRaw) {
		m_abyRaw = abyRaw;
		m_aEntry = new TableEntry[4096];
		for (int i=0; i < 258; i++) {
			m_aEntry[i] = new TableEntry(-1, i);
		}
		m_abyBuffer = new byte[8];
	}
	
	private void clearTable() {
		m_nPosition = 0x102;
		m_nSavePrefix = -1;
	}
	
	private void getByteSequence(int key) {
		if (DEBUG>0) System.out.println("get(" +  Utilities.toHex(key, 3) + ")");
		if (key > m_nPosition) {
			if (DEBUG>0) System.out.println("key=" +  Utilities.toHex(key, 3) + " not in table");
		}
		else {
			if (key == m_nPosition) {
				if (DEBUG>0) System.out.println("key=" + Utilities.toHex(key, 3) + " being created");
				if (DEBUG>0) System.out.println("save prefix = " + Utilities.toHex(m_nSavePrefix, 3));
				
				// Get the byte sequence associated to this key
				int pkey = m_nSavePrefix;
				TableEntry te = m_aEntry[pkey];
				int i = 1;
				m_nBufLen = 0;
				while (te.prefix!=-1) {
					te = m_aEntry[te.prefix];
					i++;
				}
				if (i+1 > m_abyBuffer.length) m_abyBuffer = new byte[i+1];
				m_nBufLen = i+1; 

				te = m_aEntry[pkey];
				while (i>0) {
					m_abyBuffer[i-1] = (byte)(te.suffix & 0xff);
					if (te.prefix != -1) te = m_aEntry[te.prefix];
					i--;
				}
				m_abyBuffer[m_nBufLen-1] = m_abyBuffer[0];
			}
			else {
				// Get the byte sequence associated to this key
				TableEntry te = m_aEntry[key];
				int i = 1;
				m_nBufLen = 0;
				while (te.prefix!=-1) {
					te = m_aEntry[te.prefix];
					i++;
				}
				if (i > m_abyBuffer.length) m_abyBuffer = new byte[i];
				m_nBufLen = i; 
				te = m_aEntry[key];
				while (i>0) {
					m_abyBuffer[i-1] = (byte)(te.suffix & 0xff);
					if (te.prefix != -1) te = m_aEntry[te.prefix];
					i--;
				}
			}
			
			if (DEBUG>0) System.out.println("Return (" + m_nBufLen + ") = " + Utilities.byteToHex(m_abyBuffer, m_nBufLen));
			
			// Insert new entry
			if (m_nSavePrefix != -1) {
				m_aEntry[m_nPosition] = new TableEntry(m_nSavePrefix, m_abyBuffer[0] & 0xff);
				if (DEBUG>0) System.out.println("entry [" + Utilities.toHex(m_nPosition,3) + "] = (" + Utilities.toHex(m_nSavePrefix,3) + ", " + Utilities.toHex(m_abyBuffer[0]&0xff,3) + ")");
				m_nPosition++;
			}
			else {
				if (DEBUG>0) System.out.println("first character read ... no entry, saving " + Utilities.toHex(key, 3));
			}
			m_nSavePrefix = key;
		}
	}
	
	public byte[] uncompress() throws FormatException {
			// Clear code: [000]1 0000 0000 
			// Stop code:  [000]1 0000 0001
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		Bitstream bs = new Bitstream(m_abyRaw, 9);
		int nValue = 0;
		int nPeek257 = 0;
		boolean bTerminate = false;
		clearTable();
		bs.resetWidth();

		while (bs.available() && !bTerminate) {
			nValue = bs.read();
			if (nValue == 256) {
				clearTable();
				bs.resetWidth();
				if (DEBUG>0) System.out.println("Got clear table code");
			}
			else {
				if (nValue == 257) {
					bTerminate = true;
					if (DEBUG>0) System.out.println("Got termination code");
				}
				else {
					getByteSequence(nValue); // in m_abyBuffer, len
					baos.write(m_abyBuffer, 0, m_nBufLen);

					if (DEBUG>0) {
						for (int i=0; i < m_nBufLen; i++) System.out.print(Utilities.toHex(m_abyBuffer[i],2) + " ");
						System.out.print("\"");
						for (int i=0; i < m_nBufLen; i++) { if (m_abyBuffer[i] >=32) System.out.print((char)m_abyBuffer[i]); }
						System.out.println("\"");
						//System.out.println("hex: " + Utilities.byteToHex(m_abyBuffer, m_nBufLen));

						System.out.println("p=" + m_nPosition + ", w=" + bs.getWidth() + ", s=" + (m_nPosition>>bs.getWidth()));
					}
					if ((m_nPosition >> bs.getWidth()) !=0) {
						if (bs.getWidth()==12) {
							 if (DEBUG>0) System.out.println("At end of table");
						}
						else {
							if (bs.read()==257) {
								if (DEBUG>0) System.out.println("Found termination code; cancel increase");
								bTerminate = true;
							}
							else {
								bs.pushBack();
								bs.increaseWidth();
								if (DEBUG>0) System.out.println("Increase width to " + bs.getWidth());
							}
						}
					}
				}
			}
		}
		
		if (DEBUG>0) {
			System.out.println("bs.available = " + bs.available() + ", len = " + Utilities.toHex(m_abyRaw.length,4));
			System.out.println("\n" + Utilities.hexdump(0, 0, baos.toByteArray(), baos.toByteArray().length, false));
		}
		
		if (!bTerminate) throw new FormatException(TIImageTool.langstr("Archive"), TIImageTool.langstr("LZWMissingEOA"));
		return baos.toByteArray();
	}
	
	/** Delivers the index and the length of the sequence that fits the
		current sub-sequence in the source array.
		@return int[0] Position, int[1] length
	*/
	private int[] findByteSequence(byte[] aby, int nStartPos) {
		int[] anResult = new int[2];
		int nPos = 0;
		int nLength = 0;
		int nPosTmp = 0; 
		// System.out.println("Find byte sequence at position " + nStartPos + " starting with " + Utilities.toHex(aby[nStartPos],2) + " ; dictionary.length=" + m_nPosition);
		while (nPos < m_nPosition) {
			// Get the length of the current sequence
			nLength = 0;
			nPosTmp = nPos;
			while (nPosTmp != -1) {
				nLength++;
				nPosTmp = m_aEntry[nPosTmp].prefix;
			}
			// Back to the current index
			// System.out.println("nLength = " + nLength);
			nPosTmp = nPos;
			boolean bFail = false;
			int nOffset = nLength-1;
			if (nStartPos + nOffset < aby.length) {
				// Start from the end
				while (nPosTmp != -1 && !bFail) {
					// System.out.println("suffix = " + Utilities.toHex(m_aEntry[nPosTmp].suffix, 2));
					if (m_aEntry[nPosTmp].suffix != (aby[nStartPos + nOffset] & 0xff)) bFail = true;
					nOffset--;
					nPosTmp = m_aEntry[nPosTmp].prefix;
				}
				// System.out.println("nPos = " + Utilities.toHex(nPos,3) + ": fail = " + bFail);
				if (!bFail) {
					anResult[0] = nPos;
					anResult[1] = nLength;
				}
			}
			nPos++;
		}
		return anResult;
	}
	
	public byte[] compress() {
		int nPos = 0;
		// We assume that the result is always shorter than the original
		if (DEBUG>0) System.out.println("Compressing byte array, length " + m_abyRaw.length);
		byte[] abyResult = new byte[m_abyRaw.length];
		Bitstream bs = new Bitstream(abyResult, 9);
		bs.write(256);
		clearTable();
		bs.resetWidth();
		
		while (nPos < m_abyRaw.length) {
			int[] anSeq = findByteSequence(m_abyRaw, nPos);
			// Caution: next debug line may cause OutOfBounds 
			if (DEBUG>0) {
				System.out.print("input[" + Utilities.toHex(nPos,4) + "] = ");
				System.out.print(Utilities.toHex(m_abyRaw[nPos],2) + "; " );
				if (nPos+1 < m_abyRaw.length) System.out.print(Utilities.toHex(m_abyRaw[nPos+1],2));
				System.out.println("");
				System.out.println("output "+ Utilities.toHex(anSeq[0],3) + ", " + bs.getWidth() + " bits");
			}
			bs.write(anSeq[0]);
			if (DEBUG>0) System.out.println("advance input by " + anSeq[1]);
			nPos += anSeq[1];

			// Insert new entry unless we are done
			// We do not increase bit width when we are at the end of the input data
			boolean bWrite = true;
			if (nPos < m_abyRaw.length) {
				if (m_nPosition >> bs.getWidth() != 0) {
					if (bs.getWidth()==12) {
						if (DEBUG>0) System.out.println("At end of table");
						bs.write(256);
						clearTable();
						bs.resetWidth();
						bWrite = false;
					}
					else {
						bs.increaseWidth();
						if (DEBUG>0) System.out.println("Increase width to " + bs.getWidth());
					}
				}
				if (bWrite) {
					m_aEntry[m_nPosition] = new TableEntry(anSeq[0], m_abyRaw[nPos]& 0xff);
					if (DEBUG>0) System.out.println("Add new entry entry[" + Utilities.toHex(m_nPosition,3) + "] = (" + Utilities.toHex(anSeq[0], 3) + ", " + Utilities.toHex(m_abyRaw[nPos]&0xff,2) + ")");
					m_nPosition++;
				}
			}
		}
		if (DEBUG>0) System.out.println("Write End-of-archive; current width=" + bs.getWidth());
		bs.write(257);
		return bs.toByteArray();		
	}
}


/*

0000000: 800b 4a44 5209 109a 4510 4250 0030 0019  ..JDR...E.BP.0..
0000010: 0650 0300 0a24 4844 262c 2008 8000 0312  .P...$HD&, .....
0000020: f46a 2514 2f91 09e4 38b4 2c00 485c 9409  .j%./...8.,.H\..


100000000   0100
000101101	002d
001010010	0052
001000101	0045
001000001	0041
001000100	0044
001001101	004D
001000101   0045
000100000	0020
100001001	0109
010000000   0080
000000011	0003
000000000	0000
000000110	0006
01

now next out    key value
-	R	-		102	-R
R	E	R		103	RE
E	A	E		104	EA
A	D	A		105	AD
D	M	D		106	DM
M	E	M		107	ME 
E	.	E		108	E.  
.	.	.		109 ..
..	<80><109>	10a <109><80>   
<80><3><80>		10b <80><3>
<3><0>


Decode:

in	out	full	conjecture
-	-	-			102 -?
R	R	102 -R		103 R?
E	E	103 RE		104 E?
A	A	104 EA		105 A?
D	D	105 AD		106 D?
M	M	106 DM		107 M?
E	E	107 ME		108 E?
.	.	108 E.		109 .?
<109>

*/
/*
000000: 55 50 44 41 54 45 5f 32 30 30 90 03 00 91 aa 50     UPDATE_200.....P
000010: 91 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
000020: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
000030: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
000040: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
000050: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
000060: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
000070: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
000080: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
000090: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000a0: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000b0: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000c0: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000d0: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000e0: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000f0: 00 00 00 00 00 00 00 00 00 00 00 00 45 4e 44 21     ............END!
*/
/*
000000: 2d 52 45 41 44 4d 45 20 20 20 80 03 00 06 83 50     -README   .....P
000010: 06 00 51 44 45 20 20 20 20 20 20 20 11 00 00 31     ..QDE       ...1
000020: 5e 00 00 00 51 44 45 5f 44 4f 43 20 20 20 80 03     ^...QDE_DOC   ..
000030: 00 48 b9 50 48 00 51 44 45 5f 48 53 20 20 20 20     .H.PH.QDE_HS    
000040: 90 03 00 07 86 50 07 00 00 00 00 00 00 00 00 00     .....P..........
000050: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
000060: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
000070: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
000080: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
000090: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000a0: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000b0: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000c0: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000d0: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000e0: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     ................
0000f0: 00 00 00 00 00 00 00 00 00 00 00 00 45 4e 44 21     ............END!

Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: 22016
        at de.mizapf.timt.util.LZW$Bitstream.write(LZW.java:114)
        at de.mizapf.timt.util.LZW.compress(LZW.java:360)
        at de.mizapf.timt.files.Archive.rebuild(Archive.java:445)
        at de.mizapf.timt.files.Archive.insertFile(Archive.java:272)
        at de.mizapf.timt.files.Directory.updateFile(Directory.java:784)
        at de.mizapf.timt.files.Archive.insertFile(Archive.java:276)
        at de.mizapf.timt.image.PasteAction.go(PasteAction.java:286)
        at de.mizapf.timt.image.TIImageTool.processUserInput(TIImageTool.java:594)
        at de.mizapf.timt.image.TIImageTool.main(TIImageTool.java:460)


*/
