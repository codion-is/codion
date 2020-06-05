/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import java.text.DecimalFormat;

final class DecimalDocument<T extends Number> extends NumberField.NumberDocument<T> {

  static final int MAXIMUM_FRACTION_DIGITS = 340;

  DecimalDocument(final DecimalFormat format) {
    super(new DecimalDocumentFilter<>(format));
  }

  int getMaximumFractionDigits() {
    final int maximumFractionDigits = getFormat().getMaximumFractionDigits();

    return maximumFractionDigits == MAXIMUM_FRACTION_DIGITS ? -1 : maximumFractionDigits;
  }

  void setMaximumFractionDigits(final int maximumFractionDigits) {
    if (maximumFractionDigits < 1 && maximumFractionDigits != -1) {
      throw new IllegalArgumentException("Maximum fraction digits must be larger than 0, or -1 for no maximum");
    }
    getFormat().setMaximumFractionDigits(maximumFractionDigits == -1 ? MAXIMUM_FRACTION_DIGITS : maximumFractionDigits);
    setText("");
  }
}
