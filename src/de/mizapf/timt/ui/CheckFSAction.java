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

public class CheckFSAction extends Activity {

	public String getMenuName() {
		return "Check filesystem";
	}
	
	public String getActionName() {
		return "CHECK";
	}
	
	public void go() {
		int nRet = 0;
		AllocationMap allocMap = null;
		boolean bChangedAlloc = false;
		boolean bErrors = false;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		PrintStream ps = new PrintStream(baos);
		
		DirectoryView dv = imagetool.getSelectedView();
		Volume vol = dv.getVolume();
		JCheckBox cb = null;
				
		/* Part 1: Check underallocation */
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		int savedResp = -1;		
		ps.println("===========  Checking underallocation ===========\n");
		allocMap = vol.getAllocationMap();
		ArrayList<AllocationGapList> broken = new ArrayList<AllocationGapList>();

		// Do the check
		ImageCheck.checkUnderAllocationInDir(vol, vol.getRootDirectory(), allocMap, broken, "", ps);

		// Now report
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));			
		if (broken.size()>0) bErrors = true;
		else ps.println("No underallocation errors.\n");

		for (AllocationGapList agl:broken) {

			StringBuilder sbMsg = new StringBuilder();
			sbMsg.append("<html>");
			sbMsg.append("The file <span style=\"color:blue\">").append(agl.getName()).append("</span> extends over the following unallocated locations:<br><br>");
			ps.println("File " + agl.getName() + " extends over the following unallocated locations:\n");
			int[] nAU = agl.getAllocationGaps();
			for (int n=0; n < nAU.length-1; n++) {
				sbMsg.append(Integer.valueOf(nAU[n])).append(", ");
				ps.print(Integer.valueOf(nAU[n]) + ", ");
				if ((n % 10==9) && (n!=nAU.length-2)) sbMsg.append("<br>");
			}
			sbMsg.append(Integer.valueOf(nAU[nAU.length-1])).append(".<br>");
			ps.println(Integer.valueOf(nAU[nAU.length-1]));
			sbMsg.append("<br>This may lead to data loss or file corruption when new files are stored.");
			sbMsg.append("<br><br>Allocated these AUs for this file?");
			sbMsg.append("</html>");

			if (savedResp == -1) {
				JPanel jp = new JPanel();		
				JLabel jl = new JLabel(sbMsg.toString());
				
				cb = new JCheckBox("Repeat my decision for all AUs to be allocated");
				jp.add(cb);
				nRet = JOptionPane.showConfirmDialog(m_parent, jp, "File system check", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
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
		ps.println("===========  Checking overallocation ===========\n");

		ArrayList<AllocationDomain> alloc = new ArrayList<AllocationDomain>();

		// Do the check
		ImageCheck.findAllocationFaults(vol, allocMap, alloc, ps);
		
		// Now report
		savedResp = -1;
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));			
		if (alloc.size()>0) bErrors = true;
		else ps.println("No overallocation errors.\n");
		
		for (AllocationDomain ad:alloc) {
			if (ad.isUnassigned()) {
				ps.println("AU " + ad.getAU() + " is not assigned to any reachable file or directory.");
				
				if (savedResp == -1) {
					StringBuilder sbMsg = new StringBuilder();
					sbMsg.append("<html>");				
					sbMsg.append("The allocation unit <span style=\"color:blue\">").append(ad.getAU()).append("</span> is not assigned to any reachable file or directory.");
					sbMsg.append("<br>Free this allocation unit?");
					sbMsg.append("</html>");
				
					JPanel jp = new JPanel();
					jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
					JLabel jl = new JLabel(sbMsg.toString());
					jp.add(jl);
					cb = new JCheckBox("Repeat my decision for all AUs to be freed");
					jp.add(cb);
					nRet = JOptionPane.showConfirmDialog(m_parent, jp, "File system check", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
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
				sbMsg.append("<html>");				
				sbMsg.append("The allocation unit <span style=\"color:blue\">").append(ad.getAU()).append("</span> is allocated to the following file system objects:<br>");

				ps.println("AU " + ad.getAU() + " is allocated to the following file system objects:\n");

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
					
					sbMsg.append("<br>You should make copies of those files. Check whether they are corrupt or can be repaired.<br>");
					sbMsg.append("Then you must manually delete these files from this image. No action can be taken at this point.");
					sbMsg.append("<br>Continue this test?");
					sbMsg.append("</html>");
				
					JPanel jp = new JPanel();
					jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
					JLabel jl = new JLabel(sbMsg.toString());
					jp.add(jl);
					nRet = JOptionPane.showConfirmDialog(m_parent, jp, "File system check", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
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
		ps.println("===========  Checking broken sectors ===========\n");
		savedResp = -1;
		
		ArrayList<SectorFaultList> brokensect = new ArrayList<SectorFaultList>();
		ImageCheck.findBrokenSectors(vol.getRootDirectory(), brokensect, "", ps);
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));			
		if (brokensect.size()>0) {
			bErrors = true;
		}
		else {
			ps.println("No sector data errors.\n");
		}
			
		for (SectorFaultList sfl:brokensect) {
			StringBuilder sbMsg = new StringBuilder();
			sbMsg.append("<html>");
			sbMsg.append("The file <span style=\"color:blue\">" + sfl.getName() + "</span> contains sectors that are likely to be broken:<br><br>"); 

			ps.println("The file " + sfl.getName() + " contains sectors that are likely to be broken:");
			
			int[] anSect = sfl.getFaultySectors();
			if (sfl.getProblem()!=null) {
				sbMsg.append(sfl.getProblem());
				ps.println(sfl.getProblem());
			}
			else {
				int n = 0;
				boolean first = true;
				for (n=0; n < anSect.length && n < 100; n++) {
					if (!first) sbMsg.append(", ");
					first = false;
					if (n % 10==9) sbMsg.append("<br>");
					sbMsg.append(Integer.valueOf(anSect[n]));
				}
				if (n !=  anSect.length) { 
					sbMsg.append(" ... (" + (anSect.length-101) + " omitted) ... ");
				}

				for (n=0; n < anSect.length; n++) {
					ps.println(Integer.valueOf(anSect[n]));
				}

				sbMsg.append(".<br>");				
				sbMsg.append("<br>These sectors have been found to be filled with E5E5, DEAD, or D7A5.<br>");
				sbMsg.append("The values may indicate that the original contents were lost.<br>");
				sbMsg.append("You should verify the integrity of these files. No action can be taken at this point.<br>");
			}

			if (savedResp == -1) {
				JPanel jp = new JPanel();
				jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
				jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
				sbMsg.append("<br>Continue this test?");
				sbMsg.append("</html>");
				JLabel jl = new JLabel(sbMsg.toString());
				jp.add(jl);
				nRet = JOptionPane.showConfirmDialog(m_parent, jp, "File system check", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				
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
		ps.println("===========  Checking L3 swap issue ===========\n");
		boolean changedL3 = false;
		savedResp = -1;

		ArrayList<TFile> brokenL3 = new ArrayList<TFile>(); 
		ImageCheck.checkL3(vol.getRootDirectory(), brokenL3);
		ArrayList<TFile> fixL3 = new ArrayList<TFile>();
		
		if (brokenL3.size()>0) bErrors = true;
		else {
			ps.println("No L3 swap errors.\n");
		}

		JCheckBox cb2 = null;
	
		for (TFile brL3file : brokenL3) {
			ps.println("The file " + brL3file.getPathname() + " has a swapped record count (L3).");
			if (savedResp == -1) {
				JPanel jp = new JPanel();
				jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
				StringBuilder sbMsg = new StringBuilder();
				sbMsg.append("<html>");
				sbMsg.append("The file <span style=\"color:blue\">" + brL3file.getPathname() + "</span> has a swapped record count (L3)."); 
				sbMsg.append("<br>This may have been caused by a bug in a floppy DSR.");
				sbMsg.append("<br>Fix this issue?<br><br>");
				sbMsg.append("</html>");
				
				JLabel jl = new JLabel(sbMsg.toString());
				jp.add(jl);
				cb2 = new JCheckBox("Repeat my decision for all L3 issues");
				jp.add(cb2);
				
				nRet = JOptionPane.showConfirmDialog(m_parent, jp, "File system check", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
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
			ps.println("===========  Checking CRC issues (TDF only) ===========\n");
			
			if (badcrc > 0) {
				ps.println("Found " + badcrc + " CRC errors");
				JPanel jp = new JPanel();
				jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
				StringBuilder sbMsg = new StringBuilder();
				sbMsg.append("<html>");
				sbMsg.append("The disk image has sectors with CRC errors in the header or data part.");
				sbMsg.append("<br>Total count: ").append(badcrc);
				sbMsg.append("<br><br>Fix this issue?<br><br>");
				sbMsg.append("</html>");
				JLabel jl = new JLabel(sbMsg.toString());
				jp.add(jl);
				cb2 = new JCheckBox("Reset the CRC fields to F7F7");
				jp.add(cb2);
				nRet = JOptionPane.showConfirmDialog(m_parent, jp, "File system check", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
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
				ps.println("No CRC errors.");
			}
		}
		
		/**************************************************************   
		            Done with the check. 
		 **************************************************************/
		if (bChangedAlloc || changedL3) {
			nRet = JOptionPane.showConfirmDialog(m_parent, "Commit the changes to the image now?", "File system check", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (nRet == JOptionPane.OK_OPTION) {
//				if (vol.isFloppyImage()) {
					try {
						vol.reopenForWrite();
						if (bChangedAlloc) vol.saveAllocationMap();
						if (changedL3) {
							// Write all affected sectors
							for (TFile f : fixL3) {
								ps.println("Swapped L3 count in file " + f.getPathname());
								f.rewriteFIB();
							}
						}
						vol.reopenForRead();
					}
					catch (FileNotFoundException fnfx) {
						JOptionPane.showMessageDialog(m_parent, "Image file is write-protected.", "Write error", JOptionPane.ERROR_MESSAGE); 
						return;
					}
					catch (IOException iox) {
						JOptionPane.showMessageDialog(m_parent, "Image broken: " + iox.getClass().getName(), "Write error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					catch (ImageException ix) {
						JOptionPane.showMessageDialog(m_parent, "Image broken: " + ix.getMessage(), "Write error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					catch (ProtectedException px) {
						JOptionPane.showMessageDialog(m_parent, "Image protected: " + px.getMessage(), "Write error", JOptionPane.ERROR_MESSAGE);
						return;
					}
//				}
			}
			imagetool.showTextContent("Results", baos.toString());
		}
		else {
			if (!bErrors) {
				nRet = JOptionPane.showConfirmDialog(m_parent, "No errors found. Do you want to see a report?", "File system check", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
				if (nRet == JOptionPane.YES_OPTION) {
					imagetool.showTextContent("Results", baos.toString());  			
				}
			}
			else
				imagetool.showTextContent("Results", baos.toString());
		}
		
		imagetool.refreshPanel(vol);			
	}
}
