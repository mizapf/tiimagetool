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
    
    Copyright 2013 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import java.awt.Cursor;
import java.io.*;
import java.util.List;
import java.util.LinkedList;

import javax.swing.JOptionPane;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.*;
import de.mizapf.timt.TIImageTool;

public class InstallGenOSAction extends Activity {
	
	/*  
	    0.98: other FDD   -> Floppy boot: SYSTEM/SYS in DSK1.
	          HFDC        -> Floppy boot: LOAD-MFM as LOAD/SYS in DSK1., SYSTEM-SYS in DSK1.
	                         HD boot:     LOAD-MFM as LOAD/SYS in HDS1.DSK1., SYSTEM-SYS in HDS1.
	          SCSI        -> Floppy boot: SYSTEM/SYS in DSK1.
	                         HD boot:     no direct support (use custom bootloader)
	          IDE         -> Floppy boot: SYSTEM/SYS in DSK1.
	                         HD boot:     no direct support (use custom bootloader)
	          
	    0.99: same as 2.00
	          
	    1.00: other FDD   -> Floppy boot: SYSTEM/SYS in DSK1.
	          HFDC        -> Floppy boot: LOAD-MFM as LOAD/SYS in DSK1., SYSTEM-SYS in DSK1.
	                         HD boot:     LOAD-MFM as LOAD/SYS in HDS1.DSK1., SYSTEM-SYS in HDS1.	                         
	          SCSI        -> Floppy boot: SYSTEM/SYS in DSK1. 
	                         HD boot:     LOAD-SCS as SCSI/SYS in SCS1. and SYSTEM-SYS in SCS1.
	          IDE         -> Floppy boot: SYSTEM/SYS in DSK1.
	                         HD boot:     no direct support (use custom bootloader)
	                    
	    2.00: other FDD   -> Floppy boot: SYSTEM-SYS in DSK1.
 	          HFDC        -> Floppy boot: LOAD-MFM in DSK1., SYSTEM-SYS in DSK1.
	                         HD boot:     LOAD-MFM in HDS1.DSK1., SYSTEM-SYS in HDS1.
	          SCSI        -> Floppy boot: SYSTEM-SYS in DSK1.
	                         HD boot:     LOAD-SCS in SCS1., SYSTEM-SYS in SCS1.
	          IDE         -> Floppy boot: SYSTEM-SYS in DSK1.
	                         HD boot:     LOAD-IDE in IDE1., SYSTEM-SYS in IDE1.
	                         
	                         
       The formerly used SCSI/SYS (2.1) leads to a CRC failure with EPROM 1.00,
       and does not work at all with EPROM 2.00.

       In order to boot, IDE and SCSI do NOT need a floppy controller to be present.
       (tested for 0.99, 1.00 (SCSI only), 2.00)
       
       AUTOEXEC is only taken from DSK1 for SCSI and IDE. Only HFDC loads 
       AUTOEXEC from the hard disk (tested for all EPROM versions).
       
       TIPI: AUTOEXEC is loaded correctly from TIPI. No floppy controller seems
       to be required.
       
	*/
	
	// TODO: Check what happens with existing files
	
	final static int E098 = 1;
	final static int E100 = 2;
	final static int E200 = 3;	
	
	final static int HFDC = 1;
	final static int OTHER = 2;

	DirectoryView m_dvCurrent;
	
	public String getMenuName() {
		return TIImageTool.langstr("InstallGeneveOS");
	}
	
	public String getActionName() {
		return "INSTALLGOS";
	}
	
