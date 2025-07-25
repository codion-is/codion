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

import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.common.property.PropertyValue;
import is.codion.framework.domain.entity.EntityType;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static is.codion.common.Configuration.*;

/**
 * Defines an Attribute configuration including validation, formatting, and behavior settings.
 * <p>
 * AttributeDefinition instances specify how attributes behave within entities, including:
 * <ul>
 *   <li>Display properties (caption, description, format patterns)
 *   <li>Validation rules (nullable, min/max values, length constraints)
 *   <li>Default values and value generation
 *   <li>UI behavior (items for combo boxes, comparators for sorting)
 *   <li>Data conversion and formatting
 * </ul>
 * <p>
 * AttributeDefinitions are created using the builder pattern through attribute definers:
 * {@snippet :
 * public class Store extends DefaultDomain {
 *
 *     interface Product {
 *         EntityType TYPE = DOMAIN.entityType("store.product");
 *
 *         Column<Integer> ID = TYPE.integerColumn("id");
 *         Column<String> NAME = TYPE.stringColumn("name");
 *         Column<String> DESCRIPTION = TYPE.stringColumn("description");
 *         Column<BigDecimal> PRICE = TYPE.bigDecimalColumn("price");
 *         Column<String> CATEGORY = TYPE.stringColumn("category");
 *         Column<Boolean> ACTIVE = TYPE.booleanColumn("active");
 *         Column<LocalDateTime> CREATED_DATE = TYPE.localDateTimeColumn("created_date");
 *     }
 *
 *     void defineProduct() {
 *         Product.TYPE.define(
 *                 Product.ID.define()
 *                     .primaryKey()
 *                     .keyGenerator(KeyGenerator.identity())
 *                     .caption("Product ID"),
 *
 *                 Product.NAME.define()
 *                     .column()
 *                     .caption("Product Name")
 *                     .nullable(false)
 *                     .maximumLength(100)
 *                     .description("The name of the product"),
 *
 *                 Product.DESCRIPTION.define()
 *                     .column()
 *                     .caption("Description")
 *                     .maximumLength(500)
 *                     .nullable(true),
 *
 *                 Product.PRICE.define()
 *                     .column()
 *                     .caption("Price")
 *                     .nullable(false)
 *                     .minimumValue(BigDecimal.ZERO)
 *                     .maximumValue(new BigDecimal("99999.99"))
 *                     .maximumFractionDigits(2)
 *                     .defaultValue(BigDecimal.ZERO),
 *
 *                 Product.CATEGORY.define()
 *                     .column()
 *                     .caption("Category")
 *                     .nullable(false)
 *                     .items(List.of(
 *                         Item.item("ELECTRONICS", "Electronics"),
 *                         Item.item("CLOTHING", "Clothing"),
 *                         Item.item("BOOKS", "Books"),
 *                         Item.item("HOME", "Home & Garden"))),
 *
 *                 Product.ACTIVE.define()
 *                     .column()
 *                     .caption("Active")
 *                     .nullable(false)
 *                     .defaultValue(true),
 *
 *                 Product.CREATED_DATE.define()
 *                     .column()
 *                     .caption("Created")
 *                     .nullable(false)
 *                     .hasDatabaseDefault(true) // Database sets this
 *                     .updatable(false))
 *             .build();
 *     }
 * }
 *}
 * @param <T> the underlying type
 * @see Attribute#define()
 * @see Builder
 */
public interface AttributeDefinition<T> {

	/**
	 * The suffix used for the mnemonic resource key.
	 * <ul>
	 * <li>name=Name
	 * <li>name.mnemonic=N
	 * </ul>
	 */
	String MNEMONIC_RESOURCE_SUFFIX = ".mnemonic";

	/**
	 * The default maximum fraction digits for floating point numbers
	 */
	int DEFAULT_MAXIMUM_FRACTION_DIGITS = 10;

	/**
	 * Specifies the default maximum number of fraction digits for double property values<br>
	 * Note that values are rounded when set.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 10
	 * </ul>
	 */
	PropertyValue<Integer> MAXIMUM_FRACTION_DIGITS = integerValue("codion.domain.maximumFractionDigits", DEFAULT_MAXIMUM_FRACTION_DIGITS);

