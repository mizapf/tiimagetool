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
    
    Copyright 2016 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.files;

import java.io.EOFException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.lang.reflect.*;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.ui.Settings;

/** ImageFormat handles everything concerning the physical image. It delivers or
    takes sector contents, but does not care about their meaning.
    
    An image format is assumed to consist of metadata and format units (FU). The 
    metadata may determine the position of the format units in the image.
    
    Format            Format unit
    -----------------------------
    Sector dump       Track
    Track dump        Track
    CF7 image         Track
    HFE               Cylinder
    MAME CHD          Hunk
    Raw HD            Track

    CF7 volumes are special sector dumps
    
    Partitions are numbered from 1 to n (0 = no partitions); 
    the partition number must be provided with the sector number and delivers a
    FU number. Since both CF7 images and CHD images may be partitioned, there is
    no reason to define CF7 differently.
    
    ImageFormat translates (secnum, part) to a format unit number
    
    FormatCodec: encodes FUs (format units) from sectors or decodes them to sectors
    512-byte sector handling is hidden inside the FormatCodec
    ImageFormat is responsible for the proper position of the format unit in the
    image.

    FileSystems deal with structures on top of the sector level, i.e. allocation
    or deallocaton of sectors for files/directories, files, directories,
    management of the metadata of the file system (VIB)
    
    Structures:
    Cylinders / tracks: ImageFormat
    Heads: ImageFormat
    Sectors: ImageFormat 
        c/h/s may need to be taken from the FloppyFileSystem because the file
        length may imply several geometries (e.g. DSSD vs. SSDD or 40/80 tracks)
        
    Sector dump       File system
    Track dump        Image format
    CF7 image         Image format
    HFE               Image format
    CHD               Image format
    Raw HD            File system    
    
    Conflicts between image and volume information on opening an image
      
    Sector Dump: Same sizes may have different geometries
           Image size -> assume #cyl, #head, #sect
           VIB -> override #cyl, #head, #sect if plausible, else error (also if no VIB is present)
           
    Track Dump: Single-sided images are unformatted on the other side; no 
           difference in size
           Image size -> #cyl, #sect, assume #head=2
           VIB -> #head, error for different #cyl, #sect

    HFE image: 
           Image header -> #cyl, #sides, encoding, "HXCPICFE"
           VIB -> #sect, error for different #cyl, #sides
           
    CF7 image:
           Image size -> #part; #head=2, #cyl=40, #sect=20
           VIB in part -> warning for different #head, #sect, #cyl, suggest to fix
           
    Raw HD image:
           Image size -> must be larger than a sector dump for floppy disk
           VIB -> #cyl, #head, #sect (error if not present)
           
    MAME CHD image:
           Image header -> #totalsect, "MComprHD"
           Detect partitions, default #part=0
           VIB (part) -> #cyl, #head, #sect (error if not plausible)
           
    Newly created images take a MemoryImageFormat; it is always 
    consistent with the VIB (which is immediately created).
        
    Unformatted images (with invalid VIB) cannot be opened in TIMT.
*/
public abstract class ImageFormat  {
	
	FormatCodec m_codec; 
	SectorCache m_writeCache;
	TFileSystem m_fs;
	
	// Used by the type selection dialog
	public final static int NOTYPE = -1;
	public final static int MEMORY = 0;
	public final static int SECTORDUMP = 1;
	public final static int TRACKDUMP = 2;
	public final static int HFE = 3;
	public final static int CHD = 4;
	public final static int RAWHD = 5;
	
	public final static int NONE = -1;

	public final static String[] suffix = { "", "dsk", "dtk", "hfe", "hd", "raw" };

	// Independent of the file system, a physical property
	// int m_nTotalSectors;
	
	// Indicates whether the current format unit has been changed 
	boolean m_bDirty;

	protected static String formatline;
	
	protected static Class[] m_formatClass;
	
	protected static Settings m_Settings; 

