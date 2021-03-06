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

import java.io.*;

/** Used with "Open recent". */  
public class OpenOneImageAction extends OpenImageAction {

	private String m_fileName;
	private int m_number;
	
	@Override
	public int getKeyCode() {
		return 0;
	}

	public OpenOneImageAction(String file, int number) {
		m_fileName = file;
		m_number = number;
	}
	
	@Override
	public String getMenuName() {
		return m_fileName;
	}
	
	@Override
	public String getActionName() {
		return "OPENIMAGE" + m_number;
	}
		
	// Used from the Menu
	@Override
	public void go() {
		java.io.File[] selectedfiles = new java.io.File[1];
		selectedfiles[0] = new File(m_fileName);		
		open(selectedfiles);
	}
}