	/**
	 * Specifies the default rounding mode used for decimal property values
	 * <ul>
	 * <li>Value type: {@link RoundingMode}
	 * <li>Default value: {@link RoundingMode#HALF_EVEN}
	 * </ul>
	 * @see #MAXIMUM_FRACTION_DIGITS
	 * @see Builder#decimalRoundingMode(RoundingMode)
	 */
	PropertyValue<RoundingMode> DECIMAL_ROUNDING_MODE = enumValue("codion.domain.decimalRoundingMode", RoundingMode.class, RoundingMode.HALF_EVEN);

	/**
	 * The default date format pattern to use when showing time values in tables and when creating default time input fields
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: HH:mm
	 * </ul>
	 */
	PropertyValue<String> TIME_FORMAT = stringValue("codion.domain.timeFormat", LocaleDateTimePattern.builder()
					.hoursMinutes()
					.build()
					.timePattern()
					.orElseThrow(IllegalStateException::new));

	/**
	 * The default date/time format pattern to use when showing date/time values in tables and when creating default date/time input fields
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: dd-MM-yyyy HH:mm [month/day order is locale specific]
	 * </ul>
	 */
	PropertyValue<String> DATE_TIME_FORMAT = stringValue("codion.domain.dateTimeFormat", LocaleDateTimePattern.builder()
					.delimiterDash()
					.yearFourDigits()
					.hoursMinutes()
					.build()
					.dateTimePattern());

	/**
	 * The default date format pattern to use when showing date values in tables and when creating default date input fields
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: dd-MM-yyyy [month/day order is locale specific]
	 * </ul>
	 */
	PropertyValue<String> DATE_FORMAT = stringValue("codion.domain.dateFormat", LocaleDateTimePattern.builder()
					.delimiterDash()
					.yearFourDigits()
					.build()
					.datePattern());

	/**
	 * Specifies whether number format grouping is used by default
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> NUMBER_FORMAT_GROUPING = booleanValue("codion.domain.numberFormatGrouping", false);

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
	 * The {@link Attribute} this definition is based on, should be unique within an Entity.
	 * By default, the {@link Attribute#name()} serves as column name for database columns.
	 * @return the attribute this definition is based on
	 */
	Attribute<T> attribute();

	/**
	 * @return the type of the entity this attribute is associated with
	 */
	EntityType entityType();

	/**
	 * @return the caption or the attribute name if no caption has been specified
	 * @see Attribute#name()
	 */
	String caption();

	/**
	 * @return a String describing this attribute or an empty Optional if none is available
	 */
	Optional<String> description();

	/**
	 * Returns a string representation of the given value formatted with this attributes format.
	 * If no format is available {@link Object#toString()} is used. By default, null values result in an empty string.
	 * @param value the value to format.
	 * @return the value formatted as a string
	 * @see Builder#format(Format)
	 * @see Builder#dateTimePattern(String)
	 */
	String string(@Nullable T value);

	/**
	 * @return true if a default value has been set for this attribute
	 */
	boolean hasDefaultValue();

	/**
	 * @return the default value for this attribute, if no default value has been set null is returned
	 * @see #hasDefaultValue()
	 */
	@Nullable T defaultValue();

	/**
	 * @return true if this attribute should be hidden in table views
	 */
	boolean hidden();

	/**
	 * @return the maximum allowed value for this attribute, an empty Optional if none is defined,
	 * only applicable to numerical attributes
	 */
	Optional<Number> maximumValue();

	/**
	 * @return the minimum allowed value for this attribute, an empty Optional if none is defined,
	 * only applicable to numerical attributes
	 */
	Optional<Number> minimumValue();

	/**
	 * @return the maximum number of fraction digits to use for this attribute value,
	 * -1 if this attribute is not based on Types.DOUBLE or Types.DECIMAL
	 * @see #decimalRoundingMode()
	 */
	int maximumFractionDigits();

	/**
	 * @return the rounding mode to use when working with decimal values
	 * @see #DECIMAL_ROUNDING_MODE
	 * @see #maximumFractionDigits()
	 */
	RoundingMode decimalRoundingMode();

