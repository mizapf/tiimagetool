package de.mizapf.timt.util;

import java.io.*;
import java.nio.charset.*;

/** Reads a HFE format file.
*/
public class HFEReader {

	byte[] m_abyContent;
	int[] m_tracklen;
	int[] m_trackoffset;
	int m_sectpertrack;
	int m_tracks;
	
	public static void main(String[] arg) {
		byte[] abyFile = null;
		try {
			FileInputStream fis = new FileInputStream(arg[0]);
			DataInputStream dis = new DataInputStream(fis);
			abyFile = new byte[fis.available()];
			int i=0;
			while (true) {
				int ch = fis.read();
				if (ch==-1) break;
				abyFile[i++] = (byte)ch;
			}

			HFEReader readhfe = new HFEReader();
			byte[] output = readhfe.read(abyFile);
			
			if (arg.length >= 2) {
				FileOutputStream fos = new FileOutputStream(arg[1]);
				fos.write(output);
				fos.close();
			}
		}
		catch (FileNotFoundException fnfx) {
			System.err.println("File not found: " + fnfx.getMessage());
		}
		catch (IOException iox) {
			System.err.println("File unreadable: " + iox.getMessage());
		}
	}
	
	public HFEReader() {
	}
	
	public byte[] read(byte[] content) throws IOException {
		String sig = null;
		
		try {
			sig = new String(content, 0, 8, Charset.forName("ISO-8859-1"));
		}
		catch (UnsupportedCharsetException ux) {
			ux.printStackTrace();
			sig = new String(content, 0, 8);
		}
		catch (StringIndexOutOfBoundsException six) {
			six.printStackTrace();
			sig = "*unknown**";
		} 
		
		if (!sig.equals("HXCPICFE")) {
			throw new IOException("Not a HFE format file");
		}
		
		String[] encoding = { "ISO MFM", "Amiga MFM", "ISO FM", "EMU FM", "unknown" };
		String[] flopintf = { "IBM PC DD", "IBM PC HD", "Atari ST DD", "Atari ST HD",
			"Amiga DD", "Amiga HD", "CPC DD", "Generic Shugart DD", "IBM PC ED",
		"MSX2 DD", "C64 DD", "EMU Shugart", "S950 DD", "S950 HD", "Disable" };
		
		int revision = content[8] & 0xff;
		m_tracks = content[9] & 0xff;
		int sides = content[10] & 0xff;
		int enc = content[11] & 0xff;
		int bitrate = Utilities.getInt16rev(content, 12);
		int rpm = Utilities.getInt16rev(content, 14);
		int mode = content[16] & 0xff;
		int tracklistoffset = Utilities.getInt16rev(content, 18); 
		boolean writable = (content[20] & 0xff) == 0xff;
		boolean singlestep = (content[21] & 0xff)==0xff;
		boolean track0s0_alt = (content[22] & 0xff)==0x00;
		int track0s0_enc = content[23] & 0xff;
		boolean track0s1_alt = (content[24] & 0xff)==0x00;
		int track0s1_enc = content[25] & 0xff;
				
		System.out.println("Revision = " + revision);
		System.out.println("Tracks = " + m_tracks);
		System.out.println("Sides = " + sides);
		System.out.println("Encoding = " + encoding[enc]);
		System.out.println("Bitrate = " + bitrate + " kHz");
		System.out.println("RPM = " + rpm);
		System.out.println("Interface mode = " + flopintf[mode]);
		System.out.println("Track list offset = " + tracklistoffset);
		System.out.println("writable = " + (writable? "yes" : "no"));
		System.out.println("singlestep = " + singlestep);
		if (track0s0_alt) System.out.println("Special encoding for track 0, side 0 = " + track0s0_enc);
		if (track0s1_alt) System.out.println("Special encoding for track 0, side 1 = " + track0s1_enc);
		
		// Lookup Table
		m_trackoffset = new int[m_tracks];
		m_tracklen = new int[m_tracks];
		
		for (int i=0; i < m_tracks; i++) {
			m_trackoffset[i] = 512 * Utilities.getInt16rev(content, tracklistoffset * 512 + 4*i);
			m_tracklen[i] = Utilities.getInt16rev(content, tracklistoffset * 512 + 4*i + 2);
			// System.out.println("Track " + i + ": " + trackoffset[i] + ", length = " + tracklen[i]);
		}
		
		m_sectpertrack = ((enc==2)?9 : 18);
		byte[] image = new byte[256 * sides * m_tracks * m_sectpertrack];
		
		boolean doublesamp = (bitrate == 250 && enc == 2);

		// Track 0
		for (int track = 0; track < m_tracks; track++) {
			
			// Read side 1
			byte[] side1 = new byte[m_tracklen[track]/2];
			byte[] side2 = new byte[m_tracklen[track]/2];
			int pos1 = 0;
			
			int remaining = m_tracklen[track];
			
			int count = 512;
			int pos = m_trackoffset[track];		
			
			while (remaining > 0) {
				// System.out.println("Remaining = " + remaining);
				if (remaining < 512) {
					count = remaining/2;
					remaining = 0;
				}
				else {
					count = 256;
					remaining -= 512;
				}
				
				for (int i=0; i < count; i++) {
					side1[pos1] =  content[pos+i];
					side2[pos1] =  content[pos+i+256];
					pos1++;
				}
				pos += 512;				
			}
						
			readTrack(side1, track, image, doublesamp, false);
			readTrack(side2, track, image, doublesamp, false);
		}
		return image;
	}

