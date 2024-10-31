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
    
    Copyright 2011-2024 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

/*
	TODOs for 3.0.7
	-------------
    
    Images
	[x] SCSI/IDE harddisk (512 bytes/sector)
	[x] IDE harddisk support (incl. partitions)
	[x] Add default hard disk format selection ("Seagate ST-225 | ... | maxAU8 | maxAU16 | user-defined")
	[?] Add check for CF card read (check for newly created image file)
	[-] Search for CF7 card and for dd / chown automatically.
	[-] Check for CF7 open issues in Windows
	[x] "Please wait" window for CF7
	[x] Define suffixes for images
	[x] Add Edit CF7 to utilities (add/del/rename volumes)
	[x] Allow for up to 8 IDE partitions (new Nov 2023)

	Disassembler
	[?] Disassembler problem in symbolic mode; see disassembler file
    [?] Disassembly stops 6 bytes too early when using header
    [?] IDT label in Disassembler

    Display
	    Fix font size:
	[x] - Change CHD version (file name is truncated)
	[x] - Serial bridge
	[x] - Search dialog
	[-] Paste error: If last entry is dir, object will be pasted there (not reproducible)
	[x] Safe area for right-click outside of file
    [x] Periods appear doubled in XB file listing -> appears when . is used as escape character       
    [x] Add note to avoid "~" or "." as escape character
	[?] Recent files need escaping for semicolon in file name    
    [x] Right-click on another file does not deselect the previously marked file → javax.swing.ListSelectionModel
    [x] Keep dimension of text output window
    [ ] Dis/Fix 255 display (and pos. other files) should be improved
	
    Files
    [-] Cannot drag&drop into Archive
    [x] YAPP G7 cannot be loaded; treated wrongly as G6 (ImageFrame:246)
    [ ] Show 192 line graphics without black bar (192 line detection seems to be broken: Check whether possible at all)
	[ ] Show embedded machine language in BASIC (or indicate at least)
	[?] Allow for more DIS/VAR formats to be viewed (needs test)
    [x] Use empty sector pattern 

    Utils
    [x] Next sector does not work in Sector Editor with raw file
    [x] Write back with sector editor
	[ ] Cartridge creator (RPK)
	[x] Change MacOS defaults (see below in this file)
	[x] Update hints
    [x] Allow Return key for New floppy image
    [x] Allow Return key for new element
    [ ] Encode control characters in files from escape sequence (like §81 -> CTRL-a)
    [x] Complete undo
    [x] Complete redo
  	[x] Use a full editor for textual files
  	[x] Drop CHD conversion (saving converts to v5)
  	[x] Drop RAW/CHD conversion
  	[x] Drop HFDC/SCSI conversions (would imply sector length change)
    [x] Allow mass-import of non-headered files (Option Import all text files as DV80 / binary files as PRG; Preferences)
    [x] Allow binary import as DIS/FIX128 (see ImportContentAction:309, remove the second option)
    [x] Auto-split text lines in import to record length
    
  	Open bugs
    -

    Fixed bugs
    [x] Avoid pasting the end-of-list
    [x] Serial bridge standalone frame display
    [x] File sizes in CommandShell dir are one too low
    [x] Improve output of InvalidEscape
    [x] Importing a text or binary file (without TFI header) does not commit correctly
    [x] ViewImage when there is no color file
    [x] Failed import of a file leads to dirty image
    [x] Import of a directory copies all files, but they are gone after reopening (directory not committed)
    [x] Empty files are copied with a bad sector allocation [022..fff]
    [x] Track dump format delivers a better error message for 80 track images
    [x] Defining more than 4 partitions triggers ArrayIndexOOBException 
*/

package de.mizapf.timt;

// AWT imports
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.FlowLayout;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.Point;
// 
import java.awt.font.*;
import java.awt.event.*;
//
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
// 
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.Properties;
import java.util.Set;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.PropertyResourceBundle;
// 

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.mizapf.timt.assm.Hint;

import de.mizapf.timt.files.*;
import de.mizapf.timt.ui.*;
import de.mizapf.timt.util.*;
// 

public class TIImageTool implements ActionListener, ComponentListener, WindowListener, ChangeListener {
	
	JFrame m_frmMain;

	public final static String VERSION = "3.0.7";
	public final static String MONTH = "November";
	public final static String YEAR = "2024";
	
	private static final String TITLE = "TIImageTool";

	// ============= Supported OS =====================

	public final static int UNIX = 1;
	public final static int MACOS = 2;
	public final static int WINDOWS = 3;
		
	// ============= Properties ======================
	
	public final static String CONTFONT = "contentfont";
	public final static String LOOKANDFEEL = "lookandfeel";
	public final static String TEMPDIR = "tempdir";
	public final static String WINDOWSIZE = "windowsize";
	public final static String CONTSIZE = "contentsize";
	public final static String FILEDIALOG = "fdialogsize";
	public final static String SOURCEDIR = "sourcedir";
	public final static String EXPLOWER = "lower";
	public final static String CONVERT = "convert";
	public final static String UNDERSCORE = "underscore";
	public final static String SUFFIX = "suffix";
	public final static String KEEPNAME = "keepname";
	public final static String FORCEUPPER = "forceupper";
	public final static String IMPTEXT = "imptext";
	public final static String HINTS = "hints";
	public final static String HINTSTART = "hintstart";
	public final static String BASICVER = "basicver";
	public final static String TFIFILTER = "tfifilter";
	public final static String NEWFRAME = "newframe";
	public final static String ESCAPE = "escape";
	public final static String LOGFILE = "logging";
	public final static String RECENT = "recent";
	public final static String LANG = "lang";
	public final static String DNDC = "dndc";
	public final static String VERBOSE = "verbose";
	public final static String FONTSIZE = "fontsize";
	public final static String UIFONT = "uifont";
	public final static String FILLPAT = "fillpat";
	public final static String FILLHPAT = "fillhpat";
	public final static String IMGFORM = "imgform";
	public final static String IMGSUF = "imgsuf";
	public final static String CONTEXT = "context";
	public final static String GENOSPATH = "genospath";
	public final static String MEMORY = "memory";
	
	Properties m_propNames;
	
	// ============ Menu =============================

	JMenuBar m_mbar; 

	JMenu m_mFile;
	JMenu m_mNew;
	JMenuItem m_iNewFloppy;
	JMenuItem m_iNewHD;
	JMenuItem m_iNewIDE;
//	JMenuItem m_iNewCF7Vol;
	JMenuItem m_iNewCF7Img;
	JMenuItem m_iOpen;
	JMenu     m_mOpenRecent;
	JMenuItem m_iClose;
	JMenuItem m_iCloseAll;
	JMenuItem m_iSave;
	JMenuItem m_iSaveAs;
	// JMenuItem m_iSaveAll;

	JMenuItem m_iExport;
	JMenuItem m_iPreferences;
	JMenuItem m_iQuit;

	EditMenu m_mEdit;

	JMenu m_mUtility;
	JMenuItem m_iConsole;
	JMenuItem m_iCheck;
	JMenuItem m_iSectorEdit;
	JMenuItem m_iToScsi;
	JMenuItem m_iToHfdc;
	JMenuItem m_iSearch;
	JMenuItem m_iReadCF;
	JMenuItem m_iWriteCF;
	JMenuItem m_iEditCF;
	JMenuItem m_iInstallGenOS;
	JMenuItem m_iSerialBridge;

	JMenuItem m_iNewFile;
	JMenuItem m_iNewDirectory;
	
	JMenu	  m_mHelp;
	
	BackTabbedPane m_jtViews;

	LinkedList<DirectoryView> m_Views;	

	/** Count for unnamed files (see export) */
	private int m_nUnnamedFileCount;
	
	/** Count for unnamed volumes (for newly created volumes) */
	private int m_nUnnamedVolumeCount;
	
	/** Thread for background activity. */
	Thread m_thrBackground;
	
	/** Selected view */
	DirectoryView m_dvSelected;
		
	public final static String GPLPREFIX = "gpl";
	public final static String ASMPREFIX = "asm";
	
	/** Console output buffer. */
	java.io.File m_logFile;
	
	/** Serial package */
	public static final String SERIALPACKAGE="gnu.io.";
	// public static final String SERIALPACKAGE="com.fazecast.jSerialComm.";	 // TIMT does not compile with JSerialComm (missing objects/methods)
	
	// ===============================================
	
	/** Directory on the PC file system where to open the file dialog. */
	java.io.File m_flSourceDirectory = null;
	
	public final static String TEMPDIRNAME = "tiimagetool_tmp";
		
	// Localization
	// Add more languages into both lists (in the same order)
	// The second list is a list of keys into the Strings_xx.properties files
	// so the language names can be translated as well
	// Note that you need a Strings_xx.properties, hints_xx.txt, and command_xx.txt file for
	// each language
	private static final Locale[] locale = { Locale.ENGLISH, Locale.GERMAN, Locale.ITALIAN, Locale.FRENCH };
	private static final String[] langs = { "English", "German", "Italian", "French" }; 

	public final static String LANGTEXT = "de.mizapf.timt.ui.Strings";

	Settings m_Settings;
	String m_sPropertiesPath;

	private static ResourceBundle m_resources;
	
	/** Used to stop the main thread. */
	boolean m_bRunning;
	
