
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.Settings;
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ASR_ASURA_PTXT extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_ASR_ASURA_PTXT() {

    super("ASR_ASURA_PTXT", "ASR_ASURA_PTXT");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Sniper Elite");
    setExtensions("asr");
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
      if (fm.readString(12).equals("Asura   PTXT")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (fm.readInt() + 24 == arcSize) {
        rating += 5;
      }

      // Version
      if (fm.readInt() == 5) {
        rating += 5;
      }

      fm.skip(8);

      // Number Of Text Strings
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 8 - Header 1 (Asura   )
      // 4 - Header 2 (PTXT)
      // 4 - Archive Length [+24]
      // 8 - Version (5)
      // 4 - Unknown
      fm.skip(28);

      // 4 - Number Of Text Strings
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      fm.skip(4);

      // X - Description String
      // 1 - null Description Terminator
      int descLength = fm.readNullString().length() + 1;

      // 0-3 - null Padding to a multiple of 4 bytes
      int padding = 4 - (descLength % 4);
      if (padding < 4) {
        fm.skip(padding);
      }

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - String Length [*2 for unicode] (including null terminators)
        //System.out.println(fm.getOffset());
        long length = fm.readInt() - 1;
        FieldValidator.checkLength(length, arcSize);

        // X - Text String (unicode text)
        long offset = fm.getOffset();
        String filename = fm.readUnicodeString((int) length);

        if (filename.length() > 100) {
          filename = filename.substring(0, 100);
        }

        //filename = FileBuffer.validateFilename(new File(filename));

        // 2 - null Filename Terminator
        fm.skip(2);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length * 2);

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

  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      int archiveSize = 20 + (numFiles * 10);
      for (int i = 0; i < numFiles; i++) {
        archiveSize += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // 8 - Header 1 (Asura   )
      // 4 - Header 2 (PTXT)
      fm.writeBytes(src.readBytes(12));

      // 4 - Archive Length [+24]
      fm.writeInt(archiveSize);
      src.skip(4);

      // 8 - Version (0)
      // 4 - Unknown
      // 4 - Number Of Text Strings
      // 4 - null
      fm.writeBytes(src.readBytes(20));

      // X - Description String
      // 1 - null Description Terminator
      int descLength = 0;
      int descByte = src.readByte();
      while (descByte != 0) {
        fm.writeByte(descByte);
        descLength++;
        descByte = src.readByte();
      }
      descLength++;

      // 0-3 - null Padding to a multiple of 4 bytes
      int padding = 4 - (descLength % 4);
      if (padding < 4) {
        fm.skip(padding);
      }

      src.close();

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength() / 2 + 1;

        // 4 - String Length [*2 for unicode] (including null terminators)
        fm.writeInt((int) length);

        // X - Text String (unicode text)
        write(resources[i], fm);

        // 2 - null Filename Terminator
        fm.writeShort((short) 0);
      }

      // 16 - null
      for (int i = 0; i < 16; i++) {
        fm.writeByte(0);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
