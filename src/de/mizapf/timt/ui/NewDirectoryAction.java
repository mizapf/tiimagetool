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
import java.io.IOException;
import java.awt.font.*;
import java.awt.Graphics2D;
import java.awt.FontMetrics;

import java.awt.Cursor;
import java.awt.Dimension;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class NewDirectoryAction extends Activity {

	public String getMenuName() {
		return "New directory ...";
	}
	
	public String getActionName() {
		return "NEWDIR";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		DirectoryPanel dp = dvCurrent.getPanel();

		Volume vol = dvCurrent.getVolume();
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		// Create a simple dialog
		FontMetrics fm = ((Graphics2D)(imagetool.getMainFrame().getGraphics())).getFontMetrics(TIImageTool.dialogFont);
		FontRenderContext frc = ((Graphics2D)(imagetool.getMainFrame().getGraphics())).getFontRenderContext();
		LineMetrics lm = TIImageTool.dialogFont.getLineMetrics("DIRECTORY", 0, 2, frc);
		
		int nFieldWidth = fm.stringWidth("DIRWITH10CA");
		int nFieldHeight = (int)Math.ceil(lm.getHeight()*1.5);
		
		JPanel jp = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.X_AXIS));
		jp.add(new JLabel("New directory name:"));
		jp.add(Box.createHorizontalStrut(10));
		jp.add(Box.createHorizontalGlue());
		JTextField jtName = new JTextField();
		jtName.setPreferredSize(new Dimension(nFieldWidth, nFieldHeight));
		jtName.setMaximumSize(new Dimension(nFieldWidth, nFieldHeight));
		jp.add(jtName);
		
		boolean ok = true;

		int confirm = JOptionPane.showConfirmDialog(m_parent, jp, "Create new directory", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (confirm == JOptionPane.OK_OPTION) {
			String sName = jtName.getText().trim();
			if (vol.isFloppyImage()) {
				if (!dirCurrent.isRootDirectory()) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Floppy file systems can only have directories in the root directory.", "Illegal operation", JOptionPane.ERROR_MESSAGE);
					ok = false;
				}
				if (vol.getRootDirectory().getDirectories().length>2) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Cannot have more than three directories", "Illegal operation", JOptionPane.ERROR_MESSAGE);
					ok = false;
				}
			}
			
			if (ok && !Directory.validName(sName)) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Illegal name for new directory: \"" + sName + "\".", "Illegal operation", JOptionPane.ERROR_MESSAGE);
				ok = false;
			}

			if (ok) {
				try {
					dirCurrent.createSubdirectory(sName, true);
				}
				catch (ImageFullException ifx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "No space to create new directory: \"" + sName + "\".", "Illegal operation", JOptionPane.ERROR_MESSAGE);
				}
				catch (Exception ex) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Failed to create new directory: \"" + sName + "\" (" + ex.getClass().getName() + ").", "Illegal operation", JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
			}
		}
		
		imagetool.refreshPanel(vol);			
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
