/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

final class DefaultColumn<T> extends DefaultAttribute<T> implements Column<T> {

  private static final long serialVersionUID = 1;

  DefaultColumn(String name, Class<T> valueClass, EntityType entityType) {
    super(name, valueClass, entityType);
  }
}
