/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.framework.domain.entity.Attribute;

import static java.util.Objects.requireNonNull;

final class DefaultColumnPreferences implements EntityTableModel.ColumnPreferences {

  private final Attribute<?> attribute;
  private final int index;
  private final int width;

  DefaultColumnPreferences(Attribute<?> attribute, int index, int width) {
    this.attribute = requireNonNull(attribute);
    this.index = index;
    this.width = width;
  }

  @Override
  public Attribute<?> attribute() {
    return attribute;
  }

  @Override
  public int index() {
    return index;
  }

  @Override
  public boolean isVisible() {
    return index != -1;
  }

  @Override
  public int width() {
    return width;
  }
}
