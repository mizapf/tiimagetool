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

import javax.swing.*;
import java.io.IOException;
import java.awt.Cursor;

import de.mizapf.timt.assm.GPLAssembler;

import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class GPLDisassembleAction extends Activity {

	public String getMenuName() {
		return imagetool.langstr("DisGPL");
	}
	
	public String getActionName() {
		return "GDISASS";
	}
	
	public void go() {
		String sText = imagetool.langstr("NoContent");

		DirectoryView dvCurrent = imagetool.getSelectedView();
		DirectoryPanel dp = dvCurrent.getPanel();
		Directory dirCurrent = dvCurrent.getDirectory();

		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		for (Element selected : dvCurrent.getSelectedEntries()) {
			if (selected instanceof TFile) {
				try {
					byte[] content = ((TFile)selected).getRawContent();
					DisassParamDialog disparm = new DisassParamDialog(dvCurrent.getFrame(), content, true);
					disparm.createGui(imagetool.boldFont);
					String sHash = Utilities.getHash(content);
					disparm.setParams(imagetool.loadDisassParams(TIImageTool.GPLPREFIX, sHash));
					disparm.setVisible(true);
					int len = disparm.getLength();
					if (len==0) len = content.length; 
					if (disparm.confirmed()) {
						boolean bInvAddr = disparm.skipInvalidAddresses();
						imagetool.saveDisassParams(TIImageTool.GPLPREFIX, sHash, disparm.getStartAddress(), disparm.getOffset(), len, disparm.getHint(), bInvAddr);
						GPLAssembler gplasm = new GPLAssembler();
						sText = gplasm.disassemble(content, disparm.getOffset(), disparm.getStartAddress(), len, disparm.getHint(), bInvAddr, disparm.showDataLoc());

						imagetool.showTextContent(imagetool.langstr("GPLDisasmd") + " " + selected.getName(), sText);
					}
				}
				catch (FormatException fx) {
					fx.printStackTrace();
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), imagetool.langstr("GPLInvSkip"), imagetool.langstr("DisasmBadParam"), JOptionPane.ERROR_MESSAGE); 
				}
				catch (IOException iox) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), imagetool.langstr("IOError") + ": " + iox.getClass().getName(), imagetool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), imagetool.langstr("ImageError") + ": " + ix.getMessage(), imagetool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
				}
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
