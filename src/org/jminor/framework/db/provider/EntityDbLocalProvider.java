/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.db.User;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.EntityDbConnection;

import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Properties;

/**
 * A class responsible for managing a local db connection.
 */
public class EntityDbLocalProvider implements EntityDbProvider {

  private static final Logger log = Util.getLogger(EntityDbLocalProvider.class);

  /**
   * The user used by this db provider when connecting to the database server
   */
  protected final User user;

  protected final Database database;

  /**
   * The EntityDb instance used by this db provider
   */
  protected EntityDbConnection entityDb;

  private final Properties connectionProperties = new Properties();

  public EntityDbLocalProvider(final User user) {
    this(user, DatabaseProvider.createInstance());
  }

  public EntityDbLocalProvider(final User user, final Database database) {
    if (user == null)
      throw new RuntimeException("User is null");
    if (database == null)
      throw new RuntimeException("Database is null");
    this.user = user;
    this.database = database;
    this.connectionProperties.put("user", user.getUsername());
    this.connectionProperties.put("password", user.getPassword());
  }

  /** {@inheritDoc} */
  public final EntityDb getEntityDb() {
    validateDbConnection();

    return entityDb;
  }

  public String getDescription() {
    final String sid = database.getSid();
    if (sid == null)
      return database.getHost();

    return sid;
  }

  /** {@inheritDoc} */
  public void disconnect() {
    try {
      if (entityDb != null && entityDb.isConnectionValid()) {
        entityDb.disconnect();
        if (entityDb.getDatabase().isEmbedded())
          entityDb.getDatabase().shutdownEmbedded(connectionProperties);
        entityDb = null;
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void validateDbConnection() {
    try {
      if (entityDb == null)
        connect();

      if (!entityDb.isConnectionValid()) {
        //db unreachable
        //try to reconnect once in case db has become reachable
        entityDb = null;
        connect();
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void connect() throws ClassNotFoundException, SQLException {
    log.debug("Initializing connection for " + user);
    entityDb = new EntityDbConnection(database, user);
  }
}