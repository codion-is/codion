/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import java.text.DecimalFormat;
import java.text.ParseException;

import static is.codion.swing.common.ui.textfield.ParsingDocumentFilter.ParseResult.parseResult;
import static is.codion.swing.common.ui.textfield.ParsingDocumentFilter.parsingDocumentFilter;

final class DecimalDocument<T extends Number> extends NumberDocument<T> {

  static final int MAXIMUM_FRACTION_DIGITS = 340;

  DecimalDocument(final DecimalFormat format) {
    super(parsingDocumentFilter(new DecimalDocumentParser<>(format), new NumberRangeValidator<>()));
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

  /* Automatically adds a 0 in front of a decimal separator, when it's the first character entered*/
  private static final class DecimalDocumentParser<T extends Number> extends NumberParser<T> {

    private DecimalDocumentParser(final DecimalFormat format) {
      super(format);
    }

    @Override
    public ParsingDocumentFilter.ParseResult<T> parse(final String string) {
      final char decimalSeparator = ((DecimalFormat) getFormat()).getDecimalFormatSymbols().getDecimalSeparator();
      if (string.equals(Character.toString(decimalSeparator))) {
        try {
          //use the format for the correct type
          return parseResult("0" + decimalSeparator, (T) getFormat().parse("0"), 1);
        }
        catch (final ParseException e) {/*Wont happen*/}
      }

      return super.parse(string);
    }
  }
}
