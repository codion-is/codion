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

import is.codion.demos.chinook.domain.api.Chinook.Artist;
import is.codion.demos.chinook.domain.api.Chinook.Employee;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.model.EntityQueryModel;
import is.codion.framework.model.EntityTableModel;

import java.util.List;

/**
 * The {@link EntityQueryModel} javadoc snippets, each the region of the same name.
 */
final class EntityQueryModelSnippets {

	void dataSource(EntityTableModel<?, ?> tableModel) {
		tableModel.query().dataSource().set(query -> { // @start region=dataSource
			EntityConnection connection = query.connection();

			return connection.select(Employee.LASTNAME.equalTo("Peacock"));
		}); // @end
	}

	void defaults(EntityTableModel<?, ?> tableModel) {
		// Replace defaults with a minimal set // @start region=defaults
		EntityQueryModel query = tableModel.query();
		query.attributes().defaults().set(List.of(Employee.ID, Employee.LASTNAME));
		tableModel.items().refresh();

		// Revert to entity definition defaults
		query.attributes().defaults().clear();
		tableModel.items().refresh(); // @end
	}

	void excluded(EntityTableModel<?, ?> tableModel) {
		// Exclude expensive computed columns // @start region=excluded
		EntityQueryModel query = tableModel.query();
		query.attributes().excluded().add(Artist.NUMBER_OF_TRACKS);
		tableModel.items().refresh();

		// Re-include the column
		query.attributes().excluded().remove(Artist.NUMBER_OF_TRACKS);
		tableModel.items().refresh(); // @end
	}
}
