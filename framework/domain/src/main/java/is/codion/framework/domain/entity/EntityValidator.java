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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;

import java.util.List;
import java.util.stream.Collectors;

import static is.codion.common.utilities.Configuration.booleanValue;
import static java.util.Objects.requireNonNull;

/**
 * Responsible for providing validation for entities.
 * <p>
 * Entity validators enforce business rules and data integrity constraints beyond basic database constraints.
 * They validate entity values before insert/update operations and can provide context-aware validation
 * that considers the entity's current state and relationships.
 * <p>
 * Custom validators can be implemented for complex business logic:
 * {@snippet :
 * // Custom validator for Customer entity
 * public class CustomerValidator implements EntityValidator {
 *
 *     @Override
 *     public <T> void validate(Entity customer, Attribute<T> attribute) {
 *         // Start with super.validate(), which performs null validation
 *         EntityValidator.super.validate(customer, attribute);
 *         // Validate email format
 *         if (attribute.equals(Customer.EMAIL)) {
 *             String email = customer.get(Customer.EMAIL);
 *             // Email is non-null, since super.validate() checks that
 *             if (!isValidEmail(email)) {
 *                 throw new ValidationException(Customer.EMAIL, email, "Invalid email format");
 *             }
 *         }
 *     }
 *
 *     private static boolean isValidEmail(String email) {
 *         return email.contains("@") && email.contains(".");
 *     }
 * }
 *
 * // Usage in domain definition
 * Customer.TYPE.as(
 *         Customer.EMAIL.as()
 *             .column(),
 *         Customer.ACTIVE.as()
 *             .column(),
 *         Customer.BIRTH_DATE.as()
 *             .column())
 *     .validator(new CustomerValidator())
 *     .build();
 *}
 * @see EntityDefinition.Builder#validator(EntityValidator)
 * @see ValidationException
 */
public interface EntityValidator {

	/**
	 * Specifies whether the default validator performs strict validation or not.
	 * By default, all non-read-only attribute values are validated if the entity
	 * is being inserted (as in, when it does not exist according to {@link Entity#exists()}).
	 * If the entity exists and is being updated, only modified values are validated by default.
	 * With strict validation enabled all values are always validated, regardless of whether the entity exists or not
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> STRICT_VALIDATION = booleanValue("codion.domain.strictValidation", false);

	/**
	 * Returns true if the value based on the given attribute accepts a null value for the given entity,
	 * by default this method simply returns the nullable state of the underlying attribute.
	 * {@snippet :
	 * // Context-aware nullable validation
	 * public class OrderValidator implements EntityValidator {
	 *
	 *     @Override
	 *     public boolean nullable(Entity order, Attribute<?> attribute) {
	 *         // Normally nullable, but not for shipped orders
	 *         if (attribute.equals(Order.TRACKING_NUMBER)) {
	 *             String status = order.get(Order.STATUS);
	 *             return !"SHIPPED".equals(status); // Tracking number required when shipped
	 *         }
	 *
	 *         // Use default nullable behavior for other attributes
	 *         return EntityValidator.super.nullable(order, attribute);
	 *     }
	 * }
	 *
	 * // Usage during validation
	 * Entity order = entities.entity(Order.TYPE)
	 *     .with(Order.STATUS, "SHIPPED")
	 *     .build(); // No tracking number
	 *
	 * boolean nullable = validator.nullable(order, Order.TRACKING_NUMBER); // false
	 *}
	 * @param entity the entity being validated
	 * @param attribute the attribute
	 * @return true if the attribute is non-value based or accepts a null value
	 * @see ValueAttributeDefinition#nullable()
	 */
	default boolean nullable(Entity entity, Attribute<?> attribute) {
		AttributeDefinition<?> definition = requireNonNull(entity).definition().attributes().definition(attribute);
		if (definition instanceof ValueAttributeDefinition<?>) {
			return ((ValueAttributeDefinition<?>) definition).nullable();
		}
		else if (attribute instanceof ForeignKey) {
			return ((ForeignKey) attribute).references().stream()
							.map(ForeignKey.Reference::column)
							.map(entity.definition().columns()::definition)
							.anyMatch(ValueAttributeDefinition::nullable);
		}

		return true;
	}

