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
    
    Copyright 2020 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import java.awt.event.KeyEvent;
import de.mizapf.timt.TIImageTool;

public class SaveImageAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_S;
	}
	
	public String getMenuName() {
		return TIImageTool.langstr("Save");
	}
	
	public String getActionName() {
		return "SAVE";
	}
	
	public void go() {
		System.out.println("Save");
	}
}

/*
Save concept

- Determine Maximum heap space on startup
- When an operation takes more space than available on the heap:
  (or 75% of the maximum space)
  
  * Warning: This operation cannot be undone due to lack of buffer space. All
             unsaved changes to this image will be written to disk before 
             continuing.
             [Continue] [Abort]
  
  (can be avoided by -Xmx parameter on startup)
  
  In this case, the previous image state is committed, and this change
  is directly written to disk (write-around).
  
  The image is then marked as not modified.


  This problem is only likely to appear with hard disk images
  (e.g. recursive copy of all files from one HD to another)

  This may require a pre-check for required buffer space.
  
  
*/