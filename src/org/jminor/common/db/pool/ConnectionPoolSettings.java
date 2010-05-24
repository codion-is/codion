/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.model.User;

import java.io.Serializable;

/**
 * A class encapsulating database connection pool settings
 */
public class ConnectionPoolSettings implements Serializable {

  private static final long serialVersionUID = 1;

  private final User user;
  private int poolCleanupInterval;
  private boolean enabled;
  private int pooledConnectionTimeout;
  private int maximumPoolSize;
  private int minimumPoolSize;

  public ConnectionPoolSettings(final User user, final boolean enabled, final int pooledConnectionTimeout,
                                final int minimumPoolSize, final int poolCleanupInterval) {
    this.user = user;
    this.poolCleanupInterval = poolCleanupInterval;
    setEnabled(enabled);
    setPooledConnectionTimeout(pooledConnectionTimeout);
    setMinimumPoolSize(minimumPoolSize);
    setMaximumPoolSize(minimumPoolSize * 2);
  }

  public User getUser() {
    return user;
  }

  public int getPoolCleanupInterval() {
    return poolCleanupInterval;
  }

  public void setPoolCleanupInterval(final int poolCleanupInterval) {
    this.poolCleanupInterval = poolCleanupInterval;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public int getMinimumPoolSize() {
    return minimumPoolSize;
  }

  public void setMinimumPoolSize(final int minimumPoolSize) {
    this.minimumPoolSize = minimumPoolSize;
  }

  public int getMaximumPoolSize() {
    return maximumPoolSize;
  }

  public void setMaximumPoolSize(final int maximumPoolSize) {
    this.maximumPoolSize = maximumPoolSize;
  }

  public int getPooledConnectionTimeout() {
    return pooledConnectionTimeout;
  }

  public void setPooledConnectionTimeout(final int pooledConnectionTimeout) {
    this.pooledConnectionTimeout = pooledConnectionTimeout;
  }

  public void set(final ConnectionPoolSettings poolSettings) {
    this.enabled = poolSettings.enabled;
    this.maximumPoolSize = poolSettings.maximumPoolSize;
    this.minimumPoolSize = poolSettings.minimumPoolSize;
    this.poolCleanupInterval = poolSettings.poolCleanupInterval;
    this.pooledConnectionTimeout = poolSettings.pooledConnectionTimeout;
  }

  public static ConnectionPoolSettings getDefault(final User user) {
    return new ConnectionPoolSettings(user, true, 60000, 4, 20000);
  }
}