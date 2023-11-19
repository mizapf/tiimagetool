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
    
    Copyright 2023 Michael Zapf
    www.mizapf.de
    
****************************************************************************/

package de.mizapf.timt.files;
import de.mizapf.timt.util.NotImplementedException;
import java.io.FileNotFoundException;
import java.io.IOException;

/** Represents a image format containing one or more subvolumes. Used with 
    CF7 images and IDE images.
    
    SCSI: not partitioned
    HFDC: not partitioned
    CF7: Partitioned
    IDE: Partitioned    
    
    CF7:
    External format:
       Sequence of internal format instances, no special separators
    Internal format:
       2*409600 bytes (1600 sectors = 40*2*20), sector dump, only on even bytes
       May also be used stand-alone (all bytes, 409600 bytes long)
    
    
    IDE:
    External format:
    00000000: 2a54 4939 3946 5350 542a ffff 0000 5054  *TI99FSPT*....PT
    00000010: 0000 0000 000b 4000 0000 0001 0002 e000  ......@.........
    00000020: 0002 e001 0002 e000 0005 c001 0002 e000  ................
    00000030: 0008 a001 0002 9fff 0000 0000 0000 0000  ................
    
    00-09: Disk name (default: *TI99FSPT*)
    0a-0b: Total number of AUs                             FFFF
    0c-0d: 0000
    0e-0f: "PT"
    10-13: 0000 0000                                       (sectors 512 bytes)
    14-17: Total #sectors (4 bytes)                        000b4000 = 360 MiB
    18-1b: Offset 1st partition                            00000001
    1c-1f: #sectors 1st partition                          0002e000 = 92 MiB
    20-23: Offset 2nd partition                            0002e001
    24-27: #sectors 2nd partition                          0002e000 = 92 MiB
    28-2b: Offset 3rd partition                            0005c001
    2c-2f: #sectors 3rd partition                          0002e000 = 92 MiB
    30-33: Offset 4th partition                            0008a001
    34-37: #sectors 4th partition                          00029fff = 84 MiB
    fe-ff: 5AA5       
    
    Offset of partition in multiples of sector size (512)
    
    Processing:
    
    open Image -> instanceof PartitionedImageFormat? Select partition : Open image
    
    ----------------------------------
    
    Erkenntnisse zum Thema Partitionen
    
    FormatUnits beziehen sich auf das endgültige Speicherformat, also etwa
    CHD. CHDs beinhalten außerdem (für IDE) die Partitionen.
    FU können Blöcke aus zwei Partitionen beinhalten (Ende Part 1, Anfang Part 2)
    
    Der Zugriff auf Sektoren erfordert die Angabe der Partitionsnummer
    (0 = keine Partition).
    
    Partitionen existieren auch ohne Dateisystem.
    Sektoren existieren auch ohne Dateisystem.
    
    --> Die gewählte Partition steht nicht im Dateisystem
    
    Operationen: 
    - Neues Partitionssystem -> direktes Speichern
    - Neue Partition -> direktes Speichern
    - Neues Dateisystem
    
    Typischer Vorgang:
    - Neues Partitionssystem
      Auswahl: 
      - IDE oder CF7? (weitere?)
      - Größe
        - muss das vorher bestimmt werden? 
          - IDE: ja, da die Zahl der Partitionen begrenzt ist;
                 verwendet CHD, muss die Zahl der Hunks festlegen
          - CF7: eigentlich nicht; jede neue Partition kann angehängt werden
                 jedoch sinnvoller: Festlegen auf Größe der Speicherkarte (z.B. 128 MiB)
      - Speicherdialog
                 
    - Neue Partition
      - Ladedialog
      - erkennt IDE (hd) oder CF7 (bin)
      - Anbieten möglicher Partitionen
        - fortlaufend? (Nachschauen bei CF7, IDE)
    
   
        
    Schlussfolgerung: Ist das PIFormat ein ImageFormat?
    Interface PartitionedStorage
    
    CF7ImageFormat implements PartitionedStorage
    MameCHDFormat implements PartitionedStorage
    RawHDFormat implements PartitionedStorage
    
    
    
    Neues Dateisystem

*/	

public interface PartitionedStorage {
	
	void setPartition(int part);
	void setupPartitionTable() throws ImageException, IOException;
	int getActivePartition();
}
