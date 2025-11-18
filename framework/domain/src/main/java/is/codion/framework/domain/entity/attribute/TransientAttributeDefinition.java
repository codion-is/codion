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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.attribute.DefaultTransientAttributeDefinition.DefaultTransientAttributeDefinitionBuilder;

/**
 * An attribute that does not map to an underlying database column.
 * <p>
 * TransientAttributeDefinition extends {@link ValueAttributeDefinition} for attributes
 * used for temporary data, UI state, calculated values, or any other data that should
 * not be persisted to the database. They are initialized to null when entities are
 * loaded and ignored during DML operations.
 * <p>
 * The value of transient attributes can be set and retrieved like normal attributes
 * but are ignored during insert, update, and delete operations. By default, setting
 * a transient value marks the entity as modified, but trying to update an entity
 * with only transient values modified will result in an error.
 * <p>
 * Transient attributes are useful for UI state, temporary calculations, and derived values:
 * {@snippet :
 * public class Store extends DefaultDomain {
 *
 *     interface Customer {
 *         EntityType TYPE = DOMAIN.entityType("store.customer");
 *
 *         // Database columns
 *         Column<Integer> ID = TYPE.integerColumn("id");
 *         Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
 *         Column<String> LAST_NAME = TYPE.stringColumn("last_name");
 *         Column<String> EMAIL = TYPE.stringColumn("email");
 *
 *         // Transient attributes
 *         Attribute<String> FULL_NAME = TYPE.stringAttribute("full_name");
 *         Attribute<Boolean> SELECTED = TYPE.booleanAttribute("selected");
 *         Attribute<String> TEMP_NOTES = TYPE.stringAttribute("temp_notes");
 *         Attribute<Object> UI_STATE = TYPE.attribute("ui_state", Object.class);
 *     }
 *
 *     void defineCustomer() {
 *         Customer.TYPE.as(
 *                 // Database columns
 *                 Customer.ID.as()
 *                     .primaryKey(),
 *                 Customer.FIRST_NAME.as()
 *                     .column(),
 *                 Customer.LAST_NAME.as()
 *                     .column(),
 *                 Customer.EMAIL.as()
 *                     .column(),
 *
 *                 // Derived transient attribute (computed from other attributes)
 *                 Customer.FULL_NAME.as()
 *                     .derived()
 * 				             .from(Customer.FIRST_NAME, Customer.LAST_NAME)
 *                     .with(source -> {
 *                         String first = source.get(Customer.FIRST_NAME);
 *                         String last = source.get(Customer.LAST_NAME);
 *                         return ((first != null ? first : "") + " " +
 *                                 (last != null ? last : "")).trim();
 *                     })
 *                     .caption("Full Name"),
 *
 *                 // UI state attribute that doesn't modify entity
 *                 Customer.SELECTED.as()
 *                     .attribute()
 *                     .modifies(false) // Doesn't mark entity as modified
 *                     .defaultValue(false)
 *                     .caption("Selected"),
 *
 *                 // Temporary notes (modifies entity by default)
 *                 Customer.TEMP_NOTES.as()
 *                     .attribute()
 *                     .caption("Temporary Notes"),
 *
 *                 // Generic UI state storage
 *                 Customer.UI_STATE.as()
 *                     .attribute()
 *                     .modifies(false)
 *                     .caption("UI State"))
 *             .build();
 *     }
 * }
 *
 * // Usage examples
 * Entity customer = entities.entity(Customer.TYPE)
 *     .with(Customer.FIRST_NAME, "John")
 *     .with(Customer.LAST_NAME, "Doe")
 *     .with(Customer.EMAIL, "john@example.com")
 *     .build();
 *
 * // Transient attributes can be used for UI state
 * customer.set(Customer.SELECTED, true);  // Doesn't mark entity as modified
 * customer.set(Customer.TEMP_NOTES, "Important customer"); // Marks entity as modified
 *
 * // Derived transient values are computed automatically
 * String fullName = customer.get(Customer.FULL_NAME); // "John Doe"
 *
 * // UI state storage
 * customer.set(Customer.UI_STATE, new HashMap<String, Object>());
 *
 * // Transient attributes are ignored during database operations
 * connection.insert(customer); // Only persists database columns
 *
 * // Check modification state
 * boolean isModified = customer.modified(); // Only true if database columns changed
 *}
 * @param <T> the attribute value type
 * @see #modifies()
 * @see DerivedAttributeDefinition
 */
public sealed interface TransientAttributeDefinition<T> extends ValueAttributeDefinition<T> permits DefaultTransientAttributeDefinition {

	/**
	 * @return true if the value of this attribute being modified should result in a modified entity
	 */
	boolean modifies();

	/**
	 * Builds a transient AttributeDefinition instance
	 * @param <T> the attribute value type
	 */
	sealed interface Builder<T, B extends Builder<T, B>> extends ValueAttributeDefinition.Builder<T, B>
					permits DefaultTransientAttributeDefinitionBuilder {

		/**
		 * Default true.
		 * @param modifies if false then modifications to the value will not result in the owning entity becoming modified
		 * @return this builder instance
		 */
		Builder<T, B> modifies(boolean modifies);
	}
}
