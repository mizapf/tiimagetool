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
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import java.io.*;
import java.util.*;
import java.awt.print.*;
import de.mizapf.timt.TIImageTool;

class DVEditorFrame extends JFrame implements ActionListener, UndoableEditListener, DocumentListener {

	public final static String FROMEDITOR = ".FRMED";
	
	public final static String CLOSE = "CLOSE";
	public final static String QUIT = "QUIT";
	public final static String UNDO = "UNDO";
	public final static String REDO = "REDO";
	public final static String CUT = "CUT";
	public final static String COPY = "COPY";
	public final static String PASTE = "PASTE";
	public final static String SELECT_ALL = "SELECTALL";
	
	protected UndoAction m_UndoAction;
	protected RedoAction m_RedoAction;
	UndoManager m_UndoManager = null;
	
	Document m_doc;

	String m_sTitle;
	
	HashMap<String,Action> m_hmActions;

	JMenuItem m_jmiClose;
	JMenuItem m_jmiQuit;
	JMenuItem m_jmiUndo;
	JMenuItem m_jmiRedo;
	JMenuItem m_jmiCut;
	JMenuItem m_jmiCopy;
	JMenuItem m_jmiPaste;
	JMenuItem m_jmiSelectAll;
	
	File m_flText;

	Cursor m_curNormal;
	boolean m_bAbort;
	
	JEditorPane m_jep;
	
	ImportContentAction m_importact;
	DirectoryView m_dvCurrent;
	
	class UndoAction extends AbstractAction {
		public UndoAction() {
			super(TIImageTool.langstr("Undo"));
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			try {
				m_UndoManager.undo();
			} 
			catch (CannotUndoException ex) {
				error(TIImageTool.langstr("UndoCannot") + ": " + ex.getMessage());
			}
			updateUndoState();
			m_RedoAction.updateRedoState();
		}

		protected void updateUndoState() {
			if (m_UndoManager.canUndo()) {
				setUndoEnabled(true);
				// putValue(Action.NAME, m_UndoManager.getUndoPresentationName());
			} 
			else {
				setUndoEnabled(false);
				// putValue(Action.NAME, "Undo");
			}
		}
	}

	class RedoAction extends AbstractAction {
		public RedoAction() {
			super(TIImageTool.langstr("Redo"));
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			try {
				m_UndoManager.redo();
			} 
			catch (CannotRedoException ex) {
				error(TIImageTool.langstr("RedoCannot") + ": " + ex.getMessage());
			}
			updateRedoState();
			m_UndoAction.updateUndoState();
		}

		protected void updateRedoState() {
			if (m_UndoManager.canRedo()) {
				setRedoEnabled(true);
				// putValue(Action.NAME, m_UndoManager.getRedoPresentationName());
			} else {
				setRedoEnabled(false);
				// putValue(Action.NAME, "Redo");
			}
		}
	}

	DVEditorFrame(Frame owner, ImportContentAction ia, DirectoryView dv) {
		// Parameter dialog should probably be raised in this class
		super(TIImageTool.langstr("DVEditorTitle"));
		m_flText = null;
		m_sTitle = TIImageTool.langstr("Unnamed");
		createUI("");
		setWindowTitle();
		m_importact = ia;
		m_dvCurrent = dv;
		setVisible(true);
	}
		
	private void createUI(String sText) {
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		JMenuBar jb = new JMenuBar();
		JMenu jmFile = new JMenu(TIImageTool.langstr("File"));
		jb.add(jmFile);
		JMenu jmEdit = new JMenu(TIImageTool.langstr("Edit"));
		jb.add(jmEdit);

		m_jmiClose = new JMenuItem(TIImageTool.langstr("CloseSave"));
		m_jmiQuit = new JMenuItem(TIImageTool.langstr("ExitNoSave"));
		
		jmFile.add(m_jmiClose);
		jmFile.addSeparator();
		jmFile.add(m_jmiQuit);

		m_jmiUndo = new JMenuItem(TIImageTool.langstr("Undo"));
		m_jmiRedo = new JMenuItem(TIImageTool.langstr("Redo"));
		m_jmiCut = new JMenuItem(TIImageTool.langstr("Cut"));
		m_jmiCopy = new JMenuItem(TIImageTool.langstr("Copy"));
		m_jmiPaste = new JMenuItem(TIImageTool.langstr("Paste"));
		m_jmiSelectAll = new JMenuItem(TIImageTool.langstr("SelectAll"));
		
		jmEdit.add(m_jmiUndo);
		jmEdit.add(m_jmiRedo);		  
		jmEdit.addSeparator();
		jmEdit.add(m_jmiCut);
		jmEdit.add(m_jmiCopy);
		jmEdit.add(m_jmiPaste);
		jmEdit.addSeparator();
		jmEdit.add(m_jmiSelectAll);
	
		m_jmiUndo.setEnabled(false);
		m_jmiRedo.setEnabled(false);

		// -----------------------------------
		m_jmiClose.setActionCommand(CLOSE);
		m_jmiClose.addActionListener(this);
		m_jmiQuit.setActionCommand(QUIT);
		m_jmiQuit.addActionListener(this);
		// -----------------------------------

		m_jmiUndo.setActionCommand(UNDO);
		m_jmiUndo.addActionListener(this);		  
		m_jmiRedo.setActionCommand(REDO);
		m_jmiRedo.addActionListener(this);		  
		m_jmiCut.setActionCommand(CUT);
		m_jmiCut.addActionListener(this);		 
		m_jmiCopy.setActionCommand(COPY);
		m_jmiCopy.addActionListener(this);		  
		m_jmiPaste.setActionCommand(PASTE);
		m_jmiPaste.addActionListener(this);		   
		m_jmiSelectAll.setActionCommand(SELECT_ALL);
		m_jmiSelectAll.addActionListener(this);		
				
		setJMenuBar(jb);

		Container cntEditor = getContentPane();
		m_jep = new JEditorPane("text/plain", sText);
		m_jep.setFont(TIImageTool.contentFont);
		
		m_doc = m_jep.getDocument();
		m_doc.addUndoableEditListener(this);
		m_doc.addDocumentListener(this);
		
		JScrollPane jp = new JScrollPane(m_jep);
		cntEditor.add(jp);
		setSize(new Dimension(800,600));

		// Undo Manager
		m_UndoManager = new UndoManager();

		m_UndoAction = new UndoAction();
		m_RedoAction = new RedoAction();
		
		// Commands
		Action[] act = m_jep.getActions();	// possible actions
		m_hmActions = new HashMap<String,Action>();
		// put possible editor commands by name into hashtable
		for (Action a:act) m_hmActions.put((String)a.getValue(Action.NAME), a);
		
		m_curNormal = m_jep.getCursor();
		m_bAbort = true;
	}
	
