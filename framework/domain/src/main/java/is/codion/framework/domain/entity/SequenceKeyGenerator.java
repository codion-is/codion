/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.framework.domain.property.ColumnProperty;

import java.sql.SQLException;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class SequenceKeyGenerator extends AbstractQueriedKeyGenerator {

  private final String sequenceName;

  SequenceKeyGenerator(final String sequenceName) {
    this.sequenceName = requireNonNull(sequenceName, "sequenceName");
  }

  @Override
  public void beforeInsert(final Entity entity, final List<ColumnProperty<?>> primaryKeyProperties,
                           final DatabaseConnection connection) throws SQLException {
    ColumnProperty<?> primaryKeyProperty = primaryKeyProperties.get(0);
    if (entity.isNull(primaryKeyProperty.getAttribute())) {
      selectAndPut(entity, primaryKeyProperty, connection);
    }
  }

  @Override
  protected String getQuery(final Database database) {
    return database.getSequenceQuery(sequenceName);
  }
}
