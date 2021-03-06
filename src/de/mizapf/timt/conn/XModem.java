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

package de.mizapf.timt.conn;

import java.io.*;
import java.util.*;
import de.mizapf.timt.util.Utilities;
import javax.swing.*;
import de.mizapf.timt.TIImageTool;

import static de.mizapf.timt.conn.SerialConnection.*;

public class XModem {

	private InputStream m_is;
	private OutputStream m_os;
	private ProgressView m_view;
	
	private boolean m_bUseCrc;
	
	public final static int XMODEM = 0;
	public final static int XMODEM_UP_1K = 1;
	public final static int XMODEM_DOWN_CRC = 1;
	
	public final static boolean TRACE = true;
	
	private synchronized void debug(String s) {
		System.out.println(s);
	}

	public XModem(InputStream is, OutputStream os, ProgressView view) {
		m_is = is;
		m_os = os;
		m_view = view;
	}
	
	public void send(byte[] abyTIFile, int nBlockSize) throws IOException {
	
		if (m_view != null) m_view.setVisible(true);
		// length multiple of 1024 (padded with 0x1a)
		
		boolean bDone = false; 
		
		int nNumber = 1; // see XModem spec
		int nPosition = 0;
		
		int nRetry = 10;  
		
		boolean bCRC = false;

		// Wait for handshake from receiver
		// CRC or NAK
		byte by = -1;
		
		boolean bAbort = false;
		
		while (!bAbort && by == -1) {
			if (TRACE) debug("Trying to read from receiver");
			by = (byte)m_is.read();

			if (m_view.stopRequested() || nRetry==0) {
				bAbort = true;
			}
			
			if (by==CRC) {  // 'C'
				// XModem/CRC
				if (TRACE) {
					if (nBlockSize==1024) {
						debug("Starting XModem-1K");
					}
					else debug("Starting XModem/CRC");
				}
				bCRC = true;
			}
			else { 
				if (by==NAK) {
					if (TRACE) debug("Starting normal XModem");
					nBlockSize = 128;
				}
				else {
					nRetry--;
					by = -1;
				}
			}
		}
		
		// Consume more bytes from the receiver if available
		// maybe from previous timeouts
		by = 0;
		
		if (TRACE) debug("Still " + m_is.available() + " bytes in the input queue");
		if (m_is.available() > 0) {
			while (by != -1) {
				if (TRACE) debug("Consume byte " + by + " from receiver before sending");
				by = (byte)m_is.read();
			}
		}

		nRetry = 10;
		if (bAbort) {
			m_os.write(CAN);
			return;
		}
		
		bDone = false;
		m_view.setUseCRC16(bCRC);
		m_view.setBlockSize(nBlockSize);

		while (!bDone) {
			if (m_view.stopRequested()) {
				bDone = true;
				m_os.write(CAN);
				continue;
			}

			if (nPosition < abyTIFile.length) {
				// create packet
				if (TRACE) debug("Write start");
				if (nBlockSize==1024) m_os.write(STX);
				else m_os.write(SOH);
				
				if (nNumber==256) nNumber = 0;	// see XModem spec

				if (TRACE) debug("Write number: " + nNumber);
				m_os.write((byte)nNumber);
				m_os.write((byte)(255-nNumber));

				short nCrc = init_crc16();
				short nChecksum = 0;
				
				// Send packet
				for (int i=0; i < nBlockSize; i++) {
					byte by1;
					if (nPosition+i < abyTIFile.length) {
						by1 = abyTIFile[nPosition+i];
					}
					else {
						by1 = (byte)0x1a;
					}
					m_os.write(by1);
					if (bCRC) {
						nCrc = update_crc16(nCrc, by1);
					}
					else {
						nChecksum += by1;
					}
				}

				if (TRACE) debug("Write CRC/Checksum");
				if (bCRC) {
					m_os.write((byte)((nCrc >> 8)&0xff));
					m_os.write((byte)(nCrc & 0xff));
				}
				else {
					m_os.write((byte)(nChecksum & 0xff));
				}
				m_os.flush();
				
				// Wait for ack
				if (TRACE) debug("Wait for Acknowledge");
				by = (byte)m_is.read();
				if (by==ACK) {
					if (TRACE) debug("Transmission of bytes " + nPosition + " to " + (nPosition+nBlockSize) + " acknowledged");
					nNumber++;
					nPosition += nBlockSize;
					m_view.setTransferredBytes(nPosition);
					m_view.setStatus(TIImageTool.langstr("OK"));
					nRetry = 10;
				}
				else {
					if (by==NAK) {
						if (TRACE) debug("Transmission of bytes " + nPosition + " to " + (nPosition+nBlockSize-1) + " not acknowledged");
						m_view.setStatus(TIImageTool.langstr("Error"));
						nRetry--;
						if (nRetry==0) {
							if (TRACE) debug("Abort transmission");
							m_view.setStatus(TIImageTool.langstr("Abort"));
							m_os.write(CAN);
							m_os.write(CAN);
							bDone = true;
						}
					}
					else {
						if (TRACE) debug("Unknown value received: " + by);
					}
				}
			}
			else {
				// Fertig
				bDone = true;
				nRetry = 10;
				while (nRetry>0) {
					m_os.write(EOT);
					m_os.flush();

					by = (byte)m_is.read();
					if (by==ACK) {
						// ACK
						if (TRACE) debug("Transfer completed successfully");
						m_view.setStatus(TIImageTool.langstr("XModemComplete"));
						break;
					}
					else {
						if (by!=NAK) {
							System.err.println(TIImageTool.langstr("XModemInvalid") + ": " + (by&0xff));
							m_view.setStatus(TIImageTool.langstr("Warning") + " (P)");
						}
					}
				}
				if (nRetry==0) {
					System.err.println(TIImageTool.langstr("XModemCloseFailed"));
					m_view.setStatus(TIImageTool.langstr("Warning") + " (C)");
				}
			}
		}
	}
		
