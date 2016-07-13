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
import java.util.ArrayList;
import java.util.Iterator;

public class AllocationGapList {
	
	String m_sEntity;
	List<Integer> m_lst;
	
	public AllocationGapList(String sName) {
		m_sEntity = sName;
		m_lst = new ArrayList<Integer>();
	}
	
	public String getName() {
		return m_sEntity;
	}

	public void addAU(int nAU) {
		m_lst.add(new Integer(nAU));
	}
	
	public void setList(Integer[] anAU) {
		for (int au:anAU) {
			addAU(au);
		}
	}
	
	public int size() {
		return m_lst.size();
	}
	
	public int[] getAllocationGaps() {
		int[] aGap = new int[m_lst.size()];
		Iterator<Integer> it = m_lst.iterator();
		int i=0;
		while (it.hasNext()) {
			aGap[i++] = it.next().intValue();
		}
		return aGap;
	}
	
	public String toString() {
		return "File " + m_sEntity + " has allocation gaps " + m_lst;
	}
}