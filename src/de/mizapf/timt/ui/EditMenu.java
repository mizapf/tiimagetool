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
import javax.swing.*;
import de.mizapf.timt.files.Volume;
import de.mizapf.timt.files.Directory;
import de.mizapf.timt.TIImageTool;

public class EditMenu extends JMenu {
	
	JFrame m_frmMain;
	TIImageTool m_app;
	
	JMenuItem m_iUndo;
	JMenuItem m_iRedo;
	
	JMenuItem m_iCutm;
	JMenuItem m_iCopym;
	JMenuItem m_iPastem;
	JMenu     m_mOperations;
	JMenuItem m_iDeletem;
	JMenuItem m_iRenamem;
	JMenuItem m_iRenamevm;
	JMenuItem m_iSelectm;
	JMenuItem m_iInsert;
	JMenuItem m_iInsertBinary;
	JMenuItem m_iInsertContent;
	JMenuItem m_iInsertRemote;
	JMenuItem m_iInsertEmulate;
	JMenuItem m_iCreateDirectory;	
	JMenuItem m_iCreateArchive;
	
	// Actions submenu
	JMenuItem m_iViewFIB;
	JMenuItem m_iViewText;
	JMenuItem m_iViewImage;
	JMenuItem m_iViewDump;
	JMenuItem m_iAssemble;
	JMenuItem m_iLink;
	JMenuItem m_iDisass;
	JMenuItem m_iGPLDisass;
	JMenuItem m_iList;
	JMenuItem m_iSaveTfi;
	JMenuItem m_iSaveDump;
	JMenuItem m_iSendRem;
	
	public EditMenu(JFrame frm, TIImageTool app) {
		super(TIImageTool.langstr("Edit"));
		m_frmMain = frm;
		m_app = app;
		
		m_iUndo = createMenuItem(new UndoAction());
		add(m_iUndo);
		m_iRedo = createMenuItem(new RedoAction());
		add(m_iRedo);
		addSeparator();
		
		m_iCutm = createMenuItem(new CutAction());
		add(m_iCutm);
		m_iCopym = createMenuItem(new CopyAction());
		add(m_iCopym);
		m_iPastem = createMenuItem(new PasteAction());
		add(m_iPastem);
		addSeparator();
		m_iDeletem = createMenuItem(new DeleteAction());
		add(m_iDeletem);
		addSeparator();
		m_iRenamem = createMenuItem(new RenameAction());
		add(m_iRenamem);
		m_iRenamevm = createMenuItem(new RenameVolumeAction());
		add(m_iRenamevm);
		addSeparator();	
		m_iSelectm = createMenuItem(new SelectAllAction());
		add(m_iSelectm);
		addSeparator();
		
		m_mOperations = new JMenu(TIImageTool.langstr("Menu.Edit.Actions"));
		m_iViewFIB = m_app.createMenuItem(new ViewFIBAction());
		m_mOperations.add(m_iViewFIB);
		m_iViewText =  m_app.createMenuItem(new ViewTextAction());
		m_mOperations.add(m_iViewText);
		m_iViewImage =  m_app.createMenuItem(new ViewImageAction());
		m_mOperations.add(m_iViewImage);
		m_iViewDump =  m_app.createMenuItem(new ViewDumpAction());
		m_mOperations.add(m_iViewDump);
		m_iAssemble =  m_app.createMenuItem(new AssembleAction());
		m_mOperations.add(m_iAssemble);
		m_iLink =  m_app.createMenuItem(new LinkAction());
		m_mOperations.add(m_iLink);
		m_iDisass =  m_app.createMenuItem(new DisassembleAction());
		m_mOperations.add(m_iDisass);
		m_iGPLDisass = m_app.createMenuItem(new GPLDisassembleAction());
		m_mOperations.add(m_iGPLDisass);
		m_iList = m_app.createMenuItem(new ListAction());
		m_mOperations.add(m_iList);
		m_iSaveTfi = m_app.createMenuItem(new SaveTFIAction());
		m_mOperations.add(m_iSaveTfi);
		m_iSaveDump = m_app.createMenuItem(new SavePlainAction());
		m_mOperations.add(m_iSaveDump);
		m_iSendRem = m_app.createMenuItem(new ExportRemoteAction());
		m_mOperations.add(m_iSendRem);
		
		add(m_mOperations);
		m_mOperations.setEnabled(false);
		
		addSeparator();		
		m_iCreateDirectory = createMenuItem(new NewDirectoryAction());
		add(m_iCreateDirectory);
		m_iCreateArchive = createMenuItem(new CreateArchiveAction());
		add(m_iCreateArchive);
		addSeparator();
		
		m_iInsert = createMenuItem(new ImportFilesAction());
		add(m_iInsert);
		m_iInsertBinary = createMenuItem(new ImportBinaryAction());
		add(m_iInsertBinary);
		m_iInsertContent = createMenuItem(new ImportContentAction());
		add(m_iInsertContent);
		m_iInsertRemote = createMenuItem(new ImportRemoteAction());
		add(m_iInsertRemote);		
		m_iInsertEmulate = createMenuItem(new ImportEmulateAction());
		add(m_iInsertEmulate);		
	}	
		
	public void activateMenuItems(boolean bUndo, boolean bRedo, boolean bOpenImage, boolean bDirPossible, boolean bClipboard, boolean bSerial, boolean bHFDC) {
				
/*		if (bOpenImage) {
			Volume vol = m_View.getVolume();
			bDirPossible = true;
			if (vol.isFloppyImage()) {
				Directory dirCurrent = m_View.getDirectory(); 
				if (!dirCurrent.isRootDirectory()) bDirPossible = false;
				if (dirCurrent.getDirectories().length>2) bDirPossible = false;
			}			
			bSelected = true;
		}
		
		boolean bClipboard = !m_app.clipboardIsEmpty();
*/		
		bClipboard = true; // We have to lock paste to true, otherwise ctrl-v from outside does not work
		m_iCutm.setEnabled(bOpenImage);
		m_iCopym.setEnabled(bOpenImage);
		m_iPastem.setEnabled(bClipboard && bOpenImage);
		m_iDeletem.setEnabled(bOpenImage);
		m_iSelectm.setEnabled(bOpenImage);
		m_iRenamem.setEnabled(bOpenImage);
		m_iRenamevm.setEnabled(bOpenImage);
		
		m_iInsert.setEnabled(bOpenImage);
		m_iInsertBinary.setEnabled(bOpenImage);
		m_iInsertContent.setEnabled(bOpenImage);
		m_iInsertRemote.setEnabled(bOpenImage && bSerial);
		m_iInsertEmulate.setEnabled(bOpenImage && bHFDC);
		m_iCreateDirectory.setEnabled(bOpenImage && bDirPossible);
		m_iCreateArchive.setEnabled(bOpenImage);
		
		m_iUndo.setEnabled(bUndo);
		m_iRedo.setEnabled(bRedo);
	}	
	
	public void activateActionMenu(boolean bActive) {
		m_mOperations.setEnabled(bActive);
	}
	
	private JMenuItem createMenuItem(Activity act) {
		JMenuItem mi = new JMenuItem(act.getMenuName());
		mi.setActionCommand(act.getActionName());
		mi.addActionListener(m_app);
		m_app.registerActivity(act);
		if (act.getKeyCode()!=0) mi.setAccelerator(KeyStroke.getKeyStroke(act.getKeyCode(), act.getModifier()));
		return mi;
	}
	
	public void setPaste(boolean state) {
		m_iPastem.setEnabled(state);
	}	
}

