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

package de.mizapf.timt.files;

public class FormatParameters {
	public  String name;
	public  String formatclass; 		// hat keinen Sinn hier; das sind Angaben zum Dateisystem und nicht zum image
	public 	int cylinders;
	public 	int heads;
	public 	int sectors;
	public 	int reservedAUs;
	public 	int stepRate;
	public 	int reducedWriteCurrent;
	public 	int auSize;
	public 	boolean bufferedStep;
	public 	int writePrecompensation;
	public 	Time time;
	public 	boolean format;
	public  int type;
	public  int chdVersion; 
	public  int totalsectors;
	public  int formatUnitSectors;
	
	public FormatParameters(String sVolumeName, boolean bFormat) {
		name = sVolumeName;
		format = bFormat;
	}
	
	public void setCHS(int nCylinders, int nHeads, int nSectorsPerTrack) {
		cylinders = nCylinders;
		heads = nHeads;
		sectors = nSectorsPerTrack;	
	}
	
	public void setTotal(int nTotal) {
		totalsectors = nTotal;
	}
	
	public void setFormatUnitSectors(int nFU) {
		formatUnitSectors = nFU;
	}

	public void setMFM(int nStepRate, int nReducedWriteCurrent, int nWritePrecompensation, boolean bBuffered) {
		stepRate = nStepRate;
		reducedWriteCurrent = nReducedWriteCurrent;
		writePrecompensation = nWritePrecompensation;
		bufferedStep = bBuffered;
	}
	
	public void setHD(Time tCreated, int nAUSize, int nReserved, int nType) {
		time = tCreated;
		auSize = nAUSize;
		reservedAUs = nReserved;
		type = nType;
	}
	
	public int getTotalSectors() {
		if (totalsectors != 0) return totalsectors;
		return cylinders * heads * sectors;
	}
	
	int getFormatUnitLength() {
		if (formatUnitSectors != 0) return formatUnitSectors * TFileSystem.SECTOR_LENGTH;
		return sectors * TFileSystem.SECTOR_LENGTH;
	}
	
	public boolean isHFDC() {
		return (type==HarddiskFileSystem.MFM);
	}
}
