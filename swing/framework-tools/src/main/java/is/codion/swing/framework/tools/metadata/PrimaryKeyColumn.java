/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

public final class PrimaryKeyColumn {

  private final String columnName;
  private final int keySeq;

  PrimaryKeyColumn(final String columnName, final int keySeq) {
    this.columnName = columnName;
    this.keySeq = keySeq;
  }

  public String getColumnName() {
    return columnName;
  }

  public int getKeySeq() {
    return keySeq;
  }
}
