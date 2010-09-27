/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.tools;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnections;
import org.jminor.common.db.PoolableConnection;
import org.jminor.common.db.PoolableConnectionProvider;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPools;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.LoadTestModel;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A load test implementation for testing database queries.
 */
public final class QueryLoadTestModel extends LoadTestModel<QueryLoadTestModel.QueryApplication> {

  private static final int DEFAULT_MAXIMUM_THINK_TIME_MS = 500;
  private static final int DEFAULT_LOGIN_DELAY_MS = 2;
  private static final int DEFAULT_BATCH_SIZE = 5;
  private static final int DEFAULT_QUERY_WARNING_TIME_MS = 50;

  private final ConnectionPool pool;

  /**
   * Instantiates a new QueryLoadTest.
   * @param database the database
   * @param user the user
   * @param scenarios the query scenarios
   */
  public QueryLoadTestModel(final Database database, final User user, final Collection<? extends QueryScenario> scenarios) {
    super(user, scenarios, DEFAULT_MAXIMUM_THINK_TIME_MS, DEFAULT_LOGIN_DELAY_MS, DEFAULT_BATCH_SIZE, DEFAULT_QUERY_WARNING_TIME_MS);
    this.pool = ConnectionPools.createPool(new ConnectionProvider(database), user);
    addExitListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        pool.close();
      }
    });
  }

  /**
   * @return the underlying connection pool
   */
  public ConnectionPool getConnectionPool() {
    return pool;
  }

  @Override
  protected void disconnectApplication(final QueryApplication application) {}

  @Override
  protected QueryApplication initializeApplication() throws CancelException {
    return new QueryApplication(pool);
  }

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

    private static final long serialVersionUID = 1;

    private final String query;
    private final boolean transactional;

    /**
     * Instantiates a new non-transactional QueryScenario.
     * @param name a unique name for the scenario
     * @param query the query
     */
    public QueryScenario(final String name, final String query) {
      this(name, query, false);
    }

    /**
     * Instantiates a new QueryScenario.
     * @param name a unique name for the scenario
     * @param query the query
     * @param transactional if true, commit and rollback is performed on success and error respectively
     */
    public QueryScenario(final String name, final String query, final boolean transactional) {
      super(name);
      this.query = query;
      this.transactional = transactional;
    }

    /**
     *
     * @param application the connection pool providing connections
     * @throws ScenarioException in case of an exception during the scenario run
     */
    @Override
    protected final void performScenario(final QueryApplication application) throws ScenarioException {
      PoolableConnection connection = null;
      PreparedStatement statement = null;
      ResultSet resultSet = null;
      try {
        connection = application.pool.getConnection();
        statement = connection.getConnection().prepareCall(query);
        final List<Object> parameters = getParameters();
        if (parameters != null && !parameters.isEmpty()) {
          int index = 1;
          for (final Object parameter : getParameters()) {
            statement.setObject(index++, parameter);
          }
        }
        resultSet = statement.executeQuery();
        final int columnCount = resultSet.getMetaData().getColumnCount();
        if (columnCount > 0) {
          while (resultSet.next()) {
            for (int i = 1; i <= columnCount; i++) {
              resultSet.getObject(i);
            }
          }
        }
        if (transactional) {
          connection.commit();
        }
      }
      catch (Exception e) {
        if (transactional && connection != null) {
          try {
            connection.rollback();
          }
          catch (SQLException e1) {/**/}
        }
        throw new ScenarioException(e);
      }
      finally {
        if (connection != null) {
          application.pool.returnConnection(connection);
        }
        Util.closeSilently(resultSet);
        Util.closeSilently(statement);
      }
    }

    /**
     * For overriding, returns the parameter values to use for the next query execution,
     * these must of course match the parameter slots in the underlying query.
     * @return a list of parameters for the next query run
     */
    protected List<Object> getParameters() {
      return Collections.emptyList();
    }
  }

  private static class ConnectionProvider implements PoolableConnectionProvider {

    private final Database database;

    private ConnectionProvider(final Database database) {
      this.database = database;
    }

    /** {@inheritDoc} */
    public PoolableConnection createConnection(final User user) throws ClassNotFoundException, SQLException {
      return DatabaseConnections.createConnection(database, user, database.createConnection(user));
    }

    /** {@inheritDoc} */
    public void destroyConnection(final PoolableConnection connection) {
      connection.disconnect();
    }
  }
}
