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
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.AbstractValueAttributeDefinition.AbstractValueAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.attribute.DefaultForeignKeyDefinition.DefaultForeignKeyDefinitionBuilder;
import is.codion.framework.domain.entity.exception.NullValidationException;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

abstract sealed class AbstractAttributeDefinition<T> implements AttributeDefinition<T>, Serializable
				permits AbstractValueAttributeDefinition, DefaultForeignKeyDefinition {

	@Serial
	private static final long serialVersionUID = 1;

	private static final MessageBundle MESSAGES =
					messageBundle(AbstractAttributeDefinition.class, getBundle(AbstractAttributeDefinition.class.getName()));

	private static final String VALUE_REQUIRED_KEY = "value_is_required";
	static final String INVALID_ITEM_VALUE_KEY = "invalid_item_value";
	private static final String INVALID_ITEM_SUFFIX_KEY = "invalid_item_suffix";
	static final String INVALID_ITEM_SUFFIX = MESSAGES.getString(INVALID_ITEM_SUFFIX_KEY);

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
	private final @Nullable String description;
	private final char mnemonic;
	private transient @Nullable String resourceCaption;
	private transient @Nullable Character resourceMnemonic;
	private transient @Nullable String resourceDescription;
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
		this.defaultValueSupplier = builder.defaultValueSupplier;
		this.nullable = builder.nullable;
		this.hidden = builder.hidden;
		this.description = builder.description;
		this.mnemonic = builder.mnemonic;
		this.comparator = builder.comparator;
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
	public Comparator<T> comparator() {
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
	public final void validate(Entity entity) {
		validate(entity, nullable());
	}

	@Override
	public void validate(Entity entity, boolean nullable) {
		requireNonNull(entity);
		if (!(attribute instanceof Column) || !entity.definition().foreignKeys().foreignKeyColumn((Column<?>) attribute)) {
			validateNull(entity, nullable);
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
	public String format(T value) {
		if (value == null) {
			return "";
		}

		return value.toString();
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


	Comparator<T> defaultComparator(Attribute<T> attribute) {
		if (attribute.type().isString() && ValueAttributeDefinition.USE_LEXICAL_STRING_COMPARATOR.getOrThrow()) {
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


	abstract static sealed class AbstractAttributeDefinitionBuilder<T, B extends AttributeDefinition.Builder<T, B>>
					implements AttributeDefinition.Builder<T, B>
					permits AbstractValueAttributeDefinitionBuilder, DefaultForeignKeyDefinitionBuilder {

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
		private @Nullable String description;
		private char mnemonic;
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
			nullable = true;
			defaultValueSupplier = (ValueSupplier<T>) DEFAULT_VALUE_SUPPLIER;
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