	protected byte[] m_fillPattern;
	
	// Called from TIImageTool during startup
	public static void setFormats(String formstr) {
		formatline = formstr;
		String[] formats = formatline.split(",\\s*");
		m_formatClass = new Class[6];
		for (int i=0; i < formats.length; i++) {
			Object[] ao = new Object[0];
			try {
				Class<?> fmt = Class.forName("de.mizapf.timt.files." + formats[i]);
				System.out.print(formats[i] + ": ");
				Method type = fmt.getDeclaredMethod("getImageTypeStatic");
				Integer typeval = (Integer)type.invoke(null, ao);
				int index = typeval.intValue();
				System.out.println(index);
				if (m_formatClass[index] == null) {
					m_formatClass[index] = fmt;
				}
				else {
					System.err.println("Ignoring class " + formats[i] + ": Duplicate type number");
				}
			}
			catch (ArrayIndexOutOfBoundsException abx) {
				System.err.println("Ignoring class " + formats[i] + ": Invalid type number");
			}
			catch (ClassNotFoundException cnfx) {
				System.err.println("Ignoring class " + formats[i] + ": Not found");
			}
			catch (NoSuchMethodException nmx) {
				System.err.println("Ignoring class " + formats[i] + ": Does not implement type method");
			}
			catch (IllegalAccessException iax) {
				System.err.println("Ignoring class " + formats[i] + ": No access to vote method");
			}
			catch (InvocationTargetException itx) {
				System.err.println("Ignoring class " + formats[i] + ": Invocation target exception");
				itx.printStackTrace();
			}
		}
	}	

	public static void setSettings(Settings set) {
		m_Settings = set;
		setFormats(m_Settings.getPropertyString(TIImageTool.IMGFORM));
		// setFillPattern(m_Settings.getPropertyString(FILLPAT));
	}
	
	public void setFillPattern(String pat) {
		m_fillPattern = new byte[pat.length()/2];
		for (int i=0; i < m_fillPattern.length; i++) {
			m_fillPattern[i] = (byte)Integer.parseInt(pat.substring(i*2, (i+1)*2), 16);
		}
	}
	
	/** Determine the image format. */
	public static ImageFormat determineImageFormat(String sFile) throws FileNotFoundException, IOException, ImageException {
		
		File fl = new File(sFile);
		long nLength = fl.length();
		if (nLength == 0) throw new ImageException(sFile + ": " + TIImageTool.langstr("ImageEmpty"));
		
		for (Class<?> cls : m_formatClass) {
			try {
				if (cls != null) {
					Method vote = cls.getDeclaredMethod("vote", String.class);
					if (((Integer)vote.invoke(null, sFile)).intValue() > 50) { 
						Constructor<?> cons = cls.getConstructor(String.class);
						return (ImageFormat)cons.newInstance(sFile);
					}
				}
			}
			catch (NoSuchMethodException nmx) {
				System.err.println("Ignoring class " + cls.getName() + ": Does not implement vote method");
			}
			catch (IllegalAccessException iax) {
				System.err.println("Ignoring class " + cls.getName() + ": No access to vote method");
			}
			catch (InvocationTargetException itx) {
				if (itx.getCause() instanceof FileNotFoundException) 
					throw (FileNotFoundException)itx.getCause();
				else {
					if (itx.getCause() instanceof IOException) 
						throw (IOException)itx.getCause();
					else {
						if (itx.getCause() instanceof ImageException) {
							throw (ImageException)itx.getCause();
						}
						else {
							itx.printStackTrace();
						}
					}
				}
			}
			catch (InstantiationException iax) {
				System.err.println("Ignoring class " + cls.getName() + ": Cannot instantiate class");
			}
		}
		throw new ImageException(sFile + ": " + TIImageTool.langstr("ImageUnknown"));
	}
		
	static Class<?> getClassForFormat(int nFormat) {
		return m_formatClass[nFormat];
	}
	
