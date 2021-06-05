/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import static is.codion.swing.common.ui.textfield.Parser.parseResult;

/**
 * A DocumentFilter which parses a value from the document text and allowes for validation of the parsed value.
 * @param <T> the value type
 */
public class ParsingDocumentFilter<T> extends AbstractParsingDocumentFilter<T> {

  public static final Parser<String> STRING_PARSER = text -> parseResult(text, text);

  public ParsingDocumentFilter(final Parser<T> parser) {
    super(parser);
  }

  @Override
  public final void insertString(final FilterBypass filterBypass, final int offset, final String string,
                                 final AttributeSet attributeSet) throws BadLocationException {
    final String transformedString = transform(string);
    final Document document = filterBypass.getDocument();
    final StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
    builder.insert(offset, transformedString);
    final Parser.ParseResult<T> parseResult = getParser().parse(builder.toString());
    if (parseResult.successful()) {
      if (parseResult.getValue() != null) {
        validate(parseResult.getValue());
      }
      super.insertString(filterBypass, offset, transformedString, attributeSet);
    }
  }

  @Override
  public final void remove(final FilterBypass filterBypass, final int offset, final int length) throws BadLocationException {
    final Document document = filterBypass.getDocument();
    final StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
    builder.replace(offset, offset + length, "");
    final Parser.ParseResult<T> parseResult = getParser().parse(builder.toString());
    if (parseResult.successful()) {
      if (parseResult.getValue() != null) {
        validate(parseResult.getValue());
      }
      super.remove(filterBypass, offset, length);
    }
  }

  @Override
  public final void replace(final FilterBypass filterBypass, final int offset, final int length, final String text,
                            final AttributeSet attributeSet) throws BadLocationException {
    final String transformedText = transform(text);
    final Document document = filterBypass.getDocument();
    final StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
    builder.replace(offset, offset + length, transformedText);
    final Parser.ParseResult<T> parseResult = getParser().parse(builder.toString());
    if (parseResult.successful()) {
      if (parseResult.getValue() != null) {
        validate(parseResult.getValue());
      }
      super.replace(filterBypass, offset, length, transformedText, attributeSet);
    }
  }

  /**
   * Perform any required transformation of the string, the resulting string
   * must be of the same length as the original string.
   * By default returns the string unchanged.
   * @param string the string to transform
   * @return the transformed string
   */
  protected String transform(final String string) {
    return string;
  }
}
