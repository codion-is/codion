package org.jminor.common.db;

import org.jminor.common.model.User;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DatabaseConnections {

  /**
   * A synchronized query counter
   */
  public static final QueryCounter QUERY_COUNTER = new QueryCounter();

  /**
   * A result packer for fetching integers from an result set containing a single integer column
   */
  public static final ResultPacker<Integer> INT_PACKER = new ResultPacker<Integer>() {
    /** {@inheritDoc} */
    public List<Integer> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Integer> integers = new ArrayList<Integer>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        integers.add(resultSet.getInt(1));
      }

      return integers;
    }
  };

  /**
   * A result packer for fetching strings from an result set containing a single string column
   */
  public static final ResultPacker<String> STRING_PACKER = new ResultPacker<String>() {
    /** {@inheritDoc} */
    public List<String> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<String> strings = new ArrayList<String>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        strings.add(resultSet.getString(1));
      }

      return strings;
    }
  };

  /**
   * Constructs a new instance of the DbConnection class, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param user the user for the db-connection
   * @return a new DatabaseConnection instance
   * @throws SQLException in case there is a problem connecting to the database
   * @throws ClassNotFoundException in case the JDBC driver class was not found
   */
  public static DatabaseConnection createConnection(final Database database, final User user) throws ClassNotFoundException, SQLException {
    return new DatabaseConnectionImpl(database, user, database.createConnection(user));
  }

  /**
   * Constructs a new instance of the DbConnection class, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param user the user for the db-connection
   * @param connection the Connection object to base this DbConnection on
   * @throws SQLException in case there is a problem connecting to the database
   * @return a new DatabaseConnection instance
   */
  public static DatabaseConnection createConnection(final Database database, final User user, final Connection connection) throws SQLException {
    return new DatabaseConnectionImpl(database, user, connection);
  }

  /**
   * @return a DatabaseStatistics object containing the most recent statistics from the underlying database
   */
  public static Database.Statistics getDatabaseStatistics() {
    return new DatabaseStatistics(QUERY_COUNTER.getQueriesPerSecond(),
            QUERY_COUNTER.getSelectsPerSecond(), QUERY_COUNTER.getInsertsPerSecond(),
            QUERY_COUNTER.getDeletesPerSecond(), QUERY_COUNTER.getUpdatesPerSecond());
  }

  /**
   * A class for counting query types, providing avarages over time
   */
  public static final class QueryCounter {

    private static final int DEFAULT_UPDATE_INTERVAL_MS = 2000;

    private long queriesPerSecondTime = System.currentTimeMillis();
    private int queriesPerSecond = 0;
    private int queriesPerSecondCounter = 0;
    private int selectsPerSecond = 0;
    private int selectsPerSecondCounter = 0;
    private int insertsPerSecond = 0;
    private int insertsPerSecondCounter = 0;
    private int updatesPerSecond = 0;
    private int updatesPerSecondCounter = 0;
    private int deletesPerSecond = 0;
    private int deletesPerSecondCounter = 0;
    private int undefinedPerSecond = 0;
    private int undefinedPerSecondCounter = 0;

    private QueryCounter() {
      new Timer(true).schedule(new TimerTask() {
        @Override
        public void run() {
          updateQueriesPerSecond();
        }
      }, new Date(), DEFAULT_UPDATE_INTERVAL_MS);
    }

    /**
     * Counts the given query, base on it's first character
     * @param sql the sql query
     */
    public synchronized void count(final String sql) {
      queriesPerSecondCounter++;
      switch (Character.toLowerCase(sql.charAt(0))) {
        case 's':
          selectsPerSecondCounter++;
          break;
        case 'i':
          insertsPerSecondCounter++;
          break;
        case 'u':
          updatesPerSecondCounter++;
          break;
        case 'd':
          deletesPerSecondCounter++;
          break;
        default:
          undefinedPerSecondCounter++;
      }
    }

    /**
     * @return the number of queries being run per second
     */
    public synchronized int getQueriesPerSecond() {
      return queriesPerSecond;
    }

    /**
     * @return the number of select queries being run per second
     */
    public synchronized int getSelectsPerSecond() {
      return selectsPerSecond;
    }

    /**
     * @return the number of delete queries being run per second
     */
    public synchronized int getDeletesPerSecond() {
      return deletesPerSecond;
    }

    /**
     * @return the number of insert queries being run per second
     */
    public synchronized int getInsertsPerSecond() {
      return insertsPerSecond;
    }

    /**
     * @return the number of update queries being run per second
     */
    public synchronized int getUpdatesPerSecond() {
      return updatesPerSecond;
    }

    /**
     * @return the number of undefined queries being run per second
     */
    public synchronized int getUndefinedPerSecond() {
      return undefinedPerSecond;
    }

    private synchronized void updateQueriesPerSecond() {
      final long current = System.currentTimeMillis();
      final double seconds = (current - queriesPerSecondTime) / 1000d;
      if (seconds > 5) {
        queriesPerSecond = (int) (queriesPerSecondCounter / (double) seconds);
        selectsPerSecond = (int) (selectsPerSecondCounter / (double) seconds);
        insertsPerSecond = (int) (insertsPerSecondCounter / (double) seconds);
        deletesPerSecond = (int) (deletesPerSecondCounter / (double) seconds);
        updatesPerSecond = (int) (updatesPerSecondCounter / (double) seconds);
        undefinedPerSecond = (int) (undefinedPerSecondCounter / (double) seconds);
        queriesPerSecondCounter = 0;
        selectsPerSecondCounter = 0;
        insertsPerSecondCounter = 0;
        deletesPerSecondCounter = 0;
        updatesPerSecondCounter = 0;
        undefinedPerSecondCounter = 0;
        queriesPerSecondTime = current;
      }
    }
  }

  /**
   * A default DatabaseStatistics implementation.
   */
  private static final class DatabaseStatistics implements Database.Statistics, Serializable {

    private static final long serialVersionUID = 1;

    private final long timestamp = System.currentTimeMillis();
    private final int queriesPerSecond;
    private final int selectsPerSecond;
    private final int insertsPerSecond;
    private final int deletesPerSecond;
    private final int updatesPerSecond;

    /**
     * Instantiates a new DbStatistics object
     * @param queriesPerSecond the number of queries being run per second
     * @param selectsPerSecond the number of select queries being run per second
     * @param insertsPerSecond the number of insert queries being run per second
     * @param deletesPerSecond the number of delete queries being run per second
     * @param updatesPerSecond the number of update queries being run per second
     */
    private DatabaseStatistics(final int queriesPerSecond, final int selectsPerSecond, final int insertsPerSecond,
                               final int deletesPerSecond, final int updatesPerSecond) {
      this.queriesPerSecond = queriesPerSecond;
      this.selectsPerSecond = selectsPerSecond;
      this.insertsPerSecond = insertsPerSecond;
      this.deletesPerSecond = deletesPerSecond;
      this.updatesPerSecond = updatesPerSecond;
    }

    /** {@inheritDoc} */
    public int getQueriesPerSecond() {
      return queriesPerSecond;
    }

    /** {@inheritDoc} */
    public int getDeletesPerSecond() {
      return deletesPerSecond;
    }

    /** {@inheritDoc} */
    public int getInsertsPerSecond() {
      return insertsPerSecond;
    }

    /** {@inheritDoc} */
    public int getSelectsPerSecond() {
      return selectsPerSecond;
    }

    /** {@inheritDoc} */
    public int getUpdatesPerSecond() {
      return updatesPerSecond;
    }

    /** {@inheritDoc} */
    public long getTimestamp() {
      return timestamp;
    }
  }
}
