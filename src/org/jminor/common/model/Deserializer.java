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

  List<V> deserialize(final String values) throws DeserializeException;

  public class DeserializeException extends Exception {
    public DeserializeException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
