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
import java.io.*;
import java.awt.Dimension;
import java.awt.event.KeyEvent;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class NewFloppyImageAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_N;
	}
	
	public String getMenuName() {
		return TIImageTool.langstr("FloppyImage") + "...";
	}
	
	public String getActionName() {
		return "NEWFIMAGE";
	}
	
	public void go() {
		NewFloppyImageDialog newimage = new NewFloppyImageDialog(m_parent);

		try {
			newimage.createGui(imagetool.boldFont);
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		newimage.setVisible(true);

		Volume newVolume = null;
		
		if (newimage.confirmed()) {
			FloppyFileSystem ffs = new FloppyFileSystem(newimage.getParameters());
			newVolume = new Volume(ffs, imagetool.nextUnnamedIndex());
			Directory root = newVolume.getRootDirectory();					
			imagetool.addDirectoryView(root);
			newVolume.nextGeneration(true);
		}
	}
}
