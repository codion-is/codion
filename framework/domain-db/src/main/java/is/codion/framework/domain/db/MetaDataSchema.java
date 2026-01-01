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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.db;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.domain.db.MetaDataTable.TablePacker;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

final class MetaDataSchema {

	static final MetaDataSchema NO_SCHEMA = new MetaDataSchema("NO_SCHEMA");

	private final String name;
	private final Map<String, MetaDataTable> tables = new HashMap<>();

	MetaDataSchema(String name) {
		this.name = requireNonNull(name);
	}

	String name() {
		return name;
	}

	boolean none() {
		return this == NO_SCHEMA;
	}

	Map<String, MetaDataTable> tables() {
		return unmodifiableMap(tables);
	}

	void populate(DatabaseMetaData metaData, Map<String, MetaDataSchema> schemas) {
		populate(metaData, schemas, new HashSet<>());
	}

	private void populate(DatabaseMetaData metaData, Map<String, MetaDataSchema> schemas, Set<String> populatedSchemas) {
		if (!populatedSchemas.contains(name)) {
			tables.clear();
			try (ResultSet resultSet = metaData.getTables(null, none() ? null : name, null, new String[] {"TABLE", "VIEW"})) {
				tables.putAll(new TablePacker(this, metaData, null).pack(resultSet).stream()
								.collect(toMap(MetaDataTable::tableName, Function.identity())));
				tables.values().stream()
								.flatMap(table -> table.referencedSchemaNames().stream())
								.map(schemas::get)
								.forEach(schema -> schema.populate(metaData, schemas, populatedSchemas));
				tables.values().forEach(table -> table.resolveForeignKeys(schemas));
				populatedSchemas.add(name);
			}
			catch (SQLException e) {
				throw new DatabaseException(e);
			}
		}
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		MetaDataSchema schema = (MetaDataSchema) object;

		return name.equals(schema.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
