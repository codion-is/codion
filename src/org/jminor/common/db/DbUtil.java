package org.jminor.common.db;

import org.jminor.common.db.dbms.H2Database;

import org.h2.tools.RunScript;

import java.sql.SQLException;
import java.util.Properties;

public final class DbUtil {

  private DbUtil() {}

  public static void createEmbeddedH2Database(final H2Database database, final String scriptPath) throws SQLException {
    if (!database.isEmbedded()) {
      throw new IllegalArgumentException("Database " + database.getDatabaseType() + " is not embedded");
    }

    final Properties properties = new Properties();
    properties.put("user","sa");
    new RunScript().run(new String[] {"-url", database.getURL(properties), "-showResults", "-script", scriptPath});
  }
}
