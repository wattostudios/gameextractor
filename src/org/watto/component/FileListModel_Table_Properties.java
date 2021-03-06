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

package org.watto.component;

import org.watto.datatype.Archive;
import org.watto.datatype.Resource;

public class FileListModel_Table_Properties extends FileListModel_Table {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileListModel_Table_Properties() {
    super();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean isCellEditable(int row, int column) {
    if (columns[column].isEditable()) {
      return true;
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void reload() {
    readPlugin = Archive.getReadPlugin();
    resources = readPlugin.getProperties();
    columns = readPlugin.getViewingPropColumns();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void reload(Resource[] resources) {
    this.resources = resources;
    readPlugin = Archive.getReadPlugin();
    columns = readPlugin.getViewingPropColumns();
  }

}