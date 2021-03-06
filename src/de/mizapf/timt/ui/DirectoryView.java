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
	boolean			m_bFocused;
	JFrame			m_frmOwn;
	
	JMenuBar		m_mbar;
	JMenu			m_mFile;
	EditMenu		m_mEdit;
	
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
	JMenuItem m_iViewUtil;
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
	
	public DirectoryView(Directory dir, boolean bAttached, TIImageTool app) {
		m_dirCurrent = dir;
		m_bAttached = bAttached;
		m_app = app;
		m_panel = new DirectoryPanel(this);
			
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
		m_iViewUtil = app.createMenuItem(new ViewUtilAction());
		m_iDisass = app.createMenuItem(new DisassembleAction());
		m_iGPLDisass = app.createMenuItem(new GPLDisassembleAction());
		m_iList = app.createMenuItem(new ListAction());
		m_iSaveTfi = app.createMenuItem(new SaveTFIAction()); 
		m_iSaveDump = app.createMenuItem(new SavePlainAction());
		m_iSendRem = app.createMenuItem(new ExportRemoteAction());
		m_iChangeDirectory = m_app.createMenuItem(new ChangeDirectoryAction());
		m_iToggleEmulate = m_app.createMenuItem(new ToggleEmulateAction());
		m_iExportEmulate = m_app.createMenuItem(new ExportEmulateAction());
		m_mEdit = app.getEditMenu();
		
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
		m_frmOwn = new JFrame(m_dirCurrent.getVolume().getShortImageName());
		m_frmOwn.setIconImage(m_app.m_frameicon.getImage());
		m_frmOwn.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		m_frmOwn.addWindowListener(this);
		Container cnt = m_frmOwn.getContentPane();
		cnt.setLayout(new BoxLayout(cnt, BoxLayout.Y_AXIS));
		cnt.add(m_panel);

		m_mbar = new JMenuBar();
		m_mFile = new JMenu(m_app.langstr("File"));
		
		m_iClose = new JMenuItem(m_app.langstr("Attach"));
		m_iClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
		m_iClose.addActionListener(this);
		m_mFile.add(m_iClose);
		
		m_mbar.add(m_mFile);
		m_mEdit = new EditMenu(m_frmOwn, m_app);
		m_frmOwn.setJMenuBar(m_mbar);
		m_mbar.add(m_mEdit);
		
		m_frmOwn.setSize(m_app.getFrameSize());

		m_app.removeTab(m_panel);
		cnt.setBackground(BACK);		

		Point loc = m_app.getMainFrame().getLocationOnScreen();		
		m_frmOwn.setLocation(loc.x+20, loc.y+20);
		m_frmOwn.setVisible(true);
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
		m_app.attachView(vol.getImageName(), this, false);
		m_frmOwn = null;
		m_mEdit = m_app.getEditMenu();
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
	
	void enterDirectory(Directory dir) {
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
			pa.setLinks(m_app, getFrame());
			
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
		pa.setLinks(m_app, getFrame());
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
	
	// ================================================================
	//   ActionListener
	// ================================================================

	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand() == DNDCOPY) {
		//	System.out.println(DNDCOPY);
			m_app.markForCut(false);
			PasteAction pa = new PasteAction();
			pa.setLinks(m_app, getFrame());
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
				pa.setLinks(m_app, getFrame());
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
				else
					// Attach the stand-alone frame
					reattach();
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
		
		boolean bClipboard = m_app.clipboardNotEmpty();	
		m_mEdit.activateMenuItems(true, bDirPossible, bClipboard, m_app.offersSerialConnection(), getVolume().isHFDCImage());
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
		mnew.add(m_iArchive);  // FIXME: Exception, disabled
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
	
	void openEntryContextMenu(Component whereclicked, int xpos, int ypos, boolean bWithShift, boolean bWithControl) {
		// we're in the AWT thread here, no need for an invokeLater
		m_ctxmenu = new JPopupMenu();

		boolean bCut = false; //true;
		boolean bCopy = false; //true;
		boolean bPaste = false; //true;
		boolean bRename = false; //true;
		
		boolean bText = false; //true;
		boolean bDump = false; //true;
		boolean bUtil = false; //true;
		boolean bImage = false; //true;
		boolean bDisass = false; //true;
		boolean bGDisass = false; //true;
		boolean bBasic = false; //true;
		boolean bSaveRem = false; //true;
		boolean bSaveTfi = false; //true;
		boolean bSaveDump = false; //true;
		boolean bDelete = false; //true;
		boolean bArchive = false;

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
			m_ctxmenu.add(m_iViewUtil); 
			m_ctxmenu.add(m_iDisass); 
			m_ctxmenu.add(m_iGPLDisass); 
			m_ctxmenu.add(m_iList); 
		}
		m_ctxmenu.addSeparator();
		m_ctxmenu.add(m_iSaveTfi);
		m_ctxmenu.add(m_iSaveDump);
		m_ctxmenu.add(m_iSendRem);
		if (!isEmulate) {
			m_ctxmenu.addSeparator();
			m_ctxmenu.add(m_iArchive);
		}

		if (selected.size() > 0) {
			bArchive = true;
			bDump = true;
			bSaveRem = true;
			bSaveDump = true;
			bSaveTfi = true;	
			bUtil = true;
			bImage = true;
			bCut = true;
			bCopy = true;
			bBasic = true;
		}
		
		for (Element el:selected) {
			if (el instanceof TFile) {
				TFile file = (TFile)el;
				try {
					if (!file.isBasicFile()) {
						bBasic = false;
						if (!file.isProgram()) {
							bGDisass = false;
							if (!file.isTaggedObjectCodeFile()) bDisass = false;
						}
					}
				}
				catch (Exception iox) {
					bBasic = false;
					bDisass = false;
					bGDisass = false;
				}
				try {
					if (!file.hasUtilFormat()) bUtil = false;
				}
				catch (Exception ex) {
					bUtil = false;
				}
				
				if (!file.isTextFile()) {
					bText = false;
					bDisass = true;
					bGDisass = true;
				}
				else {
					bText = true;
				}
				
				if (!file.isImageFile()) bImage = false;
			}
			else {
				bText = false;
				bDump = false;
				bUtil = false;
				bDisass = false;
				bGDisass = false;
				bBasic = false;
				bSaveRem = false;
				bSaveDump = false;
				bSaveTfi = false;
				bImage = false;
				bArchive = false;
			}
		}
		
		if (bBasic) {
			bDisass = false;
			bGDisass = false;
			bImage = false;
		}
		
		if (!m_app.offersSerialConnection() || selected.size()>1) bSaveRem = false;

		m_iPaste.setEnabled(true /*m_app.clipboardNotEmpty() */);
		
		m_iCut.setEnabled(bCut);
		m_iCopy.setEnabled(bCopy);
		m_iPaste.setEnabled(bPaste);
		m_iDelete.setEnabled(bCut);
		m_iRename.setEnabled(bCut);
		m_iSelect.setEnabled(bCut);
		m_iViewText.setEnabled(bText);
		m_iViewImage.setEnabled(bImage);
		m_iViewUtil.setEnabled(bUtil);
		m_iViewDump.setEnabled(bDump);
		m_iViewFIB.setEnabled(bDump);
		m_iDisass.setEnabled(bDisass);
		m_iGPLDisass.setEnabled(bGDisass);
		m_iList.setEnabled(bBasic);
		m_iSendRem.setEnabled(bSaveRem);
		m_iSaveTfi.setEnabled(bSaveTfi);
		m_iSaveDump.setEnabled(bSaveDump);
		m_iArchive.setEnabled(bArchive);		

//		m_dvSelected.backpaintEntryLines();
		
		m_ctxmenu.show(whereclicked, xpos, ypos);		
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
					list.addSelectionInterval(index, index);
					openEntryContextMenu(list, act.getX(), act.getY(), act.isShiftDown(), act.isControlDown());
				}
			}
		}
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
					va.setLinks(m_app, getFrame());
					va.showPicture(file, file.getVolume(), this);
				}
				else {
					String escape = m_app.getPropertyString(TIImageTool.ESCAPE);
					if (file.isTextFile()) {
						byte[] content = file.getRecordContent();
						if (Utilities.checkForText(content)==false) {
							int nRet = JOptionPane.showConfirmDialog(getFrame(), TIImageTool.langstr("LotUnprint"), TIImageTool.langstr("Attention"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if (nRet==JOptionPane.NO_OPTION) {
								frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
								return;
							}
						}
						sText = Utilities.sanitizeText(content, escape, m_app.getPropertyBoolean(TIImageTool.VERBOSE));
					}
					else {
						// Do this only when applicable
						if (file.isBasicFile()) {
							sText = file.listBasic((m_app.getPropertyBoolean(TIImageTool.BASICVER)==true)? BasicLine.EX_BASIC : BasicLine.TI_BASIC, escape);
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
					m_app.showTextContent(file.getName(), sText);
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
