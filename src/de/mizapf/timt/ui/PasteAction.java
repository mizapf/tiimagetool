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

import java.awt.Cursor;
import java.util.Set;
import java.util.SortedSet;
import java.util.Iterator;
import java.util.List;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.awt.event.KeyEvent;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;

import de.mizapf.timt.files.*;
import de.mizapf.timt.util.TIFiles;
import de.mizapf.timt.TIImageTool;

public class PasteAction extends Activity {

	public int getKeyCode() {
		return KeyEvent.VK_V;
	}
	
 	public String getMenuName() {
		return TIImageTool.langstr("Paste");
	}
	
	public String getActionName() {
		return "PASTE";
	}
	
	public void go() {
		DirectoryView dvCurrent = imagetool.getSelectedView();

		Action paste = TransferHandler.getPasteAction();
		paste.actionPerformed(new ActionEvent(dvCurrent.getPanel().getLinkToJList(), ActionEvent.ACTION_PERFORMED, null));
		imagetool.markForCut(false);		
		// paste(dvCurrent);
	}	

	public void paste(DirectoryView dvCurrent) {
		paste(dvCurrent, dvCurrent.getDirectory());
	}
	
	@SuppressWarnings("unchecked")	// needed because of getTransferData
	public void paste(DirectoryView dvCurrent, Directory dirTarget) {
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		// System.out.println("Paste action");
		imagetool.setClipboardLoaded(false);
		
		DirectoryPanel dp = dvCurrent.getPanel();
		Volume volTarget = dvCurrent.getVolume();
		
		Transferable trans = imagetool.getClipboard();
		List<Element> list = null;
		
		try {
			list = (List<Element>)trans.getTransferData(DDData.elementCollection);
		}
		catch (UnsupportedFlavorException ufx) {
			System.err.println(TIImageTool.langstr("UnknownFlavor") + ": " + ufx);
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		catch (IOException iox) {
			iox.printStackTrace();
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}

		if (list.size()==0) {
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("PasteEmptyClipboard"), TIImageTool.langstr("InternalError"), JOptionPane.ERROR_MESSAGE); 
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}

		Iterator<Element> it = list.iterator();
		Element first = it.next();
		if (first.getName()==null) {
			// System.err.println("Empty paste");
			m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		
		if (first.getName().equals("..")) {
			if (!it.hasNext()) {
				JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("PasteNotParentLink"), TIImageTool.langstr("Error"), JOptionPane.ERROR_MESSAGE); 
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				return;				
			}
			first = it.next();
		}
		Directory dirSource = first.getContainingDirectory();
		
		Volume volSource = dirSource.getVolume();
		String sError = null;

		boolean bSameImage = (volSource.equals(volTarget));
		boolean bMove = dvCurrent.markedForCut();
		
		// Clear the cut flag (so that the files are not marked for cut after the paste) 
		imagetool.markForCut(false);

		if (bMove && bSameImage) {
			boolean bSourceArchive = dirSource instanceof Archive;
			boolean bTargetArchive = dirTarget instanceof Archive;
			boolean bArchive = bSourceArchive || bTargetArchive;
			
			// We will move the elements from the source to the target

			// if (sameDirectory) error(Cannot move in same directory)
			if (dirTarget.equals(dirSource)) {
				sError = TIImageTool.langstr("PasteNotToSelf");
			}

			// TODO: Replace sError by Exception
			if (sError == null) {
				// if (element is dir and target is a childDirectory) error(Cannot move parent into child)
				it = list.iterator();
				while (it.hasNext() && sError == null) {
					Element el = it.next();
					if (el instanceof Directory) {
						// Ignore the parent link
						if (((Directory)el).isParentLink()) continue;

						if (bArchive) {
							sError = TIImageTool.langstr("PasteNotDirArchive");
							break;
						}
						
						if (bSameImage && (volSource.isFloppyImage() || volSource.isCF7Volume())) {
							sError = TIImageTool.langstr("PasteNotDirSelf");
						}
						else {
							Directory dirStep = dirTarget;
							while (dirStep != null && sError == null) {
								if (dirStep.equals((Directory)el)) {
									sError = TIImageTool.langstr("PasteNotDirChild");
								}
								else dirStep = dirStep.getContainingDirectory();
							}
						}
					}
				}
			}

			if (sError == null) {
				// Moving in the same image
				boolean bReload = false;
				try {
					for (Element el : list) {
						// System.out.println("Element " + el);

						if (el instanceof TFile) {
							TFile file = (TFile)el;

							// First: move into target 
							// If that fails, the file is still at the source
							if (bSourceArchive) {
								TIFiles tif = TIFiles.createFromFile(file);
								// Need to allocate the file
								// This may lead to a ImageFull, but the alternative would be to 
								// delete first and then probably fail to insert, losing the file
								dirTarget.insertFile(tif.toByteArray(), null, false);
							}
							else {
								// Keep the allocation
								dirTarget.moveinFile(file);
							}

							if (bTargetArchive) {
								// Deallocate the file at the source
								dirSource.deleteFile(file, true);
							}
							else {
								// Keep the allocation
								dirSource.moveoutFile(file);
							}
						}
						else {
							Directory dir = (Directory)el;
							// Ignore the parent link
							if (dir.isParentLink()) continue;
							
							// Keep the allocation
							dirTarget.moveinDir(dir);
							dirSource.moveoutDir(dir);
						}
					}
					// System.out.println("paste done");
					// Commit after the loop
					dirSource.commit(false);
					dirTarget.commit(true);					
					// Should the generation better be static?
				}
				catch (FileNotFoundException fnfx) {
					if (fnfx.getMessage().indexOf("ermission")!=-1) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("PasteNoPermission"), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE); 
					}
					else {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("NoReopenWrite"), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE);
					}
					bReload = true;
				}
				catch (IOException iox) {
					iox.printStackTrace();
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE);					
					bReload = true;
				}
				catch (ProtectedException px) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE);
					imagetool.closeCurrentView();					
					bReload = true;
				}
				catch (FileExistsException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("PasteAlreadyExists") + ": " + ix.getMessage(), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE);					
					bReload = true;					
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), ix.getMessage(), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE);					
					bReload = true;					
				}
				catch (IllegalOperationException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), ix.getMessage(), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE);					
					bReload = true;					
				}
				catch (InvalidNameException inx) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), inx.getMessage(), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE);					
					bReload = true;					
				}
				
				if (bReload) {
					try {
						imagetool.reloadVolume(volTarget);
					}
					catch (Exception ex) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("NotReopen"), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE); 
					}
				}

				imagetool.refreshPanel(volSource);
				imagetool.refreshPanel(volTarget);
				
				if (sError != null) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), sError, TIImageTool.langstr("IllegalOperation"), JOptionPane.ERROR_MESSAGE); 
					imagetool.clearClipboard();
				}
				m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));		
			}
			
			// ********* We will treat moving between images like copying **************  
			
			// if (sameImage and element is file) moveFile(file, targetdir)
			// if (sameImage and element is dir) moveDir(dir, targetdir)
			// if (otherImage and target is not rootdir and otherimage is floppy) error(Cannot move directory into floppy subdir)
			// if (otherImage and otherimage is floppy and otherimagedirs=3) error(Cannot create another subdirectory on target)
			// if (otherImage and element is file) copyFile(file, targetdir); deleteFile(file);
			// if (otherImage and element is dir) copyDir(dir, targetdir); deleteDir(dir);
			// if (name already present) popupQuery(newname, overwrite, skip);
			
			// moveFile:
			//   Floppy:
			//     FDR stays as is
			//     FDIR of source directory is changed
			//     FDIR of target directory is changed
			//   Hard disk:
			//     FDR stays as is
			//     FDIR of source directory is changed
			//     FDIR of target directory is changed
			//     DDR of source dir is changed
			//     DDR of target dir is changed		
			
			// moveDir:
			//   Hard disk:
			//     DDR(source).pointerToParent is changed
			//     DDR of parent(source) is changed
			//     DDR of target is changed

		}
		else { // not move or not same image
			// System.out.println("Not move, or different images");
			// We will copy the elements from the source to the target
			
			// if (element is dir and target is a childDirectory) error(Cannot copy parent into child)		
			
			// First we check for obvious illegal operations
			// We must not 
			// - move/copy a directory on a disk anywhere else than into the root directory
			// - move/copy a directory in one of its children or deeper descendants
			for (Element el : list) {
				if (el instanceof Directory) {
					if (((Directory)el).isParentLink()) continue;

					Directory dirStep = dirTarget;
					
					if (volTarget.isCF7Volume()) {
						sError = TIImageTool.langstr("CF7NoDirectory");
					}
					else {
						if (volTarget.isFloppyImage() && !dirTarget.isRootDirectory()) {
							sError = TIImageTool.langstr("PasteNotFloppyDir");
						}
					}
					
					if (bSameImage) {
						while (dirStep != null && sError == null) {
							if (dirStep.equals((Directory)el)) {
								sError = TIImageTool.langstr("PasteNotDirChild");
							}
							else dirStep = dirStep.getContainingDirectory();
						}
					}
				}
				if (sError != null) break;
			}

			if (sError == null) {
				try {
					boolean bAbort = false;
					String sImportName = null;

					for (Element el : list) {
						// System.out.println("Element " + el);

						// System.out.println("el = " + el);
						// File copy
						if (el instanceof TFile) {
							TFile file = (TFile)el;
							TIFiles tif = TIFiles.createFromFile(file);
							sImportName = file.getName();
							boolean bRetry = false;
							// We may need several attempts before we get a useful file name
							do {
								try {
									dirTarget.insertFile(tif.toByteArray(), sImportName, false);
									// Can use true since we iterate over the clipboard, not over the file list
									if (bMove) dirSource.deleteFile(file, true);
									bRetry = false;
								}
								catch (ProtectedException px) {
									JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE); 
									bAbort = true;
								}
								catch (FileExistsException fxx) {
									// File name already exists
									bRetry = true;
									sImportName = getAlternativeName(true, file.getName(), dvCurrent.getFrame());
									if (sImportName.equals(NAME_SKIP)) {
										bRetry = false; // skip
										sImportName = null;
									}
									else {
										if (sImportName.equals(NAME_ABORT)) {
											bAbort = true; 
											sImportName = null;
										}
									}
								}
								catch (InvalidNameException fx) {
									// Invalid file name
									bRetry = true;
									sImportName = getAlternativeName(true, file.getName(), dvCurrent.getFrame());
									if (sImportName.equals(NAME_SKIP)) {
										bRetry = false; // skip
										sImportName = null;
									}
									else {
										if (sImportName.equals(NAME_ABORT)) {
											bAbort = true; 
											sImportName = null;
										}
									}
								}
								catch (ImageFullException ifx) {
									JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("PasteImageFull"), file.getName()), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE);										
									// Give up
									System.out.println("Image full");
									bAbort = true;									
								}
							} while (bRetry && !bAbort);
						}
						else {
							// Directory copy
							if (el instanceof Directory) {
								if (((Directory)el).isParentLink()) continue;
								
								sImportName = null;
								boolean bRetry = false;
								do {
									boolean bNewName = false;
									try {
										copyDir((Directory)el, sImportName, dirTarget, dvCurrent);
										if (bMove) dirSource.delDir((Directory)el, true);
										bRetry = false;
									}
									catch (FileExistsException fxx) {
										bNewName = true;
									}
									catch (InvalidNameException inx) {
										bNewName = true;
									}
									catch (FormatException fx) {
										bNewName = true;
									}
									catch (ProtectedException px) {
										JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE); 
										bAbort = true;
									}
									catch (ImageFullException ifx) {
										if (ifx.getCode()==ImageFullException.DIREXC) {
											// Cannot hold more directories
											int nRet = JOptionPane.showConfirmDialog(dvCurrent.getFrame(), TIImageTool.langstr("FloppyDirectoryOnly3"), TIImageTool.langstr("PasteError"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE); 
											if (nRet == JOptionPane.CANCEL_OPTION) {
												bAbort = true;
												bRetry = false;
											}
										}
										else {
											JOptionPane.showMessageDialog(dvCurrent.getFrame(), String.format(TIImageTool.langstr("PasteImageFull"), el.getName()), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE);										
											bAbort = true;
										}
									}
									
									if (bNewName) {
										bRetry = true;
										sImportName = getAlternativeName(false, el.getName(), dvCurrent.getFrame());
										if (sImportName.equals(NAME_SKIP)) {
											bRetry = false; // skip
											sImportName = null;
										}
										else {
											if (sImportName.equals(NAME_ABORT)) {
												bAbort = true; 
												sImportName = null;
											}
										}
									}

								} while (bRetry && !bAbort);
							}
							if (bAbort) break;
						}
					}
					// Save also if aborted
					if (bMove) dirSource.commit(true);
					if (bAbort) volTarget.rollback();
					else dirTarget.commit(true);
				}
				catch (ImageException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("ImageError") + ": " + ix.getMessage(), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE);					
				}
				catch (FileNotFoundException fnfx) {
					if (fnfx.getMessage().indexOf("ermission")!=-1) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("PasteNoPermission"), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE); 
					}
					else {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(),  TIImageTool.langstr("NoReopenWrite"), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE);
					}
					try {
						imagetool.reloadVolume(volTarget);
					}
					catch (Exception ex) {
						JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("NotReopen"), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE); 
					}
				}
				catch (IOException iox) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), TIImageTool.langstr("IOError") + ": " + iox.getClass().getName(), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE);
					iox.printStackTrace();
				}
				catch (IllegalOperationException ix) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), ix.getMessage(), TIImageTool.langstr("IllegalOperation"), JOptionPane.ERROR_MESSAGE);					
				}
				catch (ProtectedException px) {
					JOptionPane.showMessageDialog(dvCurrent.getFrame(), px.getMessage(), TIImageTool.langstr("PasteError"), JOptionPane.ERROR_MESSAGE); 
				}	
				imagetool.refresh(null);				
			}
		}
		if (sError != null) {
			JOptionPane.showMessageDialog(dvCurrent.getFrame(), sError, TIImageTool.langstr("IllegalOperation"), JOptionPane.ERROR_MESSAGE); 
			imagetool.clearClipboard();
		}
		m_parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	/** Copy the directory dir (with its contents) into the directory dirTarget.
		@exception FormatException if the provided file or directory name is invalid
	*/
	private void copyDir(Directory dir, String sUseThisName, Directory dirTarget, DirectoryView dvTarget) throws ProtectedException, InvalidNameException, FileExistsException, ImageFullException, ImageException, IOException, IllegalOperationException {
		Volume volSource = dir.getVolume();
		String sName = (sUseThisName!=null)? sUseThisName : dir.getName();

		// Sanity checks
		if (dirTarget.getVolume().isCF7Volume()) throw new IllegalOperationException(TIImageTool.langstr("PasteNotDirIntoCF7"));

		Directory[] dirs = dir.getDirectories();
		if (dirs.length !=0 && dirTarget.getVolume().isFloppyImage()) throw new IllegalOperationException(TIImageTool.langstr("PasteNotFloppyDir"));
		
		Directory dirNew = dirTarget.createSubdirectory(sName, false);

		// All files from dir
		TFile[] files = dir.getFiles();
		boolean bAbort = false;
		for (int i=0; i < files.length && !bAbort; i++) {
			TFile file = files[i];
			TIFiles tif = TIFiles.createFromFile(file);
			String sImportName = null;
			boolean bRetry = false;
			do {
				boolean bNewName = false;
				try {
					dirNew.insertFile(tif.toByteArray(), sImportName, false);
					bRetry = false;
				}
				catch (FileExistsException fex) {
					bNewName = true;
				}
				catch (InvalidNameException fex) {
					bNewName = true;
				}
				if (bNewName) {
					bRetry = true;
					sImportName = getAlternativeName(true, file.getName(), dvTarget.getFrame());
					if (sImportName.equals(NAME_SKIP)) {
						bRetry = false; // skip
						sImportName = null;
					}
					else {		
						if (sImportName.equals(NAME_ABORT)) {
							bAbort = true;
							sImportName = null;
						}
					}
				}
			} while (bRetry && !bAbort);
		}
		dirNew.commit(false);
		
		// Now recurse. We cannot have any more FileExistsException here
		// if the source file system is correct (even if not, the sets will
		// not allow for multiple occurances of the same name)
		for (int i=0; i < dirs.length; i++) {
			copyDir(dirs[i], null, dirNew, dvTarget);
		}		
	}
}
