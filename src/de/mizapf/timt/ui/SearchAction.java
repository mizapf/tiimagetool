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
package de.mizapf.timt.ui;

import javax.swing.*;
import java.awt.event.KeyEvent;
import de.mizapf.timt.TIImageTool;
import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import de.mizapf.timt.files.*;
import java.awt.Cursor;
import java.util.regex.*;

public class SearchAction extends Activity {

	String[] m_validExt;
	String m_searchString;
	boolean m_searchArchives;
	boolean m_searchSubdir;
	SearchProgressView m_view;
	boolean m_useRegex;
	int m_count = 0;
	Pattern m_pattern;
	int m_maxhits = 0;
	boolean m_searchContents;
		
	public int getKeyCode() {
		return KeyEvent.VK_F;
	}

	public String getMenuName() {
		return imagetool.langstr("Search");
	}
	
	public String getActionName() {
		return "SEARCH";
	}
	
	public void go() {	
		SearchDialog sd = new SearchDialog(m_parent, imagetool);

		sd.createGui(imagetool.boldFont);
		sd.setVisible(true);
		
		if (sd.confirmed()) {
			ArrayList<File> allfiles = new ArrayList<File>();

			m_searchString = sd.getSearchString().trim();
			if (m_searchString.length()==0 || sd.getSelectedFiles()==null) {
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}
			m_validExt = sd.getValidExtensions();		
			m_searchArchives = sd.searchArchives();
			m_searchSubdir = sd.searchSubdirectories();
			m_useRegex = sd.isRegex();
			m_searchContents = sd.searchInsideFiles();

			try {
				m_maxhits = sd.getMaximumHits();
			}
			catch (NumberFormatException nfx) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("SearchInvalidMax"), TIImageTool.langstr("SearchError"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			if (m_useRegex) {
				try {				
					m_pattern = Pattern.compile(m_searchString);
				}
				catch (PatternSyntaxException psx) {
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("SearchInvalidRegex"), TIImageTool.langstr("SearchError"), JOptionPane.ERROR_MESSAGE);
					return;
				}
				catch (IllegalArgumentException iax) {
					JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("SearchBug"), TIImageTool.langstr("SearchError"), JOptionPane.ERROR_MESSAGE);
					iax.printStackTrace();
					return;
				}
			}
			
			m_count = 0;
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			for (File f : sd.getSelectedFiles()) collectFiles(f, allfiles, false);
			
			ArrayList<SearchResult> list = new ArrayList<SearchResult>();
			
			m_view = new SearchProgressView(TIImageTool.langstr("Searching"), m_parent);
			m_view.createGui(imagetool.boldFont);
			m_view.setVisible(true);
			
			for (File image : allfiles) {
				search(image, list);
			}
			
			if (m_view.stopRequested()) System.out.println(TIImageTool.langstr("SearchStop"));
			if (m_count >=m_maxhits) System.out.println("Maximum hits reached");
			
			SearchResult[] result = new SearchResult[list.size()];
			list.toArray(result);
			imagetool.showSearchResult(TIImageTool.langstr("SearchResults"), result); 
			m_view.dispose();
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	private void collectFiles(File filedir, ArrayList<File> filelist, boolean nextlevel) {
		if (filedir.isDirectory() && (!nextlevel || m_searchSubdir)) {
			for (File child : filedir.listFiles()) collectFiles(child, filelist, true);
		}
		else {
			boolean found = false;
			if (m_validExt == null || m_validExt.length==0) found = true;
			else for (int i=0; i < m_validExt.length; i++) {
				if (filedir.getName().toLowerCase().endsWith(m_validExt[i])) {
					found = true;
					break;
				}
			}
			if (found) filelist.add(filedir);
		}
	}
	
	private void search(File image, ArrayList<SearchResult> list) {
		try {
			Volume vol = new Volume(image.getAbsolutePath());
			// System.out.println("Searching image " + image.getAbsolutePath());
			m_view.setFilename(image.getAbsolutePath());
			if (!m_view.stopRequested() && m_count < m_maxhits) {
				searchDir(vol.getRootDirectory(), list);
			}
			vol.close();
		}
		catch (Exception ix) {
		}
	}
	
	private void searchDir(Directory dir, ArrayList<SearchResult> list) {
		for (TFile tf : dir.getFiles()) {		
			if (m_view.stopRequested() || m_count >= m_maxhits) break;
			if (m_useRegex) {
				Matcher m = m_pattern.matcher(getCandidateString(tf));
				if (m.matches()) {
					list.add(new SearchResult(dir.getVolume().getImageName(), tf.getPathname(), m_searchString));
					m_view.setResultCount(++m_count);
				}
			}
			else {
				// System.out.println(m_count + " - " + tf.getPathname());
				if (getCandidateString(tf).contains(m_searchString)) {
					list.add(new SearchResult(dir.getVolume().getImageName(), tf.getPathname(), m_searchString));
					m_view.setResultCount(++m_count);
				}
			}
			
			if (m_searchArchives && tf.hasArchiveFormat()) {
				try {
					Archive arc = tf.unpackArchive();
					searchDir(arc, list);
				}
				catch (IllegalOperationException iox) {
					// just ignore
				}
				catch (FormatException fx) {
					// just ignore
				}
				catch (IOException ixx) {
					// just ignore
				}
				catch (ImageException imx) {
					// just ignore
				}
			}
		}
		if (m_view.stopRequested() || m_count >=m_maxhits) return;
		for (Directory sdir : dir.getDirectories()) {
			if (m_view.stopRequested() || m_count >=m_maxhits) return;
			searchDir(sdir, list);
		}
	}
	
	private String getCandidateString(TFile tf) {
		String res = null;
		if (m_searchContents) {
			try {
				StringBuilder sb = new StringBuilder();
				byte[] content = tf.getSectorContent();
				for (int i=0; i < content.length; i++) {
					int cont = content[i] & 0xff;
					if (cont >= 32 && cont < 127) sb.append((char)cont);
					else sb.append(" ");
				}
				res = sb.toString();
			}
			catch (IOException iox) {
				res = " ";
			}
			catch (ImageException ix) {
				res = " ";
			}
		}
		else res = tf.getName();
		return res;
	}
}
