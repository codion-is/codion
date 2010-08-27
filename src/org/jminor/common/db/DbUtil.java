/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.dbms.H2Database;

import org.h2.tools.RunScript;

import java.sql.SQLException;
import java.util.Properties;

/**
 * A static DB utility class.
 */
public final class DbUtil {

  private DbUtil() {}

  /**
   * Initializes a new H2 database, with the given script
   * @param database the H2Database instance
   * @param scriptPath the path to the initilization script
   * @throws SQLException in case of an exception
   */
  public static void createEmbeddedH2Database(final H2Database database, final String scriptPath) throws SQLException {
    if (!database.isEmbedded()) {
      throw new IllegalArgumentException("Database " + database.getDatabaseType() + " is not embedded");
    }

    final Properties properties = new Properties();
    properties.put("user","sa");
    new RunScript().run(new String[] {"-url", database.getURL(properties), "-showResults", "-script", scriptPath});
  }
}
