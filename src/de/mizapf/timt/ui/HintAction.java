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

	private String[] entry = { "Maybe you already know",
								"Here's another tip for you",
								"It might be interesting to note that",
								"I bet you're glad to hear that",
								"Just thought I should tell you that" };
	
	public String getMenuName() {
		return imagetool.langstr("Hints");
	}
	
	public String getActionName() {
		return "HINTS";
	}
	
	public void go() {	
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
		
		JCheckBox cb1 = new JCheckBox("I know that one, don't tell me again.");
		cb1.setFont(TIImageTool.dialogFont);
		jp.add(cb1);
		
		JCheckBox cb2 = new JCheckBox("Don't show me hints on startup.");
		cb2.setFont(TIImageTool.dialogFont);
		jp.add(cb2);
		
		JCheckBox cb3 = new JCheckBox("I forgot the old hints; please show them again.");
		cb3.setFont(TIImageTool.dialogFont);
		jp.add(cb3);

		jp.add(Box.createVerticalStrut(10));

		JOptionPane.showMessageDialog(m_parent, jp, "Hint", JOptionPane.INFORMATION_MESSAGE); 
		
		imagetool.setHintResponse(cb1.isSelected(), cb2.isSelected(), cb3.isSelected()); 
	}
}