	void setWaitCursor(boolean bWait) {
		Cursor wait = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
		m_jep.setCursor(bWait? wait : m_curNormal);
	}
	
	private void setUndoEnabled(boolean b) {
		m_jmiUndo.setEnabled(b);
	}

	private void setRedoEnabled(boolean b) {
		m_jmiRedo.setEnabled(b);
	}

	private void quit(boolean bAbort) {
		m_bAbort = bAbort;
		dispose();
	}

	public String getText() {
		try {
			return m_doc.getText(0, m_doc.getLength());
		}
		catch (BadLocationException blx) {
			blx.printStackTrace();
			return null;
		}
	}

	public boolean aborted() {
		return m_bAbort;
	}
	
	private void setWindowTitle() {
		setTitle(m_sTitle);
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals(CLOSE)) {
			m_bAbort = false;
			dispose();
			m_importact.convertAndImport(getText().getBytes(), m_dvCurrent, FROMEDITOR, true);
			return;
		}
		if (ae.getActionCommand().equals(QUIT)) {
			m_bAbort = true;
			dispose();
			return;
		}

		if (ae.getActionCommand().equals(UNDO)) {
			m_UndoAction.actionPerformed(ae);
			return;
		}
		if (ae.getActionCommand().equals(REDO)) {
			m_RedoAction.actionPerformed(ae);
			return;
		}
		if (ae.getActionCommand().equals(CUT)) {
			Action ac = m_hmActions.get(DefaultEditorKit.cutAction);
			if (ac==null) {
				error(String.format(TIImageTool.langstr("DVEditorIntErr"), TIImageTool.langstr("Cut")));
				return;
			}
			// m_jmiPaste.setEnabled(true);
			ac.actionPerformed(ae);
			return;
		}
		if (ae.getActionCommand().equals(COPY)) {
			Action ac = m_hmActions.get(DefaultEditorKit.copyAction);
			if (ac==null) {
				error(String.format(TIImageTool.langstr("DVEditorIntErr"), TIImageTool.langstr("Copy")));
				return;
			}
			// m_jmiPaste.setEnabled(true);
			ac.actionPerformed(ae);
			return;
		}
		if (ae.getActionCommand().equals(PASTE)) {
			Action ac = m_hmActions.get(DefaultEditorKit.pasteAction);
			if (ac==null) {
				error(String.format(TIImageTool.langstr("DVEditorIntErr"), TIImageTool.langstr("Paste")));
				return;
			}
			ac.actionPerformed(ae);
			return;
		}
		if (ae.getActionCommand().equals(SELECT_ALL)) {
			Action ac = m_hmActions.get(DefaultEditorKit.selectAllAction);
			if (ac==null) {
				error(String.format(TIImageTool.langstr("DVEditorIntErr"), TIImageTool.langstr("SelectAll")));
				return;
			}
			ac.actionPerformed(ae);
			return;
		}
	}
	
	public void undoableEditHappened(UndoableEditEvent e) {
		//Remember the edit and update the menus.
		m_UndoManager.addEdit(e.getEdit());
		// NPX ↓
		m_UndoAction.updateUndoState();
		m_RedoAction.updateRedoState();
	}
	
	public void changedUpdate(DocumentEvent de) {
		setWindowTitle();
	}

	public void insertUpdate(DocumentEvent de) {
		setWindowTitle();
	}

	public void removeUpdate(DocumentEvent de) {
		setWindowTitle();
	}
 
	int check(String sMessage) {
		return JOptionPane.showConfirmDialog(this, sMessage, TIImageTool.langstr("Attention"), 
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE);
	}
		
	public void info(String sInfo) {
		JOptionPane.showMessageDialog(this, sInfo, TIImageTool.langstr("Information"), JOptionPane.INFORMATION_MESSAGE);
	}
	
	public void error(String sError) {
		JOptionPane.showMessageDialog(this, sError, TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE);
	}
}
