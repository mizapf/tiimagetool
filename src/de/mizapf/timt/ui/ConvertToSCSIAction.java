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

import de.mizapf.timt.files.*;

public class ConvertToSCSIAction extends Activity {

	public String getMenuName() {
		return imagetool.langstr("ConvertSCSI");
	}
	
	public String getActionName() {
		return "TOSCSI";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Volume vol = dvCurrent.getVolume();

		if (vol.isCHDImage()) {
			JOptionPane.showMessageDialog(m_parent, "Cannot convert the image inside the CHD container. You must extract the raw contents,\nconvert those, and import into a new CHD.", "Convert problem", JOptionPane.WARNING_MESSAGE);						
			return;
		}
		
		int nRet = JOptionPane.showConfirmDialog(m_parent, "This will remove the CHS information from the file system. Continue?", "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (nRet == JOptionPane.OK_OPTION) {
			try {
				vol.hfdc2scsi();
				// TODO: Close image
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent, "Image broken: " + iox.getClass().getName(), "Convert error", JOptionPane.ERROR_MESSAGE); 
			}			
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(m_parent, "Image broken: " + ix.getMessage(), "Convert error", JOptionPane.ERROR_MESSAGE); 
			}
			catch (ProtectedException px) {
				JOptionPane.showMessageDialog(m_parent, "Image protected: " + px.getMessage(), "Convert error", JOptionPane.ERROR_MESSAGE); 
				return;
			}
			try {
				imagetool.reloadVolume(vol);
				imagetool.refreshPanel(vol);
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(m_parent, "Could not re-open image file: " + e.getMessage(), "Read error", JOptionPane.ERROR_MESSAGE); 
			}
		}		
	}
}
