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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.generator.model;

import is.codion.framework.domain.db.SchemaDomain;
import is.codion.framework.domain.db.SchemaDomain.SchemaSettings;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class SchemaRow {

	private final String catalog;
	private final String schema;

	private SchemaSettings schemaSettings;
	private SchemaDomain domainModel;

	SchemaRow(SchemaSettings schemaSettings) {
		this(null, "NO_SCHEMA", schemaSettings);
	}

	SchemaRow(String catalog, String schema, SchemaSettings schemaSettings) {
		this.catalog = catalog;
		this.schema = schema;
		this.schemaSettings = schemaSettings;
	}

	String catalog() {
		return catalog;
	}

	String schema() {
		return schema;
	}

	String name() {
		return schema;
	}

	boolean populated() {
		return domainModel != null;
	}

	public SchemaSettings schemaSettings() {
		return schemaSettings;
	}

	public void setSchemaSettings(SchemaSettings schemaSettings) {
		this.schemaSettings = requireNonNull(schemaSettings);
	}

	Optional<SchemaDomain> domain() {
		return Optional.ofNullable(domainModel);
	}

	void setDomain(SchemaDomain domain) {
		this.domainModel = domain;
	}
}
