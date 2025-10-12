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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.Text;
import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.common.resource.MessageBundle;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.DefaultColumnDefinition.DefaultColumnDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultDerivedAttributeDefinition.DefaultDenormalizedAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultDerivedAttributeDefinition.DefaultDerivedAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultForeignKeyDefinition.DefaultForeignKeyDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultTransientAttributeDefinition.DefaultTransientAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.exception.ItemValidationException;
import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toMap;

abstract sealed class AbstractAttributeDefinition<T> implements AttributeDefinition<T>, Serializable
				permits DefaultColumnDefinition, DefaultForeignKeyDefinition, DefaultDerivedAttributeDefinition, DefaultTransientAttributeDefinition {

	@Serial
	private static final long serialVersionUID = 1;

	private static final MessageBundle MESSAGES =
					messageBundle(AbstractAttributeDefinition.class, getBundle(AbstractAttributeDefinition.class.getName()));

	private static final String VALUE_REQUIRED_KEY = "value_is_required";
	private static final String INVALID_ITEM_VALUE_KEY = "invalid_item_value";
	private static final String INVALID_ITEM_SUFFIX_KEY = "invalid_item_suffix";
	private static final String INVALID_ITEM_SUFFIX = MESSAGES.getString(INVALID_ITEM_SUFFIX_KEY);

	private static final Comparator<String> LEXICAL_COMPARATOR = Text.collator();
	private static final Comparator<Comparable<Object>> COMPARABLE_COMPARATOR = new DefaultComparator();
	private static final Comparator<Object> TO_STRING_COMPARATOR = new ToStringComparator();
	private static final ValueSupplier<Object> DEFAULT_VALUE_SUPPLIER = new NullDefaultValueSupplier();

	private final Attribute<T> attribute;
	private final @Nullable String caption;
	private final @Nullable String captionResourceBundleName;
	private final @Nullable String mnemonicResourceBundleName;
	private final @Nullable String descriptionResourceBundleName;
	private final String captionResourceKey;
	private final String mnemonicResourceKey;
	private final String descriptionResourceKey;
	private final ValueSupplier<T> defaultValueSupplier;
	private final boolean nullable;
	private final boolean hidden;
	private final int maximumLength;
	private final boolean trim;
	private final @Nullable Number maximum;
	private final @Nullable Number minimum;
	private final @Nullable String description;
	private final char mnemonic;
	private final @Nullable Format format;
	private final @Nullable LocaleDateTimePattern localeDateTimePattern;
	private final RoundingMode roundingMode;
	private final Comparator<T> comparator;
	private final @Nullable List<Item<T>> items;
	private final @Nullable Map<T, Item<T>> itemMap;
	private transient @Nullable String resourceCaption;
	private transient @Nullable Character resourceMnemonic;
	private transient @Nullable String resourceDescription;
	private transient @Nullable String dateTimePattern;
	private transient @Nullable DateTimeFormatter dateTimeFormatter;

	protected AbstractAttributeDefinition(AbstractAttributeDefinitionBuilder<T, ?> builder) {
		requireNonNull(builder);
		this.attribute = builder.attribute;
		this.caption = builder.caption;
		this.captionResourceBundleName = builder.captionResourceBundleName;
		this.mnemonicResourceBundleName = builder.mnemonicResourceBundleName;
		this.descriptionResourceBundleName = builder.descriptionResourceBundleName;
		this.captionResourceKey = builder.captionResourceKey;
		this.mnemonicResourceKey = builder.mnemonicResourceKey;
		this.descriptionResourceKey = builder.descriptionResourceKey;
		this.defaultValueSupplier = builder.defaultValueSupplier;
		this.nullable = builder.nullable;
		this.hidden = builder.hidden;
		this.maximumLength = builder.maximumLength;
		this.trim = builder.trim;
		this.maximum = builder.maximum;
		this.minimum = builder.minimum;
		this.description = builder.description;
		this.mnemonic = builder.mnemonic;
		this.format = builder.format;
		this.localeDateTimePattern = builder.localeDateTimePattern;
		this.roundingMode = builder.roundingMode;
		this.comparator = builder.comparator;
		this.dateTimePattern = builder.dateTimePattern;
		this.dateTimeFormatter = builder.dateTimeFormatter;
		this.items = builder.items;
		this.itemMap = items == null ? null : items.stream()
						.collect(toMap(Item::get, Function.identity()));
	}

	@Override
	public final String toString() {
		return caption();
	}

	@Override
	public Attribute<T> attribute() {
		return attribute;
	}

	@Override
	public boolean derived() {
		return false;
	}

	@Override
	public final EntityType entityType() {
		return attribute.entityType();
	}

	@Override
	public final boolean hidden() {
		return hidden;
	}

	@Override
	public final boolean hasDefaultValue() {
		return !(defaultValueSupplier instanceof AbstractAttributeDefinition.NullDefaultValueSupplier);
	}

	@Override
	public final @Nullable T defaultValue() {
		return defaultValueSupplier.get();
	}

	@Override
	public final boolean nullable() {
		return nullable;
	}

	@Override
	public final int maximumLength() {
		return maximumLength;
	}

	@Override
	public final boolean trim() {
		return trim;
	}

	@Override
	public final Optional<Number> maximum() {
		return Optional.ofNullable(maximum);
	}

	@Override
	public final Optional<Number> minimum() {
		return Optional.ofNullable(minimum);
	}

	@Override
	public final Optional<String> description() {
		if (descriptionResourceBundleName != null) {
			if (resourceDescription == null) {
				ResourceBundle bundle = getBundle(descriptionResourceBundleName);
				resourceDescription = bundle.containsKey(descriptionResourceKey) ? bundle.getString(descriptionResourceKey) : "";
			}

			if (!resourceDescription.isEmpty()) {
				return Optional.of(resourceDescription);
			}
		}

		return Optional.ofNullable(description);
	}

	@Override
	public final Optional<Format> format() {
		return Optional.ofNullable(format);
	}

	@Override
	public final Optional<String> dateTimePattern() {
		return Optional.ofNullable(dateTimePatternInternal());
	}

	@Override
	public final Optional<DateTimeFormatter> dateTimeFormatter() {
		return Optional.ofNullable(dateTimeFormatterInternal());
	}

	@Override
	public final Comparator<T> comparator() {
		return comparator;
	}

	@Override
	public final boolean validItem(@Nullable T value) {
		return itemMap == null || itemMap.containsKey(value);
	}

	@Override
	public final List<Item<T>> items() {
		return items == null ? emptyList() : items;
	}

	@Override
	public final int fractionDigits() {
		if (!(format instanceof NumberFormat)) {
			return -1;
		}

		return ((NumberFormat) format).getMaximumFractionDigits();
	}

	@Override
	public final RoundingMode roundingMode() {
		return roundingMode;
	}

	@Override
	public final String caption() {
		if (captionResourceBundleName != null) {
			if (resourceCaption == null) {
				ResourceBundle bundle = getBundle(captionResourceBundleName);
				resourceCaption = bundle.containsKey(captionResourceKey) ? bundle.getString(captionResourceKey) : "";
			}

			if (!resourceCaption.isEmpty()) {
				return resourceCaption;
			}
		}

		return caption == null ? attribute.name() : caption;
	}

	@Override
	public final char mnemonic() {
		if (mnemonicResourceBundleName != null) {
			if (resourceMnemonic == null) {
				ResourceBundle bundle = getBundle(mnemonicResourceBundleName);
				String mnemonicResource = bundle.containsKey(mnemonicResourceKey) ? bundle.getString(mnemonicResourceKey) : "";
				if (!mnemonicResource.isEmpty()) {
					resourceMnemonic = mnemonicResource.charAt(0);
				}
				else {
					resourceMnemonic = 0;
				}
			}

			if (resourceMnemonic.charValue() != 0) {
				return resourceMnemonic;
			}
		}

		return mnemonic;
	}

	@Override
	public final void validate(Entity entity) {
		validate(entity, nullable());
	}

	@Override
	public final void validate(Entity entity, boolean nullable) {
		requireNonNull(entity);
		if (!(attribute instanceof Column) || !entity.definition().foreignKeys().foreignKeyColumn((Column<?>) attribute)) {
			validateNull(entity, nullable);
		}
		T value = entity.get(attribute);
		if (!items().isEmpty()) {
			validateItem(value);
		}
		if (value != null) {
			if (attribute.type().isNumeric()) {
				validateRange((Number) value);
			}
			else if (attribute.type().isString()) {
				validateLength((String) value);
			}
		}
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		AbstractAttributeDefinition<?> that = (AbstractAttributeDefinition<?>) obj;

		return attribute.equals(that.attribute);
	}

	@Override
	public final int hashCode() {
		return attribute.hashCode();
	}

	@Override
	public final String format(T value) {
		if (itemMap != null) {
			Item<T> item = itemMap.get(value);

			return item == null ? invalidItemString(value) : item.caption();
		}

		if (value == null) {
			return "";
		}
		if (attribute.type().isTemporal()) {
			DateTimeFormatter formatter = dateTimeFormatterInternal();
			if (formatter != null) {
				return formatter.format((TemporalAccessor) value);
			}
		}
		if (format != null) {
			return format.format(value);
		}

		return value.toString();
	}

	private @Nullable String dateTimePatternInternal() {
		if (dateTimePattern == null) {
			dateTimePattern = localeDateTimePattern == null ? defaultDateTimePattern() : localeDateTimePattern.dateTimePattern();
		}

		return dateTimePattern;
	}

	private @Nullable DateTimeFormatter dateTimeFormatterInternal() {
		if (dateTimeFormatter == null) {
			String pattern = dateTimePatternInternal();
			dateTimeFormatter = pattern == null ? null : ofPattern(pattern);
		}

		return dateTimeFormatter;
	}

	private String invalidItemString(T value) {
		if (value == null && nullable()) {
			//technically valid
			return "";
		}
		//mark invalid values
		return value + " <" + INVALID_ITEM_SUFFIX + ">";
	}

	private @Nullable String defaultDateTimePattern() {
		if (attribute.type().isLocalDate()) {
			return DATE_FORMAT.getOrThrow();
		}
		else if (attribute.type().isLocalTime()) {
			return TIME_FORMAT.getOrThrow();
		}
		else if (attribute.type().isLocalDateTime()) {
			return DATE_TIME_FORMAT.getOrThrow();
		}
		else if (attribute.type().isOffsetDateTime()) {
			return DATE_TIME_FORMAT.getOrThrow();
		}

		return null;
	}

	private void validateNull(Entity entity, boolean nullable) throws NullValidationException {
		if (!nullable && entity.isNull(attribute)) {
			if ((entity.primaryKey().isNull() || entity.originalPrimaryKey().isNull()) && !(attribute instanceof ForeignKey)) {
				//a new entity being inserted, allow null for columns with default values and generated primary key values
				boolean nonKeyColumnWithoutDefaultValue = isNonKeyColumnWithoutDefaultValue();
				boolean primaryKeyColumnWithoutAutoGenerate = isNonGeneratedPrimaryKeyColumn();
				if (nonKeyColumnWithoutDefaultValue || primaryKeyColumnWithoutAutoGenerate) {
					throw new NullValidationException(attribute,
									MessageFormat.format(MESSAGES.getString(VALUE_REQUIRED_KEY), caption()));
				}
			}
			else {
				throw new NullValidationException(attribute,
								MessageFormat.format(MESSAGES.getString(VALUE_REQUIRED_KEY), caption()));
			}
		}
	}

	private boolean isNonGeneratedPrimaryKeyColumn() {
		return (this instanceof ColumnDefinition
						&& ((ColumnDefinition<?>) this).primaryKey()
						&& !((ColumnDefinition<?>) this).generated());
	}

	private boolean isNonKeyColumnWithoutDefaultValue() {
		return this instanceof ColumnDefinition
						&& !((ColumnDefinition<?>) this).primaryKey()
						&& !((ColumnDefinition<?>) this).withDefault();
	}

	private void validateItem(@Nullable T value) {
		if (value == null && nullable()) {
			return;
		}
		if (!validItem(value)) {
			throw new ItemValidationException(attribute(), value, MESSAGES.getString(INVALID_ITEM_VALUE_KEY) + ": " + value);
		}
	}

	private <T extends Number> void validateRange(T value) {
		if (minimum != null && value.doubleValue() < minimum.doubleValue()) {
			throw new RangeValidationException(attribute(), value, "'" + caption() + "' " +
							MESSAGES.getString("value_too_small") + " " + minimum);
		}
		if (maximum != null && value.doubleValue() > maximum.doubleValue()) {
			throw new RangeValidationException(attribute(), value, "'" + caption() + "' " +
							MESSAGES.getString("value_too_large") + " " + maximum);
		}
	}

	private void validateLength(String value) {
		if (maximumLength != -1 && value.length() > maximumLength) {
			throw new LengthValidationException(attribute(), value, "'" + caption() + "' " +
							MESSAGES.getString("value_too_long") + " " + maximumLength + "\n:'" + value + "'");
		}
	}

	static class DefaultValueSupplier<T> implements ValueSupplier<T>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final @Nullable T defaultValue;

		DefaultValueSupplier(@Nullable T defaultValue) {
			this.defaultValue = defaultValue;
		}

		@Override
		public @Nullable T get() {
			return defaultValue;
		}
	}

	private static final class NullDefaultValueSupplier extends DefaultValueSupplier<Object> {

		@Serial
		private static final long serialVersionUID = 1;

		private NullDefaultValueSupplier() {
			super(null);
		}
	}

	private static final class DefaultComparator implements Comparator<Comparable<Object>>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		@Override
		public int compare(Comparable<Object> o1, Comparable<Object> o2) {
			return o1.compareTo(o2);
		}
	}

	private static final class ToStringComparator implements Comparator<Object>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		@Override
		public int compare(Object o1, Object o2) {
			return o1.toString().compareTo(o2.toString());
		}
	}

	private static final class ItemComparator<T> implements Comparator<T>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final Map<T, String> captions;

		private ItemComparator(List<Item<T>> items) {
			this.captions = items.stream()
							.collect(toMap(Item::get, Item::caption));
		}

		@Override
		public int compare(T o1, T o2) {
			return LEXICAL_COMPARATOR.compare(captions.getOrDefault(o1, ""), captions.getOrDefault(o2, ""));
		}
	}

	abstract static sealed class AbstractAttributeDefinitionBuilder<T, B extends AttributeDefinition.Builder<T, B>> implements AttributeDefinition.Builder<T, B>
					permits DefaultColumnDefinitionBuilder, DefaultDerivedAttributeDefinitionBuilder, DefaultDenormalizedAttributeDefinitionBuilder,
					DefaultForeignKeyDefinitionBuilder, DefaultTransientAttributeDefinitionBuilder {

		private static final String NO_RESOURCE_BUNDLE_SPECIFIED_FOR_ENTITY = "No resource bundle specified for entity: ";
		private static final String RESOURCE = "Resource ";
		private static final String NOT_FOUND_IN_BUNDLE = " not found in bundle: ";

		private final Attribute<T> attribute;

		private @Nullable String caption;
		private ValueSupplier<T> defaultValueSupplier;
		private @Nullable String captionResourceBundleName;
		private @Nullable String mnemonicResourceBundleName;
		private @Nullable String descriptionResourceBundleName;
		private String captionResourceKey;
		private String mnemonicResourceKey;
		private String descriptionResourceKey;
		private boolean nullable;
		private boolean hidden;
		private int maximumLength;
		private boolean trim;
		private @Nullable Number maximum;
		private @Nullable Number minimum;
		private @Nullable String description;
		private char mnemonic;
		private @Nullable Format format;
		private @Nullable LocaleDateTimePattern localeDateTimePattern;
		private RoundingMode roundingMode;
		private Comparator<T> comparator;
		private @Nullable String dateTimePattern;
		private @Nullable DateTimeFormatter dateTimeFormatter;
		private @Nullable List<Item<T>> items;

		AbstractAttributeDefinitionBuilder(Attribute<T> attribute) {
			this.attribute = requireNonNull(attribute);
			format = defaultFormat(attribute);
			comparator = defaultComparator(attribute);
			captionResourceBundleName = attribute.entityType().resourceBundleName().orElse(null);
			mnemonicResourceBundleName = captionResourceBundleName;
			descriptionResourceBundleName = captionResourceBundleName;
			captionResourceKey = attribute.name();
			mnemonicResourceKey = captionResourceKey + MNEMONIC_RESOURCE_SUFFIX;
			descriptionResourceKey = captionResourceKey + DESCRIPTION_RESOURCE_SUFFIX;
			hidden = resourceNotFound(captionResourceBundleName, captionResourceKey);
			nullable = true;
			maximumLength = attribute.type().isCharacter() ? 1 : -1;
			trim = TRIM_STRINGS.getOrThrow();
			defaultValueSupplier = (ValueSupplier<T>) DEFAULT_VALUE_SUPPLIER;
			roundingMode = ROUNDING_MODE.getOrThrow();
			minimum = defaultMinimum();
			maximum = defaultMaximum();
		}

		@Override
		public final Attribute<T> attribute() {
			return attribute;
		}

		@Override
		public final B caption(String caption) {
			this.caption = caption;
			this.hidden = caption == null;
			return self();
		}

		@Override
		public final B captionResource(String captionResourceKey) {
			return captionResource(attribute.entityType().resourceBundleName().orElseThrow(() ->
							new IllegalStateException(NO_RESOURCE_BUNDLE_SPECIFIED_FOR_ENTITY + attribute.entityType())), captionResourceKey);
		}

		@Override
		public final B captionResource(String resourceBundleName, String captionResourceKey) {
			if (caption != null) {
				throw new IllegalStateException("Caption has already been set for attribute: " + attribute);
			}
			requireNonNull(captionResourceKey);
			if (resourceNotFound(requireNonNull(resourceBundleName), requireNonNull(captionResourceKey))) {
				throw new IllegalArgumentException(RESOURCE + captionResourceKey + NOT_FOUND_IN_BUNDLE + resourceBundleName);
			}
			this.captionResourceBundleName = resourceBundleName;
			this.captionResourceKey = captionResourceKey;
			this.hidden = false;
			return self();
		}

		@Override
		public final B mnemonicResource(String mnemonicResourceKey) {
			return mnemonicResource(attribute.entityType().resourceBundleName().orElseThrow(() ->
							new IllegalStateException(NO_RESOURCE_BUNDLE_SPECIFIED_FOR_ENTITY + attribute.entityType())), mnemonicResourceKey);
		}

		@Override
		public final B mnemonicResource(String resourceBundleName, String mnemonicResourceKey) {
			if (mnemonic != 0) {
				throw new IllegalStateException("Mnemonic has already been set for attribute: " + attribute);
			}
			requireNonNull(captionResourceKey);
			if (resourceNotFound(requireNonNull(resourceBundleName), requireNonNull(mnemonicResourceKey))) {
				throw new IllegalArgumentException(RESOURCE + mnemonicResourceKey + NOT_FOUND_IN_BUNDLE + resourceBundleName);
			}
			this.mnemonicResourceBundleName = resourceBundleName;
			this.mnemonicResourceKey = mnemonicResourceKey;
			return self();
		}

		@Override
		public final B hidden(boolean hidden) {
			this.hidden = hidden;
			return self();
		}

		@Override
		public final B defaultValue(T defaultValue) {
			return defaultValue(new DefaultValueSupplier<>(defaultValue));
		}

		@Override
		public B defaultValue(ValueSupplier<T> supplier) {
			if (supplier != null) {
				attribute.type().validateType(supplier.get());
			}
			this.defaultValueSupplier = supplier == null ? (ValueSupplier<T>) DEFAULT_VALUE_SUPPLIER : supplier;
			return self();
		}

		@Override
		public B nullable(boolean nullable) {
			this.nullable = nullable;
			return self();
		}

		@Override
		public B maximumLength(int maximumLength) {
			if (!attribute.type().isString()) {
				throw new IllegalStateException("maximumLength is only applicable to string attributes: " + attribute);
			}
			if (maximumLength <= 0) {
				throw new IllegalArgumentException("maximumLength must be a positive integer: " + attribute);
			}
			this.maximumLength = maximumLength;
			return self();
		}

		@Override
		public final B trim(boolean trim) {
			if (!attribute.type().isString()) {
				throw new IllegalStateException("trim is only applicable to string attributes: " + attribute);
			}
			this.trim = trim;
			return (B) this;
		}

		@Override
		public final B minimum(Number minimum) {
			return range(requireNonNull(minimum), null);
		}

		@Override
		public final B maximum(Number maximum) {
			return range(null, requireNonNull(maximum));
		}

		@Override
		public B range(@Nullable Number minimum, @Nullable Number maximum) {
			if (!attribute.type().isNumeric()) {
				throw new IllegalStateException("range is only applicable to numerical attributes");
			}
			if (maximum != null && minimum != null && maximum.doubleValue() < minimum.doubleValue()) {
				throw new IllegalArgumentException("minimum value must be smaller than maximum value: " + attribute);
			}
			this.minimum = minimum;
			this.maximum = maximum;
			return self();
		}

		@Override
		public final B numberGrouping(boolean numberGrouping) {
			if (!attribute.type().isNumeric()) {
				throw new IllegalStateException("numberGrouping is only applicable to numerical attributes: " + attribute);
			}
			requireNonNull(format);
			((NumberFormat) format).setGroupingUsed(numberGrouping);
			return self();
		}

		@Override
		public final B description(String description) {
			this.description = description;
			return self();
		}

		@Override
		public final B descriptionResource(String descriptionResourceKey) {
			return descriptionResource(attribute.entityType().resourceBundleName().orElseThrow(() ->
							new IllegalStateException(NO_RESOURCE_BUNDLE_SPECIFIED_FOR_ENTITY + attribute.entityType())), descriptionResourceKey);
		}

		@Override
		public final B descriptionResource(String resourceBundleName, String descriptionResourceKey) {
			if (description != null) {
				throw new IllegalStateException("Description has already been set for attribute: " + attribute);
			}
			requireNonNull(descriptionResourceKey);
			if (resourceNotFound(requireNonNull(resourceBundleName), requireNonNull(descriptionResourceKey))) {
				throw new IllegalArgumentException(RESOURCE + descriptionResourceKey + NOT_FOUND_IN_BUNDLE + resourceBundleName);
			}
			this.descriptionResourceBundleName = resourceBundleName;
			this.descriptionResourceKey = descriptionResourceKey;
			return self();
		}

		@Override
		public final B mnemonic(char mnemonic) {
			this.mnemonic = mnemonic;
			return self();
		}

		@Override
		public final B format(Format format) {
			requireNonNull(format);
			if (attribute.type().isNumeric() && !(format instanceof NumberFormat)) {
				throw new IllegalArgumentException("NumberFormat required for numerical attribute: " + attribute);
			}
			if (attribute.type().isTemporal()) {
				throw new IllegalStateException("Use dateTimePattern() or localeDateTimePattern() for temporal attributes: " + attribute);
			}
			this.format = format;
			return self();
		}

		@Override
		public final B dateTimePattern(String dateTimePattern) {
			requireNonNull(dateTimePattern);
			if (!attribute.type().isTemporal()) {
				throw new IllegalStateException("dateTimePattern is only applicable to temporal attributes: " + attribute);
			}
			if (this.localeDateTimePattern != null) {
				throw new IllegalStateException("localeDateTimePattern has already been set for attribute: " + attribute);
			}
			this.dateTimePattern = dateTimePattern;
			this.dateTimeFormatter = ofPattern(dateTimePattern);
			return self();
		}

		@Override
		public final B dateTimePattern(LocaleDateTimePattern dateTimePattern) {
			requireNonNull(dateTimePattern);
			if (!attribute.type().isTemporal()) {
				throw new IllegalStateException("dateTimePattern is only applicable to temporal attributes: " + attribute);
			}
			if (this.dateTimePattern != null) {
				throw new IllegalStateException("dateTimePattern has already been set for attribute: " + attribute);
			}
			this.localeDateTimePattern = dateTimePattern;
			this.dateTimePattern = dateTimePattern.dateTimePattern();
			this.dateTimeFormatter = dateTimePattern.formatter();
			return self();
		}

		@Override
		public final B fractionDigits(int fractionDigits) {
			if (!attribute.type().isDecimal()) {
				throw new IllegalStateException("fractionDigits is only applicable to decimal attributes: " + attribute);
			}
			requireNonNull(format);
			((NumberFormat) format).setMaximumFractionDigits(fractionDigits);
			return self();
		}

		@Override
		public final B roundingMode(RoundingMode roundingMode) {
			if (!attribute.type().isDecimal()) {
				throw new IllegalStateException("roundingMode is only applicable to decimal attributes: " + attribute);
			}
			this.roundingMode = requireNonNull(roundingMode);
			return self();
		}

		@Override
		public B comparator(Comparator<T> comparator) {
			this.comparator = requireNonNull(comparator);
			return self();
		}

		@Override
		public final B items(List<Item<T>> items) {
			this.items = validateItems(items);
			this.comparator = new ItemComparator<>(this.items);
			return self();
		}

		protected final B self() {
			return (B) this;
		}

		private static boolean resourceNotFound(@Nullable String resourceBundleName, String captionResourceKey) {
			if (resourceBundleName == null) {
				return true;
			}
			try {
				return !getBundle(resourceBundleName).containsKey(captionResourceKey);
			}
			catch (MissingResourceException e) {
				return true;
			}
		}

		private static <T> List<Item<T>> validateItems(List<Item<T>> items) {
			if (requireNonNull(items).size() != items.stream()
							.distinct()
							.count()) {
				throw new IllegalArgumentException("Item list contains duplicate values: " + items);
			}

			return unmodifiableList(new ArrayList<>(items));
		}

		private static @Nullable Format defaultFormat(Attribute<?> attribute) {
			if (attribute.type().isNumeric()) {
				NumberFormat numberFormat = defaultNumberFormat(attribute);
				if (attribute.type().isDecimal()) {
					((DecimalFormat) numberFormat).setParseBigDecimal(attribute.type().isBigDecimal());
					numberFormat.setMaximumFractionDigits(AttributeDefinition.FRACTION_DIGITS.getOrThrow());
				}

				return numberFormat;
			}

			return null;
		}

		private static NumberFormat defaultNumberFormat(Attribute<?> attribute) {
			boolean grouping = NUMBER_GROUPING.getOrThrow();
			if (attribute.type().isInteger() || attribute.type().isLong()) {
				return setSeparators(grouping ? NumberFormat.getIntegerInstance() : nonGroupingIntegerFormat());
			}

			return setSeparators(grouping ? NumberFormat.getNumberInstance() : nonGroupingNumberFormat());
		}

		private static NumberFormat nonGroupingNumberFormat() {
			NumberFormat format = NumberFormat.getNumberInstance();
			format.setGroupingUsed(false);

			return format;
		}

		private static NumberFormat nonGroupingIntegerFormat() {
			NumberFormat format = NumberFormat.getIntegerInstance();
			format.setGroupingUsed(false);

			return format;
		}

		private static NumberFormat setSeparators(NumberFormat numberFormat) {
			if (numberFormat instanceof DecimalFormat) {
				Character defaultGroupingSeparator = GROUPING_SEPARATOR.get();
				Character defaultDecimalSeparator = DECIMAL_SEPARATOR.get();
				if (defaultGroupingSeparator != null && defaultDecimalSeparator != null) {
					DecimalFormatSymbols symbols = ((DecimalFormat) numberFormat).getDecimalFormatSymbols();
					symbols.setDecimalSeparator(defaultDecimalSeparator);
					symbols.setGroupingSeparator(defaultGroupingSeparator);
					((DecimalFormat) numberFormat).setDecimalFormatSymbols(symbols);
				}
			}

			return numberFormat;
		}

		private static <T> Comparator<T> defaultComparator(Attribute<T> attribute) {
			if (attribute.type().isString() && USE_LEXICAL_STRING_COMPARATOR.getOrThrow()) {
				return (Comparator<T>) LEXICAL_COMPARATOR;
			}
			if (Comparable.class.isAssignableFrom(attribute.type().valueClass())) {
				return (Comparator<T>) COMPARABLE_COMPARATOR;
			}

			return (Comparator<T>) TO_STRING_COMPARATOR;
		}

		private @Nullable Number defaultMinimum() {
			if (attribute.type().isNumeric()) {
				if (attribute.type().isShort()) {
					return Short.MIN_VALUE;
				}
				if (attribute.type().isInteger()) {
					return Integer.MIN_VALUE;
				}
				if (attribute.type().isLong()) {
					return Long.MIN_VALUE;
				}
				if (attribute.type().isDouble()) {
					return -Double.MAX_VALUE;
				}
			}

			return null;
		}

		private @Nullable Number defaultMaximum() {
			if (attribute.type().isNumeric()) {
				if (attribute.type().isShort()) {
					return Short.MAX_VALUE;
				}
				if (attribute.type().isInteger()) {
					return Integer.MAX_VALUE;
				}
				if (attribute.type().isLong()) {
					return Long.MAX_VALUE;
				}
				if (attribute.type().isDouble()) {
					return Double.MAX_VALUE;
				}
			}

			return null;
		}
	}
}