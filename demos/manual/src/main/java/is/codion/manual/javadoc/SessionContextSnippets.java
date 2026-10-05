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
package is.codion.manual.javadoc;

import is.codion.common.db.database.ClientInfo;
import is.codion.common.db.database.SessionContext;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * The {@link SessionContext} javadoc snippets, each the region of the same name.
 */
final class SessionContextSnippets {

  void usage() {
    class AuditContext implements SessionContext { // @start region=usage

      @Override
      public void prepare(Connection connection, ClientInfo clientInfo) throws SQLException {
        try (CallableStatement statement = connection.prepareCall("{call set_audit_user(?)}")) {
          statement.setString(1, clientInfo.user());
          statement.execute();
        }
      }

      @Override
      public void release(Connection connection, ClientInfo clientInfo) throws SQLException {
        try (CallableStatement statement = connection.prepareCall("{call clear_audit_user()}")) {
          statement.execute();
        }
      }
    } // @end
  }
}
