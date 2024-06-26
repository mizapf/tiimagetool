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
package de.mizapf.timt.util;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.EOFException;
import java.util.*;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.PrintStream;
import de.mizapf.timt.files.*;
import de.mizapf.timt.util.Utilities;

import de.mizapf.timt.TIImageTool;

public class ImageCheck {
	
	public static void checkUnderAllocationInDir(Volume image, Directory dir, AllocationMap map, ArrayList<AllocationGapList> broken, String dirName, PrintStream ps) {
		TFile[] aFile = dir.getFiles();
		// For each file in this directory
		
		for (int i=0; i < aFile.length; i++) {
			StringBuilder sbFullname = new StringBuilder();
			if (dirName.length()>0) sbFullname.append(dirName).append(".");
			sbFullname.append(aFile[i].getName());
			AllocationGapList agl = new AllocationGapList(sbFullname.toString());

			int[] afib = aFile[i].getFIBLocations();
			// Check whether all FIBs of this file are allocated
			for (int nFib : afib) {
				int nAuFib = nFib / map.getAUSize();
				ps.print(String.format(TIImageTool.langstr("SectorFIB"), nFib, sbFullname.toString()));
				if (!map.hasAllocated(nAuFib)) {
					agl.addAU(nAuFib);
					ps.println(":  " + String.format(TIImageTool.langstr("NotAllocatedFIB"), nFib));
				}
			}
			Interval[] ainv = aFile[i].getAllocatedBlocks();
			try {
				if ((ainv.length == 1) && (ainv[0].start == ainv[0].end)) {
					ps.print(", " + TIImageTool.langstr("Check.OccupyingS") + " " + ainv[0].start);
				}
				else {
					if (ainv.length > 0) {
						ps.print(", " + TIImageTool.langstr("Check.OccupyingI") + " ");
						boolean first = true;
						for (int j=0; j < ainv.length; j++) {
							if (!first) ps.print(", ");
							first = false;
							ps.print(ainv[j]);
							Integer[] aret = map.getUnallocatedLocations(ainv[j]);
							if (aret.length>0) {
								agl.setList(aret);
							}
						}
					}
					else ps.print(", " + TIImageTool.langstr("Check.OccupyingE"));
				}
				ps.print("\n");
			}
			catch (IndexOutOfBoundsException ix) {		
				ps.println(":  " + String.format(TIImageTool.langstr("InvalidCluster"), sbFullname.toString()));
			}
			if (agl.size()>0) broken.add(agl);
		}

		// For each subdirectory in this directory		
		Directory[] ad = dir.getDirectories();
		for (int i=0; i < ad.length; i++) {
			StringBuilder sbFullname = new StringBuilder();
			if (dirName.length()>0) sbFullname.append(dirName).append(".");
			sbFullname.append(ad[i].getName());
			// Check whether the DIB is allocated
			if (!map.hasAllocated(ad[i].getDDRSector() / map.getAUSize())) {
				AllocationGapList agl = new AllocationGapList(ad[i].getName());
				agl.addAU(ad[i].getDDRSector() / map.getAUSize());
				broken.add(agl);
			}
			else {
				ps.print(String.format(TIImageTool.langstr("SectorDDR"), ad[i].getDDRSector(), sbFullname.toString()));
				ps.print("\n");
			}
			// Recurse
			StringBuilder sbNew = new StringBuilder();
			if (dir.getName()!=null) sbNew.append(dir.getName()).append(".");
			sbNew.append(ad[i].getName());
			checkUnderAllocationInDir(image, ad[i], map, broken, sbNew.toString(), ps);
		}
	}

	public static boolean checkCF7Inconsistency(Volume vol, int[] geom) throws IOException, ImageException {
		Sector sector0 = vol.readSector(0);
		byte[] abySect0 = sector0.getData();
		geom[0] = Utilities.getInt16(abySect0, 0x0a);
		geom[1] = abySect0[0x12] & 0xff;  // heads
		geom[2] = abySect0[0x11] & 0xff;  // tracks
		geom[3] = abySect0[0x0c] & 0xff;  // sectors/track
		geom[4] = abySect0[0x13] & 0xff;  // density
		return (geom[0] != 1600) || (geom[1] != 2) || (geom[2] != 40) || (geom[3] != 20) || (geom[4] != 2);
	}
	
	public static void checkL3(Directory dir, ArrayList<TFile> fileList) {
		TFile[] aFile = dir.getFiles();
		// For each file in this directory
		
		for (int i=0; i < aFile.length; i++) {
			TFile f = aFile[i];
			if (f.hasSwappedL3Count() || f.hasBadL3Count()) {
				fileList.add(f);
			}
		}

		// For each subdirectory in this directory		
		Directory[] ad = dir.getDirectories();
		for (int i=0; i < ad.length; i++) {
			checkL3(ad[i], fileList);
		}
	}

	
	private static boolean checkFill(byte[] aby, int offset, int value) {
		boolean bBroken = true;
		for (int i=offset; i < offset + TFileSystem.SECTOR_LENGTH && bBroken; i+=2) {
			if (Utilities.getInt16(aby, i) != value) bBroken = false;
		}
		return bBroken;
	}
	