	/* Differs from crc16_get by rem=0 */
	private short init_crc16() {
		// Big-endian, x^16+x^12+x^5+1 = (1) 0001 0000 0010 0001 = 0x1021
		short rem = 0;
		return rem;
	}

	/* Differs from crc16_get by rem=0 */
	private short update_crc16(short rem, byte by) {
		// A popular variant complements rem here
		rem = (short)(rem ^ (by << 8)); 
		for (int j=0; j < 8; j++)  {	// Assuming 8 bits per byte
			if ((rem & 0x8000) == 0x8000) {
				rem = (short)((rem << 1) ^ 0x1021);
			} 
			else {
				rem = (short)(rem << 1);
			}
		}
		// A popular variant complements rem here
		return (short)(rem & 0xffff);
	}
	
	// ---------------- Receiver part ------------------------
	
	public byte[] receive(boolean bUseCrc) throws IOException {
		// Read file into buffer, length multiple of 1024 (padded with 0x1a)
				
		if (m_view != null) m_view.setVisible(true);
		boolean bDone = false; 
		int nBlockSize = 1024;
		
		int nNumber = 1; // see XModem spec
		int nPosition = 0;
		byte by;
		byte[] abyRecord;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		int nRetry = 10;  
		
		boolean bCRC = false;
				
		// wait for handshake
		// CRC = use CRC/1024
		// NAK = use plain XModem/128
		m_view.setUseCRC16(bUseCrc);
		sendHandshake(bUseCrc);
		
		boolean bError = false;
		boolean bStart = true;
		
		while (!bDone) {
			bError = false;
			abyRecord = null;
			if (m_view.stopRequested()) {
				bDone = true;
				continue;
			}
			
			by = (byte)m_is.read();
			
			switch (by) {
			case TIMEOUT:
				if (TRACE) debug("Timeout");
				m_view.setStatus(TIImageTool.langstr("XModemTimeout"));
				nRetry--;
				if (nRetry==0) {
					System.err.println(TIImageTool.langstr("XModemGiveup"));
					m_view.setStatus(TIImageTool.langstr("Abort"));
					bDone = true;
					break;
				}
				if (bStart) {
					m_view.setStatus(TIImageTool.langstr("XModemLinking"));
					sendHandshake(bUseCrc);
				}
				else {
					if (TRACE) debug("Sending NAK");
					m_os.write(NAK);
				}
				break;
			case STX: 
				if (TRACE) debug("XModem block ");
				m_view.setBlockSize(1024);
				bStart = false;
				abyRecord = getRecord(1024);
				if (abyRecord==null) {
					m_os.write(NAK);
				}
				else {
					m_view.setStatus(TIImageTool.langstr("OK"));
					m_os.write(ACK);
				}
				break;
			case SOH:
				if (TRACE) debug("XModem block ");
				m_view.setBlockSize(128);
				bStart = false;
				abyRecord = getRecord(128);
				if (abyRecord==null) {
					m_os.write(NAK);
				}
				else {
					m_view.setStatus(TIImageTool.langstr("OK"));
					m_os.write(ACK);
				}
				break;
			case EOT: 
				if (TRACE) debug("Got EOT");
				m_view.setStatus(TIImageTool.langstr("XModemComplete"));
				m_os.write(ACK);
				bDone = true;
				break;
			case ACK:
				System.err.println(TIImageTool.langstr("XModemUnexpAck"));
				break;
			case CAN:
				System.err.println(TIImageTool.langstr("XModemCancel"));
				break;
			default:
				System.err.println(TIImageTool.langstr("XModemUnknown") + ": " + by);
				m_view.setStatus(TIImageTool.langstr("Warning") + " (P)");
				bError = true;
			}
			
			if (abyRecord != null) {
				baos.write(abyRecord);
				m_view.setTransferredBytes(baos.size());
			}
			abyRecord=null;
			m_os.flush();
		}
		baos.close();
		return baos.toByteArray();
	}
	
