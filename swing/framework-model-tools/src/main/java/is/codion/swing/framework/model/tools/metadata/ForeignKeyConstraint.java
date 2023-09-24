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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class ForeignKeyConstraint {

  private final Table referencedTable;
  private final Map<MetadataColumn, MetadataColumn> references = new LinkedHashMap<>();

  ForeignKeyConstraint(Table referencedTable) {
    this.referencedTable = requireNonNull(referencedTable);
  }

  public Table referencedTable() {
    return referencedTable;
  }

  public Map<MetadataColumn, MetadataColumn> references() {
    return Collections.unmodifiableMap(references);
  }

  void addReference(MetadataColumn fkColumn, MetadataColumn pkColumn) {
    references.put(fkColumn, pkColumn);
  }
}
