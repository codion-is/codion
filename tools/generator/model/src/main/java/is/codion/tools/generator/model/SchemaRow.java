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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.tools.generator.model;

import is.codion.tools.generator.domain.DatabaseDomain;

import java.util.Optional;

public final class SchemaRow {

	private final String catalog;
	private final String schema;

	private DatabaseDomain domainModel;

	SchemaRow(String catalog, String schema) {
		this.catalog = catalog;
		this.schema = schema;
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

	Optional<DatabaseDomain> domain() {
		return Optional.ofNullable(domainModel);
	}

	void setDomain(DatabaseDomain domain) {
		this.domainModel = domain;
	}
}
