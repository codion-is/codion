/*
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import is.codion.common.property.PropertyValue;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A utility class for working with text, such as sorting and reading from files
 */
public final class Text {

  /**
   * Specifies the default collator locale language.<br>
   * Value type: String<br>
   * Default value: {@code Locale.getDefault().getLanguage()}.
   * @see #collator()
   * @see #collate(List)
   * @see Locale#toLanguageTag()
   */
  public static final PropertyValue<String> DEFAULT_COLLATOR_LANGUAGE =
          Configuration.stringValue("codion.defaultCollatorLanguage", Locale.getDefault().getLanguage());

  /**
   * Specifies the wildcard character used<br>
   * Value type: Character<br>
   * Default value: %
   */
  public static final PropertyValue<Character> WILDCARD_CHARACTER = Configuration.characterValue("codion.wildcardCharacter", '%');

  private static final Random RANDOM = new Random();
  private static final char SPACE = ' ';
  private static final char UNDERSCORE = '_';
  private static final String ALPHA_NUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private Text() {}

  /**
   * Creates a random string from alphanumeric uppercase characters
   * @param minimumLength the minimum length
   * @param maximumLength the maximum length
   * @return a random string
   */
  public static String randomString(int minimumLength, int maximumLength) {
    if (minimumLength > maximumLength) {
      throw new IllegalArgumentException("Minimum length can not exceed maximum length");
    }
    int length = minimumLength == maximumLength ? minimumLength : RANDOM.nextInt(maximumLength - minimumLength) + minimumLength;

    return IntStream.range(0, length)
            .mapToObj(i -> String.valueOf(ALPHA_NUMERIC.charAt(RANDOM.nextInt(ALPHA_NUMERIC.length()))))
            .collect(joining());
  }

  /**
   * Sorts the string representations of the list contents, using the space aware collator
   * @param values the list to sort (collate)
   * @param <T> the list element type
   * @return the sorted list
   * @see Text#collator()
   */
  public static <T> List<T> collate(List<T> values) {
    requireNonNull(values).sort(collator());

    return values;
  }

  /**
   * Creates a Comparator which compares the string representations of the objects
   * using the default Collator, taking spaces into account.
   * @param <T> the type of the objects to compare
   * @return a space aware collator
   * @see #DEFAULT_COLLATOR_LANGUAGE
   */
  public static <T> Comparator<T> collator() {
    return collator(new Locale(DEFAULT_COLLATOR_LANGUAGE.get()));
  }

  /**
   * Creates a Comparator which compares the string representations of the objects
   * using the default Collator, taking spaces into account.
   * @param <T> the type of the objects to compare
   * @param locale the collator locale
   * @return a space aware collator
   * @see #DEFAULT_COLLATOR_LANGUAGE
   */
  public static <T> Comparator<T> collator(Locale locale) {
    return new SpaceAwareComparator<>(requireNonNull(locale));
  }

  /**
   * Right pads the given string with the given pad character until a length of {@code length} has been reached
   * @param string the string to pad
   * @param length the desired length
   * @param padChar the character to use for padding
   * @return the padded string
   */
  public static String rightPad(String string, int length, char padChar) {
    return padString(string, length, padChar, false);
  }

  /**
   * Left pads the given string with the given pad character until a length of {@code length} has been reached
   * @param string the string to pad
   * @param length the desired length
   * @param padChar the character to use for padding
   * @return the padded string
   */
  public static String leftPad(String string, int length, char padChar) {
    return padString(string, length, padChar, true);
  }

  /**
   * Creates a delimited string from the given input lists.
   * @param header the header
   * @param lines the lines
   * @param columnDelimiter the column delimiter
   * @return a String comprised of the given header and lines using the given column delimiter
   */
  public static String delimitedString(List<String> header, List<List<String>> lines, String columnDelimiter) {
    requireNonNull(header, "header");
    requireNonNull(lines, "lines");
    requireNonNull(columnDelimiter, "delimiter");
    StringBuilder contents = new StringBuilder();
    contents.append(String.join(columnDelimiter, header))
            .append(Separators.LINE_SEPARATOR)
            .append(lines.stream().map(line -> String.join(columnDelimiter, line))
                    .collect(joining(Separators.LINE_SEPARATOR)));

    return contents.toString();
  }

  /**
   * Fetch the entire contents of a resource text file, and return it in a String, using the default Charset.
   * @param resourceClass the resource class
   * @param resourceName the name of the resource to retrieve
   * @param <T> the resource class type
   * @return the contents of the resource file
   * @throws IOException in case an IOException occurs
   */
  public static <T> String textFileContents(Class<T> resourceClass, String resourceName) throws IOException {
    return textFileContents(resourceClass, resourceName, Charset.defaultCharset());
  }

