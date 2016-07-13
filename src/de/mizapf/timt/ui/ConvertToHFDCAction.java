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
import java.io.FileNotFoundException;
import javax.swing.*;

import de.mizapf.timt.files.*;
import de.mizapf.timt.util.Utilities;

public class ConvertToHFDCAction extends Activity {

	public String getMenuName() {
		return "Convert to HFDC...";
	}
	
	public String getActionName() {
		return "TOHFDC";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();
		Volume vol = dvCurrent.getVolume();

		if (vol.isCHDImage()) {
			JOptionPane.showMessageDialog(m_parent, "Cannot convert the image inside the CHD container. You must extract the raw contents,\nconvert those, and import into a new CHD.", "Convert problem", JOptionPane.WARNING_MESSAGE);						
			return;
		}
		
		CHSDialog chs = new CHSDialog(m_parent);
		chs.createGui(imagetool.boldFont);

		int nTotalSectors = vol.getTotalSectors();

		// Round up to a suitable multiple of sectors
		nTotalSectors = ((nTotalSectors + 511) / 512) * 512;

		int nCylinders = 0;
		int nSectorsPerTrack = 32; // assume this is always the case
		int nHeads = 0;
		int[] param = null;
		
		nCylinders = Utilities.guessGeometry(nSectorsPerTrack, nTotalSectors);
		if (nCylinders != -1) {
			nHeads = nTotalSectors / nSectorsPerTrack / nCylinders;
			param = Utilities.guessAdvanced(nCylinders, nHeads, nSectorsPerTrack);
		}
		else {
			JOptionPane.showMessageDialog(m_parent, "Cannot calculate a suitable geometry for the image.", "Convert problem", JOptionPane.WARNING_MESSAGE);				
			nHeads = 0;
			nCylinders = 0;
			nSectorsPerTrack = 0;
			param = new int[2];
		}
				
		chs.setParameters(nCylinders, nHeads, nSectorsPerTrack, 256, param[1], param[1], 1, false);
		chs.setVisible(true);
		
		// TODO: Close image
		if (chs.confirmed()) {
			try {
				vol.scsi2hfdc(chs.getSectorsPerTrack(), 
					chs.getStepSpeed(), 
					chs.getReducedWriteCurrent(), 
					chs.getHeads(), 
					(chs.getBufferedStep())? 1:0,
					chs.getWritePrecompensation());
			}
			catch (FileNotFoundException fnfx) {
				JOptionPane.showMessageDialog(m_parent, "Image file cannot be found or is write-protected", "Convert error", JOptionPane.ERROR_MESSAGE); 
				return;
			}
			catch (IOException iox) {
				JOptionPane.showMessageDialog(m_parent, "Image broken: " + iox.getClass().getName(), "Convert error", JOptionPane.ERROR_MESSAGE); 
				return;
			}			
			catch (ImageException ix) {
				JOptionPane.showMessageDialog(m_parent, "Image broken: " + ix.getMessage(), "Convert error", JOptionPane.ERROR_MESSAGE); 
				return;
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
