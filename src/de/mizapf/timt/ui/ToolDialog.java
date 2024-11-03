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

import de.mizapf.timt.TIImageTool;

import javax.swing.*; 

public class ToolDialog extends JDialog implements ActionListener, KeyListener {

	protected boolean	m_bSet;
	protected JFrame 	m_frmMain;
	protected int		m_nColumnWidth;
	protected JButton	m_btnOK;
	protected JButton	m_btnCancel;
	
	protected Settings	settings;
	
	protected static final int DEVLINE = 1;
	protected static final int FILELINE = 2;
	protected static final int DDLINE = 3;
	protected static final int SULINE = 4;
	protected static final int COLINE = 5;
	protected static final String CFICON = "cfcard1.png";
	protected static final String IMGICON = "imgicon1.png";
	protected static final String PRGICON = "gear1.png";
	protected static final String SEAICON = "automt.png";
	
	protected static final String DISKSICON = "disks.png";
	
	public static final int ONLY_OK = 1;
	public static final int OK_AND_CANCEL = 2;
	public static final int ONLY_CANCEL = 3;
	public static final int NONE = 4;
		
	protected ToolDialog(JFrame owner, String sTitle) {
		super(owner, sTitle, true);
		m_frmMain = owner;
	}
	
	protected ToolDialog(JFrame owner, String sTitle, boolean modal) {
		super(owner, sTitle, modal);
		m_frmMain = owner;
	}
	
	protected FontMetrics getDialogFontMetrics() {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(TIImageTool.dialogFont);
		return fm;
	}
	
