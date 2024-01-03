/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.result.ResultIterator;
import is.codion.common.db.result.ResultPacker;
import is.codion.framework.domain.entity.Entity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.NoSuchElementException;

final class EntityResultIterator implements ResultIterator<Entity> {

  private final Statement statement;
  private final ResultSet resultSet;
  private final ResultPacker<Entity> resultPacker;

  private boolean hasNext;
  private boolean hasNextCalled;

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
    hasNext = resultSet.next();
    hasNextCalled = true;

    return hasNext;
  }

  @Override
  public Entity next() throws SQLException {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    hasNextCalled = false;

    return resultPacker.get(resultSet);
  }

  @Override
  public void close() {
    closeSilently(resultSet);
    closeSilently(statement);
  }

  private static void closeSilently(AutoCloseable closeable) {
    try {
      closeable.close();
    }
    catch (Exception ignored) {/*ignored*/}
  }
}
