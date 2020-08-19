/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import java.util.Objects;

/**
 * A database metadata column.
 */
public final class Column {

  private final String columnName;
  private final Class<?> columnTypeClass;
  private final int columnSize;
  private final int decimalDigits;
  private final int nullable;
  private final boolean hasDefaultValue;
  private final String comment;
  private final int keySeq;

  Column(final String columnName, final Class<?> columnTypeClass, final int columnSize, final int decimalDigits,
         final int nullable, final boolean hasDefaultValue, final String comment, final int keySeq) {
    this.columnName = columnName;
    this.columnTypeClass = columnTypeClass;
    this.columnSize = columnSize;
    this.decimalDigits = decimalDigits;
    this.nullable = nullable;
    this.hasDefaultValue = hasDefaultValue;
    this.comment = comment;
    this.keySeq = keySeq;
  }

  public String getColumnName() {
    return columnName;
  }

  public int getKeySeq() {
    return keySeq;
  }

  public Class<?> getColumnTypeClass() {
    return columnTypeClass;
  }

  public boolean hasDefaultValue() {
    return hasDefaultValue;
  }

  public int getNullable() {
    return nullable;
  }

  public int getColumnSize() {
    return columnSize;
  }

  public int getDecimalDigits() {
    return decimalDigits;
  }

  public String getComment() {
    return comment;
  }

  @Override
  public String toString() {
    return columnName;
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    final Column column = (Column) object;

    return columnName.equals(column.columnName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columnName);
  }
}
