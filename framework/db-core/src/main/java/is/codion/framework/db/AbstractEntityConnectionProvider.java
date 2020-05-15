/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.event.Event;
import is.codion.common.event.EventListener;
import is.codion.common.event.Events;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.domain.entity.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

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
  private Entities entities;

  @Override
  public final Entities getEntities() {
    synchronized (lock) {
      if (entities == null) {
        doConnect();
      }

      return entities;
    }
  }

  @Override
  public final User getUser() {
    synchronized (lock) {
      return user;
    }
  }

  @Override
  public final EntityConnectionProvider setUser(final User user) {
    synchronized (lock) {
      disconnect();
      this.user = user;

      return this;
    }
  }

  @Override
  public final String getDomainClassName() {
    synchronized (lock) {
      if (nullOrEmpty(domainClassName)) {
        throw new IllegalArgumentException("Domain class name has not been specified");
      }

      return domainClassName;
    }
  }

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

  @Override
  public final UUID getClientId() {
    synchronized (lock) {
      return clientId;
    }
  }

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

  @Override
  public final String getClientTypeId() {
    synchronized (lock) {
      if (nullOrEmpty(clientTypeId)) {
        throw new IllegalArgumentException("Client type id has not been specified");
      }

      return clientTypeId;
    }
  }

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

  @Override
  public final EntityConnectionProvider setClientVersion(final Version clientVersion) {
    synchronized (lock) {
      disconnect();
      this.clientVersion = clientVersion;

      return this;
    }
  }

  @Override
  public final boolean isConnected() {
    synchronized (lock) {
      return entityConnection != null;
    }
  }

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

  @Override
  public void addOnConnectListener(final EventListener listener) {
    onConnectEvent.addListener(listener);
  }

  @Override
  public void removeOnConnectListener(final EventListener listener) {
    onConnectEvent.removeListener(listener);
  }

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
    entities = entityConnection.getEntities().register();
    onConnectEvent.onEvent();
  }
}
