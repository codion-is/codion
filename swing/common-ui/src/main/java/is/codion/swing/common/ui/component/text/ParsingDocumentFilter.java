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
import is.codion.swing.common.model.component.text.DocumentAdapter;

import org.jspecify.annotations.Nullable;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

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
	// true while a replacement is being applied, a removal followed by an insertion, the removal leaving an intermediate text
	private boolean replacing = false;

	ParsingDocumentFilter(Parser<T> parser) {
		this.parser = requireNonNull(parser);
	}

	@Override
	public final void insertString(FilterBypass filterBypass, int offset, String string,
																 @Nullable AttributeSet attributeSet) throws BadLocationException {
		replace(filterBypass, offset, 0, string, attributeSet);
	}

	@Override
	public final void remove(FilterBypass filterBypass, int offset, int length) throws BadLocationException {
		replace(filterBypass, offset, length, "", null);
	}

	@Override
	public final void replace(FilterBypass filterBypass, int offset, int length, @Nullable String string,
														@Nullable AttributeSet attributeSet) throws BadLocationException {
		String transformedString = string == null ? "" : transform(string);
		Document document = filterBypass.getDocument();
		StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
		builder.replace(offset, offset + length, transformedString);
		Parser.ParseResult<T> parseResult = parser.parse(builder.toString());
		if (validate(parseResult, singleCharacter(transformedString))) {
			apply(new ReplacementBypass(filterBypass), offset, length, transformedString, parseResult, attributeSet);
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
		return Stream.concat(silentValidators.stream(), validators.stream()).toList();
	}

	/**
	 * Validates the result of an edit, before it is applied, an unsuccessful parse is rejected silently.
	 * @param parseResult the result of parsing the edited text
	 * @param singleCharacter true if the edit inserts at most a single character, in which case
	 * a failing {@link SilentValidator} rejects the edit silently instead of throwing
	 * @return true if the value is valid, false if the edit should be rejected silently
	 * @throws IllegalArgumentException in case validation fails
	 */
	protected boolean validate(Parser.ParseResult<T> parseResult, boolean singleCharacter) {
		if (!parseResult.successful()) {
			return false;
		}
		T value = parseResult.value();
		if (value == null) {
			return true;
		}
		for (SilentValidator<T> validator : silentValidators) {
			try {
				validator.validate(value);
			}
			catch (IllegalArgumentException e) {
				if (singleCharacter) {
					return false;
				}
				throw e;
			}
		}
		validators.forEach(validator -> validator.validate(value));

		return true;
	}

	/**
	 * Applies a validated edit, replacing the given range with the given string by default.
	 * @param filterBypass the filter bypass
	 * @param offset the offset of the edit
	 * @param length the length of the text being replaced
	 * @param string the transformed string being inserted, empty in case of a removal
	 * @param parseResult the result of parsing the edited text
	 * @param attributeSet the attributes, if any
	 * @throws BadLocationException in case of an invalid location
	 */
	protected void apply(FilterBypass filterBypass, int offset, int length, String string,
											 Parser.ParseResult<T> parseResult, @Nullable AttributeSet attributeSet) throws BadLocationException {
		filterBypass.replace(offset, length, string, attributeSet);
	}

	/**
	 * Adds a listener notified when the text of the given document changes. The document notifies a replacement in
	 * steps, a removal followed by an insertion, the removal leaving an intermediate text. With a
	 * {@link ParsingDocumentFilter}, a replacement it applies is notified once, by the insertion, a change made outside
	 * the filter, by undoing an edit for example, being notified as the document notifies it.
	 * @param document the document
	 * @param listener the listener
	 */
	static void addTextListener(Document document, Runnable listener) {
		DocumentFilter documentFilter = document instanceof AbstractDocument ? ((AbstractDocument) document).getDocumentFilter() : null;
		if (documentFilter instanceof ParsingDocumentFilter<?>) {
			ParsingDocumentFilter<?> parsingDocumentFilter = (ParsingDocumentFilter<?>) documentFilter;
			document.addDocumentListener((DocumentAdapter) event -> {
				if (!parsingDocumentFilter.replacing || event.getType() != DocumentEvent.EventType.REMOVE) {
					listener.run();
				}
			});
		}
		else {
			document.addDocumentListener((DocumentAdapter) event -> listener.run());
		}
	}

	private static boolean singleCharacter(String string) {
		return string.length() <= 1;
	}

	/**
	 * A validator rejecting a single character edit, such as a keystroke, silently,
	 * while throwing in case of a longer edit, such as a paste or setting the text.
	 * @param <T> the value type
	 */
	interface SilentValidator<T> extends Validator<T> {}

	private final class ReplacementBypass extends FilterBypass {

		private final FilterBypass filterBypass;

		private ReplacementBypass(FilterBypass filterBypass) {
			this.filterBypass = filterBypass;
		}

		@Override
		public Document getDocument() {
			return filterBypass.getDocument();
		}

		@Override
		public void remove(int offset, int length) throws BadLocationException {
			filterBypass.remove(offset, length);
		}

		@Override
		public void insertString(int offset, String string, @Nullable AttributeSet attributeSet) throws BadLocationException {
			filterBypass.insertString(offset, string, attributeSet);
		}

		@Override
		public void replace(int offset, int length, @Nullable String string, @Nullable AttributeSet attributeSet) throws BadLocationException {
			replacing = length > 0 && string != null && !string.isEmpty();
			try {
				filterBypass.replace(offset, length, string, attributeSet);
			}
			finally {
				replacing = false;
			}
		}
	}

	private static final class StringParser implements Parser<String> {
		@Override
		public ParseResult<String> parse(String text) {
			return new DefaultParseResult<>(text, text, true);
		}
	}
}
