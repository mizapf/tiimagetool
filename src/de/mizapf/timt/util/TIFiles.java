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

import java.io.*;
import java.util.Calendar;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class TIFiles {
		
	public static final String NOHEADER = ".NOHEADER";
	public static final String NONAME = ".NONAME";
	
	byte[] m_abyTFContent;

	// For creating a new file
	String m_name;
	int m_type;
	int m_reclen;
	int m_pos;
	int m_records;
	int m_sectors;
	ByteArrayOutputStream m_baos;
	//
	
	public TIFiles(byte[] abyTFContent) {
		m_abyTFContent = abyTFContent;
	}
		
	public TIFiles(String name, int type, int reclen) {
		m_name = name;
		m_type = type;
		m_reclen = reclen;
		m_baos = new ByteArrayOutputStream();
		m_pos = 0;
		m_records = 0;
		m_sectors = 0;
	}
	
	public static boolean hasHeader(byte[] aby) {
		byte[] files = "TIFILES".getBytes();
		// Allow for 08 header
		if (aby[0] != 0x07 && aby[0] != 0x08) return false;
		for (int i=0; i < 7; i++) {
			if (aby[i+1] != files[i]) return false;
		}
		return true;
	}
	
	/** Must be 128 + n*256, or at least as long as to hold all bytes. 
		Returns a new byte array aligned to sector sizes.
		
		Note: Some variants do not pad the last sector with zeros. 
	*/
	public static byte[] normalizeLength(byte[] aby) {
		int expectedSize = getTotalNumberOfSectors(aby) * TFileSystem.SECTOR_LENGTH;
		if (aby.length == expectedSize + 128) return aby;

		byte[] newcont = new byte[expectedSize + 128];
		int count = aby.length;
		if (count > newcont.length) {
			System.err.println(String.format(TIImageTool.langstr("TFITrunc"), count - newcont.length));
			count = newcont.length;
		}
		System.arraycopy(aby, 0, newcont, 0, count);
		return newcont;
	}
	
	public static TIFiles createFromFile(TFile file) throws IOException, ImageException {
		
		byte[] abyContent = file.getSectorContent(); // throws IOX
		
		int nLength = abyContent.length + 128;
		byte[] abyTFContent = new byte[nLength];
		
		int nTotalSectors = file.getAllocatedSectors();
		int nEofOffset = file.getEOFOffset();
		
		int nRecordCount = 0;
		int nRecordsPerSector = 0;
		
		if (!file.isProgram() && !file.isEmulateFile()) {
			nRecordsPerSector = file.getRecordsPerSector();
			if (!file.hasFixedRecordLength()) {	
				// Variable record files have L3 = total sectors
				nRecordCount = file.getRecordCount();
				nTotalSectors = nRecordCount;
				if (nRecordCount>65535) {
					System.err.println(TIImageTool.langstr("TFINotSupp"));
					throw new ImageException(TIImageTool.langstr("TFISectors"));
				}
			}
			else {
				// Fixed record files have L3 = file length in bytes / Record length
				nRecordCount = file.getRecordCount();
			}
		}
		
		abyTFContent[0] = (byte)7;
		System.arraycopy("TIFILES".getBytes(), 0, abyTFContent, 1, 7);		
		abyTFContent[8] = (byte)((nTotalSectors&0xff00)>>8);
		abyTFContent[9] = (byte)(nTotalSectors&0x00ff);

		// We remove the modified flag because older disk controllers may
		// get into trouble (in particular the TI FDC)
		abyTFContent[10] = (byte)(file.getFlags() & ~TFile.MODIFIED); 
		
		abyTFContent[11] = (byte)nRecordsPerSector;
		abyTFContent[12] = (byte)nEofOffset;
		abyTFContent[13] = (byte)file.getRecordLength();

		// this is reversed byte order
		abyTFContent[15] = (byte)((nRecordCount&0xff00)>>8);
		abyTFContent[14] = (byte)(nRecordCount&0x00ff);

		for (int i=16; i < 26; i++) abyTFContent[i] = (byte)0x20;
		byte[] abyFileName = file.getName().getBytes();
		System.arraycopy(abyFileName, 0, abyTFContent, 16, abyFileName.length);

		// MXT/resv
		abyTFContent[26] = (byte)0;
		abyTFContent[27] = (byte)0;

		for (int i=30; i < 127; i++) abyTFContent[i] = (byte)0x00;

		byte[] acreate = file.getCreationTime().getBytes();
		byte[] aupdate = file.getUpdateTime().getBytes();

		// Extended header		  
		for (int i=0; i < 4; i++) {
			if (acreate[i]!=(byte)0 || aupdate[i]!=(byte)0) {
				abyTFContent[28] = (byte)0xff;
				abyTFContent[29] = (byte)0xff;
				break;
			}
		}
		System.arraycopy(acreate, 0, abyTFContent, 30, 4);
		System.arraycopy(aupdate, 0, abyTFContent, 34, 4);
				
		// Contents		   
		System.arraycopy(abyContent, 0, abyTFContent, 128, abyContent.length);
		
		return new TIFiles(abyTFContent);
	}

	public byte[] toByteArray() {
		return m_abyTFContent;
	}
	
	public static int getTotalNumberOfSectors(byte[] abyContent) {
		return Utilities.getInt16(abyContent, 0x08);
	}

	public byte getFlags() {
		return m_abyTFContent[0x0a];
	}
	
	public int getRecordsPerSector() {
		return (int)(m_abyTFContent[0x0b]) & 0xff;
	}
	
	public int getEOFOffset() {
		return (int)(m_abyTFContent[0x0c]) & 0xff;
	}

	public int getRecordLength() {
		return (int)(m_abyTFContent[0x0d]) & 0xff;
	}

	public int getNumberOfRecords() {
		return Utilities.getInt16rev(m_abyTFContent, 0x0e); 
	}

	public String getTFIName() {
		return Utilities.getString10(m_abyTFContent, 0x10);
	}
	
	public Time getCreationTime() {
		byte[] nulltime = new byte[4];
		int ext = Utilities.getInt16(m_abyTFContent, 0x1c);
		if (ext==0xffff) return new Time(m_abyTFContent, 0x1e);
		else return new Time(nulltime); 
	}

	public Time getUpdateTime() {
		byte[] nulltime = new byte[4];
		int ext = Utilities.getInt16(m_abyTFContent, 0x1c);
		if (ext==0xffff) return new Time(m_abyTFContent, 0x22);
		else return new Time(nulltime); 
	}
	
	public static boolean hasFDRPrefix(byte[] plain) {
		try {
			// Consists of text?
			if (Utilities.checkForText(plain, 128)) return false;
			
			for (int i=0; i < 10; i++) {
				if (plain[i] < 32 || plain[i] > 127 || plain[i]=='.') {
					System.err.println(TIImageTool.langstr("InvalidFileName"));
					return false;
				}
			}
			
			if (plain.length > 254) {
				// ARC format
				if ((plain[254]=='D') &&(plain[255]=='!')) {
					return false;
				}
			}
			
			// Program files are 01, 11, 09, 19
			// 11100111
			if ((plain[12] & 0xe7)==0x01) return true;
			
			// Emulate files cannot be imported
			if ((plain[12] & 0x20)!=0x00) return false;
			
			// Reserved bits
			if ((plain[12] & 0x44)!=0x00) return false;
			
			// Data files
			if ((plain[12] & 0x01)==0x00) {
				int nSect = ((plain[14]<<8) | (plain[15]&0xff))&0xffff;
				int l3 = ((plain[19]<<8) | (plain[18]&0xff))&0xffff;
				if ((plain[12] & 0x80)!=0x00) {
					// variable
					// l3recno = nsect (swapped)
					if (l3 != nSect) {
						System.err.println("L3 error: l3="+l3 + ", sect=" + nSect);
						return false;
					}
				}
				else {
					// fixed
					if (l3 > nSect * (plain[13]&0xff)) {
						System.err.println("L3 error: l3="+l3 + ", sect*rec/sec=" + (nSect * (plain[13]&0xff)));
						return false;
					}
				}
			}
			else {
				// program files have 0000 as l3recno
				if (plain[18]!=0 || plain[19]!=0) {
					System.err.println(TIImageTool.langstr("TFIL3Error"));
					return false;
				}
			}
			return true;
		}
		catch (ArrayIndexOutOfBoundsException ax) {
			return false;
		}
	}
	
	public static void transformPrefix(byte[] plain) throws FormatException {
		// Check for validity of dir entry
		byte[] name = new byte[10];
		for (int i=0; i < 10; i++) {
			if (plain[i] < 32 || plain[i] > 127 || plain[i]=='.') 
				throw new FormatException(TIImageTool.langstr("Import"), TIImageTool.langstr("InvalidFileName"));
			name[i] = plain[i];
		}
		byte flags = plain[12];
		byte recsec = plain[13];
		byte reclen = plain[17];
		byte l3a = plain[18];
		byte l3b = plain[19];

		byte sect1 = plain[14];
		byte sect2 = plain[15];
		byte eof = plain[16];		
		
		byte[] time = new byte[8];
		System.arraycopy(plain, 0x14, time, 0, 8);
		//
		plain[0x00] = (byte)7;
		System.arraycopy("TIFILES".getBytes(), 0, plain, 1, 7);
		plain[0x08] = sect1;
		plain[0x09] = sect2;
		plain[0x0a] = flags;
		plain[0x0b] = recsec;
		plain[0x0c] = eof;
		plain[0x0d] = reclen;
		plain[0x0e] = l3a;
		plain[0x0f] = l3b;
		System.arraycopy(name, 0, plain, 0x10, 10);
		plain[0x1a] = (byte)0;
		plain[0x1b] = (byte)0;
		plain[0x1c] = (byte)0xff;
		plain[0x1d] = (byte)0xff;
		System.arraycopy(time, 0, plain, 0x1e, 8);
		for (int i=0x26; i < 0x80; i++) plain[i] = (byte)0;
	}
	
	/*
		Some notes:
		* EOF markers only appear in VAR files
		* The EOF marker appears behind the last record in each sector
		* The EOF can appear at offset 0xff (last byte)
		* Every sector must have an EOF marker 0xff! 
		* The EOF does not appear at offset 0x00.
		* For VAR files, the EOF field can never be 0
	*/
	
	public static byte[] createTfi(byte[] plain, String sName, byte flags, int reclen, int l3recno) {
		int nRoundUpLength = ((plain.length + 255) & 0xffffff00);
		byte[] newimage = new byte[nRoundUpLength+128];
		System.arraycopy(plain, 0, newimage, 128, plain.length);
		newimage[0] = (byte)0x07;
		System.arraycopy("TIFILES".getBytes(), 0, newimage, 1, 7);
		Utilities.setInt16(newimage, 8, nRoundUpLength/256);  // sector count
		newimage[10] = flags;
		
		if ((flags&1)!=0) /* prg */ newimage[11] = (byte)0;
		else newimage[11] = (byte)(256/reclen);
		int nEOF = plain.length - (nRoundUpLength-256); 
		if (nEOF==256) nEOF=0;
		if ((flags & 0x80)!=0) /* var */ nEOF--; // EOF is on last byte
		newimage[12] = (byte)nEOF;
		newimage[13] = (byte)reclen;
			
		// prg: l3 = 0
		if ((flags&1)!=0) /* prg */ l3recno = 0;
		else {
			// var: l3 = number of sectors
			if ((flags&0x80)!=0) /* var */ l3recno = nRoundUpLength/256;
			else {
				// fixed: l3 = rec/sect * number of sect
				// fixed records do not have a length byte, so we need to know the
				// number precisely, or we have to assume that the last sector is full.
				if (l3recno==0) l3recno = nRoundUpLength/256 * (256 / reclen);
				// only use that value if we don't have a given value
			}
		}
		
		// little-endian!		
		newimage[15] = (byte)((l3recno>>8)&0xff);
		newimage[14] = (byte)(l3recno&0xff);
		System.arraycopy(sName.getBytes(), 0, newimage, 16, (sName.length()>10)? 10: sName.length());
		for (int i=0; i < 10-sName.length(); i++) newimage[16+sName.length()+i]=(byte)32;
		for (int i=0x1a; i<0x80; i++) newimage[i] = (byte)0x00; 
		
		// Date and time
		Utilities.setInt16(newimage, 0x1c, 0xffff);
		Utilities.setTime(newimage, 0x1e, Time.createNow());
		
		return newimage;
	}
	
	/** Used to create a new file by writing records. */
	public void writeRecord(byte[] content) throws IOException {
		writeRecord(content, 0x00);
	}

	/** Used to create a new file by writing records. */
	public void writeRecord(byte[] content, int fill) throws IOException {
		if (content.length > 254) throw new IOException(TIImageTool.langstr("TFIRecord") + ": " + content.length);
			
		if (m_type == TFile.T_DISVAR || m_type == TFile.T_INTVAR) {
			if (m_pos + content.length + 1 >= 256) {
				// Write a "next sector" mark
				m_baos.write(0xff);
				// Fill the rest with 0x00
				m_pos++;
				while (m_pos++ < 256) m_baos.write(fill);
				// always relative to the recent sector
				m_pos = 0;
				// next sector
				m_sectors++;
			}
			// Variable records need a length byte
			m_baos.write(content.length);
			// then the contents
			m_baos.write(content);
			m_pos = m_pos + content.length + 1;
		}
		else {
			if (content.length > m_reclen) throw new IOException(TIImageTool.langstr("TFIRecord") + ": " + content.length);
			int recpersect = 256 / m_reclen;
			
			if (m_type == TFile.T_DISFIX || m_type == TFile.T_INTFIX) {
				if (m_records % recpersect == 0 && m_pos > 0) {
					// Fill the last sector
					while (m_pos++ < 256) m_baos.write(fill);
					m_pos = 0;
					// next sector
					m_sectors++;
				}
				// then the contents
				m_baos.write(content);
				m_pos = m_pos + content.length;
				// Fill up to record length
				while (m_pos % m_reclen != 0) {
					m_baos.write(fill);
					m_pos++;
				}
			}
			else
				throw new IOException(TIImageTool.langstr("TFIUnsupported"));
		}
		m_records++;
	}		

	public byte[] closeAndGetBytes(boolean prot, boolean mod) {
		// Write end
		if ((m_type == TFile.T_DISVAR || m_type == TFile.T_INTVAR) && m_pos < 256) m_baos.write(0xff);
		byte flags = TFile.fileTypeToFlags(m_type, prot, mod);
		byte[] complete = createTfi(m_baos.toByteArray(), m_name, flags, m_reclen, m_records);
		return complete;
	}
	
	public static void maketifiles(String sFile, String sName) throws FileNotFoundException, IOException {
		DataInputStream dis = new DataInputStream(new FileInputStream(sFile));
		byte[] image = new byte[dis.available()];
		dis.readFully(image);
		byte[] newimage = createTfi(image, sName, (byte)0x01, (byte)0x00, 0);		
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(sFile + ".tfi"));
		dos.write(newimage);
		dos.close();
	}
	
	public static String getName(byte[] content) {
		byte[] abyName = new byte[10];
		if (content.length < 26) return TIFiles.NOHEADER;
		System.arraycopy(content, 16, abyName, 0, 10);
		String sContName = new String(abyName, java.nio.charset.Charset.forName("ISO-8859-1"));
		if (hasHeader(content)) {
			if (abyName[0]==(byte)0xca && abyName[1]==(byte)0x53 && abyName[2]==(byte)0xca && abyName[3]==(byte)0x53) {
				// System.out.println("No name");
				sContName = "";
			}
			if (sContName.trim().length()==0) {
				sContName = TIFiles.NONAME;
			}
		}
		else {
			sContName = TIFiles.NOHEADER;
		}
		return sContName;
	}
}
