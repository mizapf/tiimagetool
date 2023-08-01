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
	
	Copyright 2014 Michael Zapf
	www.mizapf.de

****************************************************************************/

package de.mizapf.timt.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.util.List;
import java.util.TooManyListenersException;
import java.io.IOException;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.Component;
import javax.swing.plaf.UIResource;
import javax.swing.event.EventListenerList;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import java.awt.Cursor;

import de.mizapf.timt.files.*;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;

import javax.swing.TransferHandler;
import java.awt.event.InputEvent;
import javax.swing.Action;

/**
	We have four cases:
	
	1. Transfer between two views of the same TIImageTool
	importData: called after drop; flavor = DDData
	exportDone: called after drop; flavor = DDData
	
	2. Transfer between views of different TIImageTools
	Exporter:
	importData: not called
	exportDone: called after drop

	Importer:
	importData: no data flavors (need more in DDData?)
	exportDone: not called
	
	--> 2. not feasible at this point
	
	3. Export from TIImageTool to file system
	importData: not called
	exportDone: called after drop 

	Problem: Although we offer to move, we cannot actually know what happens in the
	outside world. Accordingly, we only have copies, no moves.
	
	
	4. Import from file system to TIImageTool
	importData: called after drop; javaFileListFlavor
	exportDone: not called
*/

class DDTransferHandler extends TransferHandler {

	// ===================================================================
	//   Import	
	// ===================================================================
	
	private DirectoryPanel m_panel;
	private Settings m_Settings;
	
	DDTransferHandler(DirectoryPanel panel, Settings set) {
		m_panel = panel;
		m_Settings = set;
	}
	
	public boolean canImport(TransferHandler.TransferSupport support) {
		DataFlavor[] adf = support.getDataFlavors(); 
				
		if (support.isDataFlavorSupported(DDData.elementCollection)) {
			return true;
		}

		if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			return true;
		}
		
/*		System.err.println("Unsupported DnD data flavor; flavor count = " + adf.length);
		for (DataFlavor df : adf) {
			System.out.println("* " + df);
		} */
		return false;
	}
	
	@SuppressWarnings("unchecked")	// needed because of getTransferData
	public boolean importData(TransferHandler.TransferSupport support) {
		if (!canImport(support)) {
			return false;
		}
		Transferable t = support.getTransferable();
		Directory dir = m_panel.getDirectory();
		Volume vol = dir.getVolume();
		
		try {
			if (support.isDataFlavorSupported(DDData.elementCollection)) {
				int action = 0;
				if (support.isDrop()) {
					action = support.getDropAction();
					// action = 1 for CTRL
					// action = 2 for Shift or no key
					
					// Trick: If no modifier has been pressed, change the action
					// to ACTION_NONE
					if (action == DnDConstants.ACTION_MOVE && !m_panel.shiftPressed())
						action = DnDConstants.ACTION_NONE;

					// The lastSelected is the element that is rendered last
					// due to the mouse pointer moved over it
					Element last = m_panel.getLastSelected();
					String elname = last.getName();
					if (elname != null && elname.equals("..")) {
						// When dragging over "..", move to parent
						m_panel.getView().paste(t, action, dir.getContainingDirectory());
					}
					else {
						m_panel.getView().paste(t, action, last);
					}
				}
				else {
					m_panel.getView().paste(t);
				}					
			}
			else {
				if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {					
					Object trans = t.getTransferData(DataFlavor.javaFileListFlavor);
					java.util.List<File> l = (java.util.List<File>)trans;
					java.io.File[] afile = new java.io.File[l.size()];
					l.toArray(afile);
					
					TIImageTool app = m_panel.getView().getImageTool();
					ImportFilesAction action = new ImportFilesAction();
					action.setLinks(app, app.getMainFrame(), m_Settings);
					action.importElements(afile, m_panel.getView());
				}
			} 
		}
		catch (UnsupportedFlavorException e) {
			System.out.println(TIImageTool.langstr("UnknownFlavor") + ": " + e);
			return false;
		} 
		catch (Exception e) {
			e.printStackTrace();
			JFrame frmMain = m_panel.getView().getFrame();
			frmMain.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			JOptionPane.showMessageDialog(frmMain, TIImageTool.langstr("BUG"), TIImageTool.langstr("InternalError"), JOptionPane.ERROR_MESSAGE);						
		}
		
		m_panel.getView().refreshAll();
		return true;
	}
	
	// ===================================================================
	//   Export	
	// ===================================================================
	
	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}
	
	@SuppressWarnings("unchecked")	// needed because of JList cast
	public Transferable createTransferable(JComponent c) {
		if (c instanceof JList) {
			JList<Element> jl = (JList<Element>)c;
			List<Element> aind = jl.getSelectedValuesList();
			return new DDData(aind, m_panel.getView());	
		}
		else {
			System.out.println("c = " + c);
		}
		return new StringSelection("foo");
	}

	public void exportDone(JComponent c, Transferable t, int action) {
		
//		System.out.println("export done; transferable " + t);
		m_panel.getView().setCCPAction(t, action);
    }
}



