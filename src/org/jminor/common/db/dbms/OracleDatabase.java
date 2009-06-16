package org.jminor.common.db.dbms;

import org.jminor.common.model.formats.LongDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;

import java.util.Date;
import java.util.Properties;

public class OracleDatabase implements IDatabase {

  /** {@inheritDoc} */
  public String getDatabaseType() {
    return DATABASE_TYPE_ORACLE;
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName("oracle.jdbc.driver.OracleDriver");
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return "select " + idSource + ".currval from dual";
  }

  /** {@inheritDoc} */
  public String getSequenceSQL(final String sequenceName) {
    return "select " + sequenceName + ".nextval from dual";
  }

  /** {@inheritDoc} */
  public String getSQLDateString(final Date value, final boolean longDate) {
    return longDate ?
            "to_date('" + LongDateFormat.get().format(value) + "', 'DD-MM-YYYY HH24:MI')" :
            "to_date('" + ShortDashDateFormat.get().format(value) + "', 'DD-MM-YYYY')";
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String host = System.getProperty(DATABASE_HOST_PROPERTY);
    if (host == null || host.length() == 0)
      throw new RuntimeException(DATABASE_HOST_PROPERTY + " is required for database type " + getDatabaseType());
    final String port = System.getProperty(DATABASE_PORT_PROPERTY);
    if (port == null || port.length() == 0)
      throw new RuntimeException(DATABASE_PORT_PROPERTY + " is required for database type " + getDatabaseType());
    final String sid = System.getProperty(DATABASE_SID_PROPERTY);
    if (sid == null || sid.length() == 0)
      throw new RuntimeException(DATABASE_SID_PROPERTY + " is required for database type " + getDatabaseType());

    return "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;
  }

  /** {@inheritDoc} */
  public String getUserInfoString(final Properties connectionProperties) {
    return null;
  }

  /** {@inheritDoc} */
  public boolean isEmbedded() {
    return false;
  }

  /** {@inheritDoc} */
  public void onDisconnect(final Properties connectionProperties) {}

  /** {@inheritDoc} */
  public boolean supportsNoWait() {
    return true;
  }

  /** {@inheritDoc} */
  public boolean supportsIsValid() {
    return false;
  }
}
