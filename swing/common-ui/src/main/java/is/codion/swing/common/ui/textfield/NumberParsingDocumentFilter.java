/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;

final class NumberParsingDocumentFilter<T extends Number> extends AbstractParsingDocumentFilter<T> {

  private final NumberRangeValidator<T> rangeValidator;

  private Caret caret;

  NumberParsingDocumentFilter(final NumberParser<T> parser) {
    this(parser, new NumberRangeValidator<>());
  }

  NumberParsingDocumentFilter(final NumberParser<T> parser, final NumberRangeValidator<T> rangeValidator) {
    super(parser, rangeValidator);
    this.rangeValidator = rangeValidator;
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
    final NumberParser.NumberParseResult<T> parseResult = ((NumberParser<T>) getParser()).parse(builder.toString());
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

  /**
   * @return the NumberRangeValidator used by this document filter
   */
  protected NumberRangeValidator<T> getNumberRangeValidator() {
    return rangeValidator;
  }

  /**
   * Sets the caret, necessary for keeping the correct caret position when editing
   * @param caret the text field caret
   */
  protected void setCaret(final Caret caret) {
    this.caret = caret;
  }
}
