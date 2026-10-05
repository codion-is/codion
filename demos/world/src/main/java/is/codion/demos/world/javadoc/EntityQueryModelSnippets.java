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
package is.codion.demos.world.javadoc;

import is.codion.demos.world.domain.api.World.Country;
import is.codion.framework.model.EntityQueryModel;
import is.codion.framework.model.EntityTableModel;

/**
 * The {@link EntityQueryModel} javadoc snippets, each the region of the same name.
 */
final class EntityQueryModelSnippets {

  void included(EntityTableModel<?, ?> tableModel) {
    // Include a lazy blob column on-demand // @start region=included
    EntityQueryModel query = tableModel.query();
    query.attributes().included().add(Country.FLAG);
    tableModel.items().refresh();

    // Remove the lazy attribute
    query.attributes().included().remove(Country.FLAG);
    tableModel.items().refresh(); // @end
  }
}
