/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;

import java.sql.SQLException;
import java.sql.Statement;

import static java.util.Objects.requireNonNull;

final class AutomaticKeyGenerator extends AbstractQueriedKeyGenerator {

  private final String valueSource;

  AutomaticKeyGenerator(String valueSource) {
    this.valueSource = requireNonNull(valueSource, "valueSource");
  }

  @Override
  public boolean isInserted() {
    return false;
  }

  @Override
  public void afterInsert(Entity entity, DatabaseConnection connection, Statement insertStatement) throws SQLException {
    selectAndPopulate(entity, entity.definition().primaryKey().definitions().get(0), connection);
  }

  @Override
  protected String query(Database database) {
    return database.autoIncrementQuery(valueSource);
  }
}
