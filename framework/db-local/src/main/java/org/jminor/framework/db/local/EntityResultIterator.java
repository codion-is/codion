/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.db.ResultIterator;
import org.jminor.common.db.ResultPacker;
import org.jminor.framework.domain.Entity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.jminor.common.db.Databases.closeSilently;

final class EntityResultIterator implements ResultIterator<Entity> {

  private final Statement statement;
  private final ResultSet resultSet;
  private final ResultPacker<Entity> resultPacker;
  private final int fetchCount;
  private int counter = 0;

  /**
   * @param statement the Statement, closed on exception or exhaustion
   * @param resultSet the ResultSet, closed on exception or exhaustion
   * @param resultPacker the ResultPacker
   * @param fetchCount the maximum number of records to fetch from the result set
   */
  EntityResultIterator(final Statement statement, final ResultSet resultSet,
                       final ResultPacker<Entity> resultPacker, final int fetchCount) {
    this.statement = statement;
    this.resultSet = resultSet;
    this.resultPacker = resultPacker;
    this.fetchCount = fetchCount;
  }

  @Override
  public boolean hasNext() throws SQLException {
    try {
      if ((fetchCount < 0 || counter < fetchCount) && resultSet.next()) {
        return true;
      }
      close();

      return false;
    }
    catch (final SQLException e) {
      close();
      throw e;
    }
  }

  @Override
  public Entity next() throws SQLException {
    counter++;
    try {
      return resultPacker.fetch(resultSet);
    }
    catch (final SQLException e) {
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
