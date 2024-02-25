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
    
    MacOS additions by Henrik Wedekind 2016
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import de.mizapf.timt.TIImageTool;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import static de.mizapf.timt.TIImageTool.WINDOWS;
import static de.mizapf.timt.TIImageTool.MACOS;
import static de.mizapf.timt.TIImageTool.UNIX;

class ReadWriteCFDialog extends ToolDialog {

	TIImageTool imagetool;
	JFrame m_parent;
/*	JTextField m_tfDevice;
	JTextField m_tfImageFile;
	JTextField m_tfddpath;
	
	private final static int DEV = 1;
	private final static int FILE = 2;
	private final static int DD = 3;
	
	JTextField m_tfCommandLine;
*/			
	JTextArea m_taExplain;
	JTextField m_tfCommand;
	boolean m_read;

	ReadWriteCFDialog(JFrame owner, TIImageTool timt, boolean read) {
		super(owner, TIImageTool.langstr(read? "ReadWriteCFTitleR" : "ReadWriteCFTitleW"));
		imagetool = timt;
		m_parent = owner;
		m_read = read;
	}	
	
	public void createGui(Font font) {
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(10));
		/*
		m_taExplain = new JTextArea(TIImageTool.langstr(m_read? "Warning.CFReading" : "Warning.CFWriting"));
		m_taExplain.setFont(imagetool.dialogFont);
		m_taExplain.setLineWrap(true);
		m_taExplain.setWrapStyleWord(true);
		m_taExplain.setBackground(new Color(10,10,10,0));
		add(m_taExplain);
		*/
		
		putMultiTextLine(this, TIImageTool.langstr(m_read? "Warning.CFReading1" : "Warning.CFWriting1"));
		add(Box.createVerticalStrut(10));
		putMultiTextLine(this, TIImageTool.langstr(m_read? "Warning.CFReading2" : "Warning.CFWriting2"));
		add(Box.createVerticalStrut(10));
		int type = imagetool.getOperatingSystem();
		if (type==WINDOWS) {
			add(Box.createVerticalStrut(10));
			m_tfCommand = putTextField(this, TIImageTool.langstr("Warning.CFReading3Win"), TIImageTool.langstr(m_read? "Warning.CFReadingExWin" : "Warning.CFWritingExWin"), 0 ,0);
			putMultiTextLine(this, TIImageTool.langstr(m_read? "Warning.CFReading4Win" : "Warning.CFWriting4Win"));
		}
		else {
			add(Box.createVerticalStrut(10));
			m_tfCommand = putTextField(this, TIImageTool.langstr("Warning.CFReading3Lin"), TIImageTool.langstr(m_read? "Warning.CFReadingExLin" : "Warning.CFWritingExLin"), 0 ,0);
			putMultiTextLine(this, TIImageTool.langstr(m_read? "Warning.CFReading4Lin" : "Warning.CFWriting4Lin"));
		}
		m_tfCommand.setEditable(false);
		m_tfCommand.setFont(imagetool.contentFont);
		m_tfCommand.setBackground(Color.WHITE);
		putMultiTextLine(this, TIImageTool.langstr("Warning.CFReading5"));
	
		add(Box.createVerticalStrut(10));
		
		if (!m_read) putMultiTextLine(this, "!" + TIImageTool.langstr("Warning.CFWriting6"));
		
		addButtons(ONLY_OK);
	}

	
	
