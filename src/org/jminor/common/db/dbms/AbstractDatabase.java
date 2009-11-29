package org.jminor.common.db.dbms;

public abstract class AbstractDatabase implements Database {

  private final String databaseType;
  private final String host;
  private final String sid;
  private final String port;
  private final boolean embedded;

  /**
   * Instantiates a new AbstractDatabase using host/port/sid/embedded settings specified
   * by system properties
   * @param databaseType a string identifying the database type
   * @see #DATABASE_HOST
   * @see #DATABASE_PORT
   * @see #DATABASE_SID
   * @see #DATABASE_EMBEDDED
   */
  public AbstractDatabase(final String databaseType) {
    this(databaseType, System.getProperty(DATABASE_HOST));
  }

  /**
   * Instantiates a new AbstractDatabase using port/sid/embedded settings specified
   * by system properties
   * @param databaseType a string identifying the database type
   * @param host the database host name
   * @see #DATABASE_PORT
   * @see #DATABASE_SID
   * @see #DATABASE_EMBEDDED
   */
  public AbstractDatabase(final String databaseType, final String host) {
    this(databaseType, host, System.getProperty(DATABASE_PORT));
  }

  /**
   * Instantiates a new AbstractDatabase using sid/embedded settings specified
   * by system properties
   * @param databaseType a string identifying the database type
   * @param host the database host name
   * @param port the database server port
   * @see #DATABASE_SID
   * @see #DATABASE_EMBEDDED
   */
  public AbstractDatabase(final String databaseType, final String host, final String port) {
    this(databaseType, host, port, System.getProperty(DATABASE_SID));
  }

  /**
   * Instantiates a new AbstractDatabase using the embedded settings specified
   * by the system property
   * @param databaseType a string identifying the database type
   * @param host the database host name
   * @param port the database server port
   * @param sid the service identifier
   * @see #DATABASE_EMBEDDED
   */
  public AbstractDatabase(final String databaseType, final String host, final String port, final String sid) {
    this(databaseType, host, port, sid, System.getProperty(Database.DATABASE_EMBEDDED, "false").equalsIgnoreCase("true"));
  }

  /**
   * Instantiates a new AbstractDatabase
   * @param databaseType a string identifying the database type
   * @param host the database host name
   * @param port the database server port
   * @param sid the service identifier
   * @param embedded true if the database is embedded
   */
  public AbstractDatabase(final String databaseType, final String host, final String port, final String sid,
                          final boolean embedded) {
    this.databaseType = databaseType;
    this.host = host;
    this.port = port;
    this.sid = sid;
    this.embedded = embedded;
  }

  /** {@inheritDoc} */
  public String getDatabaseType() {
    return databaseType;
  }

  /** {@inheritDoc} */
  public String getHost() {
    return host;
  }

  /** {@inheritDoc} */
  public String getPort() {
    return port;
  }

  /** {@inheritDoc} */
  public String getSid() {
    return sid;
  }

  /** {@inheritDoc} */
  public boolean isEmbedded() {
    return embedded;
  }
}
