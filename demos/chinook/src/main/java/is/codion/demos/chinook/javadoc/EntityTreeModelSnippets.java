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

import is.codion.demos.chinook.domain.api.Chinook.Employee;
import is.codion.framework.model.EntityTreeModel;

import static is.codion.framework.domain.entity.OrderBy.ascending;

/**
 * The {@link EntityTreeModel} javadoc snippets, each the region of the same name.
 */
final class EntityTreeModelSnippets {

  void roots() {
    EntityTreeModel.builder() // @start region=roots
            .roots(Employee.TYPE, roots -> roots
                    .condition(() -> Employee.LASTNAME.equalTo("Edwards"))
                    .select(select -> select.orderBy(ascending(Employee.LASTNAME)))); // @end
  }

  void children(EntityTreeModel.Builder<?> builder) {
    builder.children(Employee.REPORTSTO_FK, children -> children // @start region=children
            .condition(() -> Employee.EMAIL.isNotNull())
            .select(select -> select.referenceDepth(0))); // @end
  }
}
