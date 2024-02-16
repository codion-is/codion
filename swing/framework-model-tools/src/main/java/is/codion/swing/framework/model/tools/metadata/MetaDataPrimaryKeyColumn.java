/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class MetaDataPrimaryKeyColumn {

  private final String columnName;
  private final int index;

  MetaDataPrimaryKeyColumn(String columnName, int index) {
    this.columnName = columnName;
    this.index = index;
  }

  public String columnName() {
    return columnName;
  }

  public int index() {
    return index;
  }

  static final class PrimaryKeyColumnPacker implements ResultPacker<MetaDataPrimaryKeyColumn> {

    @Override
    public MetaDataPrimaryKeyColumn get(ResultSet resultSet) throws SQLException {
      return new MetaDataPrimaryKeyColumn(resultSet.getString("COLUMN_NAME"), resultSet.getInt("KEY_SEQ"));
    }
  }
}
