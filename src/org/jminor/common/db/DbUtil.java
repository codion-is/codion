/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A static utility class.
 */
public final class DbUtil {

  private DbUtil() {}

  /**
   * Closes the given ResultSet instances, swallowing any SQLExceptions that occur
   * @param resultSets the result sets to close
   */
  public static void closeSilently(final ResultSet... resultSets) {
    if (resultSets == null) {
      return;
    }
    for (final ResultSet resultSet : resultSets) {
      try {
        if (resultSet != null) {
          resultSet.close();
        }
      }
      catch (SQLException ignored) {}
    }
  }

  /**
   * Closes the given Statement instances, swallowing any SQLExceptions that occur
   * @param statements the statements to close
   */
  public static void closeSilently(final Statement... statements) {
    if (statements == null) {
      return;
    }
    for (final Statement statement : statements) {
      try {
        if (statement != null) {
          statement.close();
        }
      }
      catch (SQLException ignored) {}
    }
  }
}
