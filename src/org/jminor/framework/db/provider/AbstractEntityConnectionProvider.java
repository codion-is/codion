/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.TaskScheduler;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * An abstract EntityConnectionProvider implementation.
 */
public abstract class AbstractEntityConnectionProvider implements EntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityConnectionProvider.class);
  private static final boolean SCHEDULE_VALIDITY_CHECK = Configuration.getBooleanValue(Configuration.CLIENT_SCHEDULE_CONNECTION_VALIDATION);
  private static final int VALIDITY_CHECK_INTERVAL_SECONDS = 10;
  protected static final String IS_CONNECTED = "isConnected";
  protected static final String IS_VALID = "isValid";
  private final State connectedState = States.state();
  private final TaskScheduler validityCheckScheduler = new TaskScheduler(new Runnable() {
    @Override
    public void run() {
      connectedState.setActive(isConnectionValid());
      if (!connectedState.isActive()) {
        validityCheckScheduler.stop();
      }
    }
  }, VALIDITY_CHECK_INTERVAL_SECONDS, 0, TimeUnit.SECONDS);

  /**
   * The user used by this connection provider when connecting to the database server
   */
  private User user;
  private EntityConnection entityConnection;

  /**
   * Instantiates a new AbstractEntityConnectionProvider.
   * @param user the user to base the db provider on
   */
  public AbstractEntityConnectionProvider(final User user) {
    Util.rejectNullValue(user, "user");
    this.user = user;
    if (SCHEDULE_VALIDITY_CHECK) {
      this.validityCheckScheduler.start();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getConnectedObserver() {
    return connectedState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final synchronized User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  @Override
  public final synchronized void setUser(final User user) {
    if (Util.equal(user, this.user)) {
      return;
    }
    disconnect();
    this.user = user;
  }

  /** {@inheritDoc} */
  @Override
  public final synchronized boolean isConnected() {
    return entityConnection != null;
  }

  /** {@inheritDoc} */
  @Override
  public final synchronized EntityConnection getConnection() {
    if (user == null) {
      throw new IllegalStateException("No user set");
    }

    validateConnection();

    return entityConnection;
  }

  /**
   * @return true if the connection is valid, false if it is invalid or has not been initialized
   */
  protected abstract boolean isConnectionValid();

  /**
   * @return an established connection
   */
  protected abstract EntityConnection connect();

  protected final synchronized EntityConnection getConnectionInternal() {
    return entityConnection;
  }

  protected final synchronized void setConnection(final EntityConnection entityConnection) {
    this.entityConnection = entityConnection;
  }

  private void validateConnection() {
    if (entityConnection == null) {
      doConnect();
    }
    else if (!isConnectionValid()) {
      LOG.info("Previous connection invalid, reconnecting");
      try {//try to disconnect just in case
        entityConnection.disconnect();
      }
      catch (Exception ignored) {}
      doConnect();
    }
  }

  private void doConnect() {
    entityConnection = connect();
    if (SCHEDULE_VALIDITY_CHECK) {
      validityCheckScheduler.start();
    }
  }
}
