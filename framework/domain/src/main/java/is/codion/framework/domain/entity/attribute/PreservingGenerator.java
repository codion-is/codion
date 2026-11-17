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
package is.codion.framework.domain.entity.attribute;

import is.codion.common.db.database.Database;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column.Generator;
import is.codion.framework.domain.entity.attribute.Column.Generator.Preserving;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static java.util.Objects.requireNonNull;

final class PreservingGenerator<T> implements Preserving<T> {

	private final Generator<T> generator;

	PreservingGenerator(Generator<T> generator) {
		this.generator = requireNonNull(generator);
	}

	@Override
	public void beforeInsert(Entity entity, Column<T> column, Database database, Connection connection) throws SQLException {
		if (!entity.present(column)) {
			// a null held is not written, the column left out where the database makes the value
			entity.remove(column);
			generator.beforeInsert(entity, column, database, connection);
		}
	}

	@Override
	public void afterInsert(Entity entity, Column<T> column, Database database, Statement statement) throws SQLException {
		if (!entity.present(column)) {
			generator.afterInsert(entity, column, database, statement);
		}
	}

	// the column written where the entity holds a value, the insert writing the columns it holds after beforeInsert()
	@Override
	public boolean inserted() {
		return true;
	}

	@Override
	public boolean generatedKeys() {
		return generator.generatedKeys();
	}
}
