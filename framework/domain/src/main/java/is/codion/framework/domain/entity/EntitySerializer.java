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
 * Handles serialisation of {@link DefaultEntity} and {@link DefaultKey}.
 */
interface EntitySerializer {

  Map<String, EntitySerializer> SERIALIZERS = new ConcurrentHashMap<>();

  /**
   * Serializes the given entity to the given output stream
   * @param entity the entity
   * @param stream the output stream
   * @throws IOException in case of an exception
   */
  void serialize(DefaultEntity entity, ObjectOutputStream stream) throws IOException;

  /**
   * Populates the given entity with the deserialized entity from the given input stream
   * @param entity the entity
   * @param stream the input stream
   * @throws IOException in case of an exception
   */
  void deserialize(DefaultEntity entity, ObjectInputStream stream) throws IOException, ClassNotFoundException;

  /**
   * Serializes the given key to the given output stream
   * @param key the key
   * @param stream the output stream
   * @throws IOException in case of an exception
   */
  void serialize(DefaultKey key, ObjectOutputStream stream) throws IOException;

  /**
   * Populates the given key with the deserialized key from the given input stream
   * @param key the keyh
   * @param stream the input stream
   * @throws IOException in case of an exception
   */
  void deserialize(DefaultKey key, ObjectInputStream stream) throws IOException, ClassNotFoundException;

  /**
   * Returns the serializer associated with the given domain name
   * @param domainName the domain name
   * @return the serializer to use for the given domain
   * @throws IllegalArgumentException in case no serialiser has been associated with the domain
   */
  static EntitySerializer getSerializer(String domainName) {
    EntitySerializer serializer = SERIALIZERS.get(requireNonNull(domainName));
    if (serializer == null) {
      throw new IllegalArgumentException("No EntitySerializer specified for domain: " + domainName);
    }

    return serializer;
  }

  /**
   * Sets the given serializer for the given domain name
   * @param domainName the domain name
   * @param serializer the serializer to use for the given domain
   */
  static void setSerializer(String domainName, EntitySerializer serializer) {
    SERIALIZERS.put(requireNonNull(domainName), requireNonNull(serializer));
  }
}
