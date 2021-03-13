package de.mizapf.timt.util;

import java.io.*;
import java.nio.charset.*;

import de.mizapf.timt.files.*;

/** Reads a HFE format file.
*/
public class HFEReader {

	byte[] m_abyContent;
	int[] m_bufferlen;
	int[] m_trackoffset;
	int m_sectpertrack;
	int m_tracks;
	HFEFormat m_format;
	
	public static void main(String[] arg) {
		byte[] abyFile = null;
		try {
			ImageFormat format = ImageFormat.getImageFormat(arg[0]);
			if (format instanceof HFEFormat) {
				HFEReader readhfe = new HFEReader((HFEFormat)format);
				byte[] output = readhfe.read(abyFile);
				if (arg.length >= 2) {
					FileOutputStream fos = new FileOutputStream(arg[1]);
					fos.write(output);
					fos.close();
				}
				else {
					System.out.println(Utilities.hexdump(output));
				}
			}
			else {
				System.err.println("Not a HFE format image");
			}
		}
		catch (ImageException ix) {
			System.err.println("Image exception: " + ix.getMessage());
		}
		catch (FileNotFoundException fnfx) {
			System.err.println("File not found: " + fnfx.getMessage());
		}
		catch (IOException iox) {
			System.err.println("File unreadable: " + iox.getMessage());
		}
	}
	
	public HFEReader(HFEFormat form) {
		m_format = form;
	}
	
	public byte[] read(byte[] content) throws IOException, ImageException {
		
		System.out.println(m_format.getHeaderInformation());
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		FloppyFileSystem ffs = (FloppyFileSystem)m_format.getFileSystem();
		for (int i=0; i < ffs.getCylinders(); i++) {
			baos.write(m_format.getTrackBytes(i, 0));
		}
		if (ffs.getHeads()>1) {
			for (int i=ffs.getCylinders()-1; i >=0; i--) {
				baos.write(m_format.getTrackBytes(i, 1));
			}
		}

		return baos.toByteArray();
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
