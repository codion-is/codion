/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.value.Value;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

final class NumberParsingDocumentFilter<T extends Number> extends ValidationDocumentFilter<T> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(NumberParsingDocumentFilter.class.getName());

  private final NumberRangeValidator<T> rangeValidator;
  private final NumberParser<T> parser;

  private Caret caret;

  NumberParsingDocumentFilter(final NumberParser<T> parser) {
    this.parser = requireNonNull(parser, "parser");
    this.rangeValidator = new NumberRangeValidator<>();
    addValidator(rangeValidator);
  }

  @Override
  public void insertString(final FilterBypass filterBypass, final int offset, final String string,
                           final AttributeSet attributeSet) throws BadLocationException {
    replace(filterBypass, offset, 0, string, attributeSet);
  }

  @Override
  public void remove(final FilterBypass filterBypass, final int offset, final int length) throws BadLocationException {
    replace(filterBypass, offset, length, "", null);
  }

  @Override
  public void replace(final FilterBypass filterBypass, final int offset, final int length, final String text,
                      final AttributeSet attributeSet) throws BadLocationException {
    final Document document = filterBypass.getDocument();
    final StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
    builder.replace(offset, offset + length, text);
    final NumberParser.NumberParseResult<T> parseResult = parser.parse(builder.toString());
    if (parseResult.successful()) {
      if (parseResult.getValue() != null) {
        validate(parseResult.getValue());
      }
      super.replace(filterBypass, 0, document.getLength(), parseResult.getText(), attributeSet);
      if (caret != null) {
        try {
          caret.setDot(offset + text.length() + parseResult.getCharetOffset());
        }
        catch (final NullPointerException e) {
          e.printStackTrace();
          //Yeah, here's a hack, this error occurs occasionally, within DefaultCaret.setDot(),
          //probably EDT related, so I'll suppress it until I understand what's going on
        }
      }
    }
  }

  Parser<T> getParser() {
    return parser;
  }

  void setRange(final double min, final double max) {
    rangeValidator.minimumValue = min;
    rangeValidator.maximumValue = max;
  }

  double getMaximumValue() {
    return rangeValidator.maximumValue;
  }

  double getMinimumValue() {
    return rangeValidator.minimumValue;
  }

  /**
   * Sets the caret, necessary for keeping the correct caret position when editing
   * @param caret the text field caret
   */
  protected void setCaret(final Caret caret) {
    this.caret = caret;
  }

  private static final class NumberRangeValidator<T extends Number> implements Value.Validator<T> {

    private double minimumValue = Double.NEGATIVE_INFINITY;
    private double maximumValue = Double.POSITIVE_INFINITY;

    @Override
    public void validate(final T value) {
      if (!isWithinRange(value.doubleValue())) {
        throw new IllegalArgumentException(MESSAGES.getString("value_outside_range") + " " + minimumValue + " - " + maximumValue);
      }
    }

    /**
     * @param value the value to check
     * @return true if this value falls within the allowed range for this document
     */
    boolean isWithinRange(final double value) {
      return value >= minimumValue && value <= maximumValue;
    }
  }
}
