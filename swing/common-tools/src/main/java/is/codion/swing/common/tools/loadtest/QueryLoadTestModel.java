/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.loadtest;

import is.codion.common.Util;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPool;
import is.codion.common.db.pool.ConnectionPoolProvider;
import is.codion.common.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A load test implementation for testing database queries.
 */
public final class QueryLoadTestModel extends LoadTestModel<QueryLoadTestModel.QueryApplication> {

  private static final int DEFAULT_MAXIMUM_THINK_TIME_MS = 500;
  private static final int DEFAULT_LOGIN_DELAY_MS = 2;
  private static final int DEFAULT_BATCH_SIZE = 5;

  private final ConnectionPool pool;

  /**
   * Instantiates a new QueryLoadTest.
   * @param database the database
   * @param user the user
   * @param scenarios the query scenarios
   * @throws DatabaseException in case of an exception while constructing the initial connections
   */
  public QueryLoadTestModel(final Database database, final User user, final Collection<? extends QueryScenario> scenarios) throws DatabaseException {
    super(user, scenarios, DEFAULT_MAXIMUM_THINK_TIME_MS, DEFAULT_LOGIN_DELAY_MS, DEFAULT_BATCH_SIZE);
    final ConnectionPoolProvider poolProvider = ConnectionPoolProvider.getConnectionPoolProvider();
    this.pool = poolProvider.createConnectionPool(database, user);
    addShutdownListener(pool::close);
  }

  /**
   * @return the underlying connection pool
   */
  public ConnectionPool getConnectionPool() {
    return pool;
  }

  @Override
  protected void disconnectApplication(final QueryApplication application) {/*Not required*/}

  @Override
  protected QueryApplication initializeApplication() {
    return new QueryApplication(pool);
  }

  /**
   * A class used internally
   */
  public static final class QueryApplication {

    private final ConnectionPool pool;

    private QueryApplication(final ConnectionPool pool) {
      this.pool = pool;
    }
  }

  /**
   * A usage scenario based on a SQL query.
   */
  public static class QueryScenario extends AbstractUsageScenario<QueryApplication> {

    private final User user;
    private final String query;
    private final boolean transactional;

    /**
     * Instantiates a new non-transactional QueryScenario.
     * @param user the user
     * @param name a unique name for the scenario
     * @param query the query
     */
    public QueryScenario(final User user, final String name, final String query) {
      this(user, name, query, false);
    }

    /**
     * Instantiates a new QueryScenario.
     * @param user the user
     * @param name a unique name for the scenario
     * @param query the query
     * @param transactional if true, commit and rollback is performed on success and error respectively
     */
    public QueryScenario(final User user, final String name, final String query, final boolean transactional) {
      super(name);
      this.user = user;
      this.query = query;
      this.transactional = transactional;
    }

    /**
     * @param application the connection pool providing connections
     * @throws ScenarioException in case of an exception during the scenario run
     */
    @Override
    protected final void performScenario(final QueryApplication application) throws ScenarioException {
      Connection connection = null;
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      try {
        connection = application.pool.getConnection(user);
        statement = connection.prepareCall(query);
        setStatementParameters(statement);
        resultSet = statement.executeQuery();
        fetchResult(resultSet);
        if (transactional) {
          connection.commit();
        }
      }
      catch (final Exception e) {
        if (transactional && connection != null) {
          try {
            connection.rollback();
          }
          catch (final SQLException ignored) {/*ignored*/}
        }
        throw new ScenarioException(e);
      }
      finally {
        Database.closeSilently(connection);
        Database.closeSilently(resultSet);
        Database.closeSilently(statement);
      }
    }

    /**
     * For overriding, returns the parameter values to use for the next query execution,
     * these must of course match the parameter slots in the underlying query.
     * @return a list of parameters for the next query run
     */
    protected List<Object> getParameters() {
      return emptyList();
    }

    private void setStatementParameters(final PreparedStatement statement) throws SQLException {
      final List<Object> parameters = getParameters();
      if (!Util.nullOrEmpty(parameters)) {
        int index = 1;
        for (final Object parameter : parameters) {
          statement.setObject(index++, parameter);
        }
      }
    }

    private static void fetchResult(final ResultSet resultSet) throws SQLException {
      final int columnCount = resultSet.getMetaData().getColumnCount();
      if (columnCount > 0) {
        while (resultSet.next()) {
          for (int i = 1; i <= columnCount; i++) {
            resultSet.getObject(i);
          }
        }
      }
    }
  }
}
