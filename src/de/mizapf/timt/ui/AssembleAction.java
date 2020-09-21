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
    
    Copyright 2020 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.awt.Cursor;
import java.util.List;
import java.util.LinkedList;

import de.mizapf.timt.assm.Assembler;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.util.TIFiles;

public class AssembleAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("AsmTMS") + "...";
	}
	
	public String getActionName() {
		return "ASSM";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		DirectoryPanel dp = dvCurrent.getPanel();
		
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		AssembleParamDialog asparm = new AssembleParamDialog(dvCurrent.getFrame(), imagetool);
		asparm.createGui(imagetool.boldFont);
		asparm.setVisible(true);
		if (asparm.confirmed()) { 
			
			int nIndex = 0;
			
			List<String> fileList = new LinkedList<String>();
			
			for (Element selected : dvCurrent.getSelectedEntries()) {
				if (selected instanceof TFile) {
					nIndex++;
					StringBuilder sb = new StringBuilder("_asm");
					if (nIndex < 100) sb.append("0");
					if (nIndex < 10) sb.append("0");
					sb.append(nIndex);
					System.out.println("Assemble " + selected.getName() + " as " + sb.toString() + ".tis in " + imagetool.getTemporaryDirectory());
					
					File asmFile = new File(imagetool.getTemporaryDirectory(), sb.toString() + ".tis");
					File objFile = new File(imagetool.getTemporaryDirectory(), sb.toString() + ".obj");
					try {
						byte[] content = ((TFile)selected).getRecordContent();
						FileOutputStream fos = new FileOutputStream(asmFile);
						fos.write(content);
						fos.close();
					}
					catch (IOException iox) {
						iox.printStackTrace();
					}
					catch (FormatException fx) {
						fx.printStackTrace();
					}
					catch (ImageException ix) {
						ix.printStackTrace();
					}
					
					boolean bOK = true;
					String sSrcFile = null;
					String sObjFile = null;
					
					try {
						sSrcFile = asmFile.getPath();
						System.out.println("File " + sSrcFile);
						sObjFile = objFile.getPath();
						String command = createCommandLine(asparm.getCommand(), sSrcFile, sObjFile);

						Runtime runtime = Runtime.getRuntime();

						String[] cmdarray = command.split("\\s");
						// for (String s : cmdarray)
						// 		System.out.println(s);
						
						Process p = runtime.exec(cmdarray); 
						InputStream ierr = p.getErrorStream();
						int val = p.waitFor();					

						int ch = 0;
						StringBuilder sbErr = new StringBuilder();
						while (ch != -1) {
							ch = ierr.read();
							if (ch != -1) sbErr.append((char)ch);
						}

						if (sbErr.length()>0) {
							if (val == 0)
								System.out.println("Process warning output: ");
							else
							{
								bOK = false;
								System.out.println("Process error output: ");
							}
							System.out.println(sbErr.toString());
						}
					}
					catch (FormatException fx) {
						JOptionPane.showMessageDialog(m_parent, fx.getMessage(), TIImageTool.langstr("AsmTMS"), JOptionPane.ERROR_MESSAGE);
						fx.printStackTrace();
						bOK = false;
					}
					catch (InterruptedException ix) {
						ix.printStackTrace();
						bOK = false;
					}
					catch (IOException iox) {
						// iox.printStackTrace();
						// Linux: java.io.IOException: Cannot run program "xxx": error=2, Datei oder Verzeichnis nicht gefunden
						// Windows: java.io.IOException: Cannot run program "xxx": CreateProcess error=2, Das System kann die angegebene Datei nicht finden
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("AsmFailed"), TIImageTool.langstr("AsmTMS"), JOptionPane.ERROR_MESSAGE);
						bOK = false;
					}
					
					if (bOK) {
						System.out.println("Successfully assembled " + selected.getName());
						fileList.add(selected.getName() + "." + sObjFile);
					}
				}
			}
				
			// Now import the files
			boolean bRetry = false;
			boolean bAbort = false;

			for (String sPart : fileList) {
				int nPart = sPart.indexOf("."); 
				String sImportName = sPart.substring(0, nPart);
				String sObjFile = sPart.substring(nPart+1);
				bRetry = false;
				bAbort = false;
				do {
					try {						
						// Import the file and remove the temp files
						FileInputStream fis = new FileInputStream(sObjFile);
						DataInputStream dis = new DataInputStream(fis);
						byte[] abyFile = new byte[dis.available()];
						dis.readFully(abyFile);
						
						// Create import name
						if (!bRetry) {
							// System.out.println("sImportName = " + sImportName + ", asparm.remove=" + asparm.getRemove());
							if (sImportName.endsWith(asparm.getRemove())) {
								sImportName = sImportName.substring(0, sImportName.length()-asparm.getRemove().length());
							}
							sImportName = sImportName + asparm.getAppend();
						}	
						
						// int l3count = ((abyFile.length + 79) / 80); 
						// byte[] abyTif = TIFiles.createTfi(abyFile, sImportName, (byte)0, 80, l3count);  // DIS/FIX 80
						
						// Create an importable file
						TIFiles impFile = new TIFiles(sImportName, TFile.T_DISFIX, 80); // DIS/FIX 80
						byte[] abyRecord = new byte[80];  // Record
						
						try {
							int j = 0;
							while (j < abyFile.length) {
								for (int i=0; i < 80; i++) {
									abyRecord[i] = (j < abyFile.length)? abyFile[j++] : (byte)0;
								}
								impFile.writeRecord(abyRecord, 0);
							}
						}
						catch (IOException iox) {
							iox.printStackTrace();
						}
						byte[] abyTif = impFile.closeAndGetBytes(false, false);
						
						dirCurrent.insertFile(abyTif, null, false, asparm.allowOverwrite());
						System.out.println("Successfully imported " + sImportName);
						imagetool.refreshPanel(dirCurrent.getVolume());
						
						// Remove the files
						File asmFile = new File(sObjFile);
						asmFile.delete();
						// objFile.delete();
						bRetry = false;
					}
					catch (FileNotFoundException fnfx) {
						System.err.println("Object file " + sObjFile + " not found");
					}
					catch (IOException iox) {
						iox.printStackTrace();
					}
					catch (InvalidNameException inx) {
						System.err.println("Invalid name: " + sImportName);
					}
					catch (ImageFullException ifx) {					
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), sImportName + ": " + ifx.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
						bAbort = true;
					}
					catch (ProtectedException px) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("VolumeWP") + ": " + px.getMessage(), TIImageTool.langstr("ConvertError"), JOptionPane.ERROR_MESSAGE); 
						bAbort = true;
					}
					catch (FileExistsException fx) {
						
						bRetry = true;
						sImportName = getAlternativeName(true, sImportName, dvCurrent.getFrame());
						if (sImportName.equals(NAME_SKIP)) {
							bRetry = false; // skip
							sImportName = null;
						}
						else {
							if (sImportName.equals(NAME_ABORT)) {
								bAbort = true; 
								sImportName = null;
							}
						}
					}
					catch (ImageException ix) {
						ix.printStackTrace();
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
						bAbort = true;
					}
					
				} while (bRetry && !bAbort);

				if (bAbort) break;
			}
			try {
				dirCurrent.commit(true);
			}
			catch (ProtectedException px) {
				// JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("VolumeWP") + ": " + px.getMessage(), TIImageTool.langstr("ConvertError"), JOptionPane.ERROR_MESSAGE);
				px.printStackTrace();
			}
			catch (ImageException ix) {
				ix.printStackTrace();
				// JOptionPane.showMessageDialog(dvCurrent.getFrame(), ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 				
			}
			catch (IOException iox) {
				iox.printStackTrace();
			}
		}
		
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	private String createCommandLine(String sTemplate, String sSource, String sObject) throws FormatException {
		int nSrc = sTemplate.indexOf("$SRC");
		int nObj = sTemplate.indexOf("$OBJ");
		if (nSrc==-1) throw new FormatException("property", String.format(TIImageTool.langstr("AsmMissingParam"), "$SRC"));
		if (nObj==-1) throw new FormatException("property", String.format(TIImageTool.langstr("AsmMissingParam"), "$OBJ"));
		
		StringBuilder sb = new StringBuilder();
		sb.append(sTemplate.substring(0, nSrc));
//		sb.append("\""); 
		sb.append(sSource);
//		sb.append("\"");
		sb.append(sTemplate.substring(nSrc+4));
		String s1 = sb.toString();
		
		nObj = s1.indexOf("$OBJ");
		sb = new StringBuilder();
		sb.append(s1.substring(0, nObj));
//		sb.append("\"");
		sb.append(sObject);
//		sb.append("\"");
		sb.append(s1.substring(nObj+4));
		return sb.toString();
	}
}
