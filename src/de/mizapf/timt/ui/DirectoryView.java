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

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.LinkedList;
import javax.swing.*;

import de.mizapf.timt.files.*;
import de.mizapf.timt.util.Utilities;
//
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.basic.BasicLine;


/** View of a directory. May be attached to the tabbed pane or be a 
	separate frame. Note that there may be more than one view pointing
	to the same directory.
	
	Serves as the 
	   window listener in the detached state.
	   action listener for the close action.
*/
public class DirectoryView implements WindowListener, ActionListener, MouseListener {
	
	Directory 		m_dirCurrent;		// links to Volume and Image
	boolean			m_bAttached;
	DirectoryPanel	m_panel;
	TIImageTool		m_app;
	Settings		m_Settings;
	boolean			m_bFocused;
	JFrame			m_frmOwn;
	
	JMenuBar		m_mbar;
	JMenu			m_mFile;
	EditMenu		m_mEdit;
	
	JLabel			m_TabLabel;
	
	// File menu
	JMenuItem		m_iSave;
	JMenuItem		m_iSaveAs;
	JMenuItem		m_iClose;
	
	// Context menu	
	JMenuItem m_iNewFile;
	JMenuItem m_iNewDirectory;
	JMenuItem m_iArchive;

	JMenuItem m_iCut;
	JMenuItem m_iCopy;
	JMenuItem m_iPaste;
	JMenuItem m_iDelete;
	JMenuItem m_iSelect;
	JMenuItem m_iRename;

	JMenuItem m_iViewFIB;
	JMenuItem m_iViewText;
	JMenuItem m_iViewImage;
	JMenuItem m_iViewDump;
//	JMenuItem m_iViewUtil;
	JMenuItem m_iAssemble;
	JMenuItem m_iLink;
	JMenuItem m_iDisass;
	JMenuItem m_iGPLDisass;
	JMenuItem m_iList;
	JMenuItem m_iSaveTfi;
	JMenuItem m_iSaveDump;
	JMenuItem m_iSendRem;
	JMenuItem m_iChangeDirectory;
	JMenuItem m_iToggleEmulate;
	JMenuItem m_iExportEmulate;
	
	Element m_clickedElement;
	
	public static final int IT_CUT 			= 1<<0;
	public static final int IT_COPY 		= 1<<1;
	public static final int IT_PASTE		= 1<<2;
	public static final int IT_DELETE 		= 1<<3;
	public static final int IT_RENAME 		= 1<<4;
	public static final int IT_SELECT 		= 1<<5;
	public static final int IT_VIEWTEXT 	= 1<<6;
	public static final int IT_VIEWIMAGE 	= 1<<7;
	public static final int IT_VIEWDUMP 	= 1<<8;
	public static final int IT_VIEWFIB 	= 1<<9;
	public static final int IT_ASSEMBLE 	= 1<<10;
	public static final int IT_LINK 		= 1<<11;
	public static final int IT_DISASS	 	= 1<<12;
	public static final int IT_GPLDIS	 	= 1<<13;
	public static final int IT_LIST		 	= 1<<14;
	public static final int IT_SEND		 	= 1<<15;
	public static final int IT_SAVETFI	 	= 1<<16;
	public static final int IT_SAVEDUMP 	= 1<<17;
	public static final int IT_ARCHIVE 	= 1<<18;
	
	public static final int IT_MULTI = IT_ARCHIVE | IT_VIEWDUMP | IT_SEND | IT_RENAME | IT_SELECT |							IT_SAVETFI | IT_SAVEDUMP | IT_LINK |
										IT_VIEWIMAGE | IT_CUT | IT_COPY | IT_PASTE | IT_DELETE |
										IT_LIST | IT_VIEWTEXT | IT_DISASS | 
										IT_GPLDIS | IT_ASSEMBLE;
										
	public static final int IT_BASIC = IT_ARCHIVE | IT_VIEWDUMP | IT_SEND | IT_DELETE | IT_RENAME |
										IT_SAVETFI | IT_SAVEDUMP | IT_SELECT |
										IT_CUT | IT_COPY |	IT_PASTE |IT_LIST | IT_VIEWTEXT;
										
