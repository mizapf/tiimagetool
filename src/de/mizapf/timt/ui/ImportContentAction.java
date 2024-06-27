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
import java.awt.Point;
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

	public final static int NONE = 0;
	public final static int BASIC = 1;
	public final static int TEXT = 2;
	public final static int NEW = 3;
	
	TFile m_file;
	int m_nType;
	
	private int getType(TFile file) throws IOException, ImageException {
		try {
			if (file.isBasicFile()) return BASIC;
			if (!file.isProgram() && file.mayBePrintable()) return TEXT;
		}
		catch (FormatException fx) {
			if (file.mayBePrintable()) return TEXT;
		}
		return NONE;
	}
	
	public String getMenuName() {
		return TIImageTool.langstr("ImpText") + "...";
	}
	
	public String getActionName() {
		return "IMPORTCONT";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		m_nType = NEW;
		EditorFrame dv = new EditorFrame(imagetool, dvCurrent.getFrame(), this, dvCurrent, null, null, true);
		dv.setSize(settings.getPropertyDim(TIImageTool.CONTSIZE));
		Point loc = imagetool.getFrameLocation();		
		dv.setLocation(loc.x+20, loc.y+20);
	}
	
	public void go(TFile file, String sText) {
		m_file = file;
		DirectoryView dvCurrent = imagetool.getSelectedView();

		try {
			m_nType = getType(file);
			// System.out.println(m_nType);
			EditorFrame dv = new EditorFrame(imagetool, dvCurrent.getFrame(), this, dvCurrent, file.getName(), sText, m_nType != NONE);
			dv.setSize(settings.getPropertyDim(TIImageTool.CONTSIZE));
			Point loc = imagetool.getFrameLocation();		
			dv.setLocation(loc.x+20, loc.y+20);
		}
		catch (IOException iox) {
			iox.printStackTrace();
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
		}
		catch (ImageException ix) {
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
		}
	}

	public boolean convertAndImport(byte[] abyContent, DirectoryView dvCurrent, String sSuggested, boolean bCommit) {
		if (!imagetool.viewStillThere(dvCurrent)) {
			JOptionPane.showMessageDialog(imagetool.getMainFrame(), TIImageTool.langstr("ImportContentViewClosed"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
			return true;
		}
		boolean bValid = false;
		String sName = null;
		Directory dirCurrent = dvCurrent.getDirectory();
		
		ImportDialog impdia = null;
		boolean bDone = false;
		ImportParameters impParam = null;
		boolean bOK = true;
		boolean bUpdate = false;
		
		while (!bDone) {   // While file exists
			if (m_file == null) {
				while (!bValid) {
					int mode = BasicCruncher.contentLooksLikeBasic(abyContent, settings.getPropertyBoolean(TIImageTool.VERBOSE))? ImportDialog.BASIC : ImportDialog.TEXTONLY;					
					impdia = new ImportDialog(dvCurrent.getFrame(), sSuggested, false, mode);
					impdia.setContent(abyContent);
					impdia.setSettings(settings);
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
						return false;
					}
					impParam = impdia.getParameters();
				}
			}
			else {
				// Editing an existing file
				/*
				impParam = new ImportParameters();
				impParam.asBasic = m_file.isBasicFile();
				impParam.asText = m_file.isTextFile();
				impParam.asWideText = m_file.isWideTextFile();
				impParam.binary = m_file.isProgram();
				inpParam.other = m_file.isProgram();
				inpParam.fileName = m_file.getName();
				*/
				int mode = BasicCruncher.contentLooksLikeBasic(abyContent, settings.getPropertyBoolean(TIImageTool.VERBOSE))? ImportDialog.BASIC : ImportDialog.TEXTONLY;					
				// System.out.println("mode = " + mode);
				impdia = new ImportDialog(dvCurrent.getFrame(), m_file.getName(), false, mode);
				impdia.setFromEditor(true);
				impdia.setContent(abyContent);
				impdia.setSettings(settings);
				impdia.createGui();
				impdia.setVisible(true);
				if (!impdia.confirmed()) {
					return false;
				}
				impParam = impdia.getParameters();
				bUpdate = true;
			}
			
			bDone = true;
			byte[] abyTif = prepareFileImage(dvCurrent, impParam, abyContent);

			if (abyTif == null) {
				bDone = false;
				bValid = false;
				continue;
			}
			
			//		System.out.println(Utilities.hexdump(0, 0, abyTif, abyTif.length, false));
			/* Insert it into the image */
			try {
				if (bUpdate) dirCurrent.deleteFile(m_file, true);
				dirCurrent.insertFile(abyTif, impdia.getFileName(), bCommit);
				dirCurrent.commit(bCommit);
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
				if (bUpdate) bOK = false;				
			}
			catch (FileExistsException fx) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("ImportFileExists"), fx.getMessage()), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
				bDone = false;
				bValid = false;
			}
			catch (IOException iox) {
				iox.printStackTrace();
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
				if (bUpdate) bOK = false;				
			}			
			catch (ImageFullException ix) {
				String sMess = ix.getMessage();
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), sMess, TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
				if (bUpdate) bOK = false;				
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
		if (!bOK) {
			dirCurrent.getVolume().rollback();
		}
		imagetool.refresh(dvCurrent);
		return bOK;
	}
	
	public byte[] convertForImport(byte[] abyContent, DirectoryView dvCurrent, String sSuggested) {
		if (!imagetool.viewStillThere(dvCurrent)) {
			JOptionPane.showMessageDialog(imagetool.getMainFrame(), TIImageTool.langstr("ImportContentViewClosed"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		boolean bValid = false;
		String sName = null;
		Directory dirCurrent = dvCurrent.getDirectory();
		
		ImportDialog impdia = null;
		boolean bDone = false;
		ImportParameters impParam = null;
		boolean bOK = true;
		boolean bUpdate = false;
		byte[] abyTif = null;
		
		while (!bDone) {   // While file exists
			while (!bValid) {
				int mode = BasicCruncher.contentLooksLikeBasic(abyContent, settings.getPropertyBoolean(TIImageTool.VERBOSE))? ImportDialog.BASIC : ImportDialog.TEXTONLY;					

				if (mode != ImportDialog.BASIC && settings.getPropertyBoolean(TIImageTool.IMPTEXT) && looksLikePlainText(abyContent)) {
					impParam = new ImportParameters(sSuggested);
					sName = sSuggested;
					bValid = TFile.validName(sName);
					if (!bValid) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("InvalidFileName") + ": " + sName, TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);				
					}
				}
				else {
					impdia = new ImportDialog(dvCurrent.getFrame(), sSuggested, false, mode);
					impdia.setContent(abyContent);
					impdia.setSettings(settings);
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
						return null;
					}
					impParam = impdia.getParameters();
				}
			}
			
			bDone = true;
			abyTif = prepareFileImage(dvCurrent, impParam, abyContent);
			
			if (abyTif == null) {
				bDone = false;
				bValid = false;
				continue;
			}
		}
		return abyTif;
	}
	
	private byte[] prepareFileImage(DirectoryView dvCurrent, ImportParameters impParam, byte[] abyContent) {
		byte[] abyTif = null;
		boolean bTab = false;
		
		if (impParam.asBasic) {
			BasicCruncher bc = new BasicCruncher();
			try {
				String fullprg = new String(abyContent, Charset.forName("ISO-8859-1"));
				String split = Utilities.getSeparator(fullprg);
				String[] lines = fullprg.split(split);					
				byte[] basicfile = null;
				abyTif = bc.textToBasic(lines, impParam.fileName, impParam.basicVersion, impParam.saveFormat, impParam.protect);
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
				return null;
				
			}				
			catch (IOException iox) {
				iox.printStackTrace();
				return null;
			}
		}
		else {
			
			if (impParam.asText || impParam.asWideText) {
				
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
						return null;							
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
						return null;							
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
				TIFiles impfile = new TIFiles(impParam.fileName, TFile.flagsToType(impParam.flags), impParam.recordLength);
				
				try {
					for (int i=0; i < lines.length; i++) {
						impfile.writeRecord(lines[i].getBytes(), 0x20);
					}
				}
				catch (IOException iox) {
					System.out.println("IOException when importing " + impParam.fileName);
					iox.printStackTrace();
				}
				sectors = impfile.closeAndGetBytes(false, false);
				abyTif = sectors;
			}
			else {
				if (impParam.binary || impParam.other) {
					abyTif = TIFiles.createTfi(abyContent, impParam.fileName, impParam.flags, impParam.recordLength, 0);
				}
				else {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImportContentBug"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		return abyTif;
	}
	
	private boolean looksLikePlainText(byte[] abyContent) {
		boolean bResult = true;
		int nLineLength = 0;
		for (int i=0; i < abyContent.length; i++) {
			nLineLength++;
			if (nLineLength > 81) { bResult = false; break; }
			
			if (abyContent[i] < 32 || abyContent[i] > 126) {
				if (abyContent[i] != 0x0a &&  abyContent[i] != 0x0d) {
					System.out.println("Not a text file: character = " + abyContent[i]);
					bResult = false;
				}
				else nLineLength = 0;
			}
		}
		return bResult;
	}
}
