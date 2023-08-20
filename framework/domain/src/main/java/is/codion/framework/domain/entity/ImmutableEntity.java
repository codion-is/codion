/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static is.codion.framework.domain.entity.DefaultKey.serializerForDomain;

final class ImmutableEntity extends DefaultEntity implements Serializable {

  private static final long serialVersionUID = 1;

  private static final String ERROR_MESSAGE = "This entity instance is immutable";

  ImmutableEntity(DefaultEntity entity) {
    definition = entity.definition();
    values = new HashMap<>(entity.values);
    values.forEach(new ReplaceWithImmutable(values));
    if (entity.originalValues != null) {
      originalValues = new HashMap<>(entity.originalValues);
      originalValues.forEach(new ReplaceWithImmutable(originalValues));
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
  public Map<Attribute<?>, Object> set(Entity entity) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.entityType().domainType().name());
    serializerForDomain(definition.entityType().domainType().name()).serialize(this, stream);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    serializerForDomain((String) stream.readObject()).deserialize(this, stream);
  }

  private static final class ReplaceWithImmutable implements BiConsumer<Attribute<?>, Object> {

    private final Map<Attribute<?>, Object> map;

    private ReplaceWithImmutable(Map<Attribute<?>, Object> map) {
      this.map = map;
    }

    @Override
    public void accept(Attribute<?> attribute, Object value) {
      if (value instanceof Entity && !(value instanceof ImmutableEntity)) {
        map.replace(attribute, ((Entity) value).immutable());
      }
    }
  }
}
