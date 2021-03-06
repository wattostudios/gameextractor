
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       GAME EXTRACTOR                                       //
//                               Extensible Game Archive Editor                               //
//                                http://www.watto.org/extract                                //
//                                                                                            //
//                           Copyright (C) 2002-2009  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////
import org.watto.io.FileManipulator;
import org.watto.io.StringHelper;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FTR_MFIL extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_FTR_MFIL() {

    super("FTR_MFIL", "FTR_MFIL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("ftr");
    setGames("Mortal Kombat 3");
    setPlatforms("PC");

    setFileTypes("clas", "WAV Sound File",
        "spec", "WAV Sound File",
        "csmp", "Sound Descriptor",
        "ssmp", "Sound Descriptor");

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // Header
      if (fm.readString(4).equals("MFIL")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      fm.skip(2);

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt())) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header
      fm.skip(4);

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Unknown
      fm.skip(2);

      long arcSize = fm.getLength();

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Directory Header (CDIR)
      // 2 - Unknown
      // 8 - Directory Name (FLAGBAG!)
      // 2 - null
      // 4 - Directory Size (not including the directory header data)
      // 4 - Directory Size (not including the directory header data)
      // 4 - null
      // 4 - Unknown
      fm.skip(32);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      for (int i = 0; i < numFiles; i++) {

        // 4 - File Type / Extension
        String ext = fm.readNullString(4);

        // 2 - Unknown
        fm.skip(2);

        // 8 - Filename (terminated by spaces)
        String filename = StringHelper.readTerminatedString(fm.getBuffer(), (byte) 32, 8) + "." + ext.toLowerCase();

        // 2 - Unknown
        fm.skip(2);

        // 4 - File Offset
        long offset = fm.readInt() + 32;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 6 - null
        // 2 - Unknown
        fm.skip(8);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }

  }

}