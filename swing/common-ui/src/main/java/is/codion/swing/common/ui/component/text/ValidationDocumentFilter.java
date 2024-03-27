/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;

import javax.swing.text.DocumentFilter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
	public final void addValidator(Value.Validator<T> validator) {
		validators.add(requireNonNull(validator, "validator"));
	}

	/**
	 * @return an unmodifiable view of the document validators
	 */
	public final Collection<Value.Validator<T>> validators() {
		return Collections.unmodifiableSet(new HashSet<>(validators));
	}

	/**
	 * Validates the given value using all the underlying validators (if any).
	 * @param value the value to validate
	 * @throws IllegalArgumentException in case of an invalid value
	 * @see #addValidator(Value.Validator)
	 */
	protected final void validate(T value) {
		validators.forEach(validator -> validator.validate(value));
	}
}
