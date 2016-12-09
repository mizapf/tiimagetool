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

public class DeleteAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_DELETE;
	}
	
	public int getModifier() {
		return 0;
	}
	
	public String getMenuName() {
		return imagetool.langstr("Delete");
	}
	
	public String getActionName() {
		return "DELETE";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();

		Volume vol = dirCurrent.getVolume();
		String sText = "no content";
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		JPanel jp = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
		JLabel jl = new JLabel("Are you sure you want to delete the marked objects?");
		jp.add(Box.createVerticalStrut(10));
		jp.add(jl);

		boolean bHasDirs = false;
		
		for (Element selected : dvCurrent.getSelectedEntries()) {
			if (selected instanceof Directory) bHasDirs = true;			
		}

		JCheckBox cb = new JCheckBox("Delete directories even if not empty");
		if (bHasDirs) jp.add(cb);

		int nRet = JOptionPane.showConfirmDialog(dvCurrent.getFrame(), jp, "Delete objects", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (nRet == JOptionPane.OK_OPTION) {
			for (Element selected: dvCurrent.getSelectedEntries()) {
				if (selected instanceof TFile) {
					try {
						dirCurrent.deleteFile((TFile)selected, true);
					}
					catch (FileNotFoundException fnfx) {
						System.err.println("File " + fnfx.getMessage() + " not found but selected for deletion.");
					}
					catch (IOException iox) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "I/O error when trying to delete " + selected.getName(), "Delete error", JOptionPane.ERROR_MESSAGE);
					}
					catch (ImageException ix) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Image error when trying to delete " + selected.getName() + ": " + ix.getMessage(), "Delete error", JOptionPane.ERROR_MESSAGE);
					}
					catch (ProtectedException px) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Cannot delete file " + selected.getName() + " from a read-only directory.", "Delete error", JOptionPane.ERROR_MESSAGE);
					}
				}
				if (selected instanceof Directory) {
					try {
						dirCurrent.delDir((Directory)selected, cb.isSelected());
					}
					catch (FileNotFoundException fnfx) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Selected directory " + selected.getName() + " not found anymore.", "Delete error", JOptionPane.ERROR_MESSAGE);
					}
					catch (FormatException fx) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Directory " + selected.getName() + " is not empty.", "Delete error", JOptionPane.ERROR_MESSAGE);
					}
					catch (IOException iox) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "I/O error when trying to delete " + selected.getName(), "Delete error", JOptionPane.ERROR_MESSAGE);
					}
					catch (ImageException ix) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Image error when trying to delete " + selected.getName(), "Delete error", JOptionPane.ERROR_MESSAGE);
					}
					catch (ProtectedException px) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Cannot delete directory " + selected.getName() + " from read-only directory." , "Delete error", JOptionPane.ERROR_MESSAGE);
					}
					catch (IllegalOperationException ix) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), ix.getMessage(), "Delete error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
			try {
				dirCurrent.commit(true);
			}
			catch (FileNotFoundException fnfx) {
				if (fnfx.getMessage().indexOf("ermission")!=-1) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "No permission to write to image", "Write error", JOptionPane.ERROR_MESSAGE); 
				}
				else {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Cannot open the file/device for writing", "Write error", JOptionPane.ERROR_MESSAGE);
				}
				try {
					imagetool.reloadVolume(vol);
				}
				catch (Exception ex) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Cannot reload volume.", "Read error", JOptionPane.ERROR_MESSAGE); 
				}
			}
			catch (IOException iox) {
				System.err.println("IO error when trying to update directory " + dirCurrent.getName());
			}
			catch (ImageException ix) {
				System.err.println("Image error when trying to update directory " + dirCurrent.getName());
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), "Delete error", JOptionPane.ERROR_MESSAGE);
				imagetool.closeCurrentView();
			}
			dvCurrent.refreshAll();			
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
