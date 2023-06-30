/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;

final class SchemaPacker implements ResultPacker<Schema> {

  private final String fieldName;

  SchemaPacker(String fieldName) {
    this.fieldName = fieldName;
  }

  @Override
  public Schema fetch(ResultSet resultSet) throws SQLException {
    String name = resultSet.getString(fieldName);

    return name == null ? null : new Schema(name);
  }
}