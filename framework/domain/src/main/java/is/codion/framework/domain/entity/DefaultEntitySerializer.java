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

import static java.util.Objects.requireNonNull;

final class DefaultEntitySerializer implements EntitySerializer {

  private final Entities entities;

  DefaultEntitySerializer(Entities entities) {
    this.entities = requireNonNull(entities);
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
    key.primaryKey = stream.readBoolean();
    int valueCount = stream.readInt();
    key.attributes = new ArrayList<>(valueCount);
    key.values = new HashMap<>(valueCount);
    for (int i = 0; i < valueCount; i++) {
      Attribute<Object> attribute = key.definition.attribute((String) stream.readObject());
      key.attributes.add(attribute);
      key.values.put(attribute, attribute.validateType(stream.readObject()));
    }
    key.singleIntegerKey = valueCount == 1 && key.attributes.get(0).isInteger();
    key.hashCodeDirty = true;
  }

  private static void serializeValues(DefaultEntity entity, ObjectOutputStream stream) throws IOException {
    serializeValues(entity.values, stream);
    boolean isModified = entity.originalValues != null;
    stream.writeBoolean(isModified);
    if (isModified) {
      serializeValues(entity.originalValues, stream);
    }
  }

  private static void deserializeValues(DefaultEntity entity, ObjectInputStream stream) throws IOException, ClassNotFoundException {
    entity.values = deserializeValues(entity.definition, stream.readInt(), stream);
    if (stream.readBoolean()) {
      entity.originalValues = deserializeValues(entity.definition, stream.readInt(), stream);
    }
  }

  private static void serializeValues(Map<Attribute<?>, Object> valueMap, ObjectOutputStream stream) throws IOException {
    stream.writeInt(valueMap.size());
    for (Map.Entry<Attribute<?>, Object> entry : valueMap.entrySet()) {
      stream.writeObject(entry.getKey().name());
      stream.writeObject(entry.getValue());
    }
  }

  private static Map<Attribute<?>, Object> deserializeValues(EntityDefinition definition, int valueCount,
                                                             ObjectInputStream stream) throws IOException, ClassNotFoundException {
    Map<Attribute<?>, Object> map = new HashMap<>(valueCount);
    for (int i = 0; i < valueCount; i++) {
      Attribute<Object> attribute = definition.attribute((String) stream.readObject());
      if (attribute != null) {
        map.put(attribute, attribute.validateType(stream.readObject()));
      }
    }

    return map;
  }
}
