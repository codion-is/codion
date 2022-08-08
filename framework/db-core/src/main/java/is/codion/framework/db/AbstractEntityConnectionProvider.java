/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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

import static java.util.Objects.requireNonNull;

/**
 * An abstract EntityConnectionProvider implementation.
 */
public abstract class AbstractEntityConnectionProvider implements EntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityConnectionProvider.class);
  protected static final String IS_CONNECTED = "isConnected";
  private final Object lock = new Object();
  private final Event<EntityConnection> onConnectEvent = Event.event();

  private final User user;
  private final String domainClassName;
  private final UUID clientId;
  private final Version clientVersion;
  private final String clientTypeId;

  private EntityConnection entityConnection;
  private Entities entities;

  protected AbstractEntityConnectionProvider(AbstractBuilder<?, ?> builder) {
    requireNonNull(builder);
    this.user = requireNonNull(builder.user, "user");
    this.domainClassName = requireNonNull(builder.domainClassName, "domainClassName");
    this.clientId = requireNonNull(builder.clientId, "clientId");
    this.clientTypeId = builder.clientTypeId;
    this.clientVersion = builder.clientVersion;
  }

  @Override
  public final Entities entities() {
    synchronized (lock) {
      if (entities == null) {
        doConnect();
      }

      return entities;
    }
  }

  @Override
  public final User user() {
    return user;
  }

  @Override
  public final String domainClassName() {
    return domainClassName;
  }

  @Override
  public final UUID clientId() {
    return clientId;
  }

  @Override
  public final String clientTypeId() {
    return clientTypeId;
  }

  @Override
  public final Version clientVersion() {
    return clientVersion;
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
  public final EntityConnection connection() {
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
      }
      entityConnection = null;
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

  protected String domainTypeName(String domainClass) {
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
    entities = entityConnection.entities();
    onConnectEvent.onEvent(entityConnection);
  }

  public abstract static class AbstractBuilder<T extends EntityConnectionProvider,
          B extends Builder<T, B>> implements Builder<T, B> {

    private final String connectionType;

    private User user;
    private String domainClassName = CLIENT_DOMAIN_CLASS.get();
    private UUID clientId = UUID.randomUUID();
    private String clientTypeId;
    private Version clientVersion;

    protected AbstractBuilder(String connectionType) {
      this.connectionType = requireNonNull(connectionType);
    }

    @Override
    public final String connectionType() {
      return connectionType;
    }

    @Override
    public final B user(User user) {
      this.user = requireNonNull(user);
      return (B) this;
    }

    @Override
    public final B domainClassName(String domainClassName) {
      this.domainClassName = requireNonNull(domainClassName);
      return (B) this;
    }

    @Override
    public final B clientId(UUID clientId) {
      this.clientId = requireNonNull(clientId);
      return (B) this;
    }

    @Override
    public final B clientTypeId(String clientTypeId) {
      this.clientTypeId = requireNonNull(clientTypeId);
      return (B) this;
    }

    @Override
    public final B clientVersion(Version clientVersion) {
      this.clientVersion = clientVersion;
      return (B) this;
    }
  }
}
