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

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.AbstractAttributeDefinition.AbstractAttributeDefinitionBuilder;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Optional;

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
 *                     .generator(Generator.identity())
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
 *                     .minimum(BigDecimal.ZERO)
 *                     .maximum(new BigDecimal("99999.99"))
 *                     .fractionDigits(2)
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
 *                     .withDefault(true) // Database sets this
 *                     .updatable(false))
 *             .build();
 *     }
 * }
 *}
 * @param <T> the underlying type
 * @see Attribute#define()
 * @see Builder
 */
public sealed interface AttributeDefinition<T> permits AbstractAttributeDefinition, ValueAttributeDefinition, ForeignKeyDefinition {

	/**
	 * The suffix used for the mnemonic resource key.
	 * <ul>
	 * <li>name=Name
	 * <li>name.mnemonic=N
	 * </ul>
	 */
	String MNEMONIC_RESOURCE_SUFFIX = ".mnemonic";

	/**
	 * The suffix used for the description resource key.
	 * <ul>
	 * <li>name=Name
	 * <li>name.description=The customer name
	 * </ul>
	 */
	String DESCRIPTION_RESOURCE_SUFFIX = ".description";

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
	 */
	String format(@Nullable T value);

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
	 * @return true if null is a valid value for this attribute
	 */
	boolean nullable();

	/**
	 * Returns the mnemonic associated with this attribute.
	 * @return the mnemonic to use when creating a label for this attribute, 0 meaning no mnemonic
	 */
	char mnemonic();

	/**
	 * @return the Comparator to use when comparing values associated with this attribute
	 */
	Comparator<T> comparator();

	/**
	 * Validates the value of this attribute as found in the given entity.
	 * <p>Note: When validating non-nullable attributes during entity insertion
	 * (when the entity does not exist), null values are allowed for:
	 * <ul>
	 * <li>Columns with default values - the database will provide the default value
	 * <li>Generated primary key columns - the database will generate the key value
	 * </ul>
	 * @param entity the {@link Entity} the containing the value to validate
	 * @throws ValidationException in case of an invalid value
	 */
	void validate(Entity entity);

	/**
	 * Validates the value of this attribute as found in the given entity.
	 * <p>Note: When validating non-nullable attributes during entity insertion
	 * (when the entity does not exist), null values are allowed for:
	 * <ul>
	 * <li>Columns with default values - the database will provide the default value
	 * <li>Generated primary key columns - the database will generate the key value
	 * </ul>
	 * @param entity the entity containing the value to validate
	 * @param nullable true if null values are allowed in this validation context,
	 * false if null should trigger a {@link NullValidationException}
	 * @throws ValidationException in case of an invalid value
	 */
	void validate(Entity entity, boolean nullable);

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
	sealed interface Builder<T, B extends Builder<T, B>>
					permits AbstractAttributeDefinitionBuilder, ValueAttributeDefinition.Builder, ForeignKeyDefinition.Builder {

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
		B captionResource(String captionResourceKey);

		/**
		 * Specifies the key to use when retrieving the caption for this attribute from the given resource bundle.
		 * Note that this configures the attribute to not be hidden.
		 * @param resourceBundleName the resource bundle name
		 * @param captionResourceKey the caption resource bundle key
		 * @return this builder instance
		 * @throws IllegalStateException in case the caption has already been set
		 * @throws IllegalStateException in case the caption resource is not found in the given resource bundle
		 */
		B captionResource(String resourceBundleName, String captionResourceKey);

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
		B mnemonicResource(String mnemonicResourceKey);

		/**
		 * Specifies the key to use when retrieving the mnemonic for this attribute from the given resource bundle.
		 * @param resourceBundleName the resource bundle name
		 * @param mnemonicResourceKey the mnemonic resource bundle key
		 * @return this builder instance
		 * @throws IllegalStateException in case the mnemonic has already been set
		 * @throws IllegalStateException in case the mnemonic resource is not found in the given resource bundle
		 */
		B mnemonicResource(String resourceBundleName, String mnemonicResourceKey);

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
		 * Specifies whether this attribute is nullable. Note that this will not prevent
		 * the value from being set to null, only prevent successful validation of the entity.
		 * @param nullable specifies whether null is a valid value for this attribute
		 * @return this builder instance
		 */
		B nullable(boolean nullable);

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
		 * Specifies the key to use when retrieving the description for this attribute from the entity resource bundle,
		 * in case it differs from {@link Attribute#name()} + {@link #DESCRIPTION_RESOURCE_SUFFIX}, which is the default value.
		 * @param descriptionResourceKey the description resource bundle key
		 * @return this builder instance
		 * @throws IllegalStateException in case the description has already been set
		 * @throws IllegalStateException in case no resource bundle is specified for the entity
		 * @throws IllegalStateException in case the description resource is not found in the entity resource bundle
		 * @see EntityType#resourceBundleName()
		 */
		B descriptionResource(String descriptionResourceKey);

		/**
		 * Specifies the key to use when retrieving the description for this attribute from the entity resource bundle,
		 * in case it differs from {@link Attribute#name()} + {@link #DESCRIPTION_RESOURCE_SUFFIX}, which is the default value.
		 * @param resourceBundleName the resource bundle name
		 * @param descriptionResourceKey the description resource bundle key
		 * @return this builder instance
		 * @throws IllegalStateException in case the description has already been set
		 * @throws IllegalStateException in case the description resource is not found in the given resource bundle
		 */
		B descriptionResource(String resourceBundleName, String descriptionResourceKey);

		/**
		 * @param comparator the Comparator to use when comparing values for this attribute
		 * @return this builder instance
		 */
		B comparator(Comparator<T> comparator);

		/**
		 * Builds a new attribute definition instance
		 * @return a new attribute definition instance based on this builder
		 */
		AttributeDefinition<T> build();
	}
}
