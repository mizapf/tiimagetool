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
//import com.fazecast.jSerialComm.*;   // TIMT does not compile with JSerialComm (missing objects/methods)

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;

import java.awt.*;
import java.awt.font.*;
import javax.swing.*; 

import java.net.*;

import java.awt.event.*;

/** Creates a socket/serial bridge. This bridge is useful for use in the MESS
emulator. */

public class SerialBridge implements Runnable, SerialPortEventListener {
	
	SerialBridgeDisplay m_Display;
	
	String m_sAdapter;
	int m_nPort;
	Thread m_thrSerial;
	
	InputStream m_isFromSocket;
	InputStream m_isFromSerial;

	OutputStream m_osToSocket;
	OutputStream m_osToSerial;
	
	boolean m_bDone;

	int m_nDebugChars;
	
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
	
	public final static boolean TRACESIG = false;
	public final static boolean TRACECONF = false;
	public final static boolean TRACEIN = false;
	public final static boolean TRACEOUT = false;
	public final static boolean TRACECHAR = false;
		
	SerialPort m_sp;
	
	private synchronized void debug(String s) {
		System.out.print(s);
	}
	private synchronized void debug(char ch) {
		System.out.print(ch);
	}
	
	private synchronized void debugch(byte byIn, boolean bIn) {
		debug(bIn? ">" : "<");
		debug(Utilities.toHex(byIn,2));		
		if (TRACECHAR) {
			debug("[");
			if (byIn >= 32 && byIn < 128) debug((char)byIn);
			else debug(".");
			debug("]");
		}
		debug(" ");
		if (m_nDebugChars++>16) { m_nDebugChars = 0; debug("\n"); }
	}
	
	public SerialBridge(String sAdapter, int nPort, SerialBridgeDisplay disp) {
		m_Display = disp;
		m_nPort = nPort;
		m_sAdapter = sAdapter;
		m_bDone = false;
		m_nMode = NORMAL;
	}

	public void create() throws SocketException, IOException, ConnectionException {
				
		try {
			openSerialConnection(m_sAdapter);
			setSerialDataBits(8);
			setSerialParity(0);
			setSerialStopBits(1);
			setSerialSpeed(9600);
			adjustSerialParameters();
		}
		catch (ConnectionException cx) {
			System.err.println(TIImageTool.langstr("ConnectionError") + ": " + cx);
			return;
		}

		// Start a thread for one direction
		m_thrSerial = new Thread(this);
		m_thrSerial.start();

		if (TRACECONF) debug("Bridge established\n");
		
		// We only allow for one connection
		Socket sock = null;
		ServerSocket ssock = new ServerSocket(m_nPort);
		ssock.setSoTimeout(1000);
		while (!m_bDone) {
			try {
				sock = ssock.accept();
				m_isFromSocket = sock.getInputStream();
				m_osToSocket = sock.getOutputStream();			
				fromSocketToSerial();
			}
			catch (SocketTimeoutException stx) {
				// just ignore
			}
		}
		if (m_isFromSocket != null) {
			if (TRACECONF) debug("Closing socket\n");
			m_isFromSocket.close();
			m_osToSocket.close();
			sock.close();
		}
		ssock.close();
		closeSerialConnection();
	}

