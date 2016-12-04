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
package de.mizapf.timt.ui;

import javax.swing.*;
import de.mizapf.timt.TIImageTool;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

class ReadWriteCFDialog extends ToolDialog {

	TIImageTool imagetool;
	JFrame m_parent;
	boolean m_windows;
	JTextField m_tfDevice;
	JTextField m_tfImageFile;
	JTextField m_tfddpath;
	boolean m_read;
	
	private final static int DEV = 1;
	private final static int FILE = 2;
	private final static int DD = 3;
	
	JTextField m_tfCommandLine;
			
	ReadWriteCFDialog(JFrame owner, TIImageTool timt, boolean windows, boolean read) {
		super(owner, read? "Reading CF card" : "Writing CF card");
		imagetool = timt;
		m_parent = owner;
		m_windows = windows;
		m_read = read;
	}	
	
/*
	| 	Read / Write CF card								|

		Provide the CF path. For Windows this is something like "e:", 
		for Linux or Mac this looks like "/dev/sdc"

		[1] CF device   _________   [btn]
		
		[2] File on file system    [.....] [btn]
		
		Path to dd program  ___/usr/bin/dd_____
		
		Command line ___dd.exe if=\\.\[1] of=[2] bs=4096 _____
		Command line ___kdesu dd if=[1] of=[2] bs=4096 _____
		
				+-------+			+-----------+
				|	OK	|			|	Cancel	|
				+-------+           +-----------+
*/	
	public void createGui(Font font) {

		prepareGui();

		// ======================
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth("Flash card device path (like \"/dev/sdc\")");

		boolean isMac = System.getProperty("os.name").startsWith("Mac");
		
		if (m_read) {
			putTextLine(this, "!Reading a Compact Flash card to an image file", 0);
			add(Box.createVerticalStrut(10));
			if (m_windows) {
				putTextLine(this, "TIImageTool uses the program \"dd.exe\" to copy the contents of the CF card to an image file. Please specify the drive letter", 0);
				putTextLine(this, "of your CF card, the name of the image file where the CF contents shall be copied to, and where the dd.exe program is located.", 0);
				putTextLine(this, "See the Preferences in the File menu to change the defaults for the fields.", 0);  
			}
			else {
				if (isMac) {
					putTextLine(this, "TIImageTool uses the Unix \"dd\" program to copy the contents of a CF card to an image file. Please specify the device name", 0);
					putTextLine(this, "of your CF card (like /dev/disk1, without partition number i.e. \"s1\"), and the image file on your hard disk where the", 0);
					putTextLine(this, "CF contents shall be copied to. See the Preferences in the File menu to change the defaults for the fields.", 0);
					putTextLine(this, "To get an overview of all disks mounted on your system, type \"diskutil list\" into Terminal. From its output you can copy", 0);
					putTextLine(this, "the device path.", 0);
				} else {
					putTextLine(this, "TIImageTool uses the dd program to copy the contents of a CF card to an image file. Please specify the device name", 0);
					putTextLine(this, "of your CF card (like /dev/sdc, without partition number), and the image file on your hard disk where the CF contents", 0);
					putTextLine(this, "shall be copied to. See the Preferences in the File menu to change the defaults for the fields.", 0);
				}  
				add(Box.createVerticalStrut(10));
				if (isMac) {
					putTextLine(this, "Access to the raw device requires elevated privileges. You will be ask for your super user password if you proceed.", 0);
				} else {
					putTextLine(this, "Access to the raw device requires elevated privileges. For KDE this can be done by using the kdesu command.", 0);
					putTextLine(this, "Check your OS and desktop environment for the appropriate command. You can set it in the Preferences.", 0);
				}			
			}
			add(Box.createVerticalStrut(10));
			putTextLine(this, "Please check the flash card specification. Since you are about to read it, nothing serious should happen on error.", 0);  
		}
		else { // Writing on CF
			putTextLine(this, "!Writing an image file to a Compact Flash card", 0);
			add(Box.createVerticalStrut(10));
			if (m_windows) {
				putTextLine(this, "TIImageTool uses the program \"dd.exe\" to write the contents of an image file to a CF card. Please specify the image file", 0); 
				putTextLine(this, "on your hard disk, the drive letter of your CF card where the image shall be written to, and where the dd.exe program is located.", 0);
				putTextLine(this, "See the Preferences in the File menu to change the defaults for the fields.", 0);  
			}
			else {
				if (isMac) {
					putTextLine(this, "TIImageTool uses the Unix \"dd\" program to write the contents of an image file to a CF card. Please specify the image file", 0);
					putTextLine(this, "on your hard disk, and the device name of your CF card where the image shall be written to (like /dev/disk1, without partition",0);
					putTextLine(this, "number i.e. \"s1\").	See the Preferences in the File menu to change the presets for the fields.", 0);
					putTextLine(this, "To get an overview of all disks mounted on your system, type \"diskutil list\" into Terminal. From its output you can copy the", 0);
					putTextLine(this, "device path.", 0);
				} else {
					putTextLine(this, "TIImageTool uses the \"dd\" program to write the contents of an image file to a CF card. Please specify the image file", 0);
					putTextLine(this, "on your hard disk, and the device name of your CF card where the image shall be written to (like /dev/sdc, without partition",0);
					putTextLine(this, "number).	See the Preferences in the File menu to change the presets for the fields.", 0);
				}
				add(Box.createVerticalStrut(10));
				if (isMac) {
					putTextLine(this, "Access to the raw device requires elevated privileges. You will be ask for your super user password if you proceed.", 0);
				} else {
					putTextLine(this, "Access to the raw device requires elevated privileges. For KDE this can be done by using the kdesu command.", 0);
					putTextLine(this, "Check your OS and desktop environment for the appropriate command. You can set it in the Preferences.", 0);
				}
			}
			add(Box.createVerticalStrut(10));
			putTextLine(this, "!Double check the flash card device name. In the worst case, you could accidentally overwrite", 0);
			putTextLine(this, "!your system hard disk and destroy your whole computer setup and lose all files!", 0);
		}		
		
		add(Box.createVerticalStrut(10));
		putTextLine(this, "Before you click on OK, have a final look at the last field. It reflects the command line that will be executed", 0);
		putTextLine(this, "and can be modified as desired.", 0);

		add(Box.createVerticalStrut(10));

		String devprompt = (m_windows)? "Flash card drive name (like \"e:\")" : (isMac)? "Flash card device path (like \"/dev/disk1\")" : "Flash card device path (like \"/dev/sdc\")";
		String lastPath = imagetool.getPropertyString(imagetool.CFCARD);
		String fileprompt = "File name for CF image";
		String ddprompt = "Path to DD.EXE program";
		m_tfImageFile = new JTextField(m_read? "click to select (suggested: *.cf7 suffix)" : "click to select");
		
		m_tfDevice = new JTextField(lastPath);

		if (m_read) {
			addChoiceLine(nColumnWidth, devprompt, DEVLINE, DEV, m_tfDevice, 45);
			add(Box.createVerticalStrut(10));
			addChoiceLine(nColumnWidth, fileprompt, FILELINE, FILE, m_tfImageFile, 32);
			add(Box.createVerticalStrut(10));
		}
		else {
			addChoiceLine(nColumnWidth, fileprompt, FILELINE, FILE, m_tfImageFile, 32);
			add(Box.createVerticalStrut(10));
			addChoiceLine(nColumnWidth, devprompt, DEVLINE, DEV, m_tfDevice, 45);
			add(Box.createVerticalStrut(10));
		}
		
		if (m_windows) {
			m_tfddpath = new JTextField(imagetool.getPropertyString(imagetool.DDPATH));
			addChoiceLine(nColumnWidth, ddprompt, DDLINE, DD, m_tfddpath, 45);
			add(Box.createVerticalStrut(10));
		}
				
		m_tfCommandLine = putTextField(this, "Command line", "", nColumnWidth, 0);
		setupCommand();
		add(Box.createVerticalGlue());
		addButtons();		
	}

