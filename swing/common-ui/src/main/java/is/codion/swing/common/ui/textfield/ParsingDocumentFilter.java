/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.value.Value;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import static is.codion.swing.common.ui.textfield.Parser.parseResult;

/**
 * A DocumentFilter which parses a value from the document text and allowes for validation of the parsed value.
 * @param <T> the value type
 * @see #parsingDocumentFilter()
 * @see #parsingDocumentFilter(Parser)
 * @see #parsingDocumentFilter(Value.Validator)
 * @see #parsingDocumentFilter(Parser, Value.Validator)
 */
public class ParsingDocumentFilter<T> extends AbstractParsingDocumentFilter<T> {

  public static final Parser<String> STRING_PARSER = text -> parseResult(text, text);

  protected ParsingDocumentFilter(final Parser<T> parser) {
    super(parser);
  }

  protected ParsingDocumentFilter(final Parser<T> parser, final Value.Validator<T> validator) {
    super(parser, validator);
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

  /**
   * Instantiates a String based new {@link ParsingDocumentFilter} without a validator
   * and using the default string parser.
   * @return a new document filter
   */
  public static ParsingDocumentFilter<String> parsingDocumentFilter() {
    return new ParsingDocumentFilter<>(STRING_PARSER);
  }

  /**
   * Instantiates a new {@link ParsingDocumentFilter} without a validator.
   * @param parser the parser
   * @param <T> the value type
   * @return a new document filter
   */
  public static <T> ParsingDocumentFilter<T> parsingDocumentFilter(final Parser<T> parser) {
    return new ParsingDocumentFilter<>(parser);
  }

  /**
   * Instantiates a new String based {@link ParsingDocumentFilter} with the given validator.
   * @param validator the validator
   * @return a new document filter
   */
  public static ParsingDocumentFilter<String> parsingDocumentFilter(final Value.Validator<String> validator) {
    return new ParsingDocumentFilter<>(STRING_PARSER, validator);
  }

  /**
   * Instantiates a new {@link ParsingDocumentFilter} with the given validator.
   * @param parser the parser
   * @param validator the validator
   * @param <T> the value type
   * @return a new document filter
   */
  public static <T> ParsingDocumentFilter<T> parsingDocumentFilter(final Parser<T> parser, final Value.Validator<T> validator) {
    return new ParsingDocumentFilter<>(parser, validator);
  }
}
