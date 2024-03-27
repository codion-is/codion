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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class MetaDataForeignKeyConstraint {

	private final MetaDataTable referencedTable;
	private final Map<MetaDataColumn, MetaDataColumn> references = new LinkedHashMap<>();

	MetaDataForeignKeyConstraint(MetaDataTable referencedTable) {
		this.referencedTable = requireNonNull(referencedTable);
	}

	public MetaDataTable referencedTable() {
		return referencedTable;
	}

	public Map<MetaDataColumn, MetaDataColumn> references() {
		return Collections.unmodifiableMap(references);
	}

	void addReference(MetaDataColumn fkColumn, MetaDataColumn pkColumn) {
		references.put(fkColumn, pkColumn);
	}
}
