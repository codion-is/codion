/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;

final class PrimaryKeyColumnPacker implements ResultPacker<PrimaryKeyColumn> {

  @Override
  public PrimaryKeyColumn fetch(final ResultSet resultSet) throws SQLException {
    return new PrimaryKeyColumn(resultSet.getString("COLUMN_NAME"), resultSet.getInt("KEY_SEQ"));
  }
}
