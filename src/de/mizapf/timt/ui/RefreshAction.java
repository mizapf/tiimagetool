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
    
    Copyright 2024 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import de.mizapf.timt.TIImageTool;
import java.awt.event.KeyEvent;

public class RefreshAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_F5;
	}

	public int getModifier() {
		return 0;
	}

	public String getMenuName() {
		return TIImageTool.langstr("Menu.Help.Refresh");
	}
	
	public String getActionName() {
		return "RFSH";
	}
	
	public void go() {
		System.out.println("Refresh");
		imagetool.refresh();
	}
}
