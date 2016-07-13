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
package de.mizapf.timt;
import de.mizapf.timt.files.*;
import de.mizapf.timt.basic.BasicLine;
import java.util.*;
import java.io.*;

public class CommandShell {

	public static void main(String[] arg) {
		if (arg.length < 1) {
			System.out.println("Usage: CommandShell <command> <arguments>");
			System.out.println("Enter -h as command to get help");
			return;
		}

		if (arg[0].equals("-h")) {
			try {
				InputStream is = CommandShell.class.getResourceAsStream("command.txt");
				if (is==null) {
					System.err.println("Could not find help text in jar file.");
					return;
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				while (br.ready()) {
					System.out.println(br.readLine());
				}
			}
			catch (IOException iox) {
				System.err.println("Could not read help text.");
			}
			return;
		}
		byte[] abyFileContent = null;
		
		CommandShell com = new CommandShell();
		try {
			if (arg[0].equals("dir") || arg[0].equals("ls") || arg[0].equals("lsf")) {
				String sSubdir = null;
				if (arg.length<2) {
					System.err.println("Missing arguments for dir/ls/lsf. Use -h for help.");
					return;
				}
				if (arg.length>2) {
					sSubdir = arg[2];
				}
				try {
					System.out.println(com.directory(arg[1], sSubdir, arg[0]));
				}
				catch (ImageException ix) {
					if (ix.getMessage().equals(".HEAD")) {
						System.err.println("Cannot open image file: Missing floppy signature (DSK)");
					}
					else {
						System.err.println("Image error: " + ix.getMessage());						
					}
					return;
				}
				catch (FileNotFoundException fnfx) {
					System.err.println("File not found: " + fnfx.getMessage());
					return;
				} 
				catch (IOException iox) {
					System.err.println("Error getting file: " + iox.getMessage());
					iox.printStackTrace();
					return;
				}
			}
			if (arg[0].equals("type")) {
				if (arg.length < 3) {
					System.err.println("Missing arguments for type");
					return;
				}
				System.out.println(com.type(arg[1], arg[2]));
			}
			if (arg[0].equals("list")) {
				if (arg.length < 3) {
					System.err.println("Missing arguments for list");
					return;
				}
				System.out.println(com.list(arg[1], arg[2]));
			}
		}
		catch (ImageException ix) {
			System.err.println("Image error: " + ix.getMessage());
		}
		catch (FormatException fx) {
			System.err.println("Format error: " + fx.getMessage());
		}
		catch (FileNotFoundException fnfx) {
			System.err.println("File not found: " + fnfx.getMessage());
		}
		catch (IOException iox) {
			iox.printStackTrace();
			System.err.println("Error getting file: " + iox.getMessage());
		}
	}
		
	CommandShell() {
	}
	
	private static Directory descendToDirectory(Volume image, String[] aSubdir, boolean bDir) throws FileNotFoundException {
		// We need to descent to the given directory
		Directory dirCurrent = image.getRootDirectory();
		if (aSubdir == null) return dirCurrent;
		int nPathLength = aSubdir.length;
		
		if (!bDir) nPathLength--;
		
		for (int i=0; i < nPathLength; i++) {
			Directory[] aDir = dirCurrent.getDirectories();
			boolean bFound = false;
			for (int j=0; j < aDir.length; j++) {
				if (aDir[j].getName().equals(aSubdir[i])) {
					dirCurrent = aDir[j];
					bFound = true;
					break;
				}
			}
			// Test for Archive
			TFile[] aFile = dirCurrent.getFiles();
			for (int j=0; j < aFile.length; j++) {
				if (aFile[j].getName().equals(aSubdir[i]) && aFile[j].hasArchiveFormat()) {
					try {
						Archive ark = aFile[j].unpackArchive();
						dirCurrent = ark;
						bFound = true;
					}
					catch (IllegalOperationException iox) {
						System.err.println("Illegal operation: " + iox.getMessage());
					}
					catch (FormatException fx) {
						System.err.println("Error while unpacking " + aFile[j].getPathname());
					}		
					catch (IOException iox) {
						System.err.println("Error while unpacking " + aFile[j].getPathname());
					}
					catch (ImageException ix) {
						System.err.println("Error while unpacking " + aFile[j].getPathname());
					}
				}
			}
			if (!bFound) throw new FileNotFoundException("Subdirectory " + aSubdir[i] + " not found");
		}
		return dirCurrent;
	}
	
	private static String buildString(String s, int nCount) {
		StringBuilder sb = new StringBuilder(s.length() * nCount);
		for (int i=0; i < nCount; i++) sb.append(s);
		return sb.toString();
	}

	private String[] getPath(String sSubdir) {
		String[] asArg = null;
		if (sSubdir!=null) asArg = sSubdir.split("\\x2e");
		else asArg = new String[0]; 
		List<String> dirPath = new ArrayList<String>();
		for (int i=0; i < asArg.length; i++) dirPath.add(asArg[i]);
		return dirPath.toArray(new String[dirPath.size()]);
	}
	
	public String directory(String sImagename, String sSubdir, String sCommand) throws FileNotFoundException, IOException, ImageException {
		Volume image = new Volume(sImagename);
		boolean bOnlyNames = false;
		boolean bDecorate = false;
		
		if (sCommand.equals("ls")) {
			bOnlyNames = true;
			bDecorate = false;
		}
		if (sCommand.equals("lsf")) {
			bOnlyNames = true;
			bDecorate = true;
		}
		
		StringBuffer sbDir = new StringBuffer();
		
		// We need to descent to the given directory
		String[] dirPath = getPath(sSubdir);
		Directory dirCurrent = descendToDirectory(image, dirPath, true);
		
		String sVolume = dirCurrent.getVolume().getDeviceName();
		
		StringBuilder sb = new StringBuilder();
		if (!bOnlyNames) {
			sb.append("Volume in ").append(sVolume).append(" is ");
			sb.append(image.getName());
			sb.append("\nDirectory of ").append(sVolume);
			if (sSubdir!=null) sb.append(".").append(sSubdir);
		}

		// String.format
		
		Directory[] aDir = dirCurrent.getDirectories();
		TFile[] aFile = dirCurrent.getFiles();
		
		if (!bOnlyNames) {			  
			if (aFile.length!=0 || aDir.length != 0) {
				sb.append("\n\nFilename   Size  Type   Length P F  Created              Updated");
				sb.append("\n").append(buildString("-",79));
			}
			else {
				sb.append("\nEmpty directory\n");
			}
			sb.append("\n");
		}
		
		if (bOnlyNames) {
			boolean bFirst = true;
			for (int i=0; i < aDir.length; i++) {
				if (!bFirst) sb.append("\n");
				bFirst = false;
				sb.append(aDir[i].getName());
				if (bDecorate) sb.append(".d");
			}
			for (int i=0; i < aFile.length; i++) {
				if (!bFirst) sb.append("\n");
				sb.append(aFile[i].getName());
				bFirst = false;
				if (bDecorate && aFile[i].isDisplay() && !aFile[i].hasFixedRecordLength() && aFile[i].getRecordLength()==80) sb.append(".t");
			}
			return sb.toString();
		}		   
		
		for (int i=0; i < aDir.length; i++) {
			sb.append(aDir[i].toFormattedString()).append("\n");
		}
		
		int nFileTotal = 0;
		for (int i=0; i < aFile.length; i++) {
			sb.append(aFile[i].toFormattedString()).append("\n");
			nFileTotal += aFile[i].getUsedSectors();
		}
		
		int nAlloc = image.getAllocatedSectorCount();
		
		int nSysAlloc = image.getSystemAllocatedSectors();
		
		int nDiffer = nAlloc - nSysAlloc - nFileTotal;
		
		sb.append("\n");
		if (nFileTotal > 0) {
			if (aFile.length != 0 || aDir.length != 0) sb.append(nFileTotal).append(" sectors used in ");
			if (aFile.length != 0) {
				sb.append(aFile.length).append(" files");
			}
		}
		if (aDir.length!=0) {
			if (aFile.length!=0) sb.append(" and ");
			sb.append(aDir.length).append(" directories");
		}
		
		if (aFile.length != 0 || aDir.length != 0) sb.append(",\n");

		if (nDiffer!=0) {
			sb.append(nDiffer + " sectors used not in this directory,\n");
		}

		sb.append(image.getTotalSectors() - nAlloc).append(" sectors free\n");
		return sb.toString();
	}	
	
	public String type(String sImagename, String sFilename) throws FileNotFoundException, IOException, ImageException, FormatException {
		Volume image = new Volume(sImagename);
		// We need to descent to the given directory
		String[] dirPath = getPath(sFilename);
		Directory dirCurrent = descendToDirectory(image, dirPath, false);
		TFile fl = dirCurrent.getFile(dirPath[dirPath.length-1]);
		return fl.getTextContent();		
	}
	
	// TODO: Add method to help.html	
	public String list(String sImagename, String sFilename) throws FileNotFoundException, IOException, ImageException, FormatException {
		Volume image = new Volume(sImagename);
		// We need to descent to the given directory
		String[] dirPath = getPath(sFilename);
		Directory dirCurrent = descendToDirectory(image, dirPath, false);
		TFile fl = dirCurrent.getFile(dirPath[dirPath.length-1]);
		if (fl.isBasicFile()) {
			return fl.listBasic(BasicLine.EX_BASIC);
		}
		else return "Not a BASIC program";		
	}
}
