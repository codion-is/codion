/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
   * @param headers any headers to write first
   * @param data the data
   * @param delimiter the delimiter
   * @param file the file to write to
   * @throws IOException in case of an exception
   */
  public static void writeDelimitedFile(final String[][] headers, final String[][] data, final String delimiter,
                                        final File file) throws IOException {
    writeFile(TextUtil.getDelimitedString(headers, data, delimiter), file);
  }

  /**
   * @param contents the contents to write to the file, overwriting the contents
   * @param file the file
   * @throws IOException in case of an exception
   */
  public static void writeFile(final String contents, final File file) throws IOException {
    writeFile(contents, file, false);
  }

  /**
   * @param contents the contents to write to the file
   * @param file the file
   * @param append if true the contents are appended, otherwise overwritten
   * @throws IOException in case of an exception
   */
  public static void writeFile(final String contents, final File file, final boolean append) throws IOException {
    Objects.requireNonNull(contents, "contents");
    Objects.requireNonNull(file, "file");
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file, append))) {
      writer.write(contents);
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
   * Srializes a Collection of Objects to a given file
   * @param objects the objects to serialize
   * @param file the file
   * @throws IOException in case the file can not be written
   */
  public static void serializeToFile(final Collection objects, final File file) throws IOException {
    try (final ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file))) {
      for (final Object object : objects) {
        outputStream.writeObject(object);
      }
    }
  }

  /**
   * Reads the complete file into memory, so this is not meant for reading large files.
   * @param file the file
   * @return the bytes comprising the given file
   * @throws IOException in case of an exception
   */
  public static byte[] getBytesFromFile(final File file) throws IOException {
    Objects.requireNonNull(file, "file");
    try (final InputStream inputStream = new FileInputStream(file)) {
      final byte[] bytes = new byte[(int) file.length()];

      int offset = 0;
      int numRead = inputStream.read(bytes, offset, bytes.length - offset);
      while (offset < bytes.length && numRead >= 0) {
        offset += numRead;
        numRead = inputStream.read(bytes, offset, bytes.length - offset);
      }

      // Ensure all the bytes have been read in
      if (offset < bytes.length) {
        throw new IOException("Could not completely read file " + file.getName());
      }

      return bytes;
    }
  }
}
