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
import gnu.io.*;
import de.mizapf.timt.util.Utilities;
import javax.swing.*;

import static de.mizapf.timt.conn.SerialConnection.*;

public class XModem {

	private InputStream m_is;
	private OutputStream m_os;
	private ProgressView m_view;
	
	private boolean m_bUseCrc;
	
	public final static int XMODEM = 0;
	public final static int XMODEM_UP_1K = 1;
	public final static int XMODEM_DOWN_CRC = 1;
	
	public XModem(InputStream is, OutputStream os, ProgressView view) {
		m_is = is;
		m_os = os;
		m_view = view;
	}
	
	public void send(byte[] abyTIFile, int nBlockSize) throws IOException, ProtocolException {
	
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
			by = (byte)m_is.read();

			if (m_view.stopRequested() || nRetry==0) {
				bAbort = true;
			}
			
			if (by==CRC) {
				// XModem/CRC
				// if (nBlockSize==1024) {
				//System.out.println("Starting XModem-1K");
				// }
				// else System.out.println("Starting XModem/CRC");
				bCRC = true;
			}
			else { 
				if (by==NAK) {
					// System.out.println("Starting normal XModem");
					nBlockSize = 128;
				}
				else {
					nRetry--;
				}
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
				if (nBlockSize==1024) m_os.write(STX);
				else m_os.write(SOH);
				
				if (nNumber==256) nNumber = 0;	// see XModem spec

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

				if (bCRC) {
					m_os.write((byte)((nCrc >> 8)&0xff));
					m_os.write((byte)(nCrc & 0xff));
				}
				else {
					m_os.write((byte)(nChecksum & 0xff));
				}
				m_os.flush();
				
				// Wait for ack
				by = (byte)m_is.read();
				if (by==ACK) {
					// System.out.println("Transmission of bytes " + nPosition + " to " + (nPosition+nBlockSize) + " acknowlegded");
					nNumber++;
					nPosition += nBlockSize;
					m_view.setTransferredBytes(nPosition);
					m_view.setStatus("OK");
					nRetry = 10;
				}
				else {
					if (by==NAK) {
						// System.err.println("Transmission of bytes " + nPosition + " to " + (nPosition+nBlockSize-1) + " not acknowlegded");
						m_view.setStatus("Error");
						nRetry--;
						if (nRetry==0) {
							// System.err.println("Abort transmission");
							m_view.setStatus("Abort");
							m_os.write(CAN);
							m_os.write(CAN);
							bDone = true;
						}
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
						// System.out.println("Transfer completed successfully");
						m_view.setStatus("Complete");
						break;
					}
					else {
						if (by!=NAK) {
							System.err.println("Invalid character received: " + (by&0xff));
							m_view.setStatus("Warn (P)");
						}
					}
				}
				if (nRetry==0) {
					System.err.println("Data transmitted, but connection close failed");
					m_view.setStatus("Warn (C)");
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
				// System.out.println("Timeout");
				m_view.setStatus("Timeout");
				nRetry--;
				if (nRetry==0) {
					System.err.println("Giving up");
					m_view.setStatus("Abort");
					bDone = true;
					break;
				}
				if (bStart) {
					m_view.setStatus("Linking");
					sendHandshake(bUseCrc);
				}
				else {
					// System.out.println("Sending NAK");
					m_os.write(NAK);
				}
				break;
			case STX: 
				// System.out.print("XModem block ");
				m_view.setBlockSize(1024);
				bStart = false;
				abyRecord = getRecord(1024);
				if (abyRecord==null) {
					m_os.write(NAK);
				}
				else {
					m_view.setStatus("OK");
					m_os.write(ACK);
				}
				break;
			case SOH:
				// System.out.print("XModem block ");
				m_view.setBlockSize(128);
				bStart = false;
				abyRecord = getRecord(128);
				if (abyRecord==null) {
					m_os.write(NAK);
				}
				else {
					m_view.setStatus("OK");
					m_os.write(ACK);
				}
				break;
			case EOT: 
				// System.out.println("Got EOT");
				m_view.setStatus("Complete");
				m_os.write(ACK);
				bDone = true;
				break;
			case ACK:
				System.err.println("Got ACK - but unexpected");
				break;
			case CAN:
				System.err.println("Sender sent CANcel signal");
				break;
			default:
				System.err.println("Got unknown value: " + by);
				m_view.setStatus("Warn (P)");
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
			// System.out.println("Starting XModem/CRC or 1K");
		}
		else {
			m_os.write(NAK);
			m_bUseCrc = false;
			// System.out.println("Starting plain XModem");
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
		
		// System.out.println(nNumber + ", " + nBlockSize + " bytes, crc = " + m_bUseCrc);
		
		if (nNumber + nCheckNumber != 255) {
			System.err.println("Data error: Record number failed check");
			m_view.setStatus("Invalid record");
			bError = true;
		}
		else {
			// System.out.println("Record number OK");
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
					System.err.println("CRC error: received " + Integer.toHexString(nCrcSender & 0xffff) + " but expected " + Integer.toHexString(nCrc & 0xffff));
					m_view.setStatus("CRC error");
					bError = true;
				}
			}
			else {
				nChecksum = (short)(nChecksum & 0xff);
				short nChecksumSender = (short)(m_is.read() & 0xff);
				if (nChecksumSender != nChecksum) {
					System.err.println("Checksum error: received " + nChecksumSender + " but expected " + nChecksum);
					m_view.setStatus("Checksum error");
					bError = true;
				} 
			}
		}
		if (bError) {
			return null;
		}

		// System.out.println(Utilities.hexdump(0, 0, abyRecord, nBlockSize, false));
		return abyRecord;
	}
}
