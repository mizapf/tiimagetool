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

package de.mizapf.timt.ui;

import java.io.IOException;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;
import java.awt.dnd.DragSource;
import java.awt.dnd.DnDConstants;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;

/**
	Heading plus JList below
*/
public class DirectoryPanel extends JComponent implements ListCellRenderer<Element>, ListSelectionListener {

	DirectoryView m_dvCurrent;
	JList<Element> m_Listing;

	Set<String> m_names;
	int m_nFontHeight;

	Element m_lastSelected;
	
	DirectoryListModel m_dlm;
	
	JComponent m_head;
	
	static final Color NORM = new Color(250,255,255);
	private static final Color COLTEXT = new Color(51,51,51);
	private static final Color COLTEXTCONT = new Color(20,20,190);
	private static final Color COLTEXTCUT = new Color(150,150,150);
	private static final Color COLTEXTCONTCUT = new Color(150,180,210);
	
	private static final Color PSEL = new Color(210,230,230);
	private static final Color FOCUSED = new Color(200,220,220);

	// Pseudo-element, never shown
	class EndOfListElement extends Element {
		public int getAllRequiredSectors(int nAUSize) { return -1; }		
	}
	
	class DirectoryListModel extends AbstractListModel<Element> {
		
		Directory m_dir;
		Element m_eol;
		
		DirectoryListModel(Directory dir) {
			m_dir = dir;
			m_eol = new EndOfListElement();
		}
		
		public Element getElementAt(int index) {
			Directory[] adir = m_dir.getDirectories();
			TFile[] afile = m_dir.getFiles();
			int dirSize = adir.length;
			
			if (!m_dir.isRootDirectory()) {
				if (index==0) return Directory.parent;
				else index--;
			}
			
			if (index >= adir.length + afile.length) 
				return m_eol; 
			
			if (index < dirSize) 
				return adir[index];
			else
				return afile[index - dirSize];
		}		
		
		public int getSize() {
			int length = m_dir.getFiles().length + m_dir.getDirectories().length;
			if (!m_dir.isRootDirectory()) length++;
			length++; // add the EndOfListElement
			return length;
		}
		
		public void changed() {
			fireContentsChanged(this, 0, getSize()-1);
		}
	}
	
	DirectoryPanel(DirectoryView dv) {
		m_dvCurrent = dv;
		setOpaque(false);
		setLayout(new BoxLayout(DirectoryPanel.this, BoxLayout.Y_AXIS));
		createGui();
	}

	void rebuild() {
		createGui();
	}
	
	private void createGui() {
		removeAll();
		Directory dirCurrent = m_dvCurrent.getDirectory();		
		m_head = new JPanel();
		m_head.setOpaque(false);
		m_head.setLayout(new BoxLayout(m_head, BoxLayout.Y_AXIS));
		add(m_head);
		createHeader(m_head);

		m_dlm = new DirectoryListModel(dirCurrent);		
		m_Listing = new JList<Element>(m_dlm);
		m_Listing.setCellRenderer(this);
		m_Listing.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		// m_Listing.setBackground(Color.YELLOW);
		
		JScrollPane jsp = new JScrollPane(m_Listing);
		add(jsp);		
//		m_Listing.addListSelectionListener(this);
		m_Listing.setDragEnabled(true);
		m_Listing.setDropMode(DropMode.ON);
		m_Listing.setTransferHandler(new DDTransferHandler(this));
			
		m_Listing.addMouseListener(m_dvCurrent);
		
		InputMap imap = m_Listing.getInputMap();
		imap.put(KeyStroke.getKeyStroke("ctrl X"), "anothercut");
		imap.put(KeyStroke.getKeyStroke("ctrl C"), "anothercopy");
		imap.put(KeyStroke.getKeyStroke("ctrl V"), "anotherpaste");
	}
	
