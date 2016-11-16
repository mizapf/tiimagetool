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
//import java.util.*;
//import gnu.io.*;

import de.mizapf.timt.TIImageTool;

import javax.swing.*; 

public class ToolDialog extends JDialog implements ActionListener {

	protected boolean	m_bSet;
	protected JFrame 	m_frmMain;
	protected int		m_nColumnWidth;
	protected JButton	m_btnOK;
	protected JButton	m_btnCancel;
	
	protected static final int DEVLINE = 1;
	protected static final int FILELINE = 2;
	protected static final int DDLINE = 3;
	protected static final int SULINE = 4;
	protected static final int COLINE = 5;
	protected static final String CFICON = "cfcard1.png";
	protected static final String IMGICON = "imgicon1.png";
	protected static final String PRGICON = "gear1.png";
	
	protected static final String DISKSICON = "disks.png";
	
	protected ToolDialog(JFrame owner, String sTitle) {
		super(owner, sTitle, true);
		m_frmMain = owner;
	}
	
	protected void prepareGui() {
		m_bSet = false;			
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));
	}
	
	protected void addButtons() {
		add(Box.createVerticalStrut(10));		
		Box box7 = new Box(BoxLayout.X_AXIS);		
		m_btnOK = new JButton("OK");
		m_btnOK.addActionListener(this);
		m_btnCancel = new JButton("Cancel");
		m_btnCancel.addActionListener(this);
		m_btnOK.setPreferredSize(new Dimension(100, 25));
		m_btnCancel.setPreferredSize(new Dimension(100, 25));
		box7.add(Box.createHorizontalGlue());		
		box7.add(Box.createHorizontalStrut(10));		
		box7.add(m_btnOK);
		box7.add(Box.createHorizontalStrut(10));		
		box7.add(m_btnCancel);
		box7.add(Box.createHorizontalStrut(10));		
		box7.add(Box.createHorizontalGlue());		
		add(box7);		
		add(Box.createVerticalStrut(10));		
		
		pack();
		setLocationRelativeTo(getParent());
	}
		
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==m_btnOK) {
			m_bSet = true;
			dispose();
		}
		if (ae.getSource()==m_btnCancel) {
			m_bSet = false;
			dispose();
		}
	}
			
	protected void determineWidth(String s) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(TIImageTool.dialogFont);
		m_nColumnWidth = fm.stringWidth(s);
	}
	
	protected int determineFieldWidth(String s) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(TIImageTool.dialogFont);
		return fm.stringWidth(s);
	}
	
	protected void addLine(String sLabel) {
		Box box1 = new Box(BoxLayout.X_AXIS);
		box1.add(Box.createHorizontalStrut(10));
		JLabel jl = new JLabel(sLabel, SwingConstants.LEFT);
		jl.setFont(TIImageTool.dialogFont);
		box1.add(jl);
		box1.add(Box.createHorizontalStrut(10));
		box1.add(Box.createHorizontalGlue());
		add(box1);
		add(Box.createVerticalStrut(2));			
	}
	
	protected void addLine(String sLabel, JComponent jc) {
		Box box1 = new Box(BoxLayout.X_AXIS);
		box1.add(Box.createHorizontalStrut(10));
		JLabel jl = new JLabel(sLabel, SwingConstants.LEFT);
		jl.setFont(TIImageTool.dialogFont);
		jl.setPreferredSize(new Dimension(m_nColumnWidth, TIImageTool.dialogHeight));
		box1.add(jl);
		box1.add(Box.createHorizontalStrut(15));
//		jc.setPreferredSize(new Dimension(m_nColumnWidth, m_nFontHeight)); 
		jc.setFont(TIImageTool.dialogFont);
		box1.add(jc);
		box1.add(Box.createHorizontalStrut(10));
		box1.add(Box.createHorizontalGlue());
		add(box1);
		add(Box.createVerticalStrut(2));	
	}
	
	protected void addLine(String sLabel1, JComponent jc1, String sLabel2, JComponent jc2) {
		Box box1 = new Box(BoxLayout.X_AXIS);

		box1.add(Box.createHorizontalStrut(10));

		JLabel jl1 = new JLabel(sLabel1, SwingConstants.LEFT);
		jl1.setFont(TIImageTool.dialogFont);
		jl1.setPreferredSize(new Dimension(m_nColumnWidth, TIImageTool.dialogHeight));
		box1.add(jl1);
		box1.add(Box.createHorizontalStrut(15));
		jc1.setFont(TIImageTool.dialogFont);
		box1.add(jc1);

		JLabel jl2 = new JLabel(sLabel2, SwingConstants.LEFT);
		jl2.setFont(TIImageTool.dialogFont);
		jl2.setPreferredSize(new Dimension(m_nColumnWidth, TIImageTool.dialogHeight));
		box1.add(jl2);
		box1.add(Box.createHorizontalStrut(15));
		jc2.setFont(TIImageTool.dialogFont);
		box1.add(jc2);
		
		box1.add(Box.createHorizontalStrut(10));

		add(box1);
		add(Box.createVerticalStrut(2));	
	}
	
	final protected int getHeight(Font font, String sSample) {
		FontRenderContext frc = ((Graphics2D)(m_frmMain.getGraphics())).getFontRenderContext();
		LineMetrics lm = font.getLineMetrics(sSample, 0, 2, frc);
		return (int)Math.ceil(lm.getHeight()*1.5);
	}

	final protected void addField(Box box, JComponent comp, int nWidth, int nHeight, int nSpace) {
		comp.setMinimumSize(new Dimension(nWidth, nHeight));

		if (nWidth!=0) {
			comp.setPreferredSize(new Dimension(nWidth, nHeight));
			comp.setMaximumSize(new Dimension(nWidth, nHeight));
		}
		box.add(comp);
		if (nSpace!=0) box.add(Box.createHorizontalStrut(10));
		else box.add(Box.createGlue());
	}
	
	final protected void putTextLine(Container where, String sText, int nWidth) {
		Box box0 = new Box(BoxLayout.X_AXIS);
		box0.add(Box.createHorizontalStrut(10));

		Font font = TIImageTool.dialogFont;
		if (sText.charAt(0)=='!') {
			sText = sText.substring(1);			
			font = TIImageTool.boldDialogFont;
		}

		JLabel jl = new JLabel(sText, SwingConstants.LEFT);
		jl.setFont(font);
		
		addField(box0, jl,  nWidth, 18,  0);
		box0.add(Box.createHorizontalStrut(10));		
		where.add(box0);		
	}
	
	final protected JTextField putTextField(Container where, String sText, String sDefaultEntry, int nColumnWidth, int nFieldWidth) {
		Box box = new Box(BoxLayout.X_AXIS);
		JTextField tf = new JTextField();
		tf.setText(sDefaultEntry);
		box.add(Box.createHorizontalStrut(10));		
		JLabel jl = new JLabel(sText, SwingConstants.LEFT); 
		jl.setFont(TIImageTool.dialogFont);
		addField(box, jl,  nColumnWidth, 25,  10);
		addField(box, tf, nFieldWidth, 25,  0);
		box.add(Box.createHorizontalStrut(10));		
		where.add(box);
		return tf;
	}
	
	final protected JLabel putLabel(Container where, String sText, String sDefaultEntry, int nColumnWidth) {
		Box box1 = new Box(BoxLayout.X_AXIS);
		JLabel jl = new JLabel();
		jl.setText(sDefaultEntry);
		jl.setFont(TIImageTool.dialogFont);
		box1.add(Box.createHorizontalStrut(10));
		JLabel jla = new JLabel(sText, SwingConstants.LEFT);
		jla.setFont(TIImageTool.dialogFont);		
		addField(box1, jla,  nColumnWidth, 25,  10);
		addField(box1, jl, 0, 25,  0);
		box1.add(Box.createHorizontalStrut(10));		
		where.add(box1);
		return jl;
	}
	
	final protected JComboBox<String> putComboBox(Container where, String sText, String[] options, int nSelectedIndex, int nColumnWidth) {
		Box box3 = new Box(BoxLayout.X_AXIS);
		JComboBox<String> jc = new JComboBox<String>(options);
		jc.setSelectedIndex(nSelectedIndex);
		jc.setFont(TIImageTool.dialogFont);
		box3.add(Box.createHorizontalStrut(10));
		JLabel jl = new JLabel(sText, SwingConstants.LEFT);
		jl.setFont(TIImageTool.dialogFont);
		addField(box3, jl,  nColumnWidth, 25,  10);
		addField(box3, jc, 0, 25,  0);
		box3.add(Box.createHorizontalStrut(10));		
		where.add(box3);
		return jc;
	}
	
	final protected JCheckBox putCheckBox(Container where, String sText, boolean bSelected, int nLabelWidth) {
		Box box7 = new Box(BoxLayout.X_AXIS);
		JCheckBox chb = new JCheckBox();
		chb.setSelected(bSelected);
		box7.add(Box.createHorizontalStrut(10));		
		JLabel jl = new JLabel(sText, SwingConstants.LEFT);
		jl.setFont(TIImageTool.dialogFont);
		addField(box7, jl,  nLabelWidth, 25,  10);
		addField(box7, chb, 100, 25,  0);
		box7.add(Box.createHorizontalStrut(10));		
		where.add(box7);
		return chb;
	}
	
	final protected JRadioButton putRadioButton(Container where, String sText, boolean bSelected, int nLabelWidth, int nValueWidth, ButtonGroup bg) {
		Box box7 = new Box(BoxLayout.X_AXIS);
		JRadioButton chb = new JRadioButton();
		chb.setSelected(bSelected);
		box7.add(Box.createHorizontalStrut(10));	
		JLabel jl = new JLabel(sText, SwingConstants.LEFT);
		jl.setFont(TIImageTool.dialogFont);
		addField(box7, jl,  nLabelWidth, 25,  10);
		addField(box7, chb, nValueWidth, 25,  0);
		box7.add(Box.createHorizontalStrut(10));		
		where.add(box7);
		bg.add(chb);
		return chb;
	}

	final protected JRadioButton[] putRadioButtons(Container where, String sLabel, int nLabelWidth, String[] asOption, int[] anWidth, int nSelected) {
		Box box7 = new Box(BoxLayout.X_AXIS);
		ButtonGroup bg = new ButtonGroup();
		JRadioButton[] arb = new JRadioButton[asOption.length];
		boolean bHoriz = (anWidth!=null);
		JRadioButton rb = null;
		
		box7.add(Box.createHorizontalStrut(10));	
		JLabel jl = new JLabel(sLabel, SwingConstants.LEFT);
		jl.setFont(TIImageTool.dialogFont);

		if (bHoriz) {
			// Horizontal
			//    Prompt      o Option 1   * Option 2    o Option 3
			addField(box7, jl,  nLabelWidth, 25,  10);
			for (int i=0; i < asOption.length; i++) {
				rb = new JRadioButton(asOption[i]);
				rb.setFont(TIImageTool.dialogFont);
				arb[i] = rb;
				bg.add(rb);
				int nSpace = (i==asOption.length-1)? 0 : 10; 
				addField(box7, rb, anWidth[i], 25, nSpace);
				rb.setSelected(i==nSelected);
			}
			box7.add(Box.createHorizontalStrut(10));		
			where.add(box7);
		}
		else {
			// Vertical 
			//    Prompt      o Option 1
			//                * Option 2
			//                o Option 3
			//
			addField(box7, jl,  nLabelWidth, 25,  10);
			for (int i=0; i < asOption.length; i++) {
				rb = new JRadioButton(asOption[i]);
				rb.setFont(TIImageTool.dialogFont);
				bg.add(rb);
				arb[i] = rb;
				addField(box7, rb, 0, 25, 0);	
				rb.setSelected(i==nSelected);
				where.add(box7);
				box7 = new Box(BoxLayout.X_AXIS);
				box7.add(Box.createHorizontalStrut(10));
				addField(box7, new JLabel(""), nLabelWidth, 25,  10);
			}
			
		}
		return arb;
	}

	protected void addChoiceLine(int nColumnWidth, String prompt, int line, JTextField textField, int width) {

		JButton button = null;
			
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(10));
		JLabel jl = new JLabel(prompt, SwingConstants.LEFT);
		jl.setFont(TIImageTool.dialogFont);

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
		ImageIcon diskicon = null;
		java.net.URL iconurl = null;
		switch (line) {
		case DEVLINE: 
			iconurl = ToolDialog.class.getResource(CFICON);
			break;
		case FILELINE: 
			iconurl = ToolDialog.class.getResource(IMGICON);
			break;
		case DDLINE: 
			iconurl = ToolDialog.class.getResource(PRGICON);
			break;
		}

		if (iconurl != null) {
			diskicon = new ImageIcon(iconurl);
			button = new JButton(diskicon);
		} 
		else {
			System.err.println("Error: Could not locate icon image in package " + iconurl);
			button = new JButton("Choose");
		}
		
		button.setActionCommand(String.valueOf(line));
		button.addActionListener(this);
		button.setMinimumSize(new Dimension(width, 32));
		button.setPreferredSize(new Dimension(width, 32));
		button.setMaximumSize(new Dimension(width, 32));
		box.add(button);
		box.add(Box.createHorizontalStrut(10));
		
		// Selected path
		textField.setEditable(false);
		textField.setFont(TIImageTool.dialogFont);
		textField.setMinimumSize(new Dimension(300, 20));
		textField.setMaximumSize(new Dimension(1000, 20));

		box.add(textField);
		box.add(Box.createHorizontalStrut(10));
		
		add(box);
	}
	
	public boolean confirmed() {
		return m_bSet;
	}
}
