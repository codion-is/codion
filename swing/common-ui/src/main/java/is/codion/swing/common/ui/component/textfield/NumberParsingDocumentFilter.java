/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.common.value.Value;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

final class NumberParsingDocumentFilter<T extends Number> extends ValidationDocumentFilter<T> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(NumberParsingDocumentFilter.class.getName());

  private final NumberRangeValidator<T> rangeValidator;
  private final NumberParser<T> parser;

  private JTextComponent textComponent;

  NumberParsingDocumentFilter(NumberParser<T> parser) {
    this.parser = requireNonNull(parser, "parser");
    this.rangeValidator = new NumberRangeValidator<>();
    addValidator(rangeValidator);
  }

  @Override
  public void insertString(FilterBypass filterBypass, int offset, String string,
                           AttributeSet attributeSet) throws BadLocationException {
    replace(filterBypass, offset, 0, string, attributeSet);
  }

  @Override
  public void remove(FilterBypass filterBypass, int offset, int length) throws BadLocationException {
    replace(filterBypass, offset, length, "", null);
  }

  @Override
  public void replace(FilterBypass filterBypass, int offset, int length, String text,
                      AttributeSet attributeSet) throws BadLocationException {
    Document document = filterBypass.getDocument();
    StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
    builder.replace(offset, offset + length, text);
    NumberParser.NumberParseResult<T> parseResult = parser.parse(builder.toString());
    if (parseResult.successful()) {
      if (parseResult.getValue() != null) {
        validate(parseResult.getValue());
      }
      super.replace(filterBypass, 0, document.getLength(), parseResult.getText(), attributeSet);
      if (textComponent != null) {
        textComponent.getCaret().setDot(offset + text.length() + parseResult.getCharetOffset());
      }
    }
  }

  Parser<T> getParser() {
    return parser;
  }

  void setMaximumValue(Number maximumValue) {
    rangeValidator.maximumValue = maximumValue;
  }

  Number getMaximumValue() {
    return rangeValidator.maximumValue;
  }

  void setMinimumValue(Number minimumValue) {
    rangeValidator.minimumValue = minimumValue;
  }

  Number getMinimumValue() {
    return rangeValidator.minimumValue;
  }

  /**
   * Sets the text component, necessary for keeping the correct caret position when editing
   * @param textComponent the text component
   */
  void setTextComponent(JTextComponent textComponent) {
    this.textComponent = textComponent;
  }

  private static final class NumberRangeValidator<T extends Number> implements Value.Validator<T> {

    private Number minimumValue;
    private Number maximumValue;

    @Override
    public void validate(T value) {
      if (!isWithinRange(value)) {
        throw new IllegalArgumentException(MESSAGES.getString("value_outside_range") + " " + minimumValue + " - " + maximumValue);
      }
    }

    boolean isWithinRange(T value) {
      return value == null || (greaterThanMinimum(value) && lessThanMaximum(value));
    }

    private boolean greaterThanMinimum(T value) {
      return minimumValue == null || value.doubleValue() >= minimumValue.doubleValue();
    }

    private boolean lessThanMaximum(T value) {
      return maximumValue == null || value.doubleValue() <= maximumValue.doubleValue();
    }
  }
}
