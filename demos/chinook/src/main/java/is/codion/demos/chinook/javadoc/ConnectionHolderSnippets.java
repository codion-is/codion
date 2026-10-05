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

import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.ConnectionHolder;

/**
 * The {@link ConnectionHolder} javadoc snippets, each the region of the same name.
 */
final class ConnectionHolderSnippets {

  void attach(EntityConnection entityConnection, ConnectionPoolWrapper connectionPool, User user) {
    // @start region=attach
    ((ConnectionHolder) entityConnection).attach(connectionPool.connection(user)); // @end
  }
}
