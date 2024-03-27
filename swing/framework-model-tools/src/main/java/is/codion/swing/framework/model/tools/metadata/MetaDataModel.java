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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.metadata;

import is.codion.common.db.exception.DatabaseException;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;

public final class MetaDataModel {

	private final Map<String, MetaDataSchema> schemas;
	private final DatabaseMetaData metaData;

	public MetaDataModel(DatabaseMetaData metaData) throws DatabaseException {
		this.metaData = requireNonNull(metaData);
		try {
			this.schemas = discoverSchemas(metaData);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

	public Collection<MetaDataSchema> schemas() {
		return unmodifiableCollection(schemas.values());
	}

	public void populateSchema(String schemaName, Consumer<String> schemaNotifier) {
		MetaDataSchema schema = schemas.get(requireNonNull(schemaName));
		if (schema == null) {
			throw new IllegalArgumentException("Schema not found: " + schemaName);
		}
		schema.populate(metaData, schemas, schemaNotifier, new HashSet<>());
	}

	private static Map<String, MetaDataSchema> discoverSchemas(DatabaseMetaData metaData) throws SQLException {
		Map<String, MetaDataSchema> schemas = new HashMap<>();
		try (ResultSet resultSet = metaData.getSchemas()) {
			while (resultSet.next()) {
				String tableSchem = resultSet.getString("TABLE_SCHEM");
				if (tableSchem != null) {
					schemas.put(tableSchem, new MetaDataSchema(tableSchem, resultSet.getString("TABLE_CATALOG")));
				}
			}
		}

		return schemas;
	}
}
