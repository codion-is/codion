/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.value.Value;

import javax.swing.text.DocumentFilter;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A {@link DocumentFilter} extension providing validation and parsing.
 * @param <T> the value type
 */
public class ValidationDocumentFilter<T> extends DocumentFilter {

  private final Set<Value.Validator<T>> validators = new LinkedHashSet<>();

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
}
