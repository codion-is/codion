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

import is.codion.common.utilities.format.LocaleDateTimePattern;
import is.codion.common.utilities.item.Item;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.domain.entity.attribute.DefaultColumnDefinition.DefaultColumnDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultDerivedAttributeDefinition.DefaultDenormalizedAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultDerivedAttributeDefinition.DefaultDerivedAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultTransientAttributeDefinition.DefaultTransientAttributeDefinitionBuilder;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toMap;

abstract sealed class AbstractValueAttributeDefinition<T> extends AbstractAttributeDefinition<T> implements ValueAttributeDefinition<T>
				permits DefaultColumnDefinition, DefaultDerivedAttributeDefinition, DefaultTransientAttributeDefinition {

	@Serial
	private static final long serialVersionUID = 1;

	private static final MessageBundle MESSAGES =
					messageBundle(AbstractValueAttributeDefinition.class, getBundle(AbstractAttributeDefinition.class.getName()));

	private final int maximumLength;
	private final boolean trim;
	private final @Nullable Number maximum;
	private final @Nullable Number minimum;
	private final @Nullable Format format;
	private final @Nullable LocaleDateTimePattern localeDateTimePattern;
	private final RoundingMode roundingMode;
	private final @Nullable List<Item<T>> items;
	private final @Nullable Map<T, Item<T>> itemMap;
	private transient @Nullable String dateTimePattern;
	private transient @Nullable DateTimeFormatter dateTimeFormatter;
	private transient @Nullable Comparator<T> comparator;

	protected AbstractValueAttributeDefinition(AbstractValueAttributeDefinitionBuilder<T, ?> builder) {
		super(builder);
		this.maximumLength = builder.maximumLength;
		this.trim = builder.trim;
		this.maximum = builder.maximum;
		this.minimum = builder.minimum;
		this.format = builder.format;
		this.localeDateTimePattern = builder.localeDateTimePattern;
		this.roundingMode = builder.roundingMode;
		this.dateTimePattern = builder.dateTimePattern;
		this.dateTimeFormatter = builder.dateTimeFormatter;
		this.items = builder.items;
		this.itemMap = items == null ? null : items.stream()
						.collect(toMap(Item::get, Function.identity()));
	}

	@Override
	public boolean derived() {
		return false;
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
	public final String format(T value) {
		if (itemMap != null) {
			Item<T> item = itemMap.get(value);

			return item == null ? invalidItemString(value) : item.caption();
		}

		if (value == null) {
			return "";
		}
		if (attribute().type().isTemporal()) {
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
		return value + " <" + AbstractAttributeDefinition.INVALID_ITEM_SUFFIX + ">";
	}

	private @Nullable String defaultDateTimePattern() {
		if (attribute().type().isLocalDate()) {
			return LocaleDateTimePattern.DATE_PATTERN.getOrThrow();
		}
		else if (attribute().type().isLocalTime()) {
			return LocaleDateTimePattern.TIME_PATTERN.getOrThrow();
		}
		else if (attribute().type().isLocalDateTime()) {
			return LocaleDateTimePattern.DATE_TIME_PATTERN.getOrThrow();
		}
		else if (attribute().type().isOffsetDateTime()) {
			return LocaleDateTimePattern.DATE_TIME_PATTERN.getOrThrow();
		}

		return null;
	}

	@Override
	public void validate(is.codion.framework.domain.entity.Entity entity, boolean nullable) {
		super.validate(entity, nullable);
		T value = entity.get(attribute());
		if (!items().isEmpty()) {
			validateItem(value);
		}
		if (value != null) {
			if (attribute().type().isNumeric()) {
				validateRange((Number) value);
			}
			else if (attribute().type().isString()) {
				validateLength((String) value);
			}
		}
	}

	private void validateItem(@Nullable T value) {
		if (value == null && nullable()) {
			return;
		}
		if (!validItem(value)) {
			throw new is.codion.framework.domain.entity.exception.ItemValidationException(attribute(), value,
							MESSAGES.getString(AbstractAttributeDefinition.INVALID_ITEM_VALUE_KEY) + ": " + value);
		}
	}

	private <N extends Number> void validateRange(N value) {
		Optional<Number> min = minimum();
		Optional<Number> max = maximum();
		if (min.isPresent() && value.doubleValue() < min.get().doubleValue()) {
			throw new is.codion.framework.domain.entity.exception.RangeValidationException(attribute(), value,
							"'" + caption() + "' " + MESSAGES.getString("value_too_small") + " " + min.get());
		}
		if (max.isPresent() && value.doubleValue() > max.get().doubleValue()) {
			throw new is.codion.framework.domain.entity.exception.RangeValidationException(attribute(), value,
							"'" + caption() + "' " + MESSAGES.getString("value_too_large") + " " + max.get());
		}
	}

	private void validateLength(String value) {
		if (maximumLength() != -1 && value.length() > maximumLength()) {
			throw new is.codion.framework.domain.entity.exception.LengthValidationException(attribute(), value,
							"'" + caption() + "' " + MESSAGES.getString("value_too_long") + " " + maximumLength() + "\n:'" + value + "'");
		}
	}

	@Override
	public final Comparator<T> comparator() {
		if (comparator == null) {
			comparator = defaultValueComparator();
		}

		return comparator;
	}

	private Comparator<T> defaultValueComparator() {
		if (!items().isEmpty()) {
			return new ItemComparator<>(items());
		}

		return defaultComparator(attribute());
	}

	private static final class ItemComparator<T> implements Comparator<T>, java.io.Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final Map<T, String> captions;

		private ItemComparator(List<Item<T>> items) {
			this.captions = items.stream()
							.collect(java.util.stream.Collectors.toMap(Item::get, Item::caption));
		}

		@Override
		public int compare(T o1, T o2) {
			return is.codion.common.utilities.Text.collator().compare(
							captions.getOrDefault(o1, ""), captions.getOrDefault(o2, ""));
		}
	}

	abstract static sealed class AbstractValueAttributeDefinitionBuilder<T, B extends ValueAttributeDefinition.Builder<T, B>>
					extends AbstractAttributeDefinitionBuilder<T, B>
					implements ValueAttributeDefinition.Builder<T, B>
					permits DefaultColumnDefinitionBuilder, DefaultDerivedAttributeDefinitionBuilder,
					DefaultDenormalizedAttributeDefinitionBuilder, DefaultTransientAttributeDefinitionBuilder {

		int maximumLength;
		boolean trim;
		@Nullable Number maximum;
		@Nullable Number minimum;
		@Nullable Format format;
		@Nullable LocaleDateTimePattern localeDateTimePattern;
		RoundingMode roundingMode;
		@Nullable String dateTimePattern;
		@Nullable DateTimeFormatter dateTimeFormatter;
		@Nullable List<Item<T>> items;

		AbstractValueAttributeDefinitionBuilder(Attribute<T> attribute) {
			super(attribute);
			format = defaultFormat(attribute);
			maximumLength = attribute.type().isCharacter() ? 1 : -1;
			trim = TRIM_STRINGS.getOrThrow();
			roundingMode = ROUNDING_MODE.getOrThrow();
			minimum = defaultMinimum();
			maximum = defaultMaximum();
		}

		@Override
		public B maximumLength(int maximumLength) {
			if (!attribute().type().isString()) {
				throw new IllegalStateException("maximumLength is only applicable to string attributes: " + attribute());
			}
			if (maximumLength <= 0) {
				throw new IllegalArgumentException("maximumLength must be a positive integer: " + attribute());
			}
			this.maximumLength = maximumLength;
			return self();
		}

		@Override
		public final B trim(boolean trim) {
			if (!attribute().type().isString()) {
				throw new IllegalStateException("trim is only applicable to string attributes: " + attribute());
			}
			this.trim = trim;
			return self();
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
			if (!attribute().type().isNumeric()) {
				throw new IllegalStateException("range is only applicable to numerical attributes");
			}
			if (maximum != null && minimum != null && maximum.doubleValue() < minimum.doubleValue()) {
				throw new IllegalArgumentException("minimum value must be smaller than maximum value: " + attribute());
			}
			this.minimum = minimum;
			this.maximum = maximum;
			return self();
		}

		@Override
		public final B numberGrouping(boolean numberGrouping) {
			if (!attribute().type().isNumeric()) {
				throw new IllegalStateException("numberGrouping is only applicable to numerical attributes: " + attribute());
			}
			requireNonNull(format);
			((NumberFormat) format).setGroupingUsed(numberGrouping);
			return self();
		}

		@Override
		public final B format(Format format) {
			requireNonNull(format);
			if (attribute().type().isNumeric() && !(format instanceof NumberFormat)) {
				throw new IllegalArgumentException("NumberFormat required for numerical attribute: " + attribute());
			}
			if (attribute().type().isTemporal()) {
				throw new IllegalStateException("Use dateTimePattern() or localeDateTimePattern() for temporal attributes: " + attribute());
			}
			this.format = format;
			return self();
		}

		@Override
		public final B dateTimePattern(String dateTimePattern) {
			requireNonNull(dateTimePattern);
			if (!attribute().type().isTemporal()) {
				throw new IllegalStateException("dateTimePattern is only applicable to temporal attributes: " + attribute());
			}
			if (this.localeDateTimePattern != null) {
				throw new IllegalStateException("localeDateTimePattern has already been set for attribute: " + attribute());
			}
			this.dateTimePattern = dateTimePattern;
			this.dateTimeFormatter = ofPattern(dateTimePattern);
			return self();
		}

		@Override
		public final B dateTimePattern(LocaleDateTimePattern dateTimePattern) {
			requireNonNull(dateTimePattern);
			if (!attribute().type().isTemporal()) {
				throw new IllegalStateException("dateTimePattern is only applicable to temporal attributes: " + attribute());
			}
			if (this.dateTimePattern != null) {
				throw new IllegalStateException("dateTimePattern has already been set for attribute: " + attribute());
			}
			this.localeDateTimePattern = dateTimePattern;
			this.dateTimePattern = dateTimePattern.dateTimePattern();
			this.dateTimeFormatter = dateTimePattern.formatter();
			return self();
		}

		@Override
		public final B fractionDigits(int fractionDigits) {
			if (!attribute().type().isDecimal()) {
				throw new IllegalStateException("fractionDigits is only applicable to decimal attributes: " + attribute());
			}
			requireNonNull(format);
			((NumberFormat) format).setMaximumFractionDigits(fractionDigits);
			return self();
		}

		@Override
		public final B roundingMode(RoundingMode roundingMode) {
			if (!attribute().type().isDecimal()) {
				throw new IllegalStateException("roundingMode is only applicable to decimal attributes: " + attribute());
			}
			this.roundingMode = requireNonNull(roundingMode);
			return self();
		}

		@Override
		public final B items(List<Item<T>> items) {
			this.items = validateItems(items);
			return self();
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
					numberFormat.setMaximumFractionDigits(FRACTION_DIGITS.getOrThrow());
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

		private @Nullable Number defaultMinimum() {
			if (attribute().type().isNumeric()) {
				if (attribute().type().isShort()) {
					return Short.MIN_VALUE;
				}
				if (attribute().type().isInteger()) {
					return Integer.MIN_VALUE;
				}
				if (attribute().type().isLong()) {
					return Long.MIN_VALUE;
				}
				if (attribute().type().isDouble()) {
					return -Double.MAX_VALUE;
				}
			}

			return null;
		}

		private @Nullable Number defaultMaximum() {
			if (attribute().type().isNumeric()) {
				if (attribute().type().isShort()) {
					return Short.MAX_VALUE;
				}
				if (attribute().type().isInteger()) {
					return Integer.MAX_VALUE;
				}
				if (attribute().type().isLong()) {
					return Long.MAX_VALUE;
				}
				if (attribute().type().isDouble()) {
					return Double.MAX_VALUE;
				}
			}

			return null;
		}
	}
}
