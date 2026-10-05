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
package is.codion.demos.chinook.javadoc;

import is.codion.framework.domain.entity.query.EntitySelectQuery;

/**
 * The {@link EntitySelectQuery} javadoc snippets, each the region of the same name.
 */
final class EntitySelectQuerySnippets {

  void with() {
    EntitySelectQuery.builder() // @start region=with
            .with("track_revenue")
            .as("SELECT track_id, SUM(unitprice * quantity) AS revenue FROM chinook.invoiceline GROUP BY track_id")
            .from("track_revenue")
            .columns("track_id, revenue")
            .build(); // @end
  }
}
