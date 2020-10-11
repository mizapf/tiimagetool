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

public class GenCounter {
	int m_nGeneration;
	
	public GenCounter() {
		m_nGeneration = 0;
	}
	
	public void setGeneration(int gen) {
		m_nGeneration = gen;
	}
	
	public void nextGeneration() {
		System.out.println("+++ next gen");
		m_nGeneration++;
	}
	
	public void sameGeneration() {
		m_nGeneration--;
	}
	
	public int getGeneration() {
		return m_nGeneration;
	}

	public void init() {
		m_nGeneration = 0;
	}	
}

