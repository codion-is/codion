package org.jminor.common.db.dbms;

public abstract class AbstractDatabase implements Database {

  private final String databaseType;
  private final String host;
  private final String sid;
  private final String port;
  private final boolean embedded;

  public AbstractDatabase(final String databaseType) {
    this(databaseType, System.getProperty(DATABASE_HOST), System.getProperty(DATABASE_PORT),
            System.getProperty(DATABASE_SID), System.getProperty(Database.DATABASE_EMBEDDED, "false").toUpperCase().equals("TRUE"));
  }

  public AbstractDatabase(final String databaseType, final String host) {
    this(databaseType, host, null);
  }

  public AbstractDatabase(final String databaseType, final String host, final String port) {
    this(databaseType, host, port, null);
  }

  public AbstractDatabase(final String databaseType, final String host, final String port, final String sid) {
    this(databaseType, host, port, sid, false);
  }

  public AbstractDatabase(final String databaseType, final String host, final String port, final String sid,
                      final boolean embedded) {
    this.databaseType = databaseType;
    this.host = host;
    this.port = port;
    this.sid = sid;
    this.embedded = embedded;
  }

  public String getDatabaseType() {
    return databaseType;
  }

  public String getHost() {
    return host;
  }

  public String getPort() {
    return port;
  }

  public String getSid() {
    return sid;
  }

  public boolean isEmbedded() {
    return embedded;
  }
}
