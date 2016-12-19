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

import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*; 
import de.mizapf.timt.util.*;
import de.mizapf.timt.conn.SerialConnection;
import de.mizapf.timt.TIImageTool;

/** Creates a socket/serial bridge. This bridge is useful for use in the MESS
emulator. */

/*

	| 	Serial bridge configuration					|

		Serial adapter			[v|/dev/ttyS0]
		Port					[xxxxx]			
		
			+-----------+			+-----------+
			|	Start	|			|	Cancel	|
			+-----------+           +-----------+

*/

public class SBConfigDialog extends ToolDialog implements ActionListener  {

	JComboBox<String>		m_jcAdapters;
	JTextField 		m_tfPort;
	
	public SBConfigDialog(JFrame owner) {
		super(owner, TIImageTool.langstr("SBConfigTitle"));
	}
	
	public void createGui(Font font) throws IOException {
		prepareGui();
		
		determineWidth(TIImageTool.langstr("SBConfigColumn"));
		
		String[] asAdapters = SerialConnection.getPorts(); /* { "/dev/tty0", "/dev/tty1" }; */
		if (asAdapters==null) throw new IOException(TIImageTool.langstr("NoSerialPorts"));

		m_jcAdapters = new JComboBox<String>(asAdapters);
		addLine(TIImageTool.langstr("SerialAdapter"), m_jcAdapters);

		m_tfPort = new JTextField();
		m_tfPort.setText("10000");
		addLine(TIImageTool.langstr("SBConfigPort"), m_tfPort);
				
		addButtons();
	}
	
	public String getAdapter() {
		return (String)m_jcAdapters.getSelectedItem();
	}

	public int getPort() {
		return Integer.parseInt(m_tfPort.getText());
	}
}
