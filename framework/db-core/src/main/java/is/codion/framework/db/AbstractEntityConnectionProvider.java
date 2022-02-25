/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
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
  private final Event<EntityConnection> onConnectEvent = Event.event();

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
  public final EntityConnectionProvider setUser(User user) {
    synchronized (lock) {
      close();
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
  public final EntityConnectionProvider setDomainClassName(String domainClassName) {
    synchronized (lock) {
      if (nullOrEmpty(domainClassName)) {
        throw new IllegalArgumentException("Domain class name must be specified");
      }
      close();
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
  public final EntityConnectionProvider setClientId(UUID clientId) {
    synchronized (lock) {
      if (clientId == null) {
        throw new IllegalArgumentException("Client id must be specified");
      }
      close();
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
  public final EntityConnectionProvider setClientTypeId(String clientTypeId) {
    synchronized (lock) {
      close();
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
  public final EntityConnectionProvider setClientVersion(Version clientVersion) {
    synchronized (lock) {
      close();
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
      catch (RuntimeException e) {
        LOG.debug("Connection deemed invalid", e);
        return false;
      }
    }
  }

  @Override
  public final void addOnConnectListener(EventDataListener<EntityConnection> listener) {
    onConnectEvent.addDataListener(listener);
  }

  @Override
  public final void removeOnConnectListener(EventDataListener<EntityConnection> listener) {
    onConnectEvent.removeDataListener(listener);
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
  public final void close() {
    synchronized (lock) {
      if (isConnectionValid()) {
        close(entityConnection);
        entityConnection = null;
      }
    }
  }

  /**
   * @return an established connection
   */
  protected abstract EntityConnection connect();

  /**
   * Closes the given connection
   * @param connection the connection to be closed
   */
  protected abstract void close(EntityConnection connection);

  protected String getDomainTypeName(String domainClass) {
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
        entityConnection.close();
      }
      catch (Exception ignored) {/*ignored*/}
      entityConnection = null;
      doConnect();
    }
  }

  private void doConnect() {
    if (user == null) {
      throw new IllegalStateException("User has not been set for this connection provider");
    }
    entityConnection = connect();
    entities = entityConnection.getEntities();
    onConnectEvent.onEvent(entityConnection);
  }
}
