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
import java.util.ArrayList;
import java.util.Arrays;

import de.mizapf.timt.files.Partition;
import de.mizapf.timt.files.Interval;
import de.mizapf.timt.TIImageTool;

class EditCF7Dialog extends ToolDialog implements ActionListener {
		
	String[] m_partName;
	JTextField[] m_partField;
	JButton m_jbAdd;
	JButton m_jbRemove;
	JPanel m_pnlPartitions;
	
	int m_nColumnWidth;
	int m_nNumWidth;
	
	EditCF7Dialog(JFrame owner, String[] part) {
		super(owner, TIImageTool.langstr("Title.CF7.Edit"));	
		m_partName = part;
	}
	
	public void createGui(Font font) {
		prepareGui();
		putMultiTextLine(this, TIImageTool.langstr("Dialog.CF7.Exp1"));
		putMultiTextLine(this, TIImageTool.langstr("Dialog.CF7.Exp2"));
		add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));
	
		m_nColumnWidth = determineFieldWidth(TIImageTool.langstr("VolumeName"));
		m_nNumWidth = determineFieldWidth("XXXXXXXXXXX");
						
		m_pnlPartitions = new JPanel();
		m_pnlPartitions.setLayout(new BoxLayout(m_pnlPartitions, BoxLayout.Y_AXIS));
		
		buildPartitionList();

		JPanel jp = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
		jp.add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));
		jp.add(m_pnlPartitions);
		m_jbAdd = putAddButton(jp, TIImageTool.langstr("Dialog.CF7.AddVolume"), m_nColumnWidth);
		
		JScrollPane jsp = new JScrollPane(jp);
		add(jsp);
		
		m_jbAdd.addActionListener(this);		
		
		addButtons();
	}
	
	private JComponent[] putTextField2(Container where, String sText, String sDefaultEntry, int nColumnWidth, int nFieldWidth, boolean bLast) {
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(TIImageTool.dialogHeight/2));		
		
		JLabel jl = new JLabel(sText, SwingConstants.LEFT); 
		JTextField tf = new JTextField(sDefaultEntry);
		JButton jb = null;
				
		addField(box, jl,  nColumnWidth, TIImageTool.dialogHeight+10, TIImageTool.dialogHeight/2);
		addField(box, tf, nFieldWidth, TIImageTool.dialogHeight*3/2,  TIImageTool.dialogHeight/2);
		if (bLast) {
			jb = new JButton("-");
			addField(box, jb, 50, TIImageTool.dialogHeight*3/2, TIImageTool.dialogHeight/2);
		}
		box.add(Box.createGlue());		

		where.add(box);
		where.add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));
		
		JComponent[] jc = new JComponent[2];
		jc[0] = tf;
		jc[1] = jb;
		
		return jc;
	}
	
	private JButton putAddButton(Container where, String sText, int nColumnWidth) {
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(TIImageTool.dialogHeight/2));		
		
		JLabel jl = new JLabel(sText, SwingConstants.LEFT); 		
		JButton bt = new JButton("+");
				
		addField(box, jl,  nColumnWidth, TIImageTool.dialogHeight+10, TIImageTool.dialogHeight/2);
		addField(box, bt, 50, TIImageTool.dialogHeight*3/2, 0);

		where.add(box);
		where.add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));
		
		return bt;
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==m_jbRemove) {
			removePartition();
		}
		else {
			if (ae.getSource()==m_jbAdd) {				
				addPartition();
			}
		}
		super.actionPerformed(ae);
	}
	
	private void addPartition() {
		int newLen = m_partName.length+1;
		m_partName = Arrays.copyOf(m_partName, newLen);
		m_partName[newLen-1] = "          ";
		buildPartitionList();
		pack();
	}	
	
	private void removePartition() {
		int newLen = m_partName.length-1;
		m_partName = Arrays.copyOf(m_partName, newLen);
		
		buildPartitionList();
		pack();
	}
	
	String[] getPartitionNames() {
		String[] as = new String[m_partField.length];
		for (int i=0; i < as.length; i++) {
			as[i] = m_partField[i].getText().trim();
		}
		return as;
	}
	
	private void buildPartitionList() {
		int j=0;
		String partName = null;

		m_partField = new JTextField[m_partName.length];

		m_pnlPartitions.removeAll();
		for (int i=0; i < m_partField.length; i++) {
			if (m_partName[i] != null) {
				partName = m_partName[i];
			}
			else 
				partName = "---";
			
			// System.out.println(partName);
			
			JComponent[] jc = putTextField2(m_pnlPartitions, String.format(TIImageTool.langstr("Dialog.CF7.Volume"), (i+1)), partName, m_nColumnWidth, m_nNumWidth, (i==m_partField.length-1));
			
			m_partField[i] = (JTextField)jc[0]; 
			if (jc[1] != null) {
				m_jbRemove = (JButton)jc[1];
				m_jbRemove.addActionListener(this);
			}
		}
	}
}
