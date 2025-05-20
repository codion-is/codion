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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityQueries;
import is.codion.framework.domain.entity.Entities;

import static java.util.Objects.requireNonNull;

final class DefaultEntityQueries implements EntityQueries {

	private final Entities entities;
	private final SelectQueries selectQueries;

	DefaultEntityQueries(Database database, Entities entities) {
		this.entities = requireNonNull(entities);
		this.selectQueries = new SelectQueries(requireNonNull(database));
	}

	@Override
	public String select(Select select) {
		requireNonNull(select);

		return selectQueries.builder(entities.definition(select.where().entityType())).select(select).build();
	}
}
