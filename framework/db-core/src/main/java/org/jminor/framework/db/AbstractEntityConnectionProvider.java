/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.TaskScheduler;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.Version;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * An abstract EntityConnectionProvider implementation.
 */
public abstract class AbstractEntityConnectionProvider<T extends EntityConnection> implements EntityConnectionProvider<T> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityConnectionProvider.class);
  private static final int VALIDITY_CHECK_INTERVAL_SECONDS = 10;
  protected static final String IS_CONNECTED = "isConnected";
  private final State connectedState = States.state();
  private final boolean scheduleValidityCheck;
  private final TaskScheduler validityCheckScheduler = new TaskScheduler(this::checkValidity,
          VALIDITY_CHECK_INTERVAL_SECONDS, 0, TimeUnit.SECONDS);

  /**
   * The user used by this connection provider when connecting to the database server
   */
  private User user;
  private String domainClassName;
  private UUID clientId = UUID.randomUUID();
  private Version clientVersion;
  private String clientTypeId;

  private T entityConnection;
  private Entities domain;
  private EntityConditions entityConditions;

  /**
   * Instantiates a new AbstractEntityConnectionProvider.
   * @param scheduleValidityCheck if true then a connection validity check is run every 10 seconds
   */
  public AbstractEntityConnectionProvider(final boolean scheduleValidityCheck) {
    this.scheduleValidityCheck = scheduleValidityCheck;
    if (this.scheduleValidityCheck) {
      this.validityCheckScheduler.start();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Entities getDomain() {
    if (domain == null) {
      doConnect();
    }

    return domain;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConditions getConditions() {
    if (entityConditions == null) {
      entityConditions = new EntityConditions(getDomain());
    }

    return entityConditions;
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getConnectedObserver() {
    return connectedState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final synchronized User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  @Override
  public final synchronized EntityConnectionProvider setUser(final User user) {
    disconnect();
    this.user = user;

    return this;
  }

  /** {@inheritDoc} */
  @Override
  public String getDomainClassName() {
    if (Util.nullOrEmpty(domainClassName)) {
      throw new IllegalArgumentException("Domain class name has not been specified");
    }

    return domainClassName;
  }

  /** {@inheritDoc} */
  @Override
  public EntityConnectionProvider setDomainClassName(final String domainClassName) {
    disconnect();
    this.domainClassName = Objects.requireNonNull(domainClassName);

    return this;
  }

  /** {@inheritDoc} */
  @Override
  public UUID getClientId() {
    return clientId;
  }

  /** {@inheritDoc} */
  @Override
  public EntityConnectionProvider setClientId(final UUID randomUUID) {
    disconnect();
    this.clientId = Objects.requireNonNull(clientId);

    return this;
  }

  /** {@inheritDoc} */
  @Override
  public String getClientTypeId() {
    if (Util.nullOrEmpty(clientTypeId)) {
      throw new IllegalArgumentException("Client type id has not been specified");
    }

    return clientTypeId;
  }

  /** {@inheritDoc} */
  @Override
  public EntityConnectionProvider setClientTypeId(final String clientTypeId) {
    disconnect();
    this.clientTypeId = Objects.requireNonNull(clientTypeId);

    return this;
  }

  @Override
  public Version getClientVersion() {
    return clientVersion;
  }

  /** {@inheritDoc} */
  @Override
  public EntityConnectionProvider setClientVersion(final Version clientVersion) {
    disconnect();
    this.clientVersion = clientVersion;

    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final synchronized boolean isConnected() {
    return entityConnection != null;
  }

  /** {@inheritDoc} */
  @Override
  public final synchronized T getConnection() {
    if (user == null) {
      throw new IllegalStateException("No user set");
    }

    validateConnection();

    return entityConnection;
  }

  /** {@inheritDoc} */
  @Override
  public final synchronized void disconnect() {
    if (isConnectionValid()) {
      disconnect(entityConnection);
      entityConnection = null;
      connectedState.setActive(false);
    }
  }

  /**
   * @return true if the connection is valid, false if it is invalid or has not been initialized
   */
  protected final boolean isConnectionValid() {
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

  /**
   * @return an established connection
   */
  protected abstract T connect();

  /**
   * Disconnects the given connection
   * @param connection the connection to be disconnected
   */
  protected abstract void disconnect(final EntityConnection connection);

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
    connectedState.setActive(true);
    if (scheduleValidityCheck) {
      validityCheckScheduler.start();
    }
  }

  private void checkValidity() {
    connectedState.setActive(isConnectionValid());
    if (!connectedState.isActive()) {
      validityCheckScheduler.stop();
    }
  }
}
