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

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_TEX_4;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TEX_4_BCT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TEX_4_BCT() {
    super("TEX_4_BCT", "Dead Rising 4 BCT Image");
    setExtensions("bct");

    setGames("Dead Rising 4");
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
      if (plugin instanceof Plugin_TEX_4) {
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

      fm.skip(4);

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
        rating += 5;
      }

      fm.skip(24);

      long arcSize = fm.getLength();

      // 4 - Largest MipMap Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Largest MipMap Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      // 3 - Unknown
      fm.skip(3);

      // 1 - Image Format (96=BC5, 224=DXT1, 228=DXT5)
      int imageFormat = ByteConverter.unsign(fm.readByte());

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 8 - Unknown
      int imageFormat2 = ByteConverter.unsign(fm.readByte());

      // 4 - Header Size (32)
      // 8 - null
      // 2 - Unknown (32)
      // 2 - Unknown (1)
      fm.skip(23);

      // 4 - Largest MipMap Offset
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Largest MipMap Length
      int dataLength = fm.readInt();
      FieldValidator.checkLength(dataLength, arcSize);

      // X - Unknown
      fm.skip(dataOffset - 40);

      // X - Image Data (DXT5)
      ImageResource imageResource = null;
      if (imageFormat2 == 40) {
        imageResource = ImageFormatReader.readBC7(fm, width, height);
      }
      else if (imageFormat == 228) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat == 224) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 96) {
        imageResource = ImageFormatReader.readBC5(fm, width, height);
      }
      else if (width * height == dataLength) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }

      fm.close();

      //ColorConverter.convertToPaletted(resource);

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