/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * A utility class for working with text
 */
public final class TextUtil {

  public enum Alignment {
    LEFT, RIGHT
  }

  private static final Random RANDOM = new Random();
  private static final String SPACE = " ";
  private static final String UNDERSCORE = "_";
  private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private TextUtil() {}

  /**
   * Parses an Integer from the given String.
   * A null or empty String results in null, "-" in -1.
   * @param text the text to parse
   * @return an Integer based on the given text
   */
  public static Integer getInt(final String text) {
    if (text == null) {
      return null;
    }

    final String noGrouping = text.replace(".", "").replace(",", "");
    if (noGrouping.length() == 0) {
      return null;
    }
    if ("-".equals(noGrouping)) {
      return  -1;
    }

    return Integer.parseInt(noGrouping);
  }

  /**
   * Parses a Double from the given String.
   * A null or empty String results in null, "-" in -1.
   * @param text the text to parse
   * @return a Double based on the given text
   */
  public static Double getDouble(final String text) {
    if (Util.nullOrEmpty(text)) {
      return null;
    }

    if ("-".equals(text)) {
      return -1d;
    }

    return Double.parseDouble(text.replace(',', '.'));
  }

  /**
   * Parses a Long from the given String.
   * A null or empty String results in null, "-" in -1.
   * @param text the text to parse
   * @return a Long based on the given text
   */
  public static Long getLong(final String text) {
    if (text == null) {
      return null;
    }

    final String noGrouping = text.replace(".", "").replace(",", "");
    if (noGrouping.length() == 0) {
      return null;
    }
    if ("-".equals(noGrouping)) {
      return  -1L;
    }

    return Long.parseLong(noGrouping);
  }

  /**
   * Creates a random string from alphanumeric uppercase characters
   * @param minLength the minimum length
   * @param maxLength the maximum length
   * @return a random string
   */
  public static String createRandomString(final int minLength, final int maxLength) {
    if (minLength > maxLength) {
      throw new IllegalArgumentException("Minimum length can not exceed maximum length");
    }
    final StringBuilder sb = new StringBuilder();
    final int length = minLength == maxLength ? minLength : RANDOM.nextInt(maxLength - minLength) + minLength;
    for( int i = 0; i < length; i++ ) {
      sb.append(AB.charAt(RANDOM.nextInt(AB.length())));
    }

    return sb.toString();
  }

  /**
   * Sorts the string representations of this lists contents, using
   * the space aware collator
   * @see TextUtil#getSpaceAwareCollator()
   * @param values the list to sort (collate)
   */
  public static void collate(final List values) {
    Collections.sort(values, getSpaceAwareCollator());
  }

  /**
   * @param <T> the comparison type
   * @return a Comparator which compares the string representations of the objects
   * using the default Collator, taking spaces into account.
   */
  public static <T> Comparator<T> getSpaceAwareCollator() {
    final Collator collator = Collator.getInstance();
    return (o1, o2) -> collateSansSpaces(collator, o1.toString(), o2.toString());
  }

  /**
   * Collates the contents of the list, replacing spaces with underscores before sorting
   * @param collator the collator
   * @param list the list
   */
  public static void collateSansSpaces(final Collator collator, final List<?> list) {
    Collections.sort(list, (o1, o2) -> collateSansSpaces(collator, o1.toString(), o2.toString()));
  }

  /**
   * Collates the given strings after replacing spaces with underscores
   * @param collator the collator to use
   * @param stringOne the first string
   * @param stringTwo the second string
   * @return the collation result
   */
  public static int collateSansSpaces(final Collator collator, final String stringOne, final String stringTwo) {
    Objects.requireNonNull(collator, "collator");
    Objects.requireNonNull(stringOne, "stringOne");
    Objects.requireNonNull(stringTwo, "stringTwo");

    return collator.compare(stringOne.replaceAll(SPACE, UNDERSCORE), stringTwo.replaceAll(SPACE, UNDERSCORE));
  }

  /**
   * Pads the given string with the given pad character until a length of <code>length</code> has been reached
   * @param string the string to pad
   * @param length the desired length
   * @param padChar the character to use for padding
   * @param alignment the padding alignment, left or right side
   * @return the padded string
   */
  public static String padString(final String string, final int length, final char padChar, final Alignment alignment) {
    Objects.requireNonNull(string, "string");
    if (string.length() >= length) {
      return string;
    }

    final StringBuilder stringBuilder = new StringBuilder(string);
    while (stringBuilder.length() < length) {
      if (alignment.equals(Alignment.LEFT)) {
        stringBuilder.insert(0, padChar);
      }
      else {
        stringBuilder.append(padChar);
      }
    }

    return stringBuilder.toString();
  }

