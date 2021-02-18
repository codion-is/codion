/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.framework.domain.property.ColumnProperty;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class AutomaticKeyGenerator extends AbstractQueriedKeyGenerator {

  private final String valueSource;

  AutomaticKeyGenerator(final String valueSource) {
    this.valueSource = requireNonNull(valueSource, "valueSource");
  }

  @Override
  public boolean isInserted() {
    return false;
  }

  @Override
  public void afterInsert(final Entity entity, final List<ColumnProperty<?>> primaryKeyProperties,
                          final DatabaseConnection connection, final Statement insertStatement) throws SQLException {
    selectAndPut(entity, primaryKeyProperties.get(0), connection);
  }

  @Override
  protected String getQuery(final Database database) {
    return database.getAutoIncrementQuery(valueSource);
  }
}
