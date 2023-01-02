/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

  SequenceKeyGenerator(String sequenceName) {
    this.sequenceName = requireNonNull(sequenceName, "sequenceName");
  }

  @Override
  public void beforeInsert(Entity entity, List<ColumnProperty<?>> primaryKeyProperties,
                           DatabaseConnection connection) throws SQLException {
    ColumnProperty<?> primaryKeyProperty = primaryKeyProperties.get(0);
    if (entity.isNull(primaryKeyProperty.attribute())) {
      selectAndPut(entity, primaryKeyProperty, connection);
    }
  }

  @Override
  protected String query(Database database) {
    return database.sequenceQuery(sequenceName);
  }
}
