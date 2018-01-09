/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * A text field for numbers.
 */
public class NumberField extends JTextField {

  /**
   * Instantiates a new NumberField
   * @param document the document to use
   * @param columns the number of columns
   */
  public NumberField(final NumberDocument document, final int columns) {
    super(document, null, columns);
    document.setCaret(getCaret());
    if (document.getFormat() instanceof DecimalFormat) {
      addKeyListener(new GroupingSkipAdapter());
    }
    //todo remove this when grouping functionality is "bullet proof"
    document.getFormat().setGroupingUsed(false);
  }

  /**
   * Set whether or not grouping will be used in this field.
   * @param groupingUsed true if grouping should be used false otherwise
   */
  public void setGroupingUsed(final boolean groupingUsed) {
    ((NumberDocument) getDocument()).getFormat().setGroupingUsed(groupingUsed);
  }

  /**
   * @param number the number to display in this field
   */
  public final void setNumber(final Number number) {
    ((NumberDocument) getDocument()).setNumber(number);
  }

  /**
   * @return the number being displayed in this field
   */
  public final Number getNumber() {
    return ((NumberDocument) getDocument()).getNumber();
  }

  /**
   * Sets the range of values this field should allow
   * @param min the minimum value
   * @param max the maximum value
   */
  public final void setRange(final double min, final double max) {
    ((NumberDocumentFilter) ((NumberDocument) getDocument()).getDocumentFilter()).setRange(min, max);
  }

  /**
   * @return the minimum value this field should accept
   */
  public final double getMinimumValue() {
    return ((NumberDocumentFilter) ((NumberDocument) getDocument()).getDocumentFilter()).getMinimumValue();
  }

  /**
   * @return the maximum value this field should accept
   */
  public final double getMaximumValue() {
    return ((NumberDocumentFilter) ((NumberDocument) getDocument()).getDocumentFilter()).getMaximumValue();
  }

  /**
   * Set the decimal and grouping separators for this field
   * @param decimalSeparator the decimal separator
   * @param groupingSeparator the grouping separator
   * @throws IllegalArgumentException in case both separators are the same character
   */
  public void setSeparators(final char decimalSeparator, final char groupingSeparator) {
    ((NumberDocument) getDocument()).setSeparators(decimalSeparator, groupingSeparator);
  }

  /**
   * A Document implementation for numerical values
   */
  protected static class NumberDocument extends PlainDocument {

    protected NumberDocument(final NumberDocumentFilter documentFilter) {
      super.setDocumentFilter(documentFilter);
    }

    /**
     * @param filter the filter
     * @throws UnsupportedOperationException always
     */
    @Override
    public final void setDocumentFilter(final DocumentFilter filter) {
      throw new UnsupportedOperationException("Changing the DocumentFilter of SizedDocument and its descendants is not allowed");
    }

    protected final NumberFormat getFormat() {
      return ((NumberDocumentFilter) getDocumentFilter()).getFormat();
    }

    protected final void setNumber(final Number number) {
      setText(number == null ? "" : getFormat().format(number));
    }

