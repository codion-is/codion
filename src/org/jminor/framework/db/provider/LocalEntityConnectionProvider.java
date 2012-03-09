/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * A class responsible for managing a local EntityConnection.
 */
public final class LocalEntityConnectionProvider extends AbstractEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(LocalEntityConnectionProvider.class);

  /**
   * The underlying database implementation
   */
  private final Database database;

  private final Properties connectionProperties = new Properties();

  /**
   * Instantiates a new LocalEntityConnectionProvider
   * @param user the user
   */
  public LocalEntityConnectionProvider(final User user) {
    this(user, Databases.createInstance());
  }

  /**
   * Instantiates a new LocalEntityConnectionProvider
   * @param user the user
   * @param database the Database implementation
   */
  public LocalEntityConnectionProvider(final User user, final Database database) {
    super(user);
    Util.rejectNullValue(database, "database");
    this.database = database;
    this.connectionProperties.put("user", user.getUsername());
    this.connectionProperties.put(Database.PASSWORD_PROPERTY, user.getPassword());
  }

  /**
   * @return the service identifier (sid) of the underlying database or the hostname no sid is available
   */
  @Override
  public String getDescription() {
    final String sid = database.getSid();
    if (sid == null) {
      return database.getHost();
    }

    return sid;
  }

  /** {@inheritDoc} */
  @Override
  public String getHostName() {
    return database.getHost();
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect() {
    if (getConnectionInternal() != null && getConnectionInternal().isValid()) {
      getConnectionInternal().disconnect();
      if (getConnectionInternal().getDatabaseConnection().getDatabase().isEmbedded()) {//todo is this proper?
        getConnectionInternal().getDatabaseConnection().getDatabase().shutdownEmbedded(connectionProperties);
      }
      setConnection(null);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected EntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", getUser());
      return EntityConnections.createConnection(database, getUser());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected boolean isConnectionValid() {
    return isConnected() && getConnectionInternal().isValid();
  }
}