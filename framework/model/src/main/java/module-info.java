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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
/**
 * Framework application model classes, such as:<br>
 * <br>
 * {@link is.codion.framework.model.EntityModel}<br>
 * {@link is.codion.framework.model.EntityEditModel}<br>
 * {@link is.codion.framework.model.EntityTableModel}<br>
 * {@link is.codion.framework.model.EntityTableConditionModel}<br>
 * {@link is.codion.framework.model.EntityApplicationModel}<br>
 */
module is.codion.framework.model {
  requires org.slf4j;
  requires org.json;
  requires transitive is.codion.common.model;
  requires transitive is.codion.framework.db.core;

  exports is.codion.framework.model;
}