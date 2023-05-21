/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

final class ImmutableEntity extends DefaultEntity implements Serializable {

  private static final long serialVersionUID = 1;

  private static final String ERROR_MESSAGE = "This entity instance is immutable";

  ImmutableEntity(DefaultEntity entity) {
    definition = entity.definition();
    values = new HashMap<>(entity.values);
    if (entity.originalValues != null) {
      originalValues = new HashMap<>(entity.originalValues);
    }
  }

  @Override
  public <T> T put(Attribute<T> attribute, T value) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public Entity clearPrimaryKey() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public void save(Attribute<?> attribute) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public void saveAll() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public void revert(Attribute<?> attribute) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public void revertAll() {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public <T> T remove(Attribute<T> attribute) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public Map<Attribute<?>, Object> setAs(Entity entity) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }
}
