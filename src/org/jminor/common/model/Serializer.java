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

  String serialize(final List<V> values) throws SerializeException;

  public class SerializeException extends Exception {
    public SerializeException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
