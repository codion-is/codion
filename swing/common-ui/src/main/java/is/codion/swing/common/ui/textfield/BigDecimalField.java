/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.event.EventDataListener;
import is.codion.swing.common.model.textfield.DocumentAdapter;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * A text field for BigDecimal.
 */
public final class BigDecimalField extends NumberField<BigDecimal> {

  private static final int MAXIMUM_FRACTION_DIGITS = 340;

  /**
   * Instantiates a new DecimalField.
   */
  public BigDecimalField() {
    this(0);
  }

  /**
   * Instantiates a new DecimalField
   * @param columns the number of columns
   */
  public BigDecimalField(final int columns) {
    this(createDefaultFormat(), columns);
  }

  /**
   * Instantiates a new DecimalField
   * @param format the format to use
   */
  public BigDecimalField(final DecimalFormat format) {
    this(format, 0);
  }

  /**
   * Instantiates a new DecimalField
   * @param format the format to use
   * @param columns the number of columns
   */
  public BigDecimalField(final DecimalFormat format, final int columns) {
    super(new BigDecimalDocument(format), columns);
    ((DecimalFormat) getTypedDocument().getFormat()).setParseBigDecimal(true);
  }

  /**
   * @return the maximum number of fraction digits this field shows
   */
  public int getMaximumFractionDigits() {
    return ((BigDecimalDocument) getTypedDocument()).getMaximumFractionDigits();
  }

  /**
   * @param maximumFractionDigits the maximum number of fraction digits this field shows
   */
  public void setMaximumFractionDigits(final int maximumFractionDigits) {
    ((BigDecimalDocument) getTypedDocument()).setMaximumFractionDigits(maximumFractionDigits);
  }

  /**
   * @return the current value
   * @see DecimalFormat#setParseBigDecimal(boolean)
   */
  public BigDecimal getBigDecimal() {
    return getTypedDocument().getBigDecimal();
  }

  /**
   * @param value the value to set
   */
  public void setBigDecimal(final BigDecimal value) {
    getTypedDocument().setNumber(value);
  }

  /**
   * @param listener a listener notified when the value changes
   */
  public void addBigDecimalListener(final EventDataListener<BigDecimal> listener) {
    final NumberDocument<BigDecimal> document = getTypedDocument();
    document.addDocumentListener((DocumentAdapter) e -> listener.onEvent(document.getBigDecimal()));
  }

  private static DecimalFormat createDefaultFormat() {
    final DecimalFormat format = new DecimalFormat();
    format.setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);

    return format;
  }

  private static final class BigDecimalDocument extends NumberDocument<BigDecimal> {

    private BigDecimalDocument(final DecimalFormat format) {
      super(new BigDecimalDocumentFilter(format));
    }

    private int getMaximumFractionDigits() {
      final int maximumFractionDigits = getFormat().getMaximumFractionDigits();

      return maximumFractionDigits == MAXIMUM_FRACTION_DIGITS ? -1 : maximumFractionDigits;
    }

    private void setMaximumFractionDigits(final int maximumFractionDigits) {
      if (maximumFractionDigits < 1 && maximumFractionDigits != -1) {
        throw new IllegalArgumentException("Maximum fraction digits must be larger than 0, or -1 for no maximum");
      }
      getFormat().setMaximumFractionDigits(maximumFractionDigits == -1 ? MAXIMUM_FRACTION_DIGITS : maximumFractionDigits);
      setText("");
    }
  }

  private static final class BigDecimalDocumentFilter extends NumberDocumentFilter<BigDecimal> {

    private BigDecimalDocumentFilter(final DecimalFormat format) {
      super(format);
    }

    @Override
    protected FormatResult format(final String string) {
      final char decimalSeparator = ((DecimalFormat) getFormat()).getDecimalFormatSymbols().getDecimalSeparator();
      if (string.equals(Character.toString(decimalSeparator))) {
        return new FormatResult(1, "0" + decimalSeparator);
      }

      return super.format(string);
    }
  }
}
