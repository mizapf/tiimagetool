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
import java.util.*;
import java.awt.font.*;
import de.mizapf.timt.TIImageTool;

class CF7VolumeSelection extends ToolDialog {

	String[] m_volume;
	JTextField m_tfVolumeNumber;

	class ClickListener implements MouseListener {
		// ===================================================================
		//   MouseListener
		// ================================================================
		
		CF7VolumeSelection select;
		int number;
		
		ClickListener(int num, CF7VolumeSelection sel) {
			select = sel;
			number = num;
		}
		
		public void mouseEntered(MouseEvent act) { }
		public void mouseExited(MouseEvent act) { }
		public void mousePressed(MouseEvent act) { 	}
		public void mouseReleased(MouseEvent act) { }
		
		// Click occured on panel.
		public void mouseClicked(MouseEvent act) {
			if (act.getClickCount()>=2) {
				select.setNumberAndGo(number);
			}
			else {
				select.setNumber(number);
			}
		}		
	}	

	CF7VolumeSelection(JFrame owner) {
		super(owner, TIImageTool.langstr("SelectVolume"));
	}
	
/*
	| 	Select volume             								|
        CF7 dump: <pathname>

              +--------+
	          | 1 VOL1 |
	          | 2 VOL2 |
	          |  ....  |
		      +--------+
		      
		Volume number [ 1   ]
			+-------+			+-----------+
			|	OK	|			|	Cancel	|
			+-------+           +-----------+
*/	
	void createGui() {
		prepareGui();
		int nLabelWidth = determineFieldWidth(TIImageTool.langstr("CF7VolumeColumn"));
		
		JPanel jpSelection = new JPanel();
		jpSelection.setLayout(new BoxLayout(jpSelection, BoxLayout.Y_AXIS));

		Color[] color = new Color[2];
		color[0] = new Color(180,200,220);
		color[1] = new Color(220,230,240);

		for (int i=0; i < m_volume.length; i++) {
			int pos = m_volume[i].indexOf(":");
			if (pos == -1) {
				System.err.println(String.format(TIImageTool.langstr("CF7VolumeBug"), m_volume[i]));
			}
			else {
				int num = -1;
				try {
					num = Integer.parseInt(m_volume[i].substring(0,pos));
					String volname = m_volume[i].substring(pos+1).trim();
					if (volname.length()==0) volname = "<" + TIImageTool.langstr("Unnamed") + ">";
					Box b1 = addEntry(num+1, nLabelWidth, volname); 
					b1.setOpaque(true);
					b1.setBackground(color[i&1]);
					jpSelection.add(b1);
					b1.addMouseListener(new ClickListener(num+1, this));
				}
				catch (NumberFormatException nx) {
					System.err.println(String.format(TIImageTool.langstr("CF7VolumeBug"), m_volume[i]));
				}
			}							
		}
	
		JScrollPane scp = new JScrollPane(jpSelection);
		add(scp);
		add(Box.createVerticalStrut(10));
		
		m_tfVolumeNumber = putTextField(this, TIImageTool.langstr("VolumeNumber"), "", nLabelWidth, 0);
		m_tfVolumeNumber.setText("1");

		addButtons();	
	}
		
	void setContent(String[] volList) {
		m_volume = volList;
	}

	int getNumber() throws NumberFormatException {
		return Integer.parseInt(m_tfVolumeNumber.getText().trim()) - 1;
	}
	
	void setNumber(int num) {
		m_tfVolumeNumber.setText(String.valueOf(num));
	}
	
	void setNumberAndGo(int num) {
		setNumber(num);
		m_bSet = true;
		dispose();
	}
	
	Box addEntry(int number, int width, String sName) {
		Box box1 = new Box(BoxLayout.X_AXIS);
		box1.add(Box.createHorizontalStrut(10));
		JLabel jl = new JLabel(String.valueOf(number), SwingConstants.LEFT);
		jl.setFont(TIImageTool.dialogFont);
		jl.setPreferredSize(new Dimension(width, TIImageTool.dialogHeight));
		box1.add(jl);
		box1.add(Box.createHorizontalStrut(15));
//		jc.setPreferredSize(new Dimension(m_nColumnWidth, m_nFontHeight));
		JLabel jc = new JLabel(String.valueOf(sName));
		jc.setFont(TIImageTool.dialogFont);
		box1.add(jc);
		box1.add(Box.createHorizontalStrut(10));
		box1.add(Box.createHorizontalGlue());
		add(box1);
		add(Box.createVerticalStrut(2));
		box1.setMinimumSize(new Dimension(0, 3*TIImageTool.dialogHeight/2)); 
		box1.setPreferredSize(new Dimension(2*width, 3*TIImageTool.dialogHeight/2));
		return box1;
	}
}
