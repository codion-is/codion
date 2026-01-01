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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.db.database.Database;
import is.codion.framework.domain.entity.Entity;

import java.sql.Connection;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

final class SequenceGenerator<T> extends AbstractQueriedGenerator<T> {

	private final String sequenceName;

	SequenceGenerator(String sequenceName) {
		this.sequenceName = requireNonNull(sequenceName);
	}

	@Override
	public void beforeInsert(Entity entity, Column<T> column, Database database, Connection connection) throws SQLException {
		selectAndPopulate(entity, column, database, connection);
	}

	@Override
	protected String query(Database database) {
		return database.sequenceQuery(sequenceName);
	}
}
