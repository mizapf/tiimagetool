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
import java.util.Enumeration;
import java.util.TooManyListenersException;
import gnu.io.*;

import de.mizapf.timt.util.Utilities;

import java.awt.*;
import java.awt.font.*;
import javax.swing.*; 

import java.awt.event.*;

/** Creates a socket/serial bridge. This bridge is useful for use in the MESS
emulator. */

public class SerialTest implements Runnable, SerialPortEventListener {
		
	String m_sAdapter;
	int m_nPort;
	Thread m_thrSerial;
	
	InputStream m_isFromSocket;
	InputStream m_isFromSerial;

	OutputStream m_osToSocket;
	OutputStream m_osToSerial;
	
	boolean m_bDone;

	// ==============
	private int m_nMode;
	final static int NORMAL = 1;
	final static int ESC = 2;
	
	// ============== Serial parameters
	private int m_nSpeed;
	private int m_nData;
	private int m_nStop;
	private int m_nParity;
	
	// ============== Lines
	private byte m_byLines;
	private final static byte RTS = (byte)0x20;
	private final static byte CTS = (byte)0x10;
	private final static byte DSR = (byte)0x08;
	private final static byte DCD = (byte)0x04;
	private final static byte DTR = (byte)0x02;
	private final static byte RI  = (byte)0x01;

	private final static byte BRK = (byte)0x02;
	private final static byte FRMERR = (byte)0x04;
	private final static byte PARERR = (byte)0x06;
	
	private final static byte CONFIG = (byte)0x80;
	private final static byte EXCEPT = (byte)0x40;
	private final static byte LINES = (byte)0x00;
	
	SerialPort m_sp;
	