	private void setupCommand() {
		StringBuilder sb = new StringBuilder();
		String cfcard = m_tfDevice.getText();
		String image = m_tfImageFile.getText();
		String command = "";
		String user = "";
		String chown = "";
		String elevate = "";
		String bsize = "";
		
		// We don't need this for Windows. Linux and Mac can only access the
		// flash card as root, and the dump file must be given to the current
		// user afterwards.
		if (!m_windows) {
			elevate = imagetool.getPropertyString(imagetool.SUPATH);
			chown = imagetool.getPropertyString(imagetool.COPATH);
			user = System.getProperty("user.name");
		}
		
		command = imagetool.getPropertyString(imagetool.DDPATH);
		bsize = imagetool.getPropertyString(imagetool.BSIZE);
		
		if (!cfcard.equals("click to select") 
			&& !image.startsWith("click to select") 
			&& !command.equals("click to select")) {

			if (elevate.length()>0) sb.append(elevate).append(" -c \"");
			sb.append(command).append(" ");
			sb.append("if=");
			if (m_read) {
				// Add the special path prefix for Windows raw devices
				if (m_windows) sb.append("\\\\.\\");
				sb.append(cfcard).append(" ");
				sb.append("of=").append(image).append(" ");
			}
			else {
				sb.append(image).append(" ");
				sb.append("of=");
				if (m_windows) sb.append("\\\\.\\");
				sb.append(cfcard).append(" ");
			}
			sb.append("bs=").append(bsize);			
			
			// For Linux and Mac, we have to add a chown command afterwards, 
			// or our dump file will belong to root
			// For writing there is nothing to do
			if (!m_windows && m_read) {
				sb.append("; ").append(chown).append(" ").append(user).append(" ").append(image);
			}
			
			// Closing quote
			if (elevate.length()>0) sb.append("\"");
		}
		m_tfCommandLine.setText(sb.toString());
	}
		
