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
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CLT_AWAD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CLT_AWAD() {

    super("CLT_AWAD", "CLT_AWAD");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("clt");
    setGames("Abomination: The Nemesis Project");
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

      // Header
      if (fm.readString(4).equals("AWAD")) {
        rating += 50;
      }

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
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

      // 4 - Header (AWAD)
      fm.skip(4);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      for (int i = 0; i < numFiles; i++) {
        System.out.println(fm.getOffset());
        // 260 - Filename (null)
        String filename = fm.readNullString(260);
        FieldValidator.checkFilename(filename);

        // 4 - File Length
        int length = fm.readInt();
        if (length < 0) {
          length = 0 - length;
        }
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize + 1); // +1 to allow empty files at end of archive

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

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Header (AWAD)
      fm.writeString("AWAD");

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 8 + (numFiles * 268);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 260 - Filename (null)
        fm.writeNullString(resource.getName(), 260);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        // 4 - File Offset
        fm.writeInt((int) offset);

        offset += decompLength;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}