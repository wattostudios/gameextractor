/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BNK_BKPK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BNK_BKPK() {

    super("BNK_BKPK", "BNK_BKPK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Batman: Arkham Knight");
    setExtensions("bnk"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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
      if (fm.readString(4).equals("BKPK")) {
        rating += 50;
      }

      fm.skip(16);

      // Number Of Files
      if (FieldValidator.checkNumFiles((int) fm.readLong())) {
        rating += 5;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readLong(), arcSize)) {
        rating += 5;
      }

      // Header Length (140)
      if (fm.readLong() == 140) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (BKPK)
      // 2 - null
      // 2 - Unknown
      // 2 - Unknown
      // 2 - Unknown
      // 8 - null
      fm.skip(20);

      // 8 - Number of Files
      int numFiles = (int) fm.readLong();
      FieldValidator.checkNumFiles(numFiles);

      // 8 - null
      // 8 - Directory Length (not including padding)
      fm.skip(16);

      // 8 - Header Length (140)
      long headerLength = fm.readLong();
      FieldValidator.checkLength(headerLength, arcSize);

      // 8 - Unknown
      // 8 - Unknown
      // 8 - null
      // 8 - null
      // 8 - Length of Directory + Header (not including padding)
      // 8 - null
      // 8 - null
      // 8 - null
      // 8 - File Data Length
      fm.skip(72);

      // 8 - File Data Offset
      long dataOffset = fm.readLong();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 8 - Unknown (2/3)
      fm.relativeSeek(headerLength);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fm.skip(4);

        // 8 - File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 8 - File Offset (relative to the start of the FILE DATA)
        long offset = dataOffset + fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
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

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
