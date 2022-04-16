/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import static is.codion.swing.common.ui.component.textfield.Parser.parseResult;
import static java.util.Objects.requireNonNull;

/**
 * A DocumentFilter which parses a value from the document text and allows for validation of the parsed value.
 * @param <T> the value type
 */
public class ParsingDocumentFilter<T> extends ValidationDocumentFilter<T> {

  public static final Parser<String> STRING_PARSER = text -> parseResult(text, text);

  private final Parser<T> parser;

  public ParsingDocumentFilter(Parser<T> parser) {
    this.parser = requireNonNull(parser, "parser");
  }

  @Override
  public final void insertString(FilterBypass filterBypass, int offset, String string,
                                 AttributeSet attributeSet) throws BadLocationException {
    String transformedString = transform(string);
    Document document = filterBypass.getDocument();
    StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
    builder.insert(offset, transformedString);
    Parser.ParseResult<T> parseResult = parser.parse(builder.toString());
    if (parseResult.successful()) {
      if (parseResult.getValue() != null) {
        validate(parseResult.getValue());
      }
      super.insertString(filterBypass, offset, transformedString, attributeSet);
    }
  }

  @Override
  public final void remove(FilterBypass filterBypass, int offset, int length) throws BadLocationException {
    Document document = filterBypass.getDocument();
    StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
    builder.replace(offset, offset + length, "");
    Parser.ParseResult<T> parseResult = parser.parse(builder.toString());
    if (parseResult.successful()) {
      if (parseResult.getValue() != null) {
        validate(parseResult.getValue());
      }
      super.remove(filterBypass, offset, length);
    }
  }

  @Override
  public final void replace(FilterBypass filterBypass, int offset, int length, String string,
                            AttributeSet attributeSet) throws BadLocationException {
    String transformedString = transform(string);
    Document document = filterBypass.getDocument();
    StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
    builder.replace(offset, offset + length, transformedString);
    Parser.ParseResult<T> parseResult = parser.parse(builder.toString());
    if (parseResult.successful()) {
      if (parseResult.getValue() != null) {
        validate(parseResult.getValue());
      }
      super.replace(filterBypass, offset, length, transformedString, attributeSet);
    }
  }

  /**
   * Perform any required transformation of the string, the resulting string
   * must be of the same length as the original string.
   * Returns the string unchanged by default.
   * @param string the string to transform
   * @return the transformed string
   */
  protected String transform(String string) {
    return string;
  }
}