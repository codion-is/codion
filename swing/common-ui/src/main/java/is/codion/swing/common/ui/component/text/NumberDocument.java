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

import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.swing.common.ui.component.text.NumberDocument.NumberParser.NumberParseResult;

import org.jspecify.annotations.Nullable;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Objects;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
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
		if (number != null) {
			getDocumentFilter().validateRange(number);
		}
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
				replace(0, getLength(), text, null);
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
			if (clazz.equals(BigInteger.class) && format instanceof DecimalFormat) {
				// a Long or Double can not hold every BigInteger
				((DecimalFormat) format).setParseBigDecimal(true);
			}
		}

		@Override
		public NumberParseResult<T> parse(String string) {
			if (string.isEmpty()) {
				return new DefaultNumberParseResult<>(string, null);
			}
			if (string.equals(negativePrefix())) {
				// the start of a negative number
				return new DefaultNumberParseResult<>(string, null);
			}

			Number number = parseFormat(string);
			T parsedNumber = number == null ? null : (T) toType(clazz, number);
			if (parsedNumber != null) {
				String formattedNumber = format.format(parsedNumber);
				if (negativeZero(string, parsedNumber)) {
					// BigDecimal has no negative zero, format the Double one
					formattedNumber = format.format(-0d);
				}
				//handle trailing decimal symbol and trailing decimal zeros, within the maximum fraction digits
				if (format instanceof DecimalFormat && format.getMaximumFractionDigits() > 0) {
					String decimalSeparator =
									String.valueOf(((DecimalFormat) format).getDecimalFormatSymbols().getDecimalSeparator());
					if (!formattedNumber.contains(decimalSeparator) && string.endsWith(decimalSeparator)) {
						formattedNumber += decimalSeparator;
					}
					int decimalSeparatorIndex = string.indexOf(decimalSeparator);
					if (decimalSeparatorIndex >= 0 && string.substring(decimalSeparatorIndex).endsWith("0")) {
						formattedNumber += (formattedNumber.contains(decimalSeparator) ? "" : decimalSeparator) +
										trailingDecimalZeros(string, decimalSeparatorIndex,
														format.getMaximumFractionDigits() - fractionDigits(formattedNumber, decimalSeparator));
					}
				}

				// the value of the formatted text, which may have dropped fraction digits
				return new DefaultNumberParseResult<>(formattedNumber, parseNumber(formattedNumber),
								countAddedGroupingSeparators(string, formattedNumber), true);
			}
			if (number != null && finite(number)) {
				// exceeds the range of the number type
				return DefaultNumberParseResult.overflow(string, number);
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
		 * @return the prefix of a negative number
		 */
		final String negativePrefix() {
			return format instanceof DecimalFormat ? ((DecimalFormat) format).getNegativePrefix() : MINUS_SIGN;
		}

		/**
		 * @param string the parsed string
		 * @param number the parsed number
		 * @return true if the string represents a negative zero, which a BigDecimal can not hold
		 */
		private boolean negativeZero(String string, T number) {
			if (number instanceof BigDecimal && ((BigDecimal) number).signum() == 0 && format instanceof DecimalFormat) {
				String negativePrefix = ((DecimalFormat) format).getNegativePrefix();

				return !negativePrefix.isEmpty() && string.startsWith(negativePrefix);
			}

			return false;
		}

		/**
		 * @param text the text to parse
		 * @return a number if the format can parse it and it fits the number type, null otherwise
		 */
		private @Nullable T parseNumber(String text) {
			Number number = parseFormat(text);

			return number == null ? null : (T) toType(clazz, number);
		}

		/**
		 * @param text the text to parse
		 * @return the number parsed by the format, null if it can not parse the text
		 */
		private @Nullable Number parseFormat(String text) {
			if (text.isEmpty()) {
				return null;
			}

			ParsePosition position = new ParsePosition(0);
			Number number = format.parse(text, position);
			if (position.getIndex() != text.length() || position.getErrorIndex() != -1) {
				return null;
			}

			return number;
		}

		private static boolean finite(Number number) {
			return !(number instanceof Double || number instanceof Float) || Double.isFinite(number.doubleValue());
		}

		private static @Nullable Number toType(Class<? extends Number> clazz, Number number) {
			if (clazz.equals(Short.class)) {
				return toShort(number);
			}
			if (clazz.equals(Integer.class)) {
				return toInteger(number);
			}
			else if (clazz.equals(BigInteger.class)) {
				return toBigInteger(number);
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

		private static @Nullable Number toShort(Number number) {
			BigInteger integer = toBigInteger(number);

			return integer == null || integer.bitLength() >= Short.SIZE ? null : integer.shortValue();
		}

		private static @Nullable Number toInteger(Number number) {
			BigInteger integer = toBigInteger(number);

			return integer == null || integer.bitLength() >= Integer.SIZE ? null : integer.intValue();
		}

		private static @Nullable Number toLong(Number number) {
			BigInteger integer = toBigInteger(number);

			return integer == null || integer.bitLength() >= Long.SIZE ? null : integer.longValue();
		}

		private static Number toDouble(Number number) {
			if (number instanceof Double) {
				return number;
			}

			return Double.valueOf(number.doubleValue());
		}

		private static Number toBigDecimal(Number number) {
			if (number instanceof BigDecimal) {
				return number;
			}

			return BigDecimal.valueOf(number.doubleValue());
		}

		/**
		 * @param number the number
		 * @return the integer part of the given number, null in case of NaN or infinity
		 */
		private static @Nullable BigInteger toBigInteger(Number number) {
			if (number instanceof BigInteger) {
				return (BigInteger) number;
			}
			if (number instanceof BigDecimal) {
				return ((BigDecimal) number).toBigInteger();
			}
			if (number instanceof Double || number instanceof Float) {
				double value = number.doubleValue();

				return Double.isNaN(value) || Double.isInfinite(value) ? null : new BigDecimal(value).toBigInteger();
			}

			return BigInteger.valueOf(number.longValue());
		}

		private int countAddedGroupingSeparators(String currentNumber, String newNumber) {
			DecimalFormatSymbols symbols = ((DecimalFormat) format).getDecimalFormatSymbols();

			return count(newNumber, symbols.getGroupingSeparator()) - count(currentNumber, symbols.getGroupingSeparator());
		}

		/**
		 * @param string the string
		 * @param decimalSeparatorIndex the index of the decimal separator in the string
		 * @param maximum the maximum number of zeros
		 * @return the trailing decimal zeros of the given string, at most {@code maximum}
		 */
		private static String trailingDecimalZeros(String string, int decimalSeparatorIndex, int maximum) {
			StringBuilder builder = new StringBuilder();
			int index = string.length() - 1;
			char c = string.charAt(index);
			while (c == '0' && index > decimalSeparatorIndex && builder.length() < maximum) {
				builder.append('0');
				c = string.charAt(--index);
			}

			return builder.toString();
		}

		private static int fractionDigits(String number, String decimalSeparator) {
			int decimalSeparatorIndex = number.indexOf(decimalSeparator);

			return decimalSeparatorIndex < 0 ? 0 : number.length() - decimalSeparatorIndex - 1;
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

			/**
			 * @return the parsed number in case it exceeds the range of the number type, otherwise null
			 */
			@Nullable Number overflow();
		}

		protected static final class DefaultNumberParseResult<T extends Number>
						extends DefaultParseResult<T> implements NumberParseResult<T> {

			private final int charetOffset;
			private final @Nullable Number overflow;

			private DefaultNumberParseResult(String text, @Nullable T value) {
				this(text, value, 0, true);
			}

			DefaultNumberParseResult(String text, @Nullable T value, int charetOffset,
															 boolean successful) {
				this(text, value, charetOffset, successful, null);
			}

			private DefaultNumberParseResult(String text, @Nullable T value, int charetOffset,
																			 boolean successful, @Nullable Number overflow) {
				super(text, value, successful);
				this.charetOffset = charetOffset;
				this.overflow = overflow;
			}

			@Override
			public int charetOffset() {
				return charetOffset;
			}

			@Override
			public @Nullable Number overflow() {
				return overflow;
			}

			private static <T extends Number> DefaultNumberParseResult<T> overflow(String text, Number number) {
				return new DefaultNumberParseResult<>(text, null, 0, false, number);
			}
		}
	}

	static final class NumberParsingDocumentFilter<T extends Number> extends ParsingDocumentFilter<T> {

		private static final MessageBundle MESSAGES =
						messageBundle(NumberParsingDocumentFilter.class, getBundle(NumberParsingDocumentFilter.class.getName()));

		private final NumberRangeValidator rangeValidator;
		private final NumberParser<T> parser;
		private final Value<T> value = Value.nullable();

		private @Nullable JTextComponent textComponent;
		private boolean convertGroupingToDecimalSeparator = true;

		NumberParsingDocumentFilter(NumberParser<T> parser) {
			super(parser);
			this.parser = parser;
			this.rangeValidator = new NumberRangeValidator(parser.clazz);
		}

		@Override
		protected String transform(String string) {
			return convertMinusSign(convertSingleGroupingToDecimalSeparator(string));
		}

		/**
		 * A single character edit, typing, is checked against the range widened to include zero, since
		 * a value typed one digit at a time passes through it, and rejected silently, while a longer edit,
		 * such as a paste, is checked against the range itself, throwing in case of a value outside it.
		 */
		@Override
		protected boolean validate(Parser.ParseResult<T> parseResult, boolean singleCharacter) {
			// the parse result comes from the NumberParser
			Number overflow = ((NumberParseResult<T>) parseResult).overflow();
			if (overflow != null) {
				if (singleCharacter) {
					return false;
				}
				throw rangeValidator.outsideRange(overflow);
			}
			T number = parseResult.value();
			if (number != null) {
				if (singleCharacter) {
					if (!rangeValidator.withinTypingRange(number)) {
						return false;
					}
				}
				else {
					rangeValidator.validate(number);
				}
			}
			else if (parseResult.text().equals(parser.negativePrefix()) && !rangeValidator.negativeAllowed()) {
				// a lone minus sign, when negative values are not allowed
				return false;
			}

			return super.validate(parseResult, singleCharacter);
		}

		/**
		 * Replaces the whole text with the formatted one, grouping separators included
		 */
		@Override
		protected void apply(FilterBypass filterBypass, int offset, int length, String string,
												 Parser.ParseResult<T> parseResult, @Nullable AttributeSet attributeSet) throws BadLocationException {
			filterBypass.replace(0, filterBypass.getDocument().getLength(), parseResult.text(), attributeSet);
			value.set(parseResult.value());
			if (textComponent != null) {
				// the parse result comes from the NumberParser
				textComponent.getCaret().setDot(offset + string.length() + ((NumberParseResult<T>) parseResult).charetOffset());
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

		/**
		 * @param number the number to validate
		 * @throws IllegalArgumentException in case the number is outside the range
		 */
		void validateRange(Number number) {
			rangeValidator.validate(number);
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

		/**
		 * Some locales use a minus sign other than the hyphen-minus found on keyboards, such as U+2212,
		 * so interpret a hyphen-minus as the minus sign of the format.
		 */
		private String convertMinusSign(String text) {
			if (parser.format instanceof DecimalFormat) {
				return text.replace('-', ((DecimalFormat) parser.format).getDecimalFormatSymbols().getMinusSign());
			}

			return text;
		}

		private static final class NumberRangeValidator {

			private final @Nullable Number typeMinimum;
			private final @Nullable Number typeMaximum;

			private @Nullable Number minimumValue;
			private @Nullable Number maximumValue;

			private NumberRangeValidator(Class<? extends Number> numberClass) {
				this.typeMinimum = typeMinimum(numberClass);
				this.typeMaximum = typeMaximum(numberClass);
			}

			/**
			 * @param value the value to validate
			 * @throws IllegalArgumentException in case the value is outside the range
			 */
			private void validate(Number value) {
				if (!within(value, minimum(), maximum())) {
					throw outsideRange(value);
				}
			}

			/**
			 * @param value the value
			 * @return true if the value is within the range widened to include zero, which a value
			 * typed one digit at a time passes through on its way to a value within the range
			 */
			private boolean withinTypingRange(Number value) {
				Number minimum = minimum();
				Number maximum = maximum();
				if (minimum != null && minimum.doubleValue() > 0) {
					minimum = 0;
				}
				if (maximum != null && maximum.doubleValue() < 0) {
					maximum = 0;
				}

				return within(value, minimum, maximum);
			}

			private boolean negativeAllowed() {
				Number minimum = minimum();

				return minimum == null || minimum.doubleValue() < 0;
			}

			private IllegalArgumentException outsideRange(Number value) {
				return new IllegalArgumentException(MESSAGES.getString("value_outside_range") + ": " + value + " [" + minimum() + " - " + maximum() + "]");
			}

			/**
			 * @return the minimum value, the minimum of the number type in case none is specified
			 */
			private @Nullable Number minimum() {
				return minimumValue == null ? typeMinimum : minimumValue;
			}

			/**
			 * @return the maximum value, the maximum of the number type in case none is specified
			 */
			private @Nullable Number maximum() {
				return maximumValue == null ? typeMaximum : maximumValue;
			}

			private static boolean within(Number value, @Nullable Number minimum, @Nullable Number maximum) {
				return (minimum == null || value.doubleValue() >= minimum.doubleValue())
								&& (maximum == null || value.doubleValue() <= maximum.doubleValue());
			}

			private static @Nullable Number typeMinimum(Class<? extends Number> numberClass) {
				if (numberClass.equals(Short.class)) {
					return Short.MIN_VALUE;
				}
				if (numberClass.equals(Integer.class)) {
					return Integer.MIN_VALUE;
				}
				if (numberClass.equals(Long.class)) {
					return Long.MIN_VALUE;
				}

				return null;
			}

			private static @Nullable Number typeMaximum(Class<? extends Number> numberClass) {
				if (numberClass.equals(Short.class)) {
					return Short.MAX_VALUE;
				}
				if (numberClass.equals(Integer.class)) {
					return Integer.MAX_VALUE;
				}
				if (numberClass.equals(Long.class)) {
					return Long.MAX_VALUE;
				}

				return null;
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

		void setMaximumFractionDigits(int maximumFractionDigits) {
			if (maximumFractionDigits < -1) {
				throw new IllegalArgumentException("Maximum fraction digits must be => 0, or -1 for no maximum");
			}
			format().setMaximumFractionDigits(maximumFractionDigits == -1 ? MAXIMUM_FRACTION_DIGITS : maximumFractionDigits);
			setText("");
		}

		/* Automatically adds a 0 in front of a decimal separator, when it's the first character entered or follows a leading minus sign */
		private static final class DecimalDocumentParser<T extends Number> extends NumberParser<T> {

			private DecimalDocumentParser(DecimalFormat format, boolean parseBigDecimal) {
				super(format, parseBigDecimal ? (Class<T>) BigDecimal.class : (Class<T>) Double.class);
			}

			@Override
			public NumberParseResult<T> parse(String string) {
				DecimalFormat format = (DecimalFormat) format();
				String decimalSeparator = String.valueOf(format.getDecimalFormatSymbols().getDecimalSeparator());
				if (format.getMaximumFractionDigits() > 0 &&
								(string.equals(decimalSeparator) || string.equals(format.getNegativePrefix() + decimalSeparator))) {
					NumberParseResult<T> parseResult = super.parse(string.replace(decimalSeparator, "0" + decimalSeparator));

					return new DefaultNumberParseResult<>(parseResult.text(), parseResult.value(),
									parseResult.charetOffset() + 1, parseResult.successful());
				}

				return super.parse(string);
			}
		}
	}
}