	protected void prepareGui() {
		m_bSet = false;			
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));
	}
	
	void setSettings(Settings set) {
		settings = set;
	}
	
	protected void addButtons() {
		addButtons(OK_AND_CANCEL);
	}
		
	protected Component vspace(int ratio) {
		int textHeight = TIImageTool.dialogHeight;
		return Box.createVerticalStrut((textHeight * ratio) / 100);
	}
	
	protected void addButtons(int buttons) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(TIImageTool.boldDialogFont);		
		int textWidth = fm.stringWidth(TIImageTool.langstr("ButtonSampleText"));
		
		int textHeight = TIImageTool.dialogHeight;
		
		Insets margin = (Insets)UIManager.get("Button.margin");
		add(Box.createVerticalStrut(textHeight/2));		
		Box box7 = new Box(BoxLayout.X_AXIS);

		if (buttons == ONLY_OK || buttons == OK_AND_CANCEL) {		
			m_btnOK = new JButton(TIImageTool.langstr("OK"));
			m_btnOK.addActionListener(this);
			m_btnOK.setPreferredSize(new Dimension(textWidth + margin.left + margin.right + 10, textHeight + margin.top + margin.bottom + 10));
			box7.add(Box.createHorizontalGlue());		
			box7.add(Box.createHorizontalStrut(textHeight/2));		
			box7.add(m_btnOK);
		}

		if (buttons == ONLY_CANCEL || buttons == OK_AND_CANCEL) {
			m_btnCancel = new JButton(TIImageTool.langstr("Cancel"));
			m_btnCancel.addActionListener(this);
			m_btnCancel.setPreferredSize(new Dimension(textWidth + margin.left + margin.right + 10, textHeight + margin.top + margin.bottom + 10));
			box7.add(Box.createHorizontalStrut(textHeight/2));		
			box7.add(m_btnCancel);
			box7.add(Box.createHorizontalStrut(textHeight/2));		
		}
		
		if (buttons != NONE) {
			box7.add(Box.createHorizontalGlue());		
			add(box7);
		}
		add(Box.createVerticalStrut(textHeight/2));		
		
		pack();
		setLocationRelativeTo(getParent());
	}
	
	protected Box createPathSelectBox(JComponent[] comp, String sLabel, String sPath, int nColumnWidth) {
		ImageIcon diskicon = null;
		java.net.URL iconurl = ToolDialog.class.getResource(DISKSICON);
		JButton btn = null;
		FontMetrics fm = getDialogFontMetrics();
		
		if (iconurl != null) {
			diskicon = new ImageIcon(iconurl);
			btn = new JButton(diskicon);
		} 
		else {
			System.err.println(TIImageTool.langstr("NoImage") + " " + iconurl);
			btn = new JButton(TIImageTool.langstr("ImagePlaceholder"));
		}
		comp[0] = btn;
		
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
		JLabel jl = new JLabel(sLabel, SwingConstants.LEFT); 
		jl.setFont(TIImageTool.dialogFont);
		String lastPath = sPath;
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
		btn.setMinimumSize(new Dimension(35, 32));
		btn.setPreferredSize(new Dimension(35, 32));
		btn.setMaximumSize(new Dimension(35, 32));
		box.add(btn);
		box.add(Box.createHorizontalStrut(10));
		
		// Selected path
		int nPathWidth = fm.stringWidth(lastPath);
		JTextField text = new JTextField(lastPath);
		text.setEditable(false);
		text.setFont(TIImageTool.dialogFont);
		text.setMinimumSize(new Dimension(nPathWidth, 20));
		text.setMaximumSize(new Dimension(1000, 20));

		box.add(text);
		box.add(Box.createHorizontalStrut(10));

		comp[1] = text;

		return box;
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
	
	public void keyReleased(KeyEvent ke) {
	}
			
	public void keyPressed(KeyEvent ke) {
	}

	public void keyTyped(KeyEvent ke) {
		if (ke.getKeyChar()==KeyEvent.VK_ENTER) {
			m_bSet = true;
			dispose();
		}
		if (ke.getKeyChar()==KeyEvent.VK_ESCAPE) {
			m_bSet = false;
			dispose();
		}
	}

	protected int getColumnWidth(int count) {
		StringBuilder sb = new StringBuilder();
		while (count-- > 0) sb.append("x");
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(TIImageTool.dialogFont);
		return fm.stringWidth(sb.toString());
	}
	
	protected void determineWidth(String s) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(TIImageTool.dialogFont);
		m_nColumnWidth = fm.stringWidth(s);
	}

	protected int getBoldWidth(String s) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(TIImageTool.boldDialogFont);
		return fm.stringWidth(s);
	}
	
	protected int determineFieldWidth(String s) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(TIImageTool.dialogFont);
		return fm.stringWidth(s);
	}
	
	protected int determineMaxWidth(Font fnt, String s1, String s2) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(fnt);
		int col1 = fm.stringWidth(s1);
		int col2 = fm.stringWidth(s2);
		if (col1 > col2) return col1;
		return col2;
	}
	
	protected int determineMaxWidth(String[] keys) {
		int nColumnWidth = 0;
		for (String stext : keys) {
			int nc1 = determineFieldWidth(TIImageTool.langstr(stext));
			if (nc1 > nColumnWidth) nColumnWidth = nc1;
		}
		return nColumnWidth;
	}
	
	protected void addLine(String sLabel) {
		Box box1 = new Box(BoxLayout.X_AXIS);
		box1.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
		JLabel jl = new JLabel(sLabel, SwingConstants.LEFT);
		box1.add(jl);
		box1.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
		box1.add(Box.createHorizontalGlue());
		add(box1);
		add(Box.createVerticalStrut(TIImageTool.dialogHeight/5));			
	}
	
	protected void addLine(String sLabel, JComponent jc) {
		Box box1 = new Box(BoxLayout.X_AXIS);
		box1.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
		JLabel jl = new JLabel(sLabel, SwingConstants.LEFT);
		jl.setPreferredSize(new Dimension(m_nColumnWidth, TIImageTool.dialogHeight));
		box1.add(jl);
		box1.add(Box.createHorizontalStrut(TIImageTool.spaceWidth+5));
//		jc.setPreferredSize(new Dimension(m_nColumnWidth, m_nFontHeight)); 
		box1.add(jc);
		box1.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
		box1.add(Box.createHorizontalGlue());
		add(box1);
		add(Box.createVerticalStrut(TIImageTool.dialogHeight/5));	
	}
	
	protected void addLine(String sLabel1, JComponent jc1, String sLabel2, JComponent jc2) {
		Box box1 = new Box(BoxLayout.X_AXIS);

		box1.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));

		JLabel jl1 = new JLabel(sLabel1, SwingConstants.LEFT);
		jl1.setPreferredSize(new Dimension(m_nColumnWidth, TIImageTool.dialogHeight));
		box1.add(jl1);
		box1.add(Box.createHorizontalStrut(TIImageTool.spaceWidth+5));
		box1.add(jc1);

		JLabel jl2 = new JLabel(sLabel2, SwingConstants.LEFT);
		jl2.setPreferredSize(new Dimension(m_nColumnWidth, TIImageTool.dialogHeight));
		box1.add(jl2);
		box1.add(Box.createHorizontalStrut(TIImageTool.spaceWidth+5));
		box1.add(jc2);
		
		box1.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));

		add(box1);
		add(Box.createVerticalStrut(TIImageTool.dialogHeight/5));	
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
		if (nSpace!=0) box.add(Box.createHorizontalStrut(nSpace));
		else box.add(Box.createGlue());
	}
	
	final protected void putTextLine(Container where, String sText, int nWidth) {
		Box box0 = new Box(BoxLayout.X_AXIS);
		box0.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));

		Font font = TIImageTool.dialogFont;
		if (sText.charAt(0)=='!') {
			sText = sText.substring(1);			
			font = TIImageTool.boldDialogFont;
		}

		JLabel jl = new JLabel(sText, SwingConstants.LEFT);
		jl.setFont(font);
	
		addField(box0, jl,  nWidth, 18,  0);
		box0.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));		
		where.add(box0);		
	}
	
	/* Problem with HTML text: A JLabel does not wrap automatically; it
	   extend as far as a single line will take it, unless there are explicit
	   break tags. It may be possible but difficult to tell the layout manager
	   to define a label of an initial width (not maximum, may be resized) 
	   but a variable height. Note that the pack method belongs to Window.
	   We will use an own format which is parsed here.	   
	*/
	final protected void putMultiTextLine(Container where, String sText) {
		Font font = TIImageTool.dialogFont;
		if (sText.charAt(0)=='!') {
			sText = sText.substring(1);			
			font = TIImageTool.boldDialogFont;
		}
		
		String[] textLines = sText.split("\n");
		for (String line : textLines) {
			Box box0 = new Box(BoxLayout.X_AXIS);
			box0.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
			JLabel jl = new JLabel(line, SwingConstants.LEFT);
			jl.setFont(font);
			box0.add(jl);
			box0.add(Box.createGlue());
			jl.setMaximumSize(new Dimension(600, 10000));
			box0.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));		
			where.add(box0);
		}
	}	
		
	final protected JTextField putTextField(Container where, String sText, String sDefaultEntry, int nColumnWidth, int nFieldWidth) {
		Box box = new Box(BoxLayout.X_AXIS);
		JTextField tf = new JTextField();
		tf.setText(sDefaultEntry);
		box.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));		
		JLabel jl = new JLabel(sText, SwingConstants.LEFT); 
		addField(box, jl,  nColumnWidth, TIImageTool.dialogHeight+10, TIImageTool.spaceWidth);
		addField(box, tf, nFieldWidth, TIImageTool.dialogHeight*3/2,  0);
		box.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));		
		where.add(box);
		where.add(Box.createVerticalStrut(TIImageTool.spaceWidth));
		return tf;
	}
	
	final protected JTextField[] putTextFieldN(Container where, String sText, int nFieldCount, int nColumnWidth, int nFieldWidth) {
		Box box = new Box(BoxLayout.X_AXIS);
		JTextField[] atf = new JTextField[nFieldCount];

		JLabel jl = new JLabel(sText, SwingConstants.LEFT); 
		box.add(Box.createHorizontalStrut(TIImageTool.dialogHeight));	
		addField(box, jl,  nColumnWidth/2, TIImageTool.dialogHeight+10, 0);
		box.add(Box.createHorizontalStrut(nColumnWidth));	
		
		for (int i=0; i < nFieldCount; i++) {
			atf[i] = new JTextField();
			atf[i].setText("");
			addField(box, atf[i], nFieldWidth, TIImageTool.dialogHeight*3/2,  0);			
		}
		box.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));		

		where.add(box);
		where.add(Box.createVerticalStrut(TIImageTool.spaceWidth));
		return atf;
	}
	
	final protected JLabel putLabel(Container where, String sText, String sDefaultEntry, int nColumnWidth) {
		return putLabel(where, sText, sDefaultEntry, nColumnWidth, 0);
	}
	
	final protected JLabel putLabel(Container where, String sText, String sDefaultEntry, int nColumnWidth, int nFieldWidth) {
		Box box1 = new Box(BoxLayout.X_AXIS);
		JLabel jl = new JLabel();
		jl.setText(sDefaultEntry);
		box1.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
		JLabel jla = new JLabel(sText, SwingConstants.LEFT);
		addField(box1, jla,  nColumnWidth, TIImageTool.dialogHeight, TIImageTool.spaceWidth);
		addField(box1, jl, nFieldWidth, TIImageTool.dialogHeight,  0);
		box1.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));		
		where.add(box1);
		return jl;
	}
	
	final protected JComboBox<String> putComboBox(Container where, String sText, String[] options, int nSelectedIndex, int nColumnWidth) {
		Box box3 = new Box(BoxLayout.X_AXIS);
		JComboBox<String> jc = new JComboBox<String>(options);
		jc.setSelectedIndex(nSelectedIndex);
		box3.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
		JLabel jl = new JLabel(sText, SwingConstants.LEFT);
		addField(box3, jl,  nColumnWidth, TIImageTool.dialogHeight,  TIImageTool.spaceWidth);
		addField(box3, jc, 0, TIImageTool.dialogHeight,  0);
		box3.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));		
		where.add(box3);
		return jc;
	}
	
	final protected JCheckBox putCheckBox(Container where, String sText, boolean bSelected, int nLabelWidth) {
		Box box7 = new Box(BoxLayout.X_AXIS);
		JCheckBox chb = new JCheckBox();
		chb.setSelected(bSelected);
		box7.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));		
		JLabel jl = new JLabel(sText, SwingConstants.LEFT);
		addField(box7, jl,  nLabelWidth, TIImageTool.dialogHeight,  TIImageTool.spaceWidth);
		addField(box7, chb, 100, TIImageTool.dialogHeight,  0);
		box7.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));		
		where.add(box7);
		where.add(Box.createVerticalStrut(TIImageTool.dialogHeight/2));
		return chb;
	}
	
	final protected JRadioButton putRadioButton(Container where, String sText, boolean bSelected, int nLabelWidth, int nValueWidth, ButtonGroup bg) {
		Box box7 = new Box(BoxLayout.X_AXIS);
		JRadioButton chb = new JRadioButton();
		chb.setSelected(bSelected);
		box7.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));	
		JLabel jl = new JLabel(sText, SwingConstants.LEFT);
		addField(box7, jl,  nLabelWidth, TIImageTool.dialogHeight,  TIImageTool.spaceWidth);
		addField(box7, chb, nValueWidth, TIImageTool.dialogHeight,  0);
		box7.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));		
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
		
		box7.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));	
		JLabel jl = new JLabel(sLabel, SwingConstants.LEFT);

		if (bHoriz) {
			// Horizontal
			//    Prompt      o Option 1   * Option 2    o Option 3
			addField(box7, jl,  nLabelWidth, 25,  10);
			for (int i=0; i < asOption.length; i++) {
				rb = new JRadioButton(asOption[i]);
				arb[i] = rb;
				bg.add(rb);
				int nSpace = (i==asOption.length-1)? 0 : 10; 
				addField(box7, rb, anWidth[i], TIImageTool.dialogHeight, nSpace);
				rb.setSelected(i==nSelected);
			}
			box7.add(Box.createHorizontalStrut(TIImageTool.dialogHeight));		
			where.add(box7);
		}
		else {
			// Vertical 
			//    Prompt      o Option 1
			//                * Option 2
			//                o Option 3
			//
			addField(box7, jl,  nLabelWidth, TIImageTool.dialogHeight,  TIImageTool.spaceWidth);
			for (int i=0; i < asOption.length; i++) {
				rb = new JRadioButton(asOption[i]);
				bg.add(rb);
				arb[i] = rb;
				addField(box7, rb, 0, TIImageTool.dialogHeight, 0);	
				rb.setSelected(i==nSelected);
				where.add(box7);
				box7 = new Box(BoxLayout.X_AXIS);
				box7.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
				addField(box7, new JLabel(""), nLabelWidth, TIImageTool.dialogHeight,  TIImageTool.spaceWidth);
			}
			
		}
		return arb;
	}