    protected final Number getNumber() {
      try {
        return ((NumberDocumentFilter) getDocumentFilter()).parseNumber(getText(0, getLength()));
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

    protected final void setText(final String text) {
      try {
        remove(0, getLength());
        insertString(0, text, null);
      }
      catch (final BadLocationException e) {
        throw new RuntimeException(e);
      }
    }

    private void setCaret(final Caret caret) {
      ((NumberDocumentFilter) getDocumentFilter()).setCaret(caret);
    }

    private void setSeparators(final char decimalSeparator, final char groupingSeparator) {
      if (decimalSeparator == groupingSeparator) {
        throw new IllegalArgumentException("Decimal separator must not be the same as grouping separator");
      }
      final DecimalFormatSymbols symbols = ((DecimalFormat) getFormat()).getDecimalFormatSymbols();
      symbols.setDecimalSeparator(decimalSeparator);
      symbols.setGroupingSeparator(groupingSeparator);
      final Number number = getNumber();
      ((DecimalFormat) getFormat()).setDecimalFormatSymbols(symbols);
      setNumber(number);
    }
  }

  /**
   * A DocumentFilter for restricting input to numerical values
   */
  protected static class NumberDocumentFilter extends DocumentFilter {

    private static final String MINUS_SIGN = "-";

    private final NumberFormat format;

    private Caret caret;

    private double minimumValue = Double.NEGATIVE_INFINITY;
    private double maximumValue = Double.POSITIVE_INFINITY;

    protected NumberDocumentFilter(final NumberFormat format) {
      this.format = format;
      this.format.setRoundingMode(RoundingMode.DOWN);
    }

    @Override
    public final void insertString(final FilterBypass filterBypass, final int offset, final String string,
                                   final AttributeSet attributeSet) throws BadLocationException {
      replace(filterBypass, offset, 0, string, attributeSet);
    }

    @Override
    public final void remove(final FilterBypass filterBypass, final int offset, final int length) throws BadLocationException {
      replace(filterBypass, offset, length, "", null);
    }

    @Override
    public final void replace(final FilterBypass filterBypass, final int offset, final int length, final String string,
                              final AttributeSet attributeSet) throws BadLocationException {
      final Document document = filterBypass.getDocument();
      final StringBuilder numberBuilder = new StringBuilder(document.getText(0, document.getLength()));
      numberBuilder.replace(offset, offset + length, string);
      final FormatResult formatResult = format(numberBuilder.toString());
      if (formatResult != null) {
        super.replace(filterBypass, 0, document.getLength(), formatResult.formatted, attributeSet);
        if (caret != null) {
          try {
            caret.setDot(offset + string.length() + formatResult.added);
          }
          catch (final NullPointerException e) {
            e.printStackTrace();
            //Yeah, here's a hack, this error occurs occasionally, within DefaultCaret.setDot(),
            //probably EDT related, so I'll suppress it until I understand what's going on
          }
        }
      }
    }

    protected FormatResult format(final String string) {
      if (string.isEmpty() || MINUS_SIGN.equals(string)) {
        return new FormatResult(0, string);
      }

      final Number parsedNumber = parseNumber(string);
      if (parsedNumber != null && isWithinRange(parsedNumber.doubleValue())) {
        String formattedNumber = format.format(parsedNumber);
        //handle trailing decimal symbol and trailing decimal zeros
        if (format instanceof DecimalFormat) {
          final String decimalSeparator = String.valueOf(((DecimalFormat) format).getDecimalFormatSymbols().getDecimalSeparator());
          if (!formattedNumber.contains(decimalSeparator) && string.endsWith(decimalSeparator)) {
            formattedNumber += decimalSeparator;
          }
          final int decimalSeparatorIndex = string.indexOf(decimalSeparator);
          if (decimalSeparatorIndex >= 0 && string.substring(decimalSeparatorIndex, string.length()).endsWith("0")) {
            formattedNumber += (formattedNumber.contains(decimalSeparator) ? "" : decimalSeparator) +
                    getTrailingDecimalZeros(string, decimalSeparatorIndex);
          }
        }

        return new FormatResult(countAddedGroupingSeparators(string, formattedNumber), formattedNumber);
      }

      return null;
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
     * Sets the caret, necessary for keeping the correct caret position when editing
     * @param caret the text field caret
     */
    private void setCaret(final Caret caret) {
      this.caret = caret;
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
    private Number parseNumber(final String text) {
      if (text.isEmpty()) {
        return null;
      }

      final ParsePosition position = new ParsePosition(0);
      final Number number = format.parse(text, position);
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

    private int count(final String string, final char groupingSeparator) {
      int counter = 0;
      for (final char c : string.toCharArray()) {
        if (c == groupingSeparator) {
          counter++;
        }
      }

      return counter;
    }
  }

  protected static final class FormatResult {
    private final int added;
    private final String formatted;

    protected FormatResult(final int added, final String formatted) {
      this.added = added;
      this.formatted = formatted;
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
      final NumberDocument numberDocument = (NumberDocument) getDocument();
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
