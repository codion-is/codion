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
 * {@link is.codion.common.db.database.DatabaseFactory} implementation for HSQLDB.<br>
 * <br>
 * {@link is.codion.dbms.hsqldb.HSQLDatabaseFactory}
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.hsqldb {
  requires transitive is.codion.common.db;

  exports is.codion.dbms.hsqldb;

  provides is.codion.common.db.database.DatabaseFactory
          with is.codion.dbms.hsqldb.HSQLDatabaseFactory;
}