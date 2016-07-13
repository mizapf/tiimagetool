package de.mizapf.timt.conn;
import javax.swing.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.*;

public class ProgressView extends JDialog implements ActionListener {
	
	String m_sText;
	JLabel m_jlBlockSize;
	JLabel m_jlChecksum;	
	JLabel m_jlBytes;
	JLabel m_jlStatus;
	JFrame m_frmMain;
	JButton m_btnStop;
	int m_nColumnWidth;
	boolean m_bStop;
	
	public ProgressView(String sTitle, JFrame frmMain) {
		super(frmMain, sTitle, false);
		m_frmMain = frmMain;
	}
	
/*
	| 	XModem upload/download progress										|

		Block size 				128
		Integrity check 		Checksum / CRC16
		
		Bytes read / written	1234
		Status					OK / Error
			
				+---------------+	
				|	Abort/close	|
				+---------------+   
*/	
	public void createGui(Font font) {
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		setFont(font);

		FontMetrics fm = ((Graphics2D)(m_frmMain.getGraphics())).getFontMetrics(font);
		m_nColumnWidth = fm.stringWidth("Bytes transferred");
		
		int nHeight = getHeight(font, "B");
		
		m_bStop = false;
		
		m_jlBlockSize = new JLabel();
		setBlockSize(0);
		
		m_jlBytes = new JLabel();
		setTransferredBytes(0);
		
		m_jlChecksum = new JLabel();
		setUseCRC16(false);
		
		m_jlStatus = new JLabel();
		setStatus("-");
		
		m_btnStop = new JButton("");
		setButtonText("Abort");
		
		add(Box.createVerticalStrut(10));		
		createLine("Block size", nHeight, m_jlBlockSize);
		createLine("Integrity check", nHeight, m_jlChecksum);
		add(Box.createVerticalStrut(10));		

		createLine("Bytes transferred", nHeight, m_jlBytes);
		createLine("Status", nHeight, m_jlStatus);
		
		add(Box.createVerticalStrut(10));		
	
		Box box = new Box(BoxLayout.X_AXIS);		
		m_btnStop.addActionListener(this);
		m_btnStop.setPreferredSize(new Dimension(100, 25));
		box.add(m_btnStop);
		add(box);		
		add(Box.createVerticalStrut(10));		
		
		pack();
		setLocationRelativeTo(getParent());
	}

	private void createLine(String sText, int nHeight, JComponent jc) {
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(10));	
		JLabel jl = new JLabel(sText, SwingConstants.LEFT);
		jl.setPreferredSize(new Dimension(m_nColumnWidth, nHeight));
		box.add(jl);
		box.add(Box.createHorizontalStrut(20));
		box.add(jc);
		box.add(Box.createHorizontalStrut(10));
		box.add(Box.createHorizontalGlue());
		add(box);		
	}
	
	int getHeight(Font font, String sSample) {
		FontRenderContext frc = ((Graphics2D)(m_frmMain.getGraphics())).getFontRenderContext();
		LineMetrics lm = font.getLineMetrics(sSample, 0, 2, frc);
		return (int)Math.ceil(lm.getHeight()*1.03);
	}
	
	void setBlockSize(int nValue) {
		if (nValue==0) m_jlBlockSize.setText("-");
		else m_jlBlockSize.setText(String.valueOf(nValue));
	}
	
	void setUseCRC16(boolean bUsed) {
		if (bUsed) m_jlChecksum.setText("CRC16");
		else m_jlChecksum.setText("Checksum");
	}
	
	void setTransferredBytes(int nAmount) {
		m_jlBytes.setText(String.valueOf(nAmount));
	}
	
	void setStatus(String sText) {
		m_jlStatus.setText(sText);
	}
	
	void setButtonText(String sText) {
		m_btnStop.setText(sText);
	}
	
	public boolean stopRequested() {
		return m_bStop;
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource()==m_btnStop) {
			m_bStop = true;
		}
	}
}
