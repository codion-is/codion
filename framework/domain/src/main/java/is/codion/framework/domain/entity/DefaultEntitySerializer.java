/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

final class DefaultEntitySerializer implements EntitySerializer {

  @Override
  public void serialize(DefaultEntity entity, ObjectOutputStream stream) throws IOException {
    EntityDefinition definition = entity.definition;
    stream.writeObject(definition.domainName());
    stream.writeObject(definition.type().name());
    serializeValues(entity, stream);
  }

  @Override
  public void deserialize(DefaultEntity entity, ObjectInputStream stream) throws IOException, ClassNotFoundException {
    Entities entities = DefaultEntities.entities((String) stream.readObject());
    entity.definition = entities.definition((String) stream.readObject());
    deserializeValues(entity, stream);
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
