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
    
    Copyright 2016 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import de.mizapf.timt.TIImageTool;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;

class ReadWriteCFDialog extends ToolDialog {

	TIImageTool imagetool;
	JFrame m_parent;
	boolean m_windows;
	JTextField m_tfDevice;
	JButton m_btnFileChooser;
		
	ReadWriteCFDialog(JFrame owner, TIImageTool timt, boolean windows) {
		super(owner, "Search files");
		imagetool = timt;
		m_parent = owner;
		m_windows = windows;
	}	
	
	public void createGui(Font font) {
/*
	| 	Read / Write CF card								|

		Provide the CF path. For Windows this is something like "e:", 
		for Linux or Mac this looks like "/dev/sdc"

		[1] CF device   _________   [btn]
		
		[2] File on file system    [.....] [btn]
		
		Path to dd program  ___/usr/bin/dd_____
		
		Command line ___dd.exe if=\\.\[1] of=[2] bs=4096 _____
		Command line ___kdesu dd if=[1] of=[2] bs=4096 _____
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
		prepareGui();
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth("Flash card device path (like \"/dev/sdc\")");

		// ======================
		ImageIcon diskicon = null;
		java.net.URL iconurl = ToolDialog.class.getResource(DISKSICON);
		if (iconurl != null) {
			diskicon = new ImageIcon(iconurl);
			m_btnFileChooser = new JButton(diskicon);
		} 
		else {
			System.err.println("Error: Could not locate icon image in package " + iconurl);
			m_btnFileChooser = new JButton("Choose");
		}
		
		m_btnFileChooser.addActionListener(this);
		
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(10));
		JLabel jl = null;
		if (m_windows) {
			jl = new JLabel("Flash card drive name (like \"e:\")", SwingConstants.LEFT);
		}
		else {
			jl = new JLabel("Flash card device path (like \"/dev/sdc\")", SwingConstants.LEFT);
		}
		jl.setFont(TIImageTool.dialogFont);
		String lastPath = "click to select";
		add(Box.createVerticalStrut(20));

		// Path setup
		// Prompt
		jl.setMinimumSize(new Dimension(nColumnWidth, 25));
		if (nColumnWidth!=0) {
			jl.setPreferredSize(new Dimension(nColumnWidth, 25));
			jl.setMaximumSize(new Dimension(nColumnWidth, 25));
		}
		box.add(jl);
		box.add(Box.createHorizontalStrut(10));
		
		// Button
		m_btnFileChooser.setMinimumSize(new Dimension(35, 32));
		m_btnFileChooser.setPreferredSize(new Dimension(35, 32));
		m_btnFileChooser.setMaximumSize(new Dimension(35, 32));
		box.add(m_btnFileChooser);
		box.add(Box.createHorizontalStrut(10));
		
		// Selected path
		int nPathWidth = fm.stringWidth(lastPath);
		m_tfDevice = new JTextField(lastPath);
		m_tfDevice.setEditable(false);
		m_tfDevice.setFont(TIImageTool.dialogFont);
		m_tfDevice.setMinimumSize(new Dimension(nPathWidth, 20));
		m_tfDevice.setMaximumSize(new Dimension(1000, 20));

		box.add(m_tfDevice);
		box.add(Box.createHorizontalStrut(10));
		
		add(box);
		
		// ======================
		
		add(Box.createVerticalGlue());

		addButtons();		
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==m_btnOK) {
			m_bSet = true;
			dispose();
		}
		if (ae.getSource()==m_btnCancel) {
			m_bSet = false;
			dispose();
		}
		if (ae.getSource()==m_btnFileChooser) {
			JFileChooser jfc = null;
			jfc = new JFileChooser();
			if (m_windows) jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			else jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			
			Dimension dim = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			
			int nReturn = jfc.showOpenDialog(m_parent);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				File file = jfc.getSelectedFile();
				m_tfDevice.setText(file.getAbsolutePath());
			}
		}
	}
}
