/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.List;

/**
 * Deserializes a string into a list of values
 * @param <V> the type of values being deserialized
 */
public interface Deserializer<V> {

  /**
   * Deserializes the given string into a list of values
   * @param values the string representing the values
   * @return a list containing the values in the same order they appear in the string
   * @throws DeserializeException in case of an exception
   */
  List<V> deserialize(final String values) throws DeserializeException;

  /**
   * An exception occurring during deserialization
   */
  class DeserializeException extends Exception {

    /**
     * Instantiates a new DeserializeException.
     * @param message the message
     * @param cause the root cause
     */
    public DeserializeException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
