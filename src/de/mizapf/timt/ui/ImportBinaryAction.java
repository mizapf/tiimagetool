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
import java.awt.Dimension;
import java.awt.Cursor;

import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class ImportBinaryAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("ImpBinary") + "...";
	}
	
	public String getActionName() {
		return "IMPORTBIN";
	}
	
	public void go() {
		boolean bOK = true;
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		DirectoryPanel dp = dvCurrent.getPanel();

		JFileChooser jfc = null;
		if (imagetool.getSourceDirectory("impexp")!=null) jfc = new JFileChooser(imagetool.getSourceDirectory("impexp"));
		else jfc = new JFileChooser();
		Dimension dim = settings.getPropertyDim(TIImageTool.FILEDIALOG);
		if (dim!=null) jfc.setPreferredSize(dim);

		jfc.setMultiSelectionEnabled(true);
		int nReturn = jfc.showOpenDialog(dvCurrent.getFrame());

		if (nReturn == JFileChooser.APPROVE_OPTION) {
			settings.put(TIImageTool.FILEDIALOG, jfc.getWidth() + "x" + jfc.getHeight());
			java.io.File[] afile = jfc.getSelectedFiles();
			imagetool.setSourceDirectory(afile[0].getParentFile(), "impexp");
			
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			Volume volTarget = dirCurrent.getVolume();

			try {
				volTarget.reopenForWrite();
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImageFWP"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE); 				
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}

			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("NotReopen"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 				
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;
			}

			boolean bGuess = false; 
			for (java.io.File iofile:afile) {
				try {
					FileInputStream fis = new FileInputStream(iofile);
					DataInputStream dis = new DataInputStream(fis);
					byte[] abyTif = new byte[dis.available()];
					dis.readFully(abyTif);
					try {
						bGuess = imagetool.putBinaryFileIntoImage(dirCurrent, abyTif, iofile.getName(), afile.length>1, bGuess);
					}
					catch (ProtectedException px) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					}
					catch (FileExistsException fx) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("ImportFileExists"), fx.getMessage()), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					}
					catch (EOFException ex) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), ex.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					}
					catch (InvalidNameException ix) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("InvalidName") + ": " + ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					}
					dis.close();
					fis.close();
				}
				catch (FileNotFoundException fnfx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("FileNotFound") + ": " + fnfx.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE);
					bOK = false;
				}
				catch (IOException iox) {
					iox.printStackTrace();
					bOK = false;
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
				}
				catch (ImageException ix) {
					bOK = false;
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), ix.getMessage(), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 
					break;
				}
			}
			try {
				volTarget.reopenForRead();
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("NotReopen"), TIImageTool.langstr("ImportError"), JOptionPane.ERROR_MESSAGE); 				
			}			
			imagetool.refreshPanel(volTarget);			
			if (bOK && afile.length>1) JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("Completed"), TIImageTool.langstr("Import")), TIImageTool.langstr("ImpBinary"), JOptionPane.INFORMATION_MESSAGE);

			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
}
