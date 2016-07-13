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

public abstract class Element implements Comparable<Element> {

	/** Time of creation. */
	protected Time	m_tCreation;

	/** Name of the file or directory. */
	protected String m_sName;

	/** Protection flag. */
	protected boolean m_bProtected;

	/** Directory that contains this element. */
	protected Directory m_dirParent;
	
	/** Delivers the required sectors. */
	public abstract int getAllRequiredSectors(int nAUSize);
	
	/** Also used when copying or moving elements. */
	public void setContainingDirectory(Directory dir) {
		m_dirParent = dir;
	}
	
	public Directory getContainingDirectory() {
		return m_dirParent;
	}
	
	public final String getName() {
		return m_sName;
	}

	public Time getCreationTime() {
		return m_tCreation;
	}
	
	public final void setName(String sName) {
		m_sName = sName;
	}

	public boolean isProtected() {
		return m_bProtected;
	}

	public int compareTo(Element e) {
		return getName().trim().compareTo(e.getName().trim());
	}
	
	public Volume getVolume() {
		return m_dirParent.getVolume();
	}
	
	// TODO: implement equals (must consider java.io.file which contains the image) 
	
	public String toString() {
		return getName();
	}
	
	public String getPathname() {
		StringBuilder sb = new StringBuilder();
		if (m_dirParent != null && !m_dirParent.isRootDirectory()) sb.append(m_dirParent.getName()).append(".");
		sb.append(m_sName);
		return sb.toString(); 
	}
}