/*	protected void addChoiceLineWithAuto(int nColumnWidth, String prompt, int line, int number, JTextField textField, int width) {
		addChoiceLineAuto(nColumnWidth, prompt, line, number, textField, width, true);
	}

	protected void addChoiceLine(int nColumnWidth, String prompt, int line, int number, JTextField textField, int width) {
		addChoiceLineAuto(nColumnWidth, prompt, line, number, textField, width);
	}
*/
	protected void addSearchLine(int nColumnWidth, String prompt, JTextField textField) {
		JButton searchbutton = null;
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
		JLabel jl = new JLabel(prompt, SwingConstants.LEFT);
		// jl.setMinimumSize(new Dimension(nColumnWidth, TIImageTool.dialogHeight));
//		jl.setPreferredSize(new Dimension(nColumnWidth, TIImageTool.dialogHeight));
//		jl.setMaximumSize(new Dimension(nColumnWidth, TIImageTool.dialogHeight));
		box.add(jl);
		box.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));

		ImageIcon searchicon = null;
		java.net.URL searchurl = ToolDialog.class.getResource(SEAICON);
		searchicon = new ImageIcon(searchurl);
		searchbutton = new JButton(searchicon);			
		
		box.add(Box.createHorizontalStrut(10));

		textField.setEditable(false);
		textField.setMinimumSize(new Dimension(300, 20));
		textField.setPreferredSize(new Dimension(300, 20));
		textField.setMaximumSize(new Dimension(100000, 20));

		box.add(textField);
		box.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
		searchbutton.setActionCommand("AUTOSEARCH");
		searchbutton.addActionListener(this);
		searchbutton.setMinimumSize(new Dimension(38, 35));
		searchbutton.setPreferredSize(new Dimension(38, 35));
		searchbutton.setMaximumSize(new Dimension(38, 35));
		box.add(searchbutton);		
		
		add(box);
		add(Box.createVerticalStrut(TIImageTool.dialogHeight));		
	}
	
	protected void addChoiceLine(int nColumnWidth, String prompt, int line, int number, JTextField textField, int width) {

		JButton button = null;
		JButton searchbutton = null;
			
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
		JLabel jl = new JLabel(prompt, SwingConstants.LEFT);

		// Path setup
		// Prompt
		jl.setMinimumSize(new Dimension(nColumnWidth, TIImageTool.dialogHeight));
		if (nColumnWidth!=0) {
			jl.setPreferredSize(new Dimension(nColumnWidth, TIImageTool.dialogHeight));
			jl.setMaximumSize(new Dimension(nColumnWidth, TIImageTool.dialogHeight));
		}
		box.add(jl);
		box.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
		
		// Button
		ImageIcon diskicon = null;
		ImageIcon searchicon = null;
		java.net.URL iconurl = null;
		java.net.URL searchurl = ToolDialog.class.getResource(SEAICON);
		
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
		
		if (iconurl != null && searchurl != null) {
			diskicon = new ImageIcon(iconurl);
			button = new JButton(diskicon);
			
			searchicon = new ImageIcon(searchurl);
		//	searchbutton = new JButton(searchicon);			
		} 
		else {
			System.err.println(TIImageTool.langstr("NoImage") + " " + iconurl);
			button = new JButton(TIImageTool.langstr("ImagePlaceholder"));
		}
		
		button.setActionCommand(String.valueOf(number));
		button.addActionListener(this);
		button.setMinimumSize(new Dimension(width, 32));
		button.setPreferredSize(new Dimension(width, 32));
		button.setMaximumSize(new Dimension(width, 32));
		box.add(button);
		box.add(Box.createHorizontalStrut(10));
		
		// Selected path
		textField.setEditable(false);
		textField.setMinimumSize(new Dimension(300, 20));
		textField.setMaximumSize(new Dimension(1000, 20));

		box.add(textField);
		box.add(Box.createHorizontalStrut(TIImageTool.spaceWidth));
		
/*		if (line==DEVLINE) {
			searchbutton.setActionCommand("AUTO");
			searchbutton.addActionListener(this);
			searchbutton.setMinimumSize(new Dimension(38, 35));
			searchbutton.setPreferredSize(new Dimension(38, 35));
			searchbutton.setMaximumSize(new Dimension(38, 35));
			box.add(searchbutton);		
		}
*/		
		add(box);
		add(Box.createVerticalStrut(TIImageTool.dialogHeight));
	}
	
	public boolean confirmed() {
		return m_bSet;
	}
}
