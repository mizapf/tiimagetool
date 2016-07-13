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
    
    Copyright 2015 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.basic;

public class CrunchException extends Exception {
	String reason;
	public int line;
	public int pos;
	
	CrunchException(String detail, String what, int lineNumber, int position) {
		super(detail);
		reason = what;
		line = lineNumber;
		pos = position;
	}
}	

