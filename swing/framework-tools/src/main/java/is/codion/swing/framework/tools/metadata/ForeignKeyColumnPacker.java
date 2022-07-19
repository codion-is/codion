/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;

final class ForeignKeyColumnPacker implements ResultPacker<ForeignKeyColumn> {

  @Override
  public ForeignKeyColumn fetch(ResultSet resultSet) throws SQLException {
    String pktableSchem = resultSet.getString("PKTABLE_SCHEM");
    if (pktableSchem == null) {
      pktableSchem = resultSet.getString("PKTABLE_CAT");
    }
    String fktableSchem = resultSet.getString("FKTABLE_SCHEM");
    if (fktableSchem == null) {
      fktableSchem = resultSet.getString("FKTABLE_CAT");
    }
    return new ForeignKeyColumn(pktableSchem,
            resultSet.getString("PKTABLE_NAME"),
            resultSet.getString("PKCOLUMN_NAME"),
            resultSet.getString("FKTABLE_NAME"),
            fktableSchem,
            resultSet.getString("FKCOLUMN_NAME"),
            resultSet.getInt("KEY_SEQ"));
  }
}
