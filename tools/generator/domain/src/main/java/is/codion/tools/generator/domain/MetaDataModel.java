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
package is.codion.tools.generator.domain;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

final class MetaDataModel {

	private final MetaDataSchema schema;
	private final Map<String, MetaDataSchema> schemas;

	MetaDataModel(DatabaseMetaData metaData, String schemaName) throws SQLException {
		this.schemas = discoverSchemas(metaData);
		this.schema = schemas.get(schemaName);
		if (this.schema == null) {
			throw new IllegalArgumentException("Schema not found: " + schemaName);
		}
		this.schema.populate(metaData, schemas);
	}

	MetaDataSchema schema() {
		return schema;
	}

	private static Map<String, MetaDataSchema> discoverSchemas(DatabaseMetaData metaData) throws SQLException {
		Map<String, MetaDataSchema> schemas = new HashMap<>();
		try (ResultSet resultSet = metaData.getSchemas()) {
			while (resultSet.next()) {
				String tableSchem = resultSet.getString("TABLE_SCHEM");
				if (tableSchem != null) {
					schemas.put(tableSchem, new MetaDataSchema(tableSchem));
				}
			}
		}

		return schemas;
	}
}