	/**
	 * Returns true if the given entity contains only valid values according to this validator.
	 * @param entity the entity
	 * @return true if the given entity contains only valid values
	 */
	default boolean valid(Entity entity) {
		try {
			validate(entity);
			return true;
		}
		catch (ValidationException e) {
			return false;
		}
	}

	/**
	 * Checks if the values in the given entity are valid.
	 * Note that by default, if the entity instance does not exist according to
	 * {@link Entity#exists()} all values are validated, otherwise only modified values are validated.
	 * Use the {@link #STRICT_VALIDATION} configuration value to change the default behaviour
	 * or override {@link #strict()} in order to configure the strictness of a specific instance.
	 * {@snippet :
	 * // Validation during entity lifecycle
	 * Entity customer = entities.entity(Customer.TYPE)
	 *     .with(Customer.NAME, "John Doe")
	 *     .with(Customer.EMAIL, "invalid-email") // Invalid format
	 *     .with(Customer.ACTIVE, true)
	 *     .build();
	 *
	 * EntityValidator validator = entities.definition(Customer.TYPE).validator();
	 *
	 * try {
	 *     validator.validate(customer);
	 *     // Validation passed
	 *     connection.insert(customer);
	 * } catch (ValidationException e) {
	 *     // Handle validation error
	 *     System.err.println("Validation failed for attribute " + e.attribute() + ": " + e.getMessage());
	 * }
	 *
	 * // Check if entity is valid without throwing exception
	 * if (validator.valid(customer)) {
	 *     connection.insert(customer);
	 * } else {
	 *     // Handle invalid entity
	 * }
	 *}
	 * @param entity the entity
	 * @throws ValidationException in case of an invalid value
	 * @see #strict()
	 */
	default void validate(Entity entity) throws ValidationException {
		List<Attribute<?>> attributes = requireNonNull(entity).definition().attributes().definitions().stream()
						.filter(definition -> validated(entity, definition))
						.map(AttributeDefinition::attribute)
						.collect(Collectors.toList());
		for (Attribute<?> attribute : attributes) {
			validate(entity, attribute);
		}
	}

	/**
	 * Checks if the value associated with the give attribute is valid, throws a ValidationException if not
	 * @param entity the entity to validate
	 * @param attribute the attribute the value is associated with
	 * @throws ValidationException if the given value is not valid for the given attribute
	 */
	default void validate(Entity entity, Attribute<?> attribute) throws ValidationException {
		AttributeDefinition<?> definition = requireNonNull(entity).definition().attributes().definition(attribute);
		if (definition instanceof ValueAttributeDefinition<?>) {
			((ValueAttributeDefinition<?>) definition).validate(entity, nullable(entity, attribute));
		}
		else if (definition instanceof ForeignKeyDefinition) {
			((ForeignKeyDefinition) definition).validate(entity, nullable(entity, attribute));
		}
	}

	/**
	 * Specifies whether the given attribute should be validated
	 * @param entity the entity
	 * @param definition the attribute definition
	 * @return true if the value of the given attribute should be validated
	 * @see #strict()
	 */
	default boolean validated(Entity entity, AttributeDefinition<?> definition) {
		if (definition.derived()) {
			return false;
		}
		if (definition instanceof ColumnDefinition && ((ColumnDefinition<?>) definition).readOnly()) {
			return false;
		}
		if (!entity.exists()) {
			// validate all values when inserting
			return true;
		}

		// only validate modified values when updating
		return entity.modified(definition.attribute()) || strict();
	}

	/**
	 * <p>Specifies whether this validator performs strict validation or not.
	 * By default, all non-read-only attribute values are validated if the entity
	 * is being inserted (as in, when it does not exist according to {@link Entity#exists()}).
	 * If the entity exists and is being updated, only modified values are validated by default.
	 * <p>With strict validation enabled all values are always validated, regardless of whether the entity exists or not.
	 * <p>The default implementation simply returns the value of {@link #STRICT_VALIDATION}, override to specify
	 * validator specific strictness.
	 * @return true if strict validation is enabled
	 * @see #STRICT_VALIDATION
	 */
	default boolean strict() {
		return STRICT_VALIDATION.getOrThrow();
	}
}
