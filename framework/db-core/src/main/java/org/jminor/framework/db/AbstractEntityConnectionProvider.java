/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.Version;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.Events;
import org.jminor.common.user.User;
import org.jminor.framework.domain.Domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * An abstract EntityConnectionProvider implementation.
 */
public abstract class AbstractEntityConnectionProvider implements EntityConnectionProvider {

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

  private EntityConnection entityConnection;
  private Domain domain;

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
      if (nullOrEmpty(domainClassName)) {
        throw new IllegalArgumentException("Domain class name has not been specified");
      }

      return domainClassName;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider setDomainClassName(final String domainClassName) {
    synchronized (lock) {
      if (nullOrEmpty(domainClassName)) {
        throw new IllegalArgumentException("Domain class name must be specified");
      }
      disconnect();
      this.domainClassName = domainClassName;

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
      if (clientId == null) {
        throw new IllegalArgumentException("Client id must be specified");
      }
      disconnect();
      this.clientId = clientId;

      return this;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final String getClientTypeId() {
    synchronized (lock) {
      if (nullOrEmpty(clientTypeId)) {
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
      this.clientTypeId = requireNonNull(clientTypeId);

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
  public final EntityConnection getConnection() {
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
  }

  /**
   * @return an established connection
   */
  protected abstract EntityConnection connect();

  /**
   * Disconnects the given connection
   * @param connection the connection to be disconnected
   */
  protected abstract void disconnect(EntityConnection connection);

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
    onConnectEvent.onEvent();
  }
}
