package de.mizapf.timt.util;

import java.io.*;
import java.util.*;

public class FileFinder {
	public static void main(String[] arg) {
//		try {
			FileFinder ff = new FileFinder(arg[1]);
			String path = ff.find(arg[0]);
			System.out.println(path);
//		}
//		catch (FileNotFoundException fnfx) {
//			fnfx.printStackTrace();
//		}
	}
	
	String[] m_bases = null;
	
	public FileFinder(String basePath) {
		m_bases = basePath.split(";");
	}
	
	public String find(String file) {
		for (String basepath : m_bases) {
			File base = new File(basepath);
			System.out.println("base = " + base.getAbsolutePath());
			File[] filesHere = base.listFiles();
			if (filesHere != null) {
				for (int i=0; i < filesHere.length; i++) {
					if (!filesHere[i].isDirectory()) {
						if (file.equals(filesHere[i].getName())) return filesHere[i].getAbsolutePath();
					}
					else {
						// Path path = FileSystems.getDefault().getPath(
						//if (Files.isSymbolicLink());
						if (!filesHere[i].getName().equals("X11")) { 
							FileFinder ff1 = new FileFinder(filesHere[i].getAbsolutePath());
							String path = ff1.find(file);
							if (path != null) return path;
						}
					}
				}
			}
		}
		return null;
	}
}