	/** Indicates whether serial connections are available. */	
	boolean m_bSerial;
	
	Map<String,Activity> activities;
	Activity m_UserInput;
	boolean m_bAction;
	
	boolean m_markForCut;
	
	Set<JFrame> m_frames;
	
	java.io.File m_tmpDir;
	
	// Is this the first view to be opened?
	// Because of invokeLater, we cannot count the tabs.
	boolean m_first; 
	
	// Recent list
	List<String> m_recent;
	
	// LogStream
	LogStream m_logger;
	
	// Start image file
	String m_startImage;
	
	// Title icon
	public ImageIcon m_frameicon = null;
	
	public long m_maxMemory;
		
	String m_sStartupError;	
	// ===============================================
	// Clipboard for cut-copy-paste
	
	Transferable m_clipboard;
	
	boolean m_cliploaded;
	
	public static String FONT = Font.SANS_SERIF;
	public static String CONTENTFONT = Font.MONOSPACED;

	public static int plainHeight;
	public static int boldHeight;
	public static int dialogHeight;
	public static Font plainFont;
	public static Font boldFont;
	public static Font dialogFont;
	public static Font subDialogFont;
	public static Font boldDialogFont;
	public static Font contentFont;
	public static Font menuFont;
	
	public static int fontSize;
	
	private static final int MAXDEPTH = 10;
	
	private static final Color HOVE = new Color(0, 230, 230);
	
	private static final String BACKGROUND = "background.jpg";
	private static final String FRAMEICON = "frameicon.png";
	
	// ================================================
	// Hints
	// We save the deactivated hints as a bit pattern of multiple bytes and a hashcode
	
	private String[] m_ahint;
	private byte[] m_turnedoff;
	private int m_hinthash;
	private int m_lastHint;
	
	// ===================================================================
	// invokeLaters
	// ===================================================================
	
	/** Used for invokeLater. */
	class CreateGui implements Runnable {
		public void run() {		
			//		m_fm = m_frmMain.getFontMetrics(Font.decode(FONT));
			//		System.out.println("width = " + m_fm.stringWidth("FILENAME") + ", height = " + m_fm.getHeight());
			
			Container cnt = m_frmMain.getContentPane();
			cnt.setLayout(new BoxLayout(cnt, BoxLayout.Y_AXIS));
			m_mbar = new JMenuBar();

			m_frmMain.setVisible(true);
			// Must be visible before next line
			Graphics2D g2d = (Graphics2D)m_frmMain.getGraphics();

			FontRenderContext frc = g2d.getFontRenderContext();
			plainFont = new Font(FONT, Font.PLAIN, fontSize); 
			LineMetrics lm = plainFont.getLineMetrics("XX_X", 0, 2, frc);
			
			plainHeight = (int)Math.ceil(lm.getHeight()*1.03);
			boldFont = new Font(FONT, Font.BOLD, fontSize);
			lm = boldFont.getLineMetrics("XXX", 0, 2, frc);
			boldHeight = (int)Math.round(lm.getHeight());
			menuFont = new Font(FONT, Font.PLAIN, fontSize);
			dialogFont = new Font(FONT, Font.PLAIN, fontSize);	
			subDialogFont = new Font(FONT, Font.PLAIN, fontSize*5/6);	// for additional dialog parts
			boldDialogFont = new Font(FONT, Font.BOLD, fontSize);
			
			lm = dialogFont.getLineMetrics("XXX", 0, 2, frc);
			dialogHeight = (int)Math.round(lm.getHeight());
			
			contentFont = new Font(CONTENTFONT, Font.PLAIN, fontSize);	
			
			m_jtViews = new BackTabbedPane();
			cnt.add(m_jtViews);
			m_jtViews.addChangeListener(TIImageTool.this);
	
			UIManager.put("OptionPane.messageFont", plainFont);
			UIManager.put("OptionPane.buttonFont", boldFont);
			UIManager.put("MenuBar.font", menuFont);
			UIManager.put("Menu.font", menuFont);
			UIManager.put("MenuItem.font", menuFont);
			UIManager.put("Label.font", dialogFont);
			UIManager.put("RadioButton.font", dialogFont);
			UIManager.put("ComboBox.font", dialogFont);
			UIManager.put("TextField.font", dialogFont);
			UIManager.put("Button.font", boldDialogFont);
			
			Font accFont = new Font(FONT, Font.PLAIN, fontSize*3/4);	
			UIManager.put("MenuItem.acceleratorFont", accFont);
			UIManager.put("FileChooser.listFont", boldDialogFont);
			
			// -------------------------------------------
			/* Main window
			
			File
			----
			New > Floppy image...
			Hard disk image...
			Single CF7 volume...
			Complete CF7 image...
			Open...
			Open recent file > ...
			Save
			Save as
			Save all
			Close
			Close all
			Export image  -> to Utility
			Preferences...
			Exit */
			
			m_mFile = new JMenu(langstr("File"));
			m_mbar.add(m_mFile);
			
			m_mNew = new JMenu(langstr("New"));
			m_mFile.add(m_mNew);
			
			m_iNewFloppy = createMenuItem(new NewFloppyImageAction());
			m_iNewHD = createMenuItem(new NewHDImageAction());
			m_iNewIDE = createMenuItem(new NewIDEImageAction());
			m_iNewCF7Img = createMenuItem(new NewCF7ImageAction());
			m_mNew.add(m_iNewFloppy);
			m_mNew.add(m_iNewHD);
			m_mNew.add(m_iNewIDE);
			m_mNew.add(m_iNewCF7Img);
			m_iOpen = createMenuItem(new OpenImageAction());
			m_mFile.add(m_iOpen);

			m_mOpenRecent = new JMenu(langstr("OpenRecentFile"));
			m_mFile.add(m_mOpenRecent);
			
			// Fill recent menu
			int i = 0;
			String recents = m_Settings.getPropertyString(RECENT);
			if (recents != null && recents.length() > 0) {
				String[] recent = recents.split(System.getProperty("path.separator"));
				for (String s : recent) {
					m_mOpenRecent.add(createMenuItem(new OpenOneImageAction(s, i++)));
					m_recent.add(s);
				}
			}
			m_mOpenRecent.setEnabled(i > 0);
			m_mFile.addSeparator();
			
			m_iSave = createMenuItem(new SaveImageAction());
			m_mFile.add(m_iSave);
			m_iSaveAs = createMenuItem(new SaveAsImageAction());
			m_mFile.add(m_iSaveAs);
			// m_iSaveAll = createMenuItem(new SaveAllImageAction());
			// m_mFile.add(m_iSaveAll);
			setSaveOptions();
			
			m_mFile.addSeparator();

			m_iClose = createMenuItem(new CloseAction());
			m_mFile.add(m_iClose);
			m_iCloseAll = createMenuItem(new CloseAllAction());
			m_mFile.add(m_iCloseAll);
			
			m_mFile.addSeparator();
						
			m_iExport = createMenuItem(new ExportImageAction());
			m_mFile.add(m_iExport);
			
			m_mFile.addSeparator();
			m_iPreferences = createMenuItem(new PreferencesAction());
			m_mFile.add(m_iPreferences);
			
			m_mFile.addSeparator();
			m_iQuit = createMenuItem(new QuitAction());
			m_mFile.add(m_iQuit);
			
			// -------------------------------------------
			m_mEdit = new EditMenu(m_frmMain, TIImageTool.this, null); 
			m_mbar.add(m_mEdit);
			
			// -------------------------------------------
			
			m_mUtility = new JMenu(langstr("Utility"));
			m_mbar.add(m_mUtility);
			
			m_iConsole = createMenuItem(new ViewConsoleAction());
			m_mUtility.add(m_iConsole);
			m_mUtility.addSeparator();
			
			if (m_logFile == null) m_iConsole.setEnabled(false);

			m_iCheck = createMenuItem(new CheckFSAction());
			m_mUtility.add(m_iCheck);
			m_iInstallGenOS = createMenuItem(new InstallGenOSAction());
			m_mUtility.add(m_iInstallGenOS);
			m_iSectorEdit = createMenuItem(new SectorEditAction());
			m_mUtility.add(m_iSectorEdit);
			
			m_mUtility.addSeparator();
			
			m_iSearch = createMenuItem(new SearchAction());
			m_mUtility.add(m_iSearch);
			
			m_mUtility.addSeparator();
			m_iReadCF = createMenuItem(new ReadCFCardAction());
			m_mUtility.add(m_iReadCF);	
			m_iWriteCF = createMenuItem(new WriteCFCardAction());
			m_mUtility.add(m_iWriteCF);	
			m_iEditCF = createMenuItem(new EditCF7Action());
			m_mUtility.add(m_iEditCF);

			m_mUtility.addSeparator();
			m_iSerialBridge = createMenuItem(new SerialBridgeAction());
			m_mUtility.add(m_iSerialBridge);
			
			// -------------------------------------------
			
			m_mHelp = new JMenu(langstr("Help"));
			m_mbar.add(m_mHelp);
			
			m_mHelp.add(createMenuItem(new HelpAction()));
			m_mHelp.add(createMenuItem(new HintAction()));
			m_mHelp.addSeparator();
			m_mHelp.add(createMenuItem(new RefreshAction()));
			m_mHelp.addSeparator();
			m_mHelp.add(createMenuItem(new AboutAction()));
			
			// -------------------------------------------
			m_frmMain.setJMenuBar(m_mbar);	
						
			try {
				// Check for presence of RXTX
				// If not available, block the remote functions
				Class cls = Class.forName(SERIALPACKAGE + "SerialPort");
				m_bSerial = true;
				gnu.io.DriverManager.getInstance().loadDrivers();                
			}
			catch (ClassNotFoundException cnfx) {
				m_bSerial = false;
			}
			catch (NoClassDefFoundError nc) {
				// We will only get here when the gnu.io.SerialPort is available, but not the gnu.io.DriverManager
				// Does not make sense to try get it going here, because I cannot build TIMT for both versions.
				// Reflection API?
				System.err.println("Warning: The RXTX library is outdated and cannot be used with TIImageTool.");
				m_bSerial = false;
			}
						
			activateMenuItems();
			
			Dimension dim = m_Settings.getPropertyDim(WINDOWSIZE);
			if (dim==null) dim = new Dimension(800,600);
			
			m_frmMain.setSize(dim);
			//		m_frmMain.repaint();
			m_frames = new HashSet<JFrame>();
						
			// Shall we open an image now?
			if (m_startImage != null) {
				OpenImageAction act = (OpenImageAction)getActivity("OPENIMAGE"); 
				act.openLine(new File(m_startImage));
			}
			
			// Let the hint window pop up on start
			if (m_Settings.getPropertyBoolean(HINTSTART)) setUserInput("HINTS");
			
			if (m_sStartupError != null) 
				JOptionPane.showMessageDialog(m_frmMain, m_sStartupError, TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE); 
		}
	}
	
