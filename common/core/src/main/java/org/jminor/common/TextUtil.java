/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.jminor.common.value.PropertyValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * A utility class for working with text, such as sorting a reading from files
 */
public final class TextUtil {

  /**
   * Specifies the default collator locale language.<br>
   * Value type: String<br>
   * Default value: {@code Locale.getDefault().getLanguage()}.
   * @see #getSpaceAwareCollator()
   * @see #collate(List)
   * @see Locale#toLanguageTag()
   */
  public static final PropertyValue<String> DEFAULT_COLLATOR_LANGUAGE =
          Configuration.stringValue("jminor.defaultCollatorLanguage", Locale.getDefault().getLanguage());

  /**
   * Left or right alignment
   */
  public enum Alignment {
    LEFT, RIGHT
  }

  private static final Random RANDOM = new Random();
  private static final String SPACE = " ";
  private static final String UNDERSCORE = "_";
  private static final String ALPHA_NUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

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
      return -1;
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
    if (nullOrEmpty(text)) {
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
      return -1L;
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
    for (int i = 0; i < length; i++) {
      sb.append(ALPHA_NUMERIC.charAt(RANDOM.nextInt(ALPHA_NUMERIC.length())));
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
    values.sort(getSpaceAwareCollator());
  }

  /**
   * @param <T> the comparison type
   * @return a Comparator which compares the string representations of the objects
   * using the default Collator, taking spaces into account.
   * @see #DEFAULT_COLLATOR_LANGUAGE
   */
  public static <T> Comparator<T> getSpaceAwareCollator() {
    return getSpaceAwareCollator(new Locale(DEFAULT_COLLATOR_LANGUAGE.get()));
  }

  /**
   * @param <T> the comparison type
   * @param locale the collator locale
   * @return a Comparator which compares the string representations of the objects
   * using the default Collator, taking spaces into account.
   */
  public static <T> Comparator<T> getSpaceAwareCollator(final Locale locale) {
    return new ComparatorSansSpace<>(locale);
  }

  /**
   * Collates the contents of the list, replacing spaces with underscores before sorting
   * @param collator the collator
   * @param list the list
   */
  public static void collateSansSpaces(final Collator collator, final List list) {
    list.sort((o1, o2) -> collateSansSpaces(collator, o1.toString(), o2.toString()));
  }

  /**
   * Collates the given strings after replacing spaces with underscores
   * @param collator the collator to use
   * @param stringOne the first string
   * @param stringTwo the second string
   * @return the collation result
   */
  public static int collateSansSpaces(final Collator collator, final String stringOne, final String stringTwo) {
    requireNonNull(collator, "collator");
    requireNonNull(stringOne, "stringOne");
    requireNonNull(stringTwo, "stringTwo");

    return collator.compare(stringOne.replaceAll(SPACE, UNDERSCORE), stringTwo.replaceAll(SPACE, UNDERSCORE));
  }

  /**
   * Pads the given string with the given pad character until a length of {@code length} has been reached
   * @param string the string to pad
   * @param length the desired length
   * @param padChar the character to use for padding
   * @param alignment the padding alignment, left or right side
   * @return the padded string
   */
  public static String padString(final String string, final int length, final char padChar, final Alignment alignment) {
    requireNonNull(string, "string");
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

  /**
   * @param headers the headers
   * @param data the data
   * @param delimiter the delimiter
   * @return a String comprised of the given headers and data with the given delimiter
   */
  public static String getDelimitedString(final String[][] headers, final String[][] data, final String delimiter) {
    requireNonNull(headers, "headers");
    requireNonNull(data, "data");
    requireNonNull(delimiter, "delimiter");
    final StringBuilder contents = new StringBuilder();
    for (final String[] header : headers) {
      contents.append(String.join(delimiter, header)).append(Util.LINE_SEPARATOR);
    }
    for (final String[] line : data) {
      contents.append(String.join(delimiter, line)).append(Util.LINE_SEPARATOR);
    }
    //remove the last line separator
    contents.replace(contents.length() - Util.LINE_SEPARATOR.length(), contents.length(), "");

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
    requireNonNull(resourceClass, "resourceClass");
    requireNonNull(resourceName, "resourceName");
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
    requireNonNull(filename, "filename");

    return getTextFileContents(new File(filename), charset);
  }

  /**
   * Fetch the entire contents of a textfile, and return it in a String
   * @param file the file
   * @param charset the charset to use
   * @return the file contents as a String
   * @throws IOException in case of an exception
   */
  public static String getTextFileContents(final File file, final Charset charset) throws IOException {
    requireNonNull(file, "file");
    try (final FileInputStream inputStream = new FileInputStream(file)) {
      return getTextFileContents(inputStream, charset);
    }
  }

  /**
   * Fetch the entire contents of an InputStream, and return it in a String.
   * Does not close the stream.
   * @param inputStream the input stream to read
   * @param charset the charset to use
   * @return the stream contents as a String
   * @throws IOException in case of an exception
   */
  public static String getTextFileContents(final InputStream inputStream, final Charset charset) throws IOException {
    requireNonNull(inputStream, "inputStream");
    final StringBuilder contents = new StringBuilder();
    try (final BufferedReader input = new BufferedReader(new InputStreamReader(inputStream, charset))) {
      String line = input.readLine();
      while (line != null) {
        contents.append(line);
        line = input.readLine();
        if (line != null) {
          contents.append(Util.LINE_SEPARATOR);
        }
      }
    }

    return contents.toString();
  }

  /**
   * @param commaSeparatedValues a String with comma separated values
   * @return the trimmed values
   */
  public static List<String> parseCommaSeparatedValues(final String commaSeparatedValues) {
    final List<String> values = new ArrayList<>();
    if (!nullOrEmpty(commaSeparatedValues)) {
      final String[] strings = commaSeparatedValues.split(",");
      for (final String value : strings) {
        values.add(value.trim());
      }
    }

    return values;
  }

  private static final class ComparatorSansSpace<T> implements Comparator<T>, Serializable {

    private static final long serialVersionUID = 1;

    private final Locale locale;

    private transient Collator collator;

    private ComparatorSansSpace(final Locale locale) {
      this.locale = locale;
    }

    @Override
    public int compare(final T o1, final T o2) {
      return collateSansSpaces(getCollator(), o1.toString(), o2.toString());
    }

    private Collator getCollator() {
      if (collator == null) {
        collator = Collator.getInstance(this.locale);
      }

      return collator;
    }
  }
}
