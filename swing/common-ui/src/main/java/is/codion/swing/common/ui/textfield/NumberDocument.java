/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Objects;

/**
 * A Document implementation for numerical values
 */
class NumberDocument<T extends Number> extends PlainDocument {

  protected NumberDocument(final NumberParsingDocumentFilter<T> documentFilter) {
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
  public final NumberParsingDocumentFilter<T> getDocumentFilter() {
    return (NumberParsingDocumentFilter<T>) super.getDocumentFilter();
  }

  protected final NumberFormat getFormat() {
    return ((NumberParser<T>) getDocumentFilter().getParser()).getFormat();
  }

  protected final void setNumber(final T number) {
    setText(number == null ? "" : getFormat().format(number));
  }

  protected final T getNumber() {
    try {
      return getDocumentFilter().getParser().parse(getText(0, getLength())).getValue();
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

  void setCaret(final Caret caret) {
    getDocumentFilter().setCaret(caret);
  }

  void setSeparators(final char decimalSeparator, final char groupingSeparator) {
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