	class ActivateMenuItems implements Runnable {
		public void run() {
			activateMenuItems();
		}
	}
	
	class AddTab implements Runnable {
		String sName;
		DirectoryView dv;
		
		AddTab(String s, DirectoryView _dv) {
			sName = s;
			dv = _dv;
		}
		
		public void run() {
			String sShortName = dv.getDirectory().getVolume().getModShortImageName();
			m_jtViews.addTab(null, dv.getPanel());
			int index = m_jtViews.indexOfComponent(dv.getPanel());
			CloseableTab ct = new CloseableTab(sShortName, dv);
			m_jtViews.setTabComponentAt(index, ct);
			dv.setTabLabel(ct.getLabel());
			m_jtViews.setSelectedComponent(dv.getPanel());
			selectView(dv);
			activateMenuItems();
		}
	}

	class CloseView implements Runnable {
		
		DirectoryView m_dv = null;
		
		CloseView(DirectoryView dv) {
			m_dv = dv;
		}
		
		public void run() {
			boolean bOKToClose = true;
			if (m_dv.getVolume().isModified()) {
				bOKToClose = false;
				// Check whether there is another view of that volume 
				for (DirectoryView dv : m_Views) {
					if (dv != m_dv) {
						if (dv.getVolume() == m_dv.getVolume()) {
							bOKToClose = true;
						}
					}
				}
			}

			if (!bOKToClose) {
				int nRet = JOptionPane.showConfirmDialog(m_frmMain, TIImageTool.langstr("UnsavedChanges") + ". " + TIImageTool.langstr("ReallyClose"), TIImageTool.langstr("Leaving"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (nRet == JOptionPane.NO_OPTION) return;
			}
			
			m_dv.close();
			m_Views.remove(m_dv);
			m_jtViews.remove(m_dv.getPanel());
			if (m_Views.size()==0) m_first = true;
			activateMenuItems();
			updateMemoryInfo();
			setSaveOptions();
		}
	}
	
	class NewDirectoryView implements Runnable {
		Directory m_dirdv = null;
		boolean m_bAttachdv = false;
		
		NewDirectoryView(Directory dir, boolean bAttach) {
			m_dirdv = dir;
			m_bAttachdv = bAttach;
		}
		
		public void run() {
			DirectoryView dv = new DirectoryView(m_dirdv, m_bAttachdv, TIImageTool.this, m_Settings);
			m_Views.add(dv);
			activateMenuItems();
			setSaveOptions();
		}
	}
	
	class ViewRefresher implements Runnable {
		Volume volume;
		boolean backtoroot;	
		
		/* vol is used as a filter. When null, all views are selected. */
		ViewRefresher(Volume vol) {
			volume = vol;
			backtoroot = false;
		}

		ViewRefresher(Volume vol, boolean reset) {
			volume = vol;
			backtoroot = reset;
		}
		
		public void run() {
			// System.out.println("Volume " + volume);
			for (DirectoryView dv : m_Views) {
				if (volume == null) {
					// System.out.println("VR refresh " + dv + (dv.isDetached()? " (detached)" : ""));
					dv.refreshView();
				}
				else {
					if (volume.equals(dv.getVolume()))
						dv.refreshView();
					if (backtoroot) dv.gotoRootDirectory();
				}
			}
			// Refresh the selected view last
			if (volume == null) {
				if (m_dvSelected != null)
					m_dvSelected.refreshView();
				// else System.out.println("m_dvSel == null");
			}
		}
	}
		
	class ContentShow implements Runnable {
	
		String name;
		String content;
		boolean withClear;
		boolean withUpdate;
		ContentFrame m_content;
		
		ContentShow(String sname, String scontent, boolean bWithClear, boolean bWithUpdate) {
			name = sname;
			content = scontent;
			withClear = bWithClear;
			withUpdate = bWithUpdate;
		}

		ContentShow(String sname, String scontent, boolean bWithClear) {
			this(sname, scontent, bWithClear, false);
		}
		
		public void run() {
			m_content = new ContentFrame(name, TIImageTool.this, withClear);
			m_content.createGui(content, contentFont);
			Point loc = m_frmMain.getLocationOnScreen();		
			m_content.setLocation(loc.x+20, loc.y+20);
			m_content.setLocationByPlatform(true);
			m_content.setVisible(true);
			if (withUpdate) m_logger.register(m_content);
			Dimension dim = m_Settings.getPropertyDim(CONTSIZE);
			if (dim==null) dim = new Dimension(800,600);
			m_content.setPreferredSize(dim);
			m_content.pack();
		}
	}
	
	class ContentEditShow implements Runnable {
	
		String name;
		String content;
		ContentFrame m_content;
		TFile m_file;
		
		ContentEditShow(TFile file, String scontent) {
			name = null;
			content = scontent;
			m_file = file;
		}
		
		public void run() {
			ImportContentAction ia = new ImportContentAction();
			ia.setLinks(TIImageTool.this, m_frmMain, m_Settings);
			ia.go(m_file, content);
		}
	}
	
	class SearchResultShow implements Runnable {
	
		String name;
		SearchResult[] content;
		SearchResultFrame m_content;
		
		SearchResultShow(String sname, SearchResult[] result) {
			name = sname;
			content = result;
		}
		
		public void run() {
			m_content = new SearchResultFrame(name, TIImageTool.this);
			m_content.createGui(content, contentFont, m_Settings.getPropertyDim(CONTSIZE));
			Point loc = m_frmMain.getLocationOnScreen();		
			m_content.setLocation(loc.x+20, loc.y+20);
			m_content.setLocationByPlatform(true);
			m_content.setVisible(true);
			m_content.pack();
			m_content.toFront();
		}
	}
	
	class SectorEditorShow implements Runnable {
	
		ImageFormat m_image;
		SectorEditFrame m_edit;
		String m_name;
		
		SectorEditorShow(String name, ImageFormat image) {
			m_name = name;
			m_image = image;
		}
		
		public void run() {
			m_edit = new SectorEditFrame(m_name, m_image, TIImageTool.this);
			m_edit.createGui(contentFont);
			Point loc = m_frmMain.getLocationOnScreen();		
			m_edit.setLocation(loc.x+20, loc.y+20);
			m_edit.setLocationByPlatform(true);
			m_edit.setVisible(true);
			m_edit.pack();
			m_edit.toFront();
		}
	}
	// ===================================================================
	// Inner classes
	// ===================================================================
	
	class BackTabbedPane extends JTabbedPane {
		Image m_image;		// background image
		
		BackTabbedPane() {
			super();
			m_image = null;
			ImageIcon icon = null;
			java.net.URL imgURL = ToolDialog.class.getResource(BACKGROUND);
			if (imgURL != null) {
				icon = new ImageIcon(imgURL);
				m_image = icon.getImage();
			} 
			else {
				System.err.println(langstr("NoBackground") + ": " + imgURL);
			}
		}
		
		public void paintComponent(Graphics g) {
			int nWidth = getWidth();
			int nHeight = getHeight();
			if (m_image != null) g.drawImage(m_image, 0, 0, nWidth, nHeight, this);
			super.paintComponent(g);
		}
	}

	// ===================  Tab with close button ===========================
	
	private class CloseableTab extends JPanel {
		JLabel m_TabLabel = null;
		
		public CloseableTab(String sLabel, DirectoryView dv) {
			super(new FlowLayout(FlowLayout.LEFT, 0, 0));
			setOpaque(false);
			m_TabLabel = new JLabel(sLabel);
			m_TabLabel.setFont(boldFont);
			add(m_TabLabel);
			m_TabLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
			add(new DetachTabButton(dv));
			add(new CloseTabButton(dv));
			setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		}
		
		public void setTabName(String sName) {
			m_TabLabel.setText(sName);
		}
		
		public JLabel getLabel() {
			return m_TabLabel;
		}
	}

	private class CloseTabButton extends JButton implements MouseListener {
		private DirectoryView m_dv;
		
		public CloseTabButton(DirectoryView dv) {
			m_dv = dv;
			int size = 17;
			setPreferredSize(new Dimension(size, size));
			setToolTipText(langstr("CloseThisView"));
			// setUI(new BasicButtonUI());
			setContentAreaFilled(false);
			setFocusable(false);
			setBorder(BorderFactory.createEtchedBorder());
			setBorderPainted(false);
			addMouseListener(this);
			setRolloverEnabled(true);
		}
				
		// paint the cross
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			//shift the image for pressed buttons
			if (getModel().isPressed()) {
				g2.translate(1, 1);
			}
			g2.setStroke(new BasicStroke(2));
			g2.setColor(Color.BLACK);
			if (getModel().isRollover()) {
				g2.setColor(HOVE);
			}
			int delta = 6;
			g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
			g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
			g2.dispose();
		}

		public void mouseEntered(MouseEvent e) {
			Component component = e.getComponent();
			if (component instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(true);
			}
		}
 
		public void mouseExited(MouseEvent e) {
			Component component = e.getComponent();
			if (component instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(false);
			}
		}

		public void mouseClicked(MouseEvent e) {
			SwingUtilities.invokeLater(new CloseView(m_dv));
		}

		public void mousePressed(MouseEvent act) { 	}
		public void mouseReleased(MouseEvent act) { 	}
	}
	
		
	private class DetachTabButton extends JButton implements ActionListener, MouseListener {
		private Directory m_dir;
		private DirectoryPanel m_dt;
		private DirectoryView m_dv;
		