	public static void main(String[] arg) {
		SerialTest st = new SerialTest(arg[0]);
		try {
			st.create(arg.length>1);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public SerialTest(String sAdapter) {
		m_sAdapter = sAdapter;
		m_bDone = false;
		m_nMode = NORMAL;
	}

	public void create(boolean bNoSend) throws IOException, ConnectionException {
				
		try {
			openSerialConnection(m_sAdapter);
			setSerialDataBits(8);
			setSerialParity(0);
			setSerialStopBits(1);
			setSerialSpeed(1200);
			adjustSerialParameters();
		}
		catch (ConnectionException cx) {
			System.err.println("Connection exception: " + cx);
			return;
		}

		// Start a thread for one direction
		m_thrSerial = new Thread(this);
		m_thrSerial.start();

		if (!bNoSend) {
			sendSerial();
		}
	}

	/*
		Asynchronous method run in a thread. This part is the serial->socket
		direction
	*/
	public void run() {
		// At first determine the initial line state
		System.out.println("Detected DCD=" + m_sp.isCD() + " on start");
		System.out.println("Detected CTS=" + m_sp.isCTS() + " on start");
		System.out.println("Detected DSR=" + m_sp.isDSR() + " on start");
		System.out.println("Detected RI=" + m_sp.isRI() + " on start");
		try {
			receiveSerial();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		close();
	}
	
	void sendSerial() throws IOException, ConnectionException {
		byte byOut = (byte)0;
		for (int i=0; i < 10240; i++) {
			try { Thread.sleep(1000); } catch (InterruptedException irx) {}
			m_osToSerial.write(byOut);
			if (m_sp.isCTS())
				System.out.print("<" + Integer.toHexString(byOut & 0xff) + "> ");
			else
				System.out.print("{" + Integer.toHexString(byOut & 0xff) + "} ");
			byOut = (byte)((byOut+1) & 0xff);
		}
		closeSerialConnection();
	}
	
	/*
		This part is the serial->socket direction
	*/
	void receiveSerial() throws IOException {
		setLineDTR(true);
		// We assume a default serial connection of 9600,8,N,1
		// This must be changed via the socket connection.
		while (!m_bDone) {
			int nSerial = m_isFromSerial.read();
			System.out.print("[" + Integer.toHexString(nSerial & 0xff) + "] ");
		}
	}
	
	public void close() {
		m_bDone = true;
	}
		
	private void setSerialSpeed(int nSpeed) {
		m_nSpeed = nSpeed;
	}

	private void setSerialDataBits(int nData) {
		int[] data = { SerialPort.DATABITS_5,  SerialPort.DATABITS_6, SerialPort.DATABITS_7, SerialPort.DATABITS_8 };
		m_nData = data[nData-5];
	}

	private void setSerialStopBits(int nStop) {
		int[] stop = { SerialPort.STOPBITS_1, SerialPort.STOPBITS_2, SerialPort.STOPBITS_1_5 };
		m_nStop = stop[nStop-1];
	}
	
	private void setSerialParity(int nParity) {
		int[] parity = { SerialPort.PARITY_NONE, SerialPort.PARITY_ODD, SerialPort.PARITY_EVEN, SerialPort.PARITY_MARK, SerialPort.PARITY_SPACE };
		m_nParity = parity[nParity];
	}
	
	private void adjustSerialParameters() throws ConnectionException {
		try {
			m_sp.setSerialPortParams(m_nSpeed, m_nData, m_nStop, m_nParity);
		}
		catch (UnsupportedCommOperationException ucx) {
			throw new ConnectionException(m_sAdapter, "Serial port operation not supported: " + ucx.getMessage());
		}
	}
	
	private void openSerialConnection(String sPort) throws ConnectionException, IOException {
		try {
			CommPortIdentifier cpi = null;
			
			Enumeration ePorts = CommPortIdentifier.getPortIdentifiers();
			while (ePorts.hasMoreElements()) {
				CommPortIdentifier cpit = (CommPortIdentifier)ePorts.nextElement();
				if (cpit.getName().equals(sPort)) cpi=cpit;
			}
			if (cpi==null) throw new ConnectionException(sPort, "Port not found");
			
			m_sp = (SerialPort)cpi.open("de.mizapf.timt.conn.SerialTest", 1000);
			m_isFromSerial = m_sp.getInputStream();
			m_osToSerial = m_sp.getOutputStream();	
			m_sp.disableReceiveThreshold();
			
//			m_sp.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
			
			m_sp.addEventListener(this);

			m_sp.enableReceiveTimeout(1000);
			m_sp.notifyOnBreakInterrupt(true);
			m_sp.notifyOnCarrierDetect(true);
			m_sp.notifyOnFramingError(true);
			m_sp.notifyOnOverrunError(true);
			m_sp.notifyOnRingIndicator(true);
			m_sp.notifyOnParityError(true);
			m_sp.notifyOnCTS(true);
			m_sp.notifyOnDSR(true);
			
			m_byLines = (byte)0;
		}
		catch (PortInUseException px) {
			throw new ConnectionException(sPort, "Port in use");
		}
		catch (UnsupportedCommOperationException ucx) {
			throw new ConnectionException(m_sAdapter, "Serial port operation not supported: " + ucx.getMessage());
		}
		catch (TooManyListenersException tmx) {
			throw new ConnectionException(sPort, "Too many listeners");
		}
	}
	
	private void closeSerialConnection() {
		m_sp.close();
	}
	
	private void setLineRTS(boolean bVal) {
		m_sp.setRTS(bVal);
		System.out.println("Setting RTS=" + bVal);
	}

	private void setLineDTR(boolean bVal) {
		m_sp.setDTR(bVal);		
		System.out.println("Setting DTR=" + bVal);
	}

	private void sendBreak(boolean bVal) {
		if (bVal) {
			m_sp.sendBreak(100);	// Library does not allow to keep BRK set, needs time	
			System.out.println("Sending BRK");
		}
	}
	
	// ==========================================================
	
	// Protocol 0x1b len bytes
	// here: len=1
	public void serialEvent(SerialPortEvent se) {
		byte byVal = (byte)0;
		int nType = se.getEventType();
		switch (nType) {
		case SerialPortEvent.CD:
//			System.out.println("carrier detect=" + se.getNewValue());
			break;
		case SerialPortEvent.CTS:
//			System.out.println("clear to send=" + se.getNewValue());
			break;
		case SerialPortEvent.DSR:
			System.out.println("data set ready=" + se.getNewValue());
			break;
		case SerialPortEvent.RI:
			System.out.println("ring indicator");
			break;

		case SerialPortEvent.BI:
			System.out.println("break in=" + se.getNewValue());
			return;
		case SerialPortEvent.FE:
			System.out.println("framing error");
			return;
		case SerialPortEvent.PE:
			System.out.println("parity error");
			return;
		}
	}	
}
