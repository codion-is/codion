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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.utilities.format.LocaleDateTimePattern;
import is.codion.common.utilities.item.Item;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.domain.entity.attribute.AbstractValueAttributeDefinition.AbstractValueAttributeDefinitionBuilder;

import org.jspecify.annotations.Nullable;

import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static is.codion.common.utilities.Configuration.*;

/**
 * Defines an Attribute that holds concrete values (String, Integer, BigDecimal, etc.)
 * requiring type-specific validation, formatting, and constraints.
 * <p>
 * Contrast with {@link ForeignKeyDefinition} which holds Entity references and delegates
 * formatting to the referenced entity's stringFactory.
 * @param <T> the value type
 * @see AttributeDefinition
 * @see ColumnDefinition
 * @see DerivedAttributeDefinition
 * @see TransientAttributeDefinition
 */
public sealed interface ValueAttributeDefinition<T> extends AttributeDefinition<T>
				permits AbstractValueAttributeDefinition, ColumnDefinition, DerivedAttributeDefinition, TransientAttributeDefinition {

	/**
	 * The default maximum fraction digits for floating point numbers
	 */
	int DEFAULT_FRACTION_DIGITS = 10;

	/**
	 * Specifies the default maximum number of fraction digits for double property values<br>
	 * Note that values are rounded when set.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 10
	 * </ul>
	 */
	PropertyValue<Integer> FRACTION_DIGITS = integerValue("codion.domain.fractionDigits", DEFAULT_FRACTION_DIGITS);

	/**
	 * Specifies the default rounding mode used for decimal property values
	 * <ul>
	 * <li>Value type: {@link RoundingMode}
	 * <li>Default value: {@link RoundingMode#HALF_EVEN}
	 * </ul>
	 * @see #FRACTION_DIGITS
	 * @see Builder#roundingMode(RoundingMode)
	 */
	PropertyValue<RoundingMode> ROUNDING_MODE = enumValue("codion.domain.roundingMode", RoundingMode.class, RoundingMode.HALF_EVEN);

	/**
	 * Specifies whether number grouping is used by default
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> NUMBER_GROUPING = booleanValue("codion.domain.numberGrouping", false);

	/**
	 * Specifies the default number grouping separator
	 * <ul>
	 * <li>Value type: Character
	 * <li>Default value: The grouping separator for the default locale
	 * </ul>
	 */
	PropertyValue<Character> GROUPING_SEPARATOR = characterValue("codion.domain.groupingSeparator",
					DecimalFormatSymbols.getInstance().getGroupingSeparator());

	/**
	 * Specifies the default number decimal separator.
	 * <ul>
	 * <li>Value type: Character
	 * <li>Default value: The decimal separator for the default locale
	 * </ul>
	 */
	PropertyValue<Character> DECIMAL_SEPARATOR = characterValue("codion.domain.decimalSeparator",
					DecimalFormatSymbols.getInstance().getDecimalSeparator());

	/**
	 * Specifies whether String values should use a lexical comparator by default
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> USE_LEXICAL_STRING_COMPARATOR = booleanValue("codion.domain.useLexicalStringComparator", true);

	/**
	 * Specifies whether String values should be trimmed by default
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see String#trim()
	 */
	PropertyValue<Boolean> TRIM_STRINGS = booleanValue("codion.domain.trimStrings", true);

	/**
	 * @return the maximum allowed value for this attribute, an empty Optional if none is defined,
	 * only applicable to numerical attributes
	 */
	Optional<Number> maximum();

	/**
	 * @return the minimum allowed value for this attribute, an empty Optional if none is defined,
	 * only applicable to numerical attributes
	 */
	Optional<Number> minimum();

	/**
	 * @return the maximum number of fraction digits to use for this attribute value,
	 * -1 if this attribute is not based on Types.DOUBLE or Types.DECIMAL
	 * @see #roundingMode()
	 */
	int fractionDigits();

	/**
	 * @return the rounding mode to use when working with decimal values
	 * @see ValueAttributeDefinition#ROUNDING_MODE
	 * @see #fractionDigits()
	 */
	RoundingMode roundingMode();

	/**
	 * @return the maximum length of this attribute value, -1 is returned if the maximum length is undefined,
	 * this only applies to String (varchar) based attributes
	 */
	int maximumLength();

	/**
	 * @return if string values should be trimmed, this applies to String (varchar) based attributes
	 */
	boolean trim();

	/**
	 * Returns the Format used when presenting values for this attribute, an empty Optional if none has been specified.
	 * @return the Format object used to format the value of attributes when being presented
	 */
	Optional<Format> format();

	/**
	 * Returns the date time format pattern used when presenting and inputting values for this attribute.
	 * @return the date/time format pattern
	 */
	Optional<String> dateTimePattern();

	/**
	 * Returns the date time formatter used when presenting and inputting values for this attribute.
	 * @return the DateTimeFormatter for this attribute or an empty Optional if this is not a date/time based attribute
	 */
	Optional<DateTimeFormatter> dateTimeFormatter();

	/**
	 * Validates the given value against the valid items for this attribute.
	 * Always returns true if this is not an item based attribute.
	 * @param value the value to validate
	 * @return true if the given value is a valid item for this attribute
	 * @see #items()
	 */
	boolean validItem(@Nullable T value);

	/**
	 * Returns the valid items for this attribute or an empty list in
	 * case this is not an item based attribute
	 * @return an unmodifiable view of the valid items for this attribute
	 */
	List<Item<T>> items();

	/**
	 * The value of a derived attribute can not be set, as it's value is derived from other values
	 * @return true if the value of this attribute is derived from one or more values
	 */
	boolean derived();

	/**
	 * Builds a ValueAttributeDefinition instance
	 * @param <T> the value type
	 * @param <B> the builder type
	 */
	sealed interface Builder<T, B extends Builder<T, B>> extends AttributeDefinition.Builder<T, B>
					permits AbstractValueAttributeDefinitionBuilder, ColumnDefinition.Builder,
					DerivedAttributeDefinition.DerivedBuilder, DerivedAttributeDefinition.DenormalizedBuilder,
					TransientAttributeDefinition.Builder {

		/**
		 * Only applicable to numerical attributes
		 * @param minimum the minimum allowed value for this attribute
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a numerical attribute
		 */
		B minimum(Number minimum);

		/**
		 * Only applicable to numerical attributes
		 * @param maximum the maximum allowed value for this attribute
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a numerical attribute
		 */
		B maximum(Number maximum);

		/**
		 * Only applicable to numerical attributes
		 * @param minimum the minimum allowed value for this attribute
		 * @param maximum the maximum allowed value for this attribute
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a numerical attribute
		 */
		B range(Number minimum, Number maximum);

		/**
		 * Sets the maximum length of this attribute value, this applies to String (varchar) based attributes
		 * @param maximumLength the maximum length
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a String attribute
		 */
		B maximumLength(int maximumLength);

		/**
		 * Sets the maximum fraction digits to show for this attribute, only applicable to attributes based on decimal types.
		 * This setting is overridden during subsequent calls to {@link #format(Format)}.
		 * Note that values associated with this attribute are automatically rounded to {@link #fractionDigits()} digits.
		 * @param fractionDigits the maximum fraction digits
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a decimal attribute
		 * @see #roundingMode(RoundingMode)
		 */
		B fractionDigits(int fractionDigits);

		/**
		 * Sets the rounding mode to use when working with decimals
		 * @param roundingMode the rounding mode
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a decimal attribute
		 * @see #fractionDigits(int)
		 */
		B roundingMode(RoundingMode roundingMode);

		/**
		 * Specifies whether to use number grouping when presenting the value associated with this attribute.
		 * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale.
		 * By default, grouping is not used.
		 * Only applicable to numerical attributes.
		 * This setting is overridden during subsequent calls to {@link #format(Format)}
		 * @param numberGrouping if true then number grouping is used
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a numerical attribute
		 */
		B numberGrouping(boolean numberGrouping);

		/**
		 * Sets the Format to use when presenting attribute values
		 * @param format the format to use
		 * @return this builder instance
		 * @throws NullPointerException in case format is null
		 * @throws IllegalArgumentException in case this is a numerical attribute and the given format is not a NumberFormat.
		 * @throws IllegalStateException if the underlying attribute is temporal, in which case
		 * {@link #dateTimePattern(String)} or {@link #dateTimePattern(LocaleDateTimePattern)} should be used.
		 */
		B format(Format format);

		/**
		 * Sets the date/time format pattern used when presenting and inputtind values
		 * @param dateTimePattern the format pattern
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the pattern is invalid
		 * @throws IllegalStateException in case this is not a temporal attribute
		 * @throws IllegalStateException in case {@link #dateTimePattern(LocaleDateTimePattern)} has been set
		 */
		B dateTimePattern(String dateTimePattern);

		/**
		 * Sets the locale aware date/time format pattern used when presenting and inputting values
		 * @param dateTimePattern the format pattern
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a temporal attribute
		 * @throws IllegalStateException in case {@link #dateTimePattern(String)} has been set
		 */
		B dateTimePattern(LocaleDateTimePattern dateTimePattern);

		/**
		 * Specifies whether string values should be trimmed, this applies to String (varchar) based attributes.
		 * @param trim true if strings values should be trimmed
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a String attribute
		 * @see String#trim()
		 * @see ValueAttributeDefinition#TRIM_STRINGS
		 */
		B trim(boolean trim);

		/**
		 * Note that by default items are sorted by to their caption, not their value.
		 * Use {@link #comparator(java.util.Comparator)} to set a custom comparator.
		 * @param items the {@link Item}s representing all the valid values for this attribute
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the valid item list contains duplicate values
		 */
		B items(List<Item<T>> items);

		@Override
		ValueAttributeDefinition<T> build();
	}
}
