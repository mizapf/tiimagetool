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

import java.io.*;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.ArrayList;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.ImageCheck;
import de.mizapf.timt.TIImageTool;

public class CheckFSAction extends Activity {

	public String getMenuName() {
		return TIImageTool.langstr("Check");
	}
	
	public String getActionName() {
		return "CHECK";
	}
	
	public void go() {
		int nRet = 0;
		AllocationMap allocMap = null;
		boolean bChangedAlloc = false;
		boolean bErrors = false;
		boolean bChangedSector0 = false;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		PrintStream ps = new PrintStream(baos);
		
		DirectoryView dv = imagetool.getSelectedView();
		Volume vol = dv.getVolume();
		JCheckBox cb = null;
				
		/* Part 0: Check CF7 inconsistency */
		if (vol.isCF7Volume()) {
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			ps.println("===========  " + TIImageTool.langstr("CheckingCF7Incons") + " ===========\n");
	
			try {
				int[] geom = new int[5];
				boolean badcf7 = ImageCheck.checkCF7Inconsistency(vol, geom);
				// total, heads, tracks, sectors, density 
				if (badcf7) {
					ps.println(TIImageTool.langstr("CF7Incons1") + "\n");
					ps.println(String.format(TIImageTool.langstr("CF7InconsGeom"), geom[0], geom[1], geom[2], geom[3], geom[4]));
					
					StringBuilder sbMsg = new StringBuilder();
					sbMsg.append("<html>");
					sbMsg.append(String.format(TIImageTool.langstr("CF7Incons2"), 1600, 2, 20, 40, 2));
					sbMsg.append("<br><br>");
					sbMsg.append(String.format(TIImageTool.langstr("CF7Incons3"), geom[0], geom[1], geom[2], geom[3], geom[4]));
					sbMsg.append("<br><br>");
					sbMsg.append(TIImageTool.langstr("CF7Incons4"));
					sbMsg.append("</html>");
					JPanel jp = new JPanel();		
					JLabel jl = new JLabel(sbMsg.toString());
					jp.add(jl);
					nRet = JOptionPane.showConfirmDialog(m_parent, jp, TIImageTool.langstr("FileSystemCheck"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					
					if (nRet == JOptionPane.YES_OPTION) {
						bChangedSector0 = true;
						vol.setGeometry(1600, 40, 2, 20, 2); 
					}
					else {
						if (nRet == JOptionPane.CANCEL_OPTION) {
							// Stop it, don't change anything
							m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
							return;
						}
					}
					bErrors = true;
				}
				else {
					ps.println(TIImageTool.langstr("NoIncons") + "\n");
				}
			}
			catch (IOException iox) {
				iox.printStackTrace();
				ps.println(TIImageTool.langstr("IOError") + iox.getClass().getName() + ", " + iox.getMessage());
			}
			catch (ImageException ix) {
				ix.printStackTrace();
				ps.println(TIImageTool.langstr("ImageError") + ": " + ix.getMessage());
			}
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));			
		}		
		
		/* Part 1: Check underallocation */
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		int savedResp = -1;		
		ps.println("===========  " + TIImageTool.langstr("CheckingUnder") +  " ===========\n");
		allocMap = vol.getAllocationMap();
		ArrayList<AllocationGapList> broken = new ArrayList<AllocationGapList>();

		// Do the check
		ImageCheck.checkUnderAllocationInDir(vol, vol.getRootDirectory(), allocMap, broken, "", ps);

		// Now report
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));			
		if (broken.size()>0) bErrors = true;
		else ps.println(TIImageTool.langstr("NoUnder") + "\n");

		for (AllocationGapList agl:broken) {

			StringBuilder sbMsg = new StringBuilder();
			sbMsg.append("<html>");
			String fileblue = "<span style=\"color:blue\">" + agl.getName() + "</span>";
			sbMsg.append(String.format(TIImageTool.langstr("FileExtends"), fileblue));
			sbMsg.append("<br><br>");
			
			ps.println();
			ps.print(String.format(TIImageTool.langstr("FileExtends"), agl.getName()) + ": ");

			int[] nAU = agl.getAllocationGaps();
			for (int n=0; n < nAU.length-1; n++) {
				sbMsg.append(Integer.valueOf(nAU[n])).append(", ");
				ps.print(Integer.valueOf(nAU[n]) + ", ");
				if ((n % 10==9) && (n!=nAU.length-2)) sbMsg.append("<br>");
			}
			sbMsg.append(Integer.valueOf(nAU[nAU.length-1])).append(".<br>");
			ps.println(Integer.valueOf(nAU[nAU.length-1]));
			
			sbMsg.append("<br>");
			sbMsg.append(TIImageTool.langstr("DataLoss"));
			sbMsg.append("</html>");

			if (savedResp == -1) {
				JPanel jp = new JPanel();
				jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
				JLabel jl = new JLabel(sbMsg.toString());
				jp.add(jl);			
				jp.add(Box.createVerticalStrut(10));
				cb = new JCheckBox(TIImageTool.langstr("RepeatAUAlloc"));
				cb.setFont(TIImageTool.dialogFont);
				jp.add(cb);
				nRet = JOptionPane.showConfirmDialog(m_parent, jp, TIImageTool.langstr("FileSystemCheck"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			}
			else {
				nRet = savedResp;
			}

			if (savedResp == -1 && cb.isSelected()) {
				savedResp = nRet;
			}					

			if (nRet == JOptionPane.YES_OPTION) {
				bChangedAlloc = true;
				for (int au:agl.getAllocationGaps()) {
					allocMap.allocate(au);
				}
			}
			else {
				if (nRet == JOptionPane.CANCEL_OPTION) {
					// Stop it, don't change anything
					return;
				}
			}
		}

		/* 
			Part 2: Overallocation and cross-allocation
			We have an overallocation when there are sector allocations that
			do not belong to any accessible file. This happens when the
			bitmap is inconsistent with the allocations in the FIBs.
			A cross-allocation is an allocation of a sector for two files.
			This usually means that at least one of both files is corrupt at
			that point.
		*/
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		ps.println();		
		ps.println("===========  " + TIImageTool.langstr("CheckingOver") +  " ===========\n");

		ArrayList<AllocationDomain> alloc = new ArrayList<AllocationDomain>();

		// Do the check
		ImageCheck.findAllocationFaults(vol, allocMap, alloc, ps);
		
		// Now report
		savedResp = -1;
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));			
		if (alloc.size()>0) bErrors = true;
		else ps.println(TIImageTool.langstr("NoOver") + "\n");
		
		for (AllocationDomain ad:alloc) {
			if (ad.isUnassigned()) {
				ps.println(String.format(TIImageTool.langstr("AUNotAssigned"), String.valueOf(ad.getAU())));
				
				if (savedResp == -1) {
					String aublue = "<span style=\"color:blue\">" + ad.getAU() + "</span>";
					StringBuilder sbMsg = new StringBuilder();
					sbMsg.append("<html>");		
					sbMsg.append(String.format(TIImageTool.langstr("AUNotAssigned"), aublue));
					sbMsg.append("<br><br>");
					sbMsg.append(TIImageTool.langstr("AUFree"));
					sbMsg.append("</html>");
				
					JPanel jp = new JPanel();
					jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
					JLabel jl = new JLabel(sbMsg.toString());
					jp.add(Box.createVerticalStrut(10));
					jp.add(jl);
					jp.add(Box.createVerticalStrut(10));
					cb = new JCheckBox(TIImageTool.langstr("RepeatAUFree"));
					cb.setFont(TIImageTool.dialogFont);
					jp.add(cb);
					nRet = JOptionPane.showConfirmDialog(m_parent, jp, TIImageTool.langstr("FileSystemCheck"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				}
				else {
					nRet = savedResp;
				}
				
				if (savedResp == -1 && cb.isSelected()) {
					savedResp = nRet;
				}					
				
				if (nRet == JOptionPane.YES_OPTION) {
					bChangedAlloc = true;
					allocMap.deallocate(ad.getAU());
				}
				else {
					if (nRet == JOptionPane.CANCEL_OPTION) {
						return;
					}
				}
			}
		}
	
		for (AllocationDomain ad:alloc) {
			if (!ad.isUnassigned()) {
				StringBuilder sbMsg = new StringBuilder();
				String aublue = "<span style=\"color:blue\">" + ad.getAU() + "</span>";
				sbMsg.append("<html>");				
				sbMsg.append(String.format(TIImageTool.langstr("AUCrossAllocated"), aublue));
				sbMsg.append("<br>");	
				ps.println(String.format(TIImageTool.langstr("AUCrossAllocated"), String.valueOf(ad.getAU())));

				String[] asEl = ad.getAllocations();
				boolean first = true;
				for (int n=0; n < asEl.length; n++) {
					if (!first) sbMsg.append(", ");
					else first = false; 
					if (n % 10==5) sbMsg.append("<br>");
					String el = asEl[n];
					if (asEl[n].endsWith(".#")) el = asEl[n].substring(0,asEl[n].length()-2) + " (dir)"; 
					sbMsg.append(el);
					ps.println(el);
				}
				sbMsg.append(".<br>");
				
				if (savedResp == -1) {
					sbMsg.append(".<br>");
					sbMsg.append(TIImageTool.langstr("AUCrossSuggest"));
					sbMsg.append("<br>");
					sbMsg.append(TIImageTool.langstr("ContinueTest"));
					sbMsg.append("</html>");
				
					JPanel jp = new JPanel();
					jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
					JLabel jl = new JLabel(sbMsg.toString());
					jp.add(jl);
					nRet = JOptionPane.showConfirmDialog(m_parent, jp, TIImageTool.langstr("FileSystemCheck"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				}
				
				if (nRet == JOptionPane.NO_OPTION) {
					savedResp = 0;
				}
				else {
					if (nRet == JOptionPane.CANCEL_OPTION) {
						return;
					}
				}
			}
		}

		/* Part 3: Broken sectors */
		ps.println("===========  " + TIImageTool.langstr("CheckingBroken") +  " ===========\n");
		savedResp = -1;
		
		ArrayList<SectorFaultList> brokensect = new ArrayList<SectorFaultList>();
		ImageCheck.findBrokenSectors(vol.getRootDirectory(), brokensect, "", ps);
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));			
		if (brokensect.size()>0) {
			bErrors = true;
		}
		else {
			ps.println(TIImageTool.langstr("NothingBroken") + "\n");
		}
			
		for (SectorFaultList sfl:brokensect) {
			String fileblue = "<span style=\"color:blue\">" + sfl.getName() + "</span>";

			StringBuilder sbMsg = new StringBuilder();
			sbMsg.append("<html>");
			sbMsg.append(String.format(TIImageTool.langstr("BrokenSectors"), fileblue));
			sbMsg.append(":<br><br>");			

			ps.println(String.format(TIImageTool.langstr("BrokenSectors"), sfl.getName()));
			
			int[] anSect = sfl.getFaultySectors();
			if (sfl.getProblem()!=null) {
				sbMsg.append(sfl.getProblem());
				ps.println(sfl.getProblem());
			}
			else {
				int n = 0;
				for (n=0; n < anSect.length && n < 100; n++) {
					sbMsg.append(Integer.valueOf(anSect[n]));
					if (n != anSect.length-1) sbMsg.append(", "); 
					if (n % 10==9) sbMsg.append("<br>");
				}
				if (n !=  anSect.length) { 
					sbMsg.append("... (");				
					sbMsg.append(String.format(TIImageTool.langstr("Omitted"), anSect.length-101));
					sbMsg.append(") ...");
				}

				for (n=0; n < anSect.length; n++) {
					ps.println(Integer.valueOf(anSect[n]));
				}

				sbMsg.append(".<br><br>");			
				sbMsg.append(TIImageTool.langstr("ReportSector"));
			}

			if (savedResp == -1) {
				JPanel jp = new JPanel();
				jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
				jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
				sbMsg.append("<br>");
				sbMsg.append(TIImageTool.langstr("ContinueTest"));
				sbMsg.append("</html>");
				JLabel jl = new JLabel(sbMsg.toString());
				jp.add(jl);
				nRet = JOptionPane.showConfirmDialog(m_parent, jp, TIImageTool.langstr("FileSystemCheck"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				
				if (nRet == JOptionPane.NO_OPTION) {
					savedResp = 0;
				}
				else {
					if (nRet == JOptionPane.CANCEL_OPTION) {
						return;
					}
				}
			}
		}
		
		/* Part 4. L3 check. */
		ps.println("===========  " + TIImageTool.langstr("CheckL3") +  " ===========\n");
		boolean changedL3 = false;
		savedResp = -1;

		ArrayList<TFile> brokenL3 = new ArrayList<TFile>(); 
		ImageCheck.checkL3(vol.getRootDirectory(), brokenL3);
		ArrayList<TFile> fixL3 = new ArrayList<TFile>();
		
		if (brokenL3.size()>0) bErrors = true;
		else {
			ps.println(TIImageTool.langstr("L3OK") + ".\n");
		}

		JCheckBox cb2 = null;
	
		for (TFile brL3file : brokenL3) {
			String fileblue = "<span style=\"color:blue\">" + brL3file.getPathname() + "</span>";
			
			if (brL3file.hasSwappedL3Count()) {
				ps.println(String.format(TIImageTool.langstr("FileL3Swap"), brL3file.getPathname()));
			}
			else {
				ps.println(String.format(TIImageTool.langstr("FileL3Bad"), brL3file.getPathname(), brL3file.getBadRecordCount()));
			}
			
			if (savedResp == -1) {
				JPanel jp = new JPanel();
				jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
				StringBuilder sbMsg = new StringBuilder();
				sbMsg.append("<html>");
				
				if (brL3file.hasSwappedL3Count()) {
					sbMsg.append(String.format(TIImageTool.langstr("FileL3Swap"), fileblue));
					sbMsg.append("<br>");
					sbMsg.append(TIImageTool.langstr("L3Cause"));
				}
				else {
					sbMsg.append(String.format(TIImageTool.langstr("FileL3Bad"), fileblue, brL3file.getBadRecordCount()));					
				}
				sbMsg.append("<br>");
				sbMsg.append(TIImageTool.langstr("L3Repair"));
					
				sbMsg.append("<br><br></html>");				
				JLabel jl = new JLabel(sbMsg.toString());
				jp.add(jl);
				cb2 = new JCheckBox(TIImageTool.langstr("RepeatL3"));
				cb2.setFont(TIImageTool.dialogFont);
				jp.add(cb2);
				
				nRet = JOptionPane.showConfirmDialog(m_parent, jp, TIImageTool.langstr("FileSystemCheck"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			}
			else {
				nRet = savedResp;
			}

			if (savedResp == -1 && cb2.isSelected()) {
				savedResp = nRet;
			}
			
			if (nRet == JOptionPane.YES_OPTION) {
				changedL3 = true;
				fixL3.add(brL3file);
			}
			else {
				if (nRet == JOptionPane.CANCEL_OPTION) {
					return;
				}
			}
		}
				
		/* Part 5. CRC check. */
		int badcrc = -1;
		
		try {
			badcrc = ImageCheck.findCRCErrors(vol, false, false);
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}
		
		if (badcrc != -1) {
			ps.println("===========  " + TIImageTool.langstr("CheckCRC") +  " ===========\n");
			
			if (badcrc > 0) {
				ps.println(String.format(TIImageTool.langstr("FoundCRC"), badcrc));
				JPanel jp = new JPanel();
				jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
				StringBuilder sbMsg = new StringBuilder();
				sbMsg.append("<html>");
				sbMsg.append(TIImageTool.langstr("HasCRCErrors"));
				sbMsg.append("<br>");
				sbMsg.append(String.format(TIImageTool.langstr("TotalCount"), badcrc));
				sbMsg.append("<br><br>");
				sbMsg.append(TIImageTool.langstr("FixCRC"));
				sbMsg.append("<br><br></html>");
				JLabel jl = new JLabel(sbMsg.toString());
				jp.add(jl);
				cb2 = new JCheckBox(TIImageTool.langstr("ResetF7F7"));
				cb2.setFont(TIImageTool.dialogFont);
				jp.add(cb2);
				nRet = JOptionPane.showConfirmDialog(m_parent, jp, TIImageTool.langstr("FileSystemCheck"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				if (nRet == JOptionPane.CANCEL_OPTION) {
					return;
				}
				if (nRet == JOptionPane.YES_OPTION) {
					try {
						bErrors = true;
						ImageCheck.findCRCErrors(vol, true, cb2.isSelected());
					}
					catch (IOException iox) {
						iox.printStackTrace();
					}
				}			
			}
			else {
				ps.println(String.format(TIImageTool.langstr("FoundCRC"), 0));
			}
		}
		
		/**************************************************************   
		            Done with the check. 
		 **************************************************************/
		if (bChangedSector0 || bChangedAlloc || changedL3) {
			nRet = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("CommitCheck"), TIImageTool.langstr("FileSystemCheck"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (nRet == JOptionPane.OK_OPTION) {
//				if (vol.isFloppyImage()) {
					try {
						vol.reopenForWrite();
						if (bChangedSector0) vol.update();
						if (bChangedAlloc) vol.saveAllocationMap();
						if (changedL3) {
							// Write all affected sectors
							for (TFile f : fixL3) {
								ps.println(String.format(TIImageTool.langstr("SwappedL3"), f.getPathname()));
								f.rewriteFIB();
							}
						}
						vol.reopenForRead();
					}
					catch (FileNotFoundException fnfx) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImageFWP"), TIImageTool.langstr("WriteError"), JOptionPane.ERROR_MESSAGE); 
						return;
					}
					catch (IOException iox) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("WriteError"), JOptionPane.ERROR_MESSAGE);
						return;
					}
					catch (ImageException ix) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("WriteError"), JOptionPane.ERROR_MESSAGE);
						return;
					}
					catch (ProtectedException px) {
						JOptionPane.showMessageDialog(m_parent, TIImageTool.langstr("VolumeWP") + ": " + px.getMessage(), TIImageTool.langstr("WriteError"), JOptionPane.ERROR_MESSAGE);
						return;
					}
//				}
			}
			imagetool.showTextContent(TIImageTool.langstr("Results"), baos.toString());
		}
		else {
			if (!bErrors) {
				nRet = JOptionPane.showConfirmDialog(m_parent, TIImageTool.langstr("NoErrors"), TIImageTool.langstr("FileSystemCheck"), JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
				if (nRet == JOptionPane.YES_OPTION) {
					imagetool.showTextContent(TIImageTool.langstr("Results"), baos.toString());  			
				}
			}
			else
				imagetool.showTextContent(TIImageTool.langstr("Results"), baos.toString());
		}
		
		imagetool.refreshPanel(vol);			
	}
}
