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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_79 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_79() {

    super("DAT_79", "DAT_79");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("RollerCoaster Tycoon");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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

      // Check for the Index file
      String filename = fm.getFile().getAbsolutePath();
      int lastDot = filename.lastIndexOf(".");
      if (lastDot <= 0) {
        return 0;
      }

      String indexFilename = filename.substring(0, lastDot) + "i." + filename.substring(lastDot + 1);
      if (new File(indexFilename).exists()) {
        rating += 25;
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

      long arcSize = path.length();

      // Get the Index file
      String pathFilename = path.getAbsolutePath();
      int lastDot = pathFilename.lastIndexOf(".");
      if (lastDot <= 0) {
        return null;
      }

      String indexFilename = pathFilename.substring(0, lastDot) + "i." + pathFilename.substring(lastDot + 1);
      File indexPath = new File(indexFilename);
      if (!indexPath.exists()) {
        return null;
      }

      FileManipulator fm = new FileManipulator(indexPath, false);

      int numFiles = ((int) fm.getLength() / 16);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset (relative to the start of the File Data)
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - Image Width
        short imageWidth = fm.readShort();
        FieldValidator.checkWidth(imageWidth);

        // 2 - Image Height
        short imageHeight = fm.readShort();
        FieldValidator.checkWidth(imageHeight);

        // 2 - X Offset
        short xOffset = fm.readShort();

        // 2 - Y Offset
        short yOffset = fm.readShort();

        // 2 - Flags (only the lower 4 bits are relevant --> 1=bitmap, 5=compressed, 8=palette)
        int flags = fm.readShort() & 15;

        // 2 - Unknown
        fm.skip(2);

        String filename = Resource.generateFilename(i);
        if (flags == 1) {
          filename += ".bitmap";
        }
        else if (flags == 5) {
          filename += ".compressed_bitmap";
        }
        else if (flags == 8) {
          filename += ".palette";
        }

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset);
        resource.addProperty("Width", "" + imageWidth);
        resource.addProperty("Height", "" + imageHeight);
        resource.addProperty("XOffset", "" + xOffset);
        resource.addProperty("YOffset", "" + yOffset);
        resources[i] = resource;

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
