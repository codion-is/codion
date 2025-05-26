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

import is.codion.common.property.PropertyValue;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.exception.ValidationException;

import static is.codion.common.Configuration.booleanValue;

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
 *     public boolean valid(Entity customer) {
 *         try {
 *             validate(customer);
 *             return true;
 *         } catch (ValidationException e) {
 *             return false;
 *         }
 *     }
 *     
 *     @Override
 *     public void validate(Entity customer) throws ValidationException {
 *         // Validate email format
 *         String email = customer.get(Customer.EMAIL);
 *         if (email != null && !isValidEmail(email)) {
 *             throw new ValidationException("Invalid email format: " + email);
 *         }
 *         
 *         // Business rule: Active customers must have email
 *         Boolean active = customer.get(Customer.ACTIVE);
 *         if (Boolean.TRUE.equals(active) && 
 *             (email == null || email.trim().isEmpty())) {
 *             throw new ValidationException("Active customers must have an email address");
 *         }
 *         
 *         // Age validation
 *         LocalDate birthDate = customer.get(Customer.BIRTH_DATE);
 *         if (birthDate != null && birthDate.isAfter(LocalDate.now().minusYears(13))) {
 *             throw new ValidationException("Customer must be at least 13 years old");
 *         }
 *     }
 *     
 *     @Override
 *     public <T> void validate(Entity customer, Attribute<T> attribute) 
 *             throws ValidationException {
 *         T value = customer.get(attribute);
 *         
 *         if (attribute.equals(Customer.EMAIL)) {
 *             String email = (String) value;
 *             if (email != null && !isValidEmail(email)) {
 *                 throw new ValidationException("Invalid email format");
 *             }
 *         }
 *         // Additional attribute-specific validation...
 *     }
 *     
 *     private boolean isValidEmail(String email) {
 *         return email.contains("@") && email.contains(".");
 *     }
 * }
 * 
 * // Usage in domain definition
 * Customer.TYPE.define(
 *         Customer.EMAIL.define()
 *             .column(),
 *         Customer.ACTIVE.define()
 *             .column(),
 *         Customer.BIRTH_DATE.define()
 *             .column())
 *     .validator(new CustomerValidator())
 *     .build();
 * }
 * @see EntityDefinition.Builder#validator(EntityValidator)
 * @see ValidationException
 */
public interface EntityValidator {

	/**
	 * Specifies whether the default validator performs strict validation or not.
	 * By default, all non-read-only attribute values are validated if the entity
	 * is being inserted (as in, when it does not exist according to {@link Entity#exists()}).
	 * If the entity exists, only modified values are validated.
	 * With strict validation enabled all values are validated, regardless
	 * of whether the entity exists or not
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
	 *     public <T> boolean nullable(Entity order, Attribute<T> attribute) {
	 *         // Normally nullable, but not for shipped orders
	 *         if (attribute.equals(Order.TRACKING_NUMBER)) {
	 *             String status = order.get(Order.STATUS);
	 *             return !"SHIPPED".equals(status); // Tracking number required when shipped
	 *         }
	 *         
	 *         // Use default nullable behavior for other attributes
	 *         return attribute.nullable();
	 *     }
	 * }
	 * 
	 * // Usage during validation
	 * Entity order = entities.builder(Order.TYPE)
	 *     .with(Order.STATUS, "SHIPPED")
	 *     .build(); // No tracking number
	 * 
	 * boolean canBeNull = validator.nullable(order, Order.TRACKING_NUMBER); // false
	 * }
	 * @param entity the entity being validated
	 * @param attribute the attribute
	 * @param <T> the value type
	 * @return true if the attribute accepts a null value
	 */
	<T> boolean nullable(Entity entity, Attribute<T> attribute);

	/**
	 * Returns true if the given entity contains only valid values.
	 * @param entity the entity
	 * @return true if the given entity contains only valid values
	 */
	boolean valid(Entity entity);

	/**
	 * Checks if the values in the given entity are valid.
	 * Note that by default, if the entity instance does not exist according to
	 * {@link Entity#exists()} all values are validated, otherwise only modified values are validated.
	 * Use the {@link #STRICT_VALIDATION} configuration value to change the default behaviour.
	 * {@snippet :
	 * // Validation during entity lifecycle
	 * Entity customer = entities.builder(Customer.TYPE)
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
	 *     System.err.println("Validation failed: " + e.getMessage());
	 *     // e.getMessage() might be: "Invalid email format: invalid-email"
	 * }
	 * 
	 * // Check if entity is valid without throwing exception
	 * if (validator.valid(customer)) {
	 *     connection.insert(customer);
	 * } else {
	 *     // Handle invalid entity
	 * }
	 * }
	 * @param entity the entity
	 * @throws ValidationException in case of an invalid value
	 * @see #STRICT_VALIDATION
	 */
	void validate(Entity entity) throws ValidationException;

	/**
	 * Checks if the value associated with the give attribute is valid, throws a ValidationException if not
	 * @param entity the entity to validate
	 * @param attribute the attribute the value is associated with
	 * @param <T> the value type
	 * @throws ValidationException if the given value is not valid for the given attribute
	 */
	<T> void validate(Entity entity, Attribute<T> attribute) throws ValidationException;
}
