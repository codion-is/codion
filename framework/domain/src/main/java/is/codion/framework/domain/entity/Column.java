/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

public interface Column<T> extends Attribute<T> {

  /**
   * Creates a new {@link Column}, associated with the given entityType.
   * @param entityType the entityType owning this column
   * @param name the column name
   * @param valueClass the class representing the column value type
   * @param <T> the column type
   * @return a new {@link Column}
   */
  static <T> Column<T> column(EntityType entityType, String name, Class<T> valueClass) {
    return new DefaultColumn<>(name, valueClass, entityType);
  }
}