	public static final int IT_NOBASIC = IT_ARCHIVE | IT_VIEWDUMP | IT_SEND | IT_DELETE | IT_RENAME |
										IT_SAVETFI | IT_SAVEDUMP | IT_LINK | IT_SELECT |
										IT_VIEWIMAGE | IT_CUT | IT_COPY | IT_PASTE |
										IT_VIEWTEXT | IT_ASSEMBLE;
										
	public static final int IT_DIR =  IT_CUT | IT_COPY | IT_PASTE | IT_DELETE |     // betreten(1), cut, copy, del, ren, selall
										IT_RENAME | IT_SELECT;
	
	public static final Color BACK = new Color(200,221,242);
	
	/** The (only one) context menu. Needed to decide whether to react on mouseentered. */
	JPopupMenu m_ctxmenu;

	/** Support for DnD menu. */
	JPopupMenu m_dndmenu;
	JMenuItem m_iDnDMove;
	JMenuItem m_iDnDCopy;
	JMenuItem m_iDnDCancel;	
	Element m_lastSelected;
	
	final static String DNDMOVE = "dndmove";
	final static String DNDCOPY = "dndcopy";
	final static String DNDCANCEL = "dndcancel";
	
	public DirectoryView(Directory dir, boolean bAttached, TIImageTool app, Settings set) {
		m_dirCurrent = dir;
		m_bAttached = bAttached;
		m_app = app;
		m_Settings = set;
		m_panel = new DirectoryPanel(this, set);
			
		m_bFocused = false;
		
		// Context menu
		m_iNewFile = app.createMenuItem(new ImportContentAction());
		m_iNewDirectory = app.createMenuItem(new NewDirectoryAction());
		m_iArchive = app.createMenuItem(new CreateArchiveAction());
		m_iSelect = app.createMenuItem(new SelectAllAction());
		
		m_iNewFile.setText(app.langstr("TextBasic"));
		m_iNewDirectory.setText(app.langstr("Directory"));
		m_iArchive.setText(app.langstr("Archive"));

		m_iCut = app.createMenuItem(new CutAction());
		m_iCopy = app.createMenuItem(new CopyAction());
		m_iPaste = app.createMenuItem(new PasteAction());
		m_iDelete = app.createMenuItem(new DeleteAction());

		m_iRename = app.createMenuItem(new RenameAction());
		m_iViewFIB = app.createMenuItem(new ViewFIBAction());
		m_iViewText = app.createMenuItem(new ViewTextAction());
		m_iViewImage = app.createMenuItem(new ViewImageAction());
		m_iViewDump = app.createMenuItem(new ViewDumpAction());
//		m_iViewUtil = app.createMenuItem(new ViewUtilAction());
		m_iAssemble = app.createMenuItem(new AssembleAction());
		m_iLink = app.createMenuItem(new LinkAction());
        m_iDisass = app.createMenuItem(new DisassembleAction());
		m_iGPLDisass = app.createMenuItem(new GPLDisassembleAction());
		m_iList = app.createMenuItem(new ListAction());
		m_iSaveTfi = app.createMenuItem(new SaveTFIAction()); 
		m_iSaveDump = app.createMenuItem(new SavePlainAction());
		m_iSendRem = app.createMenuItem(new ExportRemoteAction());
		m_iChangeDirectory = m_app.createMenuItem(new ChangeDirectoryAction());
		m_iToggleEmulate = m_app.createMenuItem(new ToggleEmulateAction());
		m_iExportEmulate = m_app.createMenuItem(new ExportEmulateAction());
		m_mEdit = app.getEditMenu();   // use the main edit menu
		
		Volume vol = dir.getVolume();
		app.attachView(vol.getImageName(), this, true);
		
		// TODO: Does not work at this point
		if (!bAttached) {
			detach();
		}
	}
		
