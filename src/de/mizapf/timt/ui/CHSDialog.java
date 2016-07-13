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
import java.awt.font.*;

class CHSDialog extends ToolDialog implements ActionListener {
	
	JTextField		m_tfCylinders;
	JTextField	 	m_tfHeads; // 1-16
	JTextField 		m_tfSectorsPerTrack; // 0-255
	JTextField 		m_tfSectorLength; // 256
	JTextField 		m_tfStepSpeed; // 0-255
	JTextField 		m_tfReducedCurrent; // 0-255
	JCheckBox	 	m_chbBufferedStep; // true/false
	JTextField		m_tfPrecomp; // 0-127
		
	CHSDialog(JFrame owner) {
		super(owner, "Define geometry");
	}
	
/*
	| 	Define geometry										|

		Cylinders		        [1984]
		Heads					[16_]
		Sectors per track		[32_]
		Sector length			[256]

		Step speed				[1__]
		Reduced write current	[58_]
		Write precompensation   [29_]
		Buffered step			[_]
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	void createGui(Font font) {
		m_bSet = false;
		add(Box.createVerticalStrut(10));			
		
		int nHeight = getHeight(font, "W");
		
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth("Write precompensationXXX");
		int nColumnWidth1 = fm.stringWidth("XXXXX");
		
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		m_tfCylinders = putTextField(this, "Cylinders", "-", nColumnWidth, nColumnWidth1);
		m_tfHeads = putTextField(this, "Heads", "16", nColumnWidth, nColumnWidth1); 
		m_tfSectorsPerTrack = putTextField(this, "Sectors per track", "32", nColumnWidth, nColumnWidth1);
		m_tfSectorLength = putTextField(this, "Sector length", "256", nColumnWidth, nColumnWidth1);
		add(Box.createVerticalStrut(10));		

		m_tfReducedCurrent = putTextField(this, "Reduced write current", "58", nColumnWidth, nColumnWidth1);
		m_tfPrecomp = putTextField(this, "Write precompensation", "29", nColumnWidth, nColumnWidth1);
		m_tfStepSpeed = putTextField(this, "Step speed", "1", nColumnWidth, nColumnWidth1);
		m_chbBufferedStep = putCheckBox(this, "Buffered step", false, nColumnWidth);

		add(Box.createVerticalStrut(10));		
		add(Box.createVerticalGlue());

		addButtons();
	}
	
	void setParameters(int nCylinders, int nHeads, int nSectorsPerTrack, int nSectorLength, int nReduced, int nPrecomp, int nStep, boolean bBuffered) {
		m_tfCylinders.setText(String.valueOf(nCylinders));
		m_tfHeads.setText(String.valueOf(nHeads));
		m_tfSectorsPerTrack.setText(String.valueOf(nSectorsPerTrack));
		m_tfSectorLength.setText(String.valueOf(nSectorLength));
		m_tfStepSpeed.setText(String.valueOf(nStep));
		m_tfReducedCurrent.setText(String.valueOf(nReduced));
		m_tfPrecomp.setText(String.valueOf(nPrecomp));
		m_chbBufferedStep.setSelected(bBuffered);
	}
	
	int getCylinders() throws NumberFormatException {
		return Integer.parseInt(m_tfCylinders.getText());		
	}

	int getHeads() throws NumberFormatException {
		return Integer.parseInt(m_tfHeads.getText());
	}
	
	int getSectorsPerTrack() throws NumberFormatException {
		return Integer.parseInt(m_tfSectorsPerTrack.getText());
	}
	
	int getSectorLength() throws NumberFormatException {
		return Integer.parseInt(m_tfSectorsPerTrack.getText());
	}

	int getStepSpeed() throws NumberFormatException {
		return Integer.parseInt(m_tfStepSpeed.getText());
	}

	int getReducedWriteCurrent() throws NumberFormatException {
		return Integer.parseInt(m_tfReducedCurrent.getText());
	}

	int getWritePrecompensation() throws NumberFormatException {
		return Integer.parseInt(m_tfPrecomp.getText());
	}

	boolean getBufferedStep() {
		return m_chbBufferedStep.isSelected();
	}
}