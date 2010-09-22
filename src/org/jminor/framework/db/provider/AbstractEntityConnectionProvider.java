/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityConnection;

import java.util.Timer;
import java.util.TimerTask;

/**
 * An abstract EntityConnectionProvider implementation.
 */
public abstract class AbstractEntityConnectionProvider implements EntityConnectionProvider {

  /**
   * The user used by this db provider when connecting to the database server
   */
  private User user;
  private EntityConnection entityConnection;
  private final State stConnectionValid = States.state();
  private Timer validTimer;

  /**
   * Instantiates a new AbstractEntityConnectionProvider.
   * @param user the user to base the db provider on
   */
  public AbstractEntityConnectionProvider(final User user) {
    Util.rejectNullValue(user, "user");
    this.user = user;
    startValidTimer();
  }

  /** {@inheritDoc} */
  public final User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  public final void setUser(final User user) {
    if (Util.equal(user, this.user)) {
      return;
    }
    disconnect();
    this.user = user;
  }

  /** {@inheritDoc} */
  public final boolean isConnected() {
    return entityConnection != null;
  }

  /** {@inheritDoc} */
  public final StateObserver getConnectedState() {
    return stConnectionValid.getObserver();
  }

  /** {@inheritDoc} */
  public final EntityConnection getConnection() {
    if (user == null) {
      throw new IllegalStateException("No user set");
    }

    validateConnection();

    return entityConnection;
  }

  /**
   * @return true if the connection is valid
   */
  protected abstract boolean isConnectionValid();

  /**
   * @return an established connection
   */
  protected abstract EntityConnection connect();

  protected final EntityConnection getConnectionInternal() {
    return entityConnection;
  }

  protected final void setConnection(final EntityConnection entityConnection) {
    this.entityConnection = entityConnection;
  }

  private void validateConnection() {
    if (entityConnection == null || !isConnectionValid()) {
      entityConnection = connect();
      startValidTimer();
    }
  }

  private void startValidTimer() {
    if (validTimer != null) {
      validTimer.cancel();
    }
    validTimer = new Timer();
    validTimer.schedule(new TimerTask() {
      /** {@inheritDoc} */
      @Override
      public void run() {
        final boolean valid = isConnectionValid();
        stConnectionValid.setActive(valid);
        if (!valid) {
          validTimer.cancel();
        }
      }
    }, 0, 1000);
  }
}
