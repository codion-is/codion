/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

import java.sql.SQLException;

final class ConfigureDb extends DefaultDomain {

  private static final DomainType DOMAIN = DomainType.domainType(ConfigureDb.class);

  ConfigureDb() {
    super(DOMAIN);
    configured();
  }

  interface Configured {
    EntityType TYPE = DOMAIN.entityType("scott.configured");
    Column<Integer> ID = TYPE.integerColumn("id");
  }

  void configured() {
    add(Configured.TYPE.define(Configured.ID.primaryKey()));
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
