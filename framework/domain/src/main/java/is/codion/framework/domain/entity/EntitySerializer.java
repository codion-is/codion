/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * Handles serialisation of DefaultEntity and DefaultKey.
 */
interface EntitySerializer {

  Map<String, EntitySerializer> SERIALIZERS = new ConcurrentHashMap<>();

  void serialize(DefaultEntity entity, ObjectOutputStream stream) throws IOException;

  void deserialize(DefaultEntity entity, ObjectInputStream stream) throws IOException, ClassNotFoundException;

  void serialize(DefaultKey key, ObjectOutputStream stream) throws IOException;

  void deserialize(DefaultKey key, ObjectInputStream stream) throws IOException, ClassNotFoundException;

  static EntitySerializer getSerializer(String domainName) {
    EntitySerializer serializer = SERIALIZERS.get(requireNonNull(domainName));
    if (serializer == null) {
      throw new IllegalArgumentException("No EntitySerializer specified for domain: " + domainName);
    }

    return serializer;
  }

  static void setSerializer(String domainName, EntitySerializer serializer) {
    SERIALIZERS.put(requireNonNull(domainName), requireNonNull(serializer));
  }
}
