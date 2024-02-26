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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.tools.loadtest;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.model.loadtest.LoadTest.Scenario;
import is.codion.common.model.loadtest.LoadTest.Scenario.Performer;
import is.codion.common.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Collections.emptyList;

/**
 * A load test implementation for testing database queries.
 */
public final class QueryLoadTestModel {

  private static final int DEFAULT_MAXIMUM_THINK_TIME_MS = 500;
  private static final int DEFAULT_LOGIN_DELAY_FACTOR = 2;
  private static final int DEFAULT_BATCH_SIZE = 5;

  private final LoadTest<QueryApplication> loadTest;
  private final ConnectionPoolWrapper pool;

  /**
   * Instantiates a new QueryLoadTest.
   * @param database the database
   * @param user the user
   * @param scenarios the query scenarios
   * @throws DatabaseException in case of an exception while constructing the initial connections
   */
  public QueryLoadTestModel(Database database, User user,
                            Collection<? extends Scenario<QueryApplication>> scenarios) throws DatabaseException {
    this.loadTest = LoadTest.builder(this::createApplication, this::disconnectApplication)
            .user(user)
            .scenarios(scenarios)
            .minimumThinkTime(DEFAULT_MAXIMUM_THINK_TIME_MS / 2)
            .maximumThinkTime(DEFAULT_MAXIMUM_THINK_TIME_MS)
            .loginDelayFactor(DEFAULT_LOGIN_DELAY_FACTOR)
            .applicationBatchSize(DEFAULT_BATCH_SIZE)
            .build();
    ConnectionPoolFactory poolProvider = ConnectionPoolFactory.instance();
    this.pool = poolProvider.createConnectionPool(database, user);
    loadTest.addShutdownListener(pool::close);
  }

  public LoadTest<QueryApplication> loadTest() {
    return loadTest;
  }

  /**
   * @return the underlying connection pool
   */
  public ConnectionPoolWrapper connectionPool() {
    return pool;
  }

  private void disconnectApplication(QueryApplication application) {/*Not required*/}

  private QueryApplication createApplication(User user) {
    return new QueryApplication(pool);
  }

  /**
   * A class used internally
   */
  public static final class QueryApplication {

    private final ConnectionPoolWrapper pool;

    private QueryApplication(ConnectionPoolWrapper pool) {
      this.pool = pool;
    }
  }

  public static class QueryPerformer implements Performer<QueryApplication> {

    private final User user;
    private final String query;
    private final boolean transactional;

    /**
     * Instantiates a new non-transactional QueryPerformer.
     * @param user the user
     * @param query the query
     */
    public QueryPerformer(User user, String query) {
      this(user, query, false);
    }

    /**
     * Instantiates a new QueryPerformer.
     * @param user the user
     * @param query the query
     * @param transactional if true, commit and rollback is performed on success and error respectively
     */
    public QueryPerformer(User user, String query, boolean transactional) {
      this.user = user;
      this.query = query;
      this.transactional = transactional;
    }

    @Override
    public void perform(QueryApplication application) throws Exception {
      Connection connection = null;
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      try {
        connection = application.pool.connection(user);
        statement = connection.prepareCall(query);
        setStatementParameters(statement);
        resultSet = statement.executeQuery();
        fetchResult(resultSet);
        if (transactional) {
          connection.commit();
        }
      }
      catch (Exception e) {
        if (transactional && connection != null) {
          try {
            connection.rollback();
          }
          catch (SQLException ignored) {/*ignored*/}
        }
        throw e;
      }
      finally {
        closeSilently(connection);
        closeSilently(resultSet);
        closeSilently(statement);
      }
    }

    /**
     * For overriding, returns the parameter values to use for the next query execution,
     * these must of course match the parameter slots in the underlying query.
     * @return a list of parameters for the next query run
     */
    protected List<Object> parameters() {
      return emptyList();
    }

    private void setStatementParameters(PreparedStatement statement) throws SQLException {
      List<Object> parameters = parameters();
      if (!nullOrEmpty(parameters)) {
        int index = 1;
        for (Object parameter : parameters) {
          statement.setObject(index++, parameter);
        }
      }
    }

    private static void fetchResult(ResultSet resultSet) throws SQLException {
      int columnCount = resultSet.getMetaData().getColumnCount();
      if (columnCount > 0) {
        while (resultSet.next()) {
          for (int i = 1; i <= columnCount; i++) {
            resultSet.getObject(i);
          }
        }
      }
    }

    private static void closeSilently(AutoCloseable closeable) {
      try {
        if (closeable != null) {
          closeable.close();
        }
      }
      catch (Exception ignored) {/*ignored*/}
    }
  }
}
