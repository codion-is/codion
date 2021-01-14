/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.value.Value;

import javax.swing.text.DocumentFilter;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A {@link DocumentFilter} extension providing validation and parsing.
 * @param <T> the type being parsed
 */
public abstract class AbstractParsingDocumentFilter<T> extends DocumentFilter {

  private final List<Value.Validator<T>> validators = new ArrayList<>(0);

  private final Parser<T> parser;

  /**
   * @param parser the value parser
   */
  protected AbstractParsingDocumentFilter(final Parser<T> parser) {
    this.parser = requireNonNull(parser, "parser");
  }

  /**
   * @param parser the value parser
   * @param validator the validator
   */
  protected AbstractParsingDocumentFilter(final Parser<T> parser, final Value.Validator<T> validator) {
    this.parser = requireNonNull(parser, "parser");
    addValidator(validator);
  }

  /**
   * Adds a validator to this validation document
   * @param validator the validator to add
   */
  public final void addValidator(final Value.Validator<T> validator) {
    validators.add(requireNonNull(validator, "validator"));
  }

  /**
   * Validates the given value using all the underlying validators (if any).
   * @param value the value to validate
   * @see #addValidator(Value.Validator)
   */
  protected final void validate(final T value) {
    validators.forEach(validator -> validator.validate(value));
  }

  /**
   * @return the underlying {@link Parser}
   */
  protected final Parser<T> getParser() {
    return parser;
  }
}
