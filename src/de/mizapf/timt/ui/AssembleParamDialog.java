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
    
    Copyright 2020 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import de.mizapf.timt.assm.Hint;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.FormatException;

import de.mizapf.timt.TIImageTool;

class AssembleParamDialog extends ToolDialog implements ActionListener {
	
	TIImageTool m_app;
	JTextField 		m_tfCommand;
	JTextField 		m_tfRemove;
	JTextField 		m_tfAppend;
	JCheckBox       m_chbOverwrite;

	AssembleParamDialog(JFrame owner, TIImageTool app) {
		super(owner, TIImageTool.langstr("AsmParameters"));
		m_app = app;
	}
	
/*
	| 	Assembler Parameters									|

		Using external assembler command [....]

		$SRC is the source file, $OBJ is the object file
		
		Name of the object file: 
		Remove [   ]
		Append [   ]
		
		Object files will be inserted in current image.
		
		Overwrite existing files [ ]
		
		The selected files use COPY directives. 
		
		Specify the image files for the referenced drives 
		in the source files:
		
		[DSK1] -      <current image> [JFileChooser]
		[DSK2] -      <current image> [JFileChooser]
		[HDS1.ASM.] - <current image> [JFileChooser]
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	void createGui(Font font) {
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("AsmColumn"));
		int nColumnWidth1 = fm.stringWidth("XXXXXX");

		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));		
		putTextLine(this, "!" + TIImageTool.langstr("AsmTitle"),0);
		add(Box.createVerticalStrut(10));		
		m_tfCommand = putTextField(this, TIImageTool.langstr("AsmExternal"), settings.getPropertyString("asmtool"), nColumnWidth, 0);
		add(Box.createVerticalStrut(10));		
		putTextLine(this, TIImageTool.langstr("AsmExplainParam"), 0);
		add(Box.createVerticalStrut(10));		
		putTextLine(this, TIImageTool.langstr("AsmNameFile"), 0);
		m_tfRemove = putTextField(this, TIImageTool.langstr("AsmRemove"), "_S", nColumnWidth, nColumnWidth1);
		m_tfAppend = putTextField(this, TIImageTool.langstr("AsmAppend"), "_O", nColumnWidth, nColumnWidth1);
		add(Box.createVerticalStrut(10));		
		putTextLine(this, TIImageTool.langstr("AsmExplainInsert"), 0);			
		m_chbOverwrite = putCheckBox(this, TIImageTool.langstr("OverwriteExisting"), false, nColumnWidth);		
		add(Box.createVerticalStrut(10));
		add(Box.createVerticalGlue());

		addButtons();
	}
	
	String getCommand() {
		return m_tfCommand.getText();
	}
	
	String getRemove() {
		return m_tfRemove.getText();
	}
	String getAppend() {
		return m_tfAppend.getText();
	}
	
	boolean allowOverwrite() {
		return m_chbOverwrite.isSelected();
	}
}
