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
	
	Copyright 2014 Michael Zapf
	www.mizapf.de

****************************************************************************/

package de.mizapf.timt.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

class DDData implements Transferable {
	
	List<Element> m_list;
	DirectoryView m_view;

	static DataFlavor[] m_supportedFlavors;
	static DataFlavor elementCollection; 
	
	static {
		m_supportedFlavors = new DataFlavor[2];
		try {
			m_supportedFlavors[0] = new DataFlavor("application/x-tiimagetool-elementcollection; class=de.mizapf.timt.ui.DDData");
			m_supportedFlavors[1] = DataFlavor.javaFileListFlavor;
			elementCollection = m_supportedFlavors[0];
		}
		catch (ClassNotFoundException cnfx) {
			cnfx.printStackTrace();
		}
	}

	DDData(List<Element> list, DirectoryView view) {
		m_list = list;
		m_view = view;
	}
	
	public Object getTransferData(DataFlavor flavor) {
		TIImageTool app = m_view.getImageTool();

		if (flavor.equals(DataFlavor.javaFileListFlavor)) {
//			System.out.println("DDData: Creating temporary files for DnD/CCP");
			
			Directory dirCurrent = m_view.getDirectory();
			List<java.io.File> lst = null;
			try {
				lst = app.exportDirectory(dirCurrent, app.getTemporaryDirectory(), m_list, true);
			}
			catch (ReplaceTableException rx) {
				rx.printStackTrace();
			}
			catch (InvalidNameException inx) {
				inx.printStackTrace();
			}
			catch (ImageException ix) {
				ix.printStackTrace();
			}
			catch (IOException iox) {
				iox.printStackTrace();
			}
			return lst;
		}
		else {
//			System.out.println("DDData: Delivering an elementCollection for DnD/CCP");
			if (flavor.equals(elementCollection)) {
				return m_list;
			}
		}
		System.out.println("*** Unknown Data Flavor");
		return null;
	}
	
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return (flavor.equals(DataFlavor.javaFileListFlavor) || flavor.equals(elementCollection));
	}
	
	public DataFlavor[] getTransferDataFlavors() { 
		return m_supportedFlavors; 
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		int i=0;
		for (Element el : m_list) {
			sb.append(el);
			i++;
			if (i < m_list.size()) sb.append(", ");
		}
		sb.append("]");
		return sb.toString();
	}
}
