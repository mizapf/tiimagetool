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
import java.util.*;
import de.mizapf.timt.TIImageTool;

public class PreferencesAction extends Activity {
	
	public String getMenuName() {
		return TIImageTool.langstr("Preferences") + "...";
	}
	
	public String getActionName() {
		return "PREFER";
	}
	
	public void go() {
		PreferencesDialog param = new PreferencesDialog(imagetool.getMainFrame(), imagetool);
		param.setSettings(settings);
		param.createGui(imagetool.boldFont);
		param.setVisible(true);
		if (param.confirmed()) {
			Map<String,JComponent> oursettings = param.getSettings();
			for (String prop : oursettings.keySet()) {
				JComponent jc = oursettings.get(prop);
				if (jc instanceof JTextField) {
					String value = ((JTextField)jc).getText();
					settings.put(prop, value);
				}
				else {
					if (jc instanceof JCheckBox) {
						boolean selected = ((JCheckBox)jc).isSelected();
						settings.put(prop, selected? "true" : "false");
					}
					else {
						if (jc instanceof JComboBox) {
							int index = ((JComboBox)jc).getSelectedIndex();
							settings.put(prop, String.valueOf(index));
						}
					}
				}
			}
		}	
	}
}