	/*
		Asynchronous method run in a thread. This part is the serial->socket
		direction
	*/
	public void run() {
		// At first determine the initial line state
		m_byLines = (byte)0;

		if (m_sp.isCD()) {
			m_byLines |= DCD;
			m_Display.setDCD(true);
		}
		if (m_sp.isCTS()) {
			m_byLines |= CTS;
			m_Display.setCTS(true);
		}
		if (m_sp.isDSR()) {
			m_byLines |= DSR;
			m_Display.setDSR(true);
		}
		if (m_sp.isRI()) {
			m_byLines |= RI;
//			m_Display.setRI(true);
		}
		send_line_state(m_byLines, false);
		
		try {
			fromSerialToSocket();
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
	}
	
	/*
		This part is the socket->serial direction. Data come from e.g. the MESS
		emulator and are forwarded to the serial interface.
	*/
	void fromSocketToSerial() throws IOException, ConnectionException {
		while (!m_bDone) {
			// Attention: you can also read FF from the socket; this is not 
			// the same as -1!
			int nSocket = m_isFromSocket.read();
			if (nSocket!=-1) processOut((byte)nSocket);
			else return;
			// We should not close the socket, or MESS will crash (SIGPIPE)
		}
	}
	
	/*
		This part is the serial->socket direction
	*/
	void fromSerialToSocket() throws IOException {
		// We assume a default serial connection of 9600,8,N,1
		// This must be changed via the socket connection.
		while (!m_bDone) {
			int nSerial = m_isFromSerial.read();
			if (nSerial != -1) processIn((byte)nSerial);
		}
//		System.out.println("done");
	}
	
	public void close() {
//		System.out.println("Bridge closed");
		m_bDone = true;
	}
		
	private void setSerialSpeed(int nSpeed) {
		// NOTE: RXTX fails for rates above 19200. Maybe there is a fix; 
		// have to check
		// if (nSpeed > 19200) nSpeed = 19200;
		m_nSpeed = nSpeed;
		m_Display.setSpeed(nSpeed);
	}

	private void setSerialDataBits(int nData) {
		int[] data = { SerialPort.DATABITS_5,  SerialPort.DATABITS_6, SerialPort.DATABITS_7, SerialPort.DATABITS_8 };
		m_nData = data[nData-5];
		m_Display.setDataBits(nData);
	}

	private void setSerialStopBits(int nStop) {
		int[] stop = { SerialPort.STOPBITS_1, SerialPort.STOPBITS_2, SerialPort.STOPBITS_1_5 };
		m_nStop = stop[nStop-1];
		m_Display.setStopBits(nStop);
	}
	
	private void setSerialParity(int nParity) {
		int[] parity = { SerialPort.PARITY_NONE, SerialPort.PARITY_ODD, SerialPort.PARITY_EVEN, SerialPort.PARITY_MARK, SerialPort.PARITY_SPACE };
		m_nParity = parity[nParity];
		m_Display.setParity(nParity);
	}
	
	private void adjustSerialParameters() throws ConnectionException {
		if (TRACECONF) debug("speed=" + m_nSpeed + ", data=" + m_nData + ", stop=" + m_nStop + ", parity=" + m_nParity + "\n");
		if (m_nSpeed > 19200) m_nSpeed = 19200;
		try {
			m_sp.setSerialPortParams(m_nSpeed, m_nData, m_nStop, m_nParity);
		}
		catch (UnsupportedCommOperationException ucx) {
			throw new ConnectionException(m_sAdapter, String.format(TIImageTool.langstr("SerialBridgeFailed"), m_nSpeed, m_nData, m_nStop, m_nParity));
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
			if (cpi==null) throw new ConnectionException(sPort, TIImageTool.langstr("SerialBridgePort"));
			
			m_sp = (SerialPort)cpi.open("de.mizapf.timt.conn.SerialBridge", 1000);
			m_isFromSerial = m_sp.getInputStream();
			m_osToSerial = m_sp.getOutputStream();	
			m_sp.disableReceiveThreshold();
			
			m_sp.addEventListener(this);
//			m_sp.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);

			m_sp.enableReceiveTimeout(1000);
			m_sp.notifyOnBreakInterrupt(true);
			m_sp.notifyOnCarrierDetect(true);
			m_sp.notifyOnFramingError(true);
			m_sp.notifyOnOverrunError(true);
			m_sp.notifyOnRingIndicator(true);
			m_sp.notifyOnParityError(true);
			m_sp.notifyOnCTS(true);
			m_sp.notifyOnDSR(true);
			
			setLineDTR(true);
			
			m_byLines = (byte)0;
			if (TRACECONF) debug("Serial connection opened\n");
		}
		catch (PortInUseException px) {
			throw new ConnectionException(sPort, TIImageTool.langstr("SerialBridgeInUse"));
		}
		catch (UnsupportedCommOperationException ucx) {
			throw new ConnectionException(m_sAdapter, TIImageTool.langstr("SerialBridgeNotSupp") + ": " + ucx.getMessage());
		}
		catch (TooManyListenersException tmx) {
			throw new ConnectionException(sPort, TIImageTool.langstr("SerialBridgeList"));
		}
	}
	
	private void closeSerialConnection() {
		m_sp.close();
	}
	
	private void setLineRTS(boolean bVal) {
		// System.out.println("Set RTS = " + bVal);
		if (TRACESIG) debug(bVal? ">RTS " : ">rts ");
		m_sp.setRTS(bVal);
		m_Display.setRTS(bVal);		
	}

	private void setLineDTR(boolean bVal) {
		if (TRACESIG) debug(bVal? ">DTR " : ">dtr ");
		// System.out.println("Set DTR = " + bVal);
		m_sp.setDTR(bVal);		
		m_Display.setDTR(bVal);
	}

	private void sendBreak(boolean bVal) {
		if (bVal) {
			m_sp.sendBreak(100);	// Library does not allow to keep BRK set, needs time	
			m_Display.setBreakOut(bVal);
		}
	}
	
	// ==========================================================
	
	/** Don't let this interfere with the normal byte reception. */
	private synchronized void send_line_state(byte byValue, boolean bException) {
		try {
			if (m_osToSocket != null) {
				m_osToSocket.write(0x1b);
				m_osToSocket.write(0x01);
				if (bException) byValue |= EXCEPT;
				m_osToSocket.write(byValue);
			}
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
	}
	
	private void setLine(byte byVal, boolean bSet) {
		if (bSet) {
			m_byLines |= byVal;
		}
		else {
			m_byLines &= ~byVal;
		}	
	}
	
	// Transmit the line state
	// | 0 | 0 | RTS | CTS | DSR | DCD | DTR | RI 
	//
	// Protocol 0x1b len bytes
	// here: len=1
	public void serialEvent(SerialPortEvent se) {
		byte byVal = (byte)0;
		int nType = se.getEventType();
		boolean bNewVal = se.getNewValue();
		switch (nType) {
		case SerialPortEvent.CD:
//			System.out.println("carrier detect=" + se.getNewValue());
			if (TRACESIG) debug(bNewVal? "<CD " : "<cd ");
			m_Display.setDCD(bNewVal);
			setLine(DCD, bNewVal);
			break;
		case SerialPortEvent.CTS:
			// System.out.println("clear to send=" + se.getNewValue());
			if (TRACESIG) debug(bNewVal? "<CTS " : "<cts ");
			m_Display.setCTS(bNewVal);
			setLine(CTS, bNewVal);
			break;
		case SerialPortEvent.DSR:
			// System.out.println("data set ready=" + se.getNewValue());
			if (TRACESIG) debug(bNewVal? "<DSR " : "<dsr ");
			m_Display.setDSR(bNewVal);
			setLine(DSR, bNewVal);
			break;
		case SerialPortEvent.RI:
			// System.out.println("ring indicator");
			if (TRACESIG) debug(bNewVal? "<RI " : "<ri ");
			m_Display.setRingIndicator(bNewVal);
			setLine(RI, bNewVal);
			break;

		case SerialPortEvent.BI:
			// System.out.println("break in=" + se.getNewValue());
			if (TRACESIG) debug(bNewVal? "<BREAK " : "<break ");
			m_Display.setBreakIn(bNewVal);
			byVal = BRK;
			if (bNewVal) byVal |= 1;
			send_line_state(byVal, true);
			return;
		case SerialPortEvent.FE:
			// System.out.println("framing error");
			if (TRACESIG) debug(bNewVal? "<FRAMEERR " : "<frameerr ");
			m_Display.setFrameError(bNewVal);
			send_line_state(FRMERR, true);
			return;
		case SerialPortEvent.PE:
			// System.out.println("parity error");
			if (TRACESIG) debug(bNewVal? "<PARERR " : "<parerr ");
			m_Display.setParityError(bNewVal);
			send_line_state(PARERR, true);
			return;
		}
		// Send the state to the other side
		send_line_state(m_byLines, false);
	}
	
	// ==========================================================
	
	/** Handels bytes going outbound (to the serial adapter). */
	private void processOut(byte byOut) throws IOException, ConnectionException {
		switch (m_nMode) {
		case NORMAL:
			if (byOut==0x1b) {
				m_nMode = ESC;
			}
			else {
				m_Display.pulseDataOut();
				m_osToSerial.write(byOut);
				if (TRACEOUT) {
					debugch(byOut, false);
				}
			}
			break;
		case ESC:
			// ESC ESC = send ESC
			if (byOut==0x1b) {
				m_Display.pulseDataOut();
				if (TRACEOUT) debug("<ESC> ");
				m_osToSerial.write(byOut);
			}
			else {
				// ESC length byte[length] (length!=0x1b)
				byte[] aby = new byte[byOut];
				for (int i=0; i < aby.length; i++)
					aby[i] = (byte)m_isFromSocket.read();
				control(aby);
			}
			m_nMode = NORMAL;
			break;
		default:
			System.err.println(TIImageTool.langstr("SerialBridgeInvMode") + ": " + m_nMode);
			m_nMode = NORMAL;
			break;
		}
	}
	
	/**
		Handels bytes going inbound (from the serial adapter).
		Don't let this interfere with send_line_state
	*/	
	private synchronized void processIn(byte byIn) throws IOException {
		m_Display.pulseDataIn();
		if (TRACEIN) {
			debugch(byIn, true);
		}
		if (m_osToSocket != null) {
			if (byIn == 0x1b) // When ESC arrives, escape it with another one
				m_osToSocket.write(byIn);
			m_osToSocket.write(byIn);
			m_osToSocket.flush();
		}
	}
	
	private boolean isConfig(byte by) {
		return ((by & CONFIG)!=0);
	}
	
	private boolean isException(byte by) {
		return ((by & EXCEPT)!=0);
	}
	
	private boolean isLineState(byte by) {
		return (((by & CONFIG)==0) && (by & EXCEPT)==0);
	}
	
	private void control(byte[] aby) throws ConnectionException {
		if (isConfig(aby[0])) {
			int nType = aby[0] & 0x07;
			if (nType != 1) {
				System.err.println(TIImageTool.langstr("SerialBridgeOnly9902"));
				return;
			}
			// Configuration		
			int nOperation = (aby[0] >> 4) & 0x07;
			double fRate=0.0;
			double fint = 3000000.0;
			double div = 0.0;
			int nRateVal = 0;
			int nData = 0;
			int nStop = 0;
			int nParity = 0;
			int nRate = 0;
			
			switch (nOperation) {
			case 7:  // Receive rate
				if ((aby[1] & 0x80)==0x80) fint = fint / 4;
				else fint = fint / 3;
				if ((aby[1] & 0x40)==0x40) div = 8.0;
				else div = 1.0;
				nRateVal = ((aby[1] & 0x3f)<<4) | ((aby[2] & 0xf0) >> 4);
				fRate = fint / (2.0*div*nRateVal);
				if (fRate < 300) nRate = 110;
				// Need to round for TELCO (sets value 0x35 which yields 9433 baud)
				else nRate = ((int)((fRate+170)/300))*300;
				setSerialSpeed(nRate);
				break;
			case 6: // Send rate
				if ((aby[1] & 0x80)==0x80) fint = fint / 4;
				else fint = fint / 3;
				if ((aby[1] & 0x40)==0x40) div = 8.0;
				else div = 1.0;
				nRateVal = ((aby[1] & 0x3f)<<4) | ((aby[2] & 0xf0) >> 4);
				fRate = fint / (2.0*div*nRateVal);
				if (fRate < 300) nRate = 110;
				// Need to round for TELCO (sets value 0x35 which yields 9433 baud)
				else nRate = ((int)((fRate+170)/300))*300;
				setSerialSpeed(nRate);
				break;
			case 5: // Data bits
				nData = (aby[1] & 0x03) + 5;
				setSerialDataBits(nData);
				break;
			case 4: // Stop bits
				nStop = aby[1] & 0x03;
				if (nStop > 1) nStop = 1;
				else {
					nStop = 3-nStop;
				}
				setSerialStopBits(nStop);
				break;
			case 3: // Parity
				nParity = aby[1] & 0x03;
				if ((nParity & 0x02)==0) nParity = 0;
				else nParity = 4-nParity;
				setSerialParity(nParity);
				break;				
			}
			adjustSerialParameters();
		}		
		else {
			if (isException(aby[0])) {
				if ((aby[0] & BRK) != 0) {
					sendBreak((aby[0] & 0x01)==0x01);
				}
			}
			else {
				// Set a line (directed at serial adapter)
				// 00ab cdef = setting line RTS=a, CTS=b, DSR=c, DCD=d, DTR=e, RI=f 
				
				// Only these lines are outbound
				setLineRTS((aby[0] & RTS)==RTS);
				setLineDTR((aby[0] & DTR)==DTR);
			}
		}
	}
}