	private void sendHandshake(boolean bUseCrc) throws IOException {
		if (bUseCrc) {
			m_os.write(CRC);
			m_bUseCrc = true;
			if (TRACE) debug("Starting XModem/CRC or 1K");
		}
		else {
			m_os.write(NAK);
			m_bUseCrc = false;
			if (TRACE) debug("Starting plain XModem");
		}
		m_os.flush();
	}
	
	private byte[] getRecord(int nBlockSize) throws IOException {

		// STRANGE: TELCO seems to send CRCs even though it says checksum.
		// MORE: TELCO sends an ACK as sender at the beginning (should we also?)
		//
		// m_bUseCrc = true;
		boolean bError = false;
		int nNumber;
		nNumber = m_is.read();
		int nCheckNumber = m_is.read() & 0xff;
		byte[] abyRecord = new byte[nBlockSize];
		
		if (TRACE) debug(nNumber + ", " + nBlockSize + " bytes, crc = " + m_bUseCrc);
		
		if (nNumber + nCheckNumber != 255) {
			System.err.println(TIImageTool.langstr("XModemDataError"));
			m_view.setStatus(TIImageTool.langstr("XModemInvRecord"));
			bError = true;
		}
		else {
			if (TRACE) debug("Record number OK");
			short nCrc = init_crc16();
			short nChecksum = 0;
			for (int i=0; i < nBlockSize; i++) {
				abyRecord[i] = (byte)m_is.read();
				if (m_bUseCrc) {
					nCrc = update_crc16(nCrc, abyRecord[i]); 
				}
				else {
					nChecksum += abyRecord[i];
				}
			}
			if (m_bUseCrc) {
				short nCrcSender = (short)((m_is.read()<<8) & 0xff00);
				nCrcSender |= (m_is.read()&0xff);
				if (nCrcSender != nCrc) {
					System.err.println(String.format(TIImageTool.langstr("XModemCRCError"), Integer.toHexString(nCrcSender & 0xffff), Integer.toHexString(nCrc & 0xffff)));
					m_view.setStatus(TIImageTool.langstr("XModemCRC"));
					bError = true;
				}
			}
			else {
				nChecksum = (short)(nChecksum & 0xff);
				short nChecksumSender = (short)(m_is.read() & 0xff);
				if (nChecksumSender != nChecksum) {
					System.err.println(String.format(TIImageTool.langstr("XModemChecksumError"), nChecksumSender, nChecksum));
					m_view.setStatus(TIImageTool.langstr("XModemChecksum"));
					bError = true;
				} 
			}
		}
		if (bError) {
			return null;
		}

		if (TRACE) debug(Utilities.hexdump(0, 0, abyRecord, nBlockSize, false));
		return abyRecord;
	}
}
