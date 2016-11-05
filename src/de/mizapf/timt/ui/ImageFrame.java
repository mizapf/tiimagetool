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

package de.mizapf.timt.ui;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.imageio.*;
import java.awt.image.*;

import de.mizapf.timt.files.FormatException;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;

public class ImageFrame extends JFrame {
	
	static final int TIARTIST = 1;
	static final int MYART = 2;
	static final int YAPP = 3;
	static final int AUSTRI = 4;
	static final int FRACTALS = 5;

	TIImageTool m_app;
	
	byte[] m_abyContent;
	int m_nType;
	int m_nRecLength;
	boolean m_bInterlace;
	String m_sName;
	int m_nRows;
	int m_nColumns;	
	
	public class ImagePanel extends Component {
		BufferedImage m_bi;
		Dimension m_dim;
		
		ImagePanel(BufferedImage bi, Dimension dim) {
			m_bi = bi;
			m_dim = dim;
		}
		
		public void paint(Graphics g) {
			g.drawImage(m_bi, 0, 0, (int)m_dim.getWidth(), (int)m_dim.getHeight(), null);
		}
		
		public Dimension getPreferredSize() {
			if (m_bi == null) {
				return new Dimension(100,100);
			} 
			else {
				return new Dimension((int)m_dim.getWidth(), (int)m_dim.getHeight());
			}
		}
	}
	
	ImageFrame(String sName, byte[] abyContent, int nRecLength, TIImageTool app) {
		super(sName);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		m_nRecLength = nRecLength;
		m_sName = sName;
		m_abyContent = abyContent;
		m_app = app;
		if (m_app != null) m_app.registerFrame(this);
	}
	
	public static void main(String[] arg) {
		try {
			DataInputStream dis = new DataInputStream(new FileInputStream(arg[0]));
			byte[] abyContent = new byte[dis.available()];
			dis.readFully(abyContent);
			
/*			if (arg[0].endsWith(".bmp")) {
				int nStart = Utilities.getInt32le(abyContent, 0x000a);
				System.out.println("Start = " + Utilities.toHex(nStart,4));
				int nWidth = Utilities.getInt32le(abyContent, 0x0012);
				System.out.println("Width = " + nWidth);
				int nColumn = 0;
				int nOffset = 0;
				while (nOffset + nStart < abyContent.length) {
					System.out.println(nColumn + " = " + Utilities.toHex(abyContent[nStart + nOffset], 2) + " " + Utilities.toHex(abyContent[nStart + nOffset+1], 2) + " " +  Utilities.toHex(abyContent[nStart + nOffset+2], 2)); 
					nOffset = nOffset + 3;
					nColumn++;
					if (nColumn==nWidth) {
						System.out.println("nOffset = " + Utilities.toHex(nOffset, 8));
						nOffset = (nOffset + 3) & 0xfffffffc;
						nColumn = 0;
						System.out.println("new nOffset = " + Utilities.toHex(nOffset, 8));
					}
				}
			} */
			ImageFrame ifr = new ImageFrame(arg[0], abyContent, 0, null);
			ifr.showImage1(null, 640);
		}
		catch (FileNotFoundException fnfx) {
			System.err.println("Could not find file " + arg[0]);
		}
		catch (IOException iox) {
			System.err.println("Could not load file " + arg[0]);
			iox.printStackTrace();
		}
	}

