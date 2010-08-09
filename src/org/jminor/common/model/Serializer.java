/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.List;

/**
 * Serializes a list of values into a string.
 * @param <V> the type of the values being serialized
 */
public interface Serializer<V> {

  /**
   * Serializes the given values to a string
   * @param values the values to serialize
   * @return a string representing the given values
   * @throws SerializeException in case of an exception
   */
  String serialize(final List<V> values) throws SerializeException;

  class SerializeException extends Exception {
    public SerializeException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
