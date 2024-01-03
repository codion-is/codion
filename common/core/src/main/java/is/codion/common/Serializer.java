/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Utility class for serialization.
 */
public final class Serializer {

  private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  private Serializer() {}

  /**
   * Serializes the given Object, null object results in an empty byte array
   * @param object the object
   * @return a byte array representing the serialized object, an empty byte array in case of null
   * @throws IOException in case of an exception
   */
  public static byte[] serialize(Object object) throws IOException {
    if (object != null) {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      new ObjectOutputStream(byteArrayOutputStream).writeObject(object);

      return byteArrayOutputStream.toByteArray();
    }

    return EMPTY_BYTE_ARRAY;
  }

  /**
   * Deserializes the given byte array into a T, null or an empty byte array result in a null return value
   * @param bytes a byte array representing the serialized object
   * @param <T> the type of the object represented in the byte array
   * @return the deserialized object
   * @throws IOException in case of an exception
   * @throws ClassNotFoundException in case the deserialized class is not found
   */
  public static <T> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
    if (bytes != null && bytes.length > 0) {
      return (T) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
    }

    return null;
  }
}
