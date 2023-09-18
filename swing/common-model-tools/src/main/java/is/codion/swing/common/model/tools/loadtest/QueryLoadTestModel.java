/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.tools.loadtest;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
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

  private final LoadTestModel<QueryApplication> loadTestModel;
  private final ConnectionPoolWrapper pool;

  /**
   * Instantiates a new QueryLoadTest.
   * @param database the database
   * @param user the user
   * @param scenarios the query scenarios
   * @throws DatabaseException in case of an exception while constructing the initial connections
   */
  public QueryLoadTestModel(Database database, User user, Collection<? extends QueryScenario> scenarios) throws DatabaseException {
    this.loadTestModel = LoadTestModel.builder(this::createApplication, this::disconnectApplication)
            .user(user)
            .usageScenarios(scenarios)
            .minimumThinkTime(DEFAULT_MAXIMUM_THINK_TIME_MS / 2)
            .maximumThinkTime(DEFAULT_MAXIMUM_THINK_TIME_MS)
            .loginDelayFactor(DEFAULT_LOGIN_DELAY_FACTOR)
            .applicationBatchSize(DEFAULT_BATCH_SIZE)
            .build();
    ConnectionPoolFactory poolProvider = ConnectionPoolFactory.instance();
    this.pool = poolProvider.createConnectionPoolWrapper(database, user);
    loadTestModel.addShutdownListener(pool::close);
  }

  public LoadTestModel<QueryApplication> loadTestModel() {
    return loadTestModel;
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

  /**
   * A usage scenario based on an SQL query.
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
    public QueryScenario(User user, String name, String query) {
      this(user, name, query, false);
    }

    /**
     * Instantiates a new QueryScenario.
     * @param user the user
     * @param name a unique name for the scenario
     * @param query the query
     * @param transactional if true, commit and rollback is performed on success and error respectively
     */
    public QueryScenario(User user, String name, String query, boolean transactional) {
      super(name);
      this.user = user;
      this.query = query;
      this.transactional = transactional;
    }

    /**
     * @param application the connection pool providing connections
     * @throws Exception in case of an exception during the scenario run
     */
    @Override
    protected final void perform(QueryApplication application) throws Exception {
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
