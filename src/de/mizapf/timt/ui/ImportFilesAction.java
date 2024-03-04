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

import javax.swing.*;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.EOFException;
import java.io.FileReader;
import java.awt.Dimension;
import java.awt.Cursor;
import java.util.Properties;

import de.mizapf.timt.files.*;
import de.mizapf.timt.util.TIFiles;
import de.mizapf.timt.TIImageTool;

public class ImportFilesAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("ImportFiles") + "...";
	}
	
	public String getActionName() {
		return "IMPORTFILE";
	}

	/** Recursive method. */
	private void importDirectory(DirectoryView dvCurrent, Directory dirCurrent, java.io.File iofDirectory) throws ProtectedException, IOException, FileExistsException, ImageFullException, ImageException, InvalidNameException, IllegalOperationException {
		java.io.File[] aiof = iofDirectory.listFiles();

		String sDirName = iofDirectory.getName().toUpperCase();
		
		// Sanitize name
		sDirName = imagetool.createValidInputFileName(sDirName);
							
		// Shall we look for the metafile?
		if (settings.getPropertyBoolean(TIImageTool.KEEPNAME)==false) {
			for (java.io.File iofile : aiof) {
				Directory sub = null;
				if (iofile.getName().endsWith("meta.inf")) {
					Properties propdir = new Properties();
					propdir.load(new FileReader(iofile));
					sDirName = propdir.getProperty("dirname"); 
				}
			}
		}
				
		Directory sub = dirCurrent.createSubdirectory(sDirName);

		for (java.io.File iofile : aiof) {
			if (iofile.isDirectory()) {
				importDirectory(dvCurrent, sub, iofile);
			}
			else {
				if (!iofile.getName().endsWith("meta.inf")) {
					FileInputStream fis = new FileInputStream(iofile);
					DataInputStream dis = new DataInputStream(fis);
					byte[] abyTif = new byte[dis.available()];
					dis.readFully(abyTif);
					dis.close();
					fis.close();

					try {
						imagetool.putTIFileIntoImage(sub, dvCurrent, abyTif, iofile.getName());
					}
					catch (FileExistsException fx) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("ImportFileExists"), fx.getMessage()), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					}
					catch (EOFException ex) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), ex.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					}
				}
			}
		}
	}
	
	private void doImport(java.io.File[] afile, DirectoryView dvCurrent) {
		boolean bOK = true;

		Directory dirCurrent = dvCurrent.getDirectory();
		Volume volTarget = dirCurrent.getVolume();

		for (java.io.File iofile:afile) {
			try {
				if (iofile.isDirectory()) {
					importDirectory(dvCurrent, dirCurrent, iofile);
				}
				else {
					FileInputStream fis = new FileInputStream(iofile);
					DataInputStream dis = new DataInputStream(fis);
					byte[] abyTif = new byte[dis.available()];
					dis.readFully(abyTif);
					
					try {
						int nSectorsInTif = TIFiles.getTotalNumberOfSectors(abyTif);
						if (TIFiles.hasHeader(abyTif)) {
							abyTif = TIFiles.normalizeLength(abyTif);
//							if (!TIFiles.hasProperSize(abyTif)) {
//								JOptionPane.showMessageDialog(dvCurrent.getFrame(), iofile.getName() + ": File is clipped; should be at least " + (nSectorsInTif* TFileSystem.SECTOR_LENGTH + 128) + " bytes long.", "Import error", JOptionPane.ERROR_MESSAGE);
//								continue;
//							}
//							else {
								if ((abyTif.length - 128) != nSectorsInTif * TFileSystem.SECTOR_LENGTH) {
									// Clip the file
									byte[] abyNew = new byte[nSectorsInTif * TFileSystem.SECTOR_LENGTH + 128];
									System.arraycopy(abyTif, 0, abyNew, 0, abyNew.length);
									abyTif = abyNew;									
								}
//							}
						}
						imagetool.putTIFileIntoImage(dirCurrent, dvCurrent, abyTif, iofile.getName());
					}
					catch (FileExistsException fx) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("ImportFileExists"), fx.getMessage()), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					}
					catch (ArrayIndexOutOfBoundsException ex) {
						if (abyTif.length==0) {
							JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("ImportFileEmpty"), iofile.getName()), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
						}
						else {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("ImportFileFailed"), iofile.getName()), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
						}
					}					
					catch (EOFException ex) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), ex.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					}
					catch (InvalidNameException ix) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("InvalidFileName") + ": " + ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					}
					dis.close();
					fis.close();
				}
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
			}
			catch (IOException iox) {
				iox.printStackTrace();
				bOK = false;
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
			}
			catch (FileExistsException fnfx) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("ImportFileExists"), iofile.getName()), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
				bOK = false;
			}
			catch (ImageException ix) {
				bOK = false;
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), iofile.getName() + ": " + ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
				break;
			}
			catch (InvalidNameException ix) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("InvalidFileName") + ": " + ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
				bOK = false;
			}
			catch (IllegalOperationException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IllegalOperation") + ": " + iox.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
				bOK = false;
			}
		}
		try {
			dirCurrent.commit(true);
		}
		catch (ImageException ix) {
			ix.printStackTrace();
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImageError"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
		}		
		catch (ProtectedException px) {
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
		}
		
		imagetool.refreshPanel(volTarget);			
		//			if (bOK && afile.length>1) JOptionPane.showMessageDialog(dvCurrent.getFrame(), "Import completed sucessfully", "Import files", JOptionPane.INFORMATION_MESSAGE);
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		DirectoryPanel dp = dvCurrent.getPanel();

		JFileChooser jfc = null;
		if (imagetool.getSourceDirectory("impexp")!=null) jfc = new JFileChooser(imagetool.getSourceDirectory("impexp"));
		else jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		Dimension dim = settings.getPropertyDim(TIImageTool.FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);

		jfc.setMultiSelectionEnabled(true);

		TFIFileFilter im = new TFIFileFilter();
		jfc.addChoosableFileFilter(im);

		if (settings.getPropertyBoolean(imagetool.TFIFILTER)) {
			jfc.setFileFilter(im);
		}
		
		int nReturn = jfc.showOpenDialog(dvCurrent.getFrame());

		if (nReturn == JFileChooser.APPROVE_OPTION) {
			// Save dialog dimensions and directory for next time
			settings.put(TIImageTool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
			java.io.File[] afile = jfc.getSelectedFiles();
			imagetool.setSourceDirectory(afile[0].getParentFile(), "impexp");
			
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			doImport(afile, dvCurrent);
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	public void importElements(java.io.File[] afile, DirectoryView targetView) {
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		doImport(afile, targetView);		
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