	/**
	 * @return true if null is a valid value for this attribute
	 */
	boolean nullable();

	/**
	 * The value of a derived attribute can not be set, as it's value is derived from other values
	 * @return true if the value of this attribute is derived from one or more values
	 */
	boolean derived();

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
	 * Returns the mnemonic associated with this attribute.
	 * @return the mnemonic to use when creating a label for this attribute, 0 meaning no mnemonic
	 */
	char mnemonic();

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
	 * @return the Comparator to use when comparing values associated with this attribute
	 */
	Comparator<T> comparator();

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
	 * Supplies values, for example default ones.
	 * @param <T> the value type
	 */
	interface ValueSupplier<T> extends Serializable {

		/**
		 * @return the value
		 */
		@Nullable T get();
	}

	/**
	 * Builds a attribute definition instance
	 * @param <T> the value type
	 * @param <B> the builder type
	 */
	interface Builder<T, B extends Builder<T, B>> {

		/**
		 * @return the underying attribute
		 */
		Attribute<T> attribute();

		/**
		 * Note that this method has a side effect, when setting the caption to a null value
		 * this attribute is automatically hidden via {@link #hidden(boolean)}, when
		 * a non-null value is used it is automatically made visible (as in, not hidden).
		 * @param caption the caption
		 * @return this builder instance
		 * @see #hidden(boolean)
		 */
		B caption(String caption);

		/**
		 * Specifies the key to use when retrieving the caption for this attribute from the entity resource bundle,
		 * in case it differs from the attribute name ({@link Attribute#name()}), which is the default value.
		 * Note that this configures the attribute to not be hidden.
		 * @param captionResourceKey the caption resource bundle key
		 * @return this builder instance
		 * @throws IllegalStateException in case the caption has already been set
		 * @throws IllegalStateException in case no resource bundle is specified for the entity
		 * @throws IllegalStateException in case the caption resource is not found in the entity resource bundle
		 * @see EntityType#resourceBundleName()
		 */
		B captionResourceKey(String captionResourceKey);

		/**
		 * Specifies the key to use when retrieving the mnemonic for this attribute from the entity resource bundle,
		 * in case it differs from {@link Attribute#name()} + {@link #MNEMONIC_RESOURCE_SUFFIX}, which is the default value.
		 * @param mnemonicResourceKey the mnemonic resource bundle key
		 * @return this builder instance
		 * @throws IllegalStateException in case the mnemonic has already been set
		 * @throws IllegalStateException in case no resource bundle is specified for the entity
		 * @throws IllegalStateException in case the mnemonic resource is not found in the entity resource bundle
		 * @see EntityType#resourceBundleName()
		 */
		B mnemonicResourceKey(String mnemonicResourceKey);

		/**
		 * Sets the default value for this attribute, overrides the underlying column default value, if any
		 * @param defaultValue the value to use as default
		 * @return this builder instance
		 */
		B defaultValue(T defaultValue);

		/**
		 * Sets the default value supplier, use in case of dynamic default values.
		 * @param supplier the default value supplier
		 * @return this builder instance
		 */
		B defaultValue(ValueSupplier<T> supplier);

		/**
		 * Specifies whether this attribute should be hidden in table views
		 * @param hidden true if this attribute should be hidden
		 * @return this builder instance
		 */
		B hidden(boolean hidden);

		/**
		 * Only applicable to numerical attributes
		 * @param minimumValue the minimum allowed value for this attribute
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a numerical attribute
		 */
		B minimumValue(Number minimumValue);

		/**
		 * Only applicable to numerical attributes
		 * @param maximumValue the maximum allowed value for this attribute
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a numerical attribute
		 */
		B maximumValue(Number maximumValue);

		/**
		 * Only applicable to numerical attributes
		 * @param minimumValue the minimum allowed value for this attribute
		 * @param maximumValue the maximum allowed value for this attribute
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a numerical attribute
		 */
		B valueRange(Number minimumValue, Number maximumValue);

