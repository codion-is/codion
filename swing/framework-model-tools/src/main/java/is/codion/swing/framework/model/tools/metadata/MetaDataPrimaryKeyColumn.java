/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