	@Override
	public void actionPerformed(ActionEvent ae) {
		JFileChooser jfc = null;
		if (ae.getSource()==m_btnOK) {
			/* 
			 * TODO: Check if command line has content and check if output file is set. Disable OK button if not.
			 */
			m_bSet = true;
			dispose();
		}
		if (ae.getSource()==m_btnCancel) {
			m_bSet = false;
			dispose();
		}
		if (ae.getActionCommand().equals(String.valueOf(DEV))) {
			String lastPath = imagetool.getPropertyString(imagetool.CFCARD);
			if (lastPath != null && lastPath.length() > 0) {
				try {
					if (m_windows) {
						jfc = new JFileChooser();
						File dummy_file = new File(new File("C:\\").getCanonicalPath());
						jfc.setCurrentDirectory(dummy_file);
						jfc.changeToParentDirectory();
					}
					else {
						File lastFile = new File(lastPath);
						jfc = new JFileChooser(lastFile.getParentFile());
					}
				}
				catch (IOException iox) {
					iox.printStackTrace();
				}
			}
			else jfc = new JFileChooser(); 

			if (m_windows) jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			else jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			
			Dimension dim = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			
			int nReturn = jfc.showOpenDialog(m_parent);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				File file = jfc.getSelectedFile();
				if (m_windows) {
					String dev = file.getAbsolutePath();
					// Strip off the trailing backslash
					if (dev.endsWith(File.separator)) {
						dev = dev.substring(0, dev.length() - File.separator.length());
					}
					m_tfDevice.setText(dev);
				}
				else {
					m_tfDevice.setText(file.getAbsolutePath());
				}
			}
			setupCommand();
		}
		if (ae.getActionCommand().equals(String.valueOf(FILE))) {
			jfc = new JFileChooser();
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			
			Dimension dim = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			
			int nReturn = jfc.showOpenDialog(m_parent);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				File file = jfc.getSelectedFile();
				String filename = file.getAbsolutePath();
				// TODO: I think that there are more special cases to catch
				filename = filename.replaceAll(" ", "\\\\ ").replaceAll(":", "\\\\:").replaceAll("\\*", "\\\\*");
				m_tfImageFile.setText(filename);
			}
			setupCommand();
		}
		if (ae.getActionCommand().equals(String.valueOf(DDLINE))) {
			String sLastDD = imagetool.getPropertyString(imagetool.DDPATH);
			
			if (sLastDD != null && sLastDD.length() > 0) {
				if (m_windows) {
					jfc = new JFileChooser();
				}
				else {
					File lastDD = new File(sLastDD);
					jfc = new JFileChooser(lastDD);
				}				
			}
			else jfc = new JFileChooser(); 

			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			
			Dimension dim = imagetool.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			
			int nReturn = jfc.showOpenDialog(m_parent);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				File file = jfc.getSelectedFile();
				m_tfddpath.setText(file.getAbsolutePath());
				imagetool.setProperty(imagetool.DDPATH, file.getAbsolutePath());
			}
			setupCommand();
		}
	}
	
	/*
	 * This method generates an String array of commands to execute a device copy process with the Unix 'dd' on a Apple Macintosh.
	 * These commands are wrapped around with some extra code for (un)mounting devices and obtaining super user access.
	 */
	String[] getMacCommandLine(boolean doWriting) {
		String shellScript = m_tfCommandLine.getText();
		if (shellScript.isEmpty()) return null;

		String cfcard = m_tfDevice.getText();	// getting device for (un)mounting it
		
		/*
		 * To avoid changes during copy process unmount the CF-Card device before operation begins. This is always not necessary for CF7
		 * formatted cards cause they aren't mounted due to their unknown file system, but anyway, for safety.
		 * Usually newly written CF-Cards cannot be mounted by macOS due to the change of the legacy CF7 file format. Therefore generally
		 * no (re)mount is necessary.
		 */
		shellScript = "/usr/sbin/diskutil unmountDisk " + cfcard + "; " + shellScript.replaceAll("\\\\", "\\\\\\\\") + (doWriting? "" : "; /usr/sbin/diskutil mountDisk " + cfcard);
		String[] retVal = {"/usr/bin/osascript", "-e", "do shell script \"" + shellScript + "\" with administrator privileges"};
		return retVal;
	}
	
	String[] getCommandLine() {
		String sLine = m_tfCommandLine.getText();
		if (sLine.isEmpty()) return null;
		
		boolean inQuotes = false;
		StringBuilder sb = new StringBuilder();
		
		ArrayList<String> cmds = new ArrayList<String>();
		char c = 0;
		
		for (int i=0; i < sLine.length(); i++) {
			c = sLine.charAt(i);
			if (c == '\"') {
				inQuotes = !inQuotes;
				continue;
			}
			if (c != ' ' || inQuotes) sb.append(c);
			else {
				cmds.add(sb.toString());
				sb.setLength(0);
			}
		}
		if (c=='\"') cmds.add(sb.toString());  // we had no chance to write the remainder
		String[] result = cmds.toArray(new String[cmds.size()]);		
		return result;
	}
}
