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
import de.mizapf.timt.util.*;
import de.mizapf.timt.ui.Settings;
import java.util.*;
import java.io.*;

public class CommandShell {

	Settings m_Settings;
	
	public static void main(String[] arg) {
		TIImageTool.localize();
		if (arg.length < 1) {
			System.out.println(TIImageTool.langstr("CommandUsage"));
			return;
		}

		if (arg[0].equals("-h")) {
			Locale loc = TIImageTool.getSysLocale();
			try {
				InputStream is = CommandShell.class.getResourceAsStream("/de/mizapf/timt/ui/command_" + loc.getLanguage() + ".txt");
				if (is==null) {
					System.err.println(TIImageTool.langstr("CommandNoHelp"));
					is = CommandShell.class.getResourceAsStream("/de/mizapf/timt/ui/command_en.txt");
					if (is==null) {
						System.err.println("Error: No language file for CommandShell.");
						return;
					}
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				while (br.ready()) {
					System.out.println(br.readLine());
				}
			}
			catch (IOException iox) {
				System.err.println(TIImageTool.langstr("CommandHelpError"));
			}
			return;
		}
		byte[] abyFileContent = null;
		
		CommandShell com = new CommandShell();
		try {
			if (arg[0].equals("dir") || arg[0].equals("ls") || arg[0].equals("lsf")) {
				String sSubdir = null;
				if (arg.length<2) {
					System.err.println(TIImageTool.langstr("CommandMissArg"));
					return;
				}
				if (arg.length>2) {
					sSubdir = arg[2];
				}
				try {
					System.out.println(com.directory(arg[1], sSubdir, arg[0]));
				}
				catch (MissingHeaderException mx) {
					System.err.println(TIImageTool.langstr("CommandMissHeader"));
					return;
				}
				catch (ImageException ix) {
					System.err.println(TIImageTool.langstr("ImageError") + ": " + ix.getMessage());						
					return;
				}
				catch (FileNotFoundException fnfx) {
					System.err.println(TIImageTool.langstr("FileNotFound") + ": " + fnfx.getMessage());
					return;
				} 
				catch (IOException iox) {
					System.err.println(TIImageTool.langstr("IOError") + ": " + iox.getClass().getName());
					iox.printStackTrace();
					return;
				}
			}
			if (arg[0].equals("type")) {
				if (arg.length < 3) {
					System.err.println(TIImageTool.langstr("CommandMissArg"));
					return;
				}
				System.out.println(com.type(arg[1], arg[2]));
			}
			if (arg[0].equals("list")) {
				if (arg.length < 3) {
					System.err.println(TIImageTool.langstr("CommandMissArg"));
					return;
				}
				System.out.println(com.list(arg[1], arg[2]));
			}
			if (arg[0].equals("export")) {
				String sSubdir = null;
				if (arg.length<2) {
					System.err.println(TIImageTool.langstr("CommandMissArg"));
					return;
				}
				if (arg.length>2) {
					sSubdir = arg[2];
				}
				try {
					com.export(arg[1], sSubdir);
				}
				catch (ImageException ix) {
					System.err.println(TIImageTool.langstr("ImageError") + ": " + ix.getMessage());						
					return;
				}
				catch (FileNotFoundException fnfx) {
					System.err.println(TIImageTool.langstr("FileNotFound") + ": " + fnfx.getMessage());
					return;
				} 
				catch (IOException iox) {
					System.err.println(TIImageTool.langstr("IOError") + ": " + iox.getClass().getName());
					iox.printStackTrace();
					return;
				}
			}
			if (arg[0].equals("import")) {
				String sSubdir = null;
				if (arg.length<2) {
					System.err.println(TIImageTool.langstr("CommandMissArg"));
					return;
				}
				if (arg.length>2) {
					sSubdir = arg[2];
				}
				try {
					com.importFile(arg[1], sSubdir);
				}
				catch (ImageException ix) {
					System.err.println(TIImageTool.langstr("ImageError") + ": " + ix.getMessage());						
					return;
				}
				catch (FileNotFoundException fnfx) {
					System.err.println(TIImageTool.langstr("FileNotFound") + ": " + fnfx.getMessage());
					return;
				} 
				catch (IOException iox) {
					System.err.println(TIImageTool.langstr("IOError") + ": " + iox.getClass().getName());
					iox.printStackTrace();
					return;
				}
			}
		}
		catch (ImageException ix) {
			System.err.println(TIImageTool.langstr("ImageError") + ": " + ix.getMessage());						
		}
		catch (FormatException fx) {
			System.err.println(TIImageTool.langstr("Error") + ": " + fx.getMessage());
		}
		catch (FileNotFoundException fnfx) {
			System.err.println(TIImageTool.langstr("FileNotFound") + ": " + fnfx.getMessage());
		} 
		catch (IOException iox) {
			System.err.println(TIImageTool.langstr("IOError") + ": " + iox.getClass().getName());
			iox.printStackTrace();
		}
		catch (NotImplementedException ix) {
			System.err.println(String.format(TIImageTool.langstr("Error.NotImplemented"), ix.getMessage())); 
		}
		catch (IllegalArgumentException iax) {
			System.err.println(iax.getMessage());
		}
	}
		
	CommandShell() {
		// SectorCache.setGen(0);
		m_Settings = new Settings(TIImageTool.getOperatingSystem());
		ImageFormat.setSettings(m_Settings);
	}
	
	private static Directory descendToDirectory(Volume image, String[] aSubdir, boolean bDir) throws FileNotFoundException {
		// We need to descent to the given directory
		if (image == null) return null;
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
						System.err.println(iox.getMessage());
					}
					catch (FormatException fx) {
						System.err.println(TIImageTool.langstr("UnpackError") + ": " + aFile[j].getPathname());
					}		
					catch (IOException iox) {
						System.err.println(TIImageTool.langstr("UnpackError") + ": " + aFile[j].getPathname());
					}
					catch (ImageException ix) {
						System.err.println(TIImageTool.langstr("UnpackError") + ": "+  aFile[j].getPathname());
					}
				}
			}
			if (!bFound) throw new FileNotFoundException(String.format(TIImageTool.langstr("VolumeDirNotFound"), aSubdir[i]));
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
	
	private ImageFormat getImage(String sFile) throws FileNotFoundException, IOException, ImageException {
		ImageFormat image = ImageFormat.getImageFormat(sFile);
		return image;
	}
	
	private Volume openImage(String sAbsFile) throws FileNotFoundException, IOException, ImageException {
		// Get partition/volume number
		int nHashPos = sAbsFile.lastIndexOf("#");
		int nVolumeNumber = 0;
		if (nHashPos != -1 && nHashPos < sAbsFile.length()-1) {
			String sPart = sAbsFile.substring(nHashPos+1);
			try {
				nVolumeNumber = Integer.parseInt(sPart);
			}
			catch (NumberFormatException nfx) {
				nVolumeNumber = -1;
			}
			sAbsFile = sAbsFile.substring(0, nHashPos);
		}
		
		// ============== Open the image	
		FileImageFormat image = (FileImageFormat)ImageFormat.getImageFormat(sAbsFile); // throws ImageExc if unknown
								
		byte[] vibmap = null;
		TFileSystem fs = null;	
		Volume vol = null;
				
		if (image instanceof FloppyImageFormat) {
			vibmap = image.readSector(0).getData();			
			fs = ((FloppyImageFormat)image).getFileSystem(vibmap);
			fs.setupAllocationMap(vibmap);
		}
				
		if (image.isPartitioned()) {
			// need the partition number
			if (nVolumeNumber < 0) {
				throw new IllegalArgumentException(TIImageTool.langstr("Error.InvPartVolumeNumber"));
			}
			if (nVolumeNumber == 0) {
				throw new IllegalArgumentException(TIImageTool.langstr("Error.NoPartVolumeNumber"));
			}
			image.setPartition(nVolumeNumber-1);
			
			if (image instanceof CF7ImageFormat) {
				vibmap = image.readSector(0).getData();
				// int check = CF7bla.checkFormat(vibmap);
				// TODO: Add check
				
				fs = ((CF7ImageFormat)image).getFileSystem(vibmap);
				fs.setupAllocationMap(vibmap);
			}
		}	
		else {
			if (nVolumeNumber != 0) {
				throw new IllegalArgumentException(TIImageTool.langstr("Error.InvPartVolumeNumber"));
			}
		}
				
		// image may have been replaced by the selected partition
		if (image instanceof HarddiskImageFormat) {
			HarddiskImageFormat hif = (HarddiskImageFormat)image;
			
			vibmap = image.readSector(0).getData();						
			fs = hif.getFileSystem(vibmap);
			// Allocation map skips the first SECTOR_LENGTH bytes
			fs.setupAllocationMap(image.getContent(0, 31));
		}
		try {
			vol = new Volume(image, fs);					
		}
		catch (ImageException ix) {
			System.err.println(ix.getMessage());
			return null;
		}
		
		if (vol.isReadOnly()) {
			System.err.println(TIImageTool.langstr("ImageFWP"));
		}
		
		Directory root = vol.getRootDirectory();	
		image.setCheckpoint();      // req
		vol.nextGeneration(true);   // req

		return vol;
	}
	
	public String directory(String sImagename, String sSubdir, String sCommand) throws FileNotFoundException, IOException, ImageException {
		Volume image = openImage(sImagename);
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
		if (dirCurrent == null) return null;

		Volume vol = dirCurrent.getVolume();
		
		StringBuilder sb = new StringBuilder();
		if (!bOnlyNames) {
			// First line
			if (vol.getName().trim().length()==0)
				sb.append(String.format(TIImageTool.langstr("PanelVolumeUnnamed"), vol.getDeviceName())); 
			else 
				sb.append(String.format(TIImageTool.langstr("PanelVolumeNamed"), vol.getDeviceName(), vol.getName()));
			
			if (vol.isFloppyImage()) {
				sb.append(", ");
				sb.append(String.format(TIImageTool.langstr("PanelFloppyParams"), vol.getFloppyFormatName(), vol.getTracksPerSide()));
			}
			sb.append(", ");
			sb.append(String.format(TIImageTool.langstr("PanelParams"), vol.getTotalSectors()));
			if (vol.getAUSize()!=1) {
				sb.append(", ");
				sb.append(String.format(TIImageTool.langstr("PanelAU"), vol.getAUSize()));
			}
			if (vol.isProtected()) {
				sb.append(" ");
				sb.append(TIImageTool.langstr("PanelProt"));
			}
			sb.append(", ");
			sb.append(vol.dumpFormat());
			sb.append("\n");
			sb.append(String.format(TIImageTool.langstr("PanelDir"), vol.getDeviceName(), dirCurrent.getFullPathname()));
		}
				
		// String.format
		
		Directory[] aDir = dirCurrent.getDirectories();
		TFile[] aFile = dirCurrent.getFiles();
		
		if (!bOnlyNames) {			  
			if (aFile.length!=0 || aDir.length != 0) {
				sb.append("\n\n").append(TIImageTool.langstr("CommandDirHead"));
				sb.append("\n").append(buildString("-",79));
			}
			else {
				sb.append("\n").append(TIImageTool.langstr("CommandDirEmpty")).append("\n");
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
		
		int nFileTotal = 0;

		// DDIRs of subdirs
		for (int i=0; i < aDir.length; i++) {
			nFileTotal += aDir[i].getAllRequiredSectors(vol.getAUSize());
			sb.append(aDir[i].toFormattedString()).append("\n");
		}

		for (int i=0; i < aFile.length; i++) {
			nFileTotal += aFile[i].getAllRequiredSectors(vol.getAUSize());
			sb.append(aFile[i].toFormattedString()).append("\n");
		}
			
		int nDiffer = (vol.getAllocatedSectorCount() - vol.getSysAllocated()) - nFileTotal;
		
		sb.append("\n");
		if (aFile.length > 0 || aDir.length > 0) {
			sb.append(String.format(TIImageTool.langstr("CommandDirSummary1"), vol.getAllocatedSectorCount(), aFile.length));
		}
		if (aDir.length!=0) {
			sb.append(" ").append(String.format(TIImageTool.langstr("CommandDirSummary2"), aDir.length));
		}
		
		if (aFile.length != 0 || aDir.length != 0) sb.append(",\n");

		if (nDiffer!=0) {
			sb.append(String.format(TIImageTool.langstr("PanelInThisDir"), nFileTotal));
			sb.append(",\n");
		}

		sb.append(String.format(TIImageTool.langstr("CommandDirSummary4"), image.getTotalSectors() - image.getAllocatedSectorCount()));
		return sb.toString();
	}	
	
	public String type(String sImagename, String sFilename) throws FileNotFoundException, IOException, ImageException, FormatException {
		Volume image = openImage(sImagename);
		// We need to descent to the given directory
		String[] dirPath = getPath(sFilename);
		Directory dirCurrent = descendToDirectory(image, dirPath, false);
		TFile fl = dirCurrent.getFile(dirPath[dirPath.length-1]);
		if (fl == null) throw new FileNotFoundException(dirPath[dirPath.length-1]);
		return fl.getTextContent();		
	}
	
	public String list(String sImagename, String sFilename) throws FileNotFoundException, IOException, ImageException, FormatException {
		Volume image = openImage(sImagename);
		// We need to descent to the given directory
		String[] dirPath = getPath(sFilename);
		Directory dirCurrent = descendToDirectory(image, dirPath, false);
		TFile fl = dirCurrent.getFile(dirPath[dirPath.length-1]);
		if (fl == null) throw new FileNotFoundException(dirPath[dirPath.length-1]);
		
		if (fl.isBasicFile()) {
			return fl.listBasic(BasicLine.EX_BASIC, "§%");
		}
		else return TIImageTool.langstr("CommandListNotBasic");		
	}

	private String transliterate(String sExpFile, String sSubstSrc, String sSubstTar) {
		StringBuilder sbNewFile = new StringBuilder();
		for (int i=0; i < sExpFile.length(); i++) {
			char ch = sExpFile.charAt(i);
			if (ch>32 && ch<127) {
				int nPos = sSubstSrc.indexOf(ch);
				if (nPos != -1) {
					char chNew = sSubstTar.charAt(nPos);
					if (chNew != '?') sbNewFile.append(sSubstTar.charAt(nPos));
				}
				else sbNewFile.append(ch);
			}
			else {
				String hex = Utilities.toHex((int)ch, 2, true);
				sbNewFile.append('=').append(hex);
			}
		}
		return sbNewFile.toString();
	}
	
	public void export(String sImagename, String sDirname) throws FileNotFoundException, IOException, ImageException, FormatException {
		Volume image = openImage(sImagename);
		// We need to descent to the given directory
		String[] dirPath = getPath(sDirname);
		Directory dirCurrent = descendToDirectory(image, dirPath, true);
		
		String fromList = "/\\*><:";
		String toList = "__x___";
		
		for (TFile file : dirCurrent.getFiles()) {
			
			String sExpFile = file.getName();
			
			byte[] abyCont = null;
			TIFiles tif = TIFiles.createFromFile(file);
			abyCont = tif.toByteArray();
			
			// Transform the file name
			sExpFile = transliterate(sExpFile, "_", ".");
			sExpFile = transliterate(sExpFile, fromList, toList);
			sExpFile = sExpFile.toLowerCase() + ".tfi";
			
			// Create full pathname for exporting
			java.io.File iofSave = new java.io.File(sExpFile);

			// Save the file
			FileOutputStream fos = new FileOutputStream(iofSave);
			fos.write(abyCont);
			fos.close();
		}		
	}
		
	public void importFile(String sImagename, String sDirname) throws FileNotFoundException, IOException, ImageException, FormatException {
		Volume image = openImage(sImagename);
		// We need to descent to the given directory
		String[] dirPath = getPath(sDirname);
		Directory dirCurrent = descendToDirectory(image, dirPath, true);
		File iofDirectory = new File(".");
		java.io.File[] aiof = iofDirectory.listFiles();
	
		for (java.io.File iofile : aiof) {
			if (!iofile.isDirectory()) {
				if (!iofile.getName().endsWith("meta.inf")) {
					FileInputStream fis = new FileInputStream(iofile);
					DataInputStream dis = new DataInputStream(fis);
					byte[] abyTif = new byte[dis.available()];
					dis.readFully(abyTif);
					dis.close();
					fis.close();

					try {
						String sName = TIFiles.getName(abyTif);
						// Is this a TIFILES file?
						if (TIFiles.hasHeader(abyTif)) {
							dirCurrent.insertFile(abyTif, sName, false);
						}
						else {
							System.err.println(iofile.getName() + " not a TIFILES file");
						}
					}
					catch (ProtectedException px) {
						System.err.println("Image is write protected");
					}
					catch (InvalidNameException ix) {
						System.err.println(iofile.getName() + ": invalid name");
					}
					catch (FileExistsException fx) {
						System.err.println(iofile.getName() + " already exists");
					}
				}
			}
		}
		try {
			dirCurrent.commit(true);
			image.saveImage();
		}
		catch (ProtectedException px) {
			System.err.println("Image is write protected");
		}
	}
}
