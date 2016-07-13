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
	public String name;
	public 	int cylinders;
	public 	int heads;
	public 	int sectorsPerTrack;
	public 	int sectorLength;
	public 	int reservedAUs;
	public 	int stepRate;
	public 	int reducedWriteCurrent;
	public 	int auSize;
	public 	boolean bufferedStep;
	public 	int writePrecompensation;
	public 	Time time;
	public 	boolean format;
	public	boolean forHfdc;
	public  int chdVersion;
	
	public FormatParameters(int nCylinders, int nHeads, int nSectors, int nSectorLength, int nFormat) {
		cylinders = nCylinders;
		heads = nHeads;
		sectorsPerTrack = nSectors;
		sectorLength = nSectorLength;
		chdVersion = nFormat;
		format = false;
	}
	
	public FormatParameters(String sName, int nCylinders, int nHeads, int nSectors, int nSectorLength,
		int nReserved, int nStepRate, int nReducedWriteCurrent, int nAUSize,  boolean bBuffered, 
		int nWritePrecompensation, Time tCreated, boolean bFormat, boolean bHfdc, int nCHDVersion) {
	
		name = sName;
		cylinders = nCylinders;
		heads = nHeads;
		sectorsPerTrack = nSectors;
		sectorLength = nSectorLength;
		reservedAUs = nReserved;
		stepRate = nStepRate;
		reducedWriteCurrent = nReducedWriteCurrent;
		auSize = nAUSize;
		bufferedStep = bBuffered;
		writePrecompensation = nWritePrecompensation;
		time = tCreated;
		format = bFormat;
		forHfdc = bHfdc;
		chdVersion = nCHDVersion; 
	}
}
