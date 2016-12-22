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

import java.io.IOException;
import javax.swing.*;
import de.mizapf.timt.TIImageTool;
import de.mizapf.timt.files.*;

public class ConvertToSCSIAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("ConvertSCSI") + "...";
	}
	
	public String getActionName() {
		return "TOSCSI";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Volume vol = dvCurrent.getVolume();

		if (vol.isCHDImage()) {
			JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ConvNotInCHD"), TIImageTool.langstr("ConvertError"), JOptionPane.ERROR_MESSAGE);						
			return;
		}
		
		int nRet = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("ConvLoseInfo"), TIImageTool.langstr("Warning"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (nRet == JOptionPane.OK_OPTION) {
			try {
				vol.hfdc2scsi();
				// TODO: Close image
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("ConvertError"), JOptionPane.ERROR_MESSAGE); 
			}			
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("ConvertError"), JOptionPane.ERROR_MESSAGE); 
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("VolumeWP") + ": " + px.getMessage(), TIImageTool.langstr("ConvertError"), JOptionPane.ERROR_MESSAGE); 
				return;
			}
			try {
				imagetool.reloadVolume(vol);
				imagetool.refreshPanel(vol);
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("NotReopen") + ": " + e.getMessage(), TIImageTool.langstr("ReadError"), JOptionPane.ERROR_MESSAGE); 
			}
		}		
	}
}
