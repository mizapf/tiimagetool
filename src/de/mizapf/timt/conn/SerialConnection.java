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
import de.mizapf.timt.TIImageTool;

public class SerialConnection {

	final static byte SOH = (byte)0x01;
	final static byte STX = (byte)0x02;
	final static byte EOT = (byte)0x04;
	final static byte ACK = (byte)0x06;
	final static byte NAK = (byte)0x15;
	final static byte CAN = (byte)0x18;
	final static byte CRC = 'C';
	final static byte TIMEOUT = (byte)0xff;
	
	private final static int CONN_TIMEOUT = 20000;
		
	SerialPort m_sp = null;
	
	public static String[] getPorts() {
		CommPortIdentifier cpi = null;
		List<String> ports = new ArrayList<String>();
		
		Enumeration ePorts = CommPortIdentifier.getPortIdentifiers();
		if (!ePorts.hasMoreElements()) {
			return null;
		}
		while (ePorts.hasMoreElements()) {
			CommPortIdentifier cpit = (CommPortIdentifier)ePorts.nextElement();
			ports.add(cpit.getName());
		}
		return ports.toArray(new String[ports.size()]);
	}
	
	public void initializeConnection(String sPort, int nSpeed, int nData, int nParity, int nStop, int nTimeout) throws ConnectionException, IOException {
		try {
			CommPortIdentifier cpi = null;
			
			Enumeration ePorts = CommPortIdentifier.getPortIdentifiers();
			while (ePorts.hasMoreElements()) {
				CommPortIdentifier cpit = (CommPortIdentifier)ePorts.nextElement();
				if (cpit.getName().equals(sPort)) cpi=cpit;
			}
			
			CommPort cp = null;
			cp = cpi.open("de.mizapf.timt.conn.SerialConnection", 1000);
			SerialPort sp = (SerialPort)cp;
			
			int[] data = { SerialPort.DATABITS_7, SerialPort.DATABITS_8 };
			int[] stop = { SerialPort.STOPBITS_1, SerialPort.STOPBITS_2 };
			int[] parity = { SerialPort.PARITY_NONE, SerialPort.PARITY_ODD, SerialPort.PARITY_EVEN };
			
			sp.setSerialPortParams(nSpeed, data[nData-7], stop[nStop-1], parity[nParity]);
			sp.disableReceiveThreshold();
			sp.enableReceiveTimeout(nTimeout);
			// System.out.println("ReceiveTimeout: " + m_sp.isReceiveTimeoutEnabled());	 // CRASH!
			m_sp = sp;
		}
		catch (UnsupportedCommOperationException ucx) {
			ucx.printStackTrace();
			if (ucx.getMessage().contains(TIImageTool.langstr("SerialConnectInvParm")))
				throw new ConnectionException(sPort, TIImageTool.langstr("SerialConnectNotSettings"));
			else
				throw new ConnectionException(sPort, TIImageTool.langstr("SerialConnectUnsupp") + ": " + ucx.getMessage());
		}
		catch (PortInUseException pux) {
			throw new ConnectionException(sPort, TIImageTool.langstr("SerialBridgeInUse"));
		}
	}
	
	public InputStream getInputStream() throws IOException {
		return m_sp.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException  {
		return m_sp.getOutputStream();
	}
	
	public void close() {
		if (m_sp != null) m_sp.close();
	}
}
