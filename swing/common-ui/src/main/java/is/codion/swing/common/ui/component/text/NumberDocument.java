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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.observer.Observable;
import is.codion.common.resource.MessageBundle;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.text.NumberDocument.NumberParser.NumberParseResult;

import org.jspecify.annotations.Nullable;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Objects;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

/**
 * A Document implementation for numerical values
 */
class NumberDocument<T extends Number> extends PlainDocument {

	NumberDocument(NumberFormat format, Class<T> clazz) {
		this(new NumberParsingDocumentFilter<>(new NumberParser<>(format, clazz)));
	}

	protected NumberDocument(NumberParsingDocumentFilter<T> documentFilter) {
		super.setDocumentFilter(documentFilter);
	}

	/**
	 * @param filter the filter
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public final void setDocumentFilter(DocumentFilter filter) {
		throw new UnsupportedOperationException("Changing the DocumentFilter of NumberDocument and its descendants is not allowed");
	}

	@Override
	public final NumberParsingDocumentFilter<T> getDocumentFilter() {
		return (NumberParsingDocumentFilter<T>) super.getDocumentFilter();
	}

	protected final NumberFormat format() {
		return ((NumberParser<T>) getDocumentFilter().parser()).format();
	}

	protected final void set(@Nullable T number) {
		setText(number == null ? "" : format().format(number));
	}

	protected final @Nullable T get() {
		try {
			return getDocumentFilter().parser().parse(getText(0, getLength())).value();
		}
		catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	protected final void setText(String text) {
		try {
			if (!Objects.equals(getText(0, getLength()), text)) {
				remove(0, getLength());
				insertString(0, text, null);
			}
		}
		catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	final Observable<T> observable() {
		return getDocumentFilter().value.observable();
	}

	void setTextComponent(JTextComponent textComponent) {
		getDocumentFilter().setTextComponent(textComponent);
	}

	void setGroupingUsed(boolean groupingUsed) {
		T value = get();
		format().setGroupingUsed(groupingUsed);
		set(value);
	}

	void setSeparators(char decimalSeparator, char groupingSeparator) {
		if (decimalSeparator == groupingSeparator) {
			throw new IllegalArgumentException("Decimal separator must not be the same as grouping separator");
		}
		DecimalFormatSymbols symbols = ((DecimalFormat) format()).getDecimalFormatSymbols();
		symbols.setDecimalSeparator(decimalSeparator);
		symbols.setGroupingSeparator(groupingSeparator);
		T value = get();
		((DecimalFormat) format()).setDecimalFormatSymbols(symbols);
		set(value);
	}

	void setDecimalSeparator(char decimalSeparator) {
		DecimalFormatSymbols symbols = ((DecimalFormat) format()).getDecimalFormatSymbols();
		if (decimalSeparator == symbols.getGroupingSeparator()) {
			symbols.setGroupingSeparator(symbols.getDecimalSeparator());
		}
		symbols.setDecimalSeparator(decimalSeparator);
		T value = get();
		((DecimalFormat) format()).setDecimalFormatSymbols(symbols);
		set(value);
	}

	void setGroupingSeparator(char groupingSeparator) {
		DecimalFormatSymbols symbols = ((DecimalFormat) format()).getDecimalFormatSymbols();
		if (groupingSeparator == symbols.getDecimalSeparator()) {
			symbols.setDecimalSeparator(symbols.getGroupingSeparator());
		}
		symbols.setGroupingSeparator(groupingSeparator);
		T value = get();
		((DecimalFormat) format()).setDecimalFormatSymbols(symbols);
		set(value);
	}

	static class NumberParser<T extends Number> implements Parser<T> {

		private static final String MINUS_SIGN = "-";

		private final NumberFormat format;
		private final Class<T> clazz;

		protected NumberParser(NumberFormat format, Class<T> clazz) {
			this.format = requireNonNull(format);
			this.format.setRoundingMode(RoundingMode.DOWN);
			this.clazz = requireNonNull(clazz);
		}

		@Override
		public NumberParseResult<T> parse(String string) {
			if (string.isEmpty() || MINUS_SIGN.equals(string)) {
				return new DefaultNumberParseResult<>(string, null);
			}

			T parsedNumber = parseNumber(string);
			if (parsedNumber != null) {
				String formattedNumber = format.format(parsedNumber);
				//handle trailing decimal symbol and trailing decimal zeros
				if (format instanceof DecimalFormat) {
					String decimalSeparator =
									String.valueOf(((DecimalFormat) format).getDecimalFormatSymbols().getDecimalSeparator());
					if (!formattedNumber.contains(decimalSeparator) && string.endsWith(decimalSeparator)) {
						formattedNumber += decimalSeparator;
					}
					int decimalSeparatorIndex = string.indexOf(decimalSeparator);
					if (decimalSeparatorIndex >= 0 && string.substring(decimalSeparatorIndex).endsWith("0")) {
						formattedNumber += (formattedNumber.contains(decimalSeparator) ? "" : decimalSeparator) +
										trailingDecimalZeros(string, decimalSeparatorIndex);
					}
				}

				return new DefaultNumberParseResult<>(formattedNumber, parsedNumber, countAddedGroupingSeparators(string, formattedNumber), true);
			}

			return new DefaultNumberParseResult<>(string, null, 0, false);
		}

		/**
		 * @return the underlying format
		 */
		protected final NumberFormat format() {
			return format;
		}

