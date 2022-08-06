/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

public final class PrimaryKeyColumn {

  private final String columnName;
  private final int index;

  PrimaryKeyColumn(String columnName, int index) {
    this.columnName = columnName;
    this.index = index;
  }

  public String columnName() {
    return columnName;
  }

  public int index() {
    return index;
  }
}
