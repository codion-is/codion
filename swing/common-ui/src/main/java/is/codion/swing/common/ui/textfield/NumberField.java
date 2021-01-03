/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.Configuration;
import is.codion.common.value.PropertyValue;
import is.codion.common.value.Value;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A text field for numbers.
 * @param <T> the Number type
 */
public class NumberField<T extends Number> extends JTextField {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(NumberField.class.getName());

  /**
   * Specifies whether NumberFields disable grouping by default.<br>
   * Value type: Boolean<br>
   * Default value: false.
   */
  public static final PropertyValue<Boolean> DISABLE_GROUPING =
          Configuration.booleanValue("codion.swing.common.ui.disableNumberFieldGrouping", false);

  /**
   * Instantiates a new NumberField
   * @param document the document to use
   * @param columns the number of columns
   */
  public NumberField(final NumberDocument<T> document, final int columns) {
    super(document, null, columns);
    document.setCaret(getCaret());
    if (document.getFormat() instanceof DecimalFormat) {
      addKeyListener(new GroupingSkipAdapter());
    }
    if (DISABLE_GROUPING.get()) {
      document.getFormat().setGroupingUsed(false);
    }
  }

  /**
   * Set whether or not grouping will be used in this field.
   * @param groupingUsed true if grouping should be used false otherwise
   */
  public final void setGroupingUsed(final boolean groupingUsed) {
    getTypedDocument().getFormat().setGroupingUsed(groupingUsed);
  }

  /**
   * @param number the number to display in this field
   */
  public final void setNumber(final T number) {
    getTypedDocument().setNumber(number);
  }

  /**
   * @return the number being displayed in this field
   */
  public final T getNumber() {
    return getTypedDocument().getNumber();
  }

  /**
   * Sets the range of values this field should allow
   * @param min the minimum value
   * @param max the maximum value
   */
  public final void setRange(final double min, final double max) {
    getTypedDocument().getDocumentFilter().setRange(min, max);
  }

  /**
   * @return the minimum value this field should accept
   */
  public final double getMinimumValue() {
    return getTypedDocument().getDocumentFilter().getMinimumValue();
  }

  /**
   * @return the maximum value this field should accept
   */
  public final double getMaximumValue() {
    return getTypedDocument().getDocumentFilter().getMaximumValue();
  }

  /**
   * Set the decimal and grouping separators for this field
   * @param decimalSeparator the decimal separator
   * @param groupingSeparator the grouping separator
   * @throws IllegalArgumentException in case both separators are the same character
   */
  public final void setSeparators(final char decimalSeparator, final char groupingSeparator) {
    getTypedDocument().setSeparators(decimalSeparator, groupingSeparator);
  }

  /**
   * Can't override getDocument() with type cast since it's called before setting the document with a class cast exception.
   * @return the typed document.
   */
  protected final NumberDocument<T> getTypedDocument() {
    return (NumberDocument<T>) super.getDocument();
  }

  /**
   * A Document implementation for numerical values
   */
  protected static class NumberDocument<T extends Number> extends PlainDocument {

    protected NumberDocument(final NumberDocumentFilter<T> documentFilter) {
      super.setDocumentFilter(documentFilter);
    }

    /**
     * @param filter the filter
     * @throws UnsupportedOperationException always
     */
    @Override
    public final void setDocumentFilter(final DocumentFilter filter) {
      throw new UnsupportedOperationException("Changing the DocumentFilter of NumberDocument and its descendants is not allowed");
    }

    @Override
    public final NumberDocumentFilter<T> getDocumentFilter() {
      return (NumberDocumentFilter<T>) super.getDocumentFilter();
    }

    protected final NumberFormat getFormat() {
      return getDocumentFilter().getFormat();
    }

    protected final void setNumber(final T number) {
      setText(number == null ? "" : getFormat().format(number));
    }

    protected final T getNumber() {
      try {
        return getDocumentFilter().parseNumber(getText(0, getLength()));
      }
      catch (final BadLocationException e) {
        throw new RuntimeException(e);
      }
    }

    protected final Integer getInteger() {
      final Number number = getNumber();

      return number == null ? null : number.intValue();
    }

    protected final Long getLong() {
      final Number number = getNumber();

      return number == null ? null : number.longValue();
    }

    protected final Double getDouble() {
      final Number number = getNumber();

      return number == null ? null : number.doubleValue();
    }

    protected final BigDecimal getBigDecimal() {
      return (BigDecimal) getNumber();
    }

    protected final void setText(final String text) {
      try {
        if (!Objects.equals(getText(0, getLength()), text)) {
          remove(0, getLength());
          insertString(0, text, null);
        }
      }
      catch (final BadLocationException e) {
        throw new RuntimeException(e);
      }
    }

    private void setCaret(final Caret caret) {
      getDocumentFilter().setCaret(caret);
    }