		public DetachTabButton(DirectoryView dv) {
			m_dir = dv.getDirectory();
			m_dv = dv;
			m_dt = dv.getPanel();
			int size = 17;
			setPreferredSize(new Dimension(size, size));
			setToolTipText(langstr("ShowAsWindow"));
			// setUI(new BasicButtonUI());
			setContentAreaFilled(false);
			setFocusable(false);
			setBorder(BorderFactory.createEtchedBorder());
			setBorderPainted(false);
			addMouseListener(this);
			setRolloverEnabled(true);
			addActionListener(this);
		}
		
		public void actionPerformed(ActionEvent e) {
			m_dv.detach();
		}
		
		// paint the window shape
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			//shift the image for pressed buttons
			if (getModel().isPressed()) {
				g2.translate(1, 1);
			}
			g2.setStroke(new BasicStroke(2));
			g2.setColor(Color.BLACK);
			if (getModel().isRollover()) {
				g2.setColor(HOVE);
			}
			int delta = 5;
			g2.drawLine(delta, delta, getWidth() - delta - 1, delta);
			g2.drawLine(delta, getHeight() - delta - 1, getWidth() - delta - 1, getHeight() - delta - 1);
			g2.drawLine(delta, delta, delta, getHeight() - delta - 1);
			g2.drawLine(getWidth() - delta - 1, delta, getWidth() - delta - 1, getHeight() - delta - 1);
			g2.dispose();
		}

		public void mouseEntered(MouseEvent e) {
			Component component = e.getComponent();
			if (component instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(true);
			}
		}
 
		public void mouseExited(MouseEvent e) {
			Component component = e.getComponent();
			if (component instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(false);
			}
		}

		public void mouseClicked(MouseEvent e) { }
		public void mousePressed(MouseEvent act) { 	}
		public void mouseReleased(MouseEvent act) { 	}
	}
	
	// ===================================================================
	
