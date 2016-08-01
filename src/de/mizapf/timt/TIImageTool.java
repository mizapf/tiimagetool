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

/*
	TODOs for 2.x
	-------------
	[x] Drag and Drop
	[x] Hints
    [x] Import text as BASIC
    [ ] Open CF7
    [ ] Search function
    [ ] Sector editor
    [x] Fix import of CRLF in imported texts
    [x] Fix plain dump for dis/fix 80 (ends too early)
    
    New for 2.1
    [x] Redirect standard output
    [x] Open recent
    [x] Fix copying of file sets with an empty file included 
    [x] Fix LONG format issue with small BASIC programs (programs that are too short cannot be saved as LONG)
    [x] Fix L3 issue; add to CheckFS ("The file xx has a swapped L3 count (see manual). Shall this be fixed?") 
    [x] Enhanced filesystem check, opens text window for results
    [x] Create sparsely labeled disassembly for PRG files
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.ByteArrayOutputStream;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
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

	public final static String VERSION = "2.2";
	public final static String DATE = "July 2016";
	
	private static final String TITLE = "TIImageTool";
	
	// ============= Properties ======================
	
	public final static String CONTFONT = "contentfont";
	public final static String LOOKANDFEEL = "lookandfeel";
	public final static String TEMPDIR = "tempdir";
	public final static String WINDOWSIZE = "windowsize";
	public final static String FILEDIALOG = "fdialogsize";
	public final static String SOURCEDIR = "sourcedir";
	public final static String EXPLOWER = "lower";
	public final static String CONVERT = "convert";
	public final static String UNDERSCORE = "underscore";
	public final static String SUFFIX = "suffix";
	public final static String KEEPNAME = "keepname";
	public final static String FORCEUPPER = "forceupper";
	public final static String HINTS = "hints";
	public final static String HINTSTART = "hintstart";
	public final static String BASICVER = "basicver";
	public final static String TFIFILTER = "tfifilter";
	public final static String NEWFRAME = "newframe";
	public final static String ESCAPE = "escape";
	public final static String LOGFILE = "logging";
	public final static String RECENT = "recent";
	
	Properties m_propNames;
	
	// ============ Menu =============================

	JMenuBar m_mbar; 

	JMenu m_mFile;
	JMenu m_mNew;
	JMenuItem m_iNewFloppy;
	JMenuItem m_iNewHD;
	JMenuItem m_iOpen;
	JMenu	  m_mOpenRecent;
	JMenuItem m_iClose;
	JMenuItem m_iCloseAll;
	JMenuItem m_iExport;
	JMenuItem m_iPreferences;
	JMenuItem m_iQuit;

	EditMenu m_mEdit;

	JMenu m_mUtility;
	JMenuItem m_iConsole;
	JMenuItem m_iCheck;
	JMenuItem m_iToScsi;
	JMenuItem m_iToHfdc;
	JMenuItem m_iChangeCHDFormat;
	JMenuItem m_iCHDToRaw;
	JMenuItem m_iRawToCHD;
	JMenuItem m_iInstallGenOS;
	JMenuItem m_iSerialBridge;

	JMenuItem m_iNewFile;
	JMenuItem m_iNewDirectory;
	
	JMenu	  m_mHelp;
	
	BackTabbedPane m_jtViews;

	LinkedList<DirectoryView> m_Views;	

	/** Count for unnamed files (see export) */
	int m_nUnnamedCount;
	
	/** Thread for background activity. */
	Thread m_thrBackground;
	
	/** Selected view */
	DirectoryView m_dvSelected;
		
	public final static String GPLPREFIX = "gpl";
	public final static String ASMPREFIX = "asm";
	
	/** Console output buffer. */
	java.io.File m_logFile;
	
	// ===============================================
	
	/** Directory on the PC file system where to open the file dialog. */
	java.io.File m_flSourceDirectory = null;
	
	public final static String TEMPDIRNAME = "tiimagetool_tmp";
	
	Properties m_Settings;
	String m_sPropertiesPath;

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
	
	// ===============================================
	// Clipboard for cut-copy-paste
	
	Transferable m_clipboard;
	
	boolean m_cliploaded;
	
	public static String FONT = Font.SANS_SERIF;

	public static String contentFont;
	public static int plainHeight;
	public static int boldHeight;
	public static int dialogHeight;
	public static Font plainFont;
	public static Font boldFont;
	public static Font dialogFont;
	public static Font boldDialogFont;
	
	private static final int MAXDEPTH = 10;
	
	private static final Color HOVE = new Color(0, 230, 230);
	
	private static final String BACKGROUND = "background.jpg";
	
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
			plainFont = new Font(FONT, Font.PLAIN, 12); 
			LineMetrics lm = plainFont.getLineMetrics("XX_X", 0, 2, frc);
			
			plainHeight = (int)Math.ceil(lm.getHeight()*1.03);
			boldFont = new Font(FONT, Font.BOLD, 12);
			lm = boldFont.getLineMetrics("XXX", 0, 2, frc);
			boldHeight = (int)Math.round(lm.getHeight());
			dialogFont = new Font(FONT, Font.PLAIN, 12);	
			boldDialogFont = new Font(FONT, Font.BOLD, 12);	
			lm = dialogFont.getLineMetrics("XXX", 0, 2, frc);
			dialogHeight = (int)Math.round(lm.getHeight());
			
			m_jtViews = new BackTabbedPane();
			cnt.add(m_jtViews);
			m_jtViews.addChangeListener(TIImageTool.this);
			
			// -------------------------------------------
			m_mFile = new JMenu("File");
			m_mFile.setFont(dialogFont);
			m_mbar.add(m_mFile);
			
			m_mNew = new JMenu("New");
			m_mNew.setFont(dialogFont);
			m_mFile.add(m_mNew);
			
			m_iNewFloppy = createMenuItem(new NewImageAction());
			m_iNewHD = createMenuItem(new NewHDImageAction());
			m_mNew.add(m_iNewFloppy);
			m_mNew.add(m_iNewHD);
			m_iOpen = createMenuItem(new OpenImageAction());
			m_mFile.add(m_iOpen);

			m_mOpenRecent = new JMenu("Open recent file");
			m_mFile.add(m_mOpenRecent);
			m_mOpenRecent.setFont(dialogFont);
			
			// Fill recent menu
			int i = 0;
			String recents = getPropertyString(RECENT);
			if (recents != null && recents.length() > 0) {
				String[] recent = recents.split(System.getProperty("path.separator"));
				for (String s : recent) {
					m_mOpenRecent.add(createMenuItem(new OpenOneImageAction(s, i++)));
					m_recent.add(s);
				}
			}
			m_mOpenRecent.setEnabled(i > 0);
			
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
			m_mEdit = new EditMenu(m_frmMain, TIImageTool.this); 
			m_mbar.add(m_mEdit);
			
			// -------------------------------------------
			
			m_mUtility = new JMenu("Utility");
			m_mbar.add(m_mUtility);
			m_mUtility.setFont(dialogFont);
			
			m_iConsole = createMenuItem(new ViewConsoleAction());
			m_mUtility.add(m_iConsole);
			m_mUtility.addSeparator();
			
			if (m_logFile == null) m_iConsole.setEnabled(false);

			m_iCheck = createMenuItem(new CheckFSAction());
			m_mUtility.add(m_iCheck);
			m_iInstallGenOS = createMenuItem(new InstallGenOSAction());
			m_mUtility.add(m_iInstallGenOS);
			
			m_mUtility.addSeparator();
			
			m_iToHfdc = createMenuItem(new ConvertToHFDCAction());
			m_mUtility.add(m_iToHfdc);
			
			m_iToScsi = createMenuItem(new ConvertToSCSIAction());
			m_mUtility.add(m_iToScsi);
			m_mUtility.addSeparator();
			
			m_iChangeCHDFormat = createMenuItem(new ChangeCHDFormatAction());
			m_mUtility.add(m_iChangeCHDFormat);
			
			m_iCHDToRaw = createMenuItem(new CHDRawAction());
			m_mUtility.add(m_iCHDToRaw);
			
			m_iRawToCHD = createMenuItem(new RawCHDAction());
			m_mUtility.add(m_iRawToCHD);
			
			m_mUtility.addSeparator();
			m_iSerialBridge = createMenuItem(new SerialBridgeAction());
			m_mUtility.add(m_iSerialBridge);
			
			// -------------------------------------------
			
			m_mHelp = new JMenu("Help");
			m_mbar.add(m_mHelp);
			m_mHelp.setFont(dialogFont);
			
			m_mHelp.add(createMenuItem(new HelpAction()));
			m_mHelp.add(createMenuItem(new HintAction()));
			m_mHelp.addSeparator();
			m_mHelp.add(createMenuItem(new AboutAction()));
			
			// -------------------------------------------
			m_frmMain.setJMenuBar(m_mbar);	
						
			try {
				// Check for presence of RXTX
				// If not available, block the remote functions
				Class cls = Class.forName("gnu.io.SerialPort");
				m_bSerial = true;
			}
			catch (ClassNotFoundException cnfx) {
				m_bSerial = false;
			}
				
			activateMenuItems();
			
			Dimension dim = getPropertyDim(WINDOWSIZE);
			if (dim==null) dim = new Dimension(800,600);
			
			m_frmMain.setSize(dim);
			//		m_frmMain.repaint();
			m_frames = new HashSet<JFrame>();

			// Let the hint window pop up on start
			if (getPropertyBoolean(HINTSTART)) setUserInput("HINTS");
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
			String sShortName = dv.getDirectory().getVolume().getShortImageName();
			m_jtViews.addTab(sName, dv.getPanel());
			int index = m_jtViews.indexOfComponent(dv.getPanel());
			m_jtViews.setTabComponentAt(index, new CloseableTab(sShortName, dv));
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
			m_dv.close();
			m_Views.remove(m_dv);
			m_jtViews.remove(m_dv.getPanel());
			if (m_Views.size()==0) m_first = true;
			activateMenuItems();
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
			DirectoryView dv = new DirectoryView(m_dirdv, m_bAttachdv, TIImageTool.this);
			m_Views.add(dv);
			activateMenuItems();
		}
	}
	
	class ViewRefresher implements Runnable {
		Volume volume;
		boolean backtoroot;	
		
		ViewRefresher(Volume vol) {
			volume = vol;
			backtoroot = false;
		}

		ViewRefresher(Volume vol, boolean reset) {
			volume = vol;
			backtoroot = reset;
		}
		
		public void run() {
			for (DirectoryView dv : m_Views) {
				if (volume == null) {
					dv.refreshView();
				}
				else {
					if (dv.getVolume().equals(volume))
						dv.refreshView();
					if (backtoroot) dv.gotoRootDirectory();
				}
			}
		}
	}
		
	class ContentShow implements Runnable {
	
		String name;
		String content;
		boolean withClear;
		
		ContentShow(String sname, String scontent, boolean bWithClear) {
			name = sname;
			content = scontent;
			withClear = bWithClear;
		}
		
		public void run() {
			ContentFrame cf = new ContentFrame(name, TIImageTool.this, withClear);
			cf.createGui(content, contentFont);
			Point loc = m_frmMain.getLocationOnScreen();		
			cf.setLocation(loc.x+20, loc.y+20);
			cf.setLocationByPlatform(true);
			cf.setVisible(true);
			cf.pack();
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
				System.err.println("Error: Could not locate background image in package " + imgURL);
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
		public CloseableTab(String sLabel, DirectoryView dv) {
			super(new FlowLayout(FlowLayout.LEFT, 0, 0));
			setOpaque(false);
			JLabel label = new JLabel(sLabel);
			add(label);
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
			add(new DetachTabButton(dv));
			add(new CloseTabButton(dv));
			setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		}
	}

	private class CloseTabButton extends JButton implements ActionListener, MouseListener {
		private DirectoryView m_dv;
		
		public CloseTabButton(DirectoryView dv) {
			m_dv = dv;
			int size = 17;
			setPreferredSize(new Dimension(size, size));
			setToolTipText("Close this view");
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
			SwingUtilities.invokeLater(new CloseView(m_dv));
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
			setToolTipText("Show as window");
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
				try {
					// Check for presence of RXTX
					// If not available, block the remote functions
					Class cls = Class.forName("gnu.io.SerialPort");
				}
				catch (ClassNotFoundException cnfx) {
					System.err.println("FATAL: Serial library missing, implementing gnu.io.SerialPort (like RXTX).");
					return;
				}
				
				if (arg.length>2) {
					String sSerial = arg[1];
					String sPort = arg[2];
					SerialBridgeAction act = new SerialBridgeAction();
					act.setup(sSerial, sPort);
				}
				else
					System.err.println("Missing parameters for BRIDGE; usage: BRIDGE <serialdev> <port>");
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
		if (arg.length>0) {
			ie.openFromCommandLine(new java.io.File(arg[0]));
		}		
		ie.processUserInput();
	}
	
	TIImageTool() {
		m_sPropertiesPath = null;
		m_Settings = new Properties();
		loadProperties();

		// Redirect Console output
		String sLogFile = getPropertyString(LOGFILE, "tiimagetool.log");
		if (sLogFile.length()>0) {
			try {
				m_logFile = new java.io.File(sLogFile); 
				FileOutputStream fos = new FileOutputStream(m_logFile, true); // append
				PrintStream psConsole = new PrintStream(fos);
				System.setOut(psConsole);
				System.setErr(psConsole);
				if (m_logFile.length()>0) System.out.println("========================");
			}
			catch (FileNotFoundException fx) {
				System.err.println("Cannot open log file. Outputting to console.");
			}
		}
		
		FONT = m_Settings.getProperty("uifont", Font.SANS_SERIF);
		contentFont = m_Settings.getProperty(CONTFONT);
		if (contentFont == null) setProperty(CONTFONT, Font.MONOSPACED);
		m_frmMain = new JFrame(TITLE);
		m_frmMain.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		m_frmMain.addWindowListener(this);
		m_frmMain.addComponentListener(this);
			
		activities = new HashMap<String, Activity>();
		m_clipboard = null;
		m_cliploaded = false;
		m_bRunning = true;	
		m_first = true;
		
		m_nUnnamedCount = 0;
		
		// Set the look and feel
		String lafclass = getPropertyString(LOOKANDFEEL, "javax.swing.plaf.metal.MetalLookAndFeel");
		try {
			UIManager.setLookAndFeel(lafclass);
		}
		catch (UnsupportedLookAndFeelException e) {
			System.err.println("Unsupported look-and-feel: " + lafclass);
		}
		catch (ClassNotFoundException e) {
			System.err.println("Could not find look-and-feel class: " + lafclass);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		m_Views = new LinkedList<DirectoryView>();

		m_recent = new LinkedList<String>();
		
		// Create temporary directory
		String sTempDir = m_Settings.getProperty(TEMPDIR, System.getProperty("java.io.tmpdir"));
		if (sTempDir == null || sTempDir.length() == 0) sTempDir = "."; 

		m_tmpDir = new java.io.File(sTempDir, TEMPDIRNAME);
		if (!m_tmpDir.exists()) {
			m_tmpDir.mkdir();
			System.out.println("Created new tiimagetool temporary dir: " + m_tmpDir.getName());
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
			InputStream is = ToolDialog.class.getResourceAsStream("hints.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
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
			System.err.println("Error in hint file: " + hint);
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
		m_turnedoff = new byte[maxnumber / 8 +1];

		String hintnotes = getPropertyString(HINTS,"0");
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
			System.err.println("Error when reading the hint flags: " + nfx.getMessage());
		}
		saveHintSettings();
		
		SwingUtilities.invokeLater(new CreateGui());
	}

	public JFrame getMainFrame() {
		return m_frmMain;
	}
	
	void saveHintSettings() {
		StringBuilder sb = new StringBuilder();
		sb.append(Utilities.toHex(m_hinthash, 8, true));
		for (int i=0; i < m_turnedoff.length; i++) {
			sb.append(",").append((int)m_turnedoff[i] & 255);
		}
		setProperty(HINTS, sb.toString());
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
		if (!found) return "you know all the hints by now!";
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

		if (notInterested) setProperty(HINTSTART, "false");
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

	private void loadProperties() {
		String sFile = null;

		if (System.getProperty("os.name").startsWith("Windows")) sFile="tiimagetool.prop";
		else sFile = ".tiimagetoolrc";
				
		// Load the property texts
		m_propNames = new Properties();
		try {
       		m_propNames.load(ToolDialog.class.getResourceAsStream("names.prop"));
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}		
		
		// Try to load
		// 1. from the current directory
		// 2. from the special path 
		FileReader fr = null;
		try {
			fr = new FileReader(sFile);
		}
		catch (FileNotFoundException fx) {
			sFile = System.getProperty("user.home") + System.getProperty("file.separator") + sFile; 
		}
		try {
			fr = new FileReader(sFile);
		}
		catch (FileNotFoundException fx) {
			// Try to get the value and pre-set it if not available 
			setDefaults();
			saveProperties(); // No properties
			return;
		}
		m_sPropertiesPath = sFile;
		try {
			m_Settings.load(fr);
		}
		catch (IOException iox) {
			System.err.println("Error loading properties ... using defaults.");
		}
		
		setDefaults();
		
		try {
			fr.close();
		}
		catch (IOException iox1) {
			System.err.println("Error on close after loading properties."); 
			iox1.printStackTrace();
		}
	}
	
	private void setDefaults() {
		getPropertyString(CONVERT, "/\\* __x");
		getPropertyString(SUFFIX, ".tfi");
		getPropertyString(UNDERSCORE, "true");		
		getPropertyString(EXPLOWER, "true");	
		getPropertyString(CONVERT, "true");	
		getPropertyString(KEEPNAME, "false");	
		getPropertyString(FORCEUPPER, "true");	
		getPropertyString(HINTSTART, "true");
		getPropertyString(BASICVER, "true");
		getPropertyString(TFIFILTER, "true");
		getPropertyString(NEWFRAME, "false");
		getPropertyString(ESCAPE, ".");
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
	
	public void saveProperties() {
		if (m_frmMain != null) setProperty(WINDOWSIZE, m_frmMain.getWidth() + "x" + m_frmMain.getHeight());
		else setProperty(WINDOWSIZE, "640x480");
		if (m_sPropertiesPath == null) {
			String sFile = null;
			if (System.getProperty("os.name").startsWith("Windows")) sFile="tiimagetool.prop";
			else sFile = ".tiimagetoolrc";
			m_sPropertiesPath = System.getProperty("user.home") + System.getProperty("file.separator") + sFile; 			
		}
		
		try {
			FileWriter fw = new FileWriter(m_sPropertiesPath);
			m_Settings.store(fw, "Settings for TIImageTool");
			fw.close();
		}
		catch (IOException iox) {
			System.err.println("Error storing properties");
			iox.printStackTrace();
		}
	}
	
	public void setProperty(String prop, String value) {
		m_Settings.put(prop, value);
	}

	public Dimension getPropertyDim(String sKey) {
		Dimension dim = null; 
		String sVal = m_Settings.getProperty(sKey);
		if (sVal!=null && (sVal.indexOf("x")!=-1)) {
			String[] asPart = sVal.split("x");
			try {
				dim = new Dimension(Integer.parseInt(asPart[0]), Integer.parseInt(asPart[1]));
			}
			catch (NumberFormatException nfx) {
				System.err.println("Invalid window dimension in property " + sKey);
			}
			catch (ArrayIndexOutOfBoundsException ax) {
				System.err.println("Invalid window dimension in property " + sKey);
			}
		}
		return dim;		
	}
	
	public String getPropertyString(String sKey) {
		return m_Settings.getProperty(sKey);
	}	
	
	public String getPropertyString(String sKey, String def) {
		String value = m_Settings.getProperty(sKey);
		if (value==null) {
			value = def;
			m_Settings.put(sKey, value);
		}
		return value;
	}	
	
	public boolean getPropertyBoolean(String key) {
		String value = m_Settings.getProperty(key);
		if (value==null) value = "false";
		return (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
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
		m_Settings.put(sPrefix + "_" + sFile, sb.toString());
	}
	
	public String loadDisassParams(String sPrefix, String sFile) {
		String sValues = m_Settings.getProperty(sPrefix + "_" + sFile);
		return sValues;
	}
	
// JComponent.getComponentGraphics -> getFontMetrics -> stringWidth

	public JMenuItem createMenuItem(Activity act) {
		JMenuItem mi = new JMenuItem(act.getMenuName());
		mi.setActionCommand(act.getActionName());
		mi.addActionListener(this);
		registerActivity(act);
		mi.setFont(dialogFont);
		if (act.getKeyCode()!=0) mi.setAccelerator(KeyStroke.getKeyStroke(act.getKeyCode(), act.getModifier()));		
		return mi;
	}

	public void registerActivity(Activity act) {
		act.setLinks(this, m_frmMain);
		activities.put(act.getActionName(), act);
	}
	
	void processUserInput() {
		while (m_bRunning) {
			synchronized(this) {
				if (m_UserInput != null) {
					try {
						m_UserInput.go();
					}
					catch (Exception e) {
						m_frmMain.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						e.printStackTrace();
						JOptionPane.showMessageDialog(m_frmMain, "BUG: Exception occured; see console output", "Internal error", JOptionPane.ERROR_MESSAGE);						
					}
					m_UserInput = null;
					// SwingUtilities.invokeLater(new ActivateMenuItems());
				}
				try {
					if (m_bRunning) wait();
				}
				catch (InterruptedException ix) {
					JOptionPane.showMessageDialog(m_frmMain, "BUG: Wait interrupted in processUserInput", "Internal error", JOptionPane.ERROR_MESSAGE); 
				}
				m_bAction = false;
			}
		}
	}

	public boolean userActionInProgress() {
		return m_bAction;
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

	private void openFromCommandLine(java.io.File file) {
		OpenImageAction act = new OpenImageAction();
		act.setLinks(this, m_frmMain);
		act.openLine(file);
	}
	
	public Dimension getFrameSize() {
		return m_frmMain.getSize();		
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
//		System.out.println("selected view of " + dv.getDirectory().getVolume().getImageName());
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
		Dimension dim = getPropertyDim(FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);
		
		if (bAsTifile) {
			TFIFileFilter im = new TFIFileFilter();
			jfc.addChoosableFileFilter(im);
			jfc.setFileFilter(im);
		}
		int nReturn = jfc.showSaveDialog(m_frmMain);
		if (nReturn == JFileChooser.APPROVE_OPTION) {
			setProperty(FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
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
		catch (InvalidNameException ix) {
			ix.printStackTrace();
		}
		if (name.length() > 10) name = name.substring(0,10);
		
		if (getPropertyBoolean(FORCEUPPER)) name = name.toUpperCase();
		return name;
	}
	
	private String transliterate(String sExpFile, String sSubstSrc, String sSubstTar) throws InvalidNameException {
		StringBuilder sbNewFile = new StringBuilder();
		if (sSubstSrc.length() > 0) {
			if (sSubstTar.length() != sSubstSrc.length()) {
				throw new InvalidNameException(".REPLACE");
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
			m_nUnnamedCount++;
			sbNewFile.append("unnamed").append(m_nUnnamedCount);
		}
		return sbNewFile.toString();
	}
	
	/** 
		Called from ExportImageAction and SaveTFIAction. Recursion inside. 
		Delivers a list of File objects for DnD.
	*/	
	public List<java.io.File> exportDirectory(Directory dirCurrent, java.io.File iofBaseDir, List<Element> selected, boolean deleteOnExit) throws InvalidNameException, IOException, FileNotFoundException, ImageException {
		List<java.io.File> lst = new ArrayList<java.io.File>();
		
		String value = getPropertyString(CONVERT, "/\\* __x");
		String fromList = null;
		String toList = null;

		int separ = value.indexOf(" ");

		// For safety
		if (separ==-1) {
			value = "/\\* __x";
			setProperty(CONVERT, value);
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
			if (getPropertyBoolean(UNDERSCORE)) sExpFile = transliterate(sExpFile, "_", ".");
			
			sExpFile = transliterate(sExpFile, fromList, toList);
			
			if (getPropertyBoolean(EXPLOWER)==true) sExpFile = sExpFile.toLowerCase();
			sExpFile += getPropertyString(SUFFIX, ".tfi");
						
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

			if (getPropertyBoolean(EXPLOWER)==true) sDirName = sDirName.toLowerCase();
			
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
	
	public void putTIFileIntoImage(Directory dir, DirectoryView dvCurrent, byte[] abyTif, String sDefaultFilename) throws ProtectedException, InvalidNameException, FileNotFoundException, ImageException, IOException {

		String sName = createValidInputFileName(sDefaultFilename);
			
		// Shall we keep the external filename or take the name in the TIFILES header?
		if (getPropertyBoolean(KEEPNAME)==false)  
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
				return;
			}
			
			if (sName.equals(TIFiles.NOHEADER)) {
				if (TIFiles.hasFDRPrefix(abyTif)) {
					try {
						TIFiles.transformPrefix(abyTif);
						sName = TIFiles.getName(abyTif);
					}
					catch (FormatException fx) {
						System.err.println("BUG: TIFiles is unable to transform the header, although the test proved to be possible.");
					}
					dir.insertFile(abyTif, sName, false);
				}
				else {					
					if (Archive.hasPlainArchiveFormat(abyTif)) {
						// System.out.println(sDefaultFilename  + " has plain archive format");
						sName = createValidInputFileName(sDefaultFilename);
						abyTif = TIFiles.createTfi(abyTif, sName, (byte)0x00, 128, (abyTif.length-1)/128+1);						
						dir.insertFile(abyTif, sName, false);
					}
					else {					
						ImportContentAction ia = new ImportContentAction();
						ia.setLinks(this, m_frmMain);
						ia.convertAndImport(abyTif, dvCurrent, createValidInputFileName(sDefaultFilename), false);
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
				ia.setLinks(this, m_frmMain);
				ia.convertAndImport(abyTif, dvCurrent, sName, false);
			}
		}
	}

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
		m_frmMain.dispose();
		for (DirectoryView dv : m_Views) {
			dv.close();
		}
		closeAllFrames();
		saveProperties();
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
		m_flSourceDirectory = dir;
		try {
			m_Settings.put(sWhich + SOURCEDIR, m_flSourceDirectory.getCanonicalPath());
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
	}
	
	public java.io.File getSourceDirectory(String sWhich) {
		String sDir = m_Settings.getProperty(sWhich + SOURCEDIR);
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
		m_bRunning = false;
	}
	
	// ================= View handling ========================================
		
	/** Return the reference to the opened volume. This is to ensure consistency 
		with multiple views of the same volume. */
	public Volume getAlreadyOpenedVolume(String sImageName) {
		for (DirectoryView dv : m_Views) {
			if (dv.getImageName().equals(sImageName)) return dv.getVolume();
		}
		return null;
	}
	
	/** Open a new view. We cannot rely on the tab count to find out whether 
		there is already a view, so we use a separate flag. */
	public void addDirectoryView(Directory dir) throws FileExistsException {
		boolean bAttach = true;
		if (!m_first) {
			bAttach = !getPropertyBoolean(NEWFRAME);
		}
		m_first = false;
		SwingUtilities.invokeLater(new NewDirectoryView(dir, bAttach));
		addRecent(dir.getVolume().getImageName());
	}
	
	public void refreshPanel(Volume vol) {
		SwingUtilities.invokeLater(new ViewRefresher(vol));		
	}

	public void reloadVolume(Volume vol) throws FileNotFoundException, IOException, ImageException {
		Volume volNew = new Volume(vol.getImageName());
		SwingUtilities.invokeLater(new ViewRefresher(vol, true));
	}
	
	DirectoryView getFocusedView() {
		DirectoryView dvFocused = null;
		for (DirectoryView dv : m_Views) {
			if (dv.isFocused()) {
				if (dvFocused != null) System.err.println("** Two views have focus now **"); 
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
				SwingUtilities.invokeLater(new ContentShow("Console output", sbConsoleContent.toString(), true));
			}
			catch (IOException iox) {
				System.setOut(System.out);
				System.setErr(System.err);		
				System.err.println("Cannot open log file. Outputting to console.");
				m_logFile = null;
			}
		}
	}
	
	public void clearLogfile() {
		if (m_logFile != null) {
			try {
				FileOutputStream fos = new FileOutputStream(m_logFile, false); // don't append
				PrintStream psConsole = new PrintStream(fos);
				System.setOut(psConsole);
				System.setErr(psConsole);
			}
			catch (FileNotFoundException fx) {
				System.setOut(System.out);
				System.setErr(System.err);
				System.err.println("Cannot open log file. Outputting to console.");
				m_iConsole.setEnabled(false);
				m_logFile = null;
			}
		}
		else {
			System.setOut(System.out);
			System.setErr(System.err);
		}
	}
	
	void addRecent(String recentFile) {
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
		setProperty(RECENT, sb.toString());
		
		if (i > 0) m_mOpenRecent.setEnabled(true);   
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

	public void refreshAllViews() {
		SwingUtilities.invokeLater(new ViewRefresher(null));		
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
			bImageIsHFDC = !m_dvSelected.getVolume().isFloppyImage() && !bImageIsSCSI;
		}
		boolean bAlways = true;
		
		m_iNewFloppy.setEnabled(bAlways);
		m_iNewHD.setEnabled(bAlways);
		m_iOpen.setEnabled(bAlways);
		m_iClose.setEnabled(bOpenImage);
		m_iCloseAll.setEnabled(bOpenImage);
		m_iExport.setEnabled(bOpenImage);
		m_iPreferences.setEnabled(bAlways);
		m_iQuit.setEnabled(bAlways);
		
		m_iCheck.setEnabled(bOpenImage);
		m_iToScsi.setEnabled(bImageIsHFDC);
		m_iToHfdc.setEnabled(bImageIsSCSI);
		m_iInstallGenOS.setEnabled(bOpenImage);
		m_iSerialBridge.setEnabled(m_bSerial);

		if (m_jtViews.getTabCount()==0) {
			m_mEdit.activateMenuItems(false, false, false, m_bSerial);
		}
		else {
			m_dvSelected.activateEditMenu();
		}
	}
	
	public EditMenu getEditMenu() {
		return m_mEdit;
	}
}
