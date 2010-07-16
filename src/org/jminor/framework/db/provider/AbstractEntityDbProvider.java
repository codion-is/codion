/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityDb;

public abstract class AbstractEntityDbProvider implements EntityDbProvider {

  /**
   * The user used by this db provider when connecting to the database server
   */
  private User user;
  protected EntityDb entityDb;

  public AbstractEntityDbProvider(final User user) {
    Util.rejectNullValue(user, "user");
    this.user = user;
  }

  public User getUser() {
    return user;
  }

  public void setUser(final User user) {
    if (Util.equal(user, this.user)) {
      return;
    }
    disconnect();
    this.user = user;
  }

  public boolean isConnected() {
    try {
      return entityDb != null && entityDb.isConnected();
    }
    catch (Exception e) {
      return false;
    }
  }

  public EntityDb getEntityDb() {
    if (user == null) {
      throw new IllegalStateException("No user set");
    }

    validateConnection();

    return entityDb;
  }

  protected abstract boolean isConnectionValid();

  protected abstract EntityDb connect();

  private void validateConnection() {
    try {
      if (entityDb == null || !isConnectionValid()) {
        entityDb = connect();
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
