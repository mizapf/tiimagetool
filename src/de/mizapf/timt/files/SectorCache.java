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
	
	HashMap<Integer,LinkedList<Sector>> m_cache;
	private static int m_nGeneration;
	
	SectorCache() {
		m_cache = new HashMap<Integer,LinkedList<Sector>>();
	}
	
	void setGeneration(int gen) {
		m_nGeneration = gen;
	}
	
	public static void nextGeneration() {
		m_nGeneration++;
	}
	
	public static void sameGeneration() {
		m_nGeneration--;
	}

	public static void init() {
		m_nGeneration = 1;
	}
	
	Sector read(int number) {
		LinkedList<Sector> secversions = m_cache.get(number);
		if (secversions==null) return null;
		System.out.println("Sector " + number + " from cache");
		return secversions.getLast();
	}
	
	void write(Sector sect) {
		LinkedList<Sector> secversions = m_cache.get(sect.getNumber());
		sect.setGeneration(m_nGeneration);

		if (secversions==null) {
			// Create a new history
			secversions = new LinkedList<Sector>();
			m_cache.put(sect.getNumber(), secversions);
		}
		secversions.add((Sector)sect.clone());
		System.out.println("Caching a new version (" + m_nGeneration + ") of sector " + sect.getNumber());
		if (sect.getNumber()<2) new Exception().printStackTrace();

	}
}
/*
	Concept for Sector Cache
	------------------------
	Cache belongs to ImageFormat (for this image)
	
	Key = sector number
	
	Problem: Cut operation joins two images
	
	(Cut operation fails when source image is closed before pasting (IOException, Stream closed);
	source/dest remains unchanged)
	
	
	Problem with insertFile:
	1. sector 1 written by insertFile(Directory.java:504): writeFDIR
	2. sector 0 written by insertFile(Directory.java:510): Volume.update
	3. sector 1 written by commit(Directory.java:631): writeFDIR
	4. sector 0 written by commit(Directory.java:633): Volume.update
	
	PasteAction calls commit (not needed?)
	
*/

