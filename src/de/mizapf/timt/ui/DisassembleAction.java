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
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.awt.Cursor;

import de.mizapf.timt.assm.Assembler;
import de.mizapf.timt.util.Utilities;
import de.mizapf.timt.files.*;
import de.mizapf.timt.TIImageTool;

public class DisassembleAction extends Activity {

	public String getMenuName() {
		return imagetool.langstr("DisTMS");
	}
	
	public String getActionName() {
		return "DISASS";
	}
	
	public void go() {
		String sText = TIImageTool.langstr("NoContent");
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Directory dirCurrent = dvCurrent.getDirectory();
		DirectoryPanel dp = dvCurrent.getPanel();
		
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		for (Element selected : dvCurrent.getSelectedEntries()) {
			if (selected instanceof TFile) {
				try {
					byte[] content = null;
					if (((TFile)selected).isProgram()) content = ((TFile)selected).getRawContent();
					else content = ((TFile)selected).getRecordContent();
					
					DisassParamDialog disparm = new DisassParamDialog(dvCurrent.getFrame(), content, false);
					disparm.setType(((TFile)selected).isProgram());
					disparm.createGui(imagetool.boldFont);
					String sHash = Utilities.getHash(content);
					disparm.setParams(imagetool.loadDisassParams(TIImageTool.ASMPREFIX, sHash));
					disparm.setVisible(true);
					if (disparm.confirmed()) {			
						imagetool.saveDisassParams(TIImageTool.ASMPREFIX, sHash, disparm.getStartAddress(), disparm.getOffset(), disparm.getLength(), disparm.getHint(), false);

						if (((TFile)selected).isProgram() && disparm.symbolic()) {
							byte[] toc = createTocFromDump(content, disparm.getOffset(), disparm.getStartAddress());
							Assembler tms9900 = new Assembler(toc);
							sText = tms9900.disassembleTaggedObjectCode(disparm.getOffset(), disparm.getStartAddress(), disparm.getLength(), disparm.getHint(), disparm.showDataLoc());						
						}
						else {
							Assembler tms9900 = new Assembler(content);
							if (((TFile)selected).isProgram())
								sText = tms9900.disassembleMemoryImage(disparm.getOffset(), disparm.getStartAddress(), disparm.getLength(), disparm.getHint(), disparm.showDataLoc());
							else 
								sText = tms9900.disassembleTaggedObjectCode(disparm.getOffset(), disparm.getStartAddress(), disparm.getLength(), disparm.getHint(), disparm.showDataLoc());
						}
							
						imagetool.showTextContent(TIImageTool.langstr("Disasmd") + " " + selected.getName(), sText);
					}
				}
				catch (IOException iox) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), iox.getClass().getName(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
				}
				catch (FormatException fx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("DisasmBadParam") + ": " + fx.getMessage(), TIImageTool.langstr("DisasmError"), JOptionPane.ERROR_MESSAGE); 
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
				}
			}
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	/** Creates a tagged object code from a dump so that it will be disassembled
		with the features of the TocDisassembler.
		
		TODO: Autoselect all binaries
	*/
	private byte[] createTocFromDump(byte[] content, int offset, int startAddr) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write((byte)0x01);
		baos.write((byte)0x00);
		baos.write((byte)0x00);
		for (int i=0; i < 8; i++) baos.write(' ');
		baos.write('9');
		baos.write((startAddr >> 8)&0xff);
		baos.write(startAddr&0xff);
		
		offset = offset & 0xfffffffe;
		for (int i=offset; i < content.length; i++) {
			if ((i & 1)==0) baos.write('B');
			baos.write(content[i]);
		}
		baos.write(':');
		return baos.toByteArray();
	}
}