	/** Test method for BMP. */
	void showImage1(Dimension dim, int nWidth) throws IOException {
		BufferedImage bi = ImageIO.read(new ByteArrayInputStream(m_abyContent));
		if (dim == null) dim = new Dimension(bi.getWidth(), bi.getHeight());
	//	ImageFrame ifr = new ImageFrame(sName, bi, dim, nWidth, (int)(nWidth * 0.75));
		int nHeight = (int)(nWidth * 0.75);
		Dimension dim1 = new Dimension(nWidth, nHeight);
		ImagePanel imp = new ImagePanel(bi, dim1);
		Container cont = getContentPane(); 
		cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));
		add(imp);		
		pack();
		setVisible(true);
	}
	
	void showImage(Dimension dim, int nWidth) throws IOException, FormatException {
		// System.out.println("Display image " + sName + ", type " + nType);
		// Create a BMP image
		
		m_nType = detectFormat();
		
		BufferedImage bi = null;
		byte[] abyResult = null;
		switch (m_nType) {
		case MYART:
		case YAPP:
			abyResult = myartToBMP(m_nColumns==512, m_nType==YAPP);
			bi = ImageIO.read(new ByteArrayInputStream(abyResult));
			if (dim == null) dim = new Dimension(bi.getWidth(), bi.getHeight());
			break;
		case FRACTALS:
			abyResult = fractalsToBMP();
			bi = ImageIO.read(new ByteArrayInputStream(abyResult));
			if (dim == null) dim = new Dimension(bi.getWidth(), bi.getHeight());
			break;
		case TIARTIST:
			abyResult = tiartistToBMP();
			bi = ImageIO.read(new ByteArrayInputStream(abyResult));
			if (dim == null) dim = new Dimension(bi.getWidth(), bi.getHeight());
			break;
		}

		int nHeight = (int)(nWidth * 0.75);
		Dimension dim1 = new Dimension(nWidth, nHeight);
		ImagePanel imp = new ImagePanel(bi, dim1);
		Container cont = getContentPane(); 
		cont.setLayout(new BoxLayout(cont, BoxLayout.Y_AXIS));
		add(imp);

		Point loc = m_app.getMainFrame().getLocationOnScreen();		
		setLocation(loc.x+20, loc.y+20);
		setLocationByPlatform(true);
		setVisible(true);
		pack();
		toFront();		
	}
	
	/*
		TODO: Use additional query dialog (ambiguous format definition)
		Flag = 9e seems to be MyArt G6 as well.
		MyArt uses x200 for 512 times in G6
		
		Try to determine mode from file contents
	*/
	byte[] myartToBMP(boolean g6, boolean yapp) throws IOException, FormatException {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		baos.write('B');
		baos.write('M');
		writeInt32le(baos, 0);   // BMP size
		writeInt32le(baos, 0);   // always 0
		writeInt32le(baos, 54);
		writeInt32le(baos, 40);  // Header length
		
		// Create internal color table (not to be used in BMP)
		int nPos = 0;
		int nColors = g6? 16 : 256;			
		int[] anColor = new int[nColors];
		
		for (int i=0; i < nColors; i++) {
			int nRed = 0;
			int nGreen = 0;
			int nBlue = 0;
			
			if (g6) {
				nRed = ((m_abyContent[i*2+2] >> 4) & 0x07) << 5;
				nGreen = (m_abyContent[i*2+3] & 0x07) << 5;
				nBlue = (m_abyContent[i*2+2] & 0x07) << 5;
			}
			else {
				nRed = ((i >> 2) & 0x07) << 5;
				nGreen = ((i >> 5) & 0x07) << 5;
				nBlue = (i & 0x03) << 6;
			}
			anColor[i] = (nRed << 16) | (nGreen << 8) | nBlue; 
		}
		
		writeInt32le(baos, g6? 512 : 256);   // width
		writeInt32le(baos, 0);   // height
		writeInt16le(baos, 0);   // planes
		writeInt16le(baos, 24);  // bitcount
		writeInt32le(baos, 0);   // compression
		writeInt32le(baos, 0);   // image size
		writeInt32le(baos, 0);   // X pixels per meter
		writeInt32le(baos, 0);   // Y pixels per meter
		writeInt32le(baos, 0);   // color table
		writeInt32le(baos, 0);   // used colors
		
		int nColumn = 0;
		int nRow = 0;
		boolean bDone = false;
		int nCount = 0;
		int nLineBytes = 0;
		
		nPos = (g6 | yapp)? 0x22 : 0x02;
		
		ByteArrayOutputStream baosLine = new ByteArrayOutputStream();
		// Now add the lines
		while (!bDone) {
			int nValue = ((m_abyContent[nPos]<<8) & 0xff00) | (m_abyContent[nPos+1] & 0xff);
			// System.out.println(Utilities.toHex(nValue, 4));
			nPos += 2;
			int nColor = 0;
			int nMaxColumn = g6? 512 : 256;
			
			if (g6) {
				nColor = anColor[(nValue >> 12) & 0xf];
				nCount = (nValue & 0x1ff);
				if (nCount==0) nCount = 512;  // CHECK: is that correct?
			}
			else {
				nColor = anColor[(nValue >> 8) & 0xff];
				nCount = nValue & 0xff;
				if (nCount==0) nCount = 256;
			}
			
			for (int i=0; i < nCount; i++) {
				writeInt24le(baosLine, nColor);
				nLineBytes += 3;
			}
			
			nColumn += nCount;
			
			if (nColumn >= nMaxColumn) {
				while ((nLineBytes & 0x03) != 0) {
					baosLine.write(0);
					nLineBytes++;
				}
				// System.out.println("Row " + nRow + ", line bytes = " + nLineBytes);
				baos.write(baosLine.toByteArray());
				if (!m_bInterlace) baos.write(baosLine.toByteArray());
				baosLine = new ByteArrayOutputStream();
				nLineBytes = 0;
				nColumn = 0;
				nRow++;
			}
			if (nRow == m_nRows || nPos >= m_abyContent.length) bDone = true;
		}
		
		byte[] abyResult = baos.toByteArray();
		setInt32le(abyResult, 0x16, -(nRow*2));					// height
		setInt32le(abyResult, 0x02, abyResult.length);
		setInt32le(abyResult, 0x22, abyResult.length - 54);	// Image size
		// System.out.println(Utilities.hexdump(0, 0, abyResult, abyResult.length, false));	
		return abyResult;
	}
	
	/** 
		FRACTALS! format 
	*/
	byte[] fractalsToBMP() throws IOException, FormatException {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		baos.write('B');
		baos.write('M');
		writeInt32le(baos, 0);   // BMP size
		writeInt32le(baos, 0);   // always 0
		writeInt32le(baos, 54);
		writeInt32le(baos, 40);  // Header length
		
		// Create internal color table (not to be used in BMP)
		int nPos = 0;
		int nColors = 16;			
		int[] anColor = new int[nColors];
		
		for (int i=0; i < nColors; i++) {
			int nRed = 0;
			int nGreen = 0;
			int nBlue = 0;
			
			nRed = ((m_abyContent[i*2+2] >> 4) & 0x07) << 5;
			nGreen = (m_abyContent[i*2+3] & 0x07) << 5;
			nBlue = (m_abyContent[i*2+2] & 0x07) << 5;
			anColor[i] = (nRed << 16) | (nGreen << 8) | nBlue; 
		}
		
		writeInt32le(baos, 512);   // width
		writeInt32le(baos, 0);   // height
		writeInt16le(baos, 0);   // planes
		writeInt16le(baos, 24);  // bitcount
		writeInt32le(baos, 0);   // compression
		writeInt32le(baos, 0);   // image size
		writeInt32le(baos, 0);   // X pixels per meter
		writeInt32le(baos, 0);   // Y pixels per meter
		writeInt32le(baos, 0);   // color table
		writeInt32le(baos, 0);   // used colors
		
		int nColumn = 0;
		int nRow = 0;
		boolean bDone = false;
		int nCount = 0;
		int nLineBytes = 0;
		boolean bSingle = false;
		int nMaxColumn = 512;
		int nSingleCount = 0;
			
		nPos = 0xff;
		
		ByteArrayOutputStream baosLine = new ByteArrayOutputStream();
		// Now add the lines
		while (!bDone) {
			int nValue = 0;
			int nColor = 0;

			if (!bSingle) {
				nValue = ((m_abyContent[nPos]<<8) & 0xff00) | (m_abyContent[nPos+1] & 0xff);
				// System.out.println(Utilities.toHex(nValue, 4));
				nPos += 2;
				
				nCount = (nValue & 0xfff);				
				if ((nCount & 0x0800)!=0) {
					bSingle = true;
					nSingleCount = nCount & 0x1ff;
					writeInt24le(baosLine, anColor[(nValue >> 12) & 0xf]);
					nLineBytes += 3;
					// nSingleCount--;
					nColumn++;
				}
				else {
					bSingle = false;
					nSingleCount = 0;
				}
				// if (nCount==0) nCount = 512;
			}
			else {
				nValue = m_abyContent[nPos] & 0xff;
				nPos++;
				// System.out.println(Utilities.toHex(nValue, 2));
				writeInt24le(baosLine, anColor[(nValue >> 4) & 0xf]);
				nLineBytes += 3;
				nColumn++;
				nValue = nValue << 4;
				nSingleCount--;
				if (nSingleCount!=0) {
					writeInt24le(baosLine, anColor[(nValue >> 4) & 0xf]);
					nLineBytes += 3;
					nColumn++;
					nSingleCount--;
				}
			}
			
			if (!bSingle) {			
				nColor = anColor[(nValue >> 12) & 0xf];
				for (int i=0; i < nCount; i++) {
					writeInt24le(baosLine, nColor);
					nLineBytes += 3;
				}
				nColumn += nCount;
			}		

			if (nSingleCount==0) bSingle = false;
			
			if (nColumn >= nMaxColumn) {
				while ((nLineBytes & 0x03) != 0) {
					baosLine.write(0);
					nLineBytes++;
				}
				// System.out.println("Row " + nRow + ", line bytes = " + nLineBytes);
				baos.write(baosLine.toByteArray());
				if (!m_bInterlace) baos.write(baosLine.toByteArray());
				baosLine = new ByteArrayOutputStream();
				nLineBytes = 0;
				nColumn = 0;
				nRow++;
			}
			if (nRow == m_nRows || nPos >= m_abyContent.length) bDone = true;
		}
		
		byte[] abyResult = baos.toByteArray();
		setInt32le(abyResult, 0x16, -(nRow*2));					// height
		setInt32le(abyResult, 0x02, abyResult.length);
		setInt32le(abyResult, 0x22, abyResult.length - 54);	// Image size
		// System.out.println(Utilities.hexdump(0, 0, abyResult, abyResult.length, false));	
		return abyResult;
	}
	
	/** 
		TIARTIST format 
		Standard colors:
			RGB_BLACK,
			RGB_BLACK,
			MAKE_RGB(33, 200, 66),
			MAKE_RGB(94, 220, 120),
			MAKE_RGB(84, 85, 237),
			MAKE_RGB(125, 118, 252),
			MAKE_RGB(212, 82, 77),
			MAKE_RGB(66, 235, 245),
			MAKE_RGB(252, 85, 84),
			MAKE_RGB(255, 121, 120),
			MAKE_RGB(212, 193, 84),
			MAKE_RGB(230, 206, 128),
			MAKE_RGB(33, 176, 59),
			MAKE_RGB(201, 91, 186),
			MAKE_RGB(204, 204, 204),
			RGB_WHITE
	*/
	byte[] tiartistToBMP() throws IOException, FormatException {
		ByteArrayOutputStream baosLine = new ByteArrayOutputStream();
		int nLineBytes = 0;
				
		int[] anColor = { 0x00000000, 0x00000000,
							0x0021c842, 0x005edc78,
							0x005455ed, 0x007d76fc,
							0x00d4524d, 0x0042ebf5,
							0x00fc5554, 0x00ff7978,
							0x00d4c154, 0x00e6ce80,
							0x0021b03b, 0x00c95bba,
							0x00cccccc, 0x00ffffff };

		int[] anMask = { 0x80, 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01 };					
							
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		baos.write('B');
		baos.write('M');
		writeInt32le(baos, 0);   // BMP size
		writeInt32le(baos, 0);   // always 0
		writeInt32le(baos, 54);
		writeInt32le(baos, 40);  // Header length
							
		writeInt32le(baos, 256);   // width
		writeInt32le(baos, 0);   // height
		writeInt16le(baos, 0);   // planes
		writeInt16le(baos, 24);  // bitcount
		writeInt32le(baos, 0);   // compression
		writeInt32le(baos, 0);   // image size
		writeInt32le(baos, 0);   // X pixels per meter
		writeInt32le(baos, 0);   // Y pixels per meter
		writeInt32le(baos, 0);   // color table
		writeInt32le(baos, 0);   // used colors
		
		for (int row=0; row<192; row++) {
			for (int col=0; col<256; col++) {
				
				int nOffset = (row/8)*256 + (col & 0x00f8) + (row % 8);
				int nBitpos = (col % 8);
				int nColorIndex = 0;
				
				boolean bSet = (m_abyContent[nOffset] & anMask[nBitpos])!=0;
				if (bSet) nColorIndex = (m_abyContent[6144+nOffset] >> 4) & 0x0f;
				else nColorIndex = m_abyContent[6144+nOffset] & 0x0f;
				
				writeInt24le(baosLine, anColor[nColorIndex]);
				nLineBytes += 3;
			}
			
			while ((nLineBytes & 0x03) != 0) {
				baosLine.write(0);
				nLineBytes++;
			}

			// System.out.println("Row " + nRow + ", line bytes = " + nLineBytes);
			baos.write(baosLine.toByteArray());
			baosLine = new ByteArrayOutputStream();
			nLineBytes = 0;
		}
		
		byte[] abyResult = baos.toByteArray();
		setInt32le(abyResult, 0x16, -192);					// height
		setInt32le(abyResult, 0x02, abyResult.length);
		setInt32le(abyResult, 0x22, abyResult.length - 54);	// Image size
		// System.out.println(Utilities.hexdump(0, 0, abyResult, abyResult.length, false));	
		return abyResult;
	}
	
	private void setInt32le(byte[] aby, int nPos, int n) {
		aby[nPos] = (byte)(n & 0xff);
		aby[nPos+1] = (byte)((n >> 8) & 0xff);
		aby[nPos+2] = (byte)((n >> 16) & 0xff);
		aby[nPos+3] = (byte)((n >> 24) & 0xff);
	}
	
	private void writeInt32le(ByteArrayOutputStream os, int n) {
		os.write(n & 0xff);
		n = n >> 8;
		writeInt24le(os, n);
	}
	
	private void writeInt24le(ByteArrayOutputStream os, int n) {
		os.write(n & 0xff);
		n = n >> 8;
		writeInt16le(os, n);
	}

	private void writeInt16le(ByteArrayOutputStream os, int n) {
		os.write(n & 0xff);
		n = n >> 8;
		os.write(n & 0xff);
	}

	private boolean doRLECheck(int nStartPos, int nWidth, int nRows, int nColumns) {
		boolean bDone = false;
		int nPos = nStartPos;
		int nColumn = 0; 
		int nMask = (nWidth == 8)? 0x00ff : 0x03ff;
		int nRow = 0;
		
		while (!bDone) {
			int nLen = Utilities.getInt16(m_abyContent, nPos) & nMask;
			if (nLen==0 && nColumns==256) nLen=256;
			nPos += 2;
			nColumn += nLen;
			// System.out.println(m_sName + ": row=" + nRow + ", col=" + nColumn + ", bytes left=" + (m_abyContent.length-nPos));
			if (nColumn == nColumns) {
				// Next row
				nColumn = 0;
				nRow++;
				if (nRow == nRows) return true;
			}
			else {
				if (nColumn > nColumns) return false;
			}
			if (nPos >= m_abyContent.length) bDone = true;
		}
		return false;
	}
	
	private int detectFormat() throws FormatException {
		m_nType = 0;
		
		if (m_nRecLength == 0) {
			m_nRows = 192;
			m_nColumns = 256;
			return TIARTIST;
		}

		if (m_nRecLength == 255) {
			if (((m_abyContent[1] == 'C')||(m_abyContent[1] == 'M')) && (m_abyContent[0x44]==(byte)0)
				&& (m_abyContent[0x45]==(byte)0) && (m_abyContent[0x46]==(byte)0) && (m_abyContent[0x47]==(byte)0)) {
				// System.out.println("Detected as FRACTALS! format");
				m_nRows = 212;
				m_nColumns = 512;
				return FRACTALS;
			}
		}
		
		// YAPP format / AUSTRI
		if ((m_abyContent[1] & 0x3b) == 0x3a) {
			int nRow = 212;
			int nColumn = 512; 
			if ((m_abyContent[1] & 0x80)==0x00) nRow = 424;
			if ((m_abyContent[1] & 0x04)==0x04) nColumn = 256;
			
			// System.out.println("Check YAPP");
			if (doRLECheck((nColumn==256)? 0x02 : 0x22, (nColumn==256)? 8 : 9, nRow, nColumn)==true) {
				m_nRows = nRow;
				m_nColumns = nColumn;
				// System.out.println("Detected as YAPP, rows=" + m_nRows + ", cols=" + m_nColumns);
				return YAPP;
			}
			// else System.out.println("Failed RLE check");
		}

		// check for MyartG6

		//	G6: Headered format, 512 columns, indexed colors.
		//	Must be a sequence of lines with exactly 512 columns, starting at 
		// offset 0x22.
		// Rows = 192 or 212		
		// System.out.println("Check MYART G6 212");
		if (doRLECheck(0x22, 9, 212, 512)==true) {
			m_nRows = 212;
			m_nColumns = 512;
			// System.out.println("Detected as MYART, rows=" + m_nRows + ", cols=" + m_nColumns);
			return MYART;
		}
		//else System.out.println("Failed RLE check");
		//System.out.println("Check MYART G6 192");
		if (doRLECheck(0x22, 9, 192, 512)==true) {
			m_nRows = 192;
			m_nColumns = 512;
			//  System.out.println("Detected as MYART, rows=" + m_nRows + ", cols=" + m_nColumns);
			return MYART;
		}
		// else System.out.println("Failed RLE check");
		
		// check for MyartG7
		//	G7: 2 bytes header, 256 columns, 256 colors.
		//  Must be a sequence of lines with exactly 256 columns, starting at 
		//	offset 0.
		//	Rows = 192 or 212
		// System.out.println("Check MYART G7 212");
		if (doRLECheck(2, 8, 212, 256)==true) {
			m_nRows = 212;
			m_nColumns = 256;
			// System.out.println("Detected as MYART, rows=" + m_nRows + ", cols=" + m_nColumns);
			return MYART;
		}
		// else System.out.println("Failed RLE check");

		// System.out.println("Check MYART G7 192");
		if (doRLECheck(2, 8, 192, 256)==true) {
			m_nRows = 192;
			m_nColumns = 256;
			// System.out.println("Detected as MYART, rows=" + m_nRows + ", cols=" + m_nColumns);
			return MYART;
		}
		// else System.out.println("Failed RLE check");
		
		throw new FormatException(m_sName, "is not a picture format");
	}
}
