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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Set;

import de.mizapf.timt.util.Utilities;

/** Caches new sector contents. It contains only new contents and delivers
    them in place of the sector content in the image file.
    
    This is the write cache of TIMT.
    
    Undo/Redo concept:
    
    Each sector has a list of changes by generation.
    
    Sector 42: [0] -> [1] -> [2] -> [3] -> [4]
    Sector 43: [0] -> [1] -> [2]                      unchanged since generation 2
    Sector 44: [0]        -> [2]        -> [4]        changed for generation 2 and 4

    Generation 0 is the sector with its content from the image (file or memory).
    
    There is a current generation pointer, normally the highest number. When
    retrieving the sector contents, the version is delivered with the highest
    generation number below or equal to the current generation.
    
    For an Undo operation, the current generation (>0) is decreased by 1.
    For a Redo operation, the current generation (<max) is increased by 1.
    The DirectoryView must be refreshed each time.
    
    When there is a new generation, the current version is relinked to the 
    new generation, and the previous end of list is dropped with no way to
    restore it again.    
    
*/
public class SectorCache {
	
	SortedMap<Integer,LinkedList<Sector>> m_cache;
	
	private int m_generation;
	private int m_checkpoint;
	private int m_current;
	private int m_maxgen;
	
	private String m_sName; // debugging
	
	SectorCache() {
		m_cache = new TreeMap<Integer,LinkedList<Sector>>();
		m_checkpoint = -1; // For memory images, the checkpoint of last save is -1
	}
	
	void setName(String sName) {
		m_sName = sName;
	}
	
	private Sector getRecentVersion(LinkedList<Sector> seclist) {
		Iterator<Sector> backit = seclist.descendingIterator();
		// System.out.println("Version list length: " + seclist.size() + ", nextgen = " + m_generation);
		Sector sect = null;
		while (backit.hasNext()) {
			Sector sec = backit.next();
			// System.out.println("Cache: Sector " + sec.getNumber() + " (v"  + sec.getGeneration() + ")");
			if (sec.getGeneration() < m_generation) {
				// System.out.println("hit");
				sect = sec;
				break;
			}
		}
		// If we could not find a version earlier than the selected generation, return null
		// i.e. take it from the image
		return sect;
	}
	
	/** Get the contents of a given sector.
	    @param number Sector number
		@return Sector, or null if the sector was never written to after start
		or after a write back
	*/
	Sector read(int number) {
		// Get the history of this sector
		LinkedList<Sector> secversions = m_cache.get(number);
		if (secversions==null) {
			// No history yet
			// System.out.println("Image: Sector " + number);
			return null;
		}
		
		return getRecentVersion(secversions);		
	}
	
	/** Store the contents of the sector at the current generation. Does not
	    change the generation number. If this is the first write operation
	    of this sector, creates a new entry in the map.
	    @param sect Sector
	*/
	void write(Sector sect) {
		// System.out.println("Write sector " + sect.getNumber() + ", gen " + m_generation);
		boolean bNew = true;
		
		// Set the generation
		sect.setGeneration(m_generation);
			
		// Get the history of this sector
		LinkedList<Sector> secversions = m_cache.get(sect.getNumber());

		if (secversions==null) {
			// No history yet
			secversions = new LinkedList<Sector>();
			m_cache.put(sect.getNumber(), secversions);
			// System.out.println("Creating new history for sector " + sect.getNumber());
		}
		else {
			Sector lsect = getRecentVersion(secversions);
			if (lsect != null) {
				if (lsect.getGeneration() == m_generation) {
					// Same generation; overwrite the sector contents
					lsect.modify(sect.getData());
					// System.out.println("Replacing the contents of sector " + sect.getNumber() + " in generation " + m_generation);
					bNew = false;
				}
			}
			else {
				// else we once had a history before undoing. In that case, bNew = true
				secversions.clear();
			}
		}
		if (bNew) {
			// Append new generation
			secversions.add((Sector)sect.clone());
			// System.out.println("Caching a new version (" + m_generation + ") of sector " + sect.getNumber());
		}
		// System.out.println(Utilities.hexdump(sect.getData()));
	}
	
	/** Indicates whether this image has unsaved changes. Note that the
		variable m_generation refers to the next change, not the current.
	*/
	public boolean hasUnsavedEntries() {
		// System.out.println("gen(" + m_sName + ") = " + m_generation + ", last save = " + m_checkpoint);  //#%
		return m_generation > m_checkpoint + 1;
	}

	public void nextGeneration(boolean bNew) {
//		Thread.currentThread().dumpStack();
		// System.out.println("+ nextgen(" + m_sName + ")");
		m_generation++;
		if (bNew) m_maxgen = m_generation;
	}
		
	public void previousGeneration() {
		// System.out.println("- prevgen(" + m_sName + ")");
		m_generation--;
	}

	public void setCheckpoint() {
		m_checkpoint = m_generation;
	}
	
	// Not used yet, and maybe identical to m_generation
	public void setCurrentGeneration(int n) {
		m_current = n;
	}
	
	public boolean canBeRedone() {
		return m_generation < m_maxgen;
	}
}

