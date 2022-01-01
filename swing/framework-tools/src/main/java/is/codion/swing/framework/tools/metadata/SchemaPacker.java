/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;

final class SchemaPacker implements ResultPacker<Schema> {

  private final String fieldName;

  SchemaPacker(final String fieldName) {
    this.fieldName = fieldName;
  }

  @Override
  public Schema fetch(final ResultSet resultSet) throws SQLException {
    return new Schema(resultSet.getString(fieldName));
  }
}
