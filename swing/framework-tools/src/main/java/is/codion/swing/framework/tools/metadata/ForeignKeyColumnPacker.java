/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;

final class ForeignKeyColumnPacker implements ResultPacker<ForeignKeyColumn> {

  @Override
  public ForeignKeyColumn fetch(final ResultSet resultSet) throws SQLException {
    return new ForeignKeyColumn(resultSet.getString("PKTABLE_SCHEM"), resultSet.getString("PKTABLE_NAME"),
            resultSet.getString("PKCOLUMN_NAME"), resultSet.getString("FKTABLE_NAME"),
            resultSet.getString("FKTABLE_SCHEM"), resultSet.getString("FKCOLUMN_NAME"));
  }
}
