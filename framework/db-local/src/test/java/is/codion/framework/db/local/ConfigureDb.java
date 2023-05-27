/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import java.sql.SQLException;

import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.property.Property.primaryKeyProperty;

final class ConfigureDb extends DefaultDomain {

  private static final DomainType DOMAIN = DomainType.domainType(ConfigureDb.class);

  ConfigureDb() {
    super(DOMAIN);
    configured();
  }

  interface Configured {
    EntityType TYPE = DOMAIN.entityType("scott.configured");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
  }

  void configured() {
    add(definition(primaryKeyProperty(Configured.ID)));
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
