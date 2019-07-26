/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.Version;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * An abstract EntityConnectionProvider implementation.
 * @param <T> the EntityConnection implementation provided by this connection provider
 */
public abstract class AbstractEntityConnectionProvider<T extends EntityConnection> implements EntityConnectionProvider<T> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityConnectionProvider.class);
  protected static final String IS_CONNECTED = "isConnected";
  private final Object lock = new Object();
  private final Event onConnectEvent = Events.event();

  /**
   * The user used by this connection provider when connecting to the database server
   */
  private User user;
  private String domainClassName;
  private UUID clientId = UUID.randomUUID();
  private Version clientVersion;
  private String clientTypeId;

  private T entityConnection;
  private Domain domain;
  private EntityConditions entityConditions;

  /** {@inheritDoc} */
  @Override
  public final Domain getDomain() {
    synchronized (lock) {
      if (domain == null) {
        doConnect();
      }

      return domain;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConditions getConditions() {
    synchronized (lock) {
      if (entityConditions == null) {
        entityConditions = new EntityConditions(getDomain());
      }

      return entityConditions;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final User getUser() {
    synchronized (lock) {
      return user;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider setUser(final User user) {
    synchronized (lock) {
      disconnect();
      this.user = user;

      return this;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final String getDomainClassName() {
    synchronized (lock) {
      if (Util.nullOrEmpty(domainClassName)) {
        throw new IllegalArgumentException("Domain class name has not been specified");
      }

      return domainClassName;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider setDomainClassName(final String domainClassName) {
    synchronized (lock) {
      disconnect();
      this.domainClassName = Objects.requireNonNull(domainClassName);

      return this;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final UUID getClientId() {
    synchronized (lock) {
      return clientId;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider setClientId(final UUID clientId) {
    synchronized (lock) {
      disconnect();
      this.clientId = Objects.requireNonNull(clientId);

      return this;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final String getClientTypeId() {
    synchronized (lock) {
      if (Util.nullOrEmpty(clientTypeId)) {
        throw new IllegalArgumentException("Client type id has not been specified");
      }

      return clientTypeId;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider setClientTypeId(final String clientTypeId) {
    synchronized (lock) {
      disconnect();
      this.clientTypeId = Objects.requireNonNull(clientTypeId);

      return this;
    }
  }

  @Override
  public final Version getClientVersion() {
    synchronized (lock) {
      return clientVersion;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider setClientVersion(final Version clientVersion) {
    synchronized (lock) {
      disconnect();
      this.clientVersion = clientVersion;

      return this;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isConnected() {
    synchronized (lock) {
      return entityConnection != null;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isConnectionValid() {
    synchronized (lock) {
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
  }

  /** {@inheritDoc} */
  @Override
  public void addOnConnectListener(final EventListener listener) {
    onConnectEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeOnConnectListener(final EventListener listener) {
    onConnectEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final T getConnection() {
    synchronized (lock) {
      if (user == null) {
        throw new IllegalStateException("No user set");
      }

      validateConnection();

      return entityConnection;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void disconnect() {
    synchronized (lock) {
      if (isConnectionValid()) {
        disconnect(entityConnection);
        entityConnection = null;
      }
    }
    onDisconnect();
  }

  /**
   * @return an established connection
   */
  protected abstract T connect();

  /**
   * Disconnects the given connection
   * @param connection the connection to be disconnected
   */
  protected abstract void disconnect(final T connection);

  /**
   * Called after {@link #disconnect()}, default implementation is, empty provided for subclasses
   */
  protected void onDisconnect() {}

  protected static String getDomainId(final String domainClass) {
    if (domainClass.contains(".")) {
      return domainClass.substring(domainClass.lastIndexOf('.') + 1);
    }

    return domainClass;
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
    if (user == null) {
      throw new IllegalStateException("User has not been set for this connection provider");
    }
    entityConnection = connect();
    domain = entityConnection.getDomain().registerDomain();
    onConnectEvent.fire();
  }
}
