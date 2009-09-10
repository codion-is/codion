/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.AuthenticationException;
import org.jminor.common.db.Database;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.Dbms;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;

import org.apache.log4j.Logger;

import java.util.Properties;

/**
 * A class responsible for managing a local db connection
 */
public class EntityDbLocalProvider implements EntityDbProvider {

  private static final Logger log = Util.getLogger(EntityDbLocalProvider.class);

  /**
   * The user used by this db provider when connecting to the database server
   */
  protected final User user;

  /**
   * The EntityDb instance used by this db provider
   */
  protected EntityDb entityDb;

  private final Properties connectionProperties = new Properties();

  public EntityDbLocalProvider(final User user) {
    this.user = user;
    this.connectionProperties.put("user", user.getUsername());
    this.connectionProperties.put("password", user.getPassword());
    final String sid = System.getProperty(Dbms.DATABASE_SID);
    if (sid != null)
      user.setProperty(Dbms.DATABASE_SID, sid);
  }

  /** {@inheritDoc} */
  public final EntityDb getEntityDb() throws UserException {
    validateDbConnection();

    return entityDb;
  }

  /** {@inheritDoc} */
  public void logout() throws UserException {
    try {
      getEntityDb().logout();
      Database.get().shutdownEmbedded(connectionProperties);
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  private void validateDbConnection() throws UserException {
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
      throw new UserException(e);
    }
  }

  private void connect() throws ClassNotFoundException, AuthenticationException {
    log.debug("Initializing connection for " + user);
    entityDb = new EntityDbConnection(user);
  }
}