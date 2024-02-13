/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.metadata;

import static java.util.Objects.requireNonNull;

/**
 * A database metadata column.
 */
public final class MetadataColumn {

  private final String columnName;
  private final int dataType;
  private final String typeName;
  private final Class<?> columnClass;
  private final int position;
  private final int columnSize;
  private final int decimalDigits;
  private final int nullable;
  private final String defaultValue;
  private final String comment;
  private final int primaryKeyIndex;
  private final boolean foreignKeyColumn;
  private final boolean autoIncrement;
  private final boolean generated;

  MetadataColumn(String columnName, int dataType, String typeName, Class<?> columnClass, int position, int columnSize,
                 int decimalDigits, int nullable, String defaultValue, String comment,
                 int primaryKeyIndex, boolean foreignKeyColumn, boolean autoIncrement, boolean generated) {
    this.columnName = requireNonNull(columnName);
    this.columnClass = requireNonNull(columnClass);
    this.dataType = dataType;
    this.typeName = typeName;
    this.position = position;
    this.columnSize = columnSize;
    this.decimalDigits = decimalDigits;
    this.nullable = nullable;
    this.defaultValue = defaultValue;
    this.comment = comment == null ? null : comment.trim();
    this.primaryKeyIndex = primaryKeyIndex;
    this.foreignKeyColumn = foreignKeyColumn;
    this.autoIncrement = autoIncrement;
    this.generated = generated;
  }

  public String columnName() {
    return columnName;
  }

  public int dataType() {
    return dataType;
  }

  public String typeName() {
    return typeName;
  }

  public int position() {
    return position;
  }

  public boolean primaryKeyColumn() {
    return primaryKeyIndex != -1;
  }

  public int primaryKeyIndex() {
    return primaryKeyIndex;
  }

  public boolean foreignKeyColumn() {
    return foreignKeyColumn;
  }

  public Class<?> columnClass() {
    return columnClass;
  }

  public String defaultValue() {
    return defaultValue;
  }

  public int nullable() {
    return nullable;
  }

  public int columnSize() {
    return columnSize;
  }

  public int decimalDigits() {
    return decimalDigits;
  }

  public String comment() {
    return comment;
  }

  public boolean autoIncrement() {
    return autoIncrement;
  }

  public boolean generated() {
    return generated;
  }

  @Override
  public String toString() {
    return columnName;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    MetadataColumn column = (MetadataColumn) object;

    return columnName.equals(column.columnName);
  }

  @Override
  public int hashCode() {
    return columnName.hashCode();
  }
}
