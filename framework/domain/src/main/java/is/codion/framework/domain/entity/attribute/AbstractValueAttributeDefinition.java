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

import is.codion.common.utilities.Text;
import is.codion.common.utilities.item.Item;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.DefaultColumnDefinition.DefaultColumnDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultTransientAttributeDefinition.DefaultTransientAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.exception.ItemValidationException;
import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toMap;

abstract sealed class AbstractValueAttributeDefinition<T> extends AbstractAttributeDefinition<T> implements ValueAttributeDefinition<T>
				permits DefaultColumnDefinition, DefaultTransientAttributeDefinition {

	@Serial
	private static final long serialVersionUID = 1;

	private static final MessageBundle MESSAGES =
					messageBundle(AbstractValueAttributeDefinition.class, getBundle(AbstractValueAttributeDefinition.class.getName()));

	private static final String INVALID_ITEM_SUFFIX = MESSAGES.getString("invalid_item_suffix");
	private static final ValueSupplier<Object> DEFAULT_VALUE_SUPPLIER = new NullDefaultValueSupplier();

	private final Collection<AttributeValidator<T>> validators;
	private final boolean nullable;
	private final int maximumLength;
	private final boolean trim;
	private final @Nullable Number maximum;
	private final @Nullable Number minimum;
	private final @Nullable List<Item<T>> items;
	private final @Nullable Map<T, Item<T>> itemMap;
	private final ValueSupplier<T> defaultValueSupplier;

	protected AbstractValueAttributeDefinition(AbstractValueAttributeDefinitionBuilder<T, ?> builder) {
		super(builder);
		this.nullable = builder.nullable;
		this.maximumLength = builder.maximumLength;
		this.trim = builder.trim;
		this.maximum = builder.maximum;
		this.minimum = builder.minimum;
		this.items = builder.items;
		this.itemMap = items == null ? null : items.stream()
						.collect(toMap(Item::get, Function.identity()));
		this.defaultValueSupplier = builder.defaultValueSupplier;
		this.validators = initializeValidators(builder);
	}

	@Override
	public final boolean nullable() {
		return nullable;
	}

	@Override
	public final boolean derived() {
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
	public final boolean validItem(@Nullable T value) {
		return itemMap == null || itemMap.containsKey(value);
	}

	@Override
	public final List<Item<T>> items() {
		return items == null ? emptyList() : items;
	}

	@Override
	public final void validate(Entity entity, boolean nullable) {
		requireNonNull(entity);
		if (!(attribute() instanceof Column) || !entity.definition().foreignKeys().foreignKeyColumn((Column<?>) attribute())) {
			validateNull(entity, nullable);
		}
		T value = entity.get(attribute());
		if (!items().isEmpty()) {
			validateItem(value);
		}
		if (value != null) {
			for (AttributeValidator<T> validator : validators) {
				validate(validator, value);
			}
		}
	}

	@Override
	public final String format(T value) {
		if (itemMap != null) {
			Item<T> item = itemMap.get(value);

			return item == null ? invalidItemString(value) : item.caption();
		}

		return super.format(value);
	}

	@Override
	public final boolean hasDefaultValue() {
		return !(defaultValueSupplier instanceof NullDefaultValueSupplier);
	}

	@Override
	public final @Nullable T defaultValue() {
		return defaultValueSupplier.get();
	}

	private String invalidItemString(T value) {
		if (value == null && nullable()) {
			//technically valid
			return "";
		}
		//mark invalid values
		return value + " <" + INVALID_ITEM_SUFFIX + ">";
	}

	private void validateNull(Entity entity, boolean nullable) throws NullValidationException {
		if (!nullable && entity.isNull(attribute())) {
			if ((entity.primaryKey().isNull() || entity.originalPrimaryKey().isNull()) && !(attribute() instanceof ForeignKey)) {
				//a new entity being inserted, allow null for columns with default values and generated primary key values
				boolean nonKeyColumnWithoutDefaultValue = isNonKeyColumnWithoutDefaultValue();
				boolean primaryKeyColumnWithoutAutoGenerate = isNonGeneratedPrimaryKeyColumn();
				if (nonKeyColumnWithoutDefaultValue || primaryKeyColumnWithoutAutoGenerate) {
					throw createNullValidationException(attribute(), caption());
				}
			}
			else {
				throw createNullValidationException(attribute(), caption());
			}
		}
	}

	private <T> Collection<AttributeValidator<T>> initializeValidators(AbstractValueAttributeDefinitionBuilder<T, ?> builder) {
		List<AttributeValidator<T>> list = new ArrayList<>();
		if (attribute().type().isNumeric() && (minimum != null || maximum != null)) {
			list.add((AttributeValidator<T>) new RangeValidator());
		}
		if (attribute().type().isString() && maximumLength != -1) {
			list.add((AttributeValidator<T>) new MaximumLengthValidator());
		}
		list.addAll(builder.validators);

		return unmodifiableCollection(new ArrayList<>(list));
	}

	static NullValidationException createNullValidationException(Attribute<?> attribute, String caption) {
		return new NullValidationException(attribute,
						MessageFormat.format(MESSAGES.getString("value_is_required"), caption));
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
			throw new ItemValidationException(attribute(), value,
							MESSAGES.getString("invalid_item_value") + ": " + value);
		}
	}

	private void validate(AttributeValidator<T> validator, T value) {
		try {
			validator.validate(value);
		}
		catch (ValidationException e) {
			throw e;
		}
		catch (IllegalArgumentException e) {
			throw new ValidationException(attribute(), value, e.getMessage());
		}
	}

	@Override
	protected Comparator<T> defaultComparator(Attribute<T> attribute) {
		if (!items().isEmpty()) {
			return new ItemComparator<>(items());
		}

		return super.defaultComparator(attribute);
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
			return Text.collator().compare(captions.getOrDefault(o1, ""), captions.getOrDefault(o2, ""));
		}
	}

	private final class RangeValidator implements AttributeValidator<T> {

		@Serial
		private static final long serialVersionUID = 1;

		@Override
		public void validate(T value) {
			Number number = (Number) value;
			if (minimum != null && number.doubleValue() < minimum.doubleValue()) {
				throw new RangeValidationException(attribute(), number,
								"'" + caption() + "' " + MESSAGES.getString("value_too_small") + " " + minimum);
			}
			if (maximum != null && number.doubleValue() > maximum.doubleValue()) {
				throw new RangeValidationException(attribute(), number,
								"'" + caption() + "' " + MESSAGES.getString("value_too_large") + " " + maximum);
			}
		}
	}

	private final class MaximumLengthValidator implements AttributeValidator<T> {

		@Serial
		private static final long serialVersionUID = 1;

		@Override
		public void validate(T value) {
			String string = (String) value;
			if (maximumLength != -1 && string.length() > maximumLength) {
				throw new LengthValidationException(attribute(), string,
								"'" + caption() + "' " + MESSAGES.getString("value_too_long") + " " + maximumLength + "\n:'" + string + "'");
			}
		}
	}

	private static final class NullDefaultValueSupplier extends DefaultValueSupplier<Object> {

		@Serial
		private static final long serialVersionUID = 1;

		private NullDefaultValueSupplier() {
			super(null);
		}
	}

	abstract static sealed class AbstractValueAttributeDefinitionBuilder<T, B extends ValueAttributeDefinition.Builder<T, B>>
					extends AbstractAttributeDefinitionBuilder<T, B> implements ValueAttributeDefinition.Builder<T, B>
					permits DefaultColumnDefinitionBuilder, DefaultTransientAttributeDefinitionBuilder {

		private final List<AttributeValidator<T>> validators = new ArrayList<>();

		private boolean nullable;
		private int maximumLength;
		private boolean trim;
		private @Nullable Number maximum;
		private @Nullable Number minimum;
		private @Nullable List<Item<T>> items;
		private ValueSupplier<T> defaultValueSupplier;

		AbstractValueAttributeDefinitionBuilder(Attribute<T> attribute, boolean nullable) {
			super(attribute);
			this.nullable = nullable;
			maximumLength = attribute.type().isCharacter() ? 1 : -1;
			trim = TRIM_STRINGS.getOrThrow();
			minimum = defaultMinimum();
			maximum = defaultMaximum();
			defaultValueSupplier = (ValueSupplier<T>) DEFAULT_VALUE_SUPPLIER;
		}

		@Override
		public final B nullable(boolean nullable) {
			this.nullable = nullable;
			return self();
		}

		@Override
		public final B maximumLength(int maximumLength) {
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
		public final B range(@Nullable Number minimum, @Nullable Number maximum) {
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
		public final B validator(AttributeValidator<T> validator) {
			validators.add(requireNonNull(validator));
			return self();
		}

		@Override
		public final B items(List<Item<T>> items) {
			this.items = validateItems(items);
			return self();
		}

		@Override
		public final B defaultValue(T defaultValue) {
			return defaultValue(new DefaultValueSupplier<>(defaultValue));
		}

		@Override
		public final B defaultValue(ValueSupplier<T> supplier) {
			if (supplier != null) {
				attribute().type().validateType(supplier.get());
			}
			this.defaultValueSupplier = supplier == null ? (ValueSupplier<T>) DEFAULT_VALUE_SUPPLIER : supplier;
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