    private void setSeparators(final char decimalSeparator, final char groupingSeparator) {
      if (decimalSeparator == groupingSeparator) {
        throw new IllegalArgumentException("Decimal separator must not be the same as grouping separator");
      }
      final DecimalFormatSymbols symbols = ((DecimalFormat) getFormat()).getDecimalFormatSymbols();
      symbols.setDecimalSeparator(decimalSeparator);
      symbols.setGroupingSeparator(groupingSeparator);
      final T number = getNumber();
      ((DecimalFormat) getFormat()).setDecimalFormatSymbols(symbols);
      setNumber(number);
    }
  }

  /**
   * A DocumentFilter for restricting input to numerical values
   */
  protected static class NumberDocumentFilter<T extends Number> extends ValidationDocumentFilter<T> {

    private static final String MINUS_SIGN = "-";

    private final NumberFormat format;

    private double minimumValue = Double.NEGATIVE_INFINITY;
    private double maximumValue = Double.POSITIVE_INFINITY;

    protected NumberDocumentFilter(final NumberFormat format) {
      this.format = format;
      this.format.setRoundingMode(RoundingMode.DOWN);
      addValidator(new RangeValidator());
    }

    protected ParseResult<T> parseValue(final String string) {
      if (string.isEmpty() || MINUS_SIGN.equals(string)) {
        try {
          //using format.parse() for the correct type
          return parseResult(string, (T) format.parse("0"));
        }
        catch (final ParseException e) {/*Wont happen*/}
      }

      final T parsedNumber = parseNumber(string);
      if (parsedNumber != null) {
        String formattedNumber = format.format(parsedNumber);
        //handle trailing decimal symbol and trailing decimal zeros
        if (format instanceof DecimalFormat) {
          final String decimalSeparator = String.valueOf(((DecimalFormat) format).getDecimalFormatSymbols().getDecimalSeparator());
          if (!formattedNumber.contains(decimalSeparator) && string.endsWith(decimalSeparator)) {
            formattedNumber += decimalSeparator;
          }
          final int decimalSeparatorIndex = string.indexOf(decimalSeparator);
          if (decimalSeparatorIndex >= 0 && string.substring(decimalSeparatorIndex).endsWith("0")) {
            formattedNumber += (formattedNumber.contains(decimalSeparator) ? "" : decimalSeparator) +
                    getTrailingDecimalZeros(string, decimalSeparatorIndex);
          }
        }

        return parseResult(formattedNumber, parsedNumber, countAddedGroupingSeparators(string, formattedNumber));
      }

      return parseResult(string, null, 0, false);
    }

    /**
     * @return the format used by this document filter
     */
    protected final NumberFormat getFormat() {
      return format;
    }

    /**
     * Sets the range of values this document filter should allow
     * @param min the minimum value
     * @param max the maximum value
     */
    private void setRange(final double min, final double max) {
      this.minimumValue = min;
      this.maximumValue = max;
    }

    /**
     * @return the minimum value this field should accept
     */
    private double getMinimumValue() {
      return minimumValue;
    }

    /**
     * @return the maximum value this field should accept
     */
    private double getMaximumValue() {
      return maximumValue;
    }

    /**
     * @param value the value to check
     * @return true if this value falls within the allowed range for this document
     */
    private boolean isWithinRange(final double value) {
      return value >= minimumValue && value <= maximumValue;
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

      return number;
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

    private int countAddedGroupingSeparators(final String currentNumber, final String newNumber) {
      final DecimalFormatSymbols symbols = ((DecimalFormat) getFormat()).getDecimalFormatSymbols();

      return count(newNumber, symbols.getGroupingSeparator()) - count(currentNumber, symbols.getGroupingSeparator());
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

    private final class RangeValidator implements Value.Validator<T> {
      @Override
      public void validate(final T value) throws IllegalArgumentException {
        if (!isWithinRange(value.doubleValue())) {
          throw new IllegalArgumentException(MESSAGES.getString("value_outside_range") + " " + minimumValue + " - " + maximumValue);
        }
      }
    }
  }

  private final class GroupingSkipAdapter extends KeyAdapter {
    @Override
    public void keyReleased(final KeyEvent e) {
      switch (e.getKeyCode()) {
        case KeyEvent.VK_BACK_SPACE:
          skipGroupingSeparator(false);
          break;
        case KeyEvent.VK_DELETE:
          skipGroupingSeparator(true);
          break;
        default:
          break;
      }
    }

    private void skipGroupingSeparator(final boolean forward) {
      final NumberDocument<?> numberDocument = getTypedDocument();
      final char groupingSeparator = ((DecimalFormat) numberDocument.getFormat()).getDecimalFormatSymbols().getGroupingSeparator();
      try {
        final int caretPosition = getCaretPosition();
        if (forward && caretPosition < getDocument().getLength() - 1) {
          final char afterCaret = numberDocument.getText(caretPosition, 1).charAt(0);
          if (groupingSeparator == afterCaret) {
            setCaretPosition(caretPosition + 1);
          }
        }
        else if (!forward && caretPosition > 0) {
          final char beforeCaret = numberDocument.getText(caretPosition - 1, 1).charAt(0);
          if (groupingSeparator == beforeCaret) {
            setCaretPosition(caretPosition - 1);
          }
        }
      }
      catch (final BadLocationException ignored) {/*Not happening*/}
    }
  }
}
