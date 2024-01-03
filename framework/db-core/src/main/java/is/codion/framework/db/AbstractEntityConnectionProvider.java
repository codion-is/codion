/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.common.event.Event;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * An abstract EntityConnectionProvider implementation.
 */
public abstract class AbstractEntityConnectionProvider implements EntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityConnectionProvider.class);

  private final Object lock = new Object();
  private final Event<EntityConnection> onConnectEvent = Event.event();

  private final User user;
  private final DomainType domainType;
  private final UUID clientId;
  private final Version clientVersion;
  private final String clientTypeId;
  private final Consumer<EntityConnectionProvider> onClose;

  private EntityConnection entityConnection;
  private Entities entities;

  protected AbstractEntityConnectionProvider(AbstractBuilder<?, ?> builder) {
    requireNonNull(builder);
    this.user = requireNonNull(builder.user, "A user must be specified");
    this.domainType = requireNonNull(builder.domainType, "A domainType must be specified");
    this.clientId = requireNonNull(builder.clientId, "A clientId must be specified");
    this.clientTypeId = builder.clientTypeId;
    this.clientVersion = builder.clientVersion;
    this.onClose = builder.onClose;
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
  public final DomainType domainType() {
    return domainType;
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
  public final boolean connectionValid() {
    synchronized (lock) {
      if (entityConnection == null) {
        return false;
      }
      try {
        return entityConnection.connected();
      }
      catch (RuntimeException e) {
        LOG.debug("Connection deemed invalid", e);
        return false;
      }
    }
  }

  @Override
  public final void addOnConnectListener(Consumer<EntityConnection> listener) {
    onConnectEvent.addDataListener(listener);
  }

  @Override
  public final void removeOnConnectListener(Consumer<EntityConnection> listener) {
    onConnectEvent.removeDataListener(listener);
  }

  @Override
  public final EntityConnection connection() {
    synchronized (lock) {
      validateConnection();

      return entityConnection;
    }
  }

  @Override
  public final void close() {
    synchronized (lock) {
      if (connectionValid()) {
        close(entityConnection);
      }
      entityConnection = null;
    }
    if (onClose != null) {
      onClose.accept(this);
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

  private void validateConnection() {
    if (entityConnection == null) {
      doConnect();
    }
    else if (!connectionValid()) {
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
    entityConnection = connect();
    entities = entityConnection.entities();
    onConnectEvent.accept(entityConnection);
  }

  public abstract static class AbstractBuilder<T extends EntityConnectionProvider,
          B extends Builder<T, B>> implements Builder<T, B> {

    private final String connectionType;

    private User user;
    private DomainType domainType;
    private UUID clientId = UUID.randomUUID();
    private String clientTypeId;
    private Version clientVersion;
    private Consumer<EntityConnectionProvider> onClose;

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
    public final B domainType(DomainType domainType) {
      this.domainType = requireNonNull(domainType);
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

    @Override
    public final B onClose(Consumer<EntityConnectionProvider> onClose) {
      this.onClose = requireNonNull(onClose);
      return (B) this;
    }
  }
}
