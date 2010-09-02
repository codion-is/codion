/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.EntityDbConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * A class responsible for managing a local db connection.
 */
public final class EntityDbLocalProvider extends AbstractEntityDbProvider {

  private static final Logger LOG = LoggerFactory.getLogger(EntityDbLocalProvider.class);

  /**
   * The underlying database implementation
   */
  private final Database database;

  private final Properties connectionProperties = new Properties();

  /**
   * Instantiates a new EntityDbLocalProvider
   * @param user the user
   */
  public EntityDbLocalProvider(final User user) {
    this(user, DatabaseProvider.createInstance());
  }

  /**
   * Instantiates a new EntityDbLocalProvider
   * @param user the user
   * @param database the Database implementation
   */
  public EntityDbLocalProvider(final User user, final Database database) {
    super(user);
    Util.rejectNullValue(database, "database");
    this.database = database;
    this.connectionProperties.put("user", user.getUsername());
    this.connectionProperties.put(Database.PASSWORD_PROPERTY, user.getPassword());
  }

  /** {@inheritDoc} */
  public String getDescription() {
    final String sid = database.getSid();
    if (sid == null) {
      return database.getHost();
    }

    return sid;
  }

  /** {@inheritDoc} */
  public void disconnect() {
    if (getEntityDbInternal() != null && getEntityDbInternal().isValid()) {
      getEntityDbInternal().disconnect();
      if (((EntityDbConnection) getEntityDbInternal()).getDatabase().isEmbedded()) {
        ((EntityDbConnection) getEntityDbInternal()).getDatabase().shutdownEmbedded(connectionProperties);
      }
      setEntityDb(null);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected EntityDb connect() {
    try {
      LOG.debug("Initializing connection for " + getUser());
      return new EntityDbConnection(database, getUser());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected boolean isConnectionValid() {
    return isConnected() && getEntityDbInternal().isValid();
  }
}