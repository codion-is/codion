/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import static java.util.Objects.requireNonNull;

/**
 * A database metadata column.
 */
public final class Column {

  private final String columnName;
  private final Class<?> columnTypeClass;
  private final int position;
  private final int columnSize;
  private final int decimalDigits;
  private final int nullable;
  private final boolean hasDefaultValue;
  private final String comment;
  private final int primaryKeyIndex;
  private final boolean foreignKeyColumn;

  Column(final String columnName, final Class<?> columnTypeClass, final int position, final int columnSize,
         final int decimalDigits, final int nullable, final boolean hasDefaultValue, final String comment,
         final int primaryKeyIndex, final boolean foreignKeyColumn) {
    this.columnName = requireNonNull(columnName);
    this.columnTypeClass = requireNonNull(columnTypeClass);
    this.position = position;
    this.columnSize = columnSize;
    this.decimalDigits = decimalDigits;
    this.nullable = nullable;
    this.hasDefaultValue = hasDefaultValue;
    this.comment = comment;
    this.primaryKeyIndex = primaryKeyIndex;
    this.foreignKeyColumn = foreignKeyColumn;
  }

  public String getColumnName() {
    return columnName;
  }

  public int getPosition() {
    return position;
  }

  public boolean isPrimaryKeyColumn() {
    return primaryKeyIndex != -1;
  }

  public int getPrimaryKeyIndex() {
    return primaryKeyIndex;
  }

  public boolean isForeignKeyColumn() {
    return foreignKeyColumn;
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
    Column column = (Column) object;

    return columnName.equals(column.columnName);
  }

  @Override
  public int hashCode() {
    return columnName.hashCode();
  }
}
