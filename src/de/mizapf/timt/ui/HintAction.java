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
    
    Copyright 2015 Michael Zapf
    www.mizapf.de
    
****************************************************************************/
package de.mizapf.timt.ui;

import javax.swing.*;
import java.awt.event.KeyEvent;
import de.mizapf.timt.TIImageTool;

public class HintAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_F3;
	}

	public int getModifier() {
		return 0;
	}

	private String[] entry;
		
	public String getMenuName() {
		return imagetool.langstr("Hints");
	}
	
	public String getActionName() {
		return "HINTS";
	}
	
	public void go() {	
		entry = new String[5];
		entry[0] = TIImageTool.langstr("Hint1");
		entry[1] = TIImageTool.langstr("Hint2");
		entry[2] = TIImageTool.langstr("Hint3");
		entry[3] = TIImageTool.langstr("Hint4"); 
		entry[4] = TIImageTool.langstr("Hint5"); 
		
		String head1 = "<html><body style='width: ";
		String head2 = "px'>"; 
		String foot = "</body></html>";
		StringBuilder sb = new StringBuilder();
		sb.append(head1).append("300").append(head2);
		sb.append("<h3>");
		sb.append(entry[(int)(Math.random()*entry.length)]);
		sb.append("...</h3>");
		String hint = imagetool.getHint();
		sb.append("<p>").append(hint).append("</p>");
		sb.append(foot);	
		JLabel jl = new JLabel(sb.toString());
		jl.setFont(TIImageTool.dialogFont);
		
		JPanel jp = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
		jp.add(jl);

		jp.add(Box.createVerticalStrut(10));
		
		JCheckBox cb1 = new JCheckBox(TIImageTool.langstr("HintIKnow"));
		cb1.setFont(TIImageTool.dialogFont);
		jp.add(cb1);
		
		JCheckBox cb2 = new JCheckBox(TIImageTool.langstr("HintDont"));
		cb2.setFont(TIImageTool.dialogFont);
		jp.add(cb2);
		
		JCheckBox cb3 = new JCheckBox(TIImageTool.langstr("HintForgot"));
		cb3.setFont(TIImageTool.dialogFont);
		jp.add(cb3);

		jp.add(Box.createVerticalStrut(10));

		JOptionPane.showMessageDialog(m_parent, jp, TIImageTool.langstr("Hint"), JOptionPane.INFORMATION_MESSAGE); 
		
		imagetool.setHintResponse(cb1.isSelected(), cb2.isSelected(), cb3.isSelected()); 
	}
}