	private void createHeader(JComponent comp) {
		if (Thread.currentThread().getName().equals("main")) {
			Thread.currentThread().dumpStack();
		}
		comp.removeAll();

		Directory dirCurrent = m_dvCurrent.getDirectory();
		Volume vol = dirCurrent.getVolume();

		comp.add(Box.createVerticalStrut(4));

		StringBuilder sb = new StringBuilder();
		
		// First line
		sb.append("Volume in ").append(vol.getDeviceName());
		if (vol.getName().trim().length()==0) sb.append(" is unnamed");
		else sb.append(" is named ").append(vol.getName());
		if (vol.isFloppyImage()) {
			sb.append(", format: ").append(vol.getFloppyFormat());
			sb.append(", tracks: ").append(vol.getTracksPerSide());
		}
		sb.append(", total sectors: ").append(vol.getTotalSectors());
		if (vol.getAUSize()!=1) sb.append(", AU size: ").append(vol.getAUSize());
		if (vol.isProtected()) sb.append(" (protected)");
		sb.append(", ").append(vol.dumpFormat()).append(" image");

		comp.add(createHeadline(sb.toString()));
		comp.add(Box.createVerticalStrut(2));

		Directory[] adir = dirCurrent.getDirectories();
		TFile[] afile = dirCurrent.getFiles();

		//		m_head1.setBackground(new Color(220,200,180));
		
		// 		Format = SSSD ... DSUD, Tracks = 40
		//		Sectors used = xx, total sectors = 1438
		
		// Second line
		int nFileTotal = 0;
		
		// this directory
		// nFileTotal += dirCurrent.getAllRequiredSectors(m_vol.getAUSize());
		// System.out.println("this dir = " + nFileTotal);
		
		for (int i=0; i < afile.length; i++) {
			nFileTotal += afile[i].getAllRequiredSectors(vol.getAUSize());
		}
		
		// DDIRs of subdirs
		for (int i=0; i < adir.length; i++) {
			nFileTotal += adir[i].getAllRequiredSectors(vol.getAUSize());
		}
		
		sb = new StringBuilder();
		sb.append("Free sectors: ");
		sb.append(vol.getTotalSectors() - vol.getAllocatedSectorCount());
		sb.append(", used: ").append(vol.getAllocatedSectorCount());
		
		//		nFileTotal += m_vol.getAllRequiredSectors(m_vol.getAUSize());
		
		if (nFileTotal != vol.getAllocatedSectorCount()) {
			sb.append(", in this directory: ").append(nFileTotal);
		}

		comp.add(createHeadline(sb.toString()));
		comp.add(Box.createVerticalStrut(2));
	
		// Third line
		sb = new StringBuilder();
		sb.append("Directory of " ).append(vol.getDeviceName()).append(dirCurrent.getFullPathname());
		comp.add(createHeadline(sb.toString()));
		comp.add(Box.createVerticalStrut(10));
		
		JComponent title = createLine(TIImageTool.boldFont, null, false, "Name", "Sectors", "Type", "Length", "Prot", "Frag", "Created", "Updated", false, false, false);

		title.setMaximumSize(new Dimension(Short.MAX_VALUE, TIImageTool.plainHeight+2));		
		title.setBackground(new Color(180,200,220));
		comp.add(title);
	}
	
	private Box createHeadline(String sText) {
		Font font = TIImageTool.boldFont;
		Box cnt = new Box(BoxLayout.X_AXIS);
		cnt.add(Box.createHorizontalStrut(10));
		JLabel field = new JLabel(sText);
		field.setFont(font);
		cnt.add(field);
		cnt.setOpaque(false);	// let the background shine through
		//		cnt.setBackground(NORM);
		cnt.add(Box.createHorizontalGlue());
		cnt.setMaximumSize(new Dimension(Short.MAX_VALUE, TIImageTool.boldHeight));
		return cnt;
	}
		
	Directory getDirectory() {
		return m_dvCurrent.getDirectory();
	}
	
	DirectoryView getView() {
		return m_dvCurrent;
	}
	
	void updateView() {
		createHeader(m_head);
		m_head.validate();
		m_dlm.changed();
	}
	
	Element getElementAtLocation(Point p) {
		int index = m_Listing.locationToIndex(p);
		if (index < 0) return null; 
		return m_dlm.getElementAt(index);
	}
	
	java.util.List<Element> getSelectedEntries() {
		java.util.List<Element> aind = m_Listing.getSelectedValuesList();
		// Check whether we picked up the invisible end of the list and remove it
		int size = aind.size();
		if (aind.get(size-1) instanceof EndOfListElement) aind.remove(size-1); 
		return aind;
	}
	
	JList<Element> getLinkToJList() {
		return m_Listing;
	}
		
	//    ListSelectionListener
	// =============================================================

	public void valueChanged(ListSelectionEvent lse) {
		int minIndex = lse.getFirstIndex();
		int maxIndex = lse.getLastIndex();
		if (!lse.getValueIsAdjusting()) {
			System.out.println("First selection index = " + minIndex + ", last selection index = " + maxIndex);
			// Source is JList
			System.out.println("Selected index = " + m_Listing.getSelectedIndex());
		}		
	}
	
	/** Returns the element where the pointer stopped. */
	Element getLastSelected() {
		return m_lastSelected;
	}

	void clearSelection() {
		m_lastSelected = null;
		m_Listing.clearSelection();
	}
	
	void selectAll() {
		m_Listing.setSelectionInterval(0, m_Listing.getModel().getSize());
	}
	
	void setSelected(Set<String> names) {
		m_names = names;
	}
	
