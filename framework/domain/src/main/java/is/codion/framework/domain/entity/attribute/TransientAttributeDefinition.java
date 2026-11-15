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
 * but are ignored during insert, update, and delete operations. Setting a transient
 * value therefore does not mark the entity as modified, unless the attribute is configured
 * to, for one standing for something a custom persistence stores by other means, see
 * {@link Builder#modifies(boolean)}.
 * <p>
 * Transient attributes are useful for UI state, temporary calculations and values the record carries without
 * storing. They are editable fields wherever the framework builds components from attribute definitions, which is
 * what makes them the way a form holds something the table does not store:
 * {@snippet class = "is.codion.demos.chinook.javadoc.TransientAttributeDefinitionSnippets" region = "usage":
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
 *                   // UI state, not marking the entity modified
 *                   Customer.SELECTED.as()
 *                           .attribute()
 *                           .defaultValue(false)
 *                           .caption("Selected"),
 *
 *                   // Notes a custom persistence stores elsewhere
 *                   Customer.NOTES.as()
 *                           .attribute()
 *                           .modifies(true) // Marks the entity as modified
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
 *   // Transient attributes are ignored by the default persistence, which
 *   // throws when updating an entity with only transient values modified
 * } // @end}
 * @param <T> the attribute value type
 * @see #modifies()
 */
public sealed interface TransientAttributeDefinition<T> extends ValueAttributeDefinition<T> permits DefaultTransientAttributeDefinition {

	/**
	 * @return true if the value of this attribute being modified results in a modified entity, false by default
	 * @see Builder#modifies(boolean)
	 */
	boolean modifies();

	/**
	 * Builds a transient AttributeDefinition instance
	 * @param <T> the attribute value type
	 */
	sealed interface Builder<T, B extends Builder<T, B>> extends ValueAttributeDefinition.Builder<T, B>
					permits DefaultTransientAttributeDefinitionBuilder {

		/**
		 * <p>Specifies whether the value being modified makes the owning entity modified, for a transient attribute
		 * standing for something a custom persistence stores by other means, a password set through a procedure for
		 * example. The entity then reads as modified, enabling an update, as an edit panel does.
		 * <p>The default persistence updates columns only, throwing for an entity with no column modified, so an update
		 * of an entity with only such values modified must be handled by the custom persistence: storing the value,
		 * saving it, and updating the entity only if a column is modified as well.
		 * <p>Default false.
		 * @param modifies true if modifying the value makes the owning entity modified
		 * @return this builder instance
		 */
		Builder<T, B> modifies(boolean modifies);
	}
}
