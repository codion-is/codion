/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common;

import dev.codion.common.value.PropertyValue;

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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * A utility class for working with text, such as sorting and reading from files
 */
public final class Text {

  /**
   * Specifies the default collator locale language.<br>
   * Value type: String<br>
   * Default value: {@code Locale.getDefault().getLanguage()}.
   * @see #getSpaceAwareCollator()
   * @see #collate(List)
   * @see Locale#toLanguageTag()
   */
  public static final PropertyValue<String> DEFAULT_COLLATOR_LANGUAGE =
          Configuration.stringValue("codion.defaultCollatorLanguage", Locale.getDefault().getLanguage());

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

  private Text() {}

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
    final int length = minLength == maxLength ? minLength : RANDOM.nextInt(maxLength - minLength) + minLength;

    return IntStream.range(0, length).mapToObj(i ->
            String.valueOf(ALPHA_NUMERIC.charAt(RANDOM.nextInt(ALPHA_NUMERIC.length()))))
            .collect(Collectors.joining());
  }

  /**
   * Sorts the string representations of this lists contents, using
   * the space aware collator
   * @see Text#getSpaceAwareCollator()
   * @param values the list to sort (collate)
   */
  public static void collate(final List values) {
    values.sort(getSpaceAwareCollator());
  }

  /**
   * Creates a Comparator which compares the string representations of the objects
   * using the default Collator, taking spaces into account.
   * @param <T> the type of the objects to compare
   * @return a space aware collator
   * @see #DEFAULT_COLLATOR_LANGUAGE
   */
  public static <T> Comparator<T> getSpaceAwareCollator() {
    return getSpaceAwareCollator(new Locale(DEFAULT_COLLATOR_LANGUAGE.get()));
  }

  /**
   * Creates a Comparator which compares the string representations of the objects
   * using the default Collator, taking spaces into account.
   * @param <T> the type of the objects to compare
   * @param locale the collator locale
   * @return a space aware collator
   * @see #DEFAULT_COLLATOR_LANGUAGE
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
   * Creates a delimited string from the given input lists.
   * @param header the header
   * @param lines the lines
   * @param columnDelimiter the column delimiter
   * @return a String comprised of the given header and lines using the given column delimiter
   */
  public static String getDelimitedString(final List<String> header, final List<List<String>> lines,
                                          final String columnDelimiter) {
    requireNonNull(header, "header");
    requireNonNull(lines, "lines");
    requireNonNull(columnDelimiter, "delimiter");
    final StringBuilder contents = new StringBuilder();
    contents.append(String.join(columnDelimiter, header)).append(Util.LINE_SEPARATOR)
            .append(lines.stream().map(line -> String.join(columnDelimiter, line))
                    .collect(Collectors.joining(Util.LINE_SEPARATOR)));

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
   * Parses and trims the given comma separated string.
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
