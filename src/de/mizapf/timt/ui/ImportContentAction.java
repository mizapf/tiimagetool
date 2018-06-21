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

import de.mizapf.timt.files.*;
import javax.swing.*;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import java.io.EOFException;
import java.nio.charset.Charset;
import de.mizapf.timt.util.*;
import de.mizapf.timt.basic.BasicCruncher;
import de.mizapf.timt.basic.CrunchException;

import de.mizapf.timt.TIImageTool;

public class ImportContentAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("ImpText") + "...";
	}
	
	public String getActionName() {
		return "IMPORTCONT";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		DirectoryPanel dp = dvCurrent.getPanel();
		DVEditorFrame dv = new DVEditorFrame(dvCurrent.getFrame(), this, dvCurrent);
	}
	
	public void convertAndImport(byte[] abyContent, DirectoryView dvCurrent, String sSuggested, boolean bReopen) {
		if (!imagetool.viewStillThere(dvCurrent)) {
			JOptionPane.showMessageDialog(imagetool.getMainFrame(), TIImageTool.langstr("ImportContentViewClosed"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		boolean bTab = false;
		boolean bValid = false;
		String sName = null;
		Directory dirCurrent = dvCurrent.getDirectory();
		
		ImportDialog impdia = null;
		boolean bDone = false;
		int mode = 0;
		mode = BasicCruncher.contentLooksLikeBasic(abyContent, imagetool.getPropertyBoolean(TIImageTool.VERBOSE))? ImportDialog.BASIC : ImportDialog.TEXTONLY;					
		
		while (!bDone) {   // While file exists
			while (!bValid) {
				impdia = new ImportDialog(dvCurrent.getFrame(), sSuggested, false, mode);
				impdia.setContent(abyContent);
				impdia.createGui();
				impdia.setVisible(true);
				if (impdia.confirmed()) {
					sName = impdia.getFileName();
					bValid = TFile.validName(sName);
					if (!bValid) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("InvalidFileName") + ": " + sName, TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);				
					}
				}
				else {
					// System.out.println("not confirmed");
					return;
				}
			}
			
			bDone = true;
			byte[] abyTif = null;
			
			if (impdia.importAsBasic()) {
				BasicCruncher bc = new BasicCruncher();
				try {
					String fullprg = new String(abyContent, Charset.forName("ISO-8859-1"));
					String split = Utilities.getSeparator(fullprg);
					String[] lines = fullprg.split(split);					
					byte[] basicfile = null;
					abyTif = bc.textToBasic(lines, impdia.getFileName(), impdia.getBasicVersion(), impdia.getSaveFormat(), impdia.getProtected());
				}
				catch (CrunchException cx) {
					if (cx.reason==CrunchException.TOOLONG) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), cx.getReason(), TIImageTool.langstr("BasicError"), JOptionPane.ERROR_MESSAGE);
					}
					else {
						if (cx.reason==CrunchException.TOOSHORT) {
							JOptionPane.showMessageDialog(dvCurrent.getFrame(), cx.getReason(), TIImageTool.langstr("BasicError"), JOptionPane.ERROR_MESSAGE);
						}
						else {
							// cx.printStackTrace();
							if (cx.textline != 0) {
								if (cx.line != 0) {
									JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("BasicErrorLineExt"), cx.textline, cx.line, cx.pos) + ": " + cx.getReason() + ";\n" + TIImageTool.langstr("BasicUseAnother"), TIImageTool.langstr("BasicError"), JOptionPane.ERROR_MESSAGE);
								}
								else {
									JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("BasicParseError"), cx.textline) + ": " + cx.getReason() + ";\n" + TIImageTool.langstr("BasicUseAnother"), TIImageTool.langstr("BasicError"), JOptionPane.ERROR_MESSAGE);
								}
							}
							else {
								JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("BasicErrorLine"), cx.line, cx.pos) + ": " + cx.getReason() + ";\n" + TIImageTool.langstr("BasicUseAnother"), TIImageTool.langstr("BasicError"), JOptionPane.ERROR_MESSAGE);
							}
						}
					}
					
					bDone = false;
					bValid = false;
					continue;
				}				
				catch (IOException iox) {
					iox.printStackTrace();
					return;
				}
			}
			else {
				
				if (impdia.importAsText()) {
					
					StringBuilder special = new StringBuilder();
					String sneu = new String(abyContent);

					// Find the special characters in the file (except for CRLF)
					// and add them into the "special" list
					// Also check whether there are tabs.
					for (int i=0; i < sneu.length(); i++) {
						char ch = sneu.charAt(i);
						if (ch==0x09) bTab = true;
						else {
							if (((ch < 0x20) && (ch!=10) && (ch!=13)) || (ch > 0x7f)) {
								boolean cfound = false;
								for (int j=0; j < special.length(); j++) {
									if (special.charAt(j) == ch) cfound = true;
								}
								if (!cfound) special.append(ch); 
							}
						}
					}
					
					StringBuilder sbNew = new StringBuilder();
					
					if (bTab || special.length()>0) {
						if (special.length() > 20) {
							JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImportContentUnprint"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
							return;							
						}
						TranslateDialog trdia = new TranslateDialog(dvCurrent.getFrame(), bTab, special.toString());
						try {
							trdia.createGui();
							trdia.setVisible(true);
						}
						catch (Exception e) {
							e.printStackTrace();
							System.err.println(TIImageTool.langstr("ImportContentTransError"));
							JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImportContentNotAsText"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
							return;							
						}
						
						if (trdia.confirmed()) {
							/* Translate the text */
							String from = trdia.getFromTranslations();
							String to = trdia.getToTranslations();
							String[] atrans = to.split(",");
							
							for (int i=0; i < sneu.length(); i++) {
								char ch = sneu.charAt(i);
								if (ch==0x09 && bTab) {
									sbNew.append(trdia.getTabTrans());
									continue;
								}
								int index = from.indexOf(ch);
								if (index != -1 && index < atrans.length) {
									sbNew.append(atrans[index].trim());
								}
								else sbNew.append(ch);
							}
						}
					}
					if (sbNew.length()==0) sbNew.append(new String(abyContent));	
					
					String text = sbNew.toString();
					
					byte[] sectors = null;
					// Convert it to a sequence of lines
					String split = Utilities.getSeparator(text);
					String[] lines = text.split(split);		

					if (impdia.importAsFixed()) {
						TIFiles impfile = new TIFiles(impdia.getFileName(), TFile.flagsToType(impdia.getFlags()), impdia.getRecordLength());
						try {
							for (int i=0; i < lines.length; i++) {
								impfile.writeRecord(lines[i].getBytes());
							}
						}
						catch (IOException iox) {
							iox.printStackTrace();
						}
						sectors = impfile.closeAndGetBytes(false, false);
						abyTif = sectors;
					}
					else {
						sectors = TFile.textToSectors(lines/*, false */);
						abyTif = TIFiles.createTfi(sectors, impdia.getFileName(),  impdia.getFlags(), impdia.getRecordLength(), 0);
					}
					// Hand over to TIFiles
				}
				else {
					if (impdia.importAsBinary() || impdia.importAsOther()) {
						abyTif = TIFiles.createTfi(abyContent, impdia.getFileName(), impdia.getFlags(), impdia.getRecordLength(), 0);
					}
					else {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImportContentBug"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
					}
/*					else {
						abyTif = TIFiles.createTfi(abyContent, impdia.getFileName(), impdia.getFlags(), impdia.getRecordLength(), impdia.getRecordCount());
					} */
				}
			}
			
			//		System.out.println(Utilities.hexdump(0, 0, abyTif, abyTif.length, false));
			/* Insert it into the image */
			try {
				dirCurrent.insertFile(abyTif, impdia.getFileName(), bReopen);
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
			}
			catch (FileExistsException fx) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("ImportFileExists"), fx.getMessage()), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
				bDone = false;
				bValid = false;
			}
			catch (EOFException eox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), eox.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
			}			
			catch (ImageFullException ix) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), sName + ": " + ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
			}
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
			}
			catch (InvalidNameException fx) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("InvalidFileName") + ": " + fx.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
				bDone = false;
				bValid = false;				
			}
		}
		imagetool.refreshPanel(dirCurrent.getVolume());
	}
}
