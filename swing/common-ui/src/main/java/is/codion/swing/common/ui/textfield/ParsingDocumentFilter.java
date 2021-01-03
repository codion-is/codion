/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.value.Value;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A DocumentFilter which parses a value from the document text and allowes for validation of the parsed value.
 * @param <T> the value type
 */
public abstract class ParsingDocumentFilter<T> extends DocumentFilter {

  private final List<Value.Validator<T>> validators = new ArrayList<>(0);

  private Caret caret;

  /**
   * Instantiates a new {@link ParsingDocumentFilter} without a validator.
   */
  public ParsingDocumentFilter() {}

  /**
   * Instantiates a new {@link ParsingDocumentFilter} with the given validator.
   * @param validator the validator
   */
  public ParsingDocumentFilter(final Value.Validator<T> validator) {
    addValidator(validator);
  }

  /**
   * Adds a validator to this validation document
   * @param validator the validator to add
   */
  public final void addValidator(final Value.Validator<T> validator) {
    validators.add(requireNonNull(validator, "validator"));
  }

  @Override
  public final void insertString(final FilterBypass filterBypass, final int offset, final String string,
                                 final AttributeSet attributeSet) throws BadLocationException {
    replace(filterBypass, offset, 0, string, attributeSet);
  }

  @Override
  public final void remove(final FilterBypass filterBypass, final int offset, final int length) throws BadLocationException {
    replace(filterBypass, offset, length, "", null);
  }

  @Override
  public final void replace(final FilterBypass filterBypass, final int offset, final int length, final String text,
                            final AttributeSet attributeSet) throws BadLocationException {
    final Document document = filterBypass.getDocument();
    final StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
    builder.replace(offset, offset + length, text);
    final ParseResult<T> parseResult = parse(builder.toString());
    if (parseResult.successful()) {
      validators.forEach(validator -> validator.validate(parseResult.getValue()));
      super.replace(filterBypass, 0, document.getLength(), parseResult.getText(), attributeSet);
      if (caret != null) {
        try {
          caret.setDot(offset + text.length() + parseResult.getCharactersAdded());
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
   * Sets the caret, necessary for keeping the correct caret position when editing
   * @param caret the text field caret
   */
  protected final void setCaret(final Caret caret) {
    this.caret = caret;
  }

  /**
   * Parses a value from the given text
   * @param text the text to parse
   * @return a value
   */
  protected abstract ParseResult<T> parse(final String text);

  /**
   * The result of parsing a value from a String
   * @param <T> the resulting value type
   */
  public interface ParseResult<T> {

    /**
     * @return the resulting document text, possibly modified
     */
    String getText();

    /**
     * @return the parsed value
     * @see #successful()
     */
    T getValue();

    /**
     * @return the number of characters added to the text during parsing
     */
    int getCharactersAdded();

    /**
     * @return true if the parsing was successful
     */
    boolean successful();
  }

  /**
   * Instantiates a new {@link ParseResult} instance.
   * @param <T> the value type
   * @param text the text being parsed
   * @param value the parsed value
   * @return a new parse result
   */
  public static <T> ParseResult<T> parseResult(final String text, final T value) {
    return parseResult(text, value, 0);
  }

  /**
   * Instantiates a new {@link ParseResult} instance.
   * @param <T> the value type
   * @param text the text being parsed
   * @param value the parsed value
   * @param charactersAdded the number of characters added during parsing (such as thousand separators for numbers)
   * @return a new parse result
   */
  public static <T> ParseResult<T> parseResult(final String text, final T value, final int charactersAdded) {
    return parseResult(text, value, charactersAdded, true);
  }

  /**
   * Instantiates a new {@link ParseResult} instance.
   * @param <T> the value type
   * @param text the text being parsed
   * @param value the parsed value
   * @param charactersAdded the number of characters added during parsing (such as thousand separators for numbers)
   * @param successful true if the parsing was successful
   * @return a new parse result
   */
  public static <T> ParseResult<T> parseResult(final String text, final T value, final int charactersAdded, final boolean successful) {
    return new DefaultParseResult<>(text, value, charactersAdded, successful);
  }

  private static final class DefaultParseResult<T> implements ParseResult<T> {

    private final String text;
    private final T value;
    private final int charactersAdded;
    private final boolean successful;

    private DefaultParseResult(final String text, final T value, final int charactersAdded, final boolean successful) {
      this.text = text;
      this.value = value;
      this.charactersAdded = charactersAdded;
      this.successful = successful;
    }

    @Override
    public String getText() {
      return text;
    }

    @Override
    public T getValue() {
      return value;
    }

    @Override
    public int getCharactersAdded() {
      return charactersAdded;
    }

    @Override
    public boolean successful() {
      return successful;
    }
  }
}
