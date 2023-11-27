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

/** Area on the medium. Covers a contiguous area, starting at the value
	start, and going to end, including. Interval(3,10) = {3,...,10}.
	Intervals are ranges of sector numbers, not allocation unit numbers. */
	
public class Interval {
	public int start;
	public int end;
	
	public Interval(int s, int e) {
		start = s;
		end = e;
	}
	
	public int length() {
		return end-start+1;
	}
	
	public String toString() {
		return "[" + start + ".." + end + "]";
	}
	
	public boolean contains(int j) {
		return (start <= j) && (j <= end);
	}
}
