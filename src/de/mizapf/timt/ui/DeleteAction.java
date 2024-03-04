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
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.EOFException;
import java.awt.Dimension;
import java.awt.Cursor;
import java.awt.event.KeyEvent;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class DeleteAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_DELETE;
	}
	
	public int getModifier() {
		return 0;
	}
	
	public String getMenuName() {
		return TIImageTool.langstr("Delete");
	}
	
	public String getActionName() {
		return "DELETE";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();

		Volume vol = dirCurrent.getVolume();
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		JPanel jp = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
		JLabel jl = new JLabel(TIImageTool.langstr("SureToDelete"));
		jl.setFont(TIImageTool.dialogFont);
		jp.add(Box.createVerticalStrut(10));
		jp.add(jl);

		boolean bHasDirs = false;
		
		for (Element selected : dvCurrent.getSelectedEntries()) {
			if (selected instanceof Directory) bHasDirs = true;			
		}

		JCheckBox cb = new JCheckBox(TIImageTool.langstr("DeleteEven"));
		if (bHasDirs) {
			cb.setFont(TIImageTool.subDialogFont);
			jp.add(cb);
		}

		int nRet = JOptionPane.showConfirmDialog(dvCurrent.getFrame(), jp, TIImageTool.langstr("DeleteObjects"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (nRet == JOptionPane.OK_OPTION) {
			try {
				for (Element selected: dvCurrent.getSelectedEntries()) {
					try {
						if (selected instanceof TFile) {
							// System.out.println("Delete " + selected);
							dirCurrent.deleteFile((TFile)selected, true);					
						}
						else {
							if (selected instanceof Directory) {
								dirCurrent.delDir((Directory)selected, cb.isSelected());
							}
							else System.err.println("BUG: Unknown type in delete");
						}
					}
					catch (ImageException ix) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("ImageErrorDelete"), selected.getName()), TIImageTool.langstr("DeleteError"), JOptionPane.ERROR_MESSAGE);
					}
					catch (FileNotFoundException fnfx) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("DeleteNotFound"), fnfx.getMessage()), TIImageTool.langstr("DeleteError"), JOptionPane.ERROR_MESSAGE);
					}
					catch (FormatException fx) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("DeleteNotEmpty"), selected.getName()), TIImageTool.langstr("DeleteError"), JOptionPane.ERROR_MESSAGE);
					}
					catch (IllegalOperationException ix) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), ix.getMessage(), TIImageTool.langstr("DeleteError"), JOptionPane.ERROR_MESSAGE);
					}
				}
				try {
					dirCurrent.commit(true);
					// System.out.println("Commit after delete, changed=" + dirCurrent.getVolume().isModified());
				}
				catch (ImageException ix) {
					System.err.println(String.format(TIImageTool.langstr("ImageErrorUpdate"), dirCurrent.getName()));
				}
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(),  TIImageTool.langstr("DeleteError"), JOptionPane.ERROR_MESSAGE);
				imagetool.closeCurrentView();
			}
			imagetool.refreshPanel(vol);			
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
