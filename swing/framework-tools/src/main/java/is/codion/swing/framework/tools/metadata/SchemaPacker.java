/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;

final class SchemaPacker implements ResultPacker<Schema> {

  @Override
  public Schema fetch(final ResultSet resultSet) throws SQLException {
    return new Schema(resultSet.getString("TABLE_SCHEM"));
  }
}