	public void detach() {
		// Remove from tabs
//		int nWidth = m_panel.getWidth();
//		int nHeight = m_panel.getHeight();
		// Create top-level window
		
		/* Detached window
		
		File      Edit
		----------------
		Save      <EditMenu>
		Save As
		Attach
		
		*/
		
		m_frmOwn = new JFrame(m_dirCurrent.getVolume().getModShortImageName());
		m_frmOwn.setIconImage(m_app.m_frameicon.getImage());
		m_frmOwn.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		m_frmOwn.addWindowListener(this);
		Container cnt = m_frmOwn.getContentPane();
		cnt.setLayout(new BoxLayout(cnt, BoxLayout.Y_AXIS));
		cnt.add(m_panel);

		m_mbar = new JMenuBar();
		m_mFile = new JMenu(m_app.langstr("File"));
		
		// Save
		m_iSave = m_app.createMenuItem(new SaveImageAction());
		m_iSaveAs = m_app.createMenuItem(new SaveAsImageAction());
		
		m_mFile.add(m_iSave);
		m_mFile.add(m_iSaveAs);
		m_iSave.setEnabled(m_dirCurrent.getVolume().isModified());
		m_iSaveAs.setEnabled(m_dirCurrent.getVolume().isModified());
		
		m_iClose = new JMenuItem(m_app.langstr("Attach"));
		m_iClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
		m_iClose.addActionListener(this);
		m_mFile.add(m_iClose);
		
		m_mbar.add(m_mFile);
		m_mEdit = new EditMenu(m_frmOwn, m_app, this);   // create an own edit menu
		m_frmOwn.setJMenuBar(m_mbar);
		m_mbar.add(m_mEdit);
		
		m_frmOwn.setSize(m_app.getFrameSize());

		m_app.removeTab(m_panel);
		cnt.setBackground(BACK);		

		Point loc = m_app.getMainFrame().getLocationOnScreen();		
		m_frmOwn.setLocation(loc.x+20, loc.y+20);
		m_frmOwn.setVisible(true);
		m_bAttached = false;
	}
		
	JFrame getFrame() {
		if (m_frmOwn == null) return m_app.getMainFrame();
		else return m_frmOwn;
	}