	public static void main(String[] arg) {
		if (arg.length>0) {
			if (arg[0].equalsIgnoreCase("bridge")) {
				
				TIImageTool.localize();
				try {
					// Check for presence of RXTX
					// If not available, block the remote functions
					Class cls = Class.forName(SERIALPACKAGE + "SerialPort");
					gnu.io.DriverManager.getInstance().loadDrivers();                
				}
				catch (ClassNotFoundException cnfx) {
					System.err.println(langstr("MainSerialMissing"));
					return;
				}
			
				String sSerial = null;
				String sPort = null;
				
				if (arg.length>2) {
					sSerial = arg[1];
					sPort = arg[2];
				}
				else {
					System.err.println(langstr("MainBridgeParamsMissing"));
					return;
				}
				
				// Set the look and feel
				String lafclass = "javax.swing.plaf.metal.MetalLookAndFeel";

				// We don't have the settings here, so let's try 16
				fontSize = 16;
				try {
					if (arg.length > 3) {
						fontSize = Integer.parseInt(arg[3]);
					}
				}
				catch (NumberFormatException nfx) {
					System.err.println(langstr(String.format("ParseError", arg[3])));
					fontSize = 16;
				}
				
				dialogFont = new Font(FONT, Font.PLAIN, fontSize);	
				boldDialogFont = new Font(FONT, Font.BOLD, fontSize);
				try {
					UIManager.setLookAndFeel(lafclass);
					UIManager.put("Label.font", dialogFont);
					UIManager.put("TextField.font", dialogFont);
					UIManager.put("Button.font", boldDialogFont);
				}
				catch (UnsupportedLookAndFeelException e) {
					System.err.println(langstr("MainUnsuppLF") + ": " + lafclass);
				}
				catch (ClassNotFoundException e) {
					System.err.println(langstr("MainNotFoundLF") + ": "+ lafclass);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
	
				SerialBridgeAction act = new SerialBridgeAction();
				act.setup(sSerial, sPort);

				return;
			}
			if (arg[0].equalsIgnoreCase("commandshell")) {
				String[] arg1 = new String[arg.length-1];
				System.arraycopy(arg,1,arg1,0,arg.length-1);
				CommandShell.main(arg1);
				return;
			}
		}
		
		TIImageTool ie = new TIImageTool();
		if (arg.length>0) ie.setStartImage(arg[0]);
		else ie.setStartImage(null);

		ie.processUserInput();
	}
	
	/** Set up localizations to a default. This must be called from stand-alone tools inside TIImageTool which
		do not instantiate TIImageTool. */
	public static void localize() {
		m_resources = ResourceBundle.getBundle(LANGTEXT, getSysLocale());		
	}
	
	public static Locale getSysLocale() {
		Locale sysloc = Locale.getDefault();	
		Locale loc = locale[0]; // English
		
		// Search for the first locale whose language parameter matches the system locale
		for (int i=0; i < locale.length; i++) {
			if (sysloc.getLanguage().equals(locale[i].getLanguage())) { 
				loc = locale[i];
				break;
			}
		}
		return loc;
	}
	
	private InputStream getLocalizedStream(String name, String suffix) {
		String resourceFile = name + getLocale(m_Settings.getPropertyString(LANG)).getLanguage() + suffix;
		InputStream is = ToolDialog.class.getResourceAsStream(resourceFile);
		if (is==null) {
			// Try to load the file from user.home
			File propFile = new File(System.getProperty("user.home"), resourceFile);
			try {
				is = new FileInputStream(propFile);
			}
			catch (FileNotFoundException fnfx) {
				// Fall back to English
				System.err.println("Could not find localized file " + resourceFile + " in package, nor in " + System.getProperty("user.home") + ". Falling back to English.");
				resourceFile = name + locale[0].getLanguage() + suffix;
				is = ToolDialog.class.getResourceAsStream(resourceFile);
			}
		}
		return is;	
	}
	
	TIImageTool() {
		m_sStartupError = null;
		m_sPropertiesPath = null;
		m_Settings = new Settings(getOperatingSystem());
		
		// Load localized strings
		// m_resources = ResourceBundle.getBundle(LANGTEXT, getLocale(m_Settings.getPropertyString(LANG)));
		try {
			InputStream is = getLocalizedStream("Strings_", ".properties");
			m_resources = new PropertyResourceBundle(new InputStreamReader(is, "UTF-8"));			
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}		

		int check = m_Settings.checkProperties();
		if (check != 0) {
			if ((check & Settings.SET_INVESC)!=0) {
				m_sStartupError = langstr("Error.InvalidEscape");
				System.err.println(m_sStartupError);
			}
		}
		
		// Load the property texts
		m_propNames = new Properties();
		try {
			InputStream is = getLocalizedStream("names_", ".prop");
			m_propNames.load(new InputStreamReader(is, "UTF-8"));
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}		
			
		// Redirect Console output
		String sLogFile = m_Settings.getPropertyString(LOGFILE, "tiimagetool.log");
		if (sLogFile.length()>0) {
			try {
				m_logFile = new java.io.File(sLogFile); 
				FileOutputStream fos = new FileOutputStream(m_logFile, true); // append
				m_logger = new LogStream(fos);
				System.setOut(m_logger);
				System.setErr(m_logger);
				if (m_logFile.length()>0) System.out.println("========================");
			}
			catch (FileNotFoundException fx) {
				System.err.println(langstr("ConsoleOutputError"));
			}
		}
		
		FONT = m_Settings.getPropertyString("uifont", Font.SANS_SERIF);
		CONTENTFONT = m_Settings.getPropertyString(CONTFONT);
		if (CONTENTFONT == null) m_Settings.put(CONTFONT, Font.MONOSPACED);
		m_frmMain = new JFrame(TITLE);
		m_frmMain.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		m_frmMain.addWindowListener(this);
		m_frmMain.addComponentListener(this);
		
		java.net.URL iconurl = ToolDialog.class.getResource(FRAMEICON);
		if (iconurl != null) {
			m_frameicon = new ImageIcon(iconurl);
			m_frmMain.setIconImage(m_frameicon.getImage());
		} 
		else {
			System.err.println(langstr("NoImage") + ": " + iconurl);
		}
		
		activities = new HashMap<String, Activity>();
		m_clipboard = null;
		m_cliploaded = false;
		m_bRunning = true;	
		m_first = true;
		
		m_nUnnamedFileCount = 0;
		m_nUnnamedVolumeCount = 0;
		
		// Set the look and feel
		String lafclass = m_Settings.getPropertyString(LOOKANDFEEL, "javax.swing.plaf.metal.MetalLookAndFeel");
		try {
			UIManager.setLookAndFeel(lafclass);
		}
		catch (UnsupportedLookAndFeelException e) {
			System.err.println(langstr("MainUnsuppLF") + ": " + lafclass);
		}
		catch (ClassNotFoundException e) {
			System.err.println(langstr("MainNotFoundLF") + ": "+ lafclass);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		m_Views = new LinkedList<DirectoryView>();

		m_recent = new LinkedList<String>();
		
		// Create temporary directory
		String sTempDir = m_Settings.getPropertyString(TEMPDIR, System.getProperty("java.io.tmpdir"));
		if (sTempDir == null || sTempDir.length() == 0) sTempDir = "."; 

		m_tmpDir = new java.io.File(sTempDir, TEMPDIRNAME);
		if (!m_tmpDir.exists()) {
			m_tmpDir.mkdir();
			if (m_tmpDir.exists())
				System.out.println(langstr("MainCreatedTmp") + ": " + m_tmpDir.getName());
			else {
				m_sStartupError = langstr("Error.MainCreatedTmp") + "(" + m_tmpDir.getAbsolutePath() + ")";
				System.err.println(m_sStartupError);
			}
		}
		m_Settings.put(TEMPDIR, sTempDir);
		
		// Get the hints
		String hint = null;
		// no more than 1000 hints
		String[] ahint = new String[1000];
		for (int i=0; i < ahint.length; i++) ahint[i] = "";
		m_hinthash = 0;		
		
		int maxnumber = 0;
		try {
			InputStream is = getLocalizedStream("hints_", ".txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			int lastInd = 0;
			
			int number = 0;
			while (br.ready()) {
				hint = br.readLine();
				m_hinthash = m_hinthash ^ hint.hashCode();
				if (hint.charAt(0)=='#') continue;
				int poseq = hint.indexOf("=");
				if (poseq >=0 && poseq < 10) {
					number = Integer.parseInt(hint.substring(0, poseq-1));
					ahint[number] = hint.substring(poseq+1).trim();
					if (number > maxnumber) maxnumber = number;
				}
				else {
					ahint[number] = ahint[number] + " " + hint;
				}
			}
			is.close();
			m_ahint = new String[maxnumber+1];
			System.arraycopy(ahint, 0, m_ahint, 0, maxnumber+1);
		}
		catch (NumberFormatException nfx) {
			System.err.println(langstr("HintError") + ": " + hint);
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
		m_turnedoff = new byte[maxnumber / 8 +1];

		String hintnotes = m_Settings.getPropertyString(HINTS, "0");
		try {
			if (hintnotes != null) {
				String[] ahintn = hintnotes.split(",");
				int oldhash = (int)(Long.parseLong(ahintn[0], 16));
				if (oldhash == m_hinthash) {
					for (int i=0; i < m_turnedoff.length; i++) {
						if (i > ahintn.length-1) m_turnedoff[i] = 0;			
						else m_turnedoff[i] = (byte)(Integer.parseInt(ahintn[i+1]) & 255);
					}
				}
			}
		}
		catch (NumberFormatException nfx) {
			System.err.println(langstr("MainHintError") + ": " + nfx.getMessage());
		}
		saveHintSettings();
		
		try {
			fontSize = Integer.parseInt(m_Settings.getPropertyString(FONTSIZE, "12"));
		}
		catch (NumberFormatException nfx) {
			fontSize = 12;
			System.err.println(langstr("ParseError") + ": " + nfx.getMessage());
		}		
		
		m_maxMemory = Runtime.getRuntime().freeMemory();

		// SectorCache.setGen(0);
		ImageFormat.setSettings(m_Settings);
		
		// new CreateGui().run();
		SwingUtilities.invokeLater(new CreateGui());
	}
	
	public void updateMemoryInfo() {
		long freeMem = Runtime.getRuntime().freeMemory();
		int nPercent = (int)(freeMem*100/m_maxMemory);
//		System.out.println("Free memory = " + (freeMem / 1024) + "KiB = " + nPercent + "%");
		if (m_jtViews == null) return;
		int nIndex = m_jtViews.getSelectedIndex();
		if (nIndex != -1) {
			DirectoryPanel dp = (DirectoryPanel)m_jtViews.getComponentAt(nIndex);
			dp.updateMemoryInfo(nPercent);
		}
	}
	
	public JFrame getMainFrame() {
		return m_frmMain;
	}
	
	private void setStartImage(String file) {
		m_startImage = file;
	}
	
	void saveHintSettings() {
		StringBuilder sb = new StringBuilder();
		sb.append(Utilities.toHex(m_hinthash, 8, true));
		for (int i=0; i < m_turnedoff.length; i++) {
			sb.append(",").append((int)m_turnedoff[i] & 255);
		}
		m_Settings.put(HINTS, sb.toString());
	}
	
	public String getHint() {
		// Check if hint[0] has been marked; otherwise return it
		if (!isHintBitSet(0)) return m_ahint[0];
		
		boolean found = false;
		int turn = 0;
		int rand = 0;
		m_lastHint = 0;

		// Do 20 dice rolls, and if still not found, pick the first from the start
		while (turn < 20 && !found) {
			rand = (int)(Math.random() * m_ahint.length);
			if (rand >= m_ahint.length) rand = m_ahint.length - 1;
			if (!isHintBitSet(rand)) found = true;
			turn++;
		}
		
		if (!found) {
			rand = 0;
			while (!found && rand < m_ahint.length) {
				if (!isHintBitSet(rand)) found = true;
				else rand++;
			}
		}
		if (!found) return langstr("HintAll");
		else {
			m_lastHint = rand;
			return m_ahint[rand];
		}
	}
	
	public void setHintResponse(boolean knowThatOne, boolean notInterested, boolean again) {
		if (again) {
			for (int i=0; i < m_turnedoff.length; i++) m_turnedoff[i]=0;
		}
		
		if (knowThatOne) setHintBit(m_lastHint);

		if (notInterested) m_Settings.put(HINTSTART, "false");
		saveHintSettings();
	}
	
	private void setHintBit(int number) {
		int block = number / 8;
		int test = 128 >> (number % 8);
		if (m_turnedoff.length < block) return;
		m_turnedoff[block] |= test;
	}
	
	private boolean isHintBitSet(int number) {
		int block = number / 8;
		int test = 128 >> (number % 8);
		if (m_turnedoff.length < block) return false;
		return ((m_turnedoff[block] & test)!=0);
	}
	
	public List<String> getPreferences(String category) {
		List<String> lst = new ArrayList<String>();
		for (Object key : m_propNames.keySet()) {
			String value = m_propNames.getProperty((String)key);
			if (value != null) {
				if (value.startsWith(category)) lst.add((String)key);
			}
		}
		return lst;
	}
	
	public char getPreferenceType(String key) {
		String value = m_propNames.getProperty(key);
		String[] aval = value.split(":");
		return aval[1].charAt(0);
	}
	
	public String getPreferenceLabel(String key) {
		String value = m_propNames.getProperty(key);
		String[] aval = value.split(":");
		return aval[2];
	}
	
	public void saveFrameSize() {
		if (m_frmMain != null) m_Settings.put(WINDOWSIZE, m_frmMain.getWidth() + "x" + m_frmMain.getHeight());
		else m_Settings.put(WINDOWSIZE, "640x480");
	}
	
	public static Locale getLocale(String loc) {
		int index = 0;
		try {
			index = Integer.parseInt(loc);
		}
		catch (NumberFormatException nfx) {
			index = 0;
		}
		if (index >= locale.length) index = locale.length-1;
		return locale[index];
	}
	
	public static String langstr(String key) {
		if (m_resources == null) return null;
		return m_resources.getString(key);
	}
	
	public String[] getLanguages() {
		String[] lang = new String[langs.length];
		for (int i=0; i < lang.length; i++) {
			lang[i] = langstr(langs[i]);
		}
		return lang;
	}
	
	public static int getOperatingSystem() {
		if (System.getProperty("os.name").startsWith("Windows")) return WINDOWS;
		else {
			if (System.getProperty("os.name").startsWith("Mac")) return MACOS;
			else return UNIX;
		}
	}
	
// JComponent.getComponentGraphics -> getFontMetrics -> stringWidth

	public JMenuItem createMenuItem(Activity act) {
		JMenuItem mi = new JMenuItem(act.getMenuName());
		mi.setActionCommand(act.getActionName());
		mi.addActionListener(this);
		registerActivity(act);
		if (act.getKeyCode()!=0) mi.setAccelerator(KeyStroke.getKeyStroke(act.getKeyCode(), act.getModifier()));		
		return mi;
	}

	public void registerActivity(Activity act) {
		act.setLinks(this, m_frmMain, m_Settings);
		activities.put(act.getActionName(), act);
	}
	
	void processUserInput() {
		while (m_bRunning) {
			updateMemoryInfo();
			synchronized(this) {
				if (m_UserInput != null) {
					try {
						m_UserInput.go();
					}
					catch (Exception e) {
						m_frmMain.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						e.printStackTrace();
						JOptionPane.showMessageDialog(m_frmMain, langstr("BUG"), langstr("InternalError"), JOptionPane.ERROR_MESSAGE);						
					}
					m_UserInput = null;
					// SwingUtilities.invokeLater(new ActivateMenuItems());
				}
				try {
					if (m_bRunning) wait();
				}
				catch (InterruptedException ix) {
					ix.printStackTrace();
					JOptionPane.showMessageDialog(m_frmMain, langstr("BUG"), langstr("InternalError"), JOptionPane.ERROR_MESSAGE); 
				}
				m_bAction = false;
			}
		}
	}

	public boolean userActionInProgress() {
		return m_bAction;
	}
		
	public Activity getActivity(String name) {
		return activities.get(name);
	}
	
	synchronized void setUserInput(Activity act) {
		m_UserInput = act;
		m_bAction = true;
		notify();
	}
	
	synchronized void setUserInput(String sCommand) {
		Activity act = activities.get(sCommand);
		if (act != null) setUserInput(act);			
	}

	public boolean offersSerialConnection() {
		return m_bSerial;
	}
	
	public Dimension getFrameSize() {
		return m_frmMain.getSize();		
	}
	
	public boolean contextFunctionsInEdit() {
		return m_Settings.getPropertyBoolean(CONTEXT);
	}
	
	/** From DirectoryView */
	public void attachView(String sName, DirectoryView dv, boolean now) {
//		System.out.println("attach view:" + Thread.currentThread());
		AddTab at = new AddTab(sName, dv);
		if (now) at.run();
		else SwingUtilities.invokeLater(at);
	}
	
	/** From DirectoryView */
	public void removeTab(DirectoryPanel dp) {
		m_jtViews.remove(dp);
		activateMenuItems();
	}
	
	public void selectView(DirectoryView dv) {
		m_dvSelected = dv;
		setSaveOptions();
		activateMenuItems();
		// System.out.println("selected view of " + dv.getDirectory().getVolume().getImageName());
	}
	
	void unselectAllViews() {
		m_dvSelected = null;
//		System.out.println("unselect current view");
	}

	public DirectoryView getSelectedView() {
		return m_dvSelected;
	}

	public void saveToDisk(byte[] content, boolean bAsTifile) throws IOException {
		JFileChooser jfc = null;
		if (m_flSourceDirectory!=null) jfc = new JFileChooser(m_flSourceDirectory);
		else jfc = new JFileChooser();
		Dimension dim = m_Settings.getPropertyDim(FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);
		
		if (bAsTifile) {
			TFIFileFilter im = new TFIFileFilter();
			jfc.addChoosableFileFilter(im);
			jfc.setFileFilter(im);
		}
		int nReturn = jfc.showSaveDialog(m_frmMain);
		if (nReturn == JFileChooser.APPROVE_OPTION) {
			m_Settings.put(FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
			java.io.File iofile = jfc.getSelectedFile();
			if (bAsTifile && !(iofile.getName().endsWith(".tfi") 
				|| iofile.getName().endsWith(".tifile") 
				|| iofile.getName().endsWith(".tifiles"))) {
				iofile = new java.io.File(iofile.getAbsolutePath() + ".tfi");
			}
			
			setSourceDirectory(iofile.getParentFile(), "impexp");

			FileOutputStream fos = new FileOutputStream(iofile);
			fos.write(content);
			fos.close();
			// JOptionPane.showMessageDialog(m_frmMain, "Export completed sucessfully", "Export files", JOptionPane.INFORMATION_MESSAGE);				
		}	
	}
	
	public String createValidInputFileName(String name) {
		// Strip off extension
		if (name.endsWith(".tfi") || name.endsWith(".tifile") || name.endsWith(".tifiles")) {
			name = name.substring(0, name.lastIndexOf("."));
		}
		try {
			name = transliterate(name, ".", "_");
		}
		catch (ReplaceTableException rx) {
			rx.printStackTrace();
		}
		catch (InvalidNameException ix) {
			ix.printStackTrace();
		}
		if (name.length() > 10) name = name.substring(0,10);
		
		if (m_Settings.getPropertyBoolean(FORCEUPPER)) name = name.toUpperCase();
		return name;
	}
	
	private String transliterate(String sExpFile, String sSubstSrc, String sSubstTar) throws ReplaceTableException, InvalidNameException {
		StringBuilder sbNewFile = new StringBuilder();
		if (sSubstSrc.length() > 0) {
			if (sSubstTar.length() != sSubstSrc.length()) {
				throw new ReplaceTableException();
			}
			
			for (int i=0; i < sExpFile.length(); i++) {
				char ch = sExpFile.charAt(i);
				if (ch>32 && ch<127) {
					int nPos = sSubstSrc.indexOf(ch);
					if (nPos != -1) {
						char chNew = sSubstTar.charAt(nPos);
						if (chNew != '?') sbNewFile.append(sSubstTar.charAt(nPos));
					}
					else sbNewFile.append(ch);
				}
				else {
					String hex = Utilities.toHex((int)ch, 2, true);
					sbNewFile.append('=').append(hex);
				}
			}
		}
		if (sbNewFile.length()==0) {
			m_nUnnamedFileCount++;
			sbNewFile.append(langstr("Unnamed")).append(m_nUnnamedFileCount);
		}
		return sbNewFile.toString();
	}
	
	/** 
		Called from ExportImageAction and SaveTFIAction. Recursion inside. 
		Delivers a list of File objects for DnD.
	*/	
	public List<java.io.File> exportDirectory(Directory dirCurrent, java.io.File iofBaseDir, List<Element> selected, boolean deleteOnExit) throws ReplaceTableException, InvalidNameException, IOException, FileNotFoundException, ImageException {
		List<java.io.File> lst = new ArrayList<java.io.File>();
		
		String value = m_Settings.getPropertyString(CONVERT, "/\\*><: __x___");
		String fromList = null;
		String toList = null;

		int separ = value.indexOf(" ");

		// For safety
		if (separ==-1) {
			value = "/\\*><: __x___";
			m_Settings.put(CONVERT, value);
			separ = value.indexOf(" ");
		}
		
		fromList = value.substring(0,separ);
		toList = value.substring(separ+1);

		for (TFile file : dirCurrent.getFiles()) {
			// selected is null for ExportImageAction. In that case, every file is selected.
			if (selected != null) {
				boolean bFound = false;
				for (Element el : selected) {
					if (el.equals(file)) bFound = true;
				}
				if (!bFound) continue;				
			}
			
			String sExpFile = file.getName();
			
			byte[] abyCont = null;
			TIFiles tif = TIFiles.createFromFile(file);
			abyCont = tif.toByteArray();
			
			// Transform the file name
			if (m_Settings.getPropertyBoolean(UNDERSCORE)) sExpFile = transliterate(sExpFile, "_", ".");
			
			sExpFile = transliterate(sExpFile, fromList, toList);
			
			if (m_Settings.getPropertyBoolean(EXPLOWER)==true) sExpFile = sExpFile.toLowerCase();
			sExpFile += m_Settings.getPropertyString(SUFFIX, ".tfi");
						
			// Create full pathname for exporting
			java.io.File iofSave = new java.io.File(iofBaseDir, sExpFile);
			if (deleteOnExit) iofSave.deleteOnExit();
			// Save the file
			FileOutputStream fos = new FileOutputStream(iofSave);
			fos.write(abyCont);
			fos.close();

			lst.add(iofSave);
		}
		for (Directory dir : dirCurrent.getDirectories()) {
			// selected is null for ExportImageAction.
			if (selected != null) {
				boolean bFound = false;
				for (Element el : selected) {
					if (el.equals(dir)) bFound = true;
				}
				if (!bFound) continue;				
			}

			// If a directory is selected, all of its contents are also selected.
			
			// Transform the directory name
			String sDirName = dir.getName();
			sDirName = transliterate(sDirName, fromList, toList);

			if (m_Settings.getPropertyBoolean(EXPLOWER)==true) sDirName = sDirName.toLowerCase();
			
			// create new directory
			java.io.File iofNewDir = new java.io.File(iofBaseDir, sDirName);
			iofNewDir.mkdir();
			lst.add(iofNewDir);
			if (deleteOnExit) iofNewDir.deleteOnExit();
			
			// Create a metafile for the directory information
			Properties propDir = new Properties();
			propDir.put("dirname", dir.getName());
			java.io.File metainf = new java.io.File(iofNewDir, "meta.inf");
			if (deleteOnExit) metainf.deleteOnExit();
			
			FileWriter fw = new FileWriter(metainf);
			propDir.store(fw, "Directory information");
			fw.close();
			
			// Recurse into the selected subdirectory. All of its files are selected. 
			exportDirectory(dir, iofNewDir, null, deleteOnExit);
		}
		return lst;
	}
	
	// TODO: Move this into Directory
	public boolean putTIFileIntoImage(Directory dir, DirectoryView dvCurrent, byte[] abyTif, String sDefaultFilename) throws ProtectedException, InvalidNameException, FileNotFoundException, ImageException, IOException {

		String sName = createValidInputFileName(sDefaultFilename);
			
		boolean bInserted = true;
		// Shall we keep the external filename or take the name in the TIFILES header?
		if (m_Settings.getPropertyBoolean(KEEPNAME)==false)  
			sName = TIFiles.getName(abyTif);

		// What if there is no valid name in TIFILES?
		if (sName.equals(TIFiles.NONAME)) {
			sName = createValidInputFileName(sDefaultFilename);
			dir.insertFile(abyTif, sName, false);
		}
		else {
			// Is this a TIFILES file?
			if (TIFiles.hasHeader(abyTif)) {
				dir.insertFile(abyTif, sName, false);
				// commit is in caller (ImportFilesAction)
				return true;
			}
			
			if (sName.equals(TIFiles.NOHEADER)) {
				if (TIFiles.hasFDRPrefix(abyTif)) {
					try {
						TIFiles.convertFDRToTFI(abyTif);
						sName = TIFiles.getName(abyTif);
					}
					catch (FormatException fx) {
						System.err.println(langstr("MainTFIError"));
					}
					dir.insertFile(abyTif, sName, false);
				}
				else {					
					if (Archive.hasPlainArchiveFormat(abyTif)) {
						// System.out.println(sDefaultFilename  + " has plain archive format");
						sName = createValidInputFileName(sDefaultFilename);
						abyTif = TIFiles.createTfi(abyTif, sName, (byte)0x00, 128, (abyTif.length-1)/128+1);						
						dir.insertFile(abyTif, sName, false);
						bInserted = true;
					}
					else {					
						ImportContentAction ia = new ImportContentAction();
						ia.setLinks(this, m_frmMain, m_Settings);
						abyTif = ia.convertForImport(abyTif, dvCurrent, createValidInputFileName(sDefaultFilename));
						if (abyTif != null) {
							sName = TIFiles.getName(abyTif);
							dir.insertFile(abyTif, sName, false);
						}
						else bInserted = false;
					}
				}
			}
			else {
				boolean bValid = true;
				// Catch cases where the file name is unplausible
				for (int i=0; i < sName.length(); i++) {
					if (sName.charAt(i) < 32 || sName.charAt(i)>126) bValid = false;
				}
				if (!bValid) {
					String sGivenName = sName;
					sName = createValidInputFileName(sDefaultFilename);
				}
				ImportContentAction ia = new ImportContentAction();
				ia.setLinks(this, m_frmMain, m_Settings);
				ia.convertAndImport(abyTif, dvCurrent, sName, false);
			}
		}
		return bInserted;
	}

	// TODO: Move this into Directory
	public boolean putBinaryFileIntoImage(Directory dir, byte[] abyTif, String sDefaultFilename, boolean bMulti, boolean bGuess) throws ProtectedException, InvalidNameException, FileNotFoundException, ImageException, IOException {
		String sName = createValidInputFileName(sDefaultFilename);
		ImportDialog impdia = new ImportDialog(m_frmMain, sName, bMulti, ImportDialog.BINARY);
		impdia.setContent(abyTif);
		impdia.createGui();
		impdia.setVisible(true);
		if (impdia.confirmed()) {
			abyTif = TIFiles.createTfi(abyTif, impdia.getFileName(), TFile.PROGRAM, 0, 0);
			sName = impdia.getFileName();
		}
		else
			return bGuess;

		dir.insertFile(abyTif, sName, false);
		return bGuess;
	}		
	
	/** Triggered when a mouse action is performed. The associated activity
		is retrieved from the map and executed. This is the main function 
		for user action handling. */
	public void actionPerformed(ActionEvent ae) {
		Activity act = activities.get(ae.getActionCommand());
		if (act != null) setUserInput(act);			
	}
		
	public void componentHidden(ComponentEvent ce) { 	}
	public void componentMoved(ComponentEvent ce) { 	}
	public void componentResized(ComponentEvent ce) {	}
	public void componentShown(ComponentEvent ce) { 	}

	public void windowClosing(WindowEvent we) { 
		if (hasUnsavedChanges()) {
			int nRet = JOptionPane.showConfirmDialog(m_frmMain, TIImageTool.langstr("UnsavedChanges") + ". " + TIImageTool.langstr("ReallyQuit"), TIImageTool.langstr("Leaving"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (nRet == JOptionPane.NO_OPTION) return;
		}
		
		m_frmMain.dispose();
		for (DirectoryView dv : m_Views) {
			dv.close();
		}
		closeAllFrames();
		saveFrameSize();
		m_Settings.saveProperties();
	}
	
	public void windowClosed(WindowEvent we) {
		m_bRunning = false;
		synchronized(this) {
			notify();
		}
	}
	
	public void windowActivated(WindowEvent we) {
	//	System.out.println("Main window selected");
		// Determine the currently selected tab
		DirectoryView dv = determineSelectedTab();
		if (dv != null) selectView(dv);
		else unselectAllViews();
	}

	public void windowDeactivated(WindowEvent we) {
		// System.out.println("Main window unselected");
		unselectAllViews();
	}
	public void windowIconified(WindowEvent we) {	}
	public void windowDeiconified(WindowEvent we) {	}
	public void windowOpened(WindowEvent we) {	}

	public void stateChanged(ChangeEvent e) {
		DirectoryView dv = determineSelectedTab();
		if (dv != null) selectView(dv);
		else unselectAllViews();
		updateMemoryInfo();
	}

	/** Find the currently selected tab. There can be only one selected tab if there
		are tabs at all. */
	private DirectoryView determineSelectedTab() {
		if (m_jtViews == null) return null;
		int nIndex = m_jtViews.getSelectedIndex();
		if (nIndex != -1) {
			DirectoryPanel dp = (DirectoryPanel)m_jtViews.getComponentAt(nIndex);
			for (DirectoryView dv : m_Views) {
				if (dv.getPanel() == dp) return dv;
			}
		}
		return null;
	}
	
	/****************************************************
		Utility functions for menu activities
	****************************************************/
			
	public void setSourceDirectory(java.io.File dir, String sWhich) {
		if (!dir.getName().equals(".")) { // sets the property only for non-UNC paths
			m_flSourceDirectory = dir;
			try {
				m_Settings.put(sWhich + SOURCEDIR, m_flSourceDirectory.getCanonicalPath());
			}
			catch (IOException iox) {
				iox.printStackTrace();
			}
		}
	}
	
	public java.io.File getSourceDirectory(String sWhich) {
		String sDir = m_Settings.getPropertyString(sWhich + SOURCEDIR);
		if (sDir == null) return null;
		else return new java.io.File(sDir);
	}
		
	public java.io.File getTemporaryDirectory() {
		return m_tmpDir;
	}
	
	public void setBridgeEnabled(boolean bEnable) {
		m_iSerialBridge.setEnabled(bEnable);
	}
	
	public void terminate() {
		saveFrameSize();
		m_Settings.saveProperties();
		m_bRunning = false;
	}
	
	public void setSaveOptions() {
		boolean bMod = false;
		DirectoryView dv = getSelectedView();
		if (dv != null) {
			bMod = dv.getVolume().isModified();
		}
		m_iSave.setEnabled(bMod);
		m_iSaveAs.setEnabled(m_Views.size()>0);  // Always enable to allow for format change
		
		for (DirectoryView dva : m_Views) {
			if (dva.getVolume().isModified()) bMod = true;
		}
		// m_iSaveAll.setEnabled(bMod);
	}
	
	public int getColumnWidth(int count) {
		StringBuilder sb = new StringBuilder();
		while (count-- > 0) sb.append("X");
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(TIImageTool.dialogFont);
		return fm.stringWidth(sb.toString());
	}
	
	// ================= View handling ========================================
		
	/** Return the reference to the opened volume. This is to ensure consistency 
		with multiple views of the same volume. */
	public Volume getAlreadyOpenedVolume(String sImageName) {
		for (DirectoryView dv : m_Views) {
			String sName = dv.getVolume().getImageName();
			if (sName != null && sName.equals(sImageName)) return dv.getVolume();
		}
		return null;
	}
	
	public boolean hasAlreadyOpenedVolume(String sImageName) {
		for (DirectoryView dv : m_Views) {
			if (sImageName.equals(dv.getVolume().getImageName())) return true;
		}
		return false;
	}
	
	public void closeVolume(Volume vol, DirectoryView closingView) throws IOException {
		boolean stillInUse = false;
		for (DirectoryView dv : m_Views) {
			if (dv != closingView && dv.getVolume()==vol) stillInUse = true;
		}
		if (!stillInUse) vol.close();
	}
	
	/** Open a new view. We cannot rely on the tab count to find out whether 
		there is already a view, so we use a separate flag. */
	public void addDirectoryView(Directory dir) {
		boolean bAttach = true;
		if (!m_first) {
			bAttach = !m_Settings.getPropertyBoolean(NEWFRAME);
		}
		m_first = false;
		SwingUtilities.invokeLater(new NewDirectoryView(dir, bAttach));
		
		// If the image is not saved yet, do not add to the recent list
		if (!dir.getVolume().isMemoryImage()) {
			String sImageName = dir.getVolume().getImageName();
			addRecent(dir.getVolume().getImageName());
		}
	}
	
	DirectoryView getFocusedView() {
		DirectoryView dvFocused = null;
		for (DirectoryView dv : m_Views) {
			if (dv.isFocused()) {
				if (dvFocused != null) System.err.println(langstr("Main2Views"));
				dvFocused = dv;
			}
		}
		return dvFocused;
	}
					
	public void registerFrame(JFrame frm) {
		m_frames.add(frm);
	}
	
	public void closeFrame(JFrame frm) {
		frm.dispose();
		m_frames.remove(frm);
	}
	
	void closeAllFrames() {
		for (JFrame frm : m_frames) {
			frm.dispose();
		}
		m_frames.clear();
	}
	
	public void showTextContent(String name, String content) {
		SwingUtilities.invokeLater(new ContentShow(name, content, false));		
	}
	
	public void showEditTextContent(TFile file, String content) {
		SwingUtilities.invokeLater(new ContentEditShow(file, content));		
	}

	public void showSearchResult(String name, SearchResult[] content) {
		SwingUtilities.invokeLater(new SearchResultShow(name, content));		
	}
	
	public void showSectorEditor(String name, ImageFormat image) {
		SwingUtilities.invokeLater(new SectorEditorShow(name, image));		
	}

	public void showConsoleContent() {
		if (m_logFile != null) {
			StringBuilder sbConsoleContent = new StringBuilder();
			try {
				BufferedReader br = new BufferedReader(new FileReader(m_logFile));
				String line = null;
				while (br.ready()) {
					line = br.readLine();
					if (line != null) sbConsoleContent.append(line).append("\n");
				} 
				SwingUtilities.invokeLater(new ContentShow(langstr("ConsoleOutput"), sbConsoleContent.toString(), true, true));
			}
			catch (IOException iox) {
				System.setOut(System.out);
				System.setErr(System.err);		
				System.err.println(langstr("ConsoleOutputError"));
				m_logFile = null;
			}
		}
	}
	
	public void clearLogfile() {
		if (m_logFile != null) {
			try {
				FileOutputStream fos = new FileOutputStream(m_logFile, false); // don't append
				m_logger = new LogStream(fos, m_logger.getContentFrame());
				System.setOut(m_logger);
				System.setErr(m_logger);		
				if (m_logFile.length()>0) System.out.println("========================");
				
			}
			catch (FileNotFoundException fx) {
				System.setOut(System.out);
				System.setErr(System.err);
				System.err.println(langstr("ConsoleOutputError"));
				m_iConsole.setEnabled(false);
				m_logFile = null;
			}
		}
		else {
			System.setOut(System.out);
			System.setErr(System.err);
		}
	}
	
	public void addRecent(String recentFile) {
		int i=0;
		boolean found = false;
		
		for (String file : m_recent) {
			if (file.equals(recentFile)) { found = true; break; }  
			i++;
		}

		if (found) m_recent.remove(i);
		m_recent.add(0, recentFile);			
		
		if (m_recent.size()>10) m_recent.remove(10);
		
		m_mOpenRecent.removeAll();
		m_mOpenRecent.setEnabled(false);
		
		i = 0;
		StringBuilder sb = new StringBuilder();
		for (String file : m_recent) {
			m_mOpenRecent.add(createMenuItem(new OpenOneImageAction(file, i++)));
			if (sb.length()>0) sb.append(System.getProperty("path.separator"));
			sb.append(file);
		}
		m_Settings.put(RECENT, sb.toString());
		
		if (i > 0) m_mOpenRecent.setEnabled(true);   
	}
	
	public void saveContentFrameDimension(int width, int height) {
		m_Settings.put(CONTSIZE, width + "x" + height);
	}
	
	public Point getFrameLocation() {
		return m_frmMain.getLocationOnScreen();	
	}
	
	// ===============================================================
	// Clipboard operations
	
	public Transferable getClipboard() {
		return m_clipboard;
	}
	
	public boolean clipboardNotEmpty() {
		return m_cliploaded; 
	}
	
	public void setClipboard(Transferable t) {
		m_clipboard = t;
	}
	
	/** Clear the clipboard and unselect all files. */
	public void clearClipboard() {
		m_clipboard = null;
		for (DirectoryView dv : m_Views) {
			dv.setPasteEnabled(false);
		}
		m_markForCut = false;
	}
	
	public void setClipboardLoaded(boolean loaded) {
		m_cliploaded = loaded;
		for (DirectoryView dv : m_Views) {
			dv.activateEditMenu();
		}
	}
	
	// ===============================================================

	public void selectAll() {
		// m_dvSelected.getPanel().selectAll();
		// m_dvSelected.backpaintEntryLines();
		m_dvSelected.selectAll();
	}
	
	public void closeCurrentView() {
		if (m_dvSelected != null) {
			SwingUtilities.invokeLater(new CloseView(m_dvSelected));
		}
	}
	
	public void closeAllViews() {
		for (DirectoryView dv : m_Views) {
			SwingUtilities.invokeLater(new CloseView(dv));
		}
	}
	
	public boolean viewStillThere(DirectoryView dvSearched) {
		for (DirectoryView dv : m_Views) {
			if (dvSearched == dv) return true;
		}
		return false;
	}
	
	public void markForCut(boolean cut) {
//		System.out.println("mark for cut = " + cut);
//		Thread.currentThread().dumpStack();
		m_markForCut = cut;
//		refreshAllViews();
	}
	
	public boolean markedForCut() {
		return m_markForCut;
	}

	public void refreshPanel(Volume vol) {
		SwingUtilities.invokeLater(new ViewRefresher(vol));		
	}

	public void reloadVolume(Volume vol) throws FileNotFoundException, IOException, ImageException {
		// Volume volNew = new Volume(vol.getImageName());
		SwingUtilities.invokeLater(new ViewRefresher(vol, true));   // back to root directory
	}
	
	public void refreshAllViews() {
		SwingUtilities.invokeLater(new ViewRefresher(null));		
	}
	
	public void refresh(DirectoryView dvRefr) {
		
		// Create new Directory trees for all open volumes
		// but don't create different trees for the same volume
		Map<Volume,Directory> list = new HashMap<Volume,Directory>();
		for (DirectoryView dv : m_Views) {
			// System.out.println("refresh " + dv);
			if (dvRefr == null || dv.getVolume() == dvRefr.getVolume()) {
				Volume vol = dv.getVolume();
				Directory dir = list.get(vol);
				try {
					if (dir == null) {
						vol.buildTree();
						vol.readAllocationMap();
						dir = vol.traverse(dv.getPath());
						list.put(vol, dir);
					}
					dv.enterDirectory(dir);
				}
				catch (ImageException ix) {
					ix.printStackTrace();
				}
				catch (IOException iox) {
					iox.printStackTrace();
				}
			}
			SwingUtilities.invokeLater(new ViewRefresher(null));
		}
	}		
	
	void clearSelection(DirectoryView dvExcept) {
		for (DirectoryView dv : m_Views) {
			if (dv != dvExcept) dv.clearSelection();
		}		
	}
	
	void activateMenuItems() {
	//	System.out.println("activateMenuItems: dvSelected = " + (m_dvSelected != null));
		boolean bOpenImage = (m_dvSelected != null);
		boolean bImageIsSCSI = false;
		boolean bImageIsHFDC = false;
		
		if (bOpenImage) {
			bImageIsSCSI = m_dvSelected.getVolume().isSCSIImage();
			bImageIsHFDC = m_dvSelected.getVolume().isHFDCImage();
		}
		boolean bAlways = true;
		
		m_iNewFloppy.setEnabled(bAlways);
		m_iNewHD.setEnabled(bAlways);
		m_iNewIDE.setEnabled(bAlways);
		m_iOpen.setEnabled(bAlways);
		m_iClose.setEnabled(bOpenImage);
		m_iCloseAll.setEnabled(bOpenImage);
		m_iExport.setEnabled(bOpenImage);
		m_iPreferences.setEnabled(bAlways);
		m_iQuit.setEnabled(bAlways);
		
		m_iCheck.setEnabled(bOpenImage);
		m_iInstallGenOS.setEnabled(bOpenImage);
		m_iSerialBridge.setEnabled(m_bSerial);

		if (m_jtViews.getTabCount()==0) {
			m_mEdit.activateMenuItems(false, false, false, false, false, m_bSerial, bImageIsHFDC);
			m_mEdit.activateActionMenu(false);
		}
		else {
			if (m_dvSelected != null) 
				m_dvSelected.activateEditMenu();
		}
	}
	
	public EditMenu getEditMenu() {
		return m_mEdit;
	}
	
	public boolean hasUnsavedChanges() {
		for (DirectoryView dv : m_Views) {
			if (dv.getVolume().isModified()) return true;
		}
		return false;
	}
	
	public int nextUnnamedIndex() {
		return ++m_nUnnamedVolumeCount;
	}	
}
