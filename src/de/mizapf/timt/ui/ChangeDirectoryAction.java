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

import de.mizapf.timt.files.*;
import javax.swing.*;
import java.io.*;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.Cursor;
import java.util.List;
import de.mizapf.timt.TIImageTool;

public class ChangeDirectoryAction extends Activity {

	public String getMenuName() {
		return imagetool.langstr("Enter");
	}
	
	public String getActionName() {
		return "CHANGEDIR";
	}
		
	// Used from the Menu
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();

		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		Element elFirst = dvCurrent.getClickedElement();
		
		Directory dir = null;
		
		try {
			if (elFirst instanceof TFile) {
				// Archive file
				dir = ((TFile)elFirst).unpackArchive();
			}
			else {
				dir = (Directory)elFirst;
			}
			dvCurrent.enterDirectory(dir);
		}
		catch (IllegalOperationException iox) {
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), iox.getMessage(), "Illegal operation", JOptionPane.ERROR_MESSAGE);
		}
		catch (FormatException fx) {
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), fx.getMessage(), "Error during unpacking", JOptionPane.ERROR_MESSAGE);
		}		
		catch (IOException iox) {
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), iox.getMessage(), "Error during unpacking", JOptionPane.ERROR_MESSAGE);
		}
		catch (ImageException ix) {
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), ix.getMessage(), "Error during unpacking", JOptionPane.ERROR_MESSAGE);
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