		/**
		 * @param text the text to parse
		 * @return a number if the format can parse it, null otherwise
		 */
		private @Nullable T parseNumber(String text) {
			if (text.isEmpty()) {
				return null;
			}

			ParsePosition position = new ParsePosition(0);
			T number = (T) format.parse(text, position);
			if (position.getIndex() != text.length() || position.getErrorIndex() != -1) {
				return null;
			}

			return toType(number);
		}

		private T toType(T number) {
			if (clazz.equals(Short.class)) {
				return toShort(number);
			}
			if (clazz.equals(Integer.class)) {
				return toInteger(number);
			}
			else if (clazz.equals(Long.class)) {
				return toLong(number);
			}
			else if (clazz.equals(Double.class)) {
				return toDouble(number);
			}
			else if (clazz.equals(BigDecimal.class)) {
				return toBigDecimal(number);
			}

			throw new IllegalArgumentException("Unsupported type class: " + clazz);
		}

		private T toShort(T number) {
			if (number instanceof Short) {
				return number;
			}

			return (T) Short.valueOf(number.shortValue());
		}

		private T toInteger(T number) {
			if (number instanceof Integer) {
				return number;
			}

			return (T) Integer.valueOf(number.intValue());
		}

		private T toLong(T number) {
			if (number instanceof Long) {
				return number;
			}

			return (T) Long.valueOf(number.longValue());
		}

		private T toDouble(T number) {
			if (number instanceof Double) {
				return number;
			}

			return (T) Double.valueOf(number.doubleValue());
		}

		private T toBigDecimal(T number) {
			if (number instanceof BigDecimal) {
				return number;
			}

			return (T) BigDecimal.valueOf(number.doubleValue());
		}

		private int countAddedGroupingSeparators(String currentNumber, String newNumber) {
			DecimalFormatSymbols symbols = ((DecimalFormat) format).getDecimalFormatSymbols();

			return count(newNumber, symbols.getGroupingSeparator()) - count(currentNumber, symbols.getGroupingSeparator());
		}

		private static String trailingDecimalZeros(String string, int decimalSeparatorIndex) {
			StringBuilder builder = new StringBuilder();
			int index = string.length() - 1;
			char c = string.charAt(index);
			while (c == '0' && index > decimalSeparatorIndex) {
				builder.append('0');
				c = string.charAt(--index);
			}

			return builder.toString();
		}

