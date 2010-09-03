/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.tools;

import org.jminor.common.db.DbConnectionImpl;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolImpl;
import org.jminor.common.db.pool.PoolableConnection;
import org.jminor.common.db.pool.PoolableConnectionProvider;
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
public final class QueryLoadTestModel extends LoadTestModel {

  private final ConnectionPool pool;

  /**
   * Instantiates a new QueryLoadTest.
   * @param database the database
   * @param user the user
   * @param scenarios the query scenarios
   */
  public QueryLoadTestModel(final Database database, final User user, final Collection<? extends QueryScenario> scenarios) {
    super(user, scenarios, 100, 2, 5, 10);
    final long time = System.currentTimeMillis();
    this.pool = new ConnectionPoolImpl(new ConnectionProvider(database), user);
    addExitListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        pool.close();
      }
    });
    while (pool.getStatistics(time).getSize() > 0) {
      try {
        Thread.sleep(20);
      }
      catch (InterruptedException e) {/**/}
    }
  }

  /**
   * @return the underlying connection pool
   */
  public ConnectionPool getConnectionPool() {
    return pool;
  }

  /** {@inheritDoc} */
  @Override
  protected Object initializeApplication() throws CancelException {
    try {
      return pool.getConnection();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void disconnectApplication(final Object application) {
    pool.returnConnection((PoolableConnection) application);
  }

  /**
   * A usage scenario based on a SQL query.
   */
  public static class QueryScenario extends AbstractUsageScenario {

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
     * @param application the application
     * @throws ScenarioException
     */
    @Override
    protected final void performScenario(final Object application) throws ScenarioException {
      PoolableConnection connection = null;
      PreparedStatement statement = null;
      try {
        connection = (PoolableConnection) application;
        statement = connection.getConnection().prepareCall(query);
        final List<Object> parameters = getParameters();
        if (parameters != null && !parameters.isEmpty()) {
          int index = 1;
          for (final Object parameter : getParameters()) {
            statement.setObject(index++, parameter);
          }
        }
        final ResultSet rs = statement.executeQuery();
        final int columnCount = rs.getMetaData().getColumnCount();
        if (columnCount > 0) {
          while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
              rs.getObject(i);
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
        Util.closeSilently(statement);
      }
    }

    /**
     * Returns the parameter values to use for the next query execution,
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
      return new DbConnectionImpl(database, user, database.createConnection(user));
    }

    /** {@inheritDoc} */
    public void destroyConnection(final PoolableConnection connection) {
      connection.disconnect();
    }
  }
}
