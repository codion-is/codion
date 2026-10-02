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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.javadoc;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.TransientAttributeDefinition;

import static is.codion.demos.chinook.domain.api.Chinook.DOMAIN;

/**
 * The {@link TransientAttributeDefinition} javadoc snippets, each the region of the same name.
 * <p>{@link Customer} mirrors a part of the Chinook domain API, with transient attributes added, for the usage region to declare.
 */
final class TransientAttributeDefinitionSnippets {

	interface Customer { // @start region=usage
		EntityType TYPE = DOMAIN.entityType("chinook.customer");

		// Database columns
		Column<Long> ID = TYPE.longColumn("id");
		Column<String> FIRSTNAME = TYPE.stringColumn("firstname");
		Column<String> LASTNAME = TYPE.stringColumn("lastname");

		// Transient attributes
		Attribute<Boolean> SELECTED = TYPE.booleanAttribute("selected");
		Attribute<String> NOTES = TYPE.stringAttribute("notes");
	}

	EntityDefinition customer() {
		return Customer.TYPE.as()
						.attributes(
										// Database columns
										Customer.ID.as()
														.primaryKey(),
										Customer.FIRSTNAME.as()
														.column()
														.caption("First name"),
										Customer.LASTNAME.as()
														.column()
														.caption("Last name"),

										// UI state attribute that doesn't modify entity
										Customer.SELECTED.as()
														.attribute()
														.modifies(false) // Doesn't mark entity as modified
														.defaultValue(false)
														.caption("Selected"),

										// Temporary notes (modifies entity by default)
										Customer.NOTES.as()
														.attribute()
														.caption("Notes"))
						.build();
	}

	void customers(EntityConnection connection) {
		// Transient attributes are initialized to null when entities are loaded
		Entity customer = connection.selectSingle(Customer.ID.equalTo(1L));

		customer.set(Customer.SELECTED, true);
		customer.modified(); // false, SELECTED doesn't modify the entity

		customer.set(Customer.NOTES, "Important customer");
		customer.modified(); // true, NOTES modifies the entity

		// Transient attributes are ignored during database operations,
		// updating an entity with only transient values modified results in an error
	} // @end
}
