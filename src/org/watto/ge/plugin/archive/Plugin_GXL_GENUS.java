
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.ReplacableResource;
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
public class Plugin_GXL_GENUS extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_GXL_GENUS() {

    super("GXL_GENUS", "Genus Microprogramming GXL");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setExtensions("gxl");
    setGames("Zorro");
    setPlatforms("PC");

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

      fm.skip(2);

      // Header
      if (fm.readString(48).equals("Copyright (c) Genus Microprogramming, Inc. 1998-")) {
        rating += 50;
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

      // 2 - Unknown
      // 50 - Header (Copyright (c) Genus Microprogramming, Inc. 1998-94) or (Copyright (c) Genus Microprogramming, Inc. 1998-90)
      // 2 - Unknown (100)
      // 74 - Unknown
      fm.skip(128);

      // 1 - null
      // 8 - Filename (null terminated)
      // 4 - File Extension (including the .)
      // 1 - null
      fm.skip(14);

      // 4 - Data Offset
      int numFiles = (fm.readInt() - 128) / 26;
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(128);

      for (int i = 0; i < numFiles; i++) {
        // 1 - null
        fm.skip(1);

        // 8 - Filename (32 terminated)
        String filename = StringHelper.readTerminatedString(fm.getBuffer(), (byte) 32, 8);
        FieldValidator.checkFilename(filename);

        // 4 - File Extension (including the .)
        String ext = fm.readString(4);
        filename += ext;

        // 1 - null
        fm.skip(1);

        // 4 - Data Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long lengthPointerLocation = fm.getOffset();
        long lengthPointerLength = 4;

        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 2 - Unknown
        // 2 - Unknown
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

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