	/** 
	 	ListCellRenderer
	*/
	public Component getListCellRendererComponent(JList<? extends Element> list, Element el, int index, boolean isSelected, boolean cellHasFocus) {

		Volume vol = m_dvCurrent.getVolume();

		Font font = TIImageTool.plainFont;
		boolean bSelectable = true;
		String sName = el.getName();
		String sSectors = " ";
		String sType = " ";
		String sLength = " ";
		String sProtected = " ";
		String sFragmented = " ";
		String sCreation = " ";
		String sUpdate = " ";
		boolean bContainer = true;
				
		m_lastSelected = el;
		
		if (el instanceof EndOfListElement) {
			JComponent line = new Box(BoxLayout.X_AXIS);
			line.setMinimumSize(new Dimension(10,10));
			return line;
		}
		
		if (el instanceof TFile) {
			TFile file = (TFile)el;
			
			int nSize = 0;
			if (file.isProgram()) nSize = file.getProgramLength();
			else nSize = file.getRecordLength();
			
			sCreation = file.getCreationTime().toString();
			sUpdate = file.getUpdateTime().toString();
			sType = file.typeToString();
			sLength = String.valueOf(nSize); 
			sProtected = file.isProtected()? "P":" ";
			sFragmented = file.isFragmented()? "F" : " ";
			bContainer = file.hasArchiveFormat();
			sSectors = String.valueOf(el.getAllRequiredSectors(vol.getAUSize()));
		}
		else {
			Directory dir = (Directory)el;
			if (!dir.isParentLink()) {
				sType = "Dir";
				sLength = String.valueOf(vol.getAUSize()*Volume.SECTOR_LENGTH);
				sCreation = dir.getCreationTime().toString();
				sSectors = String.valueOf(el.getAllRequiredSectors(vol.getAUSize()));
			}
		}
		
		JComponent line = createLine(font, el, true, sName, sSectors, sType, sLength, sProtected, sFragmented, sCreation, sUpdate, bContainer, isSelected, cellHasFocus);
		return line;
	}
	
	private void addField(Box cnt, String sValue, Color fore, int pos, int nWidth, int nSpace, Font font) {
		if (sValue==null) sValue="";
		JLabel field = new JLabel(sValue, pos);

		field.setForeground(fore);

		field.setFont(font);
		field.setPreferredSize(new Dimension(nWidth, TIImageTool.plainHeight));
		cnt.add(field);
		if (nSpace!=0) cnt.add(Box.createHorizontalStrut(nSpace));
		else cnt.add(Box.createGlue());
	}
	
	private JComponent createLine(Font font, Element el, boolean bSelectable, String sFile, 
		String sSectors, String sType, String sLength, String sProtected, String sFragmented, 
		String sCreation, String sUpdate, boolean bContainer, boolean selected, boolean focused) {

		Box cnt = new Box(BoxLayout.X_AXIS);
	
		cnt.add(Box.createRigidArea(new Dimension(10, TIImageTool.plainHeight+2)));
	
		Color col = COLTEXT;

		if (bSelectable) {
			if (bContainer) {
				col = COLTEXTCONT;
				if (m_names != null && m_names.contains(sFile) && m_dvCurrent.markedForCut()) col = COLTEXTCONTCUT;
			}
			else {
				col = COLTEXT;
				if (m_names != null && m_names.contains(sFile) && m_dvCurrent.markedForCut()) col = COLTEXTCUT;
			}
		}
		
		addField(cnt, sFile,     col,  SwingConstants.LEFT,  m_dvCurrent.calcWidth("MOMOMOMOMO"), 10, font);
		addField(cnt, sSectors,  col,  SwingConstants.RIGHT, m_dvCurrent.calcWidth("Sectors"), 10, font);
		addField(cnt, sType,     col,  SwingConstants.LEFT,  m_dvCurrent.calcWidth("Emulate*"), 10, font);
		addField(cnt, sLength,    col, SwingConstants.RIGHT, m_dvCurrent.calcWidth("XXXXXX"), 10, font);
		addField(cnt, sProtected,  col,SwingConstants.LEFT,  m_dvCurrent.calcWidth("Prot"), 10, font);
		addField(cnt, sFragmented, col,SwingConstants.LEFT,  m_dvCurrent.calcWidth("Frag"), 10, font);
		addField(cnt, sCreation,   col,SwingConstants.LEFT,  m_dvCurrent.calcWidth("XXXX-XX-XX XX:XX:XX"), 10, font);
		addField(cnt, sUpdate,     col,SwingConstants.LEFT,  m_dvCurrent.calcWidth("XXXX-XX-XX XX:XX:XX"),  0, font);

		cnt.setOpaque(true);
		cnt.setBackground(selected? PSEL : NORM);
		if (focused) cnt.setBackground(FOCUSED);

		return cnt;
	}
}
