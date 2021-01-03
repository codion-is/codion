/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.value.Value;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Base class for simple text field validation
 */
public abstract class ValidationDocumentFilter<T> extends ParsingDocumentFilter<T> {

  private final List<Value.Validator<T>> validators = new ArrayList<>(0);

  /**
   * Instantiates a new {@link ValidationDocumentFilter} without a validator.
   */
  public ValidationDocumentFilter() {}

  /**
   * Instantiates a new {@link ValidationDocumentFilter} with the given validator.
   * @param validator the validator
   */
  public ValidationDocumentFilter(final Value.Validator<T> validator) {
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
  protected final ParseResult<T> parse(final String text) {
    final ParseResult<T> parseResult = parseValue(text);
    if (parseResult.successful()) {
      validate(parseResult.getValue());

      return parseResult;
    }

    return parseResult(parseResult.getText(), null, parseResult.getCharactersAdded(), false);
  }

  /**
   * Performs the value parsing, if successful the resulting value will be validated
   * @param text the text to parse
   * @return the parse result
   */
  protected abstract ParseResult<T> parseValue(final String text);

  private void validate(final T value) throws IllegalArgumentException {
    validators.forEach(validator -> validator.validate(value));
  }
}