		/**
		 * Sets the maximum fraction digits to show for this attribute, only applicable to attributes based on decimal types.
		 * This setting is overridden during subsequent calls to {@link #format(Format)}.
		 * Note that values associated with this attribute are automatically rounded to {@code maximumFractionDigits} digits.
		 * @param maximumFractionDigits the maximum fraction digits
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a decimal attribute
		 * @see #decimalRoundingMode(RoundingMode)
		 */
		B maximumFractionDigits(int maximumFractionDigits);

		/**
		 * Sets the rounding mode to use when working with decimals
		 * @param decimalRoundingMode the rounding mode
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a decimal attribute
		 * @see #maximumFractionDigits(int)
		 */
		B decimalRoundingMode(RoundingMode decimalRoundingMode);

		/**
		 * Specifies whether to use number grouping when presenting the value associated with this attribute.
		 * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale.
		 * By default, grouping is not used.
		 * Only applicable to numerical attributes.
		 * This setting is overridden during subsequent calls to {@link #format(Format)}
		 * @param numberFormatGrouping if true then number grouping is used
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a numerical attribute
		 */
		B numberFormatGrouping(boolean numberFormatGrouping);

		/**
		 * Specifies whether this attribute is nullable. Note that this will not prevent
		 * the value from being set to null, only prevent successful validation of the entity.
		 * @param nullable specifies whether null is a valid value for this attribute
		 * @return this builder instance
		 */
		B nullable(boolean nullable);

		/**
		 * Sets the maximum length of this attribute value, this applies to String (varchar) based attributes
		 * @param maximumLength the maximum length
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a String attribute
		 */
		B maximumLength(int maximumLength);

		/**
		 * Specifies whether string values should be trimmed, this applies to String (varchar) based attributes.
		 * @param trim true if strings values should be trimmed
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a String attribute
		 * @see String#trim()
		 * @see #TRIM_STRINGS
		 */
		B trim(boolean trim);

		/**
		 * Sets the mnemonic to use when creating a label for this attribute
		 * @param mnemonic the mnemonic character
		 * @return this builder instance
		 */
		B mnemonic(char mnemonic);

		/**
		 * Sets the description for this attribute, used for tooltips f.ex.
		 * @param description a String describing this attribute
		 * @return this builder instance
		 */
		B description(String description);

		/**
		 * @param comparator the Comparator to use when comparing values for this attribute
		 * @return this builder instance
		 */
		B comparator(Comparator<T> comparator);

		/**
		 * Sets the Format to use when presenting attribute values
		 * @param format the format to use
		 * @return this builder instance
		 * @throws NullPointerException in case format is null
		 * @throws IllegalArgumentException in case this is a numerical attribute and the given format is not a NumberFormat.
		 * @throws IllegalStateException if the underlying attribute is temporal, in which case
		 * {@link #dateTimePattern(String)} or {@link #localeDateTimePattern(LocaleDateTimePattern)} should be used.
		 */
		B format(Format format);

		/**
		 * Sets the date/time format pattern used when presenting and inputtind values
		 * @param dateTimePattern the format pattern
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the pattern is invalid
		 * @throws IllegalStateException in case this is not a temporal attribute
		 * @throws IllegalStateException in case {@link #localeDateTimePattern(LocaleDateTimePattern)} has been set
		 */
		B dateTimePattern(String dateTimePattern);

		/**
		 * Sets the locale aware date/time format pattern used when presenting and inputting values
		 * @param localeDateTimePattern the format pattern
		 * @return this builder instance
		 * @throws IllegalStateException in case this is not a temporal attribute
		 * @throws IllegalStateException in case {@link #dateTimePattern(String)} has been set
		 */
		B localeDateTimePattern(LocaleDateTimePattern localeDateTimePattern);

		/**
		 * Note that by default items are sorted by to their caption, not their value.
		 * Use {@link #comparator(Comparator)} to set a custom comparator.
		 * @param items the {@link Item}s representing all the valid values for this attribute
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the valid item list contains duplicate values
		 */
		B items(List<Item<T>> items);

		/**
		 * Builds a new attribute definition instance
		 * @return a new attribute definition instance based on this builder
		 */
		AttributeDefinition<T> build();
	}
}
