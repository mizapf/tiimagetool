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
	
	JMenuItem m_iCutm;
	JMenuItem m_iCopym;
	JMenuItem m_iPastem;
	JMenuItem m_iDeletem;
	JMenuItem m_iRenamem;
	JMenuItem m_iSelectm;
	JMenuItem m_iInsert;
	JMenuItem m_iInsertBinary;
	JMenuItem m_iInsertContent;
	JMenuItem m_iInsertRemote;
	JMenuItem m_iCreateDirectory;	
	JMenuItem m_iCreateArchive;
		
	public EditMenu(JFrame frm, TIImageTool app) {
		super("Edit");
		m_frmMain = frm;
		m_app = app;

		setFont(TIImageTool.dialogFont);
		
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
		addSeparator();	
		m_iSelectm = createMenuItem(new SelectAllAction());
		add(m_iSelectm);
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
	}	
		
	public void activateMenuItems(boolean bOpenImage, boolean bDirPossible, boolean bClipboard) {
				
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
		
		m_iInsert.setEnabled(bOpenImage);
		m_iInsertBinary.setEnabled(bOpenImage);
		m_iInsertContent.setEnabled(bOpenImage);
		m_iInsertRemote.setEnabled(bOpenImage);
		m_iCreateDirectory.setEnabled(bOpenImage && bDirPossible);
		m_iCreateArchive.setEnabled(bOpenImage);
	}	
	
	private JMenuItem createMenuItem(Activity act) {
		JMenuItem mi = new JMenuItem(act.getMenuName());
		mi.setActionCommand(act.getActionName());
		mi.addActionListener(m_app);
		m_app.registerActivity(act);
		mi.setFont(TIImageTool.dialogFont);
		if (act.getKeyCode()!=0) mi.setAccelerator(KeyStroke.getKeyStroke(act.getKeyCode(), act.getModifier()));
		return mi;
	}
	
	public void setPaste(boolean state) {
		m_iPastem.setEnabled(state);
	}	
}

