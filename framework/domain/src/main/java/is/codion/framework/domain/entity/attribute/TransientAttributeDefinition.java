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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
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
 * Transient attributes are useful for UI state, temporary calculations and values the record carries without
 * storing. They are editable fields wherever the framework builds components from attribute definitions, which is
 * what makes them the way a form holds something the table does not store:
 * {@snippet class = "is.codion.demos.chinook.javadoc.TransientAttributeDefinitionSnippets" region = "usage" :
 * interface Customer { // @start region=usage
 *   EntityType TYPE = DOMAIN.entityType("chinook.customer");
 *
 *   // Database columns
 *   Column<Long> ID = TYPE.longColumn("id");
 *   Column<String> FIRSTNAME = TYPE.stringColumn("firstname");
 *   Column<String> LASTNAME = TYPE.stringColumn("lastname");
 *
 *   // Transient attributes
 *   Attribute<Boolean> SELECTED = TYPE.booleanAttribute("selected");
 *   Attribute<String> NOTES = TYPE.stringAttribute("notes");
 * }
 *
 * EntityDefinition customer() {
 *   return Customer.TYPE.as()
 *           .attributes(
 *                   // Database columns
 *                   Customer.ID.as()
 *                           .primaryKey(),
 *                   Customer.FIRSTNAME.as()
 *                           .column()
 *                           .caption("First name"),
 *                   Customer.LASTNAME.as()
 *                           .column()
 *                           .caption("Last name"),
 *
 *                   // UI state attribute that doesn't modify entity
 *                   Customer.SELECTED.as()
 *                           .attribute()
 *                           .modifies(false) // Doesn't mark entity as modified
 *                           .defaultValue(false)
 *                           .caption("Selected"),
 *
 *                   // Temporary notes (modifies entity by default)
 *                   Customer.NOTES.as()
 *                           .attribute()
 *                           .caption("Notes"))
 *           .build();
 * }
 *
 * void customers(EntityConnection connection) {
 *   // Transient attributes are initialized to null when entities are loaded
 *   Entity customer = connection.selectSingle(Customer.ID.equalTo(1L));
 *
 *   customer.set(Customer.SELECTED, true);
 *   customer.modified(); // false, SELECTED doesn't modify the entity
 *
 *   customer.set(Customer.NOTES, "Important customer");
 *   customer.modified(); // true, NOTES modifies the entity
 *
 *   // Transient attributes are ignored during database operations,
 *   // updating an entity with only transient values modified results in an error
 * } // @end}
 * @param <T> the attribute value type
 * @see #modifies()
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
