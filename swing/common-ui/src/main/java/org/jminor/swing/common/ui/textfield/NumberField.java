/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
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
    addKeyListener(new GroupingSkipAdapter());
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
  protected static class NumberDocument extends SizedDocument {

    protected NumberDocument(final NumberDocumentFilter documentFilter) {
      setDocumentFilterInternal(documentFilter);
    }

    protected final NumberFormat getFormat() {
      return ((NumberDocumentFilter) getDocumentFilter()).getFormat();
    }

    protected final void setNumber(final Number number) {
      setText(number == null ? "" : getFormat().format(number));
    }

    protected final Number getNumber() {
      return ((NumberDocumentFilter) getDocumentFilter()).parseNumber(getText());
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

    private String getText() {
      try {
        return getText(0, getLength());
      }
      catch (final BadLocationException e) {
        throw new RuntimeException(e);
      }
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
  protected static class NumberDocumentFilter extends SizedDocument.SizedDocumentFilter {

    private static final String MINUS_SIGN = "-";

    private final NumberFormat format;

    private double minimumValue = Double.NEGATIVE_INFINITY;
    private double maximumValue = Double.POSITIVE_INFINITY;

    protected NumberDocumentFilter(final NumberFormat format) {
      this.format = format;
      this.format.setRoundingMode(RoundingMode.DOWN);
    }

    @Override
    protected String transformString(final String string) {
      if (string.isEmpty() || MINUS_SIGN.equals(string)) {
        return string;
      }

      final Number parsedNumber = parseNumber(string);
      if (parsedNumber != null && isWithinRange(parsedNumber.doubleValue())) {
        String formattedNumber = format.format(parsedNumber);
        //handle trailing decimal symbol
        if (format instanceof DecimalFormat) {
          final String decimalSeparator = String.valueOf(((DecimalFormat) format).getDecimalFormatSymbols().getDecimalSeparator());
          if (!formattedNumber.contains(decimalSeparator) && string.endsWith(decimalSeparator)) {
            formattedNumber += decimalSeparator;
          }
        }

        return formattedNumber;
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
      final DecimalFormatSymbols symbols = ((DecimalFormat) numberDocument.getFormat()).getDecimalFormatSymbols();
      try {
        final int caretPosition = getCaretPosition();
        if (forward && caretPosition < getDocument().getLength() - 1) {
          final String afterCaret = numberDocument.getText(caretPosition, 1);
          if (afterCaret.charAt(0) == symbols.getGroupingSeparator()) {
            setCaretPosition(caretPosition + 1);
          }
        }
        else if (!forward && caretPosition > 0) {
          final String beforeCaret = numberDocument.getText(caretPosition - 1, 1);
          if (beforeCaret.charAt(0) == symbols.getGroupingSeparator()) {
            setCaretPosition(caretPosition - 1);
          }
        }
      }
      catch (final BadLocationException ignored) {/*Not happening*/}
    }
  }
}
