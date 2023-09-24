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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
/**
 * Core framework database connection classes, such as:<br>
 * <br>
 * {@link is.codion.framework.db.EntityConnection}<br>
 * {@link is.codion.framework.db.EntityConnectionProvider}<br>
 * @uses is.codion.framework.db.EntityConnectionProvider.Builder
 */
module is.codion.framework.db.core {
  requires org.slf4j;
  requires transitive is.codion.framework.domain;

  exports is.codion.framework.db;

  uses is.codion.framework.db.EntityConnectionProvider.Builder;
}