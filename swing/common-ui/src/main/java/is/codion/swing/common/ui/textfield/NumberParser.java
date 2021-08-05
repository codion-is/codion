/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;

import static java.util.Objects.requireNonNull;

class NumberParser<T extends Number> implements Parser<T> {

  private static final String MINUS_SIGN = "-";

  private final NumberFormat format;
  private final Class<T> clazz;

  protected NumberParser(final NumberFormat format, final Class<T> clazz) {
    this.format = requireNonNull(format, "format");
    this.format.setRoundingMode(RoundingMode.DOWN);
    this.clazz = requireNonNull(clazz, "clazz");
  }

  @Override
  public NumberParseResult<T> parse(final String string) {
    if (string.isEmpty() || MINUS_SIGN.equals(string)) {
      return new DefaultNumberParseResult<>(string, null);
    }

    final T parsedNumber = parseNumber(string);
    if (parsedNumber != null) {
      String formattedNumber = format.format(parsedNumber);
      //handle trailing decimal symbol and trailing decimal zeros
      if (format instanceof DecimalFormat) {
        final String decimalSeparator =
                String.valueOf(((DecimalFormat) format).getDecimalFormatSymbols().getDecimalSeparator());
        if (!formattedNumber.contains(decimalSeparator) && string.endsWith(decimalSeparator)) {
          formattedNumber += decimalSeparator;
        }
        final int decimalSeparatorIndex = string.indexOf(decimalSeparator);
        if (decimalSeparatorIndex >= 0 && string.substring(decimalSeparatorIndex).endsWith("0")) {
          formattedNumber += (formattedNumber.contains(decimalSeparator) ? "" : decimalSeparator) +
                  getTrailingDecimalZeros(string, decimalSeparatorIndex);
        }
      }

      return new DefaultNumberParseResult<>(formattedNumber, parsedNumber, countAddedGroupingSeparators(string, formattedNumber), true);
    }

    return new DefaultNumberParseResult<>(string, null, 0, false);
  }

  /**
   * @return the underlying format
   */
  protected final NumberFormat getFormat() {
    return format;
  }

  /**
   * @param text the text to parse
   * @return a number if the format can parse it, null otherwise
   */
  private T parseNumber(final String text) {
    if (text.isEmpty()) {
      return null;
    }

    final ParsePosition position = new ParsePosition(0);
    final T number = (T) format.parse(text, position);
    if (position.getIndex() != text.length() || position.getErrorIndex() != -1) {
      return null;
    }

    return toType(number);
  }

  private T toType(final T number) {
    if (clazz.equals(Integer.class)) {
      return toInteger(number);
    }
    else if (clazz.equals(Long.class)) {
      return toLong(number);
    }
    else if (clazz.equals(Double.class)) {
      return toDouble(number);
    }
    else if (clazz.equals(BigDecimal.class)) {
      return toBigDecimal(number);
    }

    throw new IllegalStateException("Unsupported type class: " + clazz);
  }

  private T toInteger(final T number) {
    if (number instanceof Integer) {
      return number;
    }

    return (T) Integer.valueOf(number.intValue());
  }

  private T toLong(final T number) {
    if (number instanceof Long) {
      return number;
    }

    return (T) Long.valueOf(number.longValue());
  }

  private T toDouble(final T number) {
    if (number instanceof Double) {
      return number;
    }

    return (T) Double.valueOf(number.doubleValue());
  }

  private T toBigDecimal(final T number) {
    if (number instanceof BigDecimal) {
      return number;
    }

    return (T) BigDecimal.valueOf(number.doubleValue());
  }

  private int countAddedGroupingSeparators(final String currentNumber, final String newNumber) {
    final DecimalFormatSymbols symbols = ((DecimalFormat) format).getDecimalFormatSymbols();

    return count(newNumber, symbols.getGroupingSeparator()) - count(currentNumber, symbols.getGroupingSeparator());
  }

  private static String getTrailingDecimalZeros(final String string, final int decimalSeparatorIndex) {
    final StringBuilder builder = new StringBuilder();
    int index = string.length() - 1;
    char c = string.charAt(index);
    while (c == '0' && index > decimalSeparatorIndex) {
      builder.append('0');
      c = string.charAt(--index);
    }

    return builder.toString();
  }

  private static int count(final String string, final char groupingSeparator) {
    int counter = 0;
    for (final char c : string.toCharArray()) {
      if (c == groupingSeparator) {
        counter++;
      }
    }

    return counter;
  }

  protected interface NumberParseResult<T extends Number> extends ParseResult<T> {

    /**
     * @return the number of characters added
     */
    int getCharetOffset();
  }

  protected static final class DefaultNumberParseResult<T extends Number>
          extends DefaultParseResult<T> implements NumberParseResult<T> {

    private final int charetOffset;

    private DefaultNumberParseResult(final String text, final T value) {
      this(text, value, 0, true);
    }

    DefaultNumberParseResult(final String text, final T value, final int charetOffset,
                             final boolean successful) {
      super(text, value, successful);
      this.charetOffset = charetOffset;
    }

    @Override
    public int getCharetOffset() {
      return charetOffset;
    }
  }
}
