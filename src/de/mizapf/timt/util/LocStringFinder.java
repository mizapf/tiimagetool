package de.mizapf.timt.util;

import java.io.*;
import java.util.*;

/** Checks all localization keys.

    Usage is: LocStringFinder <file> <locale>
    where 
    - the file is a path name to a Java file of TIImageTool like src/de/mizapf/timt/TIImageTool.java
    - the locale is a two-letter indicator (en, de,...) for the language
    
    Output is sent to the standard output channel in the form
    <Key>: (OK | FAIL | UNKNOWN)
    
    OK: The <key> is contained in the Strings_<locale>.properties file
    FAIL: The <key> is not contained in the Strings_<locale>.properties file
    UNKNOWN: The contents of "langstr(...)" is not a simple "<key>" but another
    construct like "read? <key1> : <key2>". This must be checked manually.
    
    The class should be executed in a script or as an argument to the "find" 
    command, and the output should be collected in a summary file.
    
    Example in Linux: 
    find src/ -name "*.java" -exec java -classpath dist/lib/tiimagetool.jar \
        de.mizapf.timt.util.LocStringFinder {} de \; > out.txt

*/
public class LocStringFinder {

	Properties m_propNames;

	public static void main(String[] arg) {
		if (arg.length < 2) {
			System.err.println("Usage: LocStringFinder <file> <locale>");
			return;
		}
		LocStringFinder lsf = new LocStringFinder(arg[1]);
		lsf.go(arg[0]);
	}
		
	LocStringFinder(String loc) {
		m_propNames = new Properties();
		try {
			String propFile = "Strings_" + loc + ".properties";
			m_propNames.load(de.mizapf.timt.ui.ToolDialog.class.getResourceAsStream(propFile));
		}
		catch (IOException iox) {
			iox.printStackTrace();
		}		
	}
	
	public void go(String file) {
		String search = "Langstr(".toLowerCase();
		try {
			FileReader fr = new FileReader(file);
			StringBuilder line = new StringBuilder();
			StringBuilder word = new StringBuilder();
			int ch;
			int state = 0;
			while ((ch=fr.read())!=-1) {
				line.append((char)ch);
				switch (state) {
				case 0:
					if (ch=='\n') {
						line = new StringBuilder();
					}
					else {
						int pos = line.indexOf(search);
						if (pos != -1) 	state = 1;
					}
					break;
				case 1:
					if (ch==')') state = 3;
					else
						word.append((char)ch);
					break;
				case 3:
//					System.out.println(word.toString());
					check(word.toString());
					word = new StringBuilder();
					line = new StringBuilder();
					state = 0;
					break;
				}
			}
			fr.close();
		}
		catch (FileNotFoundException fnfx) {
			System.err.println("File not found: " + file);
		}
		catch (IOException iox) {
			System.err.println("Could not read file: " + file);
		}
	}
	
	private void check(String name) {
		String chName = name.substring(1, name.length()-1);
		if (name.charAt(0)=='"') {
			if (m_propNames.containsKey(chName)) {
				System.out.println(chName + ": OK");
			}
			else {
				System.out.println(chName + ": FAIL");
			}
		}
		else {
			System.out.println(chName + ": UNKNOWN");
		}
	}
}
