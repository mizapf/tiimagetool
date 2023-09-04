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
import java.awt.*;
import java.awt.event.*;

import de.mizapf.timt.files.ImageFormat;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.files.FormatParameters;
import de.mizapf.timt.files.FloppyFileSystem;

class NewIDEImageDialog extends ToolDialog implements ActionListener, FocusListener {
	
	JTextField 		m_tfSize;
	JTextField[]	m_atfPartition;	

	JTextField[]	m_atfSize;
		
	int 			m_nFullCapacity;
	int[] 			m_anSize;

	String[]		m_asName;
	
	NewIDEImageDialog(JFrame owner) {
		super(owner, TIImageTool.langstr("Title.IDE.CreateNew"));
		m_atfPartition = new JTextField[4];
		m_atfSize = new JTextField[4];
		m_anSize = new int[4];
	}
	
/*		
	| 	Create new partitioned IDE image										|
	
	    Partitions can be created from 1 to 4, but they must be contiguous.

		Full capacity          [...]
	
		Partition 1 
		Name                   [...]
		Size                   [...]
		
		Partition 2 
		Name                   [...]
		Size                   [...]

		Partition 3 
		Name                   [...]
		Size                   [...]

		Partition 4 
		Name                   [...]
		Size                   [...]

				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+

*/	
	public void createGui(Font font) {
		prepareGui();
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = determineMaxWidth(font, TIImageTool.langstr("Image.IDE.FullCapacity"), TIImageTool.langstr("VolumeName"));
		int nFieldWidth = fm.stringWidth("XXXXXXXXXXXX");

		add(vspace(80));
		putMultiTextLine(this, TIImageTool.langstr("Dialog.IDE.Header"));
		putMultiTextLine(this, TIImageTool.langstr("Dialog.IDE.HeaderUnpart"));
		add(vspace(120));
		// Max capacity is 4*248 = 992 MiB
		m_tfSize = putTextField(this,  TIImageTool.langstr("Image.IDE.FullCapacity"), "400", nColumnWidth, nFieldWidth); 
		add(vspace(80));		
		m_tfSize.addActionListener(this);	
		m_tfSize.addFocusListener(this);	
		
		for (int i=1; i < 5; i++) {
			putTextLine(this, "!" + TIImageTool.langstr("Dialog.IDE.Partition") + " " + i, 0);
			add(vspace(80));
			m_atfPartition[i-1] = putTextField(this, TIImageTool.langstr("VolumeName"), (i==1)? "PART1" : "", nColumnWidth, nFieldWidth);
			m_atfSize[i-1] = putTextField(this, TIImageTool.langstr("Capacity"), (i==1)? "400" : "", nColumnWidth, nFieldWidth);
			add(vspace(80));
			m_atfSize[i-1].addActionListener(this);	
			m_atfSize[i-1].addFocusListener(this);
			
			m_atfPartition[i-1].addActionListener(this);
			m_atfPartition[i-1].addFocusListener(this);
		}
		
		add(Box.createVerticalGlue());

		m_asName = new String[4];
		for (int i=0; i < 4; i++) m_asName[i] = "";
		m_nFullCapacity = 400;
		
		addButtons();
	}
	
	// Only if there is a name, the partition is valid, and the value of the size is shown
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==m_tfSize) {
			System.out.println("Action performed");
			try {
				m_nFullCapacity = Integer.parseInt(m_tfSize.getText());
				distributeCapacity();
			}
			catch (NumberFormatException nfx) {
				m_tfSize.setText(String.valueOf(m_nFullCapacity));
			}
		}
		
		for (int i=0; i < 4; i++) {
			if (ae.getSource()==m_atfPartition[i]) {
				String editpart = m_atfPartition[i].getText().trim();
				if ((editpart.length() == 0 && m_asName[i].length() > 0)
					|| (editpart.length() > 0 && m_asName[i].length() == 0))
					distributeCapacity();
				m_asName[i] = editpart;
			}
		}
		
		super.actionPerformed(ae);
	}
	
	private void distributeCapacity() {
		int count = getDefinedPartitions();
		System.out.println("Defined partitions = " + count);
		for (int i=0; i < 4; i++) {
			if (i > count-1) {
				m_atfPartition[i].setText("");
				m_atfSize[i].setText("");
			}
			else {
				m_atfSize[i].setText(String.valueOf(m_nFullCapacity/count));
			}
		}
	}
	
	public void focusGained(FocusEvent fe) {
		System.out.println("Focus gained");
	}

	public void focusLost(FocusEvent fe) {
		System.out.println("Focus lost");
		
		if (fe.getSource()==m_tfSize) {
			try {
				m_nFullCapacity = Integer.parseInt(m_tfSize.getText());
				distributeCapacity();
			}
			catch (NumberFormatException nfx) {
				m_tfSize.setText(String.valueOf(m_nFullCapacity));
			}
			return;
		}
		
		for (int i=0; i < 4; i++) {
			if (fe.getSource()==m_atfPartition[i]) {
				String editpart = m_atfPartition[i].getText().trim();
				if ((editpart.length() == 0 && m_asName[i].length() > 0)
					|| (editpart.length() > 0 && m_asName[i].length() == 0))
				distributeCapacity();
				m_asName[i] = editpart;
			}
		}
	}
	
	private int getDefinedPartitions() {
		for (int i=0; i < 4; i++) {
			if (m_atfPartition[i].getText().trim().length()==0) return i;
		}
		return 4;
	}
	
	String[] getPartitionNames() {
		int count = getDefinedPartitions();
		String[] as = new String[count];
		for (int i=0; i < count; i++) {
			as[i] = m_atfPartition[i].getText().trim();
		}
		return as;
	}
	
	int[] getPartitionSizes() {
		int count = getDefinedPartitions();
		int[] an = new int[count];
		for (int i=0; i < count; i++) {
			try {
				an[i] = Integer.parseInt(m_atfSize[i].getText().trim());
			}
			catch (NumberFormatException nfx) {
				an[i] = -1;
			}
		}
		return an;
	}
}