	public static void findBrokenSectors(Directory dir, ArrayList<SectorFaultList> broken, String dirName, PrintStream ps)  {
		TFile[] aFile = dir.getFiles();
		
		// For each file in this directory
		for (int i=0; i < aFile.length; i++) {
			StringBuilder sbFullname = new StringBuilder();
			
			if (dirName.length()>0) sbFullname.append(dirName).append(".");
			sbFullname.append(aFile[i].getName());
			
			SectorFaultList sfl = new SectorFaultList(sbFullname.toString()); 

			if (!TFile.validName(aFile[i].getName())) {
				sfl.setProblem(String.format(TIImageTool.langstr("InvalidEntry"), aFile[i].getName()));
			}
			else {
				try {
					byte[] abyFile = aFile[i].getSectorContent();
					for (int sect=0; sect < abyFile.length / TFileSystem.SECTOR_LENGTH; sect++) {
						
						boolean bBroken = true;
						bBroken = checkFill(abyFile, sect * TFileSystem.SECTOR_LENGTH, 0xdead) 
						|| checkFill(abyFile, sect * TFileSystem.SECTOR_LENGTH, 0xe5e5)
						|| checkFill(abyFile, sect * TFileSystem.SECTOR_LENGTH, 0xd7a5);
						
						if (bBroken) {
							// System.out.println(Utilities.hexdump(0, 0, abyFile, abyFile.length, false));
							sfl.addSector(aFile[i].getPhysicalSectorNumber(sect));
						}
					}
				}
				catch (EOFException ex) {
					if (TFile.validName(aFile[i].getName())) {
						sfl.setProblem(TIImageTool.langstr("DamagedPointer"));
					}
					else {
						sfl.setProblem(String.format(TIImageTool.langstr("InvalidEntry"), aFile[i].getName()));
					}
				}
				catch (IOException iox) {
					sfl.setProblem(iox.getClass() + ", " + iox.getMessage());
				}
				catch (FormatException fx) {
					sfl.setProblem(TIImageTool.langstr("FileNoContent"));
				}
				catch (ImageException ix) {
					sfl.setProblem(TIImageTool.langstr("CannotReadFile"));
					ix.printStackTrace();
				}
			}
			
			if (sfl.size()>0 || sfl.getProblem()!=null) broken.add(sfl);
		}

		// For each subdirectory in this directory		
		Directory[] ad = dir.getDirectories();
		for (int i=0; i < ad.length; i++) {
			// Recurse
			StringBuilder sbNew = new StringBuilder();
			if (dirName.length()>0) sbNew.append(dirName).append(".");
			sbNew.append(ad[i].getName());
			findBrokenSectors(ad[i], broken, sbNew.toString(), ps);
		}
	}
	
	/** Find out whether the given allocation unit is allocated to some file or directory. 
		Also determine how many entities are allocated to this AU.
	*/
	static void findEntity(int au, Directory dir, AllocationDomain alloc, String dirName, PrintStream ps) {
		TFile[] aFile = dir.getFiles();
		Volume image = dir.getVolume();
		int nAuSize = image.getAUSize();

		if (au * nAuSize == dir.getFileIndexSector()) {
			alloc.addEntity(dirName + ".#");
//			System.out.println("found as FDR sector: " + au);
		}
		
		int pos = 0;
		for (int i=0; i < aFile.length; i++) {
			StringBuilder sFullname = new StringBuilder();
			if (dirName.length()>0) sFullname.append(dirName).append(".");
			sFullname.append(aFile[i].getName());			
			
			for (int nFib : aFile[i].getFIBLocations()) {
				if (nFib / nAuSize == au) {
					alloc.addEntity(sFullname.toString() + ".#");
//					System.out.println("found as FIB sector: " + au);
				}
			}
			
			Interval[] ainv = aFile[i].getAllocatedBlocks();
			for (int j=0; j < ainv.length; j++) {
				if ((ainv[j].start <= au * nAuSize) && (au * nAuSize <= ainv[j].end)) {
					alloc.addEntity(sFullname.toString());
					ps.println(String.format(TIImageTool.langstr("AUBelongs"), au, sFullname.toString(), pos, j));
				}
			}
			pos++;
		}
		Directory[] ad = dir.getDirectories();

		for (int i=0; i < ad.length; i++) {
			if (ad[i].getDDRSector() / nAuSize==au) {
				alloc.addEntity(dir.getName() + "." + ad[i].getName() + ".#");
//				System.out.println("found as DIB sector: " + au);
			}
			if (dir == image.getRootDirectory()) 
				findEntity(au, ad[i], alloc, ad[i].getName(), ps);
			else
				findEntity(au, ad[i], alloc, dir.getName() + "." + ad[i].getName(), ps);
		}
	}
	
	public static void findAllocationFaults(Volume image, AllocationMap allocMap, ArrayList<AllocationDomain> list, PrintStream ps) {
		boolean bLittle = image.isFloppyImage() || image.isCF7Volume();
		int nMin = 2;
		if (image.isHarddiskImage()) nMin = 64;
//		System.out.println("total AUs = " + image.getVib().getTotalNumberOfAUs());
		for (int au=nMin; au < allocMap.getMaxAU(); au++) { 
			if (allocMap.hasAllocated(au)) {
				AllocationDomain alloc = new AllocationDomain(au);
				findEntity(au, image.getRootDirectory(), alloc, "", ps);
				if (alloc.isFaulty()) list.add(alloc);
			}
		}		
	}
	
	public static int findCRCErrors(Volume image, boolean fix, boolean reset) throws IOException {
		throw new NotImplementedException("findCRCErrors");
//		return image.checkCRC(fix, reset);
	}
}
