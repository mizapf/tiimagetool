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
    
    Copyright 2019 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.files;
import de.mizapf.timt.TIImageTool;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

public class SectorCache {
	
	HashMap<Integer,List<Sector>> m_cache;
	int m_gen;
	
	SectorCache() {
		m_cache = new HashMap<Integer,List<Sector>>();
		m_gen = 0;
	}
	
	void setGeneration(int gen) {
		m_gen = gen;
	}
	
	Sector readSector(int number) {
		return null;
	}
	
	void writeSector(Sector sect) {
		List<Sector> secversions = m_cache.get(sect.getNumber());
		if (secversions==null) {
			// Create a new history
			List<Sector> list = new LinkedList<Sector>();
			sect.setGeneration(m_gen);
			list.add(sect);
			m_cache.put(sect.getNumber(), list);
			System.out.println("Caching a new version (" + m_gen + ") of sector " + sect.getNumber());
		}
		
		// Walk along the list to find the recent version
	}
}


