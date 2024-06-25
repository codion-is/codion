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
package is.codion.swing.framework.generator.model;

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.framework.generator.model.metadata.MetaDataSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public final class SchemaRow {

	final MetaDataSchema metadata;
	final String catalog;
	final String schema;

	boolean populated;
	DatabaseDomain domainModel;
	final Collection<EntityDefinition> entityDefinitions = new ArrayList<>();

	SchemaRow(MetaDataSchema metadata, String catalog, String schema, boolean populated) {
		this.metadata = metadata;
		this.catalog = catalog;
		this.schema = schema;
		this.populated = populated;
	}

	String catalog() {
		return catalog;
	}

	String name() {
		return schema;
	}

	boolean populated() {
		return populated;
	}

	Optional<DatabaseDomain> domain() {
		return Optional.ofNullable(domainModel);
	}

	void setDomain(DatabaseDomain domain) {
		this.domainModel = domain;
		this.entityDefinitions.clear();
		this.entityDefinitions.addAll(domain.entities().definitions());
		this.populated = true;
	}
}
