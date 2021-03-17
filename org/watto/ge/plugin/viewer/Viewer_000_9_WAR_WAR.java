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

package org.watto.ge.plugin.viewer;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_000_9;
import org.watto.ge.plugin.archive.Plugin_DAT_77;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_000_9_WAR_WAR extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_000_9_WAR_WAR() {
    super("000_9_WAR_WAR", "Tomb Raider WAR Image");
    setExtensions("war");

    setGames("Legacy of Kain: Soul Reaver 2",
        "Tomb Raider: Anniversary",
        "Tomb Raider: Legend",
        "Tomb Raider: Underworld");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_000_9 || plugin instanceof Plugin_DAT_77) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals("!WAR")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - Image Data Offset (128)
      if (fm.readInt() == 128) {
        rating += 5;
      }

      // 4 - Image Data Length
      if (FieldValidator.checkLength(fm.readInt(), fm.getLength())) {
        rating += 5;
      }

      fm.skip(8);

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
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
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      ImageResource imageResource = readThumbnail(fm);

      if (imageResource == null) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(imageResource);

      return preview;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (!WAR)
      fm.skip(4);

      // 4 - Image Data Offset (128)
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Image Data Length
      // 4 - Unknown (3)
      // 4 - Padding Offset (112)
      fm.skip(12);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown
      fm.skip(4);

      // 4 - Bits Per Color (32)
      int imageFormat = fm.readInt();

      // 4 - Bits Alpha (8)
      // 4 - Bits Blue (8)
      // 4 - Bits Green (8)
      // 4 - Bits Red (8)
      // 4 - Bitmask for Alpha
      // 4 - Bitmask for Blue 
      // 4 - Bitmask for Green
      // 4 - Bitmask for Red 
      // 44 - null
      // 8 - Padding (-1)
      // 8 - null
      fm.seek(dataOffset);

      // X - Pixels (BGRA)
      ImageResource imageResource = null;
      if (imageFormat == 32) {
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
      }
      else if (imageFormat == 16) {
        imageResource = ImageFormatReader.readRGBA5551(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_000_9_WAR_WAR] Unknown image format: " + imageFormat);
      }

      fm.close();

      return imageResource;

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
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}