/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;

final class PrimaryKeyColumnPacker implements ResultPacker<PrimaryKeyColumn> {

  @Override
  public PrimaryKeyColumn fetch(ResultSet resultSet) throws SQLException {
    return new PrimaryKeyColumn(resultSet.getString("COLUMN_NAME"), resultSet.getInt("KEY_SEQ"));
  }
}
