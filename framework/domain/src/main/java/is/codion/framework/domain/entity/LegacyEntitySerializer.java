/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.Property;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class LegacyEntitySerializer implements EntitySerializer {

  private final Entities entities;

  LegacyEntitySerializer(Entities entities) {
    this.entities = requireNonNull(entities);
  }

  @Override
  public void serialize(DefaultEntity entity, ObjectOutputStream stream) throws IOException {
    EntityDefinition definition = entity.definition;
    stream.writeObject(definition.type().name());
    stream.writeInt(definition.serializationVersion());
    boolean isModified = entity.originalValues != null && !entity.originalValues.isEmpty();
    stream.writeBoolean(isModified);
    List<Property<?>> properties = definition.properties();
    for (int i = 0; i < properties.size(); i++) {
      Property<?> property = properties.get(i);
      if (!(property instanceof DerivedProperty)) {
        Attribute<?> attribute = property.attribute();
        boolean containsValue = entity.values.containsKey(attribute);
        stream.writeBoolean(containsValue);
        if (containsValue) {
          stream.writeObject(entity.values.get(attribute));
          if (isModified) {
            boolean valueModified = entity.originalValues.containsKey(attribute);
            stream.writeBoolean(valueModified);
            if (valueModified) {
              stream.writeObject(entity.originalValues.get(attribute));
            }
          }
        }
      }
    }
  }

  @Override
  public void deserialize(DefaultEntity entity, ObjectInputStream stream) throws IOException, ClassNotFoundException {
    entity.definition = entities.definition((String) stream.readObject());
    if (entity.definition.serializationVersion() != stream.readInt()) {
      throw new IllegalArgumentException("Entity type '" + entity.definition.type() + "' can not be deserialized due to version difference");
    }
    boolean isModified = stream.readBoolean();
    entity.values = new HashMap<>();
    List<Property<?>> properties = entity.definition.properties();
    for (int i = 0; i < properties.size(); i++) {
      Property<Object> property = (Property<Object>) properties.get(i);
      if (!(property instanceof DerivedProperty)) {
        boolean containsValue = stream.readBoolean();
        if (containsValue) {
          Attribute<Object> attribute = property.attribute();
          entity.values.put(attribute, attribute.validateType(stream.readObject()));
          if (isModified && stream.readBoolean()) {
            setOriginalValue(entity, attribute, attribute.validateType(stream.readObject()));
          }
        }
      }
    }
  }

  @Override
  public void serialize(DefaultKey key, ObjectOutputStream stream) throws IOException {
    stream.writeObject(key.definition.type().name());
    stream.writeInt(key.definition.serializationVersion());
    stream.writeBoolean(key.primaryKey);
    stream.writeInt(key.attributes.size());
    for (int i = 0; i < key.attributes.size(); i++) {
      Attribute<?> attribute = key.attributes.get(i);
      stream.writeObject(attribute.name());
      stream.writeObject(key.values.get(attribute));
    }
  }

  @Override
  public void deserialize(DefaultKey key, ObjectInputStream stream) throws IOException, ClassNotFoundException {
    key.definition = entities.definition((String) stream.readObject());
    if (key.definition.serializationVersion() != stream.readInt()) {
      throw new IllegalArgumentException("Entity type '" + key.definition.type() + "' can not be deserialized due to version difference");
    }
    key.primaryKey = stream.readBoolean();
    int attributeCount = stream.readInt();
    key.attributes = new ArrayList<>(attributeCount);
    key.values = new HashMap<>(attributeCount);
    for (int i = 0; i < attributeCount; i++) {
      Attribute<Object> attribute = key.definition.attribute((String) stream.readObject());
      key.attributes.add(attribute);
      key.values.put(attribute, attribute.validateType(stream.readObject()));
    }
    key.singleIntegerKey = attributeCount == 1 && key.attributes.get(0).isInteger();
    key.hashCodeDirty = true;
  }

  private static <T> void setOriginalValue(DefaultEntity entity, Attribute<T> attribute, T originalValue) {
    if (entity.originalValues == null) {
      entity.originalValues = new HashMap<>();
    }
    entity.originalValues.put(attribute, originalValue);
  }
}
