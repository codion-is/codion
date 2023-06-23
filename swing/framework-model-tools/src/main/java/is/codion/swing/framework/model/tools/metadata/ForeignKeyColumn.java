/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.metadata;

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
}
