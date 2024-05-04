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
package is.codion.swing.framework.model.tools.generator;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.EntityDefinition;

public final class DefinitionRow {

	final String tableType;
	final EntityDefinition definition;
	final DatabaseDomain domain;

	DefinitionRow(EntityDefinition definition, DatabaseDomain domain) {
		this.definition = definition;
		this.domain = domain;
		this.tableType = domain.tableType(definition.entityType());
	}

	Domain domain() {
		return domain;
	}
}
