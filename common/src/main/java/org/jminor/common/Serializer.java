/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.util.List;

/**
 * Serializes a list of values to and from a String.
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

  /**
   * Deserializes the given string into a list of values
   * @param values the string representing the values
   * @return a list containing the values in the same order they appear in the string
   * @throws SerializeException in case of an exception
   */
  List<V> deserialize(final String values) throws SerializeException;

  /**
   * An exception occurring during serialization
   */
  final class SerializeException extends Exception {

    /**
     * Instantiates a new SerializeException.
     * @param message the message
     * @param cause the root cause
     */
    public SerializeException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