		private static int count(String string, char groupingSeparator) {
			int counter = 0;
			for (char c : string.toCharArray()) {
				if (c == groupingSeparator) {
					counter++;
				}
			}

			return counter;
		}

		protected interface NumberParseResult<T extends Number> extends ParseResult<T> {

			/**
			 * @return the number of characters added
			 */
			int charetOffset();
		}

		protected static final class DefaultNumberParseResult<T extends Number>
						extends DefaultParseResult<T> implements NumberParseResult<T> {

			private final int charetOffset;

			private DefaultNumberParseResult(String text, @Nullable T value) {
				this(text, value, 0, true);
			}

			DefaultNumberParseResult(String text, @Nullable T value, int charetOffset,
															 boolean successful) {
				super(text, value, successful);
				this.charetOffset = charetOffset;
			}

			@Override
			public int charetOffset() {
				return charetOffset;
			}
		}
	}

	static final class NumberParsingDocumentFilter<T extends Number> extends ValidationDocumentFilter<T> {

		private static final MessageBundle MESSAGES =
						messageBundle(NumberParsingDocumentFilter.class, getBundle(NumberParsingDocumentFilter.class.getName()));

		private final NumberRangeValidator<T> rangeValidator;
		private final NumberParser<T> parser;
		private final Value<T> value = Value.nullable();

		private @Nullable JTextComponent textComponent;
		private boolean convertGroupingToDecimalSeparator = true;
		private boolean silentValidation = false;

		NumberParsingDocumentFilter(NumberParser<T> parser) {
			this.parser = requireNonNull(parser);
			this.rangeValidator = new NumberRangeValidator<>();
			addValidator(rangeValidator);
		}

		@Override
		public void insertString(FilterBypass filterBypass, int offset, String string,
														 AttributeSet attributeSet) throws BadLocationException {
			replace(filterBypass, offset, 0, string, attributeSet);
		}

		@Override
		public void remove(FilterBypass filterBypass, int offset, int length) throws BadLocationException {
			replace(filterBypass, offset, length, "", null);
		}

		@Override
		public void replace(FilterBypass filterBypass, int offset, int length, String text,
												@Nullable AttributeSet attributeSet) throws BadLocationException {
			if (text != null) {
				text = convertSingleGroupingToDecimalSeparator(text);
				Document document = filterBypass.getDocument();
				StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
				builder.replace(offset, offset + length, text);
				NumberParseResult<T> parseResult = parser.parse(builder.toString());
				if (parseResult.successful()) {
					validateReplace(parseResult, filterBypass, attributeSet,
									offset + text.length() + parseResult.charetOffset());
				}
			}
		}

		Parser<T> parser() {
			return parser;
		}

		void setMinimumValue(@Nullable Number minimumValue) {
			this.rangeValidator.minimumValue = minimumValue;
		}

		void setMaximumValue(@Nullable Number maximumValue) {
			this.rangeValidator.maximumValue = maximumValue;
		}

		void setConvertGroupingToDecimalSeparator(boolean convertGroupingToDecimalSeparator) {
			this.convertGroupingToDecimalSeparator = convertGroupingToDecimalSeparator;
		}

		boolean isConvertGroupingToDecimalSeparator() {
			return convertGroupingToDecimalSeparator;
		}

		void setSilentValidation(boolean silentValidation) {
			this.silentValidation = silentValidation;
		}

		boolean isSilentValidation() {
			return silentValidation;
		}

		/**
		 * Sets the text component, necessary for keeping the correct caret position when editing
		 * @param textComponent the text component
		 */
		void setTextComponent(JTextComponent textComponent) {
			this.textComponent = textComponent;
		}

		/**
		 * A number field adds grouping separators internally and does not accept them when typed,
		 * so interpret a single grouping separator as a decimal separator, this solves problems related
		 * to locale, such as accepting the comma button on a numpad as a decimal separator, which
		 * is usually what we want.
		 */
		private String convertSingleGroupingToDecimalSeparator(String text) {
			if (convertGroupingToDecimalSeparator && text.length() == 1 && parser.format instanceof DecimalFormat) {
				DecimalFormatSymbols formatSymbols = ((DecimalFormat) parser.format).getDecimalFormatSymbols();

				return text.replace(formatSymbols.getGroupingSeparator(), formatSymbols.getDecimalSeparator());
			}

			return text;
		}

		private void validateReplace(NumberParseResult<T> parseResult, FilterBypass filterBypass,
																 @Nullable AttributeSet attributeSet, int dotLocation) throws BadLocationException {
			if (parseResult.value() != null) {
				try {
					validate(parseResult.value());
				}
				catch (IllegalArgumentException e) {
					if (silentValidation) {
						return;
					}
					throw e;
				}
			}
			super.replace(filterBypass, 0, filterBypass.getDocument().getLength(), parseResult.text(), attributeSet);
			value.set(parseResult.value());
			if (textComponent != null) {
				textComponent.getCaret().setDot(dotLocation);
			}
		}

		private static final class NumberRangeValidator<T extends Number> implements Value.Validator<T> {

			private @Nullable Number minimumValue;
			private @Nullable Number maximumValue;

			@Override
			public void validate(T value) {
				if (!withinRange(value)) {
					throw new IllegalArgumentException(MESSAGES.getString("value_outside_range") + ": " + minimumValue + " - " + maximumValue);
				}
			}

			private boolean withinRange(T value) {
				return value == null || (greaterThanMinimum(value) && lessThanMaximum(value));
			}

			private boolean greaterThanMinimum(T value) {
				return minimumValue == null || value.doubleValue() >= minimumValue.doubleValue();
			}

			private boolean lessThanMaximum(T value) {
				return maximumValue == null || value.doubleValue() <= maximumValue.doubleValue();
			}
		}
	}

	static final class DecimalDocument<T extends Number> extends NumberDocument<T> {

		static final int MAXIMUM_FRACTION_DIGITS = 340;

		DecimalDocument(DecimalFormat format, boolean parseBigDecimal) {
			super(new NumberParsingDocumentFilter<>(new DecimalDocumentParser<>(format, parseBigDecimal)));
			if (parseBigDecimal) {
				format.setParseBigDecimal(true);
			}
		}

		int getMaximumFractionDigits() {
			int maximumFractionDigits = format().getMaximumFractionDigits();

			return maximumFractionDigits == MAXIMUM_FRACTION_DIGITS ? -1 : maximumFractionDigits;
		}

		void setMaximumFractionDigits(int maximumFractionDigits) {
			if (maximumFractionDigits < -1) {
				throw new IllegalArgumentException("Maximum fraction digits must be => 0, or -1 for no maximum");
			}
			format().setMaximumFractionDigits(maximumFractionDigits == -1 ? MAXIMUM_FRACTION_DIGITS : maximumFractionDigits);
			setText("");
		}

		/* Automatically adds a 0 in front of a decimal separator, when it's the first character entered */
		private static final class DecimalDocumentParser<T extends Number> extends NumberParser<T> {

			private DecimalDocumentParser(DecimalFormat format, boolean parseBigDecimal) {
				super(format, parseBigDecimal ? (Class<T>) BigDecimal.class : (Class<T>) Double.class);
			}

			@Override
			public NumberParseResult<T> parse(String string) {
				char decimalSeparator = ((DecimalFormat) format()).getDecimalFormatSymbols().getDecimalSeparator();
				if (string.equals(Character.toString(decimalSeparator))) {
					try {
						//use the format for the correct type
						return new DefaultNumberParseResult<>("0" + decimalSeparator, (T) format().parse("0"), 1, true);
					}
					catch (ParseException e) {/*Won't happen*/}
				}

				return super.parse(string);
			}
		}
	}
}
