/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides Database implementations based on system settings.
 * @see Database#DATABASE_IMPLEMENTATION_CLASS
 * @see Database#DATABASE_TYPE
 */
public final class Databases {

  private static Database instance;

  private Databases() {}

  /**
   * @deprecated use {@link #getInstance()}
   * @return a Database instance based on the current runtime database type property
   */
  @Deprecated
  public static synchronized Database createInstance() {
    return getInstance();
  }

  /**
   * @return a Database instance based on the current runtime database type property
   * @see Database#DATABASE_TYPE
   * @see Database#DATABASE_IMPLEMENTATION_CLASS
   * @see Database#getDatabaseType()
   * @throws IllegalArgumentException in case an unsupported database type is specified
   * @throws RuntimeException in case of an exception occurring while instantiating the database implementation instance
   */
  public static synchronized Database getInstance() {
    try {
      final Database.Type currentType = Database.getDatabaseType();
      if (instance == null || !instance.getType().equals(currentType)) {
        //refresh the instance
        instance = (Database) Class.forName(Database.getDatabaseClassName()).getDeclaredConstructor().newInstance();
      }

      return instance;
    }
    catch (final IllegalArgumentException e) {
      throw e;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * A default DatabaseStatistics implementation.
   */
  static final class DatabaseStatistics implements Database.Statistics, Serializable {

    private static final long serialVersionUID = 1;

    private final long timestamp = System.currentTimeMillis();
    private final int queriesPerSecond;
    private final int selectsPerSecond;
    private final int insertsPerSecond;
    private final int deletesPerSecond;
    private final int updatesPerSecond;

    /**
     * Instantiates a new DatabaseStatistics object
     * @param queriesPerSecond the number of queries being run per second
     * @param selectsPerSecond the number of select queries being run per second
     * @param insertsPerSecond the number of insert queries being run per second
     * @param deletesPerSecond the number of delete queries being run per second
     * @param updatesPerSecond the number of update queries being run per second
     */
    DatabaseStatistics(final int queriesPerSecond, final int selectsPerSecond, final int insertsPerSecond,
                       final int deletesPerSecond, final int updatesPerSecond) {
      this.queriesPerSecond = queriesPerSecond;
      this.selectsPerSecond = selectsPerSecond;
      this.insertsPerSecond = insertsPerSecond;
      this.deletesPerSecond = deletesPerSecond;
      this.updatesPerSecond = updatesPerSecond;
    }

    @Override
    public int getQueriesPerSecond() {
      return queriesPerSecond;
    }

    @Override
    public int getDeletesPerSecond() {
      return deletesPerSecond;
    }

    @Override
    public int getInsertsPerSecond() {
      return insertsPerSecond;
    }

    @Override
    public int getSelectsPerSecond() {
      return selectsPerSecond;
    }

    @Override
    public int getUpdatesPerSecond() {
      return updatesPerSecond;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
    }
  }

  /**
   * Annotation for a database operation id, specifying the operation class name
   */
  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Operation {

    /**
     * @return the operation class name
     */
    String className();
  }
}