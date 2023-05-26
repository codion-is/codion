/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Handles serialisation of {@link DefaultEntity} and {@link DefaultKey}.
 */
interface EntitySerializer {

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
}
