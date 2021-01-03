/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import java.text.DecimalFormat;

final class DecimalDocumentFilter<T extends Number> extends NumberField.NumberDocumentFilter<T> {

  DecimalDocumentFilter(final DecimalFormat format) {
    super(format);
  }

  @Override
  protected ParseResult<T> parse(final String string) {
    final char decimalSeparator = ((DecimalFormat) getFormat()).getDecimalFormatSymbols().getDecimalSeparator();
    if (string.equals(Character.toString(decimalSeparator))) {
      return parseResult("0" + decimalSeparator, (T) Double.valueOf(0d), 1);
    }

    return super.parse(string);
  }
}