	public void go() {
		DirectoryView m_dvCurrent = imagetool.getSelectedView();
		Volume volTarget = m_dvCurrent.getVolume();
		Directory dirRoot = volTarget.getRootDirectory();
		InstallGenOSDialog inst = new InstallGenOSDialog(m_parent, imagetool, volTarget.isFloppyImage());

		inst.setSettings(settings);
		inst.createGui(imagetool.boldFont);
		inst.setVisible(true);

		if (inst.confirmed()) {
			String sInstallPath = settings.getPropertyString(TIImageTool.GENOSPATH);
			// System.out.println("install from " + sInstallPath);
			File ioRepo = new File(sInstallPath);

			if (!ioRepo.exists()) {
				JOptionPane.showMessageDialog(m_dvCurrent.getFrame(), TIImageTool.langstr("InstallGenOSNoRepo"), TIImageTool.langstr("InstallGenOSError"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			boolean bChanged = false;
			List<String> failed = new LinkedList<String>();
			Directory dirDSK1 = null;
			
			java.io.File[] aiof = ioRepo.listFiles();
			try {
				// For HFDC we need a DSK1 subdirectory
				if (volTarget.isHFDCImage()) {
					if (!dirRoot.hasSubdirectory("DSK1"))
						dirRoot.createSubdirectory("DSK1");
					dirDSK1 = dirRoot.getSubdirectory("DSK1");
				}
				
				for (java.io.File iofile : aiof) {
					DataInputStream dis = new DataInputStream(new FileInputStream(iofile));
					byte[] abyTif = new byte[dis.available()];
					dis.readFully(abyTif);
					dis.close();
					
					if (!TIFiles.hasHeader(abyTif)) {
						failed.add(iofile.getName());
						System.err.println(iofile.getAbsolutePath() + ": " + TIImageTool.langstr("NoTIFILESHeader"));			
					}
					else {
						TIFiles tfi = new TIFiles(abyTif);
						String sName = tfi.getTFIName();
						int bootver = inst.getEpromVersion();
						int controller = inst.getController();
						
						// Handle floppy
						if (volTarget.isFloppyImage()) {
							if (controller == OTHER) {
								if (sName.equals("SYSTEM-SYS") || sName.equals("SYSTEM/SYS")) {
									if (sName.equals("SYSTEM-SYS") && (bootver != E200)) tfi.setTFIName("SYSTEM/SYS");
									installFile(dirRoot, abyTif);
								}
							}
							else {
								if (sName.equals("LOAD-MFM")) {
									if (bootver != E200) {
										tfi.setTFIName("LOAD/SYS");
										installFile(dirRoot, abyTif);
									}
								}
								if (sName.equals("SYSTEM-SYS")) {
									installFile(dirRoot, abyTif);
								}
							}
						}
						else {
							if (sName.equals("SYSTEM-SYS")) {
								installFile(dirRoot, abyTif);
							}
							else {
								if (volTarget.isSCSIImage() && !volTarget.isIDEImage() && bootver == E100) {
									if (sName.equals("SCSI/SYS"))
										installFile(dirRoot, abyTif);
									else {
										if (sName.equals("LOAD-SCS")) {
											tfi.setTFIName("SCSI/SYS");
											installFile(dirRoot, abyTif);
										}
									}
								}
								else {
									// SCSI or IDE
									if (volTarget.isIDEImage()) {
										if (sName.equals("LOAD-IDE")) {
											installFile(dirRoot, abyTif);
										}
									}
									else {
										if (volTarget.isSCSIImage()) {
											// Unpartitioned IDE images look like SCSI
											if (sName.equals("LOAD-SCS") || sName.equals("LOAD-IDE")) {
												installFile(dirRoot, abyTif);
											}
										}
										else {  // HFDC
											if (sName.equals("LOAD-MFM")) {
												if (bootver != E200) 
													tfi.setTFIName("LOAD/SYS");
												installFile(dirDSK1, abyTif);
											}
										}
									}
								}
							}
						}
						bChanged = true;
					}
				}
			}
			catch (IllegalOperationException iopx) {
				iopx.printStackTrace();
				JOptionPane.showMessageDialog(m_dvCurrent.getFrame(), TIImageTool.langstr("IllegalOperation") + ": " + iopx.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
			}
			catch (IOException iox) {
				iox.printStackTrace();
				JOptionPane.showMessageDialog(m_dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("InstallGenOSError"), JOptionPane.ERROR_MESSAGE); 
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("VolumeWP"), TIImageTool.langstr("InstallGenOSError"), JOptionPane.ERROR_MESSAGE); 				
			}
			catch (InvalidNameException inx) {
				JOptionPane.showMessageDialog(m_dvCurrent.getFrame(), TIImageTool.langstr("InvalidFileName") + ": " + inx.getMessage(), TIImageTool.langstr("InstallGenOSError"), JOptionPane.ERROR_MESSAGE); 
			}
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("InstallGenOSImageError") + ": " + ix.getMessage(), TIImageTool.langstr("InstallGenOSError"), JOptionPane.ERROR_MESSAGE);				
			}			
			
			if (failed.size() > 0) {
				StringBuilder sb = new StringBuilder();
				for (String name : failed) {
					if (sb.length() > 0) sb.append(", ");
					sb.append(name);
				}
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("InstallGenOSFailed") + " " + sb.toString(), TIImageTool.langstr("InstallGenOSError"), JOptionPane.ERROR_MESSAGE);				
			}
		
			if (bChanged) {
				try {
					if (dirDSK1 != null) dirDSK1.commit(false);
					dirRoot.commit(true);
				}
				catch (ImageException ix) {
					ix.printStackTrace();
					JOptionPane.showMessageDialog(m_dvCurrent.getFrame(), TIImageTool.langstr("ImageError"), TIImageTool.langstr("InstallGenOSError"), JOptionPane.ERROR_MESSAGE); 
				}		
				catch (ProtectedException px) {
					JOptionPane.showMessageDialog(m_dvCurrent.getFrame(), px.getMessage(), TIImageTool.langstr("InstallGenOSError"), JOptionPane.ERROR_MESSAGE); 
				}
			}
			imagetool.refreshPanel(volTarget);	
		}
	}
	
	private void installFile(Directory dir, byte[] abyTif) throws ImageException, ProtectedException, InvalidNameException, IOException {
		TIFiles tif = new TIFiles(abyTif);
		String sName = tif.getTFIName();
		
		// Already there
		TFile tf = dir.getFile(sName); 
		if (tf != null) {
			// Delete it
			try {
				// System.out.println("Delete existing " + tf.getName()); 
				dir.deleteFile(tf, true);
			}
			catch (FileNotFoundException fnfx) {
				fnfx.printStackTrace();
				return;
			}
		}
		imagetool.putTIFileIntoImage(dir, m_dvCurrent, abyTif, "");
	}
}
