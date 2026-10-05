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
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entity;

import java.util.List;

/**
 * The {@link EntityConnection} javadoc snippets, each the region of the same name.
 */
final class EntityConnectionSnippets {

  void include(EntityConnection connection) {
    // Include a lazy blob column along with all defaults // @start region=include
    List<Entity> countries = connection.select(
            Select.all(Country.TYPE)
                    .include(Country.FLAG)); // FLAG is .selected(false)

    // Include a lazy column with explicit attributes
    List<Entity> flags = connection.select(
            Select.all(Country.TYPE)
                    .attributes(Country.CODE, Country.NAME)
                    .include(Country.FLAG)); // @end
  }

  void exclude(EntityConnection connection) {
    // Exclude an expensive subquery column // @start region=exclude
    List<Entity> countries = connection.select(
            Select.all(Country.TYPE)
                    .exclude(Country.NO_OF_CITIES));

    // Include a lazy column, exclude others
    List<Entity> flags = connection.select(
            Select.all(Country.TYPE)
                    .include(Country.FLAG)
                    .exclude(Country.NO_OF_CITIES, Country.NO_OF_LANGUAGES)); // @end
  }

  void builderInclude() {
    // Include the lazy FLAG column with all defaults // @start region=builderInclude
    Select.all(Country.TYPE)
            .include(Country.FLAG);

    // Include the lazy column with specific attributes
    Select.all(Country.TYPE)
            .attributes(Country.CODE, Country.NAME)
            .include(Country.FLAG); // @end
  }

  void builderExclude() {
    // Exclude the expensive subquery columns // @start region=builderExclude
    Select.all(Country.TYPE)
            .exclude(Country.NO_OF_CITIES, Country.NO_OF_LANGUAGES);

    // Include lazy, exclude others
    Select.all(Country.TYPE)
            .include(Country.FLAG)
            .exclude(Country.NO_OF_CITIES, Country.NO_OF_LANGUAGES); // @end
  }
}
