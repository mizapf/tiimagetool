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
    
    Copyright 2013 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import de.mizapf.timt.TIImageTool;
import java.io.PrintStream;
import java.io.FileOutputStream;

public class LogStream extends PrintStream {

	private ContentFrame m_cf;
	
	public LogStream(FileOutputStream fos) {
		super(fos);
		m_cf = null;
	}
	
	public LogStream(FileOutputStream fos, ContentFrame cf) {
		super(fos);
		m_cf = cf;
	}

	@Override
	public void print(String s) {
		super.print(s);
		if (m_cf != null) m_cf.append(s);
	}
	
	@Override
	public void println(String s) {
		super.println(s);
		if (m_cf != null) {
			m_cf.append(s);
			m_cf.append("\n");
		}
	}

	@Override
	public void print(Object obj) {
		super.print(obj);
		if (m_cf != null) {
			m_cf.append(obj.toString());
		}
	}

	@Override
	public void println(Object obj) {
		super.println(obj); // calls print(String)
		if (m_cf != null) {
			m_cf.append("\n");
		}
	}
	
	@Override
	public void println() {
		super.println();
		if (m_cf != null) {
			m_cf.append("\n");
		}
	}
	
	public void register(ContentFrame cf) {
		m_cf = cf;
	}	
	
	public ContentFrame getContentFrame() {
		return m_cf;
	}
}
