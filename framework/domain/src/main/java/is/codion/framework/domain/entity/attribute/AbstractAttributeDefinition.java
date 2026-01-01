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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.utilities.Text;
import is.codion.common.utilities.format.LocaleDateTimePattern;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.AbstractValueAttributeDefinition.AbstractValueAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultDerivedAttributeDefinition.DefaultDenormalizedAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultDerivedAttributeDefinition.DefaultDerivedAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultForeignKeyDefinition.DefaultForeignKeyDefinitionBuilder;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Comparator;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

abstract sealed class AbstractAttributeDefinition<T> implements AttributeDefinition<T>, Serializable
				permits AbstractValueAttributeDefinition, DefaultDerivedAttributeDefinition, DefaultForeignKeyDefinition {

	@Serial
	private static final long serialVersionUID = 1;

	private static final Comparator<String> LEXICAL_COMPARATOR = Text.collator();
	private static final Comparator<Comparable<Object>> COMPARABLE_COMPARATOR = new DefaultComparator();
	private static final Comparator<Object> TO_STRING_COMPARATOR = new ToStringComparator();

	private final Attribute<T> attribute;
	private final @Nullable String caption;
	private final @Nullable String captionResourceBundleName;
	private final @Nullable String mnemonicResourceBundleName;
	private final @Nullable String descriptionResourceBundleName;
	private final String captionResourceKey;
	private final String mnemonicResourceKey;
	private final String descriptionResourceKey;
	private final boolean hidden;
	private final @Nullable String description;
	private final char mnemonic;
	private final @Nullable Format format;
	private final @Nullable LocaleDateTimePattern localeDateTimePattern;
	private final RoundingMode roundingMode;
	private transient @Nullable String resourceCaption;
	private transient @Nullable Character resourceMnemonic;
	private transient @Nullable String resourceDescription;
	private transient @Nullable String dateTimePattern;
	private transient @Nullable DateTimeFormatter dateTimeFormatter;
	private @Nullable Comparator<T> comparator;

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
		this.hidden = builder.hidden;
		this.description = builder.description;
		this.mnemonic = builder.mnemonic;
		this.comparator = builder.comparator;
		this.format = builder.format;
		this.localeDateTimePattern = builder.localeDateTimePattern;
		this.roundingMode = builder.roundingMode;
		this.dateTimePattern = builder.dateTimePattern;
		this.dateTimeFormatter = builder.dateTimeFormatter;
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
	public final EntityType entityType() {
		return attribute.entityType();
	}

	@Override
	public final boolean hidden() {
		return hidden;
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
	public String format(T value) {
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
	public final Comparator<T> comparator() {
		if (comparator == null) {
			comparator = defaultComparator(attribute);
		}

		return comparator;
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

	protected Comparator<T> defaultComparator(Attribute<T> attribute) {
		if (attribute.type().isString() && USE_LEXICAL_STRING_COMPARATOR.getOrThrow()) {
			return (Comparator<T>) LEXICAL_COMPARATOR;
		}
		if (Comparable.class.isAssignableFrom(attribute.type().valueClass())) {
			return (Comparator<T>) COMPARABLE_COMPARATOR;
		}

		return (Comparator<T>) TO_STRING_COMPARATOR;
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

	abstract static sealed class AbstractAttributeDefinitionBuilder<T, B extends AttributeDefinition.Builder<T, B>>
					implements AttributeDefinition.Builder<T, B>
					permits AbstractValueAttributeDefinitionBuilder, DefaultDenormalizedAttributeDefinitionBuilder,
					DefaultDerivedAttributeDefinitionBuilder, DefaultForeignKeyDefinitionBuilder {

		private static final String NO_RESOURCE_BUNDLE_SPECIFIED_FOR_ENTITY = "No resource bundle specified for entity: ";
		private static final String RESOURCE = "Resource ";
		private static final String NOT_FOUND_IN_BUNDLE = " not found in bundle: ";

		private final Attribute<T> attribute;

		private @Nullable String caption;
		private @Nullable String captionResourceBundleName;
		private @Nullable String mnemonicResourceBundleName;
		private @Nullable String descriptionResourceBundleName;
		private String captionResourceKey;
		private String mnemonicResourceKey;
		private String descriptionResourceKey;
		private boolean hidden;
		private @Nullable String description;
		private char mnemonic;
		private @Nullable Format format;
		private @Nullable LocaleDateTimePattern localeDateTimePattern;
		private RoundingMode roundingMode;
		private @Nullable String dateTimePattern;
		private @Nullable DateTimeFormatter dateTimeFormatter;
		private @Nullable Comparator<T> comparator;

		AbstractAttributeDefinitionBuilder(Attribute<T> attribute) {
			this.attribute = requireNonNull(attribute);
			captionResourceBundleName = attribute.entityType().resourceBundleName().orElse(null);
			mnemonicResourceBundleName = captionResourceBundleName;
			descriptionResourceBundleName = captionResourceBundleName;
			captionResourceKey = attribute.name();
			mnemonicResourceKey = captionResourceKey + MNEMONIC_RESOURCE_SUFFIX;
			descriptionResourceKey = captionResourceKey + DESCRIPTION_RESOURCE_SUFFIX;
			hidden = resourceNotFound(captionResourceBundleName, captionResourceKey);
			format = defaultFormat(attribute);
			roundingMode = ROUNDING_MODE.getOrThrow();
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
		public final B hidden(boolean hidden) {
			this.hidden = hidden;
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
		public B comparator(Comparator<T> comparator) {
			this.comparator = requireNonNull(comparator);
			return self();
		}

		protected final B self() {
			return (B) this;
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
	}
}