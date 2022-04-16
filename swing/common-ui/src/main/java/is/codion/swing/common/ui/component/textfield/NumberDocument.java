/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Objects;

/**
 * A Document implementation for numerical values
 */
class NumberDocument<T extends Number> extends PlainDocument {

  protected NumberDocument(NumberParsingDocumentFilter<T> documentFilter) {
    super.setDocumentFilter(documentFilter);
  }

  /**
   * @param filter the filter
   * @throws UnsupportedOperationException always
   */
  @Override
  public final void setDocumentFilter(DocumentFilter filter) {
    throw new UnsupportedOperationException("Changing the DocumentFilter of NumberDocument and its descendants is not allowed");
  }

  @Override
  public final NumberParsingDocumentFilter<T> getDocumentFilter() {
    return (NumberParsingDocumentFilter<T>) super.getDocumentFilter();
  }

  protected final NumberFormat getFormat() {
    return ((NumberParser<T>) getDocumentFilter().getParser()).getFormat();
  }

  protected final void setNumber(T number) {
    setText(number == null ? "" : getFormat().format(number));
  }

  protected final T getNumber() {
    try {
      return getDocumentFilter().getParser().parse(getText(0, getLength())).getValue();
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  protected final void setText(String text) {
    try {
      if (!Objects.equals(getText(0, getLength()), text)) {
        remove(0, getLength());
        insertString(0, text, null);
      }
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  void setTextComponent(JTextComponent textComponent) {
    getDocumentFilter().setTextComponent(textComponent);
  }

  void setSeparators(char decimalSeparator, char groupingSeparator) {
    if (decimalSeparator == groupingSeparator) {
      throw new IllegalArgumentException("Decimal separator must not be the same as grouping separator");
    }
    DecimalFormatSymbols symbols = ((DecimalFormat) getFormat()).getDecimalFormatSymbols();
    symbols.setDecimalSeparator(decimalSeparator);
    symbols.setGroupingSeparator(groupingSeparator);
    T number = getNumber();
    ((DecimalFormat) getFormat()).setDecimalFormatSymbols(symbols);
    setNumber(number);
  }

  void setDecimalSeparator(char decimalSeparator) {
    DecimalFormatSymbols symbols = ((DecimalFormat) getFormat()).getDecimalFormatSymbols();
    symbols.setDecimalSeparator(decimalSeparator);
  }

  void setGroupingSeparator(char groupingSeparator) {
    DecimalFormatSymbols symbols = ((DecimalFormat) getFormat()).getDecimalFormatSymbols();
    symbols.setGroupingSeparator(groupingSeparator);
  }
}