  public static String getDelimitedString(final String[][] headers, final String[][] data, final String delimiter) {
    Objects.requireNonNull(headers, "headers");
    Objects.requireNonNull(data, "data");
    Objects.requireNonNull(delimiter, "delimiter");
    final StringBuilder contents = new StringBuilder();
    for (final String[] header : headers) {
      for (int j = 0; j < header.length; j++) {
        contents.append(header[j]);
        if (j < header.length - 1) {
          contents.append(delimiter);
        }
      }
      contents.append(Util.LINE_SEPARATOR);
    }

    for (int i = 0; i < data.length; i++) {
      final String[] line = data[i];
      for (int j = 0; j < line.length; j++) {
        contents.append(line[j]);
        if (j < line.length - 1) {
          contents.append(delimiter);
        }
      }
      if (i < data.length - 1) {
        contents.append(Util.LINE_SEPARATOR);
      }
    }

    return contents.toString();
  }

  /**
   * Fetch the entire contents of a resource text file, and return it in a String, using the default Charset.
   * @param resourceClass the resource class
   * @param resourceName the name of the resource to retrieve
   * @return the contents of the resource file
   * @throws IOException in case an IOException occurs
   */
  public static String getTextFileContents(final Class resourceClass, final String resourceName) throws IOException {
    return getTextFileContents(resourceClass, resourceName, Charset.defaultCharset());
  }

  /**
   * Fetch the entire contents of a resource textfile, and return it in a String.
   * @param resourceClass the resource class
   * @param resourceName the name of the resource to retrieve
   * @param charset the Charset to use when reading the file contents
   * @return the contents of the resource file
   * @throws IOException in case an IOException occurs
   */
  public static String getTextFileContents(final Class resourceClass, final String resourceName, final Charset charset) throws IOException {
    Objects.requireNonNull(resourceClass, "resourceClass");
    Objects.requireNonNull(resourceName, "resourceName");
    final InputStream inputStream = resourceClass.getResourceAsStream(resourceName);
    if (inputStream == null) {
      throw new FileNotFoundException("Resource not found: '" + resourceName + "'");
    }

    return getTextFileContents(inputStream, charset);
  }

  /**
   * Fetch the entire contents of a textfile, and return it in a String
   * @param filename the name of the file
   * @param charset the charset to use
   * @return the file contents as a String
   * @throws IOException in case of an exception
   */
  public static String getTextFileContents(final String filename, final Charset charset) throws IOException {
    Objects.requireNonNull(filename, "filename");
    return getTextFileContents(new FileInputStream(new File(filename)), charset);
  }

  /**
   * Fetch the entire contents of an InputStream, and return it in a String
   * @param inputStream the input stream to read
   * @param charset the charset to use
   * @return the stream contents as a String
   * @throws IOException in case of an exception
   */
  public static String getTextFileContents(final InputStream inputStream, final Charset charset) throws IOException {
    Objects.requireNonNull(inputStream, "inputStream");
    final StringBuilder contents = new StringBuilder();
    try (final BufferedReader input = new BufferedReader(new InputStreamReader(inputStream, charset))) {
      String line = input.readLine();
      while (line != null) {
        contents.append(line);
        contents.append(Util.LINE_SEPARATOR);
        line = input.readLine();
      }
    }

    return contents.toString();
  }

  /**
   * @param collection the collection
   * @param onePerLine if true then each item is put on a separate line, otherwise a comma separator is used
   * @return the collection contents as a string (using toString())
   */
  public static String getCollectionContentsAsString(final Collection<?> collection, final boolean onePerLine) {
    if (collection == null) {
      return "";
    }

    return getArrayContentsAsString(collection.toArray(), onePerLine);
  }

  /**
   * @param items the items
   * @param onePerLine if true then each item is put on a separate line, otherwise a comma separator is used
   * @return the array contents as a string (using toString())
   */
  public static String getArrayContentsAsString(final Object[] items, final boolean onePerLine) {
    if (items == null) {
      return "";
    }

    final StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < items.length; i++) {
      final Object item = items[i];
      if (item instanceof Object[]) {
        stringBuilder.append(getArrayContentsAsString((Object[]) item, onePerLine));
      }
      else if (!onePerLine) {
        stringBuilder.append(item).append(i < items.length - 1 ? ", " : "");
      }
      else {
        stringBuilder.append(item).append("\n");
      }
    }

    return stringBuilder.toString();
  }
}
