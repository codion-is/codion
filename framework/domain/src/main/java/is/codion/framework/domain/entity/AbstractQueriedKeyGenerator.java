/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.framework.domain.property.ColumnProperty;

import java.sql.SQLException;
import java.sql.Types;

abstract class AbstractQueriedKeyGenerator implements KeyGenerator {

  protected final <T> void selectAndPut(Entity entity, ColumnProperty<T> keyProperty,
                                        DatabaseConnection connection) throws SQLException {
    switch (keyProperty.getColumnType()) {
      case Types.INTEGER:
        entity.put((Attribute<Integer>) keyProperty.getAttribute(),
                connection.selectInteger(getQuery(connection.getDatabase())));
        break;
      case Types.BIGINT:
        entity.put((Attribute<Long>) keyProperty.getAttribute(),
                connection.selectLong(getQuery(connection.getDatabase())));
        break;
      default:
        throw new SQLException("Queried key generator only implemented for Types.INTEGER and Types.BIGINT datatypes", null, null);
    }
  }

  protected abstract String getQuery(Database database);
}
