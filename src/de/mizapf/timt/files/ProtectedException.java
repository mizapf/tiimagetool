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
package de.mizapf.timt.files;

public class ProtectedException extends Exception {
	
	public final static int UNSPEC = 0;
	public final static int IMAGE = 1;
	public final static int FILE = 2;
	
	private int m_nType;
	
	public ProtectedException(String sName, int nType) {
		super(sName);
		m_nType = nType; 
	}

	public ProtectedException(String sName) {
		this(sName, UNSPEC);
	}
	
	public int getType() {
		return m_nType;
	}
}
