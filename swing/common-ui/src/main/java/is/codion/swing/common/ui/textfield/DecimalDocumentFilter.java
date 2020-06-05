/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import java.text.DecimalFormat;

final class DecimalDocumentFilter<T extends Number> extends NumberField.NumberDocumentFilter<T> {

  DecimalDocumentFilter(final DecimalFormat format) {
    super(format);
  }

  @Override
  protected NumberField.FormatResult format(final String string) {
    final char decimalSeparator = ((DecimalFormat) getFormat()).getDecimalFormatSymbols().getDecimalSeparator();
    if (string.equals(Character.toString(decimalSeparator))) {
      return new NumberField.FormatResult(1, "0" + decimalSeparator);
    }

    return super.format(string);
  }
}
