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
    
    Copyright 2011-2016 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.ui;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Properties;
import java.awt.Dimension;

import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.assm.Hint;
import de.mizapf.timt.util.Utilities;

public class Settings {
	
	String m_sFile;
	int m_os;
	
	Properties m_Props;
	
	public final static int SET_INVESC = 1;
	
	public Settings(int os) {
		m_sFile = (os==TIImageTool.WINDOWS)? "tiimagetool.prop" : ".tiimagetoolrc";
		m_Props = new Properties();		
		loadProperties();
	}
	
	private void loadProperties() {
				
		// Try to load
		// 1. from the current directory
		// 2. from the special path 
		FileReader fr = null;
		try {
			fr = new FileReader(m_sFile);
		}
		catch (FileNotFoundException fx) {
			m_sFile = System.getProperty("user.home") + System.getProperty("file.separator") + m_sFile; 
		}
		try {
			fr = new FileReader(m_sFile);
		}
		catch (FileNotFoundException fx) {
			// Try to get the value and pre-set it if not available 
			setDefaults();
			saveProperties(); // No properties
			return;
		}
		try {
			m_Props.load(fr);
		}
		catch (IOException iox) {
			System.err.println(TIImageTool.langstr("MainNoProperties"));
		}
		
		setDefaults();
		
		try {
			fr.close();
		}
		catch (IOException iox1) {
			iox1.printStackTrace();
		}
	}
	
	public void saveProperties() {
		// if (m_frmMain != null) setProperty(WINDOWSIZE, m_frmMain.getWidth() + "x" + m_frmMain.getHeight());
		// else setProperty(WINDOWSIZE, "640x480");
		
		try {
			FileWriter fw = new FileWriter(m_sFile);
			m_Props.store(fw, "Settings for TIImageTool");
			fw.close();
		}
		catch (IOException iox) {
			System.err.println(TIImageTool.langstr("MainPropSaveError"));
			iox.printStackTrace();
		}
	}
	
	private void setDefaults() {
		getPropertyString(TIImageTool.CONVERT, "/\\* __x");
		getPropertyString(TIImageTool.SUFFIX, ".tfi");
		getPropertyString(TIImageTool.UNDERSCORE, "true");		
		getPropertyString(TIImageTool.EXPLOWER, "true");
		
		String card;
		String ddpath;
		String copath;
		
		switch(m_os) {
		case TIImageTool.WINDOWS:
			card = "k:";
			ddpath = "";
			copath = "";
			break;
		case TIImageTool.MACOS:
			card = "/dev/disk4";
			ddpath = "/bin/dd";
			copath = "/usr/sbin/chown";
			break;
		default:
			card = "/dev/sdg";
			ddpath = "/usr/bin/dd";
			copath = "/usr/bin/chown";
			break;
		}
		getPropertyString(TIImageTool.CFCARD, card);
		getPropertyString(TIImageTool.DDPATH, ddpath);
		getPropertyString(TIImageTool.COPATH, copath);
		
		getPropertyString(TIImageTool.BSIZE, "4096");
		getPropertyString(TIImageTool.CONVERT, "true");	
		getPropertyString(TIImageTool.KEEPNAME, "false");	
		getPropertyString(TIImageTool.FORCEUPPER, "true");	
		getPropertyString(TIImageTool.HINTSTART, "true");
		getPropertyString(TIImageTool.BASICVER, "true");
		getPropertyString(TIImageTool.TFIFILTER, "true");
		getPropertyString(TIImageTool.NEWFRAME, "false");
		getPropertyString(TIImageTool.ESCAPE, "§%");
		getPropertyString(TIImageTool.LANG, "0");
		getPropertyString(TIImageTool.DNDC, "true");
		getPropertyString(TIImageTool.VERBOSE, "true");
		getPropertyString(TIImageTool.FONTSIZE, "12");
		getPropertyString(TIImageTool.CONTFONT, "Monospaced");
		getPropertyString(TIImageTool.UIFONT, "SansSerif");
		getPropertyString(TIImageTool.FILLPAT, "E5");
		getPropertyString(TIImageTool.FILLHPAT, "00");
		getPropertyString(TIImageTool.IMGFORM, "CF7ImageFormat,SectorDumpFormat,TrackDumpFormat,HFEFormat,RawHDFormat,MameCHDFormat");
		getPropertyString(TIImageTool.IMGSUF, "dsk,dtk,hfe,cf7,hd,chd,raw,bin,-");
		getPropertyString(TIImageTool.CONTEXT, "false");
	}
	
	public int checkProperties() {
		int ret = 0;
		String escape = getPropertyString(TIImageTool.ESCAPE);
		if (escape.length()>0 && escape.charAt(0) < 127)
			ret |= SET_INVESC;
		return ret;
	}
	
	public String getPropertyString(String sKey) {
		return m_Props.getProperty(sKey);
	}	
	
	public String getPropertyString(String sKey, String def) {
		String value = m_Props.getProperty(sKey);
		if (value==null) {
			value = def;
			m_Props.put(sKey, value);
		}
		return value;
	}
	
	public boolean getPropertyBoolean(String key) {
		String value = m_Props.getProperty(key);
		if (value==null) value = "false";
		return (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
	}
	
	public String loadDisassParams(String sPrefix, String sFile) {
		String sValues = m_Props.getProperty(sPrefix + "_" + sFile);
		return sValues;
	}
	
	public void saveDisassParams(String sPrefix, String sFile, int nStart, int nOffset, int nLength, Hint[] hint, boolean bSkipInv) {
		StringBuilder sb = new StringBuilder();
		sb.append(Utilities.toHex(nStart, 4)).append("#").append(Utilities.toHex(nOffset, 4)).append("#").append(Utilities.toHex(nLength, 4));
		sb.append(bSkipInv? "#1" : "#0");
		if (hint.length>0) {
			sb.append("#");
		}
		for (int i=0; i < hint.length; i++) {
			if (i>0) sb.append(",");
			sb.append(hint[i].toString());
		}
		put(sPrefix + "_" + sFile, sb.toString());
	}
	
	public java.io.File getSourceDirectory(String sWhich) {
		String sDir = m_Props.getProperty(sWhich + TIImageTool.SOURCEDIR);
		if (sDir == null) return null;
		else return new java.io.File(sDir);
	}
	
	public Dimension getPropertyDim(String sKey) {
		Dimension dim = null; 
		String sVal = m_Props.getProperty(sKey);
		if (sVal!=null && (sVal.indexOf("x")!=-1)) {
			String[] asPart = sVal.split("x");
			try {
				dim = new Dimension(Integer.parseInt(asPart[0]), Integer.parseInt(asPart[1]));
			}
			catch (NumberFormatException nfx) {
				System.err.println(TIImageTool.langstr("MainInvalidDim") + " " + sKey);
			}
			catch (ArrayIndexOutOfBoundsException ax) {
				System.err.println(TIImageTool.langstr("MainInvalidDim") + " " + sKey);
			}
		}
		return dim;		
	}
	
	public void put(String prop, String value) {
		m_Props.put(prop, value);
	}
}