	public static ImageFormat getImageFormatInstance(String sFileName, int nFormat, FormatParameters param) throws FileNotFoundException, IOException, ImageException {
		ImageFormat ifmt = null;
		Class<?> cls = null;
		try {
			cls = getClassForFormat(nFormat);
			Constructor<?> cons = cls.getConstructor(String.class, FormatParameters.class);
			ifmt = (ImageFormat)cons.newInstance(sFileName, param);
		}
		catch (InstantiationException iax) {
			System.err.println("Ignoring class " + cls.getName() + ": Cannot instantiate class");
		}
		catch (NoSuchMethodException nmx) {
			System.err.println("Ignoring class " + cls.getName() + ": Does not implement specific constructor");
		}
		catch (IllegalAccessException iax) {
			System.err.println("Ignoring class " + cls.getName() + ": No access to constructor");
		}
		catch (InvocationTargetException itx) {
			if (itx.getCause() instanceof FileNotFoundException) 
				throw (FileNotFoundException)itx.getCause();
			else {
				if (itx.getCause() instanceof IOException) { 
					throw (IOException)itx.getCause();
				}
				else {
					if (itx.getCause() instanceof ImageException) {
						throw (ImageException)itx.getCause();
					}
					else {
						itx.printStackTrace();
					}
				}
			}
		}

		return ifmt;
	}
		
	public static String checkFormatCompatibility(FormatParameters params, int nFormat) {
		Class<?> cls = null;
		try {
			cls = m_formatClass[nFormat];
			if (cls != null) {
				Method check = cls.getDeclaredMethod("checkFormatCompatibility", FormatParameters.class);
				return (String)check.invoke(null, params);
			}
		}
		catch (NoSuchMethodException nmx) {
			System.err.println("Internal error: " + cls.getName() + " has no method \"checkFormatCompatibility\"");
		}
		catch (IllegalAccessException iax) {
			System.err.println("Internal error: " + cls.getName() + " does not allow access to method \"checkFormatCompatibility\"");
		}
		catch (InvocationTargetException itx) {
			itx.printStackTrace();
		}
		return null;
	}
	
	protected byte[] getFillPattern() {
		return m_fillPattern;
	}
	
	// Static methods cannot be overridden
	abstract int getImageType();
	
	protected ImageFormat() throws FileNotFoundException {
		m_writeCache = new SectorCache();
		setFillPattern(m_Settings.getPropertyString(TIImageTool.FILLPAT));
	}
	
	public TFileSystem getFileSystem() {
		return m_fs;
	}
		
	public void setFileSystem(TFileSystem fs) {
		m_fs = fs;
	}
	public abstract Sector readSector(int nSectorNumber) throws ImageException, IOException;
	
	public abstract void writeSector(Sector sect) throws ImageException, IOException, ProtectedException;
		
	void close() throws IOException {
		// TODO
		// Write back all sectors
	}
	
	boolean cacheHasUnsavedEntries() {
		if (m_writeCache == null) return false;
		return m_writeCache.hasUnsavedEntries();
	}
	
	void setStartGeneration() {
		m_writeCache.setCheckpoint();
	}
	
	// Called from Volume and SectorEditFrame
	public void nextGeneration() {
		m_writeCache.nextGeneration();
	}
	
	public void previousGeneration() {
		m_writeCache.previousGeneration();
	}
	
/*	public int getTotalSectors() {
		return m_nTotalSectors;
	} */
	
	public void setCheckpoint() {
		m_writeCache.setCheckpoint();
	}
	
	public String getModShortImageName() {
		return cacheHasUnsavedEntries()? ("*" + getShortImageName()) : getShortImageName(); 
	}
	
	abstract String getImageName();
	abstract String getShortImageName();
	
	/* To be specified by the subclasses. */
	
	abstract String getFormatName();
	
	public abstract void reopenForWrite() throws IOException, ProtectedException;
	
	public abstract void reopenForRead() throws IOException;
}
