/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
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
