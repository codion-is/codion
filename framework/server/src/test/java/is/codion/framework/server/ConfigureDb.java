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
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

import java.sql.SQLException;

public final class ConfigureDb extends DefaultDomain {

  private static final DomainType DOMAIN = DomainType.domainType(ConfigureDb.class);

  public ConfigureDb() {
    super(DOMAIN);
    configured();
  }

  interface Configured {
    EntityType TYPE = DOMAIN.entityType("scott.configured");
    Column<Integer> ID = TYPE.integerColumn("id");
  }

  void configured() {
    add(Configured.TYPE.define(Configured.ID.define().primaryKey()));
  }

  @Override
  public void configureDatabase(Database database) throws DatabaseException {
    try {
      database.createConnection(User.parse("sa")).createStatement()
              .execute("create table scott.configured(id integer)");
    }
    catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }
}
