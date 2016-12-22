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

import de.mizapf.timt.TIImageTool;

public class CrunchException extends Exception {
	public int reason;
	public int line;
	public int pos;
	public int textline;

    public static final int UNMATCHED_QUOTES = 0; 
	public static final int MULTIPLE_VAR_NUM = 1; 
	public static final int ILLEGAL_IN_PRG = 2; 
	public static final int UNRECOGNIZED = 3;
	public static final int NOLINENO = 4;
	public static final int BADLINENO = 5;	
	public static final int NOSUB = 6;
	public static final int TOOLONG = 7;
	public static final int TOOSHORT = 8;
	
	String[] text = { 
		TIImageTool.langstr("CrunchUnmatched"), 
		TIImageTool.langstr("CrunchSyntax"),
		TIImageTool.langstr("CrunchCommand"),
		TIImageTool.langstr("CrunchUnrecog"),
		TIImageTool.langstr("CrunchNoNumber"),
		TIImageTool.langstr("CrunchBadNumber"),
		TIImageTool.langstr("CrunchMissingName"),
		TIImageTool.langstr("CrunchTooLong"),
		TIImageTool.langstr("CrunchTooShort") };

	CrunchException(int what, String detail, int lineNumber, int position, int tline) {
		super(detail);
		reason = what;
		line = lineNumber;
		pos = position;
		textline = tline;
	}
	
	public String getReason() {
		return text[reason];
	}
}	

