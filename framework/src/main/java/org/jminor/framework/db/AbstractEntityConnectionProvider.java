/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.model.TaskScheduler;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * An abstract EntityConnectionProvider implementation.
 */
public abstract class AbstractEntityConnectionProvider implements EntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityConnectionProvider.class);
  private static final int VALIDITY_CHECK_INTERVAL_SECONDS = 10;
  protected static final String IS_CONNECTED = "isConnected";
  private final State connectedState = States.state();
  private final boolean scheduleValidityCheck;
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
   * @param user the user to base the connection provider on
   */
  public AbstractEntityConnectionProvider(final User user) {
    this(user, false);
  }

  /**
   * Instantiates a new AbstractEntityConnectionProvider.
   * @param user the user to base the connection provider on
   * @param scheduleValidityCheck if true then a connection validity check is run every 10 seconds
   */
  public AbstractEntityConnectionProvider(final User user, final boolean scheduleValidityCheck) {
    Util.rejectNullValue(user, "user");
    this.user = user;
    this.scheduleValidityCheck = scheduleValidityCheck;
    if (this.scheduleValidityCheck) {
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

  /** {@inheritDoc} */
  @Override
  public final synchronized void disconnect() {
    if (isConnectionValid()) {
      doDisconnect();
      entityConnection = null;
    }
  }

  /**
   * @return true if the connection is valid, false if it is invalid or has not been initialized
   */
  protected final boolean isConnectionValid() {
    if (!isConnected()) {
      return false;
    }
    try {
      return entityConnection.isConnected();
    }
    catch (final RuntimeException e) {
      LOG.debug("Connection deemed invalid", e);
      return false;
    }
  }

  /**
   * @return an established connection
   */
  protected abstract EntityConnection connect();

  /**
   * Disconnects the underlying connection
   */
  protected abstract void doDisconnect();

  /**
   * @return the underlying EntityConnection object
   */
  protected final synchronized EntityConnection getConnectionInternal() {
    return entityConnection;
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
      catch (final Exception ignored) {/*ignored*/}
      entityConnection = null;
      doConnect();
    }
  }

  private void doConnect() {
    entityConnection = connect();
    if (scheduleValidityCheck) {
      validityCheckScheduler.start();
    }
  }
}