	void readTrack(byte[] currentTrack, int track, byte[] image, boolean doublesamp, boolean debug) {

		//			System.out.print("<" + Utilities.toHex(b, 2) +"> ");
		int value = 0;
		int bits = 0;
		int previousdata = 0;
		int clock = 0;
		int currentdata = 0;
		boolean mark = false;
		int header = 0;
		int body = 0;
		boolean lastmark = false;
		int trackno = 0;
		int headno = 0;
		int sectno = 0;
		int pos = 0;
		
		for (int i=0; i < m_tracklen[track]/2; i++) {
			byte b = currentTrack[i];
	
			for (int j=0; j < 8; j++) {
				if (doublesamp) {
					if ((j&1)==1) {
						// System.out.print((char)((b&1) + 0x30));
						value = (value << 1) | (b & 1);
						bits++;
					}
				}
				else {
					//				System.out.print((char)((b&1) + 0x30));
					// 4489 = 01 00 01 00 10 00 10 01
					if ((j&1)==1) {
						currentdata = (b&1);
						value = (value << 1) | currentdata;
						if (currentdata == 0 && previousdata == 0) {
							if (clock != 1) mark = true;
						}
						previousdata = currentdata;							
						bits++;
					}
					else {
						clock = (b&1);
					}
				}
				// 0100 1001
				b >>= 1;
			}
			
			if (bits==8) {
				if (debug) {
					if (mark && !lastmark) System.out.println("");
					if (mark) System.out.print("[" + Utilities.toHex(value, 2) + "] ");
					else System.out.print(Utilities.toHex(value, 2) + " ");
				}
				
				if (header>0) {
					if (header == 4) trackno = value;
					if (header == 3) headno = value;
					if (header == 2) sectno = value;
					// else System.out.print(Utilities.toHex(value, 2) + " ");
					header--;
				}
				else {					
					if (body>0) {
						if (body==256) {
							if (headno==0) pos = (trackno * m_sectpertrack + sectno)*256;
							else pos = (((2*m_tracks-1)-trackno) * m_sectpertrack + sectno)*256;
						}
						body--;
						image[pos++] = (byte)(value&255);
						// if (debug) if (body==0) System.out.println("");
					}
					else {
						if (lastmark) {
							header = (!mark && value == 0xfe)? 4 : 0;
							body = (!mark && value == 0xfb)? 256 : 0;
						}
						// System.out.print(Utilities.toHex(value, 2) + " ");
					}
				}				
				bits = 0;
				value = 0;
				lastmark = mark;
				mark = false;
			}
		}
	}
}

/*
0000400: aaa8 a822 aaaa aaaa aaaa aaaa aaaa aaaa  ..."............
0000410: aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa  ................
0000420: aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa  ................
0000430: aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa  ................
0000440: aaaa aaaa 2222 2222 2222 2222 2222 2222  ....""""""""""""
0000450: 2222 2222 2222 2222 2222 2222 aa88 a82a  """"""""""""...*
0000460: 2222 2222 2222 2222 2222 2222 2222 22a2  """"""""""""""".
0000470: aaaa 22a2 aaa2 22aa aaaa aaaa aaaa aaaa  .."...".........
0000480: aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa  ................
0000490: aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa  ................
00004a0: aaaa aaaa 2222 2222 2222 2222 2222 2222  ....""""""""""""
00004b0: 2222 2222 2222 2222 2222 2222 aa88 28aa  """"""""""""..(.
00004c0: a222 222a a222 aaaa a222 aaaa a2a2 a222  .""*."..."....."
00004d0: 222a 2222 222a 2222 222a 2222 222a 2222  "*"""*"""*"""*""
00004e0: 222a 2222 222a 2222 2222 22a2 a22a 2a22  "*"""*"""""..**"
00004f0: 2222 2aa2 a222 a222 a2a2 22aa a222 2aaa  ""*.."."..".."*.

Double sampling: 25000 bytes per track = 200000 bits per track; FM has 50000 cells per track (per side)

Byte: 76543210
      * * * *
      <-<-<-<-
Bits 0,2,4,6 ignored
      
1111011101111010 = F77A   11111100  IXAM 

Lead-in
1111111111111111 1111111111111111 1111111111111111 1111111111111111 
1111111111111111 1111111111111111 1111111111111111 1111111111111111 
1111111111111111 1111111111111111 1111111111111111 1111111111111111 
1111111111111111 1111111111111111 1111111111111111 1111111111111111 
 
6 x 0
1010101010101010 1010101010101010 1010101010101010 1010101010101010 
1010101010101010 1010101010101010 

1111010101111110 F57E IDAM

1010101010101010 1010101010101010 1010101010101010 1010101010101011  (T,H,S,G)

CRC
1111111110101011 1111101110101111 

GAP2
1111111111111111 1111111111111111 1111111111111111 1111111111111111
1111111111111111 1111111111111111 1111111111111111 1111111111111111 
1111111111111111 1111111111111111 1111111111111111  

6 x 0
1010101010101010 1010101010101010 1010101010101010 1010101010101010 
1010101010101010 1010101010101010 

1111010101101111 F56F DAM 

1011101010101110 1011101011111111 1011101011111111 1011101110111010 
1010111010101010 1010111010101010 1010111010101010 1010111010101010 
1010111010101010 1010111010101010 1010101010101011 1011111011101010 
1010101011101011 1011101010111010 1011101110101111 1011101011101111 


*/


/*
4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 00 00 00 00 00 00 00 00 00 00 00 00 
[a1] [a1] [a1] fe 00 01 07 01 67 9a 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 00 00 00 00 00 00 00 00 00 00 00 00 
[a1] [a1] [a1] fb 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 

4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e         <--- am Ende des Tracks!!?!   112 Bytes vor Ende -> 224 koinzidiert mit der Länge des letzten Abschnitts

e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 e5 
e5 e5 
78 27 

4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 
4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 
4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 
4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 
4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e 4e
*/