	public void close() {
		if (m_frmOwn != null) m_frmOwn.dispose();
		try {
			m_app.closeVolume(m_dirCurrent.getVolume(), this);
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
	}
	
	private void reattach() {
		m_frmOwn.dispose();
		// Re-attach
		Volume vol = m_dirCurrent.getVolume();
		// System.out.println("isModified: " + vol.isModified());
		m_app.attachView(vol.getModShortImageName(), this, false);
		m_frmOwn = null;
		m_mEdit = m_app.getEditMenu();	// Use the edit menu of the main application
		m_bAttached = true;
	}

	public void setPasteEnabled(boolean state) {
		m_mEdit.setPaste(state);
	}
	
	boolean isAttached() {
		return m_bAttached;
	}		
	
	public boolean isFocused() {
		return m_bFocused;
	}
	
	public void selectAll() {
		m_panel.selectAll();
	}
	
	Dimension getSize() {
		if (m_frmOwn == null) return m_app.getFrameSize();
		else return m_frmOwn.getSize(); 
	}
	
	public void enterDirectory(Directory dir) {
		// AWT thread
		m_dirCurrent = dir;
		m_panel.rebuild();
		m_panel.validate();
		activateEditMenu();
	}
	
	public DirectoryPanel getPanel() {
		return m_panel;
	}
	
	public Directory getDirectory() {
		return m_dirCurrent;
	}
	
	public Volume getVolume() {
		if (m_dirCurrent == null) return null;
		return m_dirCurrent.getVolume();
	}
	
	public String getImageName() {
		if (m_dirCurrent == null) return null;
		return getVolume().getImageName();
	}
		
	void refreshAll() {
		m_app.refreshAllViews();
	}
	
	public void refreshView() {
		m_panel.updateView();
		activateEditMenu();
				
		Volume vol = getVolume();
		
		if (vol != null) {
			setSaveOptions(vol.isModified());
			if (m_frmOwn != null) {
				m_frmOwn.setTitle(vol.getModShortImageName());
			}
			else {
				m_TabLabel.setText(vol.getModShortImageName());
			}
		}
	}
	
	public boolean isDetached() {
		return m_frmOwn != null;
	}
	
	public void setTabLabel(JLabel jl) {
		m_TabLabel = jl;
	}
	
	int calcWidth(String sString) {
		Font font = TIImageTool.boldFont;		
		FontMetrics fm = ((Graphics2D)(getFrame().getGraphics())).getFontMetrics(font);
		int nWidth = fm.stringWidth(sString);
		return nWidth;
	}
	
	int calcHeight() {
		Font font = TIImageTool.boldFont;		
		FontMetrics fm = ((Graphics2D)(getFrame().getGraphics())).getFontMetrics(font);
		return fm.getHeight();
	}
		
	/** For operations on the directory. Called by Actions. */
	public List<Element> getSelectedEntries() {
		List<Element> selected = m_panel.getSelectedEntries();
		return selected;
	}
	
	void paste(Transferable t, int action, Element lastSelected) {
		m_app.setClipboard(t);
		
		// We use the ACTION_NONE constant if no modifier has been pressed.
		// There is no invocation of paste if the actual action (according
		// to the DnD support) is ACTION_NONE
		if (action == DnDConstants.ACTION_NONE) {
			// System.out.println("Action none");
			openDnDChoiceMenu(m_panel.getMousePosition());
			m_lastSelected = lastSelected;
		}
		else {
			m_app.markForCut(action == DnDConstants.ACTION_MOVE);
			PasteAction pa = new PasteAction();
			pa.setLinks(m_app, getFrame(), m_Settings);
			
			if (lastSelected instanceof Directory) {
				pa.paste(this, (Directory)lastSelected);
			}
			else pa.paste(this);
			
			m_panel.updateView();
		}
		// update the source!
	}
	
	/** Called for CCP operations. */
	void paste(Transferable t) {
		m_app.setClipboard(t);
		PasteAction pa = new PasteAction();
		pa.setLinks(m_app, getFrame(), m_Settings);
		pa.paste(this);
		// m_panel.updateView();
	}

	Element getClickedElement() {
		return m_clickedElement;
	}
	
	@SuppressWarnings("unchecked")	// needed because of getTransferData
	void setCCPAction(Transferable t, int action) {
		List<Element> list = null;
		try {
			list = (List<Element>)t.getTransferData(DDData.elementCollection);
		}
		catch (UnsupportedFlavorException ufx) {
			System.err.println(TIImageTool.langstr("UnknownFlavor") + ": " + ufx);
			return;
		}
		catch (IOException iox) {
			iox.printStackTrace();
			return;
		}
		Set<String> names = new HashSet<String>();
		for (Element el : list) names.add(el.getName());		
		m_panel.setSelected(names);
		m_app.refreshPanel(getVolume());
	}
	
	boolean markedForCut() {
		return m_app.markedForCut();
	}
	
	public void clearSelection() {
		m_panel.clearSelection();
	}

	TIImageTool getImageTool() {
		return m_app;
	}
	
	void setSaveOptions(boolean bMod) {
		if (!isAttached()) {
			m_iSave.setEnabled(bMod);
			m_iSaveAs.setEnabled(bMod);
		}
		m_app.setSaveOptions();
	}
	
	// ================================================================
	//   ActionListener
	// ================================================================

	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand() == DNDCOPY) {
		//	System.out.println(DNDCOPY);
			m_app.markForCut(false);
			PasteAction pa = new PasteAction();
			pa.setLinks(m_app, getFrame(), m_Settings);
			if (m_lastSelected instanceof Directory) {
				pa.paste(this, (Directory)m_lastSelected);
			}
			else pa.paste(this);			
			m_panel.updateView();
		}
		else {
			if (ae.getActionCommand() == DNDMOVE) {
				//  System.out.println(DNDMOVE);
				m_app.markForCut(true);
				PasteAction pa = new PasteAction();
				pa.setLinks(m_app, getFrame(), m_Settings);
				if (m_lastSelected instanceof Directory) {
					pa.paste(this, (Directory)m_lastSelected);
				}
				else pa.paste(this);			
				m_panel.updateView();
			}
			else {
				if (ae.getActionCommand() == DNDCANCEL) {
					// System.out.println(DNDCANCEL);
				}
				else {
					// Attach the stand-alone frame
					reattach();
				}
			}
		}
	}
	
	// ================================================================
	//   WindowListener
	// ================================================================
	
	public void windowClosing(WindowEvent we) { 
		reattach();
	}	
	
	public void windowClosed(WindowEvent we) {
	}

	public void activateEditMenu() {
		boolean bDirPossible = true;
		if (getVolume().isFloppyImage() || getVolume().isCF7Volume()) {
			Directory dirCurrent = getDirectory(); 
			if (!dirCurrent.isRootDirectory()) bDirPossible = false;
			if (dirCurrent.getDirectories().length>2) bDirPossible = false;
		}			
		boolean bUndo = getVolume().isModified() && !getVolume().isNew();
		boolean bRedo = getVolume().redoPossible();
		boolean bClipboard = m_app.clipboardNotEmpty();	
		m_mEdit.activateMenuItems(bUndo, bRedo, true, bDirPossible, bClipboard, m_app.offersSerialConnection(), getVolume().isHFDCImage());
		m_mEdit.activateActionMenu(true);
	}
	
	public void windowActivated(WindowEvent we) {
	//	System.out.println("Window activated");
		m_app.selectView(this);
		activateEditMenu();
	}
	
	public void windowDeactivated(WindowEvent we) {
	//	System.out.println("Window deactivated");
	//	m_app.unselectAllViews();
	}
	
	public void windowIconified(WindowEvent we) {	}
	public void windowDeiconified(WindowEvent we) {	}
	public void windowOpened(WindowEvent we) {	}
	
	// ===================================================================
	/** Popup menu for current directory. */	
	void openContainerContextMenu(MouseEvent act) {
		Volume vol = m_dirCurrent.getVolume();
		m_ctxmenu = new JPopupMenu();
		JMenu mnew = new JMenu(TIImageTool.langstr("CreateNew"));
		mnew.add(m_iNewFile);
		mnew.add(m_iNewDirectory);
		mnew.add(m_iArchive);
		mnew.setEnabled(!vol.isProtected() && !m_dirCurrent.isProtected());
		m_ctxmenu.add(m_iSelect);
		m_ctxmenu.addSeparator();
		m_ctxmenu.add(mnew);
		m_ctxmenu.addSeparator();	
		m_ctxmenu.add(m_iPaste);
		// We have to enable paste, otherwise pasting from outside by Ctrl-v does not work
		m_iPaste.setEnabled(true /* m_app.clipboardNotEmpty() */);
		if (vol.isCF7Volume() || (vol.isFloppyImage() && (!m_dirCurrent.isRootDirectory() || m_dirCurrent.getDirectories().length>2))) 
			m_iNewDirectory.setEnabled(false); 
		else m_iNewDirectory.setEnabled(true);
//		m_dvSelected.backpaintEntryLines();
		
		m_ctxmenu.show(act.getComponent(), act.getX(), act.getY());		
	}
	
	private void openEntryContextMenu(Component whereclicked, int xpos, int ypos, boolean bWithShift, boolean bWithControl) {
		// we're in the AWT thread here, no need for an invokeLater
		m_ctxmenu = new JPopupMenu();

		List<Element> selected = m_panel.getSelectedEntries();
			
		if (selected.get(0).getName().equals("..")) selected.remove(0);
		
		// If we right-click on a directory or archive file, add the "change directory" action
		boolean isEmulate = false;
		if (selected.size()==1) {
			if ((selected.get(0) instanceof Directory) 
				|| ((selected.get(0) instanceof TFile) 
					&& ((TFile)selected.get(0)).hasArchiveFormat())) {
				m_ctxmenu.add(m_iChangeDirectory);
				m_ctxmenu.addSeparator();
			}
			if (selected.get(0) instanceof TFile) {
				if (((TFile)selected.get(0)).isEmulateFile()) {
					m_ctxmenu.add(m_iToggleEmulate);
					m_ctxmenu.add(m_iExportEmulate);
					m_ctxmenu.addSeparator();
					isEmulate = true;
				}
			}
		}		
		
		if (selected.size()==1) m_iDelete.setText(TIImageTool.langstr("Delete"));
		else {
			m_iDelete.setText(TIImageTool.langstr("DeleteAll"));
		}
		
		m_ctxmenu.add(m_iCut);	
		m_ctxmenu.add(m_iCopy);
		m_ctxmenu.add(m_iPaste);
		m_ctxmenu.addSeparator();
		m_ctxmenu.add(m_iDelete);
		m_ctxmenu.addSeparator();
		m_ctxmenu.add(m_iRename);
		m_ctxmenu.addSeparator();	
		m_ctxmenu.add(m_iSelect);
		m_ctxmenu.addSeparator();		
		m_ctxmenu.add(m_iViewFIB);
		if (!isEmulate) m_ctxmenu.add(m_iViewText);
		if (!isEmulate) m_ctxmenu.add(m_iViewImage);
		m_ctxmenu.add(m_iViewDump);
		if (!isEmulate) {
			m_ctxmenu.add(m_iList); 			
		}
		if (!isEmulate) {
			m_ctxmenu.addSeparator();
//			m_ctxmenu.add(m_iViewUtil); 
			m_ctxmenu.add(m_iAssemble);
			m_ctxmenu.add(m_iLink);
			m_ctxmenu.add(m_iDisass); 
			m_ctxmenu.add(m_iGPLDisass); 
		}
		m_ctxmenu.addSeparator();
		m_ctxmenu.add(m_iSaveTfi);
		m_ctxmenu.add(m_iSaveDump);
		m_ctxmenu.add(m_iSendRem);
		if (!isEmulate) {
			m_ctxmenu.addSeparator();
			m_ctxmenu.add(m_iArchive);
		}

		int setting = 0;				
		setting = getSetting(selected);
				
		if (!m_app.offersSerialConnection() || selected.size()>1) setting &= ~IT_SEND;

		m_iPaste.setEnabled((setting & IT_PASTE)!=0 /*m_app.clipboardNotEmpty() */);
		
		m_iCut.setEnabled((setting & IT_CUT)!=0);
		m_iCopy.setEnabled((setting & IT_COPY)!=0);
		m_iPaste.setEnabled((setting & IT_PASTE)!=0);
		m_iDelete.setEnabled((setting & IT_DELETE)!=0);
		m_iRename.setEnabled((setting & IT_RENAME)!=0);
		m_iSelect.setEnabled((setting & IT_SELECT)!=0);
		m_iViewText.setEnabled((setting & IT_VIEWTEXT)!=0);
		m_iViewImage.setEnabled((setting & IT_VIEWIMAGE)!=0);
//		m_iViewUtil.setEnabled(bUtil);
		m_iViewDump.setEnabled((setting & IT_VIEWDUMP)!=0);
		m_iViewFIB.setEnabled((setting & IT_VIEWDUMP)!=0);
		m_iAssemble.setEnabled((setting & IT_ASSEMBLE)!=0);
		m_iLink.setEnabled((setting & IT_LINK)!=0);
		m_iDisass.setEnabled((setting & IT_DISASS)!=0);
		m_iGPLDisass.setEnabled((setting & IT_GPLDIS)!=0);
		m_iList.setEnabled((setting & IT_LIST)!=0);
		m_iSendRem.setEnabled((setting & IT_SEND)!=0);
		m_iSaveTfi.setEnabled((setting & IT_SAVETFI)!=0);
		m_iSaveDump.setEnabled((setting & IT_SAVEDUMP)!=0);
		m_iArchive.setEnabled((setting & IT_ARCHIVE)!=0);		

//		m_dvSelected.backpaintEntryLines();
		
		m_ctxmenu.show(whereclicked, xpos, ypos);		
	}
		
	static int getSetting(List<Element> selected) {
		int setting = 0;
		if (selected.size() > 0) {
			setting = IT_MULTI;
		}

		for (Element el:selected) {
			if (el instanceof TFile) {
				TFile file = (TFile)el;
				try {
					if (!file.isBasicFile()) {
						setting &= ~IT_LIST;

						if (!file.isProgram()) {
							setting &= ~IT_GPLDIS;

							if (!file.isTaggedObjectCodeFile()) {
								setting &= ~IT_DISASS;
								setting &= ~IT_LINK;
							}
						}
						else {
							setting &= ~(IT_LINK | IT_ASSEMBLE);
						}
					}
					else {
						setting = IT_BASIC;
					}
				}
				catch (Exception iox) {
					setting = IT_NOBASIC;
				}
				if (!file.mayBePrintable()) {
					setting &= ~(IT_VIEWTEXT | IT_ASSEMBLE);
				}
				
				if (!file.isAsmSourceCodeFile()) {
					setting &= ~IT_ASSEMBLE;
				}
				
				if (!file.isImageFile()) setting &= ~IT_VIEWIMAGE;
			}
			else {
				setting = IT_DIR;
				break;
			}
		}
		return setting;
	}
	
	boolean contextMenuVisible() {
		return (m_ctxmenu != null && m_ctxmenu.isVisible());
	}

	boolean active() {
		return (!contextMenuVisible() && !m_app.userActionInProgress());
	}
	
	void stepUp() {
		enterDirectory(m_dirCurrent.getParentDirectory());
	}
		
	public void gotoRootDirectory() {
		enterDirectory(getVolume().getRootDirectory());
	}
	
	public String[] getPath() {
		LinkedList<String> path = new LinkedList<String>();
		Directory dir = m_dirCurrent;
		while (!dir.isRootDirectory()) {
			path.addFirst(dir.getName());
			dir = dir.getParentDirectory();
		}
		return path.toArray(new String[path.size()]);
	}
		
	// ===================================================================
	//   MouseListener
	// ================================================================
	
	public void mouseEntered(MouseEvent act) { }
	public void mouseExited(MouseEvent act) { }
	public void mousePressed(MouseEvent act) { 	}
	public void mouseReleased(MouseEvent act) {	}
	
	// Click occured on panel.
	public void mouseClicked(MouseEvent act) {

		m_clickedElement = m_panel.getElementAtLocation(act.getPoint());

		if (act.getButton()==MouseEvent.BUTTON1) {
			if (act.getClickCount()>=2) {
				if (m_clickedElement instanceof TFile) 
					doubleClickFile((TFile)m_clickedElement);
				else {
					if (m_clickedElement instanceof Directory) {
						Directory dir = (Directory)m_clickedElement;
						if (dir.isParentLink()) stepUp();
						else enterDirectory(dir);
					}
				}
			}
		}
		if (act.getButton()==MouseEvent.BUTTON3) {
			// Click right
			
			String entryname = m_clickedElement.getName();
			if (entryname == null) {
				// Clicked below
				openContainerContextMenu(act);
			}
			else {
				if (!entryname.equals("..")) { 
					JList<Element> list = m_panel.getLinkToJList();
					int index = list.locationToIndex(act.getPoint());
					int[] selected = list.getSelectedIndices();
					// We implement the typical right-click behavior from KDE
					// Rclick on selected item (1 selected): openCM
					// Rclick on selected item (several sel): openCM
					// Rclick on unselected item (0 selected): select + openCM
					// Rclick on unselected item (several sel): unselectAll + select + openCM
					boolean clickSel = false;
					int i=0;
					while ((i < selected.length) && (clickSel==false)) {
						if (index == selected[i++]) clickSel = true;
					}
					if (!clickSel) {
						if (selected.length > 0) {
							list.clearSelection();
						}							
						list.addSelectionInterval(index, index);
					}
					openEntryContextMenu(list, act.getX(), act.getY(), act.isShiftDown(), act.isControlDown());
				}
			}
		}
		m_app.updateMemoryInfo();
	}
	
	// ===================================================================
	// Click actions
	// ===================================================================

	void doubleClickFile(TFile file) {
		JFrame frame = getFrame();
		Volume vol = file.getVolume();
		if (file.hasArchiveFormat()) {
			try {
				Archive ark = file.unpackArchive();
				enterDirectory(ark);
			}
			catch (IllegalOperationException iox) {
				JOptionPane.showMessageDialog(frame, iox.getMessage(), TIImageTool.langstr("IllegalOperation"), JOptionPane.ERROR_MESSAGE);
			}
			catch (FormatException fx) {
				JOptionPane.showMessageDialog(frame, fx.getMessage(), TIImageTool.langstr("UnpackError"), JOptionPane.ERROR_MESSAGE);
			}		
			catch (IOException iox) {
				JOptionPane.showMessageDialog(frame, iox.getMessage(), TIImageTool.langstr("UnpackError"), JOptionPane.ERROR_MESSAGE);
			}
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(frame, ix.getMessage(), TIImageTool.langstr("UnpackError"), JOptionPane.ERROR_MESSAGE);
			}
		}
		else {		
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			// File operation
			String sText = TIImageTool.langstr("NoContent");
			try {
				if (file.isImageFile()) {
					ViewImageAction va = new ViewImageAction();
					va.setLinks(m_app, getFrame(), m_Settings);
					va.showPicture(file, file.getVolume(), this);
				}
				else {
					String escape = m_Settings.getPropertyString(TIImageTool.ESCAPE);
					if (file.isTextFile()) {
						byte[] content = file.getRecordContent();
						if (Utilities.checkForText(content)==false) {
							int nRet = JOptionPane.showConfirmDialog(getFrame(), TIImageTool.langstr("LotUnprint"), TIImageTool.langstr("Attention"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if (nRet==JOptionPane.NO_OPTION) {
								frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
								return;
							}
						}
						sText = Utilities.sanitizeBytes(content, escape, m_Settings.getPropertyBoolean(TIImageTool.VERBOSE));
					}
					else {
						// Do this only when applicable
						if (file.isBasicFile()) {
							sText = file.listBasic((m_Settings.getPropertyBoolean(TIImageTool.BASICVER)==true)? BasicLine.EX_BASIC : BasicLine.TI_BASIC, escape);
						}
						else {
							byte[] content = null;
							if (file.isProgram()) {
								content = file.getRawContent();
							}
							else {
								content = file.getRecordContent();
							}
							sText = Utilities.hexdump(0, 0, content, content.length, false);
						}
					}
					m_app.showEditTextContent(file, sText);
				}
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(frame, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
			}
			catch (ArithmeticException ax) {
				JOptionPane.showMessageDialog(frame, ax.getMessage(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 				
			}
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(frame, TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
			}
			catch (FormatException fx) {
				JOptionPane.showMessageDialog(frame, fx.toString(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
			}
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	// ===============================================
	//   DnD dialog for no modifiers
	// ===============================================
	
	void openDnDChoiceMenu(Point mouse) {
		// we're in the AWT thread here, no need for an invokeLater
		m_dndmenu = new JPopupMenu();
		m_iDnDMove = new JMenuItem(TIImageTool.langstr("DnDMove"));
		m_iDnDMove.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0));
		m_iDnDMove.setActionCommand(DNDMOVE);
		m_iDnDMove.addActionListener(this);
		m_iDnDCopy = new JMenuItem(TIImageTool.langstr("DnDCopy"));
		m_iDnDCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, 0));
		m_iDnDCopy.setActionCommand(DNDCOPY);
		m_iDnDCopy.addActionListener(this);
		m_iDnDCancel = new JMenuItem(TIImageTool.langstr("Cancel"));
		m_iDnDCancel.setActionCommand(DNDCANCEL);
		
		// Accelerator does not work when the target panel has no focus
		m_iDnDCancel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
		m_iDnDCancel.addActionListener(this);

		m_dndmenu.add(m_iDnDMove);	
		m_dndmenu.add(m_iDnDCopy);
		m_dndmenu.addSeparator();
		m_dndmenu.add(m_iDnDCancel);
		
		m_dndmenu.show(m_panel, (int)mouse.getX(), (int)mouse.getY());	
	}
}
