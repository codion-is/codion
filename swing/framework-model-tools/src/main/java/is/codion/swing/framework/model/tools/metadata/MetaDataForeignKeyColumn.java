/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class MetaDataForeignKeyColumn {

  private final String pkSchemaName;
  private final String pkTableName;
  private final String pkColumnName;
  private final String fkTableName;
  private final String fkSchemaName;
  private final String fkColumnName;
  private final int keySeq;

  MetaDataForeignKeyColumn(String pkSchemaName, String pkTableName, String pkColumnName,
                           String fkTableName, String fkSchemaName, String fkColumnName,
                           int keySeq) {
    this.pkSchemaName = pkSchemaName;
    this.pkTableName = pkTableName;
    this.pkColumnName = pkColumnName;
    this.fkTableName = fkTableName;
    this.fkSchemaName = fkSchemaName;
    this.fkColumnName = fkColumnName;
    this.keySeq = keySeq;
  }

  public String pkSchemaName() {
    return pkSchemaName;
  }

  public String pkTableName() {
    return pkTableName;
  }

  public String pkColumnName() {
    return pkColumnName;
  }

  public String fkTableName() {
    return fkTableName;
  }

  public String fkSchemaName() {
    return fkSchemaName;
  }

  public String fkColumnName() {
    return fkColumnName;
  }

  public int keySeq() {
    return keySeq;
  }

  static final class ForeignKeyColumnPacker implements ResultPacker<MetaDataForeignKeyColumn> {

    @Override
    public MetaDataForeignKeyColumn get(ResultSet resultSet) throws SQLException {
      String pktableSchem = resultSet.getString("PKTABLE_SCHEM");
      if (pktableSchem == null) {
        pktableSchem = resultSet.getString("PKTABLE_CAT");
      }
      String fktableSchem = resultSet.getString("FKTABLE_SCHEM");
      if (fktableSchem == null) {
        fktableSchem = resultSet.getString("FKTABLE_CAT");
      }
      return new MetaDataForeignKeyColumn(pktableSchem,
              resultSet.getString("PKTABLE_NAME"),
              resultSet.getString("PKCOLUMN_NAME"),
              resultSet.getString("FKTABLE_NAME"),
              fktableSchem,
              resultSet.getString("FKCOLUMN_NAME"),
              resultSet.getInt("KEY_SEQ"));
    }
  }
}
