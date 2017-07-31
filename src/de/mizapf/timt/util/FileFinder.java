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
	
	File m_base = null;
	
	FileFinder(String basePath) {
		m_base = new File(basePath);
	}
	
	String find(String file) {
		System.out.println("base = " + m_base.getAbsolutePath());
		File[] filesHere = m_base.listFiles();
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
		return null;
	}
}

