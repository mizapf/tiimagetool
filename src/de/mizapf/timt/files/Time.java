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

import java.util.Arrays;
import java.util.Calendar;

public class Time {

	private byte[] m_abyTime;
	
	public Time(byte[] time) {
		m_abyTime = new byte[4];
		m_abyTime[0] = time[0];
		m_abyTime[1] = time[1];
		m_abyTime[2] = time[2];
		m_abyTime[3] = time[3];
	}
	
	public Time(byte[] data, int nOffset) {
		m_abyTime = new byte[4];
		m_abyTime[0] = data[nOffset];
		m_abyTime[1] = data[nOffset+1];
		m_abyTime[2] = data[nOffset+2];
		m_abyTime[3] = data[nOffset+3];
	}
	
	public byte[] getBytes() {
		return m_abyTime;
	}
	
	public Time() {
		m_abyTime = new byte[4];
		Arrays.fill(m_abyTime, 0, 4, (byte)0);
	}
	
	public static Time createNow() {
		Calendar cal = Calendar.getInstance();
		byte[] abyTime = new byte[4];
		abyTime[0] = (byte)(((cal.get(Calendar.HOUR_OF_DAY) << 3)&0xff) | ((cal.get(Calendar.MINUTE)>>3)&0xff));
		abyTime[1] = (byte)(((cal.get(Calendar.MINUTE) << 5)&0xff) | ((cal.get(Calendar.SECOND)>>1)&0xff));
		int nYear = cal.get(Calendar.YEAR)-1900;
		if (nYear >= 100) nYear = nYear-100; 
		abyTime[2] = (byte)(((nYear<<1)&0xff) | (((cal.get(Calendar.MONTH)+1)>>3)&0x1));
		abyTime[3] = (byte)((((cal.get(Calendar.MONTH)+1)<<5)&0xff) | cal.get(Calendar.DAY_OF_MONTH));
		return new Time(abyTime);
	}
	
	// Timestamp
	//
	// hhhh hmmm mmms ssss	2-sek-Auflösung
	// yyyy yyyM MMMd dddd
	
	// 111 = 01101111
	//	80 = 01010000
	//	14 = 00001110
	//	39 = 00010111
		
	// a826 c691 = 10101 000001 00110 1100011 0100 10001
	//			 =	21	   1	  6		99		4	 17
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		int nFirst = (int)((m_abyTime[0]<<8)&0xff00) + (int)(m_abyTime[1]&0xff);
		int nSecond = (int)((m_abyTime[2]<<8)&0xff00) + (int)(m_abyTime[3]&0xff);
		if (nFirst==0 && nSecond==0) return "";
		
		int nYear = (nSecond>>9)&0x7f;
		int nMonth = (nSecond>>5)&0x0f;
		int nDay = nSecond&0x1f;
		int nHour = (nFirst>>11)&0x1f;
		int nMinute = (nFirst>>5)&0x3f;
		int nSec = (nFirst&0x1f)*2;
		if (nYear < 70) nYear = nYear+2000;
		else nYear = nYear+1900;
		String sFormat = "%1$04d-%2$02d-%3$02d %4$02d:%5$02d:%6$02d";
		return String.format(sFormat, nYear, nMonth, nDay, nHour, nMinute, nSec);
	}
}
