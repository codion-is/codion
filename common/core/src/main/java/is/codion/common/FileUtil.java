/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A utility class for working with files
 */
public final class FileUtil {

  private FileUtil() {}

  /**
   * @param filename the name of the file
   * @return the number of lines in the given file
   * @throws java.io.IOException in case the file can not be read
   */
  public static int countLines(final String filename) throws IOException {
    return countLines(new File(filename), null);
  }

  /**
   * @param file the file
   * @return the number of lines in the given file
   * @throws IOException in case the file can not be read
   */
  public static int countLines(final File file) throws IOException {
    return countLines(file, null);
  }

  /**
   * @param file the file
   * @param excludePrefix lines are excluded from the count if they start with this string
   * @return the number of lines in the given file
   * @throws IOException in case the file can not be read
   */
  public static int countLines(final File file, final String excludePrefix) throws IOException {
    try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
      int lines = 0;
      String line = reader.readLine();
      while (line != null) {
        if (excludePrefix == null || !line.startsWith(excludePrefix)) {
          lines++;
        }
        line = reader.readLine();
      }

      return lines;
    }
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
    final List<T> objects = new ArrayList<>();
    try (final ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
      while (true) {
        objects.add((T) inputStream.readObject());
      }
    }
    catch (final EOFException ignored) {/*ignored*/}

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
