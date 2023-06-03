/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.result.ResultIterator;
import is.codion.common.db.result.ResultPacker;
import is.codion.framework.domain.entity.Entity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.NoSuchElementException;

import static is.codion.common.db.database.Database.closeSilently;

final class EntityResultIterator implements ResultIterator<Entity> {

  private final Statement statement;
  private final ResultSet resultSet;
  private final ResultPacker<Entity> resultPacker;

  private boolean hasNext;
  private boolean hasNextCalled;

  /**
   * @param statement the Statement, closed on exception or exhaustion
   * @param resultSet the ResultSet, closed on exception or exhaustion
   * @param resultPacker the ResultPacker
   */
  EntityResultIterator(Statement statement, ResultSet resultSet, ResultPacker<Entity> resultPacker) {
    this.statement = statement;
    this.resultSet = resultSet;
    this.resultPacker = resultPacker;
  }

  @Override
  public boolean hasNext() throws SQLException {
    if (hasNextCalled) {
      return hasNext;
    }
    try {
      hasNext = resultSet.next();
      hasNextCalled = true;
      if (hasNext) {
        return true;
      }
      close();

      return false;
    }
    catch (SQLException e) {
      close();
      throw e;
    }
  }

  @Override
  public Entity next() throws SQLException {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    hasNextCalled = false;
    try {
      return resultPacker.fetch(resultSet);
    }
    catch (SQLException e) {
      close();
      throw e;
    }
  }

  @Override
  public void close() {
    closeSilently(resultSet);
    closeSilently(statement);
  }
}
