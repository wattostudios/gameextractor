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

package org.watto.ge.plugin.exporter;

import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.archive.PluginGroup_U;
import org.watto.ge.plugin.archive.PluginGroup_UE3;
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.io.FileManipulator;

public class Exporter_Custom_UE3_SoundNodeWave_Generic extends ExporterPlugin {

  static Exporter_Custom_UE3_SoundNodeWave_Generic instance = new Exporter_Custom_UE3_SoundNodeWave_Generic();

  static FileManipulator readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_UE3_SoundNodeWave_Generic getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_UE3_SoundNodeWave_Generic() {
    setName("Unreal Sound File");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return readLength > 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    try {
      readSource.close();
      readSource = null;
    }
    catch (Throwable t) {
      readSource = null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter extracts the Audio from Unreal Engine files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  @Override
  public void open(Resource source) {
    try {
      long offset = source.getOffset();

      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(offset);

      readLength = source.getLength();

      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (!(readPlugin instanceof PluginGroup_UE3) && !(readPlugin instanceof PluginGroup_U)) {
        readLength = 0; // force quit
        return;
      }

      PluginGroup_UE3 ue3Plugin = (PluginGroup_UE3) readPlugin;

      // 4 - File ID Number? (incremental from 0) (thie is NOT an ID into the Names Directory!)
      readSource.skip(4);

      // for each property (until Property Name ID = "None")
      //   8 - Property Name ID
      //   8 - Property Type ID
      //   8 - Property Length
      //   X - Property Data
      UnrealProperty[] properties = ue3Plugin.readProperties(readSource);

      // 4 - null
      // 4 - Unknown
      // 4 - null
      // 4 - Audio Length (Compressed Length?)
      // 4 - Audio Length (Decompressed Length?)
      // 4 - Unknown
      readSource.skip(16);

      // Find the RIFF/OggS header (some files have other fields in here)
      // Want to do this because this is the Generic plugin, so want to suit as many cases as possible
      long currentOffset = readSource.getOffset();

      byte byte1 = readSource.readByte();
      byte byte2 = readSource.readByte();
      byte byte3 = readSource.readByte();
      byte byte4 = readSource.readByte();

      for (int i = 0; i < 20; i++) {
        if (byte1 == 82 && byte2 == 73 && byte3 == 70 && byte4 == 70) {
          // found RIFF
          break;
        }
        else if (byte1 == 79 && byte2 == 103 && byte3 == 103 && byte4 == 83) {
          // found OggS
          break;
        }
        else {
          // move the bytes along 1, and try again
          currentOffset++;

          byte1 = byte2;
          byte2 = byte3;
          byte3 = byte4;
          byte4 = readSource.readByte();
        }
      }

      readSource.relativeSeek(currentOffset);

      // X - Audio Data
      long headerLength = readSource.getOffset() - offset;

      readLength -= headerLength;

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * // TEST - NOT DONE
   **********************************************************************************************
   **/
  @SuppressWarnings("unused")
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {
      long decompLength = source.getDecompressedLength();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      //for (int i=0;i<decompLength;i++){
      while (exporter.available()) {
        destination.writeByte(exporter.read());
      }

      exporter.close();

      //destination.forceWrite();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      readLength--;
      return readSource.readByte();
    }
    catch (Throwable t) {
      return 0;
    }
  }

}