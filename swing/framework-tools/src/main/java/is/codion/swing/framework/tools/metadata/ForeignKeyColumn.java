/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

public final class ForeignKeyColumn {

  private final String pkSchemaName;
  private final String pkTableName;
  private final String pkColumnName;
  private final String fkTableName;
  private final String fkSchemaName;
  private final String fkColumnName;
  private final int keySeq;

  ForeignKeyColumn(String pkSchemaName, String pkTableName, String pkColumnName,
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

  public String getPkSchemaName() {
    return pkSchemaName;
  }

  public String getPkTableName() {
    return pkTableName;
  }

  public String getPkColumnName() {
    return pkColumnName;
  }

  public String getFkTableName() {
    return fkTableName;
  }

  public String getFkSchemaName() {
    return fkSchemaName;
  }

  public String getFkColumnName() {
    return fkColumnName;
  }

  public int getKeySeq() {
    return keySeq;
  }
}
