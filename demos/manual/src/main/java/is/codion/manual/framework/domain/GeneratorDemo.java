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
package is.codion.manual.framework.domain;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.manual.app.store.domain.Store.Address;
import is.codion.manual.app.store.domain.Store.Customer;

import static is.codion.framework.domain.entity.attribute.Column.Generator.*;
import static is.codion.framework.domain.entity.condition.Condition.all;

final class GeneratorDemo {

	void columnDefaultKey() {
		// tag::columnDefault[]
		// The key made by the column's default, random_uuid() for example, rather than the application
		Customer.ID.as()
						.primaryKey()
						.generator(columnDefault());
		// end::columnDefault[]
	}

	void preservingKey() {
		// tag::preserving[]
		// A key given is inserted as it is, identity() generating one where none is given
		Address.ID.as()
						.primaryKey()
						.generator(preserving(identity()));
		// end::preserving[]
	}

	void copyWithKeys(EntityConnection server, EntityConnection embedded) {
		// tag::preservingCopy[]
		// The addresses copied with their keys, the rows referencing them valid in both databases
		embedded.insert(server.select(all(Address.TYPE)));
		// end::preservingCopy[]
	}

	void insertAsNew(Entity address, EntityConnection connection) {
		// tag::preservingNew[]
		// A copy inserted as a new row, its key cleared, otherwise refused as a duplicate
		connection.insert(address.copy().builder()
						.clearPrimaryKey()
						.build());
		// end::preservingNew[]
	}
}
