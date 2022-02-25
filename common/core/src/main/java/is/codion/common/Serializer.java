/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
  public static byte[] serialize(final Object object) throws IOException {
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
  public static <T> T deserialize(final byte[] bytes) throws IOException, ClassNotFoundException {
    if (bytes != null && bytes.length > 0) {
      return (T) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
    }

    return null;
  }

  /**
   * Deserializes a list of Objects from the given file
   * @param file the file
   * @param <T> the type of objects to read from the file
   * @return deserialized objects
   * @throws IOException in case the file can not be read
   * @throws ClassNotFoundException in case the deserialized class is not found
   */
  public static <T> List<T> deserializeFromFile(final File file) throws IOException, ClassNotFoundException {
    List<T> objects = new ArrayList<>();
    try (final ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
      while (true) {
        objects.add((T) inputStream.readObject());
      }
    }
    catch (EOFException ignored) {/*ignored*/}

    return objects;
  }

  /**
   * Serializes a Collection of Objects to a given file
   * @param objects the objects to serialize
   * @param file the file
   * @param <T> the value type
   * @throws IOException in case the file can not be written
   */
  public static <T> void serializeToFile(final Collection<T> objects, final File file) throws IOException {
    try (final ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file))) {
      for (final T object : objects) {
        outputStream.writeObject(object);
      }
    }
  }
}
