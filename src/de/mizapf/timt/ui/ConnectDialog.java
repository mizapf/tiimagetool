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
import java.io.IOException;
import de.mizapf.timt.conn.SerialConnection;

public class ConnectDialog extends ToolDialog implements ActionListener {
	
	JComboBox<String>		m_jcAdapters;
	JComboBox<String>		m_jcSpeed;
	JComboBox<String>		m_jcDatabits;
	JComboBox<String>		m_jcParity;
	JComboBox<String>		m_jcStopbits;
	JComboBox<String>		m_jcProtocol;
			
	public ConnectDialog(JFrame owner) {
		super(owner, "Serial connection setup");
	}
	
/*
	| 	Serial connection setup									|

		Serial adapter			[v|/dev/ttyS0]
		Speed					[v|9600]
		Data bits				[v|8]
		Parity					[v|n]
		Stop bits				[v|1]
		Transfer protocol   	[v|XModem]   // only for sending
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	public void createGui(Font font, boolean bUpload) throws IOException {
		prepareGui();
		determineWidth("Serial Adapter");

		String[] asAdapters = SerialConnection.getPorts();
		if (asAdapters==null) throw new IOException("No serial ports found");

		m_jcAdapters = new JComboBox<String>(asAdapters);
		addLine("Serial adapter", m_jcAdapters);

		String[] asSpeed = { "57600", "38400", "19200", "9600", "4800", "2400", "1200", "300", "110" }; 		
		m_jcSpeed = new JComboBox<String>(asSpeed);
		m_jcSpeed.setSelectedIndex(2);
		addLine("Speed", m_jcSpeed);
		
		String[] asDatabits = { "8", "7" };
		m_jcDatabits = new JComboBox<String>(asDatabits);
		m_jcDatabits.setSelectedIndex(0);
		addLine("Data bits", m_jcDatabits);
		
		String[] asParity = { "None", "Odd", "Even" };
		m_jcParity = new JComboBox<String>(asParity);
		m_jcParity.setSelectedIndex(0);
		addLine("Parity", m_jcParity);

		String[] asStopbits = { "1", "2" };
		m_jcStopbits = new JComboBox<String>(asStopbits);
		m_jcStopbits.setSelectedIndex(0);
		addLine("Stop bits", m_jcStopbits);

		String[] asProtocolUpload = { "XModem", "XModem-1K" };
		String[] asProtocolDownload = { "XModem", "XModem/CRC" };
		if (bUpload)
			m_jcProtocol = new JComboBox<String>(asProtocolUpload);
		else
			m_jcProtocol = new JComboBox<String>(asProtocolDownload);

		addLine("Protocol", m_jcProtocol);
		addButtons();		
	}
	
	public String getAdapter() {
		return (String)m_jcAdapters.getSelectedItem();
	}

	public int getSpeed() {
		return Integer.parseInt((String)m_jcSpeed.getSelectedItem());
	}

	public int getDatabits() {
		return Integer.parseInt((String)m_jcDatabits.getSelectedItem());
	}

	public int getStopbits() {
		return Integer.parseInt((String)m_jcStopbits.getSelectedItem());
	}

	public int getParity() {
		return m_jcParity.getSelectedIndex();
	}

	public int getProtocol() {
		return m_jcProtocol.getSelectedIndex();
	}
}