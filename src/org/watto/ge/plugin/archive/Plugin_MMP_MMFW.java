/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MMP_MMFW extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MMP_MMFW() {

    super("MMP_MMFW", "MMP_MMFW");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Disney's Magic Artist Studio",
        "Scooby Doo Case Files #1: The Glowing Bug Man");
    setExtensions("mmp", "mms", "mma"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setCanScanForFileTypes(true);

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

      // 14 - Header ("MMFW Pictures" + null)
      String header = fm.readNullString(14);
      if (header.equals("MMFW Pictures") || header.equals("MMFW Sounds") || header.equals("MMFW 3 Script")) {
        rating += 50;
      }

      // 2 - null
      fm.skip(2);

      // 2 - Header ("MM")
      if (fm.readString(2).equals("MM")) {
        rating += 5;
      }

      // 2 - Version? (3)
      // 4 - Unknown
      // 4 - Unknown (12854)
      // 4 - Unknown
      // 2 - Unknown
      fm.skip(16);

      // 2 - Number of Files
      if (FieldValidator.checkNumFiles(ShortConverter.changeFormat(fm.readShort()))) {
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

      // 14 - Header ("MMFW Pictures" + null)
      //String header = fm.readNullString(14);
      fm.skip(14);

      // 2 - null
      // 2 - Header ("MM")
      fm.skip(4);

      // 2 - Version? (3)
      boolean changeFormat = true;
      short version = ShortConverter.changeFormat(fm.readShort());
      if (version == 512) {
        version = 2;
        changeFormat = false;
      }

      // 4 - Unknown
      // 4 - Unknown (12854)
      // 4 - Unknown
      // 2 - Unknown
      fm.skip(14);

      // 2 - Number of Files
      short numFiles = fm.readShort();
      if (changeFormat) {
        numFiles = ShortConverter.changeFormat(numFiles);
      }
      FieldValidator.checkNumFiles(numFiles);

      String[] names = new String[numFiles];
      boolean hasNames = false;
      if (version == 2) {
        // look for filenames
        fm.skip(numFiles * 4);

        int sizeTest = fm.readInt();
        if (changeFormat) {
          sizeTest = IntConverter.changeFormat(sizeTest);
        }

        if (sizeTest == arcSize) {

          if (fm.readShort() == 0) {
            // yep, probably has filenames

            for (int i = 0; i < numFiles; i++) {
              // 32 - Filename (null terminated, filled with nulls);
              String name = fm.readNullString(32);
              FieldValidator.checkFilename(name);
              names[i] = name;
            }

            hasNames = true;
          }
        }
      }

      fm.seek(36);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        if (changeFormat) {
          offset = IntConverter.changeFormat(offset);
        }
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);
        if (hasNames) {
          filename = names[i];
        }

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

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
