/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DefaultEntitySerializer implements EntitySerializer {

  private final Entities entities;
  private final boolean strictDeserialization;

  DefaultEntitySerializer(Entities entities, boolean strictDeserialization) {
    this.entities = requireNonNull(entities);
    this.strictDeserialization = strictDeserialization;
  }

  @Override
  public void serialize(DefaultEntity entity, ObjectOutputStream stream) throws IOException {
    stream.writeObject(entity.definition.type().name());
    serializeValues(entity, stream);
  }

  @Override
  public void deserialize(DefaultEntity entity, ObjectInputStream stream) throws IOException, ClassNotFoundException {
    entity.definition = entities.definition((String) stream.readObject());
    deserializeValues(entity, stream);
  }

  @Override
  public void serialize(DefaultKey key, ObjectOutputStream stream) throws IOException {
    stream.writeObject(key.definition.type().name());
    serializeValues(key, stream);
  }

  @Override
  public void deserialize(DefaultKey key, ObjectInputStream stream) throws IOException, ClassNotFoundException {
    key.definition = entities.definition((String) stream.readObject());
    deserializeValues(key, stream);
  }

  private void deserializeValues(DefaultEntity entity, ObjectInputStream stream) throws IOException, ClassNotFoundException {
    entity.values = deserializeValues(entity.definition, stream.readInt(), stream);
    if (stream.readBoolean()) {
      entity.originalValues = deserializeValues(entity.definition, stream.readInt(), stream);
    }
  }

  private Map<Attribute<?>, Object> deserializeValues(EntityDefinition definition, int valueCount,
                                                      ObjectInputStream stream) throws IOException, ClassNotFoundException {
    Map<Attribute<?>, Object> map = new HashMap<>(valueCount);
    for (int i = 0; i < valueCount; i++) {
      Attribute<Object> attribute = attributeByName(definition, (String) stream.readObject());
      Object value = stream.readObject();
      if (attribute != null) {
        map.put(attribute, attribute.validateType(value));
      }
    }

    return map;
  }

  private void deserializeValues(DefaultKey key, ObjectInputStream stream) throws IOException, ClassNotFoundException {
    key.primaryKey = stream.readBoolean();
    int valueCount = stream.readInt();
    key.attributes = new ArrayList<>(valueCount);
    key.values = new HashMap<>(valueCount);
    for (int i = 0; i < valueCount; i++) {
      Attribute<Object> attribute = attributeByName(key.definition, (String) stream.readObject());
      Object value = stream.readObject();
      if (attribute != null) {
        key.attributes.add(attribute);
        key.values.put(attribute, attribute.validateType(value));
      }
    }
    key.attributes = unmodifiableList(key.attributes);
    key.values = unmodifiableMap(key.values);
    key.singleIntegerKey = valueCount == 1 && isIntegerKey(key);
    key.hashCodeDirty = true;
  }

  private Attribute<Object> attributeByName(EntityDefinition definition, String attributeName) throws IOException {
    Attribute<Object> attribute = definition.attribute(attributeName);
    if (attribute == null && strictDeserialization) {
      throw new IOException("Attribute '" + attributeName + "' not found in entity '" + definition.type().name() + "'");
    }

    return attribute;
  }

  private static void serializeValues(DefaultEntity entity, ObjectOutputStream stream) throws IOException {
    serializeValues(entity.values, stream);
    boolean isModified = entity.originalValues != null;
    stream.writeBoolean(isModified);
    if (isModified) {
      serializeValues(entity.originalValues, stream);
    }
  }

  private static void serializeValues(Map<Attribute<?>, Object> valueMap, ObjectOutputStream stream) throws IOException {
    stream.writeInt(valueMap.size());
    for (Map.Entry<Attribute<?>, Object> entry : valueMap.entrySet()) {
      stream.writeObject(entry.getKey().name());
      stream.writeObject(entry.getValue());
    }
  }

  private static void serializeValues(DefaultKey key, ObjectOutputStream stream) throws IOException {
    stream.writeBoolean(key.primaryKey);
    stream.writeInt(key.attributes.size());
    for (int i = 0; i < key.attributes.size(); i++) {
      Attribute<?> attribute = key.attributes.get(i);
      stream.writeObject(attribute.name());
      stream.writeObject(key.values.get(attribute));
    }
  }

  private static boolean isIntegerKey(DefaultKey key) {
    return key.attributes.size() == 1 && key.attributes.get(0).isInteger();
  }
}