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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.reactive.value.Value.Validator;

import org.jspecify.annotations.Nullable;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A DocumentFilter which parses a value from the document text and allows for validation of the parsed value.
 * @param <T> the value type
 */
class ParsingDocumentFilter<T> extends DocumentFilter {

	static final Parser<String> STRING_PARSER = new StringParser();

	private final Parser<T> parser;
	private final Set<SilentValidator<T>> silentValidators = new LinkedHashSet<>();
	private final Set<Validator<T>> validators = new LinkedHashSet<>();

	ParsingDocumentFilter(Parser<T> parser) {
		this.parser = requireNonNull(parser);
	}

	@Override
	public final void insertString(FilterBypass filterBypass, int offset, String string,
																 AttributeSet attributeSet) throws BadLocationException {
		String transformedString = transform(string);
		transformedString = transformedString == null ? "" : transformedString;
		Document document = filterBypass.getDocument();
		StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
		builder.insert(offset, transformedString);
		Parser.ParseResult<T> parseResult = parser.parse(builder.toString());
		if (parseResult.successful()) {
			if (!validate(parseResult.value())) {
				return;
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
			if (!validate(parseResult.value())) {
				return;
			}
			super.remove(filterBypass, offset, length);
		}
	}

	@Override
	public final void replace(FilterBypass filterBypass, int offset, int length, String string,
														AttributeSet attributeSet) throws BadLocationException {
		String transformedString = transform(string);
		transformedString = transformedString == null ? "" : transformedString;
		Document document = filterBypass.getDocument();
		StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
		builder.replace(offset, offset + length, transformedString);
		Parser.ParseResult<T> parseResult = parser.parse(builder.toString());
		if (parseResult.successful()) {
			if (!validate(parseResult.value())) {
				return;
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

	final void addValidator(Validator<T> validator) {
		if (requireNonNull(validator) instanceof SilentValidator<T>) {
			silentValidators.add((SilentValidator<T>) validator);
		}
		else {
			validators.add(requireNonNull(validator));
		}
	}

	final Collection<Validator<T>> validators() {
		return validators;
	}

	/**
	 * @param value the value to validate
	 * @return true if the value is valid, false if the value fails silent validation
	 * @throws IllegalArgumentException in case validation fails
	 */
	private boolean validate(@Nullable T value) {
		if (value == null) {
			return true;
		}
		for (SilentValidator<T> validator : silentValidators) {
			try {
				validator.validate(value);
			}
			catch (IllegalArgumentException e) {
				return false;
			}
		}
		validators.forEach(validator -> validator.validate(value));

		return true;
	}

	interface SilentValidator<T> extends Validator<T> {}

	private static final class StringParser implements Parser<String> {
		@Override
		public ParseResult<String> parse(String text) {
			return new DefaultParseResult<>(text, text, true);
		}
	}
}
