/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityDb;

/**
 * An abstract EntityDbProvider implementation.
 */
public abstract class AbstractEntityDbProvider implements EntityDbProvider {

  /**
   * The user used by this db provider when connecting to the database server
   */
  private User user;
  private EntityDb entityDb;

  /**
   * Instantiates a new AbstractEntityDbProvider.
   * @param user the user to base the db provider on
   */
  public AbstractEntityDbProvider(final User user) {
    Util.rejectNullValue(user, "user");
    this.user = user;
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
    return entityDb != null && entityDb.isConnected();
  }

  /** {@inheritDoc} */
  public final EntityDb getEntityDb() {
    if (user == null) {
      throw new IllegalStateException("No user set");
    }

    validateConnection();

    return entityDb;
  }

  /**
   * @return true if the connection is valid
   */
  protected abstract boolean isConnectionValid();

  /**
   * @return an established connection
   */
  protected abstract EntityDb connect();

  protected final EntityDb getEntityDbInternal() {
    return entityDb;
  }

  protected final void setEntityDb(final EntityDb entityDb) {
    this.entityDb = entityDb;
  }

  private void validateConnection() {
    if (entityDb == null || !isConnectionValid()) {
      entityDb = connect();
    }
  }
}