/*
//      | 	Read / Write CF card								|
// 
// 		Provide the CF path. For Windows this is something like "e:", 
//		for Linux or Mac this looks like "/dev/sdc"
// 
// 		[1] CF device   _________   [btn]
// 
// 		[2] File on file system    [.....] [btn]
// 
// 		Path to dd program  ___/usr/bin/dd_____
// 
// 		Command line ___dd.exe if=\\.\[1] of=[2] bs=4096 _____
// 		Command line ___kdesu dd if=[1] of=[2] bs=4096 _____
// 
// 		 +-------+			+-----------+
//		 |	OK	 |		    |	Cancel	|
//		 +-------+         +-----------+

	public void createGui(Font font) {

		prepareGui();

		// ======================
		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		int nColumnWidth = fm.stringWidth(TIImageTool.langstr("ReadwriteCFColumn"));
		int type = imagetool.getOperatingSystem();

		if (m_read) {
			putTextLine(this, "!" + TIImageTool.langstr("ReadWriteCFTitleRLong"), 0);
			add(Box.createVerticalStrut(10));
			switch (type) {
			case WINDOWS:
				putMultiTextLine(this,	TIImageTool.langstr("ReadWriteCFHintRWin"));
				break;
			case MACOS:
				putMultiTextLine(this, TIImageTool.langstr("ReadWriteCFHintRMac"));
				add(Box.createVerticalStrut(10));
				putMultiTextLine(this, TIImageTool.langstr("ReadWriteCFElevMac"));
				break;
			default:
				putMultiTextLine(this, TIImageTool.langstr("ReadWriteCFHintRLin"));
				// add(Box.createVerticalStrut(10));
				//putMultiTextLine(this, TIImageTool.langstr("ReadWriteCFElevLin"));
				break;
			}		
			add(Box.createVerticalStrut(10));
			putMultiTextLine(this, TIImageTool.langstr("ReadWriteCFRCheck"));  
		}
		else { // Writing on CF
			putTextLine(this, "!" + TIImageTool.langstr("ReadWriteCFTitleWLong"), 0);
			add(Box.createVerticalStrut(10));
			switch (type) {
			case WINDOWS:
				putMultiTextLine(this,	TIImageTool.langstr("ReadWriteCFHintWWin"));
				break;
			case MACOS:
				putMultiTextLine(this, TIImageTool.langstr("ReadWriteCFHintWMac"));
				add(Box.createVerticalStrut(10));
				putMultiTextLine(this, TIImageTool.langstr("ReadWriteCFElevMac"));
				break;
			default:
				putMultiTextLine(this, TIImageTool.langstr("ReadWriteCFHintWLin"));
				//add(Box.createVerticalStrut(10));
//				putMultiTextLine(this, TIImageTool.langstr("ReadWriteCFElevLin")); 
				break;
			}
			add(Box.createVerticalStrut(10));
			putMultiTextLine(this, "!" + TIImageTool.langstr("ReadWriteCFWCheck"));
		}		
		
		add(Box.createVerticalStrut(10));
		putMultiTextLine(this, TIImageTool.langstr("ReadWriteCFFinal"));
		
		add(Box.createVerticalStrut(10));

		String devprompt = (type==UNIX)?	TIImageTool.langstr("ReadWriteCFPathLin") : 
					 	 ((type==WINDOWS)?	TIImageTool.langstr("ReadWriteCFPathWin") :
										  	TIImageTool.langstr("ReadWriteCFPathMac"));
		
		String lastPath = settings.getPropertyString(imagetool.CFCARD);
		String fileprompt = TIImageTool.langstr("ReadWriteCFImage");
		String ddprompt = TIImageTool.langstr("ReadWriteCFDD");
		m_tfImageFile = new JTextField(m_read? TIImageTool.langstr("ReadWriteCFClick") : TIImageTool.langstr("ClickToSelect"));
		
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

		m_tfCommandLine = putTextField(this, TIImageTool.langstr("ReadWriteCFCommand"), "", nColumnWidth, 0);
		setupCommand();
		add(Box.createVerticalGlue());
		addButtons();		
	}

	boolean isRead() {
		return m_read;
	}
	
	String getTargetImage() {
		return m_tfImageFile.getText();
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
		int type = imagetool.getOperatingSystem();

		if (type != WINDOWS) {
			elevate = settings.getPropertyString(imagetool.SUPATH);
			chown = settings.getPropertyString(imagetool.COPATH);
			user = System.getProperty("user.name");
		}
		
		command = settings.getPropertyString(imagetool.DDPATH);
		bsize = settings.getPropertyString(imagetool.BSIZE);
		
		if (!cfcard.equals(TIImageTool.langstr("ClickToSelect")) 
			&& !(image.equals(TIImageTool.langstr("ClickToSelect")) || image.equals(TIImageTool.langstr("ReadWriteCFClick"))) 
			&& !command.equals(TIImageTool.langstr("ClickToSelect"))) {

			if (elevate.length()>0) sb.append(elevate).append(" -c \"");
			if (type==WINDOWS) {
				sb.append("\"").append(command).append("\" ");
				if (m_read) {
					// Add the special path prefix for Windows raw devices
					sb.append("\"if=\\\\.\\").append(cfcard).append("\" ");
					sb.append("\"of=").append(image).append("\" ");
				}
				else {
					sb.append("\"if=").append(image).append("\" ");
					sb.append("\"of=\\\\.\\").append(cfcard).append("\" ");
				}
			}
			else {
				sb.append(command).append(" ");
				if (m_read) {
					sb.append("if=").append(cfcard).append(" ");
					sb.append("of=").append(image).append(" ");
				}
				else {
					sb.append("if=").append(image).append(" ");
					sb.append("of=").append(cfcard).append(" ");
				}
			}

			sb.append("bs=").append(bsize);			
			
			// For Linux and Mac, we have to add a chown command afterwards, 
			// or our dump file will belong to root
			// For writing there is nothing to do
			if ((type != WINDOWS) && m_read) {
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
		int type = imagetool.getOperatingSystem();
		if (ae.getSource()==m_btnOK) {
			// TODO: Check if command line has content and check if output file is set. Disable OK button if not.
			m_bSet = true;
			dispose();
		}
		if (ae.getSource()==m_btnCancel) {
			m_bSet = false;
			dispose();
		}
		if (ae.getActionCommand().equals(String.valueOf(DEV))) {
			String lastPath = settings.getPropertyString(imagetool.CFCARD);
			if (lastPath != null && lastPath.length() > 0) {
				try {
					if (type==WINDOWS) {
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

			if (type==WINDOWS) jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			else jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			
			Dimension dim = settings.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			
			int nReturn = jfc.showOpenDialog(m_parent);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				File file = jfc.getSelectedFile();
				if (type==WINDOWS) {
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
			
			Dimension dim = settings.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			
			int nReturn = jfc.showOpenDialog(m_parent);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				File file = jfc.getSelectedFile();
				String filename = file.getAbsolutePath();
				// TODO: I think that there are more special cases to catch
				if (type != WINDOWS) filename = filename.replaceAll(" ", "\\\\ ").replaceAll(":", "\\\\:").replaceAll("\\*", "\\\\*");
				m_tfImageFile.setText(filename);
			}
			setupCommand();
		}
		if (ae.getActionCommand().equals(String.valueOf(DDLINE))) {
			String sLastDD = settings.getPropertyString(imagetool.DDPATH);
			
			if (sLastDD != null && sLastDD.length() > 0) {
				if (type==WINDOWS) {
					jfc = new JFileChooser();
				}
				else {
					File lastDD = new File(sLastDD);
					jfc = new JFileChooser(lastDD);
				}				
			}
			else jfc = new JFileChooser(); 

			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			
			Dimension dim = settings.getPropertyDim(TIImageTool.FILEDIALOG);
			if (dim!=null) jfc.setPreferredSize(dim);
			
			int nReturn = jfc.showOpenDialog(m_parent);
			
			if (nReturn == JFileChooser.APPROVE_OPTION) {
				File file = jfc.getSelectedFile();
				m_tfddpath.setText(file.getAbsolutePath());
				settings.put(imagetool.DDPATH, file.getAbsolutePath());
			}
			setupCommand();
		}
		if (ae.getActionCommand().equals("AUTO")) {
			System.out.println("Autosearch");
		}
	}
	
	// This method generates an String array of commands to execute a device 
	//   copy process with the Unix 'dd' on various operating systems.
	//
	String[] getCommandLine() {
		String[] result = null;
		int type = imagetool.getOperatingSystem();
		
		if (type == MACOS) {
			// For Apple, these commands are wrapped around with some extra 
			// code for (un)mounting devices and obtaining super user access. (hw)		
			String shellScript = m_tfCommandLine.getText();
			if (shellScript.isEmpty()) return null;
			
			String cfcard = m_tfDevice.getText();	// getting device for (un)mounting it
			
			// To avoid changes during copy process unmount the CF-Card device
			// before operation begins. This is usually unnecessary for CF7
			// formatted cards because they are not mounted due to their unknown
			// file system, but anyway, for safety.
			// Usually newly written CF-Cards cannot be mounted by macOS due to 
			// the change of the legacy CF7 file format. Therefore generally
			// no (re)mount is necessary. (hw)

			StringBuilder sb = new StringBuilder();
			sb.append("/usr/sbin/diskutil unmountDisk ");
			sb.append(cfcard).append("; ");
			sb.append(shellScript.replaceAll("\\\\", "\\\\\\\\"));
			if (m_read) sb.append("; /usr/sbin/diskutil mountDisk ").append(cfcard);
			result = new String[3];
			result[0] = "/usr/bin/osascript";
			result[1] = "-e";
			result[2] = "do shell script \"" + sb.toString() + "\" with administrator privileges";
		}
		else {
			// UNIX or Windows
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
					// Leave the quotes in the Windows environment
					if (type == UNIX) continue;
				}
				if (c != ' ' || inQuotes) sb.append(c);
				else {
					cmds.add(sb.toString());
					sb.setLength(0);
				}
			}
			if (c=='\"') cmds.add(sb.toString());  // we had no chance to write the remainder
			result = cmds.toArray(new String[cmds.size()]);		
		}
		return result;			
	}
	*/
}