  /**
   * Fetch the entire contents of a resource textfile, and return it in a String.
   * @param resourceClass the resource class
   * @param resourceName the name of the resource to retrieve
   * @param charset the Charset to use when reading the file contents
   * @param <T> the resource class type
   * @return the contents of the resource file
   * @throws IOException in case an IOException occurs
   */
  public static <T> String textFileContents(Class<T> resourceClass, String resourceName, Charset charset) throws IOException {
    requireNonNull(resourceClass, "resourceClass");
    requireNonNull(resourceName, "resourceName");
    InputStream inputStream = resourceClass.getResourceAsStream(resourceName);
    if (inputStream == null) {
      throw new FileNotFoundException("Resource not found: '" + resourceName + "'");
    }

    return textFileContents(inputStream, charset);
  }

  /**
   * Fetch the entire contents of a textfile, and return it in a String
   * @param filename the name of the file
   * @param charset the charset to use
   * @return the file contents as a String
   * @throws IOException in case of an exception
   */
  public static String textFileContents(String filename, Charset charset) throws IOException {
    requireNonNull(filename, "filename");

    return textFileContents(new File(filename), charset);
  }

  /**
   * Fetch the entire contents of a textfile, and return it in a String
   * @param file the file
   * @param charset the charset to use
   * @return the file contents as a String
   * @throws IOException in case of an exception
   */
  public static String textFileContents(File file, Charset charset) throws IOException {
    requireNonNull(file, "file");
    try (FileInputStream inputStream = new FileInputStream(file)) {
      return textFileContents(inputStream, charset);
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
  public static String textFileContents(InputStream inputStream, Charset charset) throws IOException {
    requireNonNull(inputStream, "inputStream");
    requireNonNull(charset, "charset");
    StringBuilder contents = new StringBuilder();
    try (BufferedReader input = new BufferedReader(new InputStreamReader(inputStream, charset))) {
      String line = input.readLine();
      while (line != null) {
        contents.append(line);
        line = input.readLine();
        if (line != null) {
          contents.append(Separators.LINE_SEPARATOR);
        }
      }
    }

    return contents.toString();
  }

  /**
   * Parses, splits and trims the given comma separated string.
   * Returns an empty list in case of null or empty string argument.
   * @param commaSeparatedValues a String with comma separated values
   * @return the trimmed values
   */
  public static List<String> parseCommaSeparatedValues(String commaSeparatedValues) {
    if (nullOrEmpty(commaSeparatedValues)) {
      return Collections.emptyList();
    }

    return Arrays.stream(commaSeparatedValues.split(","))
            .map(String::trim)
            .filter(string -> !string.isEmpty())
            .collect(Collectors.toList());
  }

  /**
   * Converts a string with underscores into a camelCaseString.
   * Just don't use this, it's far from bulletproof.
   * @param text the text
   * @return a camelCase version of the given text
   */
  public static String underscoreToCamelCase(String text) {
    if (!requireNonNull(text, "text").contains("_")) {
      return text;
    }
    StringBuilder builder = new StringBuilder();
    boolean firstDone = false;
    List<String> strings = Arrays.stream(text.toLowerCase().split("_"))
            .filter(string -> !string.isEmpty()).collect(Collectors.toList());
    if (strings.size() == 1) {
      return strings.get(0);
    }
    for (String split : strings) {
      if (!firstDone) {
        builder.append(Character.toLowerCase(split.charAt(0)));
        firstDone = true;
      }
      else {
        builder.append(Character.toUpperCase(split.charAt(0)));
      }
      if (split.length() > 1) {
        builder.append(split.substring(1).toLowerCase());
      }
    }

    return builder.toString();
  }

  private static String padString(String string, int length, char padChar, boolean left) {
    if (requireNonNull(string, "string").length() >= length) {
      return string;
    }

    StringBuilder stringBuilder = new StringBuilder(string);
    while (stringBuilder.length() < length) {
      if (left) {
        stringBuilder.insert(0, padChar);
      }
      else {
        stringBuilder.append(padChar);
      }
    }

    return stringBuilder.toString();
  }

  private static final class SpaceAwareComparator<T> implements Comparator<T>, Serializable {

    private static final long serialVersionUID = 1;

    private final Locale locale;

    private transient Collator collator;

    private SpaceAwareComparator(Locale locale) {
      this.locale = locale;
    }

    @Override
    public int compare(T o1, T o2) {
      return collator().compare(o1.toString().replace(SPACE, UNDERSCORE), o2.toString().replace(SPACE, UNDERSCORE));
    }

    private Collator collator() {
      if (collator == null) {
        collator = Collator.getInstance(this.locale);
      }

      return collator;
    }
  }
}
