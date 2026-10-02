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
package is.codion.demos.schemabrowser.javadoc;

import is.codion.demos.schemabrowser.domain.SchemaBrowser.Table;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import static is.codion.demos.schemabrowser.domain.SchemaBrowser.DOMAIN;

/**
 * The {@link EntityType} javadoc snippets, each the region of the same name.
 * <p>{@link TableColumn} mirrors a part of the SchemaBrowser domain, for the compositeForeignKey region to declare.
 */
final class EntityTypeSnippets {

	// Composite foreign key (two columns) // @start region=compositeForeignKey
	interface TableColumn {
		EntityType TYPE = DOMAIN.entityType("column");

		Column<String> NAME = TYPE.stringColumn("column_name");

		// Foreign key columns, referencing the composite primary key of Table
		Column<String> SCHEMA = TYPE.stringColumn("table_schema");
		Column<String> TABLE_NAME = TYPE.stringColumn("table_name");

		// Composite foreign key
		ForeignKey TABLE_FK = TYPE.foreignKey("table_fk",
						SCHEMA, Table.SCHEMA,
						TABLE_NAME, Table.NAME);
	} // @end
}
