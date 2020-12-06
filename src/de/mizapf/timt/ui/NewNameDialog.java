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

import de.mizapf.timt.TIImageTool;

class NewNameDialog extends ToolDialog {
	
	String 		m_sSuggested;
	boolean		m_bFile;
	int			m_nState;
	String		m_sName;
	JTextField	m_tfFileName;
	JButton		m_btnSkip;
	
	static final int OK = 0;
	static final int SKIP = 1;
	static final int ABORT = 2;
	
	NewNameDialog(JFrame owner, boolean bFile, String sName) {
		super(owner, TIImageTool.langstr("NewNameTitle"));
		m_bFile = bFile;
		m_sName = sName;
	}
	
/*
	| 	New name									            |
		
		The target directory already contains a file/directory
		named $NAME
		
		Use this file name    [FILENAME]

			+-------+	+-----------+	 +-----------+
			|	OK	|	|	Skip	|    |	 Abort 	 |
			+-------+   +-----------+    +-----------+
*/	
	void createGui() {
		prepareGui();

		m_nColumnWidth = determineFieldWidth(TIImageTool.langstr("NewNameColumn"));

		StringBuilder sb = new StringBuilder();
		sb.append(String.format(TIImageTool.langstr(m_bFile? "NewNameContainsFile" : "NewNameContainsDir"), m_sName));
		sb.append(".");
		addLine(sb.toString());
		add(Box.createVerticalStrut(10));
		
		m_tfFileName = new JTextField();
		m_tfFileName.setFont(TIImageTool.dialogFont);
		m_tfFileName.addActionListener(this);
		
		String newName = m_sName;
		if (newName.length()==10) {
			char last = newName.charAt(9);
			if (last < 126) last++;
			newName = newName.substring(0, 9) + String.valueOf(last);
		}
		else newName = newName + "1";
		m_tfFileName.setText(newName);
		addLine(TIImageTool.langstr("NewNameUseThis"), m_tfFileName);
				
		add(Box.createVerticalStrut(10));
		add(Box.createVerticalGlue());
		addButtons();
	}
	
	String getFileName() {
		return m_tfFileName.getText().trim();
	}
	
	protected void addButtons() {
		add(Box.createVerticalStrut(10));		
		Box box7 = new Box(BoxLayout.X_AXIS);		
		m_btnOK = new JButton(TIImageTool.langstr("OK"));
		m_btnOK.addActionListener(this);
		m_btnSkip = new JButton(TIImageTool.langstr("NewNameSkip"));
		m_btnSkip.addActionListener(this);
		m_btnCancel = new JButton(TIImageTool.langstr("Abort"));
		m_btnCancel.addActionListener(this);
		m_btnOK.setMinimumSize(new Dimension(100, 25));
		m_btnSkip.setMinimumSize(new Dimension(100, 25));
		m_btnCancel.setMinimumSize(new Dimension(100, 25));
		box7.add(Box.createHorizontalGlue());		
		box7.add(Box.createHorizontalStrut(10));		
		box7.add(m_btnOK);
		box7.add(Box.createHorizontalStrut(10));		
		box7.add(m_btnSkip);
		box7.add(Box.createHorizontalStrut(10));		
		box7.add(m_btnCancel);
		box7.add(Box.createHorizontalStrut(10));		
		box7.add(Box.createHorizontalGlue());		
		add(box7);		
		add(Box.createVerticalStrut(10));		
		
		pack();
		setLocationRelativeTo(getParent());
	}
		
	public boolean ok() {
		return (m_nState==OK);
	}
	public boolean skipped() {
		return (m_nState==SKIP);
	}
	public boolean aborted() {
		return (m_nState==ABORT);
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==m_btnOK || ae.getSource() == m_tfFileName) {
			m_nState = OK;
			dispose();
		}
		if (ae.getSource()==m_btnSkip) {
			m_nState = SKIP;
			dispose();
		}
		if (ae.getSource()==m_btnCancel) {
			m_nState = ABORT;
			dispose();
		}
	}
}
