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

import java.io.File;
import javax.swing.filechooser.FileFilter;

import de.mizapf.timt.TIImageTool;

public class ImageFileFilter extends FileFilter {
	
	String[] m_asSuffix;
	
	ImageFileFilter(String sSuffixes) {
		m_asSuffix = sSuffixes.split(" |,");
	}
	
	public String getDescription() {
		return TIImageTool.langstr("FilterImg");
	}

	public boolean accept(File f) {
		if (f.isDirectory()) return true;
		
		String sName = f.getName().toLowerCase();
		int sufpnt = sName.lastIndexOf(".") + 1;
		// - stands for no suffix
		String suffix;
		if (sufpnt == 0) suffix = "-";
		else suffix = sName.substring(sufpnt);
	
		for (String s : m_asSuffix) {
			if (s.equalsIgnoreCase(suffix)) return true;
		}
		return false;
	}
}


