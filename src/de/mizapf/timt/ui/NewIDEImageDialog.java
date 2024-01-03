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
    
    Copyright 2023 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import de.mizapf.timt.files.ImageFormat;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.files.FormatParameters;
import de.mizapf.timt.files.HarddiskFileSystem;
import de.mizapf.timt.files.Time;

class NewIDEImageDialog extends ToolDialog implements ActionListener, FocusListener {
	
	JTextField 		m_tfSize;
	JTextField[]	m_atfPartition;	

	JTextField[]	m_atfSize;
		
	int 			m_nFullCapacity;
	int[] 			m_anSize;

	String[]		m_asName;
	
	NewIDEImageDialog(JFrame owner) {
		super(owner, TIImageTool.langstr("Title.IDE.CreateNew"));
		m_atfPartition = new JTextField[8];
		m_atfSize = new JTextField[8];
		m_anSize = new int[8];
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

		putMultiTextLine(this, TIImageTool.langstr("Dialog.IDE.Header"));
		putMultiTextLine(this, TIImageTool.langstr("Dialog.IDE.HeaderUnpart"));
		add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));
		
//		add(vspace(120));
		// Max capacity is 4*248 = 992 MiB
		m_tfSize = putTextField(this,  TIImageTool.langstr("Image.IDE.FullCapacity"), "200", nColumnWidth, nFieldWidth); 
		add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));
		
		//		add(vspace(80));		
		m_tfSize.addActionListener(this);	
		m_tfSize.addFocusListener(this);	

		JPanel jp = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
		
		for (int i=1; i <=8; i++) {
			putTextLine(jp, "!" + TIImageTool.langstr("Dialog.IDE.Partition") + " " + i, 0);
			jp.add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));

			m_atfPartition[i-1] = putTextField(jp, TIImageTool.langstr("VolumeName"), (i==1)? "PART1" : "", nColumnWidth, nFieldWidth);
			m_atfSize[i-1] = putTextField(jp, TIImageTool.langstr("Capacity"), (i==1)? "200" : "", nColumnWidth, nFieldWidth);
			jp.add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));

			m_atfSize[i-1].addActionListener(this);	
			m_atfSize[i-1].addFocusListener(this);
			
			m_atfPartition[i-1].addActionListener(this);
			m_atfPartition[i-1].addFocusListener(this);
		}
		
		JScrollPane jsp = new JScrollPane(jp);
		add(jsp);

		add(Box.createVerticalGlue());

		m_asName = new String[8];
		for (int i=0; i < 8; i++) m_asName[i] = "";
		m_nFullCapacity = 200;
		
		addButtons();
	}
	
	// Only if there is a name, the partition is valid, and the value of the size is shown
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==m_tfSize) {
			// System.out.println("Action performed");
			try {
				m_nFullCapacity = Integer.parseInt(m_tfSize.getText());
				distributeCapacity();
			}
			catch (NumberFormatException nfx) {
				m_tfSize.setText(String.valueOf(m_nFullCapacity));
			}
		}
		
		for (int i=0; i < 8; i++) {
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
	
	/** Assist in filling in the sizes.
	*/
	private void distributeCapacity() {
		int count = getDefinedPartitions();
		// System.out.println("Defined partitions = " + count);
		for (int i=count; i < 8; i++) {
			m_atfPartition[i].setText("");
			m_atfSize[i].setText("");
		}
		int[] sizes = getPartitionSizes();
		int sum = 0;
		for (int i=0; i < sizes.length-1; i++) {
			sum += sizes[i];
		}
		// System.out.println("sum = " + sum);	
		int prop = m_nFullCapacity - sum;
		if (prop > 248) prop = 248;
		if (prop < 0) prop = 0;
		
		m_atfSize[count-1].setText(String.valueOf(prop));
	}
	
	public void focusGained(FocusEvent fe) {
		// System.out.println("Focus gained");
	}

	public int getCapacity() {
		return m_nFullCapacity;
	}
	
	public void focusLost(FocusEvent fe) {
		// System.out.println("Focus lost");
		
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
		
		for (int i=0; i < 8; i++) {
			if (fe.getSource()==m_atfPartition[i]) {
				String editpart = m_atfPartition[i].getText().trim();
				// if ((editpart.length() == 0 && m_asName[i].length() > 0)
				//	|| (editpart.length() > 0 && m_asName[i].length() == 0))
					distributeCapacity();
				m_asName[i] = editpart;
			}
		}
	}
	
	private int getDefinedPartitions() {
		for (int i=0; i < 8; i++) {
			if (m_atfPartition[i].getText().trim().length()==0) return i;
		}
		return 8;
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
	
	FormatParameters getParameters(int part) {
		int[] size = getPartitionSizes();
		String[] name = getPartitionNames();
		
		FormatParameters params = new FormatParameters(name[part], true);
		params.setHD(Time.createNow(), getAUSize(part), getReserved(size[part]), HarddiskFileSystem.SCSI);
		
		int i = size[part] * 4096;
		
		// One sector (512 bytes) contains the partition table; take it from the size
		// of the last partition
		if (part == size.length-1) 
			i = i-2;  
			
		params.setTotal(i);
		return params;
	}
	
	int getAUSize(int i) {
		// Max AU = 63488 = 31 * 2048
		// Sectors = cap * 4096
		
		// AU size = sectors / max = cap * 4096 / (31 * 2048)
		//          = cap * 2 / 31
		double nAUSize = (getPartitionSizes()[i]*2)/31.0;
		if (nAUSize > 8) return 16;
		if (nAUSize > 4) return 8;
		if (nAUSize > 2) return 4;
		if (nAUSize > 1) return 2;
		return 1;
	}
	
	private int getReserved(int sizeMB) {
	
		int res = 0;
		// For small drives, double the reserved AUs every 16 MiB
		if (sizeMB < 50) {
			res = 2 << (sizeMB/16 + 9);
		}
		else {
			// For bigger drives, increase the reserved AUs linearly up to FF
			res = ((sizeMB-50)*2/3 + 128) * 64;
		}
		
		if (res > 16320) res = 16320;
		if (res < 512) res = 512;
		return res;
		
		// SCSI: max=0xe6*0x40 = 14720  (bei 248 MiB)
	}
}