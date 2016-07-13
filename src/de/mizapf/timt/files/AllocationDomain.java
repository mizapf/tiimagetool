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

import java.util.List;
import java.util.HashSet;

public class AllocationDomain {
	
	int m_nIndex;
	HashSet<String> m_lst;
	
	public AllocationDomain(int nIndex) {
		m_nIndex = nIndex;
		m_lst = new HashSet<String>();
	}
	
	public int getAU() {
		return m_nIndex;
	}

	public void addEntity(String sEntity) {
		m_lst.add(sEntity);
	}
	
	public boolean isUnassigned() {
		return m_lst.size()==0;
	}
	
	public boolean isFaulty() {
		return m_lst.size()!=1;
	}
	
	public String[] getAllocations() {
		String[] as = new String[m_lst.size()];
		return m_lst.toArray(as);
	}
	
	public String toString() {
		return "AU=" + m_nIndex + ": " + m_lst;
	}
}