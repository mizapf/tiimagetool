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
    generation number below the current generation.
    
    For an Undo operation, the current generation (>0) is decreased by 1.
    For a Redo operation, the current generation (<max) is increased by 1.
    The DirectoryView must be refreshed each time.
    
    When there is a new generation, the current version is relinked to the 
    new generation, and the previous end of list is dropped with no way to
    restore it again.    
    
    read --+--> null if there were no changes to the sector in the image
           +--> Sector (clone) whose generation is the highest below m_generation
           
    write--+--> new entry in the list of this sector, with gen set to m_generation
           +--> overwritten entry when gen==m_generation
    
    Undo can only succeed on performed transactions, which is declared by
    calling next_generation.
    
*/
public class SectorCache {
	
	SortedMap<Integer,LinkedList<Sector>> m_cache;
	
	private int m_generation;
	private int m_checkpoint;
	private int m_current;
	private int m_maxgen;
	
	private final static boolean DEBUG = false;
	
	private String m_sName; // debugging
	
	SectorCache() {
		m_cache = new TreeMap<Integer,LinkedList<Sector>>();
		m_checkpoint = -1; // For memory images, the checkpoint of last save is -1
	}
	
	void setName(String sName) {
		m_sName = sName;
	}
	
	private Sector getRecentVersion(LinkedList<Sector> seclist, boolean withCurrent) {
		Iterator<Sector> backit = seclist.descendingIterator();
		if (DEBUG) System.out.println("Version list length: " + seclist.size() + ", nextgen = " + m_generation);
		Sector sect = null;
		while (backit.hasNext()) {
			Sector sec = backit.next();
			if (DEBUG) System.out.print("Cache: Sector " + sec.getNumber() + " (v"  + sec.getGeneration() + "), withCurrent = " + withCurrent + ": ");
			
			// Must be "<" for a working Undo
			// Undos are done from committed changes, so nextgen is higher than
			// the latest change of any sector
			// With <=, the latest change will still be found, although it was "undone"
			// TODO: Check whether error conditions are really cleanly undone
			if ((sec.getGeneration() < m_generation) 
				|| (withCurrent && (sec.getGeneration() == m_generation))) {
				if (DEBUG) System.out.println("hit");
				sect = sec;
				break;
			}
			else {
				if (DEBUG) System.out.println("skip");
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
		
		return getRecentVersion(secversions, false);		
	}
	
	/** Store the contents of the sector at the current generation. Does not
	    change the generation number. If this is the first write operation
	    of this sector, creates a new entry in the map.
	    @param sect Sector
	*/
	void write(Sector sect) {
		if (DEBUG) System.out.println("Write sector " + sect.getNumber() + ", gen " + m_generation);
		boolean bNew = true;
		
		// Set the generation
		sect.setGeneration(m_generation);
			
		// Get the history of this sector
		LinkedList<Sector> secversions = m_cache.get(sect.getNumber());

		if (secversions==null) {
			// No history yet
			secversions = new LinkedList<Sector>();
			m_cache.put(sect.getNumber(), secversions);
			if (DEBUG) System.out.println("Creating new history for sector " + sect.getNumber());
		}
		else {
			Sector lsect = getRecentVersion(secversions, true);
			if (lsect != null) {
				if (lsect.getGeneration() == m_generation) {
					// Same generation; overwrite the sector contents
					lsect.modify(sect.getData());
					if (DEBUG) System.out.println("Replacing the contents of sector " + sect.getNumber() + " in generation " + m_generation);
					bNew = false;
				}
				else {
					if (DEBUG) System.out.println("lsect.gen=" + lsect.getGeneration() + ", m_gen=" + m_generation);
				}
			}
			else {
				// else we once had a history before undoing. In that case, bNew = true
				if (DEBUG) System.out.println("Clear the history of sector " + sect.getNumber());
				secversions.clear();
			}
		}
		if (bNew) {
			// Append new generation
			secversions.add((Sector)sect.clone());
			if (DEBUG) System.out.println("Caching a new version (" + m_generation + ") of sector " + sect.getNumber());
		}
		// System.out.println(Utilities.hexdump(sect.getData()));
	}
	
	/** Removes all entries that were not committed by a following 
	    next_generation. This effectively means that all entries are removed
	    that have the same generation as the current m_generation value.
	*/
	public void rollback() {
		if (DEBUG) System.out.println("Rollback");
		Set<Integer> number = m_cache.keySet();
		for (int i : number) {
			LinkedList<Sector> seclist = m_cache.get(i);
			Iterator<Sector> backit = seclist.descendingIterator();
			while (backit.hasNext()) {
				Sector sec = backit.next();
				if (sec.getGeneration() >= m_generation) {
					if (DEBUG) System.out.println("Removing sector " + i + ", version " + sec.getGeneration() + ", m_gen = " + m_generation);
					backit.remove();
				}
			}
		}
	}
	
	/** Indicates whether this image has unsaved changes. Note that the
		variable m_generation refers to the next change, not the current.
	*/
	public boolean hasUnsavedEntries() {
		if (DEBUG) System.out.println("gen(" + m_sName + ") = " + m_generation + ", last save = " + m_checkpoint);  //#%
		return m_generation > m_checkpoint + 1;
	}

	public void nextGeneration(boolean bNew) {
//		Thread.currentThread().dumpStack();
		if (DEBUG) System.out.println("+ nextgen(" + m_sName + "): " + (m_generation+1));
		m_generation++;
		if (bNew) m_maxgen = m_generation;
	}
		
	public void previousGeneration() {
		if (DEBUG) System.out.println("- prevgen(" + m_sName + "): " + (m_generation-1));
		m_generation--;
	}
	
	public void sameGeneration() {
		if (m_generation > 1) {
			if (DEBUG) System.out.println("- samegen(" + m_sName + "): " + (m_generation-1));
			m_generation--;
		}
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
	
	public boolean isNew() {
		return (m_checkpoint < 0) && (m_generation==1);
	